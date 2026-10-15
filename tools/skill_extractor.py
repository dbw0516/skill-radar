# -*- coding: utf-8 -*-
"""
招聘信息（JD）技能抽取：岗位标题 + JD 正文 → skills 表里的技能标准名。

为什么不直接拿 skills.aliases 做子串匹配：
  - aliases 是扩充技能词典时从面试题里提炼的"考点词"（"this"、"缓存"、"动画"、
    "category"、"多线程"……），拿来扫 JD 会大量误命中，同一个词还挂在好几个技能上；
  - 技能粒度是按面试题划的：Python 方向是"核心语法/正则/网络编程/内存管理……"这种
    考点，JD 里只会写"熟悉 Python"。如果把 Python 这个词同时映射到这几个技能，
    每条 Python 岗位都会固定带出同一串 Python* 技能——这正是以前的毛病。
    这里的做法是：泛指的语言/平台只落到该方向的"基础"技能上（Python → Python核心语法
    与内置类型），细分技能只有 JD 里出现了对应的具体说法（正则、socket……）才抽。

规则写法（RULES，每条规则对应一个技能，同一个技能可以有几条语境不同的规则）：
  en       英文关键词，自动加词边界（"mysql" 里的 "sql" 不算 SQL，"javascript" 不算 Java）
  zh       中文/混合模式，原样匹配
  title    只在岗位标题里匹配的模式（如标题写"前端"就隐含要 HTML/CSS、JS）
  mask     匹配前先抹掉的片段（"Linux操作系统"不该算成"操作系统"原理）
  context  同一个分句里还必须出现的词（"生命周期"要跟 Android/Activity 一起出现才算）
  not_context 同一个分句里出现这些词就不算（"统计学"出现在"统计学等相关专业"里是专业要求）
  requires 这条 JD 里必须已经抽到另一个技能才算（"正则"只在要 Python 的岗位里算 Python正则）
  fallback requires 不满足时改记到这个技能（"多线程"：要 Java → Java并发，否则 → 进程与线程基础）

JD 先按句号、分号、换行、"1."/"（2）"这类序号切句，句内再按逗号切——逗号后面不是
"熟悉/掌握/有……"这类另起一条要求的开头，就算同一条（"熟悉 Pandas，Numpy 等优先"）。
每个分句判断是"必需"还是"加分"（含"优先/加分/更佳"等字样，或者落在"加分项：""具备以下
条件者优先："这类小标题后面）；"公司介绍""福利待遇"段落到下一个"任职要求"类标题前整段跳过：
  required  必需技能
  preferred 加分项（"有 Redis 经验者优先"）——默认不写进 posting_skills，见
            tools/extract_posting_skills.py
"熟悉 Java/Python/Go 其中一门"这种"任选其一"的语言列表，只保留一个当必需
（标题/别处已经点名的那门；都没点名就取第一个；列表里还有 C++/PHP 这类词典外的语言，
说明会那些也行，就一门都不算必需），其余算加分项，不然一个 Python 岗位会因为列了一串
可选语言而"要求"同时会 Java 和 Go。

职业素养与求职技巧（沟通能力、团队协作）几乎每条 JD 都写，不是技术技能，不抽。

用法（调试单条 JD）：
    python tools/skill_extractor.py              粘贴 JD，单独一行 END 结束
    python tools/skill_extractor.py "标题" < jd.txt
"""

import re
import sys
import unicodedata
from collections import OrderedDict

REQUIRED = "required"
PREFERRED = "preferred"

# 英文词边界：前面不能紧挨字母/数字，后面不能紧挨字母（数字可以：html5、python3、vue3）
_L = r"(?<![a-z0-9])"
_R = r"(?![a-z])"

# 引导"开发"语境的词：Android/iOS 这种平台名在产品经理 JD 里也常出现（"熟悉 iOS、安卓设计规范"），
# 只有跟开发相关的词出现在同一分句里才算开发技能
_MOBILE_DEV = r"开发|编程|sdk|源码|framework|ndk|jni|kotlin|组件|系统架构|底层|性能|调试|工程师|客户端开发|xcode|uikit"
# 信息安全语境：单个"安全"不够（"安全稳定运行"、交通/生产安全工程师都有）
_SECURITY = (r"(?:信息|网络|数据|系统|应用|主机|云|web|终端)安全|安全(?:产品|运营|服务|技术|漏洞|事件|防护|攻防|研究|开发|工程师)"
             r"|攻击|入侵|威胁|漏洞|病毒|木马|渗透|取证|攻防|等保|等级保护")


