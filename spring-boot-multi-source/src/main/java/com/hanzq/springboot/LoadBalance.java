package com.hanzq.springboot;

/**
 * Created by 韩志强(18297397903@163.com) on 2023/5/7
 */
public interface LoadBalance<T> {

    /**
     * 获取分组连接
     *
     * @return 分组类型
     */
    T select();
}
