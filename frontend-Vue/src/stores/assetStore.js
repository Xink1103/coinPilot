import { defineStore } from 'pinia'
import { computed } from 'vue'
import { usePriceStore } from './usePriceStore'
import { toast } from 'vue3-toastify'
import 'vue3-toastify/dist/index.css'

export const useAssetStore = defineStore('asset', () => {
  
  const priceStore = usePriceStore()
  
  // 可用的股票/ETF 清單
  const availableStocks = computed(() => {
    return priceStore.getAllStocks
  })

  // 可用的外幣清單
  const availableForexs = computed(() => {
    return priceStore.getAllForexs
  })

  //權重配置表 不是寫死的資產表
  const STOCK_WEIGHTS = {
    conservative_low: {
      '006208': 41, '2881': 26, '0050': 14, '2330': 10, 
      '3231': 4, '2454': 3, '2603': 2
    },
    conservative_high: {
      '2881': 42, '006208': 24, '0050': 16, '2330': 9,
      '2454': 5, '3231': 2, '2603': 2
    },
    moderate_low: {
      '0050': 36, '2881': 24, '2330': 21, '006208': 11,
      '2454': 4, '3231': 2, '2603': 2
    },
    moderate_high: {
      '2330': 34, '0050': 26, '2881': 19, '2454': 11,
      '006208': 6, '3231': 2, '2603': 2
    },
    aggressive_low: {
      '3231': 39, '2454': 26, '2603': 16, '2330': 11,
      '0050': 4, '2881': 2, '006208': 2
    },
    aggressive_high: {
      '2454': 31, '2603': 29, '3231': 21, '2330': 12,
      '0050': 3, '2881': 2, '006208': 2
    }
  }

  //權重配置表
  const FOREX_WEIGHTS = {
    conservative_low: { 'JPY': 61, 'USD': 31, 'AUD': 8 },
    conservative_high: { 'JPY': 71, 'USD': 24, 'AUD': 5 },
    moderate_low: { 'JPY': 54, 'USD': 34, 'AUD': 12 },
    moderate_high: { 'USD': 56, 'JPY': 33, 'AUD': 11 },
    aggressive_low: { 'AUD': 72, 'JPY': 19, 'USD': 9 },
    aggressive_high: { 'AUD': 66, 'USD': 23, 'JPY': 11 }
  }

  //決定使用哪個象限的配置
  function getQuadrant(riskScore, isHighFund) {
    let riskLevel = ''
    
    if (riskScore <= 10) {
      riskLevel = 'conservative'
    } else if (riskScore <= 15) {
      riskLevel = 'moderate'
    } else {
      riskLevel = 'aggressive'
    }
    
    const fundLevel = isHighFund ? 'high' : 'low'
    return `${riskLevel}_${fundLevel}`
  }

  //根據風險評分和資金水平獲取股票/ETF 建議
  function getStockRecommendations(riskScore, isHighFund, baseAllocation) {
    const totalStockAlloc = baseAllocation.stock
    
    // 如果沒有可用股票，返回空陣列
    if (availableStocks.value.length === 0) {
      return []
    }

    // 決定使用哪個象限的權重配置
    const quadrant = getQuadrant(riskScore, isHighFund)
    const weights = STOCK_WEIGHTS[quadrant]

    // 資產分配權重
    const weightedStocks = availableStocks.value.map(asset => {
      const weight = weights[asset.assetCode] || 1
      
      return {
        ...asset,
        weight: weight
      }
    })

    // 計算總權重
    const totalWeight = weightedStocks.reduce((sum, asset) => sum + asset.weight, 0)

    // 根據權重分配比例
    const recommendations = weightedStocks.map(asset => {
      // 實際百分比 = 總股票佔比 × (該資產權重 / 總權重)
      const percentage = Math.round(totalStockAlloc * (asset.weight / totalWeight))
      
      return {
        assetId: asset.assetId,
        code: asset.assetCode,
        name: asset.assetName,
        type: asset.assetType,
        percentage: percentage
      }
    })

    // 按百分比降序排列（可選，方便前端顯示）
    return recommendations.sort((a, b) => b.percentage - a.percentage)
  }

  //根據風險評分和資金水平獲取外幣建議
  function getForexRecommendations(riskScore, isHighFund, baseAllocation) {
    const totalForexAlloc = baseAllocation.forex
    
    // 如果沒有可用外幣，返回空陣列
    if (availableForexs.value.length === 0) {
      toast.warn('沒有可用的外幣資料')
      return []
    }

    // 決定使用哪個象限的權重配置
    const quadrant = getQuadrant(riskScore, isHighFund)
    const weights = FOREX_WEIGHTS[quadrant]

    // 給後端動態返回的資產分配權重
    const weightedForexs = availableForexs.value.map(forex => {
      // 如果配置表中有這個資產代碼，使用預設權重
      // 否則給予最小權重
      const weight = weights[forex.assetCode] || 1
      
      return {
        ...forex,
        weight: weight
      }
    })

    // 計算總權重
    const totalWeight = weightedForexs.reduce((sum, forex) => sum + forex.weight, 0)

    // 根據權重分配比例
    const recommendations = weightedForexs.map(forex => {
      // 實際百分比 = 總外幣佔比 × (該資產權重 / 總權重)
      const percentage = Math.round(totalForexAlloc * (forex.weight / totalWeight))
      
      return {
        assetId: forex.assetId,
        code: forex.assetCode,
        name: forex.assetName,
        type: forex.assetType,
        percentage: percentage
      }
    })

    // 按百分比降序排列
    return recommendations.sort((a, b) => b.percentage - a.percentage)
  }

  // 呼叫 priceStore 的 fetchMarketSnapshot 拿到所有的資料
  async function fetchAvailableAssets() {
    try {
      await priceStore.fetchMarketSnapshot()
    } catch (error) {
      console.error('獲取可用資產失敗:', error)
      throw error
    }
  }

  return {
    // State
    availableStocks,
    availableForexs,

    // Actions
    getStockRecommendations,
    getForexRecommendations,
    fetchAvailableAssets
  }
})