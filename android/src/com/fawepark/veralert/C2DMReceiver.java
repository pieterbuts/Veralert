package com.fawepark.veralert;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class C2DMReceiver extends BroadcastReceiver {
    private final String TAG = "Veralert.C2DMReceiver";
    
	@Override
	public void onReceive(Context context, Intent intent) {
		// Run intent in service 			
        Log.i(TAG,"Intent received"); 

		C2DMIntentHandler.runIntentInService(context, intent);
	}
}
