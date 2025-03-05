package cf.arjun.dev.livetv;

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

public class ServerAdapter extends RecyclerView.Adapter<ServerAdapter.ServerViewHolder> {

    private Context context;
    private JSONArray servers;
    private String episodeId;
    private String serverCategory;
    private ProgressDialog dialog;

    public ServerAdapter(Context context, JSONArray servers, String episodeId, String serverCategory) {
        this.context = context;
        this.dialog = new ProgressDialog(context);
        this.servers = servers;
        this.episodeId = episodeId;
        this.serverCategory = serverCategory;
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
            JSONObject server = servers.getJSONObject(position);
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
                                        context.getString(R.string.searchAnimeUrl)
                                                + "/api/v2/hianime/episode/sources?animeEpisodeId=%s?server=%s&category=%s"
                                        , episodeId, name, serverCategory
                                ),
                                response -> {
                                    dialog.dismiss();
                                    try {
                                        JSONObject object = new JSONObject(response);
                                        object = object.getJSONObject("data");
                                        if (object.getJSONArray("sources").length() > 0) {
                                            Intent intent = new Intent(context, PlayerViewActivity.class);
                                            String url = object.getJSONArray("sources").getJSONObject(0).getString("url");
                                            JSONArray tracks = object.getJSONArray("tracks");
                                            intent.putExtra("has_track", false);
                                            for (int i = 0; i < tracks.length(); i++) {
                                                JSONObject track = tracks.getJSONObject(i);
                                                Log.d("Arjun", track.toString());
                                                if (track.has("label") && track.getString("label").contentEquals("English")) {
                                                    intent.putExtra("track_url", track.getString("file"));
                                                    intent.putExtra("track_label", "en");
                                                    intent.putExtra("has_track", true);
                                                    break;
                                                }
                                            }
                                            intent.putExtra("url", url);
                                            intent.setAction("HLS");
                                            context.startActivity(intent);
                                        } else {
                                            dialog.dismiss();
                                            Toast.makeText(context, "No sources found!", Toast.LENGTH_SHORT).show();
                                            Log.d("Arjun", "No sources found!");
                                        }
                                    } catch (JSONException e) { Log.d("Arjun", e.getMessage()); }
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
}