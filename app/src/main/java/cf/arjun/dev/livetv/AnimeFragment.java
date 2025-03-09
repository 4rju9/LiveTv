package cf.arjun.dev.livetv;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AnimeFragment extends Fragment {

    private RecyclerView rvMostPopular, rvTopAiring, rvAnime;
    private ScrollView animeContainer;
    private JSONArray searchedAnimeList = null;
    private MyAdapter adapterMP, adapterTA, adapterAnime;
    private Queue queue;
    private int currentPage = 1;
    private ProgressDialog dialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_anime, container, false);

        if (MainActivity.jsonData != null) {

            rvMostPopular = view.findViewById(R.id.rvMostPopular);
            rvTopAiring = view.findViewById(R.id.rvTopAiring);
            rvAnime = view.findViewById(R.id.recyclerViewAnime);
            animeContainer = view.findViewById(R.id.animeContainer);
            dialog = new ProgressDialog(getContext());
            queue = Queue.getInstance(getContext());
            setupRecyclerView();

        }

        return view;
    }

    private void setupRecyclerView () {

        adapterMP = new MyAdapter(getContext());
        adapterTA = new MyAdapter(getContext());
        adapterAnime = new MyAdapter(getContext());
        adapterMP.isSearched = true;
        adapterMP.isAnime = true;
        adapterTA.isSearched = true;
        adapterTA.isAnime = true;
        adapterAnime.isSearched = true;
        adapterAnime.isAnime = false;

        rvMostPopular.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvMostPopular.setAdapter(adapterMP);
        adapterMP.setData(MainActivity.MOST_POPULAR);

        rvTopAiring.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvTopAiring.setAdapter(adapterTA);
        adapterTA.setData(MainActivity.TOP_AIRING);

        rvAnime.setLayoutManager(new LinearLayoutManager(getContext()));
        rvAnime.setAdapter(adapterAnime);

    }

    private void initRecyclerViews (boolean isSearched) {

        if (isSearched) {
            animeContainer.setVisibility(View.GONE);
            rvAnime.setVisibility(View.VISIBLE);
        } else {
            animeContainer.setVisibility(View.VISIBLE);
            rvAnime.setVisibility(View.GONE);
        }

    }

    public void searchQuery (String query) {
        if (query.isEmpty()) {
            dialog.dismiss();
            initRecyclerViews(false);
        } else {
            dialog.setTitle("Searching for " + query + " !!");
            dialog.setMessage("Please wait....");
            dialog.show();
            String base_url = getString(R.string.searchAnimeUrl);
            getAnimeList(base_url, query, currentPage);
        }
    }

    private void getAnimeList (String base_url, String query, int page) {
        if (queue != null) {
            try {
                queue.makeRequest(
                        Request.Method.GET,
                        String.format(base_url + "/api/v2/hianime/search?q=%s&page=%d", query.trim().replace(" ", "+"), page),
                        response -> {
                            dialog.dismiss();
                            try {
                                JSONObject result = new JSONObject(response);
                                if (result.getBoolean("success")) {
                                    result = result.getJSONObject("data");
                                    searchedAnimeList = result.getJSONArray("animes");
                                    initRecyclerViews(true);
                                    adapterAnime.setData(searchedAnimeList);
                                } else Log.d("Arjun", "Something went wrong!");
                            } catch (JSONException error) {
                                Toast.makeText(getContext(), "Something went wrong, " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        },
                        error -> {
                            dialog.dismiss();
                            Toast.makeText(getContext(), "Something went wrong, Come back later!", Toast.LENGTH_LONG).show();
                        });
            } catch (Exception ignore) {}
        }
    }

}