<#
.SYNOPSIS
    Zone: plus-backend, the Poly+ API (port 8080).

.DESCRIPTION
    Reads plus-backend-main/.env and starts `cargo run -- serve`. Migrations run
    at startup, so Postgres has to be up first (startlocal\postgres.ps1).

    Values travel as command line flags rather than environment variables on
    purpose: STRIPE_SECRET and friends are required flags with no fallback but
    are empty in a local .env, and assigning an empty string to $env:X on Windows
    deletes the variable instead of setting it, which would make bpaf reject the
    run. The AWS credentials are the exception, since rust-s3 only reads those
    from the environment.
#>
param(
    [switch]$Stop,
    [switch]$Foreground
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\_common.ps1"

if ($Stop) {
    Write-Step 'plus-backend'
    if (Stop-Recorded 'backend') { Write-Ok 'stopped' } else { Write-Note 'was not running' }
    return
}

Write-Step 'plus-backend'
Assert-Tools @('cargo')

$dotenv = Read-DotEnv (Join-Path $BackendDir '.env')
if ($dotenv.Count -eq 0) {
    throw "No .env in $BackendDir. Copy .env.example to .env first."
}

function Get-Setting {
    param([string]$Key, [string]$Default = '')
    if ($dotenv.Contains($Key)) { return $dotenv[$Key] }
    return $Default
}

# Start-Process rejects an empty element in -ArgumentList, and several of these
# flags are legitimately empty. The literal two-character token "" survives the
# command line and the C runtime hands the process an empty argument back.
function Format-Arg {
    param([AllowEmptyString()][string]$Value)
    if ($Value -eq '' -or $Value -match '\s') { return '"' + $Value.Replace('"', '\"') + '"' }
    return $Value
}

foreach ($key in @('AWS_ACCESS_KEY_ID', 'AWS_SECRET_ACCESS_KEY', 'RUST_LOG')) {
    $value = Get-Setting $key
    if ($value -ne '') { Set-Item -Path "env:$key" -Value $value }
}

# Only advertise the render service when it is actually answering, otherwise
# every cosmetic upload waits on a connection that will never open.
$renderUrl = Get-Setting 'RENDER_SERVICE_URL'
if ($renderUrl -ne '' -and -not (Test-PortListening $Ports.render)) {
    Write-Note 'render service is not running; cosmetic covers will not be generated'
    $renderUrl = ''
}

$settings = @(
    @('--database-url', (Get-Setting 'DATABASE_URL' 'postgresql://postgres:postgres@localhost:5432/local')),
    @('--s3-bucket-name', (Get-Setting 'S3_BUCKET_NAME' 'local')),
    @('--s3-bucket-region', (Get-Setting 'S3_BUCKET_REGION' 'local')),
    @('--s3-bucket-endpoint', (Get-Setting 'S3_BUCKET_ENDPOINT' "http://localhost:$($Ports.s3)")),
    @('--admin-password', (Get-Setting 'ADMIN_PASSWORD' 'dev')),
    @('--stripe-secret', (Get-Setting 'STRIPE_SECRET')),
    @('--stripe-webhook-secret', (Get-Setting 'STRIPE_WEBHOOK_SECRET')),
    @('--stripe-success-url', (Get-Setting 'STRIPE_SUCCESS_URL')),
    @('--stripe-cancel-url', (Get-Setting 'STRIPE_CANCEL_URL')),
    @('--render-service-url', $renderUrl),
    @('--cors-origins', (Get-Setting 'CORS_ORIGINS' 'http://localhost:3000')),
    # Both loopback families: PolyPlus's BackendUrl.LOCAL is
    # http://localhost:8080, and Windows resolves localhost to ::1 first.
    @('--bind-addr', "127.0.0.1:$($Ports.backend),[::1]:$($Ports.backend)")
)

if ($Foreground) {
    # Calling cargo directly passes each value as-is, so no quoting is needed.
    $direct = @('run', '--', 'serve')
    foreach ($setting in $settings) { $direct += $setting[0]; $direct += $setting[1] }

    Push-Location $BackendDir
    try { & cargo @direct } finally { Pop-Location }
    return
}

$arguments = @('run', '--', 'serve')
foreach ($setting in $settings) {
    $arguments += $setting[0]
    $arguments += (Format-Arg $setting[1])
}

if (Test-PortListening $Ports.backend) {
    Write-Ok "already listening on $($Ports.backend)"
    return
}

$logDir = Join-Path $LocalDir 'logs'
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
$log = Join-Path $logDir 'backend.log'

$process = Start-Process -FilePath 'cargo' -PassThru -WindowStyle Hidden `
    -WorkingDirectory $BackendDir -ArgumentList $arguments `
    -RedirectStandardOutput $log -RedirectStandardError "$log.err"
Save-ServicePid 'backend' $process.Id

Write-Note "building and starting (pid $($process.Id)); the first run compiles, so give it a few minutes"
if (Wait-ForPort $Ports.backend 600) {
    Write-Ok "API up on http://127.0.0.1:$($Ports.backend) (docs at /scalar)"
} else {
    Write-Note "did not come up in time; see $log and $log.err"
}
