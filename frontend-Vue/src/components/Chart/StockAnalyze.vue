<script setup>
import { ref, onMounted, onUnmounted, computed, watch } from "vue";
//圓餅圖、動畫設計
import * as am5 from "@amcharts/amcharts5";
import * as am5percent from "@amcharts/amcharts5/percent";
import am5themes_Animated from "@amcharts/amcharts5/themes/Animated";

//柱狀圖
import * as am5xy from "@amcharts/amcharts5/xy";

import { CONST as C } from "@/libs/constant";

import { useSurveyStore } from "@/stores/surveyStore";
const surveyStore = useSurveyStore();

import { useAssetStore } from "@/stores/assetStore";
const assetStore = useAssetStore();

import { useNavStore } from "@/stores/navStore";
const navStore = useNavStore();

import { usePriceStore } from '@/stores/usePriceStore';
const priceStore = usePriceStore();

//設定Survey.vue接收的檔案
const analysisResult = computed(() => surveyStore.analysisResult);
const transactionHistory = computed(() => surveyStore.transactionHistory);
const totalInvestmentLimit = computed(() => surveyStore.totalInvestmentLimit);
const investedAmount = computed(() => surveyStore.investedAmount);
const answers = computed(() => surveyStore.answers);

const isLoadingData = ref(true);

const pieChartRef = ref(null);

//即時更新價位
const liveTransactionHistory = computed(() => {
  return transactionHistory.value.map(t => {
    const currentPrice = priceStore.getPriceByAssetId(t.assetId) || t.currentPrice;
    
    // 重新計算損益
    const costPrice = t.price;
    const quantity = t.shares;
    const profitAmount = (currentPrice - costPrice) * quantity;
    const profitRate = costPrice !== 0 ? ((currentPrice - costPrice) / costPrice) * 100 : 0;
    
    return {
      ...t,
      currentPrice: currentPrice,  
      profit: profitAmount,
      profitRate: profitRate
    };
  });
});

//篩選出只有股票跟ETF的交易紀錄
const stockAndEtfHolding = computed(() => {
  return transactionHistory.value.map(t => {
    //從 priceStore 取得即時價格
    const currentPrice = priceStore.getPriceByAssetId(t.assetId) || t.currentPrice;
    
    const costPrice = t.price;
    const quantity = t.shares;
    const profitAmount = (currentPrice - costPrice) * quantity;
    const profitRate = costPrice !== 0 ? ((currentPrice - costPrice) / costPrice) * 100 : 0;
    
    return {
      ...t,
      currentPrice: currentPrice,
      profit: profitAmount,
      profitRate: profitRate
    };
  }).filter((t) => {
    return t.assetType === "Stock/ETF";
  });
});

//計算只有股票跟ETF的消費金額，利用前面篩選出來的交易紀錄去判斷
const totalInvestmentCost = computed(() => {
  //利用reduce把錢全部累加起來
  return stockAndEtfHolding.value.reduce((sum, current) => {
    return sum + current.amount;
  }, 0);
});

//計算有幾種股票
const totalHolidingCount = computed(() => {
  //用map取出所有的code欄位
  const code = stockAndEtfHolding.value.map((t) => t.code);

  const uniqueCode = new Set(code);

  return uniqueCode.size;
});

