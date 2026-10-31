package pl.proszczok.pixelspeecheq;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.VolumeProvider;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

public final class ScreenOffMediaService extends Service {
    private static final String TAG = "PixelCenterVolume";
    private static final String CHANNEL_ID = "screen_off_media_controls";
    private static final String ACTION_VOLUME_CHANGED =
            "android.media.VOLUME_CHANGED_ACTION";
    private static final String EXTRA_VOLUME_STREAM_TYPE =
            "android.media.EXTRA_VOLUME_STREAM_TYPE";
    private static final String EXTRA_VOLUME_STREAM_VALUE =
            "android.media.EXTRA_VOLUME_STREAM_VALUE";
    private static final String EXTRA_PREVIOUS_VOLUME_STREAM_VALUE =
            "android.media.EXTRA_PREV_VOLUME_STREAM_VALUE";
    private static final int NOTIFICATION_ID = 103;
    private static final long LONG_PRESS_MS = 700;
    private static final long GESTURE_STALE_MS = 1800;
    private static final long GESTURE_WAKE_LOCK_MS = 2000;
    private static final long FINE_VOLUME_ANNOUNCEMENT_DELAY_MS = 260;
    private static final long SYSTEM_VOLUME_ECHO_WINDOW_MS = 1200;
    private static final long AUDIO_ROUTE_SETTLE_MS = 120;
    private static final long SESSION_PRIORITY_REFRESH_DELAY_MS = 120;
    private static volatile boolean running;
    private static volatile boolean capturingVolumeKeys;
    private static volatile ScreenOffMediaService currentService;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable longPressRunnable = this::handleLongPress;
    private final Runnable staleGestureRunnable = this::resetVolumeGesture;
    private final Runnable refreshSessionPriorityRunnable = this::refreshSessionPriority;
    private final Runnable refreshAudioRouteRunnable = this::refreshRemoteVolumeState;
    private AudioManager audioManager;
    private MediaSessionManager mediaSessionManager;
    private ComponentName notificationListenerComponent;
    private PowerManager powerManager;
    private PowerManager.WakeLock gestureWakeLock;
    private Vibrator vibrator;
    private MediaSession mediaSession;
    private VolumeProvider volumeProvider;
    private FineVolumeEngine fineVolumeEngine;
    private boolean receiverRegistered;
    private boolean audioCallbacksRegistered;
    private boolean remoteVolumeActive;
    private boolean remoteFineVolumeMode;
    private int gestureDirections;
    private boolean gestureActive;
    private boolean gestureHandled;
    private boolean pendingNightLevelAnnouncement;
    private boolean sessionPriorityRefreshPending;
    private boolean sessionListenerRegistered;
    private final Map<MediaSession.Token, ObservedPlayer> observedPlayers = new HashMap<>();
    private final MediaSessionManager.OnActiveSessionsChangedListener sessionsListener =
            this::onActivePlayersChanged;
    private int pendingEchoPreviousPhysical = -1;
    private int pendingEchoPhysical = -1;
    private int pendingEchoPercent = -1;
    private long pendingEchoUntil;
    private final Runnable fineVolumeAnnouncementRunnable =
            this::sendFineVolumeAnnouncement;

    private final class ObservedPlayer extends MediaController.Callback {
        final MediaController controller;
        int lastState = Integer.MIN_VALUE;

        ObservedPlayer(MediaController controller) {
            this.controller = controller;
        }

        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            int value = state == null ? PlaybackState.STATE_NONE : state.getState();
            if (value == lastState) return;
            lastState = value;
            requestSessionPriorityRefresh();
        }

