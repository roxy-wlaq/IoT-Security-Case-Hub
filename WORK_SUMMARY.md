# IoT-Security-Case-Hub V1 — 工作总结

> 生成时间：2026-09-02 01:58
> 分支：`dev/v1-implementation`（本地），`main`（远程）
> 仓库：https://github.com/roxy-wlaq/IoT-Security-Case-Hub

---

## 一、总体概览

| 阶段 | 状态 | 说明 |
|------|------|------|
| Phase 0 — 工程骨架 | ✅ 已完成并提交 | backend / frontend / deploy 三目录骨架 |
| Phase 1 — Docker/PG/Health | ✅ 已完成并提交 | docker-compose、Flyway V001、actuator health |
| Phase 2 — Identity/RBAC | ✅ 已完成并提交 | Spring Security + Session + CSRF + 5 个 Auth API |
| Phase 3 — Frontend Shell | ✅ 已完成并提交 | AppLayout、Login、RouteGuard、Axios CSRF |
| Phase 4 — 基础字典 | ⚠️ 部分文件已创建（未提交） | 用户要求停止，等待检查后再继续 |

---

## 二、Phase 0-3：已提交代码（dev/v1-implementation 分支）

### 2.1 提交历史

```
3c99b5c  docs: add implementation status for Phase 0-3 completion
0e1983a  Merge branch 'agent/deployment' into dev/v1-implementation
a68da9d  Merge branch 'agent/frontend-foundation' into dev/v1-implementation
a5f17cd  feat(frontend): add authenticated application shell          ← 前端 43 文件
aa75316  feat(backend): bootstrap Spring Boot authentication foundation ← 后端 67 文件
a2a6b31  chore(deploy): document deployment layout and environment baseline
26411c5  chore(deploy): add PostgreSQL, backend and nginx compose baseline
57a10ff  chore(deploy): add multi-stage backend and nginx production images  ← 部署 8 文件
0552da8  chore(repo): add gitignore baseline and agent workspace isolation
```

**总计：119 文件，11,433 行新增代码，无合并冲突。**

### 2.2 Backend（67 文件，2,695 行）

#### 技术栈
- Java 21 LTS + Spring Boot 3.3.5 + Maven
- Spring Security（Server-side HTTP Session + BCrypt(12) + CSRF）
- Spring Data JPA + Hibernate + Flyway
- PostgreSQL 16
- Lombok

#### 包结构（Modular Monolith）
```
com.company.casehub
├── CaseHubApplication.java          # 主入口
├── auth/                            # 认证模块（Phase 2 已实现）
│   ├── controller/AuthController    # 5 个 API 端点
│   ├── dto/                         # LoginRequest, ChangePasswordRequest, CurrentUserResponse, CsrfResponse
│   ├── security/                    # SecurityConfig, UserPrincipal, MustChangePasswordFilter, RestEntryPoint/AccessDeniedHandler, SecurityConstants
│   └── service/                     # AuthenticationService, BootstrapUserService, LoginAttemptService, PasswordPolicy, SessionRegistryService
├── user/                            # 用户/RBAC 模块（Phase 2 已实现）
│   ├── entity/                      # UserEntity, RoleEntity, PermissionEntity, UserRoleEntity, RolePermissionEntity
│   ├── repository/                  # 5 个 JPA Repository
│   └── service/                     # CurrentUserService, UserDetailsServiceImpl
├── common/                          # 公共基础设施
│   ├── BaseEntity                   # id(UUID) + createdAt + updatedAt 基类
│   ├── exception/                   # CaseHubException, ErrorCode, GlobalExceptionHandler, BusinessRuleException, ConflictException, ForbiddenOperationException, ResourceNotFoundException, ValidationException
│   ├── response/ApiErrorResponse    # 统一错误响应格式
│   └── web/TraceIdFilter            # 请求级 traceId
├── tool/                            # 占位（Phase 4 待实现）
├── standard/                        # 占位（Phase 4 待实现）※尚无此目录
├── category/                        # 占位（Phase 4 待实现）※尚无此目录
├── tag/                             # 占位（Phase 4 待实现）※尚无此目录
├── project/                         # 占位（Phase 9 待实现）
├── capability/                      # 占位（Phase 5 待实现）
├── testcase/                        # 占位（Phase 6 待实现）
├── generation/                      # 占位（Phase 11-12 待实现）
├── execution/                       # 占位（Phase 17-18 待实现）
├── evidence/                        # 占位（Phase 15 待实现）
├── changerequest/                   # 占位（Phase 22-23 待实现）
├── export/                          # 占位（Phase 25 待实现）
├── audit/                           # 占位（Phase 26 待实现）
└── storage/                         # 占位（Phase 15 待实现）
```

