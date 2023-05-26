## SpringBoot动态配置多数据源连接池（支持动态加载外部数据源）

#### 支持的数据源
| **状态**  | **数据源**              | **类型**      | **驱动类**                                      | **连接模板**                                 | **JDBC URL**                                      | **测试连接是否有效 SQL**       | **备注**                                 |
|---------|----------------------|-------------|----------------------------------------------|------------------------------------------|---------------------------------------------------|------------------------|----------------------------------------|
| **已适配** | MySQL                | MySQL       | com.mysql.cj.jdbc.Driver                     | jdbc:mysql://ip:port/db                  | jdbc:mysql://localhost:3306/xxl_job               | SELECT 1               |                                        |
| **已适配** | Oracle               | Oracle      | oracle.jdbc.OracleDriver                     | jdbc:oracle:thin:@ip:port:sid            | jdbc:oracle:thin:@localhost:1521:ORCL             | SELECT * from dual     |                                        |
| **已适配** | PostgreSQL           | PostgreSQL  | org.postgresql.Driver                        | jdbc:postgresql://ip:port/db             | jdbc:postgresql://localhost:5432/kong             | SELECT 1               |                                        |
| **已适配** | TDengine             | TDengine    | com.taosdata.jdbc.TSDBDriver                 | jdbc:TAOS://ip:port/db                   | jdbc:TAOS://localhost:6030/test                   | select server_status() | 自行动态链接库，需自测，详情见注意事项                    |
| **已适配** | MariaDB              | MariaDB     | org.mariadb.jdbc.Driver                      | jdbc:mariadb://ip:port/db                | jdbc:mariadb://localhost:3306/mysql               | SELECT 1               |                                        |
| **已适配** | Microsoft SQL Server | SQL Server  | com.microsoft.sqlserver.jdbc.SQLServerDriver | jdbc:sqlserver://ip:port;DatabaseName=db | jdbc:sqlserver://localhost:1433;databaseName=MyDb | select GETDATE()       |                                        |
| **已适配** | ClickHouse           | ClickHouse  | com.clickhouse.jdbc.ClickHouseDriver         | jdbc:clickhouse://ip:port/db             | jdbc:clickhouse://localhost:8123/system           | SELECT 1               |                                        |
| **已适配** | DB2                  | Db2         | com.ibm.db2.jcc.DB2Driver                    | jdbc:db2://ip:port/db                    | jdbc:db2://localhost:50001/SAMPLE                 | values 1               |                                        |
| **已适配** | TiDB                 | TiDB        | com.mysql.cj.jdbc.Driver                     | jdbc:mysql://ip:port/db                  | jdbc:mysql://localhost:4000/mydb                  | SELECT 1               |                                        |
| **已适配** | JDBC                 | JDBC        | xxxx                                         | xxxx                                     | xxxx                                              | xxxx                   | 创建JDBC数据源前先上传 jar到指定目录,详情见注意事项         |
| **已适配** | Apache Doris         | Doris       | com.mysql.cj.jdbc.Driver                     | jdbc:mysql://ip:port/db                  | jdbc:mysql://localhost:9031/information_schema    | SELECT 1               |                                        |
| **已适配** | Apache Hive          | Apache Hive | org.apache.hive.jdbc.HiveDriver              | jdbc:hive2://ip:port/db                  | jdbc:hive2://localhost:10000/default              | SELECT 1               |                                        |
| **已适配** | MongoDB              | MongoDB     | mongodb.jdbc.MongoDriver                     | jdbc:mongo://ip:port/db                  | jdbc:mongo://localhost:27017/runoob               | SELECT 1               | 使用驱动包 mongodb_unityjdbc_full.jar，见注意事项 |


### V1.0.0- 更新说明
```javascript
主要如下：
 1. 增加数据库脚本（多数据源集中在数据库中管理）；
 2. 增加注解TargetDataSource；
 3. 增加逻辑指定配置DynamicDbSource;
 4. 增加数据源连接分组；
 5. 增加负载均衡策略：
    A. 随机负载均衡策略；
    B. 轮询负载均衡策略；
 6. 增加动态管理配置数据源
```

### 插件简介
统一封装了多数据源连接池动态化配置插件，连接池使用的是业界号称"最快的"Hikari（SpringBoot2默认的连接池）；只需在项目中引入插件，在属性文件中添加必要的数据源连接配置信息及连接池参数，就可以通过注解进而动态切换数据源；并且在项目运行时动态加载外部数据源；动态配置生成DataSource连接池。



