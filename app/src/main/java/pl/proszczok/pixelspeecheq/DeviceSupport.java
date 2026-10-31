package pl.proszczok.pixelspeecheq;

import android.os.Build;

import java.util.Locale;

final class DeviceSupport {
    private DeviceSupport() {
    }

    static boolean isSupportedDevice() {
        return isSupportedIdentity(
                Build.MANUFACTURER, Build.BRAND, Build.MODEL, isGoogleSdkEmulator());
    }

    static boolean isSupportedIdentity(
            String manufacturer, String brand, String model, boolean googleSdkEmulator) {
        boolean googleHardware = "google".equals(normalize(manufacturer))
                && "google".equals(normalize(brand));
        if (!googleHardware) {
            return false;
        }
        String normalizedModel = normalize(model);
        return normalizedModel.startsWith("pixel")
                || (googleSdkEmulator && normalizedModel.startsWith("sdk_gphone"));
    }

    private static boolean isGoogleSdkEmulator() {
        String fingerprint = normalize(Build.FINGERPRINT);
        String hardware = normalize(Build.HARDWARE);
        return fingerprint.startsWith("google/sdk_gphone")
                && (hardware.equals("ranchu") || hardware.equals("goldfish"));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
