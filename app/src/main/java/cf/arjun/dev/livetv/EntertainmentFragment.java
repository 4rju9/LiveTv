package cf.arjun.dev.livetv;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class EntertainmentFragment extends Fragment {

    private RecyclerView recyclerView;
    private JSONArray arrayList = null;
    private JSONArray searchedAnimeList = null;
    private EditText searchBar;
    private MyAdapter adapter;
    private Queue queue;
    private int currentPage = 1;
    private Handler handler = new Handler();
    private Runnable runnable;
    private ProgressDialog dialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_entertainment, container, false);

        if (MainActivity.jsonData != null) {

            try {
                arrayList = MainActivity.jsonData.getJSONArray(MainActivity.ENTERTAINMENT);
                recyclerView = view.findViewById(R.id.recyclerViewEntertainment);
                adapter = new MyAdapter(getContext());
                dialog = new ProgressDialog(getContext());
                searchBar = view.findViewById(R.id.searchViewContainer);
                queue = Queue.getInstance(getContext());
                setupRecyclerView();
                setupSearchBar();
            } catch (JSONException ignore) {
            }

        }

        return view;
    }

    private void setupRecyclerView () {

        if (arrayList != null && arrayList.length() >= 1) {
            adapter.setData(arrayList);
        }
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(adapter);

    }

    private void setupSearchBar () {
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String query = searchBar.getText().toString();
                if (runnable != null) {
                    handler.removeCallbacks(runnable);
                }
                runnable = () -> {
                    searchQuery(query);
                };
                handler.postDelayed(runnable, 500);

            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });
    }

    private void searchQuery (String query) {
        if (query.isEmpty()) {
            if (arrayList != null && arrayList.length() >= 1) {
                dialog.dismiss();
                adapter.setData(arrayList);
                adapter.isSearched = false;
            }
        } else {
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
                                    adapter.isSearched = true;
                                    adapter.setData(searchedAnimeList);
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