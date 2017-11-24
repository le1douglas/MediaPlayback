package le1.mediaplayback.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

/**
 * Manager of {@link MediaSessionCompat}.
 * Every action related to {@link MediaSessionCompat} passes through this class
 */
class MediaSessionManager {
    private static final String TAG = "MediaSessionManager";
    private static final long PLAYBACK_SPEED_NORMAL = 1;

    private final MediaSessionCompat mediaSession;
    private final PlaybackStateCompat.Builder playbackState = new PlaybackStateCompat.Builder();


    /**
     * Build {@link #mediaSession} instance
     * @param context Application context
     * @param callback {@link MediaSessionCompat.Callback} of the {@link #mediaSession}
     */
    public MediaSessionManager(Context context, MediaSessionCompat.Callback callback) {
        mediaSession = new MediaSessionCompat(context.getApplicationContext(), TAG);
        mediaSession.setCallback(callback);
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        // set supported actions. if action is not specified here it won't do anything
        // when called through mediaSession.getController().getTransportControls()
        playbackState.setActions(PlaybackStateCompat.ACTION_PLAY |
                PlaybackStateCompat.ACTION_PLAY_PAUSE |
                PlaybackStateCompat.ACTION_PAUSE |
                PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID |
                PlaybackStateCompat.ACTION_PREPARE |
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                PlaybackStateCompat.ACTION_STOP);
    }


    /**
     * The state must be one of the following:
     * {@link PlaybackStateCompat#STATE_NONE}
     * {@link PlaybackStateCompat#STATE_STOPPED}
     * {@link PlaybackStateCompat#STATE_PLAYING}
     * {@link PlaybackStateCompat#STATE_PAUSED}
     * {@link PlaybackStateCompat#STATE_FAST_FORWARDING}
     * {@link PlaybackStateCompat#STATE_REWINDING}
     * {@link PlaybackStateCompat#STATE_BUFFERING}
     * {@link PlaybackStateCompat#STATE_ERROR}
     * {@link PlaybackStateCompat#STATE_CONNECTING}
     * {@link PlaybackStateCompat#STATE_SKIPPING_TO_PREVIOUS}
     * {@link PlaybackStateCompat#STATE_SKIPPING_TO_NEXT}
     * {@link PlaybackStateCompat#STATE_SKIPPING_TO_QUEUE_ITEM}
     *
     * for further info see {@link MediaSessionCompat#setPlaybackState(PlaybackStateCompat)}
     */
    void setPlaybackState(int state, long playerCurrentPosition) {
        playbackState.setState(state, playerCurrentPosition, PLAYBACK_SPEED_NORMAL);
        mediaSession.setPlaybackState(playbackState.build());
    }

    /**
     * @return Current playback state. see {@link #setPlaybackState(int, long)}
     */
    int getPlaybackState() {
        return mediaSession.getController().getPlaybackState().getState();
    }


    /**
     * @return {@link #mediaSession}'s playback controls
     * @see  MediaControllerCompat.TransportControls
     */
    MediaControllerCompat.TransportControls getControls() {
        return mediaSession.getController().getTransportControls();
    }

    /**
     * Set metadata of notification, wear etc
     * @param title The title of the track
     * @param id The YouTube id of this track
     * @param artist Author or YouTube channel that uploaded the video
     * @param art Bitmap of the album/video art, keep this image at a maximum of 4000x4000
     * @param artUri Uri of the album/video art, useful to get higher resolution image
     * @param duration The duration of the track, in seconds
     */
    void setMetadata(String title, String id, String artist, Bitmap art, String artUri, long duration) {
        MediaMetadataCompat.Builder metadata = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, id)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, art)
                .putString(MediaMetadataCompat.METADATA_KEY_ART_URI, artUri)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration);
        mediaSession.setMetadata(metadata.build());

    }

    /**
     * @return Metadata of current track
     */
    MediaMetadataCompat getMetadata() {
        return mediaSession.getController().getMetadata();
    }

    /**
     * @return {@link #mediaSession}'s token
     */
    MediaSessionCompat.Token getToken() {
        return mediaSession.getSessionToken();
    }

    /**
     * @see MediaSessionCompat#setActive(boolean)
     * should be called right after gaining audio focus
     */
    void setActive() {
        mediaSession.setActive(true);
    }

    /**
     * @see MediaSessionCompat#setActive(boolean)
     * should be called right after losing audio focus
     */
    void setInactive() {
        mediaSession.setActive(false);
    }

    /**
     * @see MediaSessionCompat#setMediaButtonReceiver(PendingIntent)
     */
    void setMediaButtonReceiver(PendingIntent mediaButtonReceiverIntent){
        mediaSession.setMediaButtonReceiver(mediaButtonReceiverIntent);
    }

    /**
     * @see MediaButtonReceiver#handleIntent(MediaSessionCompat, Intent)
     */
    void handleMediaButtonReceiverIntent(Intent intent) {
        MediaButtonReceiver.handleIntent(mediaSession, intent);
    }

    /**
     * @see MediaSessionCompat#release()
     */
    void destroy() {
        mediaSession.release();
    }

}
