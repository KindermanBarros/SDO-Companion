param([string]$Bash = 'C:/Program Files/Git/bin/bash.exe')
$ErrorActionPreference = 'Stop'
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../..'))
$runner = (Join-Path $root '.github/scripts/run-adaptive-ui-tests.sh').Replace('\','/')
$scratchRoot = Join-Path $root '.scratch'
$fixture = [IO.Path]::GetFullPath((Join-Path $scratchRoot ('sdo-adaptive-runner-' + [guid]::NewGuid().ToString('N'))))
$tempRoot = [IO.Path]::GetFullPath($scratchRoot).TrimEnd('\') + '\'
if (!$fixture.StartsWith($tempRoot, [StringComparison]::OrdinalIgnoreCase)) { throw 'Fixture escaped temp root' }
$encoding = New-Object Text.UTF8Encoding($false)
try {
    [IO.Directory]::CreateDirectory((Join-Path $fixture 'bin')) | Out-Null
    $adb = @'
#!/usr/bin/env bash
set -eu
printf '%s\n' "$*" >> calls.log
if [[ "$*" == 'shell am instrument '* ]]; then
  case "$RUNNER_CASE" in
    pass) printf 'OK (2 tests)\n' ;;
    fail) printf 'FAILURES!!!\nTests run: 2, Failures: 1\n' ;;
    empty) printf 'OK (0 tests)\n' ;;
    transport) exit 42 ;;
  esac
fi
'@
    [IO.File]::WriteAllText((Join-Path $fixture 'bin/adb'), $adb.Replace("`r`n","`n") + "`n", $encoding)
    [IO.File]::WriteAllText((Join-Path $fixture 'gradlew'), "#!/usr/bin/env bash`nprintf '%s\n' `"gradle `$*`" >> calls.log`n", $encoding)
    foreach ($case in @('pass','fail','empty','transport')) {
        Remove-Item -LiteralPath (Join-Path $fixture 'calls.log') -ErrorAction SilentlyContinue
        $output = @(& $Bash -c 'workdir=$(cygpath -u "$1"); cd "$workdir"; export PATH="$workdir/bin:$PATH"; export RUNNER_CASE="$3"; chmod +x bin/adb gradlew; bash "$2"' 'fixture' $fixture.Replace('\','/') $runner $case 2>&1)
        $exitCode = $LASTEXITCODE
        if (($case -eq 'pass' -and $exitCode -ne 0) -or ($case -ne 'pass' -and $exitCode -eq 0)) {
            throw "$case returned unexpected exit $exitCode : $($output -join ' ')"
        }
        $calls = [IO.File]::ReadAllText((Join-Path $fixture 'calls.log'))
        if ($calls -notmatch 'gradle :app:assembleDebug :app:assembleDebugAndroidTest' -or
            $calls -notmatch 'AdaptiveUiInstrumentedTest,com.kinderman.sdo.ui.CharacterSheetAccessInstrumentedTest' -or
            $calls -notmatch 'pull /sdcard/Android/data/com.kinderman.sdo/files/adaptive-ui') {
            throw "$case did not build/select the required tests or collect screenshots"
        }
        Write-Output "PASS: runner $case"
    }
    Write-Output 'PASS: 4 shell-control cases; no Android runtime behavior is claimed.'
}
finally {
    if ($fixture.StartsWith($tempRoot, [StringComparison]::OrdinalIgnoreCase) -and (Test-Path -LiteralPath $fixture)) {
        Remove-Item -LiteralPath $fixture -Recurse -Force
    }
    if ((Test-Path -LiteralPath $scratchRoot) -and !(Get-ChildItem -LiteralPath $scratchRoot -Force | Select-Object -First 1)) {
        Remove-Item -LiteralPath $scratchRoot
    }
}
