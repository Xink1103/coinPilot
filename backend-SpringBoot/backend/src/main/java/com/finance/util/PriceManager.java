package com.finance.util;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class PriceManager {
    
	//建立整體的實例
	private final Map<Integer,BigDecimal> priceCache = new ConcurrentHashMap<>();

	//紀錄最後更新時間
	private long lastUpdate = 0;    
		
	//取得單一價格
	public BigDecimal getPrice(Integer assetId) {
		return priceCache.get(assetId);
	}
	
	//取得所有價位
	public Map<Integer,BigDecimal> getAllPrice(){
		return priceCache;
	}
	
	//更新快取
	public void updateAll(Map<Integer,BigDecimal> newPrices) {
		priceCache.putAll(newPrices);
		this.lastUpdate = System.currentTimeMillis();
	}
	
	//取得最後時間
	public long getLastUpdate() {
		return lastUpdate;
	}
	
	//判斷有沒有值
	public boolean isEmpty() {
		return priceCache.isEmpty();
	}

}
