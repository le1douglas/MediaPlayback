package le1.mediaplayback.service;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import le1.mediaplayback.callback.AudioFocusCallback;
import le1.mediaplayback.R;

/**
 * Wrapper of {@link SimpleExoPlayer} that exposes the least amount of methods possible
 */
public class PlayerManager {
    private static final String TAG = "LE1_PlayerManager";
    private final SimpleExoPlayer player;
    private Context context;

    private static PlayerManager INSTANCE;

    public static PlayerManager getInstance(Context c){
        if (INSTANCE==null) INSTANCE= new PlayerManager(c);
        return INSTANCE;
    }

    /**
     * Build {@link #player} instance
     * @param context Application context
     */
    private PlayerManager(final Context context){
        player = ExoPlayerFactory.newSimpleInstance(context.getApplicationContext(),
                new DefaultTrackSelector(
                        new AdaptiveTrackSelection.Factory(
                                new DefaultBandwidthMeter())
                )
        );

        this.context = context;
    }

    /**
     * Register a listener for player events
     * @param listener The {@link Player.EventListener} to register
     */
    void addEventListener(Player.EventListener listener){
        player.addListener(listener);
    }

    /**
     * @return {@link #player}'s current position in seconds
     */
    public int getCurrentPosition(){
        return (int) player.getCurrentPosition()/1000;
    }

    /**
     * Start preparing playback
     * @param audioUri Audio only Uri of track
     * @param videoUri Video only Uri of track
     */
    void prepare(Uri audioUri, Uri videoUri){
        String userAgent = Util.getUserAgent(context, context.getString(R.string.app_name));
        DefaultExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context, userAgent);
        MediaSource audioSource = new ExtractorMediaSource(audioUri, dataSourceFactory, extractorsFactory, null, null);
        MediaSource videoSource = new ExtractorMediaSource(videoUri, dataSourceFactory, extractorsFactory, null, null);
        MergingMediaSource combinedSources = new MergingMediaSource(audioSource, videoSource);
        player.prepare(combinedSources, true, true);
    }

    /**
     * Start playback
     * @see Player#setPlayWhenReady(boolean)
     */
    void play(){
        Log.d(TAG, "play: called");
        player.setVolume(1.0f);
        player.setPlayWhenReady(true);
    }

    /**
     * Pause playback
     * @see Player#setPlayWhenReady(boolean)
     */
    void pause(){
        player.setVolume(1.0f);
        player.setPlayWhenReady(false);
    }

    /**
     * Stop playback
     * @see Player#stop()
     */
    void stop(){
        player.stop();
    }

    /**
     * Lower playback volume, usually used with {@link AudioFocusCallback#onAudioFocusLossTransientCanDuck()}
     */
    void duck(){
        player.setVolume(0.2f);
    }

    /**
     * Seek to a particular point in time
     * @param position in seconds
     */
    void seekTo(int position) {
        player.seekTo(position * 1000);
    }

    /**
     * @see Player#release()
     */
    void destroy(){
        player.release();
    }

    /**
     * Bind this player to a view
     * @param playerView The view to bind to.
     */
    public void setPlayerView(SimpleExoPlayerView playerView) {
        playerView.setPlayer(player);
    }
}