class Rule(object):
    def __init__(self, skill, en=(), zh=(), title=(), mask=(), context=None, not_context=None,
                 requires=None, fallback=None):
        self.skill = skill
        pats = [_L + p + _R for p in en] + list(zh)
        self.pattern = re.compile("|".join("(?:%s)" % p for p in pats)) if pats else None
        self.title_pattern = re.compile("|".join("(?:%s)" % p for p in title)) if title else None
        self.mask = re.compile("|".join("(?:%s)" % p for p in mask)) if mask else None
        self.context = re.compile(context) if context else None
        self.not_context = re.compile(not_context) if not_context else None
        self.requires = requires
        self.fallback = fallback


RULES = [
    # ---------------- 通用编程基础 ----------------
    Rule("Java基础",
         en=[r"java(?:ee|se|web)?(?!\s?script)", r"j2ee", r"j2se", r"jvm", r"jdk", r"jsp"]),
    Rule("Python核心语法与内置类型",
         en=[r"python", r"pyhon"]),  # pyhon：数据集里真有这种笔误的标题
    Rule("Go基础语法与数据类型",
         en=[r"golang", r"go(?=\s*(?:语言|开发|后端|后台|工程师|/|、|`|或|和|及|等|\)|$))",
             r"(?<=[/、(`])\s*go"],
         zh=[r"go语言"]),
    Rule("JavaScript基础",
         en=[r"javascript", r"java\s?script", r"js", r"es[56]", r"ecmascript", r"typescript",
             r"jquery", r"ajax", r"node\.?\s?js", r"vue(?:\.?js)?", r"react(?:\s?native)?",
             r"angular(?:js)?", r"webpack"],
         title=[r"(?:web|h5)?前端(?:开发|工程师|研发|岗|$)"],
         mask=[r"(?:ic|soc|芯片|数字)\s*前端", r"前端(?:设计|验证)工程师", r"前端产品经理"]),
    Rule("HTML/CSS",
         en=[r"html", r"css", r"h5", r"sass", r"scss", r"bootstrap", r"tailwind"],
         zh=[r"div\s*\+\s*css", r"网页布局", r"页面布局", r"响应式布局"],
         title=[r"(?:web|h5)?前端(?:开发|工程师|研发|岗|$)"],
         mask=[r"(?:ic|soc|芯片|数字)\s*前端", r"前端(?:设计|验证)工程师", r"前端产品经理"]),
    Rule("面向对象",
         en=[r"oop", r"ood"],
         zh=[r"面向对象", r"设计模式"],
         # iOS 方向的设计模式单独有技能
         mask=[r"ios[^,;。]{0,10}设计模式"]),
    Rule("数据结构",
         zh=[r"数据结构", r"常[用见]算法", r"基础算法", r"算法基础", r"排序算法", r"leetcode", r"刷题"],
         # "参与设计数据结构、维护数据内容"说的是数据库表结构，不是数据结构与算法
         mask=[r"数据结构设计", r"设计数据结构"]),
    Rule("Linux基础",
         en=[r"linux", r"unix", r"shell", r"centos", r"ubuntu", r"red\s?hat", r"rhel", r"debian",
             r"bash", r"suse", r"aix", r"solaris"]),
    Rule("Git",
         en=[r"git(?:hub|lab|ee)?", r"svn"],
         zh=[r"版本控制", r"版本管理(?:工具|软件|軟體|系统)", r"代码管理工具"]),

    # ---------------- 数据库 ----------------
    # MySQL 是技能词典里唯一的关系型数据库/SQL 技能，Oracle/SQL Server/PostgreSQL、写 SQL 都落到它上面
    Rule("MySQL",
         en=[r"mysql", r"sql", r"oracle", r"sql\s?server", r"postgre(?:sql)?", r"pgsql", r"mariadb"],
         zh=[r"关系型数据库", r"关系数据库"]),
    Rule("Redis", en=[r"redis"]),
    Rule("数据库性能调优",
         zh=[r"sql\s*(?:语句)?(?:优化|调优)", r"慢查询", r"索引(?:优化|设计|原理)", r"执行计划",
             r"数据库(?:性能)?(?:优化|调优)", r"数据库性能"]),
    Rule("数据库高可用与容灾",
         en=[r"rac", r"data\s?guard", r"mha", r"binlog", r"xtrabackup", r"rman"],
         zh=[r"主从(?:复制|同步|架构|切换)?", r"读写分离", r"数据库(?:的)?(?:备份|容灾|高可用|集群)",
             r"备份(?:与|和|及)?恢复"]),
    Rule("数据库事务与一致性",
         en=[r"acid", r"mvcc"],
         zh=[r"分布式事务", r"数据库事务", r"事务(?:隔离|处理|机制|原理|特性|控制)", r"隔离级别", r"两阶段提交"]),
    Rule("数据库安全管理",
         zh=[r"数据库(?:的)?(?:安全|权限|审计|加密)", r"数据脱敏"]),
    Rule("数据库设计与建模",
         en=[r"e-?r图"],
         zh=[r"数据库(?:表)?(?:结构)?设计", r"表结构(?:设计)?", r"库表设计", r"数据表设计", r"数据库建模",
             r"三大?范式", r"数据结构设计", r"设计数据结构"]),
    Rule("数据库迁移与运维协作",
         en=[r"dba"],
         zh=[r"数据(?:库)?迁移", r"数据库运维", r"数据库(?:的)?日常(?:维护|管理)",
             r"数据库(?:的)?(?:安装|部署|升级|监控|巡检|维护|管理工作)"]),

    # ---------------- Java 后端 ----------------
    Rule("Spring Boot", en=[r"spring(?:\s?(?:boot|mvc|cloud))?", r"ssm"]),
    Rule("MyBatis", en=[r"mybatis(?:-?plus)?", r"ibatis", r"ssm"]),
    # "多线程"跟着语言走：要 Java 的岗位记 Java并发，否则记进程与线程基础；"高并发"说的是系统架构，不算
    Rule("Java并发",
         en=[r"juc", r"concurrent"],
         zh=[r"多线程", r"并发(?:编程|处理|控制|机制|包)", r"线程池", r"线程安全"],
         mask=[r"[高大]并发"],
         requires="Java基础", fallback="进程与线程基础"),
    Rule("进程与线程基础", zh=[r"多进程", r"进程间通信", r"进程(?:与|和|、|/)线程"]),
    # 运维岗写"熟悉操作系统（Windows、Linux）"是会装会用，不是操作系统原理；要跟"原理/计算机基础"一类说法一起出现才算
    Rule("进程与线程基础",
         zh=[r"操作系统"],
         mask=[r"(?:linux|unix|aix|windows|centos|server|服务器|国产|嵌入式|实时|android|ios|鸿蒙|手机|移动)\s*(?:等)?操作系统"],
         context=r"原理|基础|知识|数据结构|计算机网络|网络原理|编译|组成|体系结构|内核|进程|线程|内存管理"),

    # ---------------- Python（泛指只落到核心语法，细分技能要有具体说法） ----------------
    Rule("Python正则表达式", zh=[r"正则"], requires="Python核心语法与内置类型"),
    Rule("Python网络编程",
         en=[r"socket", r"smtp"],
         zh=[r"网络编程", r"tcp/ip", r"tcp协议", r"http协议", r"网络协议"],
         requires="Python核心语法与内置类型"),
    Rule("Python异常处理",
         zh=[r"异常处理", r"异常捕获", r"try\s*except"],
         context=r"python", requires="Python核心语法与内置类型"),
    Rule("Python内存管理与对象模型",
         en=[r"gc"],
         zh=[r"垃圾回收", r"内存管理", r"引用计数"],
         context=r"python", requires="Python核心语法与内置类型"),
    Rule("Python文件与编码处理",
         zh=[r"文件(?:处理|操作|读写)", r"字符编码", r"编码转换"],
         context=r"python", requires="Python核心语法与内置类型"),

    # ---------------- Go ----------------
    Rule("Go Channel与并发通信", en=[r"channel", r"goroutine", r"csp"], requires="Go基础语法与数据类型"),
    Rule("Go调度器与GMP模型", en=[r"gmp"], zh=[r"调度器"], requires="Go基础语法与数据类型"),
    Rule("Go锁与原子操作", en=[r"mutex", r"atomic"], zh=[r"互斥锁", r"原子操作"],
         requires="Go基础语法与数据类型"),
    Rule("Go切片数组与拷贝机制", en=[r"slice"], zh=[r"切片", r"深浅拷贝"], context=r"go",
         requires="Go基础语法与数据类型"),
    Rule("Go Map实现原理", en=[r"hmap"], requires="Go基础语法与数据类型"),
    Rule("Go内存管理机制", zh=[r"内存(?:分配|逃逸|对齐)", r"垃圾回收"], context=r"go",
         requires="Go基础语法与数据类型"),

    # ---------------- 前端 ----------------
    Rule("DOM与浏览器API",
         en=[r"dom", r"bom"],
         zh=[r"浏览器(?:的)?(?:兼容|原理|渲染|内核|api|存储|缓存|工作原理)", r"跨浏览器"]),
    Rule("前端性能优化",
         zh=[r"前端性能", r"页面性能", r"首屏", r"加载(?:速度|性能)", r"渲染性能", r"web性能", r"性能优化"],
         context=r"前端|页面|网页|h5|web|vue|react|javascript|浏览器|首屏|(?<![a-z])js(?![a-z])"),

    # ---------------- 大数据 ----------------
    Rule("Hadoop与HDFS基础", en=[r"hadoop", r"hdfs", r"map\s?reduce", r"cdh", r"hdp", r"ambari"]),
    Rule("Spark计算与性能调优", en=[r"spark"]),
    Rule("消息队列与流处理",
         en=[r"kafka", r"flume", r"flink", r"storm", r"rabbitmq", r"rocketmq", r"activemq", r"zeromq",
             r"pulsar", r"mq", r"logstash"],
         zh=[r"消息队列", r"消息中间件", r"流处理", r"流式(?:计算|处理)", r"实时计算"]),
    Rule("Hive与HBase", en=[r"hive", r"hbase", r"impala", r"hql"]),
    Rule("数据仓库与数据质量",
         en=[r"etl", r"kettle", r"datax"],
         zh=[r"数据仓库", r"数仓", r"数据质量", r"数据治理", r"维度建模", r"数据集市"]),
    Rule("大数据架构设计",
         zh=[r"大数据(?:平台)?(?:的)?架构", r"大数据(?:平台|系统)(?:的)?(?:设计|规划|搭建|建设)",
             r"大数据解决方案", r"技术选型"],
         context=r"大数据|hadoop|spark|flink|hive|hbase|kafka|数仓|数据平台"),

    # ---------------- 移动端 ----------------
    # 泛指"Android 开发"落到四大组件这个基础技能；产品经理 JD 里的"熟悉 iOS、安卓设计规范"不算
    Rule("Android组件与Intent",
         en=[r"android", r"andriod", r"andorid", r"kotlin"],
         zh=[r"安卓", r"四大组件"],
         title=[r"android", r"安卓"],
         context=_MOBILE_DEV),
    Rule("Android数据存储方式",
         en=[r"sqlite", r"sharedpreferences", r"contentprovider"],
         requires="Android组件与Intent"),
    Rule("Android消息机制", en=[r"handler", r"looper", r"messagequeue"], zh=[r"消息机制"],
         requires="Android组件与Intent"),
    Rule("Android生命周期与状态管理", zh=[r"生命周期"], context=r"activity|fragment|android|安卓|组件",
         requires="Android组件与Intent"),
    Rule("Android UI与性能优化",
         en=[r"anr", r"recyclerview", r"listview"],
         zh=[r"自定义(?:view|控件)", r"ui(?:优化|性能)", r"布局优化", r"卡顿", r"流畅度", r"启动速度", r"性能优化"],
         context=r"android|安卓|app|客户端|移动|自定义|ui|anr|recyclerview|listview|卡顿|流畅|启动",
         requires="Android组件与Intent"),
    Rule("Android内存管理与调试",
         en=[r"jni", r"ndk", r"oom", r"leakcanary"],
         zh=[r"内存(?:泄漏|泄露|优化|管理|溢出)"],
         context=r"android|安卓|app|客户端|移动|jni|ndk|oom|leakcanary",
         requires="Android组件与Intent"),
    # iOS：泛指落到 UI 基础，Objective-C 单独一个技能
    Rule("iOS并发与UI基础", en=[r"ios"], title=[r"ios"], context=_MOBILE_DEV),
    # swift 也是 OpenStack 的对象存储（运维 JD 里有），要跟 iOS 相关的词一起出现才算
    Rule("iOS并发与UI基础",
         en=[r"uikit", r"swiftui", r"xcode", r"cocoa", r"gcd", r"nsoperation(?:queue)?", r"auto\s?layout", r"swift"],
         context=r"ios|苹果|uikit|swiftui|xcode|cocoa|nsoperation|auto\s?layout|gcd"),
    Rule("Objective-C语言特性", en=[r"objective-?c", r"objc"]),
    Rule("iOS内存管理", en=[r"arc", r"mrc", r"autoreleasepool"], zh=[r"自动释放池", r"内存管理"],
         requires="iOS并发与UI基础"),
    Rule("iOS设计模式", en=[r"kvo", r"kvc", r"mvvm", r"delegate"], zh=[r"设计模式"],
         context=r"ios|objective|objc|swift|kvo|kvc", requires="iOS并发与UI基础"),
    Rule("iOS数据持久化", en=[r"core\s?data", r"sqlite", r"fmdb", r"nsuserdefaults"], zh=[r"数据持久化"],
         requires="iOS并发与UI基础"),

    # ---------------- 测试 ----------------
    # 开发岗的"单元测试/联调测试/配合测试"是开发本职工作，不算测试方向技能
    # 测试类别里混着不少硬件/品质岗（电池、射频、ATE、"QA/目检/质检"），"测试报告""功能测试"这类说法
    # 两边都用，要有软件语境才算；标题里的 QA 也大多是品质岗，不当信号
    Rule("软件测试方法与工具",
         en=[r"selenium", r"jmeter", r"loadrunner", r"appium", r"postman", r"testlink", r"pytest"],
         zh=[r"软件测试", r"测试用例", r"自动化测试", r"接口测试", r"黑盒", r"白盒", r"回归测试",
             r"缺陷管理", r"bug管理", r"禅道"],
         title=[r"app测试", r"测试开发"],
         mask=[r"小黑盒"],  # 一个 App 的名字
         # 爬虫岗也用 Selenium（"熟悉 Scrapy、Selenium 等爬虫框架"）
         not_context=r"爬虫|scrapy|抓取|采集"),
    Rule("软件测试方法与工具",
         zh=[r"测试(?:计划|方案|报告|工具|方法|流程|理论|开发)", r"功能测试", r"性能测试", r"压力测试", r"压测",
             r"集成测试", r"系统测试"],
         context=r"软件|app|web|接口|自动化|用例|bug|缺陷|程序|代码|脚本|python|java|sql|linux|服务端|前端|后端"),

    # ---------------- 运维 ----------------
    Rule("服务器监控与告警",
         en=[r"zabbix", r"prometheus", r"nagios", r"grafana", r"open-?falcon", r"cacti"],
         zh=[r"监控告警", r"告警", r"监控"],
         mask=[r"(?:风险|舆情|质量|数据|价格|市场|视频|安防|舆论|品牌|竞品|销售|产品|运营|业务|用户|指标|前端)(?:的)?监控",
               r"监控(?:摄像|设备|录像|视频)"],
         context=r"服务器|集群|运维|主机|机房|告警|日志|网络|数据库|中间件|线上|运行状态|系统运行|服务运行"
                 r"|(?:性能|应用|服务|系统|平台)监控|zabbix|prometheus|nagios|grafana"),
    Rule("系统高可用与容灾",
         en=[r"keepalived", r"lvs", r"haproxy", r"nginx"],
         zh=[r"高可用", r"容灾", r"负载均衡", r"灾备", r"双活"]),
    Rule("系统安全运维",
         zh=[r"安全加固", r"系统加固", r"补丁管理", r"打补丁", r"安全审计", r"安全基线", r"堡垒机",
             r"安全运维", r"日志审计"]),
    Rule("故障排查与应急响应",
         en=[r"troubleshoot(?:ing)?"],
         zh=[r"故障(?:的)?(?:排查|排除|定位|处理|诊断|分析|修复)", r"排障", r"应急(?:处理|预案|响应)",
             r"突发(?:事件|故障)", r"线上问题", r"事故处理"],
         # 硬件/设备方向的"现场故障诊断"不是 IT 运维；安全方向的"应急响应"另有技能
         mask=[r"(?:设备|硬件|现场|机械|电气|车辆|机器|电路|产线)(?:的)?(?:常见)?故障"],
         not_context=_SECURITY),

    # ---------------- 网络安全 ----------------
    Rule("网络攻击类型与防御",
         en=[r"ddos", r"xss", r"csrf", r"waf", r"ctf", r"apt(?!-)", r"0day", r"ids", r"ips"],
         zh=[r"渗透", r"漏洞(?:挖掘|扫描|分析|利用|研究|检测|修复|验证)", r"攻防", r"sql注入", r"注入攻击",
             r"web安全", r"入侵(?:检测|防御|对抗|防护)", r"恶意代码", r"木马", r"病毒分析", r"零日",
             r"红队", r"蓝队", r"威胁情报", r"安全测试"],
         mask=[r"渗透率", r"市场渗透", r"渗透(?:到|进)"]),
    Rule("身份认证与访问控制",
         en=[r"vpn", r"iam", r"2fa", r"sso", r"oauth\s?2?", r"ldap"],
         zh=[r"防火墙", r"身份认证", r"访问控制", r"零信任", r"多因素认证", r"单点登录", r"权限(?:控制|体系)"]),
    Rule("密码学基础",
         en=[r"(?:open)?ssl", r"tls", r"pki", r"rsa", r"aes", r"sm[234]"],
         zh=[r"密码学", r"加解密", r"加密算法", r"加密技术", r"对称加密", r"非对称加密",
             r"(?:哈希|hash)(?:函数|算法)", r"数字证书", r"数字签名", r"国密", r"密码(?:安全|技术|算法)"]),
    Rule("安全事件应急响应与取证",
         zh=[r"应急响应", r"取证", r"溯源", r"安全事件", r"事件响应"],
         mask=[r"追溯"],
         context=_SECURITY + r"|取证|溯源|事件"),
    Rule("安全合规与风险管理",
         en=[r"gdpr", r"iso\s?27001"],
         zh=[r"等保", r"等级保护", r"安全合规", r"隐私合规", r"合规(?:性)?(?:检查|审计|建设|要求|标准)",
             r"风险评估", r"安全评估", r"安全管理体系"],
         context=_SECURITY + r"|合规|等保|等级保护|隐私|gdpr|27001"),
    Rule("物联网与新兴技术安全",
         zh=[r"(?:物联网|iot|工控|车联网|区块链|云|ai|大模型|智能汽车|智能网联)安全"]),

    # ---------------- 数据分析 ----------------
    Rule("Excel数据分析", en=[r"excel", r"vlookup", r"vba"], zh=[r"数据透视", r"透视表"]),
    Rule("pandas数据处理", en=[r"pandas", r"numpy", r"dataframe"]),
    Rule("数据清洗与描述性统计",
         zh=[r"清洗", r"数据预处理", r"描述性?统计", r"缺失值", r"异常值"],
         context=r"数据|统计|etl|日志|缺失|异常值"),
    # "计算机、数学、统计学等相关专业"是专业要求，不是技能
    Rule("数据清洗与描述性统计",
         zh=[r"统计学", r"统计分析", r"统计(?:知识|基础|原理)"],
         not_context=r"专业|学历|本科|硕士|博士|学位|毕业"),
    Rule("数据可视化",
         en=[r"tableau", r"power\s?bi", r"matplotlib", r"echarts", r"seaborn", r"plotly", r"superset",
             r"fine\s?report", r"fine\s?bi", r"quick\s?bi", r"d3(?:\.js)?"],
         zh=[r"可视化", r"帆软", r"数据看板", r"数据大屏"]),
    Rule("统计建模与回归分析",
         en=[r"spss", r"sas", r"stata", r"eviews"],
         zh=[r"回归分析", r"线性回归", r"逻辑回归", r"回归模型", r"统计建模", r"统计模型", r"假设检验",
             r"方差分析", r"时间序列", r"计量经济", r"多元统计", r"r语言",
             # 单独一个字母 R 只在语言列表里才算（"Python/R"、"SPSS、R"），避免误中 R&D、标题里的编号
             r"(?:[/、,(]|或|和|及)\s*r(?![a-z&0-9])"]),

    # ---------------- 游戏开发 ----------------
    Rule("游戏引擎渲染与性能优化",
         en=[r"unity(?:\s?3d)?", r"unreal", r"ue[45]", r"cocos(?:2d|creator)?", r"shader", r"opengl",
             r"directx", r"vulkan"],
         zh=[r"游戏引擎", r"渲染管线"]),
    Rule("游戏物理与碰撞检测", en=[r"box2d", r"physx", r"havok"], zh=[r"物理引擎", r"碰撞检测"]),
    Rule("游戏动画与特效系统", en=[r"spine"], zh=[r"动画状态机", r"骨骼动画", r"粒子系统", r"特效系统"]),
    Rule("游戏网络同步", en=[r"lockstep"], zh=[r"帧同步", r"状态同步", r"网络同步", r"预测回滚"]),
    Rule("游戏脚本与并发编程", en=[r"lua"], zh=[r"游戏脚本"]),
]

