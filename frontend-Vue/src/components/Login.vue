<script setup>
import { defineEmits, ref } from 'vue';
import axios from 'axios';
import { toast } from 'vue3-toastify';
import 'vue3-toastify/dist/index.css';

const emits = defineEmits(['gotoDashboard', 'gotoSignup', 'gotoResetPassword']);

// 資料綁定
const userEmail = ref('');
const userPassword = ref('');

// 錯誤訊息
const emailError = ref('');
const passwordError = ref('');
const loginError = ref(''); 

// Email 驗證規則
const validateEmail = () => {
    if (!userEmail.value) {
        emailError.value = '請輸入 Email 帳號';
        return false;
    }
    emailError.value = '';
    return true;
};

// 密碼驗證規則
const validatePassword = () => {
    if (!userPassword.value) {
        passwordError.value = '請輸入密碼';
        return false;
    }
    passwordError.value = '';
    return true;
};

const CompleteLogin = async () => {
    // 執行驗證
    const isEmailValid = validateEmail();
    const isPasswordValid = validatePassword();

    // 儲存所有驗證錯誤訊息，用於統一的 Toast 提示
    const errorMessages = [];

    // 檢查欄位是否有效
    if (!isEmailValid) {
        errorMessages.push(emailError.value);
    }
    if (!isPasswordValid) {
        errorMessages.push(passwordError.value);
    }

    // 如果有任何驗證失敗，則統一彈出 Toast 並終止
    if (errorMessages.length > 0) {
        toast.error('請檢查並填寫所有必填欄位。'); 
        return;
    }

    // 重置全局錯誤訊息
    loginError.value = '';

    try {
        const response = await axios.post('/api/users/login', {
            userEmail: userEmail.value,
            userPassword: userPassword.value
        });

        // 確保 API 呼叫成功
        if (response.status === 200) {
            const token = response.data.token;
            const user = response.data;
            
            if (token) {
                localStorage.setItem('authToken', token);
                localStorage.setItem('currentUser', JSON.stringify(user));

                // 傳遞 user 資料到父組件 (用於處理管理員導航問題)
                emits('gotoDashboard', user); 
            } else {
                loginError.value = '登入成功但未收到驗證資訊，請聯繫管理員。';
                toast.error(loginError.value);
            }
        }

    } catch (error) {
        console.error('登入失敗:', error);

        if (error.response) {
            loginError.value = error.response.data.message || '登入失敗，請檢查帳號密碼';
        } else if (error.request) {
            loginError.value = '無法連線到伺服器，請稍後再試';
        } else {
            loginError.value = '發生未知錯誤';
        }
        toast.error(loginError.value);
    }
}
</script>

<template>
    <div class="w-full min-h-screen bg-gray-900 flex flex-col lg:flex-row">
    
        <div class="flex w-full lg:w-3/5 relative min-h-screen items-center justify-center p-8">
            
            <img 
                src="../../Img/background.webp" 
                alt="財務管理數據背景"
                class="absolute inset-0 w-full h-full object-cover z-0" 
            />

            <div class="absolute inset-0 bg-black opacity-50 z-10"></div>

            <div class="relative z-20 text-white text-left max-w-2xl p-8">
                
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
        
        <div class="w-full lg:w-2/5 flex items-center justify-center p-8 lg:p-12 bg-[#121b20]">

            <div class="max-w-sm mx-auto w-full backdrop-blur-2xl bg-white/65 hover:bg-white/75 p-8 rounded-2xl shadow-2xl border border-gray-300
            transition duration-300 text-gray-800">
            
                <div class="text-center mb-8">
                    <h2 class="text-3xl font-extrabold mb-2">登入系統</h2>
                    <p class="text-md text-black">登入並開始您的智慧理財體驗!</p>
                </div>

                <form @submit.prevent="CompleteLogin" class="space-y-6">

                    <div>
                        <label for="userEmail" class="block text-xl font-medium text-black">Email 帳號</label>
        
                        <div class="mt-1 relative rounded-lg shadow-sm">
            
                            <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                                <i class="fa-solid fa-envelope text-black/70 h-5 w-5" aria-hidden="true"></i>
                            </div>
            
                            <input 
                                id="userEmail" 
                                v-model="userEmail"  
                                placeholder="請輸入 Email 帳號..."
                                :class="[
                                    'block w-full pl-12 pr-4 py-3 border rounded-lg shadow-sm',
                                    'placeholder-gray-400 focus:outline-none focus:ring-2 bg-white/70 text-black transition duration-200 border-gray-400/50 focus:ring-gray-800'
                                ]"
                            /> 
                        </div>
                        <p v-if="emailError" class="mt-2 text-sm text-red-600">
                            {{ emailError }}
                        </p>
                    </div>
                    
                    <div>
                        <label for="userPassword" class="block text-xl font-medium text-black">密碼</label>
        
                        <div class="mt-1 relative rounded-lg shadow-sm">
            
                            <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                                <i class="fa-solid fa-lock text-black/70 h-5 w-5" aria-hidden="true"></i>
                            </div>
                            
                            <input 
                                id="userPassword" 
                                v-model="userPassword" 
                                type="password" 
                                placeholder="請輸入密碼..."
                                :class="[
                                    'block w-full pl-12 pr-4 py-3 border rounded-lg shadow-sm',
                                    'placeholder-gray-400 focus:outline-none focus:ring-2 bg-white/70 text-black transition duration-200 border-gray-400/50 focus:ring-gray-800'
                                ]"
                            />
                        </div>
                        <p v-if="passwordError" class="mt-2 text-sm text-red-600">
                            {{ passwordError }}
                        </p>
                    </div>
                    
                    <p v-if="loginError" class="text-sm font-medium text-center text-red-600 border border-red-300 bg-red-50/50 p-2 rounded-lg">
                        {{ loginError }}
                    </p>

                    <button type="submit"
                    class="w-full flex justify-center py-3 px-4 rounded-lg shadow-lg text-lg font-semibold text-white
                    bg-gray-800 hover:bg-black transition duration-200">
                    登入
                    </button>

                </form>

               <div class="flex justify-center mt-8 text-sm">
    
                    <button type="button" @click="emits('gotoSignup')" 
                            class="flex items-center space-x-3 py-1 px-3 rounded-lg 
                                font-medium text-gray-700 
                                hover:text-black hover:bg-gray-100/50
                                transition duration-200 
                                cursor-pointer">
                        <i class="fa-solid fa-user-plus h-3 w-3"></i>
                        <span>還沒註冊?</span>
                    </button>
                    
                    <!-- <button type="button" @click="emits('gotoResetPassword')" 
                            class="flex items-center space-x-3 py-1 px-3 rounded-lg 
                                font-medium text-gray-700 
                                hover:text-black hover:bg-gray-100/50
                                transition duration-200 
                                cursor-pointer">
                        <i class="fa-solid fa-rotate-right h-3 w-3"></i>
                        <span>忘記密碼?</span>
                    </button> -->
                    
                </div>

            </div>

        </div>

    </div>
</template>