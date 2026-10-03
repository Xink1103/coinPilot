import { defineStore } from 'pinia'
import { ref } from 'vue'
import { CONST as C } from '@/libs/constant'
import { useAuthStore } from './authStore'
import { useSurveyStore } from './surveyStore'
import { toast } from 'vue3-toastify';
import 'vue3-toastify/dist/index.css';

//定義localstorage key
const NAV_VIEW_KEY = 'app_current_view'

export const useNavStore = defineStore('nav', () => {
  // ============ State ============
  
  //從localstorage讀取我要的
  const initialView = localStorage.getItem(NAV_VIEW_KEY) || C.TOGGLE_STRATEGY  

  // 切換要顯示的頁面
  const currentView = ref(initialView)
  
  // 側邊欄的 展開/關閉 狀態 
  const isAsideOpen = ref(false)
 
  // 帳戶頭像的選單 展開 / 關閉 狀態 
  const isAccountDropdownOpen = ref(false)

  // ============ Actions ============
  
  //切換側邊欄狀態
  function toggleAside() {
    isAsideOpen.value = !isAsideOpen.value
    // 展開側邊欄時關閉帳戶選單
    if (isAsideOpen.value) {
      isAccountDropdownOpen.value = false
    }
  }

  //關閉側邊攔
  function closeAside() {
    isAsideOpen.value = false
  }

  //切換帳戶選單狀態
  function toggleAccountDropdown() {
    isAccountDropdownOpen.value = !isAccountDropdownOpen.value
    // 展開帳戶選單時關閉側邊欄
    if (isAccountDropdownOpen.value) {
      isAsideOpen.value = false
    }
  }

  //關閉帳戶選單
  function closeAccountDropdown() {
    isAccountDropdownOpen.value = false
  }

   // 通用切換方法（給 Navbar 使用）
  function changeView(viewName) {
    currentView.value = viewName
    localStorage.setItem(NAV_VIEW_KEY,viewName)
    closeAside()
    closeAccountDropdown()
  }

  // 切換到首頁
  function gotoHome() {
    changeView(C.TOGGLE_HOME);
  }
  
  //處理登出邏輯
  function handleLogoutAndRedirect() {
    const authStore = useAuthStore()
    const surveyStore = useSurveyStore()
    
    authStore.logout() // 執行 Auth Store 的登出
    surveyStore.reset() // 執行 Survey Store 的重置
    authStore.gotoLogin()
    
  }
  
  // ============ Return ============
  return {
    // State
    currentView,
    isAsideOpen,
    isAccountDropdownOpen,
    
    // Actions
    gotoHome,
    toggleAside,
    closeAside,
    toggleAccountDropdown,
    closeAccountDropdown,
    changeView,
    handleLogoutAndRedirect
  }
})