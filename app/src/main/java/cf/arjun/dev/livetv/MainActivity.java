package cf.arjun.dev.livetv;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import com.android.volley.Request;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private Queue queue;
    public static JSONObject jsonData = null;
    public static String MUSIC = "music";
    public static String SPORTS = "sports";
    public static String ENTERTAINMENT = "entertainment";
    public static String MOVIES = "movies";
    public static String NEWS = "news";

    // UI Views.
    private BottomNavigationView bottomBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Executors.newFixedThreadPool(1).execute(() -> {
            String FETCH_URL = MainActivity.this.getString(R.string.fetchUrl);
            checkForUpdate(FETCH_URL);
        });
        setupUIViews();
        setupBottomBar();

    }

    private void setupUIViews () {

        queue = Queue.getInstance(MainActivity.this);
        bottomBar = findViewById(R.id.bottomNavigationBar);
        bottomBar.setSelectedItemId(R.id.menuMovies);

    }

    private void setupBottomBar () {

        getSupportFragmentManager().beginTransaction().replace(R.id.mainContainer, new MoviesFragment()).commit();

        bottomBar.setOnNavigationItemSelectedListener(item -> {

            Fragment fragment;
            int itemID = item.getItemId();

            if (itemID == R.id.menuSports) {
                fragment = new SportsFragment();
            } else if (itemID == R.id.menuEntertainment) {
                fragment = new EntertainmentFragment();
            } else if (itemID == R.id.menuMovies) {
                fragment = new MoviesFragment();
            } else if (itemID == R.id.menuMusic) {
                fragment = new MusicFragment();
            } else if (itemID == R.id.menuNews) {
                fragment = new NewsFragment();
            } else {
                fragment = null;
            }

            if (fragment != null) {
                getSupportFragmentManager().beginTransaction().replace(R.id.mainContainer, fragment).commit();
            }

            return true;

        });

    }

    private void checkForUpdate (String FETCH_URL) {
        if (queue != null) {
            queue.makeRequest(Request.Method.GET,
                    FETCH_URL,
                    response -> {

                        try {

                            Log.d("4rju9", "Inside the response listener");
                            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
                            int currentVersionCode = info.versionCode;
                            JSONObject jData = new JSONObject(response);

                            int latestVersionCode = jData.getInt("latestVersionCode");

                            if (currentVersionCode < latestVersionCode) {

                                String url = jData.getString("url");
                                String latestVersionName = jData.getString("latestVersion");
                                runOnUiThread(() -> showUpdateAppDialog(url, info.versionName, latestVersionName));

                            }

                        } catch (Exception ignore) {}

                    });
        }
    }

    public void showUpdateAppDialog (String url, String current, String latest) {

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Product update!")
                .setMessage("A new version is available.\nWould you like to update now?\n" +
                        "(Current: " + current + " Latest: " + latest + ")")
                .setPositiveButton("Update", (dialog1, which) -> {
                    try {
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_VIEW);
                        intent.addCategory(Intent.CATEGORY_BROWSABLE);
                        intent.setData(Uri.parse(url));
                        startActivity(intent);
                        new Handler().postDelayed( () -> finish(), 2000);
                    } catch(Exception e) {
                        Toast.makeText(getApplicationContext(), "Something went wrong.\nTry again later!", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
        dialog.setCancelable(false);

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



}