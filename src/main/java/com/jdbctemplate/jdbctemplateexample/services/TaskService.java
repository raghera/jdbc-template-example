package com.jdbctemplate.jdbctemplateexample.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.config.Task;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;

@Component
public class TaskService {

    private final static Logger logger = LoggerFactory.getLogger(TaskService.class);

    private final JdbcTemplate jdbcTemplate;

    public TaskService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertTask(String taskId) {

        logger.info("inserting a task: {}", taskId);
        int responseCode =
            jdbcTemplate.update(
                "insert into TASK_SUMMARY(TASK_ID) "
                + "values (?)", taskId);


    }

    public void insertTaskWithNoCommit(String taskId) {

        logger.info("Turning auto-commit off");
        String sql = "insert into TASK_SUMMARY(TASK_ID) values (?)";
        try {
            Connection connection = Objects.requireNonNull(jdbcTemplate.getDataSource())
                .getConnection();
            connection.setAutoCommit(false);
            assert (!connection.getAutoCommit());

            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, taskId);
            int row = stmt.executeUpdate();
//do stuff
            //connection.commit();
        } catch (SQLException e) {
            logger.error("Could not turn autocommit off!!");
            e.printStackTrace();
        }

    }

    public void commit() throws SQLException {
        Connection connection = Objects.requireNonNull(jdbcTemplate.getDataSource())
            .getConnection();
        connection.setAutoCommit(false);
        connection.commit();
    }

    public List<String> getTask(String taskId) {
        logger.info("getting a task: {}", taskId);

        String sql = "SELECT TASK_ID FROM TASK_SUMMARY WHERE TASK_ID = ?";

        final List<String> results = jdbcTemplate.query(sql, new Object[]{taskId},
            (rs, rowNum) -> rs.getString("TASK_ID"));

        logger.info("result size: {}", results.size());

        return results;
    }

}
