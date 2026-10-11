package com.finance.repository;

import com.finance.entity.User;
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
public class UserDAO {

    @Autowired private JdbcTemplate jdbcTemplate;

    //	SQL 語句
    private static final String INSERT_USER_SQL =
            "INSERT INTO user (userName, userPassword, userEmail) VALUES (?, ?, ?)";

    private static final String FIND_BY_EMAIL_SQL = "SELECT * FROM user WHERE userEmail = ?";

    private static final String FIND_BY_ID_SQL = "SELECT * FROM user WHERE userId = ?";

    // insert
    public void insert(User user) {

        // auto generate
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(
                new PreparedStatementCreator() {

                    @Override
                    public PreparedStatement createPreparedStatement(Connection con)
                            throws SQLException {
                        PreparedStatement ps =
                                con.prepareStatement(
                                        INSERT_USER_SQL, Statement.RETURN_GENERATED_KEYS);
                        ps.setString(1, user.getUserName());
                        ps.setString(2, user.getUserPassword());
                        ps.setString(3, user.getUserEmail());
                        return ps;
                    }
                },
                keyHolder);

        if (keyHolder.getKey() != null) {
            user.setUserId(keyHolder.getKey().intValue());
        }
    }

    // 查詢單一用戶 email
    public User findUserByEmail(String userEmail) {

        List<User> user =
                jdbcTemplate.query(
                        FIND_BY_EMAIL_SQL,
                        new Object[] {userEmail},
                        new BeanPropertyRowMapper<>(User.class));

        if (user.isEmpty()) {
            return null;
        } else {
            return user.get(0);
        }
    }

    // 查詢單一用戶 id
    public User findUserById(int userId) {

        List<User> user =
                jdbcTemplate.query(
                        FIND_BY_ID_SQL,
                        new Object[] {userId},
                        new BeanPropertyRowMapper<>(User.class));

        if (user.isEmpty()) {
            return null;
        } else {
            return user.get(0);
        }
    }
}
