package com.example.bai3.network;

import com.google.gson.JsonObject;
import com.example.bai3.utils.Constants;

import java.util.UUID;

/**
 * Static factory for building WebOS SSAP JSON command payloads.
 * Every command follows the format:
 * { "type": "request", "id": "<uuid>", "uri": "ssap://...", "payload": {...} }
 */
public final class CommandBuilder {

    private CommandBuilder() {
        // Utility class — no instantiation
    }

    /**
     * Generate a unique command ID with a descriptive prefix.
     */
    private static String generateId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Build a generic request with no payload.
     */
    public static String buildRequest(String uri) {
        JsonObject cmd = new JsonObject();
        cmd.addProperty("type", "request");
        cmd.addProperty("id", generateId("cmd"));
        cmd.addProperty("uri", uri);
        return cmd.toString();
    }

    /**
     * Build a generic request with a JSON payload.
     */
    public static String buildRequest(String uri, JsonObject payload) {
        JsonObject cmd = new JsonObject();
        cmd.addProperty("type", "request");
        cmd.addProperty("id", generateId("cmd"));
        cmd.addProperty("uri", uri);
        cmd.add("payload", payload);
        return cmd.toString();
    }

    /**
     * Build a subscription request.
     */
    public static String buildSubscription(String uri) {
        JsonObject cmd = new JsonObject();
        cmd.addProperty("type", "subscribe");
        cmd.addProperty("id", generateId("sub"));
        cmd.addProperty("uri", uri);
        return cmd.toString();
    }

    // ── Registration ─────────────────────────────────────────

    /**
     * Build the WebOS registration/pairing command.
     * If clientKey is null, it's a first-time pairing; otherwise it's a reconnect.
     */
    public static String buildRegister(String clientKey) {
        JsonObject cmd = new JsonObject();
        cmd.addProperty("type", "register");
        cmd.addProperty("id", "reg0"); // Nên dùng fixed id "reg0"

        JsonObject signed = new JsonObject();
        signed.addProperty("created", "20140509");
        signed.addProperty("appId", "com.example.bai3");
        signed.addProperty("vendorId", "com.example");

        JsonObject localizedAppNames = new JsonObject();
        localizedAppNames.addProperty("", "MyApp");
        signed.add("localizedAppNames", localizedAppNames);

        JsonObject localizedVendorNames = new JsonObject();
        localizedVendorNames.addProperty("", "MyCompany");
        signed.add("localizedVendorNames", localizedVendorNames);

        JsonObject manifest = new JsonObject();
        manifest.addProperty("manifestVersion", 1);
        manifest.addProperty("appVersion", "1.1");
        manifest.add("signed", signed); // ✅ dùng .add() với JsonObject

        com.google.gson.JsonArray perms = new com.google.gson.JsonArray();
        perms.add("LAUNCH");
        perms.add("CONTROL_AUDIO");
        perms.add("CONTROL_DISPLAY");
        perms.add("CONTROL_INPUT_JOYSTICK");
        perms.add("CONTROL_INPUT_MEDIA_PLAYBACK");
        perms.add("CONTROL_INPUT_TV");
        perms.add("CONTROL_POWER");
        perms.add("READ_INSTALLED_APPS");
        perms.add("CONTROL_MOUSE_AND_KEYBOARD");
        perms.add("WRITE_NOTIFICATION_TOAST");
        manifest.add("permissions", perms);

        JsonObject payload = new JsonObject();
        payload.addProperty("forcePairing", false);
        payload.addProperty("pairingType", "PROMPT");
        payload.add("manifest", manifest);

        if (clientKey != null && !clientKey.isEmpty()) {
            payload.addProperty("client-key", clientKey);
        }

        cmd.add("payload", payload);
        return cmd.toString();
    }

    // ── Power ────────────────────────────────────────────────

    public static String buildTurnOff() {
        return buildRequest(Constants.URI_TURN_OFF);
    }

    // ── Volume ───────────────────────────────────────────────

    public static String buildVolumeUp() {
        return buildRequest(Constants.URI_VOLUME_UP);
    }

    public static String buildVolumeDown() {
        return buildRequest(Constants.URI_VOLUME_DOWN);
    }

    public static String buildSetVolume(int volume) {
        JsonObject payload = new JsonObject();
        payload.addProperty("volume", volume);
        return buildRequest(Constants.URI_SET_VOLUME, payload);
    }

    public static String buildSetMute(boolean mute) {
        JsonObject payload = new JsonObject();
        payload.addProperty("mute", mute);
        return buildRequest(Constants.URI_SET_MUTE, payload);
    }

    public static String buildGetAudioStatus() {
        return buildSubscription(Constants.URI_GET_AUDIO_STATUS);
    }

    // ── Input ────────────────────────────────────────────────

