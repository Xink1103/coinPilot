<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue';
import { useNavStore } from '@/stores/navStore'; 
import { toast } from 'vue3-toastify';
import 'vue3-toastify/dist/index.css';
import axios from 'axios'; 

const navStore = useNavStore();

// 介面通用邏輯 (登出、時間、下拉選單) 
const handleLogout = () => {
    navStore.handleLogoutAndRedirect();
    toast.success("您已登出");
};

const dropdownRef = ref(null); 
const accountButtonRef = ref(null); 

const handleClickOutside = (event) => {
    if (navStore.isAccountDropdownOpen &&
        dropdownRef.value &&
        !dropdownRef.value.contains(event.target) &&
        !accountButtonRef.value.contains(event.target)) {
        navStore.closeAccountDropdown();
    }
};

const username = ref('管理者');
const currentTime = ref('');
let timer = null;

const updateTime = () => {
    const now = new Date();
    currentTime.value = now.toLocaleDateString('zh-TW', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit'
    }).replace(/\//g, '.') + ' ' + now.toLocaleTimeString('zh-TW');
};

const loadUserName = () => {
    const userJson = localStorage.getItem('currentUser');
    if (userJson) {
        try {
            const user = JSON.parse(userJson);
            if (user && user.userName) {
                username.value = user.userName;
            }
        } catch (e) {
            console.error('解析 currentUser JSON 失敗:', e);
            username.value = '讀取失敗 (Admin)';
        }
    }
};

const adminData = ref(null); // 儲存從後端取得的 Admin 完整 DTO
const isLoading = ref(true); // 載入狀態
const errorMessage = ref(''); // 錯誤訊息

// 搜尋關鍵字
const searchKeyword = ref('');

// 儲存當前展開的用戶 ID (控制用戶列表摺疊)
const expandedUserId = ref(null); 
const toggleExpansion = (userId) => {
    expandedUserId.value = expandedUserId.value === userId ? null : userId;
}

// 資產類型與顏色映射 (用於前端進度條)
const ASSET_COLORS = [
    'bg-blue-500', 
    'bg-emerald-500', 
    'bg-purple-500', 
    'bg-yellow-500', 
    'bg-red-500',
    'bg-cyan-500',
    'bg-pink-500'
];

// 根據索引取得 Tailwind CSS 顏色類別
const getAssetColor = (index) => {
    return ASSET_COLORS[index % ASSET_COLORS.length];
};

// 格式化金額：加上 NT$ 並用逗號分隔
const formatCurrency = (amount) => {
    if (amount === undefined || amount === null) return '未填寫';
    const num = parseFloat(amount); 
    if (isNaN(num)) return '未填寫';
    return `NT$ ${new Intl.NumberFormat('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 0 }).format(num)}`;
};

// 投資目標文字轉換
const getSavingGoalText = (goal) => {
    const goalMap = {
        'CapitalPreservation': '資本保存',
        'StableIncome': '穩定收入',
        'HighReturn': '高報酬'
    };
    return goalMap[goal] || '未填寫';
};

// 投資經驗文字轉換
const getInvestExpText = (exp) => {
    const expMap = {
        1: '新手',
        2: '中等經驗',
        3: '專業投資者'
    };
    return expMap[exp] || '未填寫';
};

// 偏好資產類型轉換
const getPreferredAssetText = (asset) => {
    const assetMap = {
        'Stock': '股票',
        'ETF': 'ETF',
        'ForeignCurrency': '外幣'
    };
    return assetMap[asset] || '未填寫';
};

// 搜尋過濾後的用戶列表 (使用 computed) - 僅搜尋 userId
const filteredUserList = computed(() => {
    if (!adminData.value || !adminData.value.userList) {
        return [];
    }
    
    // 如果搜尋框是空的,返回所有用戶
    if (!searchKeyword.value.trim()) {
        return adminData.value.userList;
    }
    
    // 過濾用戶 (僅根據 userId)
    const keyword = searchKeyword.value.toLowerCase().trim();
    return adminData.value.userList.filter(user => {
        const userId = String(user.userId || '').toLowerCase();
        return userId.includes(keyword);
    });
});

