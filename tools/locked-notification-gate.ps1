[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("WARM", "COLD")]
    [string]$Mode,

    [ValidateRange(1, 120)]
    [int]$WaitMinutes = 10,

    [ValidateRange(1, 20)]
    [int]$Runs = 1,

    [Parameter(Mandatory = $true)]
    [string]$Serial,

    [string]$AdbPath = ".\adb.exe",

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
    }
    catch {
        throw "adb not found. Pass -AdbPath with the path to adb.exe. Requested: $Path"
    }
}

$script:AdbResolved = Resolve-Adb -Path $AdbPath
$script:SerialResolved = $Serial
$script:EvidencePath = $null

if (($script:SerialResolved -notmatch ':\d+$') -and ($script:SerialResolved -notmatch '_adb-tls-connect\._tcp')) {
    throw "Serial '$script:SerialResolved' does not look like a wireless adb endpoint."
}

function Invoke-Adb {
    param([string[]]$Arguments)

    $argList = @("-s", $script:SerialResolved)
    $argList += $Arguments

    $oldPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"

    try {
        $lines = & $script:AdbResolved @argList 2>&1
        $exitCode = $LASTEXITCODE
    }
    catch {
        $lines = @($_.Exception.Message)
        $exitCode = 1
    }
    finally {
        $ErrorActionPreference = $oldPreference
    }

    return [pscustomobject]@{
        ExitCode = $exitCode
        Output = (($lines | ForEach-Object { "$_" }) -join [Environment]::NewLine)
    }
}

function Invoke-AdbGlobal {
    param([string[]]$Arguments)

    $oldPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"

    try {
        $lines = & $script:AdbResolved @Arguments 2>&1
        $exitCode = $LASTEXITCODE
    }
    catch {
        $lines = @($_.Exception.Message)
        $exitCode = 1
    }
    finally {
        $ErrorActionPreference = $oldPreference
    }

    return [pscustomobject]@{
        ExitCode = $exitCode
        Output = (($lines | ForEach-Object { "$_" }) -join [Environment]::NewLine)
    }
}

function Reconnect-WirelessAdb {
    param([int]$Attempts = 12)

    Write-Evidence "[$(Get-IsoTimestamp)] wireless adb reconnect begin"

    for ($attempt = 1; $attempt -le $Attempts; $attempt++) {
        $connect = Invoke-AdbGlobal -Arguments @("connect", $script:SerialResolved)
        Write-Evidence "reconnect_attempt=$attempt exit=$($connect.ExitCode) output=$($connect.Output)"

        Start-Sleep -Seconds 2

        $state = Invoke-Adb -Arguments @("get-state")
        if (($state.ExitCode -eq 0) -and ($state.Output.Trim() -eq "device")) {
            Write-Evidence "[$(Get-IsoTimestamp)] wireless adb reconnect success attempt=$attempt"
            return $true
        }

        Start-Sleep -Seconds 3
    }

    Write-Evidence "[$(Get-IsoTimestamp)] wireless adb reconnect failed"
    return $false
}

$stateCheck = Invoke-Adb -Arguments @("get-state")
if (($stateCheck.ExitCode -ne 0) -or ($stateCheck.Output.Trim() -ne "device")) {
    throw "Wireless adb device '$script:SerialResolved' is not ready. adb get-state: $($stateCheck.Output)"
}

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

    $result = Invoke-Adb -Arguments $Arguments

    Write-Evidence "[$(Get-IsoTimestamp)] $Label"
    Write-Evidence "adb -s $script:SerialResolved $($Arguments -join ' ')"
    Write-Evidence "exit=$($result.ExitCode)"

    if ([string]::IsNullOrWhiteSpace($result.Output)) {
        Write-Evidence "<no output>"
    }
    else {
        Write-Evidence $result.Output
    }

    return $result
}

