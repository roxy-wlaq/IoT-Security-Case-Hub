# IoT-Security-Case-Hub
## Deployment & Backup V1.0

> 基于：
>
> - `IoT-Security-Case-Hub_System-Design_V0.6.md`
> - `IoT-Security-Case-Hub_Technical-Architecture_V1.0.md`
> - `IoT-Security-Case-Hub_Data-Model_V1.0.md`
> - `IoT-Security-Case-Hub_Database-Schema_V1.0.md`
> - `IoT-Security-Case-Hub_Backend-Architecture_V1.0.md`
> - `IoT-Security-Case-Hub_API-Design_V1.0.md`
> - `IoT-Security-Case-Hub_Frontend-Architecture_V1.0.md`
> - `IoT-Security-Case-Hub_File-Storage-Security_V1.0.md`
> - `IoT-Security-Case-Hub_Security-RBAC-Detail_V1.0.md`
> - `IoT-Security-Case-Hub_Testing-Strategy_V1.0.md`
>
> 本文档冻结 V1 的部署、运行、备份、恢复与升级设计，包括：
>
> - Docker Compose
> - Nginx
> - Frontend
> - Spring Boot
> - PostgreSQL
> - Persistent Volume
> - HTTPS
> - Environment Variables
> - Production Profile
> - Logging
> - Health Check
> - Backup
> - Restore
> - Retention
> - Upgrade
> - Rollback
> - Storage Monitoring
>
> V1 面向公司内网环境，优先保证部署简单、可恢复、可维护。

---

# 1. V1 部署模式

正式采用：

```text
Docker Compose
```

不采用：

```text
Kubernetes
Docker Swarm
Microservice Orchestrator
```

原因：

```text
部署节点少
服务数量少
内部系统
维护成本更低
```

---

# 2. 生产拓扑

```text
User Browser
    │
    ▼
  HTTPS
    │
    ▼
  Nginx
   ├──────────────► React Frontend Static Files
   │
   └──────────────► /api/ → Spring Boot Backend
                              │
                     ┌────────┴────────┐
                     ▼                 ▼
                PostgreSQL       File Storage
```

---

# 3. V1 Container

正式部署组件：

```text
nginx
backend
postgres
```

Frontend 推荐：

```text
React build
↓
静态文件直接放入 Nginx Image
```

因此不一定需要独立：

```text
frontend container
```

最终更简洁的 V1：

```text
nginx
backend
postgres
```

---

# 4. 为什么不单独运行 Frontend Container

React 是静态 SPA。

生产构建后：

```text
dist/
```

可以直接复制到：

```text
Nginx image
```

这样减少：

```text
一个 Container
一个 Network Hop
额外维护
```

---

# 5. Docker Compose 目录

推荐：

```text
deploy/
├─ docker-compose.yml
├─ .env.example
├─ nginx/
│  ├─ nginx.conf
│  └─ conf.d/
│     └─ casehub.conf
├─ scripts/
│  ├─ backup.sh
│  ├─ restore.sh
│  └─ health-check.sh
└─ certs/
   └─ .gitkeep
```

生产证书不提交 Git。

---

# 6. Docker Network

Compose 创建内部网络：

```text
casehub-network
```

容器：

```text
nginx
backend
postgres
```

都加入内部网络。

---

# 7. PostgreSQL 不直接暴露公网/办公网

生产默认：

```text
postgres
```

不映射：

```text
5432:5432
```

只在 Docker internal network 被 Backend 访问。

如果维护确实需要：

```text
临时 tunnel
或
localhost bind
```

而不是长期暴露。

---

# 8. Backend Port

Backend 内部：

```text
8080
```

不直接向外暴露。

Nginx：

```text
/api/
↓
http://backend:8080/
```

---

# 9. Nginx

Nginx 是唯一外部入口。

负责：

```text
HTTPS
React Static Files
SPA Route Fallback
/api Reverse Proxy
Upload Size Limit
Security Headers
Compression
Access Log
```

