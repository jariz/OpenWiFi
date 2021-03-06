package me.jariz.openwifi.scanner;

/**
 * JARIZ.PRO
 * Created @ 07/08/13
 * By: JariZ
 * Project: OpenWiFi
 * Package: me.jariz.openwifi
 */

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.*;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import me.jariz.openwifi.Global;
import me.jariz.openwifi.MainActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;

/**
 * WiFiScanner does most of the non-visible scripting.
 * If this were a MVC-layout, WiFiScanner would be the 'controller' and MainActivty would be the 'view'
 */
public class WiFiScanner {

    /* PRIVATE VARS */
    WifiManager wifiManager;
    AlarmManager alarmManager;
    String TAG = "OW_WIFISCANNER";
    BroadcastReceiver wifiStatusReceiver;
    BroadcastReceiver wifiScanReceiver;
    MainActivity parent;
    SharedPreferences sharedPreferences;

    /* - CONSTANTS */

    /**
     * WiFiScanner is disabled
     */
    public static final int STATE_DESTROYED = -1;

    /**
     * Android's wifi is disabled
     */
    public static final int STATE_DISABLED = 0;

    /**
     * OpenWiFi is timing out between the scans (default 10s)
     * Note that this is something else than STATE_SCANNING because that's for when android is scanning,
     * This state is strictly for the delay between scans and not for the actual scanning itself
     */
    public static final int STATE_TIMEOUT = 1;

    /**
     * Android is scanning
     */
    public static final int STATE_SCANNING = 2;

    /**
     * Android is connecting (includes authorization, associating and whatever)
     */
    public static final int STATE_CONNECTING = 3;

    /**
     * Android is connected.
     */
    public static final int STATE_CONNECTED = 4;

    /**
     * Android's wifi is enabling.
     */
    public static final int STATE_ENABLING = 5;

    /**
     * Android's wifi is disabling
     */
    public static final int STATE_DISABLING = 6;

    /**
     * OpenWiFi is testing the connection
     */
    public static final int STATE_TESTING = 7;

    /**
     * The State property changed.
     */
    public static final int CALLBACK_STATE_CHANGED = 1;

