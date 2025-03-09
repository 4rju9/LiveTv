package cf.arjun.dev.livetv;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        fetchAnimeData();

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
                            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } catch (JSONException ignore) {}
                    });
        }
    }

    private void fetchAnimeData () {
        String base_url = getString(R.string.searchAnimeUrl);
        if (queue != null) {
            try {
                queue.makeRequest(
                        Request.Method.GET,
                        base_url + "/api/v2/hianime/home",
                        response -> {
                            try {
                                JSONObject result = new JSONObject(response);
                                if (result.getBoolean("success")) {
                                    result = result.getJSONObject("data");
                                    MainActivity.MOST_POPULAR = result.getJSONArray("mostPopularAnimes");
                                    MainActivity.TOP_AIRING = result.getJSONArray("topAiringAnimes");
                                } else Log.d("Arjun", "Something went wrong!");
                            } catch (JSONException error) {
                                Toast.makeText(this, "Something went wrong, " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            } finally {
                                String FETCH_URL = getString(R.string.getUrl);
                                getList(FETCH_URL);
                            }
                        },
                        error -> {
                            Toast.makeText(this, "Something went wrong, Come back later!", Toast.LENGTH_LONG).show();
                        });
            } catch (Exception ignore) {}
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