package com.example.goldconverter.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "gold_history")
public class GoldPriceRecord {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String typeCode; // Mã vàng (VD: SJL1L10, XAUUSD)
    public double buyPrice;
    public double sellPrice;
    public long timestamp;  // Thời gian lưu (dùng để vẽ trục X)
    public String dateString; // Ngày tháng dạng chuỗi (VD: "02/04") để dễ hiện lên trục X
}