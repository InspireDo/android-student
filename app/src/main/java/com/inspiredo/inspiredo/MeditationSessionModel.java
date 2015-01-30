package com.inspiredo.inspiredo;

import java.util.Date;

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
    private final Date mDate;

    public MeditationSessionModel(String title, int index, String desc, Date date) {
        mTitle = title;
        mIndex = index;
        mDesc = desc;
        mDate = date;
    }

    public String getTitle() {
        return mTitle;
    }

    public int getIndex() {
        return mIndex;
    }

    public String getDesc() { return mDesc; }

    public Date getDate() { return mDate; }
}
