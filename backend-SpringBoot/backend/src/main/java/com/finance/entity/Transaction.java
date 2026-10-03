package com.finance.entity;

import java.math.BigDecimal;
import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Transaction {

    private Integer transId;
	private Integer userId;
	private Integer assetId;
	private Integer surveyId;
	private BigDecimal transTradePrice;  //成交價 要存進資料庫 
	private BigDecimal transCurrentPrice; //給service層用的 不用存進資料庫
	private BigDecimal transQuantity; //持股數
	private Timestamp transCreatedAt;
	private BigDecimal transAmount;
	
	//用於 JOIN asset 表後的結果
	private String assetCode;
	private String assetName;
	private String assetType;

}
