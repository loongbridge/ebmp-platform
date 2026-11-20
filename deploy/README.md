# EBMP 企业商业管理平台 — 部署包（v1.0.0）

> OA & SRM & CRM & ERP 一体化平台 · 后端 Spring Boot 3.2.5 · 前端 Vue 3 + Element Plus · 移动端 H5（内置 Android WebView 封装 APK）

## 一、目录结构

```
deploy/
├── backend/
│   ├── ebmp-backend.jar            # 后端可执行包（Spring Boot fat-jar）
│   ├── config/application.yml      # 默认配置（H2 文件库 + JWT）
│   ├── data/                       # H2 数据库文件自动落盘目录
│   └── uploads/                    # 上传附件目录
├── frontend/
│   ├── dist/                       # 生产静态资源（放到 Nginx /usr/share/nginx/html）
│   └── nginx/
│       ├── default.conf            # Docker 容器内 Nginx 配置
│       └── ebmp-server.conf        # 独立部署（Linux）Nginx 配置
├── mobile/
│   └── EBMP-mobile-1.0.0.apk       # Android 安装包（内置 H5，默认连 http://localhost:8080）
├── docker/
│   ├── docker-compose.yml          # 一键部署：MySQL + Backend + Frontend(Nginx)
│   ├── backend.Dockerfile
│   └── frontend.Dockerfile
├── db/
│   └── init-mysql.sql              # MySQL 生产库初始化（建库 + 账号 + 34 张表）
├── scripts/
│   ├── start-backend-h2.bat        # Windows · 后端（H2 轻量版，零配置）
│   ├── start-backend-mysql.bat     # Windows · 后端（MySQL 生产版）
│   ├── start-frontend.bat          # Windows · 前端（python/node 静态托管）
│   ├── stop-all.bat                # Windows · 一键停止
│   ├── verify-db-connection.bat    # Windows · 启动后数据库连接验证
│   ├── verify-db-connection.ps1    # Windows · 上述脚本的 PowerShell 实现
│   ├── start-backend-h2.sh         # Linux/macOS · 后端 H2
│   ├── start-backend-mysql.sh      # Linux/macOS · 后端 MySQL
│   ├── start-frontend.sh           # Linux/macOS · 前端静态托管
│   ├── stop-all.sh                 # Linux/macOS · 一键停止
│   └── verify-db-connection.sh     # Linux/macOS · 启动后数据库连接验证
└── README.md                       # 本文档
```

## 二、运行环境

| 组件 | 要求 |
|------|------|
| JDK | 21+（后端已用 Java 21 编译） |
| 数据库（可选） | MySQL 8.0+（默认使用内置 H2 文件库，零配置） |
| 前端托管 | Python 3+ 或 Node 18+ 或 Nginx 1.24+ |
| Docker（可选） | Docker 24+ / Docker Compose v2 |
| 操作系统 | Windows 10/11、Linux（CentOS 7+ / Ubuntu 20.04+）、macOS 13+ |

## 三、快速启动（推荐首次体验）

### 方式 A · Windows 原生（零依赖，内置 H2）

```
1. 双击  scripts\start-backend-h2.bat    → 后端起在 http://127.0.0.1:8080
2. 双击  scripts\start-frontend.bat      → 前端起在 http://127.0.0.1
3. 浏览器访问 http://127.0.0.1
   （若前端 80 端口被占，可先 SET PORT=8081 再启动；此时访问 http://127.0.0.1:8081）
4. 停止：双击 scripts\stop-all.bat
```

### 方式 B · Linux/macOS 原生

```bash
cd deploy
bash scripts/start-backend-h2.sh         # 后端
bash scripts/start-frontend.sh 80        # 前端（端口 80）
# 停止
bash scripts/stop-all.sh
```

### 方式 C · Docker Compose（生产推荐）

```bash
cd deploy/docker
docker compose up -d
# 访问：http://服务器IP
# 日志：docker compose logs -f backend
# 数据持久化：docker volume ls  （ebmp-mysql-data、ebmp-uploads）
```

> Docker 版使用 MySQL，首次启动会自动执行 `spring.jpa.hibernate.ddl-auto=update` 创建表结构；H2 版同样自动建表。

### 方式 D · 启动后验证数据库连接

后端启动完成后，可用统一脚本验证「后端就绪 → 健康检查 → 登录读账号表 → 鉴权读用户表 → 驾驶舱多表聚合」整条链路：

```bat
rem Windows
scripts\verify-db-connection.bat
scripts\verify-db-connection.bat -BaseUrl http://10.0.0.8:8080 -WaitSeconds 90
```

```bash
# Linux/macOS
bash scripts/verify-db-connection.sh
EBMP_BASE_URL=http://10.0.0.8:8080 bash scripts/verify-db-connection.sh
```

- 脚本先轮询 `/api/health` 等待后端就绪（默认 60 秒，可调）
- 全部通过退出码 `0`，任一失败退出码 `1`，便于接入 CI / 发布流水线
- 额外设置 `MYSQL_HOST`、`MYSQL_PORT`、`MYSQL_USER`、`MYSQL_PASSWORD`、`MYSQL_DATABASE` 且本机有 `mysql` 客户端时，追加直连校验表数量（期望 34）
- 账号可用 `EBMP_USER` / `EBMP_PASSWORD` 覆盖，默认 `admin` / `Admin@123`

