param(
    [string[]]$Languages = @('pl','en','de','ru','es','fr','it'),
    [string]$Output = 'audit/2026-11-16/1.03',
    [string]$Adb = 'F:\pixel center\android-sdk\platform-tools\adb.exe'
)
$ErrorActionPreference='Continue'
$package='pl.proszczok.pixelspeecheq'
$driver='pl.proszczok.ytdownloader.auditdriver/.AuditInstrumentation'
$remote='/sdcard/Android/data/pl.proszczok.ytdownloader.auditdriver/files'
$labels=@{
    pl=@('Korektor','Noc','Przyciski','Ostatnie','Więcej')
    en=@('Equalizer','Night','Controls','Recents','More')
    de=@('Equalizer','Nacht','Tasten','Letzte Apps','Mehr')
    ru=@('Эквалайзер','Ночь','Кнопки','Недавние','Ещё')
    es=@('Ecualizador','Noche','Controles','Recientes','Más')
    fr=@('Égaliseur','Nuit','Boutons','Récentes','Plus')
    it=@('Equalizzatore','Notte','Pulsanti','Recenti','Altro')
}
New-Item -ItemType Directory -Force $Output | Out-Null
$originalLocales = (& $Adb shell cmd locale get-app-locales $package) -replace '^.*\[([^\]]*)\].*$', '$1'
function Get-AppNodes($node) {
    if ($node.package -eq $package) { $node }
    foreach($child in $node.children) { Get-AppNodes $child }
}
try {
foreach($language in $Languages) {
    & $Adb shell cmd locale set-app-locales $package --locales $language
    & $Adb shell am start -n "$package/.MainActivity" | Out-Null
    Start-Sleep -Milliseconds 700
    for($i=0;$i -lt 5;$i++) {
        $label=$labels[$language][$i]
        $result=& $Adb shell "am instrument -w -e action click -e text '$label' -e targetPackage $package -e wait 500 $driver"
        if($result -notmatch 'AUDIT_OK true') { throw "Failed $language / $label : $result" }
        & $Adb pull "$remote/result.json" "$Output/$language-tab$i.json" 2>&1 | Out-Null
        & $Adb pull "$remote/screen.png" "$Output/$language-tab$i.png" 2>&1 | Out-Null
        $snapshot = Get-Content -Raw -Encoding utf8 "$Output/$language-tab$i.json" | ConvertFrom-Json
        $nodes = @(foreach($window in $snapshot.windows) { Get-AppNodes $window.root })
        $tabs = @($nodes | Where-Object class -eq 'android.app.ActionBar$Tab')
        if ($tabs.Count -ne 5 -or @($tabs | Where-Object visible).Count -ne 5 -or @($tabs | Where-Object selected).Count -ne 1) {
            throw "Invalid tab semantics: $language / $label"
        }
        Write-Output "PASS $language / $label"
    }
}
} finally {
    if ([string]::IsNullOrWhiteSpace($originalLocales)) {
        & $Adb shell cmd locale set-app-locales $package
    } else {
        & $Adb shell cmd locale set-app-locales $package --locales $originalLocales
    }
}
