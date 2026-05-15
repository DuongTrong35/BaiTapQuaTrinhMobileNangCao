package com.example.bai3.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room entity representing a discovered / paired LG TV device.
 * Stores connection info and pairing credentials for auto-reconnect.
 */
@Entity(tableName = "tv_devices")
public class TVDevice {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @NonNull
    private String name;

    private String model;

    @NonNull
    private String ipAddress;

    /** WebOS client key received after successful pairing. Null if not yet paired. */
    private String clientKey;

    /** MAC address for Wake-on-LAN. Null if unavailable. */
    private String macAddress;

    /** Timestamp of last successful connection (epoch millis). */
    private long lastConnected;

    /** Whether this device has been explicitly paired by the user. */
    private boolean paired;

    // ── Constructors ─────────────────────────────────────────

    public TVDevice(@NonNull String name, @NonNull String ipAddress) {
        this.name = name;
        this.ipAddress = ipAddress;
        this.paired = false;
        this.lastConnected = 0;
    }

    // ── Getters & Setters ────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    @NonNull
    public String getName() { return name; }
    public void setName(@NonNull String name) { this.name = name; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    @NonNull
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(@NonNull String ipAddress) { this.ipAddress = ipAddress; }

    public String getClientKey() { return clientKey; }
    public void setClientKey(String clientKey) { this.clientKey = clientKey; }

    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }

    public long getLastConnected() { return lastConnected; }
    public void setLastConnected(long lastConnected) { this.lastConnected = lastConnected; }

    public boolean isPaired() { return paired; }
    public void setPaired(boolean paired) { this.paired = paired; }

    // ── Utility ──────────────────────────────────────────────

    /**
     * Returns a display-friendly summary: "TV Name (192.168.x.x)"
     */
    @NonNull
    @Override
    public String toString() {
        return name + " (" + ipAddress + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TVDevice device = (TVDevice) o;
        return ipAddress.equals(device.ipAddress);
    }

    @Override
    public int hashCode() {
        return ipAddress.hashCode();
    }
}
