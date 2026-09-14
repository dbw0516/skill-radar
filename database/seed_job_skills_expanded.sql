-- ============================================================
-- 由 tools/import_job_skills_expanded.py 生成
-- 给「除 Java 后端工程师以外」的岗位类别补 job_skills（岗位需要的技能+权重），
-- 这样技能词典扩充（seed_expanded_skills_and_questions.sql）新增的技能才能真正
-- 接入②差距分析 / ③学习路径引擎，而不是只是躺在 interview_questions 里。
-- 权重是人工判断的示例值（核心技能 0.7~0.9，次要 0.3~0.6，边缘/通用 0.2~0.3），
-- 跟 seed.sql 里 Java 后端那 9 行一个性质，不是真实统计出来的，说明见脚本头部。
-- 产品经理没有任何相关技能数据，这次没有覆盖，跳过。
-- ============================================================

SET NAMES utf8mb4;

-- ---------- Python开发工程师 ----------
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = 'Python开发工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'Python核心语法与内置类型' LIMIT 1), 0.85) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = 'Python开发工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'Python内存管理与对象模型' LIMIT 1), 0.55) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = 'Python开发工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'Python异常处理' LIMIT 1), 0.4) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = 'Python开发工程师' LIMIT 1), (SELECT id FROM skills WHERE name = '数据结构' LIMIT 1), 0.4) ON DUPLICATE KEY UPDATE weight = VALUES(weight);  -- 复用 Java 后端方向的技能
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = 'Python开发工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'Python正则表达式' LIMIT 1), 0.35) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = 'Python开发工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'Python网络编程' LIMIT 1), 0.35) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = 'Python开发工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'Python文件与编码处理' LIMIT 1), 0.3) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = 'Python开发工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'MySQL' LIMIT 1), 0.45) ON DUPLICATE KEY UPDATE weight = VALUES(weight);  -- 复用
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = 'Python开发工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'Git' LIMIT 1), 0.3) ON DUPLICATE KEY UPDATE weight = VALUES(weight);  -- 复用

-- ---------- 前端开发工程师 ----------
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '前端开发工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'JavaScript基础' LIMIT 1), 0.9) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '前端开发工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'HTML/CSS' LIMIT 1), 0.85) ON DUPLICATE KEY UPDATE weight = VALUES(weight);  -- 复用（seed.sql 里原本就有）
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '前端开发工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'DOM与浏览器API' LIMIT 1), 0.6) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '前端开发工程师' LIMIT 1), (SELECT id FROM skills WHERE name = '前端性能优化' LIMIT 1), 0.5) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '前端开发工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'Git' LIMIT 1), 0.3) ON DUPLICATE KEY UPDATE weight = VALUES(weight);  -- 复用

-- ---------- 大数据开发工程师 ----------
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '大数据开发工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'Spark计算与性能调优' LIMIT 1), 0.85) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '大数据开发工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'Hadoop与HDFS基础' LIMIT 1), 0.7) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '大数据开发工程师' LIMIT 1), (SELECT id FROM skills WHERE name = '消息队列与流处理' LIMIT 1), 0.65) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '大数据开发工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'Hive与HBase' LIMIT 1), 0.6) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '大数据开发工程师' LIMIT 1), (SELECT id FROM skills WHERE name = '数据仓库与数据质量' LIMIT 1), 0.55) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '大数据开发工程师' LIMIT 1), (SELECT id FROM skills WHERE name = '大数据架构设计' LIMIT 1), 0.45) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '大数据开发工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'Python核心语法与内置类型' LIMIT 1), 0.4) ON DUPLICATE KEY UPDATE weight = VALUES(weight);  -- 复用
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '大数据开发工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'MySQL' LIMIT 1), 0.35) ON DUPLICATE KEY UPDATE weight = VALUES(weight);  -- 复用

-- ---------- 数据分析师 ----------
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '数据分析师' LIMIT 1), (SELECT id FROM skills WHERE name = '数据清洗与描述性统计' LIMIT 1), 0.7) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '数据分析师' LIMIT 1), (SELECT id FROM skills WHERE name = 'Excel数据分析' LIMIT 1), 0.75) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '数据分析师' LIMIT 1), (SELECT id FROM skills WHERE name = 'MySQL' LIMIT 1), 0.65) ON DUPLICATE KEY UPDATE weight = VALUES(weight);  -- 复用
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '数据分析师' LIMIT 1), (SELECT id FROM skills WHERE name = 'pandas数据处理' LIMIT 1), 0.6) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '数据分析师' LIMIT 1), (SELECT id FROM skills WHERE name = '数据可视化' LIMIT 1), 0.55) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '数据分析师' LIMIT 1), (SELECT id FROM skills WHERE name = '统计建模与回归分析' LIMIT 1), 0.45) ON DUPLICATE KEY UPDATE weight = VALUES(weight);

