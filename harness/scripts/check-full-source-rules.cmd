@echo off
powershell -NoProfile -ExecutionPolicy RemoteSigned -File "%~dp0check-full-source-rules.ps1" %*
