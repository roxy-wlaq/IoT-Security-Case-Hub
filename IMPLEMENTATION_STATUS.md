# IoT-Security-Case-Hub — Implementation Status

> 本文件记录 V1 实现进度，每次 Lead Agent 集成完成后更新。
> 设计依据：Final Technical Review V1.0（最高优先级）→ System Design V0.6 → 各 Detail 文档。

---

## Current Phase

**Phase 0–6 已完成**，代码已集成到 `dev/v1-implementation` 分支。

- 实现状态：`Implementation Complete / Unit + Integration + Frontend Verification`
- **Round 1（2026-09-02 早）：** 修复 **8 项** Phase 0–3 Code Review 发现（HIGH-01/02/03、MEDIUM-01/02/03/04、LOW-01），每 HIGH 增加 Regression Test，并补齐前端基础测试（MEDIUM-01）。（注：此前内部记录曾误写为"9 项"，实际表格为 8 项，本轮已校正。）
- **Round 2（2026-09-02 晚）：** 修复 **8 项** Review Round 2 发现（见下方独立章节）—— LoginAttempt 回归、HTTP overlay profile、SessionExpiry 双重注册/统一错误、SessionRegistry 生命周期、Session Fixation、文档与 must_change_password 语义。
- **Round 3（2026-09-02 凌晨）：** 最后小范围修复 **3 项**（HIGH×1、MEDIUM×1、LOW×1）—— 默认 Docker HTTP 与 Secure Cookie 冲突（HIGH-03 真正闭环）、登录后 SessionAuthenticationStrategy 顺序、加强 SessionFixationIT。不开发 Phase 4。
- **Phase 4 + Phase 5 Wave（2026-09-02）：** 双 Agent 并行实现基础字典与能力库（见下方 Phase 4 / Phase 5 章节），Lead 集成 Router/Sidebar 并全量验证。不开始 Phase 6。
- 技术栈保持冻结（Java 21 / Spring Boot 3 / Spring Security / Server-side Session / CSRF / PostgreSQL 16 / Flyway / React+TS / TanStack Query / Ant Design / Nginx / Docker Compose）。未引入 JWT，未关闭 CSRF，未重新设计架构。
- **Phase 6（2026-09-02）：** 完成 Master Test Case 基础：V006 数据模型、Draft 创建/编辑、可见性查询、分页搜索、版本历史、前端测试库与 Draft 编辑器；严格未实现 Phase 7 生命周期及后续 DAG/Project/Generation/Storage。

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

### Phase 4 — 基础字典 ✅ Completed（2026-09-02）

| 项目 | 状态 | 说明 |
|------|------|------|
| V003__reference_catalog.sql | ✅ | standard_task_types / categories（两级 CHECK 约束）/ tags / tools；tags/tools 增加用户指定的 `code` 列（相对 Schema V1.0 §9 的有意扩展，已在 SQL 注释声明）；pg_trgm GIN 搜索索引 |
| Standard / Task Type | ✅ | GET/POST/PUT `/api/v1/standard-task-types?q=&enabled=&type=`（**冻结契约为 `q`，非 `search`**）；type ∈ {STANDARD, TASK_TYPE}；code 大小写不敏感唯一 |
| Category | ✅ | GET `/api/v1/categories/tree?search=&enabled=`、POST/PUT；level 服务端由 parentId 推导；两级树（Level 2 下禁止 Level 3）；拒绝 self parent / 非法 parent / 后代作 parent（含祖先链遍历） |
| Tag | ✅ | GET/POST/PUT `/api/v1/tags?search=&enabled=`；code + name 双唯一（大小写不敏感）。Tag 用途 = Search / Filter / Learning / Stable Reference / Import-Export，**不是 Generation Rule Input** |
| Tool | ✅ | GET 列表+详情、POST/PUT `/api/v1/tools`；仅元数据 CRUD，附件上传留待后续 Storage Phase |
| RBAC | ✅ | 读：任意登录用户；写：`@PreAuthorize hasAuthority('{standard,category,tag,tool}:manage')`（权限码沿用 V002 seed，未新造） |
| Frontend | ✅ | `/admin/standards` `/admin/categories`（Tree/TreeTable 两级树）/`/admin/tags` `/tools`；Table + Modal Form（React Hook Form + Zod）；TanStack Query；PermissionGuard 控制管理按钮 |
| Tests | ✅ | 4 组 Service 单测（含 Category level1/2 成功、level3/self/invalid/后代 parent 拒绝）+ 4 组 @WebMvcTest RBAC 测试；Code Review Round 后 `mvn clean test` **116 tests 0 failures** |
| V005（Review Round 追加） | ✅ | `V005__fix_coordinator_reference_read_permissions.sql`：TEST_COORDINATOR 补 5 个 read 权限（幂等、不动 ADMIN/TESTER） |
| 前端既有 16 个 TS 错误 | ✅ | 本轮全部修复（Standard type 字面量化、Category enabled 类型、TagPage antd Tag 标识符撞车改 `Tag as AntdTag`）；typecheck 0 error |

### Phase 5 — Capability Library ✅ Completed（2026-09-02）

