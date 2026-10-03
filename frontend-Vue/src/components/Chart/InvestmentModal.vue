<script setup>
import { ref } from 'vue';
import { toast } from 'vue3-toastify';
import 'vue3-toastify/dist/index.css';

//從父元件接收的資料
const props = defineProps({
    selected:{
        type:Boolean,
        default:false
    },
    assetData:{
        type:Object,
        default:()=>{
            return {
                name:"",
                code:""
            };
        }
    },
    //接受剩餘可投入的金額
    remainingAmount: {
        type: Number,
        default: 0
    },
    processing: { 
        type: Boolean,
        default: false
    }
})

//close代表modal關閉、confirm代表使用者確定要下單這個金額
const emit=defineEmits(["close","confirm"])

//下單多少錢
const inputAmount=ref("");

//按下確認
const handleConfirm=()=>{
    const amountToInvest = Number(inputAmount.value); 
    
    //簡易驗證輸入的數字
    if(!amountToInvest||amountToInvest<=0){
        toast.warning("請輸入有效的數字");
        return;
    }

    //測單筆金額會不會超過可以投入的金額
    if(amountToInvest>props.remainingAmount){
        toast.warning(`您的下單金額NT$${amountToInvest.toLocaleString()}已超過可投入資金`);
        return;
    }
    
    //將資產代碼跟金額回傳給父元件
    emit("confirm",{
        code:props.assetData.code,
        amount:amountToInvest
    });

};

</script>

<template>
    <Teleport to="body">
    <div v-if="selected" class="fixed inset-0 z-50 flex items-center justify-center px-4">
      
      <div class="absolute inset-0 bg-gray-900/60 backdrop-blur-sm transition-opacity" @click="!processing && $emit('close')"></div>

        <form class="relative bg-white rounded-2xl shadow-2xl w-full max-w-md p-6 overflow-hidden transform transition-all scale-100" @submit.prevent="handleConfirm"
        >
        
            <div class="mb-6 text-center">
                <h3 class="text-2xl font-bold text-gray-800">
                    下單確認
                </h3>
                <p class="text-gray-500 mt-2">
                    您選擇了：<span class="text-indigo-600 font-bold">{{ assetData.name }} ({{ assetData.code }})</span>
                </p>
            </div>

            <div class="mb-6">
                <label class="block text-sm font-medium text-gray-700 mb-2">欲投入金額 (NT$)</label>
                <input 
                    type="number" 
                    v-model="inputAmount"
                    step="1000"
                    min="0"
                    placeholder="請輸入金額"
                    class="w-full px-4 py-3 border-2 border-gray-300 rounded-xl focus:border-black focus:ring-1 outline-none text-lg text-center font-bold"
                    @keydown.enter.stop.prevent="handleConfirm"
                />
            </div>

            <div class="flex gap-4">
                <button 
                    @click="$emit('close')"
                    :disabled="processing"
                    class="flex-1 py-3 px-4 border-2 border-gray-300 text-gray-700 font-semibold rounded-xl transition-colors"
                    :class="{'hover:bg-gray-50': !processing, 'cursor-not-allowed opacity-70': processing}">
                    取消
                </button>
                <button 
                    :disabled="processing"
                    type="submit"
                    class="flex-1 py-3 px-4 bg-indigo-600 text-white font-semibold rounded-xl shadow-lg transition-all">
                    <span v-if="processing">處理中...</span>
                    <span v-else>確認下單</span>
                </button>
            </div>

        </form>
    </div>
  </Teleport>
</template>