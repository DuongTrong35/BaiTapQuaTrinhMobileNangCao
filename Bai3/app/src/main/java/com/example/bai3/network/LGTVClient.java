package com.example.bai3.network;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.example.bai3.model.TVCapabilities;
import com.example.bai3.model.TVDevice;
import com.example.bai3.utils.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * High-level WebOS TV client managing pairing, command dispatch,
 * capability detection, and status subscriptions.
 * Wraps LGWebOSSocket with business logic.
 */
public class LGTVClient implements LGWebOSSocket.WebSocketCallback {

    private static final String TAG = Constants.LOG_TAG;

    /**
     * Connection state machine.
     */
    public enum ConnectionState {
        DISCONNECTED,
        CONNECTING,
        PAIRING,
        CONNECTED,
        ERROR
    }

    /**
     * Callback for client events.
     */
    public interface ClientCallback {
        void onConnectionStateChanged(ConnectionState state);
        void onPairingRequired();
        void onPaired(String clientKey);
        void onCapabilitiesLoaded(TVCapabilities capabilities);
        void onVolumeChanged(int volume, boolean muted);
        void onCommandResponse(String id, boolean success, String errorMessage);
        void onError(String message);
    }

    private final LGWebOSSocket socket;
    private final LGPointerSocket pointerSocket;
    private final ExecutorService executor;
    private final Gson gson;

    private TVDevice currentDevice;
    private String clientKey;
    private ClientCallback clientCallback;
    private TVCapabilities capabilities;

    // Track command failure counts per URI for auto-dimming
    private final Map<String, Integer> commandFailCounts = new HashMap<>();
    private volatile boolean awaitingConnectResult = false;
    private volatile boolean hasConnectedSuccessfully = false;
    private volatile boolean manualDisconnectRequested = false;

    // LiveData for observable connection state
    private final MutableLiveData<ConnectionState> connectionState = new MutableLiveData<>(ConnectionState.DISCONNECTED);
    private final MutableLiveData<TVCapabilities> capabilitiesLiveData = new MutableLiveData<>();
    private final MutableLiveData<int[]> volumeState = new MutableLiveData<>(); // [volume, muted(0/1)]

    public LGTVClient() {
        socket = new LGWebOSSocket();
        socket.setCallback(this);
        pointerSocket = new LGPointerSocket();
        executor = Executors.newSingleThreadExecutor();
        gson = new Gson();
        capabilities = new TVCapabilities();
    }

    // ── Public API ───────────────────────────────────────────

    public void setCallback(ClientCallback callback) {
        this.clientCallback = callback;
    }

    public LiveData<ConnectionState> getConnectionState() {
        return connectionState;
    }

    public LiveData<TVCapabilities> getCapabilities() {
        return capabilitiesLiveData;
    }

    public LiveData<int[]> getVolumeState() {
        return volumeState;
    }

    public TVDevice getCurrentDevice() {
        return currentDevice;
    }

    /**
     * Connect to a TV device. Initiates WebSocket connection and pairing.
     */
    public void connect(TVDevice device) {
        currentDevice = device;
        clientKey = device.getClientKey();
        awaitingConnectResult = true;
        hasConnectedSuccessfully = false;
        manualDisconnectRequested = false;
        pointerSocket.disconnect();
        connectionState.postValue(ConnectionState.CONNECTING);
        capabilities = new TVCapabilities();
        capabilitiesLiveData.postValue(capabilities);
        commandFailCounts.clear();
        Log.i(TAG, "[CONNECT] Sending connect request to LG TV: " + getCurrentDeviceLog());

        executor.execute(() -> socket.connect(device.getIpAddress()));
    }

    /**
     * Disconnect from the current TV.
     */
    public void disconnect() {
        executor.execute(() -> {
            manualDisconnectRequested = true;
            socket.disconnect();
            pointerSocket.disconnect();
            connectionState.postValue(ConnectionState.DISCONNECTED);
            currentDevice = null;
            capabilities = new TVCapabilities();
        });
    }

    /**
     * Send a command string to the TV.
     */
    public void sendCommand(String command) {
        executor.execute(() -> {
            if (!socket.isConnected()) {
                Log.w(TAG, "Cannot send command — not connected");
                if (clientCallback != null) {
                    clientCallback.onError("Không thể gửi lệnh — chưa kết nối");
                }
                return;
            }
            if (sendViaPointerIfNeeded(command)) {
                return;
            }
            boolean sent = socket.send(command);
            if (!sent) {
                Log.w(TAG, "Failed to enqueue command");
            }
        });
    }

