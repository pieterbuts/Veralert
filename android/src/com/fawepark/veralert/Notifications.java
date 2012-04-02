package com.fawepark.veralert;

import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;



public class Notifications extends ListActivity {
    public static final String TAG = "Veralert";
    private static final String Preferences = "Settings";
    private static final String DeleteNotifications = "Delete";
    private static final String Register = "Register";
    private static final String Unregister = "Unregister";
    private static final int PROPERTIES = 300;
    private static final int ORDER_BY_MESSAGE = 100;
    private static final int ORDER_BY_DATE = 101;
    private static final int ORDER_BY_ALERT_TYPE = 102;
    private static final int SELECT_BY_MESSAGE = 200;
    private static final int SELECT_BY_DATE = 201;
    private static final int SELECT_BY_ALERT_TYPE = 202;
    
    private View EditOptions;
    private DBViewAdapter dbAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.notifications);
        SharedPreferences sp = getDefault(this);
       
        if (! sp.contains("ID") ) {
            Intent intent = new Intent(this, Preferences.class);
            startActivity(intent);
            finish();
        }

        CancelNotification();

        // Use the SimpleCursorAdapter to show the
        // elements in a ListView
        dbAdapter = DBViewAdapter.GetAdapter(this);
        dbAdapter.SetEdit(false);
        setListAdapter(dbAdapter);
        EditOptions = findViewById(R.id.deleteoptions);
        registerForContextMenu(getListView());
    }
    
    
    @Override    
    public void onResume() {
        super.onResume();
        CancelNotification();
        dbAdapter.Refresh();
        dbAdapter.notifyDataSetChanged();
    }   
 
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
    	
  		menu.add(0, 1, 0, Register);
    	menu.add(0, 2, 0, Preferences);
    	menu.add(0, 3, 0, DeleteNotifications);
      
    	return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	super.onPrepareOptionsMenu(menu);
    	
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String regId = sp.getString("RegID", "");
    	
        if (regId.equals("")) {
        	MenuItem item = menu.findItem(1);
        	
        	item.setTitle(Register);
    	} else {
        	MenuItem item = menu.findItem(1);
        	
        	item.setTitle(Unregister);
    	}
    	
    	return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String i = item.toString();
        Log.v(TAG,"In menu select " + i);
      
        if (i.equals(Preferences)) {
            Intent intent = new Intent(this, Preferences.class);
            startActivity(intent);
        }
        
        if (i.equals(DeleteNotifications)) {
            dbAdapter.SetEdit(true);
            dbAdapter.notifyDataSetChanged();
            EditOptions.setVisibility(View.VISIBLE);
        }
        
        if (i.equals(Register)) {
            PendingIntent app = PendingIntent.getBroadcast(this, 0, new Intent(), 0);
            Log.i(Notifications.TAG,"Attempting c2dm registration of "+ app + " for sender " + this.getResources().getStringArray(R.array.c2dm_name)[0]);
            Intent registrationIntent = new Intent("com.google.android.c2dm.intent.REGISTER");
            registrationIntent.putExtra("app", app);
            registrationIntent.putExtra("sender", this.getResources().getStringArray(R.array.c2dm_name)[0]);
            startService(registrationIntent);
            Log.i(Notifications.TAG,"c2dm registration intent started");             
        	
        }
        
        if (i.equals(Unregister)) {
            PendingIntent app = PendingIntent.getBroadcast(this, 0, new Intent(), 0);
            Log.i(Notifications.TAG,"Attempting c2dm unregistration of "+ app);
            Intent unregistrationIntent = new Intent("com.google.android.c2dm.intent.UNREGISTER");
            unregistrationIntent.putExtra("app", app);
            startService(unregistrationIntent);
            Log.i(Notifications.TAG,"c2dm unregistration intent started");             
        }

        return true;
    }
    
    public void DeleteTuples(View view) {
        dbAdapter.DeleteSelected();
        CancelDeleteTuples(view);
    }
    
    public void CancelDeleteTuples(View view) {    
        dbAdapter.SetEdit(false);
        EditOptions.setVisibility(View.GONE);
        DeSelectAll(view);
    }
    
    public void SelectAll(View view) {
        List<DBTuple> vals = dbAdapter.GetValues();
        
        for (int i = 0; i < vals.size(); i++) {
            DBTuple t = vals.get(i);
            t.Selected = true;
        }
        
        dbAdapter.notifyDataSetChanged();
    }
    
    public void DeSelectAll(View view) {
        List<DBTuple> vals = dbAdapter.GetValues();
        
        for (int i = 0; i < vals.size(); i++) {
            DBTuple t = vals.get(i);
            t.Selected = false;
        }
        
        dbAdapter.notifyDataSetChanged();
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        if (v == (View) getListView()) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            DBTuple t = dbAdapter.Get(info.position);
            String ts = t.TimeString(this);
            
            menu.setHeaderTitle(t.alertType + " " + t.message + "\n" + ts);
            menu.add(Menu.NONE, PROPERTIES, 0, "Properties");
            menu.add(Menu.NONE, ORDER_BY_MESSAGE, 0, "Order by Message");
            menu.add(Menu.NONE, ORDER_BY_DATE, 1, "Order by Date");
            menu.add(Menu.NONE, ORDER_BY_ALERT_TYPE, 1, "Order by Alert Type");
            
            if (dbAdapter.EditState) {
                menu.add(Menu.NONE, SELECT_BY_MESSAGE, 2, "Select all with this message");
                menu.add(Menu.NONE, SELECT_BY_DATE, 3, "Select all before this alert");
                menu.add(Menu.NONE, SELECT_BY_ALERT_TYPE, 3, "Select all with this alert type");
            }
        }
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        boolean Result = false;
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        DBTuple t = dbAdapter.Get(info.position);
        List<DBTuple> vals = dbAdapter.GetValues();
        switch (item.getItemId()) {
          case SELECT_BY_MESSAGE:
            for (int i = 0; i < vals.size(); i++) {
                DBTuple t2 = vals.get(i);
                
                if (t.message.equals(t2.message)) {
                    t2.Selected = true;
                }
            }
            
            Result = true;
            break;
            
          case SELECT_BY_DATE:
            for (int i = 0; i < vals.size(); i++) {
                DBTuple t2 = vals.get(i);
                
                if (t2.timeStamp < t.timeStamp) {
                    t2.Selected = true;
                }
            }
            
            Result = true;
            break;
            
          case SELECT_BY_ALERT_TYPE:
              for (int i = 0; i < vals.size(); i++) {
                  DBTuple t2 = vals.get(i);
                  
                  if (t2.alertType < t.alertType) {
                      t2.Selected = true;
                  }
              }
              
              Result = true;
              break;
              
          case ORDER_BY_MESSAGE:
            dbAdapter.SetSortOrder(DBViewAdapter.BY_MESSAGE);
            Result = true;
            break;
            
          case ORDER_BY_DATE:
            dbAdapter.SetSortOrder(DBViewAdapter.BY_DATE);
            Result = true;
            break;
            
          case ORDER_BY_ALERT_TYPE:
              dbAdapter.SetSortOrder(DBViewAdapter.BY_ALERT_TYPE);
              Result = true;
              break;

          case PROPERTIES:
        	  ShowAlertProperties(t.id);
              Result = true;
              break;
        }
        
        if (Result) {
            dbAdapter.notifyDataSetChanged();
        }
        
        return Result;
    }

    
    private void ShowAlertProperties(int alertId) {
    	AlertDialog properties = new AlertDialog.Builder(this).create();
        DBSource src = new DBSource(this);
        
        properties.setTitle("Alert Properties");
        
        src.open(false);
    	properties.setMessage(src.AlertPropertiesText(alertId));
        src.close();
    	
    	properties.setButton(DialogInterface.BUTTON_POSITIVE, "Ok", new OnClickListener() {
	    	@Override
	    	public void onClick(DialogInterface dialog, int which) {               
	    	//...
	    	}
    	});

    	properties.show();
    }
    
    private void CancelNotification() {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);

        // Cancel the persistent notification.
        // We usr a string ID because it's a unique number
        // This is the ID the notification was created with in the C2DMReceiver Class
        mNotificationManager.cancel(R.string.app_name);
    }


    protected static SharedPreferences getDefault(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx);
    }

}
