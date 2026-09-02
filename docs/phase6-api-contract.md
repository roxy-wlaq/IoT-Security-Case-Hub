# Phase 6 API Contract（代码最终实现）

本文档记录当前 `TestCaseController` 已实现并由前端使用的接口。它是代码实现的对照契约，不新增未实现的生命周期能力。

## 统一约定

- Base path：`/api/v1/test-cases`
- 需要认证；可见版本由当前用户角色和资源权限决定。普通用户可读取已发布版本以及自己创建的 Draft，管理员可读取所有已存在版本。
- 时间字段使用 ISO-8601 Instant 字符串；ID 使用 UUID 字符串。
- 校验失败返回项目统一错误响应；分页 `page` 从 0 开始。
- 本阶段只包含 Draft 创建、Draft 更新、查询、版本历史和版本详情。发布、审核、驳回、弃用、决策点、DAG、项目和生成能力不属于当前 Controller 表面。

## 接口目录

### 查询测试用例库

`GET /api/v1/test-cases`

Query 参数：

| 参数 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `q` | string | — | 匹配编码、版本名称、测试目的、步骤内容、标签名称或工具名称 |
| `categoryId` | UUID | — | 分类过滤 |
| `tagIds` | UUID[] | — | 标签过滤 |
| `toolIds` | UUID[] | — | 工具过滤 |
| `standardTaskTypeIds` | UUID[] | — | 标准任务类型过滤 |
| `status` | string | — | `DRAFT`、`REVIEW`、`PUBLISHED` 或 `DEPRECATED` |
| `page` | integer | `0` | 页码，小于 0 时按 0 处理 |
| `size` | integer | `20` | 每页 1–100 条 |
| `sort` | string | `updatedAt,desc` | 支持 `updatedAt`、`createdAt`、`caseCode`、`caseName`，方向为 `asc` 或 `desc` |

`caseName` 按版本名称排序。排序请求示例：`sort=caseName,asc`、`sort=caseName,desc`。PostgreSQL 集成测试实际校验两种方向的返回顺序。

### List Version Selection Semantics

列表中的每一个 Master 在当前请求条件下先确定且只确定一个 **List Version**；该 Version 就是最终用于构造 `TestCaseSummaryResponse` 的版本。列表的版本可见性规则为：普通用户可见已发布版本和本人创建的 Draft，管理员可见所有版本。

- `q` 的语义严格为 `MasterMatch OR VersionMatch`。`MasterMatch` 匹配 Master 的 `caseCode` 或标签名称；`VersionMatch` 匹配某一个 Version 的版本名称、测试目的、步骤标题/内容或工具名称。
- 当 `MasterMatch` 成立时，候选/选中的 Version **不要求**自身再匹配 `q`；例如 Master 编码匹配 `q=BLE` 时，`status=DRAFT` 可以选中名称不含 `BLE` 的 Draft。
- `status`、`toolIds`、`standardTaskTypeIds` 始终与 `VersionMatch` 一样作用于同一个候选 Version 行，不能由不同 Version 分别满足。若 `MasterMatch` 不成立，则同一个 Version 必须同时满足 `VersionMatch` 及这些已提供的版本级过滤条件；没有这种候选 Version 的 Master 不进入结果。
- 对每个 Master，在上述可见性与请求条件形成的候选 Version 集合中优先选择当前已发布 Version；若候选集合中不存在当前已发布 Version，则选择版本号最高的候选 Version，并以 Version ID 确定性打破同版本号并列。
- 选定的同一个 List Version 同时提供 Summary 的 `caseName`、`status`、`versionLabel`、`updatedAt`；版本侧排序也使用这一个 List Version 的值，包括 `sort=caseName`、`sort=updatedAt` 和 `sort=createdAt`。排序不会使用所有 Version 的 `MIN(caseName)` 或其他聚合值替代展示版本。
- `categoryId`、`tagIds`、Master 的 `caseCode` 与 Master 标签侧的 `q` 属于 Master 级条件；它们只决定 Master 是否候选，不改变上述 List Version 选择语义。

响应 `200`：