## 四、切换到 MySQL 生产库

Windows：
```cmd
set MYSQL_URL=jdbc:mysql://10.0.0.8:3306/ebmp?useUnicode=true^&characterEncoding=utf8^&serverTimezone=Asia/Shanghai^&allowPublicKeyRetrieval=true^&useSSL=false
set MYSQL_USER=root
set MYSQL_PASSWORD=ChangeMe@123
scripts\start-backend-mysql.bat
```

Linux/macOS：
```bash
export MYSQL_URL='jdbc:mysql://10.0.0.8:3306/ebmp?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false'
export MYSQL_USER=root
export MYSQL_PASSWORD='ChangeMe@123'
bash scripts/start-backend-mysql.sh
```

首次部署请执行初始化脚本（建库 + 应用账号 + 34 张业务表，可重复执行）：

```bash
mysql -uroot -p < db/init-mysql.sql
```

> 脚本默认创建 `ebmp`/`Ebmp_ChangeMe_2026` 应用账号并授权，请修改密码后同步 `MYSQL_USER`/`MYSQL_PASSWORD`。
> 基线数据（角色/权限/部门/默认账号）由后端首次启动时 `DataSeeder` 自动写入，无需手工插入。

## 五、Nginx 独立部署（Linux 服务器）

```bash
# 1. 复制前端资源 & Nginx 配置
sudo cp -r frontend/dist/* /usr/share/nginx/html/
sudo cp mobile/EBMP-mobile-1.0.0.apk /usr/share/nginx/mobile/
sudo cp frontend/nginx/ebmp-server.conf /etc/nginx/conf.d/ebmp.conf
# 2. 启动后端（MySQL 版）
cd .. && bash scripts/start-backend-mysql.sh
# 3. 重载 Nginx
sudo nginx -t && sudo systemctl reload nginx
```

## 六、测试账号（默认数据）

| 角色 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| 超级管理员 | admin | Admin@123 | 全权限 |
| 总经理 | gm | Gm@123 | 审批链最终节点 |
| 部门经理 | dept | Dept@123 | 部门审批 |
| 销售 | sales | Sales@123 | CRM + 销售订单 |
| 采购 | buyer | Buyer@123 | SRM + 采购订单 |
| 仓库 | wh | Wh@123 | 出入库 |
| 财务 | finance | Finance@123 | 财务相关 |

> 账号由启动时 `DataSeeder` 自动写入，修改 / 新增请登录后通过 **系统管理 → 用户管理** 操作。

## 七、移动端 APK 使用

1. 直接把 `mobile/EBMP-mobile-1.0.0.apk` 安装到 Android 设备。
2. **默认连接地址**：`http://localhost:8080` — 即后端跑在 Android 设备本机（或通过同一局域网改为 `http://PC_IP:8080`）。
3. 修改连接地址：
   - 启动页下方有「服务器地址」输入框，填入 `http://192.168.x.x:8080` 后点确定，再登录。
4. 安装后首次登录前请确保后端已在同一网段可达。

## 八、健康检查 & 接口验证

| 接口 | 说明 |
|------|------|
| `GET http://127.0.0.1:8080/api/health` | 后端健康检查（无需登录） |
| `POST /api/auth/login` | JWT 登录 |
| `GET  /api/auth/me` | 当前用户 |
| `GET  /api/workflow/todo` | 我的待审批 |
| `GET  /api/oa/notices?page=1&size=5` | 公告分页 |

## 九、常见问题

**Q1：前端 80 端口被占用？**  
A：Windows 下先 `SET PORT=8081` 再执行 `start-frontend.bat`；Linux 下 `bash scripts/start-frontend.sh 8081`。

**Q2：启动时报 `Port 8080 already in use`？**  
A：先 `scripts\stop-all.bat` 或 `netstat -ano | findstr 8080` 查占用 PID 后 `taskkill /PID N /F`。

**Q3：后端启动卡在 MySQL 连接？**  
A：确认 MySQL 服务已启动、账号密码匹配、目标库已创建；H2 版不受此影响。

**Q4：移动端登录失败？**  
A：① 后端是否可达（手机浏览器访问 http://后端IP:8080/api/health ）；② 手机是否已允许明文 HTTP（Android 9+ 默认禁用，已在 APK 内放开）。

**Q5：数据如何备份？**  
A：H2 版直接复制 `backend/data/` 目录；MySQL 版 `mysqldump -uroot -p ebmp > ebmp.sql`。

## 十、版本信息

- 后端：Spring Boot 3.2.5 · Java 21 · JPA(Hibernate 6) · H2/MySQL · JWT
- 前端：Vue 3.4 · Element Plus · Vite 5 · Vue Router 4 · Axios
- 移动端：同前端 H5 打包 WebView 壳（Capacitor 风格）
- 构建时间：2026-11-03
