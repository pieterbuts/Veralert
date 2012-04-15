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

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;


public class C2DMIntentHandler extends IntentService {
	private static final String TAG = "Veralert C2DM";
    private static final String TYPE_ALERT = "alert";
    private static final String ALERT_SERVER = "http://vera-alert.appspot.com";
    private static final String ALERT_SERVER_ACTION_REGISTER = "player/c2dm/register";
    private static final String ALERT_SERVER_ACTION_UNREGISTER = "player/c2dm/unregister";
    private static final String WAKELOCK_KEY = "VeraAlert";
    private static final String CLASS = ".C2DMIntentHandler";
    private static final String DICT_ID_A = "id_a";
    private static final String DICT_GOOGLE_CLOUD_ID = "google_cloud_id";
    private static final String ACCOUNT_MISSING = "ACCOUNT_MISSING";
    private static PowerManager.WakeLock wakeLock;
    
    public static final String ACTION_REGISTRATION = "com.google.android.c2dm.intent.REGISTRATION";
    public static final String ACTION_RECEIVE = "com.google.android.c2dm.intent.RECEIVE";
    public static final String EXTRAS_MSG = "msg";
    public static final String EXTRAS_TYPE = "type";
    public static final String EXTRAS_TONE = "tone";
    public static final String EXTRAS_FROM = "from";
    public static final String EXTRAS_TIME = "time";
    public static final String EXTRAS_COLLAPSE_KEY = "collapse_key";
    public static final String EXTRAS_PRIORITY = "priority";
    public static final String ALERT_TONE = "AlertTone";
    
    public C2DMIntentHandler() {
		super(null);
	}
    
    public C2DMIntentHandler(String name) {
		super(name);
	}
    
	@Override
	protected void onHandleIntent(Intent intent) {
		try {
			String action = intent.getAction();
			Log.w(TAG, "C2DM Receiver called");
			
			if (action.equals(ACTION_REGISTRATION)) {
				HandleRegistration(intent);
			}
			
			if (action.equals(ACTION_RECEIVE)) {
				HandleMessage(intent);
			}
		} finally {
			wakeLock.release();
		}
	}

	static void runIntentInService(Context context, Intent intent) {
		if (wakeLock == null) {
			PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_KEY);
        }
		
        wakeLock.acquire();

        String receiver = context.getPackageName() + CLASS;
        intent.setClassName(context, receiver);

        context.startService(intent);
    }

	private void HandleRegistration(Intent intent)
	{
		Log.w(TAG, "Received intent");
		String registrationId = intent.getStringExtra("registration_id");
		String unregistered = intent.getStringExtra("unregistered");
		String error = intent.getStringExtra("error");
		Context context = getApplicationContext();
		boolean result = false;
		String msg = "";
		String serverResult = "";
		
		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(C2DMController.ACTION_RESULT);
		broadcastIntent.putExtra(C2DMController.EXTRA_ACTION, intent.getAction());
		
        Map<String,String> dict = new HashMap<String,String>();

		if (error == null) {
			if (unregistered != null) {
				// Unregistration received
				
				if (unregistered.equals(context.getPackageName())) {
					// Unregistration successful
					
			        dict.put(DICT_ID_A, Preferences.getIDString(context));
			        
					broadcastIntent.putExtra(C2DMController.EXTRA_REGID, "");
			    
			        serverResult = FireAndForgetUrl(ALERT_SERVER + ALERT_SERVER_ACTION_UNREGISTER, dict);
			        
			        if (serverResult.equals("")) {
			        	result = true;
			        } else {
			        	msg = serverResult;
			        }
				} else {
		        	msg = context.getString(R.string.c2dm_error_mssgng_unexpected_response);
				}
			}
			else {
				// Registration successful
				
		        dict.put(DICT_ID_A, Preferences.getIDString(context));
		        dict.put(DICT_GOOGLE_CLOUD_ID, registrationId);

				broadcastIntent.putExtra(C2DMController.EXTRA_REGID, registrationId);

				FireAndForgetUrl(ALERT_SERVER + ALERT_SERVER_ACTION_REGISTER, dict);
			}

		}
		else {	
			if (error.equals(ACCOUNT_MISSING)) {
	            msg =   context.getString(R.string.c2dm_error_mssgng_general_msg) 
	            	  + context.getString(R.string.c2dm_error_mssgng_account_missing_msg);
			}
			else {
		        msg = context.getString(R.string.c2dm_error_mssgng_general_msg) + error;
			}
		}
		
		broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
		broadcastIntent.putExtra(C2DMController.EXTRA_SUCCESS, result);
		broadcastIntent.putExtra(C2DMController.EXTRA_MSG, msg);
		sendBroadcast(broadcastIntent);		
		
		Log.d(TAG, "result = " + result + ", msg = " + msg + ", registrationId = " + registrationId);
	}
	
	private void HandleMessage(Intent intent) {
		Log.w(TAG, "Received message");
        String type = intent.getExtras().getString(EXTRAS_TYPE);
        String msg = intent.getExtras().getString(EXTRAS_MSG);
        String tone = "";
        long when = System.currentTimeMillis();
		Context context = getApplicationContext();

        Log.i(TAG,"TimeStamp : "+ when);
        Log.d(TAG,"Message type is " + type);
        
        Bundle extras = intent.getExtras();
        
        if (extras.containsKey(EXTRAS_TONE))
        {
        	tone = intent.getExtras().getString(EXTRAS_TONE);
        }
        else
        {
            final String match_string = ALERT_TONE;
            
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
	
    private String FireAndForgetUrl(String url_string, Map<String, String> dict) {
        HttpURLConnection httpConnection = null;
		Context context = getApplicationContext();
        String msg = context.getString(R.string.c2dm_error_appl_general_msg);
        
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
               	msg = ".";
            } 
            else {
               	Log.v(TAG, "Logging scan " + post_string + " failed. Response: " + response_code); 
               	msg += context.getString(R.string.c2dm_error_appl_failure_response_msg);
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
        
        return msg;
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
        
        if (Preferences.getVibration(context)) {
        	n.defaults |= Notification.DEFAULT_VIBRATE;
        }
        
        n.ledARGB = 0xffff0000;
        n.ledOnMS = 300;
        n.ledOffMS = 300;
        n.flags |= Notification.FLAG_SHOW_LIGHTS;
         
        n.setLatestEventInfo(context, context.getString(R.string.app_notifications), human_msg, contentIntent);
         
        // We use a string ID because it's a unique number
        // We also use it to cancel the notification in the notification class
        mNotificationManager.notify(R.string.app_name, n);
    }
}
