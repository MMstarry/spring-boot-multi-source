package com.hanzq.springboot;

import java.lang.reflect.Proxy;
import java.util.List;

/**
 * Created by 韩志强(18297397903@163.com) on 2023/5/7
 */
public class LoadBalanceFactory {

    public static String getLoadBalance(List<String> dataSourceIds, String groupName, LoadBalanceType balanceType) {

        LoadBalance<String> loadBalance;

        switch (balanceType) {
            case RANDOM:
                RandomLoadBalance randomLoadBalance = new RandomLoadBalance(dataSourceIds);
                loadBalance =
                        (LoadBalance<String>) Proxy.newProxyInstance(randomLoadBalance.getClass().getClassLoader()
                                , randomLoadBalance.getClass().getInterfaces(), new LoadBalanceHandler(randomLoadBalance));
                return loadBalance.select();

            default: //默认轮询
                RoundRobinLoadBalance roundRobinLoadBalance = new RoundRobinLoadBalance(dataSourceIds, groupName);
                loadBalance = (LoadBalance<String>) Proxy.newProxyInstance(roundRobinLoadBalance.getClass().getClassLoader()
                        , roundRobinLoadBalance.getClass().getInterfaces(), new LoadBalanceHandler(roundRobinLoadBalance));
                return loadBalance.select();
        }

    }
}
