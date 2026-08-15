<#
.SYNOPSIS
    Reports which local zones are up.

.DESCRIPTION
    Read-only. A port being open is not quite the same as the service being
    healthy, so the API also gets an actual request.
#>

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\_common.ps1"

Write-Step 'Zones'
foreach ($zone in $Ports.GetEnumerator()) {
    $pidFile = Join-Path $PidDir "$($zone.Key).pid"
    $owner = if (Test-Path $pidFile) { " pid $(Get-Content $pidFile | Select-Object -First 1)" } else { '' }

    if (Test-PortListening $zone.Value) {
        Write-Host ("  UP    {0,-10} :{1}{2}" -f $zone.Key, $zone.Value, $owner) -ForegroundColor Green
    } else {
        Write-Host ("  DOWN  {0,-10} :{1}" -f $zone.Key, $zone.Value) -ForegroundColor DarkGray
    }
}

if (Test-PortListening $Ports.backend) {
    Write-Step 'API check'
    try {
        $response = Invoke-WebRequest "http://127.0.0.1:$($Ports.backend)/openapi.json" `
            -UseBasicParsing -TimeoutSec 10
        $document = $response.Content | ConvertFrom-Json
        $paths = @($document.paths.PSObject.Properties).Count
        Write-Ok "/openapi.json answered $($response.StatusCode) describing $paths paths"
    } catch {
        Write-Note "/openapi.json failed: $($_.Exception.Message)"
    }

    try {
        $cosmetics = Invoke-WebRequest "http://127.0.0.1:$($Ports.backend)/cosmetics" `
            -UseBasicParsing -TimeoutSec 10
        $count = @(($cosmetics.Content | ConvertFrom-Json).cosmetics).Count
        Write-Ok "/cosmetics answered $($cosmetics.StatusCode) with $count cosmetic(s)"
    } catch {
        Write-Note "/cosmetics failed: $($_.Exception.Message)"
    }
}
