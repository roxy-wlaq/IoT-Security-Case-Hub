# CaseHub 部署说明

> 依据：`IoT-Security-Case-Hub_Deployment-Backup_V1.0.md`、`IoT-Security-Case-Hub_Final-Technical-Review_V1.0.md`
>
> 版本：V1 Phase 1 部署基线

---

## 1. 组件与拓扑

生产只有三个容器（`Final Technical Review` 第 4 节）：

```text
User Browser
    │
    ▼
  Nginx :80（可选 :443）
   ├──► React 静态文件（/usr/share/nginx/html）
   ├──► /api/      → http://backend:8080
   ├──► /actuator/ → http://backend:8080
   └──► /healthz   → 本地直接返回 200
                        │
                        ▼
                    PostgreSQL 16（仅内部网络）
```

- 不运行独立 frontend 应用服务器。`frontend/` 经 Vite 构建产出 `frontend/dist`，在镜像构建期复制进 nginx 镜像。
- `backend` 的 `8080` 与 `postgres` 的 `5432` **不对外暴露**，只存在于 compose 内部网络。
- 启动顺序：`postgres healthy` → `backend healthy` → `nginx`。

---

## 2. 目录

```text
deploy/
├─ docker-compose.yml            生产基线
├─ docker-compose.override.yml   开发用（可选，暴露 5432 / 8080）
├─ backend.Dockerfile            多阶段构建 backend（Maven → JRE 21）
├─ nginx.Dockerfile              多阶段构建（Node 构建 frontend → nginx 镜像）
├─ .env.example                  环境变量模板（唯一允许提交的环境文件）
├─ README.md                     本文件
└─ nginx/
   ├─ nginx.conf                 worker / gzip / 安全头 / 限流 zone
   └─ conf.d/
      └─ casehub.conf            SPA、/api/ 反代、/healthz、HTTPS 预留
```

两个 Dockerfile 的 build context 都是**仓库根目录**，由 compose 的 `context: ..` 指定。
因此构建前仓库根目录必须存在 `backend/` 与 `frontend/`。

---

## 3. 宿主机目录假设

企业部署推荐 bind mount（`Deployment-Backup` 第 36-38 节）：

```text
/srv/casehub/
├─ postgres/    PostgreSQL 数据
├─ files/       Evidence / 附件 / 导出 / temp / trash
├─ backups/     备份产物
├─ logs/        运维日志
└─ certs/       TLS 证书（私钥禁止提交 Git）
```

当前 `docker-compose.yml` 默认使用 **named volume**（`postgres-data` / `file-storage`），
目的是让 `docker compose config` 与无 root 权限的环境可以直接跑通。

切换到 bind mount：

```bash
sudo mkdir -p /srv/casehub/{postgres,files,backups,logs,certs}

# PostgreSQL 镜像默认 uid/gid = 999
sudo chown -R 999:999 /srv/casehub/postgres
sudo chmod 700 /srv/casehub/postgres

# backend 容器以 app 用户运行（backend.Dockerfile 中创建）
sudo chown -R 1000:1000 /srv/casehub/files
sudo chmod 750 /srv/casehub/files
```

然后编辑 `docker-compose.yml`，把：

```yaml
- postgres-data:/var/lib/postgresql/data
- file-storage:/data/casehub
```

改成：

```yaml
- /srv/casehub/postgres:/var/lib/postgresql/data
- /srv/casehub/files:/data/casehub
```

> 禁止 `chmod -R 777`（`Deployment-Backup` 第 38 节）。

容器内存储根结构（`File Storage` 第 4 节 + `Final Technical Review` 第 27 节）：

```text
/data/casehub/
├─ evidence/
├─ test-case-attachments/
├─ tool-attachments/
├─ exports/
├─ temp/
└─ trash/          <- 删除流程用，24h+ 清理
```

---

## 4. 非 root 基线

| 服务 | 运行用户 | 说明 |
| --- | --- | --- |
| `backend` | `app`（`adduser -S app -G app`） | `USER app`；仅 `/data/casehub` 与 `/tmp` 需可写 |
| `nginx` | master = `root`，worker = `nginx` | 监听 80 需要 root；worker 以 nginx 用户运行 |
| `postgres` | 镜像默认 `postgres`（uid 999） | 未做修改 |

---

## 5. 环境变量