#### 已实现的 Auth API 端点
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/v1/auth/csrf` | 获取 CSRF token | 公开 |
| POST | `/api/v1/auth/login` | 登录 | 公开 |
| POST | `/api/v1/auth/logout` | 登出 | 已认证 |
| GET | `/api/v1/auth/me` | 获取当前用户 | 已认证 |
| POST | `/api/v1/auth/change-password` | 修改密码 | 已认证 |

#### Flyway 迁移
| 版本 | 文件 | 内容 |
|------|------|------|
| V001 | `V001__init_schema.sql` | 创建 `casehub` schema + `pg_trgm` extension（不含 pgcrypto） |
| V002 | `V002__identity_rbac.sql` | users / roles / permissions / user_roles / role_permissions 表 + seed 数据（3 角色 + 51 权限） |

#### 配置文件
| 文件 | 说明 |
|------|------|
| `application.yml` | 主配置（数据源、Flyway、JPA、Actuator） |
| `application-dev.yml` | 开发环境 |
| `application-test.yml` | 测试环境 |
| `application-prod.yml` | 生产环境 |

#### 测试文件（9 个，未运行）
| 文件 | 类型 |
|------|------|
| `SecurityConfigTest.java` | 单元测试 |
| `UserPrincipalTest.java` | 单元测试 |
| `LoginAttemptServiceTest.java` | 单元测试 |
| `PasswordPolicyTest.java` | 单元测试 |
| `GlobalExceptionHandlerTest.java` | 单元测试 |
| `CurrentUserServiceTest.java` | 单元测试 |
| `AbstractIntegrationTest.java` | 集成测试基类（Testcontainers PostgreSQL） |
| `AuthFlowIT.java` | 集成测试（登录/登出/CSRF 流程） |
| `MigrationIT.java` | 集成测试（Flyway 迁移验证） |

### 2.3 Frontend（43 文件，7,529 行）

#### 技术栈
- React 18 + TypeScript strict + Vite 5
- Ant Design 5 + @ant-design/icons
- TanStack Query 5（Server State）
- React Hook Form 7 + Zod 3（表单验证）
- React Router DOM 6
- Axios 1.7（HTTP 客户端）
- ESLint 9 + Vitest 2

#### 目录结构
```
frontend/src/
├── main.tsx                          # 应用入口
├── App.tsx                           # 根组件
├── app/
│   ├── router.tsx                    # 路由配置（RouteGuard 包裹）
│   ├── providers.tsx                 # QueryClient + ConfigProvider
│   └── queryClient.ts               # TanStack Query 配置
├── features/
│   ├── auth/
│   │   ├── LoginPage.tsx            # 登录页
│   │   ├── ChangePasswordForm.tsx   # 强制改密表单
│   │   ├── components/LoginForm.tsx # 登录表单（RHF + Zod）
│   │   ├── api/authApi.ts           # Auth API 函数
│   │   ├── hooks/                    # useLogin, useLogout, useCurrentUser, useChangePassword
│   │   └── schemas/authSchemas.ts   # Zod 验证 schema
│   └── layout/
│       ├── AppLayout.tsx            # 主布局（Header + Sider + Content）
│       ├── AppHeader.tsx            # 顶部栏（用户信息 + 登出）
│       └── AppSider.tsx             # 侧边栏（权限过滤菜单）
├── pages/
│   ├── Dashboard.tsx                # 仪表盘
│   ├── PlaceholderPage.tsx          # 占位页（待实现的模块）
│   ├── Forbidden.tsx                # 403 页面
│   └── NotFound.tsx                 # 404 页面
├── shared/
│   ├── api/
│   │   ├── httpClient.ts            # Axios 全局实例（withCredentials + CSRF + 401 拦截）
│   │   ├── csrf.ts                  # CSRF token 管理
│   │   └── apiError.ts              # 统一错误类型转换
│   ├── components/
│   │   ├── RouteGuard.tsx           # 路由守卫（auth + guestOnly + permission）
│   │   ├── PermissionGuard.tsx      # 权限守卫（role-based UI 可见性）
│   │   ├── ErrorState.tsx           # 错误状态组件
│   │   └── LoadingState.tsx         # 加载状态组件
│   ├── config/navigation.ts         # 侧边栏导航配置（角色权限过滤）
│   └── types/auth.ts                # 认证相关 TypeScript 类型
└── styles/global.css                # 全局样式
```

#### 配置文件
| 文件 | 说明 |
|------|------|
| `package.json` | 依赖和脚本（dev/build/typecheck/lint/test） |
| `tsconfig.json` | TypeScript strict 配置 |
| `vite.config.ts` | Vite 配置（dev proxy 到 localhost:8080） |
| `eslint.config.js` | ESLint 9 flat config |
| `.env.development` | 开发环境 Vite 变量（API proxy） |
| `.env.production` | 生产环境 Vite 变量（同 Origin /api/v1） |

### 2.4 Deployment（8 文件，1,059 行）

#### 文件清单
| 文件 | 说明 |
|------|------|
| `docker-compose.yml` | 生产编排：nginx + backend + postgres 三服务 |
| `docker-compose.override.yml` | 开发覆盖配置 |
| `backend.Dockerfile` | 后端多阶段构建（Maven build → JRE alpine runtime，非 root） |
| `nginx.Dockerfile` | Nginx 多阶段构建（Node build → Nginx alpine runtime） |
| `nginx/nginx.conf` | Nginx 主配置（安全头、限流 zone、日志） |
| `nginx/conf.d/casehub.conf` | 站点配置（SPA fallback、/api 反代、login 限流、healthz） |
| `.env.example` | 环境变量模板（无真实密码） |
| `README.md` | 部署文档 |

#### 关键设计决策
- 3 容器架构：nginx（唯一入口）+ backend + postgres（不运行独立 frontend server）
- PostgreSQL 16.6-alpine（固定版本，非 latest）
- React SPA 由 Vite build → dist → 复制到 Nginx 静态目录
- Nginx SPA fallback（try_files $uri $uri/ /index.html）
- /api/ 反向代理到 backend:8080
- 登录限流：每 IP 10 次/分钟
- 安全头：X-Content-Type-Options、X-Frame-Options、Referrer-Policy、Permissions-Policy、CSP
- 非 root 运行（backend 用 app 用户，nginx worker 用 nginx 用户）
- 健康检查：postgres(pg_isready) / backend(actuator/health) / nginx(/healthz)
- 持久卷：postgres-data + file-storage

---

## 三、Phase 4：部分文件（未提交，未验证）

> ⚠️ 用户要求停止 Phase 4 开发，以下文件为后台 Agent 自动创建，**尚未编译验证、尚未提交**。

### 3.1 Backend（1 个文件，未提交）

| 文件 | 说明 |
|------|------|
| `backend/src/main/resources/db/migration/V003__dictionary_tables.sql` | 我直接创建的迁移文件，定义 4 张字典表 |

V003 迁移内容：
- `standard_task_types` 表（code, name, type STANDARD/TASK_TYPE, description, enabled）
- `categories` 表（parent_id 自关联, level 1-2, CHECK 约束保证层级一致性）
- `tags` 表（name 唯一 lowercase 索引）
- `tools` 表（name 唯一 lowercase 索引, platform, website）
- 4 张表的 name 字段均创建 pg_trgm GIN 索引

> **注：** 后端 Java 代码（Entity/Repository/Service/Controller）尚未创建。后端 Agent 正在运行但未产出文件。

### 3.2 Frontend（18 个文件，未提交）

由前端 Agent 创建，目录 `frontend/src/features/dictionary/`：

| 类型 | 文件 |
|------|------|
| 类型定义 | `shared/types/dictionary.ts` |
| API | `features/dictionary/api/standardApi.ts` |
| API | `features/dictionary/api/categoryApi.ts` |
| API | `features/dictionary/api/tagApi.ts` |
| API | `features/dictionary/api/toolApi.ts` |
| Hooks | `features/dictionary/hooks/useStandards.ts` |
| Hooks | `features/dictionary/hooks/useCategories.ts` |
| Hooks | `features/dictionary/hooks/useTags.ts` |
| Hooks | `features/dictionary/hooks/useTools.ts` |
| Schema | `features/dictionary/schemas/standardSchema.ts` |
| Schema | `features/dictionary/schemas/categorySchema.ts` |
| Schema | `features/dictionary/schemas/tagSchema.ts` |
| Schema | `features/dictionary/schemas/toolSchema.ts` |
| Page | `features/dictionary/pages/StandardPage.tsx` |
| Page | `features/dictionary/pages/CategoryPage.tsx` |
| Page | `features/dictionary/pages/TagPage.tsx` |
| Page | `features/dictionary/pages/ToolPage.tsx` |
| Shared | `shared/hooks/`（1 个文件，具体内容未检查） |

> **注：** 这些文件**未经 typecheck/lint/build 验证**，可能存在编译错误。navigation.ts 和 router.tsx 可能已被 Agent 修改（需检查 git diff）。

---

## 四、验证结果

### 4.1 已通过的验证（Phase 0-3）

| 验证项 | 命令 | 结果 | 时间 |
|--------|------|------|------|
| Backend 编译 | `mvn clean compile` | ✅ BUILD SUCCESS（51 源文件） | 01:46 |
| Backend 测试编译 | `mvn test-compile` | ✅ BUILD SUCCESS（9 测试文件） | 01:46 |
| Frontend 类型检查 | `npm run typecheck` | ✅ 通过 | 01:43 |
| Frontend Lint | `npm run lint` | ✅ 0 warnings | 01:43 |
| Frontend 构建 | `npm run build` | ✅ 3174 模块，7s | 01:43 |
| Docker Compose | `docker compose config` | ✅ 验证通过 | 01:38 |

### 4.2 未执行的验证

| 验证项 | 原因 |
|--------|------|
| `mvn test`（单元测试） | 用户指示"测试暂时不用" |
| `mvn verify`（集成测试 Testcontainers） | 同上 |
| `npm test`（Vitest 前端测试） | 同上 |
| `docker compose up --build` | 未执行 |
| Phase 4 前端构建验证 | 用户要求停止 |

### 4.3 开发中修复的问题

| 问题 | 修复 |
|------|------|
| LoginForm.tsx 未使用的 `isApiError` 导入（TS6133） | 移除该导入 |
| ESLint 缺少 `globals` 包 | `npm install --save-dev globals` |
| router.tsx 同时导出常量和组件触发 react-refresh 警告 | 改为内部常量 `browserRouter` |
| .gitignore 未忽略 `*.tsbuildinfo` | 添加忽略规则 |
| .gitignore 忽略了 Vite 的 `.env.development`/`.env.production` | 添加 `!.env.development` `!.env.production` 例外 |

---

## 五、仓库状态

### 5.1 分支
| 分支 | 说明 | 远程 |
|------|------|------|
| `main` | 远程主分支（仅设计文档） | origin/main |
| `dev/v1-implementation` | 本地开发分支（Phase 0-3 已提交） | ❌ 推送失败 |
| `agent/backend-foundation` | Backend Agent 分支（已合并） | 仅本地 |
| `agent/frontend-foundation` | Frontend Agent 分支（已合并） | 仅本地 |
| `agent/deployment` | Deployment Agent 分支（已合并） | 仅本地 |

### 5.2 未推送原因
```
git push -u origin dev/v1-implementation
→ remote: Permission to roxy-wlaq/IoT-Security-Case-Hub.git denied to roxykami.
→ 403 Forbidden
```
当前 git credential helper（osxkeychain）使用 `roxykami` 账户，无 `roxy-wlaq/IoT-Security-Case-Hub` 仓库写权限。需用户配置正确的 GitHub 认证。

### 5.3 工作区状态
```
git status --short

