package pl.proszczok.pixelspeecheq;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Locale;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

final class UpdateInstaller {
    static final int REQUEST_UNKNOWN_SOURCES = 4107;
    private static final int TIMEOUT_MS = 15000;
    private static final long MAX_APK_BYTES = 150L * 1024L * 1024L;
    private static final String APK_MIME = "application/vnd.android.package-archive";

    private UpdateInstaller() { }

    static void downloadAndInstall(Activity activity, UpdateChecker.UpdateInfo info) {
        int padding = Math.round(24 * activity.getResources().getDisplayMetrics().density);
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(padding, padding / 2, padding, 0);
        TextView state = new TextView(activity);
        state.setText("Przygotowywanie pobierania…");
        state.setTextSize(16);
        ProgressBar progress = new ProgressBar(
                activity, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true);
        panel.addView(state, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        progressParams.topMargin = padding / 2;
        panel.addView(progress, progressParams);

        AtomicBoolean canceled = new AtomicBoolean(false);
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("Aktualizacja Pixel Center " + info.versionName)
                .setView(panel)
                .setNegativeButton("Anuluj", (ignored, which) -> canceled.set(true))
                .setCancelable(false)
                .create();
        dialog.show();

        Handler main = new Handler(Looper.getMainLooper());
        new Thread(() -> download(activity, info, canceled, main, dialog, state, progress),
                "pixel-center-update-download").start();
    }

    static void onUnknownSourcesResult(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O
                || activity.getPackageManager().canRequestPackageInstalls()) {
            launchInstaller(activity, UpdateFileProvider.updateFile(activity));
        } else {
            Toast.makeText(activity,
                    "Zezwól Pixel Center na instalowanie aplikacji i spróbuj ponownie.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private static void download(Activity activity, UpdateChecker.UpdateInfo info,
            AtomicBoolean canceled, Handler main, AlertDialog dialog,
            TextView state, ProgressBar progress) {
        File target = UpdateFileProvider.updateFile(activity);
        File part = new File(target.getParentFile(), "update.apk.part");
        HttpURLConnection connection = null;
        try {
            File parent = target.getParentFile();
            if ((!parent.isDirectory() && !parent.mkdirs()) || !parent.isDirectory()) {
                throw new IllegalStateException("Nie można utworzyć katalogu aktualizacji");
            }
            if (part.exists() && !part.delete()) {
                throw new IllegalStateException("Nie można usunąć starego pobierania");
            }
            connection = (HttpURLConnection) new URL(info.apkUrl).openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setUseCaches(false);
            connection.setRequestProperty("Accept", APK_MIME);
            connection.setRequestProperty("User-Agent", "Pixel-Center-Updater/"
                    + UpdateChecker.installedVersionCode(activity));
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("Serwer zwrócił HTTP " + status);
            }
            long expectedLength = connection.getContentLengthLong();
            if (expectedLength > MAX_APK_BYTES) {
                throw new IllegalStateException("Plik aktualizacji jest za duży");
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long downloaded = 0;
            int lastPercent = -1;
            byte[] buffer = new byte[64 * 1024];
            try (BufferedInputStream input = new BufferedInputStream(connection.getInputStream());
                    FileOutputStream output = new FileOutputStream(part)) {
                int count;
                while ((count = input.read(buffer)) != -1) {
                    if (canceled.get()) throw new InterruptedException("Pobieranie anulowane");
                    downloaded += count;
                    if (downloaded > MAX_APK_BYTES) {
                        throw new IllegalStateException("Plik aktualizacji jest za duży");
                    }
                    output.write(buffer, 0, count);
                    digest.update(buffer, 0, count);
                    if (expectedLength > 0) {
                        int percent = (int) Math.min(100, downloaded * 100 / expectedLength);
                        if (percent != lastPercent) {
                            lastPercent = percent;
                            int shown = percent;
                            main.post(() -> {
                                progress.setIndeterminate(false);
                                progress.setProgress(shown);
                                state.setText("Pobieranie: " + shown + "%");
                            });
                        }
                    }
                }
            }
            String actualHash = hex(digest.digest());
            if (!actualHash.equalsIgnoreCase(info.sha256)) {
                throw new SecurityException("Suma SHA-256 pobranego APK jest nieprawidłowa");
            }
            verifyApk(activity, part, info);
            if (target.exists() && !target.delete()) {
                throw new IllegalStateException("Nie można zastąpić starej aktualizacji");
            }
            if (!part.renameTo(target)) {
                throw new IllegalStateException("Nie można przygotować pobranego APK");
            }
            main.post(() -> {
                if (!activity.isFinishing() && !activity.isDestroyed()) {
                    dialog.dismiss();
                    requestInstall(activity, target);
                }
            });
        } catch (InterruptedException canceledDownload) {
            if (part.exists()) part.delete();
            main.post(dialog::dismiss);
        } catch (Exception error) {
            if (part.exists()) part.delete();
            main.post(() -> {
                dialog.dismiss();
                if (!activity.isFinishing() && !activity.isDestroyed()) {
                    new AlertDialog.Builder(activity)
                            .setTitle("Nie udało się pobrać aktualizacji")
                            .setMessage(error.getMessage() == null
                                    ? "Spróbuj ponownie później." : error.getMessage())
                            .setPositiveButton("OK", null)
                            .show();
                }
            });
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static void verifyApk(Activity activity, File apk,
            UpdateChecker.UpdateInfo info) throws Exception {
        PackageManager manager = activity.getPackageManager();
        int flags = PackageManager.GET_SIGNING_CERTIFICATES;
        PackageInfo archive = Build.VERSION.SDK_INT >= 33
                ? manager.getPackageArchiveInfo(apk.getAbsolutePath(),
                PackageManager.PackageInfoFlags.of(flags))
                : manager.getPackageArchiveInfo(apk.getAbsolutePath(), flags);
        PackageInfo installed = Build.VERSION.SDK_INT >= 33
                ? manager.getPackageInfo(activity.getPackageName(),
                PackageManager.PackageInfoFlags.of(flags))
                : manager.getPackageInfo(activity.getPackageName(), flags);
        if (archive == null || !activity.getPackageName().equals(archive.packageName)
                || archive.getLongVersionCode() != info.versionCode
                || !sameSigners(installed, archive)) {
            throw new SecurityException("APK nie jest prawidłową aktualizacją Pixel Center");
        }
    }

    private static boolean sameSigners(PackageInfo first, PackageInfo second)
            throws Exception {
        return signerHashes(first).equals(signerHashes(second));
    }

    private static Set<String> signerHashes(PackageInfo info) throws Exception {
        Signature[] signatures = info.signingInfo == null ? new Signature[0]
                : (info.signingInfo.hasMultipleSigners()
                ? info.signingInfo.getApkContentsSigners()
                : info.signingInfo.getSigningCertificateHistory());
        Set<String> hashes = new HashSet<>();
        for (Signature signature : signatures) {
            hashes.add(hex(MessageDigest.getInstance("SHA-256")
                    .digest(signature.toByteArray())));
        }
        return hashes;
    }

    private static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) result.append(String.format(Locale.US, "%02X", value));
        return result.toString();
    }

    private static void requestInstall(Activity activity, File apk) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && !activity.getPackageManager().canRequestPackageInstalls()) {
            Intent permission = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + activity.getPackageName()));
            activity.startActivityForResult(permission, REQUEST_UNKNOWN_SOURCES);
            return;
        }
        launchInstaller(activity, apk);
    }

    private static void launchInstaller(Activity activity, File apk) {
        if (!apk.isFile()) {
            Toast.makeText(activity, "Pobrany plik aktualizacji już nie istnieje.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        Uri uri = new Uri.Builder()
                .scheme("content")
                .authority(activity.getPackageName() + ".updates")
                .appendPath(UpdateFileProvider.FILE_NAME)
                .build();
        Intent install = new Intent(Intent.ACTION_INSTALL_PACKAGE)
                .setDataAndType(uri, APK_MIME)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        install.setClipData(ClipData.newRawUri("Pixel Center update", uri));
        ComponentName systemInstaller = findSystemInstaller(activity, install);
        if (systemInstaller != null) install.setComponent(systemInstaller);
        activity.startActivity(install);
    }

    private static ComponentName findSystemInstaller(Activity activity, Intent install) {
        PackageManager manager = activity.getPackageManager();
        List<ResolveInfo> candidates = Build.VERSION.SDK_INT >= 33
                ? manager.queryIntentActivities(install,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY))
                : manager.queryIntentActivities(install, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo candidate : candidates) {
            if (candidate.activityInfo == null
                    || candidate.activityInfo.applicationInfo == null) continue;
            int flags = candidate.activityInfo.applicationInfo.flags;
            if ((flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                return new ComponentName(candidate.activityInfo.packageName,
                        candidate.activityInfo.name);
            }
        }
        return null;
    }
}
