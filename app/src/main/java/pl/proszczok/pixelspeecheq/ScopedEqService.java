package pl.proszczok.pixelspeecheq;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.audiofx.Equalizer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class ScopedEqService extends Service {
    public static final String ACTION_ENABLE_GLOBAL = "pl.proszczok.pixelspeecheq.ENABLE_MASTER";
    public static final String ACTION_DISABLE = "pl.proszczok.pixelspeecheq.DISABLE_MASTER";
    public static final String ACTION_ACTIVATE_FOR_PLAYER = "pl.proszczok.pixelspeecheq.ACTIVATE_FOR_PLAYER";
    public static final String ACTION_SUSPEND = "pl.proszczok.pixelspeecheq.SUSPEND";
    public static final String ACTION_ATTACH_SESSION = "pl.proszczok.pixelspeecheq.ATTACH_SESSION";
    public static final String ACTION_DETACH_SESSION = "pl.proszczok.pixelspeecheq.DETACH_SESSION";
    public static final String ACTION_SET_PRESET = "pl.proszczok.pixelspeecheq.SET_PRESET";
    public static final String EXTRA_SESSION_ID = "audio_session_id";
    public static final String EXTRA_PLAYER_PACKAGE = "player_package";
    public static final String EXTRA_PRESET = "preset";

    public static final String PREFS = "eq_state";
    public static final String PREF_ENABLED = "enabled";
    public static final String PREF_PRESET = "preset";
    public static final String PREF_STATUS = "status";

    private static final String TAG = "PixelSpeechEQ";
    private static final String CHANNEL_ID = "speech_eq";
    private static final int NOTIFICATION_ID = 1017;
    private static final int EFFECT_PRIORITY = 1000;
    private static final int RAMP_STEPS = 6;
    private static final long RAMP_STEP_MS = 30;
    private static final Set<String> SUPPORTED_PLAYERS = Set.of(
            "com.audioteka",
            "ak.alizandro.smartaudiobookplayer",
            "org.grakovne.lissen"
    );

    private final Map<Integer, Equalizer> sessionEffects = new HashMap<>();
    private final Map<Equalizer, Integer> rampTokens = new HashMap<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Equalizer globalEffect;
    private SharedPreferences prefs;
    private int preset;
    private int nextRampToken;
    private boolean effectActive;

    public static boolean isSupportedPlayer(String packageName) {
        return packageName != null && SUPPORTED_PLAYERS.contains(packageName);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        preset = prefs.getInt(PREF_PRESET, 1);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) return START_NOT_STICKY;
        String action = intent.getAction();

        if (ACTION_ENABLE_GLOBAL.equals(action)) {
            prefs.edit().putBoolean(PREF_ENABLED, true).apply();
            saveStatus("Automatyczny EQ włączony — czekam na obsługiwany odtwarzacz");
            stopSelf();
        } else if (ACTION_DISABLE.equals(action)) {
            prefs.edit().putBoolean(PREF_ENABLED, false).apply();
            stopEq("EQ wyłączony");
        } else if (ACTION_ACTIVATE_FOR_PLAYER.equals(action)) {
            String packageName = intent.getStringExtra(EXTRA_PLAYER_PACKAGE);
            if (prefs.getBoolean(PREF_ENABLED, false) && isSupportedPlayer(packageName)) {
                enableGlobal(packageName);
            } else {
                stopSelf();
            }
        } else if (ACTION_SUSPEND.equals(action)) {
            stopEq("EQ oczekuje — żadna obsługiwana aplikacja nie odtwarza");
        } else if (ACTION_SET_PRESET.equals(action)) {
            preset = Math.max(0, Math.min(2, intent.getIntExtra(EXTRA_PRESET, 1)));
            prefs.edit().putInt(PREF_PRESET, preset).apply();
            reapplyAll();
        } else if (ACTION_ATTACH_SESSION.equals(action)) {
            int sessionId = intent.getIntExtra(EXTRA_SESSION_ID, -1);
            String packageName = intent.getStringExtra(EXTRA_PLAYER_PACKAGE);
            if (prefs.getBoolean(PREF_ENABLED, false) && sessionId > 0 && isSupportedPlayer(packageName)) {
                attachSession(sessionId, packageName);
            }
        } else if (ACTION_DETACH_SESSION.equals(action)) {
            releaseSession(intent.getIntExtra(EXTRA_SESSION_ID, -1));
        }
        return START_NOT_STICKY;
    }

    private void enableGlobal(String packageName) {
        if (!sessionEffects.isEmpty()) return;
        if (globalEffect != null && effectActive) return;
        Log.i(TAG, "EQ activation started for " + shortName(packageName));
        startEqForeground("EQ: " + shortName(packageName));
        if (globalEffect == null) {
            try {
                globalEffect = new Equalizer(EFFECT_PRIORITY, 0);
            } catch (RuntimeException error) {
                saveStatus("Globalny EQ niedostępny: " + error.getClass().getSimpleName());
                Log.w(TAG, "Global EQ unavailable", error);
                return;
            }
        }
        Equalizer effect = globalEffect;
        applyPresetSmooth(effect,
                () -> saveStatus(describeEffect("EQ: " + shortName(packageName), effect)));
    }

    private void attachSession(int sessionId, String packageName) {
        if (globalEffect != null && effectActive) {
            Log.i(TAG, "Keeping global EQ; ignoring late session " + sessionId
                    + " from " + shortName(packageName));
            return;
        }
        startEqForeground("EQ: " + shortName(packageName));
        releaseGlobal();
        Equalizer equalizer = sessionEffects.get(sessionId);
        if (equalizer == null) {
            try {
                equalizer = new Equalizer(EFFECT_PRIORITY, sessionId);
                sessionEffects.put(sessionId, equalizer);
            } catch (RuntimeException error) {
                saveStatus("Nie udało się podpiąć sesji " + sessionId + ": " + error.getClass().getSimpleName());
                Log.w(TAG, "Session EQ unavailable", error);
                return;
            }
        }
        Equalizer effect = equalizer;
        applyPresetSmooth(effect,
                () -> saveStatus(describeEffect(
                        shortName(packageName) + " / sesja " + sessionId, effect)));
    }

    private void applyPreset(Equalizer equalizer) {
        applyPreset(equalizer, false, null);
    }

    private void applyPresetSmooth(Equalizer equalizer, Runnable onComplete) {
        applyPreset(equalizer, true, onComplete);
    }

    private void applyPreset(Equalizer equalizer, boolean smooth, Runnable onComplete) {
        short[][] profiles = {
                { -500, -300, -100, 0, 0 },
                { -800, -500, -250, -150, -150 },
                { -1200, -900, -600, -450, -450 }
        };
        short[] requested = profiles[preset];
        short min = equalizer.getBandLevelRange()[0];
        short max = equalizer.getBandLevelRange()[1];
        short bandCount = equalizer.getNumberOfBands();
        short[] starts = new short[bandCount];
        short[] targets = new short[bandCount];
        boolean alreadyEnabled = equalizer.getEnabled();
        for (short band = 0; band < bandCount; band++) {
            starts[band] = alreadyEnabled ? equalizer.getBandLevel(band) : 0;
            short requestedLevel = requested[Math.min(band, requested.length - 1)];
            targets[band] = (short) Math.max(min, Math.min(max, requestedLevel));
        }

        int token = ++nextRampToken;
        rampTokens.put(equalizer, token);
        if (!smooth) {
            setLevels(equalizer, targets);
            equalizer.setEnabled(true);
            rampTokens.remove(equalizer);
            if (onComplete != null) onComplete.run();
            return;
        }

        setLevels(equalizer, starts);
        equalizer.setEnabled(true);
        for (int step = 1; step <= RAMP_STEPS; step++) {
            final int rampStep = step;
            handler.postDelayed(() -> {
                Integer activeToken = rampTokens.get(equalizer);
                if (activeToken == null || activeToken != token || !ownsEqualizer(equalizer)) return;
                short[] levels = new short[bandCount];
                for (int band = 0; band < bandCount; band++) {
                    int delta = targets[band] - starts[band];
                    levels[band] = (short) (starts[band] + delta * rampStep / RAMP_STEPS);
                }
                try {
                    setLevels(equalizer, levels);
                    if (rampStep == RAMP_STEPS) {
                        rampTokens.remove(equalizer);
                        if (onComplete != null) onComplete.run();
                    }
                } catch (RuntimeException error) {
                    rampTokens.remove(equalizer);
                    Log.w(TAG, "EQ ramp interrupted", error);
                }
            }, rampStep * RAMP_STEP_MS);
        }
    }

    private void setLevels(Equalizer equalizer, short[] levels) {
        for (short band = 0; band < levels.length; band++) {
            equalizer.setBandLevel(band, levels[band]);
        }
    }

    private boolean ownsEqualizer(Equalizer equalizer) {
        return equalizer == globalEffect || sessionEffects.containsValue(equalizer);
    }

    private void reapplyAll() {
        if (globalEffect != null) applyPresetSmooth(globalEffect, null);
        for (Equalizer equalizer : sessionEffects.values()) applyPreset(equalizer);
        saveStatus(effectActive ? "EQ aktywny | preset: " + presetName()
                : "Preset: " + presetName() + " — czekam na odtwarzacz");
    }

    private String describeEffect(String target, Equalizer equalizer) {
        StringBuilder bands = new StringBuilder();
        for (short band = 0; band < equalizer.getNumberOfBands(); band++) {
            if (band > 0) bands.append(", ");
            bands.append(equalizer.getCenterFreq(band) / 1000).append("Hz=")
                    .append(equalizer.getBandLevel(band) / 100.0f).append("dB");
        }
        return target + " | sterowanie=" + equalizer.hasControl() + " | " + bands;
    }

    private void startEqForeground(String text) {
        effectActive = true;
        startForeground(NOTIFICATION_ID, buildNotification(text));
    }

    private void stopEq(String status) {
        releaseAll();
        saveStatus(status);
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private void releaseSession(int sessionId) {
        Equalizer equalizer = sessionEffects.remove(sessionId);
        if (equalizer != null) {
            rampTokens.remove(equalizer);
            equalizer.release();
        }
    }

    private void releaseGlobal() {
        if (globalEffect != null) {
            rampTokens.remove(globalEffect);
            globalEffect.release();
            globalEffect = null;
        }
    }

    private void releaseAll() {
        handler.removeCallbacksAndMessages(null);
        rampTokens.clear();
        releaseGlobal();
        for (Equalizer equalizer : sessionEffects.values()) equalizer.release();
        sessionEffects.clear();
        effectActive = false;
    }

    private String presetName() {
        if (preset == 0) return "lekki";
        if (preset == 2) return "mocny";
        return "średni";
    }

    private String shortName(String packageName) {
        if ("com.audioteka".equals(packageName)) return "Audioteka";
        if ("ak.alizandro.smartaudiobookplayer".equals(packageName)) return "Smart AudioBook Player";
        if ("org.grakovne.lissen".equals(packageName)) return "Lissen";
        return packageName;
    }

    private void saveStatus(String status) {
        prefs.edit().putString(PREF_STATUS, status).apply();
        if (effectActive) {
            getSystemService(NotificationManager.class).notify(NOTIFICATION_ID, buildNotification(status));
        }
        Log.i(TAG, status);
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Korektor audiobooków", NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    private Notification buildNotification(String text) {
        PendingIntent openApp = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
                .setContentTitle("Pixel Center")
                .setContentText(text)
                .setContentIntent(openApp)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        releaseAll();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
