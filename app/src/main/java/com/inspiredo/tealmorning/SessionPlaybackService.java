package com.inspiredo.tealmorning;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class SessionPlaybackService extends Service {

    private final IBinder mBinder = new SessionBinder();

    public SessionPlaybackService() {
    }

    public class SessionBinder extends Binder {
        SessionPlaybackService getService() {
            return SessionPlaybackService.this;
        }
    }

    public void playSession(MyMeditation session) {
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
                Log.d("Meditation", "DONE");
                stopSelf();
            }
        });
        session.play();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
