# 就业技能雷达

面向应届生的求职技能辅助系统：对照目标岗位所需技能与用户已掌握的技能，自动生成差距清单、学习路径、学习资料与测评，测评结果回写用户画像，形成持续更新的学习闭环。

完整技术方案（含流程图）：[设计文档](https://claude.ai/code/artifact/a6e79c55-86a2-4f28-bde6-6af50f0a8f85)，或见团队群里分享的 PDF。项目结构、数据库设计、当前数据现状：[开发手册](https://claude.ai/code/artifact/b1edeb33-f97e-44f1-9945-284ca4866116)。

## 技术栈

- 后端：Spring Boot 4.1（Java 17）+ Spring Data JPA + MySQL
- 前端：Vue 3 + Vite + Vue Router + Pinia
- 数据库：MySQL 8

## 当前进度

- [x] 技术方案设计（数据流、学习路径算法、用户画像、岗位技能提取、数据维护策略）
- [x] 数据库结构 `database/schema.sql`（14 张表）+ 试点种子数据 `database/seed.sql`
- [x] 真实岗位数据：天池公开数据集导入，1,202 条、覆盖 13 个岗位类别（`database/seed_real_postings.sql`）——这部分是**招聘信息**，跟下面"技能词典扩展"是两回事，4 个新岗位类别暂时还没有对应的招聘信息
- [x] 面试题库：542 道（193 道 JavaGuide 来源的 Java 后端面试题 + 349 道团队收集的其他方向面试题，`database/seed_interview_questions.sql` + `database/seed_expanded_skills_and_questions.sql`）
- [x] 技能词典扩展：从 11 个（全是 Java 后端）扩到 73 个，新增覆盖 Go/Python/前端/大数据/Android/iOS/测试/运维/网络安全/数据分析/数据库/游戏开发，顺带新建了 4 个原来没有的岗位类别（Go后端/全栈/嵌入式开发/游戏开发工程师），见 `tools/import_expanded_interview_questions.py`
- [x] 除 Java 后端外的 15 个岗位类别都补上了 `job_skills`（岗位需要哪些技能、权重多少），实测②差距分析、③学习路径接口对这些新类别都能正常返回——`database/seed_job_skills_expanded.sql`（`tools/import_job_skills_expanded.py` 生成，权重是核心/次要/边缘的人工判断值，跟 seed.sql 里 Java 后端那 9 行一个性质，不是真实统计出来的）。**产品经理**这次没有任何相关技能数据，跳过了；**全栈工程师/嵌入式开发工程师/算法-机器学习工程师**这三个方向的原始面试题文档没有可提炼的技术内容，权重是按"这个方向通常需要什么"的通用判断给的，覆盖度和可信度都不如其他类别，需要团队后续找真实技术资料补充
- [ ] `skill_prereq`（技能依赖图谱）还没给新增的 62 个技能连边，所以这些类别的③学习路径目前是"一个大阶段"（没有先后顺序），不是真正分层——差距分析不受影响，只是学习路径展示得不够精细
- [x] 学习资料：14 条已导入，另有 24 条待技能词典扩充后导入（`database/seed_learning_resources.sql`）
- [x] ①②③④ 四个引擎已实现并跑通完整闭环：岗位推荐、差距分析（集合差+按权重排序）、学习路径（拓扑排序分层）、资料+测评（判分后回写 `user_skills`，下一次差距分析立刻反映变化）
- [x] 前端四个页面全部接入真实接口（技能差距雷达图、学习路径时间线、在线答题）
- [x] 本机装了 JDK 17 + Maven + 本地 MySQL，完整跑通一遍：建库 → 导入全部种子数据 → 启动后端 → 前端点击操作 → 提交测评 → 确认画像回写生效
- [x] 登录 / 注册：`/api/auth/register`、`/api/auth/login`，密码 BCrypt 哈希存储，前端有对应页面，`user_skills`/`target_category` 都挂在真实用户上而不是写死的演示账号
- [x] 团队共享 + 零配置访问：主机跑一个脚本，Cloudflare Tunnel 同时把后端**和前端**开成公网地址，队友什么都不用装、不用 clone 代码，浏览器打开前端地址就能用；注册数据统一落在主机这台电脑的 MySQL 里，见下方「团队共享」
- [ ] 给新扩展的 62 个技能补 `job_skills`（岗位所需技能权重）和 `skill_prereq`（依赖图谱），这样才能接入差距分析/学习路径——目前只是"有技能节点和面试题"，还没接进①②③引擎
- [ ] 选择题题库（`questions` 表当前仅 3 道示例，覆盖不够，需要人工补齐每个技能 5~10 道）

## MVP 策略

**先做窄，不做宽**：只打通"Java 后端工程师"一个岗位方向的完整闭环（岗位数据 → 差距分析 → 学习路径 → 测评回写），验证链路没问题后再复制到其他方向。种子数据已按这个方向准备好，见 `database/seed.sql`。

## 四人分工（参考）

| 分工 | 负责内容 |
|---|---|
| 后端与数据建模 | 数据库、画像与差距分析、路径生成接口 |
| 前端与可视化 | 页面、技能雷达图、学习路径时间线、测评答题界面 |
| 数据与内容整理 | 岗位技能数据、技能依赖图谱、题库 |
| 测评逻辑与联调部署 | 判分逻辑、联调测试、部署上线 |

## 数据库

```bash
mysql -u root -p -e "CREATE DATABASE skill_radar DEFAULT CHARACTER SET utf8mb4;"
mysql -u root -p skill_radar < database/schema.sql
mysql -u root -p skill_radar < database/seed.sql
mysql -u root -p skill_radar < database/seed_real_postings.sql
mysql -u root -p skill_radar < database/seed_interview_questions.sql
mysql -u root -p skill_radar < database/seed_learning_resources.sql
```

**顺序不能乱**：后三个 `seed_*.sql` 靠 `WHERE name = ...` 反查 id，得先有 `seed.sql` 建好的技能/类别才查得到。这三个都是脚本自动生成的（见 `tools/import_*.py`），改了数据源文件后重新跑脚本即可重新生成，不用手改 SQL；`job_categories.name`、`learning_resources(skill_id,url)` 都加了唯一约束，脚本用 `INSERT IGNORE`，重复执行是安全的。

14 张表对应设计文档里的模块：`job_categories`/`job_postings`（岗位库，类别与具体招聘信息分层）、`skills`/`skill_prereq`（技能图谱）、`user_skills`（用户技能画像）、`posting_skills`/`job_skills`（JD 技能抽取与权重）、`learning_resources`/`questions`/`interview_questions`/`quiz_attempts`（学习资料与测评，`interview_questions` 是开放式面试题，和能自动判分的 `questions` 是两回事）、`favorites`（用户收藏的招聘信息）。字段含义见 `schema.sql` 内注释，完整参考表见[开发手册](https://claude.ai/code/artifact/b1edeb33-f97e-44f1-9945-284ca4866116)。

## 后端

需要本机装 JDK 17+ 和 Maven（IntelliJ IDEA 打开 `backend/` 会自带，比较省事）。已经用 JDK 17 + Maven 3.9 + 本地 MySQL 完整跑通一遍——建库、导入全部种子数据、`mvn spring-boot:run` 启动、前端点击操作到提交测评、确认 `user_skills` 正确回写，没问题。

```bash
cd backend
mvn spring-boot:run
```

默认连接 `localhost:3306/skill_radar`，用户名 `root`、空密码；不同可设置环境变量 `DB_USERNAME` / `DB_PASSWORD` 覆盖（见 `application.yml`）。启动后 http://localhost:8080/api/skills 应该能看到种子数据里的技能列表。

接口一览（① ~ ④ 对应设计文档的四个引擎）：

| 接口 | 说明 |
|---|---|
| `POST /api/auth/register` | 注册：email/password/nickname，密码 BCrypt 哈希后存 |
| `POST /api/auth/login` | 登录，成功返回用户信息（没有真正的会话令牌，见下方说明） |
| `PUT /api/auth/users/{id}/target-category` | 把"当前目标岗位"持久化到这个用户身上 |
| `GET /api/job-categories` | ① 岗位类别列表 |
| `GET /api/job-categories/{id}/postings` | ① 该类别下的具体招聘信息，分页 |
| `GET /api/gap-analysis?categoryId=&userId=` | ② 差距分析：目标岗位技能，标注是否已掌握，按权重排序 |
| `GET /api/learning-path?categoryId=&userId=` | ③ 学习路径：待学技能按拓扑排序分层 |
| `GET /api/skills/{id}/resources` | ④ 该技能的学习资料 |
| `GET /api/skills/{id}/questions` | ④ 该技能的测评题（不含正确答案） |
| `POST /api/quiz-attempts` | ④ 提交作答，判分并在通过时把 `user_skills` 更新为 `quiz_verified` |

**登录状态目前是简化版**：登录成功后端只是把用户信息返回给前端，前端存进 `localStorage` 当"已登录"标记，之后请求把 `userId` 带上——没有真正的会话令牌/过期机制，谁都能编个 userId 冒充别人。这个阶段先解决"能注册登录、数据能落库"，要真正防伪造再升级成 JWT。

## 前端

```bash
cd frontend
npm install
npm run dev
```

打开 http://localhost:5173 ，四个页面都已接入真实接口：岗位推荐（选类别看具体招聘信息）→ 技能差距（雷达图 + 明细）→ 学习路径（按阶段的技能时间线）→ 在线测评（资料 + 答题，提交后回写画像）。后端没启动时会提示"确认后端是否已启动"。

以上是**改前端代码时**的本地开发方式。只是想**用**这个系统的队友不用走这套，见下方「团队共享数据库」——主机跑一个脚本，队友浏览器开个地址就行。

需要自定义后端地址时，复制 `frontend/.env.example` 为 `.env.local` 修改 `VITE_API_BASE_URL`。

## 团队共享数据库

团队不在同一个局域网，没法直接用内网 IP 互相访问，所以架构是：**一台电脑（"主机"）跑 MySQL + 后端 + 前端，其他人什么都不装、什么都不跑，浏览器打开一个地址就能用**。谁注册的账号，数据都落在主机那台电脑的数据库里，不会散成四份互相看不见的数据。

### 主机（存数据库的这台电脑）操作

前提：装好 JDK 17+、Maven、MySQL、[cloudflared](https://github.com/cloudflare/cloudflared)（`winget install Cloudflare.cloudflared`，免注册账号），并且 `frontend/` 目录 `npm install` 过一次。

**双击 `tools\start-server.bat`**（别直接双击 `.ps1`，Windows 默认不会真的执行它，双击了没反应就是这个原因）。

脚本按 5 步依次拉起，已经在跑的部分会自动跳过（重复运行安全）：

1. 本地 MySQL
2. 后端 Spring Boot（`localhost:8080`）
3. 后端的 Cloudflare Tunnel → `https://xxx.trycloudflare.com`
4. 前端 Vite dev server（`localhost:5173`）——脚本自动把上一步的后端隧道地址写进 `frontend/.env.local`，不用手改
5. 前端的 Cloudflare Tunnel → `https://yyy.trycloudflare.com`

跑完会把 **前端地址** 醒目地打印出来，窗口不自动关。

不想继续共享了（比如今天用完了），**双击 `tools\stop-server.bat`** 一键全部关掉：前端、后端、MySQL、两个隧道进程都停，重复运行也安全。停掉之后队友手里的地址就打不开了，下次要用重新跑 `start-server.bat` 即可（大概率会换一批新地址，记得把新的前端地址再发一遍群里）。

### 其他团队成员操作

分两种情况，别弄混：

- **只是想用系统**（注册、看岗位、答题）：把主机打印出来的**前端地址**发到群里，浏览器直接打开就行。不用 clone 代码、不用装 Node、不用配 `.env`。
- **要在自己电脑上改前端代码**（本地跑 Vite 才有热更新，不可能靠打开那个前端隧道地址改代码）：正常 clone 代码 + `npm install` + `npm run dev`，`frontend/.env.local` 里的 `VITE_API_BASE_URL` 填主机打印出来的**后端隧道地址**（不是前端地址）。这样本地起的前端连的是主机共享的那个数据库，改完代码提交，其他人 `git pull` 就都同步了。CORS 已经放行 `http://localhost:5173`，不用额外改。

两种情况数据都落在主机的 MySQL 里，不会分叉。

### 限制（这不是永久部署，是"先能跑起来"的临时方案）

- 两个隧道地址在**真正重开隧道时**（比如主机重启过）会各换一个新的（免费不记名隧道的限制）。只要主机不关机、进程不杀，重复跑脚本会沿用旧地址。地址变了就把新的**前端地址**重新发一遍群里。
- 主机电脑必须开着、不能休眠，MySQL / 后端 / 前端 / 两个隧道这几个进程都不能关，团队才连得上。
- 这个地址只在知道的人手里，但本质是公网可访问，没有额外的访问控制 / 限流。对内部测试够用，别当成正式发布。
- 后端 CORS 已放行 `https://*.trycloudflare.com`（见 `application.yml`），所以隧道域名换了也不用改后端。
- 后续真要稳定、随时能用，需要部署到云服务器或 PaaS（比如 Railway、阿里云 / 腾讯云学生机）——那个需要团队自己开云账号（可能涉及付费，我这边没法替你们开户），等确实需要了再做。

## 数据收集

主力数据源是阿里天池公开数据集（`database/天池数据集-计算机相关岗位.xlsx`，说明见其中"说明与统计" sheet），公开发布用于研究/竞赛，没有爬虫合规问题；`tools/import_tianchi.py` 把它转成 SQL。

`数据收集模板.xlsx`（项目根目录）+ `tools/jd_intake.py` 仍然保留，作为公开数据集覆盖不到的岗位方向的补充手段：在招聘网站上手动选中"职位描述/任职要求"复制，粘贴进命令行工具，自动清理噪声、按技能词典匹配技能、写入 Excel。

```bash
pip install openpyxl
python tools/jd_intake.py
```

不要直接写爬虫抓拉勾/BOSS直聘/猎聘等平台的数据——这几家都明确禁止自动化抓取，国内已有多起相关民事甚至刑事案例，风险远大于"违反用户协议"。

面试题/学习资料这类内容型数据走的是另一套路子：团队自己整理成文档（`面试题/*.docx`、`database/*面试题集.md` 等）放进仓库，再写个一次性脚本解析成 SQL——`tools/import_interview_questions.py`（JavaGuide 那批）和 `tools/import_expanded_interview_questions.py`（这批覆盖 Go/Python/前端/大数据/移动端/测试/运维/网络安全/数据分析/数据库/游戏开发方向）是同一个模式。后者的脚本文件头注释里写了取舍：`key_points` 是提炼过的要点提示、不是原文逐字转录，行为面试题统一挂到共享的"职业素养与求职技巧"技能，没有技术内容可提炼的岗位方向（全栈/嵌入式开发）目前只有职业素养题。
