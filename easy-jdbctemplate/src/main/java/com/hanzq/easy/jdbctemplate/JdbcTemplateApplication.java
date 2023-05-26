package com.hanzq.easy.jdbctemplate;

import com.hanzq.springboot.annotation.EnableDynamicDataSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;


@SpringBootApplication
@EnableDynamicDataSource
@EnableTransactionManagement(order = 0)
public class JdbcTemplateApplication {

    public static void main(String[] args) {
        SpringApplication.run(JdbcTemplateApplication.class, args);

    }
}