    /**
     * Check if currently connected.
     */
    public boolean isConnected() {
        return socket.isConnected() && connectionState.getValue() == ConnectionState.CONNECTED;
    }

    // ── WebSocket Callbacks ──────────────────────────────────

    @Override
    public void onConnected() {
        Log.d(TAG, "WebSocket connected, sending registration for " + getCurrentDeviceLog());
        connectionState.postValue(ConnectionState.PAIRING);

        // Send register command
        String registerCmd = CommandBuilder.buildRegister(clientKey);
        socket.send(registerCmd);
    }

    @Override
    public void onMessage(String text) {
        try {
            JsonObject json = JsonParser.parseString(text).getAsJsonObject();
            String type = json.has("type") ? json.get("type").getAsString() : "";
            String id = json.has("id") ? json.get("id").getAsString() : "";

            switch (type) {
                case "registered":
                    handleRegistered(json);
                    break;
                case "response":
                    handleResponse(id, json);
                    break;
                case "error":
                    handleError(id, json);
                    break;
                default:
                    Log.d(TAG, "Unhandled message type: " + type);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing WebSocket message: " + e.getMessage(), e);
        }
    }

    @Override
    public void onDisconnected(int code, String reason) {
        pointerSocket.disconnect();
        boolean initialConnectFailed = awaitingConnectResult && !hasConnectedSuccessfully;
        if (initialConnectFailed) {
            Log.e(TAG, "[CONNECT] Connect request failed for " + getCurrentDeviceLog() +
                    " | code=" + code + ", reason=" + reason);
            awaitingConnectResult = false;
        }

        if (!manualDisconnectRequested && currentDevice != null && hasConnectedSuccessfully && !initialConnectFailed) {
            Log.w(TAG, "Connection dropped (likely TV sleep). Keeping logical connection and auto-reconnect.");
            connectionState.postValue(ConnectionState.CONNECTED);
            if (clientCallback != null) {
                clientCallback.onConnectionStateChanged(ConnectionState.CONNECTED);
            }
            return;
        }

        connectionState.postValue(ConnectionState.DISCONNECTED);
        if (clientCallback != null) {
            clientCallback.onConnectionStateChanged(ConnectionState.DISCONNECTED);
        }
    }

    @Override
    public void onError(Throwable error) {
        Log.e(TAG, "Client error: " + error.getMessage());
        pointerSocket.disconnect();
        boolean initialConnectFailed = awaitingConnectResult && !hasConnectedSuccessfully;
        if (initialConnectFailed) {
            Log.e(TAG, "[CONNECT] Connect request failed for " + getCurrentDeviceLog() +
                    " | error=" + error.getMessage());
            awaitingConnectResult = false;
        }

        if (!manualDisconnectRequested && currentDevice != null && hasConnectedSuccessfully && !initialConnectFailed) {
            Log.w(TAG, "Socket error while session active. Keeping logical connection and auto-reconnect.");
            connectionState.postValue(ConnectionState.CONNECTED);
            if (clientCallback != null) {
                clientCallback.onConnectionStateChanged(ConnectionState.CONNECTED);
            }
            return;
        }

        connectionState.postValue(ConnectionState.ERROR);
        if (clientCallback != null) {
            clientCallback.onError(error.getMessage());
        }
    }

    // ── Message Handlers ─────────────────────────────────────

    private void handleRegistered(JsonObject json) {
        Log.d(TAG, "Registration response received");

        JsonObject payload = json.has("payload") ? json.getAsJsonObject("payload") : null;
        if (payload != null && payload.has("client-key")) {
            String newClientKey = payload.get("client-key").getAsString();
            clientKey = newClientKey;
            hasConnectedSuccessfully = true;
            awaitingConnectResult = false;
            Log.i(TAG, "[CONNECT] Connect request succeeded for " + getCurrentDeviceLog());

            connectionState.postValue(ConnectionState.CONNECTED);

            if (clientCallback != null) {
                clientCallback.onPaired(newClientKey);
                clientCallback.onConnectionStateChanged(ConnectionState.CONNECTED);
            }

            // Query capabilities immediately after pairing
            queryCapabilities();

            // Subscribe to volume changes
            socket.send(CommandBuilder.buildGetAudioStatus());
        } else {
            // Pairing required — TV will show prompt
            Log.i(TAG, "[CONNECT] Pairing confirmation required for " + getCurrentDeviceLog());
            connectionState.postValue(ConnectionState.PAIRING);
            if (clientCallback != null) {
                clientCallback.onPairingRequired();
            }
        }
    }

    private void handleResponse(String id, JsonObject json) {
        JsonObject payload = json.has("payload") ? json.getAsJsonObject("payload") : null;
        boolean returnValue = payload != null && payload.has("returnValue") && payload.get("returnValue").getAsBoolean();

        // Route capability responses
        if (id.startsWith("cap_")) {
            handleCapabilityResponse(id, payload, returnValue);
            return;
        }

        // Handle volume subscription updates
        if (id.startsWith("sub_") && payload != null) {
            handleSubscriptionUpdate(payload);
            return;
        }

        // General command response
        if ("reg0".equals(id) && !returnValue && awaitingConnectResult && !hasConnectedSuccessfully) {
            Log.e(TAG, "[CONNECT] Connect request failed during registration for " +
                    getCurrentDeviceLog() + " | error=" + getErrorMessage(payload));
            awaitingConnectResult = false;
        }

        if (clientCallback != null) {
            clientCallback.onCommandResponse(id, returnValue,
                    returnValue ? null : getErrorMessage(payload));
        }

        // Reset fail count on success
        if (returnValue) {
            // Try to extract the URI from the response if possible
            String uri = extractUriFromId(id);
            if (uri != null) {
                commandFailCounts.remove(uri);
            }
        }
    }

    private void handleError(String id, JsonObject json) {
        String errorText = json.has("error") ? json.get("error").getAsString() : "Unknown error";
        Log.w(TAG, "Command error [" + id + "]: " + errorText);
        if ("reg0".equals(id) && awaitingConnectResult && !hasConnectedSuccessfully) {
            Log.e(TAG, "[CONNECT] Connect request failed during registration for " +
                    getCurrentDeviceLog() + " | error=" + errorText);
            awaitingConnectResult = false;
        }

        if (clientCallback != null) {
            clientCallback.onCommandResponse(id, false, errorText);
        }
    }

    /**
     * Track command failures and auto-disable capabilities after threshold.
     */
    public void trackCommandFailure(String uri) {
        int count = commandFailCounts.getOrDefault(uri, 0) + 1;
        commandFailCounts.put(uri, count);

        if (count >= Constants.COMMAND_FAIL_THRESHOLD) {
            Log.w(TAG, "Command not supported (failed " + count + "x): " + uri);
            disableCapabilityForUri(uri);
            capabilitiesLiveData.postValue(capabilities);
        }
    }

    // ── Capability Detection ─────────────────────────────────

    /**
     * Send all capability query commands to determine what the TV supports.
     */
    private void queryCapabilities() {
        Log.d(TAG, "Querying TV capabilities...");

        // Use specific IDs so we can route responses
        sendCapabilityQuery("cap_services", Constants.URI_GET_SERVICE_LIST);
        sendCapabilityQuery("cap_apps", Constants.URI_LIST_APPS);
        sendCapabilityQuery("cap_inputs", Constants.URI_GET_INPUT_LIST);
        sendCapabilityQuery("cap_pointer", Constants.URI_POINTER_INPUT);
        sendCapabilityQuery("cap_audio", Constants.URI_GET_AUDIO_STATUS);
        sendCapabilityQuery("cap_channels", Constants.URI_GET_CHANNEL_LIST);
    }

    private void sendCapabilityQuery(String id, String uri) {
        JsonObject cmd = new JsonObject();
        cmd.addProperty("type", "request");
        cmd.addProperty("id", id);
        cmd.addProperty("uri", uri);
        socket.send(cmd.toString());
    }

    private void handleCapabilityResponse(String id, JsonObject payload, boolean returnValue) {
        Log.d(TAG, "Capability response: " + id + " success=" + returnValue);

        switch (id) {
            case "cap_services":
                // Service list — used for general capability detection
                if (returnValue && payload != null && payload.has("services")) {
                    // Services available — details can be used for fine-grained detection
                    Log.d(TAG, "Service list received");
                }
                break;

            case "cap_apps":
                if (returnValue && payload != null && payload.has("apps")) {
                    JsonArray apps = payload.getAsJsonArray("apps");
                    capabilities.hasNetflix = false;
                    capabilities.hasYouTube = false;
                    capabilities.hasPrimeVideo = false;
                    capabilities.hasDisneyPlus = false;
                    capabilities.hasLGContentStore = false;

                    for (JsonElement el : apps) {
                        if (el.isJsonObject()) {
                            JsonObject appObj = el.getAsJsonObject();
                            String appId = appObj.has("id") ? appObj.get("id").getAsString() : "";
                            String normalizedAppId = appId.toLowerCase();
                            String appTitle = extractAppTitle(appObj);

                            if (normalizedAppId.contains("netflix")) capabilities.hasNetflix = true;
                            if (normalizedAppId.contains("youtube")) capabilities.hasYouTube = true;
                            if (isPrimeVideoApp(normalizedAppId, appTitle))
                                capabilities.hasPrimeVideo = true;
                            if (normalizedAppId.contains("disney")) capabilities.hasDisneyPlus = true;
                            if (normalizedAppId.contains("discovery") || normalizedAppId.contains("lgstore"))
                                capabilities.hasLGContentStore = true;
                        }
                    }
                }
                break;

            case "cap_inputs":
                if (returnValue && payload != null && payload.has("devices")) {
                    capabilities.hasInputSelection = true;
                    capabilities.availableInputIds = new ArrayList<>();
                    JsonArray inputs = payload.getAsJsonArray("devices");
                    for (JsonElement el : inputs) {
                        if (el.isJsonObject() && el.getAsJsonObject().has("id")) {
                            capabilities.availableInputIds.add(
                                    el.getAsJsonObject().get("id").getAsString());
                        }
                    }
                } else {
                    capabilities.hasInputSelection = false;
                }
                break;

            case "cap_pointer":
                capabilities.hasPointerControl = returnValue;
                if (returnValue && payload != null) {
                    connectPointerSocketIfAvailable(payload);
                }
                break;

            case "cap_audio":
                if (returnValue && payload != null) {
                    capabilities.hasVolumeControl = true;
                    capabilities.hasMuteControl = true;
                    // Also extract current volume
                    if (payload.has("volume")) {
                        int vol = payload.get("volume").getAsInt();
                        boolean muted = payload.has("mute") && payload.get("mute").getAsBoolean();
                        volumeState.postValue(new int[]{vol, muted ? 1 : 0});
                    }
                } else {
                    capabilities.hasVolumeControl = false;
                    capabilities.hasMuteControl = false;
                }
                break;

            case "cap_channels":
                capabilities.hasChannelControl = returnValue;
                break;
        }

        // Check if all capabilities have been received
        // (simplified: mark as loaded after processing any cap response)
        capabilities.loaded = true;
        capabilitiesLiveData.postValue(capabilities);

        if (clientCallback != null) {
            clientCallback.onCapabilitiesLoaded(capabilities);
        }
    }

    private void handleSubscriptionUpdate(JsonObject payload) {
        if (payload.has("volume")) {
            int vol = payload.get("volume").getAsInt();
            boolean muted = payload.has("mute") && payload.get("mute").getAsBoolean();
            volumeState.postValue(new int[]{vol, muted ? 1 : 0});
            if (clientCallback != null) {
                clientCallback.onVolumeChanged(vol, muted);
            }
        }
    }

    private void disableCapabilityForUri(String uri) {
        if (uri == null) return;
        switch (uri) {
            case Constants.URI_VOLUME_UP:
            case Constants.URI_VOLUME_DOWN:
            case Constants.URI_SET_VOLUME:
                capabilities.hasVolumeControl = false;
                break;
            case Constants.URI_SET_MUTE:
                capabilities.hasMuteControl = false;
                break;
            case Constants.URI_CHANNEL_UP:
            case Constants.URI_CHANNEL_DOWN:
            case Constants.URI_GET_CHANNEL_LIST:
                capabilities.hasChannelControl = false;
                break;
            case Constants.URI_SWITCH_INPUT:
                capabilities.hasInputSelection = false;
                break;
            case Constants.URI_POINTER_INPUT:
                capabilities.hasPointerControl = false;
                break;
            case Constants.URI_PLAY:
            case Constants.URI_PAUSE:
            case Constants.URI_STOP:
            case Constants.URI_REWIND:
            case Constants.URI_FAST_FORWARD:
                capabilities.hasMediaPlayback = false;
                break;
        }
    }

    private String getErrorMessage(JsonObject payload) {
        if (payload != null && payload.has("errorText")) {
            return payload.get("errorText").getAsString();
        }
        return "Unknown error";
    }

    private String extractUriFromId(String id) {
        // IDs are formatted as "prefix_uuid" — we can't reliably extract the URI
        // This would need a mapping table; for now return null
        return null;
    }

    private String getCurrentDeviceLog() {
        if (currentDevice == null) return "name=unknown, ip=unknown";

        String name = currentDevice.getName();
        String ip = currentDevice.getIpAddress();
        return "name=" + (name == null || name.trim().isEmpty() ? "unknown" : name.trim()) +
                ", ip=" + (ip == null || ip.trim().isEmpty() ? "unknown" : ip.trim());
    }

    private boolean sendViaPointerIfNeeded(String command) {
        if (command == null || command.trim().isEmpty()) {
            return false;
        }

        try {
            JsonObject cmd = JsonParser.parseString(command).getAsJsonObject();
            if (!cmd.has("uri")) {
                return false;
            }

            String uri = cmd.get("uri").getAsString();

            if (Constants.URI_SEND_ENTER.equals(uri)) {
                boolean sent = pointerSocket.sendClick();
                if (!sent) {
                    Log.w(TAG, "Pointer socket unavailable for ENTER, fallback to SSAP request");
                }
                return sent;
            }

            if (!Constants.URI_SEND_KEY.equals(uri)) {
                return false;
            }

            JsonObject payload = cmd.has("payload") ? cmd.getAsJsonObject("payload") : null;
            if (payload == null || !payload.has("keycode")) {
                return false;
            }

            String keyCode = payload.get("keycode").getAsString();
            boolean sent = pointerSocket.sendButton(keyCode);
            if (!sent) {
                Log.w(TAG, "Pointer socket unavailable for key " + keyCode + ", fallback to SSAP request");
            }
            return sent;
        } catch (IllegalStateException e) {
            Log.w(TAG, "Invalid command JSON (state): " + e.getMessage());
            return false;
        } catch (ClassCastException e) {
            Log.w(TAG, "Invalid command JSON (type): " + e.getMessage());
            return false;
        } catch (JsonSyntaxException e) {
            Log.w(TAG, "Invalid command JSON (syntax): " + e.getMessage());
            return false;
        }
    }

    private void connectPointerSocketIfAvailable(JsonObject payload) {
        String socketPath = null;
        if (payload.has("socketPath")) {
            socketPath = payload.get("socketPath").getAsString();
        } else if (payload.has("socketpath")) {
            socketPath = payload.get("socketpath").getAsString();
        }

        if (socketPath == null || socketPath.trim().isEmpty()) {
            Log.w(TAG, "Pointer capability response missing socketPath");
            capabilities.hasPointerControl = false;
            return;
        }

        pointerSocket.connect(socketPath);
        capabilities.hasPointerControl = true;
    }

    private String extractAppTitle(JsonObject appObj) {
        if (appObj == null) return "";

        if (appObj.has("title")) {
            return appObj.get("title").getAsString().toLowerCase();
        }
        if (appObj.has("name")) {
            return appObj.get("name").getAsString().toLowerCase();
        }
        if (appObj.has("launchPointTitle")) {
            return appObj.get("launchPointTitle").getAsString().toLowerCase();
        }
        return "";
    }

    private boolean isPrimeVideoApp(String normalizedAppId, String appTitle) {
        if (normalizedAppId == null) normalizedAppId = "";
        if (appTitle == null) appTitle = "";

        return normalizedAppId.equals(Constants.APP_PRIME_VIDEO.toLowerCase()) ||
                normalizedAppId.contains("amazonvideo") ||
                normalizedAppId.contains("amazon.video") ||
                normalizedAppId.contains("amazon.ignition") ||
                normalizedAppId.contains("primevideo") ||
                normalizedAppId.contains("prime.video") ||
                appTitle.contains("prime video");
    }

    public boolean wakeCurrentDevice() {
        TVDevice device = currentDevice;
        if (device == null) {
            if (clientCallback != null) {
                clientCallback.onError("Chưa có TV để gửi Wake-on-LAN");
            }
            return false;
        }

        String mac = device.getMacAddress();
        if (mac == null || mac.trim().isEmpty()) {
            if (clientCallback != null) {
                clientCallback.onError("Không có MAC address để bật TV khi ngủ/tắt");
            }
            return false;
        }

        NetworkScanner.sendWakeOnLan(mac);
        manualDisconnectRequested = false;
        connectionState.postValue(ConnectionState.CONNECTED);
        if (clientCallback != null) {
            clientCallback.onConnectionStateChanged(ConnectionState.CONNECTED);
        }
        return true;
    }
}
