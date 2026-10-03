<script setup>
import { computed, onBeforeMount } from 'vue'
import { CONST as C } from './libs/constant'

// 引入 Stores
import { useAuthStore } from '@/stores/authStore'
import { useNavStore } from '@/stores/navStore'
import { useSurveyStore } from '@/stores/surveyStore'

// 引入組件
import Home from './components/Home.vue'
import Signup from './components/Signup.vue'
import Login from './components/Login.vue'
import ForgetPassword from './components/ForgetPassword.vue'
import Admin from './components/Admin.vue'
import Navbar from './components/Navbar.vue'
import PersonalData from './components/Chart/PersonalData.vue'
import StockAnalyze from './components/Chart/StockAnalyze.vue'
import ForexAnalyze from './components/Chart/ForexAnalyze.vue'
import Survey from './components/Chart/Survey.vue'

// 取得 Stores
const authStore = useAuthStore()
const navStore = useNavStore()
const surveyStore = useSurveyStore()

// 使用 computed 來響應式獲取 store 的數據
const authStatus = computed(() => authStore.authStatus)
const currentView = computed(() => navStore.currentView)

// ============ 登入相關方法 ============

// 前往註冊
const gotoSignup = () => {
  authStore.gotoSignup()
}

// 前往登入
const gotoLogin = () => {
  authStore.gotoLogin()
}

// 前往重設密碼
const gotoResetPassword = () => {
  authStore.gotoResetPassword()
}

// 登入成功（從 Login 組件觸發）
const gotoDashboard = async(userData) => {
  authStore.login(userData) 
  if(userData && userData.userRole === 'ADMIN'){
    navStore.changeView(C.TOGGLE_ADMIN);
  }else{
    await checkSurveyData();
  }
}

//檢查有沒有問卷資料
const checkSurveyData = async()=>{
  try{
    const surveyData = await surveyStore.loadExistingSurvey()
    if(surveyData && surveyData.surveyId){
      navStore.changeView(C.TOGGLE_PERSONDATA)
    }else{
      navStore.changeView(C.TOGGLE_STRATEGY)
    }

  }catch(e){
    console.log('檢查問卷資料時發生錯誤，預設跳轉到問卷頁面:', error)
    navStore.changeView(C.TOGGLE_STRATEGY)
  }
  
}

//實作登入後的檢查
onBeforeMount(async () => {
  await authStore.checkAuthStatus()

  if(authStore.authStatus == C.STATUE_ADMIN){
    navStore.changeView(C.TOGGLE_ADMIN)
  }else if(
    authStore.authStatus !== C.STATUE_USER && 
    authStore.authStatus !== C.STATUE_ADMIN &&
    authStore.authStatus !== C.STATUE_SIGNUP &&
    authStore.authStatus !== C.STATUE_LOGIN &&
    authStore.authStatus !== C.STATUE_RESETPASSWORD
  ){
    authStore.gotoHome()
  }
});

</script>

<template>
  <div class="min-h-screen w-full" id="app">
    
    <Home v-if="authStatus === C.TOGGLE_HOME" @gotoLogin="gotoLogin"
    @gotoSignup="gotoSignup" />

    <!-- 未登入，判斷註冊或登入 -->
    <Signup 
      v-if="authStatus === C.STATUE_SIGNUP" 
      @gotoLogin="gotoLogin"
    />
    
    <Login 
      v-if="authStatus === C.STATUE_LOGIN" 
      @gotoDashboard="gotoDashboard" 
      @gotoSignup="gotoSignup" 
      @gotoResetPassword="gotoResetPassword"
    />
    
    <ForgetPassword 
      v-if="authStatus === C.STATUE_RESETPASSWORD" 
      @gotologin="gotoLogin"
    />
    
    <!-- 已登入，顯示整個應用程式的主結構 -->
    <div v-if="authStatus === C.STATUE_USER" class="min-h-screen w-full bg-[#457B9D] flex flex-col">
      
      <!-- Nav 區塊 -->
      <Navbar/> 
      
      <!-- Homepage 區塊（不再需要 props，直接從 store 讀取） -->
      

      <!-- Chart 區塊（不再需要 props，直接從 store 讀取） -->
      <PersonalData v-if="currentView === C.TOGGLE_PERSONDATA" />
      
      <StockAnalyze v-if="currentView === C.TOGGLE_STOCKANALYZE" />
      
      <ForexAnalyze v-if="currentView === C.TOGGLE_FOREANALYZE" />
      
      <!-- Survey 不再需要 emit data，只需要通知完成 -->
      <Survey v-if="currentView === C.TOGGLE_STRATEGY" />
    
    </div>

    <div v-else-if="authStatus == C.STATUE_ADMIN" class="min-h-screen w-full">
      <Admin />
    </div>
    
  </div>
</template>