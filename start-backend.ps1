# start-backend.ps1 - Start backend (ordered) + Angular frontend (Windows)
# Run from repo root:  .\start-backend.ps1   or double-click start-backend.bat
#
# Environment (optional):
#   SKIP_FRONTEND=1   - backend only
#   FRONTEND_PORT     - default 4200
#   FRONTEND_HOST     - default 127.0.0.1
#   READINESS_HOST    - default 127.0.0.1 (use for HTTP checks; avoids localhost IPv6 issues on Windows)

$ErrorActionPreference = 'Continue'

function Info([string]$msg)  { Write-Host "[INFO]  $msg" -ForegroundColor Green }
function Warn([string]$msg)  { Write-Host "[WARN]  $msg" -ForegroundColor Yellow }
function Die([string]$msg)   { Write-Host "[ERROR] $msg" -ForegroundColor Red; exit 1 }

function Get-ReadinessBaseUrl {
    param([int] $Port)
    $h = if ($env:READINESS_HOST) { $env:READINESS_HOST.Trim() } else { '' }
    if ([string]::IsNullOrWhiteSpace($h)) { $h = '127.0.0.1' }
    return "http://${h}:$Port"
}

function Test-ActuatorHealthUp {
    param([string] $BaseUrl)
    try {
        $r = Invoke-WebRequest -Uri "$BaseUrl/actuator/health" -UseBasicParsing -TimeoutSec 2
        if ($r.StatusCode -ne 200) { return $false }
        $c = $r.Content
        if ($c -match '"status"\s*:\s*"DOWN"') { return $false }
        if ($c -match '"status"\s*:\s*"UP"') { return $true }
        return $false
    } catch { return $false }
}

function Test-ActuatorIndex {
    param([string] $BaseUrl)
    try {
        $r = Invoke-WebRequest -Uri "$BaseUrl/actuator" -UseBasicParsing -TimeoutSec 2
        if ($r.StatusCode -ne 200) { return $false }
        return ($r.Content -match '_links|"href"\s*:\s*"http')
    } catch { return $false }
}

function Test-RootUiReady {
    param([string] $BaseUrl, [string] $Label)
    try {
        $r = Invoke-WebRequest -Uri "$BaseUrl/" -UseBasicParsing -TimeoutSec 2
        if ($r.StatusCode -ne 200) { return $false }
        $c = $r.Content
        if ($c -match 'Eureka|DS Replicas|spring-cloud-netflix-eureka') { return 'Eureka UI' }
        if ($Label -match 'Angular|ng serve|dev server' -and $c -match '(?i)<!DOCTYPE\s+html|<html[\s>]') { return 'dev server HTML' }
        return $false
    } catch { return $false }
}

function Wait-ForPort {
    param(
        [int] $Port,
        [string] $Label,
        [int] $TimeoutSec = 90
    )
    $base = Get-ReadinessBaseUrl -Port $Port
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    $started = Get-Date
    $lastProgressBucket = -1
    Info "Waiting for $Label - probing ${base} (timeout ${TimeoutSec}s)..."
    while ((Get-Date) -lt $deadline) {
        if (Test-ActuatorHealthUp -BaseUrl $base) {
            Info "$Label - ready (Actuator /actuator/health reports UP) - $(([int]((Get-Date) - $started).TotalSeconds))s"
            return
        }
        if (Test-ActuatorIndex -BaseUrl $base) {
            Info "$Label - ready (Actuator index reachable; health endpoint may be restricted) - $(([int]((Get-Date) - $started).TotalSeconds))s"
            return
        }
        $ui = Test-RootUiReady -BaseUrl $base -Label $Label
        if ($ui) {
            Info "$Label - ready ($ui) - $(([int]((Get-Date) - $started).TotalSeconds))s"
            return
        }
        $elapsed = [int](((Get-Date) - $started).TotalSeconds)
        $bucket = [int]([Math]::Floor($elapsed / 10))
        if ($elapsed -ge 10 -and $bucket -gt $lastProgressBucket) {
            $lastProgressBucket = $bucket
            Write-Host "       ... still waiting for $Label (${elapsed}s elapsed)" -ForegroundColor DarkGray
        }
        Start-Sleep -Milliseconds 750
    }
    Warn "$Label - no readiness signal on ${base} within ${TimeoutSec}s (see logs for that service). Continuing."
}

function Get-MavenSpringBootCmdLine {
    param(
        [string] $Directory,
        [string] $LogPath
    )
    $mvnwCmd = Join-Path $Directory 'mvnw.cmd'
    if (Test-Path -LiteralPath $mvnwCmd) {
        return "mvnw.cmd spring-boot:run -q > `"$LogPath`" 2>&1"
    }
    $mvn = Get-Command mvn -ErrorAction SilentlyContinue
    if ($mvn) {
        return "mvn spring-boot:run -q > `"$LogPath`" 2>&1"
    }
    if (-not [string]::IsNullOrWhiteSpace($env:MAVEN_HOME)) {
        $mvnBin = Join-Path $env:MAVEN_HOME 'bin\mvn.cmd'
        if (Test-Path -LiteralPath $mvnBin) {
            return "`"$mvnBin`" spring-boot:run -q > `"$LogPath`" 2>&1"
        }
    }
    Die "Maven not found for this service (no mvnw.cmd in folder, and 'mvn' is not on PATH). Folder: $Directory`nInstall Maven or add it to PATH, or ensure mvnw.cmd exists (see Eureka module)."
}

