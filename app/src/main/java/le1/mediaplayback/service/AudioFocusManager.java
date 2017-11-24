package le1.mediaplayback.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;

import le1.mediaplayback.callback.AudioFocusCallback;

/**
 * Controls both audio focus and {@link AudioManager#ACTION_AUDIO_BECOMING_NOISY}
 */
class AudioFocusManager implements AudioManager.OnAudioFocusChangeListener {

    private final AudioManager audioManager;
    private final AudioFocusCallback callback;
    private Context context;

    /**
     * Called when there is a sudden change in audio output
     */
    private final BroadcastReceiver noisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            callback.onAudioFocusBecomingNoisy();
        }
    };

    AudioFocusManager(Context context, AudioFocusCallback callback) {
        this.context = context.getApplicationContext();
        audioManager = (AudioManager) this.context
                .getSystemService(Context.AUDIO_SERVICE);
        this.callback = callback;
    }

    /**
     * Send a request to obtain audio focus and register {@link #noisyReceiver}
     * @return true if audio focus is given, false otherwise
     */
    boolean requestAudioFocus() {
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        context.registerReceiver(noisyReceiver, intentFilter);

        int result = audioManager.requestAudioFocus(this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    /**
     * abandon audio focus and unregister {@link #noisyReceiver}
     */
    void abandonAudioFocus() {
        audioManager.abandonAudioFocus(this);
        try {
            context.unregisterReceiver(noisyReceiver);
        } catch (Exception ignored) {
            //there is no way to know if receiver is registered
        }
    }

    /**
     * @see AudioManager.OnAudioFocusChangeListener#onAudioFocusChange(int)
     */
    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                callback.onAudioFocusGain();
                return;
            case AudioManager.AUDIOFOCUS_LOSS:
                callback.onAudioFocusLoss();
                return;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                callback.onAudioFocusLossTransient();
                return;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                callback.onAudioFocusLossTransientCanDuck();
                return;
            default:
                break;
        }
    }
}
