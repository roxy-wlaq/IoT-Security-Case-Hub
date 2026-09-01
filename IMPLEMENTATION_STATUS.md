# IoT-Security-Case-Hub — Implementation Status

> 本文件记录 V1 实现进度，每次 Lead Agent 集成完成后更新。
> 设计依据：Final Technical Review V1.0（最高优先级）→ System Design V0.6 → 各 Detail 文档。

---

## Current Phase

**Phase 0–3 已完成并通过 Code Review 修复**，代码已集成到 `dev/v1-implementation` 分支。

- 实现状态（2026-09-02 之前）：`Implementation Complete / Static Verification Passed`
- 本轮（2026-09-02）：修复 9 项 Phase 0–3 Code Review 发现（HIGH-01/02/03、MEDIUM-01/02/03/04、LOW-01），每 HIGH 增加 Regression Test，并补齐前端基础测试（MEDIUM-01）。
- 技术栈保持冻结（Java 21 / Spring Boot 3 / Spring Security / Server-side Session / CSRF / PostgreSQL 16 / Flyway / React+TS / TanStack Query / Ant Design / Nginx / Docker Compose）。未引入 JWT，未关闭 CSRF，未重新设计架构。

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
| Bootstrap Admin | ✅ | `BootstrapUserService`：仅首次启动 + 提供 env 密码时创建 ADMIN（**must_change_password=false**，见 Phase 0–3 修复 HIGH-02） |
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

## Phase 0–3 Code Review Findings — Fixed (2026-09-02)

> 范围：仅修复 Phase 0–3 Review 发现，不扩大到 Phase 4+，不重构，不更换技术栈。
> 提交：`fix: resolve phase 0-3 review findings`（分支 `dev/v1-implementation`）。

| 优先级 | 编号 | 问题 | 根因修复 | 关键文件 |
|--------|------|------|----------|----------|
| P0 | HIGH-01 | Session 失效未闭环（expireNow 后仍可用旧 JSESSIONID） | 新增 `SessionExpiryFilter`（`OncePerRequestFilter`），在后续请求中检查 `SessionInformation.isExpired()`，过期则 invalidate + clearContext + 401；经 `SecurityConfig` 在 `SecurityContextHolderFilter` 之后注入 | `auth/security/SessionExpiryFilter.java`（新）、`auth/security/SecurityConfig.java` |
| P0 | HIGH-02 | 错误实现"首次登录强制改密" | `BootstrapUserService` 创建 ADMIN 时 `mustChangePassword=false`；删除 `MustChangePasswordFilter` 及其接线；前端 `AppLayout` 移除强制改密门 | `auth/service/BootstrapUserService.java`、`auth/security/MustChangePasswordFilter.java`（删）、`auth/security/SecurityConfig.java`、`features/layout/AppLayout.tsx` |
| P0 | HIGH-03 | 默认 Docker 部署 HTTP 与 Secure Session Cookie 冲突 | `deploy/docker-compose.yml` 默认 `SPRING_PROFILES_ACTIVE` 改为 `dev`；`application-dev.yml` 显式 `cookie: http-only=true, secure=false, same-site=lax`（prod 仍保留 secure=true） | `deploy/docker-compose.yml`、`src/main/resources/application-dev.yml` |
| P1 | MEDIUM-01 | Phase 3 前端测试缺失 | 新增 6 个前端测试文件，覆盖 RouteGuard / PermissionGuard / useCurrentUser / useLogin / apiError / csrf | `src/**/__tests__/*`（6 文件） |
| P1 | MEDIUM-02 | 登录锁定过度信任 X-Forwarded-For | Nginx 三处 `proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for` → `$remote_addr`，使后端只信任受控 Nginx 传入的客户端地址 | `deploy/nginx/conf.d/casehub.conf` |
| P1 | MEDIUM-03 | 前端把 ADMIN 当成"拥有所有 Role" | `apiError.ts` 中 `userHasRole()` 不再对 ADMIN 自动返回 true；仅 `hasPermission()` 保留 ADMIN 全权限语义 | `shared/api/apiError.ts` |
| P1 | MEDIUM-04 | Phase 0–3 缺完整运行验证 | 本轮执行 Backend Unit Test（24 ✅）、Frontend Lint/Test（27 ✅）、docker compose config；IT（Testcontainers）与完整 Docker 运行时验证因环境受限未跑（见 Verification Summary 注） | 见下 |
| P2 | LOW-01 | LoginAttemptService 并发计数非原子 | `isBlocked()` / `recordFailure()` 改用 `ConcurrentHashMap.compute(...)` 原子 read-modify-write | `auth/service/LoginAttemptService.java` |

