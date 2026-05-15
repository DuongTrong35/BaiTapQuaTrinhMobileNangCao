package com.example.bai3.model;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory model representing what the currently connected TV supports.
 * Populated by capability queries immediately after WebSocket pairing.
 * Not a Room entity — lives only for the duration of the connection session.
 */
public class TVCapabilities {

    public boolean hasVolumeControl;
    public boolean hasMuteControl;
    public boolean hasChannelControl;
    public boolean hasInputSelection;
    public boolean hasPointerControl;
    public boolean hasMediaPlayback;
    public boolean hasScreenShare;
    public boolean hasNetflix;
    public boolean hasYouTube;
    public boolean hasPrimeVideo;
    public boolean hasDisneyPlus;
    public boolean hasLGContentStore;
    public boolean hasVoiceInput;
    public boolean hasRecordButton;
    public List<String> availableInputIds;

    /** Whether capability detection has completed. */
    public boolean loaded;

    public TVCapabilities() {
        // Default: assume everything is supported until proven otherwise
        hasVolumeControl = true;
        hasMuteControl = true;
        hasChannelControl = true;
        hasInputSelection = true;
        hasPointerControl = false; // Must be confirmed
        hasMediaPlayback = true;
        hasScreenShare = false;
        hasNetflix = false;
        hasYouTube = false;
        hasPrimeVideo = false;
        hasDisneyPlus = false;
        hasLGContentStore = false;
        hasVoiceInput = true;
        hasRecordButton = false;
        availableInputIds = new ArrayList<>();
        loaded = false;
    }

    /**
     * Check if a specific app is available on this TV.
     */
    public boolean hasApp(String appId) {
        if (appId == null) return false;
        switch (appId.toLowerCase()) {
            case "netflix":
                return hasNetflix;
            case "youtube.leanback.v4":
            case "youtube":
                return hasYouTube;
            case "amazon":
                return hasPrimeVideo;
            case "com.disney.disneyplus-prod":
                return hasDisneyPlus;
            case "com.webos.app.discovery":
                return hasLGContentStore;
            default:
                return false;
        }
    }

    /**
     * Check if a specific input source is available.
     */
    public boolean hasInput(String inputId) {
        if (inputId == null || availableInputIds == null) return false;
        return availableInputIds.contains(inputId);
    }
}