-- ---------- 数据库管理员 ----------
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '数据库管理员' LIMIT 1), (SELECT id FROM skills WHERE name = '数据库性能调优' LIMIT 1), 0.9) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '数据库管理员' LIMIT 1), (SELECT id FROM skills WHERE name = 'MySQL' LIMIT 1), 0.8) ON DUPLICATE KEY UPDATE weight = VALUES(weight);  -- 复用
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '数据库管理员' LIMIT 1), (SELECT id FROM skills WHERE name = '数据库高可用与容灾' LIMIT 1), 0.7) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '数据库管理员' LIMIT 1), (SELECT id FROM skills WHERE name = '数据库设计与建模' LIMIT 1), 0.65) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '数据库管理员' LIMIT 1), (SELECT id FROM skills WHERE name = '数据库事务与一致性' LIMIT 1), 0.6) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '数据库管理员' LIMIT 1), (SELECT id FROM skills WHERE name = '数据库安全管理' LIMIT 1), 0.55) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '数据库管理员' LIMIT 1), (SELECT id FROM skills WHERE name = '数据库迁移与运维协作' LIMIT 1), 0.45) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '数据库管理员' LIMIT 1), (SELECT id FROM skills WHERE name = 'Linux基础' LIMIT 1), 0.4) ON DUPLICATE KEY UPDATE weight = VALUES(weight);  -- 复用

-- ---------- 测试工程师 ----------
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '测试工程师' LIMIT 1), (SELECT id FROM skills WHERE name = '软件测试方法与工具' LIMIT 1), 0.8) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '测试工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'Git' LIMIT 1), 0.35) ON DUPLICATE KEY UPDATE weight = VALUES(weight);  -- 复用
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '测试工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'MySQL' LIMIT 1), 0.3) ON DUPLICATE KEY UPDATE weight = VALUES(weight);  -- 复用
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '测试工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'Linux基础' LIMIT 1), 0.3) ON DUPLICATE KEY UPDATE weight = VALUES(weight);  -- 复用
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '测试工程师' LIMIT 1), (SELECT id FROM skills WHERE name = '进程与线程基础' LIMIT 1), 0.25) ON DUPLICATE KEY UPDATE weight = VALUES(weight);  -- 复用

-- ---------- 移动开发工程师 ----------
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '移动开发工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'Android生命周期与状态管理' LIMIT 1), 0.7) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '移动开发工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'Android UI与性能优化' LIMIT 1), 0.65) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '移动开发工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'Android组件与Intent' LIMIT 1), 0.6) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '移动开发工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'Android数据存储方式' LIMIT 1), 0.55) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '移动开发工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'Android消息机制' LIMIT 1), 0.55) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '移动开发工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'Android内存管理与调试' LIMIT 1), 0.5) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '移动开发工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'iOS设计模式' LIMIT 1), 0.55) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '移动开发工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'iOS并发与UI基础' LIMIT 1), 0.55) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '移动开发工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'Objective-C语言特性' LIMIT 1), 0.55) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '移动开发工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'iOS内存管理' LIMIT 1), 0.5) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '移动开发工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'iOS数据持久化' LIMIT 1), 0.4) ON DUPLICATE KEY UPDATE weight = VALUES(weight);

-- ---------- 算法/机器学习工程师 ----------
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '算法/机器学习工程师' LIMIT 1), (SELECT id FROM skills WHERE name = '数据结构' LIMIT 1), 0.85) ON DUPLICATE KEY UPDATE weight = VALUES(weight);  -- 复用，顶算法基础
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '算法/机器学习工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'Python核心语法与内置类型' LIMIT 1), 0.6) ON DUPLICATE KEY UPDATE weight = VALUES(weight);  -- 复用
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '算法/机器学习工程师' LIMIT 1), (SELECT id FROM skills WHERE name = '统计建模与回归分析' LIMIT 1), 0.55) ON DUPLICATE KEY UPDATE weight = VALUES(weight);  -- 复用数据分析方向的技能
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '算法/机器学习工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'pandas数据处理' LIMIT 1), 0.45) ON DUPLICATE KEY UPDATE weight = VALUES(weight);  -- 复用
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '算法/机器学习工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'Python内存管理与对象模型' LIMIT 1), 0.3) ON DUPLICATE KEY UPDATE weight = VALUES(weight);  -- 复用

