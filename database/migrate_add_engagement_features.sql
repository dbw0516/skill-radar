-- 给已经建好的库（比如团队共享主机上那份）追加以下功能所需的表/字段，跑一次即可（ADD COLUMN 不能重复跑）：
--   1) 技能内部的部分进度：记录用户读过哪些学习资料            → resource_views
--   2) 错题重练：区分正式测评和错题复习                        → quiz_attempts.kind
--   3) 错题间隔复习（1 天 / 3 天 / 7 天）                      → wrong_questions.review_stage / next_review_at
--   4) AI 模拟面试                                            → interview_sessions
-- 新建库不需要跑这个文件——直接用 schema.sql 建库时就已经包含了。
--
-- 用法：
--   mysql -u root -p skill_radar < database/migrate_add_engagement_features.sql

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS resource_views (
  user_id      BIGINT UNSIGNED NOT NULL,
  resource_id  BIGINT UNSIGNED NOT NULL,
  viewed_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, resource_id),
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (resource_id) REFERENCES learning_resources(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE quiz_attempts
  ADD COLUMN kind ENUM('quiz','review') NOT NULL DEFAULT 'quiz'
      COMMENT 'quiz = 正式测评（可触发 quiz_verified）；review = 错题复习（只计入活跃天数，不认证技能）'
      AFTER passed;

-- last_wrong_at 原来带 ON UPDATE CURRENT_TIMESTAMP：现在答对时也要更新这一行（推进复习阶段），
-- 不去掉的话"最近一次答错时间"会被答对的那次覆盖。改成由代码在答错时显式写入。
ALTER TABLE wrong_questions
  MODIFY COLUMN last_wrong_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ADD COLUMN review_stage TINYINT UNSIGNED NOT NULL DEFAULT 0
      COMMENT '间隔复习阶段：答错归 0；到期后答对一次 +1；第 3 次到期答对即移出错题本',
  ADD COLUMN next_review_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
      COMMENT '下次该复习的时间：答错后 +1 天，之后依次 +3 天、+7 天';

CREATE TABLE IF NOT EXISTS interview_sessions (
  id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id          BIGINT UNSIGNED NOT NULL,
  category_id      BIGINT UNSIGNED NOT NULL,
  question_ids     JSON NOT NULL COMMENT '本场抽中的 interview_questions.id，按提问顺序',
  current_index    TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '正在问第几题（从 0 开始）',
  follow_up_used   BOOLEAN NOT NULL DEFAULT FALSE COMMENT '当前这道题是否已追问过，每题最多追问一次',
  transcript       JSON NOT NULL COMMENT '对话记录 [{role, content, questionIndex}]',
  question_scores  JSON COMMENT '每题 AI 评分 [{questionIndex, score(1~5), comment}]',
  status           ENUM('in_progress','finished') NOT NULL DEFAULT 'in_progress',
  overall_score    TINYINT UNSIGNED COMMENT '0~100，结束时 AI 给出',
  report           JSON COMMENT '结束时 AI 生成的总评 {summary, strengths[], improvements[]}',
  created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  finished_at      DATETIME,
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (category_id) REFERENCES job_categories(id),
  INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
