<script setup>
import { ref, computed, onMounted } from 'vue';
import InvestmentModal from './InvestmentModal.vue';
import { toast } from 'vue3-toastify';
import 'vue3-toastify/dist/index.css';

import { useSurveyStore } from '@/stores/surveyStore';
const surveyStore = useSurveyStore(); // 取得 Store 實例
import { useNavStore } from '@/stores/navStore';
const navStore = useNavStore();
import { usePriceStore } from '@/stores/usePriceStore';
const priceStore = usePriceStore();
import { useAssetStore } from '@/stores/assetStore';
const assetStore = useAssetStore();

const survey = ref([
    {
        id: 1,
        question: "您的年齡層是?",
        options: [
            { option: "A. 20歲以下", survey_value: "20歲以下" },
            { option: "B. 21~35歲", survey_value: "21-35" },
            { option: "C. 36~50歲", survey_value: "36-50" },
            { option: "D. 51~65歲", survey_value: "51-65" },
            { option: "E. 65歲以上", survey_value: "65歲以上" }
        ],
        type: "radio"
    },
    {
        id: 2,
        question: "您的每月固定支出為(以新台幣計算)?",
        options: [
            { option: "A. 3萬元以下", survey_value: "30000" },
            { option: "B. 3萬~5萬元", survey_value: "40000" },
            { option: "C. 5萬~8萬元", survey_value: "65000" },
            { option: "D. 8萬元以上", survey_value: "90000" }
        ],
        type: "radio"
    },
    {
        id: 3,
        question: "您目前已有的緊急預備金總額為(以新台幣計算)?",
        options: [
            { option: "A. 10萬元以下", survey_value: "100000" },
            { option: "B. 10萬~30萬元", survey_value: "200000" },
            { option: "C. 30萬~60萬元", survey_value: "450000" },
            { option: "D. 60萬元以上", survey_value: "700000" }
        ],
        type: "radio"
    },
    {
        id: 4,
        question: "您的平均月儲蓄金額為(以新台幣計算)?",
        options: [
            { option: "A. 1萬元以下", survey_value: "10000" },
            { option: "B. 1萬~3萬元", survey_value: "20000" },
            { option: "C. 3萬~5萬元", survey_value: "40000" },
            { option: "D. 5萬元以上", survey_value: "50000" }
        ],
        type: "radio"
    },
    {
        id: 5,
        question: "您對投資風險的接受程度?",
        options: [
            { option: "A. 非常保守", survey_value: "1" },
            { option: "B. 稍微保守", survey_value: "2" },
            { option: "C. 平衡型", survey_value: "3" },
            { option: "D. 積極型", survey_value: "4" },
            { option: "E. 高風險高報酬", survey_value: "5" }
        ],
        type: "radio"
    },
    {
        id: 6,
        question: "若投資短期內下跌20%,您的反應是?",
        options: [
            { option: "A. 立刻賣出", survey_value: "1" },
            { option: "B. 觀望", survey_value: "2" },
            { option: "C. 繼續持有", survey_value: "3" },
            { option: "D. 加碼投資", survey_value: "4" }
        ],
        type: "radio"
    },
    {
        id: 7,
        question: "您最偏好投資的資產類型是?",
        options: [
            { option: "A. 股票 - 追求資本成長與較高報酬", survey_value: "Stock" },
            { option: "B. ETF - 分散風險的指數投資", survey_value: "ETF" },
            { option: "C. 外幣 - 匯率收益與資產保值", survey_value: "ForeignCurrency" }
        ],
        type: "radio"
    },
    {
        id: 8,
        question: "您的投資經驗程度?",
        options: [
            { option: "A. 新手 (幾乎沒經驗)", survey_value: "1" },
            { option: "B. 中等 (有過幾次操作)", survey_value: "2" },
            { option: "C. 專業 (熟悉多種工具與市場)", survey_value: "3" }
        ],
        type: "radio"
    },
    {
        id: 9,
        question: "您的理財目標是?",
        options: [
            { option: "A. 保本為主", survey_value: "CapitalPreservation" },
            { option: "B. 穩定收益", survey_value: "StableIncome" },
            { option: "C. 高報酬追求", survey_value: "HighReturn" }
        ],
        type: "radio"
    },
    {
        id: 10,
        question: "如果您有一筆額外資金(50萬元),且不影響生活開支,您會怎麼處理?",
        options: [
            { option: "A. 全部存進銀行或買保守型產品", survey_value: "1" },
            { option: "B. 部分存起來、部分投入比較穩健的投資", survey_value: "3" },
            { option: "C. 全部投入較高風險、可能高報酬的投資", survey_value: "5" }
        ],
        type: "radio"
    },
    {
        id: 11,
        question: "您打算投入多少資金作為虛擬投資的起始金額?",
        options: [
            { option: "請輸入金額 (新台幣)", survey_value: "input" }
        ],
        type: "input_number"
    }
]);

