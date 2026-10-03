import { defineStore } from 'pinia'
import { ref , computed } from 'vue'
import { CONST as C } from '@/libs/constant'
import axios from 'axios'

export const useAuthStore = defineStore('auth', () => {
  // ============ State ============
  
  // 登入狀態（signup, login, userinfo, resetpassword）
  const authStatus = ref(C.TOGGLE_HOME);
  
  const userInfo = ref(null);

  const isInitializing = ref(true);
  
  // ============ Getters ============
  
  // 是否已登入
  const isLoggedIn = computed(() => authStatus.value === C.STATUE_USER || authStatus.value === C.STATUE_ADMIN )
  
   // 判斷是否為管理員
  const isAdmin = computed(() => {
    return userInfo.value?.userRole === 'ADMIN'
  })

  // ============ Actions ============
  
  //設定 Auth 狀態的通用 Action
  function setAuthStatus(status) {
        authStatus.value = status;
  }

  //前往首頁
  function gotoHome(){
    authStatus.value = C.TOGGLE_HOME;
  }

  // 前往註冊頁面
  function gotoSignup() {
    authStatus.value = C.STATUE_SIGNUP
  }
  
  // 前往登入頁面
  function gotoLogin() {
    authStatus.value = C.STATUE_LOGIN
  }
  
  // 前往重設密碼頁面
  function gotoResetPassword() {
    authStatus.value = C.STATUE_RESETPASSWORD
  }
  
  // 登入成功
  function login(userData) {
    userInfo.value = userData
    if(userData?.userRole == 'ADMIN'){
      authStatus.value = C.STATUE_ADMIN; 
    }else{
      authStatus.value = C.STATUE_USER;
    }
  }
  
  // 登出
  function logout() {
    authStatus.value = C.STATUE_LOGIN
    userInfo.value = null
    localStorage.removeItem('authToken')
    localStorage.removeItem('currentUser')
  }

  //檢查登入狀態
  async function checkAuthStatus() {
    isInitializing.value = true // 開始初始化
    
    const token = localStorage.getItem('authToken')
    const savedUser = localStorage.getItem('currentUser')
    
    // 同步檢查 localStorage
    if (savedUser && token) {
      try {
        const userData = JSON.parse(savedUser)
        login(userData) // 立即登入
      } catch (e) {
        console.error('解析用戶資料失敗:', e)
      }
    }
    
    // 如果沒有 token，結束初始化
    if (!token) {
      if (isLoggedIn.value) {
        logout()
      }
      isInitializing.value = false
      return false
    }

    // 異步驗證 token 是否有效
    try {
      const response = await axios.get('/api/users/me', {
        headers: { 'Authorization': token }
      })

      if (response.status === 200) {
        login(response.data)
        // 同步更新 localStorage
        localStorage.setItem('currentUser', JSON.stringify(response.data))
        isInitializing.value = false
        return true
      }
    } catch (error) {
      console.warn('JWT 驗證失敗:', error.response?.status || error.message)
      
      if (error.response?.status === 401 || error.response?.status === 403) {
        logout()
      }
      isInitializing.value = false
      return false
    }
    
    isInitializing.value = false
    return false
  }
  
  // ============ Return ============
  return {
    // State
    authStatus,
    userInfo,
    isInitializing,
    
    // Getters
    isLoggedIn,
    isAdmin,

    // Actions
    gotoHome,
    gotoSignup,
    gotoLogin,
    gotoResetPassword,
    login,
    logout,
    checkAuthStatus
  }
})