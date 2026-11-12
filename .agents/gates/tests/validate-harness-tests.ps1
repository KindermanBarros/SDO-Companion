param([string]$Root = (Join-Path $PSScriptRoot '../../..'))
$ErrorActionPreference = 'Stop'
$source = [IO.Path]::GetFullPath($Root)
$fixture = Join-Path ([IO.Path]::GetTempPath()) ('sdo-harness-contract-' + [guid]::NewGuid().ToString('N'))
$fixture = [IO.Path]::GetFullPath($fixture)
$tempRoot = [IO.Path]::GetFullPath([IO.Path]::GetTempPath()).TrimEnd('\') + '\'
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
    foreach ($name in @('AGENTS.md','README.md','CLAUDE.md','GEMINI.md','.docs','.agents','docs')) {
        Copy-Item -LiteralPath (Join-Path $source $name) -Destination $fixture -Recurse
    }
    Run-Gate 0 'valid current harness'
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
    Write-Output 'PASS: 9 contract cases (1 positive, 8 negative).'
}
finally {
    if ($fixture.StartsWith($tempRoot, [StringComparison]::OrdinalIgnoreCase) -and (Test-Path -LiteralPath $fixture)) {
        Remove-Item -LiteralPath $fixture -Recurse -Force
    }
}
