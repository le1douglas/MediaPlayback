package le1.mediaplayback;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;

import le1.mediaplayback.application.MediaPlaybackApplication;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    SimpleExoPlayerView playerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        playerView = findViewById(R.id.exo_player);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume:" + getApplicationContext());
        ((MediaPlaybackApplication) getApplicationContext()).getMusicControl().setPlayerView(playerView);
    }

    public void start(View view) {
        ((MediaPlaybackApplication) getApplicationContext()).getMusicControl().prepareAndPlay();
    }

}