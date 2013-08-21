package me.jariz.openwifi;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.RingtoneManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import de.passsy.holocircularprogressbar.HoloCircularProgressBar;
import me.jariz.openwifi.scanner.WiFiScanner;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getSharedPreferences("OpenWiFi", 0);

        int theme = sharedPreferences.getInt("theme", 0);
        switch (theme) {
            case 0:
                setTheme(R.style.OpenWiFi);
                break;
            case 1:
                setTheme(R.style.OpenWiFi_Wallpaper);
                break;
            case 2:
                setTheme(R.style.OpenWiFi_Light_Dark);
                getActionBar().setIcon(R.drawable.ic_launcher_white);
                break;
            case 3:
                setTheme(R.style.OpenWiFi_Light);
                getActionBar().setIcon(R.drawable.ic_launcher_white);
                break;
        }

        final int timeout = sharedPreferences.getInt("timeout", 3);
        changeTimeout(timeout);

        setContentView(R.layout.activity_main);
        Global.wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        Global.mainActivity = this;
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        holo = (HoloCircularProgressBar) findViewById(R.id.holoCircularProgressBar1);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        Spinner timeoutSpinner = (Spinner) findViewById(R.id.timeouts);
        timeoutSpinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.timeouts)));
        timeoutSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                sharedPreferences.edit().putInt("timeout", i).commit();
                changeTimeout(i);
            }
            @Override public void onNothingSelected(AdapterView<?> adapterView) {}
        });
        timeoutSpinner.setSelection(timeout);

        Spinner themesSpinner = (Spinner) findViewById(R.id.themes);
        themesSpinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.themes)));
        themesSpinner.setSelection(theme);
        themesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                int old = sharedPreferences.getInt("theme", -1);
                sharedPreferences.edit().putInt("theme", i).commit();
                if (i != old) {
                    finish();
                    startActivity(getIntent());
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        Switch vibrateSwitch = (Switch)findViewById(R.id.vibration);
        vibrateSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                sharedPreferences.edit().putBoolean("vibration", b).commit();
            }
        });
        vibrateSwitch.setChecked(sharedPreferences.getBoolean("vibration", true));

        Switch soundSwitch = (Switch)findViewById(R.id.sound);
        soundSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                sharedPreferences.edit().putBoolean("sound", b).commit();
            }
        });
        soundSwitch.setChecked(sharedPreferences.getBoolean("sound", true));

        Switch networkSwitch = (Switch)findViewById(R.id.network);
        networkSwitch.setChecked(sharedPreferences.getBoolean("network", false));
        networkSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                sharedPreferences.edit().putBoolean("network", b).commit();
                if(Global.State != WiFiScanner.STATE_DESTROYED) {
                    Switch(false); Switch(true);
                }
            }
        });

        // enable ActionBar app icon to behave as action to toggle nav drawer
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        drawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                drawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */
                R.string.app_name,  /* "open drawer" description for accessibility */
                R.string.app_name /* "close drawer" description for accessibility */
        );

        drawerLayout.setDrawerListener(drawerToggle);
    }

    SharedPreferences sharedPreferences;
    ActionBarDrawerToggle drawerToggle;
    DrawerLayout drawerLayout;
    NotificationManager notificationManager;

    void changeTimeout(int timeout) {
        switch (timeout) {
            case 0:
                Global.Timeout = 3000;
                break;
            case 1:
                Global.Timeout = 5000;
                break;
            case 2:
                Global.Timeout = 7000;
                break;
            case 3:
                Global.Timeout = 10000;
                break;
            case 4:
                Global.Timeout = 15000;
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return drawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
    }

    @Override
    protected void onNewIntent (Intent intent) {
        super.onNewIntent(intent);
        if(intent.getBooleanExtra("me.jariz.openwifi.disable", false))
            if(Global.State != WiFiScanner.STATE_DESTROYED)
                onoff.setChecked(false);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    public void interfaceCallback(int type) {

        //kinda like eventhandlers in C#
        switch (type) {
            case WiFiScanner.CALLBACK_STATE_CHANGED:

                //GUI
                switch (Global.State) {
                    case WiFiScanner.STATE_CONNECTED:
                        if(onoff != null) onoff.setEnabled(true);
                        CircularAnimationUtils.pulseKeep = true;
                        CircularAnimationUtils.pulseDuration = 1000;
                        CircularAnimationUtils.pulse(holo);
                        setStatusColor(android.R.color.holo_green_light, 1000, 1000);
                        setStatusString("You are connected to '" + scanner.getCurrentSSID() + "'");
                        CircularAnimationUtils.stopProgressBar();
                        break;
                    case WiFiScanner.STATE_CONNECTING:
                        if(onoff != null) onoff.setEnabled(true);
                        CircularAnimationUtils.pulseKeep = true;
                        CircularAnimationUtils.pulseDuration = 300;
                        CircularAnimationUtils.pulse(holo);
                        setStatusColor(R.color.holo_yellow_light, 300, 300);
                        setStatusString("Connecting to '" + scanner.getCurrentSSID() + "'....");
                        CircularAnimationUtils.stopProgressBar();
                        break;
                    case WiFiScanner.STATE_DISABLING:
                        if(onoff != null) onoff.setEnabled(false);
                        setStatusString("WiFi is disabling...");
                        break;
                    case WiFiScanner.STATE_DISABLED:
                        if(onoff != null) {
                            //this will call Switch(false) which will destroy the scanner and change the state to destroyed.
                            onoff.setChecked(false);
                        }
                        break;
                    case WiFiScanner.STATE_ENABLING:
                        if(onoff != null) onoff.setEnabled(false);
                        setStatusString("WiFi is enabling...");
                        break;
                    case WiFiScanner.STATE_DESTROYED:
                        if(onoff != null) onoff.setEnabled(true);
                        CircularAnimationUtils.pulseKeep = false;
                        setStatusColor(android.R.color.darker_gray, 10000, 0);
                        setStatusString("OpenWiFi is disabled.");
                        CircularAnimationUtils.stopProgressBar();
                        break;
                    case WiFiScanner.STATE_SCANNING:
                        if(onoff != null) onoff.setEnabled(true);
                        setStatusString("Running scan...");
                        setStatusColor(android.R.color.holo_blue_dark, 100, 50);
                        CircularAnimationUtils.pulseKeep = false;
                        CircularAnimationUtils.stopProgressBar();
                        break;
                    case WiFiScanner.STATE_TIMEOUT:
                        if(onoff != null) onoff.setEnabled(true);
                        setStatusColor(android.R.color.holo_blue_bright, 100, 100);
                        setStatusString("Scanning...");
                        CircularAnimationUtils.pulseKeep = false;
                        CircularAnimationUtils.fillProgressbar(Global.Timeout, holo);
                        break;
                    case WiFiScanner.STATE_TESTING:
                        if(onoff != null) onoff.setEnabled(true);
                        setStatusColor(android.R.color.holo_orange_light, 200, 100);
                        setStatusString("Testing internet availability....");
                        CircularAnimationUtils.pulseKeep = true;
                        CircularAnimationUtils.pulseDuration = 200;
                        CircularAnimationUtils.pulse(holo);
                        CircularAnimationUtils.stopProgressBar();
                }
                break;
        }
    }

    String TAG = "OW_ACTIVITY";
    WiFiScanner scanner = new WiFiScanner();

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "MainActivity destroyed! Destroying scanner....");
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

        switch (res) {
            case android.R.color.darker_gray:
            case R.color.holo_yellow_light:
                return;
            default:
                break;
        }

        updateNotification();
    }

    void setStatusString(String statusString) {
        Log.i(TAG, "*** Status changed to '"+statusString+"' ***");
        this.statusString = statusString;
        TextView textView = (TextView) findViewById(R.id.textView);
        textView.setText(statusString);
        updateNotification();
    }

    boolean foreground = false;
    boolean justPaused = false;

    @Override
    protected void onPause() {
        super.onPause();
        foreground = false;
        justPaused = true;
        updateNotification();
    }

    @Override
    protected void onResume() {
        super.onResume();
        foreground = true;
        updateNotification();
    }

    void updateNotification() {
        if(foreground) {
            notificationManager.cancel(0xDEADBEEF);
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext())
        .setSmallIcon(R.drawable.openwifi_notification)
        .setContentTitle("OpenWiFi")
        .setContentText(statusString)
        .setLights(statusColor, onMs, offMs)
        .setOngoing(true);
        if(!justPaused) builder.setTicker(statusString);

        Intent turnOffIntent  = new Intent(this, MainActivity.class);
        turnOffIntent.putExtra("me.jariz.openwifi.disable", true);
        PendingIntent turnOffPendingIntent = PendingIntent.getActivity(this.getApplicationContext(), 0xDEADDEAD, turnOffIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Disable OpenWiFi", turnOffPendingIntent);

        Intent resultIntent = new Intent(this, MainActivity.class);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this.getApplicationContext(), 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);

        //special circumstances
        switch (Global.State) {
            case WiFiScanner.STATE_DESTROYED:
                notificationManager.cancel(0xDEADBEEF);
                return;
            case WiFiScanner.STATE_CONNECTED:
                if(justPaused) break;

                if(sharedPreferences.getBoolean("sound", true)) {
                    RingtoneManager ringtoneManager = new RingtoneManager(this);
                    builder.setSound(ringtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
                }

                if(sharedPreferences.getBoolean("vibration", true)) {
                    builder.setVibrate(new long[]{-1, 100, 50, 100, 50, 100, 50, 50, 50, 50});
                }
                break;
            case WiFiScanner.STATE_TIMEOUT:
                builder.setProgress(0, 0, true);
                break;
        }
        justPaused = false;
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
