-- ============================================================
-- 就业技能雷达 · 数据库结构（MySQL 8.0+）
-- 与设计文档《就业技能雷达》技术方案一一对应：
--   岗位库 → job_categories / job_postings / job_skills / posting_skills
--   技能图谱 → skills / skill_prereq
--   用户技能画像 → users / user_skills
--   学习资源库 + 题库 → learning_resources / questions / quiz_attempts / interview_questions
-- ============================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------
-- 专业：用户注册时选择，决定默认推荐哪些岗位类别
-- ---------------------------------------------------------
CREATE TABLE majors (
  id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(64) NOT NULL COMMENT '专业名称，如"计算机科学与技术"',
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------
-- 岗位类别（稳定层）：例如"Java 后端工程师"
-- 技能要求是从多条 job_postings 聚合出来的，不随单条招聘信息的关闭而失效
-- ---------------------------------------------------------
CREATE TABLE job_categories (
  id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(64) NOT NULL UNIQUE COMMENT '岗位类别名称，如"Java后端工程师"；唯一约束保证多批数据导入时按名字自动合并，不会同名类别插出两条',
  major_id    BIGINT UNSIGNED COMMENT '主要对应的专业，用于①岗位推荐引擎的匹配',
  description VARCHAR(255),
  status      ENUM('active','pending') NOT NULL DEFAULT 'active'
              COMMENT 'pending = 由「待归类」JD 积累但团队尚未正式确认的类别',
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (major_id) REFERENCES majors(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------
-- 具体招聘信息（易变层）：某公司某条 JD
-- 关闭只改 status，不删除——历史数据仍用于统计该类别的技能出现频率
-- ---------------------------------------------------------
CREATE TABLE job_postings (
  id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  category_id   BIGINT UNSIGNED NULL COMMENT '为空表示尚未匹配到已有类别（待归类）',
  company_name  VARCHAR(128) NOT NULL,
  title         VARCHAR(128) NOT NULL,
  location      VARCHAR(64) COMMENT '工作地点，展示用，不参与技能抽取',
  salary_text   VARCHAR(64) COMMENT '原始薪资区间文本（如"0.8-1万/月"），先存文本，暂不做结构化解析',
  source_url    VARCHAR(512),
  raw_text      TEXT NOT NULL COMMENT '原始 JD 全文，技能抽取的数据源头',
  status        ENUM('open','closed','pending_category') NOT NULL DEFAULT 'open',
  posted_at     DATE COMMENT '原始数据若只有"月-日"没有年份，导入时按采集年份补全，可能不准，仅供参考',
  closed_at     DATE,
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (category_id) REFERENCES job_categories(id),
  INDEX idx_category_status (category_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------
-- 技能节点：技能图谱的顶点，控制在 100～150 个左右，人工维护
-- ---------------------------------------------------------
CREATE TABLE skills (
  id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(64) NOT NULL UNIQUE COMMENT '技能标准名称，如"Spring Boot"',
  domain      VARCHAR(32) NOT NULL COMMENT '归类大类，用于雷达图 5 个维度左右，如"后端框架"',
  aliases     JSON COMMENT '常见别名数组，供词典匹配抽取用，如 ["SpringBoot","spring boot框架"]',
  difficulty  TINYINT UNSIGNED DEFAULT 2 COMMENT '1~5，用于同层排序/展示',
  est_hours   SMALLINT UNSIGNED COMMENT '预估学习时长（小时），用于路径排序与展示',
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------
-- 技能依赖边：③学习路径规划引擎做拓扑排序的依据
-- (skill_id 依赖 prereq_skill_id，即需要先学 prereq 再学 skill)
-- ---------------------------------------------------------
CREATE TABLE skill_prereq (
  skill_id        BIGINT UNSIGNED NOT NULL,
  prereq_skill_id BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (skill_id, prereq_skill_id),
  FOREIGN KEY (skill_id) REFERENCES skills(id),
  FOREIGN KEY (prereq_skill_id) REFERENCES skills(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------
-- 单条 JD 的技能抽取结果（原始层）：词典命中 / AI 建议 / 人工确认
-- ---------------------------------------------------------
CREATE TABLE posting_skills (
  posting_id  BIGINT UNSIGNED NOT NULL,
  skill_id    BIGINT UNSIGNED NOT NULL,
  source      ENUM('dict','llm','manual') NOT NULL DEFAULT 'dict',
  confirmed   BOOLEAN NOT NULL DEFAULT FALSE COMMENT '人工复核确认后才计入类别权重统计',
  PRIMARY KEY (posting_id, skill_id),
  FOREIGN KEY (posting_id) REFERENCES job_postings(id),
  FOREIGN KEY (skill_id) REFERENCES skills(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------
-- 岗位类别所需技能（聚合层）：weight = 该类别下提及该技能的 JD 占比
-- 由 posting_skills 定期重新统计生成，不需要用户请求时实时计算
-- ---------------------------------------------------------
CREATE TABLE job_skills (
  category_id  BIGINT UNSIGNED NOT NULL,
  skill_id     BIGINT UNSIGNED NOT NULL,
  weight       DECIMAL(4,3) NOT NULL DEFAULT 0 COMMENT '0~1，同类 JD 中的提及比例',
  updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (category_id, skill_id),
  FOREIGN KEY (category_id) REFERENCES job_categories(id),
  FOREIGN KEY (skill_id) REFERENCES skills(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------
-- 用户
-- ---------------------------------------------------------
CREATE TABLE users (
  id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  email               VARCHAR(128) NOT NULL UNIQUE,
  password_hash       VARCHAR(255) NOT NULL,
  nickname            VARCHAR(64),
  major_id            BIGINT UNSIGNED,
  target_category_id  BIGINT UNSIGNED COMMENT '当前目标岗位类别，驱动②差距分析引擎',
  location             VARCHAR(64) COMMENT '所在地区，如"北京-朝阳区"，格式与 job_postings.location 对齐方便匹配',
  target_location      VARCHAR(64) COMMENT '意向就业地区，为空表示不限；①岗位推荐引擎按它排序/过滤',
  created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (major_id) REFERENCES majors(id),
  FOREIGN KEY (target_category_id) REFERENCES job_categories(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------
-- 用户技能画像（细粒度层）：驱动②差距分析、③路径规划
-- ---------------------------------------------------------
CREATE TABLE user_skills (
  user_id     BIGINT UNSIGNED NOT NULL,
  skill_id    BIGINT UNSIGNED NOT NULL,
  status      ENUM('self_reported','quiz_verified') NOT NULL DEFAULT 'self_reported'
              COMMENT '区分自评与测评认证，差距分析优先信任 quiz_verified',
  updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, skill_id),
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (skill_id) REFERENCES skills(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------
-- 学习资源库：以外部链接为主，按技能打标签
-- ---------------------------------------------------------
CREATE TABLE learning_resources (
  id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  skill_id    BIGINT UNSIGNED NOT NULL,
  title       VARCHAR(128) NOT NULL,
  url         VARCHAR(512) NOT NULL,
  type        ENUM('article','video','course','doc') NOT NULL DEFAULT 'article',
  is_stale    BOOLEAN NOT NULL DEFAULT FALSE COMMENT '用户举报"内容过时/链接失效"后置 true，进待处理队列',
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (skill_id) REFERENCES skills(id),
  UNIQUE KEY uk_skill_url (skill_id, url(255)) COMMENT '同一技能下同一链接只留一条，多批次导入用 INSERT IGNORE 天然去重'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------
-- 题库：目标每个技能 5~10 道。三种题型共用一张表，各自只填自己用得到的列，其余留空：
--   single_choice 选择题：options + correct_index，服务端精确判分
--   fill_blank    填空题：accepted_answers，服务端归一化（去空格+小写）后
--                 精确匹配任一候选答案即算对
--   short_answer  简答题：reference_answer，开放式答案没法用字符串匹配，
--                 交给 ShortAnswerGradingService 调用本地 AI 判分
-- 单个技能题量差异很大（人工编写的少，从 interview_questions 批量转来的多），
-- 一次测评显示多少道由 QuizService 抽样封顶，不是这张表的问题。
-- uk_skill_question_text：同一技能下同一题目只留一条，批量导入脚本用 INSERT IGNORE 天然去重。
-- ---------------------------------------------------------
CREATE TABLE questions (
  id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  skill_id          BIGINT UNSIGNED NOT NULL,
  type              ENUM('single_choice','fill_blank','short_answer') NOT NULL DEFAULT 'single_choice',
  question_text     TEXT NOT NULL,
  options           JSON COMMENT '仅 single_choice，如 ["选项A","选项B","选项C","选项D"]',
  correct_index     TINYINT UNSIGNED COMMENT '仅 single_choice，正确选项在 options 中的下标',
  accepted_answers  JSON COMMENT '仅 fill_blank，可接受答案数组，如 ["索引","index"]',
  reference_answer  TEXT COMMENT '仅 short_answer，参考答案/要点，不参与服务端判分',
  difficulty        TINYINT UNSIGNED DEFAULT 2,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (skill_id) REFERENCES skills(id),
  UNIQUE KEY uk_skill_question_text (skill_id, question_text(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------
-- 面试题库：开放式问答 + 要点参考，供"面试准备"阅读用，不参与自动判分。
-- 和 questions（选择题、可自动判分）是两回事，别混用：
-- questions 驱动④的在线测评闭环；interview_questions 只是学习资料的一种补充形式。
-- ---------------------------------------------------------
CREATE TABLE interview_questions (
  id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  skill_id      BIGINT UNSIGNED NOT NULL,
  question_text TEXT NOT NULL,
  key_points    TEXT COMMENT '参考要点/答案提示，不是标准答案原文',
  source        VARCHAR(128) COMMENT '来源出处，如 "JavaGuide"，尊重原作者标注来源',
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (skill_id) REFERENCES skills(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------
-- 测评记录：判分结果驱动 user_skills 回写（闭环的关键一步）
-- ---------------------------------------------------------
CREATE TABLE quiz_attempts (
  id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id       BIGINT UNSIGNED NOT NULL,
  skill_id      BIGINT UNSIGNED NOT NULL,
  correct_count TINYINT UNSIGNED NOT NULL,
  total_count   TINYINT UNSIGNED NOT NULL,
  passed        BOOLEAN NOT NULL COMMENT 'true 时触发 user_skills.status = quiz_verified',
  attempted_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (skill_id) REFERENCES skills(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------
-- 错题本：某个用户当前还没订正的题，答对一次就从这张表里删掉——
-- 表里"存在的行"就代表"现在还错着"，不需要额外的 status 字段。
-- ---------------------------------------------------------
CREATE TABLE wrong_questions (
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

-- ---------------------------------------------------------
-- 收藏：用户在①岗位详情页收藏的具体招聘信息
-- ---------------------------------------------------------
CREATE TABLE favorites (
  user_id     BIGINT UNSIGNED NOT NULL,
  posting_id  BIGINT UNSIGNED NOT NULL,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, posting_id),
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (posting_id) REFERENCES job_postings(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
