package bourne.denis.com.wifirssi;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private final String TAG = MainActivity.class.getSimpleName();
    // Declare global variables
    private WifiManager mWifiManager;
    private WifiScanReceiver mWifiScanReceiver;
    private ListView mListView;

    private ArrayAdapter<String> mArrayAdapter;
    private List<String> mArrayList = new ArrayList<>();

    private final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Get permissions to access the location
        requestPermission();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mListView = (ListView) findViewById(R.id.listView1);

        mWifiManager = (WifiManager) getApplicationContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (!mWifiManager.isWifiEnabled()) {
            Toast.makeText(getApplicationContext(), "wifi is disabled..making it enabled", Toast.LENGTH_LONG).show();
            mWifiManager.setWifiEnabled(true);
        }

        mWifiScanReceiver = new WifiScanReceiver();
        registerReceiver(mWifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        Button start;
        start = (Button) findViewById(R.id.start);
        start.setOnClickListener(v -> {

            mArrayList.clear();
            mWifiManager.startScan();
        });
    }

    class WifiScanReceiver extends BroadcastReceiver {

        public static final int REQUEST_CODE = 12345;

        @Override
        public void onReceive(Context context, Intent intent) {

            // Clear details to refresh the screen for each new scan
            if (mArrayList.size() > 0)
            {
                try
                {
                    mArrayList.clear();
                    mArrayAdapter.clear();
                    mArrayAdapter.notifyDataSetChanged();
                }
                catch (final Exception e)
                {
                    e.printStackTrace();
                }
            }

            List<ScanResult> mResultList = mWifiManager.getScanResults();
            Log.d(TAG, "The number of AP's: " + mResultList.size());

            try {

                // Run through each signal and retrieve the mac ssid rssi
                for (final ScanResult accessPoint : mResultList) {

                    String sb = accessPoint.SSID + "\n" +
                            String.valueOf(accessPoint.level) + "\n";

                    // Add info to StringBuilder

                    // Add to List that will be displayed to user
                    mArrayList.add(sb);
                }
            } catch (Exception e) {

                Log.e(TAG, e.getMessage());
            }

            mArrayAdapter = new ArrayAdapter<>(getApplicationContext(),
                    android.R.layout.simple_list_item_1, mArrayList);
            mListView.setAdapter(mArrayAdapter);
        }
    }

    @Override
    protected void onPause() {
        unregisterReceiver(mWifiScanReceiver);
        super.onPause();
    }

    @Override
    protected void onResume() {
        registerReceiver(mWifiScanReceiver, new IntentFilter(
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        super.onResume();
    }

    /**
     * Get user to give access to location for app
     */
    public void requestPermission() {

        List<String> permissionsNeeded = new ArrayList<>();

        final List<String> permissionsList = new ArrayList<>();
        if (!addPermission(permissionsList, Manifest.permission.ACCESS_FINE_LOCATION))
            permissionsNeeded.add("GPS");

        if (permissionsList.size() > 0) {
            if (permissionsNeeded.size() > 0) {
                // Need Rationale
                String message = "You need to grant access to " + permissionsNeeded.get(0);
                for (int i = 1; i < permissionsNeeded.size(); i++)
                    message = message + ", " + permissionsNeeded.get(i);
                showMessageOKCancel(message,
                        (dialog, which) -> requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
                                REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS));
                return;
            }
            requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
                    REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
        }
    }

    private boolean addPermission(List<String> permissionsList, String permission) {
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(permission);
            // Check for Rationale Option
            if (!shouldShowRequestPermissionRationale(permission))
                return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS: {
                Map<String, Integer> perms = new HashMap<>();
                // Initial
                perms.put(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                // Fill with results
                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);
                // Check for ACCESS_FINE_LOCATION
                if (perms.get(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    // All Permissions Granted

                    Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show();
                } else {
                    // Permission Denied
                    Toast.makeText(MainActivity.this, "Some Permission is Denied", Toast.LENGTH_SHORT)
                            .show();
                }
            }
            break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    /**
     * Start background service to get users lat and long
     * using an alarm manager to run every 5 seconds
     *
     */
    public void scheduleAlarmManger() {

        Log.d(TAG, "AlarmManger has been called");

        Intent alarm = new Intent(getApplicationContext(), WifiScanReceiver.class);

        PendingIntent mPendingIntent = PendingIntent.getBroadcast(this, WifiScanReceiver.REQUEST_CODE, alarm, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        mAlarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), 5000, mPendingIntent);
    }
}
