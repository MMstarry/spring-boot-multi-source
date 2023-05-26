package com.hanzq.easy.jdbctemplate.controller;

import com.hanzq.springboot.DataSourceUtils;
import com.hanzq.easy.jdbctemplate.service.UService;
import com.hanzq.springboot.config.DynamicDbSource;

import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



@RestController
@CrossOrigin
@RequestMapping("/u")
public class UController{

    @Resource
    private UService uService;


    /**
     * 获取连接池中的数据源监控
     * @return
     */
    @GetMapping("/getDataSources")
    public String getDataSources() {
        List<String> dataSourcesList=DataSourceUtils.getDataSources();
        System.err.println(dataSourcesList);
        return dataSourcesList.toString();
    }


    /**
     * 获取连接池集合
     * @return
     */
    @GetMapping("/getDataSourcesPoolNames")
    public List<Map<String,Object>> getDataSourcesPoolNames() {
        List<Map<String,Object>> mapList=new ArrayList<>();
        List<String> pollnames= DataSourceUtils.getDataSourcesPoolName();
        for(String poolname:pollnames){
            Map<String ,Object> map=new HashMap<>();
            map.put("value",poolname);
            map.put("label",poolname+"数据源");
            mapList.add(map);
        }
        return mapList;
    }

    @PostMapping("/getDataByPoolName")
    public List<Map<String,Object>> getDataByPoolName(@RequestBody Map map){
        List<Map<String,Object>> mapList=new ArrayList<>();
        DynamicDbSource.set(map.get("poolname").toString());
        mapList=uService.getDataByPoolName(map.get("query").toString());
        return mapList;
    }

    /**
     * 加载外部数据源
     * @return
     */
    @GetMapping("/register")
    public String register() {
        Map<String, Object> datasourceentity = new HashMap();
        datasourceentity.put("driver_class_name", "com.mysql.cj.jdbc.Driver");
        datasourceentity.put("jdbc_url", "jdbc:mysql://192.168.200.75:3306/dataease");
        datasourceentity.put("pool_name", "dataease");
        datasourceentity.put("group_name",  "dataease");
        datasourceentity.put("balance_type", "");
        datasourceentity.put("username", "root");
        datasourceentity.put("password", "Password123@mysql");
        datasourceentity.put("minimum_idle", "5");
        datasourceentity.put("maximum_pool_size", "10");
        datasourceentity.put("connection_test_query", "SELECT 1");
        datasourceentity.put("type", "MySQL");
      //  boolean flag=DataSourceUtils.AddDataSource(datasourceentity);


        System.err.println(DataSourceUtils.validConnection(datasourceentity));
        return "动态加载外部数据源: ";
    }

}