```bash
cd deploy
cp .env.example .env
chmod 600 .env
# 修改所有 change-me-please
```

必须修改项：

- `POSTGRES_PASSWORD` / `DB_PASSWORD`：强随机密码，两者一致
- `CASEHUB_BOOTSTRAP_ADMIN_PASSWORD`：初始管理员密码

`SPRING_PROFILES_ACTIVE`（会话 Cookie 安全 / HTTP·HTTPS 部署）：

- 默认 `prod,http` → 纯 HTTP 部署，会话 Cookie 设为 non-Secure（无 TLS 必须）
- HTTPS 部署（nginx 终止 TLS）改为 `prod` → 会话 Cookie 保持 Secure(secure=true)
- `http` profile 仅覆盖 `server.servlet.session.cookie.secure=false`，不改动
  Actuator / Security / CSRF 配置（见 `backend/src/main/resources/application-{prod,http}.yml`）
- `dev` profile **不是**受支持的部署默认值（它会暴露全部 Actuator）

`CASEHUB_BOOTSTRAP_ADMIN_PASSWORD` 行为（`Deployment-Backup` 第 45-47 节）：

- 仅当系统中**不存在任何 ADMIN 用户**时创建初始管理员
- 创建后该账号 `must_change_password = true`，首次登录必须改密码
- **首次启动后请立即登录修改密码，并从 `.env` 中移除该变量**

上传大小三处必须一致：

| 位置 | 当前值 |
| --- | --- |
| `deploy/nginx/nginx.conf` → `client_max_body_size` | `100m` |
| `.env` → `CASEHUB_MAX_FILE_SIZE` | `100MB` |
| 后端 `spring.servlet.multipart.max-file-size` | 需 Agent A 对齐 |

> `Deployment-Backup` 第 16 节建议 Evidence 场景 500m。Phase 1 先按保守基线 100m；
> 放开时三处必须同时调整，否则会出现 Nginx 413 或后端报错。

---

## 6. 常用操作

```bash
cd deploy

# 首次 / 重建
docker compose up --build -d

# 状态
docker compose ps

# 日志
docker compose logs --no-color -f
docker compose logs --no-color --tail=50 backend

# 健康检查
curl -fsS http://localhost/healthz                 # nginx
curl -fsS http://localhost/actuator/health         # backend

# 停止（保留数据卷）
docker compose down

# 停止并删除数据卷 —— 会清空数据库与文件，生产禁止
docker compose down -v
```

开发调试（暴露 5432 / 8080 到宿主机）：

```bash
docker compose -f docker-compose.yml -f docker-compose.override.yml up --build -d
```

只校验 compose 文件（不需要 Docker 守护进程）：

```bash
docker compose -f docker-compose.yml config
docker compose -f docker-compose.yml -f docker-compose.override.yml config
```

---

## 7. 镜像标签策略

生产禁止使用 `latest`（`Deployment-Backup` 第 85 节、`Final Technical Review` 第 36 节）。

```text
casehub-backend:1.0.0
casehub-nginx:1.0.0
postgres:16.6-alpine
```

发布新版本：修改 `docker-compose.yml` 中 `image:` 的版本号，重新 `up --build`。

---

## 8. 升级流程

**先备份，再升级**（`Deployment-Backup` 第 50、87 节）。

```text
1. 阅读 Release Notes，确认是否有破坏性 Migration
2. 备份 DB + File Storage
3. 停 backend
4. 构建 / 加载新镜像
5. 启动 backend（Flyway 自动 migrate）
6. 健康检查 + 冒烟测试
```

冒烟测试清单（`Deployment-Backup` 第 96 节）：

```text
/actuator/health → UP
登录
Project List
Test Case Search
Evidence 下载
```

回滚（`Deployment-Backup` 第 90-95 节）：

- 无破坏性 Migration：直接切回旧镜像 tag
- 有破坏性 Migration：必须恢复升级前的备份（Flyway Community 无自动 down migration）

---

## 9. 备份 / 恢复

> **TODO：Phase 28 实现。**
>
> 计划位置：`deploy/scripts/backup.sh`、`deploy/scripts/restore.sh`、`deploy/scripts/health-check.sh`。
>
> 备份必须同时包含 PostgreSQL 与 File Storage，缺任何一部分都不完整
> （`Deployment-Backup` 第 60 节）。

设计基线：

