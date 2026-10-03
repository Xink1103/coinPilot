<script setup>
import { ref } from 'vue';
import axios from 'axios';
import { toast } from 'vue3-toastify';
import 'vue3-toastify/dist/index.css';

// 使用 defineEmits 宣告這個元件可以發射哪些事件
const emit = defineEmits(['gotoLogin']); 

// 表單資料綁定
const userEmail = ref('');
const userName = ref('');
const userPassword = ref('');

// 錯誤訊息 (這些 ref 現在主要用於顯示在欄位下方的文字，但您也可以選擇只用 Toast)
const emailError = ref('');
const usernameError = ref('');
const passwordError = ref('');
const registerError = ref('');

// Email 驗證規則 (新增正規表達式驗證)
const validateEmail = () => {
    // 簡單的 Email 格式檢查：包含 @ 且 @ 後面有 . (點) 和至少兩個字元
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/; 

    if (!userEmail.value) {
        emailError.value = '請輸入 Email 帳號';
        return false;
    }
    if (!emailRegex.test(userEmail.value)) {
        emailError.value = 'Email 格式不正確 (例如: example@domain.com)';
        return false;
    }
    emailError.value = '';
    return true;
};

// 使用者名稱驗證規則
const validateUsername = () => {
    if (!userName.value) {
        usernameError.value = '請輸入使用者名稱';
        return false;
    }
    if (userName.value.length < 2) {
        usernameError.value = '使用者名稱至少需要 2 個字元';
        return false;
    }
    usernameError.value = '';
    return true;
};

// 密碼驗證規則
const validatePassword = () => {
    if (!userPassword.value) {
        passwordError.value = '請輸入密碼';
        return false;
    }
    if (userPassword.value.length < 5) {
        passwordError.value = '密碼至少需要 5 個字元';
        return false;
    }
    passwordError.value = '';
    return true;
};

// 表單提交
const completeSignup = async () => {
    // 驗證所有欄位
    const isEmailValid = validateEmail();
    const isUsernameValid = validateUsername();
    const isPasswordValid = validatePassword();
    
    // 收集所有錯誤訊息
    const errorMessages = [];
    if (!isEmailValid) errorMessages.push(emailError.value);
    if (!isUsernameValid) errorMessages.push(usernameError.value);
    if (!isPasswordValid) errorMessages.push(passwordError.value);

    if (errorMessages.length > 0) {
        // 統一彈出 Toast，並顯示第一個錯誤訊息
        toast.error(errorMessages[0] || '請檢查所有必填欄位。'); 
        return; // 驗證失敗，終止執行
    }
    
    // 驗證通過，執行 API 呼叫
    const signupData = {
        userEmail: userEmail.value,
        userName: userName.value,
        userPassword: userPassword.value
    };
    
    try {
        const response = await axios.post(`/api/users/signup`, signupData);

        // 檢查 HTTP 狀態碼
        if (response.status === 201) { 
            toast.success('註冊成功！請登入。'); // 成功提示
            emit('gotoLogin'); 
        }

    } catch (error) {
        console.error('註冊 API 呼叫失敗:', error);
        
        // 處理錯誤響應
        if (error.response) {
            const status = error.response.status;

            if (status === 409) { 
                registerError.value = error.response.data.message || '註冊失敗，此 Email 已被使用。';
            } else if (status === 400) { 
                registerError.value = error.response.data.message || '請求資料有誤，請檢查欄位。';
            } else if (status === 500) {
                 registerError.value = '伺服器內部錯誤，請稍後再試。';
            } else {
                registerError.value = `發生錯誤 (Status: ${status})：${error.response.data.message || '請聯繫管理員'}`;
            }
        } else if (error.request) {
            registerError.value = '無法連線到伺服器，請檢查後端是否已啟動。';
        } else {
            registerError.value = '發生未知錯誤。';
        }
        
        // 彈出 API 錯誤 Toast
        if (registerError.value) {
            toast.error(registerError.value);
        }
    }
};  
</script>

