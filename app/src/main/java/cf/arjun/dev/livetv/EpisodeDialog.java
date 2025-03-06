package cf.arjun.dev.livetv;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

public class EpisodeDialog {

    private static AlertDialog currentDialog; // Store reference to the dialog
    public static void show(Context context, String jsonResponse, String poster) {
        try {

            if (currentDialog != null && currentDialog.isShowing()) {
                return;
            }

            JSONObject response = new JSONObject(jsonResponse);
            JSONObject data = response.getJSONObject("data");
            JSONArray episodes = data.getJSONArray("episodes");

            // Inflate custom layout
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_episode, null);
            ImageView imageView = dialogView.findViewById(R.id.dialogImage);
            RecyclerView recyclerView = dialogView.findViewById(R.id.recyclerView);

            // Load Image (Replace URL with actual image URL)
            Glide.with(context).load(poster).into(imageView);

            // Set up RecyclerView
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            recyclerView.setAdapter(new EpisodeAdapter(context, episodes));

            // Create & Show Dialog
            currentDialog = new MaterialAlertDialogBuilder(context)
                    .setView(dialogView)
                    .show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
