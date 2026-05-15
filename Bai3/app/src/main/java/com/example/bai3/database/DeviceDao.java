package com.example.bai3.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.bai3.model.TVDevice;

import java.util.List;

/**
 * Room DAO for TV device CRUD operations.
 * Provides both synchronous (for background threads) and LiveData (for UI observation) queries.
 */
@Dao
public interface DeviceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(TVDevice device);

    @Update
    void update(TVDevice device);

    @Delete
    void delete(TVDevice device);

    @Query("SELECT * FROM tv_devices ORDER BY lastConnected DESC")
    LiveData<List<TVDevice>> getAllDevices();

    @Query("SELECT * FROM tv_devices ORDER BY lastConnected DESC")
    List<TVDevice> getAllDevicesSync();

    @Query("SELECT * FROM tv_devices WHERE id = :id LIMIT 1")
    TVDevice getDeviceById(int id);

    @Query("SELECT * FROM tv_devices WHERE ipAddress = :ip LIMIT 1")
    TVDevice getDeviceByIp(String ip);

    @Query("SELECT * FROM tv_devices WHERE paired = 1 ORDER BY lastConnected DESC")
    List<TVDevice> getPairedDevices();

    @Query("DELETE FROM tv_devices WHERE id = :id")
    void deleteById(int id);

    @Query("UPDATE tv_devices SET clientKey = :key, paired = 1 WHERE id = :id")
    void updateClientKey(int id, String key);

    @Query("UPDATE tv_devices SET lastConnected = :timestamp WHERE id = :id")
    void updateLastConnected(int id, long timestamp);
}
