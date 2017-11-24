package le1.mediaplayback.service;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;

import java.util.List;

import at.huber.youtubeExtractor.VideoMeta;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YtFile;
import le1.mediaplayback.MusicControl;
import le1.mediaplayback.application.AppLifecycleObserver;
import le1.mediaplayback.callback.AudioFocusCallback;
import le1.mediaplayback.application.MediaPlaybackApplication;

/**
 * The service used for playback.
 * It starts when the app launches in {@link AppLifecycleObserver#onResume()},
 * and stops when no activity is on the recents screen (or in foreground) and no music is playing
 */
public class MusicService extends MediaBrowserServiceCompat{

    private static final String TAG = "LE1_MusicService";

    private MediaSessionManager mediaSession;
    private MediaButtonManager mediaButtonReceiver;
    private AudioFocusManager audioFocus;
    private PlayerManager player;

    /**
     * Does all the setup and sets the PlaybackState to {@link PlaybackStateCompat#STATE_NONE}
     */
    @Override
    public void onCreate() {
        super.onCreate();
        mediaSession = new MediaSessionManager(this, mediaSessionCallback);
        setSessionToken(mediaSession.getToken());
        mediaSession.setPlaybackState(PlaybackStateCompat.STATE_NONE, -1);
        mediaButtonReceiver = new MediaButtonManager(this, mediaSession);
        audioFocus = new AudioFocusManager(this, audioFocusCallback);
        player = PlayerManager.getInstance(this);
        player.addEventListener(playerListener);
        Toast.makeText(this, "onCreate", Toast.LENGTH_SHORT).show();
    }

    /**
     * Calls {@link MediaButtonReceiver#handleIntent(MediaSessionCompat, Intent)}
     * to forward any media buttons clicks to the appropriate {@link MediaSessionCompat.Callback} method
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mediaButtonReceiver.handleIntent(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        return new BrowserRoot("rootId", null);
    }

    @Override
    public void onLoadChildren(@NonNull String rootId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.sendResult(null);
    }

    /**
     * Callback that actually controls the playback.
     * Every playback command ends here in a way or another.
     * Every {@link MediaSessionManager#setPlaybackState(int, long)} is delegated to {@link #playerListener} as it's when the playback <i>actually</i>
     * starts and not when it <i>should</i> happen
     */
    private MediaSessionCompat.Callback mediaSessionCallback = new MediaSessionCompat.Callback() {

        /**
         * Starts preparing and then starts playing with {@link MusicControl#play()}
         */
        @Override
        public void onPrepareFromMediaId(String youTubeId, Bundle extras) {
            super.onPrepareFromMediaId(youTubeId, extras);
            Log.d(TAG, "onPrepare");
            //we want a fresh start if music it's already playing
            if (mediaSession.getPlaybackState() == PlaybackStateCompat.STATE_PLAYING) {
                ((MediaPlaybackApplication) getApplicationContext()).getMusicControl().stop();
            }

            //we set the playback state to STATE_BUFFERING before extracting the youtube song
            mediaSession.setPlaybackState(PlaybackStateCompat.STATE_BUFFERING, -1);
            MusicNotification.updateNotification(MusicService.this, MusicService.this, mediaSession);


            new YouTubeExtractor(MusicService.this) {
                @Override
                protected void onExtractionComplete(SparseArray<YtFile> itags, VideoMeta videoMeta) {
                    if (itags != null) {
                        Log.d(TAG, "onExtractionComplete: " + videoMeta.getVideoLength());
                        mediaSession.setMetadata(videoMeta.getTitle(), videoMeta.getVideoId(), videoMeta.getChannelId(), null, videoMeta.getMaxResImageUrl(), videoMeta.getVideoLength() * 1000);
                        //actually prepare the player
                        player.prepare(Uri.parse(itags.get(140).getUrl()), Uri.parse(itags.get(160).getUrl()));
                        //after preparing start playing
                        ((MediaPlaybackApplication) getApplicationContext()).getMusicControl().play();
                    } else {
                        mediaSession.setPlaybackState(PlaybackStateCompat.STATE_ERROR, -1);
                        MusicNotification.updateNotification(MusicService.this, MusicService.this, mediaSession);
                        Toast.makeText(MusicService.this, "itag null", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "onExtractionComplete: itags is null");
                    }
                }
            }
                    //actually extract the YouTube song. Method called at object creation
                    .extract("https://www.youtube.com/watch?v=" + youTubeId, true, true);
        }


        @Override
        public void onPause() {
            super.onPause();
            player.pause();
        }

        /**
         * Starts playing the media
         */
        @Override
        public void onPlay() {
            super.onPlay();
            Log.d(TAG, "onPlay: called");
            //ask for audio focus, if given, continue on
            if (audioFocus.requestAudioFocus()) {
                mediaSession.setActive();
                player.play();
            }


        }

        /**
         * Stops playback causing {@link Player.EventListener#onPlayerStateChanged(boolean, int)}
         * to be called with {@link Player#STATE_IDLE}
         */
        @Override
        public void onStop() {
            super.onStop();
            player.stop();
            audioFocus.abandonAudioFocus();
            mediaSession.setInactive();

            // sometime the notification is swiped while the app is not in the recent task
            // in this case we want to stop the service
            if (!MediaPlaybackApplication.isAppOpen())
                ((MediaPlaybackApplication) getApplicationContext()).getMusicControl().disconnect();

        }


        /**
         * Seeks to a different position in the same media
         * @param pos The position in seconds
         */
        @Override
        public void onSeekTo(long pos) {
            super.onSeekTo(pos);
            Log.d(TAG, "onSeekTo: " + (int) pos);
            player.seekTo((int) pos);
        }
    };

