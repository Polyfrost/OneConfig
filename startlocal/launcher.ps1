<#
.SYNOPSIS
    Zone: OneLauncher desktop app.

.DESCRIPTION
    Builds and runs the launcher. It is a native app rather than a service, so
    it takes no port and start-all leaves it out; run this yourself when you want
    the launcher.

    To point it at the local stack, open Settings > Developer once it is up:
      Poly+ backend URL   http://127.0.0.1:8080
      Admin dashboard     http://localhost:3000
    Left blank it talks to https://plus.polyfrost.org, per PLUS_BACKEND_URL in
    packages/oneclient_common/src/constants.rs.
#>
param(
    [switch]$BuildOnly,
    [switch]$Release,
    [switch]$Detached,
    [switch]$Stop
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\_common.ps1"

if ($Stop) {
    Write-Step 'OneLauncher'
    if (Stop-Recorded 'launcher') { Write-Ok 'stopped' } else { Write-Note 'was not running' }
    return
}

if ($BuildOnly -and $Release) {
    throw 'Use either -BuildOnly or -Release, not both.'
}

Write-Step 'OneLauncher'

if (-not (Test-Path $LauncherDir)) {
    throw "OneLauncher not found at $LauncherDir"
}

Assert-Tools @('cargo', 'rustup')

# The app targets the MSVC toolchain, which needs the Visual Studio C++ build
# tools. Do not test for cl.exe on PATH: that only holds inside a Developer
# Prompt, while rustc finds the linker through the VS installation record. Ask
# vswhere the same way rustc's linker discovery does.
$vswhere = Join-Path ${env:ProgramFiles(x86)} 'Microsoft Visual Studio\Installer\vswhere.exe'
if (Test-Path $vswhere) {
    $installation = & $vswhere -products '*' `
        -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 `
        -property installationPath 2>$null | Select-Object -First 1
    if ($installation) {
        Write-Ok "MSVC C++ tools: $installation"
    } else {
        Write-Note 'Visual Studio is installed but without the C++ desktop workload; linking will fail.'
        Write-Note 'winget install --id Microsoft.VisualStudio.2022.BuildTools --override "--wait --passive --add Microsoft.VisualStudio.Workload.NativeDesktop --includeRecommended"'
    }
} else {
    Write-Note 'Visual Studio Installer not found; linking will fail without the C++ build tools.'
    Write-Note 'winget install --id Microsoft.VisualStudio.2022.BuildTools --override "--wait --passive --add Microsoft.VisualStudio.Workload.NativeDesktop --includeRecommended"'
}

Invoke-Native { rustup target add x86_64-pc-windows-msvc } | Out-Null

if (Test-PortListening $Ports.backend) {
    Write-Ok "local API is up; set the Poly+ backend URL to http://127.0.0.1:$($Ports.backend) in Settings > Developer"
} else {
    Write-Note 'local API is not running; the launcher will fall back to https://plus.polyfrost.org'
}

$arguments = if ($Release) {
    @('build', '-p', 'oneclient_app', '--release')
} elseif ($BuildOnly) {
    @('build', '-p', 'oneclient_app')
} else {
    @('run', '-p', 'oneclient_app')
}

Write-Ok "cargo $($arguments -join ' ')"

# Detached is how start-all runs it: the launcher is a long-lived GUI app, so
# waiting on it would hold the whole startup script open.
if ($Detached) {
    $logDir = Join-Path $LocalDir 'logs'
    New-Item -ItemType Directory -Force -Path $logDir | Out-Null
    $log = Join-Path $logDir 'launcher.log'

    $process = Start-Process -FilePath 'cargo' -PassThru -WindowStyle Hidden `
        -WorkingDirectory $LauncherDir -ArgumentList $arguments `
        -RedirectStandardOutput $log -RedirectStandardError "$log.err"
    Save-ServicePid 'launcher' $process.Id

    Write-Note "building in the background (pid $($process.Id)); the window opens when it finishes"
    Write-Note "output: $log"
    return
}

Push-Location $LauncherDir
try {
    & cargo @arguments
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
