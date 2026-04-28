package com.monitor.health.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.monitor.health.R;
import com.monitor.health.model.Device;

import java.util.List;

public class DeviceAdapter extends ArrayAdapter<Device> {
    private List<Device> dataList;
    private Context mContext;

    public DeviceAdapter(Context context, List<Device> data) {
        super(context, R.layout.list_item_layout, data);
        this.mContext = context;
        this.dataList = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            view = inflater.inflate(R.layout.list_item_layout, null);
        }

        Device device = dataList.get(position);

        TextView nameTextView = view.findViewById(R.id.text_view_name);
        nameTextView.setText(device.getName());

        TextView addressTextView = view.findViewById(R.id.text_view_address);
        addressTextView.setText(device.getAddress());

        return view;
    }
}