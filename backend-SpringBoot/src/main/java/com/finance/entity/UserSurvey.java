package com.finance.entity;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSurvey {

    private Integer surveyId;

    private Integer userId;
    private String surveyAgeGroup;
    private Integer surveyInvestExp;
    private Integer surveyRiskTol;
    private String surveySavingGoal;
    private String surveyPreferredAsset;
    private Integer surveyMonthlySaving;
    private Integer surveySurpriseFund;
    private Integer surveyMonthlyCost;
    private Integer surveyEmergencyFund;
    private Integer surveyMarketReaction;
    private BigDecimal surveyInitFund;
    private String surveyLevel;
    private Integer surveyScore;

    public UserSurvey(Integer userId) {
        this.userId = userId;
    }
}
