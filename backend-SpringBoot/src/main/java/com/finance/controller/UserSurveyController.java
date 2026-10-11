package com.finance.controller;

import com.finance.entity.User;
import com.finance.entity.UserSurvey;
import com.finance.service.UserSurveyService;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/survey")
public class UserSurveyController {

    @Autowired private UserSurveyService userSurveyService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createSurvey(
            @RequestAttribute(value = "validateUser", required = false) User user,
            @RequestBody UserSurvey survey) {

        Map<String, Object> response = new HashMap<>();

        if (user == null) {
            response.put("message", "請先登入，才能提交問卷");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        if (user.isAdmin()) {
            response.put("message", "管理員帳號不需要填寫問卷");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        try {
            survey.setUserId(user.getUserId());

            int surveyId = userSurveyService.survey(survey);

            if (surveyId > 0) {
                response.put("surveyId", surveyId);
                response.put("message", "問卷結果已儲存");
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                response.put("message", "問卷儲存失敗");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

        } catch (IllegalStateException e) {
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("message", "伺服器內部錯誤");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getSurvey(
            @RequestAttribute(value = "validateUser", required = false) User user) {

        Map<String, Object> response = new HashMap<>();

        if (user == null) {
            response.put("message", "請先登入");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        try {
            UserSurvey survey = userSurveyService.findSurveyByUserId(user);

            if (survey != null) {
                response.put("surveyId", survey.getSurveyId());
                response.put("surveyLevel", survey.getSurveyLevel());
                response.put("surveyInitFund", survey.getSurveyInitFund());
                response.put("surveyScore", survey.getSurveyScore());
                response.put("surveyAgeGroup", survey.getSurveyAgeGroup());
                response.put("surveyMonthlyCost", survey.getSurveyMonthlyCost());
                response.put("surveyEmergencyFund", survey.getSurveyEmergencyFund());
                response.put("surveyMonthlySaving", survey.getSurveyMonthlySaving());
                response.put("surveyRiskTol", survey.getSurveyRiskTol());
                response.put("surveyMarketReaction", survey.getSurveyMarketReaction());
                response.put("surveyPreferredAsset", survey.getSurveyPreferredAsset());
                response.put("surveyInvestExp", survey.getSurveyInvestExp());
                response.put("surveySavingGoal", survey.getSurveySavingGoal());
                response.put("surveySurpriseFund", survey.getSurveySurpriseFund());

                return ResponseEntity.status(HttpStatus.OK).body(response);
            } else {
                response.put("message", "尚未填寫問卷");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.put("message", "查詢失敗：伺服器內部錯誤");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping
    public ResponseEntity<Map<String, Object>> deleteSurvey(
            @RequestAttribute(value = "validateUser", required = false) User user) {

        Map<String, Object> response = new HashMap<>();

        if (user == null) {
            response.put("message", "請先登入");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        if (user.isAdmin()) {
            response.put("message", "管理員帳號沒有問卷可刪除");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        try {
            int userId = user.getUserId();
            boolean success = userSurveyService.deleteSurvey(userId);

            if (success) {
                response.put("message", "問卷及相關資料已清除，可以重新填寫");
                return ResponseEntity.status(HttpStatus.OK).body(response);
            } else {
                response.put("message", "沒有找到問卷資料");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

        } catch (RuntimeException e) {
            e.printStackTrace();
            response.put("message", "刪除問卷失敗：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("message", "伺服器內部錯誤");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
