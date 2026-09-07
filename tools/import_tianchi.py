"""
把 database/天池数据集-计算机相关岗位.xlsx 转成可执行的 SQL 种子文件。

产出：database/seed_real_postings.sql
  - INSERT INTO job_categories：数据集里出现的全部岗位类别
  - INSERT INTO job_postings：每条真实招聘信息（公司/岗位/地点/薪资/原文JD）
  - INSERT INTO posting_skills：仅对「已匹配技能」列里、且能在当前 skills 表
    （seed.sql 里那 10 个 Java 后端技能）对上号的技能生效——其他类别的技能
    词典还没建，这批 posting 先只有岗位数据，技能关联留空，等词典扩了再补一次即可。

跳过：JD 正文短于 MIN_JD_LEN 的行（大概率是空壳/占位数据，不是真实描述）。

用法：python tools/import_tianchi.py
"""

import re
from pathlib import Path
from datetime import date

import openpyxl

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "database" / "天池数据集-计算机相关岗位.xlsx"
OUT = ROOT / "database" / "seed_real_postings.sql"

MIN_JD_LEN = 20  # 短于这个长度的原文 JD 视为空壳数据，跳过

# 当前 seed.sql 里已经存在的技能标准名（id 1~10，按 seed.sql 里的插入顺序）
# 「已匹配技能」列里的名字如果不在这个集合里，说明技能词典还没覆盖，先不建关联。
EXISTING_SKILLS = {
    "HTML/CSS": 1, "Java基础": 2, "面向对象": 3, "数据结构": 4, "MySQL": 5,
    "Spring Boot": 6, "Redis": 7, "Linux基础": 8, "Git": 9, "MyBatis": 10,
    "Java并发": 11, "多线程": 11, "并发编程": 11,  # 常见别名，直接映射到同一个技能
}


def esc(s):
    """MySQL 字符串转义：先转反斜杠，再转单引号。"""
    if s is None:
        return "NULL"
    s = str(s).replace("\\", "\\\\").replace("'", "\\'")
    return f"'{s}'"


def clean_jd(text: str) -> str:
    text = re.sub(r"[ \t]+", " ", text)
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text.strip()


def main():
    wb = openpyxl.load_workbook(SRC, read_only=True, data_only=True)
    ws = wb["岗位数据"]
    rows = list(ws.iter_rows(values_only=True))
    header, data = rows[0], rows[1:]
    # header: 岗位类别, 公司名称, 岗位名称, 工作地点, 薪资, 发布日期(月-日), 原文JD, 已匹配技能

    categories = []
    seen_cat = set()
    for r in data:
        cat = (r[0] or "").strip()
        if cat and cat not in seen_cat:
            seen_cat.add(cat)
            categories.append(cat)

    skipped_short = 0
    skill_hits, skill_misses = 0, set()

    lines = [
        "-- ============================================================",
        "-- 由 tools/import_tianchi.py 从「天池数据集-计算机相关岗位.xlsx」自动生成",
        f"-- 生成日期：{date.today().isoformat()}",
        "-- 数据来源：阿里天池公开数据集，经关键词筛选+每类封顶后的计算机相关岗位子集",
        "-- 注意：source_url 该数据集未提供，均为 NULL；posted_at 原始只有月-日、无年份，本次导入不填年份猜测值，均为 NULL",
        "-- ============================================================",
        "",
        "SET NAMES utf8mb4;",
        "",
        "-- 岗位类别（数据集里出现的全部类别，major_id 先统一挂到 1=计算机科学与技术）",
    ]
    for cat in categories:
        # INSERT IGNORE + name 唯一约束：如果 seed.sql 已经建过同名类别（比如 Java后端工程师），
        # 这里不会插出重复的一条，下面的 job_postings 会自动挂到已有的那个 category_id 上。
        lines.append(
            f"INSERT IGNORE INTO job_categories (name, major_id, description, status) VALUES "
            f"({esc(cat)}, 1, {esc('由天池数据集导入，技能画像/依赖图谱待后续补充')}, 'active');"
        )
    lines.append("")
    lines.append("-- 用 name 反查刚插入的 category id，避免手写行号")
    lines.append("-- （在同一个事务/脚本里执行即可保证下面的子查询能查到上面刚插入的行）")
    lines.append("")

    lines.append("-- 具体招聘信息 + 技能关联（用 LAST_INSERT_ID() 精确对应刚插入的那一条，不靠内容反查，避免同名/重复 JD 关联错行）")
    posting_idx = 0
    for r in data:
        cat, company, title, location, salary, posted_md, jd, matched = (list(r) + [None] * 8)[:8]
        jd = (jd or "").strip()
        if len(jd) < MIN_JD_LEN:
            skipped_short += 1
            continue
        posting_idx += 1
        cat = (cat or "").strip()
        company = (company or "未知公司").strip()
        title = (title or "未知岗位").strip()
        jd_clean = clean_jd(jd)

        lines.append(
            "INSERT INTO job_postings "
            "(category_id, company_name, title, location, salary_text, source_url, raw_text, status, posted_at) "
            "VALUES ("
            f"(SELECT id FROM job_categories WHERE name = {esc(cat)} LIMIT 1), "
            f"{esc(company)}, {esc(title)}, {esc(location)}, {esc(salary)}, NULL, "
            f"{esc(jd_clean)}, 'open', NULL);"
        )

        matched_ids = []
        if matched:
            seen_ids = set()
            for name in (s.strip() for s in str(matched).split(",") if s.strip()):
                skill_id = EXISTING_SKILLS.get(name)
                if skill_id is None:
                    skill_misses.add(name)
                elif skill_id not in seen_ids:  # 不同别名映射到同一个技能时去重，避免 posting_skills 主键冲突
                    seen_ids.add(skill_id)
                    matched_ids.append(skill_id)

        if matched_ids:
            lines.append("SET @pid := LAST_INSERT_ID();")
            for skill_id in matched_ids:
                skill_hits += 1
                lines.append(
                    f"INSERT INTO posting_skills (posting_id, skill_id, source, confirmed) "
                    f"VALUES (@pid, {skill_id}, 'dict', TRUE);"
                )

    OUT.write_text("\n".join(lines), encoding="utf-8")

    print(f"共 {len(data)} 行原始数据")
    print(f"跳过过短/空壳 JD：{skipped_short} 行")
    print(f"实际生成 job_postings：{posting_idx} 条")
    print(f"生成 posting_skills 关联：{skill_hits} 条")
    print(f"「已匹配技能」里出现、但当前 skills 表还没有的技能名（待词典扩充后处理），"
          f"共 {len(skill_misses)} 种：")
    for name in sorted(skill_misses):
        print("  -", name)
    print(f"\n已写入 {OUT}")


if __name__ == "__main__":
    main()
