<script setup>
import { onMounted, onUnmounted, computed, watch } from "vue";
import { ref } from "vue";
import { toast } from 'vue3-toastify';
import 'vue3-toastify/dist/index.css';

//圓餅圖、動畫設計
import * as am5 from "@amcharts/amcharts5";
import * as am5percent from "@amcharts/amcharts5/percent";
import am5themes_Animated from "@amcharts/amcharts5/themes/Animated";
//雷達跟座標設計
import * as am5radar from "@amcharts/amcharts5/radar";
import * as am5xy from "@amcharts/amcharts5/xy";

//接受Survey.vue的資料
import { CONST as C } from "@/libs/constant";

import { useSurveyStore } from "@/stores/surveyStore";
const surveyStore = useSurveyStore();

import { useAssetStore } from "@/stores/assetStore";
const assetStore = useAssetStore();

import { usePriceStore } from '@/stores/usePriceStore';
const priceStore = usePriceStore();

import { useNavStore } from "@/stores/navStore";
const navStore = useNavStore();

const pieChartRef = ref(null);
const gaugeChartRef = ref(null);

//設定Survey.vue接收回來的檔案
const analysisResult = computed(() => surveyStore.analysisResult);
const transactionHistory = computed(() => surveyStore.transactionHistory);
const totalInvestmentLimit = computed(() => surveyStore.totalInvestmentLimit);
const investedAmount = computed(() => surveyStore.investedAmount);
const answers = computed(() => surveyStore.answers);

const isLoadingData = ref(true);

//設定投資反應的卡片內容
const investmentEmotion = computed(() => {
  const sixth = answers.value[6];

  const reaction = {
    1: "高敏感型",
    2: "觀望猶豫型",
    3: "風險承擔型",
    4: "積極進取型",
  };

  return reaction[sixth] || "尚未填寫";
});

//設定投資年限的卡片
const investmentStyle = computed(() => {
  const experience = answers.value[8];

  if (!experience) return "尚未填寫";

  switch (experience) {
    case "1":
      return "新手 (經驗有限)";
    case "2":
      return "中階 (經驗尚足)";
    case "3":
      return "專業 (經驗豐富)";
    default:
      return "資料異常";
  }
});

const liveTransactionHistory = computed(() => {
  return transactionHistory.value.map(t => {
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
  });
});

//計算投資報酬率roi
const totalROI = computed(() => {
  if (investedAmount.value === 0 || liveTransactionHistory.value.length === 0) {
    return 0;
  }

  let currentTotalValue = 0;

  //使用即時價格的交易記錄
  liveTransactionHistory.value.forEach((t) => {
    const currentPrice = t.currentPrice;  
    const currentValue = currentPrice * t.shares;
    currentTotalValue = currentTotalValue + currentValue;
  });

  const roi = ((currentTotalValue - investedAmount.value) / investedAmount.value) * 100;
  return roi;
});

//客製化roi的樣式
const roiDisplay = computed(() => {
  const roi = totalROI.value;
  
  if (roi >= 0) {
    return `+${roi.toFixed(2)}`;
  } else {
    return roi.toFixed(2);
  }
});

//準備圓餅圖的資料
const pieChartData = computed(() => {
  //總共會是Q11投入的金額
  const limit = totalInvestmentLimit.value;
  //實際投入的金額
  const cost = investedAmount.value;

  //防呆
  if (limit === 0) {
    return [];
  }

  //把已投入的資產分組
  const assetCost = {
    股票: 0,
    外幣: 0,
  };

  // 計算每類資產的總投入成本 (使用 transaction.amount)
  transactionHistory.value.forEach((t) => {
    const amount = Number(t.amount) || 0;
    const assetType = t.assetType || "";

    if (assetType == "Stock/ETF") {
      assetCost["股票"] = assetCost["股票"] + amount;
    }
    if (assetType == "ForeignCurrency") {
      assetCost["外幣"] = assetCost["外幣"] + amount;
    }
  });

  //計算未投入的金額
  const money = limit - cost;

  //轉換成amchart需要的格式
  const data = [];

  //股票
  if (assetCost["股票"] > 0) {
    data.push({
      category: "股票",
      value: assetCost["股票"],
    });
  }

  //外幣
  if (assetCost["外幣"] > 0) {
    data.push({
      category: "外幣",
      value: assetCost["外幣"],
    });
  }

  //現金
  if (money > 0) {
    data.push({
      category: "現金",
      value: money,
    });
  }

  return data;
});

