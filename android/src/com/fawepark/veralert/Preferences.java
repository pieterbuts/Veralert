package com.fawepark.veralert;

import java.util.UUID;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
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

	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        mPreferenceAlert1 = this.findPreference("ringtonePref1");
        mPreferenceAlert2 = this.findPreference("ringtonePref2");
        mPreferenceAlert3 = this.findPreference("ringtonePref3");
        mPreferenceAlert4 = this.findPreference("ringtonePref4");
        mPreferenceAlert5 = this.findPreference("ringtonePref5");
        
        // Get the custom preference

        String ID = getIDString(this);
        
        if (ID.equals("")) {
            ID = scrambleIDS();
            Toast t = Toast.makeText(this, "ID used is : " + ID, Toast.LENGTH_SHORT);
            t.show();  
        }
        
        EditTextPreference RegID = (EditTextPreference)this.findPreference("RegistrationID");
        RegID.setTitle(ID);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mPreferenceAlert1.setSummary(getSummary("ringtonePref1", "Select an alert sound for AlertTone1"));
        mPreferenceAlert2.setSummary(getSummary("ringtonePref2", "Select an alert sound for AlertTone2"));
        mPreferenceAlert3.setSummary(getSummary("ringtonePref3", "Select an alert sound for AlertTone3"));
        mPreferenceAlert4.setSummary(getSummary("ringtonePref4", "Select an alert sound for AlertTone4"));
        mPreferenceAlert5.setSummary(getSummary("ringtonePref5", "Select an alert sound or ringtone for AlertTone5"));
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
        String pref_string = "ringtonePref" + val;
        SharedPreferences sp = getDefault(ctx);
        String res_string = sp.getString(pref_string, "");
        Log.i(Notifications.TAG,"using ringtone: " + res_string + " for AlertTone" + val); 
        
        return res_string;
    }

    public static int getMaxRetention(Context ctx) {
        SharedPreferences sp = getDefault(ctx);
        String tmp = sp.getString("MaxRetention", "0");
        
        return Integer.parseInt(tmp);
    }


    private static SharedPreferences getDefault(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx);
    }
}
