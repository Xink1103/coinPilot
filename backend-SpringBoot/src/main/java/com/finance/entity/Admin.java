package com.finance.entity;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class Admin {

    private Long totalUsers;              // 總用戶數
    private Double assetAdoptionRate;     // 建議資產採納率
    private String dominantRiskLevel;     // 整體風險傾向
    private List<UserDetail> userList;
    
    //內部類別  用戶詳細資訊
    @Data
    @AllArgsConstructor
    @NoArgsConstructor 
    public static class UserDetail {
        // 用戶基本資訊
        private Integer userId;
        private String userName;
        private String userEmail;
        
        // 問卷資訊
        private Integer surveyId;
        private String surveyLevel;
        private BigDecimal surveyInitFund;
        private Integer surveyRiskTol;
        private String surveySavingGoal;
        private Integer surveyInvestExp;
        private String surveyAgeGroup;
        
        // 投資統計
        private BigDecimal totalInvestment;
        private BigDecimal actualInvestedAmount;    // 實際投資總金額
        private Double investmentRatio;
        
        // 資產配置列表
        private List<AssetDetail> assetAllocations;
    }

    //內部類別  資產配置明細
    @Data
    @NoArgsConstructor
    @AllArgsConstructor 
    public static class AssetDetail {
        private Integer assetId;
        private String assetName;
        private String assetCode;
        private String assetType;
        private BigDecimal sugPercentage;        // 建議比例
        private BigDecimal actualAmount;         // 實際投資金額
        private Double actualPercentage;         // 實際比例
    }

}
