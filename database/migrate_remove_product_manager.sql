-- ============================================================
-- 删除「产品经理」岗位类别（已有数据库跑一次；新建库不用跑，
-- tools/import_tianchi.py 已经不导入这个类别）
--
-- 原因：技能词典里没有任何产品方向的技能，140 条产品经理 JD 里 113 条
-- 一个技术技能都抽不出来，job_skills 也一直是空的，差距分析/学习路径/测评
-- 对它都给不出东西。
--
-- 连带删除：这个类别下的招聘信息、它们的技能关联、用户对这些招聘信息的收藏、
-- 这个类别的 job_skills 和模拟面试记录（interview_sessions.category_id 不能为空）；
-- 把它设成目标岗位的用户，target_category_id 改回 NULL（需要重新选）。
-- 可重复执行：类别已经不在时 @cid 为 NULL，下面每条都不匹配任何行。
-- ============================================================

SET NAMES utf8mb4;

START TRANSACTION;

SET @cid := (SELECT id FROM job_categories WHERE name = '产品经理');

DELETE ps FROM posting_skills ps
JOIN job_postings p ON p.id = ps.posting_id
WHERE p.category_id = @cid;

DELETE f FROM favorites f
JOIN job_postings p ON p.id = f.posting_id
WHERE p.category_id = @cid;

DELETE FROM job_postings WHERE category_id = @cid;
DELETE FROM job_skills WHERE category_id = @cid;
DELETE FROM interview_sessions WHERE category_id = @cid;
UPDATE users SET target_category_id = NULL WHERE target_category_id = @cid;
DELETE FROM job_categories WHERE id = @cid;

COMMIT;

SELECT COUNT(*) AS remaining_product_manager_categories FROM job_categories WHERE name = '产品经理';
