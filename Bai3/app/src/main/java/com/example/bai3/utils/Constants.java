package com.example.bai3.utils;

/**
 * Central constants for the LG Remote Pro application.
 * Contains all SSAP URIs, WebSocket config, SSDP parameters,
 * SharedPreferences keys, and app-wide defaults.
 */
public final class Constants {

    private Constants() {
        // Utility class — no instantiation
    }

    // ── App Info ──────────────────────────────────────────────
    public static final String LOG_TAG = "LGRemotePro";
    public static final String APP_NAME = "LG Remote Pro";

    // ── WebSocket Configuration ──────────────────────────────
    public static final int WEBOS_PORT = 3000;
    public static final int WEBOS_SECURE_PORT = 3001;
    public static final String WEBOS_SCHEME = "ws://";
    public static final String WEBOS_SECURE_SCHEME = "wss://";
    public static final long HEARTBEAT_INTERVAL_MS = 30_000L;
    public static final long RECONNECT_INITIAL_DELAY_MS = 1_000L;
    public static final long RECONNECT_MAX_DELAY_MS = 30_000L;
    public static final long CONNECT_TIMEOUT_MS = 10_000L;
    public static final long READ_TIMEOUT_MS = 0L; // No read timeout for WebSocket
    public static final long WRITE_TIMEOUT_MS = 5_000L;

    // ── SSDP Discovery ───────────────────────────────────────
    public static final String SSDP_ADDRESS = "239.255.255.250";
    public static final int SSDP_PORT = 1900;
    public static final int SSDP_SCAN_TIMEOUT_MS = 5000;
    public static final String SSDP_SEARCH_TARGET = "ssdp:all";
    public static final String SSDP_SEARCH_MESSAGE =
            "M-SEARCH * HTTP/1.1\r\n" +
            "HOST: 239.255.255.250:1900\r\n" +
            "MAN: \"ssdp:discover\"\r\n" +
            "MX: 3\r\n" +
            "ST: " + SSDP_SEARCH_TARGET + "\r\n\r\n";

    // ── Wake-on-LAN ──────────────────────────────────────────
    public static final int WOL_PORT = 9;
    public static final String WOL_BROADCAST = "255.255.255.255";

    // ── WebOS SSAP URIs ──────────────────────────────────────
    public static final String URI_TURN_OFF = "ssap://system/turnOff";
    public static final String URI_SET_VOLUME = "ssap://audio/setVolume";
    public static final String URI_VOLUME_UP = "ssap://audio/volumeUp";
    public static final String URI_VOLUME_DOWN = "ssap://audio/volumeDown";
    public static final String URI_SET_MUTE = "ssap://audio/setMute";
    public static final String URI_GET_VOLUME = "ssap://audio/getVolume";
    public static final String URI_GET_AUDIO_STATUS = "ssap://audio/getStatus";
    public static final String URI_SWITCH_INPUT = "ssap://tv/switchInput";
    public static final String URI_GET_INPUT_LIST = "ssap://tv/getExternalInputList";
    public static final String URI_SEND_ENTER = "ssap://com.webos.service.ime/sendEnterKey";
    public static final String URI_DELETE_CHARS = "ssap://com.webos.service.ime/deleteCharacters";
    public static final String URI_INSERT_TEXT = "ssap://com.webos.service.ime/insertText";
    public static final String URI_CREATE_TOAST = "ssap://system.notifications/createToast";
    public static final String URI_LAUNCH_APP = "ssap://com.webos.applicationManager/launch";
    public static final String URI_LIST_APPS = "ssap://com.webos.applicationManager/listApps";
    public static final String URI_GET_CHANNEL_LIST = "ssap://tv/getChannelList";
    public static final String URI_SET_CHANNEL = "ssap://tv/setChannel";
    public static final String URI_CHANNEL_UP = "ssap://tv/channelUp";
    public static final String URI_CHANNEL_DOWN = "ssap://tv/channelDown";
    public static final String URI_GET_CURRENT_CHANNEL = "ssap://tv/getCurrentChannel";
    public static final String URI_POINTER_INPUT = "ssap://com.webos.service.networkinput/getPointerInputSocket";
    public static final String URI_SEND_KEY = "ssap://com.webos.service.ime/sendKeycode";
    public static final String URI_GET_SERVICE_LIST = "ssap://api/getServiceList";
    public static final String URI_PLAY = "ssap://media.controls/play";
    public static final String URI_PAUSE = "ssap://media.controls/pause";
    public static final String URI_STOP = "ssap://media.controls/stop";
    public static final String URI_REWIND = "ssap://media.controls/rewind";
    public static final String URI_FAST_FORWARD = "ssap://media.controls/fastForward";

