<#
.SYNOPSIS
    Starts every local zone in dependency order.

.DESCRIPTION
    Order matters: the backend runs its migrations against Postgres at startup,
    and the seed needs the schema those migrations create, so it comes last.
    The render service comes up before the backend so the backend can see it and
    enable cover generation.

    OneLauncher comes last and only with -WithLauncher: it is a desktop app, not
    a service, and its first build is long, so it would otherwise hold up the
    services behind it. It starts detached, so this script still returns.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File .\startlocal\start-all.ps1
.EXAMPLE
    powershell -ExecutionPolicy Bypass -File .\startlocal\start-all.ps1 -WithLauncher
.EXAMPLE
    powershell -ExecutionPolicy Bypass -File .\startlocal\start-all.ps1 -WithBackend1
#>
param(
    [switch]$SkipRender,
    [switch]$SkipDashboard,
    [switch]$SkipStore,
    [switch]$WithBackend1,
    [switch]$WithLauncher,
    [switch]$Reseed
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\_common.ps1"

& "$PSScriptRoot\postgres.ps1"
& "$PSScriptRoot\s3.ps1" -Seed

if (-not $SkipRender) { & "$PSScriptRoot\render.ps1" }

& "$PSScriptRoot\backend.ps1"

if (Test-PortListening $Ports.backend) {
    & "$PSScriptRoot\seed.ps1" -Force:$Reseed
} else {
    Write-Note 'skipping the seed: the API never came up, so the schema may not exist yet'
}

if ($WithBackend1) { & "$PSScriptRoot\backend1.ps1" }
if (-not $SkipDashboard) { & "$PSScriptRoot\dashboard.ps1" }
if (-not $SkipStore) { & "$PSScriptRoot\store.ps1" }

# Last, and detached: the launcher points at the API, so everything it talks to
# is already listening by now.
if ($WithLauncher) { & "$PSScriptRoot\launcher.ps1" -Detached }

Write-Host ''
Write-Step 'Stack'
Write-Host "    API              http://127.0.0.1:$($Ports.backend)"
Write-Host "    API docs         http://127.0.0.1:$($Ports.backend)/scalar"
Write-Host "    Store            http://localhost:$($Ports.store)"
Write-Host "    Admin dashboard  http://localhost:$($Ports.dashboard)"
Write-Host "    S3               http://127.0.0.1:$($Ports.s3)"
if (Test-PortListening $Ports.render) { Write-Host "    Render service   http://127.0.0.1:$($Ports.render)" }
if (Test-PortListening $Ports.backend1) { Write-Host "    backend-1        http://127.0.0.1:$($Ports.backend1)" }
Write-Host ''
Write-Host '    Admin password   ADMIN_PASSWORD in plus-backend-main/.env'
Write-Host '    Seeded player    a5331404-0e77-440e-8bef-24c071dac1ae (Wyvest)'
Write-Host ''
if ($WithLauncher) {
    Write-Host '    OneLauncher      building in the background; set Settings > Developer >'
    Write-Host "                     Poly+ backend URL to http://127.0.0.1:$($Ports.backend)"
} else {
    Write-Host '    Launcher         .\startlocal\start-all.ps1 -WithLauncher (or launcher.ps1)'
}
Write-Host '    Check            .\startlocal\status.ps1'
Write-Host '    Shut down        .\startlocal\stop-all.ps1'
