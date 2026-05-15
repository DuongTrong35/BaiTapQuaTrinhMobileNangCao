package com.example.bai3.network;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.bai3.utils.Constants;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
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
 * Secondary socket for webOS pointer/button control.
 * Receives socket URL from getPointerInputSocket and sends button/click events.
 */
public class LGPointerSocket {

    private static final String TAG = Constants.LOG_TAG;

    private final OkHttpClient client;
    private final Object lock = new Object();

    private WebSocket pointerSocket;
    private boolean connected = false;
    private String currentSocketUrl;

    public LGPointerSocket() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(Constants.CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .writeTimeout(Constants.WRITE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .readTimeout(Constants.READ_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        configureTlsForWebOs(builder);
        client = builder.build();
    }

    public void connect(String socketUrl) {
        String normalizedUrl = socketUrl == null ? null : socketUrl.trim();
        if (normalizedUrl == null || normalizedUrl.isEmpty()) {
            Log.w(TAG, "Pointer socket URL is empty");
            return;
        }

        synchronized (lock) {
            if (normalizedUrl.equals(currentSocketUrl) && connected) {
                return;
            }
            disconnectLocked();
            currentSocketUrl = normalizedUrl;
            Log.d(TAG, "Connecting pointer socket: " + normalizedUrl);

            Request request = new Request.Builder().url(normalizedUrl).build();
            pointerSocket = client.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                    synchronized (lock) {
                        pointerSocket = webSocket;
                        connected = true;
                    }
                    Log.d(TAG, "Pointer socket connected");
                }

                @Override
                public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                    synchronized (lock) {
                        connected = false;
                        pointerSocket = null;
                    }
                    Log.d(TAG, "Pointer socket closed: " + code + " " + reason);
                }

                @Override
                public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
                    synchronized (lock) {
                        connected = false;
                        pointerSocket = null;
                    }
                    Log.w(TAG, "Pointer socket failure: " + t.getMessage());
                }
            });
        }
    }

    public boolean sendButton(String buttonName) {
        String normalized = buttonName == null ? null : buttonName.trim();
        if (normalized == null || normalized.isEmpty()) {
            return false;
        }
        return sendRaw("type:button\nname:" + normalized + "\n\n");
    }

    public boolean sendClick() {
        return sendRaw("type:click\n\n");
    }

    public boolean isConnected() {
        synchronized (lock) {
            return connected && pointerSocket != null;
        }
    }

    public void disconnect() {
        synchronized (lock) {
            disconnectLocked();
        }
    }

    private boolean sendRaw(String payload) {
        synchronized (lock) {
            if (!connected || pointerSocket == null) {
                return false;
            }
            return pointerSocket.send(payload);
        }
    }

    private void disconnectLocked() {
        connected = false;
        if (pointerSocket != null) {
            try {
                pointerSocket.close(1000, "Pointer socket close");
            } catch (IllegalStateException e) {
                pointerSocket.cancel();
            }
            pointerSocket = null;
        }
        currentSocketUrl = null;
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
            Log.e(TAG, "Failed to configure TLS for pointer socket", e);
        }
    }
}