# "任选其一"语境里会互相替代的编程语言
LANGUAGE_SKILLS = {
    "Java基础", "Python核心语法与内置类型", "Go基础语法与数据类型", "JavaScript基础", "Objective-C语言特性",
}
# 技能词典里没有、但会出现在"任选一门"列表里的语言——列表里有它们，说明会它们也行
_OTHER_LANGUAGE = re.compile(
    r"(?<![a-z0-9-])(?:c\+\+|c#|c(?![a-z0-9+#端])(?!/s(?![a-z]))|php|\.net|ruby|scala|rust|perl|matlab"
    r"|delphi|kotlin|swift|vb)(?![a-z])")

# 先按句号/分号/换行/序号切成句，句内再按逗号切
_SENTENCE_SPLIT = re.compile(
    r"[。;!?\n\r|]"
    r"|(?<![a-z0-9.])\d{1,2}\s*[、.):](?!\d)"   # 1. / 2、 / 3) / 4:（不切 python3.6、10:30）
    r"|\(\d{1,2}\)"                             # (1)
    r"|[一二三四五六七八九十]+、"
    r"|[①-⑳●•▪■◆★]"
    r"|(?:^|\s)[-·]\s|(?:^|\s)\*"               # - xxx / · xxx / *xxx 这种列表
)
# 逗号后面以这些词开头才算另起一条要求，否则是接着上一段在列举，并到一起判断"优先"
# （"熟悉 Pandas，Numpy，Arima 等数据分析库优先"、"有数据分析经验者优先，如 Pandas，Spark"）
_NEW_REQUIREMENT = re.compile(
    r"(?:熟悉|熟练|掌握|精通|了解|理解|具备|具有|拥有|有|能|会|善于|擅长|负责|参与|协助|配合|完成|对|使用|运用"
    r"|懂|需|要|必须|愿意|乐于|良好|较强|优秀|本科|大专|硕士|博士|学历|专业|英语|英文|年龄|性格|身体|思路|逻辑"
    r"|沟通|团队|责任|学习|吃苦|抗压|主动|积极|认真|踏实|至少|并|且|同时|\d)")
