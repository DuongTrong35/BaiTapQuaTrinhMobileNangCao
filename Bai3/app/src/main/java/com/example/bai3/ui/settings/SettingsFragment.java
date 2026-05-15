package com.example.bai3.ui.settings;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.example.bai3.R;

/**
 * Settings fragment using PreferenceFragmentCompat.
 * Manages all app settings: connection, remote customization, appearance,
 * notifications, and about/version info.
 */
public class SettingsFragment extends PreferenceFragmentCompat {

    private SettingsViewModel viewModel;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }

    @Override
    public void onViewCreated(@NonNull android.view.View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.setBackgroundColor(requireContext().getColor(R.color.background));

        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        setupPreferences();
    }

    private void setupPreferences() {
        // Theme mode
        ListPreference themePref = findPreference("theme_mode");
        if (themePref != null) {
            themePref.setOnPreferenceChangeListener((pref, newValue) -> {
                viewModel.setThemeMode((String) newValue);
                return true;
            });
        }

        // Haptic feedback
        SwitchPreferenceCompat hapticPref = findPreference("haptic_enabled");
        if (hapticPref != null) {
            hapticPref.setOnPreferenceChangeListener((pref, newValue) -> {
                viewModel.setHapticEnabled((Boolean) newValue);
                return true;
            });
        }

        // Compact layout
        SwitchPreferenceCompat compactPref = findPreference("compact_layout");
        if (compactPref != null) {
            compactPref.setOnPreferenceChangeListener((pref, newValue) -> {
                viewModel.setCompactLayout((Boolean) newValue);
                return true;
            });
        }

        // Disconnect alert
        SwitchPreferenceCompat disconnectPref = findPreference("disconnect_alert");
        if (disconnectPref != null) {
            disconnectPref.setOnPreferenceChangeListener((pref, newValue) -> {
                viewModel.setDisconnectAlert((Boolean) newValue);
                return true;
            });
        }

        // Forget device
        Preference forgetPref = findPreference("forget_device");
        if (forgetPref != null) {
            forgetPref.setOnPreferenceClickListener(pref -> {
                viewModel.disconnectTV();
                return true;
            });
        }

        // App version
        Preference versionPref = findPreference("app_version");
        if (versionPref != null) {
            versionPref.setSummary(viewModel.getAppVersion());
        }
    }
}
