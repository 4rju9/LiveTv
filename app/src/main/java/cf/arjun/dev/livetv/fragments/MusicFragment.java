package cf.arjun.dev.livetv.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONArray;
import org.json.JSONException;

import cf.arjun.dev.livetv.MainActivity;
import cf.arjun.dev.livetv.adapters.MyAdapter;
import cf.arjun.dev.livetv.R;

public class MusicFragment extends Fragment {

    private RecyclerView recyclerView;
    private JSONArray arrayList = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_music, container, false);

        if (MainActivity.jsonData != null) {

            try {
                arrayList = MainActivity.jsonData.getJSONArray(MainActivity.MUSIC);
                recyclerView = view.findViewById(R.id.recyclerViewMusic);
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