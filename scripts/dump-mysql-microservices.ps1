param(
    [string]$ComposeFile = ".\docker-compose.yml",
    [string]$OutputDir = ".\backups\mysql",
    [string]$MysqlHost = "127.0.0.1",
    [int]$MysqlPort = 3306,
    [string]$MysqlUser = "root",
    [string]$MysqlPassword = "",
    [string]$MysqldumpPath = "mysqldump"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $ComposeFile)) {
    throw "Compose file not found: $ComposeFile"
}

$composeContent = Get-Content -LiteralPath $ComposeFile -Raw

# Capture DB names from JDBC URLs such as:
# jdbc:mysql://mysql:3306/mydb
# jdbc:mysql://mysql:3306/mydb?createDatabaseIfNotExist=true
$jdbcMatches = [regex]::Matches(
    $composeContent,
    'jdbc:mysql:\/\/[^\/\s:]+(?::\d+)?\/([a-zA-Z0-9_]+)(?:\?[^"\s]*)?'
)

$databases = New-Object System.Collections.Generic.HashSet[string]
foreach ($match in $jdbcMatches) {
    [void]$databases.Add($match.Groups[1].Value)
}

# Capture Keycloak DB URL:
# KC_DB_URL: jdbc:mysql://mysql:3306/keycloakdb
$kcMatches = [regex]::Matches(
    $composeContent,
    'KC_DB_URL\s*:\s*jdbc:mysql:\/\/[^\/\s:]+(?::\d+)?\/([a-zA-Z0-9_]+)(?:\?[^"\s]*)?'
)
foreach ($match in $kcMatches) {
    [void]$databases.Add($match.Groups[1].Value)
}

if ($databases.Count -eq 0) {
    throw "No MySQL database names detected in $ComposeFile"
}

if (-not (Get-Command $MysqldumpPath -ErrorAction SilentlyContinue)) {
    throw "mysqldump not found. Pass -MysqldumpPath 'C:\xampp\mysql\bin\mysqldump.exe' or add it to PATH."
}

if (-not (Test-Path -LiteralPath $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$runDir = Join-Path $OutputDir "dump-$timestamp"
New-Item -ItemType Directory -Path $runDir -Force | Out-Null

try {
    $dbList = @($databases) | Sort-Object
    Write-Host "Detected MySQL databases in compose:"
    $dbList | ForEach-Object { Write-Host " - $_" }
    Write-Host ""

    foreach ($db in $dbList) {
        $outFile = Join-Path $runDir ("{0}.sql" -f $db)
        Write-Host "Dumping $db -> $outFile"

        $passwordArg = if ($MysqlPassword) { "--password=$MysqlPassword" } else { "--skip-password" }
        & $MysqldumpPath `
            --host=$MysqlHost --port=$MysqlPort --user=$MysqlUser $passwordArg `
            --single-transaction --quick --skip-lock-tables --routines --events --triggers `
            --databases $db | Out-File -LiteralPath $outFile -Encoding utf8

        if ($LASTEXITCODE -ne 0) {
            throw "mysqldump failed for database '$db' (exit code $LASTEXITCODE)."
        }
    }

    Write-Host ""
    Write-Host "Backup completed successfully."
    Write-Host "Output folder: $runDir"
}
finally {
    # nothing to clean up
}
