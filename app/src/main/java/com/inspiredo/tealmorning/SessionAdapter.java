package com.inspiredo.tealmorning;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;

/**
 * Adapter for History of sessions
 */
public class SessionAdapter extends ArrayAdapter<MeditationSessionModel> {

    public SessionAdapter(Context context, int resource) {
        super(context, resource);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Inflate the view if needed
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.session_row, null);
        }

        // Get the Task
        MeditationSessionModel session = getItem(position);
        TextView date = (TextView) convertView.findViewById(R.id.tvDate);

        if (session.getIndex() == -1) {
            date.setText("Next Session");
        } else {
            SimpleDateFormat format = new SimpleDateFormat("MM/dd");
            date.setText(format.format(session.getDate()));
        }


        return convertView;
    }

}
