package com.hanzq.springboot.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;


/**
 * 动态数据源注册
 * Created by 韩志强(18297397903@163.com) on 2023/5/7
 */
public class DynamicDataSourceRegister implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private static final Logger logger = LoggerFactory.getLogger(DynamicDataSourceRegister.class);

    // 默认数据源（主数据源）
    private static DataSource defaultDataSource;

    private static String dbTableName;

    private static long mongoDBMaxLifetime=1800000;

    private static String defaultIp="";

    private static String defaultHostNameMatch="";

    public static BeanDefinitionRegistry definitionRegistry;

    //代理数据源
    public static Map<String, DataSource> customDataSources = new ConcurrentHashMap<>();

    //代理数据源 分组
    private static Map<String, List<String>> customDataSourcesGroup = new ConcurrentHashMap<>();

    public  DataSource getDefaultDataSource(){
        return defaultDataSource;
    }

    public static Map<String,String> supportDataBases=new ConcurrentHashMap<>();

    public  Map<String, DataSource> getCustomDataSources(){
        return customDataSources;
    }

    public String getDbTableName(){
        return dbTableName;
    }

    public  Map<String, List<String>> getCustomDataSourcesGroup(){
        return customDataSourcesGroup;
    }

    public  Map<String,String> getSupportDataBases(){
        return supportDataBases;
    }

    public BeanDefinitionRegistry getDefinitionRegistry(){
        return definitionRegistry;
    }

    /**
     * 加载多数据源配置
     *
     * @param env
     */
    @Override
    public void setEnvironment(@NonNull Environment env) {
        loadJar(env);
        initSupportDataBases();
        initDefaultDataSource(env);
        initCustomDbDataSources(env);
        initCustomDataSources(env);
    }

    /**
     * 启动时动态加载外部目录下的jar包
     * spring.datasource.db.open=true
     * @param env
     */
    public void loadJar(Environment env)  {
        if (Boolean.parseBoolean(env.getProperty("loadJar.open", "false"))) {
            try {
                String property = env.getProperty("loadJar.basePath");
                File libDir = null;
                if (property != null) {
                    libDir = new File(property);
                }else {
                    return;
                }
                URLClassLoader classLoader = (URLClassLoader) Thread.currentThread().getContextClassLoader();
                Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                method.setAccessible(true);

                File[] jarFiles = libDir.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.endsWith(".jar"); }
                });
                if (jarFiles != null) {
                    for (File jarFile : jarFiles) {
                        method.invoke(classLoader, jarFile.toURI().toURL());
                    }
                }
                logger.info("*** 加载外部第三方jar包成功! ***");
            }catch (Exception ignored){
                logger.error("*** 加载外部第三方jar包失败! ***");
            }
        }

    }


    /**
     * 初始化主数据源
     */
    private void initDefaultDataSource(Environment env) {
        // 读取主数据源
        DataSource dataSource = buildDataSource("", env);
        if (null == dataSource) return;
        logger.info("*** 启动时创建默认数据源成功! ***");
        printDbTable(dataSource, "default",env);
        defaultDataSource = dataSource;
    }

    /**
     * 初始化更多数据源-读取配置文件
     */
    private void initCustomDataSources(Environment environment) {
        String dsPrefixes = environment.getProperty(Contains.getDsNameKey());
        //添加分组数据
        customDataSourcesGroup.putAll(buildGroupDataSource(environment));
        if (!StringUtils.isEmpty(dsPrefixes)) {
            Arrays.stream(dsPrefixes.split(",")).forEach(dsPrefix -> {
                DataSource dataSource = buildDataSource(dsPrefix, environment);
                if (null == dataSource) return;
                logger.info("*** 启动时创建数据源 {} 成功! ***", dsPrefix);
                printDbTable(dataSource,dsPrefix,environment);
                customDataSources.put(dsPrefix, dataSource);
            });
        }
    }

    /**
     * 初始化更多数据源-读取数据库
     * spring.datasource.db.open=true
     */
    private void initCustomDbDataSources(Environment environment) {
        if (environment.getProperty(Contains.getDbOpen(), Boolean.class) != null) {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(defaultDataSource);
            String dbName = environment.getProperty(Contains.getDbTableName()) == null ? "db" : environment.getProperty(Contains.getDbTableName());
            dbTableName=dbName;
            List<Map<String, Object>> entityList = queryDbEntityList(jdbcTemplate, dbName);
            List<Map<String, Object>> entitySuccessList=new ArrayList<>();
            if(Boolean.parseBoolean(environment.getProperty(Contains.getDbSync(), "false"))){
                ExecutorService executorService = Executors.newFixedThreadPool(10);
                CountDownLatch latch = new CountDownLatch(entityList.size());
                for (int i = 0; i < entityList.size(); i++) {
                    int index = i;
                    Map<String, Object> dbEntity = entityList.get(i);
                    executorService.submit(() -> {
                        try {
                            HikariConfig hikariConfig = buildHikariConfig(dbEntity);
                            DataSource dataSource = new HikariDataSource(hikariConfig);
                            logger.info("*** 启动时创建数据源 {} 成功! ***", dbEntity.get("pool_name"));
                            printDbTable(dataSource,dbEntity.get("pool_name").toString(),environment);
                            customDataSources.put(String.valueOf(dbEntity.get("pool_name")), dataSource);

                            entitySuccessList.add(entityList.get(index));
                        }catch (Exception e){
                            logger.error("*** 启动时创建数据源 {} 失败! ***", dbEntity.get("pool_name"));
                        }
                        latch.countDown();
                    });
                }
                logger.info("等待异步数据源加载任务完成...");
                try {
                    latch.await();
                    //添加分组数据
                    customDataSourcesGroup.putAll(buildGroupDataSource(entitySuccessList));
                    logger.info("所有异步数据源加载任务完成！");
                    executorService.shutdown();
                }catch (Exception ignored){
                    logger.error("所有异步数据源加载任务失败！");
                }
            }else {
                for (Map<String, Object> dbEntity : entityList) {
                    try {
                        HikariConfig hikariConfig = buildHikariConfig(dbEntity);
                        DataSource dataSource = new HikariDataSource(hikariConfig);
                        logger.info("*** 启动时创建数据源 {} 成功! ***", dbEntity.get("pool_name"));
                        printDbTable(dataSource, dbEntity.get("pool_name").toString(), environment);
                        customDataSources.put(String.valueOf(dbEntity.get("pool_name")), dataSource);

                        entitySuccessList.add(dbEntity);
                    } catch (Exception e) {
                        logger.error("*** 启动时创建数据源 {} 失败! ***", dbEntity.get("pool_name"));
                    }
                }
                customDataSourcesGroup.putAll(buildGroupDataSource(entitySuccessList));
            }

        }
    }

    /**
     * 项目运行时 动态加载外部数据源
     * @param dbEntity
     */
    public boolean addNewDbDataSource(Map<String, Object> dbEntity){

        String type= (String) dbEntity.get("type");
        if(!supportDataBases.containsKey(type)){
            return false;
        }
        List<Map<String, Object>> entityList=new ArrayList<>();
        entityList.add(dbEntity);

        HikariConfig hikariConfig = buildHikariConfig(dbEntity);
        DataSource dataSource;
        try {
            dataSource= new HikariDataSource(hikariConfig);

        }catch (Exception e){
            return false;
        }
        customDataSourcesGroup.putAll(buildGroupDataSource(entityList));
        logger.info("*** 程序运行中创建数据源 {} 成功! ***", dbEntity.get("pool_name"));
        customDataSources.put(String.valueOf(dbEntity.get("pool_name")), dataSource);
        return true;
    }

    /**
     * 运行时卸载数据源
     * @param pool_name
     */
    public void removeDbDataSource(String pool_name){
        customDataSourcesGroup.remove(pool_name);
        customDataSources.remove(pool_name);
    }

    public HikariConfig buildHikariConfig(Map<String, Object> dbEntity) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName(String.valueOf(dbEntity.get("driver_class_name")));
        hikariConfig.setJdbcUrl(genJdbcUrl(String.valueOf(dbEntity.get("jdbc_url")),String.valueOf(dbEntity.get("extra_params")),String.valueOf(dbEntity.get("type")),String.valueOf(dbEntity.get("pool_name"))));
        hikariConfig.setPoolName(String.valueOf(dbEntity.get("pool_name")));
        hikariConfig.setUsername(String.valueOf(dbEntity.get("username")));
        hikariConfig.setPassword(String.valueOf(dbEntity.get("password")));
        hikariConfig.setMinimumIdle(Integer.parseInt(String.valueOf(dbEntity.get("minimum_idle"))));
        hikariConfig.setMaximumPoolSize(Integer.parseInt(String.valueOf(dbEntity.get("maximum_pool_size"))));
        hikariConfig.setConnectionTestQuery(String.valueOf(dbEntity.get("connection_test_query")));
        // hikariConfig.setRegisterMbeans(true);

        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setValidationTimeout(5000);
        //连接泄露检测的最大时间，默认是 0，0表示的是不开启泄露检测,开启后慢sql会引发cpu飙升
        //hikariConfig.setLeakDetectionThreshold(15000);
        hikariConfig.setIdleTimeout(600000);
        if("MongoDB".equals(String.valueOf(dbEntity.get("type")))){
            hikariConfig.setMaximumPoolSize(Integer.parseInt(String.valueOf(dbEntity.get("minimum_idle"))));
            hikariConfig.setMaxLifetime(mongoDBMaxLifetime);
        }else {
            hikariConfig.setMaxLifetime(1800000);
        }
        return hikariConfig;
    }

   public String genJdbcUrl(String jdbcUrl,String extraParams,String type,String poolName){
        String jdbc_url="";
        if(extraParams==null|| extraParams.isEmpty() || "null".equals(extraParams)){
            if("MongoDB".equals(type)){
                jdbc_url=jdbcUrl+"?"+"rebuildschema=true&authSource=admin&schema="+poolName+".xml";
            }else {
                jdbc_url= jdbcUrl;
            }
        }else {
            if("MongoDB".equals(type)) {
                jdbc_url= jdbcUrl+"?"+extraParams+"&schema="+poolName+".xml";
            }else {
                jdbc_url= jdbcUrl+"?"+extraParams;
            }
        }
        if(!defaultIp.isEmpty()){
            jdbc_url=covertIpJdbcUrl(defaultHostNameMatch,jdbc_url,defaultIp);
        }
        return jdbc_url;
    }

    private String covertIpJdbcUrl(String hostNameMatch,String jdbcUrl,String ip){
        String covertIpJdbcUrl=jdbcUrl;
        String[] matches = hostNameMatch.split(",");
        for(String start:matches){
            String end = ":";
            int startIndex = jdbcUrl.indexOf(start);
            int endIndex = jdbcUrl.indexOf(end, startIndex + start.length());
            if (startIndex != -1 && endIndex != -1) {
                String hostName = start+jdbcUrl.substring(startIndex + start.length(), endIndex);
                covertIpJdbcUrl=jdbcUrl.replace(hostName,ip);
                break;
            }
        }
        return covertIpJdbcUrl;
    }

    /**
     * 获取数据库配置字段
     *
     * @param jdbcTemplate 数据库连接
     * @param dbName       存放数据源的表明
     * @return java.util.List<java.util.Map < java.lang.String, java.lang.Object>>
     */
    public List<Map<String, Object>> queryDbEntityList(JdbcTemplate jdbcTemplate, String dbName) {
        List<Map<String, Object>> result = new ArrayList<>();
        jdbcTemplate.query("SELECT " +
                        "driver_class_name, " +
                        "jdbc_url, " +
                        "pool_name, " +
                        "group_name, " +
                        "balance_type, " +
                        "username, " +
                        "password, " +
                        "minimum_idle, " +
                        "type, " +
                        "maximum_pool_size, " +
                        "connection_test_query, " +
                        "extra_params " +
                        "FROM " + dbName +" where enable=1"
                , rs -> {
                    while (rs.next()) {
                        Map<String, Object> item = new HashMap<>();
                        String type= rs.getString("type");
                        if(!supportDataBases.containsKey(type)){
                            logger.info("暂不支持{}!",type);
                            continue;
                        }
                        item.put("type", rs.getString("type"));
                        item.put("driver_class_name", rs.getString("driver_class_name"));
                        item.put("jdbc_url", rs.getString("jdbc_url"));
                        item.put("pool_name", rs.getString("pool_name"));
                        item.put("group_name", rs.getString("group_name"));
                        item.put("balance_type", rs.getString("balance_type"));
                        item.put("username", rs.getString("username")==null?"":rs.getString("username"));
                        item.put("password", rs.getString("password")==null?"":rs.getString("password"));
                        item.put("minimum_idle", rs.getString("minimum_idle"));
                        item.put("maximum_pool_size", rs.getString("maximum_pool_size"));
                        item.put("connection_test_query", rs.getString("connection_test_query"));
                        item.put("extra_params", rs.getString("extra_params"));
                        result.add(item);
                    }
                    return result;
                });
        return result;
    }

    @Override
    public void registerBeanDefinitions(@NonNull AnnotationMetadata importingClassMetadata, @NonNull BeanDefinitionRegistry registry) {
        definitionRegistry=registry;
        Map<Object, Object> targetDataSources = new HashMap<>();
        // 将主数据源添加到更多数据源中
        targetDataSources.put("dataSource", defaultDataSource);
        DynamicDataSourceContextHolder.dataSourceIds.add("dataSource");
        // 添加更多数据源
        targetDataSources.putAll(customDataSources);
        DynamicDataSourceContextHolder.dataSourceIds.addAll(customDataSources.keySet());
        //添加分组数据源
        DynamicDataSourceContextHolder.dataSourceGroupIds.putAll(customDataSourcesGroup);

        // 创建DynamicDataSource
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(DynamicDataSource.class);
        beanDefinition.setSynthetic(true);

        MutablePropertyValues mpv = beanDefinition.getPropertyValues();
        mpv.addPropertyValue("defaultTargetDataSource", defaultDataSource);
        mpv.addPropertyValue("targetDataSources", targetDataSources);
        //注册一个Bean,指定bean名
        registry.registerBeanDefinition("dataSource", beanDefinition);
        logger.info("动态数据源注册!");
    }

    public DataSource buildDataSource(String dsName, Environment environment) {
        HikariConfig hikariConfig = new HikariConfig();
        Field[] fields = hikariConfig.getClass().getDeclaredFields();
        for (Field field : fields) {
            try {
                String propertyValue = environment.getProperty(Contains.getDsPoolPrefix(dsName, field.getName()));
                if (null != propertyValue && !propertyValue.trim().isEmpty()) {
                    PropertyDescriptor propertyDescriptor = new PropertyDescriptor(field.getName(), HikariConfig.class);
                    Method writeMethod = propertyDescriptor.getWriteMethod();
                    Class<?> type = field.getType();
                    if (type == int.class || type == Integer.class) {
                        writeMethod.invoke(hikariConfig, Integer.valueOf(propertyValue));
                    } else if (type == String.class) {
                        writeMethod.invoke(hikariConfig, propertyValue);
                    } else if (type == boolean.class || type == Boolean.class) {
                        writeMethod.invoke(hikariConfig, Boolean.valueOf(propertyValue));
                    } else if (type == long.class || type == Long.class) {
                        writeMethod.invoke(hikariConfig, Long.valueOf(propertyValue));
                    }
                }
            } catch (Exception e) {
                logger.error("构建数据源失败!");
            }
        }

        if (StringUtils.isEmpty(hikariConfig.getJdbcUrl()))
            return null;
        getDefaultIp(hikariConfig.getJdbcUrl(),environment);
        return new HikariDataSource(hikariConfig);
    }

    private void getDefaultIp(String jdbcUrl,Environment environment){
        //MaxLifetime
        String hostNameMatch=environment.getProperty(String.format("%s.%s", "spring.datasource", "hostNameMatch"), String.class);
        Long maxLifetime=environment.getProperty(String.format("%s.%s", "spring.datasource.hikari", "mongoDBMaxLifetime"), Long.class);
        System.err.println("mongoDBMaxLifetime:"+maxLifetime);
        if(maxLifetime!=null){
            mongoDBMaxLifetime=maxLifetime;
        }
        if(StringUtils.isEmpty(hostNameMatch)){
            return;
        }
        if(jdbcUrl.contains("hzylcenter-database")){
            return;
        }
        defaultHostNameMatch=hostNameMatch;
        String start="//";
        String end = ":";
        int startIndex = jdbcUrl.indexOf(start);
        int endIndex = jdbcUrl.indexOf(end, startIndex + start.length());
        if (startIndex != -1 && endIndex != -1) {
            String ip = jdbcUrl.substring(startIndex + start.length(), endIndex);
            if(ip.contains(".")){
                defaultIp=ip;
            }
        }

    }

    /**
     * 打印数据库表
     *
     * @param dataSource 数据源
     */
    public void printDbTable(DataSource dataSource,String poolName,Environment environment) {
        if (!Boolean.parseBoolean(environment.getProperty("spring.datasource.db.printTable", "false"))) {
            return;
        }

        try {
            Connection connection = dataSource.getConnection();
            DatabaseMetaData dbmd = connection.getMetaData();
            String dataBaseType = dbmd.getDatabaseProductName();
            PreparedStatement preparedStatement;
            if("DB2/LINUXX8664".equals(dataBaseType)){
                dataBaseType="Db2";
            }else if("Microsoft SQL Server".equals(dataBaseType)){
                dataBaseType="SQL Server";
            }else if("MongoDB".equals(dataBaseType)){
                logger.info("*** {} 暂不支持打印表名***.\n",dataBaseType);
                return;
            }
            String sql=supportDataBases.get(dataBaseType);
            preparedStatement = connection.prepareStatement(sql);
            logger.info("*** 打印数据源 {} 中表名开始***:", poolName);
            ResultSet executeQuery = preparedStatement.executeQuery();
            while (executeQuery.next()) {
                logger.info(executeQuery.getString(1));
            }
        } catch (Exception throwAbles) {
            logger.error("*** 打印数据源 {} 中表名失败***.\n",poolName);
        }

        logger.info("*** 打印数据源 {} 中表名结束***.\n",poolName);
    }

    /**
     * 适配的数据源类型集合
     */
    private void initSupportDataBases(){
        //数据库类型及查表Sql
        supportDataBases.put("MySQL","show tables");
        supportDataBases.put("Mongodb-BI","show tables");
        supportDataBases.put("Oracle","select TABLE_NAME from user_tables");
        supportDataBases.put("PostgreSQL","SELECT relname  FROM pg_stat_user_tables");
        supportDataBases.put("TDengine","show tables;");
        supportDataBases.put("MariaDB","show tables;");
        supportDataBases.put("SQL Server","select name from sys.tables");
        supportDataBases.put("ClickHouse","show tables;");
        supportDataBases.put("Db2","select tabname from syscat.tables");
        supportDataBases.put("TiDB","show tables;");
        supportDataBases.put("JDBC","");
        //暂未实现查询所有的集合
        supportDataBases.put("MongoDB","show tables");
        supportDataBases.put("Doris","show tables");
        supportDataBases.put("Apache Hive","show tables");

    }

    /**
     * 按照group_name分组数据源
     *
     * @param entityList
     * @return 构建分组数据
     */
    public Map<String, List<String>> buildGroupDataSource(List<Map<String, Object>> entityList) {
        Map<String, List<String>> result = new ConcurrentHashMap<>();
        for (Map.Entry<String, List<Map<String, Object>>> entryItem :
                Optional.ofNullable(entityList).orElse(new ArrayList<Map<String, Object>>()).stream()
                        .filter(entryItem -> !StringUtils.isEmpty(String.valueOf(entryItem.get("group_name"))))
                        .collect(Collectors.groupingBy(entryItem -> String.valueOf(entryItem.get("group_name"))))
                        .entrySet()) {

            String key = entryItem.getKey();
            List<String> groupIdList = entryItem.getValue().stream()
                    .map(mapItem -> String.valueOf(mapItem.get("pool_name"))).collect(Collectors.toList());
            if ("null".equals(key))
                continue;

            logger.info("*** 创建数据源分组 = {} 数据源集合 = {} 成功! ***", key, groupIdList);
            result.put(key, groupIdList);
        }
        return result;
    }

    /**
     * 按照group_name分组数据源
     *
     * @param environment
     * @return 构建分组数据
     */
    public Map<String, List<String>> buildGroupDataSource(Environment environment) {
        Map<String, List<String>> result = new ConcurrentHashMap<>();
        String dsPrefixes = environment.getProperty(Contains.getDsNameKey());
        if (!StringUtils.isEmpty(dsPrefixes)) {
            for (String dsPrefix : dsPrefixes.split(",")) {
                String group_name = environment.getProperty(Contains.getDsPoolPrefix(dsPrefix, "groupName"));

                if (StringUtils.isEmpty(group_name) || StringUtils.isEmpty(dsPrefix))
                    continue;

                List<String> groupIdList = new ArrayList<>();
                if (result.containsKey(group_name)) {
                    groupIdList = result.get(group_name);
                }

                groupIdList.add(dsPrefix);
                result.put(group_name, groupIdList);
                logger.info("*** 创建数据源分组  = {} 数据源 = {} 成功! ***", group_name, dsPrefix);
            }
        }
        return result;
    }
}

