package com.ryanmukherjee.audiecu;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BluetoothDeviceArrayAdapter extends ArrayAdapter<String> {

    private List<BluetoothDevice> mBluetoothDevices;

    public BluetoothDeviceArrayAdapter(Context context, int layout, int textField, Set<BluetoothDevice> bluetoothDeviceSet) {
        super(context, layout, textField);
        mBluetoothDevices = new ArrayList<>();
        // If there are paired devices
        if (bluetoothDeviceSet.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : bluetoothDeviceSet) {
                // Add the name and address to an array adapter to show in a ListView
                add(device.getName() + "\n" + device.getAddress());
                mBluetoothDevices.add(device);
            }
        }
    }

    public BluetoothDevice getDeviceItem(int position) {
        return mBluetoothDevices.get(position);
    }
}
