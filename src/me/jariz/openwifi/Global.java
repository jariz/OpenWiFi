package me.jariz.openwifi;

import android.net.wifi.WifiManager;
import me.jariz.openwifi.scanner.WiFiScanner;

/**
 * JARIZ.PRO
 * Created @ 11/08/13
 * By: JariZ
 * Project: OpenWiFi
 * Package: me.jariz.openwifi
 */

/**
 * Static versions of WiFiScanner and MainActivity so ScanService can access them
 */
public class Global {
    public static MainActivity mainActivity;
    public static WiFiScanner wiFiScanner;
    public static WifiManager wifiManager;
}