| 项目 | 状态 | 说明 |
|------|------|------|
| V004__capability_library.sql | ✅ | `casehub.capabilities` 自关联树；uq code + LOWER(code) 唯一；fk parent ON DELETE RESTRICT；注释声明环校验由 Service 层承担、停用不物理删除 |
| Backend | ✅ | `com.company.casehub.capability`（entity/repository/dto record/service/controller）；code 冲突 → ConflictException；非法 parent → BusinessRuleException |
| Cycle Validation | ✅ | CapabilityService 沿 parent 链向上遍历 + visited Set：拒绝 self cycle / two-node cycle / deep cycle；未引入图数据库 |
| API | ✅ | GET `/api/v1/capabilities/tree`、POST `/api/v1/capabilities`、PUT `/{id}`、POST `/{id}/enable`、POST `/{id}/disable`；读=登录用户，写=`capability:manage_library` |
| Enable/Disable | ✅ | 仅翻转 enabled，不物理 DELETE（历史引用保留）；不做子孙级联 |
| Frontend | ✅ | `/admin/capabilities`：左 Tree + 右 Detail Panel；Add Root / Add Child / Edit / Enable / Disable（PermissionGuard `capability:manage_library`）；无任何 YES/NO/UNKNOWN（属 Project Capability，后续 Phase） |
| Tests | ✅ | CapabilityServiceTest 18 项（create root/child、update、enable、disable、self/two-node/deep cycle、invalid parent、duplicate code）+ CapabilityControllerRbacTest 12 项（read allowed / tester denied / admin allowed） |
| 语义边界 | ✅ | Capability Tree 与 Category Tree 两张独立表；实体不含任何项目结论字段 |

> **Phase 4/5 Wave 测试与运行情况（如实记录）：**
> - 真实运行：`mvn clean test`（surefire，*Test 单元+RBAC 切片）**111 tests, 0 failures, 0 errors**；前端 `typecheck` 0 error / `lint` 0 warning / `test` 27 passed / `build` 成功。
> - 未运行：`mvn verify` 的 `*IT`（Testcontainers PostgreSQL 在本沙箱不可达，沿用 Round 3 结论，IT 维护 Pending，不标 PASS）；未搭建完整 Docker 运行时做浏览器端到端验证。
> - 前端 RBAC 测试基座：`common/MethodSecurityTestConfig`（@WebMvcTest 切片内恢复 @EnableMethodSecurity；Lead 预置共享，避免两个 Agent 各写一份）。POST/PUT 测试必须 `.with(csrf())`。
> - Migration 备注：V003 由 `V003__dictionary_tables.sql` 重命名为 `V003__reference_catalog.sql`（Lead 统一管理版本命名）。**已按旧 V003 建过库的开发库需 `flyway repair` 或重建 volume**；全新环境无影响。

### Phase 6 — Master Test Case 基础 ✅ Completed（2026-09-02）

| 项目 | 状态 | 说明 |
|------|------|------|
| V006 Migration | ✅ | 新增 `master_test_cases`、`test_case_versions`、`test_steps`、Master Tags、Version Tools/Standard Mappings、Version Attachment metadata；V001–V005 未修改 |
| Backend | ✅ | `testcase` 模块实现 Master/Version/Step/Relation entities、DTO、repositories、Draft service、visibility-aware query service、REST controller |
| Draft API | ✅ | 创建 `DRAFT 1.0`、全量编辑 Draft、步骤顺序重排、关系去重、Category/Tag/Tool/Standard 引用校验；Published/Review/Deprecated 不可经 Draft API 修改 |
| Query API | ✅ | `/api/v1/test-cases` 分页搜索、详情、版本历史、版本详情；实现 current published / own Draft / ADMIN 可见性与排序白名单 |
| RBAC | ✅ | 创建要求 `test_case:draft_create`；编辑要求 `test_case:draft_edit` 且资源为 Draft 所有者或 ADMIN；控制器不暴露 JPA Entity |
| Frontend | ✅ | `/test-cases` 测试库、`/test-cases/new` 创建 Draft、`/test-cases/:masterId` Draft 查看/编辑；共享类型、API、Query key、RHF/Zod 表单与 Query invalidation |
| Phase boundary | ✅ | 未实现 Publish、Return、Reject、Deprecated、Review Workflow、Decision Point、DAG、Project、Generation、Execution、Evidence、StorageService |
| Tests | ✅ | Backend `mvn clean test` 与 Frontend `npm test -- --run` 的最终结果记录于下方；V006 静态 migration contract、Draft service、visibility、RBAC、API/form 测试已加入 |

> **Phase 6 文档依据说明：** 用户任务列出的 `IoT-Security-Case-Hub_API-Design_V1.0.md` 与 `IoT-Security-Case-Hub_Frontend-Architecture_V1.0.md` 在当前仓库中不存在；本轮按 Final Technical Review、Database/Data Model、Backend/Security/Testing 文档及仓库中的冻结 `IoT-Security-Case-Hub_Phase6-API-Contract_V1.0.md` 实施。

> **Phase 6 最终验证（2026-09-02，实际执行结果）：**
> - Backend：`mvn -q clean test` 通过；`mvn -q verify` 通过，Testcontainers PostgreSQL 实际执行 18 个集成测试，0 failures / 0 errors；日志确认 Flyway V001–V006 全部成功迁移。
> - Frontend：`npm test -- --run` 通过（8 个 test files / 30 tests）；`npm run typecheck`、`npm run lint`、`npm run build` 均通过。Vite 仅提示 bundle 大小 warning，不影响构建。
> - Deployment config：`cp deploy/.env.example deploy/.env && docker compose -f deploy/docker-compose.yml config` 通过；临时 `.env` 未纳入提交。
> - Stability hardening：集成测试基类增加 `@DirtiesContext(AFTER_CLASS)`，避免 Testcontainers 生命周期与 Spring Context 缓存复用失效连接；注销流程显式移除 SessionRegistry 条目。
> - 未执行：完整 Docker 服务启动与浏览器端到端流程；本轮未实现 Phase 7 生命周期及后续模块。

