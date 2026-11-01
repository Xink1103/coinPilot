package com.finance.service;

import com.finance.entity.SurveySuggest;
import com.finance.repository.SurveySuggestDAO;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SurveySuggestService {

    @Autowired private SurveySuggestDAO surveySuggestDAO;

    // 儲存資產配置建議
    public int saveSuggestions(List<SurveySuggest> suggests) {

        // 輸入檢查 確保不處理空列表
        if (suggests == null || suggests.isEmpty()) {
            log.warn("嘗試儲存空的資產建議列表");
            return 0;
        }

        try {

            return surveySuggestDAO.insert(suggests);

        } catch (RuntimeException e) {
            log.error("儲存資產建議失敗", e);
            throw new RuntimeException("無法儲存資產配置建議。", e);
        }
    }

    public List<SurveySuggest> getSuggestionsBySurveyId(int surveyId) {

        // ID 檢查
        if (surveyId <= 0) {
            log.warn("無效的 surveyId 查詢請求: {}", surveyId);
            return List.of();
        }

        try {
            // 呼叫 DAO
            return surveySuggestDAO.findBySurveyId(surveyId);

        } catch (RuntimeException e) {
            // 異常處理
            log.error("查詢資產建議失敗, surveyId={}", surveyId, e);
            throw new RuntimeException("無法查詢資產配置建議。", e);
        }
    }
}
