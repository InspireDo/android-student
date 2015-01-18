package com.inspiredo.tealmorning;


import android.app.Fragment;
import android.os.Bundle;

import org.json.JSONArray;


/**
 * A simple {@link Fragment} subclass.
 */
public class DataFragment extends Fragment {

    private JSONArray mNextSession;
    private MyMeditation mCurrentSession;
    private SessionAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public void setNextSession(JSONArray nextSession) {
        mNextSession = nextSession;
    }

    public JSONArray getNextSession() {
        return mNextSession;
    }

    public void setCurrentSession(MyMeditation session) {
        mCurrentSession = session;
    }

    public MyMeditation getCurrentSession() {
        return mCurrentSession;
    }

    public void setAdapter(SessionAdapter adapter) {
        mAdapter = adapter;
    }

    public SessionAdapter getAdapter() {
        return mAdapter;
    }
}
