<#
.SYNOPSIS
  Run MultiCourtUmpireE2ETest across up to 4 courts (parallel if multiple adb devices).

.DESCRIPTION
  Prerequisites:
  - Host must run wyniki e2e backend on port 18087
    (wyniki-v2/docker-compose.e2e.yml maps 18087:8080).
  - Prefer JAVA_HOME = Eclipse Adoptium JDK 17.
  - Emulators/devices: LAN URL in -BaseUrl (tests call RetrofitClient.overrideBaseUrl).
  - 1 AVD => sequential courts; 2-4 AVD => parallel jobs.
  - Physical devices need the host LAN IP in -BaseUrl instead of 10.0.2.2.

  Behavior:
  - Lists adb devices.
  - Creates one shared tournament fixture via -HostBaseUrl (localhost:18087 by default).
  - If >= 2 devices: launches up to 4 connectedAndroidTest jobs in parallel
    (different e2e.courtIndex + ANDROID_SERIAL).
  - If 1 device: runs courts 0..3 sequentially on that device.
  - Cleans up the shared E2E marker when finished.

.EXAMPLE
  .\scripts\run_parallel_courts.ps1

.EXAMPLE
  .\scripts\run_parallel_courts.ps1 -BaseUrl http://10.0.2.2:18087 -HostBaseUrl http://localhost:18087

.EXAMPLE
  .\scripts\run_parallel_courts.ps1 -BaseUrl https://score.vestmedia.pl -HostBaseUrl https://score.vestmedia.pl
#>
[CmdletBinding()]
param(
    # URL seen by the Android emulator/device (instrumentation arg e2e.baseUrl).
    # Default: minipc LAN e2e (Windows host often has no local Docker).
    [string]$BaseUrl = $(if ($env:E2E_ANDROID_BASE_URL) { $env:E2E_ANDROID_BASE_URL } else { "http://192.168.31.5:18087" }),

    # URL used by this host script to create/cleanup the shared fixture.
    [string]$HostBaseUrl = $(if ($env:E2E_BASE_URL) { $env:E2E_BASE_URL } else { "http://192.168.31.5:18087" }),

    [string]$AdminPassword = $(if ($env:E2E_ADMIN_PASSWORD) { $env:E2E_ADMIN_PASSWORD } else { "e2e-admin" }),

    [ValidateRange(1, 4)]
    [int]$MaxCourts = 4,

    [string]$GradleTask = "connectedDebugAndroidTest",

    [string]$TestClass = "pl.vestmedia.tennisreferee.e2e.MultiCourtUmpireE2ETest",

    # When set, leave the shared E2E tournament for the orchestrator public assert.
    [switch]$SkipCleanup,

    # Optional path to write the shared marker (one line) for post-run asserts.
    [string]$MarkerOutFile = ""
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

function Get-AdbSerials {
    $lines = & adb devices 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "adb devices failed. Is Android platform-tools on PATH?`n$lines"
    }
    $serials = @()
    foreach ($line in $lines) {
        if ($line -match "^\s*(\S+)\s+device\s*$") {
            $serials += $Matches[1]
        }
    }
    return $serials
}

function Invoke-Json {
    param(
        [string]$Method,
        [string]$Url,
        [hashtable]$Body = $null,
        [string]$Token = "",
        [switch]$AllowFailure
    )
    $headers = @{}
    if ($Token) { $headers.Authorization = "Bearer $Token" }
    $params = @{
        Method      = $Method
        Uri         = $Url
        ContentType = "application/json"
        Headers     = $headers
    }
    if ($null -ne $Body) {
        $params.Body = ($Body | ConvertTo-Json -Depth 8 -Compress)
    }
    try {
        return Invoke-RestMethod @params
    } catch {
        if ($AllowFailure) {
            Write-Warning $_.Exception.Message
            return $null
        }
        throw
    }
}

function Get-AdminToken {
    param([string]$ApiBase, [string]$Password)
    $auth = Invoke-Json -Method POST -Url "$ApiBase/admin/api/auth" -Body @{ password = $Password }
    if (-not $auth.token) { throw "Admin auth failed against $ApiBase" }
    return [string]$auth.token
}

