package com.finance.service;

import com.finance.entity.User;
import com.finance.entity.UserSurvey;
import com.finance.repository.UserSurveyDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserSurveyService {

    @Autowired private UserSurveyDAO userSurveyDAO;

    // 新增問卷
    public int survey(UserSurvey survey) {

        // 檢查是否已存在
        UserSurvey surveyExist = userSurveyDAO.findSurveyByUserId(survey.getUserId());

        if (surveyExist == null) {
            int surveyId = userSurveyDAO.insert(survey);
            return surveyId;
        } else {
            // 錯誤：問卷已存在，禁止重複 POST
            System.err.println(
                    "使用者 " + survey.getUserId() + " 試圖重複提交問卷，ID: " + surveyExist.getSurveyId());
            throw new IllegalStateException("問卷已存在，請勿重複提交或先執行刪除操作。");
        }
    }

    // 查詢問卷
    public UserSurvey findSurveyByUserId(User user) {

        if (user == null) {
            return null;
        }

        int userId = user.getUserId();
        return userSurveyDAO.findSurveyByUserId(userId);
    }

    // 刪除問卷（重做問卷功能）
    public boolean deleteSurvey(int userId) {
        try {
            int deletedCount = userSurveyDAO.deleteByUserId(userId);
            return deletedCount > 0;
        } catch (RuntimeException e) {
            System.err.println("刪除問卷失敗: " + e.getMessage());
            throw e;
        }
    }
}