### Phase 6 Code Review Round — Fixed（2026-09-02）

> 范围：仅闭环 Phase 6 Code Review，不启动 Phase 7；V006 保持不变，V007 为本轮 Review 修复迁移，V008 保留为 Phase 7 首个迁移号。

| 优先级 | 发现 | 修复与证据 |
|--------|------|------------|
| HIGH | V006 Master/Version 删除约束与目标语义不一致 | 新增 V007：将 `fk_test_case_versions_master` 修正为 `ON DELETE RESTRICT`，并为 `master_test_cases.case_code` 增加 pg_trgm GIN 索引；`MasterTestCaseSchemaIT` 在真实 PostgreSQL 上验证迁移、约束、唯一性和删除拒绝。 |
| HIGH | 仅通过 Mock/Criteria 无法证明 `caseName` 排序 | `TestCaseQueryService` 处理版本名称排序；`TestCaseDraftIT.sortByCaseNameDoesNotFail` 通过真实持久化 Draft 调用 `caseName,asc` 与 `caseName,desc`，断言返回顺序。 |
| HIGH | Phase 6 前端详情、编辑与最终接口契约未完全闭环 | 新增只读详情/版本历史页面，编辑路由独立为 `/test-cases/{masterId}/edit`；补齐库筛选、分页、标签/更新时间、Progressive Role、标准映射备注；新增 `docs/phase6-api-contract.md` 与一致性测试，契约以最终 Controller/DTO 为准。 |
| MEDIUM | `caseName` 排序参数会落到 Master 根实体并触发数据库错误 | 排序白名单保留 `caseName`，查询显式按版本名称聚合排序；PostgreSQL IT 对升序和降序均通过。 |
| MEDIUM | 新 Draft 可引用已禁用字典项 | Category、Tag、Tool、StandardTaskType 的新建/更新引用均要求存在且启用；历史版本读取仍保留；IT 覆盖 `disabledCategoryRejected`、`disabledTagRejected`、`disabledToolRejected`、`disabledStandardRejected`。 |

**本轮提交与测试：**

- 修复提交：`12a6b3d fix(phase6): correct master test case constraints`、`9e34945 fix(phase6): enforce review round backend behavior`、`fa018c6 fix(phase6): complete test case detail and library flows`。
- Backend：`mvn -q clean test`，125 tests / 0 failures / 0 errors；`mvn -q verify`，33 PostgreSQL Testcontainers IT / 0 failures / 0 errors。
- Frontend：9 test files / 39 tests 通过；`npm run typecheck`、`npm run lint`、`npm run build` 通过。
- 新增 PostgreSQL IT：`MasterTestCaseSchemaIT` 8 项，`TestCaseDraftIT` 7 项；新增接口契约一致性测试 1 项。
- Phase 7 未开始；下一迁移号为 V008，未实现发布、审核、驳回、弃用、决策点、DAG、项目和生成等后续能力。

### Phase 6 Query Semantics Final Review — Fixed（2026-09-02）

> 本轮仅处理 Phase 6 Query Semantics Review：HIGH ×1、MEDIUM ×2；无数据库 Schema 修改，V008 未创建。

| 优先级 | 发现 | 修复与证据 |
|--------|------|------------|
| HIGH | 多版本 Master 使用 `MIN(all versions.caseName)` 排序，可能与最终展示版本不一致 | 列表先按请求条件和可见性确定唯一 List Version，再以该 Version 的 `caseName` 排序并用同一 Version 构造 Summary；PostgreSQL IT 验证 Alpha/Zulu 与 Beta 的升降序结果，以及 `caseName`、`versionLabel`、`status` 一致。 |
| MEDIUM | Version 过滤命中版本与 Response 独立选择的 visible version 不一致 | `status`、版本侧 `q`、`toolIds`、`standardTaskTypeIds` 先筛选 Version，Summary 直接使用匹配的 List Version；PostgreSQL IT 覆盖 Draft/Published 及三类版本侧过滤。 |
| MEDIUM | Summary `updatedAt` 读取 Master 时间 | Summary `updatedAt` 改为 List Version 的 `updatedAt`；PostgreSQL IT 验证 Draft 更新后列表时间随实际 Version 更新。 |

**修复 SHA 与验证结果：**

- 修复提交：`da25a69 fix(phase6): align list summaries with selected version`。
- Backend：`mvn -q clean test` 通过，128 tests / 0 failures / 0 errors / 0 skipped；`mvn -q verify` 通过，PostgreSQL Testcontainers IT 通过；Flyway 实际最高版本为 V007。
- Query 单元测试：`multiVersionCaseNameSortSemantics`、`statusFilterReturnsMatchingVersion`、`summaryUpdatedAtUsesVersion` 均通过；契约测试同步断言 List Version 语义。
- Frontend：`npm run typecheck`、`npm run lint`、`npm run test`（9 files / 39 tests）、`npm run build` 均通过；仅保留既有 Vite bundle size warning。
- `docs/phase6-api-contract.md` 已记录最终实际接口及 List Version Selection Semantics；未修改前端 API contract。

### Phase 6 Query Architecture Final Fix — Verified（2026-09-03）

> 本轮仅闭环 Phase 6 列表查询架构的 **HIGH ×2**；不启动 Phase 7，不修改数据库 Schema 或前端源码。实现修复 SHA：`9d53a5b`；文档随后单独提交。

