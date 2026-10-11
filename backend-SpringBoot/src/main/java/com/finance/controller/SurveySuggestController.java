package com.finance.controller;

import com.finance.dto.SurveySuggestRequest;
import com.finance.entity.SurveySuggest;
import com.finance.service.SurveySuggestService;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/suggest")
public class SurveySuggestController {

    @Autowired private SurveySuggestService surveySuggestService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> saveSuggestion(
            @RequestBody SurveySuggestRequest request) {

        Map<String, Object> response = new HashMap<>();

        // 驗證 surveyId
        if (request.getSurveyId() == null || request.getSurveyId() <= 0) {
            response.put("message", "surveyId 無效");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // 驗證 suggestions 是否為空
        if (request.getSuggestions() == null || request.getSuggestions().isEmpty()) {
            response.put("message", "建議清單不能為空");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        for (SurveySuggest suggest : request.getSuggestions()) {

            suggest.setSurveyId(request.getSurveyId());

            // 驗證 assetId
            if (suggest.getAssetId() <= 0) {
                response.put("message", "assetId 無效");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            BigDecimal percentage = suggest.getSugPercentage();

            if (percentage == null) {
                response.put("message", "sugPercentage 不能為空");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            if (percentage.compareTo(BigDecimal.ZERO) < 0
                    || percentage.compareTo(new BigDecimal("100")) > 0) {
                response.put("message", "百分比必須介於 0-100 之間");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        }

        try {
            // 呼叫 Service 儲存
            int insertedCount = surveySuggestService.saveSuggestions(request.getSuggestions());

            if (insertedCount == request.getSuggestions().size() && insertedCount > 0) {
                response.put("insertedCount", insertedCount);
                response.put("surveyId", request.getSurveyId());
                response.put("message", "資產配置建議已成功儲存");
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                response.put("message", "部分資料儲存失敗");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

        } catch (RuntimeException e) {
            e.printStackTrace();
            response.put("message", "儲存失敗：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getSuggestion(@RequestParam Integer surveyId) {

        Map<String, Object> response = new HashMap<>();

        if (surveyId == null || surveyId <= 0) {
            response.put("message", "surveyId 格式錯誤或無效");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        try {
            // 呼叫 Service 查詢（完全不變）
            List<SurveySuggest> suggests = surveySuggestService.getSuggestionsBySurveyId(surveyId);

            if (suggests != null && !suggests.isEmpty()) {
                response.put("suggestions", suggests);
                return ResponseEntity.status(HttpStatus.OK).body(response);
            } else {
                response.put("message", "找不到該問卷ID的資產配置建議");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

        } catch (RuntimeException e) {
            e.printStackTrace();
            response.put("message", "查詢建議失敗：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
