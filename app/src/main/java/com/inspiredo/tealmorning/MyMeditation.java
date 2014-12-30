package com.inspiredo.tealmorning;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;

/**
 * MyMeditation class allows for the playback of multiple audio files streamed urls with a
 * gap of silence between clips.
 *
 * (c) 2014. Erik Kessler
 */
public class MyMeditation {

    // Information about the session
    private int         mDuration = 0;
    private int         mCurrentTime = 0;

    // Information used during loading
    private int         mSectionsLoaded;

    // Linked list implementation to store the sections
    private MySection   mHeadSection;
    private MySection   mTailSection;
    private MySection   mCurrentSection;
    private int         mCount;

    // Listeners that can be added to the instance
    private OnMeditationDoneListener
                        mDoneListener;
    private MeditationProgressListener
                        mProgressListener;

    // Handler
    private Handler     mHandler;

    // Gets called when rest period is over
    private Runnable mRestRunnable = new Runnable() {
        @Override
        public void run() {
            mCurrentSection = mCurrentSection.getNext();

            if (mCurrentSection == null) {
                if (mDoneListener != null) mDoneListener.onMeditationDone();
            } else {
                play();
            }
        }
    };

    // Gets called when 1 second passes while the session plays
    private Runnable mTick = new Runnable() {
        @Override
        public void run() {
            mCurrentTime++;

            if(mProgressListener != null)
                mProgressListener.tick();

            if (mCurrentTime < mDuration) {

                mHandler.postDelayed(mTick, 1000);
            }
        }
    };

    // Constructor
    public MyMeditation() {
        mCount = 0;
        mHandler = new Handler();
    }

    // Add a section to the end of the linked list.
    public void addSection(MySection newSection) {

        // Check if this is the first added section
        if(mHeadSection == null) {
            // Setting the head
            Log.d("Meditation Prep", "Head Section set");
            mHeadSection = mTailSection = mCurrentSection = newSection;
        } else {
            // Adding to the tail
            Log.d("Meditation Prep", "Added Section");
            mTailSection.setNext(newSection);
            mTailSection = newSection;
        }

        mCount++; // Increment count
    }

    // Once all sections have been added, call this to fetch the clips and prepare for playback
    public void prepare(final OnMeditationReadyListener listener) {

        // Tell progress listener about number of sections we have
        if (mProgressListener != null)
            mProgressListener.numberSectionsSet(mCount);

        // Iterate through the sections preparing each for playback
        mCurrentSection = mHeadSection;
        while (mCurrentSection != null) {
            final int rest = mCurrentSection.getRest();

            // Listen for when section is prepared
            mCurrentSection.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    Log.d("Meditation Prep", "Section Ready");

                    // Update the duration and sections loaded
                    mDuration += rest + (mp.getDuration() / 1000);
                    mSectionsLoaded++;

                    // Report to listener
                    if (mProgressListener != null)
                        mProgressListener.onTrackLoaded(mSectionsLoaded, mCount);

                    // Check if we have loaded all of them
                    if (mSectionsLoaded == mCount) {
                        Log.d("Meditation Prep", "Duration: " + mDuration);

                        // Report to the listener
                        listener.onMeditationReady();

                        // Report to the progress listener about how long the session is
                        if (mProgressListener != null)
                            mProgressListener.durationSet(mDuration);
                    }
                }
            });

            mCurrentSection.prepareAsync();
            mCurrentSection = mCurrentSection.getNext();
        }

        mCurrentSection = mHeadSection; // Reset to the start of the list
    }

    // Set the listener that should be notified when the session is over
    public void setOnMeditationDoneListener(OnMeditationDoneListener doneListener) {
        this.mDoneListener = doneListener;
    }

    // Set the listener that should be notified of section loading progress and ticks.
    public void setProgressListener(MeditationProgressListener progressListener) {
        this.mProgressListener = progressListener;
    }

    // Once the sections are prepared call this to play them
    public void play() {
        // Upon completion of a section, pause for the rest time
        mCurrentSection.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.release(); // Release the resource

                // Play the next one after a delay
                mHandler.postDelayed(mRestRunnable, mCurrentSection.getRest() * 1000);
            }
        });

        // Start playing the current section
        mCurrentSection.start();

        // If this is the first section, start the clock
        if (mCurrentTime == 0)
            mHandler.postDelayed(mTick, 1000);

    }

    // Listen for when meditation is ready to be played after calling prepare().
    public interface OnMeditationReadyListener {
        public void onMeditationReady();
    }

    // Listen for when the meditation session is over after calling play().
    public interface OnMeditationDoneListener {
        public void onMeditationDone();
    }

    // Listen for updates about the session progress (ticks) and the number of sections loaded
    public interface  MeditationProgressListener {
        public void numberSectionsSet(int tracks);
        public void durationSet(int duration);
        public void onTrackLoaded(int done, int total);
        public void tick();
    }

    /**
     * Wrapper class of MediaPlayer that allows for storage of info about rest and a
     * next section to allow for a linked list implementation.
     */
    public static class MySection extends MediaPlayer {

        // Number of seconds to rest after this clip
        private int         mRest;

        // Next section to be played. Null is this is the last
        private MySection   mNext;

        public MySection(String url, int rest)  {
            this.setAudioStreamType(AudioManager.STREAM_MUSIC);
            try {
                this.setDataSource(url);
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.setRest(rest);


        }

        protected void setNext(MySection next) {
            this.mNext = next;
        }

        protected void setRest(int rest) {
            this.mRest = rest;
        }

        protected int getRest() {
            return this.mRest;
        }

        protected MySection getNext() {
            return this.mNext;
        }

    }

}