| 优先级 | 发现 | 修复与证据 |
|--------|------|------------|
| HIGH | `q` 未严格实现 Master/Version OR，可能错误要求选中的 Version 自身也匹配 Master 级命中词，或由不同 Version 分别满足 `q` 与版本过滤 | PostgreSQL 候选谓词固定为 `MasterMatch(caseCode/tag name) OR same-row VersionMatch(caseName/testPurpose/step/tool)`。Master 命中时，候选 Version 无需再匹配 `q`；但 `status`、`toolIds`、`standardTaskTypeIds` 与 VersionMatch 都约束同一个候选 Version 行。最终 Summary 的 `caseName`、`status`、`versionLabel`、`updatedAt` 及版本侧排序均使用选中的同一个 List Version。`TestCaseDraftIT` 在 PostgreSQL 16.6 上覆盖 Master 命中不限制 Draft、Version `q`/status 同行、tool/standard 同行及多版本排序字段一致性。 |
| HIGH | 列表在 Java 内全量加载并过滤、排序、分页、计数，无法保证数据库分页和 count 语义一致 | 新增 PostgreSQL 查询仓库，在数据库内完成候选过滤、每 Master 一个 List Version、排序、`LIMIT/OFFSET` 分页和 `COUNT(*)`；Service 只批量装载页面返回的 Master/Version ID，并按数据库行顺序构造 Summary，不再调用 `masterRepository.findAll()`。`databasePaginationIsStable` 验证相邻页面无重叠、顺序与总数稳定。 |

**最终契约与验证证据：**

- **Count Contract：** page SQL 与 count SQL 由同一个 `candidate_versions` / `selected_versions` CTE 字符串构造，共享全部可见性、`status`、`q`、`toolIds`、`standardTaskTypeIds` 条件，也共享 Master 级 `categoryId` 与 `tagIds`；count 是同一候选和 List Version 选择语义产生的 Master 数量。
- **Stable Pagination Contract：** `caseName`、`updatedAt`、`createdAt`、`caseCode` 的每一种升/降序最终都追加唯一且方向固定的 `selected_versions.master_id ASC` tie-breaker，然后才执行数据库 `LIMIT/OFFSET`。
- Backend：`mvn clean test` 通过，Surefire **129 tests / 0 failures / 0 errors / 0 skipped**；`mvn verify` 通过并再次执行 129 个 Surefire tests，Failsafe **43 PostgreSQL IT / 0 failures / 0 errors / 0 skipped**，其中 `TestCaseDraftIT` **17 / 0 / 0 / 0**。
- PostgreSQL/Flyway：实际测试数据库为 **PostgreSQL 16.6**；日志为 `Successfully applied 7 migrations ... now at version v007`。Migration 清单仅 V001–V007，**V008 未创建**，migration 文件无修改。
- Frontend：`npm run typecheck`、`npm run lint`、`npm run test`、`npm run build` 均通过；Vitest **9 files / 39 tests**，build 转换 3203 modules，仅有既存的 chunk >500 kB warning；`frontend/src` 无变更。

---

## Phase 0–3 Code Review Findings — Fixed (2026-09-02)

> 范围：仅修复 Phase 0–3 Review 发现，不扩大到 Phase 4+，不重构，不更换技术栈。
> 提交：`fix: resolve phase 0-3 review findings`（分支 `dev/v1-implementation`）。

| 优先级 | 编号 | 问题 | 根因修复 | 关键文件 |
|--------|------|------|----------|----------|
| P0 | HIGH-01 | Session 失效未闭环（expireNow 后仍可用旧 JSESSIONID） | 新增 `SessionExpiryFilter`（`OncePerRequestFilter`），在后续请求中检查 `SessionInformation.isExpired()`，过期则 invalidate + clearContext + 401；经 `SecurityConfig` 在 `SecurityContextHolderFilter` 之后注入 | `auth/security/SessionExpiryFilter.java`（新）、`auth/security/SecurityConfig.java` |
| P0 | HIGH-02 | 错误实现"首次登录强制改密" | `BootstrapUserService` 创建 ADMIN 时 `mustChangePassword=false`；删除 `MustChangePasswordFilter` 及其接线；前端 `AppLayout` 移除强制改密门 | `auth/service/BootstrapUserService.java`、`auth/security/MustChangePasswordFilter.java`（删）、`auth/security/SecurityConfig.java`、`features/layout/AppLayout.tsx` |
| P0 | HIGH-03 | 默认 Docker 部署 HTTP 与 Secure Session Cookie 冲突 | ⚠️ **首轮方案已被 Round 2 取代**：首轮用 `dev` profile 默认解决，但 `dev` 会暴露全部 Actuator 且非生产级。Round 2 改为 `prod` 默认 + 独立 `prod,http` overlay（`application-http.yml` 仅覆盖 `cookie.secure=false`），并删除 `application-dev.yml`。详见下方 Round 2 章节。 | `deploy/docker-compose.yml`、`src/main/resources/application-dev.yml`（已删）、`application-http.yml`（新）、`application.yml` |
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

## Phase 0–3 Code Review Findings — Round 2 (2026-09-02 晚)

> 基线：`dev/v1-implementation` @ `8d4cef8`。范围：仅修复 Review Round 2 指定问题，不扩大到 Phase 4+，不重构，技术栈冻结。
> 提交：`fix: resolve phase 0-3 review findings round 2`（独立 commit，同分支 `dev/v1-implementation`）。

