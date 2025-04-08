package cf.arjun.dev.livetv.others;

import android.content.Context;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import cf.arjun.dev.livetv.R;
import cf.arjun.dev.livetv.adapters.EpisodeAdapter;

public class EpisodeDialog {

    private static AlertDialog currentDialog; // Store reference to the dialog
    private static Handler handler = new Handler();
    private static Runnable runnable;
    public static boolean ordered = true;
    public static void show(Context context, String jsonResponse, String poster) {
        try {

            if (currentDialog != null && currentDialog.isShowing()) {
                return;
            }

            JSONObject response = new JSONObject(jsonResponse);
            JSONObject data = response.getJSONObject("data");
            JSONArray original = data.getJSONArray("episodes");

            // Inflate custom layout
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_episode, null);
            ImageView imageView = dialogView.findViewById(R.id.dialogImage);
            EditText searchBar = dialogView.findViewById(R.id.searchViewContainer);
            TextView changeOrder = dialogView.findViewById(R.id.changeOrder);
            changeOrder.setText(ordered ? R.string.tv_show_newest : R.string.tv_show_oldest);
            RecyclerView recyclerView = dialogView.findViewById(R.id.recyclerView);

            // Load Image (Replace URL with actual image URL)
            Glide.with(context).load(poster).into(imageView);

            // Set up RecyclerView
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            EpisodeAdapter adapter = new EpisodeAdapter(context, original);
            recyclerView.setAdapter(adapter);


            changeOrder.setOnClickListener( v -> {
                ordered = !ordered;
                changeOrder.setText(ordered ? R.string.tv_show_newest : R.string.tv_show_oldest);
                adapter.setData(ordered ? original : adapter.getReversed());
            });

            searchBar.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String query = searchBar.getText().toString();

                    if (runnable != null) {
                        handler.removeCallbacks(runnable);
                    }
                    runnable = () -> {
                        // Search Query
                        if (query.isEmpty()) {
                            adapter.setData(original);
                        }
                        else {
                            try {
                                JSONArray episodes = new JSONArray();
                                int array_size = original.length();
                                for (int i=0; i<array_size; i++) {
                                    JSONObject episode = original.getJSONObject(i);
                                    String index = String.valueOf(i+1);
                                    if (index.contains(query)) {
                                        episodes.put(episode);
                                    }
                                }
                                adapter.setData(episodes);
                            } catch (Exception ignore) {}
                        }
                        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(searchBar.getWindowToken(), 0);
                        }
                    };
                    handler.postDelayed(runnable, 1000);
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });

            // Create & Show Dialog
            currentDialog = new MaterialAlertDialogBuilder(context)
                    .setView(dialogView)
                    .show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