// 呼叫後端 API 取得儀表板數據
const fetchDashboardData = async () => {
    isLoading.value = true;
    errorMessage.value = '';
    
    const token = localStorage.getItem('authToken'); 
    
    try {
        const response = await axios.get('/api/admin/dashboard', {
             headers: {
                'Authorization': token 
             }
        });
        
        adminData.value = response.data;
        
    } catch (error) {
        console.error('使用者資料抓取失敗', error);
        
        // 處理錯誤訊息
        let msg = '資料讀取失敗,請檢查網路或伺服器。';
        if (error.response) {
            msg = error.response.data.message || msg;
        }
        
        errorMessage.value = msg;
        toast.error(msg);
        
    } finally {
        isLoading.value = false;
    }
};

onMounted(() => {
    fetchDashboardData();
    
    updateTime();
    timer = setInterval(updateTime, 1000);
    document.addEventListener('click', handleClickOutside);
    loadUserName();
});

onUnmounted(() => {
    if (timer) {
        clearInterval(timer);
    }
    document.removeEventListener('click', handleClickOutside);
});
</script>

<template>
    <nav class="flex justify-between sticky top-0 z-20 items-center bg-[#0E1E2B] text-[#F1FAEE] shadow-md px-8 h-16 shrink-0"
        style="
            box-shadow: 0 4px 15px rgba(0, 0, 0, 0.5), 0 0 5px rgba(255, 255, 255, 0.08);
            border-bottom: 1px solid rgba(255, 255, 255, 0.15);
        ">
        
        <div class="flex items-center"> 
            <i class="fa-solid fa-hand-holding-dollar text-xl mr-3 text-slate-300"></i> 
            <h2 class="text-xl font-semibold text-slate-300">CoinPilot</h2>
        </div>
        
        <div class="text-[#F1FAEE] font-semibold text-lg mr-auto ml-10 whitespace-nowrap">
            {{ username }} 管理者您好 
        </div>

        <div class="text-[#F1FAEE] text-base font-medium mr-4 whitespace-nowrap">
            現在時間:{{ currentTime }}
        </div>
        
        <div class="flex items-center space-x-4"> 
            <div class="relative">
                <button ref="accountButtonRef" @click.stop="navStore.toggleAccountDropdown" 
                    class="text-[#F1FAEE] hover:text-[#457B9D] transition duration-150 focus:outline-none">
                    <i class="fa-solid fa-circle-user text-2xl cursor-pointer"></i>
                </button>

                <div ref="dropdownRef" v-show="navStore.isAccountDropdownOpen" 
                    @click.stop
                    class="absolute -right-8 top-11 w-32 bg-[#2d3748] rounded-bl-xl rounded-b-xl shadow-2xl border border-gray-600 z-50 overflow-hidden">
                    
                    <div class="py-2 px-4 bg-[#1a202c] border-b border-gray-600 flex items-center">
                        <i class="fa-solid fa-user-gear text-gray-400 text-base"></i>
                        <span class="ml-2 text-sm text-gray-300">{{ username }}</span>
                    </div>
                    
                    <a @click.prevent="handleLogout"
                        class="block py-3 px-4 text-sm font-medium text-red-400 hover:bg-[#3d4a5c] hover:text-red-300 hover:pl-6 cursor-pointer transition-all duration-200">
                        <i class="fa-solid fa-right-from-bracket mr-2"></i> 登出
                    </a>
                </div>
            </div>
        </div>
    </nav>

    <div class="min-h-screen bg-linear-to-br from-slate-900 via-slate-800 to-slate-900 p-8">
        <div class="max-w-7xl mx-auto">
            
            <!-- 載入中 -->
            <div v-if="isLoading" class="text-slate-400 text-center py-12">
                <i class="fa-solid fa-spinner fa-spin text-2xl"></i> 載入儀表板數據中...
            </div>
            
            <!-- 錯誤訊息 -->
            <div v-else-if="errorMessage" class="bg-red-900/50 text-red-300 p-4 rounded-lg text-center border border-red-700/50">
                <i class="fa-solid fa-triangle-exclamation mr-2"></i> {{ errorMessage }}
            </div>

            <!-- 概覽統計卡片 -->
            <div v-else-if="adminData" class="bg-slate-800/50 backdrop-blur-sm border border-slate-700/50 rounded-xl p-8 mb-8 shadow-xl hover:scale-[1.02] hover:shadow-cyan-400/30 hover:border-cyan-400/50 transition-all duration-300">
                <h1 class="text-2xl font-semibold text-slate-200 mb-6">用戶管理概覽</h1>
                <div class="grid grid-cols-3 gap-8">
                    <div>
                        <p class="text-slate-400 text-sm mb-2">總用戶數</p>
                        <p class="text-5xl font-semibold text-blue-400">{{ adminData.totalUsers || 0 }}</p>
                    </div>
                    <div>
                        <p class="text-slate-400 text-sm mb-2">建議資產採納率</p>
                        <p class="text-5xl font-semibold text-blue-400">
                            {{ adminData.assetAdoptionRate.toFixed(2) }}%
                        </p>
                    </div>
                    <div>
                        <p class="text-slate-400 text-sm mb-2">整體風險傾向</p>
                        <p class="text-5xl font-semibold text-blue-400">{{ adminData.dominantRiskLevel || '未填寫' }}</p>
                    </div>
                </div>
            </div>

            <!-- 用戶列表 -->
            <div v-if="adminData && adminData.userList && adminData.userList.length > 0">
                <div class="flex items-center justify-between mb-6">
                    <h2 class="text-xl font-semibold text-slate-200">用戶列表</h2>
                    <div class="relative">
                        <input 
                            v-model="searchKeyword"
                            type="text" 
                            placeholder="搜尋用戶 ID..." 
                            class="w-80 px-4 py-2.5 pl-10 rounded-lg bg-slate-800/50 backdrop-blur-sm text-slate-200 border shadow-lg border-slate-700/50 hover:scale-[1.02] hover:shadow-cyan-400/30 hover:border-cyan-400/50 focus:outline-none transition-colors"
                        >
                        <svg class="w-5 h-5 text-slate-400 absolute left-3 top-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                        </svg>
                    </div>
                </div>

                <div class="bg-slate-800/50 backdrop-blur-sm border border-slate-700/50 rounded-xl overflow-hidden shadow-xl">
                    
                    <!-- 表頭 -->
                    <div class="bg-slate-800/80 px-6 py-4 border-b border-slate-700/50">
                        <div class="flex items-center text-slate-300 text-sm font-medium">
                            <div class="w-1/3">用戶 ID</div>
                            <div class="w-2/9 text-center">投資金額</div>
                            <div class="w-2/9 text-center">實際投入比例</div>
                            <div class="w-2/9 text-center">風險等級</div>
                            <div class="w-12"></div>
                        </div>
                    </div>

                    <!-- 用戶列表項目 (使用 filteredUserList) -->
                    <div v-if="filteredUserList.length > 0">
                        <div v-for="user in filteredUserList" :key="user.userId" class="border-b border-slate-700/30 last:border-b-0">
                            <div class="flex items-center px-6 py-4 hover:bg-slate-700/30 transition-colors cursor-pointer" 
                                 @click="toggleExpansion(user.userId)">
                                
                                <div class="w-1/3 flex items-center gap-4">
                                    <div>
                                        <p class="text-slate-200 font-medium">用戶 #{{ user.userId }}</p>
                                    </div>
                                </div>
                                
                                <div class="w-2/9 text-center">
                                    <p class="text-emerald-400 font-medium">
                                        {{ formatCurrency(user.totalInvestment) }}
                                    </p>
                                </div>
                                
                                <div class="w-2/9 text-center">
                                    <p :class="{
                                        'text-blue-400': user.investmentRatio > 0, 
                                        'text-red-400': user.investmentRatio === 0, 
                                        'text-gray-400': user.investmentRatio === null
                                    }" class="font-medium">
                                        {{ user.investmentRatio !== null ? user.investmentRatio.toFixed(2) + '%' : '未填寫' }}
                                    </p>
                                </div>
                                
                                <div class="w-2/9 text-center">
                                    <span class="inline-block px-3 py-1 rounded-md text-md text-blue-400 font-medium">
                                        {{ user.surveyLevel || '未填寫' }}
                                    </span>
                                </div>
                                
                                <div class="w-12 flex justify-center transform transition-transform duration-300" 
                                     :class="{'rotate-180': expandedUserId === user.userId}">
                                    <svg class="w-5 h-5 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"></path>
                                    </svg>
                                </div>
                            </div>

                            <!-- 展開的詳細資訊 -->
                            <div class="bg-slate-900/50 px-6 py-6 border-t border-slate-700/30" 
                                 v-if="expandedUserId === user.userId">
                                <div class="grid grid-cols-2 gap-6">
                                    
                                    <!-- 問卷分析結果  -->
                                    <div class="bg-slate-800/50 rounded-lg p-5 border border-slate-700/30">
                                        <h4 class="text-slate-300 font-medium mb-4 flex items-center gap-2">
                                            問卷分析結果
                                        </h4>
                                        <div class="space-y-3">
                                            <!-- 投資目標 -->
                                            <div class="flex justify-between items-center">
                                                <span class="text-slate-400 text-sm">投資目標</span>
                                                <span class="text-slate-200 font-medium">{{ getSavingGoalText(user.surveySavingGoal) }}</span>
                                            </div>
                                            <!-- 投資經驗 -->
                                            <div class="flex justify-between items-center">
                                                <span class="text-slate-400 text-sm">投資經驗</span>
                                                <span class="text-slate-200 font-medium">{{ getInvestExpText(user.surveyInvestExp) }}</span>
                                            </div>
                                            <!-- 年齡區間 -->
                                            <div class="flex justify-between items-center">
                                                <span class="text-slate-400 text-sm">年齡區間</span>
                                                <span class="text-slate-200 font-medium">{{ user.surveyAgeGroup || '未填寫' }}</span>
                                            </div>
                                        </div>
                                    </div>

                                    <!-- 資產配置 -->
                                    <div class="bg-slate-800/50 rounded-lg p-5 border border-slate-700/30">
                                        <h4 class="text-slate-300 font-medium mb-4 flex items-center gap-2">
                                            實際資產配置
                                        </h4>
                                        <div class="space-y-4">
                                            <div v-for="(asset, index) in user.assetAllocations" :key="asset.assetId" class="space-y-2">
                                                <div class="flex justify-between items-center">
                                                    <span class="text-slate-300 font-medium text-sm">
                                                        {{ asset.assetName }} 
                                                        <span v-if="asset.assetCode" class="text-slate-500">({{ asset.assetCode }})</span>
                                                    </span>
                                                    <span class="text-slate-400 text-sm">
                                                        {{ asset.actualPercentage?.toFixed(1) || 0 }}% · {{ formatCurrency(asset.actualAmount) }}
                                                    </span>
                                                </div>
                                                <div class="w-full bg-slate-700/50 rounded-full h-1.5">
                                                    <div 
                                                        :class="getAssetColor(index)" 
                                                        class="h-1.5 rounded-full transition-all duration-500" 
                                                        :style="{ width: (asset.actualPercentage || 0) + '%' }"
                                                    ></div>
                                                </div>
                                            </div>
                                            
                                            <div v-if="!user.assetAllocations || user.assetAllocations.length === 0" 
                                                 class="text-slate-500 text-sm text-center py-4">
                                                <i class="fa-solid fa-box-open mr-1"></i> 用戶尚未有實際資產投入記錄。
                                            </div>
                                        </div>
                                    </div>

                                </div>
                            </div>
                        </div>
                    </div>
                    
                    <!-- 搜尋無結果 -->
                    <div v-else class="text-slate-500 text-center py-12">
                        <i class="fa-solid fa-magnifying-glass text-2xl mb-2"></i><br>
                        找不到用戶 ID「{{ searchKeyword }}」
                    </div>

                </div>
            </div>
            
            <!-- 無資料提示 -->
            <div v-else-if="!isLoading && !errorMessage" class="text-slate-500 text-center py-12">
                <i class="fa-solid fa-box-open text-3xl mb-3"></i><br>目前沒有用戶數據可供顯示。
            </div>

        </div>
    </div>
</template>