| 优先级 | 编号 | 问题 | 根因修复 | 关键文件 |
|--------|------|------|----------|----------|
| HIGH | 1 | LoginAttemptService 回归：`isBlocked` 删除未达阈值的失败计数，导致真实登录流程计数被不断重置、锁定失效 | `isBlocked` 改用 `computeIfPresent`：未达锁定阈值（`blockedUntil==null`）的 Attempt **保留**；仅当 `blockedUntil!=null` 且已过期才删除。`recordFailure` 原子 `compute` 保持不变。 | `auth/service/LoginAttemptService.java` |
| HIGH | 2 | 不应以 `dev` profile 作为默认部署方案解决 HTTP Secure Cookie | `application.yml` 默认 `prod`；删除 `application-dev.yml`；新增 `application-http.yml`（**仅**覆盖 `server.servlet.session.cookie.secure=false`）；docker-compose 默认 `prod`，注释说明 `prod,http` overlay。prod 保持 `secure=true` + 受限 Actuator（不开 dev）。 | `application.yml`、`application-http.yml`（新）、`application-dev.yml`（删）、`deploy/docker-compose.yml` |
| MEDIUM | 3 | SessionExpiryFilter 双重注册风险（既是 Filter Bean 又被手工加入 SecurityChain） | 新增 `FilterRegistrationBean<SessionExpiryFilter>` 且 `setEnabled(false)`，使其只在 Spring Security FilterChain 指定位置执行，避免作为独立 Servlet Filter 重复运行。 | `auth/security/SecurityConfig.java` |
| MEDIUM | 4 | Session expired 未使用统一 API Error | `SessionExpiryFilter` 改为委托 `RestAuthenticationEntryPoint`（`InsufficientAuthenticationException`），统一返回 `code=AUTH_UNAUTHENTICATED` / 401 / traceId / details；不再 `response.sendError(401, "Session expired")`。 | `auth/security/SessionExpiryFilter.java`、`auth/security/SecurityConfig.java`、`integration/SessionExpirationIT.java`（新增 code 断言） |
| MEDIUM | 5 | SessionRegistry 生命周期不完整 | 新增 `HttpSessionEventPublisher` Bean，使 `SessionRegistryImpl` 在 logout / timeout / invalidate / expire 后清理无效 `SessionInformation`。 | `auth/security/SecurityConfig.java`、`integration/SessionRegistryLifecycleIT.java`（新） |
| MEDIUM | 6 | 缺少 Session Fixation 保护 | `AuthenticationService` 登录成功后应用 `SessionAuthenticationStrategy`（Composite: `ChangeSessionIdAuthenticationStrategy` + `RegisterSessionAuthenticationStrategy`），移除手写 `sessionRegistry.registerNewSession`。 | `auth/service/AuthenticationService.java`、`auth/security/SecurityConfig.java`（strategy Bean）、`integration/SessionFixationIT.java`（新） |
| LOW | 7 | 文档/注释与实现冲突 | `BootstrapUserService` 注释改为 `must_change_password=false` / Dormant；修正本文档 "9 项" 与表格（实际 8 项）不一致。 | `auth/service/BootstrapUserService.java`、`IMPLEMENTATION_STATUS.md` |
| — | 8 | must_change_password 语义澄清 | Bootstrap Admin 保持 `mustChangePassword=false`；DB 字段保留；强制改密流程标记为 **Future / Dormant Capability**（V1 尚未实现 Admin Reset Password），不声称已具备完整强制执行能力。 | `auth/service/BootstrapUserService.java`、`IMPLEMENTATION_STATUS.md` |

### Round 2 新增/修改测试

| 测试文件 | 覆盖 |
|----------|------|
| `backend/.../auth/service/LoginAttemptServiceTest.java`（改） | 新增 `isBlockedDoesNotResetCounterAcrossAttempts`：isBlocked → recordFailure ×5 → 下一次 isBlocked 必须 true |
| `backend/.../auth/security/SessionExpiryFilterTest.java`（新） | 过期 Session → 401 + `code=AUTH_UNAUTHENTICATED` + traceId + session 失效；有效 Session 放行（纯单测，无需 DB） |
| `backend/.../integration/SessionExpirationIT.java`（改） | 新增 `$.code == AUTH_UNAUTHENTICATED` 断言 |
| `backend/.../integration/SessionRegistryLifecycleIT.java`（新） | logout / server-side expire 后 SessionRegistry 不保留无效 SessionInformation |
| `backend/.../integration/SessionFixationIT.java`（新） | 匿名 Session → Login → Session ID 变化 → 新 Session 可访问 /me → 旧 Session ID 不再认证 |

> **单元测试（`*Test`，`mvn test`，无需 DB）已执行并通过**（见 Verification Summary）。
> **集成测试（`*IT`，`mvn verify`，Testcontainers PG16）因本机无 Docker 运行时未能执行**，故不标记为 PASS；代码逻辑已按 Spring 标准机制实现（`HttpSessionEventPublisher` / `SessionAuthenticationStrategy`），在具备 Docker 的环境可正常回归 Round 2 的 3 个 IT。

---

## Phase 0–3 Code Review Findings — Round 3 (2026-09-02 凌晨)

> 基线：`dev/v1-implementation` @ `84196d4`。范围：仅做最后小范围修复，不开发 Phase 4。
> 提交：`fix: resolve phase 0-3 review findings round 3`（独立 commit，同分支 `dev/v1-implementation`）。

