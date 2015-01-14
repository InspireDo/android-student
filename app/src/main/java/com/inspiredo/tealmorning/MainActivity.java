package com.inspiredo.tealmorning;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.TimeZone;

/**
 * This class is the main view that a logged in user will see.
 * It allows for the playback of meditation sessions, and it allows the user to view their history,
 * streak, and replay previous sessions.
 *
 * Created by Erik Kessler
 * (c) 2015 inspireDo.
 */
public class MainActivity extends ActionBarActivity
        implements View.OnClickListener, View.OnLongClickListener, AdapterView.OnItemClickListener {

    /**
     * UI elements
     *
     * mStreakTV        - displays streak
     * mTimerTV         - displays time left in session
     * mSessionDateTV   - displays the date of the currently selected session
     *
     * mLoadingPB       - indicates that the application is working (often server requests)
     * mDurationPB      - indicates how far through the session we are
     *
     * mStartBTN        - allows the user to start and stop the session
     * mPlayBTN         - allows the user to play and pause the session
     *
     * mControlLayout   - holds the play button, duration progress, and time left
     */
    private TextView    mStreakTV, mTimerTV, mSessionDateTV;
    private ProgressBar mLoadingPB, mDurationPB;
    private Button      mStartBTN, mPlayBTN;
    private ListView    mHistoryList;
    private RelativeLayout
                        mControlLayout;

    /**
     * Implementation for how to handle various stages of progress.
     */
    public final MyMeditation.MeditationProgressListener
            PROGRESS_LISTENER = new MyMeditation.MeditationProgressListener() {

        /**
         * When we learn about the duration of the session we set up our variables to
         * allow for the ProgressBar to reflect the playback.
         *
         * @param duration Length of the session in tenths of a second
         */
        @Override
        public void durationSet(int duration) {
            mDurationPB.setProgress(0);     // Reset the progress
            mDurationPB.setMax(duration);   // Set the max
            mDuration = duration;           // Store the duration

            // Set the timer to display the time left
            mTimerTV.setText(String.format("%d:%02d", duration/600, (duration/10)%60));

        }

        @Override
        public void tick() {

            mDurationPB.incrementProgressBy(1);
            mTimerTV.setText(String.format("%d:%02d", --mDuration/600, (mDuration/10)%60));
        }
    };

    /**
     * Holds the current session the might be playing or will be played
     */
    private MyMeditation mMeditationSession;

    /**
     * The sections for the next session
     */
    private JSONArray mNextSections;

    /**
     * The user's email. Needed for server calls
     */
    private String      mUserEmail;

    /**
     * Adapter for the history list
     */
    private ArrayAdapter<MeditationSessionModel>
                        mAdapter;

    /**
     * Binder to the session playback service
     */
    private SessionPlaybackService.SessionBinder
                        mSessionBinder;

    /**
     * Constants that describe the state of the mediation session.
     *
     * NOT_PREP - Session must be loaded by calling prepareSession()
     * PREP     - Session is ready to play
     * PLAYING  - Session is currently started
     */
    private final static int STATUS_NOT_PREP = 0;
    private final static int STATUS_PREP = 1;
    private final static int STATUS_PLAYING = 2;

    /**
     * Information about the current state of playback
     *
     * PlayState    - State of the session
     * Duration     - Length of the session in tenths of seconds
     * IsPrev       - Are we replaying a previous session
     */
    private int         mPlayState = STATUS_NOT_PREP;
    private int         mDuration;
    private boolean     mIsPrev;

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
        mStartBTN       = (Button)      findViewById(R.id.bStop);
        mPlayBTN        = (Button)      findViewById(R.id.bTogglePlay);
        mStreakTV       = (TextView)    findViewById(R.id.tvStreak);
        mTimerTV        = (TextView)    findViewById(R.id.tvTimeLeft);
        mSessionDateTV  = (TextView)    findViewById(R.id.tvSessionDate);
        mLoadingPB      = (ProgressBar) findViewById(R.id.pbGetStreak);
        mDurationPB     = (ProgressBar) findViewById(R.id.pbDuration);
        mHistoryList    = (ListView)    findViewById(R.id.lvHistory);
        mControlLayout  = (RelativeLayout)
                                        findViewById(R.id.rlControls);

        // Set click listeners
        mStartBTN.setOnClickListener(this);
        mPlayBTN.setOnClickListener(this);
        mHistoryList.setOnItemClickListener(this);

        // Set visibility
        mLoadingPB.setVisibility(View.INVISIBLE);
        mStartBTN.setVisibility(View.INVISIBLE);
        mControlLayout.setVisibility(View.INVISIBLE);

        // Get the user's email from shared preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mUserEmail = prefs.getString(LoginActivity.PREF_KEY, "");

        getJSON(); // Make a call to the server
    }

    /**
     * Gets the previous 10 sessions, the current streak, and the sections for the next session.
     */
    private void getJSON() {

        // Show that we are loading and hide the start button until it is loaded
        mLoadingPB.setVisibility(View.VISIBLE);
        mStartBTN.setVisibility(View.INVISIBLE);

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
                            mNextSections = response.getJSONArray("sections");

                            //Prepare the next sections for playback
                            prepSections(mNextSections);

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
        mSessionDateTV.setText(errorMessage);
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
        mAdapter = new SessionAdapter(MainActivity.this, R.layout.session_row);


        // Date format for parsing
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));

        // Loop and add each task to the adapter
        for (int i = 0; i < prev.length(); i++) {
            JSONObject session = prev.getJSONObject(i);

            // Get the Task properties
            Date date = df.parse(session.getString("date_complete"));
            int index = session.getInt("index");

            // Add new TaskModel to the adapter
            mAdapter.add(new MeditationSessionModel(date, index));

        }

        // Add a row to the end that has an index of -1. This will allow the user to play the
        // next session
        mAdapter.add(new MeditationSessionModel(null, -1));

        // Set the adapter and scroll to the bottom of the list
        mHistoryList.setAdapter(mAdapter);
        mHistoryList.setSelection(mAdapter.getCount() - 1);
    }

    /**
     * Creates a MyMeditation session from the JSONArray and prepares the session for playback by
     * loading the audio files from the url.
     *
     * @param jsArray The array of sections
     */
    private void prepSections(JSONArray jsArray) {

        // Check if there is a session found
        if(jsArray.length() == 0 ) {
            requestError("No Session Found");
            return;
        }

        /**
         * Check if we are dealing with an "Up Next" session as we can get an error where it says
         * there is no next session but since array.length isn't 0 there is a session.
         */
        if (!mIsPrev) {
            mSessionDateTV.setText("Next Session");

        }

        // Create a new session
        mMeditationSession = new MyMeditation();

        // Loop through the array getting the URL and rest for each section
        for(int i = 0; i < jsArray.length(); i += 2) {
            try {
                Log.d("Meditation Prep", i + " URL: " + jsArray.getString(i));
                Log.d("Meditation Prep", i + "Rest: " + jsArray.getInt(i+1));
                mMeditationSession.addSection(
                        new MyMeditation.MySection(jsArray.getString(i), jsArray.getInt(i+1))
                );
            } catch (JSONException e) {
                requestError("Parse Error");
                return;
            } catch (IOException e) {
                requestError("URL Error");
                return;
            }


        }

        // Set the progress listener so we can know what the duration is
        mMeditationSession.setProgressListener(PROGRESS_LISTENER);

        // Prepare the session for playback
        mMeditationSession.prepare(new MyMeditation.OnMeditationReadyListener() {
            @Override
            public void onMeditationReady() {
                // Set the UI correctly to start playing
                setUIForState(STATUS_PREP);
            }
        });

    }

    /**
     * Plays the session. Session must be prepared.
     */
    private void playSections() {

        // Check that the session is prepared
        if (mPlayState != STATUS_PREP) {
            Log.e("Play State", "Not in a valid state to play sections");
            return;
        }

        // Set the UI
        setUIForState(STATUS_PLAYING);

        // Use the playback service to play the session
        if (mSessionBinder != null) {
            mSessionBinder.getService().playSession(mMeditationSession, mIsPrev, mUserEmail);
        } else {
            // Start the service then bind to it
            Intent i = new Intent(MainActivity.this, SessionPlaybackService.class);
            startService(i);
            MainActivity.this.bindService(i, PLAY_SERVICE_CONNECTION, BIND_AUTO_CREATE);
        }

    }

    /**
     * Service connection for to play the session
     */
    private final ServiceConnection PLAY_SERVICE_CONNECTION = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mSessionBinder = (SessionPlaybackService.SessionBinder) service;
            mSessionBinder.getService().setActivity(MainActivity.this);
            mSessionBinder.getService().playSession(mMeditationSession, mIsPrev, mUserEmail);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mSessionBinder = null;
        }
    };

    /**
     * Called by the playback service when playback is done.
     * @param prev Was the session a new session or a replay
     */
    public void meditationDone(boolean prev) {

        // Set the UI
        setUIForState(STATUS_NOT_PREP);

        // If it was a replay we just need to set the text correctly
        if(prev) {
            mSessionDateTV.setText("Select Session");
            return;
        }

        // If it was a new one we are going to need to fetch a new session
        mLoadingPB.setVisibility(View.VISIBLE);

        /* We need to add the completed session to the list */
        // Calculate the index based on the last session
        int index = mAdapter.getItem(mAdapter.getCount() - 2).getIndex() + 1;

        // Insert the new session and update the list
        mAdapter.insert(new MeditationSessionModel(new Date(), index), mAdapter.getCount() -1);
        mAdapter.notifyDataSetChanged();
        mHistoryList.setSelection(mAdapter.getCount() - 1);

    }

    /**
     * Set the UI based on the state that the player is going into.
     *
     * @param state State we are going into
     */
    private void setUIForState(int state) {
        switch (state) {
            case STATUS_NOT_PREP:
                // Hide the start button and control panel
                mStartBTN.setVisibility(View.INVISIBLE);
                mControlLayout.setVisibility(View.INVISIBLE);
                break;
            case STATUS_PREP:
                // Hide the loading spinner and show the start button
                mLoadingPB.setVisibility(View.INVISIBLE);
                mStartBTN.setVisibility(View.VISIBLE);
                mStartBTN.setText(getString(R.string.button_start));
                break;
            case STATUS_PLAYING:
                // Show the control panel and make buttons be stop/pause
                mStartBTN.setText(getString(R.string.button_stop));
                mControlLayout.setVisibility(View.VISIBLE);
                mPlayBTN.setText(getString(R.string.button_pause));
                break;
            default:
                Log.e("Playback Status", "Invalid state");
                return;
        }

        mPlayState = state;
    }

    /**
     * Called by the service when the steak needs to be updated
     * @param streak The new streak
     */
    public void setStreakText(String streak) {
        mStreakTV.setText(streak);
    }

    /**
     * Called by the service when the next sections arrive
     * @param next The next session's sections
     */
    public void setPrepCurrSections(JSONArray next) {
        mNextSections = next;
        prepSections(mNextSections);
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
     * Handle clicks of UI elements
     * @param v View that was clicked
     */
    @Override
    public void onClick(View v) {
        // Handle based on the ID of the view
        switch (v.getId()) {

            // mStartBTN
            // Might need to prep, start, or stop
            case R.id.bStop:
                if(mPlayState == STATUS_NOT_PREP) {
                    // Prepare the next session
                    mLoadingPB.setVisibility(View.VISIBLE);
                    v.setVisibility(View.INVISIBLE);
                    prepSections(mNextSections);

                } else if (mPlayState == STATUS_PREP) {
                    // Play the loaded session
                    playSections();
                    mControlLayout.setVisibility(View.VISIBLE);

                } else if (mPlayState == STATUS_PLAYING) {
                    // Stop the playing session
                    mPlayState = STATUS_NOT_PREP;
                    mControlLayout.setVisibility(View.INVISIBLE);
                    mDurationPB.setProgress(0);
                    mStartBTN.setText("Load Session");

                    // Use the service to stop playback
                    mSessionBinder.getService().stopSession(mMeditationSession);


                }
                break;

            // mPlayBTN
            // Toggle playback of the session
            case R.id.bTogglePlay:
                // Toggle the session's playback and set the button appropriately
                mPlayBTN.setText(
                        mMeditationSession.togglePlay() ?
                                getString(R.string.button_pause) : getString(R.string.button_play ));
                break;

            default:
                Log.d("Button Click", "No action implemented");
        }
    }

    /**
     * Handle clicks of items of the history list.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        // If playing a session, return
        if (mPlayState == STATUS_PLAYING) return;

        // Get the session
        MeditationSessionModel session = mAdapter.getItem(position);

        // Check if it is the "next session" or a previous session
        if (session.getIndex() == -1) {
            /* Prepare the next session */
            mIsPrev = false;
            mSessionDateTV.setText("Next Session");
            prepSections(mNextSections);

        } else {
            /* Need to get the previous session using the index */
            mIsPrev = true;

            // Set the label to be the date of the session
            SimpleDateFormat format = new SimpleDateFormat("MM/dd");
            mSessionDateTV.setText(format.format(session.getDate()));

            mLoadingPB.setVisibility(View.VISIBLE);
            mStartBTN.setVisibility(View.INVISIBLE);

            // Need to get the session's sections
            RequestQueue queue = Volley.newRequestQueue(this);
            String url = getString(R.string.api_url) + "?index=" + session.getIndex() + "&email=" + mUserEmail;


            JsonObjectRequest jsObjRequest = new JsonObjectRequest
                    (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                prepSections(response.getJSONArray("sections"));

                            } catch (JSONException e) {
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
    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.bStop:
                String[] memes = getResources().getStringArray(R.array.dank_memes);
                Random r = new Random();
                Integer rand = r.nextInt(memes.length);
                String meme = memes[rand];

                final MediaPlayer player = new MediaPlayer();
                try {
                    player.setDataSource("http://soundboard.panictank.net/" + meme);
                    player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            player.start();
                        }
                    });
                    player.prepareAsync();

                } catch (IOException e) {
                    e.printStackTrace();
                }



                break;

            default:
                Log.d("Button LongClick", "No action implemented");
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                getJSON();
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
}
