# Java 后端工程师 · 高频面试题集

> **说明**：本文档面试题整理自 [JavaGuide](https://javaguide.cn/)（GitHub 140k+ Star），涵盖 Java 基础、面向对象、并发、MySQL、Spring、Redis、MyBatis 等核心模块。
> 
> **数据来源**：JavaGuide 开源项目（https://github.com/Snailclimb/JavaGuide）
> 
> **配套文档**：[学习资料收集-Java后端方向.md](学习资料收集-Java后端方向.md)（含网课视频链接）

---

## 目录

1. [Java 基础](#1-java-基础)
2. [面向对象](#2-面向对象)
3. [Java 并发](#3-java-并发)
4. [MySQL](#4-mysql)
5. [Spring](#5-spring)
6. [Redis](#6-redis)
7. [MyBatis](#7-mybatis)

---

## 1. Java 基础

### 基础概念与常识

| # | 题目 | 关键要点 |
|---|------|----------|
| 1 | Java 语言有哪些特点？ | 面向对象、平台无关性（JVM）、支持多线程、可靠性、安全性 |
| 2 | Java SE vs Java EE？ | SE 标准版（桌面应用）；EE 企业版（Web 应用、企业级） |
| 3 | JVM vs JDK vs JRE？ | JVM 执行字节码；JDK = JRE + 开发工具；JRE = JVM + 核心类库 |
| 4 | 什么是字节码？采用字节码的好处是什么？ | .class 文件；跨平台、安全性、性能优化 |
| 5 | 为什么说 Java 语言"编译与解释并存"？ | 先编译为字节码，再由 JVM 解释执行；JIT 编译热点代码 |
| 6 | Oracle JDK vs OpenJDK？ | Oracle JDK 商业支持；OpenJDK 开源；功能基本一致 |

### 基本语法

| # | 题目 | 关键要点 |
|---|------|----------|
| 7 | 注释有哪几种形式？ | 单行 `//`、多行 `/* */`、文档 `/** */` |
| 8 | 标识符和关键字的区别是什么？ | 标识符：用户自定义名称；关键字：语言保留字 |
| 9 | Java 语言关键字有哪些？ | 50 个关键字（class、interface、extends、implements 等） |
| 10 | 自增自减运算符？ | `i++` 先用后加；`++i` 先加后用 |
| 11 | 移位运算符？ | `<<` 左移、`>>` 右移（符号位填充）、`>>>` 无符号右移 |
| 12 | continue、break 和 return 的区别？ | continue 跳过本次循环；break 跳出循环；return 返回并结束方法 |

### 基本数据类型

| # | 题目 | 关键要点 |
|---|------|----------|
| 13 | Java 中的几种基本数据类型？ | byte(1)、short(2)、int(4)、long(8)、float(4)、double(8)、char(2)、boolean |
| 14 | 基本类型和包装类型的区别？ | 基本类型直接存值；包装类型是对象、可为 null、有方法 |
| 15 | 包装类型的缓存机制？ | Integer 缓存 -128~127；Boolean 缓存 TRUE/FALSE |
| 16 | 自动装箱与拆箱？原理是什么？ | 基本类型↔包装类自动转换；编译器调用 valueOf/xxxValue |
| 17 | 为什么浮点数运算有精度丢失风险？ | 二进制无法精确表示某些十进制小数；BigDecimal 解决 |
| 18 | 超过 long 整型的数据如何表示？ | BigInteger；任意精度整数 |

### 变量与方法

| # | 题目 | 关键要点 |
|---|------|----------|
| 19 | 成员变量与局部变量的区别？ | 成员：类中、有默认值、堆/方法区；局部：方法中、无默认值、栈 |
| 20 | 静态变量有什么作用？ | 类变量；所有实例共享；类加载时初始化 |
| 21 | 字符型常量和字符串常量的区别？ | char 单引号、占 2 字节；String 双引号、对象 |
| 22 | 静态方法为什么不能调用非静态成员？ | 静态方法属于类；非静态成员属于实例；生命周期不同 |
| 23 | 静态方法和实例方法有何不同？ | 静态：类名调用、不能用 this/super；实例：对象调用 |
| 24 | 重载和重写有什么区别？ | 重载：同类、方法名同、参数不同；重写：子类覆写父类方法 |
| 25 | 什么是可变长参数？ | `Type... args`；本质是数组；只能放参数列表最后 |

---

## 2. 面向对象

### 面向对象基础

| # | 题目 | 关键要点 |
|---|------|----------|
| 1 | 面向对象和面向过程的区别？ | 面向对象：封装继承多态、易维护；面向过程：性能高、流程清晰 |
| 2 | 创建一个对象用什么运算符？对象实例与对象引用有何不同？ | new 运算符；引用指向实例，一个实例可有多个引用 |
| 3 | 对象的相等和引用相等的区别？ | 对象相等：equals() 比较内容；引用相等：== 比较地址 |
| 4 | 如果一个类没有声明构造方法，能正确执行吗？ | 能；编译器自动提供无参默认构造方法 |
| 5 | 构造方法有哪些特点？是否可被 override？ | 与类同名、无返回值、自动调用；不可 override，可重载 |
| 6 | 面向对象三大特征？ | 封装、继承、多态 |
| 7 | 接口和抽象类有什么共同点和区别？ | 共同：都可被继承、都不能实例化。区别：接口多实现、只能有抽象方法；抽象类单继承、可有普通方法 |
| 8 | 深拷贝和浅拷贝区别？什么是引用拷贝？ | 浅拷贝：引用字段仍指向原对象；深拷贝：完全独立副本；引用拷贝：同一对象 |

### Object 类

| # | 题目 | 关键要点 |
|---|------|----------|
| 9 | Object 类的常见方法有哪些？ | equals()、hashCode()、toString()、clone()、getClass()、wait()、notify()、finalize() |
| 10 | == 和 equals() 的区别？ | == 比较地址（引用类型）或值（基本类型）；equals() 默认同 ==，String 等重写后比较内容 |
| 11 | 为什么要有 hashCode？ | 提高哈希表效率；equals 为 true → hashCode 必同；重写 equals 必须重写 hashCode |

### String 类

| # | 题目 | 关键要点 |
|---|------|----------|
| 12 | String、StringBuffer、StringBuilder 的区别？ | String 不可变；StringBuilder 可变非线程安全；StringBuffer 可变线程安全 |
| 13 | String 为什么是不可变的？ | final char[] value（JDK 9+ byte[]）；不可变保证线程安全、哈希值缓存 |
| 14 | 字符串拼接用 `+` 还是 StringBuilder？ | 少量用 `+`（编译器优化）；循环中用 StringBuilder |
| 15 | String equals() 和 Object equals() 有何区别？ | String 重写了 equals()，比较内容；Object 比较地址 |
| 16 | 字符串常量池的作用？ | 避免重复创建相同字符串；intern() 方法放入常量池 |
| 17 | `String s1 = new String("abc")` 创建了几个对象？ | 1 或 2 个；常量池有 "abc" 则 1 个（堆中 String 对象）；否则 2 个（常量池 + 堆） |
| 18 | String intern() 方法有什么作用？ | 将字符串放入常量池并返回引用；JDK 7+ 常量池在堆中 |
| 19 | String 类型的变量和常量做 `+` 运算时发生了什么？ | 编译期常量折叠；运行时 StringBuilder 拼接 |

---

## 3. Java 并发

### 线程基础

| # | 题目 | 关键要点 |
|---|------|----------|
| 1 | 什么是线程和进程？ | 进程：资源分配单位；线程：CPU 调度单位；一个进程可有多个线程 |
| 2 | Java 线程和操作系统的线程有啥区别？ | JDK 1.2 之前：用户线程；之后：一对一映射 OS 线程 |
| 3 | 线程与进程的关系、区别及优缺点？ | 线程：轻量、共享内存、切换快；进程：独立、安全、切换开销大 |
| 4 | 程序计数器为什么是私有的？ | 线程切换后恢复执行位置；字节码解释器工作依赖 |
| 5 | 虚拟机栈和本地方法栈为什么是私有的？ | 保证线程中的局部变量不被其他线程访问 |
| 6 | 如何创建线程？ | 继承 Thread、实现 Runnable、实现 Callable、线程池 |
| 7 | 线程的生命周期和状态？ | NEW→RUNNABLE→BLOCKED/WAITING/TIMED_WAITING→TERMINATED |
| 8 | 什么是线程上下文切换？ | CPU 从一个线程切换到另一个；保存/恢复状态 |
| 9 | Thread.sleep() 和 Object.wait() 对比？ | sleep：不释放锁、Thread 静态方法；wait：释放锁、Object 方法 |
| 10 | 为什么 wait() 不定义在 Thread 中？ | wait 依赖锁（monitor）；锁属于对象 |
| 11 | 可以直接调用 Thread 类的 run() 方法吗？ | 可以，但不会启动新线程；start() 才会启动 |

### 多线程

| # | 题目 | 关键要点 |
|---|------|----------|
| 12 | 并发与并行的区别？ | 并发：交替执行；并行：同时执行 |
| 13 | 同步和异步的区别？ | 同步：等待结果；异步：不等待，回调通知 |
| 14 | 为什么要使用多线程？ | 提高 CPU 利用率；提高响应速度；多核并行 |
| 15 | 单核 CPU 支持 Java 多线程吗？ | 支持；通过时间片轮转实现并发 |
| 16 | 使用多线程可能带来什么问题？ | 内存泄漏、死锁、线程安全问题 |
| 17 | 如何理解线程安全和不安全？ | 多线程访问共享数据，结果与单线程一致 = 线程安全 |
| 18 | 什么是线程死锁？ | 两个以上线程互相等待对方释放资源 |
| 19 | 如何检测死锁？ | jstack、jconsole、VisualVM |
| 20 | 如何预防和避免线程死锁？ | 破坏四个必要条件之一；按顺序加锁；超时释放 |

### volatile 关键字

| # | 题目 | 关键要点 |
|---|------|----------|
| 21 | JMM（Java 内存模型）？ | 定义线程共享变量的访问规则；主内存 vs 工作内存 |
| 22 | volatile 如何保证变量的可见性？ | 写入后立即刷新到主内存；读取时从主内存加载 |
| 23 | volatile 如何禁止指令重排序？ | 内存屏障（Memory Barrier）；LoadLoad、StoreStore 等 |
| 24 | volatile 可以保证原子性么？ | 不能；i++ 不是原子操作；需用 synchronized 或 AtomicInteger |

### 锁机制

| # | 题目 | 关键要点 |
|---|------|----------|
| 25 | 乐观锁和悲观锁？ | 悲观：先加锁再操作；乐观：提交时检测冲突（CAS） |
| 26 | CAS 算法存在哪些问题？ | ABA 问题、循环开销大、只能保证单个变量原子性 |
| 27 | synchronized 是什么？有什么用？ | 内置锁；保证原子性、可见性、有序性 |
| 28 | synchronized 底层原理？ | 同步代码块：monitorenter/monitorexit；同步方法：ACC_SYNCHRONIZED |
| 29 | JDK 1.6 之后 synchronized 做了哪些优化？ | 偏向锁→轻量级锁→重量级锁；锁升级 |
| 30 | synchronized 和 volatile 有什么区别？ | synchronized：原子性+可见性+有序性；volatile：可见性+有序性 |
| 31 | ReentrantLock 是什么？ | 可重入锁；API 层面；支持公平锁、可中断、超时 |
| 32 | synchronized 和 ReentrantLock 有什么区别？ | synchronized：JVM 层面、自动释放；ReentrantLock：API 层面、手动释放、功能更多 |

---

## 4. MySQL

### 基础

| # | 题目 | 关键要点 |
|---|------|----------|
| 1 | 什么是关系型数据库？ | 基于关系模型（表）；SQL 查询；ACID 事务 |
| 2 | 什么是 SQL？ | 结构化查询语言；DDL/DML/DCL/TCL |
| 3 | MySQL 有什么优点？ | 开源免费、性能好、社区活跃、支持多种存储引擎 |

### 字段类型

| # | 题目 | 关键要点 |
|---|------|----------|
| 4 | 整数类型的 unsigned 属性有什么用？ | 无符号；范围翻倍；避免负数 |
| 5 | char 和 varchar 的区别？ | char 定长、速度快；varchar 变长、省空间 |
| 6 | varchar(100) 和 varchar(10) 的区别？ | 存储相同数据时占用空间相同；但 varchar(100) 会消耗更多内存（排序、临时表） |
| 7 | decimal 和 float/double 的区别？ | decimal 精确（财务）；float/double 近似（科学计算） |
| 8 | datetime 和 timestamp 的区别？如何选择？ | datetime：8 字节、范围大；timestamp：4 字节、自动更新、受时区影响 |
| 9 | NULL 和 '' 的区别？ | NULL 表示未知；'' 是空字符串（确定的值） |

### 存储引擎

| # | 题目 | 关键要点 |
|---|------|----------|
| 10 | MySQL 支持哪些存储引擎？默认使用哪个？ | InnoDB（默认）、MyISAM、Memory、Archive 等 |
| 11 | MyISAM 和 InnoDB 有什么区别？ | InnoDB：事务、行锁、外键、聚簇索引；MyISAM：表锁、全文索引、不支持事务 |

### 索引

| # | 题目 | 关键要点 |
|---|------|----------|
| 12 | 索引是什么？ | 数据结构；加快查询；类似书的目录 |
| 13 | 索引为什么快？ | 减少扫描行数；B+ 树有序，范围查询高效 |
| 14 | MySQL 索引底层数据结构是什么？ | B+ 树；叶子节点有序链表；磁盘 IO 少 |
| 15 | 为什么 InnoDB 没有使用哈希作为索引？ | 哈希不支持范围查询；不支持排序 |
| 16 | 为什么 InnoDB 没有使用 B 树？ | B 树叶子节点无链表；范围查询需中序遍历 |
| 17 | 什么是覆盖索引？ | 查询列全在索引中，无需回表 |
| 18 | 联合索引及其最左前缀原则？ | 查询条件必须从索引最左列开始匹配 |
| 19 | SELECT * 会导致索引失效吗？ | 可能；覆盖索引时不会；否则需要回表 |
| 20 | 哪些字段适合创建索引？ | WHERE/JOIN/ORDER BY/GROUP BY 的列；高选择性 |
| 21 | 索引失效的原因有哪些？ | 函数/运算操作列、LIKE '%xx'、OR 条件、类型隐式转换 |

### 事务

| # | 题目 | 关键要点 |
|---|------|----------|
| 22 | 什么是事务？ | 一组操作，要么全成功，要么全失败 |
| 23 | 并发事务带来哪些问题？ | 脏读、丢失修改、不可重复读、幻读 |
| 24 | SQL 标准定义了哪些事务隔离级别？ | 读未提交→读已提交→可重复读→串行化 |
| 25 | MySQL 的默认隔离级别是什么？ | 可重复读（Repeatable Read） |
| 26 | MySQL 的隔离级别基于锁实现吗？ | 不完全是；MVCC 实现读不加锁 |

### 锁

| # | 题目 | 关键要点 |
|---|------|----------|
| 27 | 表级锁和行级锁？ | 表锁：开销小、并发低；行锁：开销大、并发高 |
| 28 | InnoDB 有哪几类行锁？ | 记录锁（Record Lock）、间隙锁（Gap Lock）、临键锁（Next-Key Lock） |
| 29 | 共享锁和排他锁？ | 共享锁（S）：读锁；排他锁（X）：写锁 |
| 30 | 当前读和快照读？ | 当前读：读最新数据，加锁；快照读：读历史版本，不加锁（MVCC） |

### 性能优化

| # | 题目 | 关键要点 |
|---|------|----------|
| 31 | 有哪些常见的 SQL 优化手段？ | EXPLAIN 分析、合理建索引、避免 SELECT *、分页优化 |
| 32 | 如何分析 SQL 的性能？ | EXPLAIN；关注 type、key、rows、Extra |
| 33 | 什么是慢查询？如何定位？ | 执行时间超过阈值的 SQL；slow_query_log |
| 34 | MySQL 的日志系统？ | binlog（主从复制）、redo log（崩溃恢复）、undo log（事务回滚） |
| 35 | 什么是 MVCC？ | 多版本并发控制；Read View + Undo Log 实现快照读 |

---

## 5. Spring

### 基础

| # | 题目 | 关键要点 |
|---|------|----------|
| 1 | 什么是 Spring 框架？ | 轻量级容器框架；IoC、AOP、事务管理、Web MVC |
| 2 | Spring 包含的模块有哪些？ | Core Container、AOP、Data Access、Web、Messaging、Test |
| 3 | Spring、Spring MVC、Spring Boot 之间什么关系？ | Spring：核心框架；Spring MVC：Web 框架；Spring Boot：快速开发、自动配置 |

### IoC

| # | 题目 | 关键要点 |
|---|------|----------|
| 4 | 什么是 IoC？ | 控制反转；对象创建和依赖管理交给容器 |
| 5 | IoC 解决了什么问题？ | 对象耦合；依赖管理复杂；便于测试 |
| 6 | 什么是 Spring Bean？ | 由 Spring 容器管理的对象 |
| 7 | 将一个类声明为 Bean 的注解有哪些？ | @Component、@Service、@Repository、@Controller |
| 8 | @Component 和 @Bean 的区别？ | @Component：类上注解；@Bean：方法上注解，用于第三方类 |
| 9 | 注入 Bean 的注解有哪些？ | @Autowired（Spring）、@Resource（JSR-250）、@Inject（JSR-330） |
| 10 | @Autowired 和 @Resource 的区别？ | @Autowired 按类型注入；@Resource 按名称注入 |
| 11 | 构造函数注入还是 Setter 注入？ | 推荐构造函数注入；必填用构造、可选用 Setter |
| 12 | Bean 的作用域有哪些？ | singleton（默认）、prototype、request、session、application |
| 13 | Bean 是线程安全的吗？ | singleton 非线程安全（共享实例）；prototype 每次创建新实例 |
| 14 | Bean 的生命周期？ | 实例化→属性注入→Aware→BeanPostProcessor→@PostConstruct→使用→@PreDestroy |

### AOP

| # | 题目 | 关键要点 |
|---|------|----------|
| 15 | 谈谈对 AOP 的了解？ | 面向切面编程；横切关注点分离；日志、事务、权限 |
| 16 | Spring AOP 和 AspectJ AOP 有什么区别？ | Spring AOP：运行时代理（JDK/CGLIB）；AspectJ：编译时织入 |
| 17 | AOP 常见的通知类型？ | 前置、后置、环绕、返回、异常 |
| 18 | 多个切面的执行顺序如何控制？ | @Order 注解；实现 Ordered 接口 |

### Spring MVC

| # | 题目 | 关键要点 |
|---|------|----------|
| 19 | Spring MVC 的核心组件？ | DispatcherServlet、HandlerMapping、HandlerAdapter、ViewResolver |
| 20 | SpringMVC 工作原理？ | 请求→DispatcherServlet→HandlerMapping→Handler→ViewResolver→响应 |
| 21 | 统一异常处理怎么做？ | @RestControllerAdvice + @ExceptionHandler |

### 循环依赖

| # | 题目 | 关键要点 |
|---|------|----------|
| 22 | Spring 循环依赖了解吗？怎么解决？ | 三级缓存；singletonFactories 提前暴露对象 |
| 23 | @Lazy 能解决循环依赖吗？ | 能；延迟初始化，打破循环 |
| 24 | SpringBoot 允许循环依赖发生么？ | 默认不允许；需配置 spring.main.allow-circular-references=true |

### 事务

| # | 题目 | 关键要点 |
|---|------|----------|
| 25 | Spring 管理事务的方式？ | 编程式（TransactionTemplate）、声明式（@Transactional） |
| 26 | Spring 事务中哪几种事务传播行为？ | REQUIRED（默认）、REQUIRES_NEW、NESTED、SUPPORTS、NOT_SUPPORTED、MANDATORY、NEVER |
| 27 | @Transactional(rollbackFor = Exception.class)？ | 默认只回滚 RuntimeException；rollbackFor = Exception.class 回滚所有异常 |

---

## 6. Redis

### 基础

| # | 题目 | 关键要点 |
|---|------|----------|
| 1 | 什么是 Redis？ | 开源内存数据库；高性能 KV 缓存；支持多种数据结构 |
| 2 | Redis 为什么这么快？ | 纯内存 + 单线程避免上下文切换 + IO 多路复用 + 高效数据结构 |
| 3 | Redis 和 Memcached 的区别？ | Redis：多种数据结构、持久化、集群；Memcached：仅 KV、多线程 |
| 4 | 为什么要用 Redis？ | 高性能（内存）；高并发（单线程）；丰富数据结构 |
| 5 | 为什么用 Redis 而不用本地缓存？ | 分布式一致性；容量大；持久化 |

### 数据类型

| # | 题目 | 关键要点 |
|---|------|----------|
| 6 | Redis 常用的数据类型？ | String、Hash、List、Set、Sorted Set（ZSet） |
| 7 | String 的应用场景？ | 缓存、计数器、分布式锁、Session |
| 8 | String 还是 Hash 存储对象数据更好？ | 简单对象用 String（JSON）；频繁部分更新用 Hash |
| 9 | String 的底层实现？ | SDS（Simple Dynamic String）；二进制安全 |
| 10 | 使用 Redis 实现排行榜怎么做？ | ZSET + ZADD/ZREVRANGE；score 为分数 |
| 11 | Redis 有序集合底层为什么用跳表而不用红黑树？ | 实现简单；范围查询友好；插入删除效率高 |
| 12 | Set 的应用场景？ | 去重、交并差集、抽奖 |
| 13 | Bitmap 统计活跃用户？ | SETBIT/GETBIT；日期为 key，用户 ID 为 offset |
| 14 | HyperLogLog 统计页面 UV？ | 基数统计；误差 0.81%；只占 12KB |

### 持久化

| # | 题目 | 关键要点 |
|---|------|----------|
| 15 | Redis 持久化机制？ | RDB（快照）、AOF（写命令日志）、混合持久化（4.0+） |
| 16 | RDB 和 AOF 的区别？ | RDB：恢复快、可能丢数据；AOF：数据安全、文件大 |

### 线程模型

| # | 题目 | 关键要点 |
|---|------|----------|
| 17 | Redis 单线程模型？ | 命令执行单线程；IO 多路复用处理连接 |
| 18 | Redis 6.0 之前为什么不用多线程？ | CPU 不是瓶颈，网络 IO 才是；单线程避免上下文切换 |
| 19 | Redis 6.0 之后为何引入多线程？ | 网络 IO 多线程；命令执行仍单线程 |

### 内存管理

| # | 题目 | 关键要点 |
|---|------|----------|
| 20 | Redis 给缓存设置过期时间有什么用？ | 避免内存无限增长；业务数据时效性 |
| 21 | Redis 如何判断数据是否过期？ | 惰性删除 + 定期删除 |
| 22 | 大量 key 集中过期怎么办？ | 随机过期时间；过期 key 异步删除 |
| 23 | Redis 内存淘汰策略？ | LRU、LFU、随机、TTL、noeviction |

### 应用

| # | 题目 | 关键要点 |
|---|------|----------|
| 24 | 如何基于 Redis 实现分布式锁？ | SET key value NX EX 30；推荐 Redisson（看门狗续期） |
| 25 | Redis 可以做消息队列么？ | 可以；List（简单）、Pub/Sub（不持久）、Stream（推荐） |
| 26 | 如何基于 Redis 实现延时任务？ | ZSET + score 为时间戳；轮询 ZRANGEBYSCORE |
| 27 | 常见的缓存读写策略？ | Cache Aside（先更新 DB，再删缓存）、Read/Write Through、Write Behind |

### 生产问题

| # | 题目 | 关键要点 |
|---|------|----------|
| 28 | 缓存穿透？ | 查不存在的数据→布隆过滤器、缓存空值 |
| 29 | 缓存击穿？ | 热点 key 过期→互斥锁、永不过期 |
| 30 | 缓存雪崩？ | 大量 key 同时过期→随机 TTL、集群 |
| 31 | 如何保证缓存和数据库数据的一致性？ | Cache Aside Pattern；延迟双删 |
| 32 | Redis BigKey？ | String > 10KB 或集合 > 5000 元素；拆分 + UNLINK |
| 33 | Redis HotKey？ | 高频访问的 Key；本地缓存、Key 分散 |
| 34 | Redis 集群方案？ | 主从复制、哨兵（Sentinel）、Cluster（16384 槽） |
| 35 | Redis 事务？ | MULTI/EXEC；不支持回滚；Lua 脚本保证原子性 |

---

## 7. MyBatis

### 基础

| # | 题目 | 关键要点 |
|---|------|----------|
| 1 | MyBatis 是什么？为什么说它是半自动 ORM？ | 需手写 SQL；Hibernate 全自动 |
| 2 | MyBatis 和 JPA/Hibernate 有什么区别？ | MyBatis：SQL 灵活；JPA：面向对象、自动生成 SQL |
| 3 | MyBatis 有哪些核心组件？ | SqlSessionFactory、SqlSession、Mapper、Executor |
| 4 | MyBatis 执行一条查询的完整流程？ | 加载配置→SqlSession→Executor→StatementHandler→ResultSetHandler |

### 参数处理与动态 SQL

| # | 题目 | 关键要点 |
|---|------|----------|
| 5 | `#{}` 和 `${}` 的区别？ | `#{}` 预编译防注入；`${}` 字符串拼接有注入风险 |
| 6 | MyBatis 动态 SQL 有哪些标签？ | if、choose/when/otherwise、where、set、foreach、trim |

### 结果映射

| # | 题目 | 关键要点 |
|---|------|----------|
| 7 | resultType 和 resultMap 的区别？ | resultType 直接映射；resultMap 手动配置映射关系 |
| 8 | association 和 collection 的区别？ | association：一对一；collection：一对多 |
| 9 | 什么是 N+1 查询？如何避免？ | 一条查询 + N 条关联查询；用 JOIN 或批量查询 |
| 10 | MyBatis 支持延迟加载吗？原理？ | 支持；CGLIB 代理，调用目标方法时拦截并加载 |

### 缓存

| # | 题目 | 关键要点 |
|---|------|----------|
| 11 | MyBatis 一级缓存？ | SqlSession 级别；默认开启；同一 SqlSession 内缓存 |
| 12 | Spring 项目中为什么一级缓存没生效？ | 每次请求创建新 SqlSession；Spring 管理事务时才共享 |
| 13 | MyBatis 二级缓存？如何开启？ | Mapper 级别；需手动开启；跨 SqlSession 共享 |
| 14 | 为什么生产项目谨慎使用二级缓存？ | 可能脏读；缓存粒度大；不适合多表关联 |

### Executor 与插件

| # | 题目 | 关键要点 |
|---|------|----------|
| 15 | MyBatis 有哪些 Executor？ | Simple、Reuse、Batch |
| 16 | MyBatis 如何执行批处理？ | ExecutorType.BATCH；rewriteBatchedStatements |
| 17 | MyBatis 如何分页？分页插件原理？ | RowBounds（逻辑分页）；PageHelper（物理分页，拦截器） |
| 18 | MyBatis 插件的原理？ | 动态代理；拦截 Executor/StatementHandler/ParameterHandler/ResultSetHandler |

### 工程问题

| # | 题目 | 关键要点 |
|---|------|----------|
| 19 | 如何使用 MyBatis 避免 SQL 注入？ | 使用 `#{}` 而非 `${}` |
| 20 | useGeneratedKeys 和 selectKey 的区别？ | useGeneratedKeys：自增主键回填；selectKey：自定义主键生成策略 |

---

## 附录：面试题统计

| 模块 | 题目数量 | 来源 |
|------|----------|------|
| Java 基础 | 25 | JavaGuide - java-basic-questions-01 |
| 面向对象 | 19 | JavaGuide - java-basic-questions-02 |
| Java 并发 | 32 | JavaGuide - java-concurrent-questions-01/02 |
| MySQL | 35 | JavaGuide - mysql-questions-01 |
| Spring | 27 | JavaGuide - spring-knowledge-and-questions-summary |
| Redis | 35 | JavaGuide - redis-questions-01/02 |
| MyBatis | 20 | JavaGuide - mybatis-interview |
| **合计** | **193** | |

---

> **数据来源**：所有面试题均整理自 [JavaGuide](https://javaguide.cn/) 开源项目
> 
> **GitHub**：https://github.com/Snailclimb/JavaGuide
> 
> **使用建议**：
> 1. 建议配合 [学习资料收集](学习资料收集-Java后端方向.md) 中的网课视频系统学习
> 2. 面试前重点复习每模块的基础题
> 3. 准备项目经验时，结合进阶题深入理解原理