| 优先级 | 编号 | 问题 | 根因修复 | 关键文件 |
|--------|------|------|----------|----------|
| HIGH | 1 | 默认 Docker HTTP 与 Secure Cookie 仍冲突：prod profile `cookie.secure=true`，而 Nginx 默认只监听 HTTP 80，直接 `docker compose up --build -d` 得到 HTTP + Secure JSESSIONID，HIGH-03 实际未闭环 | docker-compose 默认 `SPRING_PROFILES_ACTIVE=prod,http`（`http` overlay **仅**覆盖 `cookie.secure=false`，不改 Actuator / Security / CSRF）；HTTPS 部署改 `SPRING_PROFILES_ACTIVE=prod` 恢复 `secure=true`。`application-http.yml` 设计正确，**保留**。` .env.example` 与部署注释（README §5 / §10）同步说明 HTTP / HTTPS 两种 profile 用法。 | `deploy/docker-compose.yml`、`deploy/.env.example`、`deploy/README.md` |
| MEDIUM | 2 | 登录后 SessionAuthenticationStrategy 顺序不当：先持久化已认证 SecurityContext，再跑 strategy；若 strategy 抛 `SessionAuthenticationException`，会留下未经验证的已认证 context | 调整 `AuthenticationService.login()`：先 `sessionAuthenticationStrategy.onAuthentication(...)`，再创建并 `saveContext` 已认证 SecurityContext。保持 `ChangeSessionId` + `RegisterSession` 不变。 | `auth/service/AuthenticationService.java` |
| LOW | 3 | SessionFixationIT 用"重建空 MockSession → 401"作为旧 Session 失效证据，证明力有限（空 Session 本就无认证） | 重写断言：① 登录前后 session id 不同；② 新 session `/api/v1/auth/me` = 200；③ `SessionRegistry` 含新 session id；④ `SessionRegistry` 不含旧 session id。不再以空 MockSession 401 为主证据。 | `integration/SessionFixationIT.java` |

### Round 3 修改/加强测试

| 测试文件 | 覆盖 |
|----------|------|
| `backend/.../integration/SessionFixationIT.java`（改） | 登录前后 session id 不同；新 session /me = 200；SessionRegistry 含新 id、不含旧 id（移除弱 401 断言） |
| `backend/.../integration/SessionExpirationIT.java` / `SessionRegistryLifecycleIT.java` | 维持 Round 2 实现（本轮未改） |

> **单元测试（`*Test`，`mvn test`，无需 DB）已执行并通过**（见 Verification Summary）。
> **集成测试（`*IT`，`mvn verify`，Testcontainers PG16）本轮尝试执行但因 Testcontainers 网络不可达而失败，如实标记为 Pending，不标 PASS**（见注 4）。

---

## Verification Summary

| 验证项 | 命令 | 结果 |
|--------|------|------|
| Backend 编译 | `mvn clean compile` | ✅ BUILD SUCCESS |
| Backend Unit Test | `mvn test`（surefire，*Test） | ✅ Round 2 新增 `LoginAttemptServiceTest`（改）、`SessionExpiryFilterTest`（新）；全部通过，0 failures（见 Round 2 验证命令） |
| Backend 集成测试 | `mvn verify`（failsafe，*IT，Testcontainers PG16） | ⚠️ Round 3 尝试执行（Docker daemon 在线）但 Testcontainers PostgreSQL 不可达（`Connection refused`），10/16 IT error；**如实标记 Pending，不标 PASS**（见注 4） |
| Frontend 类型检查 | `npm run typecheck` | ⚠️ 16 错误，全部位于既有 Phase 4 字典文件（非本轮修改，见注 2） |
| Frontend Lint | `npm run lint` | ✅ 通过（0 warnings） |
| Frontend 测试 | `npm run test`（vitest） | ✅ 27 tests, 0 failures（本轮未新增前端测试） |
| Frontend 构建 | `npm run build` | ⚠️ 受注 2 的 Phase 4 既有类型错误阻断 |
| Docker Compose 配置 | `docker compose -f deploy/docker-compose.yml config` | ✅ 解析出 `SPRING_PROFILES_ACTIVE=prod,http`（按文档 `cp .env.example .env` 生成本地 `.env` 后验证；`.env` gitignore 不提交） |
| 完整 Docker 运行时 + 浏览器登录 | `docker compose up --build` | ⚠️ 环境受限未执行（见注 1 / 注 4） |

### Round 2 验证命令与结果（2026-09-02 晚）

| 命令 | 结果 |
|------|------|
| `mvn clean test` | ✅ BUILD SUCCESS；本轮新增/修改的单元测试全部通过（`LoginAttemptServiceTest`、`SessionExpiryFilterTest` 等） |
| `mvn verify` | ⚠️ 未执行（failsafe 需 Testcontainers PG16，本机无 Docker 运行时） |
| `npm run typecheck` | ⚠️ 16 错误，全部在既有 Phase 4 字典文件（见注 2），非本轮修改 |
| `npm run lint` | ✅ 通过（0 warnings） |
| `npm run test` | ✅ 27 tests, 0 failures |
| `npm run build` | ⚠️ 受注 2 的 Phase 4 既有类型错误阻断 |
| `docker compose -f deploy/docker-compose.yml config` | ✅ 验证通过（默认 `prod`；`prod,http` overlay 说明见 compose 注释） |

