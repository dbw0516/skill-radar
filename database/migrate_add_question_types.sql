-- 给已经建好的库（比如团队共享主机上那份）追加"填空题/简答题"支持，跑一次即可。
-- 新建库不需要跑这个文件——直接用 schema.sql 建库时就已经包含这些字段了。
--
-- 用法：
--   mysql -u root -p skill_radar < database/migrate_add_question_types.sql

ALTER TABLE questions
  ADD COLUMN type ENUM('single_choice','fill_blank','short_answer') NOT NULL DEFAULT 'single_choice' AFTER skill_id,
  MODIFY COLUMN options JSON NULL COMMENT '仅 single_choice，如 ["选项A","选项B","选项C","选项D"]',
  MODIFY COLUMN correct_index TINYINT UNSIGNED NULL COMMENT '仅 single_choice，正确选项在 options 中的下标',
  ADD COLUMN accepted_answers JSON NULL COMMENT '仅 fill_blank，可接受答案数组，如 ["索引","index"]' AFTER correct_index,
  ADD COLUMN reference_answer TEXT NULL COMMENT '仅 short_answer，参考答案/要点，不参与服务端判分' AFTER accepted_answers;

-- 已有的题都是选择题，上面 ADD COLUMN ... DEFAULT 'single_choice' 已经把它们标好类型，
-- 不需要再手动 UPDATE。
