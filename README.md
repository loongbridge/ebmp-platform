# 企业商业管理平台（EBMP）

整合 **OA 办公自动化 + SRM 供应商协同 + 进销存仓储 + CRM 客户关系** 的一体化企业商业管理平台：
统一门户登录、统一 RBAC 权限、统一主数据、统一审批引擎、统一合同中心、统一经营驾驶舱。

## 功能一览

| 模块 | 能力 |
| --- | --- |
| 经营驾驶舱 | 销售/商机/库存/审批/待办 KPI，销售采购趋势、商机漏斗、TOP 客户、库存预警 |
| 统一待办 / 审批中心 | 待我审批、审批流转时间线、通过/驳回、审批统计 |
| OA 协同办公 | 公告、任务、日程、会议、请假（走审批）、考勤打卡、文档上传 |
| CRM 客户关系 | 线索登记与转化、客户 360、商机六阶段漏斗、赢单生成销售订单、跟进记录 |
| SRM 供应商协同 | 采购需求、采购订单、收货入库、供应商四维评价、供应商绩效 |
| 进销存仓储 | 销售订单、出库发货、库存查询、库存预警、出入库台账、盘点调整 |
| 合同中心 | 客户合同 / 供应商合同，提交审批通过即生效 |
| 系统管理 | 用户、角色权限、部门组织树 |

### 跨模块链路

- **销售**：线索 → 客户 + 商机 → 赢单 → 销售订单草稿 → 审批 → 出库发货 → 扣减库存 + 台账
- **采购**：采购需求 → 审批 → 采购订单 → 审批 → 收货入库 → 增加库存 + 台账 → 供应商评价 → 绩效
- **合同**：创建合同 → 提交审批（强制总经理节点）→ 通过后 ACTIVE
- **审批**：请假 / 需求 / 采购订单 / 销售订单 / 合同统一走同一审批引擎

## 技术栈

- 后端：Java 17、Spring Boot 3.2.5、Spring Security + JWT、Spring Data JPA、H2 / MySQL 8
- 前端：Vite 5、Vue 3、TypeScript、Element Plus、Pinia、Vue Router、ECharts
- 移动端：Vite 5、Vue 3、TypeScript、Vue Router；Android 纯 Java WebView 原生壳（不依赖 Gradle / AndroidX）

## 工程结构

```
├── backend/            # Spring Boot 单体（common/system/masterdata/workflow/oa/crm/srm/erp/contract/dashboard）
├── frontend/           # Vue3 前端（30 个页面）
├── mobile/             # 移动端 H5 应用 + Android 原生壳（android/）
├── release/            # 打包产物（EBMP-mobile-1.0.0.apk）
├── docs/
│   ├── 整合方案.md      # 架构、模块、链路、数据模型、权限
│   └── 接口文档.md      # 全部 REST 接口与状态枚举
├── docker-compose.yml  # MySQL + 后端 + 前端 一键部署
└── README.md
```

## 本地开发

### 1. 启动后端（默认 H2 文件库，零外部依赖）

```bash
cd backend
mvn spring-boot:run
```

- 端口：`8080`
- 数据库：`./data/ebmp`（H2 文件库，MODE=MySQL，自动建表）
- 上传目录：`./data/uploads`
- 首次启动自动写入演示数据

### 2. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问 http://localhost:5173 ，`/api` 与 `/uploads` 已代理到 `localhost:8080`。

> Windows 提示：若项目路径包含 `&` 等 cmd 特殊字符，`npm run build` 可能被截断，可改用
> `node node_modules/vite/bin/vite.js build`。

### 3. 生产构建

```bash
cd frontend
npm run build      # 产物在 frontend/dist
cd ../backend
mvn clean package  # 产物在 backend/target/*.jar
```

## 移动应用（Android）

### 功能

移动端聚焦高频场景：经营 KPI 概览、待办审批（查看流程时间线 / 通过 / 驳回）、我的任务（标记完成）、库存查询与预警、销售线索（搜索 / 详情 / 拨打 / 转商机）、公告阅读、考勤上下班打卡、服务器地址设置。

### 开发与构建

```bash
cd mobile
npm install
npm run dev        # http://localhost:5174 ，/api 代理到 8080
npm run build      # H5 产物 mobile/dist
```

Android 安装包（需 Android SDK build-tools 34.0.0、platforms/android-34、JDK）：

```powershell
powershell -ExecutionPolicy Bypass -File mobile/android/build-apk.ps1
```

产物：`mobile/android/build/EBMP-mobile-1.0.0.apk`，并已发布到 [release/EBMP-mobile-1.0.0.apk](release/EBMP-mobile-1.0.0.apk)（约 0.1 MB）。

### 安装与连接后端

1. 将 APK 安装到 Android 5.0（API 21）及以上设备。
2. 首次打开在登录页展开「服务器地址」，填写后端地址（不含 `/api`）：
   - 模拟器：`http://10.0.2.2:8080`
   - 真机：`http://<电脑局域网 IP>:8080`（手机与电脑同一网络）
3. 登录后可在「我的 → 服务器地址」随时修改；地址变更后需重新登录。
4. 后端默认开启明文 HTTP，无需额外配置；生产环境建议改为 HTTPS。

### 实现说明

- 原生壳为纯 Java `Activity + WebView`，加载 `assets/www` 中的 H5 产物，按返回键优先回退页面。
- `file://` 页面通过 `allowUniversalAccessFromFileURLs` 访问后端；登录令牌与服务器地址经 `@JavascriptInterface` 持久化到 SharedPreferences。
- 签名密钥：`mobile/android/keystore/ebmp-release.jks`（演示用，口令 `ebmp123456`）；正式发布请替换为自己的密钥并妥善保管。

## 容器化部署

```bash
docker compose up -d --build
```

访问 http://localhost （前端 Nginx 已反向代理 `/api` 与 `/uploads` 到后端容器）。

| 服务 | 端口 | 说明 |
| --- | --- | --- |
| frontend | 80 | Nginx 静态站点 + API 代理 |
| backend | 8080 | Spring Boot（profile `mysql`） |
| mysql | 3306 | MySQL 8，库名 `ebmp` |

数据持久化卷：`ebmp-mysql-data`（数据库）、`ebmp-uploads`（上传文件）。

切换到自有 MySQL 时，修改 `docker-compose.yml` 中 backend 的 `SPRING_DATASOURCE_*` 环境变量即可。

## 演示账号

| 账号 | 密码 | 角色 |
| --- | --- | --- |
| admin | Admin@123 | 系统管理员 |
| gm | Gm@123 | 总经理 |
| sales | Sales@123 | 销售 |
| buyer | Buyer@123 | 采购 |
| wh | Wh@123 | 仓管 |
| hr | Hr@123 | 人事 |
| finance | Finance@123 | 财务 |

## 文档

- [整合方案](docs/整合方案.md)
- [接口文档](docs/接口文档.md)

## 说明

- 鉴权：`Authorization: Bearer <token>`，token 有效期 12 小时。
- 统一响应：`{ "code": 0, "message": "ok", "data": ... }`。
- 审批规则：部门负责人一级 → 金额 ≥ 5 万或合同追加总经理。
- 库存一致性：所有变动经 `StockService` 统一写入台账，记录变动前后数量。
