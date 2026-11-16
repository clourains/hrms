# 智能人事管理系统 (HRMS)

基于 **Spring Boot + Vue 3 + MyBatis Plus** 的现代化智能人事管理系统，以**智能化分析**为核心特色。

## 🌟 系统特色

### 智能分析模块（核心亮点）
- 📊 **人员结构分析** - 性别、年龄、学历、部门、职位、工龄多维度分析
- 📈 **考勤智能分析** - 出勤率、迟到率、部门对比、趋势预测
- 💰 **薪资深度分析** - 薪资分布、部门对比、构成分析、趋势追踪
- 🏆 **绩效多维分析** - 等级分布、维度雷达、部门对比、优秀员工榜
- 🤖 **AI人才预测** - 离职风险预警、晋升候选人识别、招聘需求预测、未来人员规划
- 💡 **AI智能洞察** - 自动识别问题并给出优化建议
- 🎯 **招聘分析** - 需求分布、招聘周期、效率分析
- 📚 **培训分析** - 类型分布、部门参与度、效果评估

### 核心业务模块
- 👥 **员工管理** - 员工信息增删改查、批量操作、条件筛选
- 🏢 **部门管理** - 树形结构、层级管理、部门人员
- 📅 **考勤管理** - 上下班打卡、月度统计、请假出差
- 💵 **薪资管理** - 薪资核算、工资条、月度统计
- 📋 **绩效管理** - 多维度考核、等级评定、绩效趋势

## 🏗️ 技术架构

### 后端技术栈
| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 2.7.18 | 核心框架 |
| MyBatis Plus | 3.5.3.1 | ORM框架 |
| MySQL | 8.0+ | 数据库 |
| Druid | 1.2.16 | 数据源 |
| JWT | 0.9.1 | 身份认证 |
| Hutool | 5.8.20 | 工具库 |
| Lombok | - | 简化代码 |

### 前端技术栈
| 技术 | 说明 |
|------|------|
| Vue 3 | 渐进式框架（Composition API） |
| Vite | 下一代前端构建工具 |
| Element Plus | Vue 3 组件库 |
| ECharts | 数据可视化图表 |
| Vue Router | 路由管理 |
| Pinia | 状态管理 |
| Axios | HTTP 客户端 |
| SCSS | CSS 预处理器 |

## 📁 项目结构

```
hrms/
├── hrms-backend/                 # 后端项目
│   ├── src/main/java/com/hrms/
│   │   ├── controller/           # 控制层
│   │   ├── service/              # 服务层
│   │   │   └── impl/             # 服务实现
│   │   ├── mapper/               # 数据访问层
│   │   ├── entity/               # 实体类
│   │   ├── dto/                  # 数据传输对象
│   │   ├── config/               # 配置类
│   │   └── util/                 # 工具类
│   ├── src/main/resources/
│   │   ├── mapper/               # MyBatis XML
│   │   ├── db/init.sql           # [已废弃] 早期遗留脚本，勿用，改由 db/migration 接管
│   │   ├── db/migration/         # Flyway 迁移脚本（唯一建表途径，V1~V26）
│   │   └── application.yml       # 应用配置
│   └── pom.xml                   # Maven配置
│
└── hrms-frontend/                # 前端项目
    ├── src/
    │   ├── api/                  # API接口
    │   ├── views/                # 页面组件
    │   │   ├── dashboard/        # 仪表盘
    │   │   ├── employee/         # 员工管理
    │   │   ├── department/       # 部门管理
    │   │   ├── attendance/       # 考勤管理
    │   │   ├── salary/           # 薪资管理
    │   │   ├── performance/      # 绩效管理
    │   │   └── analytics/        # 智能分析大屏
    │   ├── router/               # 路由配置
    │   ├── store/                # 状态管理
    │   ├── utils/                # 工具函数
    │   ├── styles/               # 样式文件
    │   ├── App.vue
    │   └── main.js
    ├── index.html
    ├── vite.config.js
    └── package.json
```

## 🚀 快速开始

### 环境要求
- JDK 17+
- Node.js 16+
- MySQL 8.0+
- Maven 3.6+

### 数据库初始化

> **请建一个空库，交给 Flyway 自动建表灌数据，不要手动执行 `init.sql`。**

```bash
# 1. 只建一个空库（不要导入任何脚本）
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS hrms DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
```