| 项 | 值 |
| --- | --- |
| 备份内容 | `pg_dump -Fc` + `/srv/casehub/files` 归档 |
| 备份目录 | `/srv/casehub/backups/<YYYY-MM-DD>/{database.dump,files.tar.zst,manifest.txt}` |
| 频率 | 每天一次，建议 02:00 业务低峰 |
| 保留 | Daily 14 天 / Weekly 8 周 / Monthly 12 个月 |
| 目标 | RPO ≤ 24h，RTO ≤ 4h |
| 恢复演练 | 至少每季度一次，在独立环境执行 |

当前手工备份：

```bash
# 数据库
docker compose -f docker-compose.yml exec -T postgres \
  pg_dump -Fc -U "$POSTGRES_USER" "$POSTGRES_DB" > /srv/casehub/backups/database.dump

# 文件（bind mount 场景）
tar -C /srv/casehub/files -cf /srv/casehub/backups/files.tar .

# named volume 场景
docker run --rm -v casehub_file-storage:/data -v /srv/casehub/backups:/out \
  alpine tar -cf /out/files.tar -C /data .
```

---

## 10. HTTPS

V1 内网初期允许先跑 HTTP，生产正式推荐 HTTPS（`Deployment-Backup` 第 10-11 节）。

> 切换到 HTTPS 时，必须把 `.env` 中的 `SPRING_PROFILES_ACTIVE` 改为 `prod`
> （而非默认的 `prod,http`），否则会话 Cookie 仍是非 Secure，明文 Cookie 会随
> HTTP 泄露。`http` profile 仅用于无 TLS 的纯 HTTP 场景；`prod` 才会让
> `server.servlet.session.cookie.secure=true`。详见
> `backend/src/main/resources/application-{prod,http}.yml`。

启用步骤已在 `deploy/nginx/conf.d/casehub.conf` 的注释中给出，摘要：

1. 证书放 `/srv/casehub/certs/`（私钥禁止提交 Git）
2. compose 中 nginx 增加 `- /srv/casehub/certs:/etc/nginx/certs:ro` 与 `443:443`
3. 取消 `casehub.conf` 中 443 server 块的注释
4. 全站 HTTPS 稳定后，在 `nginx.conf` 开启 HSTS

TLS 只启用 1.2 / 1.3（`Deployment-Backup` 第 12 节）。

---

## 11. 已配置的基线

| 项 | 位置 | 值 |
| --- | --- | --- |
| 安全头 | `nginx/nginx.conf` | `nosniff` / `DENY` / `no-referrer` / `Permissions-Policy` / CSP 基线 |
| 登录限流 | `nginx/conf.d/casehub.conf` | 每 IP 10 次/分钟，burst 5，超限 429 |
| SPA fallback | `nginx/conf.d/casehub.conf` | `try_files $uri $uri/ /index.html` |
| index.html 缓存 | 同上 | `no-cache` |
| assets 缓存 | 同上 | `max-age=31536000, immutable` |
| 反代超时 | 同上 | `proxy_read_timeout 300s`（Evidence / Excel 导出） |
| Docker 日志轮转 | `docker-compose.yml` | 10m × 5 files |
| 重启策略 | 同上 | `unless-stopped` |

> Evidence **严禁**由 nginx 静态暴露，所有文件读写必须经后端 Authorization
> （`Deployment-Backup` 第 19 节）。因此配置中没有任何 `/evidence/` 静态映射。

---

## 12. 禁止提交

```text
.env
私钥 / 证书 key
数据库 dump
evidence
生产日志
```

仓库只提交 `.env.example`。

---

## 13. 已知待确认事项

- `frontend/` 的包管理器：当前 `nginx.Dockerfile` 假定 **npm**（`package-lock.json` + `npm ci`）。
  若实际使用 pnpm / yarn，需要改 Dockerfile。
- `backend/` 是否包含 Maven Wrapper：当前使用 Maven 官方镜像自带的 `mvn`。
- `proxy_pass` 是否剥离 `/api` 前缀：当前**保留** `/api`，依据是后端 Spring Security
  匹配路径写作 `/api/v1/**`。若后端配置了 `server.servlet.context-path=/api`，
  需改为 `proxy_pass http://casehub_backend/;`。
- `client_max_body_size` 基线 100m，需与后端 `max-file-size` 对齐。
