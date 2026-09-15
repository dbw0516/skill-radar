-- 给已经建好的库（比如团队共享主机上那份）追加"错题本"支持，跑一次即可。
-- 新建库不需要跑这个文件——直接用 schema.sql 建库时就已经包含这张表了。
--
-- 用法：
--   mysql -u root -p skill_radar < database/migrate_add_wrong_questions.sql

CREATE TABLE IF NOT EXISTS wrong_questions (
  user_id            BIGINT UNSIGNED NOT NULL,
  question_id        BIGINT UNSIGNED NOT NULL,
  last_wrong_answer  TEXT COMMENT '最近一次答错时提交的内容，选择题存选项文字、填空/简答存原文，供回顾对照',
  wrong_count        INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '累计答错次数（含中间答对又答错的反复）',
  first_wrong_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_wrong_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, question_id),
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (question_id) REFERENCES questions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
