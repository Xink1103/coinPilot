<script setup>
    const emit = defineEmits(['gotologin']); 
    import { ref, computed } from 'vue'; 
    
    const email = ref('');
    const isLoading = ref(false); // 控制按鈕的 loading 狀態
    const message = ref({});     // { text: '提示內容', type: 'success' | 'error' | 'info' }
    const sendResetLink = () => { /* 實作發送邏輯 */ };
    const messageClass = computed(() => { /* 實作樣式邏輯 */ });

</script>

<template>
    <div class="w-full min-h-screen bg-gray-900 flex flex-col lg:flex-row items-center justify-center">
    
        <!-- 左側:營銷/背景資訊區塊 (小螢幕顯示在上方,大螢幕顯示在左側) -->
        <div class="flex w-full lg:w-3/5 relative h-64 lg:h-screen items-center justify-center p-8">
      
            <!-- 背景圖 -->
            <img src="../../Img/background.webp" alt="財務管理數據背景"
                class="absolute inset-0 w-full h-full object-cover z-0" />

            <!-- 疊加層 -->
            <div class="absolute inset-0 bg-black opacity-50 z-10"></div>

            <!-- 標語文字 -->
            <div class="relative z-20 text-white text-left max-w-2xl">
            
                <h1 class="text-3xl md:text-4xl lg:text-6xl font-extrabold leading-tight mb-8 lg:mb-12">
                    投資添財<br>
                    一站搞定所有難題
                </h1> 
            
                <p class="text-base md:text-lg lg:text-xl font-light opacity-90">
                    別再手動算得霧煞煞!<br>
                    您的數據分析師已上線,讓每一次投資都有憑有據。
                </p>

            </div>
        
        </div>
        
        <!-- 右側:忘記密碼表單區塊 (手機上全寬度,桌面切換為 40% 寬度) -->
        <div class="w-full lg:w-2/5 flex items-center justify-center min-h-screen lg:h-screen p-8 lg:p-12 bg-[#121b20]">
        
            <div class="max-w-sm mx-auto w-full backdrop-blur-2xl bg-white/65 hover:bg-white/75 p-8 rounded-2xl shadow-2xl border border-gray-300
                transition duration-300 text-gray-800">
        
                <div class="text-center mb-8">
                    <h2 class="text-3xl font-extrabold mb-2">忘記密碼</h2>
                    <p class="text-md text-black">請輸入您的信箱以接收重設連結</p>
                </div>

                <!-- 表單:綁定 sendResetLink -->
                <form @submit.prevent="sendResetLink" class="space-y-10">

                    <div>
                        <label for="email" class="block text-xl font-medium text-black">請輸入您的信箱</label>
                        
                        <div class="mt-1 relative rounded-lg shadow-sm">

                            <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                                <i class="fa-solid fa-envelope text-black/70 h-5 w-5" aria-hidden="true"></i>
                            </div>
                        
                            <input id="email" name="email" type="email" placeholder="請輸入信箱..."
                            class="block w-full pl-12 pr-4 py-3 border border-gray-400/50 rounded-lg shadow-sm
                            placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-black
                            bg-white/70 text-black transition duration-200"/>
                        </div>

                    </div>
                
                    <div>
                        <button type="submit"
                        class="w-full flex justify-center py-3 px-4 rounded-lg shadow-lg text-lg font-semibold text-white
                        bg-gray-800 hover:bg-black transition duration-200">
                        發送重設連結
                        </button>
                    </div>

                </form>
            
                <!-- 返回登入按鈕 -->
                <div class="text-center mt-6">
                    <button type="button" @click="emit('gotologin')" 
                    class="text-sm font-medium text-gray-700 hover:text-black transition duration-150 flex items-center justify-center mx-auto">
                        <i class="fa-solid fa-arrow-left h-4 w-4 mr-1"></i>
                        返回登入
                    </button>
                </div>

            </div>

        </div>

    </div>
</template>