# -*- coding: utf-8 -*-
"""
skill_extractor 的回归测试：每条对应一个曾经抽错、或者容易抽错的写法。

用法：python -m unittest discover -s tools -p "test_*.py"
"""

import re
import unittest
from pathlib import Path

from skill_extractor import RULES, extract, extract_detail

ROOT = Path(__file__).resolve().parent.parent

PYTHON_SKILLS = {
    "Python核心语法与内置类型", "Python内存管理与对象模型", "Python正则表达式",
    "Python异常处理", "Python网络编程", "Python文件与编码处理",
}


class PythonPostingTest(unittest.TestCase):
    def test_linux_is_extracted_from_python_posting(self):
        skills = extract("Python开发工程师",
                         "岗位要求：1、熟练掌握Python，熟悉Django、Flask等Web框架；2、熟悉Linux操作系统，会写Shell脚本。")
        self.assertIn("Linux基础", skills)
        self.assertIn("Python核心语法与内置类型", skills)

    def test_generic_python_maps_to_core_skill_only(self):
        skills = extract("Python开发工程师", "熟练掌握Python语言，有2年以上开发经验。")
        self.assertEqual(PYTHON_SKILLS & set(skills), {"Python核心语法与内置类型"})

    def test_specific_python_topics_need_explicit_wording(self):
        skills = extract("Python爬虫工程师", "精通Python，熟悉正则表达式、XPath，了解HTTP协议。")
        self.assertIn("Python正则表达式", skills)
        self.assertIn("Python网络编程", skills)
        self.assertNotIn("Python异常处理", skills)
        self.assertNotIn("Python文件与编码处理", skills)

    def test_python_topic_without_python_is_not_extracted(self):
        skills = extract("Java开发工程师", "熟悉Java，了解正则表达式。")
        self.assertNotIn("Python正则表达式", skills)


class KeywordBoundaryTest(unittest.TestCase):
    def test_javascript_is_not_java(self):
        skills = extract("前端开发工程师", "熟悉JavaScript、Vue.js，了解JSON格式。")
        self.assertIn("JavaScript基础", skills)
        self.assertNotIn("Java基础", skills)

    def test_nosql_is_not_mysql(self):
        self.assertNotIn("MySQL", extract("", "熟悉NoSQL数据库，如MongoDB。"))
        self.assertIn("MySQL", extract("", "能编写复杂SQL语句。"))

    def test_linux_operating_system_is_not_os_theory(self):
        self.assertNotIn("进程与线程基础", extract("运维工程师", "熟悉Linux操作系统的安装与配置。"))
        self.assertNotIn("进程与线程基础", extract("运维工程师", "熟悉操作系统（Windows、Linux）。"))
        self.assertIn("进程与线程基础", extract("", "计算机基础扎实，熟悉操作系统原理。"))

    def test_market_penetration_is_not_security(self):
        self.assertNotIn("网络攻击类型与防御", extract("销售经理", "负责提升区域市场渗透率。"))

    def test_soft_skills_are_not_extracted(self):
        self.assertEqual(extract("Java开发工程师", "具有良好的沟通能力和团队合作精神。"), ["Java基础"])


class ContextTest(unittest.TestCase):
    def test_multithreading_follows_language(self):
        self.assertIn("Java并发", extract("Java开发工程师", "熟悉Java多线程编程。"))
        skills = extract("Python开发工程师", "熟悉Python多线程编程。")
        self.assertIn("进程与线程基础", skills)
        self.assertNotIn("Java并发", skills)

    def test_product_manager_mentioning_mobile_platforms(self):
        skills = extract("产品经理", "熟悉iOS、安卓系统特性，熟悉产品交互设计规范。")
        self.assertNotIn("Android组件与Intent", skills)
        self.assertNotIn("iOS并发与UI基础", skills)

    def test_android_developer(self):
        self.assertIn("Android组件与Intent", extract("Android开发工程师", "负责公司App的开发与维护。"))

    def test_frontend_title_implies_html_and_js(self):
        skills = extract("Web前端开发工程师", "负责公司产品页面开发。")
        self.assertIn("HTML/CSS", skills)
        self.assertIn("JavaScript基础", skills)
        self.assertEqual(extract("IC前端验证工程师", "负责芯片验证。"), [])

    def test_unit_testing_by_developers_is_not_testing_skill(self):
        self.assertNotIn("软件测试方法与工具", extract("Java开发工程师", "负责模块编码和单元测试，配合测试上线。"))
        self.assertIn("软件测试方法与工具", extract("软件测试工程师", "编写测试用例，熟悉JMeter。"))


