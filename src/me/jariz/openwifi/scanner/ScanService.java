package me.jariz.openwifi.scanner;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import me.jariz.openwifi.Global;

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