> **注 1（集成测试 / 运行时验证）：** 本机 Testcontainers 无法启动 PostgreSQL 容器（`Connection refused to localhost:62774` / `CannotCreateTransaction`），属基础设施限制而非代码缺陷。后端单元测试已验证本次改动的逻辑正确性；Round 1/2 新增的 IT（`SessionExpirationIT` / `SessionRegistryLifecycleIT` / `SessionFixationIT`）在具备 Docker 的环境下可正常回归。
>
> **注 2（前端 typecheck / build）：** `npm run typecheck` 与 `npm run build` 报告的 16 个错误**全部位于上一轮已提交的 Phase 4 字典文件**（`CategoryPage.tsx` / `StandardPage.tsx` / `TagPage.tsx`）。这些文件属于 Phase 4+，**不在本轮"只修复 Phase 0–3"范围内**，本轮未改动。建议下一轮 Phase 4 Code Review 修复这些既有类型错误。
>
> **注 3（Round 2 集成测试如实报告）：** 按要求，未执行的 Integration Test **不标记为 PASS**。`mvn verify` 因本机无 Docker 运行时未能执行；Round 2 的 3 个 IT（`SessionExpirationIT` 更新、`SessionRegistryLifecycleIT`、`SessionFixationIT`）已写入仓库，其正确性依赖 Spring 标准机制（`HttpSessionEventPublisher` / `SessionAuthenticationStrategy` / `RestAuthenticationEntryPoint`），待具备 Docker 的 CI 环境回归。

### Round 3 验证命令与结果（2026-09-02 凌晨）

| 命令 | 结果 |
|------|------|
| `mvn clean test` | ✅ BUILD SUCCESS；27 tests, 0 failures（含 Round 2 新增单元测试；本轮仅改 `SessionFixationIT` 为 IT，不改单测逻辑） |
| `mvn verify` | ⚠️ Docker daemon 在线，但 Testcontainers PostgreSQL 不可达（`java.net.ConnectException: Connection refused`，`CannotCreateTransaction`），16 IT 中 10 error、0 failure → **BUILD FAILURE**。**IT 不标记 PASS，标记 Pending** |
| `docker compose -f deploy/docker-compose.yml config` | ✅ 解析出 `SPRING_PROFILES_ACTIVE: prod,http`（HIGH-03 闭环：默认 HTTP 部署不再产生 Secure JSESSIONID）；HTTPS 用 `prod` 恢复 `secure=true` |

> **注 4（Round 3 集成测试如实报告）：** 本轮 Docker daemon 实际在线，因此按用户要求尝试了 `mvn verify`。但 Testcontainers 启动的 PostgreSQL 容器从测试 JVM 不可达（`Connection refused`），所有依赖 DB 的 IT（`AuthFlowIT` ×6 setUp、`SessionFixationIT`、`SessionRegistryLifecycleIT` 等）在建立 JPA 连接时失败。这仍是**基础设施/网络限制**（Testcontainers 在该沙箱内无法正确端口映射），**非代码缺陷**。故 Integration Test **如实标记 Pending，绝不标记 PASS**；3 个 IT 已在仓库，待具备可用 Docker/Testcontainers 的 CI 环境回归。

---

### Phase 4/5 Wave 验证命令与结果（2026-09-02）

| 命令 | 结果 |
|------|------|
| `mvn clean test`（集成后全量） | ✅ BUILD SUCCESS；**111 tests, 0 failures, 0 errors**（Phase 0–3 基线 27 + Phase 4 字典 54 + Phase 5 能力库 30） |
| `mvn verify`（*IT） | ⚠️ 未执行：Testcontainers PostgreSQL 仍不可达（沿用 Round 3 结论），IT 维持 Pending |
| `npm run typecheck` | ✅ **0 error**（既有 16 个错误本轮全部修复） |
| `npm run lint` | ✅ 通过（0 warnings，`--max-warnings 0`） |
| `npm run test` | ✅ 27 tests, 0 failures |
| `npm run build` | ✅ 成功（仅 chunk >500kB 提示，非错误） |
| Agent worktree 验证 | ✅ phase4-reference：mvn 81 tests 0 failures、前端四命令全绿；phase5-capability：mvn 57 tests 0 failures、capability 文件 0 error 0 warning、27 既有测试通过 |

---

## Phase 4/5 Code Review Round（2026-09-02）

范围：**只修复 Phase 4/5 Review 发现**。禁止开始 Phase 6 / Phase 7 / MasterTestCase / TestCaseVersion —— 本轮严守边界，未新增任何 Phase 6+ 代码。

| # | 严重度 | 问题 | 修复 | 状态 |
|---|--------|------|------|------|
| 1 | **HIGH** | TEST_COORDINATOR 缺少 reference / capability read 权限（V002 未授予），导致权限驱动 UI（RouteGuard / PermissionGuard / 导航过滤）对协调员隐藏字典与能力库页面 | 新增 **`V005__fix_coordinator_reference_read_permissions.sql`**：为 TEST_COORDINATOR 追加 `standard:read` / `category:read` / `tag:read` / `tool:read` / `capability:read`；`ON CONFLICT (role_id, permission_id) DO NOTHING` 保证幂等；仅追加，**不改 ADMIN / TESTER**（ADMIN 仍 54、TESTER 仍 21）。**未修改冻结的 V002** | ✅ |
| 2 | **MEDIUM** | 能力库前端路由写成 `/capabilities` | 统一改为 **`/admin/capabilities`**：`navigation.ts` 条目 + `router.tsx` 页面映射同步修正。后端 API `/api/v1/capabilities/*` **未改动**（冻结） | ✅ |
| 3 | **MEDIUM** | Standard 列表查询参数用了 `search`，与冻结契约不符 | 统一到冻结契约的 **`q`**：`StandardTaskTypeController`（`@RequestParam("q")`）、`StandardTaskTypeService.list(String q, ...)` 及 `querySpec(q, ...)`、前端 `StandardListParams{ q }`、standardApi 注释、StandardPage 过滤状态。仅 standard 改用 `q`；category / tag / tool 仍用 `search`（其契约如此）。**不同时兼容 `search`** | ✅ |
| 4 | **LOW** | V003 注释把 Tag 的 `code` 描述为用于 "generation-rule matching"，语义错误 | 保留 `tags.code` / `tools.code` 字段，仅修正注释：Tag 正式用途 = **Search / Filter / Learning / Stable Reference / Import-Export**，并显式声明 **Tag 不是 Generation Rule Input**（生成条件由 GenerationRule 依据 project capability 判定，Phase 11） | ✅ |

