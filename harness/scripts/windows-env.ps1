# Dot-source this file from PowerShell before running harness/Gradle commands.
$HarnessProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$HarnessPythonPath = Join-Path $HarnessProjectRoot '.harness-state/runtime/python314/python.exe'
if (-not (Test-Path -LiteralPath $HarnessPythonPath)) {
    throw 'Harness Python runtime is missing. Run onboarding dependency setup; Python 3.10 or newer is required.'
}
$HarnessGitBashPath = Join-Path $env:ProgramFiles 'Git/bin'
$HarnessJdkPath = Join-Path $env:ProgramFiles 'Android/Android Studio/jbr'
$HarnessSdkPath = Join-Path $env:LOCALAPPDATA 'Android/Sdk'
foreach ($HarnessRequiredPath in @($HarnessGitBashPath, $HarnessJdkPath, $HarnessSdkPath)) {
    if (-not (Test-Path -LiteralPath $HarnessRequiredPath)) {
        throw "Required local tool directory is missing: $HarnessRequiredPath"
    }
}
$env:HARNESS_PYTHON = $HarnessPythonPath.Replace('\', '/')
$env:PYTHONIOENCODING = 'utf-8'
$env:PYTHONUTF8 = '1'
$env:JAVA_HOME = $HarnessJdkPath
$env:ANDROID_HOME = $HarnessSdkPath
$env:ANDROID_SDK_ROOT = $HarnessSdkPath
$env:Path = @(
    (Split-Path -Parent $HarnessPythonPath),
    $HarnessGitBashPath,
    (Join-Path $HarnessJdkPath 'bin'),
    (Join-Path $HarnessSdkPath 'platform-tools'),
    (Join-Path $HarnessSdkPath 'emulator'),
    $env:Path
) -join [System.IO.Path]::PathSeparator
