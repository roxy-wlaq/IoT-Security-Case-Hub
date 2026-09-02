# Phase 7 — Test Case Lifecycle — API Contract

> Lead Agent 冻结。Backend 与 Frontend 必须共同遵守本文件。
> 所有 endpoint 位于既有 `/api/v1/test-cases` 体系下，不新增第二套资源命名。
> Phase 6 契约见 [`docs/phase6-api-contract.md`](./phase6-api-contract.md)，Phase 7 只做**追加与最小兼容扩展**。

---

## 1. Endpoint 一览

| Method | Path | 说明 | Controller 权限 |
|---|---|---|---|
| `POST` | `/api/v1/test-cases/{masterId}/draft/submit-review` | DRAFT → REVIEW | `test_case:submit_review` |
| `POST` | `/api/v1/test-cases/{masterId}/versions/{versionId}/publish` | REVIEW → PUBLISHED | `test_case:review` + `test_case:publish` |
| `POST` | `/api/v1/test-cases/{masterId}/versions/{versionId}/return` | REVIEW → DRAFT | `test_case:review` |
| `POST` | `/api/v1/test-cases/{masterId}/versions/{versionId}/reject` | REVIEW → REVIEW + closed | `test_case:review` |
| `POST` | `/api/v1/test-cases/{masterId}/versions/{versionId}/deprecate` | PUBLISHED → DEPRECATED | `test_case:deprecate` |
| `POST` | `/api/v1/test-cases/{masterId}/revisions` | 由 PUBLISHED 创建新 Revision Draft | `test_case:draft_create` |
| `GET` | `/api/v1/test-cases/{masterId}/versions/{versionId}/review-records` | 版本审核历史（append-only，只读） | 已登录 |
| `GET` | `/api/v1/test-cases/{masterId}/draft/contributors` | 当前 Draft 的 Contributor 列表 | 已登录 |
| `POST` | `/api/v1/test-cases/{masterId}/draft/contributors` | 新增 Contributor | `test_case:draft_edit` |
| `DELETE` | `/api/v1/test-cases/{masterId}/draft/contributors/{userId}` | 移除 Contributor | `test_case:draft_edit` |

所有写操作均需 CSRF（`X-XSRF-TOKEN`）。
`POST /revisions` 返回 `201`；其余写操作返回 `200`。

---

## 2. 请求 DTO

### 2.1 LifecycleActionRequest（submit-review / publish / return / reject / deprecate 共用）

```jsonc
{
  "comment": "string, optional for SUBMIT/PUBLISH/DEPRECATE; required for RETURN/REJECT"
}
```

- `comment` 最大长度 2000。
- **Return / Reject 必须提供非空 comment**，否则 400 `VALIDATION_FAILED`。

### 2.2 CreateRevisionRequest

```jsonc
{
  "sourceVersionId": "uuid, optional — 省略时使用当前 current PUBLISHED 版本",
  "changeReason": "string, optional, 最大 2000"
}
```

- 服务端**不接受** `versionMajor` / `versionMinor`。
- 新版本号 = `(major = source.major, minor = MAX(minor of all versions with the same major) + 1)`。

### 2.3 AddContributorRequest

```jsonc
{ "userId": "uuid, required" }
```

---

## 3. 响应 DTO

### 3.1 AllowedActions（Phase 6 已有，Phase 7 **扩展**）

```ts
interface AllowedActions {
  editDraft: boolean;
  createDraft: boolean;
  submitReview: boolean;       // 新增
  publish: boolean;            // 新增
  returnReview: boolean;       // 新增（字段名避免 TS 保留字 return）
  reject: boolean;             // 新增
  deprecate: boolean;          // 新增
  createRevision: boolean;     // 新增
  manageContributors: boolean; // 新增
}
```

> 兼容说明：`AllowedActions` 由 Phase 6 的 2 字段扩展为 9 字段。
> 这是 Phase 7 必需的最小兼容扩展——任务书 §20 要求前端"不得只靠 Role 字符串硬编码"，
> 必须依赖后端 `allowedActions`。Phase 6 的 `editDraft` / `createDraft` 语义与位置保持不变。

计算规则（全部在 Service 层产出，Controller 不参与）：

