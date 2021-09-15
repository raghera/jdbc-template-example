package com.jdbctemplate.jdbctemplateexample.services;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TaskServiceTest {

    private final static Logger logger = LoggerFactory.getLogger(TaskServiceTest.class);

    @Autowired
    private TaskService taskService;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private DataSource basicDataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clean_db() {
        String sql = "TRUNCATE task_summary";

        try {
            final Connection connection = dataSource.getConnection();
            Statement stmt = connection.createStatement();
            stmt.executeUpdate(sql);

        } catch (SQLException e) {
            logger.error("unable to empty db table");
        }

    }

    @Test
    void create_task_in_postgres() {
        taskService.insertTask("t1");

        final List<String> tasks = taskService.getTask("t1");
        assertThat(tasks).isNotEmpty();
        assertThat(tasks.size()).isEqualTo(1);
        assertThat(tasks.get(0)).isEqualTo("t1");

    }

    @Test
    void create_task_but_dont_commit() {

        //assertThat(anotherDataSource.getConnection().getAutoCommit()).isFalse();

        assertThat(taskService.getTask("t1")).isEmpty();

        taskService.insertTaskWithNoCommit("t1");

        final List<String> tasks = taskService.getTask("t1");
        assertThat(tasks).isEmpty();

    }

    @Test
    void insert_task_and_commit_separately()throws Exception {
        String taskId1 = "taskId1";

        final String sql = "insert into TASK_SUMMARY(TASK_ID) values (?)";

        //assertThat(basicDataSource instanceof BasicDataSource).isTrue();
        assertThat(basicDataSource.getConnection().getAutoCommit()).isFalse();
        assertThat(basicDataSource.getConnection().getTransactionIsolation()).isEqualTo(Connection.TRANSACTION_READ_COMMITTED);

        Connection connection1 = basicDataSource.getConnection();
        assertThat(connection1.getAutoCommit()).isFalse();
        assertThat(connection1.getTransactionIsolation()).isEqualTo(Connection.TRANSACTION_READ_COMMITTED);

        logger.info("connections look good . . .");

        PreparedStatement stmt1 = connection1.prepareStatement(sql);
        stmt1.setString(1, taskId1);
        int row_result = stmt1.executeUpdate();

        assertThat(taskService.getTask(taskId1)).isEmpty();

        assertThat(row_result).isEqualTo(1);
        connection1.commit();

        assertThat(taskService.getTask(taskId1)).isNotEmpty();

    }

    @Test
    void concurrency_test() throws SQLException, ExecutionException, InterruptedException, TimeoutException {

        /*
         * get 2 different connections from the pool - c1, c2
         * c1 inserts but does not commit
         * c2 attempts to insert the same taskId but is unable to obtain the lock on the row
         */

        String taskId1 = "taskId1";

        final String sql = "insert into TASK_SUMMARY(TASK_ID) values (?)";

        //assertThat(basicDataSource instanceof BasicDataSource).isTrue();
        assertThat(basicDataSource.getConnection().getAutoCommit()).isFalse();

        Connection connection1 = basicDataSource.getConnection();
        Connection connection2 = basicDataSource.getConnection();

        assertThat(connection1).isNotEqualTo(connection2);
        assertThat(connection1.getAutoCommit()).isFalse();
        assertThat(connection2.getAutoCommit()).isFalse();

        assertThat(connection1.getTransactionIsolation()).isEqualTo(Connection.TRANSACTION_READ_COMMITTED);
        assertThat(connection2.getTransactionIsolation()).isEqualTo(Connection.TRANSACTION_READ_COMMITTED);

        logger.info("connections look good . . .");

        Callable<Integer> conn1InsertTask = () -> {
            PreparedStatement statement = connection1.prepareStatement(sql);
            statement.setString(1, taskId1);
            logger.info("thread 1 executing sql . . .");
            Integer rows = null;
            try {

                rows = statement.executeUpdate();
                logger.info("thread 1 sql executed . . .");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            //no commit
            return rows;
        };
        Callable<Integer> conn2InsertTask = () -> {
            TimeUnit.MILLISECONDS.sleep(3000);
            PreparedStatement statement = connection2.prepareStatement(sql);
            statement.setString(1, taskId1); //same uniqueId ... should wait for commit and re-evaluate
            logger.info("thread 2 executing sql . . .");
            int rows = statement.executeUpdate();
            logger.info("thread 1 sql executed . . .");
            return rows;
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<Integer> conn1Future = executor.submit(conn1InsertTask);
        logger.info("Thread 1 started");

        Thread.sleep(3000);

        Future<Integer> conn2Future = executor.submit(conn2InsertTask);
        logger.info("Thread 2 started");

        //could gobble
        Integer result1 = conn1Future.get(10L, TimeUnit.SECONDS);
        assertThat(result1).isEqualTo(1);

        Integer result2 = null;
        try {
            result2 = conn2Future.get(5L, TimeUnit.SECONDS);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        assertThat(result2).isEqualTo(0);

        //explicit commit
        logger.info("Committing connection 1");
        connection1.commit();

        logger.info("Committing connection 2");
        connection2.commit();

    }

}