//投資資產占比
const investmentRatio = computed(() => {
  //簡單防呆
  if (totalInvestmentLimit.value === 0) {
    return "0.0%";
  }

  const ratio = (investedAmount.value / totalInvestmentLimit.value) * 100;

  return `${ratio.toFixed(1)}`;
});

//建議投資配置
const allocationText = computed(() => {
  const result = analysisResult.value;
  if (!result) return "無配置建議";
  
  // 計算實際的股票和外幣總和
  const actualStock = result.stocks?.reduce((sum, s) => sum + s.percentage, 0) || 0;
  const actualForex = result.forexs?.reduce((sum, f) => sum + f.percentage, 0) || 0;
  
  let parts = [];
  if (actualStock > 0) parts.push(`股票${actualStock}%`);
  if (actualForex > 0) parts.push(`外幣${actualForex}%`);
  
  return parts.join(" + ");
})

//月支出 / 緊急儲備金
const monthlyExpense = computed(() => Number(answers.value[2]) || 0);
const emergencyMoney = computed(() => Number(answers.value[3]) || 0);

//可度過的區域判斷
const reverseMonth = computed(() => {
  //簡單防呆
  if (monthlyExpense.value === 0) {
    return 0;
  }

  const month = emergencyMoney.value / monthlyExpense.value;

  return Math.min(month, 12);
});

//圖表相關的變數設定
let pieRoot = null;
let pieSeries = ref(null);
const gaugeRootRef = ref(null);

// 圓餅圖初始化函式設定
const initPieChart = (domId, data) => {
  if (pieRoot) {
    pieRoot.dispose();
  }

  pieRoot = am5.Root.new(domId);
  pieRoot._logo.dispose();
  pieRoot.setThemes([am5themes_Animated.new(pieRoot)]);

  let chart = pieRoot.container.children.push(
    am5percent.PieChart.new(pieRoot, {
      endAngle: 270,
      paddingLeft: 40,
      paddingTop: 20,
      paddingRight: 40,
      paddingBottom: 20,
    })
  );

  let series = chart.series.push(
    am5percent.PieSeries.new(pieRoot, {
      valueField: "value",
      categoryField: "category",
      endAngle: 270,
      innerRadius: am5.percent(50),
      alignLabels: true,
    })
  );

  series.labels.template.setAll({
    text: "{category}",
    fontSize: 18,
    fontWeight: "bold",
    forceHidden: false,
    inside: false,
    radius: 20,
    fill: am5.color(0xffffff),
    oversizedBehavior: "wrap",
  });

  series.slices.template.setAll({
    stroke: am5.color(0x000000),
    strokeWidth: 2,
    tooltip: series.get("tooltip"),
  });

  series.ticks.template.setAll({
    visible: true,
    strokeOpacity: 0.5,
    stroke: am5.color(0xffffff),
    strokeWidth: 1,
  });

  series.data.setAll(data);
  series.appear(1000, 60);

  return series;
};

// 儀表板初始化函式設定
const initGaugeChart = (domNode, targetValue) => {
  const GAUGE_MAX = 12;
  const GAUGE_RANGES = [
    { title: "危險", color: 0xdc3545, low: 0, high: 4 },
    { title: "良好", color: 0xffc107, low: 4, high: 8 },
    { title: "理想", color: 0x28a745, low: 8, high: 12 },
  ];

  let root = am5.Root.new(domNode);
  root._logo.dispose();
  root.setThemes([am5themes_Animated.new(root)]);

  let chart = root.container.children.push(
    am5radar.RadarChart.new(root, {
      panX: false,
      panY: false,
      startAngle: 180,
      endAngle: 360,
      innerRadius: am5.percent(70),
    })
  );

  let axisRenderer = am5radar.AxisRendererCircular.new(root, {
    innerRadius: am5.percent(70),
    strokeOpacity: 0,
    minGridDistance: 50,
  });

  axisRenderer.grid.template.setAll({
    visible: false,
  });

  axisRenderer.ticks.template.setAll({
    visible: false,
  });

  axisRenderer.labels.template.setAll({
    forceHidden: true,
  });

  let yAxis = chart.yAxes.push(
    am5xy.ValueAxis.new(root, {
      min: 0,
      max: GAUGE_MAX,
      strictMinMax: true,
      renderer: axisRenderer,
    })
  );

  am5.array.each(GAUGE_RANGES, function (data) {
    let axisRange = yAxis.createAxisRange(yAxis.makeDataItem({}));

    axisRange.setAll({
      value: data.low,
      endValue: data.high,
    });

    axisRange.get("axisFill").setAll({
      visible: true,
      fill: am5.color(data.color),
      fillOpacity: 0.9,
      stroke: am5.color(0x000000),
      strokeWidth: 2,
    });

    axisRange.get("label").setAll({
      text: data.title,
      fill: am5.color(0x1a2035),
      fontSize: 16,
      fontWeight: "bold",
      forceHidden: false,
      inside: false,
      radius: -50,
      location: 0.5,
    });
  });

  const pointerColor = am5.color(0x2c73b8);
  const backgroundColor = am5.color(0x1a2035);

  let axisDataItem = yAxis.makeDataItem({});
  axisDataItem.set("value", 0);

  let clockHand = am5radar.ClockHand.new(root, {
    radius: am5.percent(60),
    innerRadius: am5.percent(10),
    bottomWidth: 10,
    topWidth: 0,
    pinRadius: 8,
    strokeWidth: 0,
  });

  clockHand.hand.setAll({
    fill: pointerColor,
    stroke: pointerColor,
  });

  clockHand.pin.setAll({
    fill: pointerColor,
    stroke: backgroundColor,
    strokeOpacity: 1,
    strokeWidth: 3,
  });

  let bullet = axisDataItem.set(
    "bullet",
    am5xy.AxisBullet.new(root, {
      sprite: clockHand,
    })
  );

  yAxis.createAxisRange(axisDataItem);

  axisDataItem.animate({
    key: "value",
    to: targetValue,
    duration: 1000,
    easing: am5.ease.out(am5.ease.cubic),
  });

  chart.appear(1000, 100);

  return root;
};

