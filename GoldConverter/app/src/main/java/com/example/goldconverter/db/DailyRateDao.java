package com.example.goldconverter.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface DailyRateDao {
    // Nếu trùng ngày và trùng mã tiền tệ thì ghi đè (REPLACE)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRates(List<DailyRateRecord> rates);

    // Lấy tối đa 7 ngày gần nhất của một mã tiền tệ, sắp xếp tăng dần theo thời gian
    @Query("SELECT * FROM daily_rates WHERE currencyCode = :currency ORDER BY date ASC LIMIT 7")
    List<DailyRateRecord> getHistoryByCurrency(String currency);

    @Query("DELETE FROM daily_rates WHERE date < :cutoffDate")
    void deleteOldRates(String cutoffDate);

    // Lấy danh sách toàn bộ tỷ giá của ngày được cập nhật gần nhất
    @Query("SELECT * FROM daily_rates WHERE date = (SELECT MAX(date) FROM daily_rates)")
    List<DailyRateRecord> getLatestRates();
}