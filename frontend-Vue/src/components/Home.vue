<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAssetStore } from '@/stores/assetStore'
import { usePriceStore } from '@/stores/usePriceStore' // 假設路徑正確

// 定義組件可以發出的事件
const emit = defineEmits(['gotoLogin', 'gotoSignup']);

// 使用 stores
const assetStore = useAssetStore()
// 使用 Pinia Store，現在它包含所有價格和資產資訊
const priceStore = usePriceStore() 




// 格式化價格顯示 (保留，用於 View 層呈現)
function formatPrice(price, decimals) {
    if (price === null || price === undefined) return '---'
    
    // 確保 price 是數字類型
    const numPrice = typeof price === 'string' ? parseFloat(price) : price
    if (isNaN(numPrice)) return '---'
    
    const fixed = numPrice.toFixed(decimals)
    
    // 為股票價格添加千分位逗號
    if (decimals === 2) {
        const parts = fixed.split('.')
        parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',')
        return parts.join('.')
    }
    
    return fixed
}


// 整合的市場數據 - 直接從 priceStore 的 getAllAssets Getter 獲取
const marketItems = computed(() => {
    // priceStore.getAllAssets 已經是所有資產的陣列，且每個物件都包含 transCurrentPrice
    const allAssets = priceStore.getAllAssets 
    
    return allAssets.map(asset => {
        // 從 asset 物件中直接提取價格和資訊
        const rawPrice = asset.transCurrentPrice
        const isForex = asset.assetType === 'FOREX'
        
        // 根據類型決定價格顯示的小數位數和代碼格式
        const decimals = isForex ? 4 : 2
        const code = isForex ? `${asset.assetCode}/TWD` : asset.assetCode
        
        return {
            type: asset.assetType.toLowerCase(), // stock 或 forex
            code: code,
            name: asset.assetName,
            // 價格處理：如果 rawPrice 有值，則格式化；否則顯示 '載入中...' 或 '---'
            price: rawPrice !== 0 && rawPrice !== null ? formatPrice(rawPrice, decimals) : '---',
            rawPrice: rawPrice,
            tag: asset.assetType
        }
    })
})


// 載入所有資產的市場價格
// 【大幅簡化】現在只需要呼叫一次 Action 即可，因為後端 /snapshot 已經返回所有資產
async function loadAllPrices() {
    try {
        // 只需要呼叫一次 fetchMarketSnapshot 即可載入所有數據到 store
        await priceStore.fetchMarketSnapshot() 
        
        // 註：不需要手動更新 pricesMap.value，因為 marketItems 已經響應式地依賴 priceStore.getAllAssets
        
    } catch (error) {
        console.error('載入價格時發生錯誤:', error)
        // 錯誤處理會在 priceStore.fetchMarketSnapshot 內部處理並拋出
    } 
    // 【刪除】finally 區塊也不需要了，因為 isLoading 的狀態由 priceStore 內部控制
}

// 組件掛載時載入價格
onMounted(() => {
    // 判斷是否需要載入：如果 priceStore 中已經有數據，則可能不需要重複載入 (依需求而定)
    if (priceStore.getAllAssets.length === 0) {
        loadAllPrices()
    }
})

// 滾動控制 (保留)
const marketContainer = ref(null)
const scrollMarket = (direction) => {
    if (marketContainer.value) {
        const scrollAmount = 400
        marketContainer.value.scrollBy({
            left: direction === 'left' ? -scrollAmount : scrollAmount,
            behavior: 'smooth'
        })
    }
}
</script>