//持股明細表的資料
// 用 liveTransactionHistory 
const holdingDetailsData = computed(() => {
  const hoildingMap = {};

  const filteredTransactions = liveTransactionHistory.value.filter((t) => {
    return t.assetType === "Stock/ETF"; 
  });

  filteredTransactions.forEach((t) => {
    const { code, name, shares, amount, currentPrice } = t;

    if (!hoildingMap[code]) {
      hoildingMap[code] = {
        code: code,
        name: name,
        shares: 0,
        totalCost: 0,
        totalCurrentValue: 0,
      };
    }

    hoildingMap[code].shares += shares;
    hoildingMap[code].totalCost += amount;
    hoildingMap[code].totalCurrentValue += shares * currentPrice; 
  });

  return Object.values(hoildingMap).map((holding) => {
    const weightedBuyPrice = holding.totalCost / holding.shares;
    const weightedCurrentPrice = holding.totalCurrentValue / holding.shares;
    const profitLossTotal = holding.totalCurrentValue - holding.totalCost;

    return {
      code: holding.code,
      name: holding.name,
      shares: holding.shares,
      buyPrice: weightedBuyPrice,
      currentPrice: weightedCurrentPrice, 
      profitLoss: profitLossTotal,
      totalCost: holding.totalCost
    };
  });
});

//算股票市值 : 股票數量 * 市場價格
const marketValue = computed(() => {
  return Object.values(holdingDetailsData.value).reduce((sum, current) => {
    return sum + current.shares * current.currentPrice;
  }, 0);
});

//統一用持股明細去算總市值
const stockValue = computed(() => {
  return marketValue.value - totalInvestmentCost.value;
});

//計算投資報酬率 利用前面兩張卡片的fn
const stockRoi = computed(() => {
  //簡單防呆
  if (
    totalInvestmentCost.value === 0 ||
    totalInvestmentCost === undefined ||
    totalInvestmentCost.value === null
  ) {
    return 0;
  }

  return (
    ((marketValue.value - totalInvestmentCost.value) /
      totalInvestmentCost.value) *
    100
  );
});

//交易明細表的表格標頭
const transactionHeaders = [
  { key: "date", label: "日期", width: "w-[20%]", align: "text-left" },
  { key: "code", label: "股票代號", width: "w-[20%]", align: "text-center" },
  { key: "name", label: "股票名稱", width: "w-[20%]", align: "text-center" },
  { key: "shares", label: "持股數", width: "w-[13%]", align: "text-right" },
  { key: "price", label: "成交價", width: "w-[13%]", align: "text-right" },
  { key: "amount", label: "扣款金額", width: "w-[14%]", align: "text-right" },
];

//搜尋功能
const searchQuery = ref("");

//過濾交易功能
const filterTranscations = computed(() => {
  if (!searchQuery.value) {
    return stockAndEtfHolding.value;
  }

  const qurey = searchQuery.value.toLowerCase();

  return stockAndEtfHolding.value.filter((item) => {
    return (
      item.code.toLowerCase().includes(qurey) ||
      item.name.toLowerCase().includes(qurey)
    );
  });
});

//綁定 DOM 元素的引用
const chartBarRef = ref(null);
let rootBar = null;
let pieRoot = null;
let stockPieSeries = ref(null);

//準備圓餅圖的資料
const stockPieChartData = computed(() => {
  //分母:總投入成本
  const totalCost = totalInvestmentCost.value;

  //防呆
  if (totalCost === 0) {
    return [];
  }

  //把相同的股票代碼的金錢加總並確定有股票名稱跟代碼
  const stockAmountTotal = stockAndEtfHolding.value.reduce((sum, current) => {
    //交易的金額
    const currentAmount = current.amount;

    //交易的名稱跟code
    const code = current.code;
    const name = current.name;
    const type = current.assetType;

    if (!sum[code]) {
      sum[code] = {
        category: name,
        code: code,
        type: type,
        value: 0,
      };
    }

    //類加相同的股票金額
    sum[code].value = sum[code].value + currentAmount;

    return sum;
  }, {});

  //轉換成amchart需要的格式
  const data = Object.values(stockAmountTotal)
    // 過濾掉成本為 0 的資料，確保圖表乾淨
    .filter((item) => item.value > 0)
    .map((item) => {
      // 這裡的 value 即為該個股的總投入成本
      return {
        category: item.category, // 股票名稱 (例如：台積電)
        value: item.value, // 總投入成本
        code: item.code,
        type: item.type === "Stock/ETF" ? "股票/ETF" : item.type,
      };
    });
  return data;
});