_PREFERRED = re.compile(
    r"优先(?!级)|加分|更佳|为佳|者佳|尤佳|从优|(?<![a-z])plus(?![a-z])|preferred|bonus|nice to have")
# 小标题："加分项："后面几条都是加分项，直到下一个"任职要求"之类的标题
_PREFERRED_HEADER = re.compile(
    r"^\s*(?:加分|优先|bonus|plus|nice to have|preferred)[^:]{0,12}:"     # 加分项: / 加分方向(满足其一即可): / 优先条件:
    r"|^\s*(?:加分项?|优先条件|bonus(?: points)?|nice to have|preferred(?: qualifications)?|plus)\s*$"
    r"|(?:优先|加分)(?:考虑)?\s*:\s*$")                                   # 具备以下三点者优先:
# 公司介绍、福利待遇这类段落里的"高可用""监控"不是对候选人的要求，到下一个"岗位职责/任职要求"标题前都跳过
# （"工作地点:xxx"这种单行信息不算段落标题，不然后面的任职要求会被一起跳过）
_IGNORE_HEADER = re.compile(
    r"^\s*(?:(?:公司|企业|团队|部门)(?:介绍|简介)|关于我们|(?:福利|薪资|薪酬)(?:待遇|福利|构成)|公司福利"
    r"|我们(?:提供|能给你)|about us|benefits)")
