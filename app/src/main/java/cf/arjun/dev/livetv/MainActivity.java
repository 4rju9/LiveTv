package cf.arjun.dev.livetv;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.Executors;

import cf.arjun.dev.livetv.databinding.ThemeViewBinding;
import cf.arjun.dev.livetv.fragments.AnimeFragment;
import cf.arjun.dev.livetv.fragments.EntertainmentFragment;
import cf.arjun.dev.livetv.fragments.MusicFragment;
import cf.arjun.dev.livetv.fragments.NewsFragment;
import cf.arjun.dev.livetv.fragments.SportsFragment;
import cf.arjun.dev.livetv.others.UpdateApk;
import cf.arjun.dev.livetv.repository.Queue;

public class MainActivity extends AppCompatActivity {

    private Queue queue;
    public static JSONObject jsonData = null;
    public static JSONObject jsonAnimeData = null;
    public static JSONArray MOST_POPULAR = new JSONArray(),
            TOP_AIRING = new JSONArray(),
            NEW_EPISODE_RELEASES = new JSONArray(),
            MOST_FAVORITES = new JSONArray(),
            TOP_TV_SERIES = new JSONArray();
    public static String ANIME_BASE_URL;
    public static String ANIME_FALLBACK_SOURCE;
    public static String MUSIC = "music";
    public static String SPORTS = "sports";
    public static String ENTERTAINMENT = "entertainment";
    public static String NEWS = "news";

    // UI Views.
    private BottomNavigationView bottomBar;
    private Handler handler = new Handler();
    private Runnable runnable;
    private Fragment fragment;
    private EditText searchBar;
    public static int THEME_INDEX = 0;
    public static int[] THEMES = {
            R.style.DeepSkyBlueNav, R.style.BrownNav, R.style.YellowNav, R.style.PurpleNav,
            R.style.PinkNav, R.style.DarkBlackTheme, R.style.DarkPurpleNav, R.style.BlackNav,
            R.style.BlueBlackNav, R.style.BrownBlackNav, R.style.YellowBlackNav, R.style.PinkBlackNav
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(THEMES[THEME_INDEX]);

        setContentView(R.layout.activity_main);

        Executors.newFixedThreadPool(1).execute(() -> {
            String FETCH_URL = MainActivity.this.getString(R.string.fetchUrl);
            checkForUpdate(FETCH_URL);
        });
        setupUIViews();
        setupMenuBar();
        setupBottomBar();

    }

    private void setupUIViews () {

        queue = Queue.getInstance(MainActivity.this);
        bottomBar = findViewById(R.id.bottomNavigationBar);
        bottomBar.setSelectedItemId(R.id.menuAnime);
        searchBar = findViewById(R.id.searchViewContainer);
        setupSearchBar();
    }

    private void setupMenuBar () {

        findViewById(R.id.main_activity_menu).setOnClickListener( view -> {

            View customDialog = LayoutInflater.from(this).inflate(R.layout.theme_view, findViewById(R.id.root), false);
            ThemeViewBinding themeBinding = ThemeViewBinding.bind(customDialog);
            AlertDialog mainDialog = new MaterialAlertDialogBuilder(this)
                    .setView(customDialog)
                    .setTitle("App Theme")
                    .create();
            mainDialog.show();

            switch (THEME_INDEX) {
                case 0: {
                    themeBinding.deepSkyBlueTheme.setBackgroundColor(Color.LTGRAY);
                    break;
                }
                case 1: {
                    themeBinding.brownTheme.setBackgroundColor(Color.LTGRAY);
                    break;
                }
                case 2: {
                    themeBinding.yellowTheme.setBackgroundColor(Color.LTGRAY);
                    break;
                }
                case 3: {
                    themeBinding.purpleTheme.setBackgroundColor(Color.LTGRAY);
                    break;
                }
                case 4: {
                    themeBinding.pinkTheme.setBackgroundColor(Color.LTGRAY);
                    break;
                }
                case 5: {
                    themeBinding.darkBlackTheme.setBackgroundColor(Color.LTGRAY);
                    break;
                }
                case 6: {
                    themeBinding.darkPurpleTheme.setBackgroundColor(Color.LTGRAY);
                    break;
                }
                case 7: {
                    themeBinding.blackTheme.setBackgroundColor(Color.LTGRAY);
                    break;
                }
                case 8: {
                    themeBinding.blueBlackTheme.setBackgroundColor(Color.LTGRAY);
                    break;
                }
                case 9: {
                    themeBinding.brownBlackTheme.setBackgroundColor(Color.LTGRAY);
                    break;
                }
                case 10: {
                    themeBinding.yellowBlackTheme.setBackgroundColor(Color.LTGRAY);
                    break;
                }
                case 11: {
                    themeBinding.pinkBlackTheme.setBackgroundColor(Color.LTGRAY);
                    break;
                }
            }

            themeBinding.deepSkyBlueTheme.setOnClickListener(v -> saveTheme(0));
            themeBinding.brownTheme.setOnClickListener(v -> saveTheme(1));
            themeBinding.yellowTheme.setOnClickListener(v -> saveTheme(2));
            themeBinding.purpleTheme.setOnClickListener(v -> saveTheme(3));
            themeBinding.pinkTheme.setOnClickListener(v -> saveTheme(4));
            themeBinding.darkBlackTheme.setOnClickListener(v -> saveTheme(5));
            themeBinding.darkPurpleTheme.setOnClickListener(v -> saveTheme(6));
            themeBinding.blackTheme.setOnClickListener(v -> saveTheme(7));
            themeBinding.blueBlackTheme.setOnClickListener(v -> saveTheme(8));
            themeBinding.brownBlackTheme.setOnClickListener(v -> saveTheme(9));
            themeBinding.yellowBlackTheme.setOnClickListener(v -> saveTheme(10));
            themeBinding.pinkBlackTheme.setOnClickListener(v -> saveTheme(11));

        });

    }

