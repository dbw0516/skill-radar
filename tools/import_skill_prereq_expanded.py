# -*- coding: utf-8 -*-
"""
给扩充技能词典时新增的 62 个技能补 skill_prereq（依赖图谱），这样③学习路径引擎
（LearningPathService，Kahn 拓扑排序）才能把新覆盖的岗位类别排出"先学什么、再学
什么"的分层结果，而不是像现在这样每个类别都是"一个大阶段"（没有边可排）。

设计说明：
  - LearningPathService 只统计"前置技能也在这个岗位待学清单里"的边（前置已经在
    job_skills 之外，或者用户已掌握，都不算阻塞）——所以这里放心按每个技术方向
    内部的自然学习顺序连边，不用操心某条边在别的岗位类别里会不会"用不上"：用不上
    就是自动被忽略，不会报错也不会产生奇怪结果。
  - 部分边刻意跨领域，把共享技能（"进程与线程基础"）也编进图里，比如
    "Go Channel与并发通信"依赖"进程与线程基础"——只在某个岗位类别的 job_skills
    里同时包含这两个技能时才会真的影响排序，其余类别里这条边就是空转。
  - 跟已有的 8 条 Java 后端依赖边（seed.sql）一个风格：不追求覆盖每一种可能的
    先修关系，只连最主要的"没学 A 学不动 B"的那种关系。
  - "职业素养与求职技巧""软件测试方法与工具"这两个技能没给它们连依赖边——前者
    是软技能，没有严格的先修关系；后者这次收集到的技术内容太薄（只有 1 个技能），
    连边意义不大，留空。

产出：database/seed_skill_prereq_expanded.sql
用法：python tools/import_skill_prereq_expanded.py
"""

from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "database" / "seed_skill_prereq_expanded.sql"


def esc(s):
    if s is None:
        return "NULL"
    s = str(s).strip().replace("\\", "\\\\").replace("'", "''")
    return f"'{s}'"