_REQUIRED_HEADER = re.compile(
    r"(?:岗位|工作|职位)(?:职责|描述|内容)|任职(?:要求|资格|条件)|(?:岗位|职位|技能|基本|招聘)要求|^\s*要求\s*:"
    r"|我们希望你|我们期待你|希望你|你需要|你将|requirements|responsibilities|qualifications|what you")
_ANY_OF = re.compile(
    r"至少|任一|任意|之一|其一|其中一|一种或|一门|任选|均可|皆可|或|例如|比如|包括但不限于|如(?![有果能需何下此期])"
    r"|(?<![a-z])(?:or|either|one of|such as|e\.g)(?![a-z])")


def normalize(text):
    """全角转半角、统一小写。NFKC 会把"，；（）：！？"转成半角，"、""。"保持不变。"""
    text = unicodedata.normalize("NFKC", text or "")
    return text.replace("\u200b", "").lower()


def _comma_segments(sentence):
    merged = []
    for seg in sentence.split(","):
        seg = seg.strip()
        if not seg:
            continue
        if merged and not _NEW_REQUIREMENT.match(seg):
            merged[-1] += "," + seg
        else:
            merged.append(seg)
    return merged


def split_clauses(text):
    """切分句并标注 required / preferred。"""
    clauses = []
    state = REQUIRED
    for sentence in _SENTENCE_SPLIT.split(text):
        for part in _comma_segments(sentence):
            if _IGNORE_HEADER.match(part):
                state = None  # 跳过这一段
            elif _PREFERRED_HEADER.search(part):
                state = PREFERRED
            elif _REQUIRED_HEADER.search(part):
                state = REQUIRED
            if state is None:
                continue
            level = PREFERRED if (state == PREFERRED or _PREFERRED.search(part)) else REQUIRED
            clauses.append((part, level))
    return clauses


