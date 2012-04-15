package com.fawepark.veralert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class DBSource {
    private SQLiteDatabase database;
    private DBHelper dbHelper;
    private Context context;
    private String[] columnsNotification = {DBHelper.COLUMN_ID, DBHelper.COLUMN_TIMESTAMP};
    private String[] columnsKeyValuePairs = {DBHelper.COLUMN_ID, DBHelper.COLUMN_VALUE};

    public DBSource(Context context) {
        this.context = context;
        dbHelper = new DBHelper(context);
    }
    
    public void open(boolean ForEdit) throws SQLException {
        if (ForEdit) {
            database = dbHelper.getWritableDatabase();
        } else {
            database = dbHelper.getReadableDatabase();
        }
    }

    public void close() {
        dbHelper.close();
    }

    public long AddNotification(long timestamp) {
        ContentValues v = new ContentValues();
        v.put(DBHelper.COLUMN_TIMESTAMP, timestamp);
        return database.insert(DBHelper.TABLE_NOTIFICATIONS, null, v);
    }
    
    public long AddKeyValuePair(long notificationId, String key, String value) {
        ContentValues v = new ContentValues();

        long keyId = GetKeyId(key);
    	
    	if (keyId < 0)
    	{
    		keyId = AddKey(key);
    	}
    	
    	v.put(DBHelper.COLUMN_NOTIFICATION_REF, notificationId);
        v.put(DBHelper.COLUMN_KEY_REF, keyId);
        v.put(DBHelper.COLUMN_VALUE, value);
        return database.insert(DBHelper.TABLE_KEYVALUEPAIRS, null, v);
    }
    
    private long GetKeyId(String key) {
    	long keyId = -1;
    	Cursor crsr = database.query(true,
    					     		 DBHelper.TABLE_KEYS, 
    			                     new String[] {DBHelper.COLUMN_ID, DBHelper.COLUMN_KEY}, 
    			                     DBHelper.COLUMN_KEY + "=\"" + key + "\"",
     			    	             null,
					    	         null,
					    	         null,
					    	         null, 
					    	         null);
    
        if (crsr != null) 
        {
        	if (crsr.moveToFirst())
        	{
        		if (crsr.getInt(1) >= 0)
	        	{
        			keyId = crsr.getLong(0);        			
	        	} 
        	}
        	
        	crsr.close();
        }
    	
        return keyId;
    }
    
    private long AddKey(String key) {
        ContentValues v = new ContentValues();
        v.put(DBHelper.COLUMN_KEY, key);
        
        long id = database.insert(DBHelper.TABLE_KEYS, null, v);
        
        return id;
    }
    
    public List<DBTuple> getAllData() {
        List<DBTuple> Results = new ArrayList<DBTuple>();
        
        Cursor crsr = database.query(DBHelper.TABLE_NOTIFICATIONS, columnsNotification, null, null, null, null, null);
        
        if (crsr != null) {
        	AddAlertData(Results, crsr);
        	crsr.close();
        }
        
        return(Results);
    }

    private void AddAlertData(List<DBTuple> Results, Cursor cur) {
        cur.moveToFirst();

        while (!cur.isAfterLast()) {
            DBTuple tuple = new DBTuple();
            tuple.id = cur.getInt(0);
            tuple.timeStamp = cur.getLong(1);

            tuple.message = GetValue(tuple.id, 1);
          	tuple.alertType = GetAlertTone(tuple.id);
          	tuple.veraId = 0;
          	
            Results.add(tuple);
            cur.moveToNext();
        }
        
        LimitData(Results);
    }
    
    private int GetAlertTone(int notificationId) {
    	String value = GetValue(notificationId, 3);
    	int result = 0;
    	
        try {
        	result = Integer.parseInt(value);
        }
        
        catch (NumberFormatException e) {
        	// Value for alerttone could not be converted to a number
        	
        	result = 0;
        }
       
        if ((result < 1) | (result > 5)) {
        	result = 0;
        }
        
        return result;
    }
    
    private String GetValue(int notificationId, int keyId) {
        String where = DBHelper.COLUMN_NOTIFICATION_REF + " = " + notificationId + " and " + DBHelper.COLUMN_KEY_REF + " = " + keyId;
    	Cursor crsr = database.query(
        		DBHelper.TABLE_KEYVALUEPAIRS, 
        		columnsKeyValuePairs, 
        		where, 
        		null, 
        		null, 
        		null, 
        		null);
        String result = "";
        
        if (crsr != null) 
        {
        	crsr.moveToFirst();
        	
        	if (crsr.moveToFirst()) {
    			result = crsr.getString(1);        			
            }
        	
        	crsr.close();
       }
        
        return result;
    }
    
    private void LimitData(List<DBTuple> Results) {
        int Max = Preferences.getMaxRetention(context);
        
        if (Max == 0) return;
        
        // Sort A copy of the data by Message
        List<DBTuple> TmpCpy = new ArrayList<DBTuple>(Results);
        Collections.sort(TmpCpy, new Comparator<DBTuple>() {
            @Override
            public int compare(DBTuple t1, DBTuple t2) {
                int c = t1.message.compareTo(t2.message);
                
                if (c == 0) {
                    if (t1.timeStamp == t2.timeStamp) {
                    	return 0;
                    }
                    
                    if (t1.timeStamp > t2.timeStamp) { 
                        return -1;
                    } else {
                        return 1;
                    }
                }
                return c;
            }
        });
        
        // Remove if we see more than Max of any message
        
        String LastMsg = "xyzzy";
        int Cnt = 1;
        
        for (int i = 0; i < TmpCpy.size(); i++) {
            DBTuple t = TmpCpy.get(i);
            
            if (LastMsg.equals(t.message)) {
                Cnt++;
                
                if (Cnt > Max) {
                    Remove(t);
                    Results.remove(t);
                }
            } else {
                Cnt = 1;
                LastMsg = t.message;
            }
        }
    }
    
    public void Remove(DBTuple t) {
        database.delete(DBHelper.TABLE_NOTIFICATIONS,
                        DBHelper.COLUMN_ID + " = ?",
                        new String[] {Integer.toString(t.id)});
        database.delete(DBHelper.TABLE_KEYVALUEPAIRS,
                        DBHelper.COLUMN_NOTIFICATION_REF + " = ?",
                        new String[] {Integer.toString(t.id)});
    }
     
    public void GetNewData(List<DBTuple> values) {
        long max = 0;
        
        for (int i = 0; i < values.size(); i++) {
            DBTuple t = values.get(i);
            
            if (t.timeStamp > max) {
            	max = t.timeStamp;
            }
        }

        Cursor crsr = database.query(DBHelper.TABLE_NOTIFICATIONS, 
        		                     columnsNotification, 
                                     DBHelper.COLUMN_TIMESTAMP + ">?", 
                                     new String[] {Long.toString(max)}, 
                                     null, null, null);
        if (crsr != null) {
        	AddAlertData(values, crsr);
        	crsr.close();
        }    	
    }
    
    public List<AlertProperty> GetAlertProperties(int alertId) {
    	List<AlertProperty> properties = new ArrayList<AlertProperty>();
    	
        String qry = 
        		"select Keys.Key, kvp.Value"
        		+ " from Keys"
        		+ " join"
        		+ " KeyValuePairs as kvp"
        		+ " on Keys.ID = kvp.KeyReference"
         		+ " where kvp.NotificationReference = " + alertId
         		+ " order by Keys.ID";
        
        Cursor crsr = database.rawQuery(qry,	null);
        
        if (crsr != null) {
	        crsr.moveToFirst();
	
	        while (!crsr.isAfterLast()) {
	        	AlertProperty property = new AlertProperty(crsr.getString(0), crsr.getString(1));
	        	
	        	properties.add(property);

	        	crsr.moveToNext();
	        }
	        
	        crsr.close();
        }
    	
    	return properties;
    }
}
