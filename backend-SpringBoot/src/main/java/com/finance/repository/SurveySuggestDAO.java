package com.finance.repository;

import com.finance.entity.SurveySuggest;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class SurveySuggestDAO {

    @Autowired private JdbcTemplate jdbcTemplate;

    private static final String INSERT_STMT =
            "INSERT INTO surveysuggest (surveyId, assetId, sugPercentage) VALUES (?, ?, ?)";

    private static final String SELECT_BY_SURVEYID_STMT =
            "SELECT s.sugId, s.surveyId, s.assetId, s.sugPercentage, "
                    + "a.assetCode, a.assetName, a.assetType "
                    + // 從 asset 表 JOIN 過來的欄位
                    "FROM surveysuggest s "
                    + "INNER JOIN asset a ON s.assetId = a.assetId "
                    + "WHERE s.surveyId = ? "
                    + // 根據問卷 ID 查詢
                    "ORDER BY s.sugId";

    @Transactional
    public int insert(List<SurveySuggest> suggests) {

        if (suggests == null || suggests.isEmpty()) {
            return 0;
        }
        int totalResult = 0;

        for (SurveySuggest suggest : suggests) {

            int result =
                    jdbcTemplate.update(
                            INSERT_STMT,
                            suggest.getSurveyId(),
                            suggest.getAssetId(),
                            suggest.getSugPercentage());
            totalResult = totalResult + result;
        }
        return totalResult;
    }

    public List<SurveySuggest> findBySurveyId(int surveyId) {

        return jdbcTemplate.query(
                SELECT_BY_SURVEYID_STMT,
                new Object[] {surveyId},
                new BeanPropertyRowMapper<>(SurveySuggest.class));
    }
}
