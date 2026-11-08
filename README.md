# AI 面试小站（interview-coach）

> 一个自用的 Java 面试训练小站：刷题、答题、AI 评分追问、自动复盘，还能上传简历做**一对一模拟面试**。

单用户、无注册登录、带访问口令，能通过公网地址在手机上打开。

---

## 它解决什么问题

背八股最大的问题是：**背了记不住，记不住又不知道自己哪儿没记住**。

刷题 App 只给"对/错"，可面试是"你答完，面试官接着追问"。所以这个项目的核心不是题库，而是**评分 + 追问**：

- 你答完，AI 按面试官标准打分，指出漏掉的点；
- 再从你的薄弱处追问一句；
- 低于 60 分的题自动进复盘本，第二天接着练。

模拟面试更进一步：上传简历 → AI 读简历 → 出 10 道**针对你项目**的题，一题一题问。

---

## 功能

| 功能 | 页面 | 说明 |
|---|---|---|
| 题库 | `/question.html` | 42 道题，关键词搜索、按分类筛选 |
| 答题 | `/answer.html?id=N` | 写回答 → AI 给分 + 点评 + 漏点 + 追问 |
| 复盘 | `/review.html` | 低分题自动收集，可标记「已掌握」 |
| **模拟面试** | `/mock.html` | 上传简历（PDF/TXT）→ 出 10 题 → 逐题作答 → 面试报告 |
| 访问口令 | `/login.html` | 挂公网时防止别人乱用（30 天免密） |

评分标准（按初级实习岗校准）：

| 表现 | 分数 |
|---|---|
| 主干对、有细节 | 75–90 |
| 方向对、漏关键点 | 55–75 |
| 只会背名词 | 35–55 |
| 答错 / 答非所问 | 0–35 |

---

## 技术栈

| 层 | 选型 | 为什么 |
|---|---|---|
| 后端 | Spring Boot 3.2 + JDK 17 | 主流，也是招聘要求 |
| 持久层 | MyBatis（注解版） | 不写 XML，够用且能讲 |
| 数据库 | MySQL 8 | 索引、事务都能用上 |
| AI | DeepSeek API（OpenAI 兼容） | 国内直连、便宜、不用代理 |
| 前端 | 原生 HTML + JS + fetch | 无打包工具，部署就是静态文件 |
| 部署 | 可执行 jar + 内网穿透 | 单机小项目，够用 |

---

## 快速开始

### 前置

