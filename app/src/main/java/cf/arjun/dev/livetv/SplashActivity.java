package cf.arjun.dev.livetv;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import com.android.volley.Request;

import org.json.JSONException;
import org.json.JSONObject;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private Queue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_activity);

        String FETCH_URL = getString(R.string.getUrl);
        queue = Queue.getInstance(SplashActivity.this);
        getList(FETCH_URL);

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (queue != null) {
            queue.onStop();
        }
        queue = null;
    }
}