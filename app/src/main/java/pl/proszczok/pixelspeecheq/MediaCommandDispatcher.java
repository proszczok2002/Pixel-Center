package pl.proszczok.pixelspeecheq;

import android.content.ComponentName;
import android.content.Context;
import android.media.AudioManager;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.SystemClock;
import android.os.Build;
import android.util.Log;
import android.view.KeyEvent;

import java.util.List;

final class MediaCommandDispatcher {
    private static final String TAG = "PixelCenterMedia";

    private MediaCommandDispatcher() {
    }

    static boolean dispatchToActivePlayer(Context context, int keyCode) {
        MediaSessionManager manager = context.getSystemService(MediaSessionManager.class);
        if (manager == null) return false;
        try {
            return dispatch(context, manager, keyCode);
        } catch (SecurityException error) {
            return false;
        }
    }

    static void dispatchGlobal(AudioManager audioManager, int keyCode) {
        if (audioManager == null) return;
        long now = SystemClock.uptimeMillis();
        audioManager.dispatchMediaKeyEvent(new KeyEvent(
                now, now, KeyEvent.ACTION_DOWN, keyCode, 0));
        audioManager.dispatchMediaKeyEvent(new KeyEvent(
                now, now, KeyEvent.ACTION_UP, keyCode, 0));
    }

    private static boolean dispatch(
            Context context, MediaSessionManager manager, int keyCode) {
        return send(selectPlayer(context, manager), keyCode);
    }

    private static MediaController selectPlayer(
            Context context, MediaSessionManager manager) {
        ComponentName listener = new ComponentName(context, PlayerMonitorService.class);
        MediaSession.Token systemPlayer = Build.VERSION.SDK_INT >= 33
                ? manager.getMediaKeyEventSession() : null;
        return selectPlayer(context, manager.getActiveSessions(listener), systemPlayer);
    }

    private static MediaController selectPlayer(
            Context context, List<MediaController> controllers, MediaSession.Token systemPlayer) {
        if (controllers == null || controllers.isEmpty()) return null;
        MediaController selected = null;
        MediaController systemSelected = null;
        for (MediaController controller : controllers) {
            if (context.getPackageName().equals(controller.getPackageName())) continue;
            if (selected == null) selected = controller;
            if (isPlaying(controller.getPlaybackState())) {
                return controller;
            }
            if (controller.getSessionToken().equals(systemPlayer)) {
                systemSelected = controller;
            }
        }
        // With playback paused, use the player Android routes headset keys to.
        // Old saved audiobook preferences must not override YouTube or music.
        return systemSelected != null ? systemSelected : selected;
    }

    private static boolean send(MediaController controller, int keyCode) {
        return controller != null
                && (dispatchExplicitPlayPauseButton(controller, keyCode)
                || dispatchTransportCommand(controller, keyCode)
                || dispatchMediaButton(controller, keyCode));
    }

    private static boolean dispatchExplicitPlayPauseButton(
            MediaController controller, int keyCode) {
        if (keyCode != KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) return false;
        PlaybackState state = controller.getPlaybackState();
        if (state == null) return false;
        int explicitKey = isPlaying(state)
                ? KeyEvent.KEYCODE_MEDIA_PAUSE : KeyEvent.KEYCODE_MEDIA_PLAY;
        boolean handled = dispatchMediaButton(controller, explicitKey);
        if (handled) {
            Log.d(TAG, "Explicit command " + explicitKey + " -> "
                    + controller.getPackageName());
        }
        return handled;
    }

    private static boolean dispatchTransportCommand(
            MediaController controller, int keyCode) {
        PlaybackState state = controller.getPlaybackState();
        if (state == null) return false;
        boolean handled = dispatchTransportCommand(controller, state, keyCode);
        if (handled) {
            Log.d(TAG, "Direct command " + keyCode + " -> "
                    + controller.getPackageName());
        }
        return handled;
    }

    private static boolean dispatchTransportCommand(
            MediaController controller, PlaybackState state, int keyCode) {
        long actions = state.getActions();
        MediaController.TransportControls controls = controller.getTransportControls();
        if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT
                && supports(actions, PlaybackState.ACTION_SKIP_TO_NEXT)) {
            controls.skipToNext();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS
                && supports(actions, PlaybackState.ACTION_SKIP_TO_PREVIOUS)) {
            controls.skipToPrevious();
            return true;
        }
        return dispatchPlayPause(controls, state, actions, keyCode);
    }

    private static boolean dispatchPlayPause(
            MediaController.TransportControls controls, PlaybackState state,
            long actions, int keyCode) {
        if (keyCode != KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) return false;
        if (isPlaying(state) && supports(actions, PlaybackState.ACTION_PAUSE)) {
            controls.pause();
            return true;
        }
        if (!isPlaying(state) && supports(actions, PlaybackState.ACTION_PLAY)) {
            controls.play();
            return true;
        }
        return false;
    }

    private static boolean dispatchMediaButton(
            MediaController controller, int keyCode) {
        long now = SystemClock.uptimeMillis();
        boolean down = controller.dispatchMediaButtonEvent(new KeyEvent(
                now, now, KeyEvent.ACTION_DOWN, keyCode, 0));
        boolean up = controller.dispatchMediaButtonEvent(new KeyEvent(
                now, now, KeyEvent.ACTION_UP, keyCode, 0));
        boolean handled = down || up;
        if (handled) {
            Log.d(TAG, "Media key " + keyCode + " -> "
                    + controller.getPackageName());
        }
        return handled;
    }

    private static boolean supports(long actions, long action) {
        return (actions & action) != 0;
    }

    private static boolean isPlaying(PlaybackState state) {
        if (state == null) return false;
        int value = state.getState();
        return value == PlaybackState.STATE_PLAYING
                || value == PlaybackState.STATE_BUFFERING
                || value == PlaybackState.STATE_CONNECTING;
    }
}
