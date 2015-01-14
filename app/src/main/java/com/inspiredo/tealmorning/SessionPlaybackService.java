package com.inspiredo.tealmorning;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class SessionPlaybackService extends Service {

    private final IBinder mBinder = new SessionBinder();
    private boolean mIsBound;
    private MainActivity mActivity;

    public SessionPlaybackService() {
    }

    public class SessionBinder extends Binder {
        SessionPlaybackService getService() {
            return SessionPlaybackService.this;
        }
    }

    public void playSession(MyMeditation session, final boolean prev, final String user) {
        Intent i = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.drawable.abc_btn_check_material)
                .setContentTitle("Meditation Session")
                .setContentText("In Progress")
                .setContentIntent(pendingIntent);

        Notification notification = builder.build();
        startForeground(1, notification);

        session.setOnMeditationDoneListener(new MyMeditation.OnMeditationDoneListener() {
            @Override
            public void onMeditationDone() {
                if (mActivity != null) mActivity.meditationDone(prev);

                if (prev) {
                    Toast.makeText(SessionPlaybackService.this, "Session Complete! Nice Work!",
                            Toast.LENGTH_LONG).show();
                    stopSelf();
                    return;
                }

                RequestQueue queue = Volley.newRequestQueue(SessionPlaybackService.this);
                String url = getString(R.string.api_url) + "?email=" + user;


                JsonObjectRequest jsObjRequest = new JsonObjectRequest
                        (Request.Method.POST, url, null, new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                Log.d("Response", response.toString());

                                try {
                                    if (response.getString("status").equals("okay")) {
                                        Toast.makeText(SessionPlaybackService.this, "Session Complete! Nice Work!",
                                                Toast.LENGTH_LONG).show();
                                        if (mActivity != null) {
                                            Log.d("LOL", "LOL");
                                            mActivity.setStreakText(response.getString("streak"));
                                            mActivity.setPrepCurrSections(response.getJSONArray("next"));
                                        }
                                        stopSelf();

                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }, new Response.ErrorListener() {

                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.d("Volley Error", error.toString());
                                stopSelf();


                            }
                        });

                queue.add(jsObjRequest);

            }
        });
        session.setProgressListener(mProgressListener);
        session.play();
    }

    public void stopSession(MyMeditation session) {
        session.stop();
        stopSelf();
    }

    public void setActivity(MainActivity activity) {
        mActivity = activity;
    }

    private MyMeditation.MeditationProgressListener mProgressListener =
            new MyMeditation.MeditationProgressListener() {
                @Override
                public void numberSectionsSet(int tracks) {
                    if (mActivity != null)
                        mActivity.mMeditationProgressListener.numberSectionsSet(tracks);
                }

                @Override
                public void durationSet(int duration) {
                    if (mActivity != null)
                        mActivity.mMeditationProgressListener.durationSet(duration);

                }

                @Override
                public void onTrackLoaded(int done, int total) {
                    if (mActivity != null)
                        mActivity.mMeditationProgressListener.onTrackLoaded(done, total);
                }

                @Override
                public void tick() {
                    if (mActivity != null)
                        mActivity.mMeditationProgressListener.tick();
                }
            };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return START_STICKY;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mActivity = null;
        return true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