class PreferredTest(unittest.TestCase):
    def test_preferred_clause_is_not_required(self):
        detail = extract_detail("Java开发工程师", "熟悉MySQL，有Redis使用经验者优先。")
        self.assertEqual(detail["MySQL"][0], "required")
        self.assertEqual(detail["Redis"][0], "preferred")
        self.assertNotIn("Redis", extract("Java开发工程师", "熟悉MySQL，有Redis使用经验者优先。"))

    def test_preferred_covers_comma_separated_list(self):
        skills = extract("Python开发工程师", "熟悉Pandas，Numpy，Arima等数据分析库优先。")
        self.assertNotIn("pandas数据处理", skills)
        skills = extract("Python开发工程师", "有数据分析经验者优先，如 Pandas，MapReduce，Spark。")
        self.assertNotIn("Spark计算与性能调优", skills)

    def test_new_requirement_after_comma_is_separate(self):
        skills = extract("Python开发工程师", "熟悉Flask或Django等Web框架，有Redis经验优先。")
        self.assertIn("Python核心语法与内置类型", skills)
        self.assertNotIn("Redis", skills)

    def test_bonus_section_header(self):
        skills = extract("Java开发工程师", "任职要求：\n熟悉Spring Boot\n加分项：\n熟悉Kafka\n熟悉Redis")
        self.assertIn("Spring Boot", skills)
        self.assertNotIn("消息队列与流处理", skills)
        self.assertNotIn("Redis", skills)

    def test_company_intro_is_ignored(self):
        skills = extract("Java开发工程师", "任职要求：熟悉Java\n公司介绍：\n我们打造高可用、负载均衡的云平台")
        self.assertNotIn("系统高可用与容灾", skills)

    def test_intro_section_ends_at_next_header(self):
        skills = extract("后端工程师", "关于我们 *我们负责稳定性体系建设 我们希望你 *熟悉Linux系统、TCP/IP")
        self.assertIn("Linux基础", skills)

    def test_single_line_info_does_not_hide_requirements(self):
        skills = extract("Python开发工程师", "工作地点：广州市\n要求：\n1、熟悉Linux平台环境的开发")
        self.assertIn("Linux基础", skills)

    def test_trailing_preferred_header(self):
        skills = extract("运维工程师", "熟悉Windows\n具备以下三点者优先：\n1、有WMS系统经验\n2、了解一些Linux常用命令")
        self.assertNotIn("Linux基础", skills)


class AnyOfLanguageTest(unittest.TestCase):
    def test_keeps_the_language_named_in_title(self):
        skills = extract("Python开发工程师", "熟悉Java/Python/Go其中一门编程语言。")
        self.assertIn("Python核心语法与内置类型", skills)
        self.assertNotIn("Java基础", skills)
        self.assertNotIn("Go基础语法与数据类型", skills)

    def test_keeps_first_when_all_are_known(self):
        skills = extract("大数据开发工程师", "熟练掌握Java、Python至少一种编程语言。")
        self.assertIn("Java基础", skills)
        self.assertNotIn("Python核心语法与内置类型", skills)

    def test_none_required_when_list_has_languages_outside_dictionary(self):
        self.assertNotIn("Java基础", extract("Python开发工程师", "了解多种开发语言，如C/C++，Java等。"))

    def test_plain_list_is_not_any_of(self):
        skills = extract("Java开发工程师", "精通Java、JSP、JavaScript、HTML、CSS等Java Web开发技术。")
        self.assertIn("Java基础", skills)
        self.assertIn("JavaScript基础", skills)


class SkillNamesTest(unittest.TestCase):
    def test_rule_skills_exist_in_seed_data(self):
        """规则里的技能名必须跟 skills 表一致，不然生成的 SQL 按名字 JOIN 不上，会被悄悄丢掉。"""
        seeded = set()
        seed = (ROOT / "database" / "seed.sql").read_text(encoding="utf-8")
        seeded.update(re.findall(r"\(\s*\d+\s*,\s*'([^']+)'", seed.split("INSERT INTO skills", 1)[1].split(";", 1)[0]))
        expanded = (ROOT / "database" / "seed_expanded_skills_and_questions.sql").read_text(encoding="utf-8")
        seeded.update(re.findall(r"INSERT INTO skills \([^)]*\) SELECT '([^']+)'", expanded))
        names = {r.skill for r in RULES} | {r.fallback for r in RULES if r.fallback}
        self.assertEqual(names - seeded, set())


if __name__ == "__main__":
    unittest.main()
