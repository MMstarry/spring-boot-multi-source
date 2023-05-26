package com.hanzq.easy.jdbctemplate.service;

import com.hanzq.easy.jdbctemplate.dao.UDao;
import com.hanzq.springboot.annotation.TargetDataSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Service
public class UService {

    @Resource
    private UDao uDao;

    @TargetDataSource
    @Transactional(rollbackFor = Exception.class)
    public List<Map<String,Object>> getDataByPoolName(String querysql){

        return  uDao.getDataByPoolName(querysql);
    }
}
