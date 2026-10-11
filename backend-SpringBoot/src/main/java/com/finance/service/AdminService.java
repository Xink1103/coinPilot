package com.finance.service;

import com.finance.entity.Admin;
import com.finance.entity.Admin.AssetDetail;
import com.finance.entity.Admin.UserDetail;
import com.finance.repository.AdminDAO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

    @Autowired private AdminDAO adminDAO;

    private static final int SCALE = 4; // 計算比例的精度（4 位小數）

    public Admin getAdminDashboardData() {
        Admin admin = new Admin();

        loadGlobalMetrics(admin);

        // 步驟 2：載入並處理用戶列表
        List<UserDetail> userList = loadAndProcessUsers();
        admin.setUserList(userList);

        return admin;
    }

    private void loadGlobalMetrics(Admin admin) {
        try {
            // 總用戶數
            Long totalUsers = adminDAO.getTotalUsers();
            admin.setTotalUsers(totalUsers);

            // 主流風險等級（最多人選擇的）
            String dominantLevel = adminDAO.getDominantRiskLevel();
            admin.setDominantRiskLevel(dominantLevel);

            // 建議資產採納率（已經是百分比）
            Double adoptionRate = adminDAO.getAssetAdoptionRate();
            admin.setAssetAdoptionRate(adoptionRate);

        } catch (Exception e) {
            throw new RuntimeException("載入總體統計數據失敗: " + e.getMessage(), e);
        }
    }

    private List<UserDetail> loadAndProcessUsers() {
        try {
            // 從 DAO 取得用戶列表
            List<UserDetail> userList = adminDAO.getAllUsersInfo();

            // 為每個用戶計算統計數據
            for (UserDetail user : userList) {
                // 計算投資比例
                calculateInvestmentRatio(user);

                // 載入並計算資產配置明細
                calculateAssetAllocations(user);
            }

            return userList;

        } catch (Exception e) {
            throw new RuntimeException("載入用戶列表失敗: " + e.getMessage(), e);
        }
    }

    private void calculateInvestmentRatio(UserDetail user) {
        BigDecimal initFund = user.getSurveyInitFund();
        BigDecimal actualInvested = user.getActualInvestedAmount();

        // 驗證分母有效性
        if (initFund == null || initFund.compareTo(BigDecimal.ZERO) <= 0) {
            user.setInvestmentRatio(0.0);
            return;
        }

        // 驗證分子有效性
        if (actualInvested == null) {
            user.setInvestmentRatio(0.0);
            return;
        }

        try {
            // 計算比例：actualInvested / initFund
            BigDecimal ratio = actualInvested.divide(initFund, SCALE, RoundingMode.HALF_UP);

            // 轉為百分比並保留兩位小數
            double percentage = ratio.doubleValue() * 100;
            user.setInvestmentRatio(Math.round(percentage * 100.0) / 100.0);

        } catch (ArithmeticException e) {
            // 計算錯誤時設為 0
            user.setInvestmentRatio(0.0);
            System.err.println("計算投資比例失敗 (User ID: " + user.getUserId() + "): " + e.getMessage());
        }
    }

    private void calculateAssetAllocations(UserDetail user) {
        try {
            // 從 DAO 取得資產配置列表
            List<AssetDetail> assetList = adminDAO.getAssetAllocationsByUserId(user.getUserId());

            // 取得總投入金額（作為分母）
            BigDecimal totalActualInvestment = user.getActualInvestedAmount();

            // 驗證分母有效性
            if (totalActualInvestment == null
                    || totalActualInvestment.compareTo(BigDecimal.ZERO) <= 0) {
                // 如果總投入為 0，所有資產比例都設為 0
                assetList.forEach(asset -> asset.setActualPercentage(0.0));
                user.setAssetAllocations(assetList);
                return;
            }

            // 計算每個資產的實際配置比例
            for (AssetDetail asset : assetList) {
                BigDecimal actualAmount = asset.getActualAmount();

                if (actualAmount == null) {
                    asset.setActualPercentage(0.0);
                    continue;
                }

                try {
                    // 計算比例：actualAmount / totalActualInvestment
                    BigDecimal percentage =
                            actualAmount.divide(totalActualInvestment, SCALE, RoundingMode.HALF_UP);

                    // 轉為百分比並保留兩位小數
                    double percentageValue = percentage.doubleValue() * 100;
                    asset.setActualPercentage(Math.round(percentageValue * 100.0) / 100.0);

                } catch (ArithmeticException e) {
                    asset.setActualPercentage(0.0);
                }
            }

            // 設定到用戶物件
            user.setAssetAllocations(assetList);

        } catch (Exception e) {
            throw new RuntimeException(
                    "計算資產配置失敗 (User ID: " + user.getUserId() + "): " + e.getMessage(), e);
        }
    }
}
