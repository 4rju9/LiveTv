package cf.arjun.dev.livetv.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cf.arjun.dev.livetv.MainActivity;
import cf.arjun.dev.livetv.R;
import cf.arjun.dev.livetv.repository.Queue;

public class EpisodeAdapter extends RecyclerView.Adapter<EpisodeAdapter.ViewHolder> {

    private JSONArray episodes;
    public static Queue queue;
    private Context context;

    public EpisodeAdapter(Context context, JSONArray episodes) {
        this.context = context;
        this.episodes = episodes;
        queue = Queue.getInstance(context);
    }

    public void setData (JSONArray episodes) {
        this.episodes = episodes;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.episode_single_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JSONObject episode = null;
        String episodeId = "";
        try {
            episode = episodes.getJSONObject(position);
            episodeId = episode.getString("id");
            String text1 = "Episode: #" + episode.getInt("episode_no");
            holder.number.setText(text1);
            String text2 = "Title: " + episode.getString("title");
            holder.title.setText(text2);
            String text3 = "Filler: " + (episode.getBoolean("filler") ? "Yes" : "No");
            holder.isFiller.setText(text3);

            // Hide initially to prevent issues
            holder.llSub.setVisibility(View.GONE);
            holder.llDub.setVisibility(View.GONE);
            holder.llRaw.setVisibility(View.GONE);

            String finalEpisodeId = episodeId;
            JSONObject finalEpisode = episode;
            holder.itemView.setOnClickListener(v -> {
                holder.progressBar.setVisibility(View.VISIBLE);

                if (queue != null) {
                    try {
                        queue.makeRequest(
                                Request.Method.GET,
                                String.format(MainActivity.ANIME_BASE_URL + "/api/servers/%s", finalEpisodeId),
                                response -> {
                                    holder.progressBar.setVisibility(View.GONE);
                                    try {
                                        JSONObject responseObject = new JSONObject(response);
                                        JSONArray results = responseObject.getJSONArray("results");

                                        JSONArray subArray = new JSONArray();
                                        JSONArray dubArray = new JSONArray();
                                        JSONArray rawArray = new JSONArray();

                                        for (int i = 0; i < results.length(); i++) {
                                            JSONObject item = results.getJSONObject(i);
                                            String type = item.getString("type");

                                            JSONObject serverObj = new JSONObject();
                                            serverObj.put("serverId", item.getInt("server_id"));
                                            serverObj.put("serverName", item.getString("serverName"));

                                            if ("sub".equals(type)) {
                                                subArray.put(serverObj);
                                            } else if ("dub".equals(type)) {
                                                dubArray.put(serverObj);
                                            } else if ("raw".equals(type)) {
                                                rawArray.put(serverObj);
                                            }
                                        }

                                        // Existing visibility and adapter setup
                                        if (subArray.length() > 0) {
                                            holder.llSub.setVisibility(View.VISIBLE);
                                            holder.rvSub.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
                                            holder.rvSub.setAdapter(new ServerAdapter(context, subArray, finalEpisodeId, "sub", finalEpisode.getString("title")));
                                        }

                                        if (dubArray.length() > 0) {
                                            holder.llDub.setVisibility(View.VISIBLE);
                                            holder.rvDub.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
                                            holder.rvDub.setAdapter(new ServerAdapter(context, dubArray, finalEpisodeId, "dub", finalEpisode.getString("title")));
                                        }

                                        if (rawArray.length() > 0) {
                                            holder.llRaw.setVisibility(View.VISIBLE);
                                            holder.rvRaw.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
                                            holder.rvRaw.setAdapter(new ServerAdapter(context, rawArray, finalEpisodeId, "raw", finalEpisode.getString("title")));
                                        }

                                    } catch (JSONException error) { Log.d("x4rju9", "Error In Episode Adapter:\n" + error.getMessage()); }
                                },
                                error -> {
                                    holder.progressBar.setVisibility(View.GONE);
                                    Toast.makeText(context, "Something went wrong, " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                        );
                    } catch (Exception ignore) {}
                }

            });

        } catch (JSONException e) {
            Log.d("x4rju9", e.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        return episodes.length();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView number, title, isFiller;
        RecyclerView rvSub, rvDub, rvRaw;
        LinearLayout llSub, llDub, llRaw;
        ProgressBar progressBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            number = itemView.findViewById(R.id.episodeNumber);
            title = itemView.findViewById(R.id.episodeTitle);
            isFiller = itemView.findViewById(R.id.isFiller);
            progressBar = itemView.findViewById(R.id.progressBar);
            rvSub = itemView.findViewById(R.id.subServersRecyclerView);
            rvDub = itemView.findViewById(R.id.dubServersRecyclerView);
            rvRaw = itemView.findViewById(R.id.rawServersRecyclerView);
            llSub = itemView.findViewById(R.id.subServers);
            llDub = itemView.findViewById(R.id.dubServers);
            llRaw = itemView.findViewById(R.id.rawServers);
        }
    }

    public JSONArray getReversed() {
        try {

            JSONArray reversedArray = new JSONArray();
            int length = episodes.length();
            for (int i = length - 1; i >= 0; i--) {
                reversedArray.put(episodes.get(i));
            }
            return reversedArray;
        } catch (Exception ignore) { return episodes; }
    }

}