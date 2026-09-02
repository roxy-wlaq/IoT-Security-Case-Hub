# Phase 7 — Test Case Lifecycle — Implementation Plan

> Lead Agent 冻结。开发顺序与边界以此文件为准。
> 基线：`dev/v1-implementation` @ `0f77b6b`（Phase 6 FINAL PASS，Flyway 最高版本 V007）。
> 接口字段名以 [`docs/phase7-api-contract.md`](./phase7-api-contract.md) 为唯一依据。

---

## 1. 设计依据与优先级

冲突时优先级（沿用既有约定）：

```text
Final Technical Review V1.0
> System Design V0.6
> Security / Testing Detail
> API / Backend / Frontend Architecture
> Database Schema V1.0
> Data Model V1.0
```

`IoT-Security-Case-Hub_API-Design_V1.0.md` 与 `IoT-Security-Case-Hub_Frontend-Architecture_V1.0.md`
在本仓库中不存在（Phase 6 已确认），因此 Phase 7 接口命名采用任务书 §19 的推荐命名，
并冻结进 `docs/phase7-api-contract.md`。

---

## 2. 冻结的业务规则（不可协商）

| 编号 | 规则 | 来源 |
|---|---|---|
| R1 | Version Status 只能 `DRAFT` / `REVIEW` / `PUBLISHED` / `DEPRECATED`；**不存在** `REJECTED` | Final Review §11 |
| R2 | Reject 后 `status = REVIEW` + `revision_closed = true` + `ReviewRecord.action = REJECT` | Final Review §11 |
| R3 | Published Version 绝对不可原地修改（含 ADMIN），修改必须走 Create Revision | Final Review §12 / SD §41 |
| R4 | 同一 Master 最多一个 `is_current_version = true`，且只允许 PUBLISHED | Schema §11.2 CHECK + partial unique index |
| R5 | Master 是稳定身份，Revision 只新建 Version，不复制 Master | Data Model §8/§9 |
| R6 | Return ≠ Reject：Return → `DRAFT` + `revision_closed = false`，可再次 Submit | 任务书 §9 |
| R7 | Deprecate → `DEPRECATED` + `is_current_version = false` + `deprecated_at != null`，历史保留 | Final Review §10 / SD §49 |
| R8 | Version Number 由服务端决定，客户端不可指定 major/minor | 任务书 §13 |
| R9 | ReviewRecord 是 append-only 历史，不用 Version 上最后一次 comment 替代 | 任务书 §14 |
| R10 | Contributor 与 Draft Owner 必须区分；资源级权限生效 | SD §43 / 任务书 §15 |

### 2.1 Version Number 规则（R8 的具体化）

SD §41 的冻结示例为：

```text
Published v1.2 → Create Revision → Draft v1.3 → Review → Published v1.3
```

因此 Phase 7 采用：

```text
source Published (major = M, minor = N)
→ Revision Draft (major = M, minor = MAX(minor of all versions with major = M) + 1)
```

- major 保持不变（Revision 不是破坏性重构，冻结设计未定义 major 递增触发条件，V1 不发明）。
- minor 取该 major 下**所有**已存在版本（含 Draft/Review/Deprecated）的最大 minor + 1，
  避免与已占用的版本号冲突。
- 计算在 `PESSIMISTIC_WRITE` 锁定的 Master 行事务内完成，禁止裸 `MAX()+1` 后无锁 insert。

---

## 3. 状态转换表