### 功能特点
	1. 项目动态支持多数据源连接池配置
	2. 理论上支持无限多个数据源连接池
	3. 通过注解切换数据源

#### 数据库脚本
```jql
CREATE TABLE `db_entity` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `driver_class_name` varchar(45) DEFAULT 'com.mysql.cj.jdbc.Driver' COMMENT '驱动类',
  `jdbc_url` varchar(45) DEFAULT NULL COMMENT '数据库连接地址<jdbc:mysql://ip:port/db>,必填！',
  `pool_name` varchar(45) NOT NULL COMMENT '连接池名称,必填！',
  `username` varchar(45) DEFAULT NULL COMMENT '用户名',
  `password` varchar(45) DEFAULT NULL COMMENT '密码',
  `minimum_idle` int(11) DEFAULT '5' COMMENT '最小空闲连接数',
  `maximum_pool_size` int(11) DEFAULT '10' COMMENT '最大连接数',
  `connection_test_query` varchar(45) DEFAULT 'SELECT 1' COMMENT '测试连接是否有效SQL',
  `group_name` varchar(45) NOT NULL COMMENT '分组名',
  `balance_type` varchar(45) DEFAULT NULL COMMENT '负载均衡类型',
  `enable` int(1) DEFAULT 0 COMMENT '状态 0 未启用 1 启用',
  `type` varchar(45) DEFAULT 'MySQL' COMMENT '数据库类型 必填',
  PRIMARY KEY (`id`),
  UNIQUE KEY `pool_name_UNIQUE` (`pool_name`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;
```

该表结构在resources/script/db_entity.sql文件中


### 使用说明
插件是基于SpringBoot开发maven管理的，使用步骤如下：

#### 1.添加插件maven依赖
```java
<dependency>
    <groupId>io.github.mmstarry</groupId>
    <artifactId>spring-boot-multi-source</artifactId>
    <version>1.0.0-RELEASE</version>
</dependency>
```

#### 2.在启动类上添加注解开启动态数据源
```java
@EnableDynamicDataSource
```

#### 3.必要的连接属性配置
```properties
server.port=8091
#插件 从数据库读取多数据源配置开关
spring.datasource.db.open=true
#插件 日志打印数据源表名
spring.datasource.db.printTable=true
#插件 存储多数据源数据库配置的表名
spring.datasource.table.name=db_entity

spring.datasource.type=com.zaxxer.hikari.util.DriverDataSource
#default
#表示使用基于DriverManager的配置
spring.datasource.hikari.jdbcUrl=jdbc:mysql://localhost:3306/db
#数据库连接用户名
spring.datasource.hikari.username=root
#数据库连接密码
spring.datasource.hikari.password=root
#数据库连接驱动
spring.datasource.hikari.driverClassName=com.mysql.cj.jdbc.Driver
#池中维护的最小空闲连接数,如果空闲连接低于此值并且总连接数小于maximumPoolSize，则HC将快速有效的添加其他连接
spring.datasource.hikari.minimumIdle=1
#池中维护的最大连接数
spring.datasource.hikari.maximumPoolSize=15
#控制连接是否自动提交事务
spring.datasource.hikari.autoCommit=true
#空闲连接闲置时间
spring.datasource.hikari.idleTimeout=30000
#连接池名称
spring.datasource.hikari.poolName=HikariCP
#连接最长生命周期,如果不等于0且小于30秒则会被重置回30秒
spring.datasource.hikari.maxLifetime=1800000
#从池中获取连接的最大等待时间默认30000毫秒，如果小于250毫秒则被重置会30秒
spring.datasource.hikari.connectionTimeout=30000
#测试连接有效性最大超时时间,默认5秒如果小于250毫秒，则重置回5秒
spring.datasource.hikari.validationTimeout=5000
#测试连接是否有效SQL，PS:不同数据库的测试SQL有可能不一样！
spring.datasource.hikari.connectionTestQuery=SELECT 1
#控制默认情况下从池中获取的Connections是否处于只读模式
spring.datasource.hikari.readOnly=false
#是否在其自己的事务中隔离内部池查询，例如连接活动测试。通常使用默认值，默认值：false
spring.datasource.hikari.isolateInternalQueries=false
#此属性控制是否注册JMX管理Bean
spring.datasource.hikari.registerMbeans=false
#此属性控制是否可以通过JMX（Java Management Extensions，即Java管理扩展，它提供了一种在运行时动态管理资源的体系结构）挂起和恢复池
spring.datasource.hikari.allowPoolSuspension=false

logging.config=classpath:logback-boot.xml
logging.level.root=info

#是否开启加载外部第三方jar包
loadJar.open=true
#外部第三方jar包 所在目录
loadJar.basePath=D:\jar
```
#### 4.使用方式
由于数据源动态切换是使用Aspect+注解完成的，所以调用时需要将Bean交给Spring的IOC容器管理。只有这样Spring才能通过AOP加强，触发我们的切换逻辑。
```java

//方式1 指定连接池(Service层或者Dao层)
@TargetDataSource("db")
//@TargetDataSource(groupName = "db")
public List<String> findById(int id) {
        return dbDao.findById();
}


//方式2 指定连接池(Controller层)
@GetMapping(value = "/update")
public void update()  {
    DynamicDbSource.set("db2");
    db2Service.update();
}

//对应的Service层 方法上加注解@TargetDataSource
@TargetDataSource
public void update(){
   db2Dao.update();
}
```
PS: DynamicDbSource.set(“连接池名称”),可以根据自己的实际业务逻辑设置数据源名称。例如我们需要根据请求的pk获取当前连接对应的数据源配置，
获取到名字后在这里设置为数据源名字即可。