---

# 10. Nginx External Port

推荐：

```text
80
443
```

HTTP：

```text
redirect HTTPS
```

如果内网暂时无证书：

```text
可先 HTTP
```

但生产正式推荐 HTTPS。

---

# 11. HTTPS

正式生产推荐：

```text
HTTPS
```

证书来源可以是：

```text
公司内部 CA
企业 PKI
受信任公有 CA
```

不建议长期使用：

```text
浏览器不受信任的自签名证书
```

---

# 12. TLS

推荐：

```text
TLS 1.2
TLS 1.3
```

禁用：

```text
TLS 1.0
TLS 1.1
```

---

# 13. Nginx SPA 配置

React Router 需要：

```nginx
location / {
    try_files $uri $uri/ /index.html;
}
```

否则刷新：

```text
/projects/xxx
```

会得到 404。

---

# 14. API Proxy

示例：

```nginx
location /api/ {
    proxy_pass http://backend:8080;
}
```

需传递：

```text
Host
X-Real-IP
X-Forwarded-For
X-Forwarded-Proto
```

---

# 15. Spring Forwarded Headers

Spring Boot 配置：

```text
forward-headers-strategy
```

正确识别：

```text
HTTPS
Client IP
```

---

# 16. Upload Size

Nginx：

```text
client_max_body_size 500m;
```

Spring Boot：

```text
max-file-size = 500MB
max-request-size = 520MB
```

两边保持一致。

---

# 17. Request Timeout

普通 API 不应使用特别长 timeout。

Evidence Upload / Download：

```text
允许较长连接
```

推荐：

```text
proxy_read_timeout 300s
proxy_send_timeout 300s
```

具体根据内网链路调整。

---

# 18. Static Cache

React hashed assets：

```text
长期缓存
```

例如：

```text
Cache-Control: public, max-age=31536000, immutable
```

`index.html`：

```text
no-cache
```

避免升级后引用旧 asset。

---

# 19. Evidence 不由 Nginx 静态暴露

严禁：

```text
location /evidence/
```

直接映射 Storage。

Evidence：

```text
必须通过 Backend Authorization
```

---

# 20. Backend Container

正式使用：

```text
Java 21 runtime image
```

推荐：

```text
Eclipse Temurin JRE 21
```

或等价稳定 JRE。

---

# 21. Multi-stage Backend Build

推荐：

```text
Maven build image
↓
JRE runtime image
```

生产 image 只包含：

```text
application.jar
必要配置
```

不包含完整 Maven 环境。

---

# 22. Non-root Backend

容器必须：

```text
non-root
```

例如：

```text
casehub user
```

---

# 23. Backend Writable Directory

应用仅需要写：

```text
/data/casehub
```

以及必要：

```text
/tmp
```

其余应用目录尽量：

```text
read-only
```

---

# 24. Backend JVM Memory

V1 初始推荐：

```text
-Xms512m
-Xmx2g
```

具体根据服务器调整。

如果 Evidence 下载流式实现正确：

```text
不需要因为 500MB 文件把 Heap 开到巨大
```

---

# 25. JVM Container Awareness

Java 21 原生支持容器资源识别。

仍建议明确：

```text
memory limit
```

避免 JVM 占用宿主机全部资源。

---

# 26. Backend Health

使用：

```text
Spring Boot Actuator
```

至少：

```text
/actuator/health
```

---

# 27. Health Detail

建议 health 检查：

```text
Application
Database
Storage Root
```

---

# 28. Storage Health

自定义：

```text
StorageHealthIndicator
```

检查：

```text
Storage Root exists
Writable
Free Space
```

---

# 29. PostgreSQL Health

Spring Actuator DataSource Health：

```text
DB reachable
```

---

# 30. Nginx Health

Compose 可以：

```text
curl http://localhost/
```

或：

```text
/healthz
```

---

# 31. Docker Healthcheck

`postgres`：