def _rule_hit(rule, clause, is_title):
    """返回命中的片段（没命中返回 None）。"""
    text = rule.mask.sub(" ", clause) if rule.mask else clause
    if is_title and rule.title_pattern:
        m = rule.title_pattern.search(text)
        if m:
            return m
    if rule.pattern is None:
        return None
    m = rule.pattern.search(text)
    if not m:
        return None
    if rule.context is not None and not rule.context.search(text):
        return None
    if rule.not_context is not None and rule.not_context.search(text):
        return None
    return m


def extract_detail(title, body):
    """
    返回 OrderedDict{技能名: (level, 证据片段)}，按在 JD 里首次出现的顺序。
    level 是 "required" 或 "preferred"。
    """
    clauses = [(normalize(title), REQUIRED, True)]
    clauses += [(c, lv, False) for c, lv in split_clauses(normalize(body))]

    # hits[skill] = [(clause_idx, pos, level, evidence)]
    hits = OrderedDict()

    def add(skill, idx, pos, level, evidence):
        hits.setdefault(skill, []).append((idx, pos, level, evidence))

    independent = [r for r in RULES if r.requires is None]
    dependent = [r for r in RULES if r.requires is not None]

    for rule in independent:
        for idx, (clause, level, is_title) in enumerate(clauses):
            m = _rule_hit(rule, clause, is_title)
            if m:
                add(rule.skill, idx, m.start(), level, m.group(0).strip())

    _resolve_any_of(hits, clauses)

    for rule in dependent:
        for idx, (clause, level, is_title) in enumerate(clauses):
            m = _rule_hit(rule, clause, is_title)
            if not m:
                continue
            req = _final_level(hits.get(rule.requires))
            if req is not None:
                # 依赖的技能只是加分项，那它的细分技能顶多也是加分项
                add(rule.skill, idx, m.start(), PREFERRED if req == PREFERRED else level, m.group(0).strip())
            elif rule.fallback:
                add(rule.fallback, idx, m.start(), level, m.group(0).strip())

    ordered = sorted(hits.items(), key=lambda kv: min((h[0], h[1]) for h in kv[1]))
    result = OrderedDict()
    for skill, hs in ordered:
        level = _final_level(hs)
        evidence = next(h[3] for h in hs if h[2] == level)
        result[skill] = (level, evidence)
    return result