// 圓餅圖初始化函式設定
const initPieChart = (domNode, data) => {
  // 防呆有的話drop掉
  if (pieRoot) {
    pieRoot.dispose();
  }

  // 1. 創建 AmCharts 的根物件 (使用 pieRoot)
  pieRoot = am5.Root.new(domNode);
  pieRoot._logo.dispose();
  pieRoot.setThemes([am5themes_Animated.new(pieRoot)]);

  // 2. 創建圓餅圖
  let chart = pieRoot.container.children.push(
    am5percent.PieChart.new(pieRoot, {
      endAngle: 270,
      paddingLeft: 40, // 減少 padding 因為空間較小
      paddingRight: 40,
      paddingTop: 20,
      paddingBottom: 20,
    })
  );

  // 3. 創建 Series
  let series = chart.series.push(
    am5percent.PieSeries.new(pieRoot, {
      valueField: "value",
      categoryField: "category",
      endAngle: 270,
      innerRadius: am5.percent(50),
      alignLabels: true, // 改為 true，讓標籤對齊
    })
  );

  // 4. 標籤設定 - 外部標籤模式
  series.labels.template.setAll({
    text: "{category}", // 只顯示類別名稱
    fontSize: 16, // 調小字體因為空間較小
    fontWeight: "bold",
    forceHidden: false,
    inside: false, // 標籤放外面
    radius: 15, // 調整標籤距離
    fill: am5.color(0xffffff), // 白色文字
    oversizedBehavior: "wrap",
  });

  // 5. 連接線設定 顯示引線
  series.ticks.template.setAll({
    visible: true, //  顯示連接線
    strokeOpacity: 0.5,
    stroke: am5.color(0xffffff),
    strokeWidth: 1,
  });

  // 6. 設定邊框樣式
  series.slices.template.setAll({
    stroke: am5.color(0x000000),
    strokeWidth: 2,
    tooltip: series.get("tooltip"),
  });

  // 7. 設定數據
  series.data.setAll(data);

  // 8. 初始動畫
  series.appear(1000, 60);

  // 這樣才會連動更新
  return series;
};

//柱狀圖來源資料
const allStocks = computed(() => assetStore.availableStocks);

//準備柱狀圖的資料
const barChartData = computed(() => {
  // 1. 檢查 allStocks 是否有資料
  if (!allStocks.value || allStocks.value.length === 0) {
    return [];
  }

  const dynamicIndustryMap = {};
  allStocks.value.forEach((asset) => {
    if (asset.assetCode && asset.stockIndustryType) {
        dynamicIndustryMap[asset.assetCode] = asset.stockIndustryType;
    }
  });

  const industryTotals = {};

  stockAndEtfHolding.value.forEach((t) => {
    const code = t.code;
    const amount = t.amount;
    const industry = dynamicIndustryMap[code]; 

    if (!industry) {
      console.log(`找不到代碼 ${code} 的產業資料，請確認 allStocks 裡是否有此代碼`);
    }

    if (industry) {
      if (!industryTotals[industry]) {
        industryTotals[industry] = 0;
      }
      industryTotals[industry] += amount;
    }
  });

  const chartData = Object.keys(industryTotals).map((key) => ({
    network: key,
    value: industryTotals[key],
  }));
  return chartData;
});

