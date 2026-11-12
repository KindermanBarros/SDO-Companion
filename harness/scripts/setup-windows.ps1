param([switch]$InstallPythonRuntime)

$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$runtimeRoot = Join-Path $projectRoot '.harness-state/runtime'
$pythonRoot = Join-Path $runtimeRoot 'python314'
$pythonExecutable = Join-Path $pythonRoot 'python.exe'
$archive = Join-Path $runtimeRoot 'python314.zip'
if (-not (Test-Path -LiteralPath $pythonExecutable)) {
    if (-not $InstallPythonRuntime) {
        throw 'Python runtime is missing. Re-run with -InstallPythonRuntime after authorizing the dependency download.'
    }
    if ((Test-Path -LiteralPath $pythonRoot) -and (Get-ChildItem -LiteralPath $pythonRoot -Force | Select-Object -First 1)) {
        throw "Incomplete runtime directory contains files; preserve and inspect it before retrying: $pythonRoot"
    }
    New-Item -ItemType Directory -Force -Path $runtimeRoot | Out-Null
    Invoke-WebRequest -UseBasicParsing -Uri 'https://www.python.org/ftp/python/3.14.10/python-3.14.10-embed-amd64.zip' -OutFile $archive
    Expand-Archive -LiteralPath $archive -DestinationPath $pythonRoot
}
$signature = Get-AuthenticodeSignature -LiteralPath $pythonExecutable
if ($signature.Status -ne 'Valid' -or $signature.SignerCertificate.Subject -notmatch 'Python Software Foundation') {
    throw 'Python runtime executable must have a valid Python Software Foundation signature.'
}
$pythonPathFile = Join-Path $pythonRoot 'python314._pth'
if (-not (Test-Path -LiteralPath $pythonPathFile)) { throw 'Python isolated-runtime path configuration is missing.' }
$scriptImportPath = (Join-Path $projectRoot 'harness/scripts').Replace('\', '/')
$pythonPathText = [IO.File]::ReadAllText($pythonPathFile)
if ($scriptImportPath -notin ($pythonPathText -split '\r?\n')) {
    [IO.File]::WriteAllText($pythonPathFile, $pythonPathText.TrimEnd() + "`n$scriptImportPath`n", [Text.UTF8Encoding]::new($false))
}
$python3Executable = Join-Path $pythonRoot 'python3.exe'
if (-not (Test-Path -LiteralPath $python3Executable)) {
    Copy-Item -LiteralPath $pythonExecutable -Destination $python3Executable
} elseif ((Get-FileHash -LiteralPath $pythonExecutable).Hash -ne (Get-FileHash -LiteralPath $python3Executable).Hash) {
    throw 'Existing python3.exe differs from the verified runtime; preserve and inspect it.'
}
& $pythonExecutable --version
if ($LASTEXITCODE -ne 0) { throw 'Python runtime startup failed.' }
Write-Output 'Windows harness runtime ready. No registry or global PATH changes were made.'
