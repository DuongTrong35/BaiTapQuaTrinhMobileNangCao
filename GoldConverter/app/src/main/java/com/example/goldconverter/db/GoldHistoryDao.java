package com.example.goldconverter.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface GoldHistoryDao {

    // 1. Chèn dữ liệu mới
    @Insert
    void insertAll(List<GoldPriceRecord> records);

    // 2. Lấy 7 ngày gần nhất để vẽ biểu đồ
    @Query("SELECT * FROM gold_history WHERE typeCode = :code ORDER BY timestamp ASC LIMIT 7")
    List<GoldPriceRecord> getHistoryByCode(String code);

    // 3. Kiểm tra xem ngày hôm nay đã có dữ liệu chưa
    @Query("SELECT COUNT(*) FROM gold_history WHERE typeCode = :code AND dateString = :today")
    int checkRecordExists(String code, String today);

    // ==========================================
    // CÁC HÀM NÂNG CẤP THÊM CHO ỨNG DỤNG THỰC TẾ
    // ==========================================

    // 4. Cập nhật lại giá nếu trong cùng một ngày vàng có biến động mới
    @Query("UPDATE gold_history SET buyPrice = :buy, sellPrice = :sell, timestamp = :time WHERE typeCode = :code AND dateString = :today")
    void updateTodayRecord(String code, String today, double buy, double sell, long time);

    // 5. Dọn rác: Xóa các bản ghi đã quá cũ (Ví dụ: cũ hơn 10 ngày trước) để app luôn nhẹ
    @Query("DELETE FROM gold_history WHERE timestamp < :oldestAllowedTimestamp")
    void deleteOldRecords(long oldestAllowedTimestamp);
}