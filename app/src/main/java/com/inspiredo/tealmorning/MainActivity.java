package com.inspiredo.tealmorning;

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

import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends ActionBarActivity implements View.OnClickListener {

    private TextView    mStreakTV;
    private ProgressBar mStreakPB;
    private Button      mGetStreakBTN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGetStreakBTN   = (Button)      findViewById(R.id.bGetStreak);
        mStreakTV       = (TextView)    findViewById(R.id.tvStreak);
        mStreakPB       = (ProgressBar) findViewById(R.id.pbGetStreak);

        mGetStreakBTN.setOnClickListener(this);

        mStreakPB.setVisibility(View.INVISIBLE);

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
}