function New-SharedE2EFixture {
    param([string]$ApiBase, [string]$Marker, [string]$Token)

    $today = Get-Date -Format "yyyy-MM-dd"
    $end = (Get-Date).AddDays(1).ToString("yyyy-MM-dd")
    $tournament = Invoke-Json -Method POST -Url "$ApiBase/admin/api/tournaments" -Token $Token -Body @{
        name             = "$Marker Android Emulator Open"
        start_date       = $today
        end_date         = $end
        active           = $true
        is_simulation    = $true
        office_password  = "test"
        city             = "E2E"
        country          = "PL"
        court_count      = 8
    }
    $tournamentId = if ($tournament.id) { [int]$tournament.id } else { [int]$tournament.tournament.id }
    $defs = @(
        @{ first = "Ana"; gender = "F"; country = "PL" },
        @{ first = "Bartosz"; gender = "M"; country = "PL" },
        @{ first = "Celina"; gender = "F"; country = "CZ" },
        @{ first = "Dominik"; gender = "M"; country = "DE" },
        @{ first = "Elena"; gender = "F"; country = "ES" },
        @{ first = "Filip"; gender = "M"; country = "FR" },
        @{ first = "Gaja"; gender = "F"; country = "IT" },
        @{ first = "Hubert"; gender = "M"; country = "GB" }
    )
    $playerIds = @()
    $names = @()
    for ($i = 0; $i -lt $defs.Count; $i++) {
        $d = $defs[$i]
        $last = "$Marker-P$($i + 1)"
        $full = "$($d.first) $last"
        $created = Invoke-Json -Method POST -Url "$ApiBase/admin/api/tournaments/$tournamentId/players" -Token $Token -Body @{
            first_name = $d.first
            last_name  = $last
            name       = $full
            gender     = $d.gender
            country    = $d.country
            category   = "E2E"
        }
        $playerIds += [int]$created.id
        $names += $full
    }

    Invoke-Json -Method PUT -Url "$ApiBase/admin/api/tournaments/$tournamentId/bracket/groups" -Token $Token -Body @{
        groups = @(
            @{ name = "Group A"; players = @($playerIds[0], $playerIds[1], $playerIds[2], $playerIds[3]) },
            @{ name = "Group B"; players = @($playerIds[4], $playerIds[5], $playerIds[6], $playerIds[7]) }
        )
    } | Out-Null

    Invoke-Json -Method PUT -Url "$ApiBase/admin/api/tournaments/$tournamentId/bracket/knockout" -Token $Token -Body @{
        knockout = @(
            @{ phase = "semifinal"; position = 1; player1_name = $names[0]; player2_name = $names[4] },
            @{ phase = "semifinal"; position = 2; player1_name = $names[1]; player2_name = $names[5] },
            @{ phase = "final"; position = 1 },
            @{ phase = "third_place"; position = 1 }
        )
    } | Out-Null

    return @{ Marker = $Marker; TournamentId = $tournamentId }
}

function Remove-SharedE2EFixture {
    param([string]$ApiBase, [string]$Marker, [string]$Token)
    Invoke-Json -Method POST -Url "$ApiBase/admin/api/e2e/cleanup" -Token $Token -Body @{ marker = $Marker } -AllowFailure | Out-Null
}

Write-Host "=== Parallel court E2E ==="
Write-Host "Device BaseUrl : $BaseUrl"
Write-Host "Host API       : $HostBaseUrl"
Write-Host "NOTE: Host must run wyniki e2e on port 18087 (docker-compose.e2e.yml)."
Write-Host ""

$serials = @(Get-AdbSerials)
if ($serials.Count -eq 0) {
    throw "No adb devices in 'device' state. Start an emulator or connect a phone."
}

Write-Host "adb devices ($($serials.Count)):"
$serials | ForEach-Object { Write-Host "  - $_" }

