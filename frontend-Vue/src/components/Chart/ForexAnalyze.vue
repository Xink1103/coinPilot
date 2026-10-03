<script setup>
import { ref, onMounted, onUnmounted, computed, watch } from "vue";
//圓餅圖、動畫設計
import * as am5 from "@amcharts/amcharts5";
import * as am5percent from "@amcharts/amcharts5/percent";
import am5themes_Animated from "@amcharts/amcharts5/themes/Animated";

import { useSurveyStore } from "@/stores/surveyStore";
//接受Survey.vue的資料
const surveyStore = useSurveyStore();

import { useAssetStore } from "@/stores/assetStore";
const assetStore = useAssetStore();

import { useNavStore } from "@/stores/navStore";
const navStore = useNavStore();

import { usePriceStore } from '@/stores/usePriceStore';
const priceStore = usePriceStore();

import { CONST as C } from "@/libs/constant";

//設定Survey.vue接收的檔案
const analysisResult = computed(() => surveyStore.analysisResult);
const transactionHistory = computed(() => surveyStore.transactionHistory);
const totalInvestmentLimit = computed(() => surveyStore.totalInvestmentLimit);
const investedAmount = computed(() => surveyStore.investedAmount);
const answers = computed(() => surveyStore.answers);

const isLoadingData = ref(true);

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
    return t.assetType === "ForeignCurrency";
  });
});

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

