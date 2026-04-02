package com.example.goldconverter.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.goldconverter.api.GoldApi;
import com.example.goldconverter.api.GoldRetrofitClient;
import com.example.goldconverter.db.AppDatabase;
import com.example.goldconverter.db.GoldPriceRecord;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Response;

public class GoldFetchWorker extends Worker {

    public GoldFetchWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            // 1. Gọi API trực tiếp (Dùng execute() thay vì enqueue() vì Worker đã chạy trên luồng ngầm)
            GoldApi api = GoldRetrofitClient.getClient().create(GoldApi.class);
            Response<ResponseBody> response = api.getGoldPrices().execute();

            if (response.isSuccessful() && response.body() != null) {
                String rawJson = response.body().string();
                org.json.JSONObject root = new org.json.JSONObject(rawJson);

                if (root.has("prices")) {
                    org.json.JSONObject pricesObject = root.getJSONObject("prices");
                    List<GoldPriceRecord> recordsToInsert = new ArrayList<>();

                    String todayString = new SimpleDateFormat("dd/MM", Locale.getDefault()).format(new Date());
                    long currentTimestamp = System.currentTimeMillis();
                    AppDatabase db = AppDatabase.getInstance(getApplicationContext());

                    java.util.Iterator<String> keys = pricesObject.keys();
                    while(keys.hasNext()) {
                        String key = keys.next();
                        org.json.JSONObject itemObj = pricesObject.getJSONObject(key);

                        double buyPrice = itemObj.optDouble("buy", 0);
                        double sellPrice = itemObj.optDouble("sell", 0);

                        if (db.goldHistoryDao().checkRecordExists(key, todayString) == 0) {
                            // CHƯA CÓ -> THÊM MỚI
                            GoldPriceRecord record = new GoldPriceRecord();
                            record.typeCode = key;
                            record.buyPrice = buyPrice;
                            record.sellPrice = sellPrice;
                            record.timestamp = currentTimestamp;
                            record.dateString = todayString;
                            recordsToInsert.add(record);
                        } else {
                            // ĐÃ CÓ RỒI -> CẬP NHẬT GIÁ MỚI NHẤT
                            db.goldHistoryDao().updateTodayRecord(key, todayString, buyPrice, sellPrice, currentTimestamp);
                        }
                    }

                    // 2. Lưu vào Database
                    if (!recordsToInsert.isEmpty()) {
                        db.goldHistoryDao().insertAll(recordsToInsert);
                        Log.d("WORKER", "Đã lưu lịch sử giá vàng cho ngày hôm nay!");
                    }
                }
            }
            return Result.success();
        } catch (Exception e) {
            Log.e("WORKER_ERROR", "Lỗi khi lấy giá vàng: " + e.getMessage());
            return Result.retry(); // Lỗi mạng thì cho phép thử lại sau
        }
    }
}