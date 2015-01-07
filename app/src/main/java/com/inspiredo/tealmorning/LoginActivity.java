package com.inspiredo.tealmorning;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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


public class LoginActivity extends ActionBarActivity implements View.OnClickListener {

    EditText mEmailField;
    Button mLoginButton;
    ProgressBar mLoginProgress;
    TextView mLoginError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String userEmail = prefs.getString("User Email", "");

        if (userEmail.length() == 0) {
            setContentView(R.layout.activity_login);

            mEmailField = (EditText) findViewById(R.id.etLoginEmail);
            mLoginButton = (Button) findViewById(R.id.bLogin);
            mLoginProgress = (ProgressBar) findViewById(R.id.pbLogin);
            mLoginError = (TextView) findViewById(R.id.tvLoginError);

            mLoginProgress.setVisibility(View.INVISIBLE);
            mLoginError.setVisibility(View.INVISIBLE);


            mLoginButton.setOnClickListener(this);


        } else {
            Intent i = new Intent(this, MainActivity.class);
            startActivity(i);
            finish();
        }
    }

    private void login() {
        final String email = mEmailField.getText().toString();

        if (email.length() == 0) {
            mLoginError.setVisibility(View.VISIBLE);
            mLoginError.setText("Enter your email...");
            return;
        }

        mLoginButton.setEnabled(false);
        mEmailField.setEnabled(false);
        mLoginProgress.setVisibility(View.VISIBLE);

        final Activity self = this;

        RequestQueue queue = Volley.newRequestQueue(this);
        String url = getString(R.string.api_url) + "?login=1&email=" + email;


        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        try {
                            response.getString("error");
                            mLoginButton.setEnabled(true);
                            mEmailField.setEnabled(true);
                            mLoginProgress.setVisibility(View.INVISIBLE);
                            mLoginError.setVisibility(View.VISIBLE);
                            mLoginError.setText("Email not found...");
                        } catch (JSONException e) {
                            SharedPreferences.Editor editor =
                                    PreferenceManager.getDefaultSharedPreferences(self).edit();
                            editor.putString("User Email", email);
                            editor.apply();

                            Intent i = new Intent(self, MainActivity.class);
                            startActivity(i);
                            self.finish();
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("Volley Error", error.toString());
                        mLoginButton.setEnabled(true);
                        mEmailField.setEnabled(true);
                        mLoginProgress.setVisibility(View.INVISIBLE);
                        mLoginError.setVisibility(View.VISIBLE);
                        mLoginError.setText("Server error. Try again...");

                    }
                });

        queue.add(jsObjRequest);


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, menu);
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
            case R.id.bLogin:
                login();
                break;
            default:
                Log.d("Button Click", "Action Not Implemented");
                break;
        }
    }
}