    /**
     * Listener of the {@link #player} actions. Gets called when the player <i>actually</i>
     * starts responding not when it <i>should</i>
     */
    private Player.EventListener playerListener = new Player.EventListener() {

        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest) {
        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

        }

        @Override
        public void onLoadingChanged(boolean isLoading) {

        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            switch (playbackState) {
                case Player.STATE_IDLE:
                    mediaSession.setPlaybackState(PlaybackStateCompat.STATE_STOPPED, -1);
                    break;
                case Player.STATE_READY:
                    if (playWhenReady) {
                        //called when it's playing
                        mediaSession.setPlaybackState(PlaybackStateCompat.STATE_PLAYING, player.getCurrentPosition());
                    } else {
                        //called when it's paused
                        mediaSession.setPlaybackState(PlaybackStateCompat.STATE_PAUSED, player.getCurrentPosition());
                    }
                    break;
                case Player.STATE_BUFFERING:
                    mediaSession.setPlaybackState(PlaybackStateCompat.STATE_BUFFERING, -1);
                    break;
                case Player.STATE_ENDED:
                    //when the song ends, stop playback
                    //TODO add queue
                    mediaSession.getControls().stop();
                    break;
            }

            MusicNotification.updateNotification(MusicService.this, MusicService.this, mediaSession);
        }

        @Override
        public void onRepeatModeChanged(int repeatMode) {

        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            mediaSession.setPlaybackState(PlaybackStateCompat.STATE_ERROR, -1);
            MusicNotification.updateNotification(MusicService.this, MusicService.this, mediaSession);
        }

        @Override
        public void onPositionDiscontinuity() {
        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

        }
    };


    private boolean wasPlaying;
    /**
     * Callback of audio focus events
     * @see AudioFocusCallback
     */
    AudioFocusCallback audioFocusCallback = new AudioFocusCallback() {

        @Override
        public void onAudioFocusGain() {
            if (wasPlaying) ((MediaPlaybackApplication) getApplicationContext()).getMusicControl().play();
            wasPlaying = false;
        }

        @Override
        public void onAudioFocusLoss() {
            if (((MediaPlaybackApplication) getApplicationContext()).getMusicControl().getPlaybackState()
                    ==PlaybackStateCompat.STATE_PLAYING) wasPlaying=true;
            ((MediaPlaybackApplication) getApplicationContext()).getMusicControl().pause();
        }

        @Override
        public void onAudioFocusLossTransient() {
            ((MediaPlaybackApplication) getApplicationContext()).getMusicControl().pause();
        }

        @Override
        public void onAudioFocusLossTransientCanDuck() {
            player.duck();
        }

        @Override
        public void onAudioFocusBecomingNoisy() {
            ((MediaPlaybackApplication) getApplicationContext()).getMusicControl().pause();
        }
    };


    /**
     * Called when the app is swiped from the recents screen
     * Stops the service if user is not listening to music
     *
     * @see MediaBrowserServiceCompat#onTaskRemoved(Intent)
     */
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        MediaPlaybackApplication.setAppOpen(false);

        //if it's playing or loading don't stop the service
        if (!(mediaSession.getPlaybackState() == PlaybackStateCompat.STATE_PLAYING) &&
                !(mediaSession.getPlaybackState() == PlaybackStateCompat.STATE_BUFFERING)) {
            ((MediaPlaybackApplication) getApplicationContext()).getMusicControl().disconnect();
        }
    }

    /**
     * Called when the service is being stopped. Calls {@link MusicControl#stop()} to perform
     * all the clean up and then destroys the objects
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "onDestroy", Toast.LENGTH_SHORT).show();
        ((MediaPlaybackApplication) getApplicationContext()).getMusicControl().stop();
        mediaSession.destroy();
        player.destroy();
    }


}
