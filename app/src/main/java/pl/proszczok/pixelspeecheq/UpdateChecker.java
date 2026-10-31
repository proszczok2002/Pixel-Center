package pl.proszczok.pixelspeecheq;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class UpdateChecker {
    private static final String MANIFEST_URL =
            "https://raw.githubusercontent.com/proszczok2002/Pixel-Center/main/server/pixel-center-update.json";
    private static final String EXPECTED_APP_ID = "pl.proszczok.pixelspeecheq";
    private static final String EXPECTED_APK_HOST = "github.com";
    private static final String EXPECTED_APK_PATH_PREFIX =
            "/proszczok2002/Pixel-Center/releases/download/";
    private static final int TIMEOUT_MS = 8000;

    interface Callback {
        void onResult(UpdateInfo info);
        void onError(Exception error);
    }

    static final class UpdateInfo {
        final long versionCode;
        final String versionName;
        final String apkUrl;
        final String changelog;
        final String sha256;

        UpdateInfo(long versionCode, String versionName, String apkUrl,
                String changelog, String sha256) {
            this.versionCode = versionCode;
            this.versionName = versionName;
            this.apkUrl = apkUrl;
            this.changelog = changelog;
            this.sha256 = sha256;
        }
    }

    private UpdateChecker() { }

    static void check(Context context, Callback callback) {
        Handler main = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL manifestUrl = new URL(MANIFEST_URL + "?t="
                        + System.currentTimeMillis());
                connection = (HttpURLConnection) manifestUrl.openConnection();
                connection.setConnectTimeout(TIMEOUT_MS);
                connection.setReadTimeout(TIMEOUT_MS);
                connection.setUseCaches(false);
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Cache-Control", "no-cache");
                connection.setRequestProperty("Pragma", "no-cache");
                connection.setRequestProperty("User-Agent", "Pixel-Center/" + installedVersionName(context));
                int status = connection.getResponseCode();
                if (status < 200 || status >= 300) {
                    throw new IllegalStateException("HTTP " + status);
                }
                String json = readBody(connection.getInputStream());
                UpdateInfo info = parse(json);
                main.post(() -> callback.onResult(info));
            } catch (Exception error) {
                main.post(() -> callback.onError(error));
            } finally {
                if (connection != null) connection.disconnect();
            }
        }, "pixel-center-update-check").start();
    }

    static long installedVersionCode(Context context) {
        try {
            PackageInfo info = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return info.getLongVersionCode();
        } catch (PackageManager.NameNotFoundException ignored) {
            return 0;
        }
    }

    private static String installedVersionName(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException ignored) {
            return "unknown";
        }
    }

    private static UpdateInfo parse(String rawJson) throws Exception {
        JSONObject json = new JSONObject(rawJson);
        if (!EXPECTED_APP_ID.equals(json.getString("appId"))) {
            throw new IllegalArgumentException("Nieprawidlowy identyfikator aplikacji");
        }
        long versionCode = json.getLong("versionCode");
        String versionName = json.getString("versionName").trim();
        String apkUrl = json.getString("apkUrl").trim();
        String changelog = json.optString("changelog", "").trim();
        String sha256 = json.getString("sha256").trim().toUpperCase();
        Uri uri = Uri.parse(apkUrl);
        String path = uri.getPath();
        if (versionCode <= 0 || versionName.isEmpty()
                || !"https".equalsIgnoreCase(uri.getScheme())
                || !EXPECTED_APK_HOST.equalsIgnoreCase(uri.getHost())
                || path == null || !path.startsWith(EXPECTED_APK_PATH_PREFIX)
                || !sha256.matches("[0-9A-F]{64}")) {
            throw new IllegalArgumentException("Nieprawidlowy manifest aktualizacji");
        }
        return new UpdateInfo(versionCode, versionName, apkUrl, changelog, sha256);
    }

    private static String readBody(InputStream input) throws Exception {
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (result.length() + line.length() > 65536) {
                    throw new IllegalArgumentException("Manifest aktualizacji jest za duzy");
                }
                result.append(line);
            }
        }
        return result.toString();
    }
}