function Start-MavenService {
    param(
        [string] $Directory,
        [string] $Label,
        [string] $LogFileName
    )
    if (-not (Test-Path -LiteralPath $Directory -PathType Container)) {
        Die "Directory not found: $Directory"
    }
    $logPath = Join-Path $script:LogDir $LogFileName
    $cmdLine = Get-MavenSpringBootCmdLine -Directory $Directory -LogPath $logPath
    $p = Start-Process -FilePath "cmd.exe" -ArgumentList @('/c', $cmdLine) `
        -WorkingDirectory $Directory -WindowStyle Hidden -PassThru
    Add-Content -Path $script:PidsFile -Value $p.Id
    Info "Started $Label (wrapper PID $($p.Id), log -> logs\$LogFileName)"
}

function Start-AngularFrontend {
    param(
        [string] $Directory,
        [string] $LogFileName
    )
    $npm = Get-Command npm -ErrorAction SilentlyContinue
    if (-not $npm) {
        Warn "npm not found in PATH - skipping frontend"
        return $false
    }
    if (-not (Test-Path -LiteralPath $Directory -PathType Container)) {
        Die "Directory not found: $Directory"
    }
    if (-not (Test-Path (Join-Path $Directory 'package.json'))) {
        Die "No package.json in $Directory"
    }
    $logPath = Join-Path $script:LogDir $LogFileName
    if (-not (Test-Path (Join-Path $Directory 'node_modules'))) {
        Warn "node_modules missing - run: cd `"$Directory`" && npm install"
    }
    $hostArg = if ($env:FRONTEND_HOST) { $env:FRONTEND_HOST } else { '127.0.0.1' }
    $portArg = if ($env:FRONTEND_PORT) { $env:FRONTEND_PORT } else { '4200' }
    $cmdLine = "npm run start -- --host $hostArg --port $portArg > `"$logPath`" 2>&1"
    $p = Start-Process -FilePath "cmd.exe" -ArgumentList @('/c', $cmdLine) `
        -WorkingDirectory $Directory -WindowStyle Hidden -PassThru
    Add-Content -Path $script:PidsFile -Value $p.Id
    Info "Started Angular dev server (wrapper PID $($p.Id), log -> logs\$LogFileName)"
    return $true
}

$Root = $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($Root)) {
    $Root = Split-Path -Parent -LiteralPath $MyInvocation.MyCommand.Path
}
if ([string]::IsNullOrWhiteSpace($Root) -or -not (Test-Path -LiteralPath (Join-Path $Root 'backEnd') -PathType Container)) {
    Write-Host '[ERROR] Run this script from the repo root (start-backend.ps1 next to the backEnd folder).' -ForegroundColor Red
    Write-Host "       Resolved folder: '$Root'" -ForegroundColor Red
    exit 1
}
$Back = Join-Path $Root 'backEnd'
$Frontend = Join-Path $Root 'frontend\smart-freelance-app'
$script:LogDir = Join-Path $Root 'logs'
$script:PidsFile = Join-Path $script:LogDir 'pids.txt'

if (-not (Test-Path $script:LogDir)) {
    New-Item -ItemType Directory -Path $script:LogDir | Out-Null
}
[System.IO.File]::WriteAllText($script:PidsFile, '')

Write-Host ""
Write-Host "============================================================"
Write-Host "  Smart Freelance - Backend + Frontend Startup"
Write-Host "============================================================"
Write-Host ""

# 1. Eureka
Start-MavenService -Directory (Join-Path $Back 'Eureka') -Label 'Eureka' -LogFileName 'eureka.log'
Wait-ForPort -Port 8420 -Label 'Eureka' -TimeoutSec 120

# 2. Config Server
Start-MavenService -Directory (Join-Path $Back 'ConfigServer') -Label 'Config Server' -LogFileName 'config-server.log'
Wait-ForPort -Port 8888 -Label 'Config Server' -TimeoutSec 90

# 3. Keycloak Auth
Start-MavenService -Directory (Join-Path $Back 'KeyCloak') -Label 'Keycloak Auth' -LogFileName 'keycloak-auth.log'
Wait-ForPort -Port 8079 -Label 'Keycloak Auth' -TimeoutSec 90

# 4. API Gateway
Start-MavenService -Directory (Join-Path $Back 'apiGateway') -Label 'API Gateway' -LogFileName 'api-gateway.log'
Wait-ForPort -Port 8078 -Label 'API Gateway' -TimeoutSec 90

