-- ============================================================
-- 由 tools/import_learning_resources.py 合并生成
-- 来源：学习资料汇总.xlsx + 学习资料收集-Java后端方向.md（汇总表部分）
-- 已过滤：B站占位搜索链接、技能词典还未覆盖到的岗位方向（见脚本运行输出）
-- ============================================================

SET NAMES utf8mb4;

INSERT IGNORE INTO learning_resources (skill_id, title, url, type) VALUES ((SELECT id FROM skills WHERE name = 'Java基础' LIMIT 1), 'The Java™ Tutorials', 'https://docs.oracle.com/javase/tutorial/', 'doc');
INSERT IGNORE INTO learning_resources (skill_id, title, url, type) VALUES ((SELECT id FROM skills WHERE name = 'Java基础' LIMIT 1), 'Dev.java - Learn Java', 'https://dev.java/learn/', 'doc');
INSERT IGNORE INTO learning_resources (skill_id, title, url, type) VALUES ((SELECT id FROM skills WHERE name = 'Spring Boot' LIMIT 1), 'Spring Boot Reference Documentation', 'https://docs.spring.io/spring-boot/docs/current/reference/', 'doc');
INSERT IGNORE INTO learning_resources (skill_id, title, url, type) VALUES ((SELECT id FROM skills WHERE name = 'Spring Boot' LIMIT 1), 'Spring Guides', 'https://spring.io/guides', 'doc');
INSERT IGNORE INTO learning_resources (skill_id, title, url, type) VALUES ((SELECT id FROM skills WHERE name = 'MySQL' LIMIT 1), 'MySQL 8.0 Reference Manual', 'https://dev.mysql.com/doc/', 'doc');
INSERT IGNORE INTO learning_resources (skill_id, title, url, type) VALUES ((SELECT id FROM skills WHERE name = 'Java基础' LIMIT 1), '黑马程序员 Java 零基础入门', 'https://www.bilibili.com/video/BV1GJ411x7h7', 'video');
INSERT IGNORE INTO learning_resources (skill_id, title, url, type) VALUES ((SELECT id FROM skills WHERE name = 'Java基础' LIMIT 1), '尚硅谷 Java 零基础教程（宋红康）', 'https://www.bilibili.com/video/BV1Kb411W75N', 'video');
INSERT IGNORE INTO learning_resources (skill_id, title, url, type) VALUES ((SELECT id FROM skills WHERE name = '数据结构' LIMIT 1), 'LeetCode 刷题平台', 'https://leetcode.cn/', 'article');
INSERT IGNORE INTO learning_resources (skill_id, title, url, type) VALUES ((SELECT id FROM skills WHERE name = 'Spring Boot' LIMIT 1), '狂神说 SpringBoot 教程', 'https://www.bilibili.com/video/BV1PE411i7CV', 'video');
INSERT IGNORE INTO learning_resources (skill_id, title, url, type) VALUES ((SELECT id FROM skills WHERE name = 'Redis' LIMIT 1), 'Redis 官方文档', 'https://redis.io/docs/', 'doc');
INSERT IGNORE INTO learning_resources (skill_id, title, url, type) VALUES ((SELECT id FROM skills WHERE name = 'Linux基础' LIMIT 1), '鸟哥的 Linux 私房菜', 'https://linux.vbird.org/', 'article');
INSERT IGNORE INTO learning_resources (skill_id, title, url, type) VALUES ((SELECT id FROM skills WHERE name = 'Git' LIMIT 1), '狂神说 Git 入门教程', 'https://www.bilibili.com/video/BV1FE41157v7', 'video');
INSERT IGNORE INTO learning_resources (skill_id, title, url, type) VALUES ((SELECT id FROM skills WHERE name = 'Git' LIMIT 1), 'Git 官方文档', 'https://git-scm.com/doc', 'doc');
INSERT IGNORE INTO learning_resources (skill_id, title, url, type) VALUES ((SELECT id FROM skills WHERE name = 'MyBatis' LIMIT 1), 'MyBatis 官方文档', 'https://mybatis.org/mybatis-3/zh/index.html', 'doc');