package me.jariz.openwifi;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.*;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import de.passsy.holocircularprogressbar.HoloCircularProgressBar;

import java.util.Iterator;
import java.util.List;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        holo = (HoloCircularProgressBar) findViewById(R.id.holoCircularProgressBar1);
        wifiManager = (WifiManager) this.getSystemService(Activity.WIFI_SERVICE);

        setStatusString("OpenWiFi is disabled");
        setStatusColor(android.R.color.darker_gray);
    }

    @Override
    protected void onPause() {
        super.onResume();
        unregisterReceiver(wifiStatusReceiver);
        unregisterReceiver(wifiScanReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        wifiStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                StateCheck();

                //state changed, so should we restart scanning again?
                if (!scanning && canScan()) {
                    ScanTask task = new ScanTask();
                    task.execute(0);
                }
            }
        };
        IntentFilter filter = new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        this.registerReceiver(wifiStatusReceiver, filter);

        wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(!enabled) return;
                processWiFis(wifiManager.getScanResults());
                StateCheck();
                //start new 'scan'
                if (canScan()) {
                    ScanTask task = new ScanTask();
                    task.execute(0);
                }
            }
        };
        filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        this.registerReceiver(wifiScanReceiver, filter);
    }

    String TAG = "OPENWIFI";
    BroadcastReceiver wifiStatusReceiver;
    BroadcastReceiver wifiScanReceiver;
    boolean enabled = false;
    boolean scanning = false;
    float timeout = 10f;
    int timeout_mil = 10000;

    void toggle() {
        enabled = !enabled;
        //make sure onoff switch is also correct

        onoff.setChecked(enabled);
    }

    void Disable() {
        toggle();
        Log.i(TAG, "OpenWiFi disabling...");
        if (wifiManager.isWifiEnabled()) wifiManager.setWifiEnabled(false);

        setStatusString("OpenWiFi is disabled");
        setStatusColor(android.R.color.darker_gray);
        CircularAnimationUtils.pulseKeep = false;
    }

    void Enable() {
        toggle();
        Log.i(TAG, "OpenWiFi enabling...");
        if (!wifiManager.isWifiEnabled()) wifiManager.setWifiEnabled(true);



        //start scannin'
        if (canScan()) {
            ScanTask task = new ScanTask();
            task.execute(0);
        }

        //we don't change the status, our receiver should take care of that
    }

    /**
     * This function checks what state to switch to (connected/scanning etc)
     * and updates the UI for the state, respectively.
     */
    void StateCheck() {
        if (!enabled) return;

        WifiInfo info = wifiManager.getConnectionInfo();
        SupplicantState state = info.getSupplicantState();
        if (!wifiManager.isWifiEnabled()) {
            Disable();
        } else if (state == SupplicantState.COMPLETED) {
            CircularAnimationUtils.pulseKeep = true;
            CircularAnimationUtils.pulse(0, holo, 700);
            setStatusColor(android.R.color.holo_green_light);
            setStatusString("You are connected to '" + info.getSSID() + "'");
        } else if (state == SupplicantState.ASSOCIATING || state == SupplicantState.AUTHENTICATING || state == SupplicantState.GROUP_HANDSHAKE || state == SupplicantState.FOUR_WAY_HANDSHAKE) {
            CircularAnimationUtils.pulseKeep = true;
            CircularAnimationUtils.pulse(0, holo, 400);
            setStatusColor(R.color.holo_yellow_light);
            setStatusString("Connecting to '" + info.getSSID() + "'...");
        } else if (scanning) {
            CircularAnimationUtils.pulseKeep = false;
            setStatusColor(android.R.color.holo_blue_light);
            setStatusString("Scanning...");
        } else if (state == SupplicantState.SCANNING) {
            CircularAnimationUtils.pulseKeep = true;
            CircularAnimationUtils.pulse(0, holo, 400);
            setStatusColor(android.R.color.holo_blue_bright);
            setStatusString("Running scan...");
        } else {
            Log.wtf(TAG, "Ermmm, supplicant state is " + state.toString() + " but i'm not sure what that is. maybe add it to the program? GUI not updated accordingly.");
        }
    }

    void StateCheckThreadSafe() {
        runOnUiThread(new Thread(new Runnable() {
            @Override
            public void run() {
                StateCheck();
            }
        }));
    }

    /**
     * Can we scan (isn't the wifimanager doing something else)
     */
    boolean canScan() {
        return wifiManager.getConnectionInfo().getSupplicantState() == SupplicantState.SCANNING;
    }

    /**
     * Function that will pick out the right networks and connect to them
     */
    void processWiFis(List<ScanResult> results) {
        Iterator<ScanResult> iterator = results.iterator();
        while(iterator.hasNext()) {
            ScanResult result = iterator.next();
            if(getScanResultSecurity(result) == OPEN) {

                WifiConfiguration wc = new WifiConfiguration();
                wc.BSSID = result.BSSID;
                wc.SSID = "\""+result.SSID+"\"";
                wc.hiddenSSID = true;
                wc.status = WifiConfiguration.Status.ENABLED;
                wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);


                //first check if network already exists, if yes, connect to it.
                List<WifiConfiguration> configurationList = wifiManager.getConfiguredNetworks();
                Iterator<WifiConfiguration> configurationIterator = configurationList.iterator();
                boolean found = false;
                while(configurationIterator.hasNext()) {
                    WifiConfiguration configuration = configurationIterator.next();
                    if(configuration.SSID == result.SSID) {
                        //match! update & enable
                        wc.networkId = configuration.networkId;
                        int id = wifiManager.updateNetwork(wc);
                        wifiManager.enableNetwork(id, true);
                        Log.i(TAG, "Updated & enabled existing network '"+wc.SSID+"' ("+wc.BSSID+"')");
                        found = true;
                        break;
                    }
                }

                //not found, insert & enable
                if(!found) {
                    int id = wifiManager.addNetwork(wc);
                    wifiManager.enableNetwork(id, true);

                    Log.i(TAG, "Inserted & enabled new network '"+wc.SSID+"' ("+wc.BSSID+"')");
                }

                wifiManager.saveConfiguration();
                wifiManager.reconnect();
                wifiManager.reassociate();
            }
        }
    }

    //Stolen from Settings.apk
    public static final String PSK = "PSK";
    public static final String WEP = "WEP";
    public static final String EAP = "EAP";
    public static final String OPEN = "Open";
    public static String getScanResultSecurity(ScanResult scanResult) {
        final String cap = scanResult.capabilities;
        final String[] securityModes = { WEP, PSK, EAP };
        for (int i = securityModes.length - 1; i >= 0; i--) {
            if (cap.contains(securityModes[i])) {
                return securityModes[i];
            }
        }

        return OPEN;
    }

    void setStatusColor(int res) {
        holo.setColor(getResources().getColor(res));
        holo.invalidate();
    }

    void setStatusString(String statusString) {
        TextView textView = (TextView) findViewById(R.id.textView);
        textView.setText(statusString);
    }

    HoloCircularProgressBar holo;
    WifiManager wifiManager;
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
                if (b) Enable();
                else Disable();
            }
        });
        return true;
    }

    /**
     * ScanTask doesn't do the actual scan process, just the timeout, the scan is async eitherway.
     */
    class ScanTask extends AsyncTask<Integer, Float, Object> {

        @Override
        protected Object doInBackground(Integer... params) {
            scanning = true;
            publishProgress(timeout);
            try {
                Thread.sleep(timeout_mil);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            scanning = false;
            wifiManager.startScan();
            StateCheckThreadSafe();
            return null;
        }

        @Override
        protected void onProgressUpdate(Float... value) {
            CircularAnimationUtils.fillProgressbar(timeout_mil, holo);
        }
    }
}
