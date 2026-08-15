<#
.SYNOPSIS
    Zone: cosmetic cover render service (port 8090).

.DESCRIPTION
    A headless skinview3d sidecar. The backend POSTs an uploaded cosmetic here
    and stores the PNG it returns as that cosmetic's cover. Everything else works
    without it: the backend logs a warning and records no cover.

    skinview3d needs WebGL, so this drives a real browser through Puppeteer.
    Puppeteer's own Chromium download is skipped in favour of the Chrome or Edge
    already installed, which saves fetching a second copy.
#>
param([switch]$Stop)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\_common.ps1"

if ($Stop) {
    Write-Step 'Render service'
    if (Stop-Recorded 'render') { Write-Ok 'stopped' } else { Write-Note 'was not running' }
    return
}

Write-Step 'Render service'
Assert-Tools @('node', 'npm')

if (Test-PortListening $Ports.render) {
    Write-Ok "already listening on $($Ports.render)"
    return
}

$chromium = Find-Chromium
if (-not $chromium) {
    Write-Note 'no Chrome or Edge found; skipping (cosmetic covers will not be generated)'
    return
}

if (-not (Test-Path (Join-Path $RenderDir 'node_modules'))) {
    Write-Note 'installing dependencies'
    Push-Location $RenderDir
    try {
        $env:PUPPETEER_SKIP_DOWNLOAD = 'true'
        Invoke-Native { npm install --no-audit --no-fund } | Out-Null
    } finally {
        Pop-Location
    }
}

if (-not (Test-Path (Join-Path $RenderDir 'assets\default-skin.png'))) {
    Push-Location $RenderDir
    try {
        Invoke-Native { node scripts/fetch-default-skin.mjs } | Out-Null
    } finally {
        Pop-Location
    }
}

# Start-Process has no portable way to pass environment across PowerShell
# versions, so set it here and let the child inherit.
$env:PORT = "$($Ports.render)"
$env:PUPPETEER_EXECUTABLE_PATH = $chromium

$logDir = Join-Path $LocalDir 'logs'
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
$log = Join-Path $logDir 'render.log'

$process = Start-Process -FilePath 'node' -PassThru -WindowStyle Hidden `
    -WorkingDirectory $RenderDir -ArgumentList @('src/server.js') `
    -RedirectStandardOutput $log -RedirectStandardError "$log.err"
Save-ServicePid 'render' $process.Id

if (Wait-ForPort $Ports.render 60) {
    Write-Ok "up on http://127.0.0.1:$($Ports.render) using $chromium (pid $($process.Id))"
} else {
    Write-Note "did not come up in time; see $log and $log.err"
}
