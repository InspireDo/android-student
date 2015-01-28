package com.inspiredo.tealmorning;

import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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
import java.util.Random;

/**
 * This class is the main view that a logged in user will see.
 * It allows for the playback of meditation sessions, and it allows the user to view their history,
 * streak, and replay previous sessions.
 *
 * Created by Erik Kessler
 * (c) 2015 inspireDo.
 */
public class SessionDetailActivity extends ActionBarActivity
        implements View.OnClickListener, View.OnLongClickListener {

    /**
     * UI elements
     *
     * mStreakTV        - displays streak
     * mTimerTV         - displays time left in session
     * mSessionTitleTV   - displays the date of the currently selected session
     *
     * mLoadingPB       - indicates that the application is working (often server requests)
     * mDurationPB      - indicates how far through the session we are
     *
     * mStartBTN        - allows the user to start and stop the session
     * mPlayBTN         - allows the user to play and pause the session
     *
     * mControlLayout   - holds the play button, duration progress, and time left
     */
    private TextView    mTimerTV, mSessionTitleTV, mDescTV, mDurationTV, mDateTV;
    private ProgressBar mLoadingPB, mDurationPB;
    private Button      mPlayBTN;
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

            mDurationTV.setText(getDurationString(duration));

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
     * Implementation for how to handle meditation ready.
     */
    public final MyMeditation.OnMeditationReadyListener
            READY_LISTENER = new MyMeditation.OnMeditationReadyListener() {
        @Override
        public void onMeditationReady() {
            // Set the UI correctly to start playing
            setUIForState(STATUS_PREP);
        }
    };

    /**
     * Holds the current session the might be playing or will be played
     */
    private MyMeditation mMeditationSession;

    /**
     * The user's email. Needed for server calls
     */
    private String      mUserEmail;

    /**
     * Binder to the session playback service
     */
    private SessionPlaybackService.SessionBinder
                        mSessionBinder;
    private ServiceConnection
                        mServiceConnection;

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
        setContentView(R.layout.activity_session_detail);

        // Load from id
        mPlayBTN        = (Button)      findViewById(R.id.bTogglePlay);
        mTimerTV        = (TextView)    findViewById(R.id.tvTimeLeft);
        mSessionTitleTV = (TextView)    findViewById(R.id.tvTitle);
        mDescTV         = (TextView)    findViewById(R.id.tvDescription);
        mDurationTV     = (TextView)    findViewById(R.id.tvDuration);
        mDateTV         = (TextView)    findViewById(R.id.tvDate);
        mLoadingPB      = (ProgressBar) findViewById(R.id.pbLoading);
        mDurationPB     = (ProgressBar) findViewById(R.id.pbDuration);
        mControlLayout  = (RelativeLayout)
                                        findViewById(R.id.rlControls);

        // Set click listeners
        mPlayBTN.setOnClickListener(this);

        // Get the user's email from shared preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mUserEmail = prefs.getString(LoginActivity.PREF_KEY, "");


        // Check if have a saved state
        if (savedInstanceState == null) {
            // Build it fresh
            // Set visibility
            mLoadingPB.setVisibility(View.VISIBLE);
            mControlLayout.setVisibility(View.INVISIBLE);

            Intent i = getIntent();

            mSessionTitleTV.setText(i.getStringExtra(SessionsActivity.TITLE));
            mDateTV.setText(i.getStringExtra(SessionsActivity.DATE));
            mIsPrev = i.getBooleanExtra(SessionsActivity.PREV, true);

            String desc = i.getStringExtra(SessionsActivity.DESC);
            int index = desc.indexOf("<vid>");

            if (index != -1) {
                String url = desc.substring(index + "<vid>".length());
                desc = desc.substring(0, index);
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            }
            mDescTV.setText(desc);


            getSession(i.getIntExtra(SessionsActivity.INDEX, -1)); // Make a call to the server
        } else {
            // Recreate the state
            mPlayState = savedInstanceState.getInt("Play State", -1);

            // find the retained fragment on activity restarts
            FragmentManager fm = getFragmentManager();
            DataFragment dataFragment = (DataFragment) fm.findFragmentByTag("data");

            if (dataFragment != null) {
                // Get the session, next sections, and adapter
                mMeditationSession = dataFragment.getCurrentSession();

                if (mMeditationSession != null) {
                    mMeditationSession.setReadyListener(READY_LISTENER);
                    mMeditationSession.setProgressListener(PROGRESS_LISTENER);
                } else {
                    getSession(getIntent().getIntExtra(SessionsActivity.INDEX, -1)); // Make a call to the server
                }

            }

            // Set the duration and duration progress bar
            mDuration = savedInstanceState.getInt("Duration");
            mDurationPB.setMax(savedInstanceState.getInt("Progress Max", 1));
            mDurationPB.setProgress(savedInstanceState.getInt("Progress", 0));
            mTimerTV.setText(String.format("%d:%02d", mDuration/600, (mDuration/10)%60));

            // Restore the next title and Is Prev
            mIsPrev = savedInstanceState.getBoolean("Is Prev");

            // Set the description and title and date
            mSessionTitleTV.setText(savedInstanceState.getString("Session Title"));
            mDescTV.setText(savedInstanceState.getString("Session Desc"));
            mDateTV.setText(savedInstanceState.getString("Session Date"));
            mDurationTV.setText(savedInstanceState.getString("Session Duration"));

            // Restore the play/pause button
            //noinspection ResourceType
            mLoadingPB.setVisibility(savedInstanceState.getInt("Loading Vis"));
            mPlayBTN.setText(savedInstanceState.getString("Play Text"));


            // Check if we are playing
            if (mPlayState == STATUS_PLAYING) {
                mControlLayout.setVisibility(View.VISIBLE); // Show the controls

                // Reconnect to the playback service
                ServiceConnection reconnect = new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName name, IBinder service) {
                        mServiceConnection = this;
                        ((SessionPlaybackService.SessionBinder) service).getService().setActivity(SessionDetailActivity.this);
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName name) {
                        mServiceConnection= null;

                    }
                };
                Intent i = new Intent(SessionDetailActivity.this, SessionPlaybackService.class);
                SessionDetailActivity.this.bindService(i, reconnect, BIND_AUTO_CREATE);
            } else if (mPlayState == STATUS_PREP) {
                mControlLayout.setVisibility(View.VISIBLE); // Show the controls
            } else {
                mControlLayout.setVisibility(View.INVISIBLE); // Hide the controls
            }



        }
    }

    /**
     * Gets the previous 10 sessions, the current streak, and the sections for the next session.
     */
    private void getSession(int index) {

        // Show that we are loading and hide the start button until it is loaded
        mLoadingPB.setVisibility(View.VISIBLE);

        RequestQueue queue = Volley.newRequestQueue(this);
        String url = getString(R.string.api_url) + "?index=" + index + "&email=" + mUserEmail;

        // Setup the json request
        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        try {
                            //Prepare the next sections for playback
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

    /**
     * Stops the loading progress spinner and sets the streak to an error message.
     * @param errorMessage The error message to display
     */
    private void requestError(String errorMessage) {
        mSessionTitleTV.setText(errorMessage);
        mLoadingPB.setVisibility(View.INVISIBLE);
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
                e.printStackTrace();
                return;
            } catch (IOException e) {
                requestError("URL Error");
                return;
            }


        }

        // Set the progress listener so we can know what the duration is
        mMeditationSession.setProgressListener(PROGRESS_LISTENER);

        // Prepare the session for playback
        mMeditationSession.prepare(READY_LISTENER);

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
            Intent i = new Intent(SessionDetailActivity.this, SessionPlaybackService.class);
            startService(i);
            SessionDetailActivity.this.bindService(i, PLAY_SERVICE_CONNECTION, BIND_AUTO_CREATE);
        }

    }

    /**
     * Service connection for to play the session
     */
    private final ServiceConnection PLAY_SERVICE_CONNECTION = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mServiceConnection = this;
            mSessionBinder = (SessionPlaybackService.SessionBinder) service;
            mSessionBinder.getService().setActivity(SessionDetailActivity.this);
            mSessionBinder.getService().playSession(mMeditationSession, mIsPrev, mUserEmail);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceConnection = null;
            mSessionBinder = null;
        }
    };

    /**
     * Called by the playback service when playback is done.
     */
    public void meditationPreDone() {

        // Set the UI
        setUIForState(STATUS_NOT_PREP);

    }

    /**
     * Called by the playback service when playback is done.
     * @param prev Was the session a new session or a replay
     */
    public void meditationDone(boolean prev, String title, String streak) {

        Intent i = new Intent();
        i.putExtra(SessionsActivity.PREV, prev);
        i.putExtra(SessionsActivity.STREAK, streak);
        i.putExtra(SessionsActivity.TITLE, title);
        setResult(RESULT_OK, i);
        finish();

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
                mControlLayout.setVisibility(View.INVISIBLE);
                break;
            case STATUS_PREP:
                // Hide the loading spinner and show the start button
                mLoadingPB.setVisibility(View.INVISIBLE);
                mControlLayout.setVisibility(View.VISIBLE);
                mPlayBTN.setText(getString(R.string.button_play));
                break;
            case STATUS_PLAYING:
                mPlayBTN.setText(getString(R.string.button_pause));
                break;
            default:
                Log.e("Playback Status", "Invalid state");
                return;
        }

        mPlayState = state;
    }

    /**
     * Gets a nice string description of the duration
     * @param duration Number of .1 seconds
     * @return String
     */
    private static String getDurationString(int duration) {

        duration /= 600;
        if (duration < 1) {
            return "less than a minute";
        } else if (duration == 1) {
            return "1 minute";
        } else {
            return duration + " minutes";
        }

    }

    /**
     * Handle clicks of UI elements
     * @param v View that was clicked
     */
    @Override
    public void onClick(View v) {
        // Handle based on the ID of the view
        switch (v.getId()) {

            // mPlayBTN
            // Toggle playback of the session
            case R.id.bTogglePlay:
                if (mPlayState == STATUS_PREP) {
                    playSections();
                } else {
                    // Toggle the session's playback and set the button appropriately
                    mPlayBTN.setText(
                            mMeditationSession.togglePlay() ?
                                    getString(R.string.button_pause) : getString(R.string.button_play));
                }
                break;

            default:
                Log.d("Button Click", "No action implemented");
        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.bTogglePlay:
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
    protected void onSaveInstanceState(Bundle outState) {

        outState.putInt("Play State", mPlayState);
        outState.putInt("Progress Max", mDurationPB.getMax());
        outState.putInt("Progress", mDurationPB.getProgress());
        outState.putInt("Duration", mDuration);
        outState.putBoolean("Is Prev", mIsPrev);
        outState.putInt("Loading Vis", mLoadingPB.getVisibility());
        outState.putString("Play Text", mPlayBTN.getText().toString());
        outState.putString("Session Title", mSessionTitleTV.getText().toString());
        outState.putString("Session Desc", mDescTV.getText().toString());
        outState.putString("Session Date", mDateTV.getText().toString());
        outState.putString("Session Duration", mDurationTV.getText().toString());

        /* Use a Fragment to store objects */
        FragmentManager fm = getFragmentManager();
        DataFragment dataFragment = (DataFragment) fm.findFragmentByTag("data");

        // create the fragment if needed
        if (dataFragment == null) {
            dataFragment = new DataFragment();
            fm.beginTransaction().add(dataFragment, "data").commit();
        }

        // Save the session, sections, and adapter
        dataFragment.setCurrentSession(mMeditationSession);



        super.onSaveInstanceState(outState);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (mPlayState == STATUS_PLAYING) {
                    mSessionBinder.getService().stopSession(mMeditationSession);
                }
                finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mPlayState == STATUS_PLAYING) {
            mSessionBinder.getService().stopSession(mMeditationSession);
        }
        finish();
        super.onBackPressed();
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unbind from the service
        if (mServiceConnection != null)
            unbindService(mServiceConnection);
    }
}
