package com.ryanmukherjee.audiecu;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothSPPService extends IntentService {

    private final static String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mSocket;
    private InputStream mInStream;
    private OutputStream mOutStream;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch(intent.getAction()) {
            }
        }
    };

    public BluetoothSPPService() {
        super("BluetoothSPPService");

        IntentFilter filter = new IntentFilter();
        filter.addAction(MainDrawerActivity.ACTION_SEND_COMMAND);

        registerReceiver(mReceiver, filter);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        BluetoothDevice bluetoothDevice = intent.getParcelableExtra("bluetoothDevice");
        mSocket = null;
        try {
            mSocket = bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString(SPP_UUID));
            if (mSocket != null) {
                mBluetoothAdapter.cancelDiscovery();
                mSocket.connect();
            }
        } catch (IOException e) {
            e.printStackTrace();

            if (mSocket != null && mSocket.isConnected()) {
                try {
                    mSocket.close();
                } catch (IOException ignored) {
                }
            }
        }

    }
}