//計算只有ForeignCurrency消費金額，利用前面篩選出來的交易紀錄去判斷
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
//主要用途是將多筆紀錄匯成同一筆
const holdingDetailsData = computed(() => {
  const hoildingMap = {};

  const filteredTransactions = liveTransactionHistory.value.filter((t) => {
    return t.assetType === "ForeignCurrency"; 
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
  { key: "code", label: "外幣代號", width: "w-[20%]", align: "text-center" },
  { key: "name", label: "外幣名稱", width: "w-[20%]", align: "text-center" },
  { key: "shares", label: "數量", width: "w-[13%]", align: "text-right" },
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
const pieChartRef = ref(null);

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
        type: item.type === "ForeignCurrency" ? "外幣" : item.type,
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

  // 1. 創建 AmCharts 的根物件
  pieRoot = am5.Root.new(domNode);
  pieRoot._logo.dispose();
  pieRoot.setThemes([am5themes_Animated.new(pieRoot)]);

  // 2. 創建圓餅圖
  let chart = pieRoot.container.children.push(
    am5percent.PieChart.new(pieRoot, {
      endAngle: 270,
      paddingLeft: 40, //
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
      alignLabels: true, //
    })
  );

  // 4. 標籤設定 - 只顯示類別名稱
  series.labels.template.setAll({
    text: "{category}", // 顯示類別名稱（如：日幣、美金）
    fontSize: 16,
    fontWeight: "bold",
    forceHidden: false,
    inside: false, //
    radius: 15,
    fill: am5.color(0xffffff), // 白色文字
    oversizedBehavior: "wrap",
  });

  // 5. 連接線設定 -
  series.ticks.template.setAll({
    visible: true, // 線
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

onMounted(async () => {
  
  isLoadingData.value = true;
  
  try{
    //確保資產價格先載入
    try {
      await assetStore.fetchAvailableAssets();
    } catch (e) {
      console.log("股票頁面價格載入失敗", e);
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

//圓餅圖的watch
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
  <!-- 主背景套用極深的 bg-gray-900 -->
  <div class="min-h-screen p-6 md:p-10 bg-gray-900">
    <div class="max-w-7xl mx-auto">
      
      <div v-if="isLoadingData" class="text-slate-400 text-center py-12">
        <i class="fa-solid fa-spinner fa-spin text-2xl"></i> 載入數據中...
      </div>

      <template v-else-if="transactionHistory && transactionHistory.length > 0">
        <!-- 標題區 -->
        <div class="mb-12 pt-4 border-b-4 border-cyan-400/50 pb-3">
          <h1 class="text-5xl font-extrabold text-gray-300 mb-2 tracking-wider">
            外幣投資分析
          </h1>
          <p class="text-gray-400 text-lg">深入分析您的外幣投資組合表現</p>
        </div>

        <!-- 1. 頂部總覽區 (3 張卡片) -->
        <h2 class="text-2xl font-semibold text-gray-300 mb-6 mt-8">總覽指標</h2>
        <div class="flex flex-wrap gap-6 mb-12">
          <!-- 外幣總市值卡片 - 套用 hover 效果和亮青色數字 -->
          <div
            class="bg-gray-800 rounded-xl p-8 flex-1 min-w-[300px] shadow-xl border border-gray-700 hover:scale-[1.02] hover:shadow-cyan-400/30 hover:border-cyan-400/50"
          >
            <div class="text-gray-400 text-base mb-3 uppercase tracking-widest">
              外幣總市值
            </div>
            <div class="text-cyan-400 text-5xl font-extrabold">
              {{ formatCurrency(marketValue) }}
            </div>
            <div class="text-gray-500 text-sm mt-4">
              持有{{ totalHolidingCount }}隻外幣
            </div>
          </div>

          <!-- 總投資成本卡片 -->
          <div
            class="bg-gray-800 rounded-xl p-8 flex-1 min-w-[300px] shadow-xl border border-gray-700 hover:scale-[1.02] hover:shadow-cyan-400/30 hover:border-cyan-400/50"
          >
            <div class="text-gray-400 text-base mb-3 uppercase tracking-widest">
              總投資成本
            </div>
            <div class="text-cyan-400 text-5xl font-extrabold">
              {{ formatCurrency(totalInvestmentCost) }}
            </div>
            <div class="text-gray-500 text-sm mt-4">平均持有成本</div>
          </div>

          <!-- 投資報酬率卡片 - 使用狀態色 -->
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

        <!-- 2. 分析區 (2 張圖表/表格) - 標題格式同步 -->
        <div class="flex gap-6">
          <h2 class="text-2xl font-semibold text-gray-300 mb-6 mt-8 flex-1">
            圖表分析
          </h2>
        </div>

        <div class="flex flex-col lg:flex-row gap-6 mb-12">
          <!-- 左側：外幣配置分佈圖表 - 套用卡片陰影和邊框 -->
          <div
            class="bg-gray-800 rounded-xl p-6 flex-1 shadow-xl border border-gray-700 hover:scale-[1.02] hover:shadow-cyan-400/30 hover:border-cyan-400/50"
          >
            <h3 class="text-xl font-bold text-gray-400 mb-4">外幣配置分布</h3>

            <!-- 圓餅圖區域 -->
            <div style="width: 100%; height: 250px">
              <div
                ref="pieChartRef"
                style="width: 100%; height: 100%"
                class="w-full"
              ></div>
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
                    {{ Math.round(item.value).toLocaleString()}}
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 右側：持股明細表格 - 套用卡片樣式、表格頭樣式、行 hover 樣式 -->
          <div
            class="bg-gray-800 rounded-xl p-6 flex-1 shadow-xl border border-gray-700 hover:scale-[1.02] hover:shadow-cyan-400/30 hover:border-cyan-400/50"
          >
            <h2 class="text-2xl font-bold text-gray-400 mb-6">持有明細</h2>

            <div class="space-y-4">
              <div
                v-for="item in holdingDetailsData"
                :key="item.code"
                class="bg-gray-700/50 rounded-lg p-4 border border-gray-600 hover:bg-gray-700 hover:border-cyan-400/50 transition-all duration-300 hover:shadow-lg hover:shadow-cyan-500/20"
              >
                <!-- 頂部：代碼 + 名稱 + 損益 -->
                <div
                  class="flex items-center justify-between mb-4 pb-3 border-b border-gray-600"
                >
                  <div class="flex items-center gap-3">
                    <span class="text-cyan-400 font-bold text-lg font-mono">{{
                      item.code
                    }}</span>
                    <span class="text-white text-lg font-medium">{{
                      item.name
                    }}</span>
                  </div>

                  <!-- 損益區域（加上小標籤） -->
                  <div class="text-right">
                    <span class="text-gray-400 text-xs mr-3">損益</span>
                    <span
                      :class="[
                        'text-3xl font-extrabold',
                        item.profitLoss >= 0
                          ? 'text-emerald-400'
                          : 'text-red-500',
                      ]"
                    >
                      {{ formatProfitLoss(item.profitLoss).text }} NT
                    </span>
                  </div>
                </div>

                <!-- 底部：詳細資訊 -->
                <div class="grid grid-cols-3 gap-4">
                  <div>
                    <p class="text-gray-400 text-xs mb-1">總持有數</p>
                    <p class="text-white font-semibold">
                      {{ item.shares.toFixed(1) }}
                    </p>
                  </div>
                  <div>
                    <p class="text-gray-400 text-xs mb-1">成交均價</p>
                    <p class="text-white font-semibold">
                      ${{ formatDecimal(item.buyPrice, 2) }}
                    </p>
                  </div>
                  <div>
                    <p class="text-gray-400 text-xs mb-1">當前市場價</p>
                    <p class="text-white font-semibold">
                      {{ formatPrice(item.currentPrice) }}
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 3. 詳細數據區 (近期交易紀錄) -->
        <h2 class="text-2xl font-semibold text-gray-300 mb-6 mt-8">交易明細</h2>

        <!-- 近期交易紀錄表 - 套用卡片樣式 -->
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
                placeholder="搜尋外幣代碼或名稱..."
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
                :class="[
                  header.width,
                  header.align,
                  ' whitespace-nowrap',
                  { 'pl-4': header.key === 'shares' },
                ]"
              >
                <span v-if="header.key === 'date'" class="text-gray-400">
                  {{ String(item.date).split(",")[0] }}
                </span>

                <span v-else-if="header.key === 'price'">
                  <span class="font-medium">
                    {{ formatPrice(item.price) }}
                  </span>
                </span>

                <span v-else-if="header.key === 'amount'">
                  <span class="font-medium">
                    {{ formatCurrency(item.amount) }}
                  </span>
                </span>

                <span v-else-if="header.key === 'shares'">
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
