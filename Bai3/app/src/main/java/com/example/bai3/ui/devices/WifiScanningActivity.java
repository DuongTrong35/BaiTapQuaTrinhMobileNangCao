package com.example.bai3.ui.devices;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;

import com.example.bai3.R;
import com.example.bai3.model.TVDevice;

import java.util.List;

public class WifiScanningActivity extends AppCompatActivity {
    
    private DeviceDiscoveryViewModel viewModel;
    private LinearLayout llFoundDevices;
    private TextView tvFoundCount;
    private AnimatorSet radarAnimatorSet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifi_scanning);

        llFoundDevices = findViewById(R.id.ll_device_list);
        tvFoundCount = findViewById(R.id.tv_found_count);

        startRadarAnimation();

        viewModel = new ViewModelProvider(this).get(DeviceDiscoveryViewModel.class);

        // Lắng nghe danh sách TV thật được dò thấy trong mạng
        viewModel.getDiscoveredDevices().observe(this, devices -> {
            updateDevicesList(devices);
        });

        // Bắt đầu dò quét mạng nội bộ bằng SSDP
        viewModel.startScan();

        Button btnManualAdd = findViewById(R.id.btn_manual_add);
        if (btnManualAdd != null) {
            btnManualAdd.setOnClickListener(v -> showManualIpDialog());
        }
    }

    private void showManualIpDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_manual_ip, null);
        dialog.setContentView(dialogView);

        TextInputEditText etIp = dialogView.findViewById(R.id.et_manual_ip);
        View btnCancel = dialogView.findViewById(R.id.btn_cancel);
        View btnConfirm = dialogView.findViewById(R.id.btn_confirm);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String ip = etIp.getText() != null ? etIp.getText().toString().trim() : "";
            if (!ip.isEmpty()) {
                dialog.dismiss();
                viewModel.stopScan();
                Intent intent = new Intent(this, TvConnectingPinActivity.class);
                intent.putExtra("TV_NAME", "LG TV (Manual)");
                intent.putExtra("TV_IP", ip);
                startActivity(intent);
            } else {
                etIp.setError("Vui lòng nhập địa chỉ IP");
            }
        });

        dialog.show();
    }

    private void startRadarAnimation() {
        View radarOuter = findViewById(R.id.radar_outer);
        View radarInner = findViewById(R.id.radar_inner);

        if (radarOuter == null || radarInner == null) return;

        // Outer circle animation
        ObjectAnimator scaleXOuter = ObjectAnimator.ofFloat(radarOuter, "scaleX", 1f, 1.5f);
        scaleXOuter.setRepeatCount(ObjectAnimator.INFINITE);
        scaleXOuter.setRepeatMode(ObjectAnimator.RESTART);
        
        ObjectAnimator scaleYOuter = ObjectAnimator.ofFloat(radarOuter, "scaleY", 1f, 1.5f);
        scaleYOuter.setRepeatCount(ObjectAnimator.INFINITE);
        scaleYOuter.setRepeatMode(ObjectAnimator.RESTART);
        
        ObjectAnimator alphaOuter = ObjectAnimator.ofFloat(radarOuter, "alpha", 1f, 0f);
        alphaOuter.setRepeatCount(ObjectAnimator.INFINITE);
        alphaOuter.setRepeatMode(ObjectAnimator.RESTART);

        // Inner circle animation
        ObjectAnimator scaleXInner = ObjectAnimator.ofFloat(radarInner, "scaleX", 1f, 1.4f);
        scaleXInner.setRepeatCount(ObjectAnimator.INFINITE);
        scaleXInner.setRepeatMode(ObjectAnimator.RESTART);
        
        ObjectAnimator scaleYInner = ObjectAnimator.ofFloat(radarInner, "scaleY", 1f, 1.4f);
        scaleYInner.setRepeatCount(ObjectAnimator.INFINITE);
        scaleYInner.setRepeatMode(ObjectAnimator.RESTART);
        
        ObjectAnimator alphaInner = ObjectAnimator.ofFloat(radarInner, "alpha", 1f, 0f);
        alphaInner.setRepeatCount(ObjectAnimator.INFINITE);
        alphaInner.setRepeatMode(ObjectAnimator.RESTART);

        // Delay inner animation slightly to create a ripple effect
        scaleXInner.setStartDelay(500);
        scaleYInner.setStartDelay(500);
        alphaInner.setStartDelay(500);

        radarAnimatorSet = new AnimatorSet();
        radarAnimatorSet.playTogether(scaleXOuter, scaleYOuter, alphaOuter, scaleXInner, scaleYInner, alphaInner);
        radarAnimatorSet.setDuration(2000);
        radarAnimatorSet.setInterpolator(new LinearInterpolator());
        radarAnimatorSet.start();
    }

    private void updateDevicesList(List<TVDevice> devices) {
        // Xoá các view tivi cũ
        llFoundDevices.removeAllViews();
        
        tvFoundCount.setText("ĐÃ TÌM THẤY (" + devices.size() + ")");

        LayoutInflater inflater = LayoutInflater.from(this);
        for (TVDevice device : devices) {
            View itemView = inflater.inflate(R.layout.item_scanned_tv, llFoundDevices, false);
            
            TextView tvName = itemView.findViewById(R.id.tv_item_name);
            TextView tvIp = itemView.findViewById(R.id.tv_item_ip);
            
            tvName.setText(device.getName());
            tvIp.setText(device.getIpAddress() + " · " + (device.getModel() != null ? device.getModel() : "WebOS"));

            itemView.setOnClickListener(v -> {
                viewModel.stopScan();
                // Tới màn hình PIN
                Intent intent = new Intent(this, TvConnectingPinActivity.class);
                intent.putExtra("TV_NAME", device.getName());
                intent.putExtra("TV_IP", device.getIpAddress());
                startActivity(intent);
            });
            
            llFoundDevices.addView(itemView);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewModel.stopScan(); // Dừng quét khi thoát
        if (radarAnimatorSet != null) {
            radarAnimatorSet.cancel();
        }
    }
}
