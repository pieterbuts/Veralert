package com.fawepark.veralert;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper  extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "VeraAlerts.db";
	private static final int DATABASE_VERSION = 2;
	private static final int msgKeyId = 1;
	private static final int typeKeyId = 2;
	private static final int alertToneKeyId = 3;

	public static final String TABLE_NOTIFICATIONS_ORG = "Notifications";
	public static final String TABLE_NOTIFICATIONS = "Notifications2";
	public static final String TABLE_KEYS = "Keys";
	public static final String TABLE_KEYVALUEPAIRS = "KeyValuePairs";
	public static final String COLUMN_ID = "ID";
	public static final String COLUMN_MESSAGE = "Message";
	public static final String COLUMN_ALERTTYPE = "AlertType";
	public static final String COLUMN_TIMESTAMP = "TimeStamp";
	public static final String COLUMN_KEY = "Key";
	public static final String COLUMN_VALUE = "Value";
	public static final String COLUMN_NOTIFICATION_REF = "NotificationReference";
	public static final String COLUMN_KEY_REF = "KeyReference";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        final String TABLE_NOTIFICATIONS_CREATE = 
                "create table " + TABLE_NOTIFICATIONS + " (" +
                COLUMN_ID + " integer primary key autoincrement, " +
                COLUMN_TIMESTAMP + " integer" +
                ");";
        database.execSQL(TABLE_NOTIFICATIONS_CREATE);
        
        final String TABLE_KEYS_CREATE = 
                "create table " + TABLE_KEYS + " (" +
                COLUMN_ID + " integer primary key autoincrement, " +
           		COLUMN_KEY + " text not null unique collate nocase" +
                ");";
        
        database.execSQL(TABLE_KEYS_CREATE);
        
        final String TABLE_KEYVALUEPAIRS_CREATE = 
                "create table " + TABLE_KEYVALUEPAIRS + " (" +
                COLUMN_ID + " integer primary key autoincrement, " +
       		    COLUMN_VALUE + " text not null, " +
        		COLUMN_NOTIFICATION_REF + " integer not null, " +
        		COLUMN_KEY_REF + " integer not null" +
                ");";
        database.execSQL(TABLE_KEYVALUEPAIRS_CREATE);
        
    	ContentValues initialValues = new ContentValues();
        initialValues.put(COLUMN_ID, msgKeyId);
        initialValues.put(COLUMN_KEY, C2DMIntentHandler.EXTRAS_MSG);
        database.insert(TABLE_KEYS, null, initialValues);
    	
    	initialValues = new ContentValues();
        initialValues.put(COLUMN_ID, typeKeyId);
        initialValues.put(COLUMN_KEY, C2DMIntentHandler.EXTRAS_TYPE);
        database.insert(TABLE_KEYS, null, initialValues);
    	
    	initialValues = new ContentValues();
        initialValues.put(COLUMN_ID, alertToneKeyId);
        initialValues.put(COLUMN_KEY, C2DMIntentHandler.EXTRAS_TONE);
        database.insert(TABLE_KEYS, null, initialValues);
    }
        
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if ((oldVersion == 1) && (newVersion == 2)) {
        	Log.w(DBHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to " + newVersion);
        	
        	onCreate(db);
        	
            Cursor crsr = db.query(false, 
            		               TABLE_NOTIFICATIONS_ORG, 
            		               new String[] {COLUMN_MESSAGE, COLUMN_ALERTTYPE, COLUMN_TIMESTAMP},
            		               null,
            				       null,
            				       null,
            				       null,
            				       null,
            				       null);

            if (crsr != null) 
            {
                if (crsr.moveToFirst())
            	{
    	        	do 	
    	        	{
    	        		/* Insert notification */
    	        		ContentValues initialValues = new ContentValues();
    	                initialValues.put(COLUMN_TIMESTAMP, crsr.getLong(2));
    	                long notifId = db.insert(TABLE_NOTIFICATIONS, null, initialValues);
    	        		
    	                /* Insert key value pairs */
    	                initialValues = new ContentValues();
     	                initialValues.put(COLUMN_NOTIFICATION_REF, notifId);
    	                initialValues.put(COLUMN_KEY_REF, msgKeyId);
    	                initialValues.put(COLUMN_VALUE, crsr.getString(0));
    	                db.insert(TABLE_KEYVALUEPAIRS, null, initialValues);
    	                
    	                initialValues = new ContentValues();
     	                initialValues.put(COLUMN_NOTIFICATION_REF, notifId);
    	                initialValues.put(COLUMN_KEY_REF, typeKeyId);
    	                initialValues.put(COLUMN_VALUE, "alert");
    	                db.insert(TABLE_KEYVALUEPAIRS, null, initialValues);
    	                
    	                initialValues = new ContentValues();
     	                initialValues.put(COLUMN_NOTIFICATION_REF, notifId);
    	                initialValues.put(COLUMN_KEY_REF, alertToneKeyId);
    	                initialValues.put(COLUMN_VALUE, crsr.getInt(1));
    	                db.insert(TABLE_KEYVALUEPAIRS, null, initialValues);
    	        	} 
    	        	while (crsr.moveToNext());
    	        	
    	            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTIFICATIONS_ORG);
            	}
                
                crsr.close();
            }
        } else {
        	Log.w(DBHelper.class.getName(),
                    "Upgrading database from version " + oldVersion + " to " + newVersion);
        	Log.w(DBHelper.class.getName(),
                    "No conversion possible");
            	
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTIFICATIONS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_KEYS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_KEYVALUEPAIRS);
            
            onCreate(db);
        }
    }
}