-- ---------- 网络安全工程师 ----------
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '网络安全工程师' LIMIT 1), (SELECT id FROM skills WHERE name = '网络攻击类型与防御' LIMIT 1), 0.8) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '网络安全工程师' LIMIT 1), (SELECT id FROM skills WHERE name = '身份认证与访问控制' LIMIT 1), 0.75) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '网络安全工程师' LIMIT 1), (SELECT id FROM skills WHERE name = '安全事件应急响应与取证' LIMIT 1), 0.6) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '网络安全工程师' LIMIT 1), (SELECT id FROM skills WHERE name = '密码学基础' LIMIT 1), 0.55) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '网络安全工程师' LIMIT 1), (SELECT id FROM skills WHERE name = '安全合规与风险管理' LIMIT 1), 0.45) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '网络安全工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'Linux基础' LIMIT 1), 0.4) ON DUPLICATE KEY UPDATE weight = VALUES(weight);  -- 复用
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '网络安全工程师' LIMIT 1), (SELECT id FROM skills WHERE name = '物联网与新兴技术安全' LIMIT 1), 0.3) ON DUPLICATE KEY UPDATE weight = VALUES(weight);

-- ---------- 软件开发工程师（通用） ----------
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '软件开发工程师（通用）' LIMIT 1), (SELECT id FROM skills WHERE name = '数据结构' LIMIT 1), 0.55) ON DUPLICATE KEY UPDATE weight = VALUES(weight);  -- 复用
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '软件开发工程师（通用）' LIMIT 1), (SELECT id FROM skills WHERE name = 'Git' LIMIT 1), 0.5) ON DUPLICATE KEY UPDATE weight = VALUES(weight);  -- 复用
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '软件开发工程师（通用）' LIMIT 1), (SELECT id FROM skills WHERE name = '面向对象' LIMIT 1), 0.45) ON DUPLICATE KEY UPDATE weight = VALUES(weight);  -- 复用
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '软件开发工程师（通用）' LIMIT 1), (SELECT id FROM skills WHERE name = 'Java基础' LIMIT 1), 0.4) ON DUPLICATE KEY UPDATE weight = VALUES(weight);  -- 复用
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '软件开发工程师（通用）' LIMIT 1), (SELECT id FROM skills WHERE name = 'MySQL' LIMIT 1), 0.4) ON DUPLICATE KEY UPDATE weight = VALUES(weight);  -- 复用
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '软件开发工程师（通用）' LIMIT 1), (SELECT id FROM skills WHERE name = 'Linux基础' LIMIT 1), 0.4) ON DUPLICATE KEY UPDATE weight = VALUES(weight);  -- 复用
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '软件开发工程师（通用）' LIMIT 1), (SELECT id FROM skills WHERE name = '进程与线程基础' LIMIT 1), 0.35) ON DUPLICATE KEY UPDATE weight = VALUES(weight);  -- 复用
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '软件开发工程师（通用）' LIMIT 1), (SELECT id FROM skills WHERE name = 'JavaScript基础' LIMIT 1), 0.3) ON DUPLICATE KEY UPDATE weight = VALUES(weight);  -- 复用

-- ---------- 运维工程师/DevOps ----------
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '运维工程师/DevOps' LIMIT 1), (SELECT id FROM skills WHERE name = 'Linux基础' LIMIT 1), 0.85) ON DUPLICATE KEY UPDATE weight = VALUES(weight);  -- 复用
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '运维工程师/DevOps' LIMIT 1), (SELECT id FROM skills WHERE name = '服务器监控与告警' LIMIT 1), 0.75) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '运维工程师/DevOps' LIMIT 1), (SELECT id FROM skills WHERE name = '故障排查与应急响应' LIMIT 1), 0.65) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '运维工程师/DevOps' LIMIT 1), (SELECT id FROM skills WHERE name = '系统高可用与容灾' LIMIT 1), 0.7) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '运维工程师/DevOps' LIMIT 1), (SELECT id FROM skills WHERE name = '系统安全运维' LIMIT 1), 0.6) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '运维工程师/DevOps' LIMIT 1), (SELECT id FROM skills WHERE name = 'Git' LIMIT 1), 0.35) ON DUPLICATE KEY UPDATE weight = VALUES(weight);  -- 复用