| Action | 源状态 | 目标状态 | 额外写入 | 权限码 | 角色可达性 |
|---|---|---|---|---|---|
| Submit Review | `DRAFT` | `REVIEW` | ReviewRecord(SUBMIT)、`revision_closed` 不变(false) | `test_case:submit_review` + 资源级 | ADMIN / COORDINATOR |
| Publish | `REVIEW` | `PUBLISHED` | `is_current_version=true`、`published_at`、`reviewed_by`、`revision_closed=true`；旧 current 置 false；ReviewRecord(PUBLISH) | `test_case:review` + `test_case:publish` | 仅 ADMIN |
| Return | `REVIEW` | `DRAFT` | `revision_closed=false`、`reviewed_by`；ReviewRecord(RETURN) | `test_case:review` | 仅 ADMIN |
| Reject | `REVIEW` | `REVIEW`（不变） | `revision_closed=true`、`reviewed_by`；ReviewRecord(REJECT) | `test_case:review` | 仅 ADMIN |
| Deprecate | `PUBLISHED` | `DEPRECATED` | `is_current_version=false`、`deprecated_at`、`revision_closed=true`；ReviewRecord(DEPRECATE) | `test_case:deprecate` | 仅 ADMIN |
| Create Revision | `PUBLISHED`（源版本） | 新建 `DRAFT` | 新 Version 行 + 复制内容 + `based_on_version_id` | `test_case:draft_create` | ADMIN / COORDINATOR |
| Edit Draft | `DRAFT` | `DRAFT` | 内容全量替换 | `test_case:draft_edit` + 资源级 | Owner / Contributor / ADMIN |

无效转换统一抛 `TEST_CASE_LIFECYCLE_TRANSITION_INVALID`（422），不静默忽略。
被 `revision_closed` 关闭的 Revision 上再做 Submit/Publish/Edit 统一抛
`TEST_CASE_REVISION_CLOSED`（409）。

### 3.1 资源级权限（Service 层，Controller `@PreAuthorize` 之外的第二道闸）

- **Edit Draft / Submit Review**：`ADMIN` **或** `version.createdBy == 当前用户` **或** 当前用户是该 Version 的 Contributor。
- **Manage Contributor**：`ADMIN` **或** `version.createdBy == 当前用户`。
- **Publish / Return / Reject / Deprecate**：只由权限码控制（ADMIN 独占），
  但**仍然**在 Service 层断言状态与 `revision_closed`。
- ADMIN 不是"自动拥有全部业务 Actor 行为"：ADMIN 走 Lifecycle API 时
  **不**获得绕过 Published Immutable 的能力，也**不**获得把别人 Draft 当成自己 Draft 编辑的能力
  —— ADMIN 的编辑权来自显式 ADMIN 分支，仍需遵守 `status == DRAFT` 与 `revision_closed == false`。

---

## 4. Migration

### V008__test_case_lifecycle.sql

新增两张表（严格按 Schema §32.1 / §33.1 的列定义）：

- `casehub.revision_contributors`
- `casehub.test_case_review_records`

**V001–V007 一律不修改、不重写。**

#### 4.1 声明式偏差：ReviewRecord action 集合

Schema §33.1 冻结：

```sql
CHECK (action IN ('PUBLISH', 'RETURN', 'REJECT'))
```

任务书 §14 要求 Review History 至少记录 `SUBMIT` / `DEPRECATE`。
由于 Schema §33.1 的表在 V001–V007 中**尚未创建**，V008 是直接建表，
不存在"修改已冻结约束"的问题；但 action CHECK 取值确实相对 §33.1 有扩展。

**决定：** V008 建表时采用 5 值 CHECK
`('SUBMIT','PUBLISH','RETURN','REJECT','DEPRECATE')`，
并在 SQL 注释中显式声明这是对 §33.1 的有意扩展及其理由
（§33.1 原文 3 值无法表达 Submit 与 Deprecate 的审计记录；Final Review §11 需要
"latest ReviewRecord" 来推导 UI 的 Rejected 语义，SUBMIT 记录是完整审计链的起点）。

该偏差将同步记录在 `IMPLEMENTATION_STATUS.md` 的 Known Limitations。

#### 4.2 不复制 Attachment metadata 的决定

任务书 §12 列出 Revision 需继承 `attachments metadata（按冻结设计）`。
Schema §11.7 对 `test_case_attachments.storage_key` 定义了**全局 UNIQUE**。
Phase 15 才实现真正的 Storage，此时复制 metadata 行必然：

- 要么复制相同 `storage_key` → 违反 UNIQUE；
- 要么伪造新的 `storage_key` → 产生指向不存在对象的幽灵记录。

**决定：** Phase 7 **不复制** Attachment metadata，Revision Draft 的 attachments 为空，
由 Phase 15（Storage）统一定义附件复制语义（预计为共享同一存储对象的独立 metadata 行，
需要该 Phase 一并调整 `storage_key` 唯一性策略）。
该决定记录于 `IMPLEMENTATION_STATUS.md` 的 Known Limitations。

