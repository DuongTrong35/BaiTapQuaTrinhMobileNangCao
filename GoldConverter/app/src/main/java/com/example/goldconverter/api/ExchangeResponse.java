package com.example.goldconverter.api;

import com.google.gson.JsonObject;

import com.google.gson.annotations.SerializedName;

public class ExchangeResponse {

    // Ánh xạ đúng tên trường "result" từ JSON trả về
    @SerializedName("result")
    public String resultStatus;

    // Đồng tiền gốc
    @SerializedName("base_code")
    public String baseCode;

    // JsonObject sẽ chứa danh sách các tỷ giá (VD: "VND": 25000, "EUR": 0.92)
    @SerializedName("conversion_rates")
    public JsonObject conversionRates;
}
