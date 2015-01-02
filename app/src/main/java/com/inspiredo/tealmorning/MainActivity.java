package com.inspiredo.tealmorning;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
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
import java.util.Random;


public class MainActivity extends ActionBarActivity
        implements View.OnClickListener, View.OnLongClickListener {

    private TextView    mStreakTV;
    private ProgressBar mStreakPB, mDurationPB;
    private Button      mGetStreakBTN, mStopBTN, mPlayBTN, mUserBTN;

    private MyMeditation.MeditationProgressListener
                        mMeditationProgressListener;

    private MyMeditation
                        mMeditationSession;

    private String      mUserEmail;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Load from id
        mGetStreakBTN   = (Button)      findViewById(R.id.bGetStreak);
        mStopBTN        = (Button)      findViewById(R.id.bStop);
        mPlayBTN        = (Button)      findViewById(R.id.bTogglePlay);
        mUserBTN        = (Button)      findViewById(R.id.bUser);
        mStreakTV       = (TextView)    findViewById(R.id.tvStreak);
        mStreakPB       = (ProgressBar) findViewById(R.id.pbGetStreak);
        mDurationPB     = (ProgressBar) findViewById(R.id.pbDuration);

        // Click listener set
        mGetStreakBTN.setOnClickListener(this);
        mStopBTN.setOnClickListener(this);
        mPlayBTN.setOnClickListener(this);
        mUserBTN.setOnClickListener(this);

        // Long click listeners set
        mGetStreakBTN.setOnLongClickListener(this);

        // Visibility set
        mStreakPB.setVisibility(View.INVISIBLE);
        mStopBTN.setVisibility(View.INVISIBLE);
        mPlayBTN.setVisibility(View.INVISIBLE);

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
            }

            @Override
            public void onTrackLoaded(int done, int total) {
                Log.d("Loading", "Loaded " + done + " of " + total);
                mDurationPB.setProgress(done);
            }

            @Override
            public void tick() {
                mDurationPB.incrementProgressBy(1);
            }
        };

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mUserEmail = prefs.getString("User Email", "kessler.penguin55@gmail.com");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bGetStreak:
                mStreakPB.setVisibility(View.VISIBLE);
                mGetStreakBTN.setEnabled(false);
                getJSON();
                break;

            case R.id.bStop:
                v.setVisibility(View.INVISIBLE);
                mPlayBTN.setVisibility(View.INVISIBLE);
                mDurationPB.setProgress(0);
                mMeditationSession.stop();
                break;

            case R.id.bTogglePlay:
                mPlayBTN.setText(
                        mMeditationSession.togglePlay() ?
                                getString(R.string.button_pause) : getString(R.string.button_play ));
                break;

            case R.id.bUser:
                changeUser();
                break;

            default:
                Log.d("Button Click", "No action implemented");
        }
    }

    private void changeUser() {
        Log.d("User", "Change User from " + mUserEmail);

        String currentUser;
        if (mUserEmail.length() <= 5) {
            currentUser = mUserEmail;
        } else {
            currentUser = mUserEmail.substring(0, 5) + "...";
        }

        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Change User");
        alert.setMessage("Current user is " + currentUser +
                "\nChange users by entering an email below");

        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        input.setHint("email");
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                mUserEmail = input.getText().toString();
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        alert.show();
    }

    public void getJSON() {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = getString(R.string.api_url) + "?email=" + mUserEmail;


        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        String streakString;

                        try {
                            streakString = response.getString("streak");
                            playSections(response.getJSONArray("sections"));

                        } catch (JSONException e) {
                            streakString = "Error";
                        }

                        mStreakTV.setText(getString(R.string.streak) + " " + streakString);
                        mStreakPB.setVisibility(View.INVISIBLE);
                        mGetStreakBTN.setEnabled(true);
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        mStreakTV.setText("Error");
                        mStreakPB.setVisibility(View.INVISIBLE);
                        mGetStreakBTN.setEnabled(true);


                    }
                });

        queue.add(jsObjRequest);
    }

    private  void playSections(JSONArray jsArray) {
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

        final Context self = this;

        mMeditationSession.setProgressListener(mMeditationProgressListener);

        mMeditationSession.setOnMeditationDoneListener(new MyMeditation.OnMeditationDoneListener() {
            @Override
            public void onMeditationDone() {
                Log.d("Mediation Status", "Mediation Done!");
                mStopBTN.setVisibility(View.INVISIBLE);
                mPlayBTN.setVisibility(View.INVISIBLE);

                RequestQueue queue = Volley.newRequestQueue(self);
                String url = getString(R.string.api_url) + "?email=" + mUserEmail;


                JsonObjectRequest jsObjRequest = new JsonObjectRequest
                        (Request.Method.POST, url, null, new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                Log.d("Response", response.toString());

                                try {
                                    if (response.getString("status").equals("okay")) {
                                        Toast.makeText(self, "Session Complete! Nice Work!",
                                                Toast.LENGTH_LONG).show();
                                        mStreakTV.setText(self.getString(R.string.streak) +
                                                " " + response.getString("streak"));
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }, new Response.ErrorListener() {

                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.d("Volley Error", error.toString());


                            }
                        });

                queue.add(jsObjRequest);
            }
        });

        mMeditationSession.prepare(new MyMeditation.OnMeditationReadyListener() {
            @Override
            public void onMeditationReady() {
                Log.d("Meditation Prep", "Meditation Ready!");
                mMeditationSession.play();
                mStopBTN.setVisibility(View.VISIBLE);
                mPlayBTN.setVisibility(View.VISIBLE);
                mPlayBTN.setText(getString(R.string.button_pause));
            }
        });

    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.bGetStreak:
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
}
