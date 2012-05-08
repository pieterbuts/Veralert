package com.fawepark.veralert;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.widget.Toast;
 

public class Preferences extends PreferenceActivity {
	private Settings settings;
	private Preference preferenceAlert1;  
	private Preference preferenceAlert2;  
	private Preference preferenceAlert3;  
	private Preference preferenceAlert4;  
	private Preference preferenceAlert5;  
	private Preference preferenceManualDeviceIdentifier;  
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        settings = new Settings(this);
        String registrationId = settings.getRegistrationIdentifier();
        
        preferenceAlert1 = this.findPreference(getString(R.string.ringtonePref1));
        preferenceAlert2 = this.findPreference(getString(R.string.ringtonePref2));
        preferenceAlert3 = this.findPreference(getString(R.string.ringtonePref3));
        preferenceAlert4 = this.findPreference(getString(R.string.ringtonePref4));
        preferenceAlert5 = this.findPreference(getString(R.string.ringtonePref5));
        preferenceManualDeviceIdentifier = this.findPreference(getString(R.string.manualDeviceIdPref));
        
        preferenceManualDeviceIdentifier.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
            	Boolean result = false;
            	String deviceId = newValue.toString().toLowerCase();
            	
            	if (deviceId.equals("") || deviceId.matches("[[a-z]\\d]{12}")) {
            		result = true;
            	} else {
                    Toast.makeText(preference.getContext(), getString(R.string.manual_device_id_error), Toast.LENGTH_LONG).show();
            	}
            		
            	return result;
            }
        });
        
        String deviceIdentifier = settings.getDeviceIdentifier();
        
        if (deviceIdentifier.equals("")) {
        	deviceIdentifier = settings.scrambleIDS();
            Toast.makeText(this, getString(R.string.generated_id) + deviceIdentifier, Toast.LENGTH_SHORT).show();
        }
        
        Preference deviceIDPref = this.findPreference(getString(R.string.deviceIdPref));
        deviceIDPref.setTitle(deviceIdentifier);
        
        if (!registrationId.equals("")) {
        	// Already registered: disable manual setting of Device Identifier
        	
        	preferenceManualDeviceIdentifier.setSummary("Unregister first");
        	preferenceManualDeviceIdentifier.setEnabled(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        preferenceAlert1.setSummary(settings.getRingtoneSummary(getString(R.string.ringtonePref1), getString(R.string.alert1_summary)));
        preferenceAlert2.setSummary(settings.getRingtoneSummary(getString(R.string.ringtonePref2), getString(R.string.alert2_summary)));
        preferenceAlert3.setSummary(settings.getRingtoneSummary(getString(R.string.ringtonePref3), getString(R.string.alert3_summary)));
        preferenceAlert4.setSummary(settings.getRingtoneSummary(getString(R.string.ringtonePref4), getString(R.string.alert4_summary)));
        preferenceAlert5.setSummary(settings.getRingtoneSummary(getString(R.string.ringtonePref5), getString(R.string.alert5_summary)));
    }

    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	String deviceId = settings.getDeviceIdentifier();
    	String manualDeviceId = settings.getManualDeviceIdentifier().toLowerCase();
    	
    	if (!deviceId.equals(manualDeviceId)) {
    		settings.setDeviceIdentifier(manualDeviceId);
    	}
    }
}
