[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("WARM", "COLD")]
    [string]$Mode,

    [ValidateRange(1, 120)]
    [int]$WaitMinutes = 10,

    [ValidateRange(1, 20)]
    [int]$Runs = 1,

    [string]$Serial = $env:ANDROID_SERIAL,

    [string]$AdbPath = "adb",

    [string]$Package = "com.cloneapp.lite",

    [string]$GuestPackage = "com.whatsapp",

    [ValidateRange(0, 99)]
    [int]$VirtualUser = 1,

    [string]$BuildLabel = "UNKNOWN",

    [ValidateRange(5, 120)]
    [int]$PostSendWaitSeconds = 20,

    [string]$OutputDir = (Join-Path ([IO.Path]::GetTempPath()) "CloneAppLite\locked-notification")
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-IsoTimestamp {
    return (Get-Date).ToString("o")
}

function Resolve-Adb {
    param([string]$Path)
    try {
        return (Get-Command $Path -ErrorAction Stop).Source
    } catch {
        throw "adb not found. Pass -AdbPath with the full path to adb.exe. Requested: $Path"
    }
}

$script:AdbResolved = Resolve-Adb -Path $AdbPath
$script:SerialResolved = $null
$script:EvidencePath = $null

function Invoke-AdbBase {
    param([string[]]$Arguments)

    $argsList = @()
    if ($script:SerialResolved) {
        $argsList += @("-s", $script:SerialResolved)
    }
    $argsList += $Arguments

    $lines = & $script:AdbResolved @argsList 2>&1
    $exitCode = $LASTEXITCODE
    return [pscustomobject]@{
        ExitCode = $exitCode
        Output   = (($lines | ForEach-Object { "$_" }) -join [Environment]::NewLine)
    }
}

function Get-WirelessSerials {
    $r = Invoke-AdbBase -Arguments @("devices")
    if ($r.ExitCode -ne 0) {
        throw "adb devices failed: $($r.Output)"
    }

    $serials = @()
    foreach ($line in ($r.Output -split "\r?\n")) {
        if ($line -match '^([^\s]+)\s+device(?:\s|$)') {
            $candidate = $Matches[1]
            if ($candidate -match ':\d+$' -or $candidate -match '_adb-tls-connect\._tcp') {
                $serials += $candidate
            }
        }
    }
    return @($serials)
}

function Resolve-WirelessSerial {
    param([string]$Requested)

    if ($Requested) {
        if ($Requested -notmatch ':\d+$' -and $Requested -notmatch '_adb-tls-connect\._tcp') {
            throw "Serial '$Requested' does not look like a wireless adb endpoint."
        }

        $available = Get-WirelessSerials
        if ($available -notcontains $Requested -and $Requested -match ':\d+$') {
            $connect = Invoke-AdbBase -Arguments @("connect", $Requested)
            if ($connect.ExitCode -ne 0) {
                throw "adb connect failed for $Requested : $($connect.Output)"
            }
            $available = Get-WirelessSerials
        }

        if ($available -notcontains $Requested) {
            throw "Wireless adb device '$Requested' is not connected. Connected wireless devices: $($available -join ', ')"
        }
        return $Requested
    }

    $found = Get-WirelessSerials
    if ($found.Count -eq 0) {
        throw "No wireless adb device is connected. Connect the Travel Phone or pass -Serial <host:port>."
    }
    if ($found.Count -gt 1) {
        throw "More than one wireless adb device is connected: $($found -join ', '). Pass -Serial explicitly."
    }
    return $found[0]
}

$script:SerialResolved = Resolve-WirelessSerial -Requested $Serial

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

function Write-Evidence {
    param([string]$Text = "")
    Add-Content -LiteralPath $script:EvidencePath -Value $Text -Encoding UTF8
}

function Write-Section {
    param([string]$Name)
    Write-Evidence ""
    Write-Evidence ("=" * 72)
    Write-Evidence $Name
    Write-Evidence ("=" * 72)
}

