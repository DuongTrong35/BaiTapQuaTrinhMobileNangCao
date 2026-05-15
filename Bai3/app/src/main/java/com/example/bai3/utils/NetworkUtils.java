package com.example.bai3.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.net.InetAddress;
import java.util.regex.Pattern;

/**
 * Network utility methods for IP validation, connectivity checks,
 * Wi-Fi info retrieval, and Wake-on-LAN packet building.
 */
public final class NetworkUtils {

    private static final String TAG = Constants.LOG_TAG;

    private static final Pattern IP_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$"
    );

    private NetworkUtils() {
        // Utility class
    }

    /**
     * Validate an IPv4 address string.
     */
    public static boolean isValidIpAddress(String ip) {
        if (ip == null || ip.isEmpty()) return false;
        return IP_PATTERN.matcher(ip).matches();
    }

    /**
     * Check if the device is connected to a Wi-Fi network.
     */
    public static boolean isWifiConnected(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;

            Network activeNetwork = cm.getActiveNetwork();
            if (activeNetwork == null) return false;

            NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
            return caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        } catch (Exception e) {
            Log.e(TAG, "Error checking Wi-Fi state: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get the device's current Wi-Fi IP address as a string.
     */
    public static String getWifiIpAddress(Context context) {
        try {
            WifiManager wifiManager = (WifiManager)
                    context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager == null) return null;

            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int ipInt = wifiInfo.getIpAddress();
            if (ipInt == 0) return null;

            return String.format("%d.%d.%d.%d",
                    (ipInt & 0xFF),
                    (ipInt >> 8 & 0xFF),
                    (ipInt >> 16 & 0xFF),
                    (ipInt >> 24 & 0xFF));
        } catch (Exception e) {
            Log.e(TAG, "Error getting Wi-Fi IP: " + e.getMessage());
            return null;
        }
    }

    /**
     * Check if a host is reachable on the network.
     * Must be called on a background thread.
     *
     * @param ip      Target IP address
     * @param timeout Timeout in milliseconds
     * @return true if reachable
     */
    public static boolean isHostReachable(String ip, int timeout) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            return address.isReachable(timeout);
        } catch (Exception e) {
            Log.w(TAG, "Host not reachable: " + ip);
            return false;
        }
    }

    /**
     * Get the SSID of the currently connected Wi-Fi network.
     */
    public static String getWifiSsid(Context context) {
        try {
            WifiManager wifiManager = (WifiManager)
                    context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager == null) return null;

            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            String ssid = wifiInfo.getSSID();
            // Remove surrounding quotes
            if (ssid != null && ssid.startsWith("\"") && ssid.endsWith("\"")) {
                ssid = ssid.substring(1, ssid.length() - 1);
            }
            return ssid;
        } catch (Exception e) {
            Log.e(TAG, "Error getting SSID: " + e.getMessage());
            return null;
        }
    }
}
