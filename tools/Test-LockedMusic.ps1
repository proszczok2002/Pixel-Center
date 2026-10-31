param(
    [string]$Adb = $(if ($env:ANDROID_HOME) {
        Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe'
    } else {
        'adb'
    }),
    [int]$Cycles = 3
)
$ErrorActionPreference = 'Stop'

function Read-MusicState {
    $dump = (& $Adb shell dumpsys media_session) -join "`n"
    $block = [regex]::Match($dump,
        '(?ms)^    YouTube playerlib com\.google\.android\.apps\.youtube\.music/.*?(?=^    \S|\z)').Value
    if (-not $block) { throw 'YouTube Music session is missing' }
    [pscustomobject]@{
        Playing = $block -match 'state=PLAYING\(3\)'
        Paused = $block -match 'state=PAUSED\(2\)'
        QueueItem = [regex]::Match($block, 'active item id=(-?\d+)').Groups[1].Value
        Description = [regex]::Match($block, 'metadata:.*description=(.*)').Groups[1].Value
    }
}

function Assert-Asleep {
    $power = (& $Adb shell dumpsys power) -join "`n"
    if ($power -notmatch 'mWakefulness=(Dozing|Asleep)') {
        throw 'Screen woke during test; this does not verify the locked-screen regression'
    }
}

function Assert-VolumePriority {
    $dump = (& $Adb shell dumpsys media_session) -join "`n"
    if ($dump -notmatch 'Sessions Stack[^\n]*\n\s+PixelCenterScreenOffVolume') {
        throw 'Pixel Center did not recover volume key priority'
    }
    $token = [regex]::Match($dump, 'PixelCenterScreenOffVolume/(\d+)').Groups[1].Value
    if ($script:SessionToken -and $script:SessionToken -ne $token) {
        throw 'Pixel Center replaced the session while the screen stayed off'
    }
    $script:SessionToken = $token
}

$script:SessionToken = $null
$initial = Read-MusicState
$appPid = (& $Adb shell pidof pl.proszczok.pixelspeecheq).Trim()
if (-not $appPid) { throw 'Pixel Center is not running' }
$logStart = (& $Adb shell "date '+%m-%d %H:%M:%S.000'").Trim()
& $Adb shell input keyevent KEYCODE_SLEEP
Start-Sleep -Milliseconds 700
try {
    for ($cycle = 1; $cycle -le $Cycles; $cycle++) {
        & $Adb shell cmd media_session dispatch pause
        Start-Sleep -Milliseconds 300
        & $Adb shell cmd media_session dispatch play
        Start-Sleep -Milliseconds 1800
        Assert-Asleep
        Assert-VolumePriority
        $before = Read-MusicState
        if (-not $before.Playing) { throw 'External resume did not start YouTube Music' }

        & $Adb shell input keyevent --duration 900 KEYCODE_VOLUME_UP
        Start-Sleep -Milliseconds 1600
        $next = Read-MusicState
        if ($next.QueueItem -eq $before.QueueItem) { throw 'Next did not change the queue item' }

        & $Adb shell input keyevent --duration 900 KEYCODE_VOLUME_DOWN
        Start-Sleep -Milliseconds 1600
        $previous = Read-MusicState
        if ($previous.QueueItem -ne $before.QueueItem) { throw 'Previous did not return to the prior queue item' }

        & $Adb shell input keycombination -t 900 24 25
        Start-Sleep -Milliseconds 600
        if (-not (Read-MusicState).Paused) { throw 'Both buttons did not pause YouTube Music' }

        & $Adb shell input keycombination -t 900 24 25
        Start-Sleep -Milliseconds 1600
        if (-not (Read-MusicState).Playing) { throw 'Both buttons did not resume YouTube Music' }
        Assert-Asleep
        Assert-VolumePriority
        Write-Output "Cycle ${cycle}: next, previous, pause, resume passed; session=$script:SessionToken"
    }
    $logs = (& $Adb logcat -d --pid=$appPid -T $logStart -s PixelCenterMedia:D PixelCenterVolume:D) -join "`n"
    $commands = [regex]::Matches($logs, 'Direct command (85|87|88) -> com\.google\.android\.apps\.youtube\.music').Count
    if ($commands -ne ($Cycles * 4)) { throw "Expected $($Cycles * 4) commands; got $commands" }
    if ($logs -match 'Direct command .* -> com\.audioteka') { throw 'A command went to the paused audiobook player' }
    Write-Output "PASS: $commands commands, all to YouTube Music, no unlock and no session replacement."
} finally {
    if (-not $initial.Playing) { & $Adb shell cmd media_session dispatch pause }
}
