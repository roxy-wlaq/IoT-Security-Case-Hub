# IoT-Security-Case-Hub — Implementation Status

> 本文件记录 V1 实现进度，每次 Lead Agent 集成完成后更新。
> 设计依据：Final Technical Review V1.0（最高优先级）→ System Design V0.6 → 各 Detail 文档。

---

## Current Phase

**Phase 0–3 已完成**，代码已集成到 `dev/v1-implementation` 分支。

---

## Completed

### Phase 0 — Repository & Project Skeleton ✅

| 项目 | 状态 | 说明 |
|------|------|------|
| `backend/` | ✅ | Java 21 / Spring Boot 3.3.5 / Maven，Modular Monolith 包结构 |
| `frontend/` | ✅ | React 18 + TypeScript strict + Vite 5 + Ant Design 5 |
| `deploy/` | ✅ | Docker Compose / Nginx / PostgreSQL 16 / Dockerfile |
| `.gitignore` | ✅ | 覆盖 target/、node_modules/、dist/、.env、.DS_Store、.worktrees/ |

### Phase 1 — Docker / PostgreSQL / Health ✅

| 项目 | 状态 | 说明 |
|------|------|------|
| `docker-compose.yml` | ✅ | 3 服务：nginx / backend / postgres，无独立 frontend server |
| PostgreSQL 16 | ✅ | `postgres:16.6-alpine` 固定版本，非 latest |
| Flyway V001 | ✅ | `casehub` schema + `pg_trgm` extension（不含 pgcrypto） |
| `/actuator/health` | ✅ | Spring Boot Actuator，Nginx 反代 |
| Health checks | ✅ | 三服务均有 healthcheck（pg_isready / actuator / /healthz） |
| Volumes | ✅ | `postgres-data` + `file-storage` named volumes |
| `.env.example` | ✅ | 无真实密码，全部 `change-me-please` 占位 |
| `docker compose config` | ✅ | 验证通过 |

### Phase 2 — Identity / RBAC ✅

| 项目 | 状态 | 说明 |
|------|------|------|
| Flyway V002 | ✅ | users / roles / permissions / user_roles / role_permissions + seed |
| `must_change_password` | ✅ | `BOOLEAN NOT NULL DEFAULT FALSE`，合并进 V002（Final Review §8） |
| Spring Security | ✅ | Server-side HTTP Session + BCrypt(12) + CSRF |
| CSRF | ✅ | `CookieCsrfTokenRepository` with `HttpOnly=false`，XSRF-TOKEN cookie → X-XSRF-TOKEN header |
| `GET /api/v1/auth/csrf` | ✅ | 返回 headerName / cookieName / token |
| `POST /api/v1/auth/login` | ✅ | 认证 + Session 建立 + SessionRegistry 注册 |
| `POST /api/v1/auth/logout` | ✅ | Session invalidate + Context clear + CSRF cookie 清除 |
| `GET /api/v1/auth/me` | ✅ | 返回 CurrentUserResponse（id/username/displayName/enabled/mustChangePassword/roles/permissions） |
| `POST /api/v1/auth/change-password` | ✅ | 验证当前密码 + PasswordPolicy + BCrypt + 清除 must_change_password |
| Login Attempt Tracking | ✅ | `LoginAttemptService`：5 次失败 → 15 分钟锁定（username + IP 维度） |
| Password Policy | ✅ | 长度 12–128，不能全空白，不能等于 username |
| Bootstrap Admin | ✅ | `BootstrapUserService`：仅首次启动 + 提供 env 密码时创建 ADMIN（must_change_password=true） |
| SessionRegistry | ✅ | `SessionRegistryService` 用于 disable/reset 时 expireSessions |
| Permission Seeds | ✅ | 51 个 permission code（Security RBAC Detail §33），3 个 role（ADMIN / TEST_COORDINATOR / TESTER） |
| Role-Permission Mapping | ✅ | ADMIN 全权限 / TEST_COORDINATOR 协调者权限 / TESTER 测试员权限（§34-37） |
| Global Exception Handler | ✅ | 统一 ApiErrorResponse（code/message/traceId/details），无 stack trace 泄露 |
| TraceId | ✅ | `TraceIdFilter` 为每个请求生成 traceId，关联日志和错误响应 |
| Backend 编译验证 | ✅ | `mvn clean compile` + `mvn test-compile` 通过（51 源文件 + 9 测试文件） |

