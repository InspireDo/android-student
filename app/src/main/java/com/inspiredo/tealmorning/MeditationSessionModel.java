package com.inspiredo.tealmorning;

/**
 * Models a previous meditation session.
 *
 * Created by Erik Kessler
 * (c) 2015 inspireDo.
 */
class MeditationSessionModel {
    private final String mTitle;
    private final int  mIndex;

    public MeditationSessionModel(String title, int index) {
        mTitle = title;
        mIndex = index;
    }

    public String getTitle() {
        return mTitle;
    }

    public int getIndex() {
        return mIndex;
    }
}
