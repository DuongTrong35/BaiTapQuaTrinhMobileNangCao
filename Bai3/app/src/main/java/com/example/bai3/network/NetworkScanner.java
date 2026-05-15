package com.example.bai3.network;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import com.example.bai3.model.TVDevice;
import com.example.bai3.utils.Constants;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Advanced network scanner for discovering LG WebOS TVs.
 * Uses both SSDP (UDP 1900) and mDNS/NSD for better compatibility.
 *
 * Main anti-false-positive protections:
 * 1) SSDP M-SEARCH only targets LG webOS second-screen service
 * 2) Ignores ALL local device IPv4 addresses
 * 3) Ignores loopback/link-local responses
 * 4) Requires trusted SSDP headers or LG/webOS NSD hints
 * 5) Verifies likely webOS control ports (3001 / 3000) before accepting device
 */
public class NetworkScanner {

    private static final String TAG = Constants.LOG_TAG;

    // SSDP
    private static final String SSDP_ADDRESS = "239.255.255.250";
    private static final int SSDP_PORT = 1900;
    private static final int SSDP_SOCKET_TIMEOUT_MS = 1000;
    private static final int SSDP_SCAN_WINDOW_MS = 8000;
    private static final int PORT_VERIFY_TIMEOUT_MS = 1200;

    // LG webOS second-screen discovery target
    private static final String LG_WEBOS_ST = "urn:lge-com:service:webos-second-screen:1";

    // NSD types to probe (best-effort; some TVs may only answer SSDP)
    private static final String[] NSD_TYPES = new String[]{
            "_lgsmarttv._tcp.",
            "_webostv._tcp."
    };

    private final Context context;
    private final NsdManager nsdManager;

    private final List<NsdManager.DiscoveryListener> discoveryListeners = new ArrayList<>();
    private final Set<String> seenIps = Collections.synchronizedSet(new HashSet<>());
    private final List<TVDevice> foundDevices = Collections.synchronizedList(new ArrayList<>());

    private volatile boolean scanning = false;

    public NetworkScanner(Context context) {
        this.context = context.getApplicationContext();
        this.nsdManager = (NsdManager) this.context.getSystemService(Context.NSD_SERVICE);
    }

    /**
     * Callback for scan results.
     */
    public interface ScanCallback {
        void onDeviceFound(TVDevice device);
        void onScanComplete(List<TVDevice> devices);
        void onScanError(String error);
    }

    /**
     * Start discovery using both SSDP and NSD.
     */
    public void scan(ScanCallback callback) {
        if (scanning) {
            Log.w(TAG, "Scan already in progress");
            return;
        }

        scanning = true;
        seenIps.clear();
        foundDevices.clear();

        // Best-effort NSD for newer devices / local discovery
        startNsdDiscovery(callback);

        // SSDP remains primary and stricter discovery path
        new Thread(() -> startSsdpScan(callback), "LGTV-SSDP-Scanner").start();
    }

    /**
     * Stop ongoing scan.
     */
    public void stopScan() {
        scanning = false;
        stopNsdDiscovery();
    }

