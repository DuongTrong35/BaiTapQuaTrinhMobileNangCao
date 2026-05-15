package com.example.bai3.ui.devices;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.example.bai3.R;
import com.example.bai3.model.TVDevice;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for the device discovery list.
 * Displays TV devices as cards with name, model, IP, status badge, and connect button.
 * Uses DiffUtil for efficient list updates.
 */
public class DeviceListAdapter extends ListAdapter<TVDevice, DeviceListAdapter.DeviceViewHolder> {

    /**
     * Click listener for device actions.
     */
    public interface OnDeviceClickListener {
        void onConnectClick(TVDevice device);
        void onDeviceLongClick(TVDevice device);
    }

    private OnDeviceClickListener listener;
    private List<TVDevice> savedDevices = new ArrayList<>();

    public DeviceListAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setOnDeviceClickListener(OnDeviceClickListener listener) {
        this.listener = listener;
    }

    public void setSavedDevices(List<TVDevice> devices) {
        this.savedDevices = devices != null ? devices : new ArrayList<>();
        notifyDataSetChanged();
    }

    private static final DiffUtil.ItemCallback<TVDevice> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<TVDevice>() {
                @Override
                public boolean areItemsTheSame(@NonNull TVDevice oldItem, @NonNull TVDevice newItem) {
                    return oldItem.getIpAddress().equals(newItem.getIpAddress());
                }

                @Override
                public boolean areContentsTheSame(@NonNull TVDevice oldItem, @NonNull TVDevice newItem) {
                    return oldItem.equals(newItem);
                }
            };

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        TVDevice device = getItem(position);
        holder.bind(device);
    }

    class DeviceViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName;
        private final TextView tvModel;
        private final TextView tvIp;
        private final TextView tvStatusBadge;
        private final MaterialButton btnConnect;

        DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_device_name);
            tvModel = itemView.findViewById(R.id.tv_device_model);
            tvIp = itemView.findViewById(R.id.tv_device_ip);
            tvStatusBadge = itemView.findViewById(R.id.tv_status_badge);
            btnConnect = itemView.findViewById(R.id.btn_connect);
        }

        void bind(TVDevice device) {
            tvName.setText(device.getName());
            tvModel.setText(device.getModel() != null ? device.getModel() : "");
            tvIp.setText(device.getIpAddress());

            // Check if this device is already paired (in saved devices)
            boolean isPaired = false;
            for (TVDevice saved : savedDevices) {
                if (saved.getIpAddress().equals(device.getIpAddress()) && saved.isPaired()) {
                    isPaired = true;
                    break;
                }
            }

            if (isPaired) {
                tvStatusBadge.setText(R.string.discovery_paired);
                tvStatusBadge.setTextColor(itemView.getContext().getColor(R.color.success));
                tvStatusBadge.setVisibility(View.VISIBLE);
            } else {
                tvStatusBadge.setText(R.string.discovery_not_paired);
                tvStatusBadge.setTextColor(itemView.getContext().getColor(R.color.text_secondary));
                tvStatusBadge.setVisibility(View.VISIBLE);
            }

            btnConnect.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onConnectClick(device);
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onDeviceLongClick(device);
                }
                return true;
            });
        }
    }
}
