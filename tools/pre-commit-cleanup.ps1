param([string]$Root = (Join-Path $PSScriptRoot '..'))
$ErrorActionPreference = 'Stop'
$rootPath = [IO.Path]::GetFullPath($Root).TrimEnd('\', '/')

$transientDirs = @('.scratch', '.harness-state', '.qa-artifacts', '.codex-attachments')
foreach ($dir in $transientDirs) {
    $target = Join-Path $rootPath $dir
    if (Test-Path -LiteralPath $target) {
        Remove-Item -LiteralPath $target -Recurse -Force
    }
}

$transientFiles = @('firestore-debug.log')
foreach ($file in $transientFiles) {
    $target = Join-Path $rootPath $file
    if (Test-Path -LiteralPath $target) {
        Remove-Item -LiteralPath $target -Force
    }
}

$scanRoots = @('.agents', '.docs', '.githooks', 'docs', 'app/src', 'catalogs', 'tools') |
    ForEach-Object { Join-Path $rootPath $_ } |
    Where-Object { Test-Path -LiteralPath $_ }
foreach ($scanRoot in $scanRoots) {
    Get-ChildItem -LiteralPath $scanRoot -Force -Recurse -File -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -in @('.DS_Store', 'Thumbs.db') -or $_.Extension -in @('.orig', '.rej') } |
        Remove-Item -Force -ErrorAction SilentlyContinue
}

$assetsRoot = Join-Path $rootPath 'docs/changes/assets'
if (Test-Path -LiteralPath $assetsRoot) {
    Get-ChildItem -LiteralPath $assetsRoot -Recurse -Directory -Filter 'previews-*' -ErrorAction SilentlyContinue |
        Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
}

git -C $rootPath worktree prune 2>$null
$worktreesDir = Join-Path $rootPath '.worktrees'
if ((Test-Path -LiteralPath $worktreesDir) -and @(Get-ChildItem -LiteralPath $worktreesDir -Force).Count -eq 0) {
    Remove-Item -LiteralPath $worktreesDir -Force
}

Write-Output 'Pre-commit cleanup complete.'
