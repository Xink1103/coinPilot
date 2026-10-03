package com.finance.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.finance.entity.Asset;
import com.finance.entity.HistoricalPrice;

import jakarta.annotation.PostConstruct;

@Service
public class ExternalPriceService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private HistoricalPriceService historicalPriceService;

    @Autowired
    private AssetService assetService;

    @Value("${exchange.rate.api.key}")
    private String apiKey;

    private final String STOCK_URL = "https://mis.twse.com.tw/stock/api/getStockInfo.jsp?ex_ch=tse_0050.tw|tse_006208.tw|tse_2330.tw|tse_2454.tw|tse_2603.tw|tse_2881.tw|tse_3231.tw&json=1&delay=0";

    private final Map<Integer, BigDecimal> priceCache = new ConcurrentHashMap<>();
    private LocalDateTime lastStockUpdateTime = null;
    private LocalDateTime lastForexUpdateTime = null;

    //初始化的時候確定會載入最新的價位
    @PostConstruct
    public void init() {
        System.out.println("ExternalPriceService 初始化 - 開始載入價格");
        refreshStockPrices();
        refreshForexPrices();
    }

    //設定股票的更新排程
    @Scheduled(cron = "0 */1 9-13 * * MON-FRI")
    public void scheduledStockRefresh() {
        System.out.println("[排程] 台股價格更新 - " + LocalDateTime.now());
        refreshStockPrices();
    }

    //設定外幣的更新排程
    @Scheduled(fixedRate = 30 * 60 * 1000)
    public void scheduledForexRefresh() {
        System.out.println("[排程] 外幣匯率更新 - " + LocalDateTime.now());
        refreshForexPrices();
    }

    //拿到單一價位
    public BigDecimal getPrice(int assetId) {
        BigDecimal price = priceCache.get(assetId);

        if (price == null) {
            HistoricalPrice hp = historicalPriceService.getRandomPrice(assetId);
            if (hp != null) {
                price = hp.getHpClosePrice();
                priceCache.put(assetId, price);
            }
        }

        return price;
    }

    //拿到全部價位
    public Map<Integer, BigDecimal> getAllPrices() {
        List<Asset> allAssets = assetService.findAll();
        
        for (Asset asset : allAssets) {
            if (!priceCache.containsKey(asset.getAssetId())) {
                HistoricalPrice hp = historicalPriceService.getRandomPrice(asset.getAssetId());
                if (hp != null) {
                    priceCache.put(asset.getAssetId(), hp.getHpClosePrice());
                }
            }
        }

        return new HashMap<>(priceCache);
    }

    //除錯用的
    public Map<String, Object> getCacheStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("lastStockUpdate", lastStockUpdateTime);
        status.put("lastForexUpdate", lastForexUpdateTime);
        status.put("cacheSize", priceCache.size());
        
        if (lastStockUpdateTime != null) {
            long elapsedMs = java.time.Duration.between(lastStockUpdateTime, LocalDateTime.now()).toMillis();
            status.put("stockUpdateSecondsAgo", elapsedMs / 1000);
        }
        
        if (lastForexUpdateTime != null) {
            long elapsedMs = java.time.Duration.between(lastForexUpdateTime, LocalDateTime.now()).toMillis();
            status.put("forexUpdateSecondsAgo", elapsedMs / 1000);
        }
        
        return status;
    }

    //把股票API抓到的價位跟資料庫的資產對應
    private void refreshStockPrices() {
        Map<String, BigDecimal> stockData = fetchStocksFromApi();
        
        if (stockData.isEmpty()) {
            System.out.println("未取得台股資料（可能非交易時間），保持現有快取");
            return;
        }

        List<Asset> allAssets = assetService.findAll();
        int updatedCount = 0;

        for (Asset asset : allAssets) {
            if (stockData.containsKey(asset.getAssetCode())) {
                priceCache.put(asset.getAssetId(), stockData.get(asset.getAssetCode()));
                updatedCount++;
            }
        }

        lastStockUpdateTime = LocalDateTime.now();
        System.out.println("台股價格更新完成: " + updatedCount + " 筆");
    }

    //把外幣API抓到的價位跟資料庫的資產對應
    private void refreshForexPrices() {
        Map<String, BigDecimal> forexData = fetchForexFromApi();
        
        if (forexData.isEmpty()) {
            System.out.println("未取得外幣資料，保持現有快取");
            return;
        }

        List<Asset> allAssets = assetService.findAll();
        int updatedCount = 0;

        for (Asset asset : allAssets) {
            if (forexData.containsKey(asset.getAssetCode())) {
                priceCache.put(asset.getAssetId(), forexData.get(asset.getAssetCode()));
                updatedCount++;
            }
        }

        lastForexUpdateTime = LocalDateTime.now();
        System.out.println("外幣匯率更新完成: " + updatedCount + " 筆");
    }

    //抓取外幣API的方法
    private Map<String, BigDecimal> fetchForexFromApi() {
        Map<String, BigDecimal> targetPrices = new HashMap<>();

        try {
            String url = "https://v6.exchangerate-api.com/v6/" + apiKey + "/latest/TWD";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && "success".equals(response.get("result"))) {
                Map<String, Object> rates = (Map<String, Object>) response.get("conversion_rates");

                targetPrices.put("AUD", calculateToTwd(rates.get("AUD")));
                targetPrices.put("JPY", calculateToTwd(rates.get("JPY")));
                targetPrices.put("USD", calculateToTwd(rates.get("USD")));

                System.out.println("外幣匯率抓取成功");
            }
        } catch (Exception e) {
            System.err.println("外幣 API 失敗: " + e.getMessage());
        }

        return targetPrices;
    }

    //抓取台股API的方法
    private Map<String, BigDecimal> fetchStocksFromApi() {
        Map<String, BigDecimal> stockPrices = new HashMap<>();

        try {
            Map<String, Object> response = restTemplate.getForObject(STOCK_URL, Map.class);

            if (response == null || !response.containsKey("msgArray")) {
                System.err.println("台股 API 回應異常");
                return stockPrices;
            }

            List<Map<String, Object>> msgArray = (List<Map<String, Object>>) response.get("msgArray");

            if (msgArray == null || msgArray.isEmpty()) {
                System.err.println("台股 msgArray 為空（可能非交易時間）");
                return stockPrices;
            }

            for (Map<String, Object> stockInfo : msgArray) {
                String code = stockInfo.get("c").toString().replace(".tw", "");

                Object zObj = stockInfo.get("z");
                Object yObj = stockInfo.get("y");
                String priceStr = null;
                String priceType = "";

                if (zObj != null && !"-".equals(zObj.toString()) && !zObj.toString().isEmpty()) {
                    priceStr = zObj.toString();
                    priceType = "即時價";
                } else if (yObj != null && !"-".equals(yObj.toString()) && !yObj.toString().isEmpty()) {
                    priceStr = yObj.toString();
                    priceType = "昨收價";
                }

                if (priceStr != null) {
                    BigDecimal price = new BigDecimal(priceStr).setScale(2, RoundingMode.HALF_UP);
                    stockPrices.put(code, price);
                    System.out.println("  " + code + " = " + price + " (" + priceType + ")");
                }
            }

            System.out.println("台股抓取成功: " + stockPrices.size() + " 檔");

        } catch (Exception e) {
            System.err.println("台股 API 失敗: " + e.getMessage());
        }

        return stockPrices;
    }

    //轉換數字的方法
    private BigDecimal calculateToTwd(Object rateObj) {
        if (rateObj == null) return BigDecimal.ZERO;

        Double rate = Double.valueOf(rateObj.toString());
        if (rate == 0) return BigDecimal.ZERO;

        return BigDecimal.valueOf(1.0 / rate).setScale(4, RoundingMode.HALF_UP);
    }
}