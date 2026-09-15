-- 把 interview_questions（面试题库，541 道，71 个技能，纯阅读用）批量转成 questions 里的
-- short_answer 题（能被 QuizService 自动判分），一次性大幅缓解"很多技能没有测评题"的问题——
-- key_points 本来就是"参考要点，不是标准答案原文"，跟 questions.reference_answer 是同一个东西，
-- 直接照抄，不用改写。
--
-- 对已经建好的库（比如团队共享主机上那份）和全新建库都适用，跑一次即可，重复跑也安全
-- （uk_skill_question_text 唯一约束 + INSERT IGNORE 天然去重）。
--
-- 用法：
--   mysql -u root -p skill_radar < database/migrate_seed_short_answer_from_interview_questions.sql
--
-- 注意：这只是"有什么就先用什么"的过渡方案——面试题的措辞、难度不是照着"考核这个技能掌握没掌握"
-- 设计的，跟人工专门编写的测评题不是一回事，后续团队要精编题库时，这批可以逐步替换/精简。

-- 全新建库用 schema.sql 时已经带了这个唯一键，这里只是给已有库补上；已存在则跳过。
SET @idx_exists = (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'questions' AND index_name = 'uk_skill_question_text'
);
SET @sql = IF(@idx_exists = 0,
  'ALTER TABLE questions ADD UNIQUE KEY uk_skill_question_text (skill_id, question_text(255))',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT IGNORE INTO questions (skill_id, type, question_text, reference_answer, difficulty)
SELECT skill_id, 'short_answer', question_text, key_points, 2
FROM interview_questions;
