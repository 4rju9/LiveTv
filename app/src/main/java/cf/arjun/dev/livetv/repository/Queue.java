package cf.arjun.dev.livetv.repository;

import android.content.Context;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class Queue {

    public static final String TAG = "REQUEST";
    private static Queue instance;
    private RequestQueue requestQueue;
    private static Context ctx;

    private Queue(Context context) {
        ctx = context;
        requestQueue = getRequestQueue();
    }

    public static synchronized Queue getInstance(Context context) {
        if (instance == null) {
            instance = new Queue(context);
        }
        return instance;
    }

    public RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            requestQueue = Volley.newRequestQueue(ctx.getApplicationContext());
        }
        return requestQueue;
    }

    public void makeRequest (int method, String url, Response.Listener<String> response) {

        StringRequest request = new StringRequest(
                method,
                url,
                response,
                error -> Toast.makeText(ctx, "Something went wrong, Come back later!", Toast.LENGTH_LONG).show()
        );
        request.setShouldCache(false);
        request.setTag(TAG);
        getRequestQueue().add(request);

    }

    public void makeRequest (int method, String url, Response.Listener<String> response, Response.ErrorListener error) {

        StringRequest request = new StringRequest(
                method,
                url,
                response,
                error
        );
        request.setRetryPolicy(new DefaultRetryPolicy(
                10_000,
                2,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        request.setTag(TAG);
        request.setShouldCache(true);
        getRequestQueue().add(request);

    }

    public void onStop () {

        if (requestQueue != null) {
            requestQueue.cancelAll(TAG);
            requestQueue.stop();
        }
        requestQueue = null;
        instance = null;

    }

}
