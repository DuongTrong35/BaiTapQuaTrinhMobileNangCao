package com.example.goldconverter.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class GoldResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("data")
    private List<GoldItem> data;

    public boolean isSuccess() { return success; }
    public List<GoldItem> getData() { return data; }
}