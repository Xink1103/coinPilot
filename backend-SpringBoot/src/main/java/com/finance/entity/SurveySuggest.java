package com.finance.entity;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SurveySuggest {

    private int sugId; // PK
    private int surveyId; // FK: 對應問卷 ID
    private int assetId; // FK: 對應資產 ID
    private BigDecimal sugPercentage;

    private String assetCode;
    private String assetName;
    private String assetType;
}