#### 5.事务回滚
```
1.启动类加注解 @EnableTransactionManagement(order = 0)
2. @Transactional与@TargetDataSource 一起出现

@TargetDataSource
@Transactional(rollbackFor = Exception.class)
public void update(){
   db2Dao.update();
}

```
#### 6.工具类
##### 获取连接池中的数据源pool_name集合
```
List<String> dataSourcesList=DataSourceUtils.getDataSources();
```
##### 动态加载外部数据源
```
Map<String, Object> datasourceentity = new HashMap();
datasourceentity.put("driver_class_name", "com.mysql.cj.jdbc.Driver");
datasourceentity.put("jdbc_url", "jdbc:mysql://192.168.200.75:3306/db1");
datasourceentity.put("pool_name", "db1");
datasourceentity.put("group_name",  "db1");
datasourceentity.put("balance_type", "");
datasourceentity.put("username", "root");
datasourceentity.put("password", "root");
datasourceentity.put("minimum_idle", "5");
datasourceentity.put("maximum_pool_size", "10");
datasourceentity.put("connection_test_query", "SELECT 1");
datasourceentity.put("type", "MySQL");

#加载外部数据源
# true:加载成功  false:加载失败
boolean flag=DataSourceUtils.AddDataSource(datasourceentity);
```

