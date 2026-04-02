package com.example.goldconverter.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface HistoryDao {
    @Insert
    void insertHistory(HistoryRecord record);

    // Lấy toàn bộ lịch sử, cái nào mới tính toán xếp lên đầu
    @Query("SELECT * FROM history_table ORDER BY timestamp DESC")
    List<HistoryRecord> getAllHistory();

    // Tùy chọn: Xóa toàn bộ lịch sử
    @Query("DELETE FROM history_table")
    void deleteAllHistory();
}