def _final_level(hs):
    if not hs:
        return None
    return REQUIRED if any(h[2] == REQUIRED for h in hs) else PREFERRED


def _resolve_any_of(hits, clauses):
    """
    "熟悉 Java/Python/Go 中的一门"、"了解多种语言，如 C/C++，Java"：带"任选/举例"语气的分句里
    列了 ≥2 门语言时，只留一门当必需——优先留标题或其他分句里也点名要求的那门；都没有的话，
    列表里全是词典里的语言就留第一门，混着 C++/PHP 这些词典外的语言就一门都不算必需（会别的也行）。
    """
    by_clause = {}
    for skill, hs in hits.items():
        if skill in LANGUAGE_SKILLS:
            for i, h in enumerate(hs):
                by_clause.setdefault(h[0], []).append((h[1], skill, i))

    groups = {}
    for idx, langs in by_clause.items():
        clause, level, is_title = clauses[idx]
        if is_title or level != REQUIRED or not _ANY_OF.search(clause):
            continue
        others = len(_OTHER_LANGUAGE.findall(clause))
        if len(langs) + others >= 2:
            groups[idx] = (sorted(langs), others)

    def required_elsewhere(skill):
        return any(h[0] not in groups and h[2] == REQUIRED for h in hits[skill])

    for idx, (langs, others) in groups.items():
        anchored = [s for _, s, _ in langs if required_elsewhere(s)]
        if anchored:
            keep = anchored[0]
        elif others:
            keep = None
        else:
            keep = langs[0][1]
        for _, skill, i in langs:
            if skill != keep:
                c, p, _, ev = hits[skill][i]
                hits[skill][i] = (c, p, PREFERRED, ev)


def extract(title, body, include_preferred=False):
    """只要技能名列表：默认只要必需技能，include_preferred=True 时加分项也算。"""
    return [skill for skill, (level, _) in extract_detail(title, body).items()
            if include_preferred or level == REQUIRED]


def _main():
    title = sys.argv[1] if len(sys.argv) > 1 else ""
    if sys.stdin.isatty():
        print("粘贴 JD 正文，单独一行 END 结束：")
    lines = []
    for line in sys.stdin:
        if line.strip() == "END":
            break
        lines.append(line)
    for skill, (level, evidence) in extract_detail(title, "".join(lines)).items():
        print("%-4s %-16s ← %s" % ("必需" if level == REQUIRED else "加分", skill, evidence))


if __name__ == "__main__":
    _main()