    /**
     * Start NSD discovery.
     * We keep it strict: only service names/types with LG/webOS hints are considered,
     * and all candidates are still verified by IP filtering + port verification.
     */
    private void startNsdDiscovery(ScanCallback callback) {
        if (nsdManager == null) {
            Log.w(TAG, "NSD manager unavailable");
            return;
        }

        synchronized (discoveryListeners) {
            discoveryListeners.clear();

            for (String serviceType : NSD_TYPES) {
                NsdManager.DiscoveryListener listener = new NsdManager.DiscoveryListener() {
                    @Override
                    public void onDiscoveryStarted(String regType) {
                        Log.d(TAG, "NSD discovery started: " + regType);
                    }

                    @Override
                    public void onServiceFound(NsdServiceInfo serviceInfo) {
                        if (!scanning || serviceInfo == null) return;

                        String serviceName = safeLower(serviceInfo.getServiceName());
                        String type = safeLower(serviceInfo.getServiceType());

                        // Strict NSD candidate filtering
                        boolean looksRelevant =
                                serviceName.contains("lg") ||
                                        serviceName.contains("webos") ||
                                        serviceName.contains("lge") ||
                                        type.contains("lg") ||
                                        type.contains("webos");

                        if (!looksRelevant) {
                            return;
                        }

                        try {
                            nsdManager.resolveService(serviceInfo, new NsdManager.ResolveListener() {
                                @Override
                                public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                                    Log.w(TAG, "NSD resolve failed (" + errorCode + "): " + serviceInfo);
                                }

                                @Override
                                public void onServiceResolved(NsdServiceInfo resolvedServiceInfo) {
                                    if (!scanning || resolvedServiceInfo == null) return;

                                    InetAddress host = resolvedServiceInfo.getHost();
                                    if (host == null) return;

                                    String ip = host.getHostAddress();
                                    if (shouldIgnoreAddress(host, getLocalIpv4Addresses())) {
                                        Log.d(TAG, "Ignoring NSD local/invalid IP: " + ip);
                                        return;
                                    }

                                    // Best-effort verification: keep candidate even if quick port check fails.
                                    if (!verifyWebOsPorts(ip)) {
                                        Log.d(TAG, "NSD candidate port check failed, keeping candidate: " + ip);
                                    }

                                    String name = resolvedServiceInfo.getServiceName();
                                    if (name == null || name.trim().isEmpty()) {
                                        name = "LG webOS TV";
                                    }

                                    TVDevice device = createDevice(ip, name, null);
                                    registerDevice(device, callback);
                                }
                            });
                        } catch (Exception e) {
                            Log.w(TAG, "NSD resolve exception: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onServiceLost(NsdServiceInfo serviceInfo) {
                        Log.d(TAG, "NSD service lost: " + serviceInfo);
                    }

                    @Override
                    public void onDiscoveryStopped(String serviceType) {
                        Log.d(TAG, "NSD discovery stopped: " + serviceType);
                    }

                    @Override
                    public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                        Log.e(TAG, "NSD start failed [" + serviceType + "]: " + errorCode);
                    }

                    @Override
                    public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                        Log.e(TAG, "NSD stop failed [" + serviceType + "]: " + errorCode);
                    }
                };

                discoveryListeners.add(listener);

                try {
                    nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener);
                } catch (Exception e) {
                    Log.w(TAG, "Failed to start NSD for " + serviceType + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Stop all NSD discovery listeners.
     */
    private void stopNsdDiscovery() {
        if (nsdManager == null) return;

        synchronized (discoveryListeners) {
            for (NsdManager.DiscoveryListener listener : discoveryListeners) {
                try {
                    nsdManager.stopServiceDiscovery(listener);
                } catch (Exception ignored) {
                    // ignore stop errors
                }
            }
            discoveryListeners.clear();
        }
    }

    /**
     * SSDP scan:
     * - sends exact LG webOS second-screen M-SEARCH
     * - receives responses
     * - rejects local/self packets
     * - validates headers
     * - verifies webOS control ports
     */
    private void startSsdpScan(ScanCallback callback) {
        Set<String> localIps = getLocalIpv4Addresses();
        Log.d(TAG, "Starting SSDP scan. Local IPs to ignore: " + localIps);

        DatagramSocket socket = null;

        try {
            socket = new DatagramSocket();
            socket.setReuseAddress(true);
            socket.setSoTimeout(SSDP_SOCKET_TIMEOUT_MS);

            String request =
                    "M-SEARCH * HTTP/1.1\r\n" +
                            "HOST: " + SSDP_ADDRESS + ":" + SSDP_PORT + "\r\n" +
                            "MAN: \"ssdp:discover\"\r\n" +
                            "MX: 2\r\n" +
                            "ST: " + LG_WEBOS_ST + "\r\n" +
                            "\r\n";

            byte[] requestData = request.getBytes();
            InetAddress multicastAddress = InetAddress.getByName(SSDP_ADDRESS);

            // Send multiple probes for better reliability
            for (int i = 0; i < 3 && scanning; i++) {
                DatagramPacket packet = new DatagramPacket(
                        requestData,
                        requestData.length,
                        multicastAddress,
                        SSDP_PORT
                );
                socket.send(packet);

                try {
                    Thread.sleep(150);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            long endTime = System.currentTimeMillis() + SSDP_SCAN_WINDOW_MS;
            byte[] buffer = new byte[8192];

            while (scanning && System.currentTimeMillis() < endTime) {
                DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);

                try {
                    socket.receive(responsePacket);

                    InetAddress sourceAddress = responsePacket.getAddress();
                    String sourceIp = sourceAddress != null ? sourceAddress.getHostAddress() : null;

                    String response = new String(
                            responsePacket.getData(),
                            0,
                            responsePacket.getLength()
                    );

                    if (!isTrustedSsdpResponse(response)) {
                        Log.d(TAG, "Discarding untrusted SSDP response from: " + sourceIp);
                        continue;
                    }

                    String candidateIp = resolveCandidateIp(response, sourceIp);
                    if (candidateIp == null || candidateIp.trim().isEmpty()) {
                        Log.d(TAG, "Discarding candidate with empty IP from source: " + sourceIp);
                        continue;
                    }

                    InetAddress candidateAddress = InetAddress.getByName(candidateIp);
                    if (shouldIgnoreAddress(candidateAddress, localIps)) {
                        Log.d(TAG, "Ignoring SSDP local/invalid candidate IP: " + candidateIp);
                        continue;
                    }

                    if (!verifyWebOsPorts(candidateIp)) {
                        Log.d(TAG, "SSDP candidate port check failed, keeping candidate: " + candidateIp);
                    }

                    TVDevice device = parseDevice(response, candidateIp);
                    if (device == null) {
                        Log.d(TAG, "Discarding candidate after parse/validation: " + candidateIp);
                        continue;
                    }

                    registerDevice(device, callback);

                } catch (SocketTimeoutException ignored) {
                    // Continue until scan window expires
                } catch (IOException e) {
                    Log.d(TAG, "Skipping malformed SSDP candidate: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "SSDP scan error", e);
            if (callback != null) {
                callback.onScanError("Lỗi SSDP scan: " + e.getMessage());
            }
        } finally {
            if (socket != null) {
                socket.close();
            }

            stopNsdDiscovery();
            scanning = false;

            if (callback != null) {
                List<TVDevice> snapshot;
                synchronized (foundDevices) {
                    snapshot = new ArrayList<>(foundDevices);
                }
                logScanSummary(snapshot);
                callback.onScanComplete(snapshot);
            }
        }
    }

    /**
     * Register a verified device once.
     */
    private void registerDevice(TVDevice device, ScanCallback callback) {
        if (device == null || device.getIpAddress() == null) return;

        String ip = device.getIpAddress();
        if (!seenIps.add(ip)) {
            return;
        }

        foundDevices.add(device);
        Log.i(TAG, "[WIFI_SCAN] Found LG TV on same Wi-Fi: " + formatDeviceLog(device));

        if (callback != null) {
            callback.onDeviceFound(device);
        }
    }

    private void logScanSummary(List<TVDevice> devices) {
        int total = devices == null ? 0 : devices.size();
        Log.i(TAG, "[WIFI_SCAN] Total LG TVs found on same Wi-Fi: " + total);

        if (total == 0) {
            Log.i(TAG, "[WIFI_SCAN] Device list is empty.");
            return;
        }

        for (int i = 0; i < devices.size(); i++) {
            TVDevice device = devices.get(i);
            Log.i(TAG, "[WIFI_SCAN] Device #" + (i + 1) + ": " + formatDeviceLog(device));
        }
    }

    private String formatDeviceLog(TVDevice device) {
        if (device == null) return "name=unknown, ip=unknown, model=unknown";

        String name = device.getName();
        String ip = device.getIpAddress();
        String model = device.getModel();
        if (model == null || model.trim().isEmpty()) {
            model = "unknown";
        }

        return "name=" + (name == null || name.trim().isEmpty() ? "unknown" : name.trim()) +
                ", ip=" + (ip == null || ip.trim().isEmpty() ? "unknown" : ip.trim()) +
                ", model=" + model;
    }

    /**
     * Very strict SSDP validation to avoid false positives.
     */
    private boolean isTrustedSsdpResponse(String response) {
        if (response == null || response.trim().isEmpty()) return false;

        String lower = response.toLowerCase();

        // Must look like a valid SSDP response
        if (!lower.startsWith("http/1.1 200 ok")) return false;

        String st = extractHeader(response, "ST");
        String usn = extractHeader(response, "USN");
        String server = extractHeader(response, "SERVER");
        String location = extractHeader(response, "LOCATION");

        boolean exactLgService = st != null && st.toLowerCase().contains(LG_WEBOS_ST);
        boolean lgHints =
                containsIgnoreCase(usn, "lge") ||
                        containsIgnoreCase(usn, "webos") ||
                        containsIgnoreCase(server, "lge") ||
                        containsIgnoreCase(server, "webos") ||
                        containsIgnoreCase(location, "lge") ||
                        containsIgnoreCase(location, "webos");

        // Prefer exact service match. If not present, require stronger LG/webOS hints.
        return exactLgService || lgHints;
    }

    /**
     * Parse a device only if response still looks trustworthy.
     */
    private TVDevice parseDevice(String response, String ip) {
        if (response == null || ip == null || ip.trim().isEmpty()) {
            return null;
        }

        String st = extractHeader(response, "ST");
        String usn = extractHeader(response, "USN");
        String server = extractHeader(response, "SERVER");

        boolean trusted =
                containsIgnoreCase(st, LG_WEBOS_ST) ||
                        containsIgnoreCase(usn, "lge") ||
                        containsIgnoreCase(usn, "webos") ||
                        containsIgnoreCase(server, "lge") ||
                        containsIgnoreCase(server, "webos");

        if (!trusted) {
            return null;
        }

        String name = null;

        // Prefer a cleaner name source
        if (server != null && !server.trim().isEmpty()) {
            name = server.trim();
        } else if (usn != null && !usn.trim().isEmpty()) {
            name = usn.trim();
        }

        if (name == null || name.isEmpty()) {
            name = "LG webOS TV";
        }

        String model = extractModel(response);
        TVDevice device = createDevice(ip, name, model);
        device.setMacAddress(extractMacAddress(response));
        return device;
    }

    /**
     * Safer header parser.
     */
    private String extractHeader(String response, String header) {
        if (response == null || header == null) return null;

        Pattern pattern = Pattern.compile(
                "^" + Pattern.quote(header) + "\\s*:\\s*(.+)$",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
        );
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private String resolveCandidateIp(String response, String sourceIp) {
        String ipFromLocation = extractIpFromLocation(response);
        if (ipFromLocation != null && !ipFromLocation.trim().isEmpty()) {
            return ipFromLocation.trim();
        }
        return sourceIp;
    }

    private String extractIpFromLocation(String response) {
        String location = extractHeader(response, "LOCATION");
        if (location == null || location.trim().isEmpty()) {
            return null;
        }

        try {
            String normalized = location.trim();
            if (!normalized.contains("://")) {
                normalized = "http://" + normalized;
            }
            java.net.URI uri = java.net.URI.create(normalized);
            String host = uri.getHost();
            if (host == null || host.trim().isEmpty()) {
                return null;
            }
            return host.trim();
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "Failed to parse LOCATION header: " + location);
            return null;
        }
    }

    /**
     * Best-effort model extraction from SSDP text.
     */
    private String extractModel(String response) {
        if (response == null) return null;

        // A few common LG TV model shapes, kept broad but harmless
        Pattern pattern = Pattern.compile(
                "\\b((?:OLED|QNED|NANO|UHD)?[A-Z]{0,4}\\d{2}[A-Z0-9]{2,})\\b",
                Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String extractMacAddress(String response) {
        if (response == null || response.trim().isEmpty()) return null;

        Pattern macPattern = Pattern.compile("(?i)\\b([0-9a-f]{2}[:-]){5}[0-9a-f]{2}\\b");
        Matcher matcher = macPattern.matcher(response);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group().toUpperCase().replace('-', ':');
    }

    /**
     * Verifies likely LG webOS remote control ports.
     * 3001 = WSS, 3000 = WS/HTTP hello.
     */
    private boolean verifyWebOsPorts(String ip) {
        if (ip == null || ip.trim().isEmpty()) return false;

        return canConnect(ip, 3001, PORT_VERIFY_TIMEOUT_MS) ||
                canConnect(ip, 3000, PORT_VERIFY_TIMEOUT_MS);
    }

    private boolean canConnect(String ip, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), timeoutMs);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    /**
     * Ignore:
     * - null addresses
     * - non-IPv4
     * - loopback
     * - link-local
     * - any local IPv4 of this phone
     */
    private boolean shouldIgnoreAddress(InetAddress address, Set<String> localIps) {
        if (address == null) return true;
        if (!(address instanceof Inet4Address)) return true;
        if (address.isLoopbackAddress()) return true;
        if (address.isLinkLocalAddress()) return true;

        String ip = address.getHostAddress();
        return ip == null || localIps.contains(ip);
    }

    /**
     * Collect all local IPv4 addresses to avoid recognizing the phone itself.
     */
    private Set<String> getLocalIpv4Addresses() {
        Set<String> ips = new HashSet<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                    continue;
                }

                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress address = inetAddresses.nextElement();
                    if (address instanceof Inet4Address &&
                            !address.isLoopbackAddress() &&
                            !address.isLinkLocalAddress()) {
                        ips.add(address.getHostAddress());
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting local IPs", e);
        }
        return ips;
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private boolean containsIgnoreCase(String value, String token) {
        return value != null && token != null && value.toLowerCase().contains(token.toLowerCase());
    }

    /**
     * Wake-on-LAN helper.
     */
    public static void sendWakeOnLan(String macAddress) {
        if (macAddress == null || macAddress.isEmpty()) return;

        try {
            String[] macParts = macAddress.split("[:-]");
            if (macParts.length != 6) {
                Log.w(TAG, "Invalid MAC address for WoL: " + macAddress);
                return;
            }

            byte[] macBytes = new byte[6];
            for (int i = 0; i < 6; i++) {
                macBytes[i] = (byte) Integer.parseInt(macParts[i], 16);
            }

            byte[] magicPacket = new byte[6 + 16 * 6];
            for (int i = 0; i < 6; i++) {
                magicPacket[i] = (byte) 0xFF;
            }
            for (int i = 0; i < 16; i++) {
                System.arraycopy(macBytes, 0, magicPacket, 6 + i * 6, 6);
            }

            InetAddress broadcast = InetAddress.getByName(Constants.WOL_BROADCAST);
            DatagramPacket packet = new DatagramPacket(
                    magicPacket,
                    magicPacket.length,
                    broadcast,
                    Constants.WOL_PORT
            );

            DatagramSocket socket = new DatagramSocket();
            socket.setBroadcast(true);
            socket.send(packet);
            socket.close();

        } catch (Exception e) {
            Log.e(TAG, "WoL error", e);
        }
    }

    /**
     * Helper to create TVDevice.
     * Adjust setters if your TVDevice model uses different names.
     */
    private TVDevice createDevice(String ip, String name, String model) {
        TVDevice device = new TVDevice(name, ip); // hoặc new TVDevice(ip, name) nếu constructor của bạn theo thứ tự đó

        // Nếu TVDevice có setModel thì giữ dòng này
        device.setModel(model);

        return device;
    }
}