// --- 元件本地狀態 (導覽控制) ---
const currentQuestionIndex = ref(0); // 當前題號

// --- Computed 屬性 (基於本地狀態和 Pinia 狀態) ---
const questionLength = computed(function () {
    return survey.value.length
});

const currentQuestion = computed(function () {
    return survey.value[currentQuestionIndex.value]
});

const progress = computed(function () {
    return ((currentQuestionIndex.value + 1) / questionLength.value) * 100;
});

// --- 核心方法 (與 Pinia Action 互動) ---

// 儲存答案：呼叫 Pinia Action 進行狀態更新
const handleOptionClick = function (optionValue) {
    surveyStore.setAnswer(currentQuestion.value.id, optionValue);
};

// 處理畫面勾選狀態：直接讀取 Pinia State (Pinia 在 <script setup> 中會自動解包)
const isOptionSelected = function (questionId, optionValue) {
    return surveyStore.answers[questionId] === optionValue;
};

// 上一題 (本地導覽控制)
const gotoBefore = () => {
    if (currentQuestionIndex.value > 0) {
        currentQuestionIndex.value--;
    }
};

// 防禦性驗證：確認答案都有填才會可以按下一題 (使用 Pinia State)
const isCurrentQuestionAnswered = computed(() => {
    const currentId = currentQuestion.value.id;
    // 使用 surveyStore.answers 替代本地 answers
    const answer = surveyStore.answers[currentId];

    if (currentQuestion.value.type === 'input_number') {
        return answer && Number(answer) > 0;
    } else {
        return answer !== undefined && answer !== null && answer !== '';
    }
});

// 下一題
const gotoNextorSubmit = () => {
    if (!isCurrentQuestionAnswered.value) {
        toast.warning("請先完成當前題目！");
        return;
    }

    if (currentQuestionIndex.value < questionLength.value - 1) {
        currentQuestionIndex.value++;
    } else {
        submitSurvey();
    }
};

// 送出問卷：計算結果並呼叫 Pinia Action 儲存
const submitSurvey = async function () {
    
    if (priceStore.getAllStocks.length === 0) {
        toast.info("正在下載最新市場資料，請稍候...");
        try {
            await priceStore.fetchMarketSnapshot();
        } catch (e) {
            toast.error("無法取得市場資料，無法計算建議結果");
            return; // 中斷執行
        }
    }
    
    // 計算結果 
    const result = surveyStore.calculateScore();
    
    // 儲存到 Pinia 
    surveyStore.setAnalysisResult(result);
    
    // 獲取用戶 ID 
    const userId = navStore.userInfo?.userId || 1; 
    
    // 呼叫 Pinia Action 處理 POST 請求 
    try {
        const response = await surveyStore.submitFullSurveyProcess(result, userId);
        
        // 根據 Pinia 模擬的 response 顯示提示
        if (response.status === 200) {
            toast.success("作答完畢，即將生成您的問卷分析結果");
        }

    } catch (error) {
        // 處理錯誤
        const errorMessage = error.message || '組裝數據錯誤';
        toast.error(`問卷提交失敗：${errorMessage}`);
    }
}

onMounted(async () => {
    
    surveyStore.isLoading = true;

    try {
        await priceStore.fetchMarketSnapshot()

        const existingSurvey = await surveyStore.loadExistingSurvey()
        if (existingSurvey) {
            surveyStore.restoreSurveyFromBackEnd(existingSurvey)
             await surveyStore.loadUserTransactions(); 
        } else {
            surveyStore.showResults = false
        }
        
    } catch (error) {
        console.error('初始化失敗:', error)
        toast.error('載入資料失敗，請重新整理頁面')
    }finally{
        surveyStore.isLoading = false;
    }
})


// --- 虛擬交易/Modal 邏輯 (使用 Pinia State 和 Action) ---

