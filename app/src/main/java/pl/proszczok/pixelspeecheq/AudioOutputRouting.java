package pl.proszczok.pixelspeecheq;

import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;

import java.util.List;

final class AudioOutputRouting {
    private static final AudioAttributes MEDIA_ATTRIBUTES =
            new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();

    private AudioOutputRouting() {
    }

    static boolean isBluetoothMediaOutput(AudioManager audioManager) {
        if (audioManager == null) return false;
        if (Build.VERSION.SDK_INT >= 33) {
            List<AudioDeviceInfo> devices =
                    audioManager.getAudioDevicesForAttributes(MEDIA_ATTRIBUTES);
            for (AudioDeviceInfo device : devices) {
                if (isBluetoothHeadphoneType(device.getType())) return true;
            }
            return false;
        }
        for (AudioDeviceInfo device
                : audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
            if (isBluetoothHeadphoneType(device.getType())) return true;
        }
        return false;
    }

    private static boolean isBluetoothHeadphoneType(int type) {
        if (type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                || type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                || type == AudioDeviceInfo.TYPE_HEARING_AID) {
            return true;
        }
        if (Build.VERSION.SDK_INT >= 31
                && type == AudioDeviceInfo.TYPE_BLE_HEADSET) {
            return true;
        }
        return Build.VERSION.SDK_INT >= 37
                && type == AudioDeviceInfo.TYPE_BLE_HEARING_AID;
    }
}
