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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.os.ResultReceiver;

import com.crashlytics.android.Crashlytics;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.UUID;

public class BluetoothSPPService extends IntentService {

    private final static String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
    public final static String ACTION_SEND_COMMAND = "com.ryanmukherjee.intent.SEND_COMMAND";
    public final static String ACTION_DISCONNECT = "com.ryanmukherjee.intent.DISCONNECT";

    public final static int BLUETOOTH_CONNECTED = 100;
    public final static int BLUETOOTH_DISCONNECTED = 200;

    public enum SerialType {
        INPUT, OUTPUT
    }

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mSocket;
    private InputStream mInStream;
    private OutputStream mOutStream;

    private boolean mDisconnect;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mSocket == null || !mSocket.isConnected())
                throw new RuntimeException("Cannot handle intents before establishing a connection!");

            switch(intent.getAction()) {
                case ACTION_SEND_COMMAND:
                    try {
                        String serialContent = intent.getStringExtra("content");
                        // Add carriage return
                        serialContent += '\r';
                        // Send to bluetooth device
                        mOutStream.write(serialContent.getBytes());
                        // Log sent command to our local content provider
                        logSerial(serialContent, SerialType.INPUT);
                    } catch (IOException e) {
                        Crashlytics.logException(e);
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

    private void logSerial(String serialContent, SerialType type) {
        ContentValues values = new ContentValues();
        values.put(SerialContentProvider.SERIAL_CONTENT, serialContent);
        values.put(SerialContentProvider.SERIAL_TYPE, type.ordinal());
        getContentResolver().insert(SerialContentProvider.SERIAL_URI, values);
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
        ResultReceiver connectionReceiver = intent.getParcelableExtra("connectionReceiver");
        mSocket = null;
        try {
            mSocket = bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString(SPP_UUID));
            if (mSocket != null) {
                mBluetoothAdapter.cancelDiscovery();
                mSocket.connect();
                mInStream = mSocket.getInputStream();
                mOutStream = mSocket.getOutputStream();

                // Keep listening to the InputStream until we receive a disconnect intent
                byte[] inBuffer = new byte[8192];
                connectionReceiver.send(BLUETOOTH_CONNECTED, null);
                mDisconnect = false;
                while (!mDisconnect) {
                    if (mInStream.available() > 0) {
                        // Read from the BufferedReader
                        int length = mInStream.read(inBuffer);
                        String content = IOUtils.toString(Arrays.copyOfRange(inBuffer, 0, length), "UTF-8");
                        logSerial(content, SerialType.OUTPUT);
                    }
                }
            }
        } catch (IOException e) {
            Crashlytics.logException(e);
        } finally {
            if (mSocket != null && mSocket.isConnected()) {
                try {
                    mSocket.close();
                } catch (IOException ignored) {
                }
            }
            connectionReceiver.send(BLUETOOTH_DISCONNECTED, null);
        }

    }
}
