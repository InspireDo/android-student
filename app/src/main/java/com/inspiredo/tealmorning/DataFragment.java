package com.inspiredo.tealmorning;


import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONArray;


/**
 * A simple {@link Fragment} subclass.
 */
public class DataFragment extends Fragment {

    private JSONArray mNextSession;
    private MyMeditation mCurrentSession;
    private SessionAdapter mAdapter;
    private SessionPlaybackService.SessionBinder mBinder;

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

    public void setBinder(SessionPlaybackService.SessionBinder binder) {
        mBinder = binder;
    }

    public SessionPlaybackService.SessionBinder getBinder() {
        return mBinder;
    }
}
