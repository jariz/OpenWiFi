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
 * Global openwifi objects accessible from anywhere
 */
public class Global {
    public static MainActivity mainActivity;
    public static int State = -1;
    public static int Timeout = 10000;
    public static WifiManager wifiManager;
}