//柱狀圖函式設定
function createBarChart() {
  //轉換物件
  const chartData = barChartData.value;

  //設定文字顏色
  const axisLabelColor = am5.color(0x9ca3af);

  // 建立 Root 元素
  rootBar = am5.Root.new(chartBarRef.value);
  rootBar._logo.dispose();

  // 設定主題
  rootBar.setThemes([am5themes_Animated.new(rootBar)]);

  // 建立 XY Chart 容器
  let chart = rootBar.container.children.push(
    am5xy.XYChart.new(rootBar, {
      // 禁用平移功能
      panX: false,
      panY: false,
      wheelX: "none",
      wheelY: "none",
      paddingLeft: 20,
      paddingBottom: 70,
    })
  );
  chart.zoomOutButton.set("forceHidden", true);

  // 建立 Y 軸 (分類軸，顯示產業名稱)
  let yRenderer = am5xy.AxisRendererY.new(rootBar, {
    minGridDistance: 30,
    minorGridEnabled: true,
  });
  yRenderer.grid.template.set("location", 1);

  // 調整 Y 軸的網格線與標籤顏色
  yRenderer.grid.template.setAll({
    stroke: am5.color(0x9ca3af), // 網格線顏色略深
    strokeOpacity: 0.1,
  });
  yRenderer.labels.template.setAll({
    fill: axisLabelColor, // 標籤文字顏色
  });

  let yAxis = chart.yAxes.push(
    am5xy.CategoryAxis.new(rootBar, {
      maxDeviation: 0,
      categoryField: "network",
      renderer: yRenderer,
      tooltip: am5.Tooltip.new(rootBar, { themeTags: ["axis"] }),
    })
  );

  // 建立 X 軸 (數值軸，顯示投資金額)
  let xAxisRenderer = am5xy.AxisRendererX.new(rootBar, {
    strokeOpacity: 0.1,
    minGridDistance: 80,
  });

  // 調整 X 軸的網格線與標籤顏色
  xAxisRenderer.grid.template.setAll({
    stroke: am5.color(0x9ca3af), // 網格線顏色略深
    strokeOpacity: 0.1,
  });

  //隱藏 X 軸下方的數字標籤
  xAxisRenderer.labels.template.setAll({
    fill: axisLabelColor,
    forceHidden: false,
  });

  //隱藏 X 軸的刻度線
  xAxisRenderer.ticks.template.setAll({
    forceHidden: true,
  });

  // 建立 X 軸 (數值軸，顯示投資金額)
  let xAxis = chart.xAxes.push(
    am5xy.ValueAxis.new(rootBar, {
      maxDeviation: 0,
      min: 0,
      // 數值格式化 (例如: 1000000 顯示為 1M)
      numberFormatter: am5.NumberFormatter.new(rootBar, {
        numberFormat: "#,###a",
      }),
      extraMax: 0.1,
      renderer: xAxisRenderer,
    })
  );

  // 建立 Series (柱狀圖)
  let series = chart.series.push(
    am5xy.ColumnSeries.new(rootBar, {
      name: "投資金額",
      xAxis: xAxis,
      yAxis: yAxis,
      valueXField: "value", // X 軸數值
      categoryYField: "network", // Y 軸分類
      tooltip: am5.Tooltip.new(rootBar, {
        pointerOrientation: "left",
        labelText: "{categoryY}: {valueX}", // 提示文字內容
      }),
    })
  );

  // 設定柱子樣式
  series.columns.template.setAll({
    cornerRadiusTR: 5, // 右上圓角
    cornerRadiusBR: 5, // 右下圓角
    strokeOpacity: 0, // 無邊框
  });

  // 柱子顏色適配器 (讓每個柱子不同色)
  series.columns.template.adapters.add("fill", (fill, target) =>
    chart.get("colors").getIndex(series.columns.indexOf(target))
  );
  series.columns.template.adapters.add("stroke", (stroke, target) =>
    chart.get("colors").getIndex(series.columns.indexOf(target))
  );

  // 將數據設定給軸和 Series
  yAxis.data.setAll(chartData);
  series.data.setAll(chartData);

  // 排序邏輯 (確保圖表上的項目順序與數值大小一致)
  function sortCategoryAxis() {
    // 依據數值大小排序 Series 資料
    series.dataItems.sort((x, y) => x.get("valueX") - y.get("valueX"));

    // 依據 Series 排序結果，更新 Y 軸資料項目的索引
    yAxis.dataItems.sort((x, y) => {
      const indexX = series.dataItems.findIndex(
        (d) => d.get("categoryY") === x.get("category")
      );
      const indexY = series.dataItems.findIndex(
        (d) => d.get("categoryY") === y.get("category")
      );
      return indexX - indexY;
    });
  }
  sortCategoryAxis(); // 初始排序

  // 載入時動畫
  series.appear(1000);
  chart.appear(1000, 100);

  // 設置游標
  chart.set(
    "cursor",
    am5xy.XYCursor.new(rootBar, {
      behavior: "none",
      xAxis: xAxis,
      yAxis: yAxis,
    })
  );
}

