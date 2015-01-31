package com.inspiredo.inspiredo;

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

/**
 * This is the class that activity that starts up first.
 * We check to see if the user has logged in before. If she has then we continue directly
 * to the MainActivity.
 *
 * The only authentication is the email. If the email is a signed up user we store the email
 * in shared preferences.
 *
 * Created by Erik Kessler
 * (c) 2015 inspireDo.
 */
public class LoginActivity extends ActionBarActivity implements View.OnClickListener {

    /**
     * UI elements
     */
    private EditText mEmailField;   // Text field for inputted email
    private Button mLoginButton;    // Button to login
    private ProgressBar mLoginProgress; // Loading spinner to indicate working
    private TextView mLoginError;   // Shows error text

    /**
     * Constants
     */
    public static final String PREF_KEY = "User Email";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the stored email if there is one
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String userEmail = prefs.getString(PREF_KEY, "");

        // Check if there is a logged in user
        if (userEmail.length() == 0) {
            // No user - need to login
            // Setup the login UI
            setContentView(R.layout.activity_login);

            // Get the UI elements from their ID's
            mEmailField = (EditText) findViewById(R.id.etLoginEmail);
            mLoginButton = (Button) findViewById(R.id.bLogin);
            mLoginProgress = (ProgressBar) findViewById(R.id.pbLogin);
            mLoginError = (TextView) findViewById(R.id.tvLoginError);

            // Set visibility
            mLoginProgress.setVisibility(View.INVISIBLE);
            mLoginError.setVisibility(View.INVISIBLE);

            // Set click listeners
            mLoginButton.setOnClickListener(this);


        } else {
            // Existing user
            // Go to MainActivity
            Intent i = new Intent(this, SessionsActivity.class);
            startActivity(i);
            finish();
        }
    }

    /**
     * Gets the email entered into the text field.
     * Makes asks the server if this is a valid email.
     */
    private void login() {
        final String email = mEmailField.getText().toString();

        // Check if user entered an email
        if (email.length() == 0) {
            mLoginError.setVisibility(View.VISIBLE);
            mLoginError.setText("Enter your email...");
            return;
        }

        // Disable the button and text field while we call the server
        mLoginButton.setEnabled(false);
        mEmailField.setEnabled(false);
        mLoginProgress.setVisibility(View.VISIBLE);

        RequestQueue queue = Volley.newRequestQueue(this);
        String url = getString(R.string.api_url) + "?login=1&email=" + email;

        // Make a request to login
        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        // See if the response has an error
                        try {
                            // Error
                            response.getString("error");
                            mLoginButton.setEnabled(true);
                            mEmailField.setEnabled(true);
                            mLoginProgress.setVisibility(View.INVISIBLE);
                            mLoginError.setVisibility(View.VISIBLE);
                            mLoginError.setText("Email not found...");
                        } catch (JSONException e) {
                            // No error
                            // Save the email
                            SharedPreferences.Editor editor =
                                    PreferenceManager.getDefaultSharedPreferences(LoginActivity.this).edit();
                            editor.putString(PREF_KEY, email);
                            editor.apply();

                            // Start the main activity
                            Intent i = new Intent(LoginActivity.this, SessionsActivity.class);
                            startActivity(i);
                            LoginActivity.this.finish();
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Error on the request
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
    public void onClick(View v) {
        // Handle each button
        switch (v.getId()) {
            case R.id.bLogin:
                login();
                break;
            default:
                Log.d("Button Click", "Action Not Implemented");
                break;
        }
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
}
