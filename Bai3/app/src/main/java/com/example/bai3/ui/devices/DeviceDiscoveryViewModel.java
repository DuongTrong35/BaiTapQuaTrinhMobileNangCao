package com.example.bai3.ui.devices;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.bai3.LGRemoteProApp;
import com.example.bai3.model.TVCapabilities;
import com.example.bai3.model.TVDevice;
import com.example.bai3.network.LGTVClient;
import com.example.bai3.network.NetworkScanner;
import com.example.bai3.repository.DeviceRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel for device discovery screen.
 * Manages SSDP scanning, device list, and connection initiation.
 */
public class DeviceDiscoveryViewModel extends AndroidViewModel {

    private final DeviceRepository deviceRepository;
    private final NetworkScanner scanner;
    private final LGTVClient tvClient;
    private final ExecutorService executor;

    private final MutableLiveData<List<TVDevice>> discoveredDevices = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isScanning = new MutableLiveData<>(false);
    private final MutableLiveData<String> scanError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> pairingRequired = new MutableLiveData<>(false);

    public DeviceDiscoveryViewModel(@NonNull Application application) {
        super(application);
        deviceRepository = new DeviceRepository(application);
        scanner = new NetworkScanner(application);
        tvClient = ((LGRemoteProApp) application).getTvClient();
        executor = Executors.newCachedThreadPool();
    }

    // ── LiveData Getters ─────────────────────────────────────

    public LiveData<List<TVDevice>> getDiscoveredDevices() {
        return discoveredDevices;
    }

    public LiveData<List<TVDevice>> getSavedDevices() {
        return deviceRepository.getAllDevices();
    }

    public LiveData<Boolean> getIsScanning() {
        return isScanning;
    }

    public LiveData<String> getScanError() {
        return scanError;
    }

    public LiveData<LGTVClient.ConnectionState> getConnectionState() {
        return tvClient.getConnectionState();
    }

    public LiveData<Boolean> getPairingRequired() {
        return pairingRequired;
    }

    // ── Actions ──────────────────────────────────────────────

    /**
     * Start scanning for LG TVs on the local network.
     */
    public void startScan() {
        isScanning.postValue(true);
        scanError.postValue(null);

        executor.execute(() -> {
            scanner.scan(new NetworkScanner.ScanCallback() {
                @Override
                public void onDeviceFound(TVDevice device) {
                    List<TVDevice> current = discoveredDevices.getValue();
                    if (current == null) current = new ArrayList<>();
                    // Check for duplicates
                    boolean exists = false;
                    for (TVDevice d : current) {
                        if (d.getIpAddress().equals(device.getIpAddress())) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        current.add(device);
                        discoveredDevices.postValue(new ArrayList<>(current));
                    }
                }

                @Override
                public void onScanComplete(List<TVDevice> devices) {
                    isScanning.postValue(false);
                }

                @Override
                public void onScanError(String error) {
                    isScanning.postValue(false);
                    scanError.postValue(error);
                }
            });
        });
    }

    /**
     * Stop the current scan.
     */
    public void stopScan() {
        scanner.stopScan();
        isScanning.postValue(false);
    }

    /**
     * Connect to a discovered or saved TV device.
     */
    public void connectToDevice(TVDevice device) {
        // Save device to database
        deviceRepository.insertDevice(device, id -> {
            device.setId(id);
            // Set up client callback
            tvClient.setCallback(new LGTVClient.ClientCallback() {
                @Override
                public void onConnectionStateChanged(LGTVClient.ConnectionState state) {
                    if (state == LGTVClient.ConnectionState.CONNECTED) {
                        deviceRepository.updateLastConnected(device.getId());
                        deviceRepository.setLastDeviceId(device.getId());
                    }
                }

                @Override
                public void onPairingRequired() {
                    pairingRequired.postValue(true);
                }

                @Override
                public void onPaired(String clientKey) {
                    pairingRequired.postValue(false);
                    device.setClientKey(clientKey);
                    device.setPaired(true);
                    deviceRepository.savePairingKey(device.getId(), clientKey);
                }

                @Override
                public void onCapabilitiesLoaded(TVCapabilities capabilities) {
                    // Handled by RemoteViewModel
                }

                @Override
                public void onVolumeChanged(int volume, boolean muted) {
                    // Handled by RemoteViewModel
                }

                @Override
                public void onCommandResponse(String id, boolean success, String errorMessage) {
                    // Handled by RemoteViewModel
                }

                @Override
                public void onError(String message) {
                    scanError.postValue(message);
                }
            });

            // Initiate connection
            tvClient.connect(device);
        });
    }

    /**
     * Add a device manually by IP address.
     */
    public void addManualDevice(String ipAddress) {
        TVDevice device = new TVDevice("LG TV", ipAddress);
        connectToDevice(device);
    }

    /**
     * Forget (unpair + delete) a device.
     */
    public void forgetDevice(TVDevice device) {
        deviceRepository.deleteDevice(device);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