onMounted(async () => {
  // 正確設定 loading 狀態
  isLoadingData.value = true;
  
  try {
    await assetStore.fetchAvailableAssets();

    //載入問卷結果
    if (!surveyStore.analysisResult) {
      try {
        const surveyData = await surveyStore.loadExistingSurvey();
        if (surveyData) {
          surveyStore.restoreSurveyFromBackEnd(surveyData);
        }
      } catch (e) {
        console.log("個人頁面問卷數據資料載入失敗", e);
      }
    }

    //載入交易紀錄
    if (transactionHistory.value.length === 0) {
      try {
        await surveyStore.loadUserTransactions();
      } catch (e) {
        console.log("個人頁面交易資料載入失敗", e);
      }
    }
  } finally {
    // 無論成功或失敗都要關閉 loading
    isLoadingData.value = false;
  }
});

onUnmounted(() => {
  if (pieRoot) {
    pieRoot.dispose();
    pieRoot = null;
  }
  if (gaugeRootRef.value) {
    gaugeRootRef.value.dispose();
    gaugeRootRef.value = null;
  }
});

watch(
  [reverseMonth, gaugeChartRef],
  ([newValue, newRef]) => {
    if (newRef && newValue >= 0 && !isLoadingData.value) { 
      if (gaugeRootRef.value) {
        gaugeRootRef.value.dispose();
        gaugeRootRef.value = null;
      }
      try {
        const chartRoot = initGaugeChart(newRef, newValue);
        gaugeRootRef.value = chartRoot;
      } catch (error) {
        console.error('初始化儀表板圖表失敗:', error);
      }
    }
  },
  { deep: true, immediate: true }
);

watch(
  [pieChartData, pieChartRef],
  ([newData, newRef]) => {
    if (newData.length > 0 && newRef && !isLoadingData.value) {
      if (pieSeries.value) {
        pieSeries.value.data.setAll(newData);
      } else {
        try {
          pieSeries.value = initPieChart(newRef, newData);
        } catch (error) {
          console.error('初始化圓餅圖失敗:', error);
        }
      }
    }
  },
  { deep: true, immediate: true }
);

//重作問卷的功能
const restartSurvey = async() => {
  const confirmed = window.confirm(
    '確定要重新填寫問卷嗎？\n\n' +
    '此操作將會清除：\n' +
    '問卷結果與風險評估\n' +
    '所有交易紀錄\n' +
    '系統推薦的資產配置\n\n' +
    '此操作無法復原！'
  );

  if(!confirmed){
    return;
  }

  try{
    const result = await surveyStore.resetSurvey();
    
    if(result.success){
      // 先清理圖表
      if (pieRoot) {
        pieRoot.dispose();
        pieRoot = null;
      }
      if (gaugeRootRef.value) {
        gaugeRootRef.value.dispose();
        gaugeRootRef.value = null;
      }
      
      // 使用 toast 顯示成功訊息
      toast.success(result.message, {
        autoClose: 1500,
        position: 'top-center'
      });
      
      // 延遲跳轉，讓使用者看到成功訊息
      setTimeout(() => {
        navStore.changeView(C.TOGGLE_STRATEGY);
      }, 1500);

    }else{
      toast.error(result.message, {
        autoClose: 3000,
        position: 'top-center'
      });
    }

  }catch(e){
    console.error('重做問卷過程發生錯誤:', e);
    toast.error('操作失敗，請稍後再試', {
      autoClose: 3000,
      position: 'top-center'
    });
  }
};

