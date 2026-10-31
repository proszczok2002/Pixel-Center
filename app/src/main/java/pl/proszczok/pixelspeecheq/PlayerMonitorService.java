package pl.proszczok.pixelspeecheq;

import android.content.ComponentName;
import android.content.Intent;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public final class PlayerMonitorService extends NotificationListenerService
        implements MediaSessionManager.OnActiveSessionsChangedListener {
    private static final String TAG = "PixelSpeechEQ";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = this::refreshPlaybackState;
    private final List<MediaController> observedControllers = new ArrayList<>();
    private final MediaController.Callback controllerCallback = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            int value = state == null ? -1 : state.getState();
            if (value == lastControllerPlaybackState) return;
            lastControllerPlaybackState = value;
            handler.removeCallbacks(refreshRunnable);
            handler.post(refreshRunnable);
        }

        @Override
        public void onSessionDestroyed() {
            handler.post(PlayerMonitorService.this::reloadControllers);
        }
    };
    private MediaSessionManager sessionManager;
    private ComponentName listenerComponent;
    private int lastControllerPlaybackState = Integer.MIN_VALUE;
    private boolean targetWasPlaying;
    private String activeTargetPackage;

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        ScreenOffMediaService.sync(this);
        sessionManager = getSystemService(MediaSessionManager.class);
        listenerComponent = new ComponentName(this, PlayerMonitorService.class);
        sessionManager.addOnActiveSessionsChangedListener(this, listenerComponent, handler);
        reloadControllers();
    }

    @Override
    public void onListenerDisconnected() {
        if (sessionManager != null) {
            sessionManager.removeOnActiveSessionsChangedListener(this);
        }
        clearControllerCallbacks();
        handler.removeCallbacks(refreshRunnable);
        if (targetWasPlaying) suspendEq();
        activeTargetPackage = null;
        super.onListenerDisconnected();
    }

    @Override
    public void onActiveSessionsChanged(List<MediaController> controllers) {
        replaceControllerCallbacks(controllers);
        refreshPlaybackState();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        scheduleRefresh(sbn.getPackageName());
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        scheduleRefresh(sbn.getPackageName());
    }

    private void scheduleRefresh(String packageName) {
        if (ScopedEqService.isSupportedPlayer(packageName)) {
            handler.removeCallbacks(refreshRunnable);
            handler.postDelayed(refreshRunnable, 40);
        }
    }

    private void reloadControllers() {
        if (sessionManager == null || listenerComponent == null) return;
        try {
            replaceControllerCallbacks(sessionManager.getActiveSessions(listenerComponent));
        } catch (SecurityException error) {
            Log.w(TAG, "Cannot register media callbacks", error);
        }
        refreshPlaybackState();
    }

    private void replaceControllerCallbacks(List<MediaController> controllers) {
        clearControllerCallbacks();
        lastControllerPlaybackState = Integer.MIN_VALUE;
        if (controllers == null) return;
        for (MediaController controller : controllers) {
            if (!ScopedEqService.isSupportedPlayer(controller.getPackageName())) continue;
            controller.registerCallback(controllerCallback, handler);
            observedControllers.add(controller);
        }
    }

    private void clearControllerCallbacks() {
        for (MediaController controller : observedControllers) {
            controller.unregisterCallback(controllerCallback);
        }
        observedControllers.clear();
    }

    private void refreshPlaybackState() {
        if (sessionManager == null || listenerComponent == null) return;
        String playingPackage = null;
        try {
            for (MediaController controller : sessionManager.getActiveSessions(listenerComponent)) {
                String packageName = controller.getPackageName();
                if (ScopedEqService.isSupportedPlayer(packageName) && isPlaying(controller.getPlaybackState())) {
                    playingPackage = packageName;
                    break;
                }
            }
        } catch (SecurityException error) {
            Log.w(TAG, "Cannot read media sessions", error);
        }

        if (playingPackage != null) {
            if (!targetWasPlaying || !playingPackage.equals(activeTargetPackage)) {
                Intent intent = new Intent(this, ScopedEqService.class)
                        .setAction(ScopedEqService.ACTION_ACTIVATE_FOR_PLAYER)
                        .putExtra(ScopedEqService.EXTRA_PLAYER_PACKAGE, playingPackage);
                startForegroundService(intent);
            }
            targetWasPlaying = true;
            activeTargetPackage = playingPackage;
        } else if (targetWasPlaying) {
            targetWasPlaying = false;
            activeTargetPackage = null;
            suspendEq();
        }
    }

    private boolean isPlaying(PlaybackState playbackState) {
        if (playbackState == null) return false;
        int state = playbackState.getState();
        return state == PlaybackState.STATE_PLAYING
                || state == PlaybackState.STATE_BUFFERING
                || state == PlaybackState.STATE_CONNECTING
                || state == PlaybackState.STATE_FAST_FORWARDING
                || state == PlaybackState.STATE_REWINDING;
    }

    private void suspendEq() {
        startService(new Intent(this, ScopedEqService.class).setAction(ScopedEqService.ACTION_SUSPEND));
    }
}
