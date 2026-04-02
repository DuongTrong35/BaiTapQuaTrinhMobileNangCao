package com.example.goldconverter.api;

import java.text.DecimalFormat;

public class GoldItem {
    private String typeCode;
    private double buy;
    private double sell;

    // Thêm các hàm Setter
    public void setTypeCode(String typeCode) { this.typeCode = typeCode; }
    public void setBuy(double buy) { this.buy = buy; }
    public void setSell(double sell) { this.sell = sell; }

    public String getTypeCode() { return typeCode; }
    public double getBuyPrice() { return buy; }
    public double getSellPrice() { return sell; }

    public String getDisplayName() {
        if (typeCode == null) return "Chưa rõ";
        switch (typeCode.toUpperCase()) {
            // --- NHÓM VÀNG THẾ GIỚI ---
            case "XAUUSD":
                return "Vàng Thế giới (XAU/USD)";

            // --- NHÓM DOJI ---
            case "DOJINHTV":
                return "Vàng Trang sức DOJI";
            case "DOHNL":
                return "Vàng DOJI (Hà Nội)";
            case "DOHCML":
                return "Vàng DOJI (TP.HCM)";

            // --- NHÓM PNJ ---
            case "PQHN24NTT":
                return "Vàng PNJ (Nhẫn 24K)";
            case "PQHNVM":
                return "Vàng PNJ (Hà Nội)";

            // --- NHÓM SJC ---
            case "SJ9999":
                return "Vàng Nhẫn SJC (9999)";
            case "SJL1L10":
                return "Vàng Miếng SJC (1L - 10L)";
            case "VNGSJC":
                return "Vàng SJC (Hệ thống VietinBank)";
            case "VIETTINMSJC":
                return "Vàng SJC (VietinBank)";

            // --- NHÓM BẢO TÍN MINH CHÂU ---
            case "BT9999NTT":
                return "Vàng Nhẫn Bảo Tín (9999)";
            case "BTSJC":
                return "Vàng Bảo Tín SJC";

            default:
                // Nếu có mã mới phát sinh, trả về mã đó viết hoa
                return typeCode.toUpperCase();
        }
    }

    public String getFormattedBuy() {
        return new DecimalFormat("#,###").format(buy);
    }

    public String getFormattedSell() {
        return new DecimalFormat("#,###").format(sell);
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}