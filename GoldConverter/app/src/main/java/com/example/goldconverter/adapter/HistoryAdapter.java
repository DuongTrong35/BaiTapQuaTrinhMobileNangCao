package com.example.goldconverter.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.goldconverter.R;
import com.example.goldconverter.db.HistoryRecord;

import java.util.ArrayList;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private List<HistoryRecord> historyList = new ArrayList<>();

    public HistoryAdapter(List<HistoryRecord> historyList) {
        this.historyList = historyList;
    }

    // Cập nhật dữ liệu mới vào Adapter
    public void setHistoryList(List<HistoryRecord> historyList) {
        this.historyList = historyList;
        notifyDataSetChanged(); // Yêu cầu vẽ lại giao diện
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        HistoryRecord record = historyList.get(position);

        // Đã xóa phần code cũ gây lỗi. Chỉ giữ lại phần này:
        String inputInfo = record.amount + " " + record.unit + " (" + record.goldBrand + ")";
        holder.tvInput.setText(inputInfo);
        holder.tvResult.setText(record.resultText);
        holder.tvDate.setText(record.dateString);
    }

    public void updateData(List<HistoryRecord> newList) {
        this.historyList = newList;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return historyList != null ? historyList.size() : 0;
    }

    // Lớp giữ tham chiếu đến các thành phần giao diện của item
    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvInput, tvResult, tvDate;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            // Nhớ đảm bảo trong file thiết kế giao diện (Ví dụ: item_history.xml)
            // của bạn cũng phải có các ID tương ứng như thế này nhé!
            tvInput = itemView.findViewById(R.id.tvInput);
            tvResult = itemView.findViewById(R.id.tvResult);
            tvDate = itemView.findViewById(R.id.tvDate);
        }
    }
}
