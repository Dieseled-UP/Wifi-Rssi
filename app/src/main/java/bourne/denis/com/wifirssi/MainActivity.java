package bourne.denis.com.wifirssi;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private final String TAG = MainActivity.class.getSimpleName();
    // Declare global variables
    private WifiManager mWifiManager;
    private WifiScanReceiver mWifiScanReceiver;
    private ListView mListView;

    private ArrayAdapter<String> mArrayAdapter;
    private List<String> mArrayList = new ArrayList<>();

    private final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 123;

    private static final String REGISTER_URL = "http://pathTo/yourUploadFile.php";
    private static final String KEY_AP = "ssid";
    private static final String KEY_RSSI = "rssi";

    private String ssid;
    private String rssi;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Get permissions to access the location
        requestPermission();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mListView = (ListView) findViewById(R.id.listView1);

        // Instantiate the WiFi Manager
        mWifiManager = (WifiManager) getApplicationContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // Check that the WiFi is enabled
        if (!mWifiManager.isWifiEnabled()) {
            Toast.makeText(getApplicationContext(), "wifi is disabled..making it enabled", Toast.LENGTH_LONG).show();
            mWifiManager.setWifiEnabled(true);
        }

        // Register the Broadcast Receiver
        mWifiScanReceiver = new WifiScanReceiver();
        registerReceiver(mWifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        Button start;
        start = (Button) findViewById(R.id.start);
        start.setOnClickListener(v -> {

            mArrayList.clear();

            // Timer added to get new scan result once every 2 seconds
            final Timer myTimer = new Timer();

            myTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    TimerMethod();
                }
            }, 0, 2000);
        });
    }

    /**
     * Timer method to run at the same time as the main activity
     */
    private void TimerMethod() {
        this.runOnUiThread(Timer_Tick);
    }

    /**
     * Runnable thread that allows for scan to be called
     * without crashing out the main thread
     */
    private final Runnable Timer_Tick = () -> {
        try {
            // start a scan of ap's
            mWifiManager.startScan();
        } catch (final Exception e) {
            e.getStackTrace();
        }
    };

    /**
     * Broadcast Receiver to capture the WiFi AP information
     */
    class WifiScanReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            // Clear details to refresh the screen for each new scan
            if (mArrayList.size() > 0) {
                try {
                    mArrayList.clear();
                    mArrayAdapter.clear();
                    mArrayAdapter.notifyDataSetChanged();
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }

            // Store all listed AP's
            List<ScanResult> mResultList = mWifiManager.getScanResults();
            Log.d(TAG, "The number of AP's: " + mResultList.size());

            try {

                // Run through each signal and retrieve the SSID & RSSI
                for (final ScanResult accessPoint : mResultList) {

                    if (getAccessPoint(accessPoint.SSID)) {

                        ssid = accessPoint.SSID;
                        rssi = String.valueOf(accessPoint.level);

                        String apDetails = accessPoint.SSID + "\n" +
                                String.valueOf(accessPoint.level) + "\n";

                        // Add to List that will be displayed to user
                        mArrayList.add(apDetails);
                    }

                }
            } catch (Exception e) {

                Log.e(TAG, e.getMessage());
            }

            // Display details in ListView
            mArrayAdapter = new ArrayAdapter<>(getApplicationContext(),
                    android.R.layout.simple_list_item_1, mArrayList);
            mListView.setAdapter(mArrayAdapter);

            StringRequest stringRequest = new StringRequest(Request.Method.POST, REGISTER_URL,
                    response -> Toast.makeText(MainActivity.this,response,Toast.LENGTH_LONG).show(),
                    error -> Toast.makeText(MainActivity.this,error.toString(),Toast.LENGTH_LONG).show()){
                @Override
                protected Map<String,String> getParams(){

                    Map<String,String> params = new HashMap<>();
                    params.put(KEY_AP, ssid);
                    params.put(KEY_RSSI, rssi);
                    return params;
                }

            };

            RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
            requestQueue.add(stringRequest);
        }
    }

    public boolean getAccessPoint(String ssid) {

        return ssid.equalsIgnoreCase("Access Point");

    }

    @Override
    protected void onPause() {

        super.onPause();

        // Unregister the Receiver
        unregisterReceiver(mWifiScanReceiver);
    }

    @Override
    protected void onResume() {

        super.onResume();

        // Start scanning again
        registerReceiver(mWifiScanReceiver, new IntentFilter(
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
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
}