后端启动时 Flyway 会自动按版本号依次执行
`hrms-backend/src/main/resources/db/migration/` 下的 `V1__baseline.sql` ~ `V26__fix_warehouse_capacity.sql`，
一次完成建表 + 演示数据。启动日志里看到 `Successfully applied N migrations` 即表示就绪。

**为什么不能用 `init.sql`**：`init.sql` 是早期遗留脚本，只建了 9 张 `sys_*` 表，
既没有外贸模块的 14 张 `biz_*` 表（订单/产品/库存/客户/供应商/物流/收款等），
也没有合同、审批、加班、公告、招聘这些后加的表。而 `application.yml` 里
`flyway.baseline-version: 1` 会在库非空时把 `V1__baseline.sql` 整个跳过，
先跑 `init.sql` 再启动后端，结果是外贸相关页面全部 `Table doesn't exist`。

如果已经误跑过 `init.sql`，把库删掉重建即可：

```bash
mysql -u root -p -e "DROP DATABASE IF EXISTS hrms;"
# 然后重新建空库，见上
```

### 后端启动

```bash
cd hrms-backend

# 按仓库根目录的 .env.example 设置环境变量
# 至少需要 HRMS_DB_USERNAME / HRMS_DB_PASSWORD / JWT_SECRET
# 在 IDE 或系统环境变量中配置即可，不要把真实口令写回 application.yml

# 启动应用
mvn spring-boot:run
```

> `application.yml` 里的默认口令仅供本地演示；公网部署前必须替换默认口令与默认 `JWT_SECRET`。
> 如需 Docker Compose 启动，先执行 `cp .env.example .env` 并修改，再运行 `docker compose up -d --build`。

后端启动成功后访问：http://localhost:8080/api

### 前端启动

```bash
cd hrms-frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端启动成功后访问：http://localhost:5173

### 默认账号
| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | 123456 | 系统管理员 |
| hr001 | 123456 | 人事管理员 |
| emp001 | 123456 | 普通员工 |

> 以上账号仅用于本地演示，部署到任何可访问环境前请立即修改密码。

## 🔌 API 接口概览

### 认证接口
- `POST /api/auth/login` - 登录
- `GET /api/auth/info` - 获取用户信息
- `POST /api/auth/logout` - 登出

### 员工管理
- `GET /api/employee/page` - 分页查询
- `GET /api/employee/{id}` - 员工详情
- `POST /api/employee` - 新增员工
- `PUT /api/employee` - 修改员工
- `DELETE /api/employee/{id}` - 删除员工

### 部门管理
- `GET /api/department/tree` - 部门树形结构
- `GET /api/department/list` - 部门列表

### 智能分析
- `GET /api/analytics/dashboard` - 仪表盘统计
- `GET /api/analytics/employee-structure` - 人员结构
- `GET /api/analytics/attendance` - 考勤分析
- `GET /api/analytics/salary` - 薪资分析
- `GET /api/analytics/performance` - 绩效分析
- `GET /api/analytics/talent-prediction` - 人才预测
- `GET /api/analytics/ai-insights` - AI洞察
- `GET /api/analytics/recruitment` - 招聘分析
- `GET /api/analytics/training` - 培训分析

## 📊 智能分析亮点

### 1. AI离职预测
基于员工工龄、绩效、薪资、部门等多维度数据，智能预测员工离职风险，提前识别高风险员工并给出干预建议。

### 2. 智能招聘需求预测
结合业务增长趋势、人员流失率、部门规划等因素，预测未来各部门招聘需求，助力人才储备。

### 3. 绩效多维分析
从工作质量、效率、态度、协作、创新五个维度全面评估员工表现，生成人才画像雷达图。

### 4. AI智能洞察
自动分析人力资源数据中的异常和趋势，生成可操作的优化建议，辅助管理层决策。

## 📝 数据库表说明

| 表名 | 说明 |
|------|------|
| sys_user | 用户表（登录账号） |
| sys_employee | 员工表 |
| sys_department | 部门表 |
| sys_attendance | 考勤表 |
| sys_salary | 薪资表 |
| sys_performance | 绩效表 |
| sys_recruitment | 招聘表 |
| sys_training | 培训表 |
| sys_emp_training | 员工培训关联表 |

## 🤝 开发说明

- 后端使用 MyBatis Plus 简化 CRUD 操作
- 前端使用 Element Plus 组件库快速构建界面
- 智能分析模块目前基于统计算法实现，可扩展接入机器学习模型
- 前后端分离，通过 RESTful API 交互

## 📄 License

本项目基于 [MIT License](LICENSE) 开源。