<template>
    <div class="w-full min-h-screen bg-gray-900 flex flex-col lg:flex-row ">
        
        <div class="flex w-full lg:w-3/5 relative min-h-screen items-center justify-center">
            
            <img 
                src="../../Img/background.webp" 
                alt="財務管理數據背景"
                class="absolute inset-0 w-full h-full object-cover z-0" 
            />

            <div class="absolute inset-0 bg-black opacity-50 z-10"></div>

            <div class="relative z-20 text-white text-left max-w-2xl p-8">
                
                <h1 class="text-3xl md:text-4xl lg:text-6xl font-extrabold leading-tight mb-8 lg:mb-16">
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
        
            <div class="max-w-sm mx-auto w-full backdrop-blur-2xl bg-white/65 p-8 rounded-2xl shadow-2xl border border-gray-300 text-gray-800 scale-95">
            
                <div class="text-center mb-8">
                    <h2 class="text-3xl font-extrabold mb-2">用戶註冊</h2>
                    <p class="text-md text-black">立即加入一起輕鬆管理財務 !</p>
                </div>

                <div class="space-y-6">

                    <div>
                        <label for="userEmail" class="block text-xl font-medium text-black">Email</label>
                        
                        <div class="mt-1 relative rounded-lg shadow-sm">

                            <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                                <i class="fa-solid fa-envelope text-black/70 h-5 w-5" aria-hidden="true"></i>
                            </div>

                            <input 
                                id="userEmail" 
                                v-model="userEmail"
                                placeholder="請輸入 Email..."
                                :class="[
                                    'block w-full pl-12 pr-4 py-3 border rounded-lg shadow-sm',
                                    'placeholder-gray-400 focus:outline-none focus:ring-2',
                                    'bg-white/70 text-black transition duration-200',
                                    'border-gray-400/50 focus:ring-gray-800'
                                ]"
                            />
                        </div>
                        
                        <p v-if="emailError" class="mt-2 text-sm text-red-600">
                            {{ emailError }}
                        </p>

                    </div>

                    <div>
                        <label for="userName" class="block text-xl font-medium text-black">使用者名稱</label>
                        
                        <div class="mt-1 relative rounded-lg shadow-sm">

                            <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                                <i class="fa-solid fa-user text-black/70 h-5 w-5" aria-hidden="true"></i>
                            </div>

                            <input 
                                id="userName" 
                                v-model="userName"
                                type="text" 
                                placeholder="請輸入使用者名稱..."
                                :class="[
                                    'block w-full pl-12 pr-4 py-3 border rounded-lg shadow-sm',
                                    'placeholder-gray-400 focus:outline-none focus:ring-2',
                                    'bg-white/70 text-black transition duration-200',
                                    'border-gray-400/50 focus:ring-gray-800'
                                ]"
                            />
                        </div>
                        
                        <p v-if="usernameError" class="mt-2 text-sm text-red-600">
                            {{ usernameError }}
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
                                    'placeholder-gray-400 focus:outline-none focus:ring-2',
                                    'bg-white/70 text-black transition duration-200',
                                     'border-gray-300 focus:ring-gray-800'
                                ]"
                            />
                        </div>
                        
                        <p v-if="passwordError" class="mt-2 text-sm text-red-600">
                            {{ passwordError }}
                        </p>
                        
                        <p class="mt-2 text-xs text-gray-600">
                            密碼須至少 5 個字元
                        </p>

                    </div>

                    <button 
                        @click="completeSignup"
                        type="button"
                        class="w-full flex justify-center py-3 px-4 rounded-lg shadow-lg text-lg font-semibold text-white
                        bg-gray-800 hover:bg-black transition duration-200">
                        註冊完成,前往登入頁面
                    </button>

                </div>

                <p class="mt-6 text-center text-sm text-black">
                已有帳號?
                    <a @click="emit('gotoLogin')" 
                        class="font-medium text-gray-800 hover:text-black cursor-pointer transition duration-150">
                        直接登入
                    </a>
                </p>

            </div>

        </div>

    </div>

</template>