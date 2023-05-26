package com.hanzq.easy.jdbctemplate.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Repository
public class UDao {

    @Resource
    private JdbcTemplate jdbcTemplate;


    public List<Map<String,Object>> getDataByPoolName(String querysql){
        return jdbcTemplate.queryForList(querysql);
    }


}
