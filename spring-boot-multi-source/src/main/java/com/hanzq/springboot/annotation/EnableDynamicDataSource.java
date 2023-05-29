package com.hanzq.springboot.annotation;

import com.hanzq.springboot.aspect.DynamicDataSourceAspect;
import com.hanzq.springboot.config.DynamicDataSourceRegister;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 开启动态数据源支持
 * @Target(ElementType.TYPE) 注解作用范围：接口、类、枚举、注解
 * @Retention(RetentionPolicy.RUNTIME) 表示注解的生命周期 运行时也存在
 * @Import一个类
 * 1.该类实现了ImportBeanDefinitionRegistrar接口，在重写的registerBeanDefinitions方法里面，能拿到BeanDefinitionRegistry bd的注册器，能手工往beanDefinitionMap中注册 beanDefinition
 * 2.该类是普通类 spring会将该类加载到spring容器中
 * Created by 韩志强(18297397903@163.com) on 2023/5/7
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({DynamicDataSourceRegister.class, DynamicDataSourceAspect.class})
public @interface EnableDynamicDataSource {
}
