package com.example.bai3.ui.settings;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.bai3.LGRemoteProApp;
import com.example.bai3.model.AppPreferences;
import com.example.bai3.network.LGTVClient;
import com.example.bai3.repository.DeviceRepository;

/**
 * ViewModel for the settings screen.
 * Manages preference state and device management actions.
 */
public class SettingsViewModel extends AndroidViewModel {

    private final AppPreferences prefs;
    private final DeviceRepository deviceRepository;
    private final LGTVClient tvClient;

    private final MutableLiveData<String> themeMode = new MutableLiveData<>();
    private final MutableLiveData<Boolean> hapticEnabled = new MutableLiveData<>();
    private final MutableLiveData<Boolean> compactLayout = new MutableLiveData<>();
    private final MutableLiveData<Boolean> disconnectAlert = new MutableLiveData<>();

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        prefs = AppPreferences.getInstance(application);
        deviceRepository = new DeviceRepository(application);
        tvClient = ((LGRemoteProApp) application).getTvClient();

        // Load initial values
        themeMode.setValue(prefs.getThemeMode());
        hapticEnabled.setValue(prefs.isHapticEnabled());
        compactLayout.setValue(prefs.isCompactLayout());
        disconnectAlert.setValue(prefs.isDisconnectAlertEnabled());
    }

    // ── LiveData Getters ─────────────────────────────────────

    public LiveData<String> getThemeMode() {
        return themeMode;
    }

    public LiveData<Boolean> getHapticEnabled() {
        return hapticEnabled;
    }

    public LiveData<Boolean> getCompactLayout() {
        return compactLayout;
    }

    public LiveData<Boolean> getDisconnectAlert() {
        return disconnectAlert;
    }

    public LiveData<LGTVClient.ConnectionState> getConnectionState() {
        return tvClient.getConnectionState();
    }

    // ── Actions ──────────────────────────────────────────────

    public void setThemeMode(String mode) {
        prefs.setThemeMode(mode);
        themeMode.postValue(mode);
    }

    public void setHapticEnabled(boolean enabled) {
        prefs.setHapticEnabled(enabled);
        hapticEnabled.postValue(enabled);
    }

    public void setCompactLayout(boolean compact) {
        prefs.setCompactLayout(compact);
        compactLayout.postValue(compact);
    }

    public void setDisconnectAlert(boolean enabled) {
        prefs.setDisconnectAlertEnabled(enabled);
        disconnectAlert.postValue(enabled);
    }

    /**
     * Disconnect from the current TV.
     */
    public void disconnectTV() {
        tvClient.disconnect();
    }

    /**
     * Get the app version name.
     */
    public String getAppVersion() {
        try {
            return getApplication().getPackageManager()
                    .getPackageInfo(getApplication().getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "1.0.0";
        }
    }
}
