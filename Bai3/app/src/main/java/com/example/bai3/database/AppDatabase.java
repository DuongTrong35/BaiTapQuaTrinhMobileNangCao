package com.example.bai3.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.bai3.model.TVDevice;
import com.example.bai3.utils.Constants;

/**
 * Room database singleton for LG Remote Pro.
 * Contains the tv_devices table for persisting paired devices.
 */
@Database(entities = {TVDevice.class}, version = Constants.DATABASE_VERSION, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    public abstract DeviceDao deviceDao();

    /**
     * Returns the singleton database instance, creating it if necessary.
     * Uses the application context to prevent Activity leaks.
     */
    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            Constants.DATABASE_NAME
                    )
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return instance;
    }
}
