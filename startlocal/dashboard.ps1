<#
.SYNOPSIS
    Zone: Poly+ admin dashboard (port 3000, Vite).

.DESCRIPTION
    The dashboard picks its backend from a dropdown; leave it on
    "Local (127.0.0.1:8080)". The admin password field takes whatever
    ADMIN_PASSWORD is set to in plus-backend-main/.env.

    Port 3000 is not incidental: it is the origin the backend allows through
    CORS by default.
#>
param([switch]$Stop)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\_common.ps1"

if ($Stop) {
    Write-Step 'Admin dashboard'
    if (Stop-Recorded 'dashboard') { Write-Ok 'stopped' } else { Write-Note 'was not running' }
    return
}

Write-Step 'Admin dashboard'
Assert-Tools @('npm')

if (Test-PortListening $Ports.dashboard) {
    Write-Ok "already listening on $($Ports.dashboard)"
    return
}

if (-not (Test-Path (Join-Path $DashboardDir 'node_modules'))) {
    Write-Note 'installing dependencies'
    Push-Location $DashboardDir
    try { Invoke-Native { npm install --no-audit --no-fund } | Out-Null } finally { Pop-Location }
}

$logDir = Join-Path $LocalDir 'logs'
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
$log = Join-Path $logDir 'dashboard.log'

$process = Start-Process -FilePath 'npm.cmd' -PassThru -WindowStyle Hidden `
    -WorkingDirectory $DashboardDir -ArgumentList @('run', 'dev') `
    -RedirectStandardOutput $log -RedirectStandardError "$log.err"
Save-ServicePid 'dashboard' $process.Id

if (Wait-ForPort $Ports.dashboard 120) {
    Write-Ok "up on http://localhost:$($Ports.dashboard) (pid $($process.Id))"
} else {
    Write-Note "did not come up in time; see $log and $log.err"
}