### 修复 Commit

| 内容 | Commit |
|------|--------|
| HIGH×1 / MEDIUM×2 / LOW×1 全部修复 | **`6c30914`** — `fix(phase4/5): resolve code review findings (HIGH x1, MEDIUM x2, LOW x1)`（本行 SHA 由紧随其后的文档提交写入，故与修复提交本身不是同一条） |

### 新增/修改测试

| 测试 | 覆盖 | 是否真实运行 |
|------|------|--------------|
| `backend/.../migration/V005CoordinatorReadPermissionsTest`（新，4 项） | V005 授予的正是那 5 个 read 权限且不含 manage；仅作用于 TEST_COORDINATOR；`ON CONFLICT DO NOTHING` 幂等、无 DELETE/UPDATE/DROP；V001–V004 仍在、V006 未被占用 | ✅ 已运行通过 |
| `backend/.../standard/controller/StandardTaskTypeControllerRbacTest`（改 + 新增 1） | `readPassesQueryFiltersToService` 改用 `?q=`；新增 `legacySearchParameterIsNotBound`：传 `search` 时服务收到 `null`，防止旧参数名被静默兼容 | ✅ 已运行通过 |
| `backend/.../integration/MigrationIT`（改） | 角色权限总数 106 → **111**（ADMIN 54 + COORDINATOR 36 + TESTER 21）；新增 `coordinatorHasReferenceAndCapabilityReadPermissions`（5 个 read 全在、0 个 manage）、`adminAndTesterPermissionSetsAreUnchangedByV005`（54 / 21 不变） | ⚠️ **Pending** —— Testcontainers 不可用（见下） |

### 本轮验证结果（真实执行）

| 命令 | 结果 |
|------|------|
| `mvn clean test` | ✅ BUILD SUCCESS；**116 tests, 0 failures, 0 errors**（上轮 111 + V005 契约测试 4 + standard q 契约测试 1） |
| `mvn verify`（*IT） | ⚠️ **本轮实际执行过，仍失败**：Docker daemon 在线但 Testcontainers PostgreSQL 从测试 JVM 不可达（`CannotCreateTransaction: Could not open JPA EntityManager`），**18 IT 中 10 error、0 failure → BUILD FAILURE**。IT 如实标记 **Pending，不标 PASS** |
| `npm run typecheck` | ✅ 0 error |
| `npm run lint` | ✅ 0 warning（`--max-warnings 0`） |
| `npm run test` | ✅ 27 tests, 0 failures |
| `npm run build` | ✅ 成功 |

> **注 5（本轮 IT 如实报告）：** 与 Round 3 结论一致，属沙箱基础设施/网络限制，非代码缺陷。新增的 `MigrationIT` 断言（111 总数 / 协调员 5 个 read / ADMIN·TESTER 不变）**已写入仓库但未执行**，待具备可用 Testcontainers 的 CI 环境回归。为让 V005 的保证在无 DB 环境下仍可验证，额外提供了可真实运行的静态契约测试 `V005CoordinatorReadPermissionsTest`（已通过）。

> **Migration 版本所有权：** V005 由本轮 Review 修复占用。**Phase 6 及后续 Migration 必须从 V006 开始。**

---

## In Progress

无。Phase 0–5 已完成，Code Review Round 修复已集成。

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
| Phase 4 Reference（Agent A） | `agent/phase4-reference` | `f320173` | 54 文件 / +2913 −322 |
| Phase 5 Capability（Agent B） | `agent/phase5-capability` | `ad4a682` | 17 文件 |

所有 agent 分支已合并到 `dev/v1-implementation`（Phase 4/5 均为 no-conflict merge，V003/V004 与各模块包零交叉）。

---

## Next Wave

Code Review 通过后，下一轮开发（**本轮未开始，遵守 Phase 边界**）：

- **Phase 6 — Master Test Case 基础**
- **Phase 7 — Test Case Lifecycle**

---

## Architecture Notes

- **Modular Monolith**：后端按业务模块组织（auth / user / project / capability / testcase / generation / execution / evidence / changerequest / export / audit / storage / common / tool）
- **Session 认证**：Server-side HTTP Session + JSESSIONID（HttpOnly, SameSite=Lax），不使用 JWT
- **CSRF**：CookieCsrfTokenRepository，XSRF-TOKEN cookie（HttpOnly=false）→ X-XSRF-TOKEN header
- **Database**：PostgreSQL 16，schema `casehub`，UUID 主键（Java 生成），TIMESTAMPTZ 时间
- **Migration**：Flyway 严格顺序，Lead 为 Migration Version Owner
- **Deployment**：3 容器（nginx + backend + postgres），Nginx 承载 React SPA + /api 反代
