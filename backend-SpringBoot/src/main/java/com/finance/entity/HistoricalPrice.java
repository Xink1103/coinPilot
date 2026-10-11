package com.finance.entity;

import java.math.BigDecimal;
import java.sql.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HistoricalPrice {

    private Integer hpPriceId;
    private Integer assetId;
    private Date hpMarketDate;
    private BigDecimal hpClosePrice;
}
