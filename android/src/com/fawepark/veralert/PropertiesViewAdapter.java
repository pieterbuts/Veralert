package com.fawepark.veralert;

import java.util.List;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class PropertiesViewAdapter extends ArrayAdapter<AlertProperty> {
	private List<AlertProperty> properties;
    private final Activity context;
    
    static public PropertiesViewAdapter GetAdapter(Activity context, int alertId) {
        DBSource src = new DBSource(context);
        List<AlertProperty> alertProperties;
        
        src.open(false);
        alertProperties = src.GetAlertProperties(alertId);
        src.close();
        
        PropertiesViewAdapter result = new PropertiesViewAdapter(context, alertProperties);
        
        return result;
    }

    private PropertiesViewAdapter(Activity context, List<AlertProperty> properties) {
        super(context, R.layout.propertyrow, properties);
        this.context = context;
        this.properties = properties;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        AlertProperty property = properties.get(position);
        View rowView;
        
        if (convertView == null) {
            rowView = inflater.inflate(R.layout.propertyrow, null);
        } else {
            rowView = convertView;
        }
        
        TextView tv = (TextView) rowView.findViewById(R.id.row_key);
        tv.setText(property.key + " =");
        tv = (TextView) rowView.findViewById(R.id.row_value);
        tv.setText(property.value);
        
        return rowView;
    }
}
