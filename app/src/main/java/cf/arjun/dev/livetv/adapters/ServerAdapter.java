package cf.arjun.dev.livetv.adapters;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import cf.arjun.dev.livetv.MainActivity;
import cf.arjun.dev.livetv.PlayerViewActivity;
import cf.arjun.dev.livetv.R;

public class ServerAdapter extends RecyclerView.Adapter<ServerAdapter.ServerViewHolder> {

    private Context context;
    private JSONArray servers;
    private String episodeId, serverCategory, title;
    private ProgressDialog dialog;

    public ServerAdapter(Context context, JSONArray servers, String episodeId, String serverCategory, String title) {
        this.context = context;
        this.dialog = new ProgressDialog(context);
        this.servers = servers;
        this.episodeId = episodeId;
        this.serverCategory = serverCategory;
        this.title = title;
    }

    @NonNull
    @Override
    public ServerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ServerViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(R.layout.server_item, parent, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ServerViewHolder holder, int position) {
        try {
            JSONObject server = servers.length() == 2 ? servers.getJSONObject(1) : servers.getJSONObject(position);
            String name = server.getString("serverName");
            String server_text = "Server " + (position + 1); // "Server 1"

            holder.serverName.setText(server_text);
            holder.serverName.setOnClickListener(view -> {

                dialog.setTitle(server_text);
                dialog.setMessage("Streaming in progress...");
                dialog.show();

                if (EpisodeAdapter.queue != null) {
                    try {

                        EpisodeAdapter.queue.makeRequest(
                                Request.Method.GET,
                                String.format(
                                        MainActivity.ANIME_BASE_URL
                                                + "/api/v2/hianime/episode/sources?animeEpisodeId=%s?server=%s&category=%s"
                                        , episodeId, name, serverCategory
                                ),
                                response -> {
                                    dialog.dismiss();
                                    handleResponse(response);
                                },
                                error -> {
                                    Toast.makeText(context, "Something went wrong, " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                        );

                    } catch (Exception ignore) {}
                }

            });


        } catch (JSONException ignore) {}
    }

    @Override
    public int getItemCount() {
        return servers.length();
    }

    public class ServerViewHolder extends RecyclerView.ViewHolder {

        public TextView serverName;

        public ServerViewHolder(@NonNull View itemView) {
            super(itemView);
            serverName = itemView.findViewById(R.id.serverButton);
        }
    }

    private void handleResponse (String response) {
        try {
            JSONObject object = new JSONObject(response);
            object = object.getJSONObject("data");
            Intent intent = new Intent(context, PlayerViewActivity.class);
            if (object.getJSONArray("sources").length() > 0) {

                String url = object.getJSONArray("sources").getJSONObject(0).getString("url");
                JSONArray tracks = object.getJSONArray("tracks");

                int track_size = tracks.length();

                if (track_size > 0) {

                    List<String> track_urls = new ArrayList<>();
                    List<String> track_labels = new ArrayList<>();

                    for (int i = 0; i < track_size; i++) {
                        JSONObject track = tracks.getJSONObject(i);
                        if (track.has("label")) {
                            track_urls.add(track.getString("file"));
                            track_labels.add(track.getString("label"));
                        }
                    }
                    intent.putExtra("has_track", !track_urls.isEmpty());
                    intent.putExtra("track_urls", track_urls.toArray(new String[0]));
                    intent.putExtra("track_labels", track_labels.toArray(new String[0]));
                } else {
                    intent.putExtra("has_track", false);
                }

                intent.putExtra("url", url);
                intent.putExtra("title", this.title);
                intent.setAction("HLS");
                context.startActivity(intent);

            } else {
                dialog.dismiss();
                Toast.makeText(context, "No sources found!", Toast.LENGTH_SHORT).show();
                Log.d("Arjun", "No sources found!");

                // Fallback to Default Source
                intent.putExtra("url", MainActivity.ANIME_FALLBACK_SOURCE);
                intent.putExtra("title", this.title);
                intent.setAction("HLS");
                context.startActivity(intent);

            }
        } catch (JSONException e) { Log.d("Arjun", Objects.requireNonNull(e.getMessage())); }
    }

}