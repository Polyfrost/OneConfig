<#
.SYNOPSIS
    Stops every local zone this folder started.

.DESCRIPTION
    Only processes recorded in .local/pids are stopped, so anything you started
    by hand is left alone. Postgres is a shared container and is left running;
    stop it yourself with `docker stop postgres` if you want it down.
#>

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\_common.ps1"

& "$PSScriptRoot\launcher.ps1" -Stop
& "$PSScriptRoot\store.ps1" -Stop
& "$PSScriptRoot\dashboard.ps1" -Stop
& "$PSScriptRoot\backend1.ps1" -Stop
& "$PSScriptRoot\backend.ps1" -Stop
& "$PSScriptRoot\render.ps1" -Stop
& "$PSScriptRoot\s3.ps1" -Stop
& "$PSScriptRoot\postgres.ps1" -Stop
