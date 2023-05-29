package com.hanzq.springboot;

import com.hanzq.springboot.config.DynamicDataSource;
import com.hanzq.springboot.config.DynamicDataSourceContextHolder;
import com.hanzq.springboot.config.DynamicDataSourceRegister;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by 韩志强(18297397903@163.com) on 2023/5/8
 */
public class DataSourceUtils {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceUtils.class);

    private static  AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext(DynamicDataSourceRegister.class);

    private static  DynamicDataSourceRegister dynamicDataSourceRegister = annotationConfigApplicationContext.getBean(DynamicDataSourceRegister.class);
    
    /**
     * 校验连接的有效性
     * @param dbEntity 数据源配置信息
     * @return
     */
    public static Map validConnection(Map<String, Object> dbEntity) {
        Map<String,Object> reMap=new ConcurrentHashMap<>();
        reMap.put("status",0);
        reMap.put("message","检验成功");
        String type=String.valueOf(dbEntity.get("type"));
        if(!dynamicDataSourceRegister.getSupportDataBases().containsKey(type)){
            logger.info("暂不支持{}!",type);
            reMap.put("status",-1);
            reMap.put("message","暂不支持"+type);
            return reMap;
        }

        try {
            Class.forName(String.valueOf(dbEntity.get("driver_class_name")));
            DriverManager.setLoginTimeout(2);
            DriverManager.getConnection(String.valueOf(dbEntity.get("jdbc_url")), String.valueOf(dbEntity.get("username")), String.valueOf(dbEntity.get("password")));
        }catch (Exception e){
            reMap.put("status",-1);
            reMap.put("message",e.getMessage());
            return reMap;
        }
        return reMap;
    }

    /**
     * 获取连接池名集合
     * @return
     */
    public static List<String> getDataSourcesPoolName(){
        List<String> dataSourcesPoolNameList=new ArrayList<>();
        dynamicDataSourceRegister.getCustomDataSources().forEach((s, dataSource) -> {
            dataSourcesPoolNameList.add(s);
        });

        return dataSourcesPoolNameList;
    }

    /**
     * 获取连接池中的数据源监控
     */
    public static List<String> getDataSources(){
        List<String> dataSourcesList=new ArrayList<>();
        dynamicDataSourceRegister.getCustomDataSources().forEach((s, dataSource) -> {

            try {
                MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
                ObjectName poolName = new ObjectName("com.zaxxer.hikari:type=Pool (" + s + ")");
                HikariPoolMXBean  proxy = JMX.newMXBeanProxy(mBeanServer, poolName, HikariPoolMXBean.class);

                dataSourcesList.add(String.format("数据源= %s 总连接数: {%s} 活动连接: {%s} 空闲连接数: {%s} 线程等待连接: {%s}", dataSource,proxy.getTotalConnections(), proxy.getActiveConnections(), proxy.getIdleConnections(), proxy.getThreadsAwaitingConnection()));
                logger.info("\n数据源= {}\n总连接数: {}\n活动连接: {}\n空闲连接数: {}\n线程等待连接: {}", dataSource,proxy.getTotalConnections(), proxy.getActiveConnections(), proxy.getIdleConnections(), proxy.getThreadsAwaitingConnection());

            }catch (Exception ignored){

            }
        });

        return dataSourcesList;
    }


    /**
     * 运行时卸载数据源
     * @param pool_name
     */
    public static void removeDbDataSource(String pool_name){
        dynamicDataSourceRegister.removeDbDataSource(pool_name);
        DynamicDataSourceContextHolder.dataSourceIds.remove(pool_name);

        DynamicDataSourceContextHolder.dataSourceGroupIds.remove(pool_name);
        dynamicDataSourceRegister.getDefinitionRegistry().removeBeanDefinition("dataSource");

        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(DynamicDataSource.class);
        beanDefinition.setSynthetic(true);
        MutablePropertyValues mpv = beanDefinition.getPropertyValues();
        mpv.addPropertyValue("defaultTargetDataSource", dynamicDataSourceRegister.getDefaultDataSource());
        Map<Object, Object> targetDataSources = new HashMap();
        targetDataSources.put("dataSource", dynamicDataSourceRegister.getDefaultDataSource());
        targetDataSources.putAll(dynamicDataSourceRegister.getCustomDataSources());
        mpv.addPropertyValue("targetDataSources", targetDataSources);

        dynamicDataSourceRegister.getDefinitionRegistry().registerBeanDefinition("dataSource", beanDefinition);
        logger.info("重新加载外部数据源!");
    }

    /**
     * 加载外部数据源
     * @param dataSourceEntity 数据源配置信息
     * @return
     */
    public static boolean AddDataSource(Map<String, Object> dataSourceEntity) {


        boolean flag=dynamicDataSourceRegister.addNewDbDataSource(dataSourceEntity);

        if(!flag){
            logger.error("动态加载外部数据源失败!");
            return false;
        }

        DynamicDataSourceContextHolder.dataSourceIds.add(dataSourceEntity.get("pool_name").toString());
        List<Map<String, Object>> entityList = new ArrayList<>();
        entityList.add(dataSourceEntity);

        DynamicDataSourceContextHolder.dataSourceGroupIds.putAll(dynamicDataSourceRegister.buildGroupDataSource(entityList));
        dynamicDataSourceRegister.getDefinitionRegistry().removeBeanDefinition("dataSource");

        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(DynamicDataSource.class);
        beanDefinition.setSynthetic(true);
        MutablePropertyValues mpv = beanDefinition.getPropertyValues();
        mpv.addPropertyValue("defaultTargetDataSource", dynamicDataSourceRegister.getDefaultDataSource());
        Map<Object, Object> targetDataSources = new HashMap();
        targetDataSources.put("dataSource", dynamicDataSourceRegister.getDefaultDataSource());
        targetDataSources.putAll(dynamicDataSourceRegister.getCustomDataSources());
        mpv.addPropertyValue("targetDataSources", targetDataSources);

        dynamicDataSourceRegister.getDefinitionRegistry().registerBeanDefinition("dataSource", beanDefinition);
        logger.info("动态加载外部数据源!");
        return true;
    }
}