##### 校验连接的有效性 等待返回值，请勿重复提交
```
# status 0:有效  -1:无效
Map validResultMap=DataSourceUtils.validConnection(datasourceentity);
```
### 7.启动信息
系统优先使用注解方法上的属性配置，注解方法上没有配置的情况下会读取DynamicDbSource设置的数据源配置。系统启动时会日志中会打印出当前创建连接
池的情况，以及连接池中的数据表。
```javascript
                    _                        _                    _                              _  _    _
                   (_)                      | |                  | |                            | || |  (_)
  ___  _ __   _ __  _  _ __    __ _  ______ | |__    ___    ___  | |_  ______  _ __ ___   _   _ | || |_  _  ______  ___   ___   _   _  _ __  ___  ___
 / __|| '_ \ | '__|| || '_ \  / _` ||______|| '_ \  / _ \  / _ \ | __||______|| '_ ` _ \ | | | || || __|| ||______|/ __| / _ \ | | | || '__|/ __|/ _ \
 \__ \| |_) || |   | || | | || (_| |        | |_) || (_) || (_) || |_         | | | | | || |_| || || |_ | |        \__ \| (_) || |_| || |  | (__|  __/
 |___/| .__/ |_|   |_||_| |_| \__, |        |_.__/  \___/  \___/  \__|        |_| |_| |_| \__,_||_| \__||_|        |___/ \___/  \__,_||_|   \___|\___|
      | |                      __/ |
      |_|                     |___/


2023-05-08 16:16:03.529  INFO 33932 --- [           main] c.h.e.j.JdbcTemplateApplication          : Starting JdbcTemplateApplication using Java 1.8.0_121 on ON with PID 33932 (jdbctemplate\target\classes started by 16688 in spring-boot-multi-source)
2023-05-08 16:16:03.531  INFO 33932 --- [           main] c.h.e.j.JdbcTemplateApplication          : No active profile set, falling back to 1 default profile: "default"
2023-05-08 16:16:03.630  WARN 33932 --- [           main] com.zaxxer.hikari.HikariConfig           : HikariCP1 - idleTimeout has been set but has no effect because the pool is operating as a fixed size pool.
2023-05-08 16:16:03.632  INFO 33932 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariCP1 - Starting...
2023-05-08 16:16:03.791  INFO 33932 --- [           main] com.zaxxer.hikari.HikariDataSource       : HikariCP1 - Start completed.
2023-05-08 16:16:03.791  INFO 33932 --- [           main] c.h.s.config.DynamicDataSourceRegister   : *** 启动时创建默认数据源成功! ***
2023-05-08 16:16:03.791  INFO 33932 --- [           main] c.h.s.config.DynamicDataSourceRegister   : *** 打印数据源 default 中表名开始***:
2023-05-08 16:16:03.800  INFO 33932 --- [           main] c.h.s.config.DynamicDataSourceRegister   : db_entity
2023-05-08 16:16:03.800  INFO 33932 --- [           main] c.h.s.config.DynamicDataSourceRegister   : *** 打印数据源 default 中表名结束***.

2023-05-08 16:16:03.811  INFO 33932 --- [           main] c.h.s.config.DynamicDataSourceRegister   : *** 创建数据源组 = xxl_job 数据源集合 = [xxl_job] 成功! ***
2023-05-08 16:16:03.811  INFO 33932 --- [           main] com.zaxxer.hikari.HikariDataSource       : xxl_job - Starting...
2023-05-08 16:16:04.068  INFO 33932 --- [           main] com.zaxxer.hikari.HikariDataSource       : xxl_job - Start completed.
2023-05-08 16:16:04.068  INFO 33932 --- [           main] c.h.s.config.DynamicDataSourceRegister   : *** 启动时创建数据源 xxl_job 成功! ***
2023-05-08 16:16:04.068  INFO 33932 --- [           main] c.h.s.config.DynamicDataSourceRegister   : *** 打印数据源 xxl_job 中表名开始***:
2023-05-08 16:16:04.069  INFO 33932 --- [           main] c.h.s.config.DynamicDataSourceRegister   : xxl_job_info
2023-05-08 16:16:04.069  INFO 33932 --- [           main] c.h.s.config.DynamicDataSourceRegister   : *** 打印数据源 xxl_job 中表名结束***.

2023-05-08 16:16:04.182  INFO 33932 --- [           main] c.h.s.config.DynamicDataSourceRegister   : 动态数据源注册!
2023-05-08 16:16:04.259  INFO 33932 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Bootstrapping Spring Data JDBC repositories in DEFAULT mode.
2023-05-08 16:16:04.264  INFO 33932 --- [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 3 ms. Found 0 JDBC repository interfaces.
2023-05-08 16:16:04.606  INFO 33932 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port(s): 8091 (http)
2023-05-08 16:16:04.612  INFO 33932 --- [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
2023-05-08 16:16:04.612  INFO 33932 --- [           main] org.apache.catalina.core.StandardEngine  : Starting Servlet engine: [Apache Tomcat/9.0.68]
2023-05-08 16:16:04.747  INFO 33932 --- [           main] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring embedded WebApplicationContext
2023-05-08 16:16:04.747  INFO 33932 --- [           main] w.s.c.ServletWebServerApplicationContext : Root WebApplicationContext: initialization completed in 1191 ms
2023-05-08 16:16:05.086  INFO 33932 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8091 (http) with context path ''
2023-05-08 16:16:05.092  INFO 33932 --- [           main] c.h.e.j.JdbcTemplateApplication          : Started JdbcTemplateApplication in 1.796 seconds (JVM running for 2.31)
```

#### 注意事项1：需要添加TDengine数据源，需要在父项目启动类上增加如下配置，找到其动态链接库
 ```html
taos.dll
```


### 注意事项2： JDBC数据源支持

##### 1.yml配置文件
```
loadJar:
  #是否开启加载外部第三方jar包
  open: true
  #外部第三方jar包 所在目录
  basePath: D:\jar
```
