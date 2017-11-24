package le1.mediaplayback.callback;


import android.media.AudioManager;

/**
 * Callback of audio focus actions.
 * Joins the {@link AudioManager#ACTION_AUDIO_BECOMING_NOISY} calls
 * with the {@link AudioManager.OnAudioFocusChangeListener#onAudioFocusChange(int)} calls
 * in a user friendly interface.
 */
public interface AudioFocusCallback {

    /**
     * @see AudioManager#AUDIOFOCUS_GAIN
     */
    void onAudioFocusGain();

    /**
     * @see AudioManager#AUDIOFOCUS_LOSS
     */
    void onAudioFocusLoss();

    /**
     * @see AudioManager#AUDIOFOCUS_LOSS_TRANSIENT
     */
    void onAudioFocusLossTransient();

    /**
     * @see AudioManager#AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
     */
    void onAudioFocusLossTransientCanDuck();

    /**
     * @see AudioManager#ACTION_AUDIO_BECOMING_NOISY
     */
    void onAudioFocusBecomingNoisy();
}
