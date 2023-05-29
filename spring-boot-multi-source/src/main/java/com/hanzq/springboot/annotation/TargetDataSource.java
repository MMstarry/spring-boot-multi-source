package com.hanzq.springboot.annotation;

import com.hanzq.springboot.LoadBalanceType;

import java.lang.annotation.*;

/**
 * 切换数据源注解
 * 在方法上使用，用于指定使用哪个数据源
 * @Target(ElementType.TYPE) 注解作用范围：方法
 * @Retention(RetentionPolicy.RUNTIME) 表示注解的生命周期 运行时也存在
 * Created by 韩志强(18297397903@163.com) on 2023/5/7
 *
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TargetDataSource {

    /**
     * 默认dataSource名称
     * 如果是分组,该字段也是分组中对应的连接池名称
     *
     * @return 数据源名
     */
    String value() default "";

    /**
     * 组名称
     * 指定使用哪个分组
     * PS: 使用分组策略中,该字段必传。切不可重复,重复分组名只去第一个
     *
     * @return 连接池分组名称
     */
    String groupName() default "";

    /**
     * 分组连接-连接池名
     * 指定使用分组中的那个链接
     *
     * @return 连接池名
     */
    String poolName() default "";

    /**
     * 指定使用负载均衡策略
     * PS: 默认使用轮询策略
     *
     * @return 负责均衡类型
     */
    LoadBalanceType balanceType() default LoadBalanceType.ROUND_ROBIN;

}