```json
{
  "content": [
    {
      "id": "master-uuid",
      "caseCode": "BLE-001",
      "caseName": "Pairing",
      "categoryId": "category-uuid",
      "categoryName": "Bluetooth",
      "status": "DRAFT",
      "versionMajor": 1,
      "versionMinor": 0,
      "versionLabel": "1.0",
      "tags": [{ "id": "tag-uuid", "code": "ble", "name": "BLE" }],
      "enabled": true,
      "updatedAt": "2026-09-02T00:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

### 创建 Draft

`POST /api/v1/test-cases`

需要权限：`test_case:draft_create`。成功返回 `201` 和完整的 `TestCaseDetailResponse`。请求体：

```json
{
  "caseCode": "BLE-001",
  "categoryId": "category-uuid",
  "caseName": "Pairing",
  "testPurpose": "Verify pairing",
  "preconditions": "Powered device",
  "selectionMode": "SINGLE",
  "evidenceRequired": true,
  "evidenceRequirement": "Logs",
  "remarkRequirement": "None",
  "progressiveRole": "ENTRY",
  "steps": [{ "title": "Connect", "content": "Connect device" }],
  "tagIds": ["tag-uuid"],
  "toolIds": ["tool-uuid"],
  "standardMappings": [{ "standardTaskTypeId": "standard-task-uuid", "mappingNote": "baseline" }]
}
```

`caseCode`、`categoryId`、`caseName`、`selectionMode` 必填；`selectionMode` 为 `SINGLE` 或 `MULTIPLE`；`progressiveRole` 可为 `ENTRY`、`NORMAL` 或 `null`。新 Draft 引用的分类、标签、工具、标准任务类型必须存在且启用。

### 查询测试用例详情

`GET /api/v1/test-cases/{masterId}`

响应 `200` 的 `TestCaseDetailResponse` 字段：

- Master：`id`、`caseCode`、`categoryId`、`categoryName`、`createdBy`、`enabled`、`createdAt`、`updatedAt`、`tags`
- 版本：`currentVersion`、`draftVersion`、`visibleVersion`、`versions`
- 权限：`allowedActions.editDraft`、`allowedActions.createDraft`

`visibleVersion` 为当前用户可以看到的版本。版本对象字段为 `id`、`masterTestCaseId`、`versionLabel`、`versionMajor`、`versionMinor`、`status`、`isCurrentVersion`、`caseName`、`testPurpose`、`preconditions`、`selectionMode`、`evidenceRequired`、`evidenceRequirement`、`remarkRequirement`、`progressiveRole`、`basedOnVersionId`、`changeReason`、`createdBy`、`reviewedBy`、`publishedAt`、`deprecatedAt`、`revisionClosed`、`steps`、`tools`、`standardMappings`、`attachments`、`createdAt`、`updatedAt`。

其中：

- `steps` 元素为 `id`、`sequenceNo`、`title`、`content`。
- `tools` 元素为 `id`、`code`、`name`。
- `standardMappings` 元素为 `standardTaskTypeId`、`standardCode`、`standardName`、`mappingNote`。
- `attachments` 元素为 `id`、`originalFilename`、`fileSize`、`contentType`、`description`、`uploadedBy`、`createdAt`。

### 更新 Draft

`PUT /api/v1/test-cases/{masterId}/draft`

需要权限：`test_case:draft_edit`。仅允许 Draft 创建者或管理员编辑 Draft；成功返回 `200` 和完整的 `TestCaseDetailResponse`。

请求体与创建 Draft 相同，但不包含 `caseCode`、`categoryId`：

```json
{
  "caseName": "Pairing - updated",
  "testPurpose": "Verify pairing after update",
  "preconditions": "Powered device",
  "selectionMode": "SINGLE",
  "evidenceRequired": true,
  "evidenceRequirement": "Logs",
  "remarkRequirement": "None",
  "progressiveRole": "NORMAL",
  "steps": [{ "title": "Connect", "content": "Connect device and verify" }],
  "tagIds": ["tag-uuid"],
  "toolIds": ["tool-uuid"],
  "standardMappings": [{ "standardTaskTypeId": "standard-task-uuid", "mappingNote": "preserved note" }]
}
```

更新会替换 Draft 的步骤、标签、工具和标准映射集合；未改变的 `mappingNote` 必须由客户端回传，服务端按请求体保存。

### 查询版本历史

`GET /api/v1/test-cases/{masterId}/versions`

响应 `200` 为版本摘要数组，每项字段为：`id`、`versionLabel`、`versionMajor`、`versionMinor`、`status`、`isCurrentVersion`、`changeReason`、`createdBy`、`publishedAt`、`createdAt`。结果按版本号从新到旧返回，并遵循当前用户的可见性规则。

### 查询版本详情

`GET /api/v1/test-cases/{masterId}/versions/{versionId}`

响应 `200` 为单个 `TestCaseVersionResponse`，结构与详情中的版本对象相同。版本不属于指定 Master 或当前用户不可见时返回统一资源不存在错误。

## 错误与边界

- 不支持的排序字段或方向返回 `TEST_CASE_SORT_FIELD_INVALID`。
- `size` 不在 1–100 范围返回校验错误。
- 不支持的 `status` 返回校验错误。
- 新建或更新 Draft 引用不存在或已禁用的分类、标签、工具、标准任务类型时返回校验错误；已有历史版本读取不因字典禁用而消失。
- 当前数据库约束保证 Master 删除时若仍有版本会被拒绝；`caseCode` 大小写不敏感唯一，版本号和关系表组合键唯一。

## 实现边界

本契约以 `backend/src/main/java/com/company/casehub/testcase/controller/TestCaseController.java`、对应 DTO 以及 `frontend/src/features/testcase/api/testCaseApi.ts` 为准。下一阶段数据库迁移从 V008 开始；本阶段未引入额外接口。
