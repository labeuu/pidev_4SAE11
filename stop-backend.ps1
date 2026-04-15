# stop-backend.ps1 - Stop processes started by start-backend.ps1 (Windows)
# Kills PIDs in reverse order with /T so child processes (java, node) are included.

$ErrorActionPreference = 'Continue'

$Root = $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($Root)) {
    $Root = Split-Path -Parent -LiteralPath $MyInvocation.MyCommand.Path
}
$PidsFile = Join-Path $Root 'logs\pids.txt'

if (-not (Test-Path -LiteralPath $PidsFile)) {
    Write-Host "[ERROR] No PID file found at $PidsFile" -ForegroundColor Red
    exit 1
}

$lines = Get-Content -LiteralPath $PidsFile -ErrorAction SilentlyContinue | ForEach-Object { $_.Trim() } | Where-Object { $_ -ne '' }
$pids = @($lines | ForEach-Object {
    $n = 0
    if ([int]::TryParse($_, [ref]$n)) { $n } else { $null }
} | Where-Object { $_ -ne $null })

if ($pids.Count -eq 0) {
    Write-Host "[WARN] No PIDs in $PidsFile" -ForegroundColor Yellow
    Remove-Item -LiteralPath $PidsFile -Force -ErrorAction SilentlyContinue
    exit 0
}

Write-Host 'Stopping services (reverse startup order, including child processes)...'

for ($i = $pids.Count - 1; $i -ge 0; $i--) {
    $procId = $pids[$i]
    $proc = Get-Process -Id $procId -ErrorAction SilentlyContinue
    if ($proc) {
        & taskkill.exe /PID $procId /T /F 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) {
            Write-Host "[TERM] PID $procId (tree)" -ForegroundColor Green
        } else {
            Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
            Write-Host "[TERM] PID $procId (fallback)" -ForegroundColor Green
        }
    }
}

Remove-Item -LiteralPath $PidsFile -Force -ErrorAction SilentlyContinue
Write-Host 'Done.'
