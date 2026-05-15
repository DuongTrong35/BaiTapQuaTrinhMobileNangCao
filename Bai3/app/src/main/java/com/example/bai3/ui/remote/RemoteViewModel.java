package com.example.bai3.ui.remote;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.bai3.LGRemoteProApp;
import com.example.bai3.model.RemoteCommand;
import com.example.bai3.model.TVCapabilities;
import com.example.bai3.model.TVDevice;
import com.example.bai3.network.LGTVClient;
import com.example.bai3.repository.CommandRepository;

import java.util.List;

/**
 * ViewModel for the main remote control screen.
 * Manages connection state, TV capabilities, volume, command dispatch, and history.
 */
public class RemoteViewModel extends AndroidViewModel {

    private final LGTVClient tvClient;
    private final CommandRepository commandRepository;

    private final MutableLiveData<List<RemoteCommand>> commandHistory = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isMuted = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> currentVolume = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> touchpadEnabled = new MutableLiveData<>(false);
    private int inputCycleIndex = 0;

    public RemoteViewModel(@NonNull Application application) {
        super(application);
        tvClient = ((LGRemoteProApp) application).getTvClient();
        commandRepository = new CommandRepository(tvClient);
    }

    // ── LiveData Getters ─────────────────────────────────────

    public LiveData<LGTVClient.ConnectionState> getConnectionState() {
        return tvClient.getConnectionState();
    }

    public LiveData<TVCapabilities> getCapabilities() {
        return tvClient.getCapabilities();
    }

    public LiveData<int[]> getVolumeState() {
        return tvClient.getVolumeState();
    }

    public LiveData<List<RemoteCommand>> getCommandHistory() {
        return commandHistory;
    }

    public LiveData<Boolean> getIsMuted() {
        return isMuted;
    }

    public LiveData<Integer> getCurrentVolume() {
        return currentVolume;
    }

    public LiveData<Boolean> getTouchpadEnabled() {
        return touchpadEnabled;
    }

    public TVDevice getCurrentDevice() {
        return tvClient.getCurrentDevice();
    }

    // ── Remote Control Actions ───────────────────────────────

    // Power
    public void powerOff() {
        if (tvClient.isConnected()) {
            commandRepository.sendPowerOff();
            refreshHistory();
        } else {
            tvClient.wakeCurrentDevice();
        }
    }

    // Navigation
    public void navigateUp() {
        commandRepository.sendUp();
        refreshHistory();
    }

    public void navigateDown() {
        commandRepository.sendDown();
        refreshHistory();
    }

    public void navigateLeft() {
        commandRepository.sendLeft();
        refreshHistory();
    }

    public void navigateRight() {
        commandRepository.sendRight();
        refreshHistory();
    }

    public void pressOk() {
        commandRepository.sendOk();
        refreshHistory();
    }

    public void pressBack() {
        commandRepository.sendBack();
        refreshHistory();
    }

    public void pressHome() {
        commandRepository.sendHome();
        refreshHistory();
    }

    // Volume & Mute
    public void volumeUp() {
        commandRepository.sendVolumeUp();
        refreshHistory();
    }

    public void volumeDown() {
        commandRepository.sendVolumeDown();
        refreshHistory();
    }

    public void toggleMute() {
        Boolean muted = isMuted.getValue();
        boolean newMute = muted == null || !muted;
        commandRepository.sendMute(newMute);
        isMuted.postValue(newMute);
        refreshHistory();
    }

    // Channels
    public void channelUp() {
        commandRepository.sendChannelUp();
        refreshHistory();
    }

    public void channelDown() {
        commandRepository.sendChannelDown();
        refreshHistory();
    }

    public void pressNumber(int number) {
        commandRepository.sendNumberKey(number);
        refreshHistory();
    }

    // Media
    public void play() {
        commandRepository.sendPlay();
        refreshHistory();
    }

    public void pause() {
        commandRepository.sendPause();
        refreshHistory();
    }

    public void stop() {
        commandRepository.sendStop();
        refreshHistory();
    }

    public void rewind() {
        commandRepository.sendRewind();
        refreshHistory();
    }

    public void fastForward() {
        commandRepository.sendFastForward();
        refreshHistory();
    }

    // Apps
    public void launchNetflix() {
        commandRepository.sendLaunchNetflix();
        refreshHistory();
    }

    public void launchYouTube() {
        commandRepository.sendLaunchYouTube();
        refreshHistory();
    }

    public void launchPrimeVideo() {
        commandRepository.sendLaunchPrimeVideo();
        refreshHistory();
    }

    public void launchDisneyPlus() {
        commandRepository.sendLaunchDisneyPlus();
        refreshHistory();
    }

    public void launchLGStore() {
        commandRepository.sendLaunchLGStore();
        refreshHistory();
    }

    // Input
    public void switchInput(String inputId) {
        commandRepository.sendSwitchInput(inputId);
        refreshHistory();
    }

    public void cycleInput() {
        TVCapabilities caps = tvClient.getCapabilities().getValue();
        if (caps == null || caps.availableInputIds == null || caps.availableInputIds.isEmpty()) {
            commandRepository.sendOpenInputPicker();
            refreshHistory();
            return;
        }

        if (inputCycleIndex >= caps.availableInputIds.size()) {
            inputCycleIndex = 0;
        }

        String inputId = caps.availableInputIds.get(inputCycleIndex);
        inputCycleIndex = (inputCycleIndex + 1) % caps.availableInputIds.size();
        commandRepository.sendSwitchInput(inputId);
        refreshHistory();
    }

    public void openTvSettings() {
        commandRepository.sendOpenTvSettings();
        refreshHistory();
    }

    // Text
    public void insertText(String text) {
        commandRepository.sendInsertText(text);
    }

    // Touchpad
    public void setTouchpadEnabled(boolean enabled) {
        touchpadEnabled.postValue(enabled);
    }

    // History
    public void replayCommand(RemoteCommand command) {
        commandRepository.replayCommand(command);
        refreshHistory();
    }

    private void refreshHistory() {
        commandHistory.postValue(commandRepository.getCommandHistory());
    }

    /**
     * Update volume/mute from subscription data.
     */
    public void updateVolumeFromSubscription(int volume, boolean muted) {
        currentVolume.postValue(volume);
        isMuted.postValue(muted);
    }
}
