<#
.SYNOPSIS
    Zone: backend-1, the maven/update-check API (port 8082).

.DESCRIPTION
    Polyfrost's v1 backend: it fronts a maven repository for update checks and
    artifact downloads. It has no database and is independent of plus-backend.

    Its own default is 0.0.0.0:8080, which plus-backend already owns, so it runs
    on 8082 here. By default it advertises the public Polyfrost maven; point
    -PublicMavenUrl elsewhere to serve your own.
#>
param(
    [switch]$Stop,
    [string]$PublicMavenUrl = 'https://repo.polyfrost.org/releases'
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\_common.ps1"

if ($Stop) {
    Write-Step 'backend-1'
    if (Stop-Recorded 'backend1') { Write-Ok 'stopped' } else { Write-Note 'was not running' }
    return
}

Write-Step 'backend-1 (maven / update-check API)'
Assert-Tools @('cargo')

if (Test-PortListening $Ports.backend1) {
    Write-Ok "already listening on $($Ports.backend1)"
    return
}

$logDir = Join-Path $LocalDir 'logs'
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
$log = Join-Path $logDir 'backend1.log'

$process = Start-Process -FilePath 'cargo' -PassThru -WindowStyle Hidden `
    -WorkingDirectory $Backend1Dir -ArgumentList @(
        'run', '--',
        '--bind', "127.0.0.1:$($Ports.backend1)",
        '--public-maven-url', $PublicMavenUrl
    ) `
    -RedirectStandardOutput $log -RedirectStandardError "$log.err"
Save-ServicePid 'backend1' $process.Id

Write-Note "building and starting (pid $($process.Id)); this crate has never been built here, so expect a long first compile"
if (Wait-ForPort $Ports.backend1 900) {
    Write-Ok "up on http://127.0.0.1:$($Ports.backend1), serving $PublicMavenUrl"
} else {
    Write-Note "did not come up in time; see $log and $log.err"
}
