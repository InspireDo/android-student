package com.inspiredo.inspiredo;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.inspiredo.inspiredo.R;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Service that allows for the playback of the session in the background.
 * This means the session will continue when the screen is off.
 *
 * Created by Erik Kessler
 * (c) 2015 inspireDo
 */
public class SessionPlaybackService extends Service {

    /* Binder that is used when an activity binds to this */
    private final IBinder mBinder = new SessionBinder();

    /**
     * Binder subclass that allows us to return this service to the binding activity
     */
    public class SessionBinder extends Binder {
        SessionPlaybackService getService() {
            return SessionPlaybackService.this;
        }
    }

    /* Activity that the service should alert about stuff */
    private SessionDetailActivity mActivity;

    /**
     * Plays a mediation session. The session should be prepared and ready to play.
     * @param session The prepared session to play
     * @param prev True if this is a replay
     * @param user Email of the user - used for the server request
     */
    public void playSession(MyMeditation session, final boolean prev, final String user) {
        /* Build a notification so we can start in the foreground */
        startForeground(1, buildNotification());

        // Handle when the mediation is done
        session.setOnMeditationDoneListener(new MyMeditation.OnMeditationDoneListener() {
            @Override
            public void onMeditationDone() {

                if (prev) {
                    Toast.makeText(SessionPlaybackService.this, "Session Complete! Nice Work!",
                            Toast.LENGTH_LONG).show();
                    if (mActivity != null) mActivity.meditationDone(true, null, null);
                    stopForeground(true);
                    stopSelf();
                } else {
                    if (mActivity != null) mActivity.meditationPreDone();

                    // Tell the backend service that we are done
                    reportSessionDone(user);
                }

            }
        });

        // Set the progress listener so we can track "ticks"
        session.setProgressListener(PROGRESS_LISTENER);
        session.play();
    }

    /**
     * Make a POST request to the server indicating that this session is done.
     * If there is an activity attached, report the progress to it.
     *
     * @param user The email of the user
     */
    private void reportSessionDone(String user) {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = getString(R.string.api_url) + "?email=" + user;


        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.POST, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (response.getString("status").equals("okay")) {
                                Toast.makeText(SessionPlaybackService.this, "Session Complete! Nice Work!",
                                        Toast.LENGTH_LONG).show();

                                // Update the activity if it exists
                                if (mActivity != null) {
                                    mActivity.meditationDone(false,
                                            response.getString("title"),
                                            response.getString("streak"));

                                }

                                // Stop the service
                                stopForeground(true);
                                stopSelf();

                            }
                        } catch (JSONException e) {
                            stopForeground(true);
                            stopSelf();
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        stopForeground(true);
                        stopSelf();


                    }
                });

        queue.add(jsObjRequest);
    }

    /**
     * Build a notification for when the session is playing
     */
    private Notification buildNotification() {
        Intent i = new Intent(this, SessionsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.drawable.abc_btn_check_material)
                .setContentTitle("Meditation Session")
                .setContentText("In Progress")
                .setContentIntent(pendingIntent);

        return builder.build();
    }

    public void stopSession(MyMeditation session) {
        session.stop();
        stopForeground(true);
        stopSelf();
    }

    public void setActivity(SessionDetailActivity activity) {
        mActivity = activity;
    }

    /**
     * Progress listener that reports to the activity if it exists
     */
    private final MyMeditation.MeditationProgressListener PROGRESS_LISTENER =
            new MyMeditation.MeditationProgressListener() {

                @Override
                public void durationSet(int duration) {
                    if (mActivity != null)
                        mActivity.PROGRESS_LISTENER.durationSet(duration);

                }

                @Override
                public void tick() {
                    if (mActivity != null)
                        mActivity.PROGRESS_LISTENER.tick();
                }
            };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return START_STICKY;
    }

    /**
     * Remove the activity
     */
    @Override
    public boolean onUnbind(Intent intent) {
        mActivity = null;
        return true;
    }

    /**
     * Return the SessionBinder to allow communication to the service
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
