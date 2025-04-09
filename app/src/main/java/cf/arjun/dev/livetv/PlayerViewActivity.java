package cf.arjun.dev.livetv;

import android.annotation.SuppressLint;
import android.app.AppOpsManager;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.media.audiofx.LoudnessEnhancer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.github.vkay94.dtpv.DoubleTapPlayerView;
import com.github.vkay94.dtpv.youtube.YouTubeOverlay;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Tracks;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaDrm;
import com.google.android.exoplayer2.drm.LocalMediaDrmCallback;
import com.google.android.exoplayer2.drm.MediaDrmCallback;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionOverride;
import com.google.android.exoplayer2.trackselection.TrackSelectionParameters;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import cf.arjun.dev.livetv.databinding.ActivityPlayerViewBinding;
import cf.arjun.dev.livetv.databinding.BoosterBinding;
import cf.arjun.dev.livetv.databinding.MoreFeaturesBinding;
import cf.arjun.dev.livetv.databinding.PlaybackSpeedDialogBinding;

public class PlayerViewActivity extends AppCompatActivity {

    // ExoPlayer Stuffs.
    private PlayerView playerView;
    private YouTubeOverlay youTubeOverlay;
    private ExoPlayer exoPlayer = null;
    private TrackSelectionParameters trackSelectionParameters;
    private DefaultTrackSelector trackSelector;
    private String currentUrl;
    private boolean playerState = true;
    private int currentMediaItem = 0;
    private long currentPosition = 0L;
    private boolean hasSubtitles = false;
    private String[] subtitleUrl, subtitleLabel;
    private ImageButton playPauseBtn, fullScreenBtn;
    private TextView videoTitle;
    private boolean isFullscreen = false, isLocked = false;
    private static float speed = 1.0f;
    private ActivityPlayerViewBinding binding;
    private static LoudnessEnhancer loudnessEnhancer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        binding = ActivityPlayerViewBinding.inflate(getLayoutInflater());
        saveTheme(-1);
        setTheme(MainActivity.THEMES[MainActivity.THEME_INDEX]);
        setContentView(binding.getRoot());

        // For Immersive Mode.
        makeImmersive();

