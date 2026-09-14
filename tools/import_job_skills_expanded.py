# -*- coding: utf-8 -*-
"""
给"剩余岗位"（除了本来就有 job_skills 的 Java后端工程师）补上 job_skills
（岗位需要哪些技能、权重多少）——这是 tools/import_expanded_interview_questions.py
留的坑：那次只加了技能节点和面试题，没接进①岗位推荐/②差距分析/③学习路径这几个
引擎，因为 job_skills 才是差距分析真正读的表。

权重定法：跟 seed.sql 里 Java 后端那 9 行一个路数——不是从真实统计算出来的，是
"这个技能对这个岗位有多核心"的人工判断（seed.sql 自己也说是"手工给的示例值，还
没按 posting_skills 重新统计"），核心技能 0.7~0.9，次要技能 0.3~0.6，边缘/通用
技能 0.2~0.3。以后要精确，得跑 posting_skills 统计脚本重新算。

覆盖不到的地方（如实说，没有硬凑）：
  - 产品经理：这次 15 份面试题文档里根本没有产品经理这份，技能词典里没有任何一个
    技能是为它准备的，这里直接跳过，不编造权重——等团队找到产品经理的面试题/技能
    资料再补。
  - 全栈工程师、嵌入式开发工程师：对应的原始 docx 通篇是行为面试模板，没有可提炼
    的技术点（这在扩充技能词典那次已经说明过），这两个类别下面的权重是按"这个
    岗位方向通常需要什么"的通用判断给的，不是从那两份文档的技术内容反推的——跟
    其他类别"权重来自文档实际技术题"的可信度不是一个量级，供参考，团队后续应该
    优先补这两个方向的真实技术资料。
  - 算法/机器学习工程师：文档也是纯行为面试模板，没有机器学习专属技能可用，这里
    复用了"数据结构"（算法基础）和几个 Python/统计技能顶一下，覆盖度明显不够，
    需要团队专门补机器学习方向的技术资料和技能节点。

产出：database/seed_job_skills_expanded.sql
用法：python tools/import_job_skills_expanded.py
"""

from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "database" / "seed_job_skills_expanded.sql"


def esc(s):
    if s is None:
        return "NULL"
    s = str(s).strip().replace("\\", "\\\\").replace("'", "''")
    return f"'{s}'"