function Get-ProcessSnapshot {
    param([string]$Label)

    $result = Invoke-Adb -Arguments @("shell", "ps", "-A")
    $allLines = @($result.Output -split '\r?\n')

    $packageRegex = [regex]::Escape($Package)
    $guestRegex = [regex]::Escape($GuestPackage)

    $hostProcesses = @(
        $allLines | Where-Object {
            $_ -match ('\s' + $packageRegex + '(?::[^\s]+)?\s*$')
        }
    )

    $hostLinuxUser = $null
    if ($hostProcesses.Count -gt 0) {
        $parts = @($hostProcesses[0].Trim() -split '\s+')
        if ($parts.Count -gt 0) {
            $hostLinuxUser = $parts[0]
        }
    }

    $guestProxyProcesses = @(
        $allLines | Where-Object {
            $_ -match ('\s' + $packageRegex + ':p\d+\s*$')
        }
    )

    $allWhatsappProcesses = @(
        $allLines | Where-Object {
            $_ -match ('\s' + $guestRegex + '(?::[^\s]+)?\s*$')
        }
    )

    $virtualGuestByHostUid = @()
    $whatsappOtherUid = @()

    foreach ($line in $allWhatsappProcesses) {
        $parts = @($line.Trim() -split '\s+')
        $linuxUser = $null
        if ($parts.Count -gt 0) {
            $linuxUser = $parts[0]
        }

        if (($null -ne $hostLinuxUser) -and ($linuxUser -eq $hostLinuxUser)) {
            $virtualGuestByHostUid += $line
        }
        else {
            $whatsappOtherUid += $line
        }
    }

    $matched = @()
    $matched += $hostProcesses
    foreach ($line in $allWhatsappProcesses) {
        if ($matched -notcontains $line) {
            $matched += $line
        }
    }

    Write-Section $Label
    Write-Evidence "timestamp=$(Get-IsoTimestamp)"
    Write-Evidence "adb_exit=$($result.ExitCode)"
    Write-Evidence "cloneapp_linux_user=$hostLinuxUser"
    Write-Evidence "cloneapp_process_count=$($hostProcesses.Count)"
    Write-Evidence "guest_px_process_count=$($guestProxyProcesses.Count)"
    Write-Evidence "virtual_guest_host_uid_process_count=$($virtualGuestByHostUid.Count)"
    Write-Evidence "whatsapp_other_uid_process_count=$($whatsappOtherUid.Count)"

    if ($matched.Count -gt 0) {
        Write-Evidence ($matched -join [Environment]::NewLine)
    }
    else {
        Write-Evidence "<no matching CloneApp/WhatsApp processes>"
    }

    return [pscustomobject]@{
        ExitCode = $result.ExitCode
        HostCount = $hostProcesses.Count
        GuestPxCount = $guestProxyProcesses.Count
        VirtualGuestCount = $virtualGuestByHostUid.Count
        WhatsappOtherUidCount = $whatsappOtherUid.Count
        HostLinuxUser = $hostLinuxUser
    }
}

function Get-NotificationSnapshot {
    param([string]$Label)

    $result = Invoke-Adb -Arguments @("shell", "dumpsys", "notification", "--noredact")
    $lines = @($result.Output -split '\r?\n')
    $packageText = "pkg=$Package"

    $records = @()

    for ($i = 0; $i -lt $lines.Count; $i++) {
        if (($lines[$i] -notlike "*NotificationRecord(*") -or ($lines[$i] -notlike "*$packageText*")) {
            continue
        }

        $end = [Math]::Min($lines.Count - 1, $i + 60)
        $blockLines = @($lines[$i..$end])
        $blockText = $blockLines -join [Environment]::NewLine

        $user = "<absent>"
        $channel = "<absent>"
        $subText = "<absent>"
        [Int64]$whenValue = 0

        $mUser = [regex]::Match($blockText, 'user=UserHandle\{([^}]+)\}')
        if ($mUser.Success) {
            $user = $mUser.Groups[1].Value
        }

        $mChannel = [regex]::Match($blockText, 'Notification\(channel=([^\s\)]+)')
        if ($mChannel.Success) {
            $channel = $mChannel.Groups[1].Value
        }

        $mSub = [regex]::Match($blockText, '(?im)^\s*(?:android\.)?subText\s*=\s*(.+)$')
        if (-not $mSub.Success) {
            $mSub = [regex]::Match($blockText, '(?i)subText=([^\r\n\)]+)')
        }
        if ($mSub.Success) {
            $subText = $mSub.Groups[1].Value.Trim()
        }

        $mWhen = [regex]::Match($blockText, '(?m)^\s*when=(\d+)')
        if ($mWhen.Success) {
            [void][Int64]::TryParse($mWhen.Groups[1].Value, [ref]$whenValue)
        }

        $records += [pscustomobject]@{
            User = $user
            Channel = $channel
            SubText = $subText
            When = $whenValue
            Block = $blockText
        }
    }

    Write-Section $Label
    Write-Evidence "timestamp=$(Get-IsoTimestamp)"
    Write-Evidence "notification_record_count=$($records.Count)"

    if ($records.Count -eq 0) {
        Write-Evidence "<no NotificationRecord for pkg=$Package>"
    }
    else {
        $recordNumber = 0
        foreach ($record in $records) {
            $recordNumber++
            Write-Evidence "record[$recordNumber].pkg=$Package"
            Write-Evidence "record[$recordNumber].user=$($record.User)"
            Write-Evidence "record[$recordNumber].subText=$($record.SubText)"
            Write-Evidence "record[$recordNumber].channel=$($record.Channel)"
            Write-Evidence "record[$recordNumber].when=$($record.When)"
            Write-Evidence $record.Block
            Write-Evidence "---"
        }
    }

    [Int64]$maxWhen = 0
    foreach ($record in $records) {
        if ($record.When -gt $maxWhen) {
            $maxWhen = $record.When
        }
    }

    return [pscustomobject]@{
        Count = $records.Count
        MaxWhen = $maxWhen
    }
}

