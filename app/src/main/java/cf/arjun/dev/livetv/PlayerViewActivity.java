package cf.arjun.dev.livetv;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaDrm;
import com.google.android.exoplayer2.drm.LocalMediaDrmCallback;
import com.google.android.exoplayer2.drm.MediaDrmCallback;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.trackselection.TrackSelectionParameters;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.util.MimeTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import cf.arjun.dev.livetv.others.TrackSelectionDialog;

public class PlayerViewActivity extends AppCompatActivity {

    // ExoPlayer Stuffs.
    private PlayerView playerView;
    private ExoPlayer exoPlayer = null;
    private String[] speed = {"0.25x","0.5x","Normal","1.5x","2x"};
    private boolean isShowingTrackSelectionDialog;
    private TrackSelectionParameters trackSelectionParameters;

    private String currentUrl;
    private boolean playerState = true;
    private int currentMediaItem = 0;
    private long currentPosition = 0L;
    private boolean hasSubtitles = false;
    private String[] subtitleUrl, subtitleLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_view);

        setupUIViews();

    }

    private void setupUIViews () {

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        playerView = findViewById(R.id.playerView);

    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        updateTrackSelectionParameters();
        updateStartPosition();

        outState.putBoolean("playerState", playerState);
        outState.putString("currentUrl", currentUrl);
        outState.putInt("currentMediaItem", currentMediaItem);
        outState.putLong("currentPosition", currentPosition);
        outState.putBundle("trackSelectionParameters", trackSelectionParameters.toBundle());

    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        currentUrl = savedInstanceState.getString("currentUrl");
        currentMediaItem = savedInstanceState.getInt("currentMediaItem");
        currentPosition = savedInstanceState.getLong("currentPosition");
        playerState = savedInstanceState.getBoolean("playerState");
        trackSelectionParameters = TrackSelectionParameters.fromBundle(savedInstanceState.getBundle("trackSelectionParameters"));

    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        switch (intent.getAction()) {
            case "HLS": {
                if (intent.getBooleanExtra("has_track", false)) {
                    hasSubtitles = true;
                    subtitleUrl = intent.getStringArrayExtra("track_urls");
                    subtitleLabel = intent.getStringArrayExtra("track_labels");
                }
                initializePlayer(intent.getStringExtra("url"));
                break;
            } case "DRM": {                String clearKey = intent.getStringExtra("k");
                String clearKeyId = intent.getStringExtra("kid");
                initializePlayer(intent.getStringExtra("url"), clearKey, clearKeyId);
                break;
            } default: {
                Toast.makeText(this, "Empty Action " + intent.getAction(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (exoPlayer != null) {
            exoPlayer.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    private void updateTrackSelectionParameters () {
        if (exoPlayer != null) {
            trackSelectionParameters = exoPlayer.getTrackSelectionParameters();
        }
    }

    private void updateStartPosition () {
        if (exoPlayer != null) {
            currentPosition = exoPlayer.getCurrentPosition();
            currentMediaItem = exoPlayer.getCurrentMediaItemIndex();
        }
    }

    private void setTrackSelectionParameters () {

        if (trackSelectionParameters != null) {
            exoPlayer.setTrackSelectionParameters(trackSelectionParameters);
        } else {
            trackSelectionParameters = new TrackSelectionParameters.Builder(PlayerViewActivity.this)
                    .setPreferredAudioLanguage("hi")
                    .setForceLowestBitrate(true)
                    .setPreferredTextLanguage("eng")
                    .build();
            exoPlayer.setTrackSelectionParameters(trackSelectionParameters);

        }

    }

    private void initializePlayer (String url) {

        currentUrl = url;

        releasePlayer();

        exoPlayer = new ExoPlayer.Builder(PlayerViewActivity.this).build();
        playerView.setPlayer(exoPlayer);

        try {
            MediaItem media;
            if (hasSubtitles) {
                List<MediaItem.SubtitleConfiguration> subTracks = new ArrayList<>();

                for (int i = 0; i < subtitleUrl.length; i++) {
                    MediaItem.SubtitleConfiguration subtitleConfiguration =
                            new MediaItem.SubtitleConfiguration.Builder(Uri.parse(subtitleUrl[i]))
                                    .setMimeType(MimeTypes.TEXT_VTT)
                                    .setLanguage(subtitleLabel[i])
                                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                                    .build();
                    subTracks.add(subtitleConfiguration);
                }

                media = new MediaItem.Builder()
                        .setUri(url)
                        .setSubtitleConfigurations(subTracks)
                        .build();
            } else {
                media = MediaItem.fromUri(url);
            }
            exoPlayer.setMediaItem(media);
            exoPlayer.seekTo(currentMediaItem, currentPosition);

            setTrackSelectionParameters();

            exoPlayer.setPlayWhenReady(playerState);
            exoPlayer.prepare();
            initializePlayerUI();
        } catch (Exception ignore) {}

    }

    private void initializePlayer (String url, String clearKey, String clearKeyId) {

        currentUrl = url;

        releasePlayer();

        exoPlayer = new ExoPlayer.Builder(PlayerViewActivity.this).build();
        playerView.setPlayer(exoPlayer);

        try {

            // TODO: Handle the DRM Protection.

            DataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory();


            String keyString = "{\"keys\":[{\"kty\":\"oct\",\"k\":\"" + clearKey + "\",\"kid\":\"" + clearKeyId + "\"}],'type':\"temporary\"}";
            MediaDrmCallback drmCallback = new LocalMediaDrmCallback(keyString.getBytes());

            MediaSource dashMediaSource = new DashMediaSource.Factory(dataSourceFactory)
                    .setDrmSessionManagerProvider(mediaItem -> new DefaultDrmSessionManager.Builder()
                            .setPlayClearSamplesWithoutKeys(true)
                            .setMultiSession(false)
                            .setKeyRequestParameters(new HashMap<String,String>())
                            .setUuidAndExoMediaDrmProvider(C.CLEARKEY_UUID, FrameworkMediaDrm.DEFAULT_PROVIDER)
                            .build(drmCallback))
                    .createMediaSource(MediaItem.fromUri(url));


            exoPlayer.setMediaSource(dashMediaSource);
            exoPlayer.setPlayWhenReady(playerState);
            setTrackSelectionParameters();
            exoPlayer.prepare();

            initializePlayerUI();

        } catch (Exception ignore) {}

    }

    private void releasePlayer () {

        if (exoPlayer != null) {
            exoPlayer.release();
        }
        exoPlayer = null;

    }

    @SuppressLint("SourceLockedOrientationActivity")
    private void initializePlayerUI () {

        ImageView forwardBtn = playerView.findViewById(R.id.fwd);
        ImageView rewBtn = playerView.findViewById(R.id.rew);
        ImageView speedBtn = playerView.findViewById(R.id.exo_playback_speed);
        TextView speedTxt = playerView.findViewById(R.id.speed);

        speedBtn.setOnClickListener(v -> {


            AlertDialog.Builder builder = new AlertDialog.Builder(PlayerViewActivity.this);
            builder.setTitle("Set Speed");
            builder.setItems(speed, (dialog, which) -> {
                // the user clicked on colors[which]

                String text = speed[which];
                PlaybackParameters param = new PlaybackParameters(1f);

                if (which==0){

                    speedTxt.setVisibility(View.VISIBLE);
                    speedTxt.setText(text);
                    param = new PlaybackParameters(0.5f);

                }  if (which==1){

                    speedTxt.setVisibility(View.VISIBLE);
                    speedTxt.setText(text);
                    param = new PlaybackParameters(0.5f);
                }
                if (which==2){

                    speedTxt.setVisibility(View.GONE);
                    param = new PlaybackParameters(1f);
                }
                if (which==3){

                    speedTxt.setVisibility(View.VISIBLE);
                    speedTxt.setText(text);
                    param = new PlaybackParameters(1.5f);
                }
                if (which==4){

                    speedTxt.setVisibility(View.VISIBLE);
                    speedTxt.setText(text);
                    param = new PlaybackParameters(2f);
                }

                exoPlayer.setPlaybackParameters(param);
            });
            builder.show();

        });


        forwardBtn.setOnClickListener(v -> {

            try {

                long currentNum = exoPlayer.getCurrentPosition();
                long seekTo = currentNum + 10000; // 10 seconds forward
                long maxSeek = exoPlayer.getDuration();

                // Handle live streams
                if (maxSeek == C.TIME_UNSET) {
                    maxSeek = exoPlayer.getBufferedPosition(); // Use buffered position for live content
                }

                // Ensure seeking doesn't exceed the max duration
                if (seekTo > maxSeek) {
                    seekTo = maxSeek;
                }

                // Seek to the calculated position
                exoPlayer.seekTo(seekTo);

            } catch (Exception ignore) {}

        });
        rewBtn.setOnClickListener(v -> {

            long num = exoPlayer.getCurrentPosition() - 10000;
            if (num < 0) {
                exoPlayer.seekTo(0);
            } else {
                exoPlayer.seekTo(num);
            }
        });

        ImageView trackSelectionView = findViewById(R.id.exo_track_selection_view);

        trackSelectionView.setOnClickListener( v -> {

            if (!isShowingTrackSelectionDialog && TrackSelectionDialog.willHaveContent(exoPlayer)) {

                isShowingTrackSelectionDialog = true;
                TrackSelectionDialog trackSelectionDialog =
                        TrackSelectionDialog.createForPlayer(
                                exoPlayer,
                                dismissDialog -> isShowingTrackSelectionDialog = false);
                trackSelectionDialog.show(getSupportFragmentManager(), null);
            }

        });

        ImageView fullscreenButton = playerView.findViewById(R.id.fullscreen);

        fullscreenButton.setOnClickListener(view -> {

            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                // code for portrait mode
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                // code for landscape mode
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        });

        findViewById(R.id.exo_play).setOnClickListener(v -> {
            if (exoPlayer.getPlayerError() != null) exoPlayer.prepare();
            exoPlayer.play();
            playerState = true;
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        });
        findViewById(R.id.exo_pause).setOnClickListener(v -> {
            exoPlayer.pause();
            playerState = false;
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        });

    }

}