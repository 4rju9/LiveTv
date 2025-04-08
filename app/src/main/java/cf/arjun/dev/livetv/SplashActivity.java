package cf.arjun.dev.livetv;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;

import org.json.JSONException;
import org.json.JSONObject;

import cf.arjun.dev.livetv.repository.Queue;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private Queue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        saveTheme(-1);
        setTheme(MainActivity.THEMES[MainActivity.THEME_INDEX]);

        setContentView(R.layout.splash_activity);

        queue = Queue.getInstance(SplashActivity.this);
        String FETCH_URL = getString(R.string.getUrl);
        getList(FETCH_URL);

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

    private void getList (String FETCH_URL) {
        if (queue != null) {
            queue.makeRequest(
                    Request.Method.GET,
                    FETCH_URL,
                    response -> {
                        try {
                            MainActivity.jsonData = new JSONObject(response);
                            MainActivity.jsonAnimeData = MainActivity.jsonData.getJSONObject("anime");
                            MainActivity.ANIME_BASE_URL = MainActivity.jsonAnimeData.getString("base");
                            MainActivity.jsonAnimeData = MainActivity.jsonAnimeData.getJSONObject("data");
                            MainActivity.MOST_POPULAR = MainActivity.jsonAnimeData.getJSONArray("mostPopularAnimes");
                            MainActivity.TOP_AIRING = MainActivity.jsonAnimeData.getJSONArray("topAiringAnimes");
                            MainActivity.NEW_EPISODE_RELEASES = MainActivity.jsonAnimeData.getJSONArray("latestEpisodeAnimes");
                            MainActivity.MOST_FAVORITES = MainActivity.jsonAnimeData.getJSONArray("mostFavoriteAnimes");
                            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } catch (JSONException error) {
                            Toast.makeText(getApplicationContext(), "Something went wrong. Try again later!", Toast.LENGTH_SHORT).show();
                            Log.d("x4rju9", error.getLocalizedMessage());
                        }
                    });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (queue != null) {
            queue.onStop();
        }
        queue = null;
    }
}