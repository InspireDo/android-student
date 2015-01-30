package com.inspiredo.inspiredo;

import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.inspiredo.tealmorning.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;


/**
 * This class is the main view that a logged in user will see.
 * It allows for the playback of meditation sessions, and it allows the user to view their history,
 * streak, and replay previous sessions.
 *
 * Created by Erik Kessler
 * (c) 2015 inspireDo.
 */
public class SessionsActivity extends ActionBarActivity
        implements AdapterView.OnItemClickListener {

    /**
     * Constants for starting the detail activity
     */
    public static final String INDEX = "INDEX";
    public static final String PREV = "PREV";
    public static final String TITLE = "TITLE";
    public static final int REQUEST_CODE = 1;
    public static final String STREAK = "STREAK";
    public static final String LOADING = "LOADING";
    public static final String DESC = "DESC";
    public static final String DATE = "DATE";

    /**
     * UI elements
     *
     * mStreakTV        - displays streak
     *
     * mHistoryList     - displays available sessions
     *
     * mLoadingPB       - indicates that the application is working (often server requests)
     */
    private TextView    mStreakTV;
    private ListView    mHistoryList;
    private ProgressBar mLoadingPB;

    /**
     * The user's email. Needed for server calls
     */
    private String      mUserEmail;

    /**
     * Title and description for the next session
     */
    private String      mNextTitle;
    private String      mNextDesc;

    /**
     * Adapter for the history list
     */
    private SessionAdapter
                        mAdapter;

    /**
     * Initialize all the UI element variables. Set the click listeners. Set visibility.
     * Get the user's email. Start the request to the server.
     *
     * @param savedInstanceState Bundle of stored values
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Load from id
        mStreakTV       = (TextView)    findViewById(R.id.tvStreak);
        mHistoryList    = (ListView)    findViewById(R.id.lvHistory);
        mLoadingPB      = (ProgressBar) findViewById(R.id.pbLoading);

        // Set click listeners
        mHistoryList.setOnItemClickListener(this);

        // Get the user's email from shared preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mUserEmail = prefs.getString(LoginActivity.PREF_KEY, "");


        // Check if have a saved state
        if (savedInstanceState == null) {
            // Build it fresh
            getHistory(); // Make a call to the server

        } else {
            mStreakTV.setText(savedInstanceState.getString(STREAK));
            mLoadingPB.setVisibility(
                    savedInstanceState.getBoolean(LOADING) ? View.VISIBLE : View.INVISIBLE
            );

            mNextTitle = savedInstanceState.getString(TITLE);
            mNextDesc = savedInstanceState.getString(DESC);

            // find the retained fragment on activity restarts
            FragmentManager fm = getFragmentManager();
            DataFragment dataFragment = (DataFragment) fm.findFragmentByTag("data");

            if (dataFragment != null) {
                // Get the session, next sections, and adapter
                mAdapter = dataFragment.getAdapter();

                // Setup the adapter
                mHistoryList.setAdapter(mAdapter);
                mHistoryList.setSelection(mAdapter.getCount() - 1);

            }

        }
    }

    /**
     * Gets the previous 10 sessions, the current streak, and the sections for the next session.
     */
    private void getHistory() {

        // Show that we are loading and hide the start button until it is loaded
        mLoadingPB.setVisibility(View.VISIBLE);
        mHistoryList.setVisibility(View.INVISIBLE);

        RequestQueue queue = Volley.newRequestQueue(this);
        String url = getString(R.string.api_url) + "?prev=1&email=" + mUserEmail;

        // Setup the json request
        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        try {

                            // Get the streak and the sections for the next session
                            mStreakTV.setText(response.getString("streak"));
                            mNextTitle = response.getString("title");
                            mNextDesc = response.getString("description");

                            // Setup the history list from the prev array
                            parsePrevious(response.getJSONArray("prev"));

                        } catch (JSONException | ParseException e) {
                            requestError("Error");
                        }

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        requestError("Error");

                    }
                });

        queue.add(jsObjRequest);
    }

    /**
     * Stops the loading progress spinner and sets the streak to an error message.
     * @param errorMessage The error message to display
     */
    private void requestError(String errorMessage) {
        mStreakTV.setText(errorMessage);
        mLoadingPB.setVisibility(View.INVISIBLE);
    }

    /**
     * Takes a JSONArray of objects with date_complete and index fields that represent previous
     * meditation sessions and puts them into a list.
     *
     * @param prev The array to parse.
     * @throws JSONException
     * @throws ParseException
     */
    private void parsePrevious(JSONArray prev) throws JSONException, ParseException{

        // Create a new adapter
        mAdapter = new SessionAdapter(SessionsActivity.this, R.layout.session_row);

        // Loop and add each task to the adapter
        String title, desc;
        Date date;
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        int index;
        for (int i = 0; i < prev.length(); i++) {
            JSONObject session = prev.getJSONObject(i);

            // Get the Task properties
            title = session.getString("title");
            index = session.getInt("index");
            desc = session.getString("description");
            date = df.parse(session.getString("date_complete"));


            // Add new TaskModel to the adapter
            mAdapter.add(new MeditationSessionModel(title, index,desc, date));

        }

        // Add a row to the end that has an index of -1. This will allow the user to play the
        // next session
        mAdapter.add(new MeditationSessionModel(null, -1, null, null));

        // Set the adapter and scroll to the bottom of the list
        mHistoryList.setAdapter(mAdapter);
        mHistoryList.setSelection(mAdapter.getCount() - 1);

        mHistoryList.setVisibility(View.VISIBLE);
        mLoadingPB.setVisibility(View.INVISIBLE);
    }

    /**
     * Logout the user by resetting the pref and returning to the login screen
     */
    private void logout() {
        // Reset the preference
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this)
                .edit();
        editor.putString(LoginActivity.PREF_KEY, "");
        editor.apply();

        // Return to the login screen
        Intent i = new Intent(this, LoginActivity.class);
        startActivity(i);
        finish();

    }

    /**
     * Handle clicks of items of the history list.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        MeditationSessionModel session = mAdapter.getItem(position);
        int index = session.getIndex();
        boolean isPrev = index != -1;
        String title = isPrev ? session.getTitle() : mNextTitle;
        String desc = isPrev ? session.getDesc() : mNextDesc;

        DateFormat df = new SimpleDateFormat("MM/dd/yy");
        String date = isPrev ? df.format(session.getDate()) : "";

        Intent i = new Intent(this, SessionDetailActivity.class);
        i.putExtra(INDEX, index);
        i.putExtra(PREV, isPrev);
        i.putExtra(TITLE, title);
        i.putExtra(DESC, desc);
        i.putExtra(DATE, date);


        startActivityForResult(i, REQUEST_CODE);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            if (!data.getBooleanExtra(PREV, false)) {
                int index = mAdapter.getItem(mAdapter.getCount() - 2).getIndex() + 1;


                MeditationSessionModel session = new MeditationSessionModel(mNextTitle, index, mNextDesc, new Date());
                mAdapter.insert(session, mAdapter.getCount() - 1);
                mAdapter.notifyDataSetChanged();
                mHistoryList.setSelection(mAdapter.getCount() - 1);

                mNextTitle = data.getStringExtra(TITLE);
                mNextDesc = data.getStringExtra(DESC);
                mStreakTV.setText(data.getStringExtra(STREAK));


            }


        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                getHistory();
                return true;
            case R.id.action_logout:
                logout();
                return true;
            default:
                Log.d("Menu Item Click", "Action not implemented");
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        /* Use a Fragment to store objects */
        FragmentManager fm = getFragmentManager();
        DataFragment dataFragment = (DataFragment) fm.findFragmentByTag("data");

        // create the fragment if needed
        if (dataFragment == null) {
            dataFragment = new DataFragment();
            fm.beginTransaction().add(dataFragment, "data").commit();
        }

        // Save the adapter
        dataFragment.setAdapter(mAdapter);

        outState.putString(STREAK, mStreakTV.getText().toString());
        outState.putBoolean(LOADING, mLoadingPB.getVisibility() == View.VISIBLE);
        outState.putString(TITLE, mNextTitle);
        outState.putString(DESC, mNextDesc);



        super.onSaveInstanceState(outState);

    }
}
