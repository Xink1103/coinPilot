import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import axios from 'axios'

export const usePriceStore = defineStore('price', () => {
    
    //市場快照資料（所有資產 + 價格）
    const marketSnapshot = ref({})

    //最後更新時間
    const lastUpdate = ref(null)

    //載入狀態
    const isLoading = ref(false)

    //------------getter--------------

    //用assetId拿價格
    const getPriceByAssetId = computed(()=>{
        return (assetId) =>{
            return marketSnapshot.value[assetId]?.transCurrentPrice || 0
        }
    })

    //用assetCode拿價格
    const getPriceByAssetCode = computed(()=>{
        return(assetCode)=>{
            const asset = Object.values(marketSnapshot.value).find(item=>item.assetCode == assetCode)
            return asset?.transCurrentPrice || 0
        }
    })

    //用assetId拿完整資訊
    const getAssetById = computed(()=>{
        return (assetId)=>{
            return marketSnapshot.value[assetId] || null
        }
    })

    //取得所有資產資料
    const getAllAssets = computed(()=>{
        return Object.values(marketSnapshot.value)
    })

    //根據類型篩選資產
    const getAssetsByType = computed(()=>{
        return (type)=>{
            return Object.values(marketSnapshot.value).filter(item=>item.assetType == type)
        }
    })

    //取得所有 STOCK 類型
    const getAllStocks = computed(() => {
        return Object.values(marketSnapshot.value)
            .filter(item => item.assetType === 'STOCK')
    })

    //取得所有外幣
    const getAllForexs = computed(() => {
        return Object.values(marketSnapshot.value)
            .filter(item => item.assetType === 'FOREX')
    })

    //從後端取得市場快照
    const fetchMarketSnapshot = async()=>{
        try{

            isLoading.value = true
            const response = await axios.get('api/prices/snapshot')

            //轉換成以 assetId 為 key 的物件，方便快速查詢
            const snapshotMap = {}
            response.data.forEach(item => {
                snapshotMap[item.assetId] = item
            })
            
            marketSnapshot.value = snapshotMap
            lastUpdate.value = new Date().toISOString()
            return marketSnapshot.value

        }catch (error) {
            console.error('取得市場快照失敗:', error)
            
            if (error.response) {
                const status = error.response.status
                if (status === 404) {
                    throw new Error('找不到價格資料端點')
                } else if (status === 500) {
                    throw new Error('伺服器錯誤，請稍後再試')
                }
            }
            
            throw new Error('無法取得市場數據')
        } finally {
            isLoading.value = false
        }
    }

    //強制更新快取
    const refreshCache = async () => {
        try {
         
            await axios.get('/api/prices/refresh')
            await fetchMarketSnapshot()
            
        } catch (error) {
            console.error('更新快取失敗:', error)
            throw error
        }
    }

    //取得快取狀態
    const getCacheStatus = async () => {
        try {
            const response = await axios.get('/api/prices/status')
            return response.data
        } catch (error) {
            console.error('取得快取狀態失敗:', error)
            return null
        }
    }

    return {
        // State
        marketSnapshot,
        lastUpdate,
        isLoading,
        
        // Getters
        getPriceByAssetId,
        getPriceByAssetCode,
        getAssetById,
        getAllAssets,
        getAssetsByType,
        getAllStocks,
        getAllForexs,
        
        // Actions
        fetchMarketSnapshot,
        refreshCache,
        getCacheStatus
    }

})