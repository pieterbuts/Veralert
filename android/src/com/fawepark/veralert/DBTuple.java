package com.fawepark.veralert;

import android.content.Context;
import android.text.format.DateFormat;
import android.text.format.Time;

public class DBTuple {
	public int id;
    public String message;
    public int alertType;
    public long timeStamp;
    public int veraId;
    public boolean Selected = false;

    public String TimeString(Context context) {
    	String time;
    	String date = DateFormat.getDateFormat(context).format(timeStamp);
    	Time t = new Time();
    	
    	t.set(timeStamp);
    	
    	if (DateFormat.is24HourFormat(context)) {
    		time = t.format("%H:%M:%S");
    	} else {
    		time = t.format("%l:%M:%S %P");
    	}
    	
        return date + " " + time;
    }
}
