package com.finance.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Asset {
    
    private Integer assetId;
    private String assetCode;
    private String assetName;
    private String assetType;  
    
    //Stock
    private String stockIndustryType;  
    private Boolean stockIsEtf;

    public Asset(Integer assetId, String assetCode, String assetName, String assetType) {
        this.assetId = assetId;
        this.assetCode = assetCode;
        this.assetName = assetName;
        this.assetType = assetType;
    }

    //判斷是否為股票類型（包含 ETF）
    public boolean isStock() {
        return "STOCK".equalsIgnoreCase(this.assetType);
    }
    
    //判斷是否為外幣類型
    public boolean isForex() {
        return "FOREX".equalsIgnoreCase(this.assetType);
    }
    
    
    //判斷是否為 ETF（僅對 STOCK 類型有效） 
    public boolean isEtf() {
        return stockIsEtf != null && stockIsEtf;
    }

}
