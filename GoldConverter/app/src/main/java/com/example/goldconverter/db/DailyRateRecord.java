package com.example.goldconverter.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(tableName = "daily_rates", primaryKeys = {"date", "currencyCode"})
public class DailyRateRecord {

    @NonNull
    public String date = ""; // Lưu theo định dạng "yyyy-MM-dd" để dễ sắp xếp

    @NonNull
    public String currencyCode = ""; // VD: "VND", "EUR"

    public double rateRelativeToUSD; // Tỷ giá so với đồng gốc (VD: USD)
}
