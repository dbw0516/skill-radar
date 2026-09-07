"""
JD 快速录入工具

两种模式：

  单条模式（默认）：python jd_intake.py
      粘贴一条 JD -> 当场看清理结果和匹配到的技能 -> 确认/修改 -> 补公司名等字段 -> 写入。
      每条都要走完这一圈，适合刚开始用、还想盯着匹配准不准的时候。

  批量模式：python jd_intake.py --batch
      一次粘贴多条，每条前面加一行 "@公司名 | 岗位名 | 链接(可选)" 作为分隔和元信息，
      不逐条打断确认，全部处理完写入，最后打印一份汇总表（每条命中了哪些技能）方便你扫一眼；
      看着不对的，直接去 Excel 里改那一行就行，比逐条在命令行里改快。
      示例输入：
          @某某科技 | Java后端工程师 | https://xxx
          （粘贴这条JD正文）
          @另一家公司 | 前端开发工程师
          （粘贴这条JD正文）
          END

两种模式都会：
    - 自动截断"相关职位推荐"等尾部噪声，按「技能清单」表里的技能词典做匹配
    - 写入「岗位JD收集」表，「技能清单」表对应技能的出现次数同步 +1

依赖：pip install openpyxl
"""

import sys
import re
from pathlib import Path
from datetime import date

try:
    import openpyxl
except ImportError:
    sys.exit("缺少 openpyxl，先运行：pip install openpyxl")

# 出现这些词之后的内容大概率是"相关职位推荐"之类的无关内容，直接截断，避免污染技能匹配
NOISE_MARKERS = [
    "相关职位推荐", "猜你喜欢", "同类职位推荐", "热门职位推荐",
    "为你推荐", "看了又看", "其他人还看了", "相似职位",
]

# 常见按钮/标签类噪声：整行等于这些词时丢弃，只是让预览更干净，不影响技能匹配的正确性
NOISE_LINES = {
    "立即沟通", "立即投递", "在线沟通", "在线咨询", "收藏", "已收藏", "分享", "举报",
    "查看更多", "查看全部", "投递简历", "申请职位", "下载APP", "扫码查看", "复制链接",
}


def clean_text(raw: str) -> str:
    text = raw
    for marker in NOISE_MARKERS:
        idx = text.find(marker)
        if idx != -1:
            text = text[:idx]
    lines = [ln.strip() for ln in text.splitlines()]
    lines = [ln for ln in lines if ln and ln not in NOISE_LINES and len(ln) > 1]
    return "\n".join(lines)


def load_skill_dict(ws_skills) -> dict:
    """从「技能清单」表读 技能名称(A) + 常见别名(D)，返回 {别名小写: 技能标准名}"""
    skill_map = {}
    for row in ws_skills.iter_rows(min_row=2):
        name_cell = row[0].value
        if not name_cell:
            continue
        name = str(name_cell).strip()
        skill_map[name.lower()] = name
        alias_cell = row[3].value if len(row) > 3 else None
        if alias_cell:
            for alias in str(alias_cell).split(","):
                alias = alias.strip()
                if alias:
                    skill_map[alias.lower()] = name
    return skill_map


def match_skills(cleaned_text: str, skill_map: dict) -> list:
    hay_lower = cleaned_text.lower()
    hit_names, seen = [], set()
    for alias in sorted(skill_map.keys(), key=len, reverse=True):
        name = skill_map[alias]
        if not alias or name in seen:
            continue
        if len(alias) <= 2 and alias.isascii():
            # 短别名（如 "Go"、"C"）容易在别的词里误命中，要求整词边界匹配
            pattern = r"(?<![a-zA-Z0-9])" + re.escape(alias) + r"(?![a-zA-Z0-9])"
            hit = re.search(pattern, cleaned_text, flags=re.IGNORECASE)
        else:
            hit = alias in hay_lower
        if hit:
            hit_names.append(name)
            seen.add(name)
    return hit_names


def read_multiline(prompt: str) -> str:
    print(prompt)
    lines = []
    while True:
        try:
            line = input()
        except EOFError:
            break
        if line.strip() == "END":
            break
        lines.append(line)
    return "\n".join(lines)


def next_empty_row(ws, min_row=2) -> int:
    r = min_row
    while ws.cell(row=r, column=1).value not in (None, ""):
        r += 1
    return r


def find_skill_row(ws_skills, skill_name):
    for row in ws_skills.iter_rows(min_row=2):
        if row[0].value == skill_name:
            return row
    return None


def write_entry(ws_jd, ws_skill, company, title, cleaned, url, posted, matched, note=""):
    """写一行到「岗位JD收集」，并把 matched 里每个技能的出现次数 +1（没有就新建）。返回写入的行号。"""
    jd_row = next_empty_row(ws_jd)
    values = [company, title, cleaned, url, posted, ", ".join(matched), note]
    for col, val in enumerate(values, start=1):
        ws_jd.cell(row=jd_row, column=col, value=val)

    for s in matched:
        row = find_skill_row(ws_skill, s)
        if row is not None:
            row[2].value = (row[2].value or 0) + 1
        else:
            new_row = next_empty_row(ws_skill)
            ws_skill.cell(row=new_row, column=1, value=s)
            ws_skill.cell(row=new_row, column=3, value=1)

    return jd_row