function Get-IdleState {
    param([string]$Label)

    $result = Invoke-Adb -Arguments @("shell", "dumpsys", "deviceidle", "get", "deep")
    $state = $result.Output.Trim()

    Write-Evidence "[$(Get-IsoTimestamp)] $Label deep_idle=$state exit=$($result.ExitCode)"

    return [pscustomobject]@{
        ExitCode = $result.ExitCode
        State = $state
    }
}

function Get-FailureScan {
    Write-Section "FAILURE WINDOW SCAN"

    $result = Invoke-Adb -Arguments @("logcat", "-d", "-v", "time")

    $patterns = @(
        "ANR in com.cloneapp.lite",
        "Timeout receiver",
        "App Died",
        "SecurityException",
        "ForegroundServiceStartNotAllowed",
        "MissingForegroundServiceType"
    )

    $hits = @()

    foreach ($line in ($result.Output -split '\r?\n')) {
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
    }
    else {
        Write-Evidence "<no matches>"
    }

    return $hits
}

function Start-OnDeviceCollector {
    param(
        [string]$RemotePath,
        [int]$DelaySeconds
    )

    $collectorScript = @'
OUT="__OUT__"
DELAY="__DELAY__"
(
    sleep "$DELAY"
    {
        echo "collector_timestamp=$(date +%s)"
        echo "=== IDLE ==="
        dumpsys deviceidle get deep
        echo "=== PROCESSES ==="
        ps -A | grep -E "com\.cloneapp\.lite|com\.whatsapp" || true
        echo "=== NOTIFICATION ==="
        dumpsys notification --noredact
        echo "=== FAILURE_SCAN ==="
        logcat -d -v time | grep -E "ANR in com\.cloneapp\.lite|Timeout receiver|App Died|SecurityException|ForegroundServiceStartNotAllowed|MissingForegroundServiceType" || true
        echo "=== END ==="
    } > "$OUT" 2>&1
) </dev/null >/dev/null 2>&1 &
echo $!
'@

    $collectorScript = $collectorScript.Replace("__OUT__", $RemotePath)
    $collectorScript = $collectorScript.Replace("__DELAY__", [string]$DelaySeconds)

    $result = Invoke-Adb -Arguments @("shell", "sh", "-c", $collectorScript)

    Write-Evidence "[$(Get-IsoTimestamp)] on-device collector scheduled"
    Write-Evidence "remote_path=$RemotePath"
    Write-Evidence "collector_delay_seconds=$DelaySeconds"
    Write-Evidence "collector_start_exit=$($result.ExitCode)"
    Write-Evidence "collector_start_output=$($result.Output)"

    return $result
}

function Get-SectionText {
    param(
        [string]$Text,
        [string]$StartMarker,
        [string]$EndMarker
    )

    $start = $Text.IndexOf($StartMarker)
    if ($start -lt 0) {
        return ""
    }

    $start += $StartMarker.Length
    $end = $Text.IndexOf($EndMarker, $start)

    if ($end -lt 0) {
        $end = $Text.Length
    }

    return $Text.Substring($start, $end - $start).Trim()
}

