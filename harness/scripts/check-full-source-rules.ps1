param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$Arguments
)

# Delegate to the complete bundle, including HTTP/security negative contracts.
$ErrorActionPreference = 'Continue'
$runtimePython = Join-Path $PSScriptRoot '../../.harness-state/runtime/python314/python.exe'
if ($env:HARNESS_PYTHON) {
    $pythonPath = $env:HARNESS_PYTHON
} elseif (Test-Path -LiteralPath $runtimePython) {
    $pythonPath = (Resolve-Path -LiteralPath $runtimePython).Path
} else {
    $pythonCommand = Get-Command python3, python -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($null -eq $pythonCommand) {
        Write-Error 'A supported Python runtime is required. Run Windows harness setup first.'
        exit 2
    }
    $pythonPath = $pythonCommand.Source
}
$bashCommand = Get-Command bash -ErrorAction SilentlyContinue
$bashPath = if ($bashCommand) { $bashCommand.Source } else { Join-Path $env:ProgramFiles 'Git/bin/bash.exe' }
if (-not (Test-Path -LiteralPath $bashPath)) {
    Write-Error 'Git Bash is required for the canonical full-source bundle.'
    exit 2
}
$env:HARNESS_PYTHON = $pythonPath.Replace('\', '/')
$env:PYTHONIOENCODING = 'utf-8'
$env:PYTHONUTF8 = '1'
$env:Path = (Split-Path -Parent $pythonPath) + [IO.Path]::PathSeparator + (Split-Path -Parent $bashPath) + [IO.Path]::PathSeparator + $env:Path
$bundle = (Join-Path $PSScriptRoot 'check-full-source-rules.sh').Replace('\', '/')
& $bashPath $bundle @Arguments
exit $LASTEXITCODE
