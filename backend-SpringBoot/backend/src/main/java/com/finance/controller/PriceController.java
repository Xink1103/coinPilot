package com.finance.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finance.entity.Asset;
import com.finance.service.AssetService;
import com.finance.service.ExternalPriceService;

@RestController
@RequestMapping("/api/prices")
public class PriceController {

    @Autowired
    private ExternalPriceService externalPriceService;

    @Autowired
    private AssetService assetService;

    @GetMapping("/snapshot")
    public ResponseEntity<List<Map<String, Object>>> getSnapshot() {
        Map<Integer, BigDecimal> priceMap = externalPriceService.getAllPrices();
        List<Asset> allAssets = assetService.findAll();
        List<Map<String, Object>> response = new ArrayList<>();

        for (Asset asset : allAssets) {
            Map<String, Object> assetData = new HashMap<>();
            assetData.put("assetId", asset.getAssetId());
            assetData.put("assetCode", asset.getAssetCode());
            assetData.put("assetName", asset.getAssetName());
            assetData.put("assetType", asset.getAssetType());
            assetData.put("transCurrentPrice", priceMap.get(asset.getAssetId()));

            if ("STOCK".equalsIgnoreCase(asset.getAssetType())) {
                if (asset.getStockIndustryType() != null) {
                    assetData.put("stockIndustryType", asset.getStockIndustryType());
                }
                if (asset.getStockIsEtf() != null) {
                    assetData.put("stockIsEtf", asset.getStockIsEtf());
                }
            }

            response.add(assetData);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getCacheStatus() {
        return ResponseEntity.ok(externalPriceService.getCacheStatus());
    }
}