| 字段 | 条件 |
|---|---|
| `editDraft` | 权限 `test_case:draft_edit` ∧ 目标版本 `status=DRAFT` ∧ `revision_closed=false` ∧ (ADMIN ∨ owner ∨ contributor) |
| `createDraft` | 权限 `test_case:draft_create` |
| `submitReview` | 权限 `test_case:submit_review` ∧ 存在 DRAFT ∧ `revision_closed=false` ∧ (ADMIN ∨ owner ∨ contributor) |
| `publish` | 权限 `test_case:review` ∧ `test_case:publish` ∧ 目标版本 `status=REVIEW` ∧ `revision_closed=false` |
| `returnReview` | 权限 `test_case:review` ∧ 目标版本 `status=REVIEW` ∧ `revision_closed=false` |
| `reject` | 权限 `test_case:review` ∧ 目标版本 `status=REVIEW` ∧ `revision_closed=false` |
| `deprecate` | 权限 `test_case:deprecate` ∧ 目标版本 `status=PUBLISHED` |
| `createRevision` | 权限 `test_case:draft_create` ∧ 存在 current PUBLISHED 版本 |
| `manageContributors` | 权限 `test_case:draft_edit` ∧ 存在 DRAFT ∧ `revision_closed=false` ∧ (ADMIN ∨ owner) |

### 3.2 TestCaseVersionResponse（Phase 6 已有，Phase 7 **扩展 1 字段**）

在原有字段之后追加：

```ts
latestReviewAction: 'SUBMIT' | 'PUBLISH' | 'RETURN' | 'REJECT' | 'DEPRECATE' | null;
```

`latestReviewAction` = 该版本 `created_at` 最大（同值时取插入序最大）的 ReviewRecord 的 action；
无记录时为 `null`。用于前端推导 `Rejected` 业务标签。

### 3.3 VersionSummaryResponse（Phase 6 已有，Phase 7 **扩展 1 字段**）

在原有字段之后追加：

```ts
revisionClosed: boolean;
```

### 3.4 ReviewRecordResponse（新增）

```ts
interface ReviewRecordResponse {
  id: string;                 // uuid
  testCaseVersionId: string;  // uuid
  action: 'SUBMIT' | 'PUBLISH' | 'RETURN' | 'REJECT' | 'DEPRECATE';
  reviewerId: string;         // uuid
  reviewerName: string;       // displayName
  comment: string | null;
  createdAt: string;          // ISO-8601
}
```

按 `created_at ASC` 返回，append-only，无分页。

### 3.5 ContributorResponse（新增）

```ts
interface ContributorResponse {
  id: string;          // revision_contributors.id
  userId: string;      // uuid
  username: string;
  displayName: string;
  addedBy: string;     // uuid
  createdAt: string;   // ISO-8601
}
```

---

## 4. 生命周期语义

### 4.1 Submit Review

- **前置**：`status = DRAFT`、`revision_closed = false`、权限 + 资源级通过、必要字段完整
  （`caseName` 非空、至少 1 个 Step、`selectionMode` 合法）。
- **写入**：`status = REVIEW`；`ReviewRecord(action=SUBMIT, reviewer=当前用户, comment)`。
- **后续**：该版本不再能通过 `PUT /{masterId}/draft` 编辑（`editDraft=false`），
  除非先 Return。

### 4.2 Publish

- **前置**：`status = REVIEW`、`revision_closed = false`、ADMIN 权限。
- **写入（同一事务，PESSIMISTIC_WRITE 锁定 Master 行）**：
  1. 该 Master 现存的 current PUBLISHED 版本 → `is_current_version = false`
  2. 目标版本 → `status = PUBLISHED`、`is_current_version = true`、`published_at = now`、
     `reviewed_by = 当前用户`、`revision_closed = true`
  3. `ReviewRecord(action=PUBLISH, ...)`
- **不变**：目标版本的业务内容（steps/tools/standardMappings/字段）在本次操作中不被修改。

### 4.3 Return

- **前置**：`status = REVIEW`、`revision_closed = false`、ADMIN 权限、comment 非空。
- **写入**：`status = DRAFT`、`revision_closed = false`、`reviewed_by = 当前用户`、
  `ReviewRecord(action=RETURN, ...)`。
- **后续**：可再次编辑、可再次 Submit Review。

### 4.4 Reject

- **前置**：`status = REVIEW`、`revision_closed = false`、ADMIN 权限、comment 非空。
- **写入**：**`status` 保持 `REVIEW`**、`revision_closed = true`、`reviewed_by = 当前用户`、
  `ReviewRecord(action=REJECT, ...)`。
- **后续**：不可编辑、不可 Submit、不可 Publish、不可 Publish/Return/Reject 再次执行。
  需要继续修改必须 `POST /{masterId}/revisions` 建新 Revision。
