package com.hanzq.springboot.config;

import com.hanzq.springboot.LoadBalanceFactory;
import com.hanzq.springboot.LoadBalanceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据源切换工具类
 * Created by 韩志强(18297397903@163.com) on 2023/5/7
 */
public class DynamicDataSourceContextHolder {

    private static final Logger logger = LoggerFactory.getLogger(DynamicDataSourceContextHolder.class);

    /**
     * 当使用ThreadLocal维护变量时，ThreadLocal为每个使用该变量的线程提供独立的变量副本，
     * 所以每一个线程都可以独立地改变自己的副本，而不会影响其它线程所对应的副本。
     */
    private static final ThreadLocal<String> contextHolder = new ThreadLocal();

    public static List<String> dataSourceIds = new ArrayList();

    public static Map<String, List<String>> dataSourceGroupIds = new HashMap();

    /**
     * 通过数据源名称设置当前数据源
     *
     * @param dataSourceName 数据源名称
     */
    public static void setDataSourceName(String dataSourceName) {
        contextHolder.set(dataSourceName);
    }

    /**
     * 设置分组
     *
     * @param dataSourceGroupName
     * @param dataSourceGroupId
     * @param balanceType
     */
    public static void setDataSourceGroup(String dataSourceGroupName, String dataSourceGroupId, LoadBalanceType balanceType) {

        if (StringUtils.isEmpty(dataSourceGroupId)) {

            List<String> dataSourceGroupList = dataSourceGroupIds.get(dataSourceGroupName);
            dataSourceGroupId = LoadBalanceFactory
                    .getLoadBalance(dataSourceGroupList, dataSourceGroupName, balanceType);
            logger.debug("使用数据源分组连接池名 = [{}] ", dataSourceGroupId);
        }

        contextHolder.set(dataSourceGroupId);
    }

    public static String getDataSource() {
        return contextHolder.get();
    }

    public static void clearDataSource() {
        contextHolder.remove();
    }

    /**
     * 判断指定DataSource当前是否存在
     *
     * @param dataSourceId 数据源名
     * @return 验证数据源是否初始化
     */
    public static boolean containsDataSource(String dataSourceId) {
        return dataSourceIds.contains(dataSourceId);
    }

    /**
     * 判断指定Group的DataSource当前是否存在
     *
     * @param groupName 数据源名
     * @return 是否存在分组
     */
    public static boolean containsDataSourceGroup(String groupName) {
        return dataSourceGroupIds.containsKey(groupName);
    }
}