```text
pg_isready
```

`backend`：

```text
/actuator/health
```

`nginx`：

```text
/healthz
```

---

# 32. Startup Dependency

Backend 启动前：

```text
PostgreSQL healthy
```

但也必须允许：

```text
DB 短暂不可用时启动失败并重启
```

Compose：

```text
restart: unless-stopped
```

---

# 33. PostgreSQL Version

推荐：

```text
PostgreSQL 16
```

生产 image 必须固定：

```text
16.x
```

不要使用：

```text
latest
```

---

# 34. Database Volume

推荐：

```text
casehub-postgres-data
```

持久化：

```text
/var/lib/postgresql/data
```

---

# 35. File Storage Volume

推荐：

```text
casehub-file-storage
```

挂载 Backend：

```text
/data/casehub
```

---

# 36. Volume 类型

两种方式均可：

```text
Docker Named Volume
Bind Mount
```

企业部署更推荐清晰的 Bind Mount：

```text
/srv/casehub/postgres
/srv/casehub/files
```

便于：

```text
备份
监控
NAS 挂载
运维定位
```

---

# 37. 推荐宿主机目录

```text
/srv/casehub/
├─ postgres/
├─ files/
├─ backups/
├─ logs/
└─ certs/
```

---

# 38. 文件权限

推荐：

```text
最小权限
```

禁止：

```text
chmod -R 777
```

Backend 只需：

```text
files/
```

读写。

PostgreSQL 只需：

```text
postgres/
```

读写。

---

# 39. Environment

至少：

```text
dev
test
prod
```

---

# 40. Spring Profile

生产：

```text
SPRING_PROFILES_ACTIVE=prod
```

---

# 41. Environment Variables

至少：

```text
SPRING_PROFILES_ACTIVE

DB_HOST
DB_PORT
DB_NAME
DB_USER
DB_PASSWORD

CASEHUB_STORAGE_ROOT
CASEHUB_MAX_FILE_SIZE

CASEHUB_INITIAL_ADMIN_USERNAME
CASEHUB_INITIAL_ADMIN_PASSWORD

SERVER_PORT
```

---

# 42. .env

仓库只提交：

```text
.env.example
```

禁止提交：

```text
.env
```

---

# 43. .env.example

只放：

```text
变量名
假值
说明
```

不能放真实：

```text
密码
IP
内部证书 Key
```

---

# 44. Secret 管理

V1 可以使用：

```text
Docker Compose environment file
```

但生产：

```text
权限 600
```

长期后续可迁移：

```text
Docker Secrets
Vault
企业 Secret Manager
```

---

# 45. Initial Admin

第一次部署：

```text
没有 Admin
```

系统通过：

```text
环境变量
```

初始化 Admin。

---

# 46. Initial Admin 初始化规则

只在：

```text
不存在任何 ADMIN 用户
```

时执行。

成功后：

```text
建议从环境变量移除 Initial Password
```

---

# 47. Admin Password

初始 Admin：

```text
must_change_password = true
```

首次登录修改。

---

# 48. Flyway

Backend 启动时：

```text
Flyway migrate
```

---

# 49. Flyway 失败

如果：

```text
Migration 失败
```

Backend：

```text
不得继续启动
```

防止：

```text
代码与 Schema 不一致
```

---

# 50. Production Migration

正式升级前：

```text
Backup
↓
Run Migration
↓
Start New Backend
```

不能无备份直接升级。

---

# 51. Logging

建议：

```text
stdout/stderr
```

由 Docker 收集。

应用：

```text
SLF4J + Logback
```

---

# 52. Docker Log Rotation

必须配置：

```text
max-size
max-file
```

例如：

```text
10m
5 files
```

避免：

```text
json-file 日志占满磁盘
```

---

# 53. Nginx Logs

记录：

```text
Access
Error
```

建议也进行：

```text
rotation
```

---

# 54. Application Log Level

生产：

```text
INFO
```

特定包：

```text
WARN
```

