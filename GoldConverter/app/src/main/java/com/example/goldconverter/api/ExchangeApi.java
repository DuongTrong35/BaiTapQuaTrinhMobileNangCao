package com.example.goldconverter.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ExchangeApi {

    // Đường dẫn lấy tỷ giá mới nhất. {api_key} và {base_currency} là các biến động.
    @GET("v6/{api_key}/latest/{base_currency}")
    Call<ExchangeResponse> getLatestRates(
            @Path("api_key") String apiKey,
            @Path("base_currency") String baseCurrency
    );
}