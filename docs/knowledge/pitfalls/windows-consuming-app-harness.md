# Windows consuming-app harness pitfalls

Recorded 2026-11-12 from the SDO onboarding run.

- `python3` may be a Microsoft Store alias even when another Python is installed. Select a supported explicit runtime; Python 3.8 cannot execute pathlib.is_relative_to-based validators.
- Embeddable Python requires an explicit script import path. Keep its signed distribution in ignored local runtime state; do not commit binaries or modify global PATH. Run `powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File harness/scripts/setup-windows.ps1 -InstallPythonRuntime` after dependency authorization. Then use a process-scoped command such as `powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -Command ". ./harness/scripts/windows-env.ps1; ./gradlew.bat :app:assembleDebug"`.
- Always decode repository documents as UTF-8 explicitly. Output encoding environment flags cannot repair an implicit file-decoding assumption in every isolated runtime configuration.
- `gradlew --offline` does not prevent the wrapper from downloading its distribution before Gradle starts. Check wrapper caches before offline-only diagnostics.
- Consumer profiles and test fixture profiles have different ownership. Pass the fixture project root explicitly or source checks may use unrelated consumer layer roots.
- Native source-rule entry points must delegate to the complete canonical bundle rather than maintain partial copies.
- Instrumentation IDs and runner classes must come from validated consumer configuration. A sample package or a fake runner fixture cannot supply SDO runtime evidence.
- Preserve a failing gate as a failure. A working harness can reveal hundreds of pre-existing application findings; configuration alone does not complete onboarding.
