<#
.SYNOPSIS
    Zone: the OneConfig Minecraft mod, built locally.

.DESCRIPTION
    Publishes OneConfig's modules into the local maven repository (~/.m2) and
    builds the mod jars, so nothing downstream has to reach
    repo.polyfrost.org for OneConfig itself. mavenLocal() sits first in the
    repository list in build.gradle.kts, which is what makes those artifacts win.

    What "offline" can and cannot mean here: third-party dependencies still come
    from Maven Central, Fabric and the Polyfrost maven the first time. Run -Warm
    once with a network connection to populate the Gradle cache and provision the
    Java 21 toolchain, and every run after that can use -Offline.

    Note that backend-1 is not a maven server. It is an update-check API that
    reads maven metadata and advertises download urls, so it cannot stand in as a
    Gradle repository.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File .\startlocal\mod.ps1 -Warm
.EXAMPLE
    powershell -ExecutionPolicy Bypass -File .\startlocal\mod.ps1 -Offline
.EXAMPLE
    powershell -ExecutionPolicy Bypass -File .\startlocal\mod.ps1 -Jars
#>
param(
    # Populate the Gradle cache and the Java toolchain from the network, once.
    [switch]$Warm,
    # Build with --offline; fails rather than reaching out.
    [switch]$Offline,
    # Also run buildAndCollect, which gathers the shippable jars.
    [switch]$Jars,
    # Extra Gradle arguments, e.g. --stacktrace.
    [string[]]$GradleArgs = @()
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\_common.ps1"

Write-Step 'OneConfig mod'

$gradlew = Join-Path $RepoRoot 'gradlew.bat'
if (-not (Test-Path $gradlew)) {
    throw "No Gradle wrapper at $gradlew"
}

# The build asks for a Java 21 toolchain. Gradle provisions one itself through
# the foojay resolver, but that needs the network, so a cached one is worth
# reporting: it is the difference between -Offline working and not.
$provisioned = Get-ChildItem (Join-Path $HOME '.gradle\jdks') -Directory -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -match '-21-' } |
    Select-Object -First 1

if ($provisioned) {
    Write-Ok "Java 21 toolchain: $($provisioned.FullName)"
} elseif ($Offline) {
    Write-Note 'No provisioned Java 21 toolchain found, and --offline cannot fetch one.'
    Write-Note 'Run this once without -Offline (or with -Warm) first.'
} else {
    Write-Note 'No Java 21 toolchain cached yet; Gradle will download one on this run.'
}

$cacheReady = Test-Path (Join-Path $HOME '.gradle\caches\modules-2')
if ($Offline -and -not $cacheReady) {
    throw 'The Gradle dependency cache is empty, so --offline cannot resolve anything. Run with -Warm first.'
}

function Invoke-Gradle {
    param([string[]]$Tasks, [switch]$AllowNetwork)

    $arguments = @($Tasks)
    if ($Offline -and -not $AllowNetwork) { $arguments += '--offline' }
    $arguments += $GradleArgs

    Write-Ok "gradlew $($arguments -join ' ')"

    Push-Location $RepoRoot
    try {
        & $gradlew @arguments
        if ($LASTEXITCODE -ne 0) {
            throw "gradlew $($arguments -join ' ') failed with exit code $LASTEXITCODE"
        }
    } finally {
        Pop-Location
    }
}

if ($Warm) {
    # Resolving the dependencies is what fills the cache; assembling would too,
    # but takes far longer for the same effect.
    Write-Note 'warming the Gradle cache and toolchain from the network'
    Invoke-Gradle -Tasks @(':modules:compose-bundle:dependencies', '--configuration', 'runtimeClasspath') -AllowNetwork
    Write-Ok 'cache warmed; later runs can use -Offline'
}

# Only the module projects, never the root task. A root-wide publishToMavenLocal
# also walks :minecraft:* (eight Minecraft versions across two loaders), which
# pulls in Stonecutter and Loom and fails long before any module is published.
# The publications live on the modules subprojects anyway.
#
# Ask Gradle which projects exist rather than inferring them from directory
# names: modules/dependencies holds a subdirectory that is not a registered
# project, and guessing produces a task path Gradle rejects.
Write-Ok 'listing module projects'
$projectList = Invoke-Native {
    Push-Location $RepoRoot
    try { & $gradlew projects -q --console=plain } finally { Pop-Location }
}

$moduleProjects = $projectList |
    ForEach-Object { if ($_ -match "Project '(:modules:[^']+)'") { $Matches[1] } } |
    Sort-Object -Unique |
    ForEach-Object { "${_}:publishToMavenLocal" }

if (-not $moduleProjects) {
    throw 'Gradle reported no :modules subprojects. Run `gradlew projects` to see why.'
}

Write-Ok "publishing $($moduleProjects.Count) module project(s) to mavenLocal"
Invoke-Gradle -Tasks $moduleProjects

$published = Join-Path $HOME '.m2\repository\org\polyfrost\oneconfig'
if (Test-Path $published) {
    $count = @(Get-ChildItem $published -Recurse -Filter *.jar -ErrorAction SilentlyContinue).Count
    Write-Ok "published $count jar(s) to $published"
} else {
    Write-Note "nothing landed in $published; check the Gradle output above"
}

if ($Jars) {
    Invoke-Gradle -Tasks @('buildAndCollect')
    $collected = Join-Path $RepoRoot 'build\libs'
    if (Test-Path $collected) {
        Get-ChildItem $collected -Filter *.jar | ForEach-Object { Write-Ok "jar: $($_.Name)" }
    }
}

Write-Host ''
Write-Ok 'OneConfig now resolves from mavenLocal() ahead of repo.polyfrost.org'
Write-Note 'third-party dependencies still come from Maven Central and Fabric on a cold cache'
