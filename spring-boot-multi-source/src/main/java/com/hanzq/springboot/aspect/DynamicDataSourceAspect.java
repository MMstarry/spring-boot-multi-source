package com.hanzq.springboot.aspect;

import com.hanzq.springboot.DataSourceUtils;
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

        
        String dsName = null;
        
        String groupName = null;
        
        LoadBalanceType balanceType = null;

        
        if (null != targetDataSource) {
            dsName = targetDataSource.value();

            
            if (StringUtils.isEmpty(dsName)) {
                dsName = targetDataSource.poolName();
            }

            
            if (StringUtils.isEmpty(dsName)) {
                dsName = targetDataSource.poolName();
                groupName = targetDataSource.groupName();
                balanceType = targetDataSource.balanceType();
            }
        }

        
        if (StringUtils.isEmpty(dsName) && StringUtils.isEmpty(groupName)) {
            
            dsName = DynamicDbSource.get();

            
            if (StringUtils.isEmpty(dsName) && null != DynamicDbSource.getGroupDataSource()) {
                dsName = DynamicDbSource.getGroupDataSource().getGroupId();
                groupName = DynamicDbSource.getGroupDataSource().getGroupName();
                balanceType = DynamicDbSource.getGroupDataSource().getBalanceType();
            }
        }

        
        if (DynamicDataSourceContextHolder.containsDataSource(dsName)) {

            logger.debug("使用数据源名称 = [{}] , signature = [{}]", dsName, point.getSignature());
            DynamicDataSourceContextHolder.setDataSourceName(dsName);
        } else if (DynamicDataSourceContextHolder.containsDataSourceGroup(groupName)) {

            logger.debug("使用数据源名称 = [{}] , signature = [{}]", dsName, point.getSignature());
            DynamicDataSourceContextHolder.setDataSourceGroup(groupName, dsName, balanceType);
        } else {
            logger.debug("数据源 [{}] 重新连接", dsName);
            try {
                
                DataSourceUtils.runLoadDataSource(dsName);
                DynamicDataSourceContextHolder.setDataSourceName(dsName);

            }catch (Exception ignored){

            }
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

        
        String dsName = null;
        
        if (null != targetDataSource) {
            dsName = targetDataSource.value();

            
            if (StringUtils.isEmpty(dsName)) {
                dsName = targetDataSource.poolName();
            }
        }

        
        if (StringUtils.isEmpty(dsName)) {
            dsName = DynamicDbSource.get();

            
            if (StringUtils.isEmpty(dsName) && null != DynamicDbSource.getGroupDataSource()) {
                dsName = DynamicDbSource.getGroupDataSource().getGroupId();
            }
        }

        if (!StringUtils.isEmpty(dsName)) {
            logger.debug("还原数据源 = [{}] ,  signature = [{}]", dsName, point.getSignature());
            
            DynamicDataSourceContextHolder.clearDataSource();
            
            DynamicDbSource.remove();
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