# (job_category_name, [(skill_name, weight, note), ...])
JOB_SKILLS = [
    ("Python开发工程师", [
        ("Python核心语法与内置类型", 0.85, None),
        ("Python内存管理与对象模型", 0.55, None),
        ("Python异常处理", 0.40, None),
        ("数据结构", 0.40, "复用 Java 后端方向的技能"),
        ("Python正则表达式", 0.35, None),
        ("Python网络编程", 0.35, None),
        ("Python文件与编码处理", 0.30, None),
        ("MySQL", 0.45, "复用"),
        ("Git", 0.30, "复用"),
    ]),
    ("前端开发工程师", [
        ("JavaScript基础", 0.90, None),
        ("HTML/CSS", 0.85, "复用（seed.sql 里原本就有）"),
        ("DOM与浏览器API", 0.60, None),
        ("前端性能优化", 0.50, None),
        ("Git", 0.30, "复用"),
    ]),
    ("大数据开发工程师", [
        ("Spark计算与性能调优", 0.85, None),
        ("Hadoop与HDFS基础", 0.70, None),
        ("消息队列与流处理", 0.65, None),
        ("Hive与HBase", 0.60, None),
        ("数据仓库与数据质量", 0.55, None),
        ("大数据架构设计", 0.45, None),
        ("Python核心语法与内置类型", 0.40, "复用"),
        ("MySQL", 0.35, "复用"),
    ]),
    ("数据分析师", [
        ("数据清洗与描述性统计", 0.70, None),
        ("Excel数据分析", 0.75, None),
        ("MySQL", 0.65, "复用"),
        ("pandas数据处理", 0.60, None),
        ("数据可视化", 0.55, None),
        ("统计建模与回归分析", 0.45, None),
    ]),
    ("数据库管理员", [
        ("数据库性能调优", 0.90, None),
        ("MySQL", 0.80, "复用"),
        ("数据库高可用与容灾", 0.70, None),
        ("数据库设计与建模", 0.65, None),
        ("数据库事务与一致性", 0.60, None),
        ("数据库安全管理", 0.55, None),
        ("数据库迁移与运维协作", 0.45, None),
        ("Linux基础", 0.40, "复用"),
    ]),
    ("测试工程师", [
        ("软件测试方法与工具", 0.80, None),
        ("Git", 0.35, "复用"),
        ("MySQL", 0.30, "复用"),
        ("Linux基础", 0.30, "复用"),
        ("进程与线程基础", 0.25, "复用"),
    ]),
    ("移动开发工程师", [
        # 这一个类别同时覆盖 Android 和 iOS 两条技术线（安卓/ios 两份 docx 都没有
        # 单独的岗位类别，都挂在"移动开发工程师"下面）
        ("Android生命周期与状态管理", 0.70, None),
        ("Android UI与性能优化", 0.65, None),
        ("Android组件与Intent", 0.60, None),
        ("Android数据存储方式", 0.55, None),
        ("Android消息机制", 0.55, None),
        ("Android内存管理与调试", 0.50, None),
        ("iOS设计模式", 0.55, None),
        ("iOS并发与UI基础", 0.55, None),
        ("Objective-C语言特性", 0.55, None),
        ("iOS内存管理", 0.50, None),
        ("iOS数据持久化", 0.40, None),
    ]),
    ("算法/机器学习工程师", [
        # 覆盖度明显不够，见文件头说明——原始 docx 没有机器学习专属技术题
        ("数据结构", 0.85, "复用，顶算法基础"),
        ("Python核心语法与内置类型", 0.60, "复用"),
        ("统计建模与回归分析", 0.55, "复用数据分析方向的技能"),
        ("pandas数据处理", 0.45, "复用"),
        ("Python内存管理与对象模型", 0.30, "复用"),
    ]),
    ("网络安全工程师", [
        ("网络攻击类型与防御", 0.80, None),
        ("身份认证与访问控制", 0.75, None),
        ("安全事件应急响应与取证", 0.60, None),
        ("密码学基础", 0.55, None),
        ("安全合规与风险管理", 0.45, None),
        ("Linux基础", 0.40, "复用"),
        ("物联网与新兴技术安全", 0.30, None),
    ]),
    ("软件开发工程师（通用）", [
        # "通用"岗位本来就该是广撒网，全复用已有技能，不新造
        ("数据结构", 0.55, "复用"),
        ("Git", 0.50, "复用"),
        ("面向对象", 0.45, "复用"),
        ("Java基础", 0.40, "复用"),
        ("MySQL", 0.40, "复用"),
        ("Linux基础", 0.40, "复用"),
        ("进程与线程基础", 0.35, "复用"),
        ("JavaScript基础", 0.30, "复用"),
    ]),
    ("运维工程师/DevOps", [
        ("Linux基础", 0.85, "复用"),
        ("服务器监控与告警", 0.75, None),
        ("故障排查与应急响应", 0.65, None),
        ("系统高可用与容灾", 0.70, None),
        ("系统安全运维", 0.60, None),
        ("Git", 0.35, "复用"),
    ]),
    ("Go后端工程师", [
        ("Go基础语法与数据类型", 0.85, None),
        ("Go Channel与并发通信", 0.65, None),
        ("Go调度器与GMP模型", 0.50, None),
        ("Go切片数组与拷贝机制", 0.55, None),
        ("Go锁与原子操作", 0.45, None),
        ("MySQL", 0.45, "复用"),
        ("Go内存管理机制", 0.35, None),
        ("Linux基础", 0.35, "复用"),
        ("Git", 0.30, "复用"),
    ]),
    ("全栈工程师", [
        # 原始 docx 没有技术内容，这里按"全栈通常需要前后端+数据库"的通用判断给权重，
        # 可信度不如其他类别，见文件头说明
        ("JavaScript基础", 0.60, "通用判断，非文档内容"),
        ("HTML/CSS", 0.55, "通用判断，非文档内容，复用技能"),
        ("MySQL", 0.55, "通用判断，非文档内容，复用技能"),
        ("Java基础", 0.40, "通用判断，非文档内容，复用技能"),
        ("Git", 0.40, "通用判断，非文档内容，复用技能"),
        ("DOM与浏览器API", 0.35, "通用判断，非文档内容"),
    ]),
    ("嵌入式开发工程师", [
        # 覆盖度明显不够，见文件头说明——通用判断，不是文档技术内容反推的
        ("Linux基础", 0.60, "通用判断，非文档内容，复用技能"),
        ("进程与线程基础", 0.55, "通用判断，非文档内容，复用技能"),
        ("Git", 0.25, "通用判断，非文档内容，复用技能"),
    ]),
    ("游戏开发工程师", [
        ("游戏引擎渲染与性能优化", 0.80, None),
        ("游戏物理与碰撞检测", 0.55, None),
        ("游戏动画与特效系统", 0.55, None),
        ("游戏脚本与并发编程", 0.50, None),
        ("游戏网络同步", 0.45, None),
        ("进程与线程基础", 0.30, "复用"),
    ]),
    # 产品经理：跳过，没有任何相关技能数据（见文件头说明）
]


def build_sql():
    lines = [
        "-- ============================================================",
        "-- 由 tools/import_job_skills_expanded.py 生成",
        "-- 给「除 Java 后端工程师以外」的岗位类别补 job_skills（岗位需要的技能+权重），",
        "-- 这样技能词典扩充（seed_expanded_skills_and_questions.sql）新增的技能才能真正",
        "-- 接入②差距分析 / ③学习路径引擎，而不是只是躺在 interview_questions 里。",
        "-- 权重是人工判断的示例值（核心技能 0.7~0.9，次要 0.3~0.6，边缘/通用 0.2~0.3），",
        "-- 跟 seed.sql 里 Java 后端那 9 行一个性质，不是真实统计出来的，说明见脚本头部。",
        "-- 产品经理没有任何相关技能数据，这次没有覆盖，跳过。",
        "-- ============================================================",
        "",
        "SET NAMES utf8mb4;",
        "",
    ]
    for category_name, skill_weights in JOB_SKILLS:
        lines.append(f"-- ---------- {category_name} ----------")
        for skill_name, weight, note in skill_weights:
            comment = f"  -- {note}" if note else ""
            lines.append(
                "INSERT INTO job_skills (category_id, skill_id, weight) VALUES ("
                f"(SELECT id FROM job_categories WHERE name = {esc(category_name)} LIMIT 1), "
                f"(SELECT id FROM skills WHERE name = {esc(skill_name)} LIMIT 1), "
                f"{weight}) "
                "ON DUPLICATE KEY UPDATE weight = VALUES(weight);" + comment
            )
        lines.append("")
    return "\n".join(lines) + "\n"


def main():
    sql = build_sql()
    OUT.write_text(sql, encoding="utf-8")
    total = sum(len(v) for _, v in JOB_SKILLS)
    print(f"覆盖 {len(JOB_SKILLS)} 个岗位类别，共 {total} 条 job_skills 记录")
    print(f"已写入 {OUT}")


if __name__ == "__main__":
    main()
