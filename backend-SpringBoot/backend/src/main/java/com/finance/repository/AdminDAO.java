package com.finance.repository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.finance.entity.Admin.AssetDetail;
import com.finance.entity.Admin.UserDetail;

@Repository
public class AdminDAO {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private static final String COUNT_TOTAL_USERS = 
    "SELECT COUNT(*) FROM user WHERE userRole = 'USER'";
    
    private static final String GET_ASSET_ADOPTION_RATE = 
    "SELECT " +
    "  (SELECT COUNT(*) FROM surveysuggest) AS totalSuggestions, " +
    "  COUNT(DISTINCT CONCAT(t.surveyId, '-', t.assetId)) AS adoptedSuggestions " +
    "FROM transaction t " +
    "INNER JOIN surveysuggest ss " +
    "  ON t.assetId = ss.assetId AND t.surveyId = ss.surveyId " +
    "WHERE t.transAmount > 0";
    
    private static final String GET_DOMINANT_RISK_LEVEL = 
    "SELECT surveyLevel " +
    "FROM usersurvey " +
    "WHERE surveyLevel IS NOT NULL " +
    "GROUP BY surveyLevel " +
    "ORDER BY COUNT(*) DESC " +
    "LIMIT 1";
    
    
    private static final String GET_ALL_USERS_INFO = 
    "SELECT " +
    "  u.userId, " +
    "  u.userName, " +
    "  u.userEmail, " +
    "  us.surveyId, " +
    "  us.surveyLevel, " +
    "  us.surveyInitFund, " +
    "  us.surveyRiskTol, " +
    "  us.surveySavingGoal, " +
    "  us.surveyInvestExp, " +
    "  us.surveyAgeGroup, " +
    "  COALESCE(SUM(t.transAmount), 0) AS actualInvestedAmount " +
    "FROM user u " +
    "LEFT JOIN usersurvey us ON u.userId = us.userId " +
    "LEFT JOIN transaction t " +
    "  ON u.userId = t.userId AND us.surveyId = t.surveyId " +
    "WHERE u.userRole = 'USER' " +
    "GROUP BY " +
    "  u.userId, u.userName, u.userEmail, " +
    "  us.surveyId, us.surveyLevel, us.surveyInitFund, " +
    "  us.surveyRiskTol, us.surveySavingGoal, " +
    "  us.surveyInvestExp, us.surveyAgeGroup " +
    "ORDER BY u.userId";
    
    
    private static final String GET_USER_ASSET_ALLOCATIONS = 
    "SELECT " +
    "  a.assetId, " +
    "  a.assetName, " +
    "  a.assetCode, " +
    "  a.assetType, " +
    "  SUM(t.transAmount) AS actualAmount " +
    "FROM transaction t " +
    "INNER JOIN asset a ON t.assetId = a.assetId " +
    "WHERE t.userId = ? " +
    "GROUP BY a.assetId, a.assetName, a.assetCode, a.assetType " +
    "HAVING actualAmount > 0 " +
    "ORDER BY actualAmount DESC";
   
    public Long getTotalUsers() {
        try {
            return jdbcTemplate.queryForObject(COUNT_TOTAL_USERS, Long.class);
        } catch (Exception e) {
            throw new RuntimeException("取得總用戶數失敗: " + e.getMessage(), e);
        }
    }
    
    public Double getAssetAdoptionRate() {
        try {
            return jdbcTemplate.queryForObject(GET_ASSET_ADOPTION_RATE, (rs, rowNum) -> {
                int adopted = rs.getInt("adoptedSuggestions");
                int total = rs.getInt("totalSuggestions");
                
                // 防止除以零
                if (total == 0) {
                    return 0.0;
                }
                
                // 計算百分比並保留兩位小數
                double rate = ((double) adopted / total) * 100;
                return Math.round(rate * 100.0) / 100.0;
            });
        } catch (Exception e) {
            throw new RuntimeException("取得採納率失敗: " + e.getMessage(), e);
        }
    }
 
    public String getDominantRiskLevel() {
        try {
            List<String> results = jdbcTemplate.query(
                GET_DOMINANT_RISK_LEVEL, 
                (rs, rowNum) -> rs.getString("surveyLevel")
            );
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            throw new RuntimeException("取得主流風險等級失敗: " + e.getMessage(), e);
        }
    }
   
    public List<UserDetail> getAllUsersInfo() {
        try {
            // 使用 BeanPropertyRowMapper 自動映射大部分欄位
            List<UserDetail> userList = jdbcTemplate.query(
                GET_ALL_USERS_INFO,
                new BeanPropertyRowMapper<>(UserDetail.class)
            );
            
            // 手動補充 totalInvestment（等於 surveyInitFund）
            userList.forEach(user -> {
                user.setTotalInvestment(user.getSurveyInitFund());
            });
            
            return userList;
            
        } catch (Exception e) {
            throw new RuntimeException("取得用戶列表失敗: " + e.getMessage(), e);
        }
    }
    
    public List<AssetDetail> getAssetAllocationsByUserId(Integer userId) {
        try {
            // 使用 BeanPropertyRowMapper 自動映射
            List<AssetDetail> assetList = jdbcTemplate.query(
                GET_USER_ASSET_ALLOCATIONS,
                new BeanPropertyRowMapper<>(AssetDetail.class),
                userId
            );
            
            // 手動補充 SQL 中沒有的欄位
            assetList.forEach(asset -> {
                asset.setSugPercentage(null);      // SQL 中沒有此欄位
                asset.setActualPercentage(null);   // 由 Service 層計算
            });
            
            return assetList;
            
        } catch (Exception e) {
            throw new RuntimeException("取得用戶資產配置失敗: " + e.getMessage(), e);
        }
    }
}