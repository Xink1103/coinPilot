package com.finance.repository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.finance.entity.HistoricalPrice;

@Repository
public class HistoricalPriceDAO {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String FIND_RANDOM_PRICE_BY_ASSET =
    "SELECT hpPriceId, assetId, hpMarketDate, hpClosePrice " +
    "FROM historicalPrice WHERE assetId = ? " +
    "ORDER BY RAND() LIMIT 1"; 

    public HistoricalPrice findRandomPriceByAsset(int assetId){

        List<HistoricalPrice> priceList = jdbcTemplate.query(
            FIND_RANDOM_PRICE_BY_ASSET,
            new Object[]{assetId},
            new BeanPropertyRowMapper<>(HistoricalPrice.class)
        ); 
        if(priceList.isEmpty()){
            return null;
        }else{
            return priceList.get(0);
        }
    }
}
