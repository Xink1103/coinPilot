package com.finance.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.finance.entity.HistoricalPrice;
import com.finance.repository.HistoricalPriceDAO;

@Service
public class HistoricalPriceService {
    
    @Autowired
    private HistoricalPriceDAO historicalPriceDAO;


    //取得指定資產的隨機價格
	public HistoricalPrice getRandomPrice(int assetId) {
		return historicalPriceDAO.findRandomPriceByAsset(assetId);
	}

}