</script>

<template>
  <div class="min-h-screen p-6 bg-[#131620]">
    <div class="max-w-7xl mx-auto">
      
      <!-- 載入中狀態 -->
      <div v-if="isLoadingData" class="text-slate-400 text-center py-12">
        <i class="fa-solid fa-spinner fa-spin text-2xl"></i> 載入數據中...
      </div>

      <!-- 有問卷資料時顯示 -->
      <template v-else-if="analysisResult">
        <div class="mb-10 pt-4 border-b-4 border-cyan-400/50 pb-3 flex justify-between items-end">
          <div>
            <h1 class="text-4xl font-extrabold text-gray-300 mb-2 tracking-wide">
              個人財務儀表板
            </h1>
            <!-- <p class="text-gray-400 text-sm">最後更新: {{ new Date().toLocaleDateString('zh-TW') }}</p> -->
          </div>

          <!-- 重做問卷按鈕 -->
          <button
            @click="restartSurvey"
            class="px-4 sm:px-6 py-2 sm:py-3 bg-linear-to-r from-cyan-500 to-blue-500 text-white text-sm sm:text-base font-bold rounded-lg sm:rounded-xl shadow-lg hover:shadow-xl hover:scale-105 transition-all duration-300"
          >
            <span class="hidden sm:inline">重填問卷</span>
            <span class="sm:hidden">重做</span>
          </button>
        </div>

        <div class="flex flex-col lg:flex-row items-stretch gap-6 mb-10">
          <div class="flex flex-col space-y-6 lg:w-1/3">
            <div class="bg-gray-800 rounded-xl p-6 pr-8 shadow-xl border border-gray-700 transform transition-all duration-300 hover:scale-[1.02] hover:shadow-cyan-400/30 hover:border-cyan-400/50">
              <div class="text-gray-400 text-[0.9rem] mt-3 mb-4 uppercase tracking-widest">
                本次虛擬配置總額
              </div>
              <div class="flex text-cyan-400 text-5xl font-extrabold mb-2">
                <span class="text-cyan-400 text-[2.5rem] font-extrabold mb-2 whitespace-nowrap overflow-hidden text-ellipsis">
                  NT$ {{ totalInvestmentLimit.toLocaleString() }}
                </span>
              </div>
            </div>

            <div class="bg-gray-800 rounded-xl p-6 shadow-xl border border-gray-700 transform transition-all duration-300 hover:scale-[1.02] hover:shadow-cyan-400/30 hover:border-cyan-400/50">
              <div class="text-gray-400 text-base mb-2 uppercase tracking-widest">
                投資資產佔比
              </div>
              <div class="text-cyan-400 text-5xl font-extrabold mb-2">
                {{ investmentRatio }}%
              </div>
            </div>

            <div class="bg-gray-800 rounded-xl p-6 shadow-xl border border-gray-700 transform transition-all duration-300 hover:scale-[1.02] hover:shadow-cyan-400/30 hover:border-cyan-400/50">
              <div class="text-gray-400 text-base mb-2 uppercase tracking-widest">
                總報酬率
              </div>
              <div
                :class="[
                  'text-5xl font-extrabold mb-2',
                  parseFloat(roiDisplay) >= 0.0 ? 'text-emerald-400' : 'text-red-500',
                ]"
              >
                {{ roiDisplay }}%
              </div>
            </div>
          </div>

          <div class="lg:w-2/3 flex">
            <div class="bg-gray-800 flex flex-1 flex-col lg:flex-row rounded-xl p-6 shadow-xl border border-gray-700 hover:scale-[1.02] hover:shadow-cyan-400/30 hover:border-cyan-400/50">
              <div class="lg:w-3/5 w-full">
                <h2 class="text-2xl font-bold text-gray-400 mb-4">資產配置</h2>
                <div style="width: 100%; height: 350px">
                  <div ref="pieChartRef" style="width: 100%; height: 100%" class="w-full"></div>
                </div>
              </div>

              <div class="lg:w-2/5 w-full p-6 lg:ml-6 mt-6 lg:mt-10 border-t lg:border-t-0 border-gray-700">
                <h3 class="text-xl font-semibold text-gray-300 mb-5 lg:mt-13 lg:mb-7">
                  配置明細
                </h3>
                <div v-for="item in pieChartData" :key="item.category" class="flex items-center mb-4">
                  <div class="w-3 h-3 rounded-full mr-3 bg-cyan-400"></div>
                  <div class="text-gray-300 text-md font-medium flex justify-between flex-1">
                    <div class="whitespace-nowrap">{{ item.category }}</div>
                    <div class="text-white font-bold ml-4">
                      NT${{  Math.round(item.value).toLocaleString() }}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="flex flex-col lg:flex-row lg:items-stretch gap-6 mb-6">
          <div class="lg:w-2/3 flex">
            <div class="bg-gray-800 flex flex-1 flex-col rounded-xl p-6 shadow-xl border border-gray-700 hover:scale-[1.02] hover:shadow-cyan-400/30 hover:border-cyan-400/50">
              <h2 class="text-2xl font-bold text-gray-400 mb-4">
                收支結構分析
              </h2>
              <div style="width: 100%; height: 350px">
                <div ref="gaugeChartRef" style="width: 100%; height: 100%"></div>
              </div>
            </div>
          </div>

          <div class="flex flex-col gap-6 lg:w-1/3">
            <div class="bg-gray-800 rounded-xl p-6 shadow-xl border border-gray-700 transform transition-all duration-300 hover:scale-[1.02] hover:shadow-cyan-400/30 hover:border-cyan-400/50">
              <div class="text-gray-400 text-base mb-2 uppercase tracking-widest">
                風險等級
              </div>
              <div class="text-cyan-400 text-3xl font-extrabold mb-3">
                {{ analysisResult?.profileType || "尚未評估" }}
              </div>
              <div class="flex items-center">
                <div class="flex-1 bg-gray-700 rounded-full h-3 mr-4">
                  <div
                    class="bg-linear-to-r from-cyan-400 to-cyan-600 h-3 rounded-full"
                    :style="{
                      width: ((analysisResult?.riskScore || 0) / 20) * 100 + '%',
                    }"
                  ></div>
                </div>
                <div class="text-white text-md font-extrabold whitespace-nowrap">
                  {{ analysisResult?.riskScore || 0 }} / 20
                </div>
              </div>
            </div>

            <div class="bg-gray-800 rounded-xl p-6 shadow-xl border border-gray-700 transform transition-all duration-300 hover:scale-[1.02] hover:shadow-cyan-400/30 hover:border-cyan-400/50">
              <div class="text-gray-400 text-base mb-2 uppercase tracking-widest">
                投資性格分析
              </div>
              <div class="text-cyan-400 text-3xl font-extrabold mb-3">
                {{ investmentEmotion }}
              </div>
              <div class="text-gray-500 text-sm">{{ investmentStyle }}</div>
            </div>

            <div class="bg-gray-800 rounded-xl p-6 shadow-xl border border-gray-700 transform transition-all duration-300 hover:scale-[1.02] hover:shadow-cyan-400/30 hover:border-cyan-400/50">
              <div class="text-gray-400 text-base mb-3 uppercase tracking-widest">
                資產配置 建議
              </div>
              <div class="text-cyan-400 text-2xl font-extrabold mb-3">
                {{ allocationText }}
              </div>
              <div class="text-gray-500 text-sm">根據您的風險類型</div>
            </div>
          </div>
        </div>
      </template>

      <!-- 沒有問卷資料時顯示 -->
      <div v-else class="survey-container w-full flex items-center justify-center bg-gray-800 shadow-2xl rounded-2xl lg:rounded-3xl overflow-hidden border border-gray-700" style="min-height: 500px;">
        <div class="text-center px-6">
          <div class="bg-gray-800 p-8 rounded-xl">
              <div class="flex justify-center mb-4">
                  <div class="relative w-20 h-20 rounded-full border-4 border-cyan-500 bg-gray-800 flex items-center justify-center shadow-inner shadow-cyan-900/50">
                      <i class="fa-solid fa-chart-pie fa-spin text-3xl text-cyan-400"></i>
                  </div>
              </div>
          </div>

          <h2 class="text-2xl font-bold text-gray-300 mb-3">尚未填寫問卷</h2>
          <p class="text-gray-400 mb-6">請先完成理財問卷，系統將為您分析財務狀況</p>
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