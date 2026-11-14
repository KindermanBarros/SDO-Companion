param([string]$Root = (Join-Path $PSScriptRoot '../../..'))
$ErrorActionPreference = 'Stop'
$source = [IO.Path]::GetFullPath($Root).TrimEnd('\','/')
$scratch = Join-Path $source '.scratch'
$fixture = [IO.Path]::GetFullPath((Join-Path $scratch ('hook-' + [guid]::NewGuid().ToString('N'))))
if (!$fixture.StartsWith($scratch + '\', [StringComparison]::OrdinalIgnoreCase)) { throw 'Fixture escaped scratch' }
$gitDirectory = Split-Path (Get-Command git).Source
$shell = [IO.Path]::GetFullPath((Join-Path $gitDirectory '../bin/sh.exe'))
if (!(Test-Path -LiteralPath $shell)) { throw 'Git shell unavailable' }
$hook = Join-Path $source '.githooks/pre-commit'
$utf8 = New-Object Text.UTF8Encoding($false)
function Assert-Hook([int]$expected, [string]$name) {
    $savedPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    & $shell $hook 2>$null
    $ErrorActionPreference = $savedPreference
    if ($LASTEXITCODE -ne $expected) { throw "$name returned $LASTEXITCODE, expected $expected" }
    Write-Output "PASS: $name"
}
try {
    New-Item -ItemType Directory -Path $fixture -Force | Out-Null
    Push-Location $fixture
    git init --quiet
    Assert-Hook 0 'scratch absent'
    New-Item -ItemType Directory -Path '.scratch' | Out-Null
    Assert-Hook 1 'empty scratch rejected'
    [IO.File]::WriteAllText((Join-Path $fixture '.scratch/probe.tmp'), 'temporary', $utf8)
    Assert-Hook 1 'nonempty scratch rejected'
    git add -f -- .scratch/probe.tmp
    Remove-Item -LiteralPath (Join-Path $fixture '.scratch') -Recurse -Force
    Assert-Hook 1 'staged scratch rejected after local removal'
}
finally {
    if ((Get-Location).Path -eq $fixture) { Pop-Location }
    if ($fixture.StartsWith($scratch + '\', [StringComparison]::OrdinalIgnoreCase) -and (Test-Path -LiteralPath $fixture)) {
        Remove-Item -LiteralPath $fixture -Recurse -Force
    }
    if ((Test-Path -LiteralPath $scratch) -and !(Get-ChildItem -LiteralPath $scratch -Force | Select-Object -First 1)) {
        Remove-Item -LiteralPath $scratch
    }
}
