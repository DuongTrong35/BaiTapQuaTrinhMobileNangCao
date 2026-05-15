package com.example.bai3;

import android.app.Application;

import com.example.bai3.model.AppPreferences;
import com.example.bai3.network.LGTVClient;

/**
 * Application class for LG Remote Pro.
 * Initializes singletons and provides global access to the TV client.
 */
public class LGRemoteProApp extends Application {

    private static LGRemoteProApp instance;
    private LGTVClient tvClient;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        // Initialize singletons
        AppPreferences.getInstance(this);
        tvClient = new LGTVClient();
    }

    public static LGRemoteProApp getInstance() {
        return instance;
    }

    /**
     * Get the global TV client instance.
     */
    public LGTVClient getTvClient() {
        return tvClient;
    }
}
