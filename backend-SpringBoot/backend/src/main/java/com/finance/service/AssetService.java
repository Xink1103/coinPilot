package com.finance.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.finance.entity.Asset;
import com.finance.repository.AssetDAO;

@Service
public class AssetService {
    
    @Autowired
    private AssetDAO assetDAO;
	
	//透過assetId找
	public Asset findByAssetId(int assetId) {
		return assetDAO.findByAssetId(assetId);
	}

	
	//透過assetCode找
	public Asset findByAssetCode(String assetCode) {
		return assetDAO.findByAssetCode(assetCode);
	}
	
	//找全部的
	public List<Asset> findAll(){
		return assetDAO.findAll();
	} 
	
	//查詢特定類型的資產
	public List<Asset> findByType(String assetType){
		return assetDAO.findByType(assetType);
	}
	
	//查詢所有股票
	public List<Asset> findAllStock(){
		return assetDAO.findByType("STOCK");
	}
	
	//查詢所有外幣
	public List<Asset> findAllForex(){
		return assetDAO.findByType("FOREX");
	}
	

}
