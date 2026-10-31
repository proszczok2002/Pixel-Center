package pl.proszczok.pixelspeecheq;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.audiofx.AudioEffect;

public final class AudioSessionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        int sessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, -1);
        String packageName = intent.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME);
        if (sessionId <= 0 || !ScopedEqService.isSupportedPlayer(packageName)) {
            return;
        }

        Intent serviceIntent = new Intent(context, ScopedEqService.class);
        serviceIntent.putExtra(ScopedEqService.EXTRA_SESSION_ID, sessionId);
        serviceIntent.putExtra(ScopedEqService.EXTRA_PLAYER_PACKAGE, packageName);
        if (AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION.equals(action)) {
            serviceIntent.setAction(ScopedEqService.ACTION_ATTACH_SESSION);
        } else if (AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION.equals(action)) {
            serviceIntent.setAction(ScopedEqService.ACTION_DETACH_SESSION);
        } else {
            return;
        }
        context.startForegroundService(serviceIntent);
    }
}