    private void setupSearchBar () {
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = searchBar.getText().toString();
                searchAnime(query);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void searchAnime (String query) {
        if (runnable != null) {
            handler.removeCallbacks(runnable);
        }
        runnable = () -> {
            if (fragment != null) {
                if (fragment instanceof AnimeFragment) {
                    ((AnimeFragment) fragment).searchQuery(query);
                }
            }
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(searchBar.getWindowToken(), 0);
            }
        };
        handler.postDelayed(runnable, 1000);
    }

    private void setupBottomBar () {

        fragment = new AnimeFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.mainContainer, fragment).commit();

        bottomBar.setOnNavigationItemSelectedListener(item -> {

            int itemID = item.getItemId();

            if (itemID == R.id.menuSports) {
                searchBar.setVisibility(View.GONE);
                fragment = new SportsFragment();
            } else if (itemID == R.id.menuEntertainment) {
                searchBar.setVisibility(View.GONE);
                fragment = new EntertainmentFragment();
            } else if (itemID == R.id.menuAnime) {
                searchBar.setVisibility(View.VISIBLE);
                fragment = new AnimeFragment();
            } else if (itemID == R.id.menuMusic) {
                searchBar.setVisibility(View.GONE);
                fragment = new MusicFragment();
            } else if (itemID == R.id.menuNews) {
                searchBar.setVisibility(View.GONE);
                fragment = new NewsFragment();
            } else {
                searchBar.setVisibility(View.VISIBLE);
                fragment = new AnimeFragment();
            }

            getSupportFragmentManager().beginTransaction().replace(R.id.mainContainer, fragment).commit();

            return true;

        });

    }

    private void checkForUpdate (String FETCH_URL) {
        if (queue != null) {
            queue.makeRequest(
                    Request.Method.GET,
                    FETCH_URL,
                    response -> {

                        try {

                            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
                            int currentVersionCode = info.versionCode;
                            JSONObject jData = new JSONObject(response);

                            int latestVersionCode = jData.getInt("latestVersionCode");
                            JSONArray releaseNotes = jData.getJSONArray("releaseNotes");
                            String notes = "";
                            for (int i = 0; i < releaseNotes.length(); i++) {
                                notes += "\n\n" + releaseNotes.get(i);
                            }

                            if (currentVersionCode < latestVersionCode) {

                                String url = jData.getString("url");
                                String latestVersionName = jData.getString("latestVersion");
                                String finalNotes = notes;
                                runOnUiThread(() -> showUpdateAppDialog(url, info.versionName, latestVersionName, finalNotes));

                            }

                        } catch (Exception e) {
                            Log.d("Arjun", e.getLocalizedMessage());
                        }

                    });
        }
    }

    public void showUpdateAppDialog (String url, String current, String latest, String notes) {

        final AlertDialog dialog = new MaterialAlertDialogBuilder(this, R.style.alertDialog)
                .setTitle("Product update!")
                .setMessage("A new version is available.\nWould you like to update now?\n" +
                        "(Current: " + current + " Latest: " + latest + ")" + notes
                )
                .setBackground(new ColorDrawable(resolveAttrColor(R.attr.ThemePrimary)))
                .setPositiveButton("Update", (dialog1, which) -> {
                    try {
                        UpdateApk.downloadApk(this, url);
                    } catch(Exception e) {
                        Toast.makeText(getApplicationContext(), "Something went wrong.\nTry again later!", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(resolveAttrColor(R.attr.ThemeTextColorSecondary));
        dialog.setCancelable(false);

    }

    private void saveTheme (int index) {

        SharedPreferences prefs = getSharedPreferences("AppThemes", Context.MODE_PRIVATE);

        if (index == -1) {
            THEME_INDEX = prefs.getInt("themeIndex", 0);
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

    @Override
    protected void onStop() {
        super.onStop();
        if (queue != null) {

            queue.getRequestQueue().cancelAll(Queue.TAG);

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (queue != null) {
            queue.getRequestQueue().stop();
            queue.onStop();
        }
        queue = null;
        jsonData = null;
    }

    private int resolveAttrColor(int attrResId) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getTheme();
        theme.resolveAttribute(attrResId, typedValue, true);
        return typedValue.data;
    }

}