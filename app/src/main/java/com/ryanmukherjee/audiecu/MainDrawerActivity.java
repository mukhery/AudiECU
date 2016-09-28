package com.ryanmukherjee.audiecu;

import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MainDrawerActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private final static int REQUEST_ENABLE_BT = 1;
    private DrawerLayout mDrawer;
    // Map for storing fragments as the user switches between them
    private Map<Integer, Fragment> mFragmentMap;
    private BluetoothAdapter mBluetoothAdapter;
    private Set<BluetoothDevice> mPairedDevices;
    private BluetoothDeviceArrayAdapter mArrayAdapter;
    private MenuItem mConnectionMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main_drawer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Get the fragment that is initialized on startup and place it into our map
        mFragmentMap = new HashMap<>();
        mFragmentMap.put(R.id.nav_terminal, getFragmentManager().findFragmentById(R.id.drawer_content));

        // Set navigation to the first drawer item
        if (savedInstanceState == null) {
            navigationView.getMenu().performIdentifierAction(R.id.nav_terminal, 0);
        }

        // Attempt to grab the current device's bluetooth radio
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    private void pickAdapterLaunchService() {
        mArrayAdapter = new BluetoothDeviceArrayAdapter(this, R.layout.bluetooth_device_item, R.id.deviceString, mBluetoothAdapter.getBondedDevices());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select OBD Adapter");

        builder.setSingleChoiceItems(mArrayAdapter, -1, null);

        builder.setPositiveButton("Connect",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // positive button logic
                        ListView listView = ((AlertDialog) dialog).getListView();
                        BluetoothDevice currDevice = mArrayAdapter.getDeviceItem(listView.getCheckedItemPosition());
                        Intent bluetoothServiceIntent = new Intent(MainDrawerActivity.this, BluetoothSPPService.class);
                        bluetoothServiceIntent.putExtra("bluetoothDevice", currDevice);

                        // Bind to the service so that we can monitor connection state
                        bindService(bluetoothServiceIntent, new ServiceConnection() {
                            @Override
                            public void onServiceConnected(ComponentName name, IBinder service) {
                                mConnectionMenu.setTitle("Disconnect");
                            }

                            @Override
                            public void onServiceDisconnected(ComponentName name) {
                                mConnectionMenu.setTitle("Connect");
                            }
                        }, 0);

                        startService(bluetoothServiceIntent);
                    }
                });

        builder.setNegativeButton(getString(android.R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Cancel the dialog
                        dialog.cancel();
                    }
                });

        AlertDialog dialog = builder.create();
        // display dialog
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK) {
                    // bluetooth was enabled
                    pickAdapterLaunchService();
                } else {
                    // bluetooth remains disabled
                    Toast newToast = Toast.makeText(this, "Functionality limited, Bluetooth is disabled!", Toast.LENGTH_LONG);
                    newToast.show();
                }
                break;
            default:
                throw new RuntimeException("Unhandled activity request code!");
        }
    }

    @Override
    public void onBackPressed() {
        if (mDrawer.isDrawerOpen(GravityCompat.START)) {
            mDrawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_drawer, menu);
        mConnectionMenu = menu.findItem(R.id.action_connection);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_connection:
                if (item.getTitle().equals("Connect")) {
                    if (mBluetoothAdapter == null) {
                        // Device does not support Bluetooth
                        Toast newToast = Toast.makeText(this, "Functionality limited, Bluetooth not supported!", Toast.LENGTH_LONG);
                        newToast.show();
                    } else {
                        // If the bluetooth adapter is not enabled, then request that it be enabled
                        if (!mBluetoothAdapter.isEnabled()) {
                            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                        } else {
                            // If the adapter is enabled, then let's prompt the user to connect
                            pickAdapterLaunchService();
                        }
                    }
                } else {
                    // Send a Bluetooth disconnect broadcast to the service
                    Intent intent = new Intent(BluetoothSPPService.ACTION_DISCONNECT);
                    sendBroadcast(intent);
                }
                return true;
            case R.id.action_settings:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        // Check if we've already initialized this fragment
        Fragment fragment = mFragmentMap.get(id);
        // If we haven't initialized the fragment
        if (fragment == null) {
            // Get the fragment class for the currently selected item
            Class fragmentClass;
            switch (id) {
                case R.id.nav_terminal:
                    fragmentClass = TerminalFragment.class;
                    break;
                case R.id.nav_diagnostics:
                case R.id.nav_logs:
                case R.id.nav_dumpecu:
                default:
                    throw new RuntimeException("Unhandled drawer item selected!");
            }

            // Initialize the fragment and store it in our map
            try {
                fragment = (Fragment) fragmentClass.newInstance();
                mFragmentMap.put(id, fragment);
            } catch (Exception e) {
                Crashlytics.logException(e);
            }
        }

        // Replace our content with the selected fragment and commit
        getFragmentManager().beginTransaction().replace(R.id.drawer_content, fragment).commitAllowingStateLoss();

        // Check the navigation drawer item now that it's selected
        item.setChecked(true);
        // Set our title to the drawer item's title
        setTitle(item.getTitle());
        // Close the drawer
        mDrawer.closeDrawer(GravityCompat.START);

        return true;
    }
}
