package com.inspiredo.tealmorning;

import java.util.Date;

/**
 * Models a previous meditation session.
 *
 * Created by Erik Kessler
 * (c) 2015 inspireDo.
 */
class MeditationSessionModel {
    private final Date mDate;
    private final int  mIndex;

    public MeditationSessionModel(Date date, int index) {
        mDate = date;
        mIndex = index;
    }

    public Date getDate() {
        return mDate;
    }

    public int getIndex() {
        return mIndex;
    }
}