    public static String buildSwitchInput(String inputId) {
        JsonObject payload = new JsonObject();
        payload.addProperty("inputId", inputId);
        return buildRequest(Constants.URI_SWITCH_INPUT, payload);
    }

    public static String buildGetInputList() {
        return buildRequest(Constants.URI_GET_INPUT_LIST);
    }

    // ── Navigation Keys ──────────────────────────────────────

    public static String buildSendKey(String keyCode) {
        JsonObject payload = new JsonObject();
        payload.addProperty("keycode", keyCode);
        return buildRequest(Constants.URI_SEND_KEY, payload);
    }

    public static String buildEnter() {
        return buildRequest(Constants.URI_SEND_ENTER);
    }

    public static String buildBack() {
        return buildSendKey(Constants.KEY_BACK);
    }

    public static String buildHome() {
        return buildSendKey(Constants.KEY_HOME);
    }

    public static String buildUp() {
        return buildSendKey(Constants.KEY_UP);
    }

    public static String buildDown() {
        return buildSendKey(Constants.KEY_DOWN);
    }

    public static String buildLeft() {
        return buildSendKey(Constants.KEY_LEFT);
    }

    public static String buildRight() {
        return buildSendKey(Constants.KEY_RIGHT);
    }

    // ── Channels ─────────────────────────────────────────────

    public static String buildChannelUp() {
        return buildRequest(Constants.URI_CHANNEL_UP);
    }

    public static String buildChannelDown() {
        return buildRequest(Constants.URI_CHANNEL_DOWN);
    }

    public static String buildGetChannelList() {
        return buildRequest(Constants.URI_GET_CHANNEL_LIST);
    }

    public static String buildSetChannel(String channelId) {
        JsonObject payload = new JsonObject();
        payload.addProperty("channelId", channelId);
        return buildRequest(Constants.URI_SET_CHANNEL, payload);
    }

    // ── Media Playback ───────────────────────────────────────

    public static String buildPlay() {
        return buildRequest(Constants.URI_PLAY);
    }

    public static String buildPause() {
        return buildRequest(Constants.URI_PAUSE);
    }

    public static String buildStop() {
        return buildRequest(Constants.URI_STOP);
    }

    public static String buildRewind() {
        return buildRequest(Constants.URI_REWIND);
    }

    public static String buildFastForward() {
        return buildRequest(Constants.URI_FAST_FORWARD);
    }

    // ── Apps ─────────────────────────────────────────────────

    public static String buildLaunchApp(String appId) {
        JsonObject payload = new JsonObject();
        payload.addProperty("id", appId);
        return buildRequest(Constants.URI_LAUNCH_APP, payload);
    }

    public static String buildListApps() {
        return buildRequest(Constants.URI_LIST_APPS);
    }

    public static String buildLaunchNetflix() {
        return buildLaunchApp(Constants.APP_NETFLIX);
    }

    public static String buildLaunchYouTube() {
        return buildLaunchApp(Constants.APP_YOUTUBE);
    }

    public static String buildLaunchPrimeVideo() {
        return buildLaunchApp(Constants.APP_PRIME_VIDEO);
    }

    public static String buildLaunchDisneyPlus() {
        return buildLaunchApp(Constants.APP_DISNEY_PLUS);
    }

    public static String buildLaunchLGStore() {
        return buildLaunchApp(Constants.APP_LG_STORE);
    }

    public static String buildOpenTvSettings() {
        return buildLaunchApp(Constants.APP_TV_SETTINGS);
    }

    public static String buildOpenInputPicker() {
        return buildLaunchApp(Constants.APP_INPUT_PICKER);
    }

    // ── Text Input ───────────────────────────────────────────

    public static String buildInsertText(String text) {
        JsonObject payload = new JsonObject();
        payload.addProperty("text", text);
        payload.addProperty("replace", false);
        return buildRequest(Constants.URI_INSERT_TEXT, payload);
    }

    public static String buildDeleteCharacters(int count) {
        JsonObject payload = new JsonObject();
        payload.addProperty("count", count);
        return buildRequest(Constants.URI_DELETE_CHARS, payload);
    }

    // ── Toast Notification on TV ─────────────────────────────

    public static String buildShowToast(String message) {
        JsonObject payload = new JsonObject();
        payload.addProperty("message", message);
        return buildRequest(Constants.URI_CREATE_TOAST, payload);
    }

    // ── Capability Queries ───────────────────────────────────

    public static String buildGetServiceList() {
        return buildRequest(Constants.URI_GET_SERVICE_LIST);
    }

    public static String buildGetPointerInput() {
        return buildRequest(Constants.URI_POINTER_INPUT);
    }

    // ── Number Input (via channel set) ───────────────────────

    public static String buildNumberKey(int number) {
        // Send number as a keycode: 0-9
        JsonObject payload = new JsonObject();
        payload.addProperty("keycode", String.valueOf(number));
        return buildRequest(Constants.URI_SEND_KEY, payload);
    }
}
