package com.example.goldconverter;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.ExistingPeriodicWorkPolicy;
import java.util.concurrent.TimeUnit;

import com.example.goldconverter.adapter.GoldAdapter;
import com.example.goldconverter.api.GoldApi;
import com.example.goldconverter.api.GoldItem;
import com.example.goldconverter.api.GoldRetrofitClient;

import com.example.goldconverter.db.AppDatabase;
import com.example.goldconverter.db.DailyRateRecord;
import com.example.goldconverter.db.GoldPriceRecord;
import com.example.goldconverter.db.HistoryRecord;
import com.example.goldconverter.worker.GoldFetchWorker;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import android.widget.AdapterView;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;

public class MainActivity extends AppCompatActivity {

    // Đã xóa tvTop1, tvTop2, tvTop3 ở đây
    private TextView tvResult;
    private Spinner spinnerGoldBrand, spinnerWeightUnit, spinnerTargetCurrency;
    private EditText etAmount;
    private Button btnConvert, btnViewHistory;

    private RecyclerView rvGoldPrices;
    private GoldAdapter goldAdapter;
    private LineChart lineChart;

    private List<GoldItem> currentGoldList = new ArrayList<>();

    // Map lưu trữ tỷ giá tiền tệ: Key là mã tiền (VD: USD), Value là tỷ giá so với VNĐ
    private Map<String, Double> currencyRates = new HashMap<>();

    private DecimalFormat formatter = new DecimalFormat("#,###.##");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupUnitSpinner();

        // Gọi hàm vẽ biểu đồ ngay khi mở app
        setupChartFramework();

        // 1. Tải giá vàng thời gian thực từ API
        fetchGoldData();

        // 2. Tải tỷ giá ngoại tệ từ Database cục bộ do Worker thu thập
        loadCurrencyRatesFromDB();

