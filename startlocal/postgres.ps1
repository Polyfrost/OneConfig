<#
.SYNOPSIS
    Zone: Postgres (port 5432).

.DESCRIPTION
    Brings up the shared Postgres container, makes sure the "local" database
    exists and that pg_trgm is installed in it. The cosmetic search calls
    word_similarity(), which comes from that extension, so /cosmetics/search
    500s without it.

    The container is shared with other projects on this machine, so -Stop only
    reports it rather than tearing it down.
#>
param([switch]$Stop)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\_common.ps1"

if ($Stop) {
    Write-Step 'Postgres'
    Write-Note "container '$PgContainer' is shared; stop it yourself with: docker stop $PgContainer"
    return
}

Write-Step 'Postgres'
Assert-Tools @('docker')

$running = Invoke-Native { docker ps --filter "name=^/$PgContainer$" --format '{{.Names}}' }
if (-not $running) {
    $exists = Invoke-Native { docker ps -a --filter "name=^/$PgContainer$" --format '{{.Names}}' }
    if ($exists) {
        Write-Note "starting existing container '$PgContainer'"
        Invoke-Native { docker start $PgContainer } | Out-Null
    } else {
        Write-Note "creating container '$PgContainer' (postgres:16)"
        Invoke-Native {
            docker run -d --name $PgContainer -e POSTGRES_PASSWORD=postgres -p 5432:5432 postgres:16
        } | Out-Null
    }

    if (-not (Wait-ForPort $Ports.postgres 60)) {
        throw "Postgres did not start listening on $($Ports.postgres)."
    }
}

# CREATE DATABASE has no IF NOT EXISTS, and its error would be noise on every
# run, so ask first.
$present = Invoke-Native {
    docker exec $PgContainer psql -U postgres -tAc "SELECT 1 FROM pg_database WHERE datname='$PgDatabase'"
}
if (($present -join '').Trim() -ne '1') {
    Invoke-Native { docker exec $PgContainer psql -U postgres -c "CREATE DATABASE $PgDatabase" } | Out-Null
    Write-Ok "created database '$PgDatabase'"
}

Invoke-Native {
    docker exec $PgContainer psql -U postgres -d $PgDatabase -c 'CREATE EXTENSION IF NOT EXISTS pg_trgm'
} | Out-Null

Write-Ok "container '$PgContainer' up, database '$PgDatabase' ready with pg_trgm"
