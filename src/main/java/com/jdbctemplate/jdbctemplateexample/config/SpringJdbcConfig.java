package com.jdbctemplate.jdbctemplateexample.config;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

@Configuration
class SpringJdbcConfig {

    //@Primary
    //@Bean
    //public DataSource dataSource() {
    //
    //    DriverManagerDataSource dataSource = new DriverManagerDataSource();
    //    //dataSource.setDriverClassName("org.postgresql.Driver");
    //    dataSource.setUrl("jdbc:postgresql://ccd-shared-database:5432/cft_task_db");
    //    dataSource.setUsername("ccd");
    //    dataSource.setPassword("ccd");
    //
    //    return dataSource;
    //}

    @Bean
    public DataSource dataSource() {

        //DriverManagerDataSource dataSource = new DriverManagerDataSource();
        //dataSource.setDriverClassName("org.postgresql.Driver");
        BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setUrl("jdbc:postgresql://ccd-shared-database:5432/cft_task_db");
        basicDataSource.setUsername("ccd");
        basicDataSource.setPassword("ccd");
        basicDataSource.setDefaultAutoCommit(false);
        basicDataSource.setAutoCommitOnReturn(false);
        //basicDataSource.setDefaultTransactionIsolation();

        return basicDataSource;
    }

}