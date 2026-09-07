"""
把 database/Java后端面试题集.md（来源：JavaGuide）解析成 SQL，写入 interview_questions 表。
这是开放式问答 + 要点，不是可自动判分的选择题——选择题(questions 表)还是要单独人工出。

产出：database/seed_interview_questions.sql
用法：python tools/import_interview_questions.py
"""

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "database" / "Java后端面试题集.md"
OUT = ROOT / "database" / "seed_interview_questions.sql"

# ## N. 模块名 -> skills 表里的技能标准名（对应 seed.sql 里的技能）
MODULE_TO_SKILL = {
    "Java 基础": "Java基础",
    "面向对象": "面向对象",
    "Java 并发": "Java并发",
    "MySQL": "MySQL",
    "Spring": "Spring Boot",
    "Redis": "Redis",
    "MyBatis": "MyBatis",
}

ROW_RE = re.compile(r"^\|\s*(\d+)\s*\|(.+)\|(.+)\|\s*$")


def esc(s):
    """反斜杠转义 + 单引号双写（不用 \\'——mysql 命令行客户端读 .sql 文件时对它有解析歧义）。"""
    if s is None:
        return "NULL"
    s = str(s).strip().replace("\\", "\\\\").replace("'", "''")
    return f"'{s}'"


def main():
    text = SRC.read_text(encoding="utf-8")
    lines = text.splitlines()

    current_skill = None
    entries = []  # (skill_name, question, key_points)
    for line in lines:
        m = re.match(r"^## \d+\.\s*(.+)$", line.strip())
        if m:
            title = m.group(1).strip()
            current_skill = MODULE_TO_SKILL.get(title)
            continue
        if current_skill is None:
            continue
        row = ROW_RE.match(line.strip())
        if not row:
            continue
        num, question, key_points = row.groups()
        if question.strip() == "题目":  # 表头行
            continue
        entries.append((current_skill, question.strip(), key_points.strip()))

    out_lines = [
        "-- ============================================================",
        "-- 由 tools/import_interview_questions.py 从「Java后端面试题集.md」自动生成",
        "-- 来源：JavaGuide（https://github.com/Snailclimb/JavaGuide），已在 source 字段标注",
        "-- 这是开放式问答，供「学习资料/面试准备」阅读用，不是自动判分的 questions 表",
        "-- ============================================================",
        "",
        "SET NAMES utf8mb4;",
        "",
    ]
    missing_skill_modules = set()
    written = 0
    for skill_name, question, key_points in entries:
        out_lines.append(
            "INSERT INTO interview_questions (skill_id, question_text, key_points, source) VALUES ("
            f"(SELECT id FROM skills WHERE name = {esc(skill_name)} LIMIT 1), "
            f"{esc(question)}, {esc(key_points)}, 'JavaGuide');"
        )
        written += 1

    OUT.write_text("\n".join(out_lines), encoding="utf-8")
    print(f"解析到 {written} 道题，已写入 {OUT}")
    print("模块 -> 技能映射：", MODULE_TO_SKILL)


if __name__ == "__main__":
    main()
