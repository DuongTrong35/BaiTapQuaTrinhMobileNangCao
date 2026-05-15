package com.example.bai3.ui.remote;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.bai3.R;
import com.example.bai3.model.AppPreferences;
import com.example.bai3.model.TVCapabilities;
import com.example.bai3.model.TVDevice;
import com.example.bai3.network.LGTVClient;
import com.example.bai3.utils.AnimationUtils;
import com.example.bai3.utils.Constants;

/**
 * Main remote control fragment.
 * Displays all remote buttons organized by group, observes connection state
 * and TV capabilities to enable/disable controls accordingly.
 */
public class RemoteFragment extends Fragment {

    private RemoteViewModel viewModel;
    private AppPreferences prefs;
    private Vibrator vibrator;

    // UI references
    private View statusDot;
    private TextView tvName, statusText, tvVolumeLevel;
    private View remoteCard;
    private LinearLayout numberPadLayout;
    private Button btnToggleNumpad;
    private boolean numpadVisible = false;

    // D-Pad — custom ring view
    private DPadView dpadView;
    // Invisible proxy IDs kept for enable/disable management
    private ImageButton btnUp, btnDown, btnLeft, btnRight;
    private Button btnOk;

    // System buttons
    private ImageButton btnPower, btnHome, btnBack, btnSettingsTv;

    // Volume & Channel
    private ImageButton btnVolUp, btnVolDown, btnMute, btnChUp, btnChDown;
    private View volumeGroup, channelGroup;

    // Media
    private ImageButton btnPlay, btnPause, btnStop, btnRewind, btnFastForward;
    private View mediaRow;

    // Apps
    private ImageButton btnNetflix, btnYoutube, btnPrime, btnDisney;

    // Number pad buttons
    private Button[] numButtons = new Button[10];

