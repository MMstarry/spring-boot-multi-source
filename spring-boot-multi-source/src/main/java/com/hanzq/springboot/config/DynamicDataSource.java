package com.hanzq.springboot.config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 动态数据源
 * AbstractRoutingDataSource类 在程序运行时通过AOP切面动态切换当前线程绑定的数据源对象
 * Created by 韩志强(18297397903@163.com) on 2023/5/7
 */
public class DynamicDataSource extends AbstractRoutingDataSource {


    /**
     * 获取要使用数据源的key
     * 代码中的determineCurrentLookupKey方法取得一个字符串
     * 该字符串将与配置文件中的相应字符串进行匹配以定位数据源
     * @return
     */
    @Override
    protected Object determineCurrentLookupKey() {

        /**
         * DynamicDataSourceContextHolder代码中使用setDataSourceName
         * 设置当前的数据源，在路由类中使用getDataSource进行获取，
         * 交给AbstractRoutingDataSource进行注入使用
         */
        return DynamicDataSourceContextHolder.getDataSource();
    }
}