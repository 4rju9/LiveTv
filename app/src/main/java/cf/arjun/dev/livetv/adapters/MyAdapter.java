package cf.arjun.dev.livetv.adapters;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

import cf.arjun.dev.livetv.MainActivity;
import cf.arjun.dev.livetv.PlayerViewActivity;
import cf.arjun.dev.livetv.R;
import cf.arjun.dev.livetv.others.EpisodeDialog;
import cf.arjun.dev.livetv.repository.Queue;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {

    private JSONArray data = new JSONArray();
    private Context context;

    public boolean isSearched = false, isAnime = false;
    private Queue queue;
    private ProgressDialog dialog;
    private SharedPreferences logPrefs;
    public MyAdapter (Context context) {
        this.context = context;
        this.dialog = new ProgressDialog(context);
        queue = Queue.getInstance(context);
        logPrefs = context.getSharedPreferences("log", Context.MODE_PRIVATE);
    }

    public void setData (JSONArray data) {
        this.data = data;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.myadapter_single_item, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return data.length();
    }

    @SuppressLint("StringFormatInvalid")
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        Intent intent = null;
        String anime_id = "";
        String poster = "";

        if (!isSearched) {
            try {
                intent = new Intent(context, PlayerViewActivity.class);
                JSONObject object = data.getJSONObject(position);
                holder.labelQuality.setVisibility(View.VISIBLE);
                holder.labelAudio.setVisibility(View.VISIBLE);
                holder.labelDuration.setVisibility(View.GONE);
                holder.labelType.setVisibility(View.GONE);
                holder.labelEpisodes.setVisibility(View.GONE);
                holder.title.setText(object.getString("title"));
                holder.quality.setText(object.getString("quality"));
                holder.lang.setText(object.getString("language"));

                String logo = object.getString("logo");
                if (!(logo.isEmpty())) {
                    Glide.with(context).load(logo).into(holder.image);
                }

                String url = object.getString("url");
                intent.putExtra("url", url);
                intent.putExtra("title", object.getString("title"));

                boolean hasDrm = object.getBoolean("hasDrm");
                if (hasDrm) {
                    intent.setAction("DRM");
                    intent.putExtra("k", object.getString("key"));
                    intent.putExtra("kid", object.getString("keyId"));
                } else intent.setAction("HLS");

            } catch (JSONException ignore) {}
        } else {
            try {
                JSONObject object = data.getJSONObject(position);

                poster = object.getString("poster");
                if (!(poster.isEmpty())) {
                    Glide.with(context).load(poster).into(holder.image);
                }

                holder.labelType.setVisibility(View.VISIBLE);
                holder.labelEpisodes.setVisibility(View.VISIBLE);
                holder.labelQuality.setVisibility(View.GONE);
                holder.labelAudio.setVisibility(View.GONE);
                holder.title.setText(object.getString("title"));

                if (!isAnime) {
                    holder.labelDuration.setVisibility(View.VISIBLE);
                    holder.duration.setText(object.getString("duration"));
                }

                JSONObject episodes = object.getJSONObject("tvInfo");
                holder.type.setText(episodes.getString("showType"));
                String subs = episodes.getString("sub");
                String dubs = episodes.getString("dub");
                if (subs != null && !subs.isEmpty() && !subs.contentEquals("null")) {
                    holder.labelSubs.setVisibility(View.VISIBLE);
                    holder.subs.setVisibility(View.VISIBLE);
                    holder.subs.setText(subs);
                }
                if (dubs != null && !dubs.isEmpty() && !dubs.contentEquals("null")) {
                    holder.labelDubs.setVisibility(View.VISIBLE);
                    holder.dubs.setVisibility(View.VISIBLE);
                    holder.dubs.setText(dubs);
                }

                anime_id = object.getString("id");

            } catch (JSONException e) {
                Log.d("x4rju9", e.getMessage());
            }
        }

        Intent finalIntent = intent;
        String finalAnime_id = anime_id;
        String finalPoster = poster;
        holder.root.setOnClickListener(view -> {

            if (!isSearched) {
                holder.root.getContext().startActivity(finalIntent);
            } else {
                dialog.setTitle("Loading Episodes !!");
                dialog.setMessage("Please wait....");
                dialog.show();
                getEpisodesList(finalAnime_id, finalPoster);
            }

        });

    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        TextView title, quality, lang, dubs, subs, type, duration, labelSubs, labelDubs;
        LinearLayout labelQuality, labelAudio, labelDuration, labelType, labelEpisodes;
        CardView root;
        ImageView image;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.tvTitle);
            quality = itemView.findViewById(R.id.tvQuality);
            lang = itemView.findViewById(R.id.tvLang);
            image = itemView.findViewById(R.id.ivSingleItem);
            dubs = itemView.findViewById(R.id.tvDubs);
            subs = itemView.findViewById(R.id.tvSubs);
            labelSubs = itemView.findViewById(R.id.labelSubs);
            labelDubs = itemView.findViewById(R.id.labelDubs);
            type = itemView.findViewById(R.id.tvType);
            duration = itemView.findViewById(R.id.tvDuration);
            labelQuality = itemView.findViewById(R.id.labelQuality);
            labelAudio = itemView.findViewById(R.id.labelAudio);
            labelDuration = itemView.findViewById(R.id.labelDuration);
            labelType = itemView.findViewById(R.id.labelType);
            labelEpisodes = itemView.findViewById(R.id.labelEpisodes);
            root = itemView.findViewById(R.id.root);

        }

    }

    private void getEpisodesList (String id, String poster) {
        if (queue != null) {
            try {
                queue.makeRequest(
                        Request.Method.GET,
                        String.format(MainActivity.ANIME_BASE_URL + "/api/episodes/%s", id),
                        response -> {
                            dialog.dismiss();
                            EpisodeDialog.show(context, response, poster);
                        },
                        error -> {
                            dialog.dismiss();
                            Toast.makeText(context, "Try again later.", Toast.LENGTH_SHORT).show();
                            if (error.networkResponse != null) {
                                String errorBody = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                                logPrefs.edit().putString(
                                        "log",
                                        "Code: " + error.networkResponse.statusCode + ", Body: " + errorBody
                                ).apply();
                            } else {
                                logPrefs.edit().putString(
                                        "log",
                                        "No network response" + error
                                ).apply();
                            }
                        }
                );
            } catch (Exception ignore) {}
        }
    }

}