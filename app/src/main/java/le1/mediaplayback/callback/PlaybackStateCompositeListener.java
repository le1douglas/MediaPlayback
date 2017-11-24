package le1.mediaplayback.callback;

import android.support.v4.media.MediaMetadataCompat;

import java.util.ArrayList;
import java.util.List;

public class PlaybackStateCompositeListener implements PlaybackStateListener {

    private List<PlaybackStateListener> playbackStateListeners = new ArrayList<>();

    public void addListener(PlaybackStateListener playbackStateListener){
        playbackStateListeners.add(playbackStateListener);
    }
    public void removeListener(PlaybackStateListener playbackStateListener){
        playbackStateListeners.remove(playbackStateListener);
    }

    @Override
    public void onMetadataLoaded(MediaMetadataCompat metadata) {
        for (PlaybackStateListener l: playbackStateListeners){
            l.onMetadataLoaded(metadata);
        }
    }

    @Override
    public void onLoading(String message) {
        for (PlaybackStateListener l: playbackStateListeners){
            l.onLoading(message);
        }
    }

    @Override
    public void onPaused(String message) {
        for (PlaybackStateListener l: playbackStateListeners){
            l.onPaused(message);
        }
    }

    @Override
    public void onPlaying(String message) {
        for (PlaybackStateListener l: playbackStateListeners){
            l.onPlaying(message);
        }
    }

    @Override
    public void onStopped(String message) {
        for (PlaybackStateListener l: playbackStateListeners){
            l.onStopped(message);
        }
    }

    @Override
    public void onError(String message) {
        for (PlaybackStateListener l: playbackStateListeners){
            l.onError(message);
        }
    }
}