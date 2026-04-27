$root = Split-Path -Parent $PSScriptRoot
$pattern = '<counter type="LINE" missed="(\d+)" covered="(\d+)"/>'
$lines = @()
$lines += "JaCoCo overall LINE coverage (root report = last LINE counter in jacoco.xml)."
$lines += "SonarQube overall coverage can differ when sonar.coverage.exclusions is set (see per-service sonar-project.properties)."
$lines += "Regenerate: backEnd/Microservices/scripts/refresh-baseline-from-disk.ps1 (after mvn clean verify per service)."
$lines += "Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm')"
$lines += ""
Get-ChildItem $root -Directory | Where-Object { Test-Path (Join-Path $_.FullName "pom.xml") } | Sort-Object Name | ForEach-Object {
    $name = $_.Name
    $jacoco = Join-Path $_.FullName "target\site\jacoco\jacoco.xml"
    if (-not (Test-Path $jacoco)) {
        $lines += "{0,-22} jacoco report missing (run mvn clean verify)" -f $name
        return
    }
    $content = Get-Content -Path $jacoco -Raw
    $matches = [regex]::Matches($content, $pattern)
    if ($matches.Count -eq 0) {
        $lines += "{0,-22} no LINE counters in jacoco.xml" -f $name
        return
    }
    $last = $matches[$matches.Count - 1]
    $miss = [int]$last.Groups[1].Value
    $covered = [int]$last.Groups[2].Value
    $total = $miss + $covered
    $pct = if ($total -gt 0) { "{0:N1}%" -f (100.0 * $covered / $total) } else { "n/a" }
    $ok = if ($total -gt 0 -and (100.0 * $covered / $total) -ge 39.995) { "yes" } else { "no" }
    $lines += "{0,-22} LINE {1} covered / {2} missed  pct={3}  >=40%={4}" -f $name, $covered, $miss, $pct, $ok
}
$out = Join-Path $root "JACOCO_COVERAGE_BASELINE.txt"
$lines | Set-Content -Path $out -Encoding UTF8
Write-Host "Wrote $out"
Get-Content $out
