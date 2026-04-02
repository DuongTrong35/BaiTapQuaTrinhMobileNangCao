package com.example.goldconverter.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.goldconverter.R;
import com.example.goldconverter.api.GoldItem;
import java.util.List;

public class GoldAdapter extends RecyclerView.Adapter<GoldAdapter.GoldViewHolder> {

    private List<GoldItem> goldList;

    public GoldAdapter(List<GoldItem> goldList) {
        this.goldList = goldList;
    }

    public void updateData(List<GoldItem> newList) {
        this.goldList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GoldViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_gold, parent, false);
        return new GoldViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GoldViewHolder holder, int position) {
        GoldItem item = goldList.get(position);
        String goldName = item.getDisplayName();

        holder.tvGoldName.setText(goldName);

        // Xử lý riêng biệt cho Vàng Thế Giới
        if (goldName != null && (goldName.toLowerCase().contains("thế giới") || goldName.toLowerCase().contains("world"))) {
            // Thêm hậu tố USD/oz vào giá mua
            holder.tvBuyPrice.setText(item.getFormattedBuy() + " USD/oz");
            // Ẩn giá bán ra đi (thay bằng dấu gạch ngang)
            holder.tvSellPrice.setText("-");
        } else {
            // Hiển thị bình thường cho các loại vàng trong nước
            holder.tvBuyPrice.setText(item.getFormattedBuy());

            // Xử lý an toàn: Nếu API trả về 0 ở các loại vàng khác, cũng nên hiển thị dấu gạch ngang cho đẹp
            String sellPrice = item.getFormattedSell();
            if (sellPrice == null || sellPrice.equals("0") || sellPrice.equals("0.0")) {
                holder.tvSellPrice.setText("-");
            } else {
                holder.tvSellPrice.setText(sellPrice);
            }
        }
    }

    @Override
    public int getItemCount() {
        return goldList == null ? 0 : goldList.size();
    }

    static class GoldViewHolder extends RecyclerView.ViewHolder {
        TextView tvGoldName, tvBuyPrice, tvSellPrice;

        public GoldViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGoldName = itemView.findViewById(R.id.tvGoldName);
            tvBuyPrice = itemView.findViewById(R.id.tvBuyPrice);
            tvSellPrice = itemView.findViewById(R.id.tvSellPrice);
        }
    }
}