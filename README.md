# 就业技能雷达

面向应届生的求职技能辅助系统：对照目标岗位所需技能与用户已掌握的技能，自动生成差距清单、学习路径、学习资料与测评，测评结果回写用户画像，形成持续更新的学习闭环。

完整技术方案（含流程图）：[设计文档](https://claude.ai/code/artifact/a6e79c55-86a2-4f28-bde6-6af50f0a8f85)，或见团队群里分享的 PDF。项目结构、数据库设计、当前数据现状：[开发手册](https://claude.ai/code/artifact/b1edeb33-f97e-44f1-9945-284ca4866116)。

## 技术栈

- 后端：Spring Boot 4.1（Java 17）+ Spring Data JPA + MySQL
- 前端：Vue 3 + Vite + Vue Router + Pinia
- 数据库：MySQL 8

## 当前进度

- [x] 技术方案设计（数据流、学习路径算法、用户画像、岗位技能提取、数据维护策略）
- [x] 数据库结构 `database/schema.sql`（13 张表）+ 试点种子数据 `database/seed.sql`
- [x] 真实岗位数据：天池公开数据集导入，1,202 条、覆盖 13 个岗位类别（`database/seed_real_postings.sql`）
- [x] 面试题库：193 道（JavaGuide 来源，`database/seed_interview_questions.sql`）
- [x] 学习资料：14 条已导入，另有 24 条待技能词典扩充后导入（`database/seed_learning_resources.sql`）
- [x] 后端骨架（跑通 `/api/skills`、`/api/job-categories` 两个最小接口）
- [x] 前端骨架（四个页面路由 + 已验证能调用后端 API）
- [ ] 技能词典扩展到 Java 后端以外的类别（已识别 78 个候选技能名，见开发手册）
- [ ] 选择题题库（`questions` 表当前仅 3 道示例，驱动④在线测评还需要人工出题）
- [ ] ①②③④ 四个引擎的真实业务逻辑（当前是 TODO 占位）
- [ ] 打通 Java 后端工程师方向的完整闭环

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

13 张表对应设计文档里的模块：`job_categories`/`job_postings`（岗位库，类别与具体招聘信息分层）、`skills`/`skill_prereq`（技能图谱）、`user_skills`（用户技能画像）、`posting_skills`/`job_skills`（JD 技能抽取与权重）、`learning_resources`/`questions`/`interview_questions`/`quiz_attempts`（学习资料与测评，`interview_questions` 是开放式面试题，和能自动判分的 `questions` 是两回事）。字段含义见 `schema.sql` 内注释，完整参考表见[开发手册](https://claude.ai/code/artifact/b1edeb33-f97e-44f1-9945-284ca4866116)。

## 后端

需要本机安装 JDK 17+ 和 Maven（本机开发环境暂未装，未能在此验证编译，建议用 IntelliJ IDEA 打开 `backend/` 直接跑，它会自带 JDK 和 Maven）。

```bash
cd backend
mvn spring-boot:run
```

默认连接 `localhost:3306/skill_radar`，用户名 `root`、空密码；不同可设置环境变量 `DB_USERNAME` / `DB_PASSWORD` 覆盖（见 `application.yml`）。启动后 http://localhost:8080/api/skills 应该能看到种子数据里的技能列表。

## 前端

```bash
cd frontend
npm install
npm run dev
```

打开 http://localhost:5173 ，"岗位推荐"页会请求后端的 `/api/job-categories`——已经本地验证过：后端没启动时会提示"确认后端是否已启动"，接口通了就会显示种子数据里的"Java后端工程师"。其余三个页面（技能差距/学习路径/在线测评）目前是 TODO 占位，对应①②③④引擎还没写真实逻辑。

需要自定义后端地址时，复制 `frontend/.env.example` 为 `.env.local` 修改 `VITE_API_BASE_URL`。

## 数据收集

主力数据源是阿里天池公开数据集（`database/天池数据集-计算机相关岗位.xlsx`，说明见其中"说明与统计" sheet），公开发布用于研究/竞赛，没有爬虫合规问题；`tools/import_tianchi.py` 把它转成 SQL。

`数据收集模板.xlsx`（项目根目录）+ `tools/jd_intake.py` 仍然保留，作为公开数据集覆盖不到的岗位方向的补充手段：在招聘网站上手动选中"职位描述/任职要求"复制，粘贴进命令行工具，自动清理噪声、按技能词典匹配技能、写入 Excel。

```bash
pip install openpyxl
python tools/jd_intake.py
```

不要直接写爬虫抓拉勾/BOSS直聘/猎聘等平台的数据——这几家都明确禁止自动化抓取，国内已有多起相关民事甚至刑事案例，风险远大于"违反用户协议"。
