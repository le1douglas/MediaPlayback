package le1.mediaplayback;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import le1.mediaplayback.application.MediaPlaybackApplication;
import le1.mediaplayback.callback.PlaybackStateListener;

/**
 * A {@link View} that mimics the player overlay of youtube.
 * This view does not contain th player, it's just an overlay
 */
public class PlayerOverlayView extends RelativeLayout implements PlaybackStateListener, SeekBar.OnSeekBarChangeListener {
    private static final String TAG = "LE1_PlayerOverlayView";

    private TextView titleView;
    private TextView currentTimeView;
    private TextView totalTimeView;
    private ProgressBar loadingIcon;
    private SeekBar seekbar;
    private ImageButton playPauseButton;

    private MusicControl musicControl;
    private boolean isUiShown;

    final Handler autoHideHandler = new Handler();
    Runnable autoHideRunnable = new Runnable() {
        @Override
        public void run() {
            hideUi();
        }

    };
    private static final int autoHideMs = 2500;

    public PlayerOverlayView(Context context) {
        this(context, null, 0);
    }

    public PlayerOverlayView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Inflates the {@link R.layout#player_overlay} layout used for the ui.
     * Start an {@link Handler} that updates the {@link #seekbar} every second.
     */
    public PlayerOverlayView(final Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        musicControl = ((MediaPlaybackApplication) context.getApplicationContext()).getMusicControl();
        musicControl.addListener(this);

        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View view = layoutInflater.inflate(R.layout.player_overlay, this);

        titleView = view.findViewById(R.id.title);
        loadingIcon = view.findViewById(R.id.loading_icon);
        currentTimeView = view.findViewById(R.id.current_time);
        totalTimeView = view.findViewById(R.id.total_time);
        seekbar = view.findViewById(R.id.seek_bar);

        seekbar.setOnSeekBarChangeListener(this);

        playPauseButton = view.findViewById(R.id.play_pause);
        playPauseButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                musicControl.playOrPause();
            }
        });


