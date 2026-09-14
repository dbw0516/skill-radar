-- ============================================================
-- 由 tools/import_skill_prereq_expanded.py 生成
-- 给技能词典扩充新增的 62 个技能补依赖边，让③学习路径引擎能对这些方向做
-- 真正的拓扑分层，不再是每个岗位类别都挤在一个大阶段里。
-- 跟 skill_prereq(skill_id, prereq_skill_id) 一样的语义：skill 依赖 prereq，
-- 即先学 prereq 再学 skill。用 INSERT IGNORE 保证重复执行安全
-- （skill_prereq 主键是 (skill_id, prereq_skill_id) 复合唯一约束）。
-- ============================================================

SET NAMES utf8mb4;

INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = 'Go切片数组与拷贝机制' LIMIT 1), (SELECT id FROM skills WHERE name = 'Go基础语法与数据类型' LIMIT 1));  -- Go切片数组与拷贝机制 依赖 Go基础语法与数据类型
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = 'Go Map实现原理' LIMIT 1), (SELECT id FROM skills WHERE name = 'Go基础语法与数据类型' LIMIT 1));  -- Go Map实现原理 依赖 Go基础语法与数据类型
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = 'Go Channel与并发通信' LIMIT 1), (SELECT id FROM skills WHERE name = 'Go基础语法与数据类型' LIMIT 1));  -- Go Channel与并发通信 依赖 Go基础语法与数据类型
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = 'Go Channel与并发通信' LIMIT 1), (SELECT id FROM skills WHERE name = '进程与线程基础' LIMIT 1));  -- Go Channel与并发通信 依赖 进程与线程基础
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = 'Go锁与原子操作' LIMIT 1), (SELECT id FROM skills WHERE name = 'Go Channel与并发通信' LIMIT 1));  -- Go锁与原子操作 依赖 Go Channel与并发通信
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = 'Go调度器与GMP模型' LIMIT 1), (SELECT id FROM skills WHERE name = 'Go锁与原子操作' LIMIT 1));  -- Go调度器与GMP模型 依赖 Go锁与原子操作
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = 'Go内存管理机制' LIMIT 1), (SELECT id FROM skills WHERE name = 'Go切片数组与拷贝机制' LIMIT 1));  -- Go内存管理机制 依赖 Go切片数组与拷贝机制
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = 'Python内存管理与对象模型' LIMIT 1), (SELECT id FROM skills WHERE name = 'Python核心语法与内置类型' LIMIT 1));  -- Python内存管理与对象模型 依赖 Python核心语法与内置类型
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = 'Python异常处理' LIMIT 1), (SELECT id FROM skills WHERE name = 'Python核心语法与内置类型' LIMIT 1));  -- Python异常处理 依赖 Python核心语法与内置类型
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = 'Python正则表达式' LIMIT 1), (SELECT id FROM skills WHERE name = 'Python核心语法与内置类型' LIMIT 1));  -- Python正则表达式 依赖 Python核心语法与内置类型
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = 'Python网络编程' LIMIT 1), (SELECT id FROM skills WHERE name = 'Python异常处理' LIMIT 1));  -- Python网络编程 依赖 Python异常处理
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = 'Python文件与编码处理' LIMIT 1), (SELECT id FROM skills WHERE name = 'Python核心语法与内置类型' LIMIT 1));  -- Python文件与编码处理 依赖 Python核心语法与内置类型
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = 'JavaScript基础' LIMIT 1), (SELECT id FROM skills WHERE name = 'HTML/CSS' LIMIT 1));  -- JavaScript基础 依赖 HTML/CSS
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = 'DOM与浏览器API' LIMIT 1), (SELECT id FROM skills WHERE name = 'JavaScript基础' LIMIT 1));  -- DOM与浏览器API 依赖 JavaScript基础
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = '前端性能优化' LIMIT 1), (SELECT id FROM skills WHERE name = 'DOM与浏览器API' LIMIT 1));  -- 前端性能优化 依赖 DOM与浏览器API
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = 'Hive与HBase' LIMIT 1), (SELECT id FROM skills WHERE name = 'Hadoop与HDFS基础' LIMIT 1));  -- Hive与HBase 依赖 Hadoop与HDFS基础
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = 'Spark计算与性能调优' LIMIT 1), (SELECT id FROM skills WHERE name = 'Hadoop与HDFS基础' LIMIT 1));  -- Spark计算与性能调优 依赖 Hadoop与HDFS基础
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = '消息队列与流处理' LIMIT 1), (SELECT id FROM skills WHERE name = 'Spark计算与性能调优' LIMIT 1));  -- 消息队列与流处理 依赖 Spark计算与性能调优
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = '数据仓库与数据质量' LIMIT 1), (SELECT id FROM skills WHERE name = 'Hive与HBase' LIMIT 1));  -- 数据仓库与数据质量 依赖 Hive与HBase
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = '大数据架构设计' LIMIT 1), (SELECT id FROM skills WHERE name = 'Spark计算与性能调优' LIMIT 1));  -- 大数据架构设计 依赖 Spark计算与性能调优
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = '大数据架构设计' LIMIT 1), (SELECT id FROM skills WHERE name = '数据仓库与数据质量' LIMIT 1));  -- 大数据架构设计 依赖 数据仓库与数据质量
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = 'Android生命周期与状态管理' LIMIT 1), (SELECT id FROM skills WHERE name = 'Android组件与Intent' LIMIT 1));  -- Android生命周期与状态管理 依赖 Android组件与Intent
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = 'Android数据存储方式' LIMIT 1), (SELECT id FROM skills WHERE name = 'Android组件与Intent' LIMIT 1));  -- Android数据存储方式 依赖 Android组件与Intent
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = 'Android消息机制' LIMIT 1), (SELECT id FROM skills WHERE name = 'Android生命周期与状态管理' LIMIT 1));  -- Android消息机制 依赖 Android生命周期与状态管理
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = 'Android UI与性能优化' LIMIT 1), (SELECT id FROM skills WHERE name = 'Android生命周期与状态管理' LIMIT 1));  -- Android UI与性能优化 依赖 Android生命周期与状态管理
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = 'Android内存管理与调试' LIMIT 1), (SELECT id FROM skills WHERE name = 'Android UI与性能优化' LIMIT 1));  -- Android内存管理与调试 依赖 Android UI与性能优化
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = 'iOS内存管理' LIMIT 1), (SELECT id FROM skills WHERE name = 'Objective-C语言特性' LIMIT 1));  -- iOS内存管理 依赖 Objective-C语言特性
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = 'iOS设计模式' LIMIT 1), (SELECT id FROM skills WHERE name = 'Objective-C语言特性' LIMIT 1));  -- iOS设计模式 依赖 Objective-C语言特性
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = 'iOS并发与UI基础' LIMIT 1), (SELECT id FROM skills WHERE name = 'iOS设计模式' LIMIT 1));  -- iOS并发与UI基础 依赖 iOS设计模式
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = 'iOS并发与UI基础' LIMIT 1), (SELECT id FROM skills WHERE name = '进程与线程基础' LIMIT 1));  -- iOS并发与UI基础 依赖 进程与线程基础
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = 'iOS数据持久化' LIMIT 1), (SELECT id FROM skills WHERE name = 'iOS设计模式' LIMIT 1));  -- iOS数据持久化 依赖 iOS设计模式
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = '服务器监控与告警' LIMIT 1), (SELECT id FROM skills WHERE name = 'Linux基础' LIMIT 1));  -- 服务器监控与告警 依赖 Linux基础
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = '系统安全运维' LIMIT 1), (SELECT id FROM skills WHERE name = 'Linux基础' LIMIT 1));  -- 系统安全运维 依赖 Linux基础
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = '系统高可用与容灾' LIMIT 1), (SELECT id FROM skills WHERE name = '服务器监控与告警' LIMIT 1));  -- 系统高可用与容灾 依赖 服务器监控与告警
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = '故障排查与应急响应' LIMIT 1), (SELECT id FROM skills WHERE name = '服务器监控与告警' LIMIT 1));  -- 故障排查与应急响应 依赖 服务器监控与告警
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = '身份认证与访问控制' LIMIT 1), (SELECT id FROM skills WHERE name = '密码学基础' LIMIT 1));  -- 身份认证与访问控制 依赖 密码学基础
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = '网络攻击类型与防御' LIMIT 1), (SELECT id FROM skills WHERE name = '身份认证与访问控制' LIMIT 1));  -- 网络攻击类型与防御 依赖 身份认证与访问控制
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = '安全事件应急响应与取证' LIMIT 1), (SELECT id FROM skills WHERE name = '网络攻击类型与防御' LIMIT 1));  -- 安全事件应急响应与取证 依赖 网络攻击类型与防御
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = '安全合规与风险管理' LIMIT 1), (SELECT id FROM skills WHERE name = '网络攻击类型与防御' LIMIT 1));  -- 安全合规与风险管理 依赖 网络攻击类型与防御
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = '物联网与新兴技术安全' LIMIT 1), (SELECT id FROM skills WHERE name = '网络攻击类型与防御' LIMIT 1));  -- 物联网与新兴技术安全 依赖 网络攻击类型与防御
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = '数据清洗与描述性统计' LIMIT 1), (SELECT id FROM skills WHERE name = 'Excel数据分析' LIMIT 1));  -- 数据清洗与描述性统计 依赖 Excel数据分析
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = '数据可视化' LIMIT 1), (SELECT id FROM skills WHERE name = '数据清洗与描述性统计' LIMIT 1));  -- 数据可视化 依赖 数据清洗与描述性统计
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = 'pandas数据处理' LIMIT 1), (SELECT id FROM skills WHERE name = 'Python核心语法与内置类型' LIMIT 1));  -- pandas数据处理 依赖 Python核心语法与内置类型
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = '统计建模与回归分析' LIMIT 1), (SELECT id FROM skills WHERE name = 'pandas数据处理' LIMIT 1));  -- 统计建模与回归分析 依赖 pandas数据处理
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = '统计建模与回归分析' LIMIT 1), (SELECT id FROM skills WHERE name = '数据清洗与描述性统计' LIMIT 1));  -- 统计建模与回归分析 依赖 数据清洗与描述性统计
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = '数据库设计与建模' LIMIT 1), (SELECT id FROM skills WHERE name = 'MySQL' LIMIT 1));  -- 数据库设计与建模 依赖 MySQL
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = '数据库事务与一致性' LIMIT 1), (SELECT id FROM skills WHERE name = '数据库设计与建模' LIMIT 1));  -- 数据库事务与一致性 依赖 数据库设计与建模
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = '数据库性能调优' LIMIT 1), (SELECT id FROM skills WHERE name = '数据库事务与一致性' LIMIT 1));  -- 数据库性能调优 依赖 数据库事务与一致性
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = '数据库高可用与容灾' LIMIT 1), (SELECT id FROM skills WHERE name = '数据库性能调优' LIMIT 1));  -- 数据库高可用与容灾 依赖 数据库性能调优
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = '数据库安全管理' LIMIT 1), (SELECT id FROM skills WHERE name = '数据库设计与建模' LIMIT 1));  -- 数据库安全管理 依赖 数据库设计与建模
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = '数据库迁移与运维协作' LIMIT 1), (SELECT id FROM skills WHERE name = '数据库高可用与容灾' LIMIT 1));  -- 数据库迁移与运维协作 依赖 数据库高可用与容灾
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = '游戏物理与碰撞检测' LIMIT 1), (SELECT id FROM skills WHERE name = '游戏引擎渲染与性能优化' LIMIT 1));  -- 游戏物理与碰撞检测 依赖 游戏引擎渲染与性能优化
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = '游戏动画与特效系统' LIMIT 1), (SELECT id FROM skills WHERE name = '游戏引擎渲染与性能优化' LIMIT 1));  -- 游戏动画与特效系统 依赖 游戏引擎渲染与性能优化
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = '游戏网络同步' LIMIT 1), (SELECT id FROM skills WHERE name = '游戏引擎渲染与性能优化' LIMIT 1));  -- 游戏网络同步 依赖 游戏引擎渲染与性能优化
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = '游戏脚本与并发编程' LIMIT 1), (SELECT id FROM skills WHERE name = '游戏引擎渲染与性能优化' LIMIT 1));  -- 游戏脚本与并发编程 依赖 游戏引擎渲染与性能优化
INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ((SELECT id FROM skills WHERE name = '游戏脚本与并发编程' LIMIT 1), (SELECT id FROM skills WHERE name = '进程与线程基础' LIMIT 1));  -- 游戏脚本与并发编程 依赖 进程与线程基础