# (skill, prereq_skill) —— skill 依赖 prereq_skill，即先学 prereq_skill 再学 skill
EDGES = [
    # ---------- Go 语言 ----------
    ("Go切片数组与拷贝机制", "Go基础语法与数据类型"),
    ("Go Map实现原理", "Go基础语法与数据类型"),
    ("Go Channel与并发通信", "Go基础语法与数据类型"),
    ("Go Channel与并发通信", "进程与线程基础"),
    ("Go锁与原子操作", "Go Channel与并发通信"),
    ("Go调度器与GMP模型", "Go锁与原子操作"),
    ("Go内存管理机制", "Go切片数组与拷贝机制"),

    # ---------- Python 语言 ----------
    ("Python内存管理与对象模型", "Python核心语法与内置类型"),
    ("Python异常处理", "Python核心语法与内置类型"),
    ("Python正则表达式", "Python核心语法与内置类型"),
    ("Python网络编程", "Python异常处理"),
    ("Python文件与编码处理", "Python核心语法与内置类型"),

    # ---------- 前端（JavaScript基础 依赖已有的 HTML/CSS 技能） ----------
    ("JavaScript基础", "HTML/CSS"),
    ("DOM与浏览器API", "JavaScript基础"),
    ("前端性能优化", "DOM与浏览器API"),

    # ---------- 大数据 ----------
    ("Hive与HBase", "Hadoop与HDFS基础"),
    ("Spark计算与性能调优", "Hadoop与HDFS基础"),
    ("消息队列与流处理", "Spark计算与性能调优"),
    ("数据仓库与数据质量", "Hive与HBase"),
    ("大数据架构设计", "Spark计算与性能调优"),
    ("大数据架构设计", "数据仓库与数据质量"),

    # ---------- Android ----------
    ("Android生命周期与状态管理", "Android组件与Intent"),
    ("Android数据存储方式", "Android组件与Intent"),
    ("Android消息机制", "Android生命周期与状态管理"),
    ("Android UI与性能优化", "Android生命周期与状态管理"),
    ("Android内存管理与调试", "Android UI与性能优化"),

    # ---------- iOS ----------
    ("iOS内存管理", "Objective-C语言特性"),
    ("iOS设计模式", "Objective-C语言特性"),
    ("iOS并发与UI基础", "iOS设计模式"),
    ("iOS并发与UI基础", "进程与线程基础"),
    ("iOS数据持久化", "iOS设计模式"),

    # ---------- 运维（服务器监控/安全运维 依赖已有的 Linux基础 技能） ----------
    ("服务器监控与告警", "Linux基础"),
    ("系统安全运维", "Linux基础"),
    ("系统高可用与容灾", "服务器监控与告警"),
    ("故障排查与应急响应", "服务器监控与告警"),

    # ---------- 网络安全 ----------
    ("身份认证与访问控制", "密码学基础"),
    ("网络攻击类型与防御", "身份认证与访问控制"),
    ("安全事件应急响应与取证", "网络攻击类型与防御"),
    ("安全合规与风险管理", "网络攻击类型与防御"),
    ("物联网与新兴技术安全", "网络攻击类型与防御"),

    # ---------- 数据分析（pandas 依赖已有的 Python核心语法 技能） ----------
    ("数据清洗与描述性统计", "Excel数据分析"),
    ("数据可视化", "数据清洗与描述性统计"),
    ("pandas数据处理", "Python核心语法与内置类型"),
    ("统计建模与回归分析", "pandas数据处理"),
    ("统计建模与回归分析", "数据清洗与描述性统计"),

    # ---------- 数据库运维（数据库设计与建模 依赖已有的 MySQL 技能） ----------
    ("数据库设计与建模", "MySQL"),
    ("数据库事务与一致性", "数据库设计与建模"),
    ("数据库性能调优", "数据库事务与一致性"),
    ("数据库高可用与容灾", "数据库性能调优"),
    ("数据库安全管理", "数据库设计与建模"),
    ("数据库迁移与运维协作", "数据库高可用与容灾"),

    # ---------- 游戏开发 ----------
    ("游戏物理与碰撞检测", "游戏引擎渲染与性能优化"),
    ("游戏动画与特效系统", "游戏引擎渲染与性能优化"),
    ("游戏网络同步", "游戏引擎渲染与性能优化"),
    ("游戏脚本与并发编程", "游戏引擎渲染与性能优化"),
    ("游戏脚本与并发编程", "进程与线程基础"),
]


def build_sql():
    lines = [
        "-- ============================================================",
        "-- 由 tools/import_skill_prereq_expanded.py 生成",
        "-- 给技能词典扩充新增的 62 个技能补依赖边，让③学习路径引擎能对这些方向做",
        "-- 真正的拓扑分层，不再是每个岗位类别都挤在一个大阶段里。",
        "-- 跟 skill_prereq(skill_id, prereq_skill_id) 一样的语义：skill 依赖 prereq，",
        "-- 即先学 prereq 再学 skill。用 INSERT IGNORE 保证重复执行安全",
        "-- （skill_prereq 主键是 (skill_id, prereq_skill_id) 复合唯一约束）。",
        "-- ============================================================",
        "",
        "SET NAMES utf8mb4;",
        "",
    ]
    for skill_name, prereq_name in EDGES:
        lines.append(
            "INSERT IGNORE INTO skill_prereq (skill_id, prereq_skill_id) VALUES ("
            f"(SELECT id FROM skills WHERE name = {esc(skill_name)} LIMIT 1), "
            f"(SELECT id FROM skills WHERE name = {esc(prereq_name)} LIMIT 1));"
            f"  -- {skill_name} 依赖 {prereq_name}"
        )
    return "\n".join(lines) + "\n"


def main():
    sql = build_sql()
    OUT.write_text(sql, encoding="utf-8")
    print(f"共 {len(EDGES)} 条依赖边")
    print(f"已写入 {OUT}")


if __name__ == "__main__":
    main()
