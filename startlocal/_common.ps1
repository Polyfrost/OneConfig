# Shared paths and helpers for the startlocal scripts.
# Dot-source it: . "$PSScriptRoot\_common.ps1"

Set-StrictMode -Version Latest

$RepoRoot = Split-Path -Parent $PSScriptRoot

$BackendDir = Join-Path $RepoRoot 'plus-backend-main'
$Backend1Dir = Join-Path $RepoRoot 'backend-1'
$DashboardDir = Join-Path $RepoRoot 'plus-admin-dashboard'
$StoreDir = Join-Path $RepoRoot 'plus-website'
$LauncherDir = Join-Path $RepoRoot 'OneLauncher'
$RenderDir = Join-Path $BackendDir 'render-service'

$LocalDir = Join-Path $BackendDir '.local'
$BucketRoot = Join-Path $LocalDir 's3'
$BucketDir = Join-Path $BucketRoot 'local'
$PidDir = Join-Path $LocalDir 'pids'

$PgContainer = 'postgres'
$PgDatabase = 'local'

$Ports = [ordered]@{
    postgres  = 5432
    s3        = 8081
    backend   = 8080
    render    = 8090
    dashboard = 3000
    store     = 3001
    backend1  = 8082
}

function Write-Step { param([string]$Message) Write-Host "==> $Message" -ForegroundColor Cyan }
function Write-Ok { param([string]$Message) Write-Host "    $Message" -ForegroundColor Green }
function Write-Note { param([string]$Message) Write-Host "    $Message" -ForegroundColor Yellow }

function Test-PortListening {
    param([int]$Port)
    $null -ne (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
}

function Wait-ForPort {
    param([int]$Port, [int]$TimeoutSeconds = 180)

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-PortListening $Port) { return $true }
        Start-Sleep -Seconds 2
    }
    return $false
}

# Windows PowerShell 5.1 turns anything a native command writes to stderr into a
# terminating error while $ErrorActionPreference is 'Stop'. Several docker and
# psql calls here are chatty but harmless, so run them with that relaxed and
# hand back the combined output as plain strings.
function Invoke-Native {
    param([scriptblock]$Command)

    $previous = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        return (& $Command 2>&1 | ForEach-Object { "$_" })
    } finally {
        $ErrorActionPreference = $previous
    }
}

function Find-Rclone {
    $command = Get-Command rclone -ErrorAction SilentlyContinue
    if ($command) { return $command.Source }

    $packages = Join-Path $env:LOCALAPPDATA 'Microsoft\WinGet\Packages'
    if (Test-Path $packages) {
        $found = Get-ChildItem $packages -Recurse -Filter rclone.exe -ErrorAction SilentlyContinue |
            Select-Object -First 1
        if ($found) { return $found.FullName }
    }

    return $null
}

function Find-Chromium {
    foreach ($candidate in @(
            (Join-Path $env:ProgramFiles 'Google\Chrome\Application\chrome.exe'),
            (Join-Path ${env:ProgramFiles(x86)} 'Google\Chrome\Application\chrome.exe'),
            (Join-Path $env:LOCALAPPDATA 'Google\Chrome\Application\chrome.exe'),
            (Join-Path ${env:ProgramFiles(x86)} 'Microsoft\Edge\Application\msedge.exe'),
            (Join-Path $env:ProgramFiles 'Microsoft\Edge\Application\msedge.exe'))) {
        if ($candidate -and (Test-Path $candidate)) { return $candidate }
    }
    return $null
}

# Reads KEY=VALUE lines out of the backend's .env. Values are kept verbatim,
# including empty ones, which matter: several backend flags are required and
# have no fallback, and Windows deletes an environment variable when you assign
# it an empty string, so those have to travel on the command line instead.
function Read-DotEnv {
    param([string]$Path)

    $values = [ordered]@{}
    if (-not (Test-Path $Path)) { return $values }

    foreach ($line in Get-Content $Path) {
        $trimmed = $line.Trim()
        if ($trimmed -eq '' -or $trimmed.StartsWith('#')) { continue }

        $split = $trimmed.IndexOf('=')
        if ($split -lt 1) { continue }

        $key = $trimmed.Substring(0, $split).Trim()
        $value = $trimmed.Substring($split + 1).Trim().Trim('"')
        $values[$key] = $value
    }

    return $values
}

function Save-ServicePid {
    param([string]$Name, [int]$ProcessId)
    New-Item -ItemType Directory -Force -Path $PidDir | Out-Null
    Set-Content -Path (Join-Path $PidDir "$Name.pid") -Value $ProcessId
}

function Stop-Recorded {
    param([string]$Name)

    $file = Join-Path $PidDir "$Name.pid"
    if (-not (Test-Path $file)) { return $false }

    $recorded = (Get-Content $file | Select-Object -First 1)
    Remove-Item $file -Force

    if (-not (Get-Process -Id $recorded -ErrorAction SilentlyContinue)) { return $false }

    Stop-Process -Id $recorded -Force -ErrorAction SilentlyContinue
    return $true
}

function Assert-Tools {
    param([string[]]$Tools)

    $env:Path = "$HOME\.cargo\bin;$env:Path"
    foreach ($tool in $Tools) {
        if (-not (Get-Command $tool -ErrorAction SilentlyContinue)) {
            throw "$tool is required but was not found on PATH."
        }
    }
}
