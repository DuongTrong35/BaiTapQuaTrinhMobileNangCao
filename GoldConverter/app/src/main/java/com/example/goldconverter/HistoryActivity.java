package com.example.goldconverter;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.goldconverter.adapter.HistoryAdapter;
import com.example.goldconverter.db.AppDatabase;
import com.example.goldconverter.db.HistoryRecord;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView rvHistory;
    private TextView btnBack, btnClear, tvEmpty;
    private HistoryAdapter historyAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // 1. Ánh xạ View
        rvHistory = findViewById(R.id.rvHistory);
        btnBack = findViewById(R.id.btnBack);
        btnClear = findViewById(R.id.btnClear);
        tvEmpty = findViewById(R.id.tvEmpty);

        // 2. Cài đặt RecyclerView
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        // Tạm thời truyền danh sách rỗng, sẽ cập nhật sau khi tải xong từ DB
        historyAdapter = new HistoryAdapter(new ArrayList<>());
        rvHistory.setAdapter(historyAdapter);

        // 3. Tải dữ liệu lịch sử từ Database
        loadHistoryData();

        // 4. Bắt sự kiện nút Quay lại
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Đóng Activity này để về lại MainActivity
            }
        });

        // 5. Bắt sự kiện nút Xóa tất cả
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearHistoryData();
            }
        });
    }

    private void loadHistoryData() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(HistoryActivity.this);
            List<HistoryRecord> historyList = db.historyDao().getAllHistory();

            runOnUiThread(() -> {
                if (historyList == null || historyList.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    rvHistory.setVisibility(View.GONE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    rvHistory.setVisibility(View.VISIBLE);

                    // Cập nhật dữ liệu vào Adapter
                    historyAdapter.updateData(historyList);
                }
            });
        }).start();
    }

    private void clearHistoryData() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(HistoryActivity.this);
            db.historyDao().deleteAllHistory();

            runOnUiThread(() -> {
                // Xóa danh sách trên màn hình
                historyAdapter.updateData(new ArrayList<>());
                tvEmpty.setVisibility(View.VISIBLE);
                rvHistory.setVisibility(View.GONE);
                Toast.makeText(HistoryActivity.this, "Đã xóa toàn bộ lịch sử", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }
}