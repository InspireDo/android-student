package com.inspiredo.tealmorning;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
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
import android.widget.Toast;

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


public class MainActivity extends ActionBarActivity
        implements View.OnClickListener, View.OnLongClickListener, AdapterView.OnItemClickListener {

    private TextView    mStreakTV, mTimerTV, mSessionDateTV;
    private ProgressBar mStreakPB, mDurationPB;
    private Button      mStopBTN, mPlayBTN;
    private ListView    mHistoryList;
    private RelativeLayout
                        mControlLayout;

    public MyMeditation.MeditationProgressListener
                        mMeditationProgressListener = new MyMeditation.MeditationProgressListener() {
        @Override
        public void numberSectionsSet(int tracks) {
            mDurationPB.setProgress(0);
            mDurationPB.setMax(tracks);
        }

        @Override
        public void durationSet(int duration) {
            Log.d("MainActivity", "Duration: " + duration);
            mDurationPB.setProgress(0);
            mDurationPB.setMax(duration);
            mDuration = duration;
            mTimerTV.setText(String.format("%d:%02d", duration/600, (duration/10)%60));

        }

        @Override
        public void onTrackLoaded(int done, int total) {
            Log.d("Loading", "Loaded " + done + " of " + total);
            mDurationPB.setProgress(done);
        }

        @Override
        public void tick() {

            mDurationPB.incrementProgressBy(1);
            mTimerTV.setText(String.format("%d:%02d", --mDuration/600, (mDuration/10)%60));
        }
    };

    private MyMeditation
                        mMeditationSession;

    private JSONArray   mCurrentSections;

    private String      mUserEmail;

    private ArrayAdapter<MeditationSessionModel>
                        mAdapter;

    private int         mPlayState = 0;
    private int         STATUS_UNPREP = 0;
    private int         STATUS_PREP = 1;
    private int         STATUS_PLAYING = 2;

    private int         mDuration;

    private MeditationSessionModel
                        mCurrentRow = new MeditationSessionModel(null, -1);

    private boolean     mIsPrev;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Load from id
        mStopBTN        = (Button)      findViewById(R.id.bStop);
        mPlayBTN        = (Button)      findViewById(R.id.bTogglePlay);
        mStreakTV       = (TextView)    findViewById(R.id.tvStreak);
        mTimerTV        = (TextView)    findViewById(R.id.tvTimeLeft);
        mSessionDateTV  = (TextView)    findViewById(R.id.tvSessionDate);
        mStreakPB       = (ProgressBar) findViewById(R.id.pbGetStreak);
        mDurationPB     = (ProgressBar) findViewById(R.id.pbDuration);
        mHistoryList    = (ListView)    findViewById(R.id.lvHistory);
        mControlLayout  = (RelativeLayout)
                                        findViewById(R.id.rlControls);

        // Click listener set
        mStopBTN.setOnClickListener(this);
        mPlayBTN.setOnClickListener(this);
        mHistoryList.setOnItemClickListener(this);

        // Visibility set
        mStreakPB.setVisibility(View.INVISIBLE);
        mStopBTN.setVisibility(View.INVISIBLE);
        mControlLayout.setVisibility(View.INVISIBLE);

        mStopBTN.setText("Start");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mUserEmail = prefs.getString("User Email", "kessler.penguin55@gmail.com");

        getJSON();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
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
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.bStop:
                if(mPlayState == STATUS_UNPREP) {
                    mStreakPB.setVisibility(View.VISIBLE);
                    v.setVisibility(View.INVISIBLE);
                    prepSections(mCurrentSections);
                } else if (mPlayState == STATUS_PREP) {
                    playSections();
                    mControlLayout.setVisibility(View.VISIBLE);
                } else if (mPlayState == STATUS_PLAYING) {
                    mControlLayout.setVisibility(View.INVISIBLE);
                    mDurationPB.setProgress(0);

                    Intent i = new Intent(this, SessionPlaybackService.class);
                    ServiceConnection sc = new ServiceConnection() {
                        @Override
                        public void onServiceConnected(ComponentName name, IBinder service) {
                            ((SessionPlaybackService.SessionBinder) service).getService().stopSession(mMeditationSession);
                        }

                        @Override
                        public void onServiceDisconnected(ComponentName name) {

                        }
                    };
                    bindService(i, sc, 0);

                    mPlayState = STATUS_UNPREP;
                    mStopBTN.setText("Load Session");
                }
                break;

            case R.id.bTogglePlay:
                mPlayBTN.setText(
                        mMeditationSession.togglePlay() ?
                                getString(R.string.button_pause) : getString(R.string.button_play ));
                break;



            default:
                Log.d("Button Click", "No action implemented");
        }
    }

    public MyMeditation.MeditationProgressListener getProgressListener() {
        return mMeditationProgressListener;
    }

    private void logout() {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this)
                .edit();
        editor.putString("User Email", "");
        editor.apply();
        Intent i = new Intent(this, LoginActivity.class);
        startActivity(i);
        finish();

    }

    public void meditationDone(boolean prev) {
        Log.d("Mediation Status", "Mediation Done!");
        mStopBTN.setVisibility(View.INVISIBLE);
        mControlLayout.setVisibility(View.INVISIBLE);
        mPlayState = STATUS_UNPREP;

        if(prev) {
            mSessionDateTV.setText("Select Session");
            return;
        }

        mStreakPB.setVisibility(View.VISIBLE);

        int index = mAdapter.getItem(mAdapter.getCount() - 2).getIndex() + 1;
        mAdapter.insert(new MeditationSessionModel(new Date(), index), mAdapter.getCount() -1);
        mAdapter.notifyDataSetChanged();
        mHistoryList.setSelection(mAdapter.getCount() -1);

    }

    public void setStreakText(String streak) {
        mStreakTV.setText(streak);
    }

    public void setPrepCurrSections(JSONArray next) {
        mCurrentSections = next;
        prepSections(mCurrentSections);
    }

    public void getJSON() {
        mStreakPB.setVisibility(View.VISIBLE);
        mStopBTN.setVisibility(View.INVISIBLE);

        RequestQueue queue = Volley.newRequestQueue(this);
        String url = getString(R.string.api_url) + "?prev=1&email=" + mUserEmail;

        final Context self = this;

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        String streakString;

                        try {
                            streakString = response.getString("streak");
                            mCurrentSections = response.getJSONArray("sections");
                            prepSections(mCurrentSections);

                            // Get the tasks array
                            JSONArray array = response.getJSONArray("prev");

                            mAdapter = new SessionAdapter(self, R.layout.session_row);


                            // Date format for parsing
                            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                            df.setTimeZone(TimeZone.getTimeZone("UTC"));

                            // Loop and add each task to the adapter
                            for (int i = 0; i < array.length(); i++) {
                                JSONObject session = array.getJSONObject(i);

                                // Get the Task properties
                                Date date = df.parse(session.getString("date_complete"));
                                int index = session.getInt("index");

                                // Add new TaskModel to the adapter
                                mAdapter.add(new MeditationSessionModel(date, index));

                            }

                            mAdapter.add(mCurrentRow);
                            mHistoryList.setAdapter(mAdapter);
                            mHistoryList.setSelection(mAdapter.getCount() -1);



                        } catch (JSONException e) {
                            streakString = "Error";
                        } catch (ParseException e) {
                            streakString = "Error";
                        }

                        mStreakTV.setText(streakString);
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        mStreakTV.setText("Error");
                        mStreakPB.setVisibility(View.INVISIBLE);


                    }
                });

        queue.add(jsObjRequest);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void prepSections(JSONArray jsArray) {
        if(jsArray.length() == 0 ) {
            mSessionDateTV.setText("No Next Session Found");
            mStreakPB.setVisibility(View.INVISIBLE);
            return;
        } else if (!mIsPrev) {
            mSessionDateTV.setText("Next Session");

        }
        mMeditationSession = new MyMeditation();

        for(int i = 0; i < jsArray.length(); i += 2) {
            try {
                Log.d("Meditation Prep", i + " URL: " + jsArray.getString(i));
                Log.d("Meditation Prep", i + "Rest: " + jsArray.getInt(i+1));
                mMeditationSession.addSection(
                        new MyMeditation.MySection(jsArray.getString(i), jsArray.getInt(i+1))
                );
            } catch (JSONException e) {
                e.printStackTrace();
            }


        }

        mMeditationSession.setProgressListener(mMeditationProgressListener);

        mMeditationSession.prepare(new MyMeditation.OnMeditationReadyListener() {
            @Override
            public void onMeditationReady() {
                Log.d("Meditation Prep", "Meditation Ready!");
                mStreakPB.setVisibility(View.INVISIBLE);
                mStopBTN.setVisibility(View.VISIBLE);
                mStopBTN.setText("Start Session");
                mPlayState = STATUS_PREP;
            }
        });

    }

    protected void playSections() {
        mPlayState = STATUS_PLAYING;
        Intent i = new Intent(MainActivity.this, SessionPlaybackService.class);
        startService(i);

        ServiceConnection serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                ((SessionPlaybackService.SessionBinder) service).getService().setActivity(MainActivity.this);
                ((SessionPlaybackService.SessionBinder) service).getService().playSession(mMeditationSession, mIsPrev, mUserEmail);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };
        MainActivity.this.bindService(i, serviceConnection, 0);
        mStopBTN.setText("Stop Session");
        mControlLayout.setVisibility(View.VISIBLE);
        mPlayBTN.setText(getString(R.string.button_pause));
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
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mPlayState == STATUS_PLAYING) return;
        MeditationSessionModel session = mAdapter.getItem(position);

        if (session.getIndex() == -1) {
            mIsPrev = false;
            mSessionDateTV.setText("Next Session");
            prepSections(mCurrentSections);
        } else {
            mIsPrev = true;
            SimpleDateFormat format = new SimpleDateFormat("MM/dd");
            mSessionDateTV.setText(format.format(session.getDate()));

            mStreakPB.setVisibility(View.VISIBLE);
            mStopBTN.setVisibility(View.INVISIBLE);

            RequestQueue queue = Volley.newRequestQueue(this);
            String url = getString(R.string.api_url) + "?index=" + session.getIndex() + "&email=" + mUserEmail;


            JsonObjectRequest jsObjRequest = new JsonObjectRequest
                    (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {

                            try {
                                prepSections(response.getJSONArray("sections"));

                            } catch (JSONException e) {

                            }

                        }
                    }, new Response.ErrorListener() {

                        @Override
                        public void onErrorResponse(VolleyError error) {
                            mStreakTV.setText("Error");
                            mStreakPB.setVisibility(View.INVISIBLE);


                        }
                    });

            queue.add(jsObjRequest);
        }
    }
}