### Phase 3 — Frontend Shell / Auth ✅

| 项目 | 状态 | 说明 |
|------|------|------|
| AppLayout | ✅ | Header + Sider + Content 布局 |
| LoginPage | ✅ | RHF + Zod 验证，Ant Design Form |
| ChangePasswordForm | ✅ | must-change-password 流程 |
| RouteGuard | ✅ | auth / guestOnly / permission 检查 |
| PermissionGuard | ✅ | role-based UI 可见性 |
| Axios httpClient | ✅ | withCredentials + CSRF token handling + 401/403 interceptors |
| TanStack Query | ✅ | useLogin / useLogout / useCurrentUser / useChangePassword hooks |
| Navigation Config | ✅ | 角色权限过滤的侧边栏菜单 |
| Error/Loading States | ✅ | ErrorState / LoadingState 共享组件 |
| Pages | ✅ | Dashboard / PlaceholderPage / Forbidden / NotFound |
| TypeScript strict | ✅ | `tsc -b --noEmit` 通过 |
| ESLint | ✅ | `eslint . --max-warnings 0` 通过 |
| Vite Build | ✅ | `vite build` 通过（3174 模块，7s） |

---

## Verification Summary

| 验证项 | 命令 | 结果 |
|--------|------|------|
| Backend 编译 | `mvn clean compile` | ✅ BUILD SUCCESS（51 源文件） |
| Backend 测试编译 | `mvn test-compile` | ✅ BUILD SUCCESS（9 测试文件） |
| Frontend 类型检查 | `npm run typecheck` | ✅ 通过 |
| Frontend Lint | `npm run lint` | ✅ 通过（0 warnings） |
| Frontend 构建 | `npm run build` | ✅ 通过（3174 模块） |
| Docker Compose 配置 | `docker compose -f deploy/docker-compose.yml config` | ✅ 验证通过 |

> **注：** 后端单元测试和集成测试（Testcontainers PostgreSQL）尚未运行。集成测试需要 Docker 拉取 PostgreSQL 镜像，在后续验证中执行。

---

## In Progress

无。Phase 0–3 已完成集成。

---

## Blocked

无。

---

## Agent Branches & Commits

| Agent | 分支 | Commit | 文件数 |
|-------|------|--------|--------|
| Backend Foundation | `agent/backend-foundation` | `aa75316` | 67 文件 / 2695 行 |
| Frontend Foundation | `agent/frontend-foundation` | `a5f17cd` | 43 文件 / 7529 行 |
| Deployment Foundation | `agent/deployment` | `a2a6b31` | 8 文件 / 1059 行 |

所有 agent 分支已合并到 `dev/v1-implementation`。

---

## Next Wave

根据 Implementation Plan V1.1，下一个开发阶段为 **Phase 4：基础字典**。

### Phase 4 — 基础字典

依赖：Phase 2（RBAC）

| 任务 | 模块 | 可并行 |
|------|------|--------|
| Standard / Task Type CRUD | backend + frontend | ✅ |
| Category（两级）CRUD | backend + frontend | ✅ |
| Tag CRUD | backend + frontend | ✅ |
| Tool CRUD | backend + frontend | ✅ |

后续 Phase 依赖关系：

```
Phase 4（字典）→ Phase 5（Capability Library）→ Phase 6（Master TestCase 基础）→ ...
```

---

## Architecture Notes

- **Modular Monolith**：后端按业务模块组织（auth / user / project / capability / testcase / generation / execution / evidence / changerequest / export / audit / storage / common / tool）
- **Session 认证**：Server-side HTTP Session + JSESSIONID（HttpOnly, SameSite=Lax），不使用 JWT
- **CSRF**：CookieCsrfTokenRepository，XSRF-TOKEN cookie（HttpOnly=false）→ X-XSRF-TOKEN header
- **Database**：PostgreSQL 16，schema `casehub`，UUID 主键（Java 生成），TIMESTAMPTZ 时间
- **Migration**：Flyway 严格顺序，Lead 为 Migration Version Owner
- **Deployment**：3 容器（nginx + backend + postgres），Nginx 承载 React SPA + /api 反代
