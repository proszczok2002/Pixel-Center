package pl.proszczok.pixelspeecheq;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.Equalizer;
import android.os.IBinder;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class EqService extends Service {
    public static final String ACTION_ENABLE_GLOBAL = "pl.proszczok.pixelspeecheq.ENABLE_GLOBAL";
    public static final String ACTION_DISABLE = "pl.proszczok.pixelspeecheq.DISABLE";
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
    private static final int GLOBAL_SESSION = 0;
    private static final int EFFECT_PRIORITY = 1000;

    private static final Set<String> SUPPORTED_PLAYERS = Set.of(
            "com.audioteka",
            "ak.alizandro.smartaudiobookplayer",
            "org.grakovne.lissen"
    );

    private final Map<Integer, Equalizer> sessionEffects = new HashMap<>();
    private Equalizer globalEffect;
    private SharedPreferences prefs;
    private int preset = 1;

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
        if (intent == null) {
            if (prefs.getBoolean(PREF_ENABLED, false)) {
                enableGlobal();
            }
            return START_STICKY;
        }

        String action = intent.getAction();
        if (ACTION_ENABLE_GLOBAL.equals(action)) {
            prefs.edit().putBoolean(PREF_ENABLED, true).apply();
            enableGlobal();
        } else if (ACTION_DISABLE.equals(action)) {
            prefs.edit().putBoolean(PREF_ENABLED, false).apply();
            releaseAll();
            updateStatus("EQ wyłączony");
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
        } else if (ACTION_SET_PRESET.equals(action)) {
            preset = clampPreset(intent.getIntExtra(EXTRA_PRESET, 1));
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
        return START_STICKY;
    }

    private void enableGlobal() {
        startForeground(NOTIFICATION_ID, buildNotification("Profil mowy aktywny"));
        if (globalEffect != null) {
            applyPreset(globalEffect);
            return;
        }
        try {
            globalEffect = new Equalizer(EFFECT_PRIORITY, GLOBAL_SESSION);
            applyPreset(globalEffect);
            updateStatus(describeEffect("Globalny EQ", globalEffect));
        } catch (RuntimeException error) {
            globalEffect = null;
            updateStatus("Globalny EQ niedostępny; czekam na sesję odtwarzacza: " + error.getClass().getSimpleName());
            Log.w(TAG, "Global equalizer unavailable", error);
        }
    }

    private void attachSession(int sessionId, String packageName) {
        startForeground(NOTIFICATION_ID, buildNotification("EQ: " + shortName(packageName)));
        Equalizer existing = sessionEffects.get(sessionId);
        if (existing != null) {
            applyPreset(existing);
            return;
        }
        try {
            Equalizer equalizer = new Equalizer(EFFECT_PRIORITY, sessionId);
            applyPreset(equalizer);
            sessionEffects.put(sessionId, equalizer);
            updateStatus(describeEffect(shortName(packageName) + " / sesja " + sessionId, equalizer));
        } catch (RuntimeException error) {
            updateStatus("Nie udało się podpiąć sesji " + sessionId + ": " + error.getClass().getSimpleName());
            Log.w(TAG, "Session equalizer unavailable: " + sessionId, error);
        }
    }

    private void applyPreset(Equalizer equalizer) {
        short[][] profiles = new short[][] {
                { -500, -300, -100, 0, 0 },
                { -800, -500, -250, -150, -150 },
                { -1200, -900, -600, -450, -450 }
        };
        short[] requested = profiles[clampPreset(preset)];
        short min = equalizer.getBandLevelRange()[0];
        short max = equalizer.getBandLevelRange()[1];
        short bandCount = equalizer.getNumberOfBands();
        for (short band = 0; band < bandCount; band++) {
            int sourceIndex = Math.min(band, requested.length - 1);
            short level = (short) Math.max(min, Math.min(max, requested[sourceIndex]));
            equalizer.setBandLevel(band, level);
        }
        equalizer.setEnabled(true);
    }

    private void reapplyAll() {
        if (globalEffect != null) {
            applyPreset(globalEffect);
        }
        for (Equalizer equalizer : sessionEffects.values()) {
            applyPreset(equalizer);
        }
        String current = prefs.getString(PREF_STATUS, "EQ aktywny");
        updateStatus(current + " | preset: " + presetName());
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

    private void releaseSession(int sessionId) {
        Equalizer equalizer = sessionEffects.remove(sessionId);
        if (equalizer != null) {
            equalizer.release();
        }
    }

    private void releaseAll() {
        if (globalEffect != null) {
            globalEffect.release();
            globalEffect = null;
        }
        for (Equalizer equalizer : sessionEffects.values()) {
            equalizer.release();
        }
        sessionEffects.clear();
    }

    private int clampPreset(int value) {
        return Math.max(0, Math.min(2, value));
    }

    private String presetName() {
        return switch (preset) {
            case 0 -> "lekki";
            case 2 -> "mocny";
            default -> "średni";
        };
    }

    private String shortName(String packageName) {
        if ("com.audioteka".equals(packageName)) return "Audioteka";
        if ("ak.alizandro.smartaudiobookplayer".equals(packageName)) return "Smart AudioBook Player";
        if ("org.grakovne.lissen".equals(packageName)) return "Lissen";
        return packageName;
    }

    private void updateStatus(String status) {
        prefs.edit().putString(PREF_STATUS, status).apply();
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.notify(NOTIFICATION_ID, buildNotification(status));
        Log.i(TAG, status);
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Korektor audiobooków",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("Informuje, gdy profil mowy jest aktywny");
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    private Notification buildNotification(String text) {
        Intent activityIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, activityIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
                .setContentTitle("Pixel Center")
                .setContentText(text)
                .setContentIntent(pendingIntent)
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
