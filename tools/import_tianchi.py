"""
把 database/天池数据集-计算机相关岗位.xlsx 转成可执行的 SQL 种子文件。

产出：database/seed_real_postings.sql
  - INSERT INTO job_categories：数据集里出现的全部岗位类别
  - INSERT INTO job_postings：每条真实招聘信息（公司/岗位/地点/薪资/原文JD）

每条 JD 要哪些技能（posting_skills）不在这里生成：由 tools/extract_posting_skills.py
从 JD 原文抽取，写到 database/seed_posting_skills.sql。数据集自带的「已匹配技能」列
是按一个只有十来个 Java 后端技能的旧词典匹配的，不再使用。

跳过：
  - JD 正文短于 MIN_JD_LEN 的行（大概率是空壳/占位数据，不是真实描述）
  - EXCLUDED_CATEGORIES 里的类别（整类不导入，原因见那里的注释）

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

# 不导入的岗位类别。产品经理：技能词典里没有任何产品方向的技能，140 条 JD 里 113 条一个技术技能都抽不出来，
# 差距分析/学习路径/测评对它都给不出东西，已从系统里删掉（已有库见 database/migrate_remove_product_manager.sql）
EXCLUDED_CATEGORIES = {"产品经理"}


def esc(s):
    """MySQL 字符串转义：反斜杠转义 + 单引号双写（不用 \\' ——
    mysql 命令行客户端读取 .sql 文件时对 \\' 有解析歧义，实测会报 "Unknown command"）。"""
    if s is None:
        return "NULL"
    s = str(s).replace("\\", "\\\\").replace("'", "''")
    return f"'{s}'"


def clean_jd(text: str) -> str:
    text = re.sub(r"[ \t]+", " ", text)
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text.strip()


def load_rows():
    """数据集「岗位数据」表除表头外的全部行。
    列：岗位类别, 公司名称, 岗位名称, 工作地点, 薪资, 发布日期(月-日), 原文JD, 已匹配技能"""
    wb = openpyxl.load_workbook(SRC, read_only=True, data_only=True)
    ws = wb["岗位数据"]
    # 不用 iter_rows(values_only=True)：openpyxl 2.6 之前没有这个参数（Anaconda 自带的是 2.5）
    return [tuple(cell.value for cell in row) for row in ws.iter_rows()][1:]


def iter_postings(data):
    """会导入成 job_postings 的行（跳过空壳 JD 和不导入的类别），字段清理成跟入库时一模一样——
    tools/extract_posting_skills.py 靠 JD 原文的哈希 + 标题在库里找回这些岗位，两边必须一致。"""
    for r in data:
        cat, company, title, location, salary, _posted_md, jd, _matched = (list(r) + [None] * 8)[:8]
        jd = (jd or "").strip()
        cat = (cat or "").strip()
        if len(jd) < MIN_JD_LEN or cat in EXCLUDED_CATEGORIES:
            continue
        yield {
            "category": cat,
            "company": (company or "未知公司").strip(),
            "title": (title or "未知岗位").strip(),
            "location": location,
            "salary": salary,
            "raw_text": clean_jd(jd),
        }


def main():
    data = load_rows()

    categories = []
    seen_cat = set()
    for r in data:
        cat = (r[0] or "").strip()
        if cat and cat not in seen_cat and cat not in EXCLUDED_CATEGORIES:
            seen_cat.add(cat)
            categories.append(cat)

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

    lines.append("-- 具体招聘信息（每条要哪些技能见 seed_posting_skills.sql，由 tools/extract_posting_skills.py 生成）")
    postings = list(iter_postings(data))
    for p in postings:
        lines.append(
            "INSERT INTO job_postings "
            "(category_id, company_name, title, location, salary_text, source_url, raw_text, status, posted_at) "
            "VALUES ("
            f"(SELECT id FROM job_categories WHERE name = {esc(p['category'])} LIMIT 1), "
            f"{esc(p['company'])}, {esc(p['title'])}, {esc(p['location'])}, {esc(p['salary'])}, NULL, "
            f"{esc(p['raw_text'])}, 'open', NULL);"
        )

    OUT.write_text("\n".join(lines), encoding="utf-8")

    print(f"共 {len(data)} 行原始数据")
    print(f"跳过过短/空壳 JD、不导入的类别：{len(data) - len(postings)} 行")
    print(f"实际生成 job_postings：{len(postings)} 条")
    print(f"\n已写入 {OUT}")
    print("技能关联要再跑一次 python tools/extract_posting_skills.py")


if __name__ == "__main__":
    main()
