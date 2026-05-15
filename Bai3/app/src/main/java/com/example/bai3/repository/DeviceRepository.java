package com.example.bai3.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.bai3.database.AppDatabase;
import com.example.bai3.database.DeviceDao;
import com.example.bai3.model.AppPreferences;
import com.example.bai3.model.TVDevice;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository for TV device data operations.
 * Abstracts Room database access and provides both LiveData (for UI observation)
 * and synchronous (for background operations) methods.
 */
public class DeviceRepository {

    private final DeviceDao deviceDao;
    private final AppPreferences prefs;
    private final ExecutorService executor;
    private final LiveData<List<TVDevice>> allDevices;

    public DeviceRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        deviceDao = db.deviceDao();
        prefs = AppPreferences.getInstance(context);
        executor = Executors.newSingleThreadExecutor();
        allDevices = deviceDao.getAllDevices();
    }

    // ── LiveData Queries ─────────────────────────────────────

    /**
     * Get all devices as observable LiveData (sorted by last connected, descending).
     */
    public LiveData<List<TVDevice>> getAllDevices() {
        return allDevices;
    }

    // ── Async Operations ─────────────────────────────────────

    /**
     * Insert a new device. Runs on background thread.
     */
    public void insertDevice(TVDevice device, InsertCallback callback) {
        executor.execute(() -> {
            // Check for duplicates by IP
            TVDevice existing = deviceDao.getDeviceByIp(device.getIpAddress());
            if (existing != null) {
                device.setId(existing.getId());
                device.setClientKey(existing.getClientKey());
                device.setPaired(existing.isPaired());
                if (device.getMacAddress() == null || device.getMacAddress().trim().isEmpty()) {
                    device.setMacAddress(existing.getMacAddress());
                }
                deviceDao.update(device);
                if (callback != null) callback.onInserted(existing.getId());
            } else {
                long id = deviceDao.insert(device);
                if (callback != null) callback.onInserted((int) id);
            }
        });
    }

    /**
     * Update an existing device. Runs on background thread.
     */
    public void updateDevice(TVDevice device) {
        executor.execute(() -> deviceDao.update(device));
    }

    /**
     * Delete a device. Runs on background thread.
     */
    public void deleteDevice(TVDevice device) {
        executor.execute(() -> {
            deviceDao.delete(device);
            prefs.removeClientKey(device.getIpAddress());
        });
    }

    /**
     * Save pairing key for a device after successful WebOS pairing.
     */
    public void savePairingKey(int deviceId, String clientKey) {
        executor.execute(() -> {
            deviceDao.updateClientKey(deviceId, clientKey);
            TVDevice device = deviceDao.getDeviceById(deviceId);
            if (device != null) {
                prefs.saveClientKey(device.getIpAddress(), clientKey);
            }
        });
    }

    /**
     * Update the last connected timestamp.
     */
    public void updateLastConnected(int deviceId) {
        executor.execute(() -> deviceDao.updateLastConnected(deviceId, System.currentTimeMillis()));
    }

    /**
     * Get a device by ID synchronously. Must be called off the main thread.
     */
    public TVDevice getDeviceByIdSync(int id) {
        return deviceDao.getDeviceById(id);
    }

    /**
     * Get a device by IP synchronously. Must be called off the main thread.
     */
    public TVDevice getDeviceByIpSync(String ip) {
        return deviceDao.getDeviceByIp(ip);
    }

    /**
     * Get all paired devices synchronously. Must be called off the main thread.
     */
    public List<TVDevice> getPairedDevicesSync() {
        return deviceDao.getPairedDevices();
    }

    /**
     * Delete a device by ID. Runs on background thread.
     */
    public void deleteDeviceById(int id) {
        executor.execute(() -> deviceDao.deleteById(id));
    }

    /**
     * Get the ID of the last connected device.
     */
    public int getLastDeviceId() {
        return prefs.getLastDeviceId();
    }

    /**
     * Save the ID of the last connected device.
     */
    public void setLastDeviceId(int id) {
        prefs.setLastDeviceId(id);
    }

    /**
     * Callback for insert operations.
     */
    public interface InsertCallback {
        void onInserted(int id);
    }
}
