package com.finance.service;

import com.finance.entity.Asset;
import com.finance.entity.Transaction;
import com.finance.repository.AssetDAO;
import com.finance.repository.TransactionDAO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TransactionService {

    @Autowired private TransactionDAO transactionDAO;

    @Autowired private AssetDAO assetDAO;

    @Autowired private ExternalPriceService externalPriceService;

    // 執行下單邏輯
    public Transaction createTransaction(
            int userId, int assetId, int surveyId, BigDecimal budgetAmount) throws Exception {

        // 取得成交價（市場價 ± 0.5% 波動）
        BigDecimal tradePrice = generateTradePrice(assetId);

        // 計算數量
        BigDecimal quantityRaw = budgetAmount.divide(tradePrice, 8, RoundingMode.DOWN);
        BigDecimal quantity = quantityRaw.setScale(1, RoundingMode.DOWN);

        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new Exception("投入金額過低，無法購買任何數量。");
        }

        // 反算實際扣款金額
        BigDecimal actualAmount = quantity.multiply(tradePrice).setScale(2, RoundingMode.HALF_UP);

        // 確保不超支
        if (actualAmount.compareTo(budgetAmount) > 0) {
            quantity = quantity.subtract(new BigDecimal("0.1"));
            actualAmount = quantity.multiply(tradePrice).setScale(2, RoundingMode.HALF_UP);
        }

        // 建立交易記錄
        Transaction trans = new Transaction();
        trans.setUserId(userId);
        trans.setAssetId(assetId);
        trans.setSurveyId(surveyId);
        trans.setTransQuantity(quantity);
        trans.setTransTradePrice(tradePrice);
        trans.setTransAmount(actualAmount);

        // 存入資料庫
        transactionDAO.insert(trans);

        // 補齊當前市價
        BigDecimal currentPrice = externalPriceService.getPrice(assetId);
        trans.setTransCurrentPrice(
                currentPrice != null ? currentPrice.setScale(2, RoundingMode.HALF_UP) : tradePrice);

        // 補齊資產資訊
        Asset assetInfo = assetDAO.findByAssetId(assetId);
        if (assetInfo == null) {
            throw new Exception("找不到資產 ID: " + assetId);
        }

        trans.setAssetCode(assetInfo.getAssetCode());
        trans.setAssetName(assetInfo.getAssetName());
        trans.setAssetType(assetInfo.getAssetType());

        return trans;
    }

    // 取得使用者交易列表
    public List<Transaction> getUserTransaction(int userId) {
        List<Transaction> trans = transactionDAO.selectAllByUserId(userId);

        // 填入當前市價
        for (Transaction t : trans) {
            BigDecimal currentPrice = externalPriceService.getPrice(t.getAssetId());
            if (currentPrice != null) {
                t.setTransCurrentPrice(currentPrice.setScale(2, RoundingMode.HALF_UP));
            }
        }

        return trans;
    }

    // 產生下單時的成交價 市場價 ± 0.5% 隨機波動
    private BigDecimal generateTradePrice(int assetId) {
        BigDecimal marketPrice = externalPriceService.getPrice(assetId);

        if (marketPrice == null) {
            throw new RuntimeException("無法取得資產 " + assetId + " 的價格資訊");
        }

        // 模擬 -0.5% ~ +0.5% 的微小波動
        double changeRate = -0.005 + Math.random() * 0.01;

        BigDecimal tradePrice = marketPrice.multiply(BigDecimal.valueOf(1 + changeRate));

        return tradePrice.setScale(2, RoundingMode.HALF_UP);
    }
}
