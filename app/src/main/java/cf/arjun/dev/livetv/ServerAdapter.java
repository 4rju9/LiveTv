package cf.arjun.dev.livetv;

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

    public ServerAdapter(Context context, JSONArray servers, String episodeId, String serverCategory) {
        this.context = context;
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

            holder.serverName.setText(name);
            holder.serverName.setOnClickListener(view -> {

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
                                    try {
                                        JSONObject object = new JSONObject(response);
                                        object = object.getJSONObject("data");
                                        String url = object.getJSONArray("sources").getJSONObject(0).getString("url");
                                        Intent intent = new Intent(context, PlayerViewActivity.class);
                                        intent.putExtra("url", url);
                                        intent.setAction("HLS");
                                        context.startActivity(intent);
                                    } catch (JSONException ignore) {}
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