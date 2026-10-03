package com.finance.repository;

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

import com.finance.entity.Transaction;

@Repository
public class TransactionDAO {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String INSERT_TRANS = "INSERT INTO transaction (userId, surveyId ,assetId, transTradePrice, transQuantity ,transAmount) " +
    "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String FIND_ALL_TRANS = 
    "SELECT t.transId, t.userId, t.surveyId, t.assetId, t.transTradePrice, " +
    "t.transQuantity, t.transCreatedAt, t.transAmount, " +
    "a.assetCode, a.assetName, a.assetType " +
    "FROM transaction t " +
    "INNER JOIN asset a ON t.assetId = a.assetId " +
    "WHERE t.userId = ? " +
    "ORDER BY t.transCreatedAt DESC";

    public int insert(Transaction trans){

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(new PreparedStatementCreator() {

            @Override
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                PreparedStatement ps = con.prepareStatement(INSERT_TRANS,
                Statement.RETURN_GENERATED_KEYS);
                ps.setInt(1, trans.getUserId());
                ps.setInt(2, trans.getSurveyId());
                ps.setInt(3, trans.getAssetId());
                ps.setBigDecimal(4, trans.getTransTradePrice());
                ps.setBigDecimal(5, trans.getTransQuantity());
                ps.setBigDecimal(6, trans.getTransAmount());
                return ps;
            }
        },keyHolder);

        if(keyHolder.getKey()!=null){
            int transId = keyHolder.getKey().intValue();
            trans.setTransId(transId);
            return transId; 
        }
        return 0;
    }

    public List<Transaction> selectAllByUserId(int userId){

        return jdbcTemplate.query(
            FIND_ALL_TRANS,
            new Object[]{userId},
            new BeanPropertyRowMapper<>(Transaction.class)
        );

    }

}
