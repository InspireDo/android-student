package com.inspiredo.tealmorning;

import java.util.Date;

/**
 * Models a previous meditation session.
 */
public class MeditationSessionModel {
    private Date mDate;
    private int  mIndex;

    public MeditationSessionModel(Date date, int index) {
        mDate = date;
        mIndex = index;
    }

    public void setDate(Date date) {
        mDate = date;
    }

    public void setIndex(int index){
        mIndex = index;
    }

    public Date getDate() {
        return mDate;
    }

    public int getIndex() {
        return mIndex;
    }
}