    // Long-press repeat handler
    private final Handler repeatHandler = new Handler(Looper.getMainLooper());
    private Runnable repeatRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_remote, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(RemoteViewModel.class);
        prefs = AppPreferences.getInstance(requireContext());
        vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);

        bindViews(view);
        setupListeners();
        observeState();

        // Launch animation
        AnimationUtils.animateLaunchEntry(remoteCard);
    }

    private void bindViews(View view) {
        statusDot = view.findViewById(R.id.status_dot);
        tvName = view.findViewById(R.id.tv_name);
        statusText = view.findViewById(R.id.status_text);
        remoteCard = view.findViewById(R.id.remote_root);

        // System
        btnPower = view.findViewById(R.id.btn_power);
        btnHome = view.findViewById(R.id.btn_home);
        btnBack = view.findViewById(R.id.btn_back);
        btnSettingsTv = view.findViewById(R.id.btn_settings_tv);

        // D-Pad
        dpadView = view.findViewById(R.id.dpad_view);
        btnUp    = view.findViewById(R.id.btn_dpad_up);
        btnDown  = view.findViewById(R.id.btn_dpad_down);
        btnLeft  = view.findViewById(R.id.btn_dpad_left);
        btnRight = view.findViewById(R.id.btn_dpad_right);
        btnOk    = (Button) view.findViewById(R.id.btn_dpad_ok);

        // Volume & Channel
        btnVolUp = view.findViewById(R.id.btn_vol_up);
        btnVolDown = view.findViewById(R.id.btn_vol_down);
        btnMute = view.findViewById(R.id.btn_mute);
        tvVolumeLevel = view.findViewById(R.id.tv_volume_level);
        btnChUp = view.findViewById(R.id.btn_ch_up);
        btnChDown = view.findViewById(R.id.btn_ch_down);
        volumeGroup = view.findViewById(R.id.volume_group);
        channelGroup = view.findViewById(R.id.channel_group);

        // Media
        btnPlay = view.findViewById(R.id.btn_play);
        btnPause = view.findViewById(R.id.btn_pause);
        btnStop = view.findViewById(R.id.btn_stop);
        btnRewind = view.findViewById(R.id.btn_rewind);
        btnFastForward = view.findViewById(R.id.btn_fast_forward);
        mediaRow = view.findViewById(R.id.media_row);

        // Apps
        btnNetflix = view.findViewById(R.id.btn_netflix);
        btnYoutube = view.findViewById(R.id.btn_youtube);
        btnPrime = view.findViewById(R.id.btn_prime);
        btnDisney = view.findViewById(R.id.btn_disney);

        // Number pad
        numberPadLayout = view.findViewById(R.id.number_pad_layout);
        btnToggleNumpad = view.findViewById(R.id.btn_toggle_numpad);
        numButtons[0] = view.findViewById(R.id.btn_num_0);
        numButtons[1] = view.findViewById(R.id.btn_num_1);
        numButtons[2] = view.findViewById(R.id.btn_num_2);
        numButtons[3] = view.findViewById(R.id.btn_num_3);
        numButtons[4] = view.findViewById(R.id.btn_num_4);
        numButtons[5] = view.findViewById(R.id.btn_num_5);
        numButtons[6] = view.findViewById(R.id.btn_num_6);
        numButtons[7] = view.findViewById(R.id.btn_num_7);
        numButtons[8] = view.findViewById(R.id.btn_num_8);
        numButtons[9] = view.findViewById(R.id.btn_num_9);
    }

    private void setupListeners() {
        // System buttons
        setButtonAction(btnPower, () -> viewModel.powerOff());
        setButtonAction(btnHome, () -> viewModel.pressHome());
        setButtonAction(btnBack, () -> viewModel.pressBack());
        setButtonAction(btnSettingsTv, () -> viewModel.openTvSettings());

        // D-Pad via DPadView listener
        if (dpadView != null) {
            dpadView.setDPadListener(new DPadView.DPadListener() {
                @Override public void onUp()    { hapticFeedback(); viewModel.navigateUp(); }
                @Override public void onDown()  { hapticFeedback(); viewModel.navigateDown(); }
                @Override public void onLeft()  { hapticFeedback(); viewModel.navigateLeft(); }
                @Override public void onRight() { hapticFeedback(); viewModel.navigateRight(); }
                @Override public void onOk()    { hapticFeedback(); viewModel.pressOk(); }
            });
        }

        // Volume & Channel
        setRepeatableButton(btnVolUp, () -> viewModel.volumeUp());
        setRepeatableButton(btnVolDown, () -> viewModel.volumeDown());
        setButtonAction(btnMute, () -> viewModel.toggleMute());
        setRepeatableButton(btnChUp, () -> viewModel.channelUp());
        setRepeatableButton(btnChDown, () -> viewModel.channelDown());

        // Media
        setButtonAction(btnPlay, () -> viewModel.play());
        setButtonAction(btnPause, () -> viewModel.pause());
        setButtonAction(btnStop, () -> viewModel.stop());
        setButtonAction(btnRewind, () -> viewModel.rewind());
        setButtonAction(btnFastForward, () -> viewModel.fastForward());

        // Apps
        setButtonAction(btnNetflix, () -> viewModel.launchNetflix());
        setButtonAction(btnYoutube, () -> viewModel.launchYouTube());
        setButtonAction(btnPrime, () -> viewModel.launchPrimeVideo());
        setButtonAction(btnDisney, () -> viewModel.launchDisneyPlus());

        // Number pad
        for (int i = 0; i < 10; i++) {
            final int num = i;
            setButtonAction(numButtons[i], () -> viewModel.pressNumber(num));
        }

        // Number pad toggle
        btnToggleNumpad.setOnClickListener(v -> {
            numpadVisible = !numpadVisible;
            numberPadLayout.setVisibility(numpadVisible ? View.VISIBLE : View.GONE);
            btnToggleNumpad.setText(numpadVisible ? R.string.btn_hide_numpad : R.string.btn_show_numpad);
        });
    }

    /**
     * Set up a button with haptic feedback and press animation.
     */
    private void setButtonAction(View button, Runnable action) {
        if (button == null) return;
        button.setOnClickListener(v -> {
            if (!v.isEnabled()) return;
            hapticFeedback();
            AnimationUtils.animateButtonPress(v);
            AnimationUtils.animateCommandFlash(v);
            action.run();
        });
    }

    /**
     * Set up a button with long-press repeat (for D-pad, volume, channel).
     */
    private void setRepeatableButton(View button, Runnable action) {
        if (button == null) return;

        button.setOnClickListener(v -> {
            if (!v.isEnabled()) return;
            hapticFeedback();
            AnimationUtils.animateButtonPress(v);
            AnimationUtils.animateCommandFlash(v);
            action.run();
        });

        button.setOnLongClickListener(v -> {
            if (!v.isEnabled()) return false;
            repeatRunnable = new Runnable() {
                @Override
                public void run() {
                    if (v.isPressed()) {
                        hapticFeedback();
                        action.run();
                        repeatHandler.postDelayed(this, Constants.DPAD_REPEAT_INTERVAL_MS);
                    }
                }
            };
            repeatHandler.postDelayed(repeatRunnable, Constants.DPAD_REPEAT_INTERVAL_MS);
            return true;
        });
    }

    private void hapticFeedback() {
        if (prefs.isHapticEnabled() && vibrator != null) {
            vibrator.vibrate(
                    VibrationEffect.createOneShot(Constants.HAPTIC_BUTTON_MS, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    private void observeState() {
        // Connection state
        viewModel.getConnectionState().observe(getViewLifecycleOwner(), state -> {
            updateConnectionUI(state);
        });

        // Capabilities
        viewModel.getCapabilities().observe(getViewLifecycleOwner(), caps -> {
            if (caps != null) {
                applyCapabilities(caps);
            }
        });

        // Volume state
        viewModel.getVolumeState().observe(getViewLifecycleOwner(), volState -> {
            if (volState != null && volState.length >= 2) {
                tvVolumeLevel.setText(String.valueOf(volState[0]));
                boolean muted = volState[1] == 1;
                btnMute.setAlpha(muted ? 1.0f : 0.6f);
            }
        });
    }

    /**
     * Update the connection status bar based on current state.
     */
    private void updateConnectionUI(LGTVClient.ConnectionState state) {
        if (state == null) return;

        switch (state) {
            case CONNECTED:
                statusDot.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.status_connected));
                statusText.setText(R.string.remote_connected);
                statusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.success));
                TVDevice device = viewModel.getCurrentDevice();
                if (device != null) {
                    tvName.setText(device.getName());
                }
                enableAllButtons(true);
                break;
            case CONNECTING:
                statusDot.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.status_connecting));
                statusText.setText(R.string.remote_connecting);
                statusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_connecting));
                enableAllButtons(false);
                break;
            case PAIRING:
                statusDot.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.status_connecting));
                statusText.setText(R.string.status_pairing);
                statusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_connecting));
                break;
            case ERROR:
                statusDot.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.status_disconnected));
                statusText.setText(R.string.status_error);
                statusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.danger));
                AnimationUtils.animateShake(statusDot);
                break;
            case DISCONNECTED:
            default:
                statusDot.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.status_disconnected));
                statusText.setText(R.string.remote_disconnected);
                statusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.danger));
                tvName.setText(R.string.remote_not_connected);
                enableAllButtons(false);
                break;
        }
    }

    /**
     * Apply TV capabilities — dim unsupported buttons to alpha 0.35.
     */
    private void applyCapabilities(TVCapabilities caps) {
        if (!caps.loaded) return;

        setCapabilityState(volumeGroup, caps.hasVolumeControl);
        setCapabilityState(btnMute, caps.hasMuteControl);
        setCapabilityState(channelGroup, caps.hasChannelControl);
        setCapabilityState(mediaRow, caps.hasMediaPlayback);
        setCapabilityState(btnNetflix, caps.hasNetflix);
        setCapabilityState(btnYoutube, caps.hasYouTube);
        setCapabilityState(btnPrime, caps.hasPrimeVideo);
        setCapabilityState(btnDisney, caps.hasDisneyPlus);

        // Number pad follows channel capability
        for (Button btn : numButtons) {
            setCapabilityState(btn, caps.hasChannelControl);
        }
    }

    /**
     * Set a view's enabled state and alpha based on capability support.
     */
    private void setCapabilityState(View view, boolean supported) {
        if (view == null) return;
        float targetAlpha = supported ? Constants.ALPHA_FULL : Constants.ALPHA_DIMMED;
        view.setEnabled(true);
        AnimationUtils.fadeToAlpha(view, targetAlpha);
    }

    /**
     * Enable or disable all remote buttons (used when connecting/disconnecting).
     */
    private void enableAllButtons(boolean enabled) {
        float alpha = enabled ? Constants.ALPHA_FULL : Constants.ALPHA_LOADING;
        View[] allButtons = {
                btnPower, btnHome, btnBack, btnSettingsTv, dpadView,
                btnUp, btnDown, btnLeft, btnRight, btnOk,
                btnVolUp, btnVolDown, btnMute, btnChUp, btnChDown,
                btnPlay, btnPause, btnStop, btnRewind, btnFastForward,
                btnNetflix, btnYoutube, btnPrime, btnDisney
        };

        for (View btn : allButtons) {
            if (btn != null) {
                btn.setEnabled(enabled);
                btn.setAlpha(alpha);
            }
        }

        for (Button btn : numButtons) {
            if (btn != null) {
                btn.setEnabled(enabled);
                btn.setAlpha(alpha);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        repeatHandler.removeCallbacksAndMessages(null);
    }
}