<template>
  <div class="min-h-screen w-full overflow-x-hidden flex flex-col"
       style="background: linear-gradient(to bottom right, #1A3451, #094776, #072B4B);">

    <!-- 1. 核心 Hero Section (品牌 + 登入/註冊) -->
    <div class="relative z-20 pt-20 pb-12 px-4 text-center">
      <div class="max-w-4xl mx-auto animate-fade-in">
        <h1 class="text-5xl md:text-7xl font-extrabold text-[#4FC3F7] mb-6 tracking-wide drop-shadow-lg">
          CoinPilot
        </h1>
        <p class="text-xl md:text-2xl text-gray-200 mb-10 font-light tracking-wider">
          您的個人化財務分析系統
        </p>

        <!-- 登入/註冊按鈕組 -->
        <div class="flex flex-col sm:flex-row justify-center gap-6">
          <button 
            @click="$emit('gotoLogin')"
            class="py-4 px-10 text-xl font-bold rounded-xl transition-all duration-300 transform hover:scale-105 hover:shadow-xl hover:shadow-blue-400/20"
            style="
              background: linear-gradient(to right, #4FC3F7, #03A9F4);
              color: #0E1E2B;
              box-shadow: 0 4px 15px rgba(3, 169, 244, 0.4);
            "
          >
            登入系統
          </button>
          
          <button 
            @click="$emit('gotoSignup')"
            class="py-4 px-10 text-xl font-bold rounded-xl transition-all duration-300 transform hover:scale-105 hover:shadow-xl hover:bg-white/5"
            style="
              background-color: transparent;
              color: #4FC3F7;
              border: 2px solid #4FC3F7;
              box-shadow: 0 0 15px rgba(79, 195, 247, 0.2);
            "
          >
            註冊新帳號
          </button>
        </div>
      </div>
    </div>

    <!-- 2. 功能展示跑馬燈 (Infinite Marquee) -->
    <div class="w-full py-12 overflow-hidden relative opacity-90">
      <div class="absolute left-0 top-0 bottom-0 w-12 md:w-20 z-10 bg-linear-to-r from-[#142940] to-transparent"></div>
      <div class="absolute right-0 top-0 bottom-0 w-12 md:w-20 z-10 bg-linear-to-l from-[#083050] to-transparent"></div>
      
      <div class="flex animate-marquee hover:pause" style="width: max-content;">
        <div class="flex gap-6 md:gap-8 px-3 md:px-4">
          
            <div class="w-72 h-48 bg-[#0F2335]/80 backdrop-blur-md border border-white/10 rounded-2xl p-4 shadow-xl flex flex-col justify-between transform">
                <div class="text-[#4FC3F7] font-bold text-sm">收支平衡指標</div>
                <div class="relative w-full h-24 flex justify-center items-end overflow-hidden">
                <div class="w-32 h-16 bg-linear-to-r from-green-400 via-yellow-400 to-red-500 rounded-t-full opacity-80"></div>
                <div class="absolute bottom-0 w-2 h-16 bg-white origin-bottom rotate-45 shadow-lg"></div>
                </div>
                <div class="text-center text-white font-mono text-lg">區間 : 安全</div>
            </div>

            <div class="w-72 h-48 bg-[#0F2335]/80 backdrop-blur-md border border-white/10 rounded-2xl p-4 shadow-xl flex items-center gap-4 transform">
                <div class="relative w-24 h-24 rounded-full border-4 border-[#1A3451]" 
                    style="background: conic-gradient(#4FC3F7 0% 60%, #F48FB1 60% 85%, #81C784 85% 100%);">
                </div>
                <div class="flex flex-col gap-2 text-xs text-gray-300">
                <div class="flex items-center gap-2"><div class="w-2 h-2 bg-[#4FC3F7] rounded-full"></div> 股票 60%</div>
                <div class="flex items-center gap-2"><div class="w-2 h-2 bg-[#F48FB1] rounded-full"></div> 外幣 25%</div>
                <div class="flex items-center gap-2"><div class="w-2 h-2 bg-[#81C784] rounded-full"></div> 現金 15%</div>
                </div>
            </div>

            <div class="w-72 h-48 bg-[#0F2335]/80 backdrop-blur-md border border-white/10 rounded-2xl p-5 shadow-xl flex flex-col justify-center gap-4 transform">
                <div>
                <div class="text-gray-400 text-xs mb-1">總投資成本</div>
                <div class="text-2xl font-bold text-white tracking-wider">$ 1,000,000</div>
                </div>
                <div class="w-full h-1 bg-white/10 rounded-full overflow-hidden">
                <div class="w-2/3 h-full bg-[#4FC3F7]"></div>
                </div>
                <div class="flex justify-between text-xs text-gray-400">
                <span>已實現損益</span>
                <span class="text-green-400 font-bold">+ $152,400</span>
                </div>
            </div>

            <div class="w-72 h-48 bg-[#0F2335]/80 backdrop-blur-md border border-white/10 rounded-2xl p-4 shadow-xl flex flex-col gap-3 transform">
                <div class="text-[#4FC3F7] font-bold text-sm border-b border-white/10 pb-2">近期交易紀錄</div>
                <div class="flex justify-between items-center text-sm">
                <span class="text-white">台積電</span>
                <span class="text-green-400">+308.4 NT</span>
                </div>
                <div class="flex justify-between items-center text-sm">
                <span class="text-white">富邦金</span>
                <span class="text-red-400">-763.3 NT</span>
                </div>
                <div class="flex justify-between items-center text-sm">
                <span class="text-white">美金</span>
                <span class="text-green-400">+102.5 NT</span>
                </div>
            </div>

            <div class="w-72 h-48 bg-[#0F2335]/80 backdrop-blur-md border border-white/10 rounded-2xl p-6 shadow-xl flex flex-col justify-between transform">
                <div>
                    <div class="text-gray-400 text-sm mb-1">風險等級評估</div>
                    <div class="text-4xl font-extrabold text-[#4FC3F7] tracking-wider">積極型</div>
                </div>
                <div class="w-full">
                    <div class="relative h-2 rounded-full bg-white/10 overflow-hidden mb-2">
                    <div class="h-full rounded-full bg-[#4FC3F7]" style="width: 80%;"></div>
                    </div>
                    <div class="text-right text-gray-400 text-xs">18 / 20</div>
                </div>
            </div>

        </div>
        
        <!-- 重複一次內容以實現無縫循環 -->
        <div class="flex gap-6 md:gap-8 px-3 md:px-4" aria-hidden="true">
            <div class="w-72 h-48 md:w-72 md:h-48 bg-[#0F2335]/80 backdrop-blur-md border border-white/10 rounded-xl md:rounded-2xl p-3 md:p-4 shadow-xl flex flex-col justify-between transform shrink-0">
                <div class="text-[#4FC3F7] font-bold text-xs md:text-sm">收支平衡指標</div>
                <div class="relative w-full h-16 md:h-24 flex justify-center items-end overflow-hidden">
                <div class="w-24 h-12 md:w-32 md:h-16 bg-linear-to-r from-green-400 via-yellow-400 to-red-500 rounded-t-full opacity-80"></div>
                <div class="absolute bottom-0 w-1.5 h-12 md:w-2 md:h-16 bg-white origin-bottom rotate-45 shadow-lg"></div>
                </div>
                <div class="text-center text-white font-mono text-base md:text-lg">區間 : 安全</div>
            </div>

            <div class="w-72 h-48 md:w-72 md:h-48 bg-[#0F2335]/80 backdrop-blur-md border border-white/10 rounded-xl md:rounded-2xl p-3 md:p-4 shadow-xl flex items-center gap-3 md:gap-4 transform shrink-0">
                <div class="relative w-20 h-20 md:w-24 md:h-24 rounded-full border-4 border-[#1A3451]" style="background: conic-gradient(#4FC3F7 0% 60%, #F48FB1 60% 85%, #81C784 85% 100%);"></div>
                <div class="flex flex-col gap-1.5 md:gap-2 text-xs text-gray-300">
                <div class="flex items-center gap-2"><div class="w-2 h-2 bg-[#4FC3F7] rounded-full"></div> 股票 60%</div>
                <div class="flex items-center gap-2"><div class="w-2 h-2 bg-[#F48FB1] rounded-full"></div> 外幣 25%</div>
                <div class="flex items-center gap-2"><div class="w-2 h-2 bg-[#81C784] rounded-full"></div> 現金 15%</div>
                </div>
            </div>

            <div class="w-72 h-48 md:w-72 md:h-48 bg-[#0F2335]/80 backdrop-blur-md border border-white/10 rounded-xl md:rounded-2xl p-4 md:p-5 shadow-xl flex flex-col justify-center gap-3 md:gap-4 transform shrink-0">
                <div><div class="text-gray-400 text-xs mb-1">總投資成本</div><div class="text-xl md:text-2xl font-bold text-white tracking-wider">$ 1,000,000</div></div>
                <div class="w-full h-1 bg-white/10 rounded-full overflow-hidden"><div class="w-2/3 h-full bg-[#4FC3F7]"></div></div>
                <div class="flex justify-between text-xs text-gray-400"><span>已實現損益</span><span class="text-green-400 font-bold">+ $152,400</span></div>
            </div>

            <div class="w-72 h-48 md:w-72 md:h-48 bg-[#0F2335]/80 backdrop-blur-md border border-white/10 rounded-xl md:rounded-2xl p-3 md:p-4 shadow-xl flex flex-col gap-2 md:gap-3 transform shrink-0">
                <div class="text-[#4FC3F7] font-bold text-xs md:text-sm border-b border-white/10 pb-2">近期交易紀錄</div>
                <div class="flex justify-between items-center text-xs md:text-sm"><span class="text-white">台積電</span><span class="text-green-400">+308.4 NT</span></div>
                <div class="flex justify-between items-center text-xs md:text-sm"><span class="text-white">富邦金</span><span class="text-red-400">-763.3 NT</span></div>
                <div class="flex justify-between items-center text-xs md:text-sm"><span class="text-white">美金</span><span class="text-green-400">+102.5 NT</span></div>
            </div>

            <div class="w-72 h-48 md:w-72 md:h-48 bg-[#0F2335]/80 backdrop-blur-md border border-white/10 rounded-xl md:rounded-2xl p-4 md:p-6 shadow-xl flex flex-col justify-between transform shrink-0">
                <div>
                    <div class="text-gray-400 text-xs md:text-sm mb-1">風險等級評估</div>
                    <div class="text-3xl md:text-4xl font-extrabold text-[#4FC3F7] tracking-wider">積極型</div>
                </div>
                <div class="w-full">
                    <div class="relative h-2 rounded-full bg-white/10 overflow-hidden mb-2">
                    <div class="h-full rounded-full bg-[#4FC3F7]" style="width: 80%;"></div>
                    </div>
                    <div class="text-right text-gray-400 text-xs">18 / 20</div>
                </div>
            </div>
        </div>
      </div>
    </div>

    <!-- 3. 市場快照 (使用 Pinia 資料 + 實時價格) -->
    <div class="flex-1 w-full max-w-7xl mx-auto py-12 px-4 flex flex-col justify-center">
      <div class="flex items-center justify-between mb-8 px-2">
        <h2 class="text-2xl md:text-3xl font-bold text-white pl-3">
          市場即時快照
        </h2>
        <div class="flex gap-2">
          <button @click="scrollMarket('left')" class="p-2 rounded-full border border-white/20 text-white hover:bg-white/10 transition">
            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/></svg>
          </button>
          <button @click="scrollMarket('right')" class="p-2 rounded-full border border-white/20 text-white hover:bg-white/10 transition">
            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/></svg>
          </button>
        </div>
      </div>

      <!-- 載入中提示 -->
      <div v-if="priceStore.isLoading" class="text-center py-12">
        <div class="inline-block animate-spin rounded-full h-12 w-12 border-4 border-[#4FC3F7] border-t-transparent"></div>
        <p class="text-gray-400 mt-4">正在載入市場價格...</p>
      </div>

      <!-- 市場卡片 -->
      <div v-else class="relative">
        <div ref="marketContainer" class="flex gap-6 overflow-x-auto scroll-smooth pb-8 scrollbar-hide snap-x snap-mandatory px-4">
          <div v-for="item in marketItems" :key="item.code" 
               class="shrink-0 w-80 md:w-96 bg-white/5 backdrop-blur-md border border-white/10 rounded-2xl transform hover:shadow-cyan-400/30 hover:border-cyan-400/50 transition-all duration-300 snap-center group">
            <div class="p-8 flex flex-col justify-between h-full min-h-[180px]">
              
              <div class="flex justify-between items-start">
                <div>
                  <div class="flex items-center gap-2 mb-2">
                    <span class="px-2 py-1 text-xs rounded bg-white/10 text-gray-300 font-mono">{{ item.tag }}</span>
                  </div>
                  <h3 class="text-3xl font-bold text-white mb-1 group-hover:text-[#4FC3F7] transition-colors">{{ item.name }}</h3>
                  <p class="text-gray-400 pl-1 font-mono text-lg tracking-widest">{{ item.code }}</p>
                </div>
              </div>

              <div class="mt-6 flex items-end justify-between border-t border-white/10 pt-4">
                <span class="text-gray-400 text-sm">Current Price</span>
                <span class="text-3xl font-bold text-white tracking-tight">
                  <span class="text-lg text-gray-400 font-normal mr-1">NT$</span>{{ item.price }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 4. 底部 Slogan -->
    <div class="py-12 text-center bg-black/20 backdrop-blur-lg border-t border-white/5">
      <p class="text-[#4FC3F7] text-lg md:text-xl font-medium mb-2 tracking-widest">READY TO START?</p>
      <h3 class="text-2xl md:text-3xl font-bold text-white mb-4">
        準備好開始更深入的投資了嗎?
      </h3>
      <p class="text-gray-400 max-w-2xl mx-auto px-4">
        登入您的帳戶,完成風險評估,立即獲取專屬投資建議與資產配置分析
      </p>
    </div>

  </div>
</template>

<style scoped>
.scrollbar-hide::-webkit-scrollbar {
  display: none;
}
.scrollbar-hide {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

@keyframes marquee {
  0% { transform: translateX(0%); }
  100% { transform: translateX(-50%); }
}

.animate-marquee {
  animation: marquee 30s linear infinite;
}

.hover\:pause:hover {
  animation-play-state: paused;
}

@keyframes fade-in {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
.animate-fade-in {
  animation: fade-in 1s ease-out;
}
</style>