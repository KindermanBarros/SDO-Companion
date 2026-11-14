param([string]$Root = (Join-Path $PSScriptRoot '../../..'))
$ErrorActionPreference = 'Stop'
$source = [IO.Path]::GetFullPath($Root)
$scratchRoot = Join-Path $source '.scratch'
$fixture = Join-Path $scratchRoot ('sdo-harness-contract-' + [guid]::NewGuid().ToString('N'))
$fixture = [IO.Path]::GetFullPath($fixture)
$tempRoot = [IO.Path]::GetFullPath($scratchRoot).TrimEnd('\') + '\'
if (!$fixture.StartsWith($tempRoot, [StringComparison]::OrdinalIgnoreCase)) { throw 'Fixture escaped temp root' }
$utf8 = New-Object Text.UTF8Encoding($false)
function Run-Gate([int]$expected, [string]$reason, [string]$diagnostic = '') {
    $output = @(& powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File (Join-Path $source '.agents/gates/validate-harness.ps1') -Root $fixture 2>&1)
    $actual = $LASTEXITCODE
    if ($actual -ne $expected) { throw "$reason expected $expected, got $actual : $($output -join ' ')" }
    if ($diagnostic -and ($output -join ' ') -notmatch [regex]::Escape($diagnostic)) { throw "$reason failed for the wrong reason" }
    Write-Output "PASS: $reason"
}
try {
    [IO.Directory]::CreateDirectory($fixture) | Out-Null
    foreach ($name in @('AGENTS.md','README.md','CLAUDE.md','GEMINI.md','.docs','.agents','.codex','.githooks','docs','harness')) {
        Copy-Item -LiteralPath (Join-Path $source $name) -Destination $fixture -Recurse
    }
    Run-Gate 0 'valid current harness'
    [IO.Directory]::CreateDirectory((Join-Path $fixture '.scratch')) | Out-Null
    Run-Gate 1 'scratch left before handoff' 'Scratch must be removed'
    Remove-Item -LiteralPath (Join-Path $fixture '.scratch')
    $probe = Join-Path $fixture '.docs/contract_probe.md'
    $cases = @(
        @('broken file', "[missing](missing.md)", 'Broken link'),
        @('broken anchor', "[missing](VALIDATION.md#missing-anchor)", 'Broken anchor'),
        @('placeholder', '# <APP_NAME>', 'Placeholder'),
        @('line limit', ((1..121 | ForEach-Object { 'line' }) -join [Environment]::NewLine), 'Line limit')
    )
    foreach ($case in $cases) {
        [IO.File]::WriteAllText($probe, $case[1], $utf8)
        Run-Gate 1 $case[0] $case[2]
        Remove-Item -LiteralPath $probe
    }
    $retired = Join-Path $fixture '.agents/skills/retired_probe.md'
    [IO.File]::WriteAllText($retired, 'Run bash harness/scripts/check-missing.sh before proceeding.', $utf8)
    Run-Gate 1 'retired script caller in a skill' 'Retired script gate reference'
    Remove-Item -LiteralPath $retired
    $claude = Join-Path $fixture 'CLAUDE.md'
    [IO.File]::WriteAllText($claude, '@OTHER.md', $utf8)
    Run-Gate 1 'wrong import' 'Import content'
    Copy-Item -LiteralPath (Join-Path $source 'CLAUDE.md') -Destination $claude -Force
    $agents = Join-Path $fixture 'AGENTS.md'
    [IO.File]::WriteAllText($agents, ([IO.File]::ReadAllText($agents).Replace('**Sealed', '**Open')), $utf8)
    Run-Gate 1 'missing sealed banner' 'Missing sealed banner'
    Copy-Item -LiteralPath (Join-Path $source 'AGENTS.md') -Destination $agents -Force
    [IO.File]::WriteAllText((Join-Path $fixture 'setup_prompt.md'), '# obsolete', $utf8)
    Run-Gate 1 'stale setup input' 'Stale setup input'
    Remove-Item -LiteralPath (Join-Path $fixture 'setup_prompt.md')
    Remove-Item -LiteralPath (Join-Path $fixture '.docs/TECH_STACK.md')
    Run-Gate 1 'missing required baseline' 'Missing: .docs/TECH_STACK.md'
    Write-Output 'PASS: 11 contract cases (1 positive, 10 negative).'
}
finally {
    if ($fixture.StartsWith($tempRoot, [StringComparison]::OrdinalIgnoreCase) -and (Test-Path -LiteralPath $fixture)) {
        Remove-Item -LiteralPath $fixture -Recurse -Force
    }
    if ((Test-Path -LiteralPath $scratchRoot) -and !(Get-ChildItem -LiteralPath $scratchRoot -Force | Select-Object -First 1)) {
        Remove-Item -LiteralPath $scratchRoot
    }
}