- **不存在** `REJECTED` 状态。UI 的 "Rejected" 标签 =
  `status === 'REVIEW' && revisionClosed === true && latestReviewAction === 'REJECT'`。

### 4.5 Deprecate

- **前置**：`status = PUBLISHED`、ADMIN 权限。
- **写入**：`status = DEPRECATED`、`is_current_version = false`、`deprecated_at = now`、
  `revision_closed = true`、`ReviewRecord(action=DEPRECATE, ...)`。
- **后续**：历史仍可读、不可物理删除、不可重新编辑。

### 4.6 Create Revision

- **前置**：`sourceVersionId`（省略则取 current PUBLISHED）指向的 Version `status = PUBLISHED`、
  权限 `test_case:draft_create`。
- **写入（同一事务，PESSIMISTIC_WRITE 锁定 Master 行）**：
  1. 新 `TestCaseVersion`：`master_test_case_id` = 同一 Master、
     `based_on_version_id` = source、`status = DRAFT`、`revision_closed = false`、
     `created_by` = 当前用户、`change_reason` = 请求值
  2. 版本号按 §2.2 规则服务端计算
  3. 复制 `caseName` / `testPurpose` / `preconditions` / `selectionMode` /
     `evidenceRequired` / `evidenceRequirement` / `remarkRequirement` / `progressiveRole`
  4. 复制 `steps`（按 `sequence_no` 升序，新版本重新从 1 编号）、
     `tools`（按 `sort_order` 升序）、`standardMappings`
  5. **Master-level Tags 不复制**（Tag 属 Master，不是 Version 级）
  6. **Attachment metadata 不复制**（见实施计划 §4.2：`storage_key` 全局 UNIQUE，
     Phase 15 Storage 再定义语义）
- **不变**：source PUBLISHED 版本任何字段不被修改。

---

## 5. Published Immutable

- `PUT /api/v1/test-cases/{masterId}/draft` 命中 `status != DRAFT` 或 `revision_closed = true`
  → `409 TEST_CASE_VERSION_IMMUTABLE`。
- 生命周期 API 中没有任何一个入口会修改 PUBLISHED / DEPRECATED 版本的业务内容：
  - Deprecate 只改 `status` / `is_current_version` / `deprecated_at` / `revision_closed`；
  - Publish 只改状态与审计字段。
- ADMIN 不例外。断言在 Service 层入口显式执行，不依赖 DB CHECK 与前端按钮隐藏。

---

## 6. 错误码

| ErrorCode | HTTP | 触发 |
|---|---|---|
| `TEST_CASE_LIFECYCLE_TRANSITION_INVALID` | 422 | 源状态不满足 Action 要求 |
| `TEST_CASE_REVISION_CLOSED` | 409 | 对 `revision_closed = true` 的版本执行 Submit/Publish/Return/Reject/Edit |
| `TEST_CASE_DRAFT_REQUIRED` | 409 | Master 无 `revision_closed = false` 的 DRAFT 时执行 Submit/Publish/Return/Reject（如 Reject 已关闭唯一修订后再次提交评审） |
| `TEST_CASE_REVIEW_COMMENT_REQUIRED` | 400 | Return / Reject 未提供 comment |
| `TEST_CASE_DRAFT_INCOMPLETE` | 422 | Submit Review 时必要字段缺失（无 caseName 或无 Step） |
| `TEST_CASE_LIFECYCLE_FORBIDDEN` | 403 | 资源级权限不通过（非 owner / 非 contributor / 非 ADMIN） |
| `TEST_CASE_REVISION_SOURCE_INVALID` | 422 | Create Revision 的 source 版本不存在或不是 PUBLISHED |
| `TEST_CASE_CONTRIBUTOR_INVALID` | 400 | 贡献者用户不存在 / 未启用 / 重复添加 |
| `TEST_CASE_VERSION_IMMUTABLE` | 409 | Published Immutable（Phase 6 已有，Phase 7 复用） |

其余沿用 Phase 6 的 `TEST_CASE_NOT_FOUND` / `TEST_CASE_VERSION_NOT_FOUND` 等。
不可见资源一律 404，不返回 403 以免泄漏存在性。

---

## 7. 前端 Query Key

```ts
testCaseReviewRecords  = ['testCaseReviewRecords', masterId, versionId]
testCaseContributors   = ['testCaseContributors', masterId]
```

生命周期 mutation 成功后必须 invalidate：
`testCases` / `testCaseDetail(masterId)` / `testCaseVersions(masterId)` /
`testCaseReviewRecords(masterId, versionId)`。