function Invoke-AdbCapture {
    param(
        [string]$Label,
        [string[]]$Arguments
    )
    $r = Invoke-AdbBase -Arguments $Arguments
    Write-Evidence "[$(Get-IsoTimestamp)] $Label"
    Write-Evidence "adb -s $script:SerialResolved $($Arguments -join ' ')"
    Write-Evidence "exit=$($r.ExitCode)"
    if ($r.Output) {
        Write-Evidence $r.Output
    } else {
        Write-Evidence "<no output>"
    }
    return $r
}

function Get-ProcessSnapshot {
    param([string]$Label)

    $r = Invoke-AdbBase -Arguments @("shell", "ps", "-A")
    $allLines = @($r.Output -split "\r?\n")
    $hostPattern = [regex]::Escape($Package)
    $guestPattern = [regex]::Escape($GuestPackage)

    $matched = @($allLines | Where-Object {
        $_ -match $hostPattern -or $_ -match $guestPattern
    })

    $host = @($matched | Where-Object {
        $_ -match "(^|\s)$hostPattern(?:$|:)"
    })

    $guestPx = @($matched | Where-Object {
        $_ -match "$hostPattern:p\d+\b"
    })

    $physicalGuest = @($matched | Where-Object {
        $_ -match "(^|\s)$guestPattern(?:$|:)"
    })

    Write-Section $Label
    Write-Evidence "timestamp=$(Get-IsoTimestamp)"
    Write-Evidence "adb_exit=$($r.ExitCode)"
    Write-Evidence "cloneapp_process_count=$($host.Count)"
    Write-Evidence "guest_px_process_count=$($guestPx.Count)"
    Write-Evidence "physical_whatsapp_process_count=$($physicalGuest.Count)"
    if ($matched.Count -gt 0) {
        Write-Evidence ($matched -join [Environment]::NewLine)
    } else {
        Write-Evidence "<no matching processes>"
    }

    return [pscustomobject]@{
        ExitCode      = $r.ExitCode
        HostCount     = $host.Count
        GuestPxCount  = $guestPx.Count
        MatchedLines  = $matched
    }
}

function Get-NotificationSnapshot {
    param([string]$Label)

    $r = Invoke-AdbBase -Arguments @("shell", "dumpsys", "notification", "--noredact")
    $lines = @($r.Output -split "\r?\n")
    $recordIndexes = @()

    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match "NotificationRecord\(" -and $lines[$i] -match "pkg=$([regex]::Escape($Package))") {
            $recordIndexes += $i
        }
    }

    $records = @()
    foreach ($idx in $recordIndexes) {
        $end = [Math]::Min($lines.Count - 1, $idx + 45)
        $block = @($lines[$idx..$end])
        $blockText = $block -join [Environment]::NewLine

        $user = "<absent>"
        $channel = "<absent>"
        $subText = "<absent>"
        $when = 0L

        if ($blockText -match 'user=UserHandle\{([^}]+)\}') {
            $user = $Matches[1]
        }
        if ($blockText -match 'Notification\(channel=([^\s\)]+)') {
            $channel = $Matches[1]
        }
        if ($blockText -match '(?im)^\s*(?:android\.)?subText\s*=\s*(.+)$') {
            $subText = $Matches[1].Trim()
        } elseif ($blockText -match '(?i)subText=([^\r\n\)]+)') {
            $subText = $Matches[1].Trim()
        }
        if ($blockText -match '(?m)^\s*when=(\d+)') {
            [void][Int64]::TryParse($Matches[1], [ref]$when)
        }

        $records += [pscustomobject]@{
            User      = $user
            Channel   = $channel
            SubText   = $subText
            When      = $when
            BlockText = $blockText
        }
    }

    Write-Section $Label
    Write-Evidence "timestamp=$(Get-IsoTimestamp)"
    Write-Evidence "notification_record_count=$($records.Count)"
    if ($records.Count -eq 0) {
        Write-Evidence "<no NotificationRecord for pkg=$Package>"
    } else {
        $n = 0
        foreach ($rec in $records) {
            $n++
            Write-Evidence "record[$n].pkg=$Package"
            Write-Evidence "record[$n].user=$($rec.User)"
            Write-Evidence "record[$n].subText=$($rec.SubText)"
            Write-Evidence "record[$n].channel=$($rec.Channel)"
            Write-Evidence "record[$n].when=$($rec.When)"
            Write-Evidence $rec.BlockText
            Write-Evidence "---"
        }
    }

    $maxWhen = 0L
    if ($records.Count -gt 0) {
        $maxWhen = ($records | Measure-Object -Property When -Maximum).Maximum
    }

    return [pscustomobject]@{
        Count   = $records.Count
        MaxWhen = [Int64]$maxWhen
        Records = $records
    }
}

