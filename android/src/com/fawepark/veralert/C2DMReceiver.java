package com.fawepark.veralert;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class C2DMReceiver extends BroadcastReceiver {
    private static final String TAG = "Veralert C2DM";
    public static final String EXTRAS_MSG = "msg";
    public static final String EXTRAS_TYPE = "type";
    public static final String EXTRAS_TONE = "tone";
    public static final String EXTRAS_FROM = "from";
    public static final String EXTRAS_TIME = "time";
    public static final String EXTRAS_COLLAPSE_KEY = "collapse_key";
    public static final String EXTRAS_PRIORITY = "priority";
    private static final String TYPE_ALERT = "alert";

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		Log.w("C2DM", "Registration Receiver called");
		
		if (action.equals("com.google.android.c2dm.intent.REGISTRATION")) {
			HandleRegistration(context, intent);
		}
		
		if (action.equals("com.google.android.c2dm.intent.RECEIVE")) {
			HandleMessage(context, intent);
		}
	}
	
	private void HandleRegistration(Context context, Intent intent)
	{
		
		Log.w("C2DM", "Received registration ID");
		String registrationId = intent.getStringExtra("registration_id");
		String unregistered = intent.getStringExtra("unregistered");
		String error = intent.getStringExtra("error");
		
        Map<String,String> dict = new HashMap<String,String>();

		if (error == null) {
			if (unregistered != null) {
				// Unregistration received
				if (unregistered.equals(context.getPackageName())) {
					// Unregistration successful
			        dict.put("id_a", Preferences.getIDString(context));

			        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
			        SharedPreferences.Editor editor = sp.edit();
			         
			        editor.putString("RegID", "").commit();
			    
			        FireAndForgetUrl(context.getResources().getStringArray(R.array.alert_server)[0] + "player/c2dm/unregister", dict);
				} else {
					// Unexpected
				}
			}
			else {
				// Registration successful
		        dict.put("id_a", Preferences.getIDString(context));
		        dict.put("google_cloud_id", registrationId);
		        
		        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		        SharedPreferences.Editor editor = sp.edit();
		         
		        editor.putString("RegID", registrationId).commit();

		        FireAndForgetUrl(context.getResources().getStringArray(R.array.alert_server)[0] + "player/c2dm/register", dict);
			}

		}
		else {	
			if (error.equals("ACCOUNT_MISSING")) {
	            Toast.makeText(context, "Registration failed: Please add a Google account", Toast.LENGTH_LONG).show();
			}
			else {
	            Toast.makeText(context, "Registration failed", Toast.LENGTH_LONG).show();
			}
		}
		
		Log.d("C2DM", "dmControl: registrationId = " + registrationId + ", error = " + error);
	}
	
	private void HandleMessage(Context context, Intent intent) {
		Log.w("C2DM", "Received message");
        String type = intent.getExtras().getString(EXTRAS_TYPE);
        String msg = intent.getExtras().getString(EXTRAS_MSG);
        String tone = "";
        long when = System.currentTimeMillis();

        Log.i(TAG,"TimeStamp : "+ when);
        Log.d(TAG,"Message type is " + type);
        
        Bundle extras = intent.getExtras();
        
        if (extras.containsKey(EXTRAS_TONE))
        {
        	tone = intent.getExtras().getString(EXTRAS_TONE);
        }
        else
        {
            final String match_string = "AlertTone";
            
            try {
            	int index = msg.indexOf(match_string);
            	
                if (index > -1) {
                    tone = msg.substring(index + match_string.length(), index + match_string.length() + 1);
                    msg = msg.replace(match_string + tone + " ", "");
                }
            }
            
            catch (StringIndexOutOfBoundsException sioobe) {
                Log.e(TAG, sioobe.toString());
                // Keep calm and carry on...
            }
        }
        
        // Store the message
        
        DBSource src = new DBSource(context);
        long notificationId = 0;
        
        src.open(true);
        
        notificationId = src.AddNotification(when);
    	src.AddKeyValuePair(notificationId, EXTRAS_MSG, msg);
    	src.AddKeyValuePair(notificationId, EXTRAS_TONE, tone);
        
        
        Set<String> keySet = extras.keySet();
        Iterator<String> iterator = keySet.iterator();
        
        if (keySet.contains(EXTRAS_TIME)) {
        	String timeStamp = extras.getString(EXTRAS_TIME);
        	
        	try {
        		when = Long.parseLong(timeStamp);
        	}
            
            catch (NumberFormatException e) {
            	// Value for timestamp could not be converted to a number
            	// 'when' is already time of message reception
            }
        }
        
        while (iterator.hasNext())
        {
        	String key = iterator.next();
  
        	if (  !key.equals(EXTRAS_MSG) 
        		& !key.equals(EXTRAS_TONE)
         		& !key.equals(EXTRAS_FROM)
        		& !key.equals(EXTRAS_COLLAPSE_KEY)
        		& !key.equals(EXTRAS_PRIORITY)
        		) {
	        	String value = extras.getString(key);
	        	src.AddKeyValuePair(notificationId, key, value);
        	}
        }
        src.close();
        
        // Set the alert
		if (type.equals(TYPE_ALERT)) {
            AlertIndicator(context, msg, tone, when);
        }   
		
	}
	
    private void FireAndForgetUrl(String url_string, Map<String, String> dict) {
        HttpURLConnection httpConnection = null;
        
        try {
            String post_string = url_string + "?";
            
            boolean first = true;
            
            for (String k  : dict.keySet()) {
                if (!first) {
                    post_string = post_string + "&";
                } 
                else {
                    first = false;
                }
                
                post_string = post_string + k + "=" + URLEncoder.encode(dict.get(k));
            }
            
            Log.d(TAG, post_string);
            
            URL post_url = new URL(post_string);
            
            httpConnection = (HttpURLConnection) post_url.openConnection();
            
            int response_code = httpConnection.getResponseCode();
                        
            if (response_code == HttpURLConnection.HTTP_OK) {
               	Log.v(TAG, "Logging scan " + post_string + " succeeded."); 
            } 
            else {
               	Log.v(TAG, "Logging scan " + post_string + " failed. Response: " + response_code); 
            }
            
        } 
        
        catch (MalformedURLException ex) {                    
            Log.e(TAG, "FireAndForget " + ex.toString());
        } 
        
        catch (IOException ex) {
            Log.e(TAG, "FireAndForget " + ex.toString());
        } 
        
        finally {
            if (httpConnection != null) {
                httpConnection.disconnect();
            }
        }
    }
    
    private void AlertIndicator(Context context, String msg, String tone, long when) {
        Log.d(TAG,"message text is " + msg);
         
        String human_msg = msg;

        // Get the static global NotificationManager object.
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager)context.getSystemService(ns);
                
        int icon = R.drawable.icon;
        CharSequence tickerText = human_msg;
         
        Intent notificationIntent = new Intent(context, Notifications.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

        Notification n = new Notification(icon, tickerText, when);

        String rt = Preferences.getAlertTone(context, tone);
        
        if (rt != "") {
            n.sound = Uri.parse(rt);
        }
        
        n.defaults |= Notification.DEFAULT_VIBRATE;

        n.ledARGB = 0xffff0000;
        n.ledOnMS = 300;
        n.ledOffMS = 300;
        n.flags |= Notification.FLAG_SHOW_LIGHTS;
         
        n.setLatestEventInfo(context, "Vera Alert", human_msg, contentIntent);
         
        // We use a string ID because it's a unique number
        // We also use it to cancel the notification in the notification class
        mNotificationManager.notify(R.string.app_name, n);
    }
}