onMounted(async () => {
  
  isLoadingData.value = true;

  try{

    if(transactionHistory.value.length == 0){
      //確保資產價格先載入
      try {
        await assetStore.fetchAvailableAssets();
      } catch (e) {
        console.log("股票頁面價格載入失敗", e);
      }
    }
  
    //載入交易紀錄
    if (transactionHistory.value.length == 0) {
      try {
        await surveyStore.loadUserTransactions();
      } catch (e) {
        console.log("股票頁面載入價格失敗", e);
      }
    }

  }finally{
    isLoadingData.value = false;
  }

});

//圓餅圖的初始化/更新
watch(
  [stockPieChartData, pieChartRef], // 同時監聽資料和 DOM 元素
  ([newData, newRef]) => {
    // 資料有長度 + DOM 已經出現 + 尚未初始化 root -> 初始化
    if (newData.length > 0 && newRef && !pieRoot) {
      stockPieSeries.value = initPieChart(newRef, newData);
    } 
    // 已經初始化過 -> 只需要更新數據
    else if (stockPieSeries.value && newData.length > 0) {
      stockPieSeries.value.data.setAll(newData);
    }
  },
  { deep: true } // 拿掉 immediate: true，因為我們要等 ref 有值
);

//柱狀圖的初始化
watch(chartBarRef, (newValue) => {
  // 只有當 chartBarRef.value 從 null 變為 Element 時才執行
  if (newValue && !rootBar) {
    createBarChart();
  }
});

//柱狀圖的更新
watch(
  barChartData,
  (newData) => {
    // 檢查全域的 root 實例是否存在
    if (rootBar) {
      // 取得圖表容器內的第一個物件 (這是我們建立的 Chart 實例)
      const chart = rootBar.container.children.values[0];

      // 取得 Series 和 Y 軸 (Category Axis)
      const series = chart?.series.values[0];
      const yAxis = chart?.yAxes.values[0];

      if (series && yAxis) {
        // 只需要對 Series 和 Y 軸的資料進行 setAll
        series.data.setAll(newData);
        yAxis.data.setAll(newData);
      }
    }
  },
  { deep: true }
);

// ------- 格式化字串區段 ----------
const formatNumber = (num) => {
  const absNum = Math.abs(num);
  
  // 小數（日圓等）：保留 4 位小數
  if (absNum < 1 && absNum > 0) {
    return absNum.toLocaleString("zh-TW", {
      minimumFractionDigits: 2,
      maximumFractionDigits: 4,
    });
  }
  
  // 中等數字：保留 2 位小數
  if (absNum < 100) {
    return absNum.toLocaleString("zh-TW", {
      minimumFractionDigits: 0,
      maximumFractionDigits: 2,
    });
  }
  
  // 大額數字（≥ 100）：不顯示小數
  return absNum.toLocaleString("zh-TW", {
    maximumFractionDigits: 0,
  });
};

//數字前面+$
const formatCurrency = (value) => {
  return `$${formatNumber(value)}`;
};

