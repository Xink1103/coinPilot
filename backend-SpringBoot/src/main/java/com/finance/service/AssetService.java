package com.finance.service;

import com.finance.entity.Asset;
import com.finance.repository.AssetDAO;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AssetService {

    @Autowired private AssetDAO assetDAO;

    // 找全部的
    public List<Asset> findAll() {
        return assetDAO.findAll();
    }
}
