# Windows launch guide

This workspace contains a Windows launcher helper for the OneLauncher project.

## Prerequisites

1. Install Rust from https://rustup.rs/
2. Install Visual Studio Build Tools 2022 with the C++ desktop workload
3. Reopen PowerShell after installation

The app is configured for the Windows MSVC toolchain, which is the correct toolchain for native Windows app builds.

Important: the Visual Studio C++ Build Tools must be installed before the project can compile on Windows.

## Install the required build tools

```powershell
winget install --id Microsoft.VisualStudio.2022.BuildTools --override "--wait --passive --add Microsoft.VisualStudio.Workload.NativeDesktop --includeRecommended" --accept-package-agreements --accept-source-agreements --disable-interactivity
```

After installation, close and reopen PowerShell.

## Run the app

From the repository root:

```powershell
powershell -ExecutionPolicy Bypass -File .\launch-windows.ps1
```

To build without launching:

```powershell
powershell -ExecutionPolicy Bypass -File .\launch-windows.ps1 -BuildOnly
```

To build a release binary:

```powershell
powershell -ExecutionPolicy Bypass -File .\launch-windows.ps1 -Release
```

## Notes

- The script ensures the correct Windows Rust toolchain is selected.
- It checks for `cl.exe` and `link.exe` before continuing.
- It runs the project from the OneLauncher subfolder.

If the build still fails, verify that the MSVC C++ tools are installed and that the terminal has been reopened after installation.