-- ---------- Go后端工程师 ----------
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = 'Go后端工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'Go基础语法与数据类型' LIMIT 1), 0.85) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = 'Go后端工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'Go Channel与并发通信' LIMIT 1), 0.65) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = 'Go后端工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'Go调度器与GMP模型' LIMIT 1), 0.5) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = 'Go后端工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'Go切片数组与拷贝机制' LIMIT 1), 0.55) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = 'Go后端工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'Go锁与原子操作' LIMIT 1), 0.45) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = 'Go后端工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'MySQL' LIMIT 1), 0.45) ON DUPLICATE KEY UPDATE weight = VALUES(weight);  -- 复用
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = 'Go后端工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'Go内存管理机制' LIMIT 1), 0.35) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = 'Go后端工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'Linux基础' LIMIT 1), 0.35) ON DUPLICATE KEY UPDATE weight = VALUES(weight);  -- 复用
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = 'Go后端工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'Git' LIMIT 1), 0.3) ON DUPLICATE KEY UPDATE weight = VALUES(weight);  -- 复用

-- ---------- 全栈工程师 ----------
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '全栈工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'JavaScript基础' LIMIT 1), 0.6) ON DUPLICATE KEY UPDATE weight = VALUES(weight);  -- 通用判断，非文档内容
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '全栈工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'HTML/CSS' LIMIT 1), 0.55) ON DUPLICATE KEY UPDATE weight = VALUES(weight);  -- 通用判断，非文档内容，复用技能
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '全栈工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'MySQL' LIMIT 1), 0.55) ON DUPLICATE KEY UPDATE weight = VALUES(weight);  -- 通用判断，非文档内容，复用技能
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '全栈工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'Java基础' LIMIT 1), 0.4) ON DUPLICATE KEY UPDATE weight = VALUES(weight);  -- 通用判断，非文档内容，复用技能
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '全栈工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'Git' LIMIT 1), 0.4) ON DUPLICATE KEY UPDATE weight = VALUES(weight);  -- 通用判断，非文档内容，复用技能
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '全栈工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'DOM与浏览器API' LIMIT 1), 0.35) ON DUPLICATE KEY UPDATE weight = VALUES(weight);  -- 通用判断，非文档内容

-- ---------- 嵌入式开发工程师 ----------
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '嵌入式开发工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'Linux基础' LIMIT 1), 0.6) ON DUPLICATE KEY UPDATE weight = VALUES(weight);  -- 通用判断，非文档内容，复用技能
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '嵌入式开发工程师' LIMIT 1), (SELECT id FROM skills WHERE name = '进程与线程基础' LIMIT 1), 0.55) ON DUPLICATE KEY UPDATE weight = VALUES(weight);  -- 通用判断，非文档内容，复用技能
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '嵌入式开发工程师' LIMIT 1), (SELECT id FROM skills WHERE name = 'Git' LIMIT 1), 0.25) ON DUPLICATE KEY UPDATE weight = VALUES(weight);  -- 通用判断，非文档内容，复用技能

-- ---------- 游戏开发工程师 ----------
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '游戏开发工程师' LIMIT 1), (SELECT id FROM skills WHERE name = '游戏引擎渲染与性能优化' LIMIT 1), 0.8) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '游戏开发工程师' LIMIT 1), (SELECT id FROM skills WHERE name = '游戏物理与碰撞检测' LIMIT 1), 0.55) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '游戏开发工程师' LIMIT 1), (SELECT id FROM skills WHERE name = '游戏动画与特效系统' LIMIT 1), 0.55) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '游戏开发工程师' LIMIT 1), (SELECT id FROM skills WHERE name = '游戏脚本与并发编程' LIMIT 1), 0.5) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '游戏开发工程师' LIMIT 1), (SELECT id FROM skills WHERE name = '游戏网络同步' LIMIT 1), 0.45) ON DUPLICATE KEY UPDATE weight = VALUES(weight);
INSERT INTO job_skills (category_id, skill_id, weight) VALUES ((SELECT id FROM job_categories WHERE name = '游戏开发工程师' LIMIT 1), (SELECT id FROM skills WHERE name = '进程与线程基础' LIMIT 1), 0.3) ON DUPLICATE KEY UPDATE weight = VALUES(weight);  -- 复用