        final Handler handler = new Handler();
        handler.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        if (isAttachedToWindow()) {
                            handler.postDelayed(this, 1000);
                            seekbar.setProgress(musicControl.getCurrentPosition());
                            currentTimeView.setText(formatMilliseconds(musicControl.getCurrentPosition()));
                        }
                    }
                }, 1000);
    }


    /**
     * Update and show the ui to reflect a {@link PlaybackStateCompat} state.
     * Automatically calls {@link #hideUi()} after {@link #autoHideMs} milliseconds
     *
     * @param playbackState one of
     *                      {@link PlaybackStateCompat#STATE_BUFFERING}
     *                      {@link PlaybackStateCompat#STATE_PLAYING}
     *                      {@link PlaybackStateCompat#STATE_PAUSED}
     *                      {@link PlaybackStateCompat#STATE_STOPPED}
     *                      {@link PlaybackStateCompat#STATE_ERROR}
     * @param metadata      the metadata used to build the ui
     */
    public void updateUi(int playbackState, MediaMetadataCompat metadata) {
        isUiShown = true;
        if (metadata != null) {
            seekbar.setMax((int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION));
            titleView.setText(metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE));
            totalTimeView.setText(formatMilliseconds((int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)));
        }
        switch (playbackState) {
            case PlaybackStateCompat.STATE_BUFFERING:
                playPauseButton.setVisibility(GONE);
                loadingIcon.setVisibility(VISIBLE);
                titleView.setVisibility(VISIBLE);
                seekbar.setVisibility(VISIBLE);
                currentTimeView.setVisibility(VISIBLE);
                totalTimeView.setVisibility(VISIBLE);

                if (titleView.getText().toString().equals("")) titleView.setText("Loading");
                seekbar.setActivated(true);
                break;
            case PlaybackStateCompat.STATE_PLAYING:
                playPauseButton.setVisibility(VISIBLE);
                loadingIcon.setVisibility(GONE);
                titleView.setVisibility(VISIBLE);
                seekbar.setVisibility(VISIBLE);
                currentTimeView.setVisibility(VISIBLE);
                totalTimeView.setVisibility(VISIBLE);

                seekbar.setActivated(true);
                playPauseButton.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.exo_controls_pause));
                break;
            case PlaybackStateCompat.STATE_PAUSED:
                playPauseButton.setVisibility(VISIBLE);
                loadingIcon.setVisibility(GONE);
                titleView.setVisibility(VISIBLE);
                seekbar.setVisibility(VISIBLE);
                currentTimeView.setVisibility(VISIBLE);
                totalTimeView.setVisibility(VISIBLE);

                seekbar.setActivated(true);
                playPauseButton.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.exo_controls_play));

                break;
            case PlaybackStateCompat.STATE_STOPPED:
                playPauseButton.setVisibility(GONE);
                loadingIcon.setVisibility(GONE);
                titleView.setVisibility(GONE);
                seekbar.setVisibility(GONE);
                currentTimeView.setVisibility(GONE);
                totalTimeView.setVisibility(GONE);

                seekbar.setActivated(false);
                break;
            case PlaybackStateCompat.STATE_ERROR:
                playPauseButton.setVisibility(GONE);
                loadingIcon.setVisibility(GONE);
                titleView.setVisibility(VISIBLE);
                seekbar.setVisibility(GONE);
                currentTimeView.setVisibility(GONE);
                totalTimeView.setVisibility(GONE);

                titleView.setText("Error");
                seekbar.setActivated(false);
                break;
            default:
                break;

        }

        // If the handler it's already started, stop it and restart it,
        // so that the runnable it's called only after the last call to this method
        autoHideHandler.removeCallbacks(autoHideRunnable);
        autoHideHandler.postDelayed(autoHideRunnable, autoHideMs);
    }

    /**
     * Hides all the ui controls for a more immersive experience.
     * Ui can be made visible again with {@link #updateUi(int, MediaMetadataCompat)}
     */
    public void hideUi() {
        isUiShown = false;
        playPauseButton.setVisibility(GONE);
        loadingIcon.setVisibility(GONE);
        titleView.setVisibility(GONE);
        seekbar.setVisibility(GONE);
        currentTimeView.setVisibility(GONE);
        totalTimeView.setVisibility(GONE);
    }


    /**
     * Restores the {@link #seekbar} and the correct view state
     * as soon as this view is visible.
     *
     * @see View#onAttachedToWindow()
     */
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (musicControl.isConnected()) {
            if (isUiShown) {
                updateUi(musicControl.getPlaybackState(), musicControl.getMetadata());
                seekbar.setProgress(musicControl.getCurrentPosition());
            }
        }
    }

    /**
     * Called every time the user touches the view and no other sub-view catches the
     * {@link MotionEvent} (for example, if a button is touched, this method is not called)
     * Toggles the visibility of the ui
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isUiShown) hideUi();
        else updateUi(musicControl.getPlaybackState(), musicControl.getMetadata());
        return super.onTouchEvent(event);
    }

    /**
     * Converts milliseconds to a human readable string formatted as mm:ss
     *
     * @param time the number of milliseconds to convert
     * @return a human readable timestamp
     */
    private String formatMilliseconds(int time) {
        time = time / 1000;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return String.format(this.getResources().getConfiguration().getLocales().get(0),
                    "%02d:%02d", time / 60, time % 60);
        } else {
            //noinspection deprecation
            return String.format(this.getResources().getConfiguration().locale,
                    "%02d:%02d", time / 60, time % 60);
        }
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    /**
     * Calls {@link MusicControl#seekTo(int)} only when user releases the finger
     */
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        Log.d(TAG, "onStopTrackingTouch: " + seekbar.getProgress());
        musicControl.seekTo(seekbar.getProgress());
    }


    /**
     * @see PlaybackStateListener#onMetadataLoaded(MediaMetadataCompat)
     */
    @Override
    public void onMetadataLoaded(MediaMetadataCompat metadata) {
        updateUi(musicControl.getPlaybackState(), metadata);
    }

    /**
     * For every playback action,
     * calls {@link #updateUi(int, MediaMetadataCompat)} with the appropriate {@link PlaybackStateCompat} state.
     */
    @Override
    public void onLoading(String message) {
        updateUi(PlaybackStateCompat.STATE_BUFFERING, musicControl.getMetadata());
    }

    @Override
    public void onPaused(String message) {
        updateUi(PlaybackStateCompat.STATE_PAUSED, musicControl.getMetadata());
    }

    @Override
    public void onPlaying(String message) {
        updateUi(PlaybackStateCompat.STATE_PLAYING, musicControl.getMetadata());
    }

    @Override
    public void onStopped(String message) {
        updateUi(PlaybackStateCompat.STATE_STOPPED, musicControl.getMetadata());
    }

    @Override
    public void onError(String message) {
        updateUi(PlaybackStateCompat.STATE_ERROR, musicControl.getMetadata());
    }


    /**
     * Saves before screen rotation
     *
     * @return the {@link Parcelable} where {@link #isUiShown} is stored
     */
    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.ssIsUiShown = isUiShown;
        Log.d(TAG, "onSaveInstanceState: " + ss.ssIsUiShown);
        return ss;
    }

    /**
     * Restores {@link #isUiShown} after screen rotation
     *
     * @param state {@link Parcelable} where {@link #isUiShown} is saved
     */
    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState ss = (SavedState) state;
        Log.d(TAG, "onRestoreInstanceState: " + ss.ssIsUiShown);
        if (ss.ssIsUiShown) updateUi(musicControl.getPlaybackState(), musicControl.getMetadata());
        else hideUi();
        super.onRestoreInstanceState(ss.getSuperState());

    }

    /**
     * A class that helps with writing and reading the {@link Parcelable} used
     * in {@link #onSaveInstanceState()} and {@link #onRestoreInstanceState(Parcelable)}
     */
    private static class SavedState extends BaseSavedState {
        boolean ssIsUiShown;

        /**
         * Constructor used to save the {@link super#onRestoreInstanceState(Parcelable)}
         *
         * @param superState the {@link super#onRestoreInstanceState(Parcelable)} to be saved
         */
        SavedState(Parcelable superState) {
            super(superState);
        }

        /**
         * Constructor used by {@link Parcelable.Creator}
         *
         * @param in the {@link Parcel} used by {@link #CREATOR}
         */
        private SavedState(Parcel in) {
            super(in);
            this.ssIsUiShown = in.readByte() != 0;
        }

        /**
         * Writes the values that we want to save (mainly {@link #isUiShown}) in a {@link Parcel}
         */
        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeByte((byte) (ssIsUiShown ? 1 : 0));
            Log.d(TAG, "writeToParcel: " + out.readByte());
        }

        /**
         * @see Parcelable.Creator
         */
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }

}