function Get-IdleState {
    param([string]$Label)

    $r = Invoke-AdbBase -Arguments @("shell", "dumpsys", "deviceidle", "get", "deep")
    $state = $r.Output.Trim()
    Write-Evidence "[$(Get-IsoTimestamp)] $Label deep_idle=$state exit=$($r.ExitCode)"
    return [pscustomobject]@{
        ExitCode = $r.ExitCode
        State    = $state
    }
}

function Get-FailureScan {
    Write-Section "FAILURE WINDOW SCAN"

    $r = Invoke-AdbBase -Arguments @("logcat", "-d", "-v", "time")
    $patterns = @(
        "ANR in com.cloneapp.lite",
        "Timeout receiver",
        "App Died",
        "SecurityException",
        "ForegroundServiceStartNotAllowed",
        "MissingForegroundServiceType"
    )

    $hits = @()
    foreach ($line in ($r.Output -split "\r?\n")) {
        foreach ($pattern in $patterns) {
            if ($line -like "*$pattern*") {
                $hits += $line
                break
            }
        }
    }

    Write-Evidence "timestamp=$(Get-IsoTimestamp)"
    Write-Evidence "patterns=$($patterns -join ' | ')"
    if ($hits.Count -gt 0) {
        Write-Evidence ($hits -join [Environment]::NewLine)
    } else {
        Write-Evidence "<no matches>"
    }

    return @($hits)
}

function Read-SoundEvidence {
    while ($true) {
        $answer = (Read-Host "Did you hear the User$VirtualUser notification sound while the phone stayed locked? [Y/N/U=unknown]").Trim().ToUpperInvariant()
        if ($answer -in @("Y", "N", "U")) {
            return $answer
        }
        Write-Host "Please enter Y, N, or U."
    }
}

function Finish-Run {
    param(
        [string]$Status,
        [string]$Reason
    )

    Write-Section "RUN SUMMARY"
    Write-Evidence "status=$Status"
    Write-Evidence "reason=$Reason"
    Write-Evidence "evidence_file=$script:EvidencePath"

    $line = "$Status | $Reason | evidence=$script:EvidencePath"
    Write-Host $line
    return $line
}

