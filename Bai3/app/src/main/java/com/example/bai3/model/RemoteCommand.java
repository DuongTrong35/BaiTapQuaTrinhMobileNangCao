package com.example.bai3.model;

/**
 * POJO representing a command that was sent to the TV.
 * Used for command history (last N commands as quick-replay chips).
 */
public class RemoteCommand {

    private String id;
    private String uri;
    private String displayName;
    private String iconResName;
    private long timestamp;

    public RemoteCommand() {
    }

    public RemoteCommand(String id, String uri, String displayName, String iconResName) {
        this.id = id;
        this.uri = uri;
        this.displayName = displayName;
        this.iconResName = iconResName;
        this.timestamp = System.currentTimeMillis();
    }

    // ── Getters & Setters ────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUri() { return uri; }
    public void setUri(String uri) { this.uri = uri; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getIconResName() { return iconResName; }
    public void setIconResName(String iconResName) { this.iconResName = iconResName; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RemoteCommand that = (RemoteCommand) o;
        return uri != null && uri.equals(that.uri);
    }

    @Override
    public int hashCode() {
        return uri != null ? uri.hashCode() : 0;
    }
}
