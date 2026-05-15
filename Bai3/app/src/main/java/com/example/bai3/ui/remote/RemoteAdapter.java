package com.example.bai3.ui.remote;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bai3.R;
import com.example.bai3.model.RemoteCommand;

/**
 * RecyclerView adapter for command history chips.
 * Displays the last N commands as tappable chips above the remote.
 */
public class RemoteAdapter extends ListAdapter<RemoteCommand, RemoteAdapter.CommandViewHolder> {

    /**
     * Callback for command replay.
     */
    public interface OnCommandClickListener {
        void onCommandClick(RemoteCommand command);
    }

    private OnCommandClickListener listener;

    public RemoteAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setOnCommandClickListener(OnCommandClickListener listener) {
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<RemoteCommand> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<RemoteCommand>() {
                @Override
                public boolean areItemsTheSame(@NonNull RemoteCommand oldItem, @NonNull RemoteCommand newItem) {
                    return oldItem.getId() != null && oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull RemoteCommand oldItem, @NonNull RemoteCommand newItem) {
                    return oldItem.equals(newItem);
                }
            };

    @NonNull
    @Override
    public CommandViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Simple chip layout — using a styled button/textview
        TextView chip = new TextView(parent.getContext());
        chip.setBackgroundResource(R.drawable.bg_button_rect);
        int padding = parent.getContext().getResources().getDimensionPixelSize(R.dimen.spacing_sm);
        chip.setPadding(padding * 2, padding, padding * 2, padding);
        chip.setTextColor(parent.getContext().getColor(R.color.text_primary));
        chip.setTextSize(12);

        ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(padding, 0, padding, 0);
        chip.setLayoutParams(params);

        return new CommandViewHolder(chip);
    }

    @Override
    public void onBindViewHolder(@NonNull CommandViewHolder holder, int position) {
        RemoteCommand command = getItem(position);
        ((TextView) holder.itemView).setText(command.getDisplayName());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCommandClick(command);
            }
        });
    }

    static class CommandViewHolder extends RecyclerView.ViewHolder {
        CommandViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