// 成交價專用格式化
const formatPrice = (price) => {
  const absPrice = Math.abs(price);
  
  // 小額價格（日圓）：保留 4 位小數
  if (absPrice < 1) {
    return `$${absPrice.toLocaleString("zh-TW", {
      minimumFractionDigits: 2,
      maximumFractionDigits: 4,
    })}`;
  }
  
  // 一般價格：保留 2 位小數
  return `$${absPrice.toLocaleString("zh-TW", {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  })}`;
};

//保留小數點後兩位的格式化函式
const formatDecimal = (num, digits = 1) => {
  return Math.abs(num).toLocaleString("zh-TW", {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits,
  });
};

//數字>0樣式綁定綠色 <0樣式綁定紅色
const formatProfitLoss = (value) => {
  const isPositive = value >= 0;
  const sign = isPositive ? "+" : "-";
  return {
    text: `${sign}${formatDecimal(value, 1)}`,
    class: isPositive ? "text-green-400" : "text-red-400",
  };
};

//投資報酬率的字串樣式設定
const formatReturnRate = (rate) => {
  if (rate === null || rate === undefined || isNaN(rate)) {
    return "0.0%";
  }
  const fixedRate = rate.toFixed(2);
  return `${rate >= 0 ? "+" : ""}${fixedRate}%`;
};
// ------- 格式化字串區段 ----------
</script>

