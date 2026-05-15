package com.example.bai3.repository;

import com.example.bai3.model.RemoteCommand;
import com.example.bai3.network.CommandBuilder;
import com.example.bai3.network.LGTVClient;
import com.example.bai3.utils.Constants;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Repository for dispatching commands to the TV and managing command history.
 * Maintains a circular buffer of the last N commands for quick-replay.
 */
public class CommandRepository {

    private final LGTVClient tvClient;
    private final LinkedList<RemoteCommand> commandHistory;

    public CommandRepository(LGTVClient tvClient) {
        this.tvClient = tvClient;
        this.commandHistory = new LinkedList<>();
    }

    // ── Command Dispatch ─────────────────────────────────────

    /**
     * Send a raw command string to the TV and add to history.
     */
    public void sendCommand(String command, String displayName, String iconResName) {
        tvClient.sendCommand(command);
        addToHistory(command, displayName, iconResName);
    }

    /**
     * Send a navigation key command.
     */
    public void sendKey(String keyCode, String displayName) {
        String cmd = CommandBuilder.buildSendKey(keyCode);
        sendCommand(cmd, displayName, null);
    }

    // ── Convenience Methods ──────────────────────────────────

    public void sendPowerOff() {
        sendCommand(CommandBuilder.buildTurnOff(), "Tắt nguồn", "ic_power");
    }

    public void sendVolumeUp() {
        sendCommand(CommandBuilder.buildVolumeUp(), "Tăng âm lượng", "ic_volume_up");
    }

    public void sendVolumeDown() {
        sendCommand(CommandBuilder.buildVolumeDown(), "Giảm âm lượng", "ic_volume_down");
    }

    public void sendMute(boolean mute) {
        sendCommand(CommandBuilder.buildSetMute(mute), mute ? "Tắt tiếng" : "Bật tiếng", "ic_mute");
    }

    public void sendUp() {
        sendCommand(CommandBuilder.buildUp(), "Lên", "ic_arrow_up");
    }

    public void sendDown() {
        sendCommand(CommandBuilder.buildDown(), "Xuống", "ic_arrow_down");
    }

    public void sendLeft() {
        sendCommand(CommandBuilder.buildLeft(), "Trái", "ic_arrow_left");
    }

    public void sendRight() {
        sendCommand(CommandBuilder.buildRight(), "Phải", "ic_arrow_right");
    }

    public void sendOk() {
        sendCommand(CommandBuilder.buildEnter(), "OK", null);
    }

    public void sendBack() {
        sendCommand(CommandBuilder.buildBack(), "Quay lại", "ic_back");
    }

    public void sendHome() {
        sendCommand(CommandBuilder.buildHome(), "Trang chủ", "ic_home");
    }

    public void sendChannelUp() {
        sendCommand(CommandBuilder.buildChannelUp(), "Kênh +", null);
    }

    public void sendChannelDown() {
        sendCommand(CommandBuilder.buildChannelDown(), "Kênh -", null);
    }

    public void sendPlay() {
        sendCommand(CommandBuilder.buildPlay(), "Phát", null);
    }

    public void sendPause() {
        sendCommand(CommandBuilder.buildPause(), "Tạm dừng", null);
    }

    public void sendStop() {
        sendCommand(CommandBuilder.buildStop(), "Dừng", null);
    }

    public void sendRewind() {
        sendCommand(CommandBuilder.buildRewind(), "Tua lại", null);
    }

    public void sendFastForward() {
        sendCommand(CommandBuilder.buildFastForward(), "Tua nhanh", null);
    }

    public void sendSwitchInput(String inputId) {
        sendCommand(CommandBuilder.buildSwitchInput(inputId), "Đầu vào: " + inputId, null);
    }

    public void sendOpenInputPicker() {
        sendCommand(CommandBuilder.buildOpenInputPicker(), "Mở danh sách đầu vào", "ic_devices_tab");
    }

    public void sendLaunchNetflix() {
        sendCommand(CommandBuilder.buildLaunchNetflix(), "Netflix", "ic_netflix");
    }

    public void sendLaunchYouTube() {
        sendCommand(CommandBuilder.buildLaunchYouTube(), "YouTube", "ic_youtube");
    }

    public void sendLaunchPrimeVideo() {
        sendCommand(CommandBuilder.buildLaunchPrimeVideo(), "Prime Video", "ic_prime");
    }

    public void sendLaunchDisneyPlus() {
        sendCommand(CommandBuilder.buildLaunchDisneyPlus(), "Disney+", "ic_disney");
    }

    public void sendLaunchLGStore() {
        sendCommand(CommandBuilder.buildLaunchLGStore(), "LG Store", null);
    }

    public void sendOpenTvSettings() {
        sendCommand(CommandBuilder.buildOpenTvSettings(), "Cài đặt TV", "ic_settings_gear");
    }

    public void sendInsertText(String text) {
        sendCommand(CommandBuilder.buildInsertText(text), "Nhập văn bản", null);
    }

    public void sendNumberKey(int number) {
        sendCommand(CommandBuilder.buildNumberKey(number), String.valueOf(number), null);
    }

    // ── Command History ──────────────────────────────────────

    private void addToHistory(String command, String displayName, String iconResName) {
        RemoteCommand entry = new RemoteCommand(
                String.valueOf(System.currentTimeMillis()),
                command,
                displayName,
                iconResName
        );

        synchronized (commandHistory) {
            // Remove duplicate if exists
            commandHistory.removeIf(c -> c.getDisplayName() != null
                    && c.getDisplayName().equals(displayName));
            commandHistory.addFirst(entry);
            // Trim to max size
            while (commandHistory.size() > Constants.MAX_COMMAND_HISTORY) {
                commandHistory.removeLast();
            }
        }
    }

    /**
     * Get the command history as an immutable snapshot.
     */
    public List<RemoteCommand> getCommandHistory() {
        synchronized (commandHistory) {
            return new ArrayList<>(commandHistory);
        }
    }

    /**
     * Replay a command from history.
     */
    public void replayCommand(RemoteCommand command) {
        if (command != null && command.getUri() != null) {
            tvClient.sendCommand(command.getUri());
        }
    }

    /**
     * Clear all command history.
     */
    public void clearHistory() {
        synchronized (commandHistory) {
            commandHistory.clear();
        }
    }
}
