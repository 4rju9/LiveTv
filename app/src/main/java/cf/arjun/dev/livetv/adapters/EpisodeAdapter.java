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
import cf.arjun.dev.livetv.repository.Queue;
import cf.arjun.dev.livetv.R;

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
            episodeId = episode.getString("episodeId");
            String text1 = "Episode: #" + episode.getInt("number");
            holder.number.setText(text1);
            String text2 = "Title: " + episode.getString("title");
            holder.title.setText(text2);
            String text3 = "Filler: " + (episode.getBoolean("isFiller") ? "Yes" : "No");
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
                                String.format(MainActivity.ANIME_BASE_URL + "/api/v2/hianime/episode/servers?animeEpisodeId=%s", finalEpisodeId),
                                response -> {
                                    holder.progressBar.setVisibility(View.GONE);
                                    try {
                                        JSONObject object = new JSONObject(response).getJSONObject("data");
                                        JSONArray sub = object.getJSONArray("sub");
                                        JSONArray dub = object.getJSONArray("dub");
                                        JSONArray raw = object.getJSONArray("raw");

                                        if (sub.length() > 0) {
                                            holder.llSub.setVisibility(View.VISIBLE);
                                            holder.rvSub.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
                                            holder.rvSub.setAdapter(new ServerAdapter(context, sub, finalEpisodeId, "sub", finalEpisode.getString("title")));
                                        }

                                        if (dub.length() > 0) {
                                            holder.llDub.setVisibility(View.VISIBLE);
                                            holder.rvDub.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
                                            holder.rvDub.setAdapter(new ServerAdapter(context, dub, finalEpisodeId, "dub", finalEpisode.getString("title"))); // Fix applied
                                        }

                                        if (raw.length() > 0) {
                                            holder.llRaw.setVisibility(View.VISIBLE);
                                            holder.rvRaw.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
                                            holder.rvRaw.setAdapter(new ServerAdapter(context, raw, finalEpisodeId, "raw", finalEpisode.getString("title"))); // Fix applied
                                        }

                                    } catch (JSONException ignore) {
                                    }
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
            Log.d("Arjun", e.getMessage());
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