$courtCount = [Math]::Min($MaxCourts, 4)
$marker = "E2E-{0:yyyyMMddHHmmss}-parallel" -f (Get-Date)
$apiBase = $HostBaseUrl.TrimEnd('/')
Write-Host "Admin auth against $apiBase ..."
$adminToken = Get-AdminToken -ApiBase $apiBase -Password $AdminPassword
Write-Host "Creating shared fixture marker=$marker ..."
$fixture = New-SharedE2EFixture -ApiBase $apiBase -Marker $marker -Token $adminToken
Write-Host "Shared tournamentId=$($fixture.TournamentId)"
Write-Host "E2E_MARKER=$marker"
if ($MarkerOutFile) {
    Set-Content -Path $MarkerOutFile -Value $marker -Encoding utf8
}

$gradlew = if ($IsWindows -or $env:OS -match "Windows") { ".\gradlew.bat" } else { "./gradlew" }
$failed = $false

try {
    if ($serials.Count -ge 2) {
        $jobs = @()
        $use = [Math]::Min($courtCount, $serials.Count)
        Write-Host "Mode: PARALLEL on $use devices / courts"
        for ($i = 0; $i -lt $use; $i++) {
            $serial = $serials[$i]
            $court = $i
            $jobs += Start-Job -Name "court-$court-$serial" -ScriptBlock {
                param($Root, $Gradlew, $Task, $Class, $Serial, $Court, $Base, $Marker, $Tid, $AdminPw)
                Set-Location $Root
                $env:ANDROID_SERIAL = $Serial
                $args = @(
                    $Task,
                    "-Pandroid.testInstrumentationRunnerArguments.class=$Class",
                    "-Pandroid.testInstrumentationRunnerArguments.e2e.baseUrl=$Base",
                    "-Pandroid.testInstrumentationRunnerArguments.e2e.adminPassword=$AdminPw",
                    "-Pandroid.testInstrumentationRunnerArguments.e2e.courtIndex=$Court",
                    "-Pandroid.testInstrumentationRunnerArguments.e2e.marker=$Marker",
                    "-Pandroid.testInstrumentationRunnerArguments.e2e.tournamentId=$Tid"
                )
                & $Gradlew @args
                if ($LASTEXITCODE -ne 0) { throw "Gradle failed for court=$Court serial=$Serial exit=$LASTEXITCODE" }
            } -ArgumentList $repoRoot, $gradlew, $GradleTask, $TestClass, $serial, $court, $BaseUrl, $fixture.Marker, $fixture.TournamentId, $AdminPassword
        }

        $jobs | Wait-Job | Out-Null
        foreach ($job in $jobs) {
            Write-Host "----- $($job.Name) -----"
            Receive-Job $job | Write-Host
            if ($job.State -ne "Completed") {
                $failed = $true
                Write-Error "Job $($job.Name) state=$($job.State)"
            }
            Remove-Job $job -Force
        }
    } else {
        $serial = $serials[0]
        Write-Host "Mode: SEQUENTIAL on single device $serial ($courtCount courts)"
        $env:ANDROID_SERIAL = $serial
        for ($court = 0; $court -lt $courtCount; $court++) {
            Write-Host "=== Court $court / serial=$serial ==="
            & $gradlew $GradleTask `
                "-Pandroid.testInstrumentationRunnerArguments.class=$TestClass" `
                "-Pandroid.testInstrumentationRunnerArguments.e2e.baseUrl=$BaseUrl" `
                "-Pandroid.testInstrumentationRunnerArguments.e2e.adminPassword=$AdminPassword" `
                "-Pandroid.testInstrumentationRunnerArguments.e2e.courtIndex=$court" `
                "-Pandroid.testInstrumentationRunnerArguments.e2e.marker=$($fixture.Marker)" `
                "-Pandroid.testInstrumentationRunnerArguments.e2e.tournamentId=$($fixture.TournamentId)"
            if ($LASTEXITCODE -ne 0) {
                $failed = $true
                Write-Error "Court $court failed with exit $LASTEXITCODE"
                break
            }
        }
    }
} finally {
    if ($SkipCleanup) {
        Write-Host "SkipCleanup: leaving shared fixture marker=$($fixture.Marker) for orchestrator assert"
    } else {
        Write-Host "Cleaning shared fixture marker=$($fixture.Marker) ..."
        Remove-SharedE2EFixture -ApiBase $apiBase -Marker $fixture.Marker -Token $adminToken
    }
}

if ($failed) {
    throw "One or more court runs failed."
}

Write-Host "All court runs finished successfully."
