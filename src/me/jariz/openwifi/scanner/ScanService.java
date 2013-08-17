package me.jariz.openwifi.scanner;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import me.jariz.openwifi.Global;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

/**
 * JARIZ.PRO
 * Created @ 07/08/13
 * By: JariZ
 * Project: OpenWiFi
 * Package: me.jariz.openwifi.scanner
 */

/**
 * ScanService simply times out between calling a new scan and manages notifications
 */
public class ScanService extends IntentService {

    public ScanService() {
        super("ScanService");
    }

    String TAG = "OW_SCANSERVICE";
    Context context;
    ConnectivityManager connectivityManager;

    @Override
    protected void onHandleIntent(Intent intent) {
        context = this.getApplicationContext();
        connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Log.i(TAG, "HELLOWORLD from ScanService, " + intent.toString());

        if (Global.wifiManager == null || Global.State == WiFiScanner.STATE_DESTROYED) {
            Log.w(TAG, "Application has vanished, service still running, attempting to unschedule me from alarm...");
            AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent scanService = new Intent(getApplicationContext(), ScanService.class);
            PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1337, scanService, 0);
            manager.cancel(pendingIntent);
            stopSelf();
            return;
        }


        try {
            switch (Global.State) {
                case WiFiScanner.STATE_TESTING:
                    /**
                     * @see http://www.chromium.org/chromium-os/chromiumos-design-docs/network-portal-detection
                     */

                    //another instance of this service is already running a request
                    if(Global.TestRunning) return;

                    //check for active connection to make sure we're testing trough wifi
                    NetworkInfo info = connectivityManager.getActiveNetworkInfo();
                    if(info != null && info.getType() != ConnectivityManager.TYPE_WIFI) {
                        Log.i(TAG, "Woops, can't run test if we're not connected to wifi yet! Waiting for WiFiScanner to call me when the connectivity changes....");
                        break;
                    }

                    int rand = new Random().nextInt(6);
                    URL url = new URL("http://clients"+ (rand == 0 ? 1 : rand) +".google.com/generate_204");
                    Log.i(TAG, "Testing for portals trough url "+url.toString());

                    Global.TestRunning = true;
                    try {
                        HttpURLConnection con = (HttpURLConnection) url.openConnection();
                        if(con.getResponseCode() == 204) {
                            Log.i(TAG, "Portal detection success, no portals found, changing state to connected!");
                            Global.State = WiFiScanner.STATE_CONNECTED;
                        }
                        else {
                            Log.i(TAG, "Portal detection success, PORTAL FOUND, changing state to timeout.");
                            Global.State = WiFiScanner.STATE_TIMEOUT;
                        }
                    } catch(Exception z) {
                        Log.i(TAG, "Portal detection failed, "+z.getMessage());
                        Global.State = WiFiScanner.STATE_TIMEOUT;
                    }
                    Global.TestRunning = false;
                    Global.mainActivity.runOnUiThread(new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Global.mainActivity.interfaceCallback(WiFiScanner.CALLBACK_STATE_CHANGED);
                        }
                    }));
                    break;
                case WiFiScanner.STATE_SCANNING:
                case WiFiScanner.STATE_TIMEOUT:
                    Global.State = WiFiScanner.STATE_TIMEOUT;
                    Global.mainActivity.runOnUiThread(new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Global.mainActivity.interfaceCallback(WiFiScanner.CALLBACK_STATE_CHANGED);
                        }
                    }));

                    try {
                        Thread.sleep(Global.Timeout);
                    } catch (InterruptedException e) {
                    }

                    Global.wifiManager.startScan();
                    break;
            }
        } catch (Exception z) {
            Log.e(TAG, z.toString());
        }
    }
}
