"""
从天池数据集每条 JD 的原文里抽技能，生成 posting_skills 的 SQL。

产出：database/seed_posting_skills.sql
  - 抽取规则见 tools/skill_extractor.py（标题 + JD 正文 → 技能词典里的技能）
  - 只写"必需"技能；"有 xx 经验者优先"这类加分项不写进 posting_skills，
    不然一个达标的候选人会因为没碰过加分项而匹配度上不去
  - 按 (JD 原文的 MD5, title) 在库里找回岗位，不依赖自增 id：新建库和已有库都能直接跑，
    重复跑也安全——每次先删掉这批岗位 source='dict' 的旧关联再重新插，
    人工确认（manual）/ AI 建议（llm）的关联不动

执行顺序：必须在 seed_real_postings.sql 和全部技能种子（seed.sql、
seed_expanded_skills_and_questions.sql）之后——按技能名 JOIN skills 表，技能不在就会被跳过。

用法：python tools/extract_posting_skills.py
"""

import hashlib
from collections import Counter
from datetime import date
from pathlib import Path

from import_tianchi import esc, iter_postings, load_rows
from skill_extractor import REQUIRED, extract_detail

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "database" / "seed_posting_skills.sql"


def jd_key(raw_text):
    """JD 原文的 MD5，算之前去掉反斜杠——对应 SQL 里的 MD5(REPLACE(raw_text, CHAR(92), ''))。
    有一条 JD 的原文带反斜杠，导进库后少了几个（xlsx 里 14 个、库里 5 个），
    不去掉的话那条岗位就对不上。"""
    return hashlib.md5(raw_text.replace("\\", "").encode("utf-8")).hexdigest()


def values_block(rows):
    return ",\n".join("(" + ", ".join(esc(v) for v in row) + ")" for row in rows)


def main():
    postings = list(iter_postings(load_rows()))

    keys = []
    links = []
    skill_counts, preferred_counts = Counter(), Counter()
    no_skill = Counter()
    seen = set()
    for p in postings:
        key = (jd_key(p["raw_text"]), p["title"])
        detail = extract_detail(p["title"], p["raw_text"])
        required = [s for s, (level, _) in detail.items() if level == REQUIRED]
        skill_counts.update(required)
        preferred_counts.update(s for s, (level, _) in detail.items() if level != REQUIRED)
        if not required:
            no_skill[p["category"]] += 1
        if key in seen:  # 数据集里有完全相同的 JD（同标题同正文），抽取结果也一样，写一遍就够
            continue
        seen.add(key)
        keys.append(key)
        links.extend(key + (skill,) for skill in required)

    lines = [
        "-- ============================================================",
        "-- 由 tools/extract_posting_skills.py 生成（抽取规则：tools/skill_extractor.py）",
        f"-- 生成日期：{date.today().isoformat()}",
        "-- 天池数据集每条 JD 要求的技能（只含必需技能，「优先/加分」的不算）。",
        "-- 按 (JD 原文去掉反斜杠后的 MD5, title) 找回岗位，不依赖自增 id（title 比较不区分大小写，所以临时表用 INSERT IGNORE）；",
        "-- 可重复执行：",
        "-- 先删这批岗位 source='dict' 的旧关联再重插，manual/llm 的关联不动。",
        "-- 执行顺序：在 seed_real_postings.sql 和全部技能种子之后。",
        "-- ============================================================",
        "",
        "SET NAMES utf8mb4;",
        "",
        "DROP TEMPORARY TABLE IF EXISTS tmp_extract_postings;",
        "CREATE TEMPORARY TABLE tmp_extract_postings (",
        "  jd_md5 CHAR(32) NOT NULL,",
        "  title  VARCHAR(128) NOT NULL,",
        "  PRIMARY KEY (jd_md5, title)",
        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;",
        "",
        "DROP TEMPORARY TABLE IF EXISTS tmp_extract_skills;",
        "CREATE TEMPORARY TABLE tmp_extract_skills (",
        "  jd_md5     CHAR(32) NOT NULL,",
        "  title      VARCHAR(128) NOT NULL,",
        "  skill_name VARCHAR(64) NOT NULL,",
        "  PRIMARY KEY (jd_md5, title, skill_name)",
        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;",
        "",
        f"-- {len(keys)} 条岗位",
        "INSERT IGNORE INTO tmp_extract_postings (jd_md5, title) VALUES",
        values_block(keys) + ";",
        "",
        f"-- {len(links)} 条技能关联",
        "INSERT IGNORE INTO tmp_extract_skills (jd_md5, title, skill_name) VALUES",
        values_block(links) + ";",
        "",
        "-- 找回库里对应的岗位 id",
        "DROP TEMPORARY TABLE IF EXISTS tmp_extract_ids;",
        "CREATE TEMPORARY TABLE tmp_extract_ids (",
        "  posting_id BIGINT UNSIGNED NOT NULL PRIMARY KEY,",
        "  jd_md5     CHAR(32) NOT NULL,",
        "  title      VARCHAR(128) NOT NULL",
        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;",
        "INSERT IGNORE INTO tmp_extract_ids (posting_id, jd_md5, title)",
        "SELECT p.id, t.jd_md5, t.title",
        "FROM job_postings p",
        "JOIN tmp_extract_postings t ON t.jd_md5 = MD5(REPLACE(p.raw_text, CHAR(92), '')) AND t.title = p.title;",
        "",
        "START TRANSACTION;",
        "DELETE ps FROM posting_skills ps",
        "JOIN tmp_extract_ids i ON i.posting_id = ps.posting_id",
        "WHERE ps.source = 'dict';",
        "INSERT IGNORE INTO posting_skills (posting_id, skill_id, source, confirmed)",
        "SELECT i.posting_id, s.id, 'dict', TRUE",
        "FROM tmp_extract_ids i",
        "JOIN tmp_extract_skills e ON e.jd_md5 = i.jd_md5 AND e.title = i.title",
        "JOIN skills s ON s.name = e.skill_name;",
        "COMMIT;",
        "",
        f"-- 跑完看一眼：matched_postings 应该是 {len(postings)}（数据集里的岗位都在库里找到了）",
        "-- （临时表在一条语句里只能引用一次，所以不能写成两个子查询）",
        "SELECT COUNT(DISTINCT i.posting_id) AS matched_postings, COUNT(ps.skill_id) AS dict_links",
        "FROM tmp_extract_ids i",
        "LEFT JOIN posting_skills ps ON ps.posting_id = i.posting_id AND ps.source = 'dict';",
        "",
        "DROP TEMPORARY TABLE tmp_extract_ids;",
        "DROP TEMPORARY TABLE tmp_extract_skills;",
        "DROP TEMPORARY TABLE tmp_extract_postings;",
        "",
    ]
    OUT.write_text("\n".join(lines), encoding="utf-8")

    total = sum(skill_counts.values())
    print(f"岗位 {len(postings)} 条，必需技能关联 {total} 条（平均每条 {total / len(postings):.2f} 个），"
          f"另有加分项 {sum(preferred_counts.values())} 个没写入")
    print(f"没抽出任何必需技能的岗位（匹配度会退回按岗位类别估算）：{sum(no_skill.values())} 条")
    for cat, n in no_skill.most_common():
        print(f"  {cat}: {n}")
    print("各技能被要求的岗位数：")
    for skill, n in skill_counts.most_common():
        print(f"  {n:4}  {skill}")
    print(f"\n已写入 {OUT}")


if __name__ == "__main__":
    main()
