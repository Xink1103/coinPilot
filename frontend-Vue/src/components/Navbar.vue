<script setup>
  import { ref,onMounted,onUnmounted,defineEmits} from 'vue';
  import { useNavStore } from '@/stores/navStore'
  import { toast } from 'vue3-toastify';
  import 'vue3-toastify/dist/index.css';

  const navStore = useNavStore();

  const menuItem=ref([
    {name:'股票分析',href:'.',viewName:'stockAnalyze'},
    {name:'外幣分析',href:'.',viewName:'forexAnalyze'},
    {name:'投資策略',href:'.',viewName:'investmentStrategy'}
  ])

  const handleAsideItem=(viewName)=>{
    navStore.changeView(viewName);
  };

  //點擊以後關閉然後傳送資料給父元件
  const handleViewPersonalData = () => {
    navStore.changeView('personalData');
  };

  //處理登出
  const handleLogout = () => {
    navStore.handleLogoutAndRedirect();
    toast.success("您已登出");
  };

  
  //處理點擊其他地方會自動關閉選單的問題
  const asideRef = ref(null);         // 側邊欄 Aside 容器
  const dropdownRef = ref(null);      // 帳戶下拉選單容器
  const asideButtonRef = ref(null);   // 漢堡選單按鈕
  const accountButtonRef = ref(null); // 帳戶圖標按鈕

  const handleClickOutside = (event) => {
  // 檢查點擊是否在 aside 選單內 或 aside 按鈕上
  if (navStore.isAsideOpen && // 
      asideRef.value && 
      !asideRef.value.contains(event.target) &&
      !asideButtonRef.value.contains(event.target)) {
    navStore.closeAside();
  }
  
  // 檢查點擊是否在 dropdown 選單內 或 dropdown 按鈕上
  if (navStore.isAccountDropdownOpen && // 
      dropdownRef.value && 
      !dropdownRef.value.contains(event.target) &&
      !accountButtonRef.value.contains(event.target)) {
    navStore.closeAccountDropdown(); // 
   }
  };

  //從27~52在做時間
  const username=ref('使用者');
  const currentTime = ref('');
  let timer = null;

  const updateTime=()=>{
    const now=new Date();
    currentTime.value=now.toLocaleDateString('zh-TW',{
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    }).replace(/\//g, '.') + ' ' + now.toLocaleTimeString('zh-TW');
  };

  //讀取使用者的名稱
  const loadUserName = () =>{
    const userJson = localStorage.getItem('currentUser');
    if(userJson){
      try{

        const user = JSON.parse(userJson);
        if(user && user.userName){
          username.value = user.userName;
        }

      }catch(e){
        console.error('解析 currentUser JSON 失敗:', e);
        username.value = '讀取失敗';
      }
    }
  }


  // 元件載入後啟動計時器
  onMounted(()=>{
    updateTime(); //立即更新一次時間
    timer = setInterval(updateTime, 1000); // 每秒更新一次

    document.addEventListener('click', handleClickOutside);
    //使用者名稱
    loadUserName();

  })

  // 元件卸載前清除計時器，避免記憶體洩漏
  onUnmounted(() => {
    if (timer) {
        clearInterval(timer);
    }

    document.removeEventListener('click', handleClickOutside);

});

</script>

<template>
  <nav class="flex justify-between sticky top-0 z-10 items-center bg-[#0E1E2B] text-[#F1FAEE] shadow-md px-8 h-16 shrink-0"
    style="
        box-shadow: 0 4px 15px rgba(0, 0, 0, 0.5), 0 0 5px rgba(255, 255, 255, 0.08);
        border-bottom: 1px solid rgba(255, 255, 255, 0.15);
    ">
    
    <div class="flex items-center"> 
      <i class="fa-solid fa-hand-holding-dollar text-xl mr-3"></i> 
      <h2 class="text-xl font-semibold text-[#F1FAEE]">CoinPilot</h2>
    </div>
    
    <div class="text-[#F1FAEE] font-semibold text-lg mr-42 ml-13 whitespace-nowrap">
      {{username}} 你好
    </div>

    <div class="text-[#F1FAEE] text-base font-medium mr-auto ml-4 whitespace-nowrap">
      現在時間：{{currentTime}}
    </div>
    
    <!-- 右側按鈕群組 -->
    <div class="flex items-center space-x-4"> 
      
      <!-- 個人頭像下拉選單 -->
      <div class="relative">
        
        <button ref="accountButtonRef" @click.stop="navStore.toggleAccountDropdown" class="text-[#F1FAEE] hover:text-[#457B9D] transition duration-150 focus:outline-none">
          <i class="fa-solid fa-circle-user text-2xl cursor-pointer"></i>
        </button>

        <div ref="dropdownRef" v-show="navStore.isAccountDropdownOpen" 
        @click.stop
        class="absolute -right-16 top-11 w-40 bg-[#2d3748] rounded-bl-xl rounded-b-xl shadow-2xl border border-gray-600 z-50 overflow-hidden">
          
          <!-- 簡化的頂部 -->
          <div class="py-3 px-4 bg-[#1a202c] border-b border-gray-600 flex items-center">
            <i class="fa-solid fa-user text-gray-400 text-base"></i>
            <span class="ml-2 text-sm text-gray-300">{{username}}</span>
          </div>
          
          <!-- 選單項目 -->
          <a @click.prevent="handleViewPersonalData"
            class="block py-3 px-4 text-sm text-gray-300 hover:bg-[#3d4a5c] hover:text-white hover:pl-7 cursor-pointer transition-all duration-200">
              個人資料
          </a>
          
          <a @click.prevent="handleLogout"
            class="block py-3 px-4 text-sm font-medium text-red-400 hover:bg-[#3d4a5c] hover:text-red-300 hover:pl-7 cursor-pointer transition-all duration-200 border-t border-gray-600">
              <i class="fa-solid fa-right-from-bracket mr-2"></i> 登出
          </a>
        </div>
          
      </div>

      <!-- 漢堡選單按鈕 -->
      <button ref="asideButtonRef" @click.stop="navStore.toggleAside" class="text-[#F1FAEE] hover:text-[#457B9D] transition duration-150">
        <i class="fa-solid fa-bars text-xl"></i>
      </button>
      
    </div>
        
  </nav>
  
  
  <!-- 側邊選單 (ASIDE) -->
  <aside ref="asideRef" v-show="navStore.isAsideOpen"
  :class="{
    'w-36':navStore.isAsideOpen,
    'w-0':!navStore.isAsideOpen
  }" 
  class="fixed top-16 right-0 bg-[#2d3748] border-l border-b border-gray-600 flex flex-col h-fit rounded-bl-xl transition-all duration-300 ease-in-out z-40 shadow-2xl">  
    <!-- 項目列表 -->
    <ul class="text-gray-300 pt-2 pb-2 z-10">
      <li v-for="item in menuItem" v-bind:key="item.name">
        <a
          @click.prevent="handleAsideItem(item.viewName)"
          class="block py-3.5 text-base transition-all duration-200 hover:bg-[#3d4a5c] hover:text-white hover:pl-5 cursor-pointer pl-10"
        >
          {{item.name}}
        </a>
      </li>
    </ul>
    
  </aside>
</template>