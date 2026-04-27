$ErrorActionPreference = "Continue"
$root = Split-Path -Parent $PSScriptRoot
$lines = @()
$lines += "JaCoCo overall LINE coverage baseline (from target/site/jacoco/jacoco.xml root counters)."
$lines += "Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm')"
$lines += "Gate target: overall line coverage >= 40% on SonarQube (configure Quality Gate on server)."
$lines += ""
$dirs = Get-ChildItem $root -Directory | Where-Object {
        Test-Path (Join-Path $_.FullName "mvnw.cmd") -and (Test-Path (Join-Path $_.FullName "pom.xml"))
    } | Sort-Object Name
foreach ($d in $dirs) {
    Push-Location $d.FullName
    Write-Host ">>> $($d.Name) ..."
    & .\mvnw.cmd -q clean verify
    $exit = $LASTEXITCODE
    $jacoco = Join-Path $d.FullName "target\site\jacoco\jacoco.xml"
    $cov = "n/a"
    $miss = ""
    $pct = "n/a"
    $ge40 = "?"
    if (Test-Path $jacoco) {
        $content = Get-Content -Path $jacoco -Raw -ErrorAction SilentlyContinue
        $matches = [regex]::Matches($content, '<counter type="LINE" missed="(\d+)" covered="(\d+)"/>')
        if ($matches.Count -gt 0) {
            $last = $matches[$matches.Count - 1]
            $miss = [int]$last.Groups[1].Value
            $covered = [int]$last.Groups[2].Value
            $total = $miss + $covered
            if ($total -gt 0) {
                $p = 100.0 * $covered / $total
                $pct = "{0:N1}%" -f $p
                $ge40 = if ($p -ge 39.995) { "yes" } else { "no" }
            }
            $cov = "$covered covered / $miss missed"
        }
    }
    $lines += "{0,-22} exit={1}  LINE={2}  pct={3}  >=40%={4}" -f $d.Name, $exit, $cov, $pct, $ge40
    Pop-Location
}
$out = Join-Path $root "JACOCO_COVERAGE_BASELINE.txt"
$lines | Set-Content -Path $out -Encoding UTF8
Write-Host "Wrote $out"
