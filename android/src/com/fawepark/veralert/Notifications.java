package com.fawepark.veralert;

import java.util.List;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;


public class Notifications extends ListActivity {
    public static final String TAG = "Veralert.Notifications";
    private View EditOptions;
    private DBViewAdapter dbAdapter;
    private C2DMController c2dmController;

    private static final int PROPERTIES = 300;
    private static final int ORDER_BY_MESSAGE = 100;
    private static final int ORDER_BY_DATE = 101;
    private static final int ORDER_BY_ALERT_TYPE = 102;
    private static final int SELECT_BY_MESSAGE = 200;
    private static final int SELECT_BY_DATE = 201;
    private static final int SELECT_BY_ALERT_TYPE = 202;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.notifications);
        SharedPreferences sp = getDefault(this);
        c2dmController = new C2DMController(this);
        c2dmController.start();
        
        if (! sp.contains("ID") ) {
            Intent intent = new Intent(this, Preferences.class);
            startActivity(intent);

            Toast.makeText(this, "Please register from the menu", Toast.LENGTH_LONG).show();
        }

        Log.i(Notifications.TAG,"Notifications.onCreate"); 
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
        Log.i(Notifications.TAG,"Notifications.onResume"); 
        CancelNotification();
        dbAdapter.Refresh();
        dbAdapter.notifyDataSetChanged();
    }   

    @Override    
    public void onNewIntent(Intent intent) {
    	super.onNewIntent(intent);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
    	
  		menu.add(0, 1, 0, getString(R.string.register));
    	menu.add(0, 2, 0, getString(R.string.preferences));
    	menu.add(0, 3, 0, getString(R.string.delete_notifications));
      
    	return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	super.onPrepareOptionsMenu(menu);
    	
        if (c2dmController.isRegistered()) {
        	MenuItem item = menu.findItem(1);
        	
        	item.setTitle(getString(R.string.unregister));
    	} else {
        	MenuItem item = menu.findItem(1);
        	
        	item.setTitle(getString(R.string.register));
    	}
    	
    	return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        c2dmController.stop();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String i = item.toString();
        Log.v(TAG,"In menu select " + i);
      
        if (i.equals(getString(R.string.preferences))) {
            Intent intent = new Intent(this, Preferences.class);
            startActivity(intent);
        }
        
        if (i.equals(getString(R.string.delete_notifications))) {
            dbAdapter.SetEdit(true);
            dbAdapter.notifyDataSetChanged();
            EditOptions.setVisibility(View.VISIBLE);
        }
        
        if (i.equals(getString(R.string.register))) {
        	c2dmController.register();
        }
        
        if (i.equals(getString(R.string.unregister))) {
        	c2dmController.unregister();
        }

        return true;
    }
    
    protected void onActivityResult(int RequestCode, int resultCode, Intent data) {
    	
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
            menu.add(Menu.NONE, PROPERTIES, 0, getString(R.string.properties));
            menu.add(Menu.NONE, ORDER_BY_MESSAGE, 0, getString(R.string.order_by_message));
            menu.add(Menu.NONE, ORDER_BY_DATE, 1, getString(R.string.order_by_date));
            menu.add(Menu.NONE, ORDER_BY_ALERT_TYPE, 1, getString(R.string.order_by_alert_type));
            
            if (dbAdapter.EditState) {
                menu.add(Menu.NONE, SELECT_BY_MESSAGE, 2, getString(R.string.select_by_message));
                menu.add(Menu.NONE, SELECT_BY_DATE, 3, getString(R.string.select_by_date));
                menu.add(Menu.NONE, SELECT_BY_ALERT_TYPE, 3, getString(R.string.select_by_alert_type));
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
        Dialog listDialog = new Dialog(this);

        listDialog.setTitle(getString(R.string.alert_properties));
        LayoutInflater li = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = li.inflate(R.layout.properties, null, false);
        listDialog.setContentView(v);
        listDialog.setCancelable(true);

        ListView list1 = (ListView) listDialog.findViewById(android.R.id.list);
        PropertiesViewAdapter adapter = PropertiesViewAdapter.GetAdapter(this, alertId);
        list1.setAdapter(adapter);

        listDialog.show();
    }
    
    private void CancelNotification() {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);

        // Cancel the persistent notification.
        // We use a string ID because it's a unique number
        // This is the ID the notification was created with in the C2DMReceiver Class
        mNotificationManager.cancel(R.string.app_name);
    }

    protected static SharedPreferences getDefault(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx);
    }
}
