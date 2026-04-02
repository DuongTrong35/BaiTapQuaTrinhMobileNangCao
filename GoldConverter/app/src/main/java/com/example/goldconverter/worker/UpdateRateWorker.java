package com.example.goldconverter.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.goldconverter.api.ExchangeApi;
import com.example.goldconverter.api.ExchangeResponse;
import com.example.goldconverter.api.RetrofitClient;
import com.example.goldconverter.db.AppDatabase;
import com.example.goldconverter.db.DailyRateRecord;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Response;

public class UpdateRateWorker extends Worker {

    // Điền lại API Key của bạn vào đây
    private static final String API_KEY = "add925f39119015a6eb67f83";

    public UpdateRateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("WorkManager", "Đang tiến hành cập nhật tỷ giá ngầm...");

        // Khởi tạo Retrofit
        ExchangeApi api = RetrofitClient.getClient().create(ExchangeApi.class);

        // Lấy tỷ giá mặc định gốc là USD để lưu trữ offline
        Call<ExchangeResponse> call = api.getLatestRates(API_KEY, "USD");

        try {
            // Dùng .execute() thay vì .enqueue() vì Worker ĐÃ chạy trên luồng nền rồi,
            // ta cần đợi nó lấy xong dữ liệu (chạy đồng bộ) thì mới báo cáo Result.
            Response<ExchangeResponse> response = call.execute();

            if (response.isSuccessful() && response.body() != null) {
                ExchangeResponse exchangeResponse = response.body();

                if ("success".equals(exchangeResponse.resultStatus)) {
                    JsonObject rates = exchangeResponse.conversionRates;

                    // 1. Lấy ngày hôm nay định dạng yyyy-MM-dd
                    String todayStr = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());

                    // 2. Tạo danh sách các tỷ giá cần lưu
                    java.util.List<DailyRateRecord> listToSave = new java.util.ArrayList<>();
                    String[] currencies = {"USD", "VND", "EUR", "JPY", "GBP", "AUD", "CAD"};

                    for (String curr : currencies) {
                        if (rates.has(curr)) {
                            DailyRateRecord record = new DailyRateRecord();
                            record.date = todayStr;
                            record.currencyCode = curr;
                            record.rateRelativeToUSD = rates.get(curr).getAsDouble();
                            listToSave.add(record);
                        }
                    }

                    // 3. Lưu vào Database
                    //AppDatabase.getInstance(getApplicationContext()).dailyRateDao().insertRates(listToSave);

                    AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                    db.dailyRateDao().insertRates(listToSave);
                    Log.d("WorkManager", "Đã lưu lịch sử tỷ giá ngày " + todayStr + " vào Database!");

                    // === THÊM ĐOẠN DỌN RÁC NÀY VÀO SAU KHI LƯU XONG ===
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    cal.add(java.util.Calendar.DAY_OF_YEAR, -7); // Lùi về 7 ngày trước
                    String cutoffDateStr = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.getTime());

                    // Xóa toàn bộ dữ liệu có ngày cũ hơn 7 ngày trước
                    db.dailyRateDao().deleteOldRates(cutoffDateStr);
                    Log.d("WorkManager", "Đã dọn dẹp các tỷ giá cũ hơn ngày " + cutoffDateStr);
                    // =================================================
                    return Result.success();
                }
            }
        } catch (Exception e) {
            Log.e("WorkManager", "Lỗi khi cập nhật tỷ giá: " + e.getMessage());
            return Result.retry(); // Yêu cầu WorkManager thử lại sau nếu bị lỗi mạng
        }

        return Result.failure();
    }
}
