package com.example.goldconverter.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "history_table")
public class HistoryRecord {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public double amount;         // Số lượng nhập (VD: 10)
    public String unit;           // Đơn vị (VD: "Lượng", "Chỉ")
    public String goldBrand;      // Tên loại vàng (VD: "Vàng SJC")
    public String targetCurrency; // Tiền tệ đích (VD: "USD")
    public String resultText;     // Kết quả hiển thị (VD: "32,500.50 USD")
    public String dateString;     // Thời gian (VD: "14:30 - 02/04/2026")
    public long timestamp;        // Để sắp xếp lịch sử mới nhất lên đầu
}