    // ── App IDs (for launch) ─────────────────────────────────
    public static final String APP_NETFLIX = "netflix";
    public static final String APP_YOUTUBE = "youtube.leanback.v4";
    public static final String APP_PRIME_VIDEO = "amazon";
    public static final String APP_DISNEY_PLUS = "com.disney.disneyplus-prod";
    public static final String APP_LG_STORE = "com.webos.app.discovery";
    public static final String APP_TV_SETTINGS = "com.palm.app.settings";
    public static final String APP_INPUT_PICKER = "com.webos.app.inputpicker";

    // ── Key Codes ────────────────────────────────────────────
    public static final String KEY_UP = "UP";
    public static final String KEY_DOWN = "DOWN";
    public static final String KEY_LEFT = "LEFT";
    public static final String KEY_RIGHT = "RIGHT";
    public static final String KEY_HOME = "HOME";
    public static final String KEY_BACK = "BACK";
    public static final String KEY_RED = "RED";
    public static final String KEY_GREEN = "GREEN";
    public static final String KEY_YELLOW = "YELLOW";
    public static final String KEY_BLUE = "BLUE";
    public static final String KEY_PLAY = "PLAY";
    public static final String KEY_PAUSE = "PAUSE";
    public static final String KEY_STOP = "STOP";
    public static final String KEY_REWIND = "REWIND";
    public static final String KEY_FAST_FORWARD = "FASTFORWARD";
    public static final String KEY_CHANNEL_UP = "CHANNELUP";
    public static final String KEY_CHANNEL_DOWN = "CHANNELDOWN";

    // ── SharedPreferences Keys ───────────────────────────────
    public static final String PREFS_NAME = "lg_remote_pro_prefs";
    public static final String PREF_ONBOARDING_COMPLETE = "onboarding_complete";
    public static final String PREF_LAST_DEVICE_ID = "last_device_id";
    public static final String PREF_HAPTIC_ENABLED = "haptic_enabled";
    public static final String PREF_COMPACT_LAYOUT = "compact_layout";
    public static final String PREF_THEME_MODE = "theme_mode";
    public static final String PREF_DISCONNECT_ALERT = "disconnect_alert";

    // ── D-Pad Long-Press ─────────────────────────────────────
    public static final long DPAD_REPEAT_INTERVAL_MS = 300L;

    // ── Haptic Durations ─────────────────────────────────────
    public static final long HAPTIC_BUTTON_MS = 15L;
    public static final long HAPTIC_ERROR_MS = 50L;

    // ── Animation Durations ──────────────────────────────────
    public static final long ANIM_LAUNCH_DURATION_MS = 350L;
    public static final long ANIM_BUTTON_PRESS_MS = 150L;
    public static final long ANIM_FLASH_MS = 200L;
    public static final long ANIM_CAPABILITY_FADE_MS = 250L;
    public static final long ANIM_PULSE_DURATION_MS = 1000L;
    public static final float BUTTON_SCALE_PRESSED = 0.92f;
    public static final float ALPHA_DIMMED = 0.35f;
    public static final float ALPHA_LOADING = 0.5f;
    public static final float ALPHA_FULL = 1.0f;

    // ── Command Failure Threshold ────────────────────────────
    public static final int COMMAND_FAIL_THRESHOLD = 2;

    // ── Room Database ────────────────────────────────────────
    public static final String DATABASE_NAME = "lg_remote_pro_db";
    public static final int DATABASE_VERSION = 1;

    // ── Command History ──────────────────────────────────────
    public static final int MAX_COMMAND_HISTORY = 10;
}
