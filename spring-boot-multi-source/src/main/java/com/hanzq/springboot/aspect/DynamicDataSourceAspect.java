package com.hanzq.springboot.aspect;

import com.hanzq.springboot.LoadBalanceType;
import com.hanzq.springboot.annotation.TargetDataSource;
import com.hanzq.springboot.config.DynamicDataSourceContextHolder;
import com.hanzq.springboot.config.DynamicDbSource;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 切换数据源Advice
 * @Order标记定义了组件的加载顺序，值越小拥有越高的优先级，可为负数,保证该AOP在@Transactional之前执行
 * @Component定义Spring管理Bean
 * @Aspect注解不能被Spring自动识别并注册为Bean，必须通过@Component注解来完成
 * Created by 韩志强(18297397903@163.com) on 2023/5/7
 */
@Aspect
//@Order(-1)
@Component
public class DynamicDataSourceAspect implements Ordered {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * @Before：在方法执行之前进行执行;
     * @annotation(targetDataSource)：会拦截注解targetDataSource的方法，否则不拦截
     * @param point
     * @param targetDataSource 通过注解指定当前数据源
     */
    @Before("@annotation(targetDataSource)")
    public void changeDataSource(JoinPoint point, TargetDataSource targetDataSource) {

        //连接池名
        String dsName = null;
        //连接池分组名
        String groupName = null;
        //负载均衡类型
        LoadBalanceType balanceType = null;

        //使用了 TargetDataSource注解
        if (null != targetDataSource) {
            dsName = targetDataSource.value();

            //注解未分组配置
            if (StringUtils.isEmpty(dsName)) {
                dsName = targetDataSource.poolName();
            }

            //注解未分组配置
            if (StringUtils.isEmpty(dsName)) {
                dsName = targetDataSource.poolName();
                groupName = targetDataSource.groupName();
                balanceType = targetDataSource.balanceType();
            }
        }

        // 未分组配置
        if (StringUtils.isEmpty(dsName) && StringUtils.isEmpty(groupName)) {
            //TargetDataSource注解上未指定具体的数据源，获取方法内通过DynamicDbSource.set("连接池名")指定的数据源
            dsName = DynamicDbSource.get();

            //连接池分组配置
            if (StringUtils.isEmpty(dsName) && null != DynamicDbSource.getGroupDataSource()) {
                dsName = DynamicDbSource.getGroupDataSource().getGroupId();
                groupName = DynamicDbSource.getGroupDataSource().getGroupName();
                balanceType = DynamicDbSource.getGroupDataSource().getBalanceType();
            }
        }

        //验证数据源是否存在
        if (DynamicDataSourceContextHolder.containsDataSource(dsName)) {

            logger.debug("使用数据源名称 = [{}] , signature = [{}]", dsName, point.getSignature());
            DynamicDataSourceContextHolder.setDataSourceName(dsName);
        } else if (DynamicDataSourceContextHolder.containsDataSourceGroup(groupName)) {

            logger.debug("使用数据源名称 = [{}] , signature = [{}]", dsName, point.getSignature());
            DynamicDataSourceContextHolder.setDataSourceGroup(groupName, dsName, balanceType);
        } else {

            logger.error("数据源 [{}] 不存在, 使用默认数据源 [{}]", dsName, point.getSignature());
        }
    }

    /**
     * @After在方法执行最后进行执行;
     * @annotation(targetDataSource)：会拦截注解targetDataSource的方法，否则不拦截
     * @param point
     * @param targetDataSource
     */
    @After("@annotation(targetDataSource)")
    public void restoreDataSource(JoinPoint point, TargetDataSource targetDataSource) {

        //连接池名
        String dsName = null;
        //使用了 TargetDataSource注解
        if (null != targetDataSource) {
            dsName = targetDataSource.value();

            //注解未分组配置
            if (StringUtils.isEmpty(dsName)) {
                dsName = targetDataSource.poolName();
            }
        }

        // 连接池未分组配置
        if (StringUtils.isEmpty(dsName)) {
            dsName = DynamicDbSource.get();

            //连接池分组配置
            if (StringUtils.isEmpty(dsName) && null != DynamicDbSource.getGroupDataSource()) {
                dsName = DynamicDbSource.getGroupDataSource().getGroupId();
            }
        }

        if (!StringUtils.isEmpty(dsName)) {
            logger.debug("还原数据源 = [{}] ,  signature = [{}]", dsName, point.getSignature());
            //销毁当前数据源信息，进行垃圾回收
            DynamicDataSourceContextHolder.clearDataSource();
            //删除线程Id
            DynamicDbSource.remove();
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
