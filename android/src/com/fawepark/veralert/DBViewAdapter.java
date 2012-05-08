package com.fawepark.veralert;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.Activity;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

public class DBViewAdapter extends ArrayAdapter<DBTuple> {
    static public final int BY_MESSAGE = 1;
    static public final int BY_DATE = 2;
    static public final int BY_ALERT_TYPE = 3;
    
    private List<DBTuple> values;
    private final Activity context;
    private int SortOrder;
    private DBSource dbSource;
    
    private int cols[] = {Color.BLACK, 
    					  Color.BLUE, 
    					  Color.rgb(0, 128, 0), // Darkish green 
    					  Color.rgb(128, 0, 128),  // Purple
    					  Color.rgb(200, 0, 0)}; // Darker red 

    public boolean EditState;

    static public DBViewAdapter GetAdapter(Activity context) {
        DBSource src = new DBSource(context);
        src.open(false);
        List<DBTuple> vals = src.getAllData();
        src.close();
        DBViewAdapter Result = new DBViewAdapter(context, vals);
        Result.dbSource = src;
        
        return Result;
    }

    private DBViewAdapter(Activity context, List<DBTuple> values) {
        super(context, R.layout.rowlayout, values);
        this.context = context;
        this.values = values;
        SetSortOrder(BY_DATE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        DBTuple t = values.get(position);
        View rowView;
        
        if (convertView == null) {
            rowView = inflater.inflate(R.layout.rowlayout, null);
        } else {
            rowView = convertView;
        }
        
        TextView tv = (TextView) rowView.findViewById(R.id.row_message);
        tv.setText(t.message);
        tv.setTextColor(cols[t.veraId]);
        tv = (TextView) rowView.findViewById(R.id.row_alerttype);
        tv.setText("" + t.alertType);
        tv = (TextView) rowView.findViewById(R.id.row_timestamp);
        tv.setText(t.TimeString(getContext()));
        CheckBox cb = (CheckBox) rowView.findViewById(R.id.row_check);
        cb.setVisibility(EditState ? View.VISIBLE : View.GONE);
        cb.setTag(t);
        cb.setOnCheckedChangeListener(null);
        cb.setChecked(t.Selected);
        
        cb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton mcb, boolean ischecked) {
                DBTuple tag = (DBTuple) mcb.getTag(); 
                tag.Selected = ischecked;               
            }
        });
        
        return rowView;
    }
    
    public void SetEdit(boolean EditState) {
        this.EditState = EditState;
    }
    
    public List<DBTuple> GetValues() {
        return values;
    }
    
    public DBTuple Get(int i) {
        return values.get(i);
    }
    
    public void SetSortOrder(int v) {
        SortOrder = v;
        switch (SortOrder) {
        case BY_MESSAGE:
            Collections.sort(values, new Comparator<DBTuple>() {
              @Override
              public int compare(DBTuple t1, DBTuple t2) {
                  int result = t1.message.compareTo(t2.message);
                  
                  if (result == 0) {
                	  result = CompareTime(t1, t2);
                  }
                  return result;
              }
            });
            break;
            
        case BY_DATE:
            Collections.sort(values, new Comparator<DBTuple>() {
                @Override
                public int compare(DBTuple t1, DBTuple t2) {
                	return CompareTime(t1, t2);
                }
            });
            break;
            
        case BY_ALERT_TYPE:
              Collections.sort(values, new Comparator<DBTuple>() {
                  @Override
                  public int compare(DBTuple t1, DBTuple t2) {
                	  int result = 0;
                	  
                      if (t1.alertType == t2.alertType) {
                    	  result = CompareTime(t1, t2);
                      } else if (t1.alertType > t2.alertType) { 
                          result = 1;
                      } else {
                          result = -1;
                      }
                      
                      return result;
                  }
              });
              break;
        }
    }
    
    private int CompareTime(DBTuple t1, DBTuple t2) {
    	int result = 0;
    	
        if (t1.timeStamp != t2.timeStamp) {
	        if (t1.timeStamp > t2.timeStamp) { 
	        	result = -1;
	        } else {
	        	result = 1;
	        }
        }
        
        return result;
    }
    
    public void DeleteSelected() {
        dbSource.open(true);
        for (int i = 0; i < values.size(); i++) {
            DBTuple t = values.get(i);
            
            if (t.Selected) {
                dbSource.Remove(t);
                this.remove(t);
                i--;
            }
        }
        dbSource.close();
    }
    
    public void Refresh() {
        dbSource.open(false);
        dbSource.GetNewData(values);
        dbSource.close();
    }
}
