package me.jariz.openwifi.scanner;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
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

    @Override
    protected void onHandleIntent(Intent intent) {
        context = this.getApplicationContext();
        Log.i(TAG, "HELLOWORLD from ScanService, " + intent.toString() + " " + context.toString());

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

                    int rand = new Random().nextInt(6);
                    URL url = new URL("http://clients"+ (rand == 0 ? 1 : rand) +".google.com/generate_204");
                    Log.i(TAG, "Testing internet connection trough url "+url.toString());

                    try {
                        HttpURLConnection con = (HttpURLConnection) url.openConnection();
                        if(con.getResponseCode() == 204) Global.State = WiFiScanner.STATE_CONNECTED;
                        else Global.State = WiFiScanner.STATE_TIMEOUT;
                    } catch(Exception z) {
                        Log.w(TAG, z.getMessage());
                        Global.State = WiFiScanner.STATE_TIMEOUT;
                    }
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


    String readStream(InputStream in) {
        BufferedReader reader = null;
        StringBuilder sb = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            String line = "";
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return sb.toString();
        }
    }
}