异常：

```text
ERROR
```

---

# 55. SQL Logging

生产禁止：

```text
Hibernate show_sql=true
```

避免：

```text
日志量过大
敏感查询参数泄露
```

---

# 56. Trace ID

Nginx / Backend 日志尽量关联：

```text
traceId
```

Backend 使用：

```text
MDC
```

---

# 57. Monitoring

V1 不强制：

```text
Prometheus
Grafana
```

但至少监控：

```text
Container status
CPU
Memory
Disk
Postgres
Storage free space
```

---

# 58. 最低监控

宿主机至少可以通过：

```text
docker ps
docker stats
df -h
```

检查。

后续可接：

```text
Prometheus
Grafana
Zabbix
```

---

# 59. Storage Warning

Backend：

```text
Free < 5 GB
或
Free < 5%
```

记录：

```text
ERROR/WARN
```

并拒绝新的大文件上传。

---

# 60. Backup 总体原则

必须备份两部分：

```text
PostgreSQL
+
File Storage
```

缺任何一部分：

```text
备份不完整
```

---

# 61. Database Backup

V1 使用：

```text
pg_dump
```

推荐格式：

```text
Custom Format
```

例如：

```bash
pg_dump -Fc
```

---

# 62. Database Backup 内容

必须包含：

```text
casehub schema
data
constraints
```

Extensions 在新环境需提前创建或由 Migration 创建。

---

# 63. File Backup

备份：

```text
/srv/casehub/files
```

包括：

```text
Evidence
Test Case Attachment
Tool Attachment
```

Temp：

```text
不需要备份
```

Export：

```text
不需要备份
```

---

# 64. Backup Directory

推荐：

```text
/srv/casehub/backups/
```

结构：

```text
2026-09-01/
├─ database.dump
├─ files.tar.zst
└─ manifest.txt
```

---

# 65. Backup Manifest

建议记录：

```text
Backup Time
App Version
Database Version
Database Filename
File Archive Filename
SHA-256
```

---

# 66. Backup Frequency

V1 推荐：

```text
Daily Backup
```

如果业务非常频繁：

```text
可增加每 6 小时
```

但第一版默认：

```text
每天一次
```

---

# 67. Backup Time

推荐：

```text
业务低峰
```

例如：

```text
02:00
```

实际部署按公司时间安排。

---

# 68. Retention

推荐：

```text
Daily: 14 days
Weekly: 8 weeks
Monthly: 12 months
```

对于内部系统已经比较稳妥。

可根据存储空间调整。

---

# 69. Backup Encryption

如果 Backup 会离开服务器：

```text
必须加密
```

尤其：

```text
Evidence
用户信息
测试资料
```

如果仅存受控加密 NAS，也应由基础设施提供加密。

---

# 70. Backup Destination

推荐至少：

```text
Primary Server
+
Second Location / NAS
```

避免：

```text
备份和业务数据在同一块硬盘
```

---

# 71. 3-2-1 原则

条件允许时：

```text
3 Copies
2 Media
1 Off-host
```

内部平台可按实际成本执行。

---

# 72. Backup Consistency

由于 DB 和 File Storage 独立：

```text
Backup 应尽量在相近时间窗口
```

---

# 73. 简单 V1 Backup Mode

可在 Backup Script 中：

```text
1. 创建 DB Dump
2. 立即备份 File Storage
```

因为 Evidence 写入流程本身具有最终一致性补偿。

---

# 74. 更严格一致性

未来如果要求更严：

```text
Maintenance / Read-only Window
```

期间：

```text
暂时禁止上传与写操作
```

完成 DB + File Snapshot。

V1 暂不强制。

---

# 75. Backup Script

推荐：

```text
deploy/scripts/backup.sh
```

流程：

```text
Check Environment
Create Timestamp Folder
pg_dump
Archive files
Create SHA-256
Write manifest
Verify files exist
Cleanup retention
```

---

# 76. Backup Exit Code

