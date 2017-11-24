package le1.mediaplayback;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.session.PlaybackState;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.google.android.exoplayer2.ui.SimpleExoPlayerView;

import le1.mediaplayback.application.MediaPlaybackApplication;
import le1.mediaplayback.callback.PlaybackStateListener;
import le1.mediaplayback.callback.PlaybackStateCompositeListener;
import le1.mediaplayback.service.MusicService;
import le1.mediaplayback.service.PlayerManager;

/**
 * Use this class for any interaction with media playback,
 * e.g media control, user friendly callbacks, retrieving of playback information
 */
public class MusicControl {
    private static final String TAG = "LE1_MusicControl";
    private Context context;

    /**
     * A collection of all the {@link PlaybackStateListener} subscribed
     */
    private PlaybackStateCompositeListener compositeListener;

    /**
     * Used to connect with {@link MusicService}
     */
    private MediaBrowserCompat mediaBrowserCompat;

    /**
     * Used to actually control playback,
     * usable only after connecting with {@link #mediaBrowserCompat}
     */
    private MediaControllerCompat mediaController;

    /**
     * Intent used to start and stop {@link MusicService}
     */
    private Intent musicServiceIntent;

    /**
     * Forward any change in playback to {@link #compositeListener}
     */
    private MediaControllerCompat.Callback mediaControllerCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);
            switch (state.getState()) {
                case PlaybackStateCompat.STATE_PLAYING:
                    compositeListener.onPlaying("playing");
                    break;
                case PlaybackStateCompat.STATE_BUFFERING:
                    compositeListener.onLoading("loading");
                    break;
                case PlaybackStateCompat.STATE_PAUSED:
                    compositeListener.onPaused("paused");
                    break;
                case PlaybackStateCompat.STATE_STOPPED:
                    compositeListener.onStopped("stopped");
                    break;
                case PlaybackStateCompat.STATE_ERROR:
                    compositeListener.onError("error");
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);
            compositeListener.onMetadataLoaded(metadata);
        }
    };

    /**
     * Callback of {@link #mediaBrowserCompat}
     */
    private MediaBrowserCompat.ConnectionCallback connectionCallback = new MediaBrowserCompat.ConnectionCallback() {
        @Override
        public void onConnected() {
            super.onConnected();
            try {
                mediaController = new MediaControllerCompat(context, mediaBrowserCompat.getSessionToken());
                mediaController.registerCallback(mediaControllerCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

    };


    /**
     * Don't use this constructor directly.
     * Use {@link MediaPlaybackApplication#getMusicControl()} instead
     *
     * @param context Application context
     */
    public MusicControl(Context context) {
        musicServiceIntent = new Intent(context.getApplicationContext(), MusicService.class);
        compositeListener = new PlaybackStateCompositeListener();
        mediaBrowserCompat = new MediaBrowserCompat(context,
                new ComponentName(context.getApplicationContext(), MusicService.class), connectionCallback, null);
        this.context = context;
    }

    /**
     * @param playerView Set a view in which to display media
     */
    public void setPlayerView(SimpleExoPlayerView playerView) {
        PlayerManager.getInstance(context).setPlayerView(playerView);
    }

    /**
     * Add a listener that will react to playback events
     *
     * @param playbackListener The listener to add
     */
    public void addListener(PlaybackStateListener playbackListener) {
        compositeListener.addListener(playbackListener);
    }

    /**
     * Connect to {@link MusicService}
     */
    public void connect() {
        context.startService(musicServiceIntent);
        if (!mediaBrowserCompat.isConnected()) mediaBrowserCompat.connect();
    }

    /**
     * Close connection and perform all the clean up
     * <p>
     * {@link MediaControllerCompat#unregisterCallback(MediaControllerCompat.Callback)}
     * it's just a ui callback so it's the first to be eliminated
     * <p>
     * {@link MediaBrowserCompat#disconnect()} should be the last operation
     * <p>
     * finally, {@link Context#stopService(Intent)} kills the service
     */
    public void disconnect() {
        mediaController.unregisterCallback(mediaControllerCallback);
        mediaBrowserCompat.disconnect();
        context.stopService(musicServiceIntent);

    }

    /**
     * @return true if connected to {@link MusicService}, false otherwise
     */
    public boolean isConnected() {
        return mediaBrowserCompat.isConnected();
    }


    /**
     * Prepare playback, load media and then start playing (as if called with {@link #play()})
     */
    public void prepareAndPlay() {
        mediaController.getTransportControls().prepareFromMediaId("A2naW_PxI2M", null);
    }

    /**
     * Start playback.
     * Only works if song was prepared with {@link #prepareAndPlay()}
     */
    public void play() {
        mediaController.getTransportControls().play();
    }

    /**
     * Pause playback.
     * Only works if song was prepared with {@link #prepareAndPlay()}
     */
    public void pause() {
        mediaController.getTransportControls().pause();
    }

    /**
     * Stops playback.
     * Can be called at any time
     */
    public void stop() {
        mediaController.getTransportControls().stop();
    }


    /**
     * @return the current playback position in milliseconds
     */
    public int getCurrentPosition() {
        return PlayerManager.getInstance(context).getCurrentPosition()*1000;
    }

    /**
     * @return the current {@link MediaMetadataCompat} of the current playback.
     * May change at any moment
     */
    public MediaMetadataCompat getMetadata(){
        return mediaController.getMetadata();
    }

    public int getPlaybackState(){
        return mediaController.getPlaybackState().getState();
    }

    /**
     * Seek to a new position in time
     * @param progress Number of milliseconds where to start playback from
     */
    public void seekTo(int progress) {
        mediaController.getTransportControls().seekTo(progress/1000);
    }

    /**
     * Toggle playing and paused
     */
    public void playOrPause() {
       if (mediaController.getPlaybackState().getState()== PlaybackStateCompat.STATE_PLAYING)
           mediaController.getTransportControls().pause();
        else
            mediaController.getTransportControls().play();
    }
}
