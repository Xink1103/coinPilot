package com.finance.repository;

import com.finance.entity.Asset;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AssetDAO {

    @Autowired private JdbcTemplate jdbcTemplate;

    private static final String FIND_BY_ASSETID_WITH_STOCK =
            "SELECT a.assetId, a.assetCode, a.assetName, a.assetType, "
                    + "s.stockIndustryType, s.stockIsEtf FROM asset a "
                    + " LEFT JOIN stock s ON a.assetId = s.assetId "
                    + "WHERE a.assetId = ?";

    private static final String FIND_BY_ASSETCODE_WITH_STOCK =
            "SELECT a.assetId, a.assetCode, a.assetName, a.assetType, "
                    + "s.stockIndustryType, s.stockIsEtf FROM asset a"
                    + " LEFT JOIN stock s ON a.assetId = s.assetId "
                    + "WHERE a.assetCode = ?";

    private static final String FIND_ALL_WITH_STOCK =
            "SELECT a.assetId, a.assetCode, a.assetName, a.assetType, "
                    + "s.stockIndustryType, s.stockIsEtf FROM asset a"
                    + " LEFT JOIN stock s ON a.assetId = s.assetId";

    private static final String SQL = FIND_ALL_WITH_STOCK + " WHERE a.assetType = ?";

    public Asset findByAssetId(int assetId) {

        List<Asset> assets =
                jdbcTemplate.query(
                        FIND_BY_ASSETID_WITH_STOCK,
                        new Object[] {assetId},
                        new BeanPropertyRowMapper<>(Asset.class));
        if (assets.isEmpty()) {
            return null;
        } else {
            return assets.get(0);
        }
    }

    public Asset findByAssetCode(String assetCode) {

        List<Asset> assets =
                jdbcTemplate.query(
                        FIND_BY_ASSETCODE_WITH_STOCK,
                        new Object[] {assetCode},
                        new BeanPropertyRowMapper<>(Asset.class));

        if (assets.isEmpty()) {
            return null;
        } else {
            return assets.get(0);
        }
    }

    public List<Asset> findAll() {

        List<Asset> assets =
                jdbcTemplate.query(FIND_ALL_WITH_STOCK, new BeanPropertyRowMapper<>(Asset.class));
        return assets;
    }

    public List<Asset> findByType(String assetType) {

        List<Asset> assets =
                jdbcTemplate.query(
                        SQL, new Object[] {assetType}, new BeanPropertyRowMapper<>(Asset.class));
        return assets;
    }
}