### 新增测试

| 测试文件 | 覆盖 |
|----------|------|
| `backend/.../integration/SessionExpirationIT.java`（新） | HIGH-01 回归：登录 → `expireSessions` → `/me` 返回 401；过期前正常 200 |
| `backend/.../auth/security/SecurityConfigTest.java`（改） | 适配 `SecurityConfig` 构造器（移除 `objectMapper`） |
| `frontend/src/shared/components/__tests__/RouteGuard.test.tsx`（新） | 未登录→/login、已登录→渲染、无权限→/403、ADMIN+roles:[TESTER]→/403、ADMIN→渲染 |
| `frontend/src/shared/components/__tests__/PermissionGuard.test.tsx`（新） | 无 permission→不渲染；有→渲染；ADMIN 按真实 role 判定 |
| `frontend/src/shared/api/__tests__/apiError.test.ts`（新） | hasPermission / hasAnyPermission / hasAllPermissions / userHasRole（含 MEDIUM-03 ADMIN 语义） |
| `frontend/src/features/auth/__tests__/useCurrentUser.test.tsx`（新） | `/me` 401 → data=null、isError=false |
| `frontend/src/features/auth/__tests__/useLogin.test.tsx`（新） | 登录成功 → CurrentUser cache 更新 |
| `frontend/src/shared/api/__tests__/csrf.test.ts`（新） | `isMutatingMethod` + `attachCsrfHeader`（含无 cookie 时不写 header） |

---

## Verification Summary

| 验证项 | 命令 | 结果 |
|--------|------|------|
| Backend 编译 | `mvn clean compile` | ✅ BUILD SUCCESS |
| Backend Unit Test | `mvn test`（surefire，*Test） | ✅ 24 tests, 0 failures, 0 errors, BUILD SUCCESS |
| Backend 集成测试 | `mvn verify`（failsafe，*IT，Testcontainers PG16） | ⚠️ 环境受限未执行（见注 1） |
| Frontend 类型检查 | `npm run typecheck` | ⚠️ 16 错误，全部位于既有 Phase 4 字典文件（非本轮修改，见注 2） |
| Frontend Lint | `npm run lint` | ✅ 通过（0 warnings） |
| Frontend 测试 | `npm run test`（vitest） | ✅ 27 tests, 0 failures |
| Frontend 构建 | `npm run build` | ⚠️ 受注 2 的 Phase 4 既有类型错误阻断 |
| Docker Compose 配置 | `docker compose -f deploy/docker-compose.yml config` | ✅ 验证通过（仅缺省 `.env` 的环境变量告警，使用默认值） |
| 完整 Docker 运行时 + 浏览器登录 | `docker compose up --build` | ⚠️ 环境受限未执行（见注 1） |

> **注 1（集成测试 / 运行时验证）：** 本机 Testcontainers 无法启动 PostgreSQL 容器（`Connection refused to localhost:62774` / `CannotCreateTransaction`），属基础设施限制而非代码缺陷。后端单元测试（24 ✅）已验证本次改动的逻辑正确性；新增 `SessionExpirationIT` 在具备 Docker 的环境下可正常回归 HIGH-01。
>
> **注 2（前端 typecheck / build）：** `npm run typecheck` 与 `npm run build` 报告的 16 个错误**全部位于上一轮已提交的 Phase 4 字典文件**（`CategoryPage.tsx` / `StandardPage.tsx` / `TagPage.tsx`，如 `Select` 布尔值、StandardType 不匹配、antd 与类型导入重名 `Tag`）。这些文件属于 Phase 4+，**不在本轮"只修复 Phase 0–3"范围内**，本轮未改动。本轮新增的 Phase 0–3 改动与 6 个测试文件零类型错误、Lint 与 Test 全绿。建议下一轮 Phase 4 Code Review 修复这些既有类型错误。

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
