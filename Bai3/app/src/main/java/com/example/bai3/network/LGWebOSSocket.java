package com.example.bai3.network;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.bai3.utils.Constants;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * OkHttp WebSocket wrapper with auto-reconnect and exponential backoff.
 * Manages the raw WebSocket lifecycle (connect, send, close, reconnect).
 */
public class LGWebOSSocket {

    private static final String TAG = Constants.LOG_TAG;

    private final OkHttpClient client;
    private WebSocket webSocket;
    private String serverUrl;
    private final List<String> endpointCandidates = new ArrayList<>();
    private int endpointIndex = 0;
    private boolean intentionallyClosed = false;
    private boolean hasConnectedOnce = false;
    private int reconnectAttempts = 0;
    private WebSocketCallback callback;
    private final Object lock = new Object();

    /**
     * Callback interface for WebSocket lifecycle events.
     */
    public interface WebSocketCallback {
        void onConnected();
        void onMessage(String text);
        void onDisconnected(int code, String reason);
        void onError(Throwable error);
    }

    public LGWebOSSocket() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(Constants.CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .writeTimeout(Constants.WRITE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .readTimeout(Constants.READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .pingInterval(Constants.HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);
        configureTlsForWebOs(builder);
        client = builder.build();
    }

    /**
     * Set the callback for WebSocket events.
     */
    public void setCallback(WebSocketCallback callback) {
        this.callback = callback;
    }

    /**
     * Connect to the TV's WebOS WebSocket endpoint.
     * @param ipAddress The TV's IP address (e.g., "192.168.1.100")
     */
    public void connect(String ipAddress) {
        String normalizedIp = ipAddress == null ? null : ipAddress.trim();
        if (normalizedIp == null || normalizedIp.isEmpty()) {
            IllegalArgumentException error =
                    new IllegalArgumentException("Cannot connect: TV IP is null or empty");
            Log.e(TAG, error.getMessage());
            if (callback != null) {
                callback.onError(error);
            }
            return;
        }

        synchronized (lock) {
            initializeEndpointCandidates(normalizedIp);
            intentionallyClosed = false;
            hasConnectedOnce = false;
            reconnectAttempts = 0;
            if (webSocket != null) {
                webSocket.cancel();
                webSocket = null;
            }
            openConnection();
        }
    }

    /**
     * Opens the WebSocket connection to the server.
     */
    private void openConnection() {
        if (serverUrl == null) {
            Log.e(TAG, "Cannot connect: server URL is null");
            return;
        }

        Log.d(TAG, "WebSocket connecting to: " + serverUrl);

        Request request = new Request.Builder()
                .url(serverUrl)
                .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {

            @Override
            public void onOpen(@NonNull WebSocket ws, @NonNull Response response) {
                Log.d(TAG, "WebSocket connected");
                synchronized (lock) {
                    webSocket = ws;
                    hasConnectedOnce = true;
                }
                reconnectAttempts = 0;
                if (callback != null) {
                    callback.onConnected();
                }
            }

            @Override
            public void onMessage(@NonNull WebSocket ws, @NonNull String text) {
                Log.d(TAG, "WebSocket message received: " + text.substring(0, Math.min(text.length(), 200)));
                if (callback != null) {
                    callback.onMessage(text);
                }
            }

            @Override
            public void onClosing(@NonNull WebSocket ws, int code, @NonNull String reason) {
                Log.d(TAG, "WebSocket closing: " + code + " " + reason);
                ws.close(code, reason);
            }

            @Override
            public void onClosed(@NonNull WebSocket ws, int code, @NonNull String reason) {
                Log.d(TAG, "WebSocket closed: " + code + " " + reason);
                synchronized (lock) {
                    webSocket = null;
                }
                if (callback != null) {
                    callback.onDisconnected(code, reason);
                }
                if (!intentionallyClosed) {
                    if (!switchToNextEndpoint()) {
                        scheduleReconnect();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull WebSocket ws, @NonNull Throwable t, @Nullable Response response) {
                Log.e(TAG, "WebSocket failure: " + t.getMessage(), t);
                synchronized (lock) {
                    webSocket = null;
                }
                if (callback != null) {
                    callback.onError(t);
                }
                if (!intentionallyClosed) {
                    if (!switchToNextEndpoint()) {
                        scheduleReconnect();
                    }
                }
            }
        });
    }

    /**
     * Send a text message through the WebSocket.
     * @return true if the message was enqueued, false otherwise
     */
    public boolean send(String message) {
        synchronized (lock) {
            if (webSocket != null) {
                Log.d(TAG, "WebSocket sending: " + message.substring(0, Math.min(message.length(), 100)));
                return webSocket.send(message);
            } else {
                Log.w(TAG, "WebSocket not connected, cannot send message");
                return false;
            }
        }
    }

    /**
     * Gracefully close the WebSocket connection without attempting to reconnect.
     */
    public void disconnect() {
        synchronized (lock) {
            intentionallyClosed = true;
            if (webSocket != null) {
                try {
                    webSocket.close(1000, "User disconnected");
                } catch (Exception e) {
                    Log.w(TAG, "Error closing WebSocket: " + e.getMessage());
                    webSocket.cancel();
                }
                webSocket = null;
            }
        }
    }

    /**
     * Check if the WebSocket is currently connected.
     */
    public boolean isConnected() {
        synchronized (lock) {
            return webSocket != null && !intentionallyClosed;
        }
    }

    /**
     * Schedule a reconnection attempt with exponential backoff.
     * Delays: 1s → 2s → 4s → 8s → 16s → 30s (max)
     */
    private void scheduleReconnect() {
        if (intentionallyClosed) return;

        long delay = Math.min(
                Constants.RECONNECT_INITIAL_DELAY_MS * (long) Math.pow(2, reconnectAttempts),
                Constants.RECONNECT_MAX_DELAY_MS
        );
        reconnectAttempts++;

        Log.d(TAG, "Scheduling reconnect attempt #" + reconnectAttempts + " in " + delay + "ms");

        new Thread(() -> {
            try {
                Thread.sleep(delay);
                if (!intentionallyClosed) {
                    synchronized (lock) {
                        if (!hasConnectedOnce && !endpointCandidates.isEmpty()) {
                            endpointIndex = 0;
                            serverUrl = endpointCandidates.get(0);
                        }
                    }
                    Log.d(TAG, "Attempting reconnect #" + reconnectAttempts);
                    openConnection();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.w(TAG, "Reconnect sleep interrupted");
            }
        }).start();
    }

    /**
     * Force cancel the WebSocket immediately without clean closure.
     */
    public void cancel() {
        synchronized (lock) {
            intentionallyClosed = true;
            if (webSocket != null) {
                webSocket.cancel();
                webSocket = null;
            }
        }
    }

    private void initializeEndpointCandidates(String ipAddress) {
        endpointCandidates.clear();
        endpointIndex = 0;
        addEndpointCandidate(Constants.WEBOS_SECURE_SCHEME, ipAddress, Constants.WEBOS_SECURE_PORT);
        addEndpointCandidate(Constants.WEBOS_SCHEME, ipAddress, Constants.WEBOS_PORT);
        serverUrl = endpointCandidates.get(0);
        Log.d(TAG, "Prepared WebSocket endpoints: " + endpointCandidates);
    }

    private void addEndpointCandidate(String scheme, String ipAddress, int port) {
        String candidate = scheme + ipAddress + ":" + port + "/";
        if (!endpointCandidates.contains(candidate)) {
            endpointCandidates.add(candidate);
        }
    }

    private boolean switchToNextEndpoint() {
        String nextUrl;
        synchronized (lock) {
            if (hasConnectedOnce) {
                return false;
            }
            if (endpointIndex >= endpointCandidates.size() - 1) {
                return false;
            }
            endpointIndex++;
            nextUrl = endpointCandidates.get(endpointIndex);
            serverUrl = nextUrl;
        }
        Log.w(TAG, "Switching to fallback WebSocket endpoint: " + nextUrl);
        openConnection();
        return true;
    }

    private void configureTlsForWebOs(OkHttpClient.Builder builder) {
        try {
            X509TrustManager trustAllManager = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustAllManager}, new SecureRandom());
            builder.sslSocketFactory(sslContext.getSocketFactory(), trustAllManager);

            HostnameVerifier allowAllHosts = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };
            builder.hostnameVerifier(allowAllHosts);
        } catch (GeneralSecurityException e) {
            Log.e(TAG, "Failed to configure TLS for webOS secure socket", e);
        }
    }
}