任何步骤失败：

```text
exit != 0
```

并明确日志。

不能：

```text
部分失败仍返回成功
```

---

# 77. Backup Test

定期：

```text
Restore Test
```

这是正式要求。

---

# 78. Restore Frequency

至少：

```text
每季度一次
```

如果系统很重要：

```text
每月
```

---

# 79. Restore Environment

Restore Test：

```text
独立测试服务器 / 临时环境
```

不能直接在生产试恢复。

---

# 80. Restore 顺序

推荐：

```text
1. 停止 Backend
2. 准备 PostgreSQL
3. Restore DB
4. Restore File Storage
5. 设置权限
6. 启动 Backend
7. Flyway Validate
8. Health Check
9. Storage Integrity Check
10. Login / Smoke Test
```

---

# 81. pg_restore

Custom Format：

```text
pg_restore
```

---

# 82. Restore 后检查

至少：

```text
User Login
Project Count
Test Case Count
Project Test Case Count
Evidence Metadata
Evidence File Exists
SHA-256 optional sampling
```

---

# 83. File Integrity Check

建议提供：

```text
Storage Integrity Command
```

输出：

```text
DB Row → Missing File
File → Missing DB Row
SHA mismatch
```

V1 可以是：

```text
Admin maintenance script
```

---

# 84. Application Version

每次发布应有：

```text
VERSION
```

例如：

```text
1.0.0
```

Backend：

```text
/info
```

或 Actuator info 返回。

Frontend 可在：

```text
About
```

展示。

---

# 85. Docker Image Tag

禁止生产使用：

```text
latest
```

使用：

```text
casehub-backend:1.0.0
casehub-nginx:1.0.0
```

---

# 86. Release Artifact

每次发布包含：

```text
Backend Image
Nginx/Frontend Image
docker-compose.yml
Migration
Release Notes
Backup Instructions
Rollback Notes
```

---

# 87. Upgrade 流程

推荐：

```text
1. Read Release Notes
2. Backup DB + Files
3. Pull / Load New Images
4. Stop Backend
5. Run New Backend / Flyway
6. Start Services
7. Health Check
8. Smoke Test
```

---

# 88. Downtime

V1 接受：

```text
短暂维护窗口
```

不追求：

```text
Zero Downtime Deployment
```

---

# 89. 为什么不做 Zero Downtime

因为：

```text
单实例
内部系统
Schema Migration
维护成本
```

没有必要增加蓝绿部署复杂度。

---

# 90. Rollback 总体原则

Rollback 必须考虑：

```text
Application
+
Database Schema
```

不能只把旧 image 启起来。

---

# 91. Backward-compatible Migration

尽量采用：

```text
Add Column
Add Table
Add Index
```

减少立即 destructive migration。

---

# 92. Destructive Migration

例如：

```text
DROP COLUMN
DROP TABLE
Change incompatible type
```

必须：

```text
独立 Release
明确 Backup
明确 Rollback
```

---

# 93. Flyway Rollback

Flyway Community 不自动：

```text
down migration
```

因此正式 Rollback 策略：

```text
恢复升级前 Backup
```

对于重大 Schema 错误最安全。

---

# 94. Minor Application Rollback

如果新版本没有不可逆 Migration：

```text
可以直接切回旧 Image
```

前提：

```text
Schema 仍兼容
```

---

# 95. Major Rollback

如果 Migration 不兼容：

```text
Stop Service
Restore DB Backup
Restore File Backup if needed
Start Old Version
```

---

# 96. Release Smoke Test

升级后立即：

```text
/actuator/health
Login
Project List
Test Case Search
My Tests
Evidence Download
```

---

# 97. Migration Smoke Test

验证：

```text
Flyway schema history
```

版本正确。

---

# 98. Scheduled Maintenance

V1 定期维护：

```text
Backup
Temp Cleanup
Log Rotation
Storage Monitoring
Database Vacuum via PostgreSQL auto-vacuum
```

