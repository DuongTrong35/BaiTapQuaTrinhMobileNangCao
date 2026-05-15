package com.example.bai3.ui.devices;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.example.bai3.R;
import com.example.bai3.model.TVDevice;
import com.example.bai3.network.LGTVClient;
import com.example.bai3.utils.AnimationUtils;
import com.example.bai3.utils.NetworkUtils;

/**
 * Device discovery fragment.
 * Scans for LG TVs via SSDP, displays found devices in a RecyclerView,
 * supports manual IP entry, and handles pairing flow.
 */
public class DeviceDiscoveryFragment extends Fragment {

    private DeviceDiscoveryViewModel viewModel;
    private DeviceListAdapter adapter;

    private RecyclerView rvDevices;
    private FrameLayout scanAnimationContainer;
    private TextView tvScanStatus;
    private LinearLayout emptyState;
    private FloatingActionButton fabAddManual;
    private MaterialButton btnRetry;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_device_discovery, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(DeviceDiscoveryViewModel.class);

        bindViews(view);
        setupRecyclerView();
        setupListeners();
        observeState();

        // Start scanning on launch
        viewModel.startScan();
    }

    private void bindViews(View view) {
        rvDevices = view.findViewById(R.id.rv_devices);
        scanAnimationContainer = view.findViewById(R.id.scan_animation_container);
        tvScanStatus = view.findViewById(R.id.tv_scan_status);
        emptyState = view.findViewById(R.id.empty_state);
        fabAddManual = view.findViewById(R.id.fab_add_manual);
        btnRetry = view.findViewById(R.id.btn_retry);
    }

    private void setupRecyclerView() {
        adapter = new DeviceListAdapter();
        adapter.setOnDeviceClickListener(new DeviceListAdapter.OnDeviceClickListener() {
            @Override
            public void onConnectClick(TVDevice device) {
                viewModel.connectToDevice(device);
            }

            @Override
            public void onDeviceLongClick(TVDevice device) {
                showDeviceOptions(device);
            }
        });

        rvDevices.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvDevices.setAdapter(adapter);
    }

    private void setupListeners() {
        fabAddManual.setOnClickListener(v -> showManualIpDialog());
        btnRetry.setOnClickListener(v -> {
            emptyState.setVisibility(View.GONE);
            viewModel.startScan();
        });
    }

    private void observeState() {
        // Discovered devices
        viewModel.getDiscoveredDevices().observe(getViewLifecycleOwner(), devices -> {
            if (devices != null && !devices.isEmpty()) {
                emptyState.setVisibility(View.GONE);
                rvDevices.setVisibility(View.VISIBLE);
                adapter.submitList(devices);
            }
        });

        // Saved devices (from Room DB)
        viewModel.getSavedDevices().observe(getViewLifecycleOwner(), devices -> {
            // Merge saved devices into the list
            if (devices != null && !devices.isEmpty()) {
                adapter.setSavedDevices(devices);
            }
        });

        // Scanning state
        viewModel.getIsScanning().observe(getViewLifecycleOwner(), scanning -> {
            scanAnimationContainer.setVisibility(scanning ? View.VISIBLE : View.GONE);
            tvScanStatus.setVisibility(scanning ? View.VISIBLE : View.GONE);

            if (scanning) {
                // Animate radar rotation
                View radar = scanAnimationContainer.findViewById(R.id.radar_circle);
                if (radar != null) {
                    radar.animate().rotation(360f).setDuration(2000).start();
                }
            } else {
                // Check if list is empty after scan
                if (adapter.getItemCount() == 0) {
                    emptyState.setVisibility(View.VISIBLE);
                    AnimationUtils.fadeIn(emptyState, 300);
                }
            }
        });

        // Connection state
        viewModel.getConnectionState().observe(getViewLifecycleOwner(), state -> {
            if (state == LGTVClient.ConnectionState.CONNECTED) {
                Snackbar.make(requireView(), R.string.status_connected, Snackbar.LENGTH_SHORT).show();
            }
        });

        // Pairing required
        viewModel.getPairingRequired().observe(getViewLifecycleOwner(), required -> {
            if (required != null && required) {
                showPairingDialog();
            }
        });

        // Scan errors
        viewModel.getScanError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Snackbar.make(requireView(), error, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Show bottom sheet dialog for manual IP entry.
     */
    private void showManualIpDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_manual_ip, null);

        EditText etIp = dialogView.findViewById(R.id.et_manual_ip);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btn_confirm);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);

        btnConfirm.setOnClickListener(v -> {
            String ip = etIp.getText().toString().trim();
            if (NetworkUtils.isValidIpAddress(ip)) {
                viewModel.addManualDevice(ip);
                dialog.dismiss();
            } else {
                etIp.setError(getString(R.string.discovery_invalid_ip));
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.setContentView(dialogView);
        dialog.show();
    }

    /**
     * Show pairing dialog while waiting for TV acceptance.
     */
    private void showPairingDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.pairing_title)
                .setMessage(R.string.pairing_message)
                .setCancelable(false)
                .setNegativeButton(R.string.cancel, (d, w) -> d.dismiss())
                .show();
    }

    /**
     * Show options for a device (forget, re-pair).
     */
    private void showDeviceOptions(TVDevice device) {
        new AlertDialog.Builder(requireContext())
                .setTitle(device.getName())
                .setItems(new String[]{
                        getString(R.string.settings_forget_device),
                        getString(R.string.settings_re_pair)
                }, (d, which) -> {
                    if (which == 0) {
                        viewModel.forgetDevice(device);
                    } else {
                        viewModel.connectToDevice(device);
                    }
                })
                .show();
    }
}