        btnConvert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performConversion();
            }
        });

        btnViewHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Chuyển sang màn hình Lịch sử
                android.content.Intent intent = new android.content.Intent(MainActivity.this, HistoryActivity.class);
                startActivity(intent);
            }
        });

        PeriodicWorkRequest goldWorkRequest = new PeriodicWorkRequest.Builder(GoldFetchWorker.class, 24, TimeUnit.HOURS)
                .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "DailyGoldFetch",
                ExistingPeriodicWorkPolicy.KEEP, // Nếu đã có lịch rồi thì giữ nguyên
                goldWorkRequest
        );
        //Ép Worker lấy tỷ giá tiền tệ chạy NGAY LẬP TỨC (OneTimeWorkRequest)
        androidx.work.OneTimeWorkRequest rateOneTimeRequest =
                new androidx.work.OneTimeWorkRequest.Builder(com.example.goldconverter.worker.UpdateRateWorker.class).build();
        WorkManager.getInstance(this).enqueue(rateOneTimeRequest);
        androidx.work.OneTimeWorkRequest goldRequest =
                new androidx.work.OneTimeWorkRequest.Builder(GoldFetchWorker.class).build();
        WorkManager.getInstance(this).enqueue(goldRequest);

        WorkManager.getInstance(this).getWorkInfoByIdLiveData(rateOneTimeRequest.getId())
                .observe(this, workInfo -> {
                    if (workInfo != null && workInfo.getState() == androidx.work.WorkInfo.State.SUCCEEDED) {
                        // KHI WORKER BÁO CÁO THÀNH CÔNG -> TẢI LẠI DỮ LIỆU TỪ DB LÊN SPINNER
                        Log.d("UI_UPDATE", "Worker lấy tỷ giá xong, cập nhật lại Spinner!");
                        loadCurrencyRatesFromDB();
                    } else if (workInfo != null && workInfo.getState() == androidx.work.WorkInfo.State.FAILED) {
                        Toast.makeText(MainActivity.this, "Tải tỷ giá thất bại, dùng dữ liệu dự phòng", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void initViews() {
        tvResult = findViewById(R.id.tvResult);

        spinnerGoldBrand = findViewById(R.id.spinnerGoldBrand);
        spinnerWeightUnit = findViewById(R.id.spinnerWeightUnit);
        etAmount = findViewById(R.id.etAmount);
        btnConvert = findViewById(R.id.btnConvert);
        btnViewHistory = findViewById(R.id.btnViewHistory);
        lineChart = findViewById(R.id.lineChart);

        rvGoldPrices = findViewById(R.id.rvGoldPrices);
        // Ngăn cuộn kép vì đã có NestedScrollView ở ngoài
        rvGoldPrices.setNestedScrollingEnabled(false);
        rvGoldPrices.setLayoutManager(new LinearLayoutManager(this));
        goldAdapter = new GoldAdapter(new ArrayList<>());
        rvGoldPrices.setAdapter(goldAdapter);

        spinnerTargetCurrency = findViewById(R.id.spinnerTargetCurrency);
        spinnerGoldBrand.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                GoldItem selectedItem = (GoldItem) parent.getItemAtPosition(position);
                updateChartData(selectedItem); // Cập nhật lại biểu đồ
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void setupUnitSpinner() {
        String[] units = {"Lượng", "Chỉ", "Gram"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, units);
        spinnerWeightUnit.setAdapter(adapter);
    }

    private void loadCurrencyRatesFromDB() {
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(MainActivity.this);
                List<DailyRateRecord> records = db.dailyRateDao().getLatestRates();

                if (records != null && !records.isEmpty()) {
                    for (DailyRateRecord record : records) {
                        currencyRates.put(record.currencyCode, record.rateRelativeToUSD);
                    }
                } else {
                    currencyRates.put("VND", 25400.0);
                    currencyRates.put("USD", 1.0);
                }

                runOnUiThread(() -> {
                    List<String> currencyCodes = new ArrayList<>(currencyRates.keySet());
                    ArrayAdapter<String> currencyAdapter = new ArrayAdapter<>(
                            MainActivity.this,
                            android.R.layout.simple_spinner_dropdown_item,
                            currencyCodes
                    );
                    spinnerTargetCurrency.setAdapter(currencyAdapter);

                    int defaultPosition = currencyAdapter.getPosition("VND");
                    if (defaultPosition >= 0) {
                        spinnerTargetCurrency.setSelection(defaultPosition);
                    }
                });
            } catch (Exception e) {
                Log.e("DB_ERROR", "Lỗi tải dữ liệu tỷ giá: " + e.getMessage());
            }
        }).start();
    }

    private void fetchGoldData() {
        GoldApi api = GoldRetrofitClient.getClient().create(GoldApi.class);
        api.getGoldPrices().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String rawJson = response.body().string();
                        Log.d("API_CHECK", "JSON GỐC: " + rawJson);
                        org.json.JSONObject root = new org.json.JSONObject(rawJson);

                        if (root.has("prices")) {
                            org.json.JSONObject pricesObject = root.getJSONObject("prices");
                            currentGoldList.clear();

                            java.util.Iterator<String> keys = pricesObject.keys();
                            while(keys.hasNext()) {
                                String key = keys.next();
                                org.json.JSONObject itemObj = pricesObject.getJSONObject(key);

                                GoldItem item = new GoldItem();
                                item.setTypeCode(key);
                                item.setBuy(itemObj.optDouble("buy", 0));
                                item.setSell(itemObj.optDouble("sell", 0));

                                currentGoldList.add(item);
                            }

                            runOnUiThread(() -> updateUI());
                        } else {
                            // Thay bằng Toast vì không còn TextView để hiển thị lỗi
                            Toast.makeText(MainActivity.this, "Lỗi: Server không trả về dữ liệu", Toast.LENGTH_SHORT).show();
                        }

                    } catch (Exception e) {
                        Log.e("API_DEBUG", "Lỗi Parse JSON: " + e.getMessage());
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Lỗi HTTP: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("API_DEBUG", "Lỗi mạng: " + t.getMessage());
                Toast.makeText(MainActivity.this, "Lỗi kết nối mạng khi tải giá vàng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI() {
        if (currentGoldList == null || currentGoldList.isEmpty()) return;

        // Cập nhật Spinner
        ArrayAdapter<GoldItem> brandAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, currentGoldList);
        spinnerGoldBrand.setAdapter(brandAdapter);

        // Cập nhật RecyclerView
        goldAdapter.updateData(currentGoldList);

        // Chủ động vẽ biểu đồ cho item đầu tiên ngay khi tải dữ liệu xong
        if (currentGoldList.size() > 0) {
            updateChartData(currentGoldList.get(0));
        }
    }

    private void performConversion() {
        String amountStr = etAmount.getText().toString();
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số lượng", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);
        String selectedUnit = spinnerWeightUnit.getSelectedItem().toString();
        GoldItem selectedBrand = (GoldItem) spinnerGoldBrand.getSelectedItem();

        if (spinnerTargetCurrency.getSelectedItem() == null) {
            Toast.makeText(this, "Đang tải dữ liệu tỷ giá, vui lòng đợi", Toast.LENGTH_SHORT).show();
            return;
        }
        String targetCurrency = spinnerTargetCurrency.getSelectedItem().toString();

        if (selectedBrand == null) return;

        double basePricePerLuong = selectedBrand.getSellPrice();
        double totalVnd = 0;

        switch (selectedUnit) {
            case "Lượng":
                // Vì API đã trả về giá theo Lượng nên chỉ cần nhân với số lượng
                totalVnd = amount * basePricePerLuong;
                break;
            case "Chỉ":
                // 1 Lượng = 10 Chỉ => Giá 1 Chỉ = Giá Lượng / 10
                totalVnd = amount * (basePricePerLuong / 10.0);
                break;
            case "Gram":
                // 1 Lượng = 37.5 Gram => Giá 1 Gram = Giá Lượng / 37.5
                totalVnd = amount * (basePricePerLuong / 37.5);
                break;
        }

        Double rateVndToUsd = currencyRates.get("VND");
        Double rateTargetToUsd = currencyRates.get(targetCurrency);

        if (rateVndToUsd == null || rateTargetToUsd == null || rateVndToUsd == 0) {
            Toast.makeText(this, "Chưa có đủ dữ liệu tỷ giá", Toast.LENGTH_SHORT).show();
            return;
        }

        double totalUsd = totalVnd / rateVndToUsd;
        double finalAmount = totalUsd * rateTargetToUsd;

        tvResult.setText(formatter.format(finalAmount) + " " + targetCurrency);

        // ==========================================
        // THÊM ĐOẠN NÀY ĐỂ LƯU LỊCH SỬ VÀO DATABASE
        // ==========================================
        String resultString = formatter.format(finalAmount) + " " + targetCurrency;
        String currentTime = new java.text.SimpleDateFormat("HH:mm - dd/MM/yyyy", java.util.Locale.getDefault()).format(new java.util.Date());

        new Thread(() -> {
            HistoryRecord history = new HistoryRecord();
            history.amount = amount;
            history.unit = selectedUnit;
            history.goldBrand = selectedBrand.getDisplayName();
            history.targetCurrency = targetCurrency;
            history.resultText = resultString;
            history.dateString = currentTime;
            history.timestamp = System.currentTimeMillis();

            // Lưu vào DB
            AppDatabase db = AppDatabase.getInstance(MainActivity.this);
            db.historyDao().insertHistory(history);

            Log.d("HISTORY", "Đã lưu lịch sử: " + amount + " " + selectedUnit + " " + history.goldBrand + " -> " + resultString);
        }).start();
    }

    private void setupChartFramework() {
        if (lineChart == null) return;

        lineChart.getDescription().setEnabled(false);
        lineChart.getAxisRight().setEnabled(false); // Ẩn trục Y bên phải

        // Chỉ cấu hình vị trí và khoảng cách trục X ở đây, KHÔNG gắn dữ liệu nhãn (dates)
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f); // Ép khoảng cách giữa các điểm là 1

        // (Đã xóa đoạn tạo mảng dates và gán IndexAxisValueFormatter ở đây)
    }

    private void updateChartData(GoldItem item) {
        if (lineChart == null || item == null) return;

        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(MainActivity.this);
            List<GoldPriceRecord> historyRecords = db.goldHistoryDao().getHistoryByCode(item.getTypeCode());

            runOnUiThread(() -> {
                ArrayList<Entry> entries = new ArrayList<>();
                ArrayList<String> dates = new ArrayList<>();

                // 1. KIỂM TRA LOẠI VÀNG VÀ CHỌN HỆ SỐ CHIA
                boolean isWorldGold = item.getDisplayName().toLowerCase().contains("thế giới") || item.getDisplayName().toLowerCase().contains("world");
                double divisor = isWorldGold ? 1.0 : 1000000.0; // Thế giới giữ nguyên, Trong nước chia 1 triệu

                if (historyRecords != null && !historyRecords.isEmpty()) {
                    for (int i = 0; i < historyRecords.size(); i++) {
                        GoldPriceRecord record = historyRecords.get(i);
                        double price = record.sellPrice == 0 ? record.buyPrice : record.sellPrice;
                        // Chia cho hệ số tương ứng
                        entries.add(new Entry((float) i, (float) (price / divisor)));
                        dates.add(record.dateString);
                    }
                } else {
                    double price = item.getSellPrice() == 0 ? item.getBuyPrice() : item.getSellPrice();
                    // Chia cho hệ số tương ứng
                    entries.add(new Entry(0f, (float) (price / divisor)));
                    dates.add(new SimpleDateFormat("dd/MM", Locale.getDefault()).format(new Date()));
                }

                // 2. GẮN NHÃN TRỤC X
                XAxis xAxis = lineChart.getXAxis();
                xAxis.setValueFormatter(new IndexAxisValueFormatter(dates));

                if (entries.size() == 1) {
                    xAxis.setAxisMinimum(-0.5f);
                    xAxis.setAxisMaximum(0.5f);
                    xAxis.setLabelCount(1);
                } else {
                    xAxis.resetAxisMinimum();
                    xAxis.resetAxisMaximum();
                    xAxis.setLabelCount(dates.size(), true);
                }

                // 3. SỬA LẠI TÊN NHÃN (LABEL) CHO BIỂU ĐỒ
                String label = "Giá " + item.getDisplayName() + (isWorldGold ? " (USD/oz)" : " (Triệu VNĐ)");
                LineDataSet dataSet = new LineDataSet(entries, label);
                dataSet.setColor(android.graphics.Color.parseColor("#FF9800"));
                dataSet.setCircleColor(android.graphics.Color.parseColor("#D32F2F"));
                dataSet.setLineWidth(2.5f);
                dataSet.setValueTextSize(10f);

                // 4. FORMAT LẠI SỐ HIỂN THỊ (Dùng 2 số thập phân là đẹp nhất cho cả USD và VNĐ)
                dataSet.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
                    private java.text.DecimalFormat mFormat = new java.text.DecimalFormat("#,##0.00");
                    @Override
                    public String getFormattedValue(float value) {
                        return mFormat.format(value);
                    }
                });

                LineData lineData = new LineData(dataSet);
                lineChart.setData(lineData);
                lineChart.animateX(800);
                lineChart.invalidate();
            });
        }).start();
    }
}