- JDK 17
- Maven 3.6+
- MySQL 8（本机，端口 3306）
- 一个 DeepSeek API Key（[platform.deepseek.com](https://platform.deepseek.com)）

### 1. 建库建表

```bash
mysql -uroot -p < src/main/resources/db/schema.sql
mysql -uroot -p < src/main/resources/db/seed_questions.sql   # 可选：种子题库
mysql -uroot -p < src/main/resources/db/mock_schema.sql      # 模拟面试相关表
```

### 2. 配置

复制 `.env.example`，或者直接用环境变量：

| 变量 | 必填 | 说明 |
|---|---|---|
| `DEEPSEEK_API_KEY` | ✅ | DeepSeek 密钥 |
| `DEEPSEEK_MODEL` | | 默认 `deepseek-chat`，本项目实测 `deepseek-flash` 约 3 秒 |
| `DEEPSEEK_TIMEOUT_SECONDS` | | 默认 20，建议 60（v4-pro 会超过 20 秒） |
| `SPRING_DATASOURCE_USERNAME/PASSWORD` | | 默认 root / 空 |
| `ACCESS_PASSWORD` | | 访问口令；**留空 = 不设防** |

### 3. 启动

```bash
mvn -DskipTests package
java -jar target/interview-coach-0.1.0.jar
```

或者用 Windows 脚本：`run.ps1`（只起应用）、`启动全部.ps1`（应用 + 公网隧道）。

### 4. 打开

```
http://localhost:8080/
```

---

## 接口清单

统一返回：`{ "code": 0, "msg": "ok", "data": {...} }`，`code` 非 0 表示失败。

| 方法 | 路径 | 用途 |
|---|---|---|
| GET | `/api/ping` | 健康检查 |
| GET | `/api/questions` | 题库列表（`keyword` / `category` / `page` / `size`） |
| GET | `/api/questions/{id}` | 题目详情 |
| POST | `/api/attempts` | 提交回答（只落库） |
| POST | `/api/attempts/{id}/evaluate` | 触发 AI 评分 |
| GET | `/api/attempts/{id}` | 查单条结果 |
| GET | `/api/review` | 复盘列表 |
| PATCH | `/api/review/{id}` | 标记已掌握 / 写备注 |
| POST | `/api/resumes` | 上传简历（multipart，PDF/TXT） |
| POST | `/api/mock/sessions` | 开始模拟面试，返回第 1 题 |
| POST | `/api/mock/sessions/{id}/answer` | 回答当前题，返回评分 + 追问 + 下一题 |
| GET | `/api/mock/sessions/{id}` | 面试报告 |
| POST | `/api/login` | 校验访问口令，发 Cookie |

---

## 数据表

| 表 | 用途 |
|---|---|
| `question` | 题库（分类、题干、参考答案、得分点） |
| `attempt` | 答题记录（回答、分数、AI 点评、漏点、追问、状态） |
| `review_item` | 复盘本（低分题、是否已掌握、备注） |
| `resume` | 简历（原文、AI 提炼的画像 JSON） |
| `mock_session` | 模拟面试场次 |
| `mock_turn` | 模拟面试的每一题（题目、回答、分数、追问） |

---

## 关键设计决策

**1. 评分拆成两段：先落库，再调 AI**

`POST /api/attempts` 先保存回答并返回 `attemptId`，再由前端调 `evaluate` 触发 AI。AI 挂了回答也不会丢，页面提示「本次回答已保存，可稍后重试」。

**2. 出题先定 10 道，再一题一题问**

一开始让模型一次性出 10 道并入库，之后逐题作答。好处是配额可控（项目 3 / Java 3 / 数据库 2 / 框架 2），不会临场跑偏。

**3. 模型选 `flash` 而不是更强的**

实测 `deepseek-v4-pro` 单次评分 20–35 秒，`flash` 约 3 秒，评分质量差别不大。前端是同步等待，超时体验比模型强弱更重要。

**4. 访问口令存在 Cookie 里的是哈希**

`SHA-256(盐 + 口令)` 的前 32 位，HttpOnly，30 天有效期。就算 Cookie 被看到也反推不出口令。

**5. 简历只在本机**

文件解析后存本机 MySQL，除了发给 DeepSeek 做分析，不上传任何第三方。

---

## 项目结构

```
interview-coach/
├── pom.xml
├── run.ps1 / tunnel.ps1 / 启动全部.ps1     # Windows 启动脚本
├── 操作说明.txt                            # 给非开发者看的操作手册
├── scripts/
│   └── smoke_test.py                      # 接口冒烟测试
└── src/main/
    ├── java/com/lujintao/coach/
    │   ├── client/DeepSeekClient.java     # HTTP 调用 + 超时 + 错误分类
    │   ├── common/                        # 统一返回、异常处理、口令拦截
    │   ├── config/WebConfig.java          # 拦截器注册
    │   ├── controller/                    # 5 个 Controller
    │   ├── mapper/                        # MyBatis 注解 Mapper
    │   ├── model/                         # 实体 + VO
    │   └── service/                       # 业务逻辑
    └── resources/
        ├── application.yml
        ├── db/                            # 建表 + 种子数据
        └── static/                        # 5 个页面 + JS/CSS + PWA
```

---

## 测试

启动应用后运行：

```bash
python scripts/smoke_test.py
```

会依次验证：健康检查 → 题库列表 → 提交回答 → AI 评分 → 模拟面试开场 → 复盘接口，最后打印 `PASS / FAIL` 汇总，全部通过时退出码为 0。

---

## 已知限制

- 单用户设计，多人用会共享同一份数据；
- 扫描版 PDF（图片）解析不出文字，需要文字版 PDF 或 TXT；
- 模拟面试的「追问」目前只展示、不能单独作答；
- 部署依赖内网穿透工具，地址会变、电脑不能关；
- 没有单元测试，只有接口冒烟测试。

---

## 后续计划

- [ ] 追问可单独作答（模拟连环追问）
- [ ] 面试报告存历史，能看到进步曲线
- [ ] 接入联网搜索，出题前参考近期面经
- [ ] 部署到云服务器，固定地址
