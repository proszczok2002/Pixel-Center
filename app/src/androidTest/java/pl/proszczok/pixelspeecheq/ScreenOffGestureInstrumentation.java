package pl.proszczok.pixelspeecheq;

import android.app.Instrumentation;
import android.app.Activity;
import android.app.UiAutomation;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.content.ComponentName;
import android.media.audiofx.DynamicsProcessing;
import android.media.AudioManager;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import java.util.List;

public final class ScreenOffGestureInstrumentation extends Instrumentation {
    private Bundle arguments;

    @Override
    public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        this.arguments = arguments;
        start();
    }

    @Override
    public void onStart() {
        if (arguments != null && "true".equals(arguments.getString("inspectOverlay"))) {
            inspectOverlay();
            return;
        }
        if (arguments != null && "true".equals(arguments.getString("effectProbe"))) {
            probeDynamicsProcessing();
            return;
        }
        if (arguments != null && "true".equals(arguments.getString("echoRegression"))) {
            testDelayedSystemVolumeEcho();
            return;
        }
        if (arguments != null && "true".equals(arguments.getString("volumeUiProbe"))) {
            probeVolumeUi();
            return;
        }
        if (arguments != null && arguments.getString("restoreFinePercent") != null) {
            AudioManager manager = getTargetContext().getSystemService(AudioManager.class);
            manager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    Integer.parseInt(arguments.getString("restoreStreamVolume")),
                    0);
            PixelatePreferences.saveFineVolumeState(
                    getTargetContext(),
                    Integer.parseInt(arguments.getString("restoreFinePercent")),
                    Integer.parseInt(arguments.getString("restoreFinePhysical")));
            // SharedPreferences.apply() is asynchronous. Keep instrumentation
            // alive long enough for the exact pre-test state to reach disk.
            SystemClock.sleep(500);
            finish(Activity.RESULT_OK, new Bundle());
            return;
        }
        if (arguments != null && arguments.getString("restorePackage") != null) {
            restorePlaybackPosition();
            return;
        }
        String stepPercent = arguments == null
                ? null : arguments.getString("stepPercent");
        if (stepPercent != null) {
            PixelatePreferences.setAdvancedVolumeEnabled(getTargetContext(), true);
            PixelatePreferences.setVolumeStepPercent(
                    getTargetContext(), Integer.parseInt(stepPercent));
        }
        String finePercent = arguments == null
                ? null : arguments.getString("finePercent");
        if (finePercent != null) {
            AudioManager manager = getTargetContext().getSystemService(AudioManager.class);
            PixelatePreferences.saveFineVolumeState(
                    getTargetContext(),
                    Integer.parseInt(finePercent),
                    manager.getStreamVolume(AudioManager.STREAM_MUSIC));
        }
        ScreenOffMediaService.sync(getTargetContext());
        SystemClock.sleep(500);
        String gesture = arguments == null ? "up" : arguments.getString("gesture", "up");
        long holdMs = arguments == null ? 850
                : Long.parseLong(arguments.getString("holdMs", "850"));
        int presses = arguments == null ? 1
                : Integer.parseInt(arguments.getString("presses", "1"));
        UiAutomation automation = getUiAutomation(
                UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
        int[] keys = "both".equals(gesture)
                ? new int[]{KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN}
                : new int[]{"down".equals(gesture)
                        ? KeyEvent.KEYCODE_VOLUME_DOWN : KeyEvent.KEYCODE_VOLUME_UP};
        for (int press = 0; press < presses; press++) {
            long downTime = SystemClock.uptimeMillis();
            for (int key : keys) {
                inject(automation, downTime, downTime, KeyEvent.ACTION_DOWN, key);
            }
            SystemClock.sleep(holdMs);
            long upTime = SystemClock.uptimeMillis();
            for (int key : keys) {
                inject(automation, downTime, upTime, KeyEvent.ACTION_UP, key);
            }
            SystemClock.sleep(250);
        }
        Bundle result = new Bundle();
        result.putString("gesture", gesture);
        result.putLong("holdMs", holdMs);
        result.putInt("presses", presses);
        finish(Activity.RESULT_OK, result);
    }

    private void inspectOverlay() {
        Bundle result = new Bundle();
        try {
            UiAutomation automation = getUiAutomation(
                    UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
            AccessibilityServiceInfo info = automation.getServiceInfo();
            info.flags |= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
            automation.setServiceInfo(info);
            long deadline = SystemClock.uptimeMillis() + 5000;
            while (SystemClock.uptimeMillis() < deadline) {
                for (AccessibilityWindowInfo window : automation.getWindows()) {
                    AccessibilityNodeInfo root = window.getRoot();
                    if (root == null) continue;
                    for (AccessibilityNodeInfo node : root.findAccessibilityNodeInfosByText(
                            "Zamknij wszystkie aplikacje")) {
                        if (!"android.widget.Button".contentEquals(node.getClassName())
                                || !getTargetContext().getPackageName().contentEquals(
                                        node.getPackageName())) continue;
                        String expected = "Zamknij wszystkie aplikacje";
                        if (!expected.contentEquals(node.getText())) {
                            throw new IllegalStateException("Unexpected button text");
                        }
                        CharSequence description = node.getContentDescription();
                        if (description != null && description.length() > 0
                                && !expected.contentEquals(description)) {
                            throw new IllegalStateException("Unexpected spoken label: " + description);
                        }
                        if (node.getPaneTitle() != null) {
                            throw new IllegalStateException("Button has an extra pane announcement");
                        }
                        result.putString("buttonText", node.getText().toString());
                        result.putString("contentDescription", String.valueOf(description));
                        result.putBoolean("extraPaneTitle", false);
                        finish(Activity.RESULT_OK, result);
                        return;
                    }
                }
                SystemClock.sleep(250);
            }
            throw new IllegalStateException("Open Recents before running the overlay check");
        } catch (Throwable error) {
            result.putString("error", error.toString());
            finish(Activity.RESULT_CANCELED, result);
        }
    }

    private void probeDynamicsProcessing() {
        Bundle result = new Bundle();
        DynamicsProcessing effect = null;
        try {
            effect = new DynamicsProcessing(1000, 0, null);
            effect.setInputGainAllChannelsTo(-12f);
            int status = effect.setEnabled(true);
            result.putBoolean("hasControl", effect.hasControl());
            result.putBoolean("enabled", effect.getEnabled());
            result.putInt("setEnabledStatus", status);
            result.putInt("channels", effect.getChannelCount());
            result.putFloat("gainDb", effect.getInputGainByChannelIndex(0));
            Log.i("PixelCenterNight", "DynamicsProcessing session0 control="
                    + effect.hasControl() + " enabled=" + effect.getEnabled()
                    + " channels=" + effect.getChannelCount()
                    + " gainDb=" + effect.getInputGainByChannelIndex(0));
            SystemClock.sleep(1500);
        } catch (Throwable error) {
            result.putString("error", error.getClass().getName() + ": " + error.getMessage());
            Log.e("PixelCenterNight", "DynamicsProcessing probe failed", error);
        } finally {
            if (effect != null) {
                effect.setEnabled(false);
                effect.release();
            }
        }
        finish(Activity.RESULT_OK, result);
    }

    private void testDelayedSystemVolumeEcho() {
        Bundle result = new Bundle();
        AudioManager manager = getTargetContext().getSystemService(AudioManager.class);
        try {
            if (!AudioOutputRouting.isBluetoothMediaOutput(manager)) {
                result.putBoolean("skippedNoBluetoothOutput", true);
                finish(Activity.RESULT_OK, result);
                return;
            }
            PixelatePreferences.setAdvancedVolumeEnabled(getTargetContext(), true);
            manager.setStreamVolume(AudioManager.STREAM_MUSIC, 1, 0);
            PixelatePreferences.saveFineVolumeState(getTargetContext(), 4, 1);
            getTargetContext().stopService(
                    new Intent(getTargetContext(), ScreenOffMediaService.class));
            SystemClock.sleep(300);
            ScreenOffMediaService.sync(getTargetContext());
            SystemClock.sleep(700);

            if (!ScreenOffMediaService.adjustVolumeFromAccessibility(
                    AudioManager.ADJUST_LOWER)) {
                throw new IllegalStateException("Usluga glosnosci nie dziala");
            }
            SystemClock.sleep(550);
            manager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
            SystemClock.sleep(700);
            int afterDownPercent = PixelatePreferences.getFineVolumePercent(
                    getTargetContext());
            int afterDownPhysical = manager.getStreamVolume(AudioManager.STREAM_MUSIC);
            result.putInt("afterDownPercent", afterDownPercent);
            result.putInt("afterDownPhysical", afterDownPhysical);
            if (afterDownPercent != 2 || afterDownPhysical != 1) {
                throw new IllegalStateException("Echo w dol nie zostalo odfiltrowane: "
                        + afterDownPercent + "% / " + afterDownPhysical);
            }

            if (!ScreenOffMediaService.adjustVolumeFromAccessibility(
                    AudioManager.ADJUST_RAISE)) {
                throw new IllegalStateException("Usluga glosnosci nie dziala");
            }
            SystemClock.sleep(550);
            manager.setStreamVolume(AudioManager.STREAM_MUSIC, 2, 0);
            SystemClock.sleep(700);
            int afterUpPercent = PixelatePreferences.getFineVolumePercent(
                    getTargetContext());
            int afterUpPhysical = manager.getStreamVolume(AudioManager.STREAM_MUSIC);
            result.putInt("afterUpPercent", afterUpPercent);
            result.putInt("afterUpPhysical", afterUpPhysical);
            if (afterUpPercent != 4 || afterUpPhysical != 1) {
                throw new IllegalStateException("Echo w gore nie zostalo odfiltrowane: "
                        + afterUpPercent + "% / " + afterUpPhysical);
            }
            Log.i("PixelCenterVolume", "Test echo OK: 4 -> 2 mimo 1 -> 0, "
                    + "2 -> 4 mimo 1 -> 2");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("error", error.getClass().getName() + ": "
                    + error.getMessage());
            Log.e("PixelCenterVolume", "Test echo nie przeszedl", error);
            finish(Activity.RESULT_CANCELED, result);
        }
    }

    private void probeVolumeUi() {
        Bundle result = new Bundle();
        AudioManager manager = getTargetContext().getSystemService(AudioManager.class);
        FineVolumeEngine engine = null;
        int beforePhysical = manager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int savedPercent = PixelatePreferences.getFineVolumePercent(getTargetContext());
        int savedPhysical = PixelatePreferences.getFineVolumePhysicalLevel(getTargetContext());
        int resultCode = Activity.RESULT_OK;
        try {
            engine = new FineVolumeEngine(getTargetContext(), manager);
            engine.initialize();
            int max = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int direction = beforePhysical < max
                    ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER;
            FineVolumeEngine.State state = engine.adjustNightStep(direction);
            result.putInt("beforePhysical", beforePhysical);
            result.putInt("probePhysical", state.physicalLevel);
            Log.i("PixelCenterVolume", "Volume UI probe active for 5 seconds; "
                    + beforePhysical + " -> " + state.physicalLevel);
            SystemClock.sleep(5000);
        } catch (Throwable error) {
            result.putString("error", error.getClass().getName() + ": "
                    + error.getMessage());
            resultCode = Activity.RESULT_CANCELED;
        } finally {
            if (engine != null) engine.release();
            manager.setStreamVolume(AudioManager.STREAM_MUSIC, beforePhysical, 0);
            if (savedPercent >= 0 && savedPhysical >= 0) {
                PixelatePreferences.saveFineVolumeState(
                        getTargetContext(), savedPercent, savedPhysical);
            } else {
                PixelatePreferences.clearFineVolumeState(getTargetContext());
            }
            // Let the asynchronous preference write complete before finish()
            // terminates the instrumentation process.
            SystemClock.sleep(500);
        }
        finish(resultCode, result);
    }

    private void restorePlaybackPosition() {
        Bundle result = new Bundle();
        String packageName = arguments.getString("restorePackage");
        long position = Long.parseLong(arguments.getString("restorePosition"));
        try {
            MediaSessionManager manager = getTargetContext().getSystemService(
                    MediaSessionManager.class);
            ComponentName listener = new ComponentName(
                    getTargetContext(), PlayerMonitorService.class);
            List<MediaController> controllers = manager.getActiveSessions(listener);
            for (MediaController controller : controllers) {
                if (!packageName.equals(controller.getPackageName())) continue;
                controller.getTransportControls().seekTo(position);
                SystemClock.sleep(400);
                result.putString("restoredPackage", packageName);
                result.putLong("restoredPosition", position);
                finish(Activity.RESULT_OK, result);
                return;
            }
            throw new IllegalStateException("Nie znaleziono sesji " + packageName);
        } catch (Throwable error) {
            result.putString("error", error.getClass().getName() + ": "
                    + error.getMessage());
            finish(Activity.RESULT_CANCELED, result);
        }
    }

    private static void inject(
            UiAutomation automation, long downTime, long eventTime, int action, int keyCode) {
        KeyEvent event = new KeyEvent(
                downTime,
                eventTime,
                action,
                keyCode,
                0,
                0,
                KeyEvent.KEYCODE_UNKNOWN,
                0,
                0,
                InputDevice.SOURCE_KEYBOARD);
        if (!automation.injectInputEvent(event, true)) {
            throw new IllegalStateException("Nie udalo sie wstrzyknac keyCode=" + keyCode);
        }
    }
}