// 開啟投資彈窗：呼叫 Pinia Action
const openInvestmentModal = (type, asset) => {
    // 呼叫 Store 的 Action，將資產資訊和狀態傳入
    surveyStore.openModal(type, asset);
};

// 處理 Modal 確認按鈕 (核心交易驗證與記錄)
const handleModalConfirm = async (payload) => {
    const amountToInvest = payload.amount;  // 使用者輸入的金額
    const assetId = surveyStore.currentModalAsset.assetId;

    if (!assetId) {
        toast.error('assetId找不到,或是傳錯assetId');
        console.error('currentModalAsset:', surveyStore.currentModalAsset);
        return;
    }

    if (surveyStore.isProcessingOrder) return;
    surveyStore.setProcessingOrder(true);

    try {
        const newTransaction = await surveyStore.createTransaction(
            assetId,
            amountToInvest
        );
        
        surveyStore.closeModal();

        //
        toast.success(
            `${newTransaction.name} 下單成功！\n` +
            `成交價格：NT$${newTransaction.price.toLocaleString()}\n` +
            `本次扣款：NT$${newTransaction.amount.toLocaleString()}\n` ,
            {
                autoClose: 800, 
                position: 'top-right'
            }
        );

    } catch (error) {
        console.error("下單處理失敗:", error);
        
        if (error.response) {
            const status = error.response.status;
            const message = error.response.data?.message || "下單失敗";
            
            if (status === 401) {
                toast.error("請先登入");
            } else if (status === 400) {
                toast.error(`輸入錯誤：${message}`);
            } else {
                toast.error(`下單失敗：${message}`);
            }
        } else if (error.request) {
            toast.error("網路錯誤，請檢查連線");
        } else {
            toast.error(`處理失敗：${error.message || "未知錯誤"}`);
        }
    } finally {
        surveyStore.setProcessingOrder(false);
    }
};

// 下單成功跳轉到其他頁面 (當完成所有操作後呼叫)
const confirmTransaction = () => {
    // 使用 Pinia State/Getter 獲取資料
    const invested = surveyStore.investedAmount.toLocaleString();
    toast.success(`恭喜您完成虛擬配置！\n總投入金額：NT$${invested}\n即將前往下一步頁面。`,{  autoClose: 500,  
         position: 'top-right'
      });

    navStore.changeView('personalData');
}

const isStocksCollapsed = ref(false); // 股票/ETF 預設展開
const isForexsCollapsed = ref(false); // 外幣 預設展開


</script>

