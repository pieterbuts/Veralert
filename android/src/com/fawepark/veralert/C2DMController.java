package com.fawepark.veralert;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;


public class C2DMController extends BroadcastReceiver {
    private final String TAG = "Veralert.C2DMController";
    private final long REGISTRATION_TIMEOUT = 10000;
	private Context context;
	private RegistrationHandler registrationHandler = this.new RegistrationHandler();
	private boolean registrationResponseReceived = false;
	private boolean registrationInProgress = false;
	private boolean unregistrationInProgress = false;
	private ProgressDialog pd;
	private Messenger replyTo;
	
    private static final String C2DM_NAME = "c2dm@barcodebeasties.com";
    private static final String INTENT_REGISTER = "com.google.android.c2dm.intent.REGISTER";
    private static final String INTENT_UNREGISTER = "com.google.android.c2dm.intent.UNREGISTER";
    private static final String EXTRA_APP = "app";
    private static final String EXTRA_SENDER = "sender";

	public static final String ACTION_RESULT = "C2DMIntentHandlerResult";
	public static final String EXTRA_ACTION = "Action";
	public static final String EXTRA_SUCCESS = "Success";
	public static final String EXTRA_MSG = "Message";
	public static final String EXTRA_REGID = "RegistrationId";
	public static final int MSG_ACTION_COMPLETED = 1;
	public static final int MSG_ALERT_RECEIVED = 2;
	
	C2DMController() {
		super();
	}
	
	C2DMController(Context context, Messenger replyTo) {
		super();
		
		this.context = context;
		this.replyTo = replyTo;
		this.registrationHandler = this.new RegistrationHandler();
	}
	
	public void register() {
		registrationResponseReceived = false;
		
		if (!registrationInProgress) {
			registrationInProgress = true;
			
	        PendingIntent app = PendingIntent.getBroadcast(context, 0, new Intent(), 0);
	        Log.i(TAG,"Attempting c2dm registration of "+ app + " for sender " + C2DM_NAME);
	        
	        Intent registrationIntent = new Intent(INTENT_REGISTER);
	        registrationIntent.putExtra(EXTRA_APP, app);
	        registrationIntent.putExtra(EXTRA_SENDER, C2DM_NAME);
	        context.startService(registrationIntent);
	        
	        Log.i(TAG, "c2dm registration started");
	        
	        Message msg = Message.obtain();
	        registrationHandler.sendMessageDelayed(msg, REGISTRATION_TIMEOUT);
	        
            pd = ProgressDialog.show(context, "", context.getString(R.string.c2dm_registering));
		}
	}

	public void unregister() {
		registrationResponseReceived = false;
		
		if (!unregistrationInProgress) {
			unregistrationInProgress = true;
			
			PendingIntent app = PendingIntent.getBroadcast(context, 0, new Intent(), 0);
	        Log.i(TAG,"Attempting c2dm unregistration of "+ app);
	
	        Intent unregistrationIntent = new Intent(INTENT_UNREGISTER);
	        unregistrationIntent.putExtra(EXTRA_APP, app);
	        context.startService(unregistrationIntent);

	        Log.i(TAG,"c2dm unregistration started");             
	 
	        Message msg = Message.obtain();
	        registrationHandler.sendMessageDelayed(msg, REGISTRATION_TIMEOUT);

	        pd = ProgressDialog.show(context, "", context.getString(R.string.c2dm_unregistering));
		}
	}
	
	public boolean isRegistered()
	{
    	Settings settings = new Settings(context);
    	
        String regId = settings.getRegistrationIdentifier();
    	boolean result = false;
    	
        if (!regId.equals("")) {
        	result = true;
    	}
    	
    	return result;
	}

	public void start() {
        IntentFilter filter = new IntentFilter(ACTION_RESULT);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        context.registerReceiver(this, filter);
	}
	
	public void stop() {
		context.unregisterReceiver(this);
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getStringExtra(EXTRA_ACTION);
		boolean success = intent.getBooleanExtra(EXTRA_SUCCESS, false);
		String msg = intent.getStringExtra(EXTRA_MSG);
		String registrationId = intent.getStringExtra(EXTRA_REGID);		

	    if (action != null) {
	    	if (action.equals(C2DMIntentHandler.ACTION_REGISTRATION)) {
			    registrationResponseReceived = true;
				
				if (success)
				{
					if (unregistrationInProgress) {
				    	Settings settings = new Settings(context);
				    	settings.setRegistrationIdentifier("");
	
				    	msg = context.getString(R.string.c2dm_unregistration_ok);
					} else if (registrationInProgress) {
				    	Settings settings = new Settings(context);
				    	settings.setRegistrationIdentifier(registrationId);
	
						msg = context.getString(R.string.c2dm_registration_ok);
					} else {
						// Unexpected
						
						success = false;
						msg = context.getString(R.string.c2dm_registration_unexpected_state);
					}
				}	
				
				completeRegistration(success, msg);
		    } else if (action.equals(C2DMIntentHandler.ACTION_RECEIVE)) {
		    	try {
		    		replyTo.send(Message.obtain(null, MSG_ALERT_RECEIVED, 0, 0));
		    	} catch (RemoteException e) {
			        Log.e(TAG,"RemoteException.");
		    	}
		    	
		    }else {
		    	// Unexpected intent, just ignore
		        Log.i(TAG,"Unexpected intent received.");
		    }
	    } else {
	    	// Unexpected intent, just ignore
	        Log.i(TAG,"Unexpected intent received.");
	    }
	}
	
    private void completeRegistration(boolean success, String msg) {
		registrationInProgress = false;
		unregistrationInProgress = false;
		int iconId = android.R.drawable.ic_dialog_alert;
    	pd.dismiss();
		
		if (success) {
			iconId = android.R.drawable.ic_dialog_info;
		}
		
    	AlertDialog.Builder builder = new AlertDialog.Builder(context);
    	
    	builder.setMessage(msg)
    		   .setTitle(R.string.app_notifications)
    		   .setIcon(iconId)
    	       .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) { }
    	       });

    	AlertDialog alertDlg = builder.create();
    	alertDlg.show();
    	
    	try {
    		replyTo.send(Message.obtain(null, MSG_ACTION_COMPLETED, 0, 0));
    	} catch (RemoteException e) {
	        Log.e(TAG,"RemoteException.");
    	}
	}
	
	class RegistrationHandler extends Handler {  
	    @Override  
	    public void handleMessage(Message msg) { 
	    	// Check if (un)registration succeeded
	    	String errorMsg = context.getString(R.string.c2dm_messaging_server_no_response);
	    	
			if (registrationInProgress | unregistrationInProgress) {
				
				if (!registrationResponseReceived) {
					// No response received within reasonable time. Most likely the (un)registration intent
					// could not be sent due to network errors
					
					if (registrationInProgress) {
						errorMsg = context.getString(R.string.c2dm_registration_nok) + errorMsg;
					} else {
						errorMsg = context.getString(R.string.c2dm_unregistration_nok) + errorMsg;
					}
					
					completeRegistration(false, errorMsg);
				}
			}
	    }  
	};  
}
