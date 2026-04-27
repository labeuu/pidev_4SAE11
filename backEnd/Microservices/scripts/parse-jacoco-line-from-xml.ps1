param([Parameter(Mandatory=$true)][string]$JacocoXmlPath)
$content = Get-Content -Path $JacocoXmlPath -Raw -ErrorAction Stop
$matches = [regex]::Matches($content, '<counter type="LINE" missed="(\d+)" covered="(\d+)"/>')
if ($matches.Count -eq 0) { return $null }
$last = $matches[$matches.Count - 1]
[pscustomobject]@{
    Missed = [int]$last.Groups[1].Value
    Covered = [int]$last.Groups[2].Value
}
