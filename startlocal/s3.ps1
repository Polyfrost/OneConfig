<#
.SYNOPSIS
    Zone: S3 object storage (port 8081, rclone).

.DESCRIPTION
    Serves plus-backend-main/.local/s3 as an S3 endpoint. rclone maps objects
    straight onto files, so the bucket can be seeded by dropping files into
    .local/s3/local rather than going through an S3 client.

    Seeding: scripts/dev-capes.json holds presigned R2 urls that have long since
    expired, so -Seed writes placeholder cape textures instead. Their object keys
    match the storage_path values in scripts/populate-db.sql, which is what makes
    the seeded rows resolve.
#>
param(
    [switch]$Stop,
    [switch]$Seed,
    [switch]$Force
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\_common.ps1"

if ($Stop) {
    Write-Step 'S3 (rclone)'
    if (Stop-Recorded 's3') { Write-Ok 'stopped' } else { Write-Note 'was not running' }
    return
}

Write-Step 'S3 (rclone)'

$rclone = Find-Rclone
if (-not $rclone) {
    throw 'rclone is required. Install it with: winget install --id Rclone.Rclone -e'
}

New-Item -ItemType Directory -Force -Path `
    (Join-Path $BucketDir 'capes'),
    (Join-Path $BucketDir 'emotes'),
    (Join-Path $BucketDir 'covers') | Out-Null

if ($Seed) {
    Add-Type -AssemblyName System.Drawing

    # A Minecraft cape texture is 64x32 with the visible back panel at (1,1),
    # 10x16. One flat colour per cape plus a light patch is enough to tell them
    # apart in the dashboard and in game.
    $capes = [ordered]@{
        oneclient   = '#4F8EF7'
        oneconfig   = '#7A5CF0'
        onelauncher = '#2FBF71'
        poly        = '#F25C54'
        moon        = '#F0C808'
    }

    foreach ($cape in $capes.GetEnumerator()) {
        $path = Join-Path $BucketDir "capes\$($cape.Key).png"
        if ((Test-Path $path) -and -not $Force) { continue }

        $bitmap = New-Object System.Drawing.Bitmap 64, 32
        $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
        try {
            $graphics.Clear([System.Drawing.Color]::Transparent)
            $colour = [System.Drawing.ColorTranslator]::FromHtml($cape.Value)
            $graphics.FillRectangle((New-Object System.Drawing.SolidBrush $colour), 1, 1, 10, 16)
            $graphics.FillRectangle([System.Drawing.Brushes]::White, 4, 5, 4, 4)
        } finally {
            $graphics.Dispose()
        }
        $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
        $bitmap.Dispose()
        Write-Ok "wrote capes/$($cape.Key).png"
    }

    # The player emote is committed; wowtext and santaguise are built from a
    # PolyCosmetics checkout, which is not part of this workspace.
    $emote = Join-Path $BackendDir 'scripts\dev-player-emote.zip'
    $emoteDest = Join-Path $BucketDir 'emotes\player.zip'
    if ((Test-Path $emote) -and ((-not (Test-Path $emoteDest)) -or $Force)) {
        Copy-Item $emote $emoteDest -Force
        Write-Ok 'wrote emotes/player.zip'
    }

    foreach ($missing in @('wowtext', 'santaguise')) {
        if (-not (Test-Path (Join-Path $BucketDir "emotes\$missing.zip"))) {
            Write-Note "emotes/$missing.zip absent (needs a PolyCosmetics checkout); that cosmetic will 404"
        }
    }
}

if (Test-PortListening $Ports.s3) {
    Write-Ok "already listening on $($Ports.s3)"
    return
}

$process = Start-Process -FilePath $rclone -PassThru -WindowStyle Hidden -ArgumentList @(
    'serve', 's3',
    '--addr', "127.0.0.1:$($Ports.s3)",
    '--auth-key', 'local,local',
    $BucketRoot
)
Save-ServicePid 's3' $process.Id

if (Wait-ForPort $Ports.s3 30) {
    Write-Ok "serving $BucketDir on $($Ports.s3) (pid $($process.Id))"
} else {
    throw "rclone did not start listening on $($Ports.s3)."
}