function Parse-NotificationText {
    param(
        [string]$Text,
        [string]$Label
    )

    $lines = @($Text -split '\r?\n')
    $packageText = "pkg=$Package"
    $records = @()

    for ($i = 0; $i -lt $lines.Count; $i++) {
        if (($lines[$i] -notlike "*NotificationRecord(*") -or ($lines[$i] -notlike "*$packageText*")) {
            continue
        }

        $end = [Math]::Min($lines.Count - 1, $i + 60)
        $blockLines = @($lines[$i..$end])
        $blockText = $blockLines -join [Environment]::NewLine

        $user = "<absent>"
        $channel = "<absent>"
        $subText = "<absent>"
        [Int64]$whenValue = 0

        $mUser = [regex]::Match($blockText, 'user=UserHandle\{([^}]+)\}')
        if ($mUser.Success) {
            $user = $mUser.Groups[1].Value
        }

        $mChannel = [regex]::Match($blockText, 'Notification\(channel=([^\s\)]+)')
        if ($mChannel.Success) {
            $channel = $mChannel.Groups[1].Value
        }

        $mSub = [regex]::Match($blockText, '(?im)^\s*(?:android\.)?subText\s*=\s*(.+)
    while ($true) {
        $answer = (Read-Host "Did you hear the User$VirtualUser notification sound while the phone stayed locked? [Y/N/U=unknown]").Trim().ToUpperInvariant()

        if (($answer -eq "Y") -or ($answer -eq "N") -or ($answer -eq "U")) {
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
    $safeBuild = $BuildLabel -replace '[^A-Za-z0-9._-]', '_'
    $script:EvidencePath = Join-Path $OutputDir ("locked-notification-{0}-{1}-run{2:D2}-{3}.txt" -f $safeBuild, $Mode, $RunNumber, $stamp)

    Write-Evidence "CloneApp Lite locked-notification gate"
    Write-Evidence "schema=3"
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
        "shell",
        "run-as",
        $Package,
        "cat",
        "shared_prefs/AppSharedPreferenceDelegate.xml"
    )

    if (($daemon.ExitCode -ne 0) -or ($daemon.Output -notmatch '<boolean\s+name="mDaemonEnable"\s+value="false"\s*/>')) {
        return (Finish-Run -Status "INCONCLUSIVE" -Reason "mDaemonEnable=false was not verified")
    }

    if ($Mode -eq "COLD") {
        Write-Host "COLD setup: force-stopping $Package. Do NOT open CloneApp or WhatsApp."
        Invoke-AdbCapture -Label "force-stop CloneApp" -Arguments @("shell", "am", "force-stop", $Package) | Out-Null
        Start-Sleep -Seconds 2
    }
    else {
        [void](Read-Host "WARM setup: unlock the Travel Phone, open CloneApp > User$VirtualUser WhatsApp once, return to Home, then press Enter here")
    }

    $before = Get-ProcessSnapshot -Label "PROCESS STATE BEFORE LOCK"

    if ($before.ExitCode -ne 0) {
        return (Finish-Run -Status "INCONCLUSIVE" -Reason "could not capture pre-lock process state")
    }

    if (($Mode -eq "COLD") -and ($before.HostCount -ne 0)) {
        return (Finish-Run -Status "INCONCLUSIVE" -Reason "COLD setup failed: CloneApp process still present after force-stop")
    }

    if ($Mode -eq "WARM") {
        $warmGuestCount = $before.GuestPxCount + $before.VirtualGuestCount

        if (($before.HostCount -eq 0) -or ($warmGuestCount -eq 0)) {
            return (Finish-Run -Status "INCONCLUSIVE" -Reason "WARM setup not verified: CloneApp or virtual guest process missing before lock")
        }
    }

    $preNotif = Get-NotificationSnapshot -Label "NOTIFICATION STATE BEFORE LOCK"

    Write-Section "LOG WINDOW"
    Invoke-AdbCapture -Label "clear logcat before lock" -Arguments @("logcat", "-c") | Out-Null

    Write-Section "LOCK + DEEP IDLE"
    Write-Evidence "[$(Get-IsoTimestamp)] sending KEYCODE_SLEEP"
    Invoke-AdbCapture -Label "lock phone" -Arguments @("shell", "input", "keyevent", "223") | Out-Null
    Start-Sleep -Seconds 1

    Write-Evidence "[$(Get-IsoTimestamp)] force-idle begin"
    $forceIdle = Invoke-AdbCapture -Label "deviceidle force-idle" -Arguments @("shell", "dumpsys", "deviceidle", "force-idle")
    $idleAfterForce = Get-IdleState -Label "after force-idle"

    if (($forceIdle.ExitCode -ne 0) -or ($idleAfterForce.ExitCode -ne 0) -or ($idleAfterForce.State -ne "IDLE")) {
        return (Finish-Run -Status "INCONCLUSIVE" -Reason "deep IDLE was not confirmed immediately after force-idle")
    }

    $remoteCapture = "/data/local/tmp/cloneapp_locked_notification_" + $stamp + "_run" + $RunNumber + ".txt"
    $collectorDelaySeconds = ($WaitMinutes * 60) + $PostSendWaitSeconds + 5
    $collector = Start-OnDeviceCollector -RemotePath $remoteCapture -DelaySeconds $collectorDelaySeconds

    if ($collector.ExitCode -ne 0) {
        return (Finish-Run -Status "INCONCLUSIVE" -Reason "on-device evidence collector could not be scheduled")
    }

    Write-Evidence "[$(Get-IsoTimestamp)] wait_start minutes=$WaitMinutes"
    Write-Host "Deep IDLE confirmed. Waiting $WaitMinutes minute(s) with the phone locked..."
    Write-Host "Wireless ADB may go offline during this wait; that is expected on this OnePlus."

    for ($minute = 1; $minute -le $WaitMinutes; $minute++) {
        Start-Sleep -Seconds 60
        Write-Host ("  waited {0}/{1} minute(s)" -f $minute, $WaitMinutes)
    }

    Write-Evidence "[$(Get-IsoTimestamp)] wait_complete_desktop_timer"

    [void](Read-Host "WAIT COMPLETE. Send ONE normal WhatsApp text to User$VirtualUser now from the other phone. Press Enter here immediately after sending")
    Write-Evidence "[$(Get-IsoTimestamp)] operator reports message sent"

    Write-Host "Keeping the Travel Phone locked for $PostSendWaitSeconds seconds..."
    Start-Sleep -Seconds $PostSendWaitSeconds

    $sound = Read-SoundEvidence

    if ($sound -eq "Y") {
        $soundText = "YES"
    }
    elseif ($sound -eq "N") {
        $soundText = "NO"
    }
    else {
        $soundText = "UNKNOWN"
    }

    Write-Section "OPERATOR SOUND EVIDENCE"
    Write-Evidence "timestamp=$(Get-IsoTimestamp)"
    Write-Evidence "notification_sound=$soundText"

    Write-Host "The on-device collector has captured the locked-state evidence."
    [void](Read-Host "Now unlock the Travel Phone so wireless ADB can return, then press Enter here")

    $reconnected = Reconnect-WirelessAdb
    if (-not $reconnected) {
        return (Finish-Run -Status "INCONCLUSIVE" -Reason "wireless ADB did not reconnect after unlock; on-device capture remains at $remoteCapture")
    }

    $remoteResult = Invoke-Adb -Arguments @("shell", "cat", $remoteCapture)
    if (($remoteResult.ExitCode -ne 0) -or [string]::IsNullOrWhiteSpace($remoteResult.Output)) {
        return (Finish-Run -Status "INCONCLUSIVE" -Reason "on-device evidence file could not be read after reconnect")
    }

    Write-Section "ON-DEVICE LOCKED-STATE CAPTURE"
    Write-Evidence "remote_path=$remoteCapture"
    Write-Evidence $remoteResult.Output

    $remoteIdleText = Get-SectionText -Text $remoteResult.Output -StartMarker "=== IDLE ===" -EndMarker "=== PROCESSES ==="
    $remoteProcessText = Get-SectionText -Text $remoteResult.Output -StartMarker "=== PROCESSES ===" -EndMarker "=== NOTIFICATION ==="
    $remoteNotificationText = Get-SectionText -Text $remoteResult.Output -StartMarker "=== NOTIFICATION ===" -EndMarker "=== FAILURE_SCAN ==="
    $remoteFailureText = Get-SectionText -Text $remoteResult.Output -StartMarker "=== FAILURE_SCAN ===" -EndMarker "=== END ==="

    Write-Section "PROCESS STATE AFTER MESSAGE (LOCKED ON-DEVICE CAPTURE)"
    Write-Evidence $remoteProcessText

    $postNotif = Parse-NotificationText -Text $remoteNotificationText -Label "NOTIFICATION STATE AFTER MESSAGE (LOCKED ON-DEVICE CAPTURE)"

    Write-Section "FAILURE WINDOW SCAN (LOCKED ON-DEVICE CAPTURE)"
    if ([string]::IsNullOrWhiteSpace($remoteFailureText)) {
        Write-Evidence "<no matches>"
        $failureHitCount = 0
    }
    else {
        Write-Evidence $remoteFailureText
        $failureHitCount = @($remoteFailureText -split '\r?\n' | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }).Count
    }

    $idleAfterMessageState = $remoteIdleText.Trim()
    Write-Evidence "locked_capture_idle_state=$idleAfterMessageState"

    Invoke-Adb -Arguments @("shell", "rm", "-f", $remoteCapture) | Out-Null

    $freshRecord = $false

    if ($postNotif.Count -gt 0) {
        if ($preNotif.Count -eq 0) {
            $freshRecord = $true
        }
        elseif ($postNotif.MaxWhen -gt $preNotif.MaxWhen) {
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
    Write-Evidence "pre_virtual_guest_host_uid_process_count=$($before.VirtualGuestCount)"
    Write-Evidence "pre_whatsapp_other_uid_process_count=$($before.WhatsappOtherUidCount)"
    Write-Evidence "post_process_state_source=on-device locked-state capture"
    Write-Evidence "idle_after_force=$($idleAfterForce.State)"
    Write-Evidence "idle_before_message=not queried over wireless adb; desktop timer used while device remained locked"
    Write-Evidence "idle_after_message=$idleAfterMessageState"
    Write-Evidence "failure_scan_hit_count=$failureHitCount"

    if (($sound -eq "Y") -and $freshRecord) {
        return (Finish-Run -Status "PASS" -Reason "sound heard and fresh/updated CloneApp NotificationRecord captured")
    }

    if (($sound -eq "N") -and (-not $freshRecord) -and ($postNotif.Count -eq 0)) {
        return (Finish-Run -Status "FAIL" -Reason "no sound and no CloneApp NotificationRecord after message")
    }

    return (Finish-Run -Status "INCONCLUSIVE" -Reason "sound and NotificationRecord evidence did not agree cleanly")
}

$allSummaries = @()

for ($run = 1; $run -le $Runs; $run++) {
    Write-Host ""
    Write-Host ("===== CloneApp Lite locked-notification gate: run {0}/{1} =====" -f $run, $Runs)

    $summary = Invoke-GateRun -RunNumber $run
    $allSummaries += $summary

    if ($run -lt $Runs) {
        [void](Read-Host "Run $run complete. Press Enter when ready to set up the next run")
    }
}

Write-Host ""
Write-Host "===== Completed $Runs run(s) ====="

foreach ($summary in $allSummaries) {
    Write-Host $summary
}
)
        if (-not $mSub.Success) {
            $mSub = [regex]::Match($blockText, '(?i)subText=([^\r\n\)]+)')
        }
        if ($mSub.Success) {
            $subText = $mSub.Groups[1].Value.Trim()
        }

        $mWhen = [regex]::Match($blockText, '(?m)^\s*when=(\d+)')
        if ($mWhen.Success) {
            [void][Int64]::TryParse($mWhen.Groups[1].Value, [ref]$whenValue)
        }

        $records += [pscustomobject]@{
            User = $user
            Channel = $channel
            SubText = $subText
            When = $whenValue
            Block = $blockText
        }
    }

    Write-Section $Label
    Write-Evidence "timestamp=$(Get-IsoTimestamp)"
    Write-Evidence "notification_record_count=$($records.Count)"

    if ($records.Count -eq 0) {
        Write-Evidence "<no NotificationRecord for pkg=$Package>"
    }
    else {
        $recordNumber = 0
        foreach ($record in $records) {
            $recordNumber++
            Write-Evidence "record[$recordNumber].pkg=$Package"
            Write-Evidence "record[$recordNumber].user=$($record.User)"
            Write-Evidence "record[$recordNumber].subText=$($record.SubText)"
            Write-Evidence "record[$recordNumber].channel=$($record.Channel)"
            Write-Evidence "record[$recordNumber].when=$($record.When)"
            Write-Evidence $record.Block
            Write-Evidence "---"
        }
    }

    [Int64]$maxWhen = 0
    foreach ($record in $records) {
        if ($record.When -gt $maxWhen) {
            $maxWhen = $record.When
        }
    }

    return [pscustomobject]@{
        Count = $records.Count
        MaxWhen = $maxWhen
    }
}

function Read-SoundEvidence {
    while ($true) {
        $answer = (Read-Host "Did you hear the User$VirtualUser notification sound while the phone stayed locked? [Y/N/U=unknown]").Trim().ToUpperInvariant()

        if (($answer -eq "Y") -or ($answer -eq "N") -or ($answer -eq "U")) {
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
    $safeBuild = $BuildLabel -replace '[^A-Za-z0-9._-]', '_'
    $script:EvidencePath = Join-Path $OutputDir ("locked-notification-{0}-{1}-run{2:D2}-{3}.txt" -f $safeBuild, $Mode, $RunNumber, $stamp)

    Write-Evidence "CloneApp Lite locked-notification gate"
    Write-Evidence "schema=2"
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
        "shell",
        "run-as",
        $Package,
        "cat",
        "shared_prefs/AppSharedPreferenceDelegate.xml"
    )

    if (($daemon.ExitCode -ne 0) -or ($daemon.Output -notmatch '<boolean\s+name="mDaemonEnable"\s+value="false"\s*/>')) {
        return (Finish-Run -Status "INCONCLUSIVE" -Reason "mDaemonEnable=false was not verified")
    }

    if ($Mode -eq "COLD") {
        Write-Host "COLD setup: force-stopping $Package. Do NOT open CloneApp or WhatsApp."
        Invoke-AdbCapture -Label "force-stop CloneApp" -Arguments @("shell", "am", "force-stop", $Package) | Out-Null
        Start-Sleep -Seconds 2
    }
    else {
        [void](Read-Host "WARM setup: unlock the Travel Phone, open CloneApp > User$VirtualUser WhatsApp once, return to Home, then press Enter here")
    }

    $before = Get-ProcessSnapshot -Label "PROCESS STATE BEFORE LOCK"

    if ($before.ExitCode -ne 0) {
        return (Finish-Run -Status "INCONCLUSIVE" -Reason "could not capture pre-lock process state")
    }

    if (($Mode -eq "COLD") -and ($before.HostCount -ne 0)) {
        return (Finish-Run -Status "INCONCLUSIVE" -Reason "COLD setup failed: CloneApp process still present after force-stop")
    }

    if ($Mode -eq "WARM") {
        $warmGuestCount = $before.GuestPxCount + $before.VirtualGuestCount

        if (($before.HostCount -eq 0) -or ($warmGuestCount -eq 0)) {
            return (Finish-Run -Status "INCONCLUSIVE" -Reason "WARM setup not verified: CloneApp or virtual guest process missing before lock")
        }
    }

    $preNotif = Get-NotificationSnapshot -Label "NOTIFICATION STATE BEFORE LOCK"

    Write-Section "LOG WINDOW"
    Invoke-AdbCapture -Label "clear logcat before lock" -Arguments @("logcat", "-c") | Out-Null

    Write-Section "LOCK + DEEP IDLE"
    Write-Evidence "[$(Get-IsoTimestamp)] sending KEYCODE_SLEEP"
    Invoke-AdbCapture -Label "lock phone" -Arguments @("shell", "input", "keyevent", "223") | Out-Null
    Start-Sleep -Seconds 1

    Write-Evidence "[$(Get-IsoTimestamp)] force-idle begin"
    $forceIdle = Invoke-AdbCapture -Label "deviceidle force-idle" -Arguments @("shell", "dumpsys", "deviceidle", "force-idle")
    $idleAfterForce = Get-IdleState -Label "after force-idle"

    if (($forceIdle.ExitCode -ne 0) -or ($idleAfterForce.ExitCode -ne 0) -or ($idleAfterForce.State -ne "IDLE")) {
        return (Finish-Run -Status "INCONCLUSIVE" -Reason "deep IDLE was not confirmed immediately after force-idle")
    }

    Write-Evidence "[$(Get-IsoTimestamp)] wait_start minutes=$WaitMinutes"
    Write-Host "Deep IDLE confirmed. Waiting $WaitMinutes minute(s) with the phone locked..."

    for ($minute = 1; $minute -le $WaitMinutes; $minute++) {
        Start-Sleep -Seconds 60
        Write-Host ("  waited {0}/{1} minute(s)" -f $minute, $WaitMinutes)
    }

    Write-Evidence "[$(Get-IsoTimestamp)] wait_complete"
    $idleBeforeMessage = Get-IdleState -Label "after wait / before message"

    if (($idleBeforeMessage.ExitCode -ne 0) -or ($idleBeforeMessage.State -ne "IDLE")) {
        return (Finish-Run -Status "INCONCLUSIVE" -Reason "device was not in deep IDLE at the end of the wait")
    }

    [void](Read-Host "WAIT COMPLETE. Send ONE normal WhatsApp text to User$VirtualUser now from the other phone. Press Enter here immediately after sending")
    Write-Evidence "[$(Get-IsoTimestamp)] operator reports message sent"

    Write-Host "Keeping the Travel Phone locked for $PostSendWaitSeconds seconds..."
    Start-Sleep -Seconds $PostSendWaitSeconds

    $sound = Read-SoundEvidence

    if ($sound -eq "Y") {
        $soundText = "YES"
    }
    elseif ($sound -eq "N") {
        $soundText = "NO"
    }
    else {
        $soundText = "UNKNOWN"
    }

    Write-Section "OPERATOR SOUND EVIDENCE"
    Write-Evidence "timestamp=$(Get-IsoTimestamp)"
    Write-Evidence "notification_sound=$soundText"

    $postNotif = Get-NotificationSnapshot -Label "NOTIFICATION STATE AFTER MESSAGE"
    $after = Get-ProcessSnapshot -Label "PROCESS STATE AFTER MESSAGE"
    $idleAfterMessage = Get-IdleState -Label "after message"
    $failureHits = @(Get-FailureScan)

    $freshRecord = $false

    if ($postNotif.Count -gt 0) {
        if ($preNotif.Count -eq 0) {
            $freshRecord = $true
        }
        elseif ($postNotif.MaxWhen -gt $preNotif.MaxWhen) {
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
    Write-Evidence "pre_virtual_guest_host_uid_process_count=$($before.VirtualGuestCount)"
    Write-Evidence "pre_whatsapp_other_uid_process_count=$($before.WhatsappOtherUidCount)"
    Write-Evidence "post_cloneapp_process_count=$($after.HostCount)"
    Write-Evidence "post_guest_px_process_count=$($after.GuestPxCount)"
    Write-Evidence "post_virtual_guest_host_uid_process_count=$($after.VirtualGuestCount)"
    Write-Evidence "post_whatsapp_other_uid_process_count=$($after.WhatsappOtherUidCount)"
    Write-Evidence "idle_after_force=$($idleAfterForce.State)"
    Write-Evidence "idle_before_message=$($idleBeforeMessage.State)"
    Write-Evidence "idle_after_message=$($idleAfterMessage.State)"
    Write-Evidence "failure_scan_hit_count=$($failureHits.Count)"

    if (($sound -eq "Y") -and $freshRecord) {
        return (Finish-Run -Status "PASS" -Reason "sound heard and fresh/updated CloneApp NotificationRecord captured")
    }

    if (($sound -eq "N") -and (-not $freshRecord) -and ($postNotif.Count -eq 0)) {
        return (Finish-Run -Status "FAIL" -Reason "no sound and no CloneApp NotificationRecord after message")
    }

    return (Finish-Run -Status "INCONCLUSIVE" -Reason "sound and NotificationRecord evidence did not agree cleanly")
}

$allSummaries = @()

for ($run = 1; $run -le $Runs; $run++) {
    Write-Host ""
    Write-Host ("===== CloneApp Lite locked-notification gate: run {0}/{1} =====" -f $run, $Runs)

    $summary = Invoke-GateRun -RunNumber $run
    $allSummaries += $summary

    if ($run -lt $Runs) {
        [void](Read-Host "Run $run complete. Press Enter when ready to set up the next run")
    }
}

Write-Host ""
Write-Host "===== Completed $Runs run(s) ====="

foreach ($summary in $allSummaries) {
    Write-Host $summary
}
