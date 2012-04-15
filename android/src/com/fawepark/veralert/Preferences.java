package com.fawepark.veralert;

import java.util.UUID;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
 
public class Preferences extends PreferenceActivity {
	private Preference mPreferenceAlert1;  
	private Preference mPreferenceAlert2;  
	private Preference mPreferenceAlert3;  
	private Preference mPreferenceAlert4;  
	private Preference mPreferenceAlert5;  
	
	private static final String RINGTONE_PREF = "ringtonePref";
	private static final String MAX_RETENTION_PREF = "MaxRetention";
	private static final String VIBRATION_PREF = "vibrationPref";
	private static final String DEVICE_ID = "RegistrationID";
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        mPreferenceAlert1 = this.findPreference(RINGTONE_PREF + "1");
        mPreferenceAlert2 = this.findPreference(RINGTONE_PREF + "2");
        mPreferenceAlert3 = this.findPreference(RINGTONE_PREF + "3");
        mPreferenceAlert4 = this.findPreference(RINGTONE_PREF + "4");
        mPreferenceAlert5 = this.findPreference(RINGTONE_PREF + "5");
        
        // Get the custom preference

        String ID = getIDString(this);
        
        if (ID.equals("")) {
            ID = scrambleIDS();
            Toast.makeText(this, getString(R.string.generated_id) + ID, Toast.LENGTH_SHORT).show();
        }
        
        Preference RegID = this.findPreference(DEVICE_ID);
        RegID.setTitle(ID);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mPreferenceAlert1.setSummary(getSummary(RINGTONE_PREF + "1", getString(R.string.alert1_summary)));
        mPreferenceAlert2.setSummary(getSummary(RINGTONE_PREF + "2", getString(R.string.alert2_summary)));
        mPreferenceAlert3.setSummary(getSummary(RINGTONE_PREF + "3", getString(R.string.alert3_summary)));
        mPreferenceAlert4.setSummary(getSummary(RINGTONE_PREF + "4", getString(R.string.alert4_summary)));
        mPreferenceAlert5.setSummary(getSummary(RINGTONE_PREF + "5", getString(R.string.alert5_summary)));
    }

    private String getSummary(String preference, String defaultSummary) {
    	String summary = "";
        SharedPreferences sp = getDefault(this);
        
        String prefText = sp.getString(preference, "");
        
        if (prefText.equals("")) {
        	// No ringtone selected or silent: display default text
        	
        	summary = defaultSummary;
        }
        else {
	        Uri ringtoneUri = Uri.parse(prefText);
	        Ringtone ringtone = RingtoneManager.getRingtone(this, ringtoneUri);
	        summary = ringtone.getTitle(this);
        }
    	
    	return summary;
    }
    
    private String scrambleIDS() {
        UUID idOne = UUID.randomUUID();
        Log.i(Notifications.TAG,"Created UUID of " + idOne);                  
        Long l1 = idOne.getLeastSignificantBits();
        
        if ( l1 < 0) { 
        	l1 = l1 + Long.MAX_VALUE; 
        }
        
        String token1 = Long.toString(l1, 36);  
        Editor ed = getEditor(this);
        ed.putString("ID", token1);
        ed.commit();
        
        return token1;
    }

    private static Editor getEditor(Context ctx) {
        return getDefault(ctx).edit();
    }

    public static String getIDString(Context ctx) {
        SharedPreferences sp = getDefault(ctx);
        return sp.getString("ID", "");
    }
    
    public static String getAlertTone(Context ctx, String val) {
        String pref_string = RINGTONE_PREF + val;
        SharedPreferences sp = getDefault(ctx);
        String res_string = sp.getString(pref_string, "-");
        
        if (res_string.equals("-")) {
        	getAlertTone(ctx, "1");
        }
        
        Log.i(Notifications.TAG,"using ringtone: " + res_string + " for AlertTone" + val); 
        
        return res_string;
    }

    public static int getMaxRetention(Context ctx) {
        SharedPreferences sp = getDefault(ctx);
        int maxRetention = 5;
        
        try {
        	maxRetention = sp.getInt(MAX_RETENTION_PREF, maxRetention);
        } catch (ClassCastException e) {
        	// Just return the default value
        }
        
        return maxRetention;
    }

    public static boolean getVibration(Context ctx) {
        SharedPreferences sp = getDefault(ctx);
        boolean vibrate = false;
        
        try {
        	vibrate = sp.getBoolean(VIBRATION_PREF, false);
        } catch (ClassCastException e) {
        	// Just return the default value
        }
        
        return vibrate;
    }

    private static SharedPreferences getDefault(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx);
    }
}