def parse_batch(raw: str):
    """按 "@公司 | 岗位 | 链接" 开头的行切分成多条 (company, title, url, body)。"""
    entries = []
    header, lines = None, []
    for line in raw.splitlines():
        if line.startswith("@"):
            if header is not None:
                entries.append((header, "\n".join(lines)))
            header, lines = line[1:].strip(), []
        else:
            lines.append(line)
    if header is not None:
        entries.append((header, "\n".join(lines)))

    parsed = []
    for header, body in entries:
        parts = [p.strip() for p in header.split("|")]
        company = parts[0] if len(parts) > 0 else ""
        title = parts[1] if len(parts) > 1 else ""
        url = parts[2] if len(parts) > 2 else ""
        parsed.append((company, title, url, body))
    return parsed


def main():
    args = [a for a in sys.argv[1:] if a != "--batch"]
    batch = "--batch" in sys.argv

    default_path = Path(__file__).resolve().parent.parent / "数据收集模板.xlsx"
    path = Path(args[0]) if args else default_path
    if not path.exists():
        sys.exit(
            f"没找到 Excel 模板：{path}\n"
            f"把「数据收集模板.xlsx」放到项目根目录下，或者运行时把路径当参数传进来：\n"
            f"python jd_intake.py \"D:\\你的路径\\数据收集模板.xlsx\""
        )

    print(f"使用模板：{path}\n")

    try:
        run_batch(path) if batch else run_loop(path)
    except (EOFError, KeyboardInterrupt):
        print("\n已退出。")


def run_loop(path):
    while True:
        wb = openpyxl.load_workbook(path)
        ws_jd = wb["岗位JD收集"]
        ws_skill = wb["技能清单"]
        skill_map = load_skill_dict(ws_skill)

        raw = read_multiline("粘贴 JD 正文（只选职位描述/任职要求那一块），粘贴完单独一行输入 END：")
        if not raw.strip():
            print("没有内容，结束。")
            break

        cleaned = clean_text(raw)
        preview = cleaned if len(cleaned) <= 800 else cleaned[:800] + " ...(已截断显示，完整内容会存进表格)"
        print("\n--- 清理后预览 ---")
        print(preview)

        matched = match_skills(cleaned, skill_map)
        print("\n--- 匹配到的技能 ---")
        if matched:
            for i, s in enumerate(matched, 1):
                print(f"{i}. {s}")
        else:
            print("(没匹配到任何技能——可能这条 JD 提到的技能还不在技能清单里)")

        remove = input("\n有不相关的技能，输入序号删除（逗号分隔，直接回车跳过）：").strip()
        if remove:
            drop = {int(x) - 1 for x in remove.split(",") if x.strip().isdigit()}
            matched = [s for i, s in enumerate(matched) if i not in drop]

        extra = input("有漏掉的技能要手动加吗？（技能名，逗号分隔，直接回车跳过）：").strip()
        if extra:
            for s in extra.split(","):
                s = s.strip()
                if s and s not in matched:
                    matched.append(s)

        company = input("公司名称：").strip()
        title = input("岗位名称：").strip()
        url = input("来源链接（可选）：").strip()
        posted = input(f"发布时间（直接回车用今天 {date.today().isoformat()}）：").strip() or date.today().isoformat()

        jd_row = write_entry(ws_jd, ws_skill, company, title, cleaned, url, posted, matched)
        wb.save(path)
        print(f"\n已写入「岗位JD收集」第 {jd_row} 行，技能清单计数已同步更新。\n")

        if input("继续录入下一条？(Y/n) ").strip().lower() == "n":
            break

    print("完成，去 Excel 里看看吧。")


def run_batch(path):
    wb = openpyxl.load_workbook(path)
    ws_jd = wb["岗位JD收集"]
    ws_skill = wb["技能清单"]
    skill_map = load_skill_dict(ws_skill)

    raw = read_multiline(
        "批量模式：每条 JD 前加一行 @公司名 | 岗位名 | 链接(可选)，然后粘贴正文，\n"
        "可以重复多条，全部粘贴完单独一行输入 END：\n"
    )
    entries = parse_batch(raw)
    if not entries:
        print("没解析到任何条目——检查一下是不是每条前面都有 @ 开头的那一行。")
        return

    print(f"\n解析到 {len(entries)} 条，处理中…")
    summary = []
    for company, title, url, body in entries:
        cleaned = clean_text(body)
        matched = match_skills(cleaned, skill_map)
        jd_row = write_entry(
            ws_jd, ws_skill, company, title, cleaned, url,
            date.today().isoformat(), matched, note="批量导入，建议复核技能匹配",
        )
        summary.append((jd_row, company, title, matched))

    wb.save(path)

    print("\n--- 批量写入完成，扫一眼下面的汇总，技能数是 0 或明显不对的去 Excel 对应行手动改 ---")
    for row_num, company, title, matched in summary:
        skill_str = "、".join(matched) if matched else "(无匹配——技能词典可能还没覆盖，或者这条粘漏了)"
        print(f"第{row_num}行 [{company} - {title}]：{skill_str}")


if __name__ == "__main__":
    main()
