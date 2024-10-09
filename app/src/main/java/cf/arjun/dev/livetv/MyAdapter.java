package cf.arjun.dev.livetv;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {

    private JSONArray data = new JSONArray();
    private Context context;

    public MyAdapter (Context context) { this.context = context; }

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

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        Intent intent = new Intent(context, PlayerViewActivity.class);

        try {
            JSONObject object = data.getJSONObject(position);
            holder.title.setText(object.getString("title"));
            holder.quality.setText(object.getString("quality"));
            holder.lang.setText(object.getString("language"));

            String logo = object.getString("logo");
            if (!(logo.isEmpty())) {
                Glide.with(context).load(logo).into(holder.image);
            }

            String url = object.getString("url");
            intent.putExtra("url", url);

            boolean hasDrm = object.getBoolean("hasDrm");
            if (hasDrm) {
                intent.setAction("DRM");
                intent.putExtra("k", object.getString("key"));
                intent.putExtra("kid", object.getString("keyId"));
            } else intent.setAction("HLS");

        } catch (JSONException ignore) {}

        holder.itemView.setOnClickListener(view -> {

            holder.itemView.getContext().startActivity(intent);

        });

    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        TextView title, quality, lang;
        ImageView image;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.tvTitle);
            quality = itemView.findViewById(R.id.tvQuality);
            lang = itemView.findViewById(R.id.tvLang);
            image = itemView.findViewById(R.id.ivSingleItem);

        }

    }

}