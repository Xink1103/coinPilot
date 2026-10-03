import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import axios from 'axios'
import { usePriceStore } from './usePriceStore'
import { useAssetStore } from '@/stores/assetStore';


export const useSurveyStore = defineStore('survey', () => {
 
  const assetStore = useAssetStore();
  const priceStore = usePriceStore();

  const DEV_MODE = false;  //開發時設為 true，上線前改成 false
  
  // ============ State ============
  
  const answers = ref({})
  const analysisResult = ref(null)
  const showResults = ref(false)
  const investedAmount = ref(0)
  const transactionHistory = ref([])
  const isProcessingOrder = ref(false)
  const isModalVisible = ref(false)
  const currentModalAsset = ref({})
  const modalKey = ref(0)

  //載入中
  const isLoading = ref(false) 
  
  // ============ Getters ============
  
  const totalInvestmentLimit = computed(() => {
    return Number(answers.value[11]) || 0
  })
  
  const remainingAmount = computed(() => {
    const remaining = totalInvestmentLimit.value - investedAmount.value
    return Math.max(0, remaining)
  })
  
  // ============ Actions ============
  
  function setAnswer(questionId, value) {
    answers.value[questionId] = value
  }
  
  function setAllAnswers(allAnswers) {
    answers.value = allAnswers
  }
  
  function setAnalysisResult(result) {
    analysisResult.value = result
    showResults.value = true
  }
  
  function addTransaction(transaction) {
    transactionHistory.value.push(transaction)
    investedAmount.value += transaction.amount
  }
  
  function setProcessingOrder(status) {
    isProcessingOrder.value = status
  }
  
  function openModal(type, asset) {
    currentModalAsset.value = { ...asset, type,assetId:asset.assetId }
    isModalVisible.value = true
    modalKey.value++
  }
  
  function closeModal() {
    isModalVisible.value = false
  }
  
  function reset() {
    answers.value = {}
    analysisResult.value = null
    showResults.value = false
    investedAmount.value = 0
    transactionHistory.value = []
    isProcessingOrder.value = false
    isModalVisible.value = false
    currentModalAsset.value = {}
    modalKey.value = 0
  }

  // 計算投資組合
function calculateScore() {
  // 計算風險分數 (Q5, Q6, Q8, Q9, Q10)
  let riskScore = 0
  riskScore += Number(answers.value[5]) || 0
  riskScore += Number(answers.value[6]) || 0
  riskScore += Number(answers.value[8]) || 0
  
  const goalScore = {
    'CapitalPreservation': 1,
    'StableIncome': 2,
    'HighReturn': 3
  }
  riskScore += goalScore[answers.value[9]] || 0
  riskScore += Number(answers.value[10]) || 0

  // 判斷資金水平 (Q11: 投資金額)
  const investmentAmount = Number(answers.value[11]) || 0
  const isHighFund = investmentAmount >= 500000
  const assetPreference = answers.value[7] || ""

  let profileType = ""
  let baseAllocation = { stock: 0, forex: 0, cash: 0 }

  if (riskScore <= 10) {
    profileType = '保守型'
    if (isHighFund) {
      baseAllocation = { stock: 25, forex: 50, cash: 25 }  // 保守型-高資金
    } else {
      baseAllocation = { stock: 15, forex: 55, cash: 30 }  // 保守型-低資金
    }
  } else if (riskScore <= 15) {
    profileType = '穩健型'
    if (isHighFund) {
      baseAllocation = { stock: 60, forex: 30, cash: 10 }  // 穩健型-高資金
    } else {
      baseAllocation = { stock: 45, forex: 40, cash: 15 }  // 穩健型-低資金
    }
  } else {
    profileType = '積極型'
    if (isHighFund) {
      baseAllocation = { stock: 75, forex: 20, cash: 5 }   // 積極型-高資金
    } else {
      baseAllocation = { stock: 85, forex: 10, cash: 5 }   // 積極型-低資金
    }
  }

  if (assetPreference === "Stock") {
    baseAllocation.stock += 10
    baseAllocation.forex -= 10
  } else if (assetPreference === "ForeignCurrency") {
    baseAllocation.forex += 10
    baseAllocation.stock -= 10
  }

  // 確保比例不會變成負數
  if (baseAllocation.stock < 0) {
    const diff = Math.abs(baseAllocation.stock)
    baseAllocation.stock = 0
    baseAllocation.cash += diff
  }
  if (baseAllocation.forex < 0) {
    const diff = Math.abs(baseAllocation.forex)
    baseAllocation.forex = 0
    baseAllocation.cash += diff
  }

  // 重新平衡到 100%
  const total = baseAllocation.stock + baseAllocation.forex + baseAllocation.cash
  if (total !== 100) {
    const diff = 100 - total
    baseAllocation.cash += diff
  }

  // 使用 assetStore 獲取股票/ETF 和外幣建議
  const stocks = assetStore.getStockRecommendations(riskScore, isHighFund, baseAllocation)
  const forexs = assetStore.getForexRecommendations(riskScore, isHighFund, baseAllocation)

  // 計算總和（用於修正誤差）
  let allocationSum = stocks.reduce((sum, current) => sum + current.percentage, 0)
  allocationSum += forexs.reduce((sum, current) => sum + current.percentage, 0)

  // 修正誤差到現金
  const totalInvestmentPercent = baseAllocation.stock + baseAllocation.forex
  const totalError = totalInvestmentPercent - allocationSum
  baseAllocation.cash += totalError

  return {
    profileType,
    riskScore,
    investmentAmount,
    assetPreference,
    allocation: baseAllocation,
    stocks,
    forexs
  }
}


  // 建立交易
  async function createTransaction(assetId, amount) {
    try {
      if (!assetId) {
        console.error('assetId 無效:', assetId)
        console.error('currentModalAsset:', currentModalAsset.value)
        throw new Error(`無效的資產 ID: ${assetId}`)
      }

      let transactionData

      if (DEV_MODE) {
      // 開發模式：使用 priceStore 的快照價格
      const asset = priceStore.getAssetById(assetId)
      
      if (!asset || !asset.transCurrentPrice) {
        throw new Error(`找不到資產 ID ${assetId} 的價格資料`)
      }
      
      const currentPrice = asset.transCurrentPrice
      
      // 計算購買數量
      const quantity = amount / currentPrice
      
      // 決定小數位數
      const decimalPlaces = 2
      
      transactionData = {
        transId: Date.now(),
        assetCode: asset.assetCode,
        assetName: asset.assetName,
        assetType: asset.assetType,
        transTradePrice: parseFloat(currentPrice.toFixed(4)),  
        transCurrentPrice: parseFloat(currentPrice.toFixed(4)), 
        transQuantity: parseFloat(quantity.toFixed(decimalPlaces)),
        transAmount: amount,
        assetId: assetId  
      }
      
      console.log("使用快照價格:", currentPrice)
      
    } else {
      const token = localStorage.getItem('authToken');
      if(!token){
        throw new Error('未找到登入憑證，請先登入。');
      }

      const config = {
        headers:{
          'Authorization': token
        }
      }

      const surveyId = analysisResult.value.surveyId;
      if(!surveyId){
        throw new Error('找不到surveyId')
      }

      const response = await axios.post('/api/transactions', {
        assetId: assetId,
        amount: amount,
        surveyId: surveyId
      }, config)
      
      transactionData = response.data
    }

    const costPrice = parseFloat(Number(transactionData.transTradePrice).toFixed(4))
    const currentPrice = parseFloat(Number(transactionData.transCurrentPrice).toFixed(4))
    const quantity = parseFloat(Number(transactionData.transQuantity).toFixed(2))
    const preciseAmount = parseFloat(Number(transactionData.transAmount).toFixed(2))

    const profitAmount = parseFloat(((currentPrice - costPrice) * quantity).toFixed(2))
    const profitRate = costPrice !== 0 
      ? parseFloat((((currentPrice - costPrice) / costPrice) * 100).toFixed(2))
      : 0

    // 轉換格式
    let frontendAssetType = transactionData.assetType
    if (transactionData.assetType === "STOCK") {
      frontendAssetType = "Stock/ETF"
    } else if (transactionData.assetType === "FOREX") {
      frontendAssetType = "ForeignCurrency"
    }

    // 組成完整交易記錄
    const newTransaction = {
      id: Date.now(),
      transId: transactionData.transId || Date.now(),
      assetId: transactionData.assetId || assetId,  
      code: transactionData.assetCode,
      name: transactionData.assetName,
      assetType: frontendAssetType,
      amount: preciseAmount,
      price: costPrice,  // 固定 4 位小數的成交價
      shares: quantity,
      currentPrice: currentPrice,  // 固定 4 位小數的當前價
      profit: profitAmount,  // 固定 2 位小數的損益金額
      profitRate: profitRate,  // 固定 2 位小數的損益率
      date: new Date().toLocaleString()
    }

    // 自動新增到歷史紀錄
    addTransaction(newTransaction)

    return newTransaction

  } catch (error) {
    console.error("交易失敗：", error)
    throw error
  }
}

  // 載入使用者所有的交易紀錄
  async function loadUserTransactions() {
    const token = localStorage.getItem('authToken');
    if (!token) {
      console.warn('未找到 Token，無法載入交易紀錄');
      return;
    }
    
    if (DEV_MODE) {
      return;
    }
    
    try {
      const response = await axios.get('/api/transactions', {
        headers: {
          'Authorization': token
        }
      });

      const backendTransaction = response.data;
      transactionHistory.value = [];
      investedAmount.value = 0;

      backendTransaction.forEach(t => {
        let frontendAssetType = t.assetType;
        if (t.assetType === "STOCK") {
          frontendAssetType = "Stock/ETF";
        } else if (t.assetType === "FOREX") {
          frontendAssetType = "ForeignCurrency";
        }

        // 固定精度
        const costPrice = parseFloat(Number(t.transTradePrice).toFixed(4));
        const quantity = parseFloat(Number(t.transQuantity).toFixed(2));
        const preciseAmount = parseFloat(Number(t.transAmount).toFixed(2));
        const currentPrice = parseFloat(Number(t.transCurrentPrice).toFixed(4));
        
        // 儲存基本資料
        const restoredTransaction = {
          id: t.transId,
          transId: t.transId,
          assetId: t.assetId, 
          code: t.assetCode,
          name: t.assetName,
          assetType: frontendAssetType,
          amount: preciseAmount,
          price: costPrice,  // 固定精度
          shares: quantity,
          currentPrice: currentPrice,  // 固定精度
          date: new Date(t.transCreatedAt).toLocaleString(),
        };

        transactionHistory.value.push(restoredTransaction);
        investedAmount.value += preciseAmount;
      });
    } catch (e) {
      console.error('載入交易紀錄失敗:', e);
    }
  }

  // 提交問卷答案
  async function submitAnswers(userId, result) {
    const postData = {
      surveyLevel: result.profileType,
      surveyInitFund: result.investmentAmount,
      surveyScore: result.riskScore,
      surveyAgeGroup: answers.value[1],
      surveyMonthlyCost: Number(answers.value[2]),
      surveyEmergencyFund: Number(answers.value[3]),
      surveyMonthlySaving: Number(answers.value[4]),
      surveyRiskTol: Number(answers.value[5]),
      surveyMarketReaction: Number(answers.value[6]),
      surveyPreferredAsset: answers.value[7],
      surveyInvestExp: Number(answers.value[8]),
      surveySavingGoal: answers.value[9],
      surveySurpriseFund: Number(answers.value[10])
    }

    const token = localStorage.getItem('authToken');
    if (!token) {
          throw new Error('未找到登入憑證，請先登入。')
    }


    if (DEV_MODE) {
      // 開發模式：模擬回應
      console.log("(開發模式) 模擬 API 回應")
      return { status: 201, surveyId: 123 }
    } else {
      // 生產模式：真實 API 呼叫
      try {
        const response = await axios.post('/api/survey', postData, {
          headers: {
            'Content-Type': 'application/json',
            'Authorization': token
          }
        })
        
        return {
          status: response.status,
          surveyId: response.data.surveyId
        }
      } catch (error) {
        console.error("提交問卷失敗:", error)
        
        if (error.response) {
          // 後端有回應但是錯誤
          throw new Error(error.response.data.message || '問卷提交失敗')
        } else if (error.request) {
          // 請求發送了但沒收到回應
          throw new Error('網路錯誤，無法連接伺服器')
        } else {
          // 其他錯誤
          throw new Error('問卷提交失敗：' + error.message)
        }
      }
    }
  }

  //查詢問卷紀錄
  async function loadExistingSurvey() {
    
    const token = localStorage.getItem('authToken');
    if(!token){
      console.warn("未找到 Token，無法載入問卷");
      return null;
    }

    if(DEV_MODE){
      console.log('(開發模式) 模擬載入問卷資料')
      return {
        surveyId: 123,
        surveyLevel: '穩健型',
        surveyInitFund: 100000,
        surveyScore: 50,
        surveyAgeGroup: '26-35歲',
        surveyMonthlyCost: 30000,
        surveyEmergencyFund: 6,
        surveyMonthlySaving: 20000,
        surveyRiskTol: 3,
        surveyMarketReaction: 2,
        surveyPreferredAsset: '股票',
        surveyInvestExp: 2,
        surveySavingGoal: '退休規劃',
        surveySurpriseFund: 3
      }
    }else{
      try{

        const response = await axios.get('api/survey',{
          headers: {
            'Authorization':token
          }
        })

        if(response.status == 200 && response.data.surveyId){
          return response.data;
        }
        return null;
      }catch(error){
         if (error.response?.status === 404) {
          console.log('使用者尚未填寫問卷')
          return null
        }
        
        // 其他錯誤
        console.error('載入問卷失敗:', error)
        return null
      }
    }

  }

  //恢復問卷狀態
  async function restoreSurveyFromBackEnd(surveyData) {
  // 恢復答案
  answers.value = {
    1: surveyData.surveyAgeGroup,
    2: surveyData.surveyMonthlyCost,
    3: surveyData.surveyEmergencyFund,
    4: surveyData.surveyMonthlySaving,
    5: surveyData.surveyRiskTol,
    6: surveyData.surveyMarketReaction,
    7: surveyData.surveyPreferredAsset,
    8: surveyData.surveyInvestExp,
    9: surveyData.surveySavingGoal,
    10: surveyData.surveySurpriseFund,
    11: surveyData.surveyInitFund
  }

  try {
    const token = localStorage.getItem('authToken')
    if (!token) {
      throw new Error('未找到登入憑證')
    }

    if (DEV_MODE) {
      // 開發模式：使用前端計算
      const result = calculateScore()
      result.surveyId = surveyData.surveyId
      setAnalysisResult(result)
    } else {
      // 生產模式：從後端載入
      const response = await axios.get(`/api/suggest?surveyId=${surveyData.surveyId}`, {
        headers: {
          'Authorization': token
        }
      })

      if (response.status === 200 && response.data.length > 0) {
        // 重組 stocks 和 forexs
        const stocks = []
        const forexs = []
        
        response.data.forEach(item => {
          const assetData = {
            assetId: item.assetId,
            code: item.assetCode,
            name: item.assetName,
            type: item.assetType,
            percentage: Number(item.sugPercentage)
          }
          
          if (item.assetType === 'STOCK') {
            stocks.push(assetData)
          } else if (item.assetType === 'FOREX') {
            forexs.push(assetData)
          }
        })

        // 計算 allocation
        const allocation = {
          stock: 0,
          etf: 0,
          forex: 0,
          cash: 0
        }
        
        stocks.forEach(s => {
          allocation.stock += s.percentage
        })
        
        forexs.forEach(f => {
          allocation.forex += f.percentage
        })
        
        allocation.cash = 100 - allocation.stock - allocation.forex

        // 組合完整的 result
        const result = {
          surveyId: surveyData.surveyId,
          profileType: surveyData.surveyLevel,
          riskScore: surveyData.surveyScore,
          investmentAmount: surveyData.surveyInitFund,
          assetPreference: surveyData.surveyPreferredAsset,
          allocation: allocation,
          stocks: stocks,
          forexs: forexs
        }

        setAnalysisResult(result)
      } else {
        // 如果後端沒有資料，使用前端計算
        const result = calculateScore()
        result.surveyId = surveyData.surveyId
        setAnalysisResult(result)
      }
    }
  } catch (error) {
    console.error('載入資產配置建議失敗:', error)
    // 降級處理：使用前端計算
    const result = calculateScore()
    result.surveyId = surveyData.surveyId
    setAnalysisResult(result)
  }

  showResults.value = true
}

  // 提交資產配置建議
  async function submitSuggestions(surveyId, suggestions) {
  
  const simplifiedSuggestions = suggestions.map(item => ({
    assetId: item.assetId,
    sugPercentage: item.percentage  
  }))

  const postData = {
    surveyId: surveyId,
    suggestions: simplifiedSuggestions
  }
  
  if (DEV_MODE) {
    console.log("(開發模式) 模擬提交資產配置建議", postData)
    return { status: 201 }
  }
  
  try {
    const token = localStorage.getItem('authToken')
    if (!token) {
      throw new Error('未找到登入憑證，請先登入。')
    }

    const response = await axios.post('/api/suggest', postData, {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': token
      }
    })
    
    
    return {
      status: response.status,
      insertedCount: response.data.insertedCount
    }
    
  } catch (error) {
    console.error("提交資產配置建議失敗:", error)
    
    if (error.response) {
      throw new Error(error.response.data.message || '資產配置建議儲存失敗')
    } else if (error.request) {
      throw new Error('網路錯誤，無法連接伺服器')
    } else {
      throw new Error('資產配置建議儲存失敗：' + error.message)
    }
  }
}


  // 提交交易紀錄
  async function submitTransactions(surveyId, history) {
    if (history.length === 0) {
      return { status: 200, message: "無交易紀錄" }
    }
    
    const simplifiedTransactions = history.map(item => ({
      assetId: item.assetId,
      amount: item.amount,
      price: item.price,
      shares: item.shares
    }))

    const postData = {
      surveyId: surveyId,
      transactions: simplifiedTransactions
    }
    
    return { status: 201 }
  }

  //重作問卷功能
    const resetSurvey = async () => {
        try {
            
            //拿到token
            const token = localStorage.getItem('authToken')
            if(!token){
              return{
                success:false,
                message:'沒有授權'
              }
            }

            // 呼叫後端 API 刪除所有相關資料
            const response = await axios.delete('/api/survey',{
              headers:{
                'Authorization':token
              }
            })
            
            if (response.status === 200) {
                //清空前端 store 的所有問卷相關資料
                reset() // 呼叫原本的 reset 方法
                
                return {
                    success: true,
                    message: response.data.message || '問卷及相關資料已清除'
                }
            }
            
        } catch (error) {
            console.error('重做問卷失敗:', error)
            
            // 處理不同的錯誤狀況
            if (error.response) {
                const status = error.response.status
                const message = error.response.data?.message || '刪除失敗'
                
                if (status === 404) {
                    // 如果後端說沒有問卷，前端還是要清空
                    reset()
                    return {
                        success: true,
                        message: '尚無問卷資料，已重置為初始狀態'
                    }
                } else if (status === 403) {
                    return {
                        success: false,
                        message: message
                    }
                }
            }
            
            return {
                success: false,
                message: '刪除問卷時發生錯誤，請稍後再試'
            }
        }
    }


  // 整合流程
  async function submitFullSurveyProcess(result, userId) {
    const riskScoreValue = result.riskScore
    if (riskScoreValue === undefined || riskScoreValue === null) {
      console.error("錯誤: result 物件中未包含 'riskScore' 欄位。")
      throw new Error("問卷分數計算錯誤或遺失。")
    }
    
    const surveyResponse = await submitAnswers(userId, result)
    const surveyId = surveyResponse.surveyId
    
    if (analysisResult.value) {
        analysisResult.value.surveyId = surveyId // 將 surveyId 附加到結果物件
    } else {
        // 為了確保 analysisResult 包含 surveyId，這裡需要確保 result 物件被正確地 setAnalysisResult
        setAnalysisResult({ ...result, surveyId: surveyId })
    }

    const suggestions = [...result.stocks, ...result.forexs]
    await submitSuggestions(surveyId, suggestions)
    
    await submitTransactions(surveyId, transactionHistory.value)

    return { status: 200 }
  }
  
  // Return 
  return {
    // State
    answers,
    analysisResult,
    showResults,
    investedAmount,
    transactionHistory,
    isProcessingOrder,
    isModalVisible,
    currentModalAsset,
    modalKey,
    isLoading,
    
    // Getters
    totalInvestmentLimit,
    remainingAmount,
    
    // Actions
    setAnswer,
    setAllAnswers,
    setAnalysisResult,
    addTransaction,
    setProcessingOrder,
    openModal,
    closeModal,
    reset,
    submitFullSurveyProcess,
    createTransaction,
    loadExistingSurvey,
    restoreSurveyFromBackEnd,
    calculateScore,
    loadUserTransactions,
    resetSurvey,

    // 開發工具
    DEV_MODE  // 讓元件可以知道目前的模式
  }
})