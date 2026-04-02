package com.example.goldconverter.db; // Giữ nguyên package của bạn

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

// Đảm bảo version đang là 3 như trong ảnh của bạn
@Database(entities = {HistoryRecord.class, DailyRateRecord.class, GoldPriceRecord.class}, version = 3)
public abstract class AppDatabase extends RoomDatabase {

    // 1. Khai báo các DAO ở đây (mỗi cái 1 lần thôi nhé)
    public abstract DailyRateDao dailyRateDao();
    public abstract HistoryDao historyDao(); // Nếu bạn có file này
    public abstract GoldHistoryDao goldHistoryDao();

    // 2. Khai báo biến INSTANCE
    private static volatile AppDatabase INSTANCE;

    // 3. Hàm khởi tạo Singleton
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "currency_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
} // <--- Dấu ngoặc nhọn đóng class chỉ xuất hiện 1 lần duy nhất ở cuối cùng này