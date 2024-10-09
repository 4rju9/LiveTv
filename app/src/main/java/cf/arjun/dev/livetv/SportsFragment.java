package cf.arjun.dev.livetv;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONArray;
import org.json.JSONException;

public class SportsFragment extends Fragment {

    private RecyclerView recyclerView;
    private JSONArray arrayList = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_sports, container, false);

        if (MainActivity.jsonData != null) {

            try {
                arrayList = MainActivity.jsonData.getJSONArray(MainActivity.SPORTS);
                recyclerView = view.findViewById(R.id.recyclerViewSports);
                setupRecyclerView();
            } catch (JSONException ignore) {
            }

        }

        return view;
    }

    private void setupRecyclerView () {

        MyAdapter adapter = new MyAdapter(getContext());
        if (arrayList != null && arrayList.length() >= 1) {
            adapter.setData(arrayList);
        }
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(adapter);

    }

}