?? backend/src/main/resources/db/migration/V003__dictionary_tables.sql  ← Phase 4 未提交
?? frontend/src/features/dictionary/                                    ← Phase 4 未提交
?? frontend/src/shared/hooks/                                           ← Phase 4 未提交
?? frontend/src/shared/types/dictionary.ts                              ← Phase 4 未提交
?? .workbuddy/                                                          ← 工作区元数据
```

### 5.4 Git Worktree
| 路径 | 分支 | 状态 |
|------|------|------|
| `/Users/ai/IoT-Security-Case-Hub` | `dev/v1-implementation` | 主工作区 |
| `.worktrees/backend-foundation` | `agent/backend-foundation` | Phase 0-3 已完成 |
| `.worktrees/frontend-foundation` | `agent/frontend-foundation` | Phase 0-3 已完成 |
| `.worktrees/deployment` | `agent/deployment` | Phase 0-3 已完成 |

---

## 六、代码审查要点

以下是我审查代码时确认符合设计文档的关键点：

### 6.1 安全合规
- ✅ CSRF 已启用（CookieCsrfTokenRepository，XSRF-TOKEN cookie HttpOnly=false）
- ✅ Server-side HTTP Session（非 JWT，非 LocalStorage）
- ✅ BCrypt cost = 12
- ✅ 密码策略：长度 12-128，不能全空白，不能等于 username
- ✅ 登录失败锁定：5 次 → 15 分钟（username + IP 维度）
- ✅ Bootstrap Admin 无硬编码密码（通过 env var 提供）
- ✅ must_change_password 合并进 V002（Final Review §8）
- ✅ 错误响应不含 stack trace
- ✅ 日志不记录 password/sessionId/csrfToken

### 6.2 数据库合规
- ✅ UUID 主键（Java 生成，不依赖 pgcrypto）
- ✅ TIMESTAMPTZ 时间字段
- ✅ VARCHAR + CHECK 约束（非 PostgreSQL ENUM）
- ✅ pg_trgm extension（不要求 pgcrypto）
- ✅ Flyway 严格顺序（V001 → V002 → V003）
- ✅ Seed 数据幂等（ON CONFLICT DO NOTHING）

### 6.3 架构合规
- ✅ Modular Monolith（按业务模块组织包）
- ✅ 生产 3 容器（nginx + backend + postgres）
- ✅ React SPA → Vite build → Nginx 静态托管
- ✅ 同 Origin（不开放 CORS）
- ✅ 不引入 Redis/Kafka/Elasticsearch/Drools 等

---

## 七、用户需要做的事

### 7.1 立即可做
1. **检查 Phase 0-3 代码**：`git log --oneline dev/v1-implementation` 查看提交历史
2. **决定 Phase 4 文件去留**：19 个未提交文件可保留继续开发或删除重来

### 7.2 推送代码
需要配置正确的 GitHub 认证后执行：
```bash
git push -u origin dev/v1-implementation
```
然后在 GitHub 上创建 PR：`dev/v1-implementation` → `main`

### 7.3 运行测试（准备好后）
```bash
# Backend 单元测试
cd backend && mvn test

# Backend 集成测试（需要 Docker）
cd backend && mvn verify

# Frontend 测试
cd frontend && npm test
```

### 7.4 Docker 部署验证
```bash
cd deploy
cp .env.example .env
# 修改 .env 中的密码
docker compose up --build
# 验证：http://localhost/actuator/health
```

---

## 八、下一步计划（待用户确认后）

根据 Implementation Plan V1.1，Phase 0-3 之后的开发顺序：

```
Phase 4（基础字典）← 当前暂停位置
  ↓
Phase 5（Capability Library）
  ↓
Phase 6（Master Test Case 基础）
  ↓
Phase 7（Test Case Lifecycle）
  ↓
Phase 8（Decision Point / DAG）
  ↓
...（共 30 个 Phase）
```

每个 Phase 遵循：代码 → Migration → 测试 → 验收，不跳过依赖关系。
