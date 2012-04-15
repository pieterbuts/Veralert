package com.fawepark.veralert;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class C2DMReceiver extends BroadcastReceiver {
    private final String TAG = "Veralert C2DM";
    
	@Override
	public void onReceive(Context context, Intent intent) {
		// Run intent in service 			

		C2DMIntentHandler.runIntentInService(context, intent);
	}
}