        setupUIViews();
        initializeUIButtons();

    }

    private void setupUIViews () {

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        playerView = findViewById(R.id.playerView);
        youTubeOverlay = findViewById(R.id.ytOverlay);
        Log.d("x4rju9", "UI Initialized " + (youTubeOverlay == null));

    }

    private void initializeUIButtons () {
        playPauseBtn = findViewById(R.id.play_pause_button);
        fullScreenBtn = findViewById(R.id.fullscreen_button);
        videoTitle = findViewById(R.id.video_title);
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
                initializePlayer(intent.getStringExtra("url"), intent.getStringExtra("title"));
                break;
            } case "DRM": {                String clearKey = intent.getStringExtra("k");
                String clearKeyId = intent.getStringExtra("kid");
                initializePlayer(intent.getStringExtra("url"), intent.getStringExtra("title"), clearKey, clearKeyId);
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
        release();
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

    private void initializePlayer (String url, String title) {

        currentUrl = url;

        videoTitle.setText(title);
        videoTitle.setSelected(true);

        release();
        speed = 1.0f;
        trackSelector = new DefaultTrackSelector(this);
        exoPlayer = new ExoPlayer.Builder(PlayerViewActivity.this)
                .setTrackSelector(trackSelector)
                .build();
        doubleTapEnable();

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
            playVideo();
            initializePlayerUI();
        } catch (Exception ignore) {}

    }

    private void initializePlayer (String url, String title, String clearKey, String clearKeyId) {

        currentUrl = url;

        videoTitle.setText(title);
        videoTitle.setSelected(true);

        release();
        speed = 1.0f;
        trackSelector = new DefaultTrackSelector(this);
        exoPlayer = new ExoPlayer.Builder(PlayerViewActivity.this)
                .setTrackSelector(trackSelector)
                .build();
        doubleTapEnable();

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
            playVideo();
            initializePlayerUI();

        } catch (Exception ignore) {}

    }

    private void playVideo () {
        playPauseBtn.setImageResource(R.drawable.pause_icon);
        exoPlayer.play();
    }

    private void pauseVideo () {
        playPauseBtn.setImageResource(R.drawable.play_icon);
        exoPlayer.pause();
    }

    private void release () {
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }

    private void changeSpeed (boolean isIncrement) {
        if (isIncrement) {
            if (speed >= 3.0f) return;
            speed += 0.25f;
        } else {
            if (speed <= 0.25f) return;
            speed -= 0.25f;
        }
        exoPlayer.setPlaybackSpeed(speed);
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private void initializePlayerUI () {

        playInFullscreen(isFullscreen);
        loudnessEnhancer = new LoudnessEnhancer(exoPlayer.getAudioSessionId());
        loudnessEnhancer.setEnabled(true);

        playerView.setControllerVisibilityListener( it -> {
            if (isLocked) binding.lockButton.setVisibility(View.VISIBLE);
            else if (playerView.isControllerVisible()) binding.lockButton.setVisibility(View.VISIBLE);
            else binding.lockButton.setVisibility(View.GONE);
        });

        findViewById(R.id.rotation_button).setOnClickListener( v -> {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
            }
        });

        findViewById(R.id.player_back_button).setOnClickListener( v -> finish());
        playPauseBtn.setOnClickListener( v -> {
            if (exoPlayer.isPlaying()) pauseVideo();
            else playVideo();
        });

        fullScreenBtn.setOnClickListener( v -> {
            if (isFullscreen) {
                isFullscreen = false;
                playInFullscreen(false);
            } else {
                isFullscreen = true;
                playInFullscreen(true);
            }
        });

        binding.lockButton.setOnClickListener( v -> {
            if (isLocked) {
                isLocked = false;
                playerView.setUseController(true);
                playerView.showController();
                binding.lockButton.setImageResource(R.drawable.lock_open_icon);
            } else {
                isLocked = true;
                playerView.hideController();
                playerView.setUseController(false);;
                binding.lockButton.setImageResource(R.drawable.lock_close_icon);
            }
        });

        findViewById(R.id.menu_button).setOnClickListener( v -> {
            pauseVideo();
            View moreFeaturesView = LayoutInflater.from(this).inflate(R.layout.more_features, binding.getRoot(), false);
            MoreFeaturesBinding featuresBinding = MoreFeaturesBinding.bind(moreFeaturesView);
            AlertDialog mainDialog = new MaterialAlertDialogBuilder(this)
                    .setView(moreFeaturesView)
                    .setOnCancelListener( d -> playVideo())
                    .setBackground(new ColorDrawable(resolveAttrColor(R.attr.ThemePrimary))).create();
            mainDialog.show();

            featuresBinding.qualityTrack.setOnClickListener(view -> {
                mainDialog.dismiss();
                pauseVideo();

                ArrayList<String> qualityList = new ArrayList<>();
                ArrayList<Pair<TrackGroup, Integer>> overrideOptions = new ArrayList<>();

                Tracks currentTracks = exoPlayer.getCurrentTracks();

                for (Tracks.Group group : currentTracks.getGroups()) {
                    if (group.getType() == C.TRACK_TYPE_VIDEO) {
                        TrackGroup trackGroup = group.getMediaTrackGroup();

                        for (int i = 0; i < trackGroup.length; i++) {
                            Format format = trackGroup.getFormat(i);

                            String resolution = format.width + " x " + format.height;
                            String bitrate = String.format("%.2f Mbps", format.bitrate / 1000000f);

                            qualityList.add(resolution + ", " + bitrate);
                            overrideOptions.add(new Pair<>(trackGroup, i));
                        }
                    }
                }

                AlertDialog myDialog = new MaterialAlertDialogBuilder(this, R.style.alertDialog)
                        .setTitle("Select Quality")
                        .setOnCancelListener(dialog -> playVideo())
                        .setPositiveButton("Auto", (dialog, which) -> {
                            trackSelector.setParameters(
                                    trackSelector.buildUponParameters()
                                            .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                                            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
                            );
                            dialog.dismiss();
                        })
                        .setBackground(new ColorDrawable(resolveAttrColor(R.attr.ThemePrimary)))
                        .setItems(qualityList.toArray(new String[0]), (dialog, selectedIndex) -> {
                            Pair<TrackGroup, Integer> selected = overrideOptions.get(selectedIndex);
                            TrackSelectionOverride override = new TrackSelectionOverride(
                                    selected.first, ImmutableList.of(selected.second));

                            trackSelector.setParameters(
                                    trackSelector.buildUponParameters()
                                            .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                                            .addOverride(override)
                                            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
                            );
                        })
                        .create();
                myDialog.show();
                myDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(resolveAttrColor(R.attr.ThemeTextColorSecondary));
            });

            featuresBinding.audioTrack.setOnClickListener( view -> {
                mainDialog.dismiss();
                pauseVideo();

                ArrayList<String> audioTrack = new ArrayList<>();
                ArrayList<String> audioList = new ArrayList<>();

                for (Tracks.Group group : exoPlayer.getCurrentTracks().getGroups()) {
                    if (group.getType() == C.TRACK_TYPE_AUDIO) {
                        TrackGroup groupInfo = group.getMediaTrackGroup();
                        int length = groupInfo.length;
                        for (int i=0; i<length; i++) {
                            audioTrack.add(groupInfo.getFormat(i).language);
                            String text = "";
                            text += audioList.size() + 1 + " ";
                            String langCode = groupInfo.getFormat(i).language;
                            String displayLang = (langCode != null) ? new Locale(langCode).getDisplayLanguage() : "Unknown";
                            text += displayLang;
                            audioList.add(text);
                        }
                    }
                }

                if (audioList.get(0).contains("null")) {
                    audioList.remove(0);
                    audioList.add(0, "1. Default Track");
                }

                AlertDialog myDialog = new MaterialAlertDialogBuilder(this, R.style.alertDialog)
                        .setTitle("Select Language")
                        .setOnCancelListener( d -> playVideo())
                        .setPositiveButton("Disable", (self, pos) -> {
                            trackSelector.setParameters(trackSelector.buildUponParameters()
                                    .setRendererDisabled(C.TRACK_TYPE_AUDIO, true));
                            self.dismiss();
                        })
                        .setBackground(new ColorDrawable(resolveAttrColor(R.attr.ThemePrimary)))
                        .setItems(audioList.toArray(new String[0]),
                                (dial, pos) -> trackSelector.setParameters(
                                        trackSelector.buildUponParameters()
                                                .setRendererDisabled(C.TRACK_TYPE_AUDIO, false)
                                                .setPreferredAudioLanguage(audioTrack.get(pos))
                                ))
                        .create();
                myDialog.show();
                myDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(resolveAttrColor(R.attr.ThemeTextColorSecondary));
            });

            featuresBinding.subtitles.setOnClickListener( view -> {
                mainDialog.dismiss();
                pauseVideo();

                ArrayList<String> subtitles = new ArrayList<>();
                ArrayList<String> subtitlesList = new ArrayList<>();

                for (Tracks.Group group : exoPlayer.getCurrentTracks().getGroups()) {
                    if (group.getType() == C.TRACK_TYPE_TEXT) {
                        TrackGroup groupInfo = group.getMediaTrackGroup();
                        int length = groupInfo.length;
                        for (int i=0; i<length; i++) {
                            subtitles.add(groupInfo.getFormat(i).language);
                            String text = "";
                            text += subtitlesList.size() + 1 + " ";
                            String langCode = groupInfo.getFormat(i).language;
                            String displayLang = (langCode != null) ? new Locale(langCode).getDisplayLanguage() : "Unknown";
                            text += displayLang;
                            subtitlesList.add(text);
                        }
                    }
                }

                AlertDialog myDialog = new MaterialAlertDialogBuilder(this, R.style.alertDialog)
                        .setTitle("Select Subtitles")
                        .setOnCancelListener( d -> playVideo())
                        .setPositiveButton("Disable", (self, pos) -> {
                            trackSelector.setParameters(trackSelector.buildUponParameters()
                                    .setRendererDisabled(C.TRACK_TYPE_VIDEO, true));
                            self.dismiss();
                        })
                        .setBackground(new ColorDrawable(resolveAttrColor(R.attr.ThemePrimary)))
                        .setItems(subtitlesList.toArray(new String[0]),
                                (dial, pos) -> trackSelector.setParameters(
                                        trackSelector.buildUponParameters()
                                                .setRendererDisabled(C.TRACK_TYPE_VIDEO, false)
                                                .setPreferredTextLanguage(subtitles.get(pos))
                                ))
                        .create();
                myDialog.show();
                myDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(resolveAttrColor(R.attr.ThemeTextColorSecondary));
            });

            featuresBinding.booster.setOnClickListener( view -> {
                mainDialog.dismiss();
                View boosterView = LayoutInflater.from(this).inflate(R.layout.booster, binding.getRoot(), false);
                BoosterBinding boosterBinding = BoosterBinding.bind(boosterView);
                AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                        .setView(boosterView)
                        .setOnCancelListener( d -> playVideo())
                        .setPositiveButton("OK", (self, pos) -> {
                            loudnessEnhancer.setTargetGain(
                                    boosterBinding.boosterBar.getProgress() * 100);
                            playVideo();
                            self.dismiss();
                        })
                        .setBackground(new ColorDrawable(resolveAttrColor(R.attr.ThemePrimary))).create();
                dialog.show();
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(resolveAttrColor(R.attr.ThemeTextColorSecondary));

                int loudness = (int)loudnessEnhancer.getTargetGain();
                boosterBinding.boosterBar.setProgress(loudness/100);
                boosterBinding.boosterText.setText("Audio Boost\n\n" + (loudness/10)  + "%");
                boosterBinding.boosterBar.setOnProgressChangeListener( integer -> {
                    boosterBinding.boosterText.setText("Audio Boost\n\n" + (integer*10) + "%");
                    return null;
                });

            });

            featuresBinding.playBackSpeed.setOnClickListener( view -> {
                mainDialog.dismiss();
                playVideo();
                View speedView = LayoutInflater.from(this).inflate(R.layout.playback_speed_dialog, binding.getRoot(), false);
                PlaybackSpeedDialogBinding speedBinding = PlaybackSpeedDialogBinding.bind(speedView);
                AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                        .setView(speedView)
                        .setCancelable(false)
                        .setPositiveButton("OK", (self, pos) -> {
                            self.dismiss();
                        })
                        .setBackground(new ColorDrawable(resolveAttrColor(R.attr.ThemePrimary))).create();
                dialog.show();
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(resolveAttrColor(R.attr.ThemeTextColorSecondary));

                speedBinding.speedText.setText(speed + "X");

                speedBinding.speedMinus.setOnClickListener( minusView -> {
                    changeSpeed(false);
                    speedBinding.speedText.setText(speed + "X");
                });

                speedBinding.speedPlus.setOnClickListener( plusView -> {
                    changeSpeed(true);
                    speedBinding.speedText.setText(speed + "X");
                });

            });

        });

    }

    private void doubleTapEnable () {

        playerView.setPlayer(exoPlayer);
        youTubeOverlay.performListener(new YouTubeOverlay.PerformListener() {
            @Override
            public void onAnimationEnd() {
                binding.ytOverlay.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationStart() {
                binding.ytOverlay.setVisibility(View.VISIBLE);
            }
        });
        binding.ytOverlay.player(exoPlayer);

    }

    private void playInFullscreen (boolean enable) {
        if (enable) {
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
            exoPlayer.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
            fullScreenBtn.setImageResource(R.drawable.fullscreen_exit_icon);
        } else {
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
            exoPlayer.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT);
            fullScreenBtn.setImageResource(R.drawable.fullscreen_icon);
        }
    }

    private void makeImmersive () {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), binding.getRoot());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    private void saveTheme (int index) {

        SharedPreferences prefs = getSharedPreferences("AppThemes", Context.MODE_PRIVATE);

        if (index == -1) {
            MainActivity.THEME_INDEX = prefs.getInt("themeIndex", 0);
        } else {
            prefs.edit().putInt("themeIndex", index).apply();
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finishAffinity();
                    System.exit(0);
                }
            }, 200); // Delay for 200ms to ensure data is saved
        }

    }

    private int resolveAttrColor(int attrResId) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getTheme();
        theme.resolveAttribute(attrResId, typedValue, true);
        return typedValue.data;
    }


}