        @Override
        public void onSessionDestroyed() {
            reloadActivePlayers();
        }
    }

    private final AudioDeviceCallback audioDeviceCallback = new AudioDeviceCallback() {
        @Override
        public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
            scheduleAudioRouteRefresh();
        }

        @Override
        public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
            scheduleAudioRouteRefresh();
        }
    };

    private final AudioManager.AudioPlaybackCallback audioPlaybackCallback =
            new AudioManager.AudioPlaybackCallback() {
                @Override
                public void onPlaybackConfigChanged(
                        List<android.media.AudioPlaybackConfiguration> configs) {
                    scheduleAudioRouteRefresh();
                }
            };

    private final MediaSession.Callback mediaSessionCallback = new MediaSession.Callback() {
        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
            Log.d(TAG, "MediaSession media button=" + mediaButtonIntent);
            return super.onMediaButtonEvent(mediaButtonIntent);
        }
    };

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_VOLUME_CHANGED.equals(intent.getAction())) {
                handleExternalVolumeChange(intent);
            } else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                activateRemoteVolume();
            } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                refreshRemoteVolumeState();
            }
        }
    };

    static void sync(Context context) {
        Intent intent = new Intent(context, ScreenOffMediaService.class);
        try {
            if (shouldRun(context)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent);
                } else {
                    context.startService(intent);
                }
            } else {
                context.stopService(intent);
            }
        } catch (RuntimeException error) {
            Log.w(TAG, "System chwilowo odrzucil synchronizacje uslugi", error);
        }
    }

    static boolean isRunning() {
        return running;
    }

    static boolean isCapturingVolumeKeys() {
        return capturingVolumeKeys;
    }

    static boolean adjustVolumeFromAccessibility(int direction) {
        ScreenOffMediaService service = currentService;
        if (service == null || !running) return false;
        service.setMusicVolumeBy(direction);
        return true;
    }

    static void refreshPriorityAfterAccessibilityGesture() {
        ScreenOffMediaService service = currentService;
        if (service == null || !running) return;
        service.handler.post(service::requestSessionPriorityRefresh);
    }

    private static boolean shouldRun(Context context) {
        return PixelatePreferences.areScreenOffMediaControlsEnabled(context)
                || PixelatePreferences.isAdvancedVolumeEnabled(context);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        running = true;
        currentService = this;
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());
        audioManager = getSystemService(AudioManager.class);
        if (audioManager != null) {
            audioManager.registerAudioDeviceCallback(audioDeviceCallback, handler);
            audioManager.registerAudioPlaybackCallback(audioPlaybackCallback, handler);
            audioCallbacksRegistered = true;
        }
        fineVolumeEngine = new FineVolumeEngine(this, audioManager);
        mediaSessionManager = getSystemService(MediaSessionManager.class);
        notificationListenerComponent = new ComponentName(this, PlayerMonitorService.class);
        powerManager = getSystemService(PowerManager.class);
        if (powerManager != null) {
            gestureWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getPackageName() + ":volume-gesture");
            gestureWakeLock.setReferenceCounted(false);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vibratorManager = getSystemService(VibratorManager.class);
            vibrator = vibratorManager == null ? null : vibratorManager.getDefaultVibrator();
        } else {
            vibrator = getSystemService(Vibrator.class);
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(ACTION_VOLUME_CHANGED);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(screenReceiver, filter);
        }
        receiverRegistered = true;
        registerSessionListener();
        refreshRemoteVolumeState();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!shouldRun(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        registerSessionListener();
        refreshRemoteVolumeState();
        requestSessionPriorityRefresh();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        running = false;
        if (currentService == this) currentService = null;
        sessionPriorityRefreshPending = false;
        handler.removeCallbacksAndMessages(null);
        resetVolumeGesture();
        if (sessionListenerRegistered && mediaSessionManager != null) {
            mediaSessionManager.removeOnActiveSessionsChangedListener(sessionsListener);
            sessionListenerRegistered = false;
        }
        for (ObservedPlayer player : observedPlayers.values()) {
            player.controller.unregisterCallback(player);
        }
        observedPlayers.clear();
        if (receiverRegistered) {
            unregisterReceiver(screenReceiver);
            receiverRegistered = false;
        }
        if (audioCallbacksRegistered && audioManager != null) {
            audioManager.unregisterAudioDeviceCallback(audioDeviceCallback);
            audioManager.unregisterAudioPlaybackCallback(audioPlaybackCallback);
            audioCallbacksRegistered = false;
        }
        releaseMediaSession();
        if (fineVolumeEngine != null) {
            fineVolumeEngine.release();
            fineVolumeEngine = null;
        }
        super.onDestroy();
    }

    private void recreateMediaSession(boolean active) {
        releaseMediaSession();
        boolean fineMode = isNightVolumeEnabledForCurrentOutput();
        int max = audioManager == null ? 15
                : audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int current = audioManager == null ? 5
                : audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int providerMax = fineMode ? FineVolumeEngine.MAX_PERCENT : max;
        int providerCurrent = fineMode && fineVolumeEngine != null
                ? fineVolumeEngine.initialize().percent : current;
        volumeProvider = new VolumeProvider(
                VolumeProvider.VOLUME_CONTROL_RELATIVE,
                providerMax, providerCurrent) {
            @Override
            public void onAdjustVolume(int direction) {
                handleVolumeAdjustment(direction);
            }
        };
        mediaSession = new MediaSession(this, "PixelCenterScreenOffVolume");
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setCallback(mediaSessionCallback, handler);
        mediaSession.setPlaybackToRemote(volumeProvider);
        mediaSession.setPlaybackState(new PlaybackState.Builder()
                .setActions(0)
                .setState(PlaybackState.STATE_PLAYING,
                        PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1f)
                .build());
        mediaSession.setActive(active);
        remoteVolumeActive = active;
        remoteFineVolumeMode = fineMode;
        capturingVolumeKeys = active;
        Log.d(TAG, "Utworzono nowa sesje, active=" + active);
    }

    private void releaseMediaSession() {
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
            mediaSession = null;
        }
        volumeProvider = null;
        remoteVolumeActive = false;
        capturingVolumeKeys = false;
    }

    private void refreshRemoteVolumeState() {
        boolean screenOff = powerManager != null && !powerManager.isInteractive();
        boolean fineMode = isNightVolumeEnabledForCurrentOutput();
        if (!fineMode && remoteFineVolumeMode && fineVolumeEngine != null) {
            fineVolumeEngine.disableAndForget();
        }
        boolean screenOffControls = screenOff
                && PixelatePreferences.areScreenOffMediaControlsEnabled(this);
        // Night attenuation is independent of the session used for screen-off keys.
        // On screen-on it is handled by accessibility and volume change events.
        if (screenOffControls) {
            if (!remoteVolumeActive || mediaSession == null
                    || remoteFineVolumeMode != fineMode) {
                recreateMediaSession(true);
                Log.d(TAG, "Przechwytywanie przyciskow glosnosci aktywne");
            }
        } else {
            deactivateRemoteVolume();
        }
    }

    private void activateRemoteVolume() {
        boolean screenOff = powerManager != null && !powerManager.isInteractive();
        boolean fineMode = isNightVolumeEnabledForCurrentOutput();
        if (!(screenOff && PixelatePreferences.areScreenOffMediaControlsEnabled(this))) {
            deactivateRemoteVolume();
            return;
        }
        if (remoteVolumeActive && mediaSession != null
                && remoteFineVolumeMode == fineMode) {
            requestSessionPriorityRefresh();
            return;
        }
        recreateMediaSession(true);
        Log.d(TAG, "Przechwytywanie przyciskow glosnosci aktywne");
    }

    private void deactivateRemoteVolume() {
        sessionPriorityRefreshPending = false;
        handler.removeCallbacks(refreshSessionPriorityRunnable);
        resetVolumeGesture();
        if (!remoteVolumeActive && mediaSession != null) return;
        recreateMediaSession(false);
        Log.d(TAG, "Zdalna regulacja nieaktywna przy wlaczonym ekranie");
    }

    private void handleVolumeAdjustment(int direction) {
        Log.d(TAG, "Odebrano callback VolumeProvider direction=" + direction);
        if (powerManager == null || powerManager.isInteractive()) {
            if (direction == AudioManager.ADJUST_RAISE
                    || direction == AudioManager.ADJUST_LOWER) {
                setMusicVolumeBy(direction);
            }
            return;
        }
        if (direction == AudioManager.ADJUST_SAME) {
            finishVolumeGesture();
            return;
        }
        if (direction != AudioManager.ADJUST_RAISE
                && direction != AudioManager.ADJUST_LOWER) return;
        if (!gestureActive) {
            gestureActive = true;
            if (gestureWakeLock != null) gestureWakeLock.acquire(GESTURE_WAKE_LOCK_MS);
            gestureDirections = 0;
            gestureHandled = false;
            handler.removeCallbacks(longPressRunnable);
            if (PixelatePreferences.areScreenOffMediaControlsEnabled(this)) {
                handler.postDelayed(longPressRunnable, LONG_PRESS_MS);
            }
        }
        gestureDirections |= direction > 0 ? 1 : 2;
        Log.d(TAG, "VolumeProvider direction=" + direction
                + ", directions=" + gestureDirections);
        handler.removeCallbacks(staleGestureRunnable);
        handler.postDelayed(staleGestureRunnable, GESTURE_STALE_MS);
    }

    private void handleLongPress() {
        if (!gestureActive || gestureHandled || gestureDirections == 0
                || powerManager == null || powerManager.isInteractive()
                || !PixelatePreferences.areScreenOffMediaControlsEnabled(this)) return;
        gestureHandled = true;
        vibrateConfirmation();
        if (gestureDirections == 3) {
            dispatchMediaButton(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        } else if (gestureDirections == 1) {
            dispatchMediaButton(KeyEvent.KEYCODE_MEDIA_NEXT);
        } else if (gestureDirections == 2) {
            dispatchMediaButton(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
        }
        Log.d(TAG, "Rozpoznano dlugie przytrzymanie po " + LONG_PRESS_MS + " ms");
    }

    private void finishVolumeGesture() {
        if (!gestureActive) return;
        if (!gestureHandled) {
            if (gestureDirections == 1) {
                setMusicVolumeBy(AudioManager.ADJUST_RAISE);
            } else if (gestureDirections == 2) {
                setMusicVolumeBy(AudioManager.ADJUST_LOWER);
            }
        }
        resetVolumeGesture();
    }

    private void resetVolumeGesture() {
        handler.removeCallbacks(longPressRunnable);
        handler.removeCallbacks(staleGestureRunnable);
        if (gestureWakeLock != null && gestureWakeLock.isHeld()) gestureWakeLock.release();
        gestureDirections = 0;
        gestureActive = false;
        gestureHandled = false;
        if (sessionPriorityRefreshPending) {
            handler.removeCallbacks(refreshSessionPriorityRunnable);
            handler.postDelayed(refreshSessionPriorityRunnable,
                    SESSION_PRIORITY_REFRESH_DELAY_MS);
        }
    }

    private void vibrateConfirmation() {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        vibrator.vibrate(VibrationEffect.createOneShot(
                55, VibrationEffect.DEFAULT_AMPLITUDE));
    }

    private void setMusicVolumeBy(int direction) {
        if (audioManager == null) return;
        if (isNightVolumeEnabledForCurrentOutput()
                && fineVolumeEngine != null) {
            int beforePercent = fineVolumeEngine.getPercent();
            int beforePhysical = fineVolumeEngine.getPhysicalLevel();
            FineVolumeEngine.State state = fineVolumeEngine.adjustNightStep(direction);
            rememberPossibleSystemVolumeEcho(
                    direction, beforePercent, beforePhysical, state);
            if (volumeProvider != null) {
                volumeProvider.setCurrentVolume(state.percent);
            }
            Log.d(TAG, "Tryb nocny -> " + state.percent + "% (fizyczny "
                    + state.physicalLevel + "/" + state.physicalMax + ")");
            if (state.nightLevelActive) announceNightLevel();
            return;
        }
        clearPendingSystemVolumeEcho();
        if (remoteFineVolumeMode && fineVolumeEngine != null) {
            fineVolumeEngine.disableAndForget();
            handler.removeCallbacks(refreshAudioRouteRunnable);
            handler.post(refreshAudioRouteRunnable);
        }
        int before = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0);
        int after = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (volumeProvider != null && !remoteFineVolumeMode) {
            volumeProvider.setCurrentVolume(after);
        }
        Log.d(TAG, "Fabryczny krok Androida: " + before + " -> " + after);
    }

    private void handleExternalVolumeChange(Intent intent) {
        if (fineVolumeEngine == null
                || !PixelatePreferences.isAdvancedVolumeEnabled(this)) return;
        int stream = intent.getIntExtra(EXTRA_VOLUME_STREAM_TYPE, -1);
        if (stream != AudioManager.STREAM_MUSIC) return;
        int physical = intent.getIntExtra(EXTRA_VOLUME_STREAM_VALUE, -1);
        int previous = intent.getIntExtra(EXTRA_PREVIOUS_VOLUME_STREAM_VALUE, -1);
        if (physical < 0) return;
        if (!AudioOutputRouting.isBluetoothMediaOutput(audioManager)) {
            clearPendingSystemVolumeEcho();
            if (remoteFineVolumeMode) {
                fineVolumeEngine.disableAndForget();
                scheduleAudioRouteRefresh();
            }
            return;
        }
        if (consumeMatchingSystemVolumeEcho(previous, physical)) return;
        FineVolumeEngine.State state =
                fineVolumeEngine.handleExternalPhysicalChange(previous, physical);
        if (state == null) return;
        if (volumeProvider != null) volumeProvider.setCurrentVolume(state.percent);
        Log.d(TAG, "Skorygowano zewnetrzna zmiane " + previous + " -> "
                + physical + ": poziom " + state.percent + "%");
        if (state.nightLevelActive) announceNightLevel();
    }

    private void rememberPossibleSystemVolumeEcho(int direction, int beforePercent,
            int beforePhysical, FineVolumeEngine.State state) {
        clearPendingSystemVolumeEcho();
        if (direction != AudioManager.ADJUST_RAISE
                && direction != AudioManager.ADJUST_LOWER) return;
        if (state.percent == beforePercent
                || state.physicalLevel != beforePhysical) return;

        int min = audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC);
        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int echoedPhysical = Math.max(min, Math.min(max,
                beforePhysical + (direction > 0 ? 1 : -1)));
        if (echoedPhysical == beforePhysical) return;

        pendingEchoPreviousPhysical = beforePhysical;
        pendingEchoPhysical = echoedPhysical;
        pendingEchoPercent = state.percent;
        pendingEchoUntil = SystemClock.uptimeMillis()
                + SYSTEM_VOLUME_ECHO_WINDOW_MS;
        Log.d(TAG, "Czekam na mozliwe echo systemu "
                + pendingEchoPreviousPhysical + " -> " + pendingEchoPhysical
                + " po ustawieniu " + pendingEchoPercent + "%");
    }

    private boolean consumeMatchingSystemVolumeEcho(int previous, int physical) {
        if (pendingEchoPercent < 0) return false;
        if (SystemClock.uptimeMillis() > pendingEchoUntil) {
            clearPendingSystemVolumeEcho();
            return false;
        }
        if (previous != pendingEchoPreviousPhysical
                || physical != pendingEchoPhysical) return false;

        int restoredPercent = pendingEchoPercent;
        clearPendingSystemVolumeEcho();
        FineVolumeEngine.State restored = fineVolumeEngine.setPercent(restoredPercent);
        if (volumeProvider != null) {
            volumeProvider.setCurrentVolume(restored.percent);
        }
        Log.d(TAG, "Pominieto opoznione echo tego samego klawisza "
                + previous + " -> " + physical + "; pozostaje "
                + restored.percent + "%");
        return true;
    }

    private void clearPendingSystemVolumeEcho() {
        pendingEchoPreviousPhysical = -1;
        pendingEchoPhysical = -1;
        pendingEchoPercent = -1;
        pendingEchoUntil = 0L;
    }

    private void announceNightLevel() {
        pendingNightLevelAnnouncement = true;
        handler.removeCallbacks(fineVolumeAnnouncementRunnable);
        handler.postDelayed(
                fineVolumeAnnouncementRunnable,
                FINE_VOLUME_ANNOUNCEMENT_DELAY_MS);
    }

    private void sendFineVolumeAnnouncement() {
        if (!pendingNightLevelAnnouncement) return;
        pendingNightLevelAnnouncement = false;
        if (PixelateAccessibilityService.announceNightLevel()) {
            Log.d(TAG, "Przekazano do uslugi dostepnosci: Cichy poziom nocny");
            return;
        }
        AccessibilityManager manager = getSystemService(AccessibilityManager.class);
        if (manager == null || !manager.isEnabled()) return;
        AccessibilityEvent event = AccessibilityEvent.obtain(
                AccessibilityEvent.TYPE_ANNOUNCEMENT);
        event.setPackageName(getPackageName());
        event.setClassName(ScreenOffMediaService.class.getName());
        event.getText().add("Cichy poziom nocny");
        manager.sendAccessibilityEvent(event);
        Log.d(TAG, "Ogłoszono: Cichy poziom nocny");
    }

    private void dispatchMediaButton(int keyCode) {
        if (audioManager == null) return;
        requestSessionPriorityRefresh();
        if (MediaCommandDispatcher.dispatchToActivePlayer(this, keyCode)) {
            return;
        }
        MediaCommandDispatcher.dispatchGlobal(audioManager, keyCode);
        Log.d(TAG, "Wyslano media key=" + keyCode);
    }

    private void requestSessionPriorityRefresh() {
        if (!running || powerManager == null || powerManager.isInteractive()
                || !PixelatePreferences.areScreenOffMediaControlsEnabled(this)) return;
        if (sessionPriorityRefreshPending) return;
        sessionPriorityRefreshPending = true;
        handler.postDelayed(refreshSessionPriorityRunnable,
                SESSION_PRIORITY_REFRESH_DELAY_MS);
    }

    private void refreshSessionPriority() {
        if (!sessionPriorityRefreshPending) return;
        sessionPriorityRefreshPending = false;
        if (!running || powerManager == null || powerManager.isInteractive()
                || !PixelatePreferences.areScreenOffMediaControlsEnabled(this)) return;
        if (mediaSession == null || !remoteVolumeActive) {
            refreshRemoteVolumeState();
            return;
        }
        try {
            if (mediaSessionManager != null) {
                List<MediaController> controllers =
                        mediaSessionManager.getActiveSessions(notificationListenerComponent);
                if (!controllers.isEmpty() && mediaSession.getSessionToken().equals(
                        controllers.get(0).getSessionToken())) return;
            }
        } catch (SecurityException error) {
            Log.w(TAG, "Brak dostepu do priorytetu sesji", error);
        }
        // Restore priority without replacing the token or VolumeProvider.
        // Key release still reaches the same gesture, including during buffering.
        mediaSession.setPlaybackState(volumePlaybackState(PlaybackState.STATE_PAUSED));
        mediaSession.setPlaybackState(volumePlaybackState(PlaybackState.STATE_PLAYING));
        Log.d(TAG, "Odzyskano priorytet przyciskow bez wymiany sesji");
    }

    private PlaybackState volumePlaybackState(int state) {
        return new PlaybackState.Builder().setActions(0)
                .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1f).build();
    }

    private void registerSessionListener() {
        if (sessionListenerRegistered || mediaSessionManager == null) return;
        try {
            mediaSessionManager.addOnActiveSessionsChangedListener(
                    sessionsListener, notificationListenerComponent, handler);
            sessionListenerRegistered = true;
            reloadActivePlayers();
        } catch (SecurityException error) {
            Log.w(TAG, "Oczekiwanie na dostep do powiadomien odtwarzaczy", error);
        }
    }

    private void reloadActivePlayers() {
        if (!running || mediaSessionManager == null) return;
        try {
            onActivePlayersChanged(
                    mediaSessionManager.getActiveSessions(notificationListenerComponent));
        } catch (SecurityException error) {
            Log.w(TAG, "Brak dostepu do aktywnych odtwarzaczy", error);
        }
    }

    private void onActivePlayersChanged(List<MediaController> controllers) {
        if (!running) return;
        Map<MediaSession.Token, ObservedPlayer> previous = new HashMap<>(observedPlayers);
        observedPlayers.clear();
        if (controllers != null) {
            for (MediaController controller : controllers) {
                if (getPackageName().equals(controller.getPackageName())) continue;
                MediaSession.Token token = controller.getSessionToken();
                ObservedPlayer player = previous.remove(token);
                if (player == null) {
                    player = new ObservedPlayer(controller);
                    controller.registerCallback(player, handler);
                }
                observedPlayers.put(token, player);
            }
        }
        for (ObservedPlayer player : previous.values()) {
            player.controller.unregisterCallback(player);
        }
        requestSessionPriorityRefresh();
    }

    private boolean isNightVolumeEnabledForCurrentOutput() {
        return PixelatePreferences.isAdvancedVolumeEnabled(this)
                && AudioOutputRouting.isBluetoothMediaOutput(audioManager);
    }

    private void scheduleAudioRouteRefresh() {
        handler.removeCallbacks(refreshAudioRouteRunnable);
        handler.postDelayed(refreshAudioRouteRunnable, AUDIO_ROUTE_SETTLE_MS);
    }

    private boolean dispatchToActivePlayer(int keyCode) {
        if (mediaSessionManager == null || notificationListenerComponent == null) return false;
        try {
            List<MediaController> controllers =
                    mediaSessionManager.getActiveSessions(notificationListenerComponent);
            MediaController selected = null;
            for (MediaController controller : controllers) {
                if (getPackageName().equals(controller.getPackageName())) continue;
                PlaybackState state = controller.getPlaybackState();
                if (state == null) continue;
                if (selected == null) selected = controller;
                int playbackState = state.getState();
                if (playbackState == PlaybackState.STATE_PLAYING
                        || playbackState == PlaybackState.STATE_BUFFERING
                        || playbackState == PlaybackState.STATE_CONNECTING) {
                    selected = controller;
                    break;
                }
            }
            if (selected == null) return false;
            long now = SystemClock.uptimeMillis();
            boolean downHandled = selected.dispatchMediaButtonEvent(new KeyEvent(
                    now, now, KeyEvent.ACTION_DOWN, keyCode, 0));
            boolean upHandled = selected.dispatchMediaButtonEvent(new KeyEvent(
                    now, now, KeyEvent.ACTION_UP, keyCode, 0));
            if (downHandled || upHandled) {
                Log.d(TAG, "Wyslano media key=" + keyCode
                        + " bezposrednio do " + selected.getPackageName());
                return true;
            }
        } catch (SecurityException error) {
            Log.w(TAG, "Brak dostepu do aktywnej sesji odtwarzacza", error);
        }
        return false;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Głośność i przyciski multimediów",
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Tryb nocny na słuchawkach Bluetooth i sterowanie na blokadzie");
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    private Notification buildNotification() {
        Intent launch = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, launch,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return builder
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Pixel Center — głośność i przyciski")
                .setContentText("Tryb nocny na słuchawkach BT i sterowanie na blokadzie")
                .setContentIntent(pendingIntent)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setOngoing(true)
                .build();
    }
}