---

# 99. Temp Cleanup

Backend：

```text
每天
```

清理：

```text
24h+ temp files
```

---

# 100. Export Cleanup

如果 Excel 临时落盘：

```text
24h+
```

清理。

---

# 101. PostgreSQL Vacuum

依赖：

```text
Autovacuum
```

不建议 V1 手工关闭。

---

# 102. Database Connection Pool

Spring Boot 默认：

```text
HikariCP
```

推荐初始：

```text
maximumPoolSize = 20
minimumIdle = 5
```

根据服务器调整。

---

# 103. Postgres max_connections

不要盲目提高。

Backend 单实例：

```text
20~30 connections
```

通常足够。

---

# 104. Database Timeout

建议配置：

```text
Connection Timeout
Statement Timeout
```

避免：

```text
坏查询无限挂住
```

---

# 105. Server Resource Baseline

小型内部部署初始推荐：

```text
CPU: 4 cores
RAM: 8 GB
Disk: 200 GB+
```

其中：

```text
Evidence
```

决定主要磁盘需求。

---

# 106. 更推荐配置

如果 Evidence 较多：

```text
CPU: 8 cores
RAM: 16 GB
Storage: 500 GB+
```

数据库本身不会是主要容量压力。

---

# 107. Storage 独立扩展

如果 Evidence 增长：

```text
/srv/casehub/files
```

可以迁移到：

```text
NAS Mount
```

只调整：

```text
CASEHUB_STORAGE_ROOT
```

业务代码不变。

---

# 108. NAS 注意事项

如果使用 NAS：

```text
确保稳定挂载
权限一致
支持文件锁/rename
低延迟
Backup 单独规划
```

---

# 109. Container Restart

推荐：

```text
restart: unless-stopped
```

---

# 110. Host Reboot

Docker Service 启动后：

```text
Compose 服务自动恢复
```

---

# 111. Systemd Wrapper

生产可增加：

```text
systemd service
```

管理：

```text
docker compose up -d
docker compose down
```

但不是必须。

---

# 112. Timezone

Container 推荐：

```text
UTC
```

数据库：

```text
UTC
```

API：

```text
UTC ISO-8601
```

前端：

```text
按浏览器本地时区显示
```

---

# 113. NTP

宿主机必须：

```text
时间同步
```

例如：

```text
chrony
systemd-timesyncd
```

---

# 114. Production Domain

推荐：

```text
casehub.company.local
```

或企业内部正式域名。

不要长期使用：

```text
裸 IP
```

特别是 HTTPS。

---

# 115. DNS

公司内部 DNS：

```text
casehub.company.local
→ Server IP
```

---

# 116. Firewall

对用户网段：

```text
允许 443
```

可选：

```text
80 → redirect
```

禁止外部直接访问：

```text
8080
5432
```

---

# 117. SSH

服务器 SSH：

```text
只开放给管理员网段
```

不属于 CaseHub 应用接口。

---

# 118. Production Checklist

部署前：

```text
HTTPS certificate
Strong DB password
Strong initial Admin password
Firewall
Storage permission
Backup destination
Docker log rotation
Disk monitoring
NTP
DNS
```

---

# 119. Daily Operations

普通运维：

```text
docker compose ps
health check
disk check
backup status
```

---

# 120. Incident：Disk Full

如果 Storage 接近满：

```text
1. Reject new uploads
2. Alert operator
3. Expand / Clean approved data
4. Verify DB remains healthy
```

不能自动删除 Evidence。

---

# 121. Incident：Database Down

```text
Backend health = DOWN
API unavailable
```

不要继续接受写请求。

恢复 PostgreSQL 后：

```text
Backend reconnect / restart
```

---

# 122. Incident：Storage Down

如果 NAS / Storage 不可写：

```text
Evidence Upload Reject
```

其它纯数据库业务：

```text
可继续
```

但 Health 可标记：

```text
DEGRADED / DOWN
```