    /* PUBLIC FUNCTIONS */
    public void Init(final MainActivity parent) {
        this.parent = parent;
        sharedPreferences = parent.getSharedPreferences("OpenWiFi", 0);
        alarmManager = (AlarmManager) parent.getSystemService(Context.ALARM_SERVICE);
        wifiManager = (WifiManager) parent.getSystemService(Context.WIFI_SERVICE);
        Global.wifiManager = wifiManager;

        if (Global.State == STATE_DESTROYED) Global.State = STATE_SCANNING; //placeholder state while we're initing

        Log.i(TAG, "####################");
        Log.i(TAG, "# WifiScanner init #");
        Log.i(TAG, "####################");

        Log.i(TAG, "- Registering receivers...");
        wifiStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int oldState = Global.State;
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();

                Log.i(TAG, "wifiStatusReceiver: Received " + (intent == null ? "UNKNOWN" : intent.toString()));

                //exceptions
                if (Global.State == STATE_DESTROYED) {
                    Log.i(TAG, "wifiStatusReceiver: WifiScanner is destroyed, ignoring state change, sending callback to mainactivity");
                    parent.interfaceCallback(CALLBACK_STATE_CHANGED);
                    return;
                }

                if(intent != null && Global.State == STATE_TESTING && intent.getAction().equals("android.net.conn.CONNECTIVITY_CHANGE")) {
                    //special circumstance where we're gonna manually launch the service because it skipped it's round because the current network isn't wifi.
                    Log.i(TAG, "wifiStatusReceiver: Connectivity change while testing, calling service manually without alarm.");
                    parent.startService(new Intent(parent.getApplicationContext(), ScanService.class));
                } else if(intent.getAction().equals("android.net.conn.CONNECTIVITY_CHANGE")) return;

                switch (wifiManager.getWifiState()) {
                    case WifiManager.WIFI_STATE_ENABLING:
                        Global.State = STATE_ENABLING;
                        break;
                    case WifiManager.WIFI_STATE_DISABLING:
                    case WifiManager.WIFI_STATE_UNKNOWN:
                        Global.State = STATE_DISABLING;
                        break;
                    case WifiManager.WIFI_STATE_DISABLED:
                        if(oldState != STATE_ENABLING)
                            Global.State = STATE_DISABLED;
                        else {
                            Global.State = STATE_ENABLING;
                            parent.interfaceCallback(CALLBACK_STATE_CHANGED); //force gui callback
                        }
                        break;
                    case WifiManager.WIFI_STATE_ENABLED:
                        switch (wifiInfo.getSupplicantState()) {
                            case INACTIVE:
                            case SCANNING:
                            case DISCONNECTED:
                                Global.State = STATE_SCANNING;
                                break;
                            case ASSOCIATING:
                            case AUTHENTICATING:
                            case FOUR_WAY_HANDSHAKE:
                            case ASSOCIATED:
                                Global.State = STATE_CONNECTING;
                                break;
                            case COMPLETED:
                                if(sharedPreferences.getBoolean("network", false) && Global.State != STATE_CONNECTED) {
                                    //change state to testing, scanservice will do the actual testing
                                    Global.State = STATE_TESTING;
                                } else Global.State = STATE_CONNECTED;
                                break;
                            case INTERFACE_DISABLED:
                            case INVALID:
                            case UNINITIALIZED:
                                Global.State = STATE_DISABLED;
                                break;
                        }
                        break;
                }

                if (oldState != Global.State) {
                    Log.i(TAG, "wifiStatusReceiver: State changed from " + oldState + " to " + Global.State);
                    parent.interfaceCallback(CALLBACK_STATE_CHANGED);
                }
            }
        };
        IntentFilter filter = new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        parent.registerReceiver(wifiStatusReceiver, filter);

        wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (Global.State) {
                    case WiFiScanner.STATE_CONNECTED:
                    case WiFiScanner.STATE_CONNECTING:
                    case WiFiScanner.STATE_TESTING:
                        Log.i(TAG, "wifiScanReceiver: Ignoring scan results because connection's busy.");
                        return;
                }
                processWiFis(wifiManager.getScanResults());
            }
        };
        filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        parent.registerReceiver(wifiScanReceiver, filter);

        Log.i(TAG, "- Starting service...");
        Intent scanService = new Intent(parent.getApplicationContext(), ScanService.class);
        PendingIntent pendingIntent = PendingIntent.getService(parent.getApplicationContext(), 1337, scanService, 0);
        try {
            //cancel service if already running
            alarmManager.cancel(pendingIntent);
            Log.i(TAG, "        - Killed service that was already running!");
        } catch (Exception e) {
        }

        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + Global.Timeout, Global.Timeout, pendingIntent);
        parent.startService(scanService);

        Log.i(TAG, "- Checking if WiFi is disabled and if so, enabling it...");
        if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
            Global.State = STATE_ENABLING; //manually set state because android doesn't directly change the state before next gui callback occurs
            wifiManager.setWifiEnabled(true);
            Log.i(TAG, "        - WiFi not enabled, enabling...");
        } else {
            Log.i(TAG, "        - WiFi already enabled.");
        }
    }

    public void Destroy() {
        if (parent == null) return;

        Log.i(TAG, "#######################");
        Log.i(TAG, "# WifiScanner destroy #");
        Log.i(TAG, "#######################");

        Log.i(TAG, "- Unregistering receivers...");
        try {
            parent.unregisterReceiver(wifiStatusReceiver);
            parent.unregisterReceiver(wifiScanReceiver);
        } catch (IllegalArgumentException z) {
            Log.w(TAG, "Tried to unregister a receiver that wasn't registered: "+z.getMessage());
        }

        Log.i(TAG, "- Killing service...");
        Intent scanService = new Intent(parent, ScanService.class);
        PendingIntent pendingIntent = PendingIntent.getService(parent, 1337, scanService, 0);
        try {
            //cancel service if already running
            alarmManager.cancel(pendingIntent);
        } catch (Exception e) {
            Log.e(TAG, "    - Unable to kill service(?)");
        }

        Global.State = STATE_DESTROYED;
        wifiStatusReceiver.onReceive(parent, null);
    }

    public String getCurrentSSID() {
        return wifiManager.getConnectionInfo().getSSID();
    }

    /* PRIVATE FUNCTIONS */

    /**
     * Function that will pick out the right networks and connect to them
     */
    void processWiFis(List<ScanResult> results) {
        WifiInfo info = wifiManager.getConnectionInfo();
        for (ScanResult result : results) {
            if (getScanResultSecurity(result).equals(OPEN)) {

                if(result.SSID == null  || result.SSID.equals("0x")|| result.SSID.equals("<unknown ssid>")) {
                    // https://code.google.com/p/android/issues/detail?id=43336
                    Log.w(TAG, "[ANDROID BUG] Tried to connect to a unknown/null SSID, aborted!");
                    continue;
                }

                WifiConfiguration wc = new WifiConfiguration();
                wc.BSSID = result.BSSID;
                wc.SSID = "\"" + result.SSID + "\"";
                wc.hiddenSSID = true;
                wc.priority = 0xBADBAD; //badbadbad
                wc.status = WifiConfiguration.Status.ENABLED;
                wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

                //first check if network already exists, if yes, connect to it.
                List<WifiConfiguration> configurationList = wifiManager.getConfiguredNetworks();
                Iterator<WifiConfiguration> configurationIterator = configurationList.iterator();
                boolean found = false;
                while (configurationIterator.hasNext()) {
                    WifiConfiguration configuration = configurationIterator.next();
                    if (wc.BSSID.equals(configuration.BSSID)) {

                        //it does match what we're looking for, but we're already connected to it....
                        if(info != null && configuration.BSSID.equals(info.getBSSID())) return;

                        //match! update & enable
                        wc.networkId = configuration.networkId;
                        int id = wifiManager.updateNetwork(wc);
                        wifiManager.enableNetwork(id, true);
                        Log.i(TAG, "Updated & enabled existing network '" + wc.SSID + "' (" + wc.BSSID + "')");
                        found = true;
                        break;
                    }
                }

                //not found, insert & enable
                if (!found) {
                    int id = wifiManager.addNetwork(wc);
                    wifiManager.enableNetwork(id, true);

                    Log.i(TAG, "Inserted & enabled new network '" + wc.SSID + "' (" + wc.BSSID + "')");
                }

                wifiManager.saveConfiguration();
                wifiManager.reconnect();
            }
        }
    }

    //Stolen from Settings.apk
    public static final String PSK = "PSK";
    public static final String WEP = "WEP";
    public static final String EAP = "EAP";
    public static final String OPEN = "Open";
    public static final String ADHOC = "IBSS";

    public static String getScanResultSecurity(ScanResult scanResult) {
        final String cap = scanResult.capabilities;
        final String[] securityModes = {WEP, PSK, EAP, ADHOC};
        for (int i = securityModes.length - 1; i >= 0; i--) {
            if (cap.contains(securityModes[i])) {
                return securityModes[i];
            }
        }

        return OPEN;
    }
}
