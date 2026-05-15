package com.example.bai3.ui.devices;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.example.bai3.LGRemoteProApp;
import com.example.bai3.MainActivity;
import com.example.bai3.R;
import com.example.bai3.model.AppPreferences;
import com.example.bai3.model.TVCapabilities;
import com.example.bai3.model.TVDevice;
import com.example.bai3.network.LGTVClient;
import com.example.bai3.utils.NetworkUtils;

public class TvConnectingPinActivity extends AppCompatActivity {
    private LGTVClient tvClient;
    private String tvName;
    private String tvIp;

    private LinearLayout screenConnecting;
    private LinearLayout screenPinInput;
    private LinearLayout screenSuccess;
    private TextView tvConnectingStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tv_connecting_pin);

        // Lấy thông tin thiết bị từ Intent
        String tvNameStr = getIntent().getStringExtra("TV_NAME");
        String tvIpStr = getIntent().getStringExtra("TV_IP");
        tvName = tvNameStr != null && !tvNameStr.trim().isEmpty() ? tvNameStr.trim() : "LG TV";
        tvIp = tvIpStr != null ? tvIpStr.trim() : null;

        // Tìm các TextView hiển thị thông tin
        TextView tvConnectingName = findViewById(R.id.tv_connecting_name);
        TextView tvConnectingIp = findViewById(R.id.tv_connecting_ip);
        TextView tvSuccessName = findViewById(R.id.tv_success_name);
        TextView tvSuccessIpModel = findViewById(R.id.tv_success_ip_model);
        TextView tvSummaryName = findViewById(R.id.tv_summary_name);
        TextView tvSummaryIp = findViewById(R.id.tv_summary_ip);
        tvConnectingStatus = findViewById(R.id.tv_connecting_status);

        // Cập nhật dữ liệu thật lên giao diện
        if (tvConnectingName != null) tvConnectingName.setText(tvName);
        if (tvSuccessName != null) tvSuccessName.setText(tvName);
        if (tvSummaryName != null) tvSummaryName.setText(tvName);
        if (tvIp != null) {
            if (tvConnectingIp != null) tvConnectingIp.setText(tvIp);
            if (tvSuccessIpModel != null) tvSuccessIpModel.setText(tvIp + " · UHD 4K");
            if (tvSummaryIp != null) tvSummaryIp.setText(tvIp);
        }

        screenConnecting = findViewById(R.id.screen_connecting);
        screenPinInput = findViewById(R.id.screen_pin_input);
        screenSuccess = findViewById(R.id.screen_success);
        Button btnTvAccepted = findViewById(R.id.btn_tv_accepted);
        Button btnEnterPinAlt = findViewById(R.id.btn_enter_pin_alt);
        Button btnConfirmPin = findViewById(R.id.btn_confirm_pin);
        Button btnOpenRemote = findViewById(R.id.btn_open_remote);
        ImageView btnBackConnecting = findViewById(R.id.btn_back_connecting);
        ImageView btnBackPin = findViewById(R.id.btn_back_pin);
        TextView tvRetryConnect = findViewById(R.id.tv_retry_connect);

        tvClient = ((LGRemoteProApp) getApplication()).getTvClient();
        tvClient.setCallback(new LGTVClient.ClientCallback() {
            @Override
            public void onConnectionStateChanged(LGTVClient.ConnectionState state) {
                runOnUiThread(() -> {
                    switch (state) {
                        case CONNECTING:
                            setConnectingStatus(R.string.status_connecting);
                            break;
                        case PAIRING:
                            setConnectingStatus(R.string.pairing_waiting);
                            break;
                        case CONNECTED:
                            showSuccessScreen();
                            break;
                        case DISCONNECTED:
                            setConnectingStatus(R.string.status_disconnected);
                            break;
                        case ERROR:
                            setConnectingStatus(R.string.status_error);
                            break;
                    }
                });
            }

            @Override
            public void onPairingRequired() {
                runOnUiThread(() -> setConnectingStatus(R.string.pairing_waiting));
            }

            @Override
            public void onPaired(String clientKey) {
                // No-op: connection state callback handles UI transition.
            }

            @Override
            public void onCapabilitiesLoaded(TVCapabilities capabilities) {
                // No-op
            }

            @Override
            public void onVolumeChanged(int volume, boolean muted) {
                // No-op
            }

            @Override
            public void onCommandResponse(String id, boolean success, String errorMessage) {
                // No-op
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> showConnectionError(message));
            }
        });

        // Nút Back
        if (btnBackConnecting != null) {
            btnBackConnecting.setOnClickListener(v -> finish());
        }
        if (btnBackPin != null) {
            btnBackPin.setOnClickListener(v -> finish());
        }

        ImageView ivSpinner = findViewById(R.id.iv_spinner);
        if (ivSpinner != null) {
            ObjectAnimator rotation = ObjectAnimator.ofFloat(ivSpinner, "rotation", 0f, 360f);
            rotation.setDuration(1500);
            rotation.setRepeatCount(ObjectAnimator.INFINITE);
            rotation.setInterpolator(new LinearInterpolator());
            rotation.start();
        }

        // Luồng 1: Xác nhận trực tiếp trên TV
        if (btnTvAccepted != null) {
            btnTvAccepted.setOnClickListener(v -> connectToSelectedTv());
        }

        // Luồng 2: Mở màn hình nhập mã PIN thay thế
        if (btnEnterPinAlt != null) {
            btnEnterPinAlt.setOnClickListener(v -> {
                if (screenConnecting != null) screenConnecting.setVisibility(View.GONE);
                if (screenPinInput != null) screenPinInput.setVisibility(View.VISIBLE);
            });
        }

        // Xác nhận PIN sau khi nhập xong (Mô phỏng)
        if (btnConfirmPin != null) {
            // Tạm thời bật sẵn nút Xác nhận cho mục đích mô phỏng
            btnConfirmPin.setEnabled(true);
            btnConfirmPin.setOnClickListener(v -> connectToSelectedTv());
        }

        if (tvRetryConnect != null) {
            tvRetryConnect.setOnClickListener(v -> connectToSelectedTv());
        }

        if (btnOpenRemote != null) {
            btnOpenRemote.setOnClickListener(v -> {
                // Đánh dấu đã hoàn thành onboarding
                AppPreferences.getInstance(this).setOnboardingComplete(true);
                
                // Chuyển tới màn hình Remote
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }

        connectToSelectedTv();
    }

    private void connectToSelectedTv() {
        if (!NetworkUtils.isValidIpAddress(tvIp)) {
            showConnectionError(getString(R.string.discovery_invalid_ip));
            return;
        }

        if (screenConnecting != null) screenConnecting.setVisibility(View.VISIBLE);
        if (screenPinInput != null) screenPinInput.setVisibility(View.GONE);
        if (screenSuccess != null) screenSuccess.setVisibility(View.GONE);
        setConnectingStatus(R.string.status_connecting);

        TVDevice device = new TVDevice(tvName, tvIp);
        tvClient.connect(device);
    }

    private void showSuccessScreen() {
        if (screenConnecting != null) screenConnecting.setVisibility(View.GONE);
        if (screenPinInput != null) screenPinInput.setVisibility(View.GONE);
        if (screenSuccess != null) screenSuccess.setVisibility(View.VISIBLE);
    }

    private void setConnectingStatus(int messageResId) {
        if (tvConnectingStatus != null) {
            tvConnectingStatus.setText(messageResId);
        }
    }

    private void showConnectionError(String message) {
        setConnectingStatus(R.string.status_error);
        Toast.makeText(this, message != null && !message.isEmpty() ? message : getString(R.string.status_error), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        if (tvClient != null) {
            tvClient.setCallback(null);
        }
        super.onDestroy();
    }
}
