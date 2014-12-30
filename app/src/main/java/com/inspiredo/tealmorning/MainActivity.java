package com.inspiredo.tealmorning;

import android.media.MediaPlayer;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
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


public class MainActivity extends ActionBarActivity
        implements View.OnClickListener, View.OnLongClickListener {

    private TextView    mStreakTV;
    private ProgressBar mStreakPB;
    private Button      mGetStreakBTN;

    private MyMeditation.MeditationProgressListener
                        mMeditationProgressListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGetStreakBTN   = (Button)      findViewById(R.id.bGetStreak);
        mStreakTV       = (TextView)    findViewById(R.id.tvStreak);
        mStreakPB       = (ProgressBar) findViewById(R.id.pbGetStreak);

        final ProgressBar pbDuration = (ProgressBar) findViewById(R.id.pbDuration);

        mGetStreakBTN.setOnClickListener(this);
        mGetStreakBTN.setOnLongClickListener(this);

        mStreakPB.setVisibility(View.INVISIBLE);

        mMeditationProgressListener = new MyMeditation.MeditationProgressListener() {
            @Override
            public void numberSectionsSet(int tracks) {
                pbDuration.setProgress(0);
                pbDuration.setMax(tracks);
            }

            @Override
            public void durationSet(int duration) {
                pbDuration.setProgress(0);
                pbDuration.setMax(duration);
            }

            @Override
            public void onTrackLoaded(int done, int total) {
                Log.d("Loading", "Loaded " + done + " of " + total);
                pbDuration.setProgress(done);
            }

            @Override
            public void tick() {
                pbDuration.incrementProgressBy(1);
            }
        };

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

            default:
                Log.d("Button Click", "No action implemented");
        }
    }

    public void getJSON() {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = getString(R.string.api_url) + "?email=kessler.penguin55@gmail.com";


        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        //Log.d("JSON Response", "Response: " + response.toString());
                        String streakString;

                        try {
                            streakString = response.getString("streak");
                            playSections(response.getJSONArray("sections"));

                        } catch (JSONException e) {
                            streakString = "Error";
                        }

                        mStreakTV.setText(getString(R.string.streak) + streakString);
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
        final MyMeditation session = new MyMeditation();

        for(int i = 0; i < jsArray.length(); i += 2) {
            try {
                Log.d("Meditation Prep", i + " URL: " + jsArray.getString(i));
                Log.d("Meditation Prep", i + "Rest: " + jsArray.getInt(i+1));
                session.addSection(
                        new MyMeditation.MySection(jsArray.getString(i), jsArray.getInt(i+1))
                );
            } catch (JSONException e) {
                e.printStackTrace();
            }


        }

        session.setProgressListener(mMeditationProgressListener);

        session.setOnMeditationDoneListener(new MyMeditation.OnMeditationDoneListener() {
            @Override
            public void onMeditationDone() {
                Log.d("Mediation Status", "Mediation Done!");
            }
        });

        session.prepare(new MyMeditation.OnMeditationReadyListener() {
            @Override
            public void onMeditationReady() {
                Log.d("Meditation Prep", "Meditation Ready!");
                session.play();
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

                MediaPlayer player = new MediaPlayer();
                try {
                    player.setDataSource("http://soundboard.panictank.net/" + meme);
                    player.prepare();
                    player.start();

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
