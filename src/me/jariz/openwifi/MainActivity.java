package me.jariz.openwifi;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import de.passsy.holocircularprogressbar.HoloCircularProgressBar;
import me.jariz.openwifi.scanner.WiFiScanner;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Global.wiFiScanner = scanner;
        Global.mainActivity = this;
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        holo = (HoloCircularProgressBar) findViewById(R.id.holoCircularProgressBar1);
    }

    NotificationManager notificationManager;

    public void interfaceCallback(int type) {

        //kinda like eventhandlers in C#
        switch (type) {
            case WiFiScanner.CALLBACK_STATE_CHANGED:

                //GUI
                switch (scanner.State) {
                    case WiFiScanner.STATE_CONNECTED:
                        if(onoff != null) onoff.setClickable(true);
                        CircularAnimationUtils.pulseKeep = true;
                        CircularAnimationUtils.pulse(holo, 1000);
                        setStatusColor(android.R.color.holo_green_light, 1000, 1000);
                        setStatusString("You are connected to '" + scanner.getCurrentSSID() + "'");
                        break;
                    case WiFiScanner.STATE_CONNECTING:
                        if(onoff != null) onoff.setClickable(true);
                        CircularAnimationUtils.pulseKeep = true;
                        CircularAnimationUtils.pulse(holo, 300);
                        setStatusColor(R.color.holo_yellow_light, 300, 300);
                        setStatusString("Connecting to '" + scanner.getCurrentSSID() + "'....");
                        break;
                    case WiFiScanner.STATE_DISABLING:
                        if(onoff != null) onoff.setClickable(false);
                        setStatusString("WiFi is disabling...");
                        break;
                    case WiFiScanner.STATE_ENABLING:
                        if(onoff != null) onoff.setClickable(false);
                        setStatusString("WiFi is enabling...");
                        break;
                    case WiFiScanner.STATE_DISABLED:
                        if(onoff != null) onoff.setClickable(true);
                        CircularAnimationUtils.pulseKeep = false;
                        setStatusColor(android.R.color.darker_gray, 10000, 0);
                        setStatusString("WiFi is disabled (CHANGEME)");
                        break;
                    case WiFiScanner.STATE_DESTROYED:
                        if(onoff != null) onoff.setClickable(true);
                        CircularAnimationUtils.pulseKeep = false;
                        setStatusColor(android.R.color.darker_gray, 10000, 0);
                        setStatusString("OpenWiFi is disabled.");

                        break;
                    case WiFiScanner.STATE_SCANNING:
                        if(onoff != null) onoff.setClickable(true);
                        setStatusString("Running scan...");
                        setStatusColor(android.R.color.holo_blue_dark, 100, 50);
                        CircularAnimationUtils.pulseKeep = false;
                        break;
                    case WiFiScanner.STATE_TIMEOUT:
                        if(onoff != null) onoff.setClickable(true);
                        setStatusColor(android.R.color.holo_blue_bright, 100, 100);
                        setStatusString("Waiting for scan");
                        CircularAnimationUtils.pulseKeep = false;
                        CircularAnimationUtils.fillProgressbar(scanner.Timeout, holo);
                        break;
                }
                break;
        }
    }

    String TAG = "OW_ACTIVITY";
    WiFiScanner scanner = new WiFiScanner();

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "MainActivity is kil, k?");
        scanner.Destroy();
    }

    void Switch(boolean enable) {
        if (enable) scanner.Init(this);
        else scanner.Destroy();
    }

    int statusColor = 0;
    String statusString = "";
    int onMs;
    int offMs;

    void setStatusColor(int res, int onMs, int offMs) {
        statusColor = getResources().getColor(res);
        holo.setColor(statusColor);
        holo.invalidate();
        this.onMs = onMs;
        this.offMs = offMs;
        updateNotification();
    }

    void setStatusString(String statusString) {
        this.statusString = statusString;
        TextView textView = (TextView) findViewById(R.id.textView);
        textView.setText(statusString);
        updateNotification();
    }

    void updateNotification() {
        Notification.Builder builder = new Notification.Builder(getApplicationContext());
        builder.setSmallIcon(R.drawable.openwifi_notification);
        builder.setContentTitle("OpenWiFi");
        builder.setContentText(statusString);
        builder.setLights(statusColor, onMs, offMs);
        builder.setOngoing(true);
        builder.setTicker("OpenWiFi - " + statusString);

        //special circumstances
        switch (scanner.State) {
            case WiFiScanner.STATE_DESTROYED:
                notificationManager.cancel(0xDEADBEEF);
                return;
            case WiFiScanner.STATE_CONNECTED:
                builder.setVibrate(new long[]{-1, 100, 50, 100, 50, 100, 50, 50, 50, 50});
                break;
            case WiFiScanner.STATE_TIMEOUT:
                builder.setProgress(0, 0, true);
                break;
        }
        notificationManager.notify(0xDEADBEEF, builder.build());
    }

    HoloCircularProgressBar holo;
    Switch onoff;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem item = menu.findItem(R.id.onoff);
        Switch s = (Switch) item.getActionView().findViewById(R.id.switch1);
        onoff = s;
        s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Switch(b);
            }
        });
        return true;
    }

}
