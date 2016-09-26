package com.ryanmukherjee.audiecu;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.support.annotation.Nullable;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothSPPService extends IntentService {

    private final static String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
    public final static String ACTION_SEND_COMMAND = "com.ryanmukherjee.intent.SEND_COMMAND";
    public final static String ACTION_DISCONNECT = "com.ryanmukherjee.intent.DISCONNECT";

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mSocket;
    private InputStream mInStream;
    private OutputStream mOutStream;

    private boolean mDisconnect;

    private final IBinder mBinder = new ServiceBinder();

    public class ServiceBinder extends Binder {
        public BluetoothSPPService getService() {
            return BluetoothSPPService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mSocket == null || !mSocket.isConnected())
                throw new RuntimeException("Cannot handle intents before establishing a connection!");

            switch(intent.getAction()) {
                case ACTION_SEND_COMMAND:
                    try {
                        String serialContent = intent.getStringExtra("content");
                        mOutStream.write(serialContent.getBytes());
                        ContentValues values = new ContentValues();
                        values.put(SerialContentProvider.SERIAL_CONTENT, serialContent);
                        values.put(SerialContentProvider.SERIAL_TIMESTAMP, " time('now') ");
                        getContentResolver().insert(SerialContentProvider.SERIAL_URI, values);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case ACTION_DISCONNECT:
                    mDisconnect = true;
                    break;
                default:
                    throw new RuntimeException("Unhandled intent action received!");
            }
        }
    };

    public BluetoothSPPService() {
        super("BluetoothSPPService");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_SEND_COMMAND);
        filter.addAction(ACTION_DISCONNECT);

        registerReceiver(mReceiver, filter);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mReceiver);
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
                mInStream = mSocket.getInputStream();
                mOutStream = mSocket.getOutputStream();

                BufferedReader reader = new BufferedReader(new InputStreamReader(mInStream));
                String content;

                // Keep listening to the InputStream until we receive a disconnect intent
                mDisconnect = false;
                while (!mDisconnect) {
                    if (reader.ready()) {
                        // Read from the BufferedReader
                        content = reader.readLine();
                        // TODO put content into the contentprovider
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (mSocket != null && mSocket.isConnected()) {
                try {
                    mSocket.close();
                } catch (IOException ignored) {
                }
            }
        }

    }
}
