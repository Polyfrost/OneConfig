<#
.SYNOPSIS
    Zone: database seed data.

.DESCRIPTION
    Applies scripts/populate-db.sql, which truncates the cosmetic tables and
    re-inserts the dev fixtures: five capes, three emotes and the Wyvest player.

    Run this after the backend has started at least once, since the schema comes
    from the migrations the backend applies on startup. The object keys it
    references are the ones startlocal\s3.ps1 -Seed writes into the bucket.
#>
param([switch]$Force)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\_common.ps1"

Write-Step 'Database seed'
Assert-Tools @('docker')

$marker = Join-Path $LocalDir 'seeded'
if ((Test-Path $marker) -and -not $Force) {
    Write-Ok "already seeded on $(Get-Content $marker | Select-Object -First 1) (-Force to reapply)"
    return
}

# Our own fixtures, not plus-backend-main/scripts/populate-db.sql: that file
# still targets the emote tables m20260704_000004_drop_emotes removed, and none
# of its cosmetics carry a price, so the shop would come up empty.
$seed = Join-Path $PSScriptRoot 'seed.sql'
if (-not (Test-Path $seed)) {
    throw "No seed file at $seed"
}

# The migrations create these; seeding before the backend's first run would fail
# on a missing table, which is a confusing way to learn the ordering.
$hasSchema = Invoke-Native {
    docker exec $PgContainer psql -U postgres -d $PgDatabase -tAc "SELECT to_regclass('public.cosmetic')"
}
if (($hasSchema -join '').Trim() -eq '') {
    throw 'The cosmetic table does not exist yet. Start the backend once so it runs its migrations, then seed.'
}

# Copy the file in and run it with -f rather than piping it to `docker exec -i`:
# stdin redirection through a PowerShell function does not reach the child, and
# psql then sits waiting on input that never arrives.
Invoke-Native { docker cp $seed "${PgContainer}:/tmp/populate-db.sql" } | Out-Null

$output = Invoke-Native {
    docker exec $PgContainer psql -U postgres -d $PgDatabase -q -v ON_ERROR_STOP=1 -f /tmp/populate-db.sql
}

# psql prefixes its diagnostics with "psql:<file>:<line>: ", so anchoring on the
# start of the line would let every real failure through as a success.
$errors = $output | Where-Object { $_ -match '(ERROR|FATAL):' }
if ($errors -or $LASTEXITCODE -ne 0) {
    $errors | ForEach-Object { Write-Note $_ }
    throw "Seeding failed (psql exit code $LASTEXITCODE)."
}

New-Item -ItemType Directory -Force -Path $LocalDir | Out-Null
Set-Content -Path $marker -Value (Get-Date -Format o)

Write-Ok 'seed applied'
Write-Ok 'player a5331404-0e77-440e-8bef-24c071dac1ae (Wyvest) now owns the dev cosmetics'
