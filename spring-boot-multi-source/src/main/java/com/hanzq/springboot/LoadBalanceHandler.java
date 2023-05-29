package com.hanzq.springboot;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Created by 韩志强(18297397903@163.com) on 2023/5/7
 */
public class LoadBalanceHandler<T> implements InvocationHandler {

    private final LoadBalance<T> loadBalance;

    public LoadBalanceHandler(LoadBalance<T> loadBalance) {
        this.loadBalance = loadBalance;
    }

    @Override
    public T invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object invoke = method.invoke(this.loadBalance, args);
        return (T) invoke;
    }
}
