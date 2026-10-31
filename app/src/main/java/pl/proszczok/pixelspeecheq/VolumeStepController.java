package pl.proszczok.pixelspeecheq;

import android.content.Context;

/**
 * Converts a percentage step into the discrete media-volume levels exposed by Android.
 * Pixel 10 exposes 25 levels, so percentages such as 2% or 10% need error diffusion
 * across successive presses instead of being rounded to the same one-level step.
 */
final class VolumeStepController {
    private static int lastDirection;
    private static int lastRange = -1;
    private static int lastPercent = -1;
    private static int lastTarget = -1;
    private static float remainder;

    private VolumeStepController() {
    }

    static synchronized int nextVolume(
            Context context, int current, int min, int max, int direction) {
        int range = Math.max(1, max - min);
        int sign = direction > 0 ? 1 : -1;

        if (!PixelatePreferences.isAdvancedVolumeEnabled(context)) {
            reset();
            return clamp(current + sign, min, max);
        }

        int percent = PixelatePreferences.getVolumeStepPercent(context);
        if (sign != lastDirection || range != lastRange || percent != lastPercent
                || (lastTarget >= 0 && current != lastTarget)) {
            remainder = 0f;
        }

        float requestedLevels = range * percent / 100f + remainder;
        int wholeLevels = Math.round(requestedLevels);
        remainder = requestedLevels - wholeLevels;

        int next = clamp(current + sign * wholeLevels, min, max);
        if (next == min || next == max) {
            remainder = 0f;
        }
        lastDirection = sign;
        lastRange = range;
        lastPercent = percent;
        lastTarget = next;
        return next;
    }

    static synchronized void reset() {
        lastDirection = 0;
        lastRange = -1;
        lastPercent = -1;
        lastTarget = -1;
        remainder = 0f;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