<template>
    <div class="min-h-screen py-4 md:py-8 px-2 sm:px-4 flex flex-col items-center"
     style="background: linear-gradient(to bottom right, #1A3451, #094776, #072B4B);">

        <div
        class="w-auto mx-auto px-6 sm:px-12 py-3 sm:py-4 flex justify-center items-center rounded-xl shadow-2xl transition-all duration-300 hover:scale-[1.01] hover:-translate-y-0.5 mb-4 md:mb-0"
        style="
        background: rgba(10, 20, 30, 0.65);
        backdrop-filter: blur(8px);
        -webkit-backdrop-filter: blur(8px);">

            <span class="text-base sm:text-xl font-semibold tracking-wider"
                style="
                color: #e0f2fe; 
                text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.7); 
                ">
                請完成問卷開始您的財務分析
            </span>
        </div>

        <!-- 載入中畫面 -->
        <div v-if="surveyStore.isLoading" 
            class="survey-container w-full max-w-5xl flex items-center justify-center bg-white shadow-2xl rounded-2xl lg:rounded-3xl overflow-hidden"
            style="min-height: 500px;">
            <i class="fa-solid fa-spinner fa-spin text-2xl"></i> 載入數據中...
        </div>

        <div class="survey-container w-full max-w-5xl flex flex-col lg:flex-row shadow-2xl rounded-2xl lg:rounded-3xl overflow-hidden"
            style="transform-origin: center;">

            <div v-if="!surveyStore.showResults" class="lg:hidden w-full bg-linear-to-r from-indigo-900 to-blue-900 p-4 sm:p-6">
                <div class="flex justify-between items-center mb-3">
                    <div class="text-white">
                        <span class="text-2xl sm:text-3xl font-bold">{{ currentQuestionIndex + 1 }}</span>
                        <span class="text-lg sm:text-xl opacity-90">/{{ questionLength }}</span>
                    </div>
                    <div class="text-white text-sm sm:text-base font-semibold">
                        {{ Math.round(progress) }}% 完成
                    </div>
                </div>
                <div class="w-full bg-white/30 h-2 sm:h-3 rounded-full overflow-hidden backdrop-blur-sm">
                    <div class="h-full bg-linear-to-r from-blue-400 to-blue-200 rounded-full transition-all duration-500"
                        :style="{ width: progress + '%' }"></div>
                </div>
            </div>

            <template v-if="!surveyStore.showResults">
                <div class="hidden lg:block lg:w-2/5 relative">
                    <div class="absolute inset-0 bg-cover bg-center"
                        style="background-image: url('../../../Img/問卷背景圖.webp');"></div>
                    <div class="absolute inset-0 backdrop-blur-md bg-linear-to-br from-indigo-900/70 to-blue-900/70"></div>

                    <div class="relative z-10 p-10 h-full flex flex-col justify-between text-white">
                        <div>
                            <div class="text-sm font-semibold mb-3 text-blue-200 tracking-wider uppercase">
                                Financial Analyze
                            </div>
                            <h2 class="text-4xl font-bold mb-4 leading-tight"
                                style="text-shadow: 2px 2px 8px rgba(0,0,0,0.3);">
                                風險評估問卷
                            </h2>
                            <p class="text-lg font-medium" style="text-shadow: 1px 1px 4px rgba(0,0,0,0.3);">
                                開始您的個人化財務分析
                            </p>
                        </div>

                        <div class="space-y-6">
                            <div>
                                <div class="text-7xl font-bold mb-2" style="text-shadow: 3px 3px 10px rgba(0,0,0,0.4);">
                                    {{ currentQuestionIndex + 1 }}<span class="text-3xl opacity-90">/{{ questionLength }}</span>
                                </div>
                                <div class="text-base font-semibold opacity-95"
                                    style="text-shadow: 1px 1px 4px rgba(0,0,0,0.3);">
                                    當前進度
                                </div>
                            </div>

                            <div>
                                <div class="w-full bg-white/30 h-3 rounded-full overflow-hidden backdrop-blur-sm">
                                    <div
                                        class="h-full bg-linear-to-r from-blue-400 to-blue-200 rounded-full transition-all duration-500 shadow-lg"
                                        :style="{ width: progress + '%' }"></div>
                                </div>
                                <div class="text-base font-semibold mt-3 opacity-95"
                                    style="text-shadow: 1px 1px 4px rgba(0,0,0,0.3);">
                                    {{ Math.round(progress) }}% 完成
                                </div>
                            </div>

                            <div class="mt-8 p-4 bg-white/20 backdrop-blur-sm rounded-xl border border-white/30">
                                <p class="text-md font-medium leading-relaxed" style="text-shadow: 1px 1px 3px rgba(0,0,0,0.2);">
                                    請根據您的財務狀況，回答每一個問題，以便我們為您提供最適合的財務建議。
                                </p>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="w-full lg:w-3/5 bg-white p-4 sm:p-6 lg:p-10 flex flex-col justify-between min-h-[500px]">

                    <div class="flex-1">
                        <div class="mb-4 sm:mb-6 lg:mb-8">
                            <div class="text-xs sm:text-sm lg:text-base text-indigo-600 font-bold mb-2 lg:mb-3 tracking-wide">
                                QUESTION {{ currentQuestionIndex + 1 }} OF {{ questionLength }}
                            </div>
                            <h3 class="text-xl sm:text-2xl lg:text-3xl font-bold text-gray-900 leading-tight">
                                {{ currentQuestion.question }}
                            </h3>
                        </div>

                        <div v-if="currentQuestion.type === 'input_number'" class="space-y-3 sm:space-y-4">
                            <div class="relative">
                                <label class="block text-base sm:text-lg font-semibold text-gray-700 mb-2 sm:mb-3">
                                    {{ currentQuestion.options[0].option }}
                                </label>
                                <div class="relative">
                                    <span
                                        class="absolute left-3 sm:left-4 top-1/2 -translate-y-1/2 text-lg sm:text-xl lg:text-2xl font-bold text-gray-500">NT$</span>
                                    <input type="number" v-model="surveyStore.answers[currentQuestion.id]"
                                        placeholder="請輸入金額" min="0" step="1000"
                                        class="w-full pl-14 sm:pl-16 lg:pl-20 pr-4 sm:pr-6 py-3 sm:py-4 lg:py-5 text-lg sm:text-xl lg:text-2xl font-semibold border-2 border-gray-300 rounded-xl focus:border-black focus:ring-1 outline-none transition-all" />
                                </div>
                                <p class="mt-2 sm:mt-3 text-xs sm:text-sm text-gray-500 flex items-center gap-2">
                                    此金額將作為您的虛擬投資起始資金
                                </p>
                            </div>
                        </div>

                        <div v-else class="space-y-2 sm:space-y-3">
                            <button v-for="vfor_option in currentQuestion.options" v-bind:key="vfor_option.survey_value"
                                class="w-full px-3 sm:px-4 lg:px-6 py-3 sm:py-3.5 lg:py-4 text-left border-2 rounded-lg lg:rounded-xl transition-all duration-200 hover:shadow-lg group"
                                v-bind:class="{
                                    'border-gray-300 hover:border-indigo-500 hover:bg-indigo-50':
                                        !isOptionSelected(currentQuestion.id, vfor_option.survey_value),
                                    'border-indigo-500 bg-indigo-100 shadow-md ring-2 ring-indigo-200': isOptionSelected(currentQuestion.id, vfor_option.survey_value)
                                }" type="button" @click="handleOptionClick(vfor_option.survey_value)">
                                <div class="flex items-center gap-2 sm:gap-3 lg:gap-4">
                                    <div
                                        class="w-5 h-5 sm:w-5.5 sm:h-5.5 lg:w-6 lg:h-6 rounded-full border-2 flex items-center justify-center shrink-0 transition-all border-gray-400"
                                        v-bind:class="{
                                            'border-indigo-600 bg-indigo-600': isOptionSelected(currentQuestion.id, vfor_option.survey_value)
                                        }">
                                        <div v-if="isOptionSelected(currentQuestion.id, vfor_option.survey_value)"
                                            class="w-2 h-2 sm:w-2.5 sm:h-2.5 bg-white rounded-full">
                                        </div>
                                    </div>

                                    <span class="text-base sm:text-lg lg:text-xl font-semibold transition-colors text-gray-800">
                                        {{ vfor_option.option }}
                                    </span>
                                </div>

                            </button>

                        </div>

                    </div>

                    <div class="flex justify-between items-center pt-4 sm:pt-6 border-t-2 border-gray-200 mt-4 sm:mt-6 lg:mt-8">
                        <div class="invisible" :class="{ 'visible': currentQuestionIndex > 0 }">
                            <button
                                class="px-3 sm:px-4 lg:px-6 py-2 sm:py-2.5 lg:py-3 text-gray-700 text-sm sm:text-base lg:text-lg font-semibold rounded-lg lg:rounded-xl transition-all duration-200 hover:bg-gray-100 flex items-center gap-1 sm:gap-2"
                                type="button" @click="gotoBefore">
                                <svg class="w-4 h-4 sm:w-5 sm:h-5 lg:w-6 lg:h-6" fill="none" stroke="currentColor"
                                    viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M15 19l-7-7 7-7" />
                                </svg>
                                上一題
                            </button>
                        </div>

                        <button
                            class="px-4 sm:px-6 lg:px-10 py-2.5 sm:py-3 lg:py-4 text-white text-sm sm:text-base lg:text-xl font-bold rounded-lg lg:rounded-xl transition-all duration-200 shadow-lg hover:shadow-xl flex items-center gap-2 sm:gap-3 disabled:opacity-50 disabled:cursor-not-allowed"
                            style="background: linear-gradient(to right, #4f46e5, #3b82f6);" type="button"
                            v-bind:disabled="!isCurrentQuestionAnswered" @click="gotoNextorSubmit">
                            {{ currentQuestionIndex < questionLength - 1 ? '下一題' : '完成問卷' }}
                                <svg class="w-4 h-4 sm:w-5 sm:h-5 lg:w-6 lg:h-6" fill="none" stroke="currentColor"
                                    viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M9 5l7 7-7 7" />
                                </svg>
                        </button>
                    </div>

                </div>
            </template>

            <div v-else class="w-full h-full bg-gray-200 flex flex-col relative"> 
                <div v-if="surveyStore.analysisResult" class="flex-1 overflow-y-auto p-4 sm:p-6 lg:p-8">

                        <h1 class="text-2xl sm:text-3xl font-extrabold text-gray-800 mb-6 lg:mb-8 pb-1">
                            您的個人投資風險評估與建議
                        </h1>

                        <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 lg:gap-8">

                            <div class="lg:col-span-1 space-y-6">

                                <div class="bg-white rounded-3xl shadow-lg p-6 sm:p-8 text-center">
                                    <p class="text-md font-semibold text-gray-700 mb-3">您的風險屬性</p>
                                    <div class="relative inline-block mb-4">
                                        <div class="w-24 h-24 sm:w-32 sm:h-32 rounded-full flex items-center justify-center bg-indigo-50 border-4 border-indigo-300">
                                            <span class="text-4xl sm:text-5xl font-extrabold text-indigo-700">{{ surveyStore.analysisResult.riskScore }}</span>
                                        </div>
                                    </div>
                                    <p class="text-4xl sm:text-5xl font-extrabold text-indigo-600 mb-1">
                                        {{ surveyStore.analysisResult.profileType }}
                                    </p>
                                    <p class="text-gray-500 text-sm">風險評分: {{ surveyStore.analysisResult.riskScore }} / 20</p>
                                </div>
                                
                                <div class="bg-white rounded-3xl shadow-lg p-6 sm:p-8 space-y-3">
                                    <h3 class="text-lg font-bold text-gray-800 border-b pb-2 mb-3">資金狀況</h3>
                                    
                                    <div class="flex justify-between items-center py-1 border-b">
                                        <span class="text-md font-medium text-gray-800">起始金額</span>
                                        <span class="text-lg font-bold text-[#5C7C9E]">NT$ {{ surveyStore.totalInvestmentLimit.toLocaleString() }}</span>
                                    </div>

                                    <div class="flex justify-between items-center py-1 border-b">
                                        <span class="text-md font-medium text-gray-800">投入總額</span>
                                        <span class="text-lg font-bold text-[#5C7C9E]">NT$ {{ surveyStore.investedAmount.toLocaleString() }}</span>
                                    </div>

                                    <div class="flex justify-between items-center bg-green-100 rounded-lg p-3 mt-6">
                                        <span class="text-base font-bold text-gray-700">剩餘</span>
                                        <span class="text-xl font-extrabold text-green-700">NT$ {{ surveyStore.remainingAmount.toLocaleString() }}</span>
                                    </div>
                                </div>

                                <div class="bg-white rounded-3xl shadow-lg p-6 sm:p-8">
                                    <div class="flex justify-between items-center mb-4">
                                        <span class="text-base font-bold text-gray-700">建議留存現金比例</span>
                                        <span class="text-4xl font-extrabold text-amber-700">{{ surveyStore.analysisResult.allocation.cash }}%</span>
                                    </div>
                                    
                                    <div class="w-full bg-amber-200 h-3 rounded-full overflow-hidden">
                                        <div 
                                            class="h-full bg-linear-to-r from-yellow-500 to-amber-600 rounded-full transition-all duration-700"
                                            :style="{ width: surveyStore.analysisResult.allocation.cash + '%' }"
                                        ></div>
                                    </div>
                                </div>

                            </div>

                            <div class="lg:col-span-2 space-y-8">

                                <div class="bg-white p-5 sm:p-6 rounded-3xl shadow-lg">
                                    <div class="flex justify-between items-center cursor-pointer mb-4 pb-2 border-b-2 border-indigo-100" 
                                        @click="isStocksCollapsed = !isStocksCollapsed"> 
                                        <h2 class="text-xl sm:text-2xl font-bold flex items-center text-indigo-600">
                                            股票/ETF 建議配置
                                        </h2>
                                        <svg :class="{'rotate-180': !isStocksCollapsed}" class="w-6 h-6 text-indigo-500 transform transition-transform duration-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M19 9l-7 7-7-7" />
                                        </svg>
                                    </div>

                                    <div v-show="!isStocksCollapsed" class="space-y-4 transition-all duration-300 ease-in-out">
                                        <div v-for="item in surveyStore.analysisResult.stocks" :key="item.code"
                                            class="p-4 sm:p-5 border-2 border-indigo-100 rounded-xl flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3 sm:gap-4 bg-indigo-50 hover:bg-indigo-100 transition-all duration-200">
                                            
                                            <div class="flex flex-col">
                                                <span class="text-base sm:text-lg font-extrabold text-gray-800">{{ item.name }}</span>
                                                <span class="text-xs sm:text-sm text-gray-500">代碼: {{ item.code }}</span>
                                            </div>
                                            
                                            <div class="flex items-center gap-4 sm:gap-6 w-full sm:w-auto justify-between sm:justify-end">
                                                <span class="text-xl sm:text-2xl font-extrabold text-indigo-700">{{ item.percentage }}%</span>
                                                <button type="button"
                                                    class="px-5 py-2 text-sm font-bold text-white rounded-xl shadow-lg hover:shadow-xl hover:-translate-y-0.5 transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-1"
                                                    style="background: linear-gradient(to right, #10b981, #059669);"
                                                    :disabled="surveyStore.remainingAmount <= 0" @click="openInvestmentModal('Stock/ETF', item)">
                                                    下單
                                                </button>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                <div class="bg-white p-5 sm:p-6 rounded-3xl shadow-lg">
                                    <div class="flex justify-between items-center cursor-pointer mb-4 pb-2 border-b-2 border-blue-100" 
                                        @click="isForexsCollapsed = !isForexsCollapsed">
                                        <h2 class="text-xl sm:text-2xl font-bold flex items-center text-blue-600">
                                            外幣 (Forexs) 建議配置
                                        </h2>
                                        <svg :class="{'rotate-180': !isForexsCollapsed}" class="w-6 h-6 text-blue-500 transform transition-transform duration-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M19 9l-7 7-7-7" />
                                        </svg>
                                    </div>

                                    <div v-show="!isForexsCollapsed" class="space-y-4 transition-all duration-300 ease-in-out">
                                        <div v-for="item in surveyStore.analysisResult.forexs" :key="item.code"
                                            class="p-4 sm:p-5 border-2 border-blue-100 rounded-xl flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3 sm:gap-4 bg-blue-50 hover:bg-blue-100 transition-all duration-200">
                                            
                                            <div class="flex flex-col">
                                                <span class="text-base sm:text-lg font-extrabold text-gray-800">{{ item.name }}</span>
                                                <span class="text-xs sm:text-sm text-gray-500">{{ item.code }}</span>
                                            </div>
                                            
                                            <div class="flex items-center gap-4 sm:gap-6 w-full sm:w-auto justify-between sm:justify-end">
                                                <span class="text-xl sm:text-2xl font-extrabold text-blue-700">{{ item.percentage }}%</span>
                                                <button type="button"
                                                    class="px-5 py-2 text-sm font-bold text-white rounded-xl shadow-lg hover:shadow-xl hover:-translate-y-0.5 transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-1"
                                                    style="background: linear-gradient(to right, #10b981, #059669);"
                                                    :disabled="surveyStore.remainingAmount <= 0" @click="openInvestmentModal('ForeignCurrency', item)">
                                                    下單
                                                </button>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div v-else class="flex-1 flex items-center justify-center text-gray-500 text-base sm:text-lg">
                        正在計算您的投資結果...
                    </div>

                    <div v-if="surveyStore.analysisResult" class="p-4 sm:p-6 border-t-2 border-indigo-200 bg-white sticky bottom-0 z-10 shadow-2xl">
                        <div class="flex justify-end items-center">
                            <button @click="confirmTransaction"
                                class="w-full sm:w-auto px-6 lg:px-10 py-3 sm:py-4 text-white text-base lg:text-xl font-extrabold rounded-xl transition-all duration-300 shadow-xl hover:shadow-2xl hover:scale-[1.02] flex items-center justify-center gap-3 disabled:opacity-50 disabled:cursor-not-allowed"
                                style="background: linear-gradient(to right, #4f46e5, #3b82f6);" type="button"
                                :disabled="surveyStore.investedAmount <= 0">
                                確認交易並前往下一步
                                <svg class="w-5 h-5 sm:w-6 sm:h-6" fill="none" stroke="currentColor"
                                    viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M9 5l7 7-7 7" />
                                </svg>
                            </button>
                        </div>
                    </div>

                </div>

            </div>

        <InvestmentModal 
            v-if="surveyStore.isModalVisible" 
            v-bind:key="surveyStore.modalKey"
            v-bind:selected="true"
            v-bind:asset-data="surveyStore.currentModalAsset"
            v-bind:remaining-amount="surveyStore.remainingAmount"
            @close="surveyStore.closeModal"
            @confirm="handleModalConfirm"
        />

    </div>
</template>

<style scoped>
@media (min-width: 1024px) {
    .survey-container {
        transform: scale(0.80);
        height: 600px;
    }
}

@media (max-width: 1023px) {
    .survey-container {
        min-height: 500px;
        height: auto;
    }
}
</style>