-- ============================================================
-- 种子数据：Java 后端工程师试点方向
-- 对应设计文档中的"举个例子"——跑通一次完整闭环用的最小数据集
-- 团队按此格式继续补充到 100~150 个技能节点、30~50 条 JD
-- ============================================================

SET NAMES utf8mb4;

INSERT INTO majors (id, name) VALUES
  (1, '计算机科学与技术');

INSERT INTO job_categories (id, name, major_id, description, status) VALUES
  (1, 'Java后端工程师', 1, '试点岗位方向，用于打通"数据→差距分析→路径→测评回写"主链路', 'active');

-- 技能节点：与设计文档「学习路径怎么排」「岗位技能怎么提取」两节的例子一致
INSERT INTO skills (id, name, domain, aliases, difficulty, est_hours) VALUES
  (1, 'HTML/CSS',    '前端基础', '["html","css","网页布局"]', 1, 20),
  (2, 'Java基础',     '后端语言', '["java","java语言","java编程"]', 2, 60),
  (3, '面向对象',      '后端语言', '["oop","面向对象编程","面向对象设计"]', 2, 20),
  (4, '数据结构',      '算法能力', '["数据结构与算法","算法基础"]', 3, 40),
  (5, 'MySQL',        '数据库',  '["mysql数据库","关系型数据库"]', 2, 30),
  (6, 'Spring Boot',  '后端框架', '["springboot","spring boot框架"]', 3, 40),
  (7, 'Redis',        '数据库',  '["redis缓存","缓存"]', 2, 15),
  (8, 'Linux基础',    '工程工具', '["linux","linux操作","shell基础"]', 1, 15),
  (9, 'Git',          '工程工具', '["git版本管理","版本控制"]', 1, 8),
  (10,'MyBatis',      '后端框架', '["mybatis框架","mybatis-plus"]', 2, 15),
  (11,'Java并发',      '后端语言', '["并发编程","多线程","juc"]', 3, 25);

-- 技能依赖边：对应「学习路径怎么排」一节的示例 DAG
INSERT INTO skill_prereq (skill_id, prereq_skill_id) VALUES
  (3, 2),   -- 面向对象 依赖 Java基础
  (4, 3),   -- 数据结构 依赖 面向对象
  (6, 3),   -- Spring Boot 依赖 面向对象
  (6, 5),   -- Spring Boot 依赖 MySQL
  (7, 5),   -- Redis 依赖 MySQL
  (10, 2),  -- MyBatis 依赖 Java基础
  (10, 5),  -- MyBatis 依赖 MySQL
  (11, 3);  -- Java并发 依赖 面向对象

-- 岗位类别所需技能与权重：对应「权重怎么定」一节的示例比例
INSERT INTO job_skills (category_id, skill_id, weight) VALUES
  (1, 2, 0.90),  -- Java基础
  (1, 6, 0.75),  -- Spring Boot
  (1, 5, 0.45),  -- MySQL
  (1, 3, 0.70),
  (1, 4, 0.55),
  (1, 7, 0.30),
  (1, 8, 0.35),
  (1, 9, 0.40),
  (1, 10, 0.20);

-- 一条示例 JD：对应「岗位技能怎么提取」一节的样例文本
INSERT INTO job_postings (id, category_id, company_name, title, raw_text, status, posted_at) VALUES
  (1, 1, '示例科技有限公司', 'Java后端开发工程师',
   '负责后端服务的设计与开发，参与核心业务系统的架构设计。任职要求：熟练掌握 Java 语言，深入理解面向对象设计思想；熟悉 Spring Boot、MyBatis 等主流开发框架；熟悉 MySQL 数据库设计与调优，了解索引原理；了解 Redis 缓存机制，有实际项目经验优先；熟悉 Linux 基本操作，了解 Git 版本管理工具；具备良好的数据结构与算法基础。',
   'open', CURDATE());

-- 该条 JD 的抽取结果：词典命中 8 项已确认，MyBatis 为词典未命中、待人工确认
INSERT INTO posting_skills (posting_id, skill_id, source, confirmed) VALUES
  (1, 2, 'dict', TRUE),
  (1, 3, 'dict', TRUE),
  (1, 6, 'dict', TRUE),
  (1, 5, 'dict', TRUE),
  (1, 7, 'dict', TRUE),
  (1, 8, 'dict', TRUE),
  (1, 9, 'dict', TRUE),
  (1, 4, 'dict', TRUE),
  (1, 10, 'llm', FALSE);

-- 学习资源示例（团队后续按此格式补齐每个技能 2~3 条）
INSERT INTO learning_resources (skill_id, title, url, type) VALUES
  (2, 'Java 官方教程', 'https://docs.oracle.com/javase/tutorial/', 'doc'),
  (6, 'Spring Boot 官方指南', 'https://spring.io/guides', 'doc'),
  (5, 'MySQL 官方文档', 'https://dev.mysql.com/doc/', 'doc');

-- 题库示例（团队后续按此格式补齐每个技能 5~10 道）
INSERT INTO questions (skill_id, question_text, options, correct_index, difficulty) VALUES
  (2, 'Java 中，以下哪个关键字用于定义常量？',
     JSON_ARRAY('final', 'const', 'static', 'immutable'), 0, 1),
  (6, 'Spring Boot 中，用于标注一个类为 REST 控制器的注解是？',
     JSON_ARRAY('@Service', '@RestController', '@Repository', '@Configuration'), 1, 2),
  (5, 'MySQL 中，为提升查询速度，通常会为频繁查询的列添加什么？',
     JSON_ARRAY('触发器', '视图', '索引', '存储过程'), 2, 2);

-- 示例账号，仅供本地联调使用（密码占位，正式接入注册接口后删除本行）
INSERT INTO users (id, email, password_hash, nickname, major_id, target_category_id) VALUES
  (1, 'demo@example.com', '__replace_with_real_hash__', '演示账号', 1, 1);

-- 该用户已掌握 HTML/CSS（与目标岗位无关，用于演示"差距=目标−已掌握"只算相关技能）
INSERT INTO user_skills (user_id, skill_id, status) VALUES
  (1, 1, 'self_reported');