### V009 / V010

仅在确实需要时创建。当前 V008 单文件即可满足 Phase 7，故 **不创建 V009/V010**。

---

## 5. 后端实现顺序

1. `V008__test_case_lifecycle.sql`
2. `entity/`：`RevisionContributorEntity`、`TestCaseReviewRecordEntity`、`ReviewRecordAction` enum
3. `repository/`：`RevisionContributorRepository`、`TestCaseReviewRecordRepository`；
   `MasterTestCaseRepository` 增加 `findByIdWithLock`（`PESSIMISTIC_WRITE`）
4. `dto/`：`ReviewRecordResponse`、`ContributorResponse`、`LifecycleActionRequest`、
   `CreateRevisionRequest`、`AddContributorRequest`；扩展 `AllowedActions`、
   `VersionSummaryResponse`（增 `revisionClosed`）、`TestCaseVersionResponse`（增 `latestReviewAction`）
5. `service/TestCaseLifecycleService` + `service/TestCaseAccessPolicy`（收敛资源级判定）
6. `controller/TestCaseLifecycleController`
7. `TestCaseDraftService` 的 Published Immutable 断言强化（Service 层显式，不依赖 DB CHECK）
8. 测试：Service 单测 → Controller RBAC 切片 → PostgreSQL IT

### 5.1 并发保护

Publish / Create Revision / Deprecate 三个入口统一：

```java
@Transactional
MasterTestCaseEntity master = masterRepository.findByIdWithLock(masterId) // PESSIMISTIC_WRITE
```

配合数据库侧：

- `uq_test_case_current_version`（partial unique index，V006 已建）
- `uq_test_case_versions_number`（master, major, minor，V006 已建）

不依赖 Java `synchronized`。

---

## 6. 前端实现顺序

1. 类型：`AllowedActions` 扩展、`ReviewRecord`、`Contributor`、版本摘要增 `revisionClosed`、版本详情增 `latestReviewAction`
2. API：`testCaseApi.ts` 增加生命周期调用，统一前缀 `/api/v1/test-cases`
3. Hooks：新增 `testCaseReviewRecords` query key；mutation 后 invalidate
   `testCases` / `testCaseDetail` / `testCaseVersions`
4. 详情页：动作条按 `AllowedActions` 渲染（不得按 Role 字符串硬编码），
   Reject 弹窗必填 comment，Return/Reject/Publish/Deprecate 走 Modal 收集 comment
5. 版本历史：显示 `revisionClosed` 与业务状态标签（`Rejected` 由
   `status === 'REVIEW' && revisionClosed === true && latestReviewAction === 'REJECT'` 推导）
6. Review History 面板（只读，append-only 展示）
7. Contributor 管理（DRAFT 且 `allowedActions.manageContributors` 时可见）
8. 测试：动作可见性 / Submit / Publish / Return / Reject / Create Revision / Deprecated 只读 / Rejected 映射 / PermissionGuard

**不得**新增任何 Decision Point / DAG / React Flow 相关代码。

---

## 7. 验证清单（必须实际执行）

```bash
cd backend && mvn clean test      # Surefire
cd backend && mvn verify          # Failsafe + Testcontainers PostgreSQL 16.6
cd frontend && npm run typecheck
cd frontend && npm run lint
cd frontend && npm run test
cd frontend && npm run build
```

Flyway 需确认从空库 V001 → V008 全部成功（由 `MigrationIT` + IT 日志断言）。

禁止把未执行的命令写成 PASS。测试数量必须与真实输出一致。

---

## 8. Phase 边界

Phase 7 完成时不包含：

```text
DecisionPoint / Transition / TransitionTarget / DAG / DagValidationService
NEXT_CASE / NEXT_CASES / PASS / FAIL / N_A / Logic Graph / React Flow
Project / ProjectCapability / GenerationRule / Generation Runtime / ProjectTestCase
Execution / Evidence Storage / Progressive Runtime / Floating / Project Logic Graph
Excel Export / Audit
```

`test_case_versions.change_request_id` 保持"仅建列、不建 FK"（Schema §31 的循环依赖处理，
Change Request 属后续 Phase）。