function Invoke-GateRun {
    param([int]$RunNumber)

    $stamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $safeBuild = ($BuildLabel -replace '[^A-Za-z0-9._-]', '_')
    $script:EvidencePath = Join-Path $OutputDir ("locked-notification-{0}-{1}-run{2:D2}-{3}.txt" -f $safeBuild, $Mode, $RunNumber, $stamp)

    Write-Evidence "CloneApp Lite locked-notification gate"
    Write-Evidence "schema=1"
    Write-Evidence "started=$(Get-IsoTimestamp)"
    Write-Evidence "build_label=$BuildLabel"
    Write-Evidence "mode=$Mode"
    Write-Evidence "run=$RunNumber/$Runs"
    Write-Evidence "wait_minutes=$WaitMinutes"
    Write-Evidence "post_send_wait_seconds=$PostSendWaitSeconds"
    Write-Evidence "wireless_serial=$script:SerialResolved"
    Write-Evidence "host_package=$Package"
    Write-Evidence "guest_package=$GuestPackage"
    Write-Evidence "virtual_user=$VirtualUser"

    Write-Section "DEVICE"
    Invoke-AdbCapture -Label "device state" -Arguments @("get-state") | Out-Null
    Invoke-AdbCapture -Label "device model" -Arguments @("shell", "getprop", "ro.product.model") | Out-Null
    Invoke-AdbCapture -Label "android release" -Arguments @("shell", "getprop", "ro.build.version.release") | Out-Null
    Invoke-AdbCapture -Label "android sdk" -Arguments @("shell", "getprop", "ro.build.version.sdk") | Out-Null

    Write-Section "DAEMON PREF"
    $daemon = Invoke-AdbCapture -Label "mDaemonEnable" -Arguments @(
        "shell", "run-as", $Package, "cat", "shared_prefs/AppSharedPreferenceDelegate.xml"
    )

    if ($daemon.ExitCode -ne 0 -or $daemon.Output -notmatch '<boolean\s+name="mDaemonEnable"\s+value="false"\s*/>') {
        return Finish-Run -Status "INCONCLUSIVE" -Reason "mDaemonEnable=false was not verified"
    }

    if ($Mode -eq "COLD") {
        Write-Host "COLD setup: force-stopping $Package. Do NOT open CloneApp or WhatsApp."
        Invoke-AdbCapture -Label "force-stop CloneApp" -Arguments @("shell", "am", "force-stop", $Package) | Out-Null
        Start-Sleep -Seconds 2
    } else {
        [void](Read-Host "WARM setup: unlock the Travel Phone, open CloneApp > User$VirtualUser WhatsApp once, return to Home, then press Enter here")
    }

    $before = Get-ProcessSnapshot -Label "PROCESS STATE BEFORE LOCK"

    if ($before.ExitCode -ne 0) {
        return Finish-Run -Status "INCONCLUSIVE" -Reason "could not capture pre-lock process state"
    }

    if ($Mode -eq "COLD" -and $before.HostCount -ne 0) {
        return Finish-Run -Status "INCONCLUSIVE" -Reason "COLD setup failed: CloneApp process still present after force-stop"
    }

    if ($Mode -eq "WARM" -and ($before.HostCount -eq 0 -or $before.GuestPxCount -eq 0)) {
        return Finish-Run -Status "INCONCLUSIVE" -Reason "WARM setup not verified: CloneApp/guest pX process missing before lock"
    }

    $preNotif = Get-NotificationSnapshot -Label "NOTIFICATION STATE BEFORE LOCK"

    Write-Section "LOCK + DEEP IDLE"
    Write-Evidence "[$(Get-IsoTimestamp)] sending KEYCODE_SLEEP"
    Invoke-AdbCapture -Label "lock phone" -Arguments @("shell", "input", "keyevent", "223") | Out-Null
    Start-Sleep -Seconds 1

    Write-Evidence "[$(Get-IsoTimestamp)] force-idle begin"
    $forceIdle = Invoke-AdbCapture -Label "deviceidle force-idle" -Arguments @("shell", "dumpsys", "deviceidle", "force-idle")
    $idle1 = Get-IdleState -Label "after force-idle"

    if ($forceIdle.ExitCode -ne 0 -or $idle1.ExitCode -ne 0 -or $idle1.State -ne "IDLE") {
        return Finish-Run -Status "INCONCLUSIVE" -Reason "deep IDLE was not confirmed immediately after force-idle"
    }

    Write-Evidence "[$(Get-IsoTimestamp)] wait_start minutes=$WaitMinutes"
    Write-Host "Deep IDLE confirmed. Waiting $WaitMinutes minute(s) with the phone locked..."

    for ($minute = 1; $minute -le $WaitMinutes; $minute++) {
        Start-Sleep -Seconds 60
        Write-Host ("  waited {0}/{1} minute(s)" -f $minute, $WaitMinutes)
    }

    Write-Evidence "[$(Get-IsoTimestamp)] wait_complete"
    $idle2 = Get-IdleState -Label "after wait / before message"

    if ($idle2.ExitCode -ne 0 -or $idle2.State -ne "IDLE") {
        return Finish-Run -Status "INCONCLUSIVE" -Reason "device was not in deep IDLE at the end of the wait"
    }

    [void](Read-Host "WAIT COMPLETE. Send ONE normal WhatsApp text to User$VirtualUser now from the other phone. Press Enter here immediately after sending")
    Write-Evidence "[$(Get-IsoTimestamp)] operator reports message sent"
    Write-Host "Keeping the Travel Phone locked for $PostSendWaitSeconds seconds..."
    Start-Sleep -Seconds $PostSendWaitSeconds

    $sound = Read-SoundEvidence
    $soundText = switch ($sound) {
        "Y" { "YES" }
        "N" { "NO" }
        default { "UNKNOWN" }
    }

    Write-Section "OPERATOR SOUND EVIDENCE"
    Write-Evidence "timestamp=$(Get-IsoTimestamp)"
    Write-Evidence "notification_sound=$soundText"

    $postNotif = Get-NotificationSnapshot -Label "NOTIFICATION STATE AFTER MESSAGE"
    $after = Get-ProcessSnapshot -Label "PROCESS STATE AFTER MESSAGE"
    $idle3 = Get-IdleState -Label "after message"
    $failureHits = Get-FailureScan

    $freshRecord = $false
    if ($postNotif.Count -gt 0) {
        if ($preNotif.Count -eq 0) {
            $freshRecord = $true
        } elseif ($postNotif.MaxWhen -gt $preNotif.MaxWhen) {
            $freshRecord = $true
        }
    }

    Write-Section "EVALUATION INPUTS"
    Write-Evidence "notification_sound=$soundText"
    Write-Evidence "pre_notification_record_count=$($preNotif.Count)"
    Write-Evidence "post_notification_record_count=$($postNotif.Count)"
    Write-Evidence "pre_notification_max_when=$($preNotif.MaxWhen)"
    Write-Evidence "post_notification_max_when=$($postNotif.MaxWhen)"
    Write-Evidence "fresh_or_updated_notification_record=$freshRecord"
    Write-Evidence "pre_cloneapp_process_count=$($before.HostCount)"
    Write-Evidence "pre_guest_px_process_count=$($before.GuestPxCount)"
    Write-Evidence "post_cloneapp_process_count=$($after.HostCount)"
    Write-Evidence "post_guest_px_process_count=$($after.GuestPxCount)"
    Write-Evidence "idle_after_force=$($idle1.State)"
    Write-Evidence "idle_before_message=$($idle2.State)"
    Write-Evidence "idle_after_message=$($idle3.State)"
    Write-Evidence "failure_scan_hit_count=$($failureHits.Count)"

    if ($sound -eq "Y" -and $freshRecord) {
        return Finish-Run -Status "PASS" -Reason "sound heard and fresh/updated CloneApp NotificationRecord captured"
    }

    if ($sound -eq "N" -and -not $freshRecord -and $postNotif.Count -eq 0) {
        return Finish-Run -Status "FAIL" -Reason "no sound and no CloneApp NotificationRecord after message"
    }

    return Finish-Run -Status "INCONCLUSIVE" -Reason "sound/NotificationRecord evidence did not agree cleanly"
}

$allSummaries = @()

for ($run = 1; $run -le $Runs; $run++) {
    Write-Host ""
    Write-Host ("===== CloneApp Lite locked-notification gate: run {0}/{1} =====" -f $run, $Runs)
    $allSummaries += Invoke-GateRun -RunNumber $run

    if ($run -lt $Runs) {
        [void](Read-Host "Run $run complete. Press Enter when ready to set up the next run")
    }
}

Write-Host ""
Write-Host "===== Completed $Runs run(s) ====="
foreach ($summary in $allSummaries) {
    Write-Host $summary
}
