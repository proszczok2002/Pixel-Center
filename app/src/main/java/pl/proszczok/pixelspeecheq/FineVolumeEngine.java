package pl.proszczok.pixelspeecheq;

import android.content.Context;
import android.media.AudioManager;
import android.media.audiofx.DynamicsProcessing;
import android.util.Log;

final class FineVolumeEngine {
    static final int MAX_PERCENT = 100;
    private static final String TAG = "PixelCenterNight";
    private static final int EFFECT_PRIORITY = 1000;

    static final class State {
        final int percent;
        final int physicalLevel;
        final int physicalMax;
        final float attenuationDb;
        final boolean fineAttenuationActive;
        final boolean nightLevelActive;

        State(int percent, int physicalLevel, int physicalMax,
                float attenuationDb, boolean fineAttenuationActive,
                boolean nightLevelActive) {
            this.percent = percent;
            this.physicalLevel = physicalLevel;
            this.physicalMax = physicalMax;
            this.attenuationDb = attenuationDb;
            this.fineAttenuationActive = fineAttenuationActive;
            this.nightLevelActive = nightLevelActive;
        }
    }

    private final Context context;
    private final AudioManager audioManager;
    private DynamicsProcessing attenuationEffect;
    private int virtualPercent = -1;
    private int lastPhysicalLevel = -1;

    FineVolumeEngine(Context context, AudioManager audioManager) {
        this.context = context.getApplicationContext();
        this.audioManager = audioManager;
    }