具体实现视 HealthIndicator。

---

# 123. Incident：Backend Down

Nginx：

```text
API 502
```

重启 Backend。

Session：

```text
如果使用内存 Session
用户重新登录
```

---

# 124. Backup Security

Backup 中包含：

```text
用户数据
Evidence
项目资料
```

权限至少：

```text
600 / restricted operator
```

---

# 125. Backup Log

记录：

```text
Started
Completed
Duration
DB size
Files size
Checksum
Failure
```

---

# 126. Retention Cleanup

只清理：

```text
过期 Backup
```

不能自动清理生产 Evidence。

---

# 127. Disaster Recovery 最低目标

V1 推荐：

```text
RPO ≤ 24 hours
RTO ≤ 4 hours
```

对于每日备份的内部系统合理。

---

# 128. 如果要求更低 RPO

可增加：

```text
PostgreSQL WAL Archiving
更频繁 File Snapshot
```

V1 不实现。

---

# 129. Deployment Testing

部署方案必须通过：

```text
Fresh Install
Upgrade
Backup
Restore
Rollback
Host Reboot
Container Restart
```

---

# 130. Fresh Install Acceptance

全新服务器：

```text
docker compose up
↓
Postgres init
↓
Flyway migrate
↓
Admin init
↓
Health PASS
↓
Login
```

---

# 131. Upgrade Acceptance

旧版本：

```text
有真实测试数据
```

升级后：

```text
数据保留
Evidence 可下载
Migration 成功
```

---

# 132. Restore Acceptance

使用 Backup：

```text
新环境恢复
```

验证：

```text
用户
Project
Test Case
Evidence
```

---

# 133. Rollback Acceptance

至少模拟一次：

```text
升级失败
↓
恢复旧 Version
```

确保流程不是纸面方案。

---

# 134. Docker Compose 示例组件关系

```text
services:

nginx
  depends_on:
    backend

backend
  depends_on:
    postgres
  volumes:
    - file-storage:/data/casehub

postgres
  volumes:
    - postgres-data:/var/lib/postgresql/data
```

---

# 135. 不在 Compose 写真实密码

生产：

```text
env_file
```

或：

```text
secret management
```

---

# 136. Source Repository 不保存

禁止提交：

```text
database dump
evidence
private cert
private key
.env
production logs
```

---

# 137. .gitignore

必须覆盖：

```text
.env
.local-data/
backups/
certs/*.key
certs/*.pem
*.dump
*.log
```

具体证书公钥链是否提交由部署策略决定。

---

# 138. Deployment & Backup V1.0 最终冻结

V1 正式采用：

```text
Docker Compose
Nginx
Spring Boot
PostgreSQL 16
Local Persistent File Storage
HTTPS
Flyway
Docker Healthcheck
Daily Backup
Restore Test
Versioned Docker Images
Short Maintenance Window Upgrade
Backup-based Major Rollback
```

推荐生产结构：

```text
/srv/casehub/
├─ postgres
├─ files
├─ backups
├─ logs
└─ certs
```

核心保护对象：

```text
PostgreSQL
+
File Storage
```

推荐：

```text
RPO ≤ 24h
RTO ≤ 4h
```

正式升级必须：

```text
Backup First
```

正式备份必须：

```text
定期 Restore Test
```

这套部署与备份方案作为 V1 运维实现唯一基准。

---

# 139. 下一阶段

只剩最后一步：

```text
IoT-Security-Case-Hub_Final-Technical-Review_V1.0.md
```

该文档会统一审查：

```text
产品设计
技术栈
数据模型
数据库
Backend
API
Frontend
Storage
Security
Testing
Deployment
```

重点寻找：

```text
冲突
遗漏
重复设计
过度设计
无法实现的关系
版本不一致
接口与数据库不一致
权限冲突
```

完成 Final Technical Review 后：

```text
技术方案冻结
↓
Implementation Plan 按最终架构修订
↓
正式开始编码
```
