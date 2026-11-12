param([string]$Root = (Join-Path $PSScriptRoot '../..'))
$ErrorActionPreference = 'Stop'
$rootPath = [IO.Path]::GetFullPath($Root).TrimEnd('\','/')
$issues = New-Object 'System.Collections.Generic.List[string]'
function Add-Issue([string]$message) { $issues.Add($message) }
function Get-Anchors([string]$body) {
    $seen = @{}
    foreach ($heading in [regex]::Matches($body, '(?m)^#{1,6}\s+(.+?)\s*$')) {
        $slug = $heading.Groups[1].Value.ToLowerInvariant() -replace '[^\p{L}\p{Nd}\s_-]', ''
        $slug = $slug -replace '\s', '-'
        $base = $slug
        if ($seen.ContainsKey($base)) { $seen[$base]++; $slug = "$base-$($seen[$base])" }
        else { $seen[$base] = 0 }
        $slug
    }
}
$required = @('AGENTS.md','README.md','CLAUDE.md','GEMINI.md',
    '.docs/ARCHITECTURE.md','.docs/CODE_STANDARDS.md','.docs/GIT_STANDARDS.md',
    '.docs/PROJECT_CONTEXT.md','.docs/TECH_STACK.md','.docs/VALIDATION.md',
    '.docs/PERSONAS.md','.docs/SKILLS.md','.docs/TASK_PLANNING.md','.docs/HISTORY.md',
    '.docs/ArchitectureDecisions/architecture_decisions.md',
    '.docs/HistoryRecords/history_records.md','.docs/Personas/laziest_senior_developer.md',
    '.agents/gates/project-contract.md','.agents/gates/ci-checks.md',
    '.agents/gates/review-checklist.md','.agents/gates/release-checklist.md')
foreach ($relative in $required) {
    if (!(Test-Path -LiteralPath (Join-Path $rootPath $relative) -PathType Leaf)) { Add-Issue "Missing: $relative" }
}
$files = @($required[0..3] | ForEach-Object { Get-Item (Join-Path $rootPath $_) -ErrorAction SilentlyContinue })
foreach ($folder in @('.docs','.agents/workflows','.agents/agents','.agents/gates')) {
    $path = Join-Path $rootPath $folder
    if (Test-Path -LiteralPath $path) { $files += @(Get-ChildItem -LiteralPath $path -Recurse -Filter '*.md' -File) }
    else { Add-Issue "Missing folder: $folder" }
}
foreach ($file in $files) {
    $body = [IO.File]::ReadAllText($file.FullName)
    $relative = $file.FullName.Substring($rootPath.Length + 1).Replace('\','/')
    $limit = 120
    if ($relative -eq 'AGENTS.md') { $limit = 60 }
    if ($relative -notmatch '^\.docs/HistoryRecords/' -and @($body.TrimEnd() -split '\r?\n').Count -gt $limit) {
        Add-Issue "Line limit: $relative"
    }
    if ($body -match '<(APP_NAME|TIMEZONE|ZONE|PREFIX|BASE_BRANCH|HOST|Kotlin \| Java|module|library|version|n)>') {
        Add-Issue "Placeholder: $relative"
    }
    $activeBody = [regex]::Replace($body, '(?ms)^(\x60{3,}|~{3,})[^\r\n]*\r?\n.*?^\1[ \t]*$', '')
    foreach ($link in [regex]::Matches($activeBody, '\]\(([^)]+)\)')) {
        $target = $link.Groups[1].Value.Trim('<','>')
        if ($target -match '^(https?://|mailto:)') { continue }
        $parts = $target.Split('#', 2)
        $destination = $file.FullName
        if ($parts[0]) { $destination = [IO.Path]::GetFullPath((Join-Path $file.DirectoryName ([Uri]::UnescapeDataString($parts[0])))) }
        if (!(Test-Path -LiteralPath $destination -PathType Leaf)) { Add-Issue "Broken link: $relative -> $target"; continue }
        if ($parts.Count -eq 2 -and $parts[1]) {
            $anchors = @(Get-Anchors ([IO.File]::ReadAllText($destination)))
            if ($anchors -notcontains ([Uri]::UnescapeDataString($parts[1]))) { Add-Issue "Broken anchor: $relative -> $target" }
        }
    }
}
foreach ($pair in @(@('CLAUDE.md','@AGENTS.md'),@('GEMINI.md','Read AGENTS.md first and follow it.'))) {
    $path = Join-Path $rootPath $pair[0]
    if ((Test-Path -LiteralPath $path) -and [IO.File]::ReadAllText($path).Trim() -cne $pair[1]) { Add-Issue "Import content: $($pair[0])" }
}
foreach ($relative in @('AGENTS.md','.docs/ARCHITECTURE.md','.docs/CODE_STANDARDS.md','.docs/GIT_STANDARDS.md','.docs/PERSONAS.md','.docs/HISTORY.md','.docs/Personas/laziest_senior_developer.md')) {
    $path = Join-Path $rootPath $relative
    if ((Test-Path -LiteralPath $path) -and [IO.File]::ReadAllText($path) -notmatch '\*\*Sealed') { Add-Issue "Missing sealed banner: $relative" }
}
if (Test-Path -LiteralPath (Join-Path $rootPath 'setup_prompt.md')) { Add-Issue 'Stale setup input: setup_prompt.md' }
if ($issues.Count) { $issues | ForEach-Object { Write-Output "FAIL: $_" }; exit 1 }
Write-Output "PASS: $($files.Count) harness documents; links, anchors, limits, imports, banners, placeholders and cleanup."
Write-Output 'Scope: instruction integrity only; no application/source/runtime gate was executed.'
exit 0
