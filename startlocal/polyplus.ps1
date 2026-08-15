<#
.SYNOPSIS
    Zone: PolyPlus, the cosmetics mod that draws the in-game store.

.DESCRIPTION
    This is the mod behind the Wardrobe / Store / History screen. OneConfig only
    provides the shell around it: its own navigation has just Themes and
    Preferences under Personalization, so a OneConfig build alone can never
    change what the store shows.

    You usually do not need to build anything. PolyPlus picks its backend from an
    in-game OneConfig dropdown that defaults to production:

        PolyPlusConfig.kt   @Dropdown("API URL")  var apiUrl = BackendUrl.PRODUCTION
        BackendUrl.kt       LOCAL("http://localhost:8080")

    Setting that dropdown to LOCAL in game fires a callback that reconnects the
    websocket and refreshes the catalogue, so the store switches to the local
    backend live. Build from source only when you want to change the mod itself.

    The build resolves OneConfig from ~/.m2 rather than repo.polyfrost.org, so
    run startlocal\mod.ps1 first and keep deps.oneconfig in
    PolyPlus/stonecutter.properties.toml matching what that published.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File .\startlocal\polyplus.ps1
.EXAMPLE
    powershell -ExecutionPolicy Bypass -File .\startlocal\polyplus.ps1 -McVersion 1.21.8
#>
param(
    [string]$McVersion = '1.21.11',
    [switch]$Offline,
    [string[]]$GradleArgs = @()
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\_common.ps1"

Write-Step 'PolyPlus (cosmetics mod)'

$polyPlusDir = Join-Path $RepoRoot 'PolyPlus'
$gradlew = Join-Path $polyPlusDir 'gradlew.bat'
if (-not (Test-Path $gradlew)) {
    throw "PolyPlus not found at $polyPlusDir. Clone it: git clone https://github.com/Polyfrost/PolyPlus.git"
}

# Report the version pairing up front: a mismatch here is the difference between
# building against the local OneConfig and silently pulling a released one.
$properties = Get-Content (Join-Path $polyPlusDir 'stonecutter.properties.toml')
$wanted = ($properties | Select-String -Pattern '^\s*deps\.oneconfig\s*=' | Select-Object -First 1) -replace '.*"(.+)".*', '$1'
$modVersion = ($properties | Select-String -Pattern '^\s*mod\.version\s*=' | Select-Object -First 1) -replace '.*"(.+)".*', '$1'

Write-Ok "PolyPlus $modVersion for Minecraft $McVersion, against OneConfig $wanted"

$published = Join-Path $HOME ".m2\repository\org\polyfrost\oneconfig\$McVersion-fabric\$wanted"
if (Test-Path $published) {
    Write-Ok "found $McVersion-fabric:$wanted in mavenLocal"
} else {
    Write-Note "OneConfig $McVersion-fabric:$wanted is not in mavenLocal"
    Write-Note 'run startlocal\mod.ps1 first, or align deps.oneconfig with what it published'
}

$arguments = @(":${McVersion}:build")
if ($Offline) { $arguments += '--offline' }
$arguments += $GradleArgs

Write-Ok "gradlew $($arguments -join ' ')"
Write-Note 'the first build downloads Minecraft and its mappings, so give it a while'

Push-Location $polyPlusDir
try {
    & $gradlew @arguments
    if ($LASTEXITCODE -ne 0) {
        throw "gradlew $($arguments -join ' ') failed with exit code $LASTEXITCODE"
    }
} finally {
    Pop-Location
}

$libs = Join-Path $polyPlusDir "versions\$McVersion\build\libs"
if (Test-Path $libs) {
    Get-ChildItem $libs -Filter *.jar |
        Where-Object { $_.Name -notmatch 'sources|dev|shadow' } |
        ForEach-Object { Write-Ok "jar: $($_.FullName)" }
} else {
    Write-Note "no jars found under $libs"
}

Write-Host ''
Write-Note 'drop the jar in your mods folder next to OneConfig, then set'
Write-Note "PolyPlus > API URL > LOCAL in game to point it at http://localhost:$($Ports.backend)"
