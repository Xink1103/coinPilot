package com.finance.repository;

import com.finance.entity.UserSurvey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class UserSurveyDAO {

    @Autowired private JdbcTemplate jdbcTemplate;

    private static final String INSERT_SURVEY_PSTMT =
            "INSERT INTO usersurvey ("
                    + "userId, surveyAgeGroup, surveyInvestExp, surveyRiskTol, "
                    + "surveySavingGoal, surveyPreferredAsset, surveyMonthlySaving, "
                    + "surveySurpriseFund, surveyMonthlyCost, surveyEmergencyFund, "
                    + "surveyMarketReaction, surveyInitFund, surveyLevel ,surveyScore"
                    + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?)";

    private static final String FIND_BY_USERID_QUERY = "SELECT * FROM usersurvey WHERE userId = ? ";

    private static final String DELETE_BY_USER_ID = "DELETE FROM usersurvey WHERE userId = ?";

    // insert
    public int insert(UserSurvey survey) {

        // auto
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(
                new PreparedStatementCreator() {

                    @Override
                    public PreparedStatement createPreparedStatement(Connection con)
                            throws SQLException {

                        PreparedStatement ps =
                                con.prepareStatement(
                                        INSERT_SURVEY_PSTMT, Statement.RETURN_GENERATED_KEYS);
                        ps.setInt(1, survey.getUserId());
                        ps.setString(2, survey.getSurveyAgeGroup());
                        ps.setInt(3, survey.getSurveyInvestExp());
                        ps.setInt(4, survey.getSurveyRiskTol());
                        ps.setString(5, survey.getSurveySavingGoal());
                        ps.setString(6, survey.getSurveyPreferredAsset());
                        ps.setInt(7, survey.getSurveyMonthlySaving());
                        ps.setInt(8, survey.getSurveySurpriseFund());
                        ps.setInt(9, survey.getSurveyMonthlyCost());
                        ps.setInt(10, survey.getSurveyEmergencyFund());
                        ps.setInt(11, survey.getSurveyMarketReaction());
                        ps.setBigDecimal(12, survey.getSurveyInitFund());
                        ps.setString(13, survey.getSurveyLevel());
                        ps.setInt(14, survey.getSurveyScore());
                        return ps;
                    }
                },
                keyHolder);

        if (keyHolder.getKey() != null) {
            int surveyId = keyHolder.getKey().intValue();
            survey.setSurveyId(surveyId);
            return surveyId;
        }
        return 0;
    }

    // 透過userId查詢問卷
    public UserSurvey findSurveyByUserId(int userId) {

        List<UserSurvey> surveys =
                jdbcTemplate.query(
                        FIND_BY_USERID_QUERY,
                        new Object[] {userId},
                        new BeanPropertyRowMapper<>(UserSurvey.class));

        if (surveys.isEmpty()) {
            return null;
        } else {
            return surveys.get(0);
        }
    }

    // 刪除問卷
    public int deleteByUserId(int userId) {
        return jdbcTemplate.update(DELETE_BY_USER_ID, userId);
    }
}
