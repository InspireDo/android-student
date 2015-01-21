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
    private final String mDesc;

    public MeditationSessionModel(String title, int index, String desc) {
        mTitle = title;
        mIndex = index;
        mDesc = desc;
    }

    public String getTitle() {
        return mTitle;
    }

    public int getIndex() {
        return mIndex;
    }

    public String getDesc() { return mDesc; }
}