    State initialize() {
        int min = audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC);
        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int physical = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int savedPercent = PixelatePreferences.getFineVolumePercent(context);
        int savedPhysical = PixelatePreferences.getFineVolumePhysicalLevel(context);
        if (savedPercent >= 0 && savedPhysical == physical) {
            virtualPercent = normalizeSavedPercent(savedPercent, physical, min, max);
        } else {
            virtualPercent = percentFromPhysical(physical, min, max);
        }
        return apply(virtualPercent, min, max);
    }

    State adjustNightStep(int direction) {
        syncWithExternalVolume();
        int min = audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC);
        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int physical = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int firstPercent = firstSystemPercent(min, max);
        int nightPercent = nightPercent(min, max);

        if (direction > 0) {
            if (virtualPercent <= 0) return setPercentFromUser(nightPercent);
            if (virtualPercent < firstPercent) return setPercentFromUser(firstPercent);
            int nextPhysical = Math.min(max, Math.max(min + 1, physical + 1));
            return setPercentFromUser(percentFromPhysical(nextPhysical, min, max));
        }
        if (direction < 0) {
            if (virtualPercent <= 0) return setPercentFromUser(0);
            if (virtualPercent < firstPercent) return setPercentFromUser(0);
            if (physical <= min + 1) return setPercentFromUser(nightPercent);
            int nextPhysical = Math.max(min + 1, physical - 1);
            return setPercentFromUser(percentFromPhysical(nextPhysical, min, max));
        }
        return apply(virtualPercent, min, max, AudioManager.FLAG_SHOW_UI);
    }

    State setPercent(int requestedPercent) {
        return setPercent(requestedPercent, 0);
    }

    private State setPercentFromUser(int requestedPercent) {
        return setPercent(requestedPercent, AudioManager.FLAG_SHOW_UI);
    }

    private State setPercent(int requestedPercent, int flags) {
        int min = audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC);
        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        virtualPercent = Math.max(0, Math.min(MAX_PERCENT, requestedPercent));
        return apply(virtualPercent, min, max, flags);
    }

    State handleExternalPhysicalChange(int previousPhysical, int physical) {
        int min = audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC);
        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int safePhysical = Math.max(min, Math.min(max, physical));
        if (virtualPercent < 0) initialize();
        if (safePhysical == lastPhysicalLevel) return null;

        int firstPhysical = Math.min(max, min + 1);
        int firstPercent = firstSystemPercent(min, max);
        if (previousPhysical == firstPhysical && safePhysical == min) {
            return virtualPercent >= firstPercent
                    ? setPercent(nightPercent(min, max))
                    : setPercent(0);
        }
        if (previousPhysical == min && safePhysical == firstPhysical
                && virtualPercent <= 0) {
            return setPercent(nightPercent(min, max));
        }
        if (previousPhysical == firstPhysical
                && safePhysical == Math.min(max, firstPhysical + 1)
                && virtualPercent > 0 && virtualPercent < firstPercent) {
            return setPercent(firstPercent);
        }

        virtualPercent = percentFromPhysical(safePhysical, min, max);
        return apply(virtualPercent, min, max);
    }

    int getPercent() {
        if (virtualPercent < 0) initialize();
        return virtualPercent;
    }

    int getPhysicalLevel() {
        if (virtualPercent < 0 || lastPhysicalLevel < 0) initialize();
        return lastPhysicalLevel;
    }

    void disableAndForget() {
        disableEffect();
        virtualPercent = -1;
        lastPhysicalLevel = -1;
        PixelatePreferences.clearFineVolumeState(context);
    }

    void release() {
        disableEffect();
        if (attenuationEffect != null) {
            attenuationEffect.release();
            attenuationEffect = null;
        }
    }

    private void syncWithExternalVolume() {
        int min = audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC);
        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int physical = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (virtualPercent < 0) {
            initialize();
        } else if (lastPhysicalLevel >= 0 && physical != lastPhysicalLevel) {
            virtualPercent = percentFromPhysical(physical, min, max);
            disableEffect();
            PixelatePreferences.saveFineVolumeState(context, virtualPercent, physical);
            lastPhysicalLevel = physical;
        }
    }

    private State apply(int percent, int min, int max) {
        return apply(percent, min, max, 0);
    }

    private State apply(int percent, int min, int max, int flags) {
        int range = Math.max(1, max - min);
        int physical;
        float attenuationDb = 0f;
        if (percent <= 0) {
            physical = min;
        } else {
            float exactLevel = min + range * percent / 100f;
            physical = Math.max(min + 1, Math.min(max, (int) Math.ceil(exactLevel)));
            float ceilingPercent = (physical - min) * 100f / range;
            attenuationDb = (float) (20.0 * Math.log10(percent / ceilingPercent));
            attenuationDb = Math.min(0f, Math.max(-30f, attenuationDb));
        }

        boolean needsFineAttenuation = percent > 0 && attenuationDb < -0.05f;
        int currentPhysical = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (physical > currentPhysical && needsFineAttenuation) {
            enableAttenuation(attenuationDb);
        }
        if (currentPhysical != physical || flags != 0) {
            // A virtual night step can keep the same physical level. Passing
            // FLAG_SHOW_UI even then keeps Android's normal volume panel visible.
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, physical, flags);
        }
        if (needsFineAttenuation) {
            enableAttenuation(attenuationDb);
        } else {
            disableEffect();
        }

        virtualPercent = percent;
        lastPhysicalLevel = physical;
        PixelatePreferences.saveFineVolumeState(context, percent, physical);
        boolean active = attenuationEffect != null && attenuationEffect.getEnabled();
        boolean nightActive = active
                && physical == min + 1
                && percent == nightPercent(min, max);
        Log.d(TAG, "Poziom " + percent + "% -> fizyczny " + physical + "/" + max
                + ", tlumienie=" + attenuationDb + " dB, active=" + active);
        return new State(percent, physical, max, attenuationDb, active, nightActive);
    }

    private void enableAttenuation(float attenuationDb) {
        try {
            if (attenuationEffect == null) {
                attenuationEffect = new DynamicsProcessing(EFFECT_PRIORITY, 0, null);
            }
            attenuationEffect.setInputGainAllChannelsTo(attenuationDb);
            if (!attenuationEffect.getEnabled()) {
                attenuationEffect.setEnabled(true);
            }
        } catch (RuntimeException error) {
            Log.e(TAG, "Nie udalo sie wlaczyc tlumienia nocnego", error);
            if (attenuationEffect != null) {
                try {
                    attenuationEffect.release();
                } catch (RuntimeException ignored) {
                }
                attenuationEffect = null;
            }
        }
    }

    private void disableEffect() {
        if (attenuationEffect == null) return;
        try {
            if (attenuationEffect.getEnabled()) {
                attenuationEffect.setEnabled(false);
            }
        } catch (RuntimeException error) {
            Log.w(TAG, "Nie udalo sie wylaczyc tlumienia", error);
        }
    }

    private static int percentFromPhysical(int physical, int min, int max) {
        int range = Math.max(1, max - min);
        return Math.max(0, Math.min(MAX_PERCENT,
                Math.round((physical - min) * 100f / range)));
    }

    private static int firstSystemPercent(int min, int max) {
        return percentFromPhysical(Math.min(max, min + 1), min, max);
    }

    private static int nightPercent(int min, int max) {
        return Math.max(1, Math.round(firstSystemPercent(min, max) / 2f));
    }

    private static int normalizeSavedPercent(int savedPercent, int physical,
            int min, int max) {
        if (physical <= min || savedPercent <= 0) return 0;
        int firstPercent = firstSystemPercent(min, max);
        if (physical == min + 1 && savedPercent < firstPercent) {
            return nightPercent(min, max);
        }
        return percentFromPhysical(physical, min, max);
    }
}
