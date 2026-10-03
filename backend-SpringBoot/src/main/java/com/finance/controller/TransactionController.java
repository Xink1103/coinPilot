package com.finance.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finance.entity.Transaction;
import com.finance.entity.User;
import com.finance.service.TransactionService;



@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @PostMapping
    public ResponseEntity<Map<String,Object>> creationTransaction(
        @RequestAttribute(value="validateUser",required=false)User user,
        @RequestBody Map<String,Object>requestBody)
    {
        Map<String,Object> response = new HashMap<>();
        
        if (user == null) {
            response.put("message", "請先登入");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        try{

            if (!requestBody.containsKey("surveyId") || !requestBody.containsKey("assetId") 
                    || !requestBody.containsKey("amount")) {
                response.put("message", "交易資料不完整，需包含 surveyId、assetId 和 amount");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            int surveyId = ((Number)requestBody.get("surveyId")).intValue();
            int assetId = ((Number)requestBody.get("assetId")).intValue();
            BigDecimal amount = new BigDecimal(requestBody.get("amount").toString());

            Transaction newTransaction = transactionService.createTransaction(
                    user.getUserId(), assetId, surveyId, amount);

            // 組裝回應資料
            response.put("message", "交易成功");
            response.put("transId", newTransaction.getTransId());
            response.put("transAmount", newTransaction.getTransAmount());
            response.put("assetCode", newTransaction.getAssetCode());
            response.put("assetName", newTransaction.getAssetName());
            response.put("assetType", newTransaction.getAssetType());
            response.put("transTradePrice", newTransaction.getTransTradePrice());
            response.put("transQuantity", newTransaction.getTransQuantity());
            response.put("transCurrentPrice", newTransaction.getTransCurrentPrice());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);


        } catch (IllegalArgumentException e) {
            response.put("message", "參數格式錯誤：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("message", "伺服器內部錯誤：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping
    public ResponseEntity<?> getTransactions(
            @RequestAttribute(value = "validateUser", required = false) User user) {

        Map<String, Object> response = new HashMap<>();

        if (user == null) {
            response.put("message", "請先登入");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        try {
            // 呼叫 Service 查詢交易歷史
            List<Transaction> transactions = transactionService.getUserTransaction(user.getUserId());

            // 組裝交易列表回應
            List<Map<String, Object>> transactionList = new ArrayList<>();

            if (transactions != null) {
                for (Transaction t : transactions) {
                    Map<String, Object> transMap = new HashMap<>();
                    
                    // 交易基本資訊
                    transMap.put("transId", t.getTransId());
                    transMap.put("userId", t.getUserId());
                    transMap.put("assetId", t.getAssetId());
                    transMap.put("surveyId", t.getSurveyId());
                    transMap.put("transCreatedAt", t.getTransCreatedAt().toString());
                    
                    // 交易價格/數量
                    transMap.put("transTradePrice", t.getTransTradePrice());
                    transMap.put("transQuantity", t.getTransQuantity());
                    transMap.put("transAmount", t.getTransAmount());
                    transMap.put("transCurrentPrice", t.getTransCurrentPrice());
                    
                    // 資產資訊
                    transMap.put("assetCode", t.getAssetCode());
                    transMap.put("assetName", t.getAssetName());
                    transMap.put("assetType", t.getAssetType());
                    
                    transactionList.add(transMap);
                }
            }
            return ResponseEntity.status(HttpStatus.OK).body(transactionList);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("message", "查詢失敗：伺服器內部錯誤");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

}
