<#
.SYNOPSIS
    Zone: the Poly+ store front, plus-website (port 3001).

.DESCRIPTION
    Next.js. It reads the API location from NEXT_PUBLIC_BACKEND_URL, which Next
    inlines at build time, so it is written to plus-website/.env.local rather
    than passed through the environment.

    Port 3001, not Next's default 3000: the admin dashboard already has 3000.
    That origin has to be in CORS_ORIGINS in plus-backend-main/.env, which this
    checks.

    Cart checkout posts to /stripe/create, which needs a real STRIPE_SECRET and a
    cosmetic carrying a Stripe price id. Seeded cosmetics have neither, so
    browsing works but checkout will fail until Stripe is configured and the
    cosmetics are created through the admin dashboard.
#>
param(
    [switch]$Stop,
    [switch]$Foreground
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\_common.ps1"

if ($Stop) {
    Write-Step 'Store'
    if (Stop-Recorded 'store') { Write-Ok 'stopped' } else { Write-Note 'was not running' }
    return
}

Write-Step 'Store (plus-website)'
Assert-Tools @('npm')

if (-not (Test-Path (Join-Path $StoreDir 'package.json'))) {
    throw "plus-website not found at $StoreDir"
}

if (Test-PortListening $Ports.store) {
    Write-Ok "already listening on $($Ports.store)"
    return
}

# NEXT_PUBLIC_* is substituted into the client bundle when Next compiles, so it
# has to be on disk before the dev server starts.
$envLocal = Join-Path $StoreDir '.env.local'
$backendUrl = "http://127.0.0.1:$($Ports.backend)"
if (-not (Test-Path $envLocal)) {
    Set-Content -Path $envLocal -Value "NEXT_PUBLIC_BACKEND_URL=$backendUrl"
    Write-Ok "wrote .env.local pointing at $backendUrl"
} else {
    $current = (Read-DotEnv $envLocal)['NEXT_PUBLIC_BACKEND_URL']
    Write-Ok "using existing .env.local (backend: $current)"
}

# A missing origin surfaces in the browser as an opaque network error, so say it
# plainly rather than letting the store look broken.
$dotenv = Read-DotEnv (Join-Path $BackendDir '.env')
$origin = "http://localhost:$($Ports.store)"
if ($dotenv.Contains('CORS_ORIGINS') -and $dotenv['CORS_ORIGINS'] -notlike "*$origin*") {
    Write-Note "CORS_ORIGINS in plus-backend-main/.env does not list $origin"
    Write-Note 'add it and restart the backend, or the store cannot call the API'
}

if (-not (Test-Path (Join-Path $StoreDir 'node_modules'))) {
    Write-Note 'installing dependencies (skinview3d builds from git, so this is not quick)'
    Push-Location $StoreDir
    try { Invoke-Native { npm install --no-audit --no-fund } | Out-Null } finally { Pop-Location }
}

if ($Foreground) {
    Push-Location $StoreDir
    try { & npm.cmd run dev -- --port $Ports.store } finally { Pop-Location }
    return
}

$logDir = Join-Path $LocalDir 'logs'
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
$log = Join-Path $logDir 'store.log'

$process = Start-Process -FilePath 'npm.cmd' -PassThru -WindowStyle Hidden `
    -WorkingDirectory $StoreDir `
    -ArgumentList @('run', 'dev', '--', '--port', "$($Ports.store)") `
    -RedirectStandardOutput $log -RedirectStandardError "$log.err"
Save-ServicePid 'store' $process.Id

if (Wait-ForPort $Ports.store 180) {
    Write-Ok "up on $origin (pid $($process.Id))"
    if (-not (Test-PortListening $Ports.backend)) {
        Write-Note 'the API is not running; start it with startlocal\backend.ps1'
    }
} else {
    Write-Note "did not come up in time; see $log and $log.err"
}
