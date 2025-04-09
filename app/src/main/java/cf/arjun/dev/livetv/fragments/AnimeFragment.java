package cf.arjun.dev.livetv.fragments;

import android.app.ProgressDialog;
import android.os.Bundle;
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

import cf.arjun.dev.livetv.MainActivity;
import cf.arjun.dev.livetv.R;
import cf.arjun.dev.livetv.adapters.MyAdapter;
import cf.arjun.dev.livetv.repository.Queue;

public class AnimeFragment extends Fragment {

    private RecyclerView rvMostPopular, rvTopAiring, rvNewEpisodeReleases, rvMostFavorites, rvAnime;
    private ScrollView animeContainer;
    private JSONArray searchedAnimeList = null;
    private MyAdapter adapterMP, adapterTA, adapterNER, adapterMF, adapterAnime;
    private Queue queue;
    private int currentPage = 1;
    private ProgressDialog searchQueryDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_anime, container, false);

        if (MainActivity.jsonData != null) {

            rvMostPopular = view.findViewById(R.id.rvMostPopular);
            rvTopAiring = view.findViewById(R.id.rvTopAiring);
            rvNewEpisodeReleases = view.findViewById(R.id.rvNewEpisodeReleases);
            rvMostFavorites = view.findViewById(R.id.rvMostFavorites);
            rvAnime = view.findViewById(R.id.recyclerViewAnime);
            animeContainer = view.findViewById(R.id.animeContainer);
            searchQueryDialog = new ProgressDialog(getContext());
            queue = Queue.getInstance(getContext());
            setupRecyclerView();

        }

        return view;
    }

    private void setupRecyclerView () {

        adapterMP = new MyAdapter(getContext());
        adapterTA = new MyAdapter(getContext());
        adapterNER = new MyAdapter(getContext());
        adapterMF = new MyAdapter(getContext());
        adapterAnime = new MyAdapter(getContext());
        adapterMP.isSearched = true;
        adapterMP.isAnime = true;
        adapterTA.isSearched = true;
        adapterTA.isAnime = true;
        adapterNER.isSearched = true;
        adapterNER.isAnime = true;
        adapterMF.isSearched = true;
        adapterMF.isAnime = true;
        adapterAnime.isSearched = true;
        adapterAnime.isAnime = false;

        rvMostPopular.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvMostPopular.setAdapter(adapterMP);
        adapterMP.setData(MainActivity.MOST_POPULAR);

        rvTopAiring.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvTopAiring.setAdapter(adapterTA);
        adapterTA.setData(MainActivity.TOP_AIRING);

        rvNewEpisodeReleases.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvNewEpisodeReleases.setAdapter(adapterNER);
        adapterNER.setData(MainActivity.NEW_EPISODE_RELEASES);

        rvMostFavorites.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvMostFavorites.setAdapter(adapterMF);
        adapterMF.setData(MainActivity.MOST_FAVORITES);

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
        try {
            if (query.isEmpty()) {
                searchQueryDialog.dismiss();
                initRecyclerViews(false);
            } else {
                searchQueryDialog.setTitle("Searching for " + query + " !!");
                searchQueryDialog.setMessage("Please wait....");
                searchQueryDialog.show();
                getAnimeList(MainActivity.ANIME_BASE_URL, query, currentPage);
            }
        } catch (Exception ignore) {}
    }

    private void getAnimeList (String base_url, String query, int page) {
        if (queue != null) {
            try {
                queue.makeRequest(
                        Request.Method.GET,
                        String.format(MainActivity.ANIME_BASE_URL + "/api/v2/hianime/search?q=%s&page=%d", query.trim().replace(" ", "+"), page),
                        response -> {
                            searchQueryDialog.dismiss();
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
                            searchQueryDialog.dismiss();
                            Toast.makeText(getContext(), "Something went wrong, Come back later!", Toast.LENGTH_LONG).show();
                        });
            } catch (Exception ignore) {}
        }
    }

    private void fetchAnimeData () {
        if (queue != null) {
            try {
                queue.makeRequest(
                        Request.Method.GET,
                        MainActivity.ANIME_BASE_URL + "/api/v2/hianime/home",
                        response -> {
                            try {
                                JSONObject result = new JSONObject(response);
                                if (result.getBoolean("success")) {
                                    MainActivity.jsonAnimeData = result;
                                    result = result.getJSONObject("data");
                                    MainActivity.MOST_POPULAR = result.getJSONArray("mostPopularAnimes");
                                    MainActivity.TOP_AIRING = result.getJSONArray("topAiringAnimes");
                                    MainActivity.TOP_AIRING = result.getJSONArray("topAiringAnimes");
                                    MainActivity.NEW_EPISODE_RELEASES = result.getJSONArray("latestEpisodeAnimes");
                                    MainActivity.MOST_FAVORITES = result.getJSONArray("mostFavoriteAnimes");
                                    adapterMP.setData(MainActivity.MOST_POPULAR);
                                    adapterTA.setData(MainActivity.TOP_AIRING);
                                    adapterNER.setData(MainActivity.NEW_EPISODE_RELEASES);
                                    adapterMF.setData(MainActivity.MOST_FAVORITES);
                                } else Log.d("Arjun", "Something went wrong!");
                            } catch (JSONException error) {
                                Toast.makeText(getActivity(), "Something went wrong, " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        },
                        error -> {
                            searchQueryDialog.dismiss();
                            Toast.makeText(requireContext(), "Something went wrong, " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } catch (Exception ignore) {}
        }
    }

}