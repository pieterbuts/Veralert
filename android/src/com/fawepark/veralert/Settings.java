package com.fawepark.veralert;

import java.util.UUID;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
 

public class Settings {
    private final String TAG = "Veralert.Settings";
	private SharedPreferences prefs;
	private Context context;
	
	// Preference names that are not used in the preference dialog are defined here
	public static final String PREF_REGISTRATION_ID = "ID";
	public static final String PREF_DEVICE_ID_ORG = "RegistrationID";
	public static final String PREF_ORDER = "SortOrder";
	
	Settings(Context context) {
		this.context = context;
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		// Copy the Device Identifier from the original preference (RegistrationID) if present
		// The original preference is then removed
		
		if (prefs.contains(PREF_DEVICE_ID_ORG)) {
			Editor ed = prefs.edit();
			String deviceId = "";
			
	        try {
	        	deviceId = prefs.getString(PREF_DEVICE_ID_ORG, "");
	        } catch (ClassCastException e) {
	        	// Just return the default value
	        }
			
	        ed.putString(context.getString(R.string.deviceIdPref), deviceId);
	        ed.remove(PREF_DEVICE_ID_ORG);
	        ed.commit();
		}
	}
	
    public String getRingtoneSummary(String preference, String defaultSummary) {
    	String summary = "";
        
        String prefText = prefs.getString(preference, "");
        
        if (prefText.equals("")) {
        	// No ringtone selected or silent: display default text
        	
        	summary = defaultSummary;
        }
        else {
	        Uri ringtoneUri = Uri.parse(prefText);
	        Ringtone ringtone = RingtoneManager.getRingtone(context, ringtoneUri);
	        summary = ringtone.getTitle(context);
        }
    	
    	return summary;
    }
    
    public String scrambleIDS() {
        UUID idOne = UUID.randomUUID();
        Log.i(TAG,"Created UUID of " + idOne);                  
        Long l1 = idOne.getLeastSignificantBits();
        
        if ( l1 < 0) { 
        	l1 = l1 + Long.MAX_VALUE; 
        }
        
        String token1 = Long.toString(l1, 36);  
        setDeviceIdentifier(token1);
        
        return token1;
    }

    public String getAlertTone(String val) {
        String pref_string = context.getString(R.string.ringtonePref) + val;
        String res_string = prefs.getString(pref_string, "-");
        
        if (res_string.equals("-")) {
        	getAlertTone("1");
        }
        
        Log.i(TAG,"using ringtone: " + res_string + " for AlertTone" + val); 
        
        return res_string;
    }

    public int getMaxRetention() {
        int maxRetention = 5;
        
        try {
        	maxRetention = prefs.getInt(context.getString(R.string.maxRetentionPref), maxRetention);
        } catch (ClassCastException e) {
        	// Just return the default value
        }
        
        return maxRetention;
    }

    public boolean getVibration() {
        boolean vibrate = false;
        
        try {
        	vibrate = prefs.getBoolean(context.getString(R.string.vibrationPref), false);
        } catch (ClassCastException e) {
        	// Just return the default value
        }
        
        return vibrate;
    }

    public String getDeviceIdentifier() {
        String deviceId = "";

        try {
        	deviceId = prefs.getString(context.getString(R.string.deviceIdPref), "");
        } catch (ClassCastException e) {
        	// Just return the default value
        }
        
        return deviceId;
    }

    public String getManualDeviceIdentifier() {
        String deviceId = "";

        try {
        	deviceId = prefs.getString(context.getString(R.string.manualDeviceIdPref), "");
        } catch (ClassCastException e) {
        	// Just return the default value
        }
        
        return deviceId;
    }

    public void setDeviceIdentifier(String deviceId) {
        Editor ed = prefs.edit();
        
        ed.putString(context.getString(R.string.deviceIdPref), deviceId);
        ed.putString(context.getString(R.string.manualDeviceIdPref), deviceId);
        ed.putString(PREF_REGISTRATION_ID, "");
        ed.commit();
    }
    
    public String getRegistrationIdentifier() {
        String registrationId = "";

        try {
        	registrationId = prefs.getString(PREF_REGISTRATION_ID, "");
        } catch (ClassCastException e) {
        	// Just return the default value
        }
        
        return registrationId;
    }
    
    public void setRegistrationIdentifier(String registrationId) {
        Editor ed = prefs.edit();
        
        ed.putString(PREF_REGISTRATION_ID, registrationId);
        ed.commit();
    }
    
    public int getOrder() {
        int order = DBViewAdapter.BY_MESSAGE;
        
        try {
        	order = prefs.getInt(PREF_ORDER, order);
        } catch (ClassCastException e) {
        	// Just return the default value
        }
        
        return order;
    }

    public void setOrder(int order) {
        Editor ed = prefs.edit();
        
        ed.putInt(PREF_ORDER, order);
        ed.commit();
    }
}
