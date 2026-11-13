package pl.proszczok.pixelspeecheq;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.List;
public final class PixelateAccessibilityService extends AccessibilityService
        implements OverlayController.Listener, ClearAllController.Listener {
    private static final String TAG = "PixelCenterMedia";
    private static final long OVERVIEW_STABLE_MS = 300;
    private static final long WINDOW_RECOVERY_MS = 2500;
    private static final long WINDOW_RETRY_MS = 250;
    private static final long MEDIA_LONG_PRESS_MS = 500;
    private static final long MEDIA_WAKE_LOCK_TIMEOUT_MS = 2000;
    private static final int IDLE_EVENT_TYPES = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            | AccessibilityEvent.TYPE_WINDOWS_CHANGED;
    private static final int OVERVIEW_EVENT_TYPES = IDLE_EVENT_TYPES
            | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
    private static WeakReference<PixelateAccessibilityService> current = new WeakReference<>(null);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable evaluateRunnable = () -> {
        evaluationScheduled = false;
        evaluateOverview();
    };
    private final Runnable mediaLongPressRunnable = this::handleMediaLongPress;

    private OverlayController overlay;
    private ClearAllController clearer;
    private boolean connected;
    private boolean evaluationScheduled;
    private long evaluationDueAt;
    private long windowRecoveryUntil;
    private boolean eventMonitoringInitialized;
    private boolean monitoringOverviewContent;
    private boolean suppressUntilOverviewExit;
    private long overviewVisibleSince;
    private AudioManager audioManager;
    private PowerManager powerManager;
    private PowerManager.WakeLock mediaGestureWakeLock;
    private boolean mediaGestureActive;
    private boolean volumeUpPressed;
    private boolean volumeDownPressed;
    private boolean volumeUpParticipated;
    private boolean volumeDownParticipated;
    private boolean mediaLongPressHandled;
    private boolean gestureCanControlMedia;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        if (!DeviceSupport.isSupportedDevice()) {
            Toast.makeText(this, R.string.unsupported_service_disabled, Toast.LENGTH_LONG).show();
            disableSelf();
            stopSelf();
            return;
        }
        connected = true;
        current = new WeakReference<>(this);
        audioManager = getSystemService(AudioManager.class);
        powerManager = getSystemService(PowerManager.class);
        if (powerManager != null) {
            mediaGestureWakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    getPackageName() + ":screen-off-media-keys");
            mediaGestureWakeLock.setReferenceCounted(false);
        }
        overlay = new OverlayController(this, this);
        clearer = new ClearAllController(this, this);
        ScreenOffMediaService.sync(this);
        updateEventMonitoring(false);
        windowRecoveryUntil = SystemClock.uptimeMillis() + WINDOW_RECOVERY_MS;
        scheduleEvaluation(250);
        MainActivity.onServiceConnectionChanged();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!connected) {
            return;
        }
        if ((event.getEventType() & IDLE_EVENT_TYPES) != 0
                && !getPackageName().contentEquals(
                        event.getPackageName() == null ? "" : event.getPackageName())) {
            windowRecoveryUntil = SystemClock.uptimeMillis() + WINDOW_RECOVERY_MS;
        }
        scheduleEvaluation(80);
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (keyCode != KeyEvent.KEYCODE_VOLUME_UP
                && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
            return false;
        }
        Log.d(TAG, "Klawisz glosnosci: action=" + event.getAction()
                + ", repeat=" + event.getRepeatCount()
                + ", interactive=" + (powerManager != null && powerManager.isInteractive()));
        if (!mediaGestureActive) {
            if (event.getAction() != KeyEvent.ACTION_DOWN
                    || event.getRepeatCount() != 0
                    || !shouldStartMediaGesture()) {
                return false;
            }
            mediaGestureActive = true;
            mediaLongPressHandled = false;
            gestureCanControlMedia = canControlMediaNow();
            volumeUpParticipated = false;
            volumeDownParticipated = false;
            if (mediaGestureWakeLock != null) {
                mediaGestureWakeLock.acquire(MEDIA_WAKE_LOCK_TIMEOUT_MS);
            }
            if (gestureCanControlMedia) {
                handler.postDelayed(mediaLongPressRunnable, MEDIA_LONG_PRESS_MS);
            }
        }

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                volumeUpPressed = true;
                volumeUpParticipated = true;
            } else {
                volumeDownPressed = true;
                volumeDownParticipated = true;
            }
            if (gestureCanControlMedia
                    && (event.getRepeatCount() > 0 || event.isLongPress())) {
                handleMediaLongPress();
            }
            return true;
        }

        if (event.getAction() == KeyEvent.ACTION_UP) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                volumeUpPressed = false;
            } else {
                volumeDownPressed = false;
            }
            if (!volumeUpPressed && !volumeDownPressed) {
                finishMediaGesture((event.getFlags() & KeyEvent.FLAG_CANCELED) != 0);
            }
            return true;
        }

        return true;
    }

    @Override
    public void onInterrupt() {
        cancelMediaGesture();
        cancelWork();
        updateEventMonitoring(false);
        if (overlay != null) {
            overlay.hide();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (overlay != null) {
            overlay.onConfigurationChanged();
        }
        scheduleEvaluation(200);
    }

    @Override
    public void onDestroy() {
        connected = false;
        if (current.get() == this) {
            current.clear();
        }
        MainActivity.onServiceConnectionChanged();
        cancelWork();
        cancelMediaGesture();
        if (overlay != null) {
            overlay.destroy();
        }
        super.onDestroy();
    }

    @Override
    public void onClearAllRequested() {
        if (clearer != null && !clearer.isRunning()) {
            overlay.setBusy(true);
            clearer.start();
        }
    }

    @Override
    public void onClearSucceeded() {
        suppressUntilOverviewExit = true;
        overlay.setBusy(false);
        overlay.hide();
        announceResult(R.string.clear_success);
        scheduleEvaluation(500);
    }

    @Override
    public void onClearFailed(boolean noTasks) {
        overlay.setBusy(false);
        announceResult(noTasks ? R.string.clear_no_tasks : R.string.clear_failure);
        scheduleEvaluation(250);
    }

    private void evaluateOverview() {
        if (!connected || overlay == null) {
            return;
        }
        List<AccessibilityWindowInfo> windows = getWindows();
        boolean overview = OverviewDetector.isOverviewVisible(
                windows, getPackageName(), this);
        updateEventMonitoring(overview);
        boolean tasks = overview && OverviewDetector.hasRecentTasks(
                windows, getPackageName(), this);
        if (!overview) {
            overviewVisibleSince = 0;
            suppressUntilOverviewExit = false;
            cancelClearOperation();
            overlay.hide();
        } else if (suppressUntilOverviewExit) {
            overlay.hide();
        } else {
            long now = SystemClock.uptimeMillis();
            if (overviewVisibleSince == 0) {
                overviewVisibleSince = now;
                overlay.hide();
                scheduleEvaluation(OVERVIEW_STABLE_MS);
            } else {
                long remaining = OVERVIEW_STABLE_MS - (now - overviewVisibleSince);
                if (remaining > 0) {
                    overlay.hide();
                    scheduleEvaluation(remaining);
                } else if (!tasks) {
                    overlay.hide();
                } else {
                    if (clearer == null || !clearer.isRunning()) {
                        overlay.setBusy(false);
                    }
                    overlay.show(PixelatePreferences.shouldFocusClearButton(this));
                }
            }
        }
        // Zdarzenie okna może wyprzedzić gotowość drzewa dostępności. Ponawiamy
        // wyłącznie podczas krótkiego przejścia i tylko, gdy launcher albo brak
        // drzewa może jeszcze zmienić się w widok Ostatnich.
        long retryRemaining = windowRecoveryUntil - SystemClock.uptimeMillis();
        if ((!overview || !tasks) && retryRemaining > 0
                && OverviewDetector.mayBeOverviewPending(windows, getPackageName())) {
            scheduleEvaluation(Math.min(WINDOW_RETRY_MS, retryRemaining));
        }
    }

    private void scheduleEvaluation(long delayMs) {
        long dueAt = SystemClock.uptimeMillis() + delayMs;
        if (evaluationScheduled && evaluationDueAt <= dueAt) {
            return;
        }
        handler.removeCallbacks(evaluateRunnable);
        evaluationScheduled = true;
        evaluationDueAt = dueAt;
        handler.postDelayed(evaluateRunnable, delayMs);
    }

    private void updateEventMonitoring(boolean overviewVisible) {
        if (eventMonitoringInitialized && monitoringOverviewContent == overviewVisible) {
            return;
        }
        AccessibilityServiceInfo info = getServiceInfo();
        if (info == null) {
            return;
        }
        eventMonitoringInitialized = true;
        monitoringOverviewContent = overviewVisible;
        info.eventTypes = overviewVisible ? OVERVIEW_EVENT_TYPES : IDLE_EVENT_TYPES;
        info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
        setServiceInfo(info);
    }

    private void cancelWork() {
        handler.removeCallbacks(evaluateRunnable);
        evaluationScheduled = false;
        windowRecoveryUntil = 0;
        cancelClearOperation();
    }

    private void cancelClearOperation() {
        if (clearer != null) {
            clearer.cancel();
        }
        if (overlay != null) {
            overlay.setBusy(false);
        }
    }

    private boolean shouldStartMediaGesture() {
        if (audioManager != null && audioManager.getMode() != AudioManager.MODE_NORMAL) {
            return false;
        }
        boolean advancedVolume = PixelatePreferences.isAdvancedVolumeEnabled(this)
                && AudioOutputRouting.isBluetoothMediaOutput(audioManager);
        return advancedVolume || canControlMediaNow();
    }

    private boolean canControlMediaNow() {
        return PixelatePreferences.areScreenOffMediaControlsEnabled(this)
                && powerManager != null && !powerManager.isInteractive();
    }

    private void handleMediaLongPress() {
        if (!mediaGestureActive || mediaLongPressHandled || !gestureCanControlMedia
                || !canControlMediaNow()) {
            return;
        }
        if (volumeUpPressed && volumeDownPressed) {
            dispatchMediaButton(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        } else if (volumeUpPressed) {
            dispatchMediaButton(KeyEvent.KEYCODE_MEDIA_NEXT);
        } else if (volumeDownPressed) {
            dispatchMediaButton(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
        } else {
            return;
        }
        mediaLongPressHandled = true;
    }

    private void dispatchMediaButton(int keyCode) {
        if (audioManager == null) return;
        if (!MediaCommandDispatcher.dispatchToActivePlayer(this, keyCode)) {
            MediaCommandDispatcher.dispatchGlobal(audioManager, keyCode);
        }
    }

    private void finishMediaGesture(boolean canceled) {
        boolean mediaCommandHandled = mediaLongPressHandled;
        if (!canceled && !mediaLongPressHandled && audioManager != null) {
            if (volumeUpParticipated) {
                adjustMusicVolume(AudioManager.ADJUST_RAISE);
            }
            if (volumeDownParticipated) {
                adjustMusicVolume(AudioManager.ADJUST_LOWER);
            }
        }
        clearMediaGestureState();
        if (mediaCommandHandled) {
            ScreenOffMediaService.refreshPriorityAfterAccessibilityGesture();
        }
    }

    private void adjustMusicVolume(int direction) {
        if (audioManager == null) return;
        if (PixelatePreferences.isAdvancedVolumeEnabled(this)
                && AudioOutputRouting.isBluetoothMediaOutput(audioManager)
                && ScreenOffMediaService.adjustVolumeFromAccessibility(direction)) {
            Log.d(TAG, "Krotkie nacisniecie przekazane do cichego trybu nocnego");
            return;
        }
        audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC, direction, 0);
        Log.d(TAG, "Krotkie nacisniecie przekazane do fabrycznej glosnosci Androida");
    }

    private void cancelMediaGesture() {
        clearMediaGestureState();
    }

    private void clearMediaGestureState() {
        handler.removeCallbacks(mediaLongPressRunnable);
        if (mediaGestureWakeLock != null && mediaGestureWakeLock.isHeld()) {
            mediaGestureWakeLock.release();
        }
        mediaGestureActive = false;
        volumeUpPressed = false;
        volumeDownPressed = false;
        volumeUpParticipated = false;
        volumeDownParticipated = false;
        mediaLongPressHandled = false;
        gestureCanControlMedia = false;
    }

    private void announceResult(int stringId) {
        if (overlay != null) {
            overlay.announce(getString(stringId));
        }
        Toast.makeText(this, stringId, Toast.LENGTH_SHORT).show();
    }

    static boolean isConnected() {
        PixelateAccessibilityService service = current.get();
        return service != null && service.connected;
    }

    static boolean announceNightLevel() {
        PixelateAccessibilityService service = current.get();
        if (service == null || !service.connected) return false;
        service.handler.post(() -> service.sendReaderAnnouncement(
                "Cichy poziom nocny"));
        return true;
    }

    private void sendReaderAnnouncement(String message) {
        AccessibilityEvent event = AccessibilityEvent.obtain(
                AccessibilityEvent.TYPE_ANNOUNCEMENT);
        event.setPackageName(getPackageName());
        event.setClassName(PixelateAccessibilityService.class.getName());
        event.setEnabled(true);
        event.getText().add(message);
        AccessibilityManager manager = getSystemService(AccessibilityManager.class);
        if (manager != null && manager.isEnabled()) {
            manager.sendAccessibilityEvent(event);
        }
        Log.d(TAG, "Wyslano komunikat czytnika: " + message);
    }

    static boolean openRecentsForTest() {
        PixelateAccessibilityService service = current.get();
        if (service == null || !service.connected) {
            return false;
        }
        boolean accepted = service.performGlobalAction(GLOBAL_ACTION_RECENTS);
        if (accepted) {
            service.scheduleEvaluation(500);
        }
        return accepted;
    }
}