Info '--- Wave A: Eureka / lb backends (Offer, AImodel) ---'
Start-MavenService -Directory (Join-Path $Back 'Microservices\Offer') -Label 'Offer' -LogFileName 'Offer.log'
Start-MavenService -Directory (Join-Path $Back 'Microservices\AImodel') -Label 'AImodel' -LogFileName 'AImodel.log'

Wait-ForPort -Port 8082 -Label 'Offer' -TimeoutSec 120
Wait-ForPort -Port 8095 -Label 'AImodel' -TimeoutSec 120

Info '--- Wave B: Config clients (planning, Vendor, Subcontracting) ---'
Start-MavenService -Directory (Join-Path $Back 'Microservices\planning') -Label 'planning' -LogFileName 'planning.log'
Start-MavenService -Directory (Join-Path $Back 'Microservices\Vendor') -Label 'Vendor' -LogFileName 'Vendor.log'
Start-MavenService -Directory (Join-Path $Back 'Microservices\Subcontracting') -Label 'Subcontracting' -LogFileName 'Subcontracting.log'

Wait-ForPort -Port 8081 -Label 'planning' -TimeoutSec 120
Wait-ForPort -Port 8093 -Label 'Vendor' -TimeoutSec 120
Wait-ForPort -Port 8110 -Label 'Subcontracting' -TimeoutSec 120

Info '--- Wave C: task (after AI + config peers) ---'
Start-MavenService -Directory (Join-Path $Back 'Microservices\task') -Label 'task' -LogFileName 'task.log'
Wait-ForPort -Port 8091 -Label 'task' -TimeoutSec 120

Info '--- Wave D: remaining microservices ---'
$remaining = @(
    @{ Name = 'user'; Port = 8090 },
    @{ Name = 'Contract'; Port = 8083 },
    @{ Name = 'Project'; Port = 8084 },
    @{ Name = 'review'; Port = 8085 },
    @{ Name = 'Portfolio'; Port = 8086 },
    @{ Name = 'gamification'; Port = 8088 },
    @{ Name = 'FreelanciaJob'; Port = 8097 },
    @{ Name = 'ticket-service'; Port = 8094 },
    @{ Name = 'Chat'; Port = 8096 },
    @{ Name = 'Meeting'; Port = 8101 },
    @{ Name = 'Notification'; Port = 8098 }
)

foreach ($r in $remaining) {
    $dir = Join-Path $Back ('Microservices\' + $r.Name)
    if (Test-Path -LiteralPath $dir -PathType Container) {
        Start-MavenService -Directory $dir -Label $r.Name -LogFileName ($r.Name + '.log')
    } else {
        Warn "Directory not found, skipping: $dir"
    }
}

Write-Host ""
Info 'Waiting for Wave D services...'
Write-Host ""

foreach ($r in $remaining) {
    $dir = Join-Path $Back ('Microservices\' + $r.Name)
    if (Test-Path -LiteralPath $dir -PathType Container) {
        Wait-ForPort -Port $r.Port -Label $r.Name -TimeoutSec 120
    }
}

$fePort = if ($env:FRONTEND_PORT) { [int]$env:FRONTEND_PORT } else { 4200 }
$feHost = if ($env:FRONTEND_HOST) { $env:FRONTEND_HOST } else { '127.0.0.1' }

Write-Host ""
if (-not (Test-Path -LiteralPath $Frontend -PathType Container)) {
    Warn "Frontend directory not found, skipping: $Frontend"
} elseif ($env:SKIP_FRONTEND -eq '1') {
    Info 'SKIP_FRONTEND=1 - not starting Angular dev server'
} else {
    Info '--- Frontend: Angular (ng serve) ---'
    if (Start-AngularFrontend -Directory $Frontend -LogFileName 'frontend-angular.log') {
        Info 'Waiting for dev server (first build may take several minutes)...'
        Wait-ForPort -Port $fePort -Label 'Angular dev server' -TimeoutSec 300
    }
}

Write-Host ""
Write-Host "============================================================"
Write-Host "  All services started!"
Write-Host ""
Write-Host "  Eureka dashboard : http://localhost:8420"
Write-Host "  API Gateway      : http://localhost:8078"
Write-Host "  Keycloak Auth    : http://localhost:8079"
Write-Host "  Config Server    : http://localhost:8888"
if ($env:SKIP_FRONTEND -ne '1' -and (Test-Path -LiteralPath $Frontend -PathType Container)) {
    Write-Host "  Angular app      : http://${feHost}:$fePort"
}
Write-Host ""
Write-Host "  Logs in: $script:LogDir"
Write-Host "  PIDs in: $script:PidsFile"
Write-Host ""
Write-Host "  To stop everything:"
Write-Host "    stop-backend.bat  (or  .\stop-backend.ps1)"
Write-Host "============================================================"
