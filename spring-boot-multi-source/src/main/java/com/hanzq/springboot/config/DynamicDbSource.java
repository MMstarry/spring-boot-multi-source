package com.hanzq.springboot.config;

import com.hanzq.springboot.GroupDataSource;
import com.hanzq.springboot.LoadBalanceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
/**
 * 该方法数据源设置需要在AOP之前执行
 * Created by 韩志强(18297397903@163.com) on 2023/5/7
 */
public class DynamicDbSource {

    static Logger logger = LoggerFactory.getLogger(DynamicDbSource.class);

    private final static ThreadLocal<GroupDataSource> dataSource = new ThreadLocal<GroupDataSource>();

    /**
     * 设置连接池
     *
     * @param targetDataSourceId
     */
    public static void set(String targetDataSourceId) {
        logger.info("设置线程Id = " + Thread.currentThread().getId() + " ;目标数据源分组连接池名 = " + targetDataSourceId);
        dataSource.set(new GroupDataSource(targetDataSourceId));
    }

    /**
     * 设置分组连接池
     *
     * @param targetDataSourceGroupName 分组名
     */
    public static void setGroupName(String targetDataSourceGroupName) {
        dataSource.set(new GroupDataSource(targetDataSourceGroupName, null, LoadBalanceType.ROUND_ROBIN));
    }

    /**
     * 设置分组连接池
     *
     * @param targetDataSourceGroupName 分组名
     * @param targetDataSourceId        分组连接池名
     */
    public static void setGroupNamePoolName(String targetDataSourceGroupName, String targetDataSourceId) {
        dataSource.set(new GroupDataSource(targetDataSourceGroupName, targetDataSourceId, LoadBalanceType.ROUND_ROBIN));
    }

    /**
     * 设置分组连接池
     *
     * @param targetDataSourceGroupName 分组名
     * @param targetDataSourceId        分组连接池名
     * @param targetBalanceType         负载均衡类型
     */
    public static void setGroupNamePoolNameBalanceType(String targetDataSourceGroupName, String targetDataSourceId
            , LoadBalanceType targetBalanceType) {
        targetDataSourceId = StringUtils.isEmpty(targetDataSourceId) ? "" : targetDataSourceId;
        logger.info("设置线程Id = " + Thread.currentThread().getId()
                + " ;目标数据源分组名 = " + targetDataSourceGroupName
                + " ;目标数据源分组连接池名 = " + targetDataSourceId
                + " ;目标数据源均衡类型 = " + targetBalanceType);
        dataSource.set(new GroupDataSource(targetDataSourceGroupName, targetDataSourceId, targetBalanceType));
    }

    public static String get() {
        GroupDataSource groupDataSource = dataSource.get();
        return null == groupDataSource ? "" : groupDataSource.getGroupId();
    }

    public static GroupDataSource getGroupDataSource() {
        return dataSource.get();
    }

    public static void remove() {
        logger.info("删除线程Id = " + Thread.currentThread().getId() + " ;目标数据源分组连接池名 = " + dataSource.get().getGroupId()+"\n");
        dataSource.remove();
    }
}
