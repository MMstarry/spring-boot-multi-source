package com.hanzq.springboot;

/**
 * 定义负载均衡类型
 * Created by 韩志强(18297397903@163.com) on 2023/5/7
 */
public enum LoadBalanceType {

    /**
     * 随机
     */
    RANDOM,

    /**
     * 轮询
     */
    ROUND_ROBIN
}
