package com.example.bai3.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bai3.MainActivity;
import com.example.bai3.R;
import com.example.bai3.model.AppPreferences;

/**
 * Splash/Onboarding activity shown on first launch.
 */
public class OnboardingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        // Auto-navigate to WifiScanningActivity after a short delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(this, com.example.bai3.ui.devices.WifiScanningActivity.class));
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }, 2000);
    }
}