<template>
  <div class="min-h-screen p-6 md:p-10 bg-gray-900">
    <div class="max-w-7xl mx-auto">
      
      <div v-if="isLoadingData" class="text-slate-400 text-center py-12">
        <i class="fa-solid fa-spinner fa-spin text-2xl"></i> 載入數據中...
      </div>

      <template v-else-if="transactionHistory && transactionHistory.length > 0">
        <div class="mb-12 pt-4 border-b-4 border-cyan-400/50 pb-3">
          <h1 class="text-5xl font-extrabold text-gray-300 mb-2 tracking-wider">
            股票投資分析
          </h1>
          <p class="text-gray-400 text-lg">深入分析您的股票投資組合表現</p>
        </div>

        <h2 class="text-2xl font-semibold text-gray-300 mb-6 mt-8">總覽指標</h2>
        <div class="flex flex-wrap gap-6 mb-12">
          <div
            class="bg-gray-800 rounded-xl p-8 flex-1 min-w-[300px] shadow-2xl border border-gray-700 hover:scale-[1.02] hover:shadow-cyan-400/30 hover:border-cyan-400/50"
          >
            <div class="text-gray-400 text-base mb-3 uppercase tracking-widest">
              股票總市值
            </div>
            <div class="text-cyan-400 text-5xl font-extrabold">
              {{ formatCurrency(marketValue) }}
            </div>
            <div class="text-gray-500 text-sm mt-4">
              持有{{ totalHolidingCount }}檔股票
            </div>
          </div>

          <div
            class="bg-gray-800 rounded-xl p-8 flex-1 min-w-[300px] shadow-2xl border border-gray-700 hover:scale-[1.02] hover:shadow-cyan-400/30 hover:border-cyan-400/50"
          >
            <div class="text-gray-400 text-base mb-3 uppercase tracking-widest">
              總投資成本
            </div>
            <div class="text-cyan-400 text-5xl font-extrabold">
              {{ formatCurrency(totalInvestmentCost) }}
            </div>
            <div class="text-gray-500 text-sm mt-4">平均持有成本</div>
          </div>

          <div
            class="bg-gray-800 rounded-xl p-8 flex-1 min-w-[300px] shadow-xl border border-gray-700 hover:scale-[1.02] hover:shadow-cyan-400/30 hover:border-cyan-400/50"
          >
            <div class="text-gray-400 text-base mb-3 uppercase tracking-widest">
              投資報酬率
            </div>
            <div
              :class="[
                'text-6xl font-extrabold',
                stockRoi >= 0 ? 'text-emerald-400' : 'text-red-500', // 使用高亮狀態色
              ]"
            >
              {{ formatReturnRate(stockRoi) }}
            </div>
            <div class="text-gray-500 text-sm mt-4">目前投資績效</div>
          </div>
        </div>

        <div class="flex gap-6">
          <h2 class="text-2xl font-semibold text-gray-300 mb-6 flex-1">
            圖表分析
          </h2>
        </div>

        <div class="flex flex-col lg:flex-row gap-6 mb-12">
          <div
            class="bg-gray-800 rounded-xl p-6 flex-1 shadow-xl border border-gray-700 hover:scale-[1.02] hover:shadow-cyan-400/30 hover:border-cyan-400/50"
          >
            <h3 class="text-xl font-bold text-gray-400 mb-4">持股配置分佈</h3>

            <!-- 圓餅圖區域 -->
            <div style="width: 100%; height: 250px">
              <div ref="pieChartRef" class="w-full h-full"></div>
            </div>

            <!-- 配置明細 -->
            <div class="mt-6 pt-4 border-t border-gray-700">
              <h4 class="text-lg font-semibold text-gray-300 mb-4">配置明細</h4>

              <div
                v-for="item in stockPieChartData"
                :key="item.category"
                class="flex items-center mb-3"
              >
                <div class="w-3 h-3 rounded-full mr-3 bg-cyan-400"></div>

                <div
                  class="text-gray-300 text-base font-medium flex justify-between flex-1"
                >
                  <div class="whitespace-nowrap">{{ item.category }}</div>
                  <div class="text-white font-bold ml-4">
                    {{ Math.round(item.value).toLocaleString() }}
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div
            class="bg-gray-800 rounded-xl p-6 flex-1 shadow-xl border border-gray-700 hover:scale-[1.02] hover:shadow-cyan-400/30 hover:border-cyan-400/50"
          >
            <h3 class="text-xl font-bold text-gray-400 mb-4">產業分佈分析</h3>

            <div style="width: 100%; height: 100%">
              <div ref="chartBarRef" class="w-full h-full"></div>
            </div>
          </div>
        </div>

        <h2 class="text-2xl font-semibold text-gray-300 mb-6 mt-8">
          交易與持股明細
        </h2>

        <div
          class="bg-gray-800 rounded-xl p-8 mb-8 shadow-xl border border-gray-700 hover:scale-[1.02] hover:shadow-cyan-400/30 hover:border-cyan-400/50"
        >
          <h2 class="text-2xl font-bold text-gray-400 pb-3">持股明細</h2>

          <!-- 表頭 -->
          <div
            class="flex text-cyan-400 text-sm font-bold border-b border-gray-600 py-3 mb-3 uppercase tracking-wider"
          >
            <span class="w-1/6 text-center">代碼</span>
            <span class="w-1/6 text-center">名稱</span>
            <span class="w-1/6 text-center">當前市場價</span>
            <span class="w-1/6 text-center">成交均價</span>
            <span class="w-1/6 text-center">總持有股數</span>
            <span class="w-1/6 text-center">總損益</span>
          </div>

          <div class="space-y-3">
            <!-- 資料列 -->
            <div
              v-for="item in holdingDetailsData"
              :key="item.code"
              class="flex items-center text-white text-sm py-3 rounded-lg transition duration-300 ease-in-out hover:bg-cyan-900/40 hover:shadow-xl hover:shadow-cyan-500/10 hover:ring-1 hover:ring-cyan-400/50"
            >
              <span class="w-1/6 font-mono text-center">{{ item.code }}</span>
              <span class="w-1/6 text-center">{{ item.name }}</span>

              <span class="w-1/6 text-white text-center font-medium">
                ${{ formatDecimal(item.currentPrice, 1) }}
              </span>

              <span class="w-1/6 text-white text-center font-bold">
                ${{ formatDecimal(item.buyPrice, 2) }}
              </span>

              <span class="w-1/6 text-white text-center">
                {{item.shares.toFixed(1)}}
              </span>

              <span
                :class="[
                  'w-1/6',
                  'font-extrabold',
                  'text-center',
                  item.profitLoss >= 0 ? 'text-emerald-400' : 'text-red-500',
                ]"
              >
                {{ formatProfitLoss(item.profitLoss).text }} NT
              </span>
            </div>
          </div>
        </div>

        <!-- 近期交易紀錄 -->
        <div
          class="bg-gray-800 rounded-xl p-8 shadow-xl border border-gray-700 hover:scale-[1.02] hover:shadow-cyan-400/30 hover:border-cyan-400/50"
        >
          <h3
            class="text-2xl font-bold text-gray-400 border-gray-700 pb-3 mb-2"
          >
            近期交易紀錄
          </h3>

          <div class="flex flex-col sm:flex-row gap-4 mb-6">
            <div class="flex-1 relative">
              <input
                type="text"
                placeholder="搜尋股票代碼或名稱..."
                class="w-full bg-gray-700 text-gray-300 rounded-lg py-2.5 pl-10 pr-4 focus:outline-none focus:ring-2 focus:ring-cyan-400 focus:bg-gray-700/70 placeholder-gray-500"
                v-model="searchQuery"
              />
              <i
                class="fa-solid fa-magnifying-glass absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-500"
              ></i>
            </div>
          </div>

          <div
            class="flex text-cyan-400 font-bold text-sm border-b border-gray-600 py-3 mb-3 tracking-wider uppercase"
          >
            <div
              v-for="header in transactionHeaders"
              :key="header.key"
              :class="[header.width, header.align, 'pl-4', 'pr-2']"
            >
              {{ header.label }}
            </div>
          </div>

          <div class="space-y-3">
            <div
              v-for="item in filterTranscations"
              :key="item.id"
              class="flex items-center text-white text-sm py-3 px-4 rounded-lg transition duration-300 ease-in-out hover:bg-cyan-900/40 hover:shadow-xl hover:shadow-cyan-500/10 hover:ring-1 hover:ring-cyan-400/50"
            >
              <div
                v-for="header in transactionHeaders"
                :key="header.key"
                :class="[header.width, header.align, ' whitespace-nowrap']"
              >
                <span v-if="header.key === 'date'" class="text-gray-400">
                  {{ String(item.date).split(",")[0] }}
                </span>

                <span v-else-if="['price', 'amount'].includes(header.key)">
                  <span class="font-medium">
                    ${{ formatNumber(item[header.key]) }}
                  </span>
                </span>

                <span v-else-if="header.key === 'shares'" class="pr-2">
                  {{ formatDecimal(item.shares, 1) }}
                </span>

                <span v-else>
                  {{ item[header.key] }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </template>

      <div v-else class="survey-container w-full flex items-center justify-center bg-gray-800 shadow-2xl rounded-2xl lg:rounded-3xl overflow-hidden border border-gray-700" style="min-height: 500px;">
        <div class="text-center px-6">
          <div class="bg-gray-800 p-8 rounded-xl">
              <div class="flex justify-center mb-4">
                  <div class="relative w-20 h-20 rounded-full border-4 border-cyan-500 bg-gray-800 flex items-center justify-center shadow-inner shadow-cyan-900/50">
                      <i class="fa-solid fa-chart-pie fa-spin text-3xl text-cyan-400"></i>
                  </div>
              </div>
          </div>

          <h2 class="text-2xl font-bold text-gray-300 mb-3">尚未下單</h2>
          <p class="text-gray-400 mb-6">請先完成虛擬下單，系統將為您分析財務狀況</p>
          <button
              @click="navStore.changeView(C.TOGGLE_STRATEGY)"
              class="px-6 py-3 bg-linear-to-r from-cyan-500 to-blue-500 text-white font-bold rounded-xl shadow-lg hover:shadow-xl hover:scale-105 transition-all duration-300"
          >
              前往填寫問卷
          </button>
        </div>
      </div>

    </div>
  </div>
</template>
