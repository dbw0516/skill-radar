"""
合并两个学习资料来源，生成可执行 SQL：
  - database/学习资料汇总.xlsx （多岗位方向，官方文档为主）
  - database/学习资料收集-Java后端方向.md 里的"汇总表"（B站视频为主）

只导入：① 技能名能在当前 skills 表里对上号的　② 不是占位搜索链接
（形如 https://www.bilibili.com/搜索"关键词" 的不是真实播放地址，不能直接当资料链接用，
 需要人工搜出具体 BV 号后再补，本脚本会把这些单独列出来，不写进 SQL）。

产出：database/seed_learning_resources.sql
用法：python tools/import_learning_resources.py
"""

import re
from pathlib import Path

import openpyxl

ROOT = Path(__file__).resolve().parent.parent
XLSX = ROOT / "database" / "学习资料汇总.xlsx"
MD = ROOT / "database" / "学习资料收集-Java后端方向.md"
OUT = ROOT / "database" / "seed_learning_resources.sql"

KNOWN_SKILLS = {
    "Java基础", "面向对象", "数据结构", "MySQL", "Spring Boot", "Redis",
    "Linux基础", "Git", "MyBatis", "Java并发", "HTML/CSS",
}
# .md 汇总表里技能名和 skills 表标准名有几个对不上，做个别名映射
SKILL_ALIAS = {"数据结构": "数据结构"}  # 目前一致，留着方便以后加

PLACEHOLDER_RE = re.compile(r'bilibili\.com/搜索')

TYPE_MAP = {"文档": "doc", "视频": "video", "课程": "course", "教程": "article",
            "在线书籍": "article", "刷题平台": "article", "资源合集": "article"}


def esc(s):
    """反斜杠转义 + 单引号双写（不用 \\'——mysql 命令行客户端读 .sql 文件时对它有解析歧义）。"""
    if s is None:
        return "NULL"
    s = str(s).strip().replace("\\", "\\\\").replace("'", "''")
    return f"'{s}'"


def load_xlsx_rows():
    wb = openpyxl.load_workbook(XLSX, data_only=True)
    ws = wb["学习资料汇总"]
    rows = list(ws.iter_rows(min_row=2, values_only=True))
    out = []
    for job, skill, title, url, rtype, note in rows:
        if not skill or not url:
            continue
        out.append((skill.strip(), title, url.strip(), rtype or "文档"))
    return out


def load_md_rows():
    text = MD.read_text(encoding="utf-8")
    # 只取"汇总表"这一节，避免把每个模块下面重复的小表也解析一遍
    marker = "## 汇总表"
    idx = text.find(marker)
    section = text[idx:] if idx != -1 else text
    out = []
    for line in section.splitlines():
        line = line.strip()
        if not line.startswith("|"):
            continue
        cells = [c.strip() for c in line.strip("|").split("|")]
        if len(cells) != 5 or cells[0] in ("技能名称", "----------"):
            continue
        if set(cells[0]) <= {"-"}:  # 分隔行 |---|---|
            continue
        skill, title, url, rtype, note = cells
        out.append((skill, title, url, rtype))
    return out


def main():
    xlsx_rows = load_xlsx_rows()
    md_rows = load_md_rows()

    seen_urls = set()
    importable = []
    placeholder_skipped = []
    unknown_skill_skipped = []

    for skill, title, url, rtype in xlsx_rows + md_rows:
        skill = SKILL_ALIAS.get(skill, skill)
        if PLACEHOLDER_RE.search(url):
            placeholder_skipped.append((skill, title, url))
            continue
        if skill not in KNOWN_SKILLS:
            unknown_skill_skipped.append((skill, title, url))
            continue
        if url in seen_urls:
            continue
        seen_urls.add(url)
        importable.append((skill, title, url, rtype))

    out_lines = [
        "-- ============================================================",
        "-- 由 tools/import_learning_resources.py 合并生成",
        "-- 来源：学习资料汇总.xlsx + 学习资料收集-Java后端方向.md（汇总表部分）",
        "-- 已过滤：B站占位搜索链接、技能词典还未覆盖到的岗位方向（见脚本运行输出）",
        "-- ============================================================",
        "",
        "SET NAMES utf8mb4;",
        "",
    ]
    for skill, title, url, rtype in importable:
        db_type = TYPE_MAP.get(rtype, "article")
        out_lines.append(
            "INSERT IGNORE INTO learning_resources (skill_id, title, url, type) VALUES ("
            f"(SELECT id FROM skills WHERE name = {esc(skill)} LIMIT 1), "
            f"{esc(title)}, {esc(url)}, {esc(db_type)});"
        )
    OUT.write_text("\n".join(out_lines), encoding="utf-8")

    print(f"可导入：{len(importable)} 条 -> {OUT}")
    print(f"\n跳过（占位搜索链接，需人工找到真实 BV 号后手动补）：{len(placeholder_skipped)} 条")
    for skill, title, url in placeholder_skipped:
        print(f"  [{skill}] {title} -> {url}")
    print(f"\n跳过（技能还不在当前 skills 表里，等词典扩充后再导）：{len(unknown_skill_skipped)} 条，涉及技能：")
    for s in sorted({s for s, _, _ in unknown_skill_skipped}):
        print("  -", s)


if __name__ == "__main__":
    main()
