# IoT-Security-Case-Hub
## Backend Architecture V1.0

> 基于：
>
> - `IoT-Security-Case-Hub_System-Design_V0.6.md`
> - `IoT-Security-Case-Hub_Technical-Architecture_V1.0.md`
> - `IoT-Security-Case-Hub_Data-Model_V1.0.md`
> - `IoT-Security-Case-Hub_Database-Schema_V1.0.md`
>
> 本文档冻结 Spring Boot 后端代码架构，包括：
>
> - Package Structure
> - Domain Module Boundary
> - Entity
> - Repository
> - Service
> - Controller
> - DTO
> - Mapper
> - Transaction Boundary
> - Exception Model
> - RBAC Enforcement
> - GenerationEngine
> - ProgressiveRuntimeService
> - CapabilityEngine
> - DAG Validation
> - StorageService
> - Audit
>
> 后续 API Design 与代码实现均以本文档为基准。

---

# 1. 后端总体风格

V1 正式采用：

```text
Modular Monolith
+
按业务模块分包
+
模块内轻量分层
```

不采用：

```text
全局 controller / service / repository 大目录
```

也不采用：

```text
重型 DDD
Hexagonal Architecture
Clean Architecture 全套抽象
Microservices
```

目标：

```text
结构清楚
模块独立
事务清晰
易维护
不过度设计
```

---

# 2. Backend Root Package

推荐根包：

```text
com.company.casehub
```

正式代码初始化时可替换成公司真实域名。

目录：

```text
backend/
└─ src/main/java/com/company/casehub/
```

---

# 3. 顶级模块

推荐：

```text
casehub/
├─ auth/
├─ user/
├─ project/
├─ capability/
├─ testcase/
├─ tool/
├─ generation/
├─ execution/
├─ evidence/
├─ changerequest/
├─ export/
├─ audit/
├─ storage/
└─ common/
```

每个模块必须有明确职责。

禁止：

```text
跨模块随意直接操作对方 Repository
```

跨模块业务优先通过 Service / Facade 调用。

---

# 4. 模块内标准结构

典型模块：

```text
project/
├─ controller/
├─ dto/
├─ entity/
├─ repository/
├─ service/
├─ mapper/
├─ exception/
└─ model/
```

不是所有模块都必须机械拥有全部目录。

如果模块不需要：

```text
mapper
exception
```

可以省略。

---

# 5. Controller 原则

Controller 只负责：

```text
HTTP Request
参数解析
Bean Validation
权限入口校验
调用 Service
返回 DTO
```

禁止在 Controller 中：

```text
写业务流程
直接操作 EntityManager
直接操作 Repository
写 Progressive 逻辑
写 Generation Rule 逻辑
拼复杂 SQL
```

标准调用：

```text
Controller
↓
Service / Application Service
↓
Repository / Domain Engine
↓
Database
```

---

# 6. DTO 原则

API 不直接暴露 JPA Entity。

统一：

```text
Request DTO
Response DTO
```

例如：

```text
CreateProjectRequest
UpdateProjectRequest
ProjectDetailResponse
ProjectListItemResponse
```

禁止：

```text
Controller 返回 Entity
```

原因：

```text
避免 Lazy Loading 问题
避免字段泄漏
避免 Entity 与 API 强耦合
便于版本演进
```

---

# 7. Mapper

V1 推荐：

```text
MapStruct
```

用于：

```text
Entity → Response DTO
Request DTO → Command / Entity
```

复杂业务转换仍然手写。

不要为了 MapStruct 强行映射复杂关系图。

---

# 8. Entity 原则

Entity 只表达：

```text
Persistence State
Relationship
Simple State Method
```

Entity 不承载大型跨模块业务流程。

例如允许：

```text
project.archive()
version.markPublished()
```

不允许：

```text
entity.completeAndGenerateNextCases()
```

Progressive Runtime 必须放 Service。

---

# 9. JPA Entity 基础类

建议：

```text
BaseEntity
```

包含：

```text
UUID id
Instant createdAt
Instant updatedAt
```

只放真正通用字段。

不要把：

```text
status
enabled
createdBy
```

全部塞入 BaseEntity。

---

# 10. Repository 原则

Repository 只负责：

```text
Persistence
Query
Lock / Existence Check
```

推荐：

```text
Spring Data JPA Repository
```

复杂查询可以使用：

```text
JPQL
Native Query
Projection
```

禁止：

```text
Repository 承担业务判断
```

---

# 11. Service 类型

后端区分两类 Service。

## 11.1 Domain / Module Service

处理模块内部业务。

例如：

```text
ProjectService
TestCaseVersionService
CapabilityService
EvidenceService
```

## 11.2 Cross-Module Orchestrator

处理跨模块流程。

例如：

```text
GenerationEngine
ProgressiveRuntimeService
CapabilityEngine
ProjectTestPlanService
```

---

# 12. auth 模块

目录：

```text
auth/
├─ controller/
│  └─ AuthController
├─ dto/
│  ├─ LoginRequest
│  ├─ LoginResponse
│  └─ CurrentUserResponse
├─ service/
│  ├─ AuthenticationService
│  └─ CurrentUserService
├─ security/
│  ├─ SecurityConfig
│  ├─ AuthenticationFilter
│  ├─ AuthenticationEntryPoint
│  ├─ AccessDeniedHandler
│  └─ UserPrincipal
└─ exception/
```

---

# 13. AuthenticationService

职责：

```text
登录
密码验证
用户启用状态验证
建立认证状态
登出
```

不负责：

```text
Project 权限
Test Case 权限
```

---

# 14. CurrentUserService

统一提供：

```text
currentUserId()
currentUser()
hasRole()
```

业务代码不要到处直接读取：

```text
SecurityContextHolder
```

---

# 15. user 模块

目录：

```text
user/
├─ controller/
├─ dto/
├─ entity/
│  ├─ UserEntity
│  ├─ RoleEntity
│  ├─ PermissionEntity
│  ├─ UserRoleEntity
│  └─ RolePermissionEntity
├─ repository/
└─ service/
```

核心 Service：

```text
UserService
RoleService
PermissionService
```

---

# 16. UserService

职责：

```text
创建用户
修改用户
启用 / 禁用
修改显示名
重置密码
查询用户
```

---

# 17. RoleService

职责：

```text
分配 Role
移除 Role
查询用户 Role
```

Role 修改必须写：

```text
AuditLog
```

---

# 18. project 模块

目录建议：

```text
project/
├─ controller/
│  ├─ ProjectController
│  └─ ProjectTestPlanController
├─ dto/
│  ├─ CreateProjectRequest
│  ├─ UpdateProjectRequest
│  ├─ ProjectDetailResponse
│  ├─ ProjectListItemResponse
│  └─ ProjectTestCaseListResponse
├─ entity/
│  ├─ ProjectEntity
│  ├─ ProjectStandardEntity
│  ├─ ProjectCoordinatorEntity
│  ├─ ProjectTestCaseEntity
│  ├─ ProjectTestCaseAssigneeEntity
│  └─ ProjectTestCaseSourceEntity
├─ repository/
└─ service/
```

---

# 19. ProjectService

职责：

```text
Create Project
Update Project
Change Project Status
Archive Project
Complete Project
Project Standard Update
Coordinator 管理
```

不负责：

```text
Generation Rule Evaluation
Progressive Runtime
```

---

# 20. ProjectCompletionService

建议单独：

```text
ProjectCompletionService
```

职责：

```text
判断所有未 Removed ProjectTestCase 是否 Completed
标记 Project Completed
```

防止 ProjectService 变得过大。

---

# 21. ProjectTestPlanService

核心跨模块 Service。

职责：

```text
Add Recommended Test Case
Ignore Recommendation
Manual Add Master Test Case
Add Custom Test Case
Remove Project Test Case
Restore Project Test Case
调整 Assignees
Test Case Version Upgrade
```

---

# 22. ProjectTestPlanService 不负责

不负责：

```text
执行 Decision Point
自动 NEXT_CASE
Generation Rule 匹配
```

这些分别属于：

```text
ProgressiveRuntimeService
GenerationEngine
```

---

# 23. capability 模块

目录：

```text
capability/
├─ controller/
├─ dto/
├─ entity/
│  ├─ CapabilityEntity
│  ├─ ProjectCapabilityEntity
│  └─ CapabilityUpdateRequestEntity
├─ repository/
├─ service/
│  ├─ CapabilityService
│  ├─ ProjectCapabilityService
│  ├─ CapabilityEngine
│  └─ CapabilityUpdateRequestService
└─ model/
```

---

# 24. CapabilityService

负责 Capability Library：

```text
新增能力
修改能力
停用能力
调整 Parent
读取能力树
```

必须验证：

```text
Capability Tree 无环
```

---

# 25. ProjectCapabilityService

负责：

```text
读取 Project Capability
Coordinator 手工填写
更新 Capability
查询 Capability 来源
```

---

# 26. CapabilityEngine

核心职责：

```text
Parent = NO
Child Rule Evaluation 不适用

Parent = YES
Child 不自动 YES

Child = YES
向上 Derived Parent = YES
```

对外主要接口示例：

```text
applyCapabilityValue(...)
resolveEffectiveValue(...)
recalculateDerivedParents(...)
```

---

# 27. Effective Capability

Generation Rule 不应直接简单读取：

```text
project_capabilities.value
```

应通过：

```text
CapabilityEngine
```

获取 Effective Value。

原因：

```text
Parent NO
Derived YES
Row 不存在视为 UNKNOWN
```

都需要统一处理。

---

# 28. CapabilityUpdateRequestService

职责：

```text
Submit
Approve
Reject
```

Approve Transaction：

```text
1. 更新 Request
2. 更新 Project Capability
3. CapabilityEngine 推导
4. 创建新的 GenerationRun
```

注意：

```text
只产生 Recommendation
不自动改变 Project Test Plan
```

---

# 29. testcase 模块

建议目录：

```text
testcase/
├─ controller/
│  ├─ TestCaseController
│  ├─ TestCaseDraftController
│  └─ TestCaseGraphController
├─ dto/
├─ entity/
│  ├─ MasterTestCaseEntity
│  ├─ TestCaseVersionEntity
│  ├─ TestStepEntity
│  ├─ CategoryEntity
│  ├─ TagEntity
│  ├─ TestCaseTagEntity
│  ├─ TestCaseToolEntity
│  ├─ TestCaseStandardMappingEntity
│  ├─ DecisionPointEntity
│  ├─ TransitionEntity
│  └─ TransitionTargetEntity
├─ repository/
├─ service/
│  ├─ TestCaseQueryService
│  ├─ TestCaseDraftService
│  ├─ TestCaseVersionService
│  ├─ TestCasePublishService
│  └─ DagValidationService
└─ mapper/
```

---

# 30. TestCaseQueryService

专门用于查询：

```text
Test Case Library
Search
Detail
History
Deprecated
Related Test Case
Logic Graph
```

读服务与写服务分开，有利于控制复杂度。

---

# 31. TestCaseDraftService

负责：

```text
Create Master Test Case
Create First Draft
Create Revision Draft
Edit Draft
Edit Steps
Edit Tools
Edit Decision Points
Edit Attachments
```

只能操作：

```text
DRAFT
```

---

# 32. TestCaseVersionService

负责：

```text
Version Number
basedOnVersion
Current Version Lookup
Version Upgrade Information
Version Diff Data
```

---

# 33. TestCasePublishService

负责：

```text
Submit Review
Return
Reject
Publish
Deprecated
```

Publish 必须是事务。

---

# 34. Publish Transaction

Publish 流程：

```text
1. 检查当前 Version = REVIEW
2. DagValidationService.validate()
3. 检查 Decision Point / Transition 完整性
4. 旧 Current Version → isCurrentVersion=false
5. 新 Version → PUBLISHED
6. isCurrentVersion=true
7. publishedAt
8. 写 Review Record
9. 写 AuditLog
```

全部：

```text
@Transactional
```

---

# 35. DagValidationService

职责：

```text
Transition Target 数量校验
NEXT_CASE = 1 Target
NEXT_CASES >= 1 Target
PASS/FAIL/N_A = 0 Target
Cycle Detection
Target Master Test Case 可用性
```

---

# 36. Cycle Detection

建议 Service 使用：

```text
DFS
```

或：

```text
PostgreSQL Recursive CTE
```

V1 推荐：

```text
Java DFS
```

原因：

```text
发布频率低
图规模小
实现可测试
逻辑清楚
```

---

# 37. tool 模块

目录：

```text
tool/
├─ controller/
├─ dto/
├─ entity/
├─ repository/
└─ service/
```

核心：

```text
ToolService
```

负责：

```text
CRUD
Enable / Disable
Attachment
Search
```

---

# 38. generation 模块

目录：

```text
generation/
├─ controller/
│  └─ GenerationController
├─ dto/
├─ entity/
│  ├─ GenerationRuleEntity
│  ├─ GenerationConditionGroupEntity
│  ├─ GenerationConditionEntity
│  ├─ GenerationRuleOutputEntity
│  ├─ GenerationRunEntity
│  ├─ GenerationRecommendationEntity
│  └─ GenerationRecommendationRuleEntity
├─ repository/
├─ service/
│  ├─ GenerationRuleService
│  ├─ GenerationEngine
│  ├─ GenerationRecommendationService
│  └─ GenerationRunService
└─ model/
```

---

# 39. GenerationRuleService

负责：

```text
Rule CRUD
Enabled / Disabled
Condition Group 编辑
Output Test Case 编辑
Rule Validation
```

---

# 40. GenerationEngine

这是 V1 核心 Engine。

职责：

```text
读取 Project Standard / Task Type
读取 Effective Capability
读取 Enabled Rules
按 Mode 过滤
执行 Condition Group
合并 Rule Output
Test Case 去重
解析 Current Published Version
生成 Recommended Because
应用 Project Ignore Preference
生成 GenerationRecommendation
```

---

# 41. GenerationEngine 输入

建议定义内部模型：

```text
GenerationContext
```

包含：

```text
projectId
generationMode
standards
capabilities
triggerType
executedBy
```

---

# 42. GenerationEngine 输出

返回：

```text
GenerationResult
```

例如：

```text
GenerationRunId
Recommendations[]
MatchedRules[]
IgnoredCases[]
ConfigurationErrors[]
```

---

# 43. Rule Evaluation

不要在数据库 SQL 中实现完整 Rule Engine。

推荐：

```text
Repository
↓
读取结构化 Rule
↓
Java GenerationEngine 计算
```

原因：

```text
AND / OR 逻辑更好测试
UNKNOWN 规则清楚
未来扩展容易
```

---

# 44. Rule Condition Evaluator

建议单独：

```text
GenerationConditionEvaluator
```

支持：

```text
EQ_YES
EQ_NO
EQ_UNKNOWN
NE_NO
NE_YES
PRESENT
```

---

# 45. Condition Group Evaluator

建议：

```text
GenerationGroupEvaluator
```

实现：

```text
AND
OR
Root + One Child Level
```

不要把所有 Evaluation 写进一个超大方法。

---

# 46. generation Recommendation Service

负责：

```text
List Recommendations
Add
Ignore
Restore Ignore
Recommended Because
```

Add 时调用：

```text
ProjectTestPlanService
```

---

# 47. execution 模块

这是系统运行时最重要模块之一。

建议：

```text
execution/
├─ controller/
│  ├─ ExecutionController
│  └─ MyTestController
├─ dto/
├─ entity/
│  ├─ ProjectDecisionSelectionEntity
│  ├─ ProjectBranchOutcomeEntity
│  └─ ProjectTestCaseTriggerEntity
├─ repository/
├─ service/
│  ├─ ExecutionService
│  ├─ ProgressiveRuntimeService
│  ├─ RelationStateService
│  └─ MyTestQueryService
└─ model/
```

---

# 48. ExecutionService

负责普通执行状态：

```text
Start Test Case
Update Execution Data
Complete Test Case
Reopen Completed
```

真正 Complete 时调用：

```text
ProgressiveRuntimeService
```

---

# 49. Complete Test Case 输入

建议：

```text
CompleteProjectTestCaseCommand
```

包含：

```text
projectTestCaseId
selectedDecisionPointIds[]
```

不额外要求：

```text
Result
```

---

# 50. Complete 前校验

ExecutionService 检查：

```text
当前用户是 Assignee
ProjectTestCase removed = false
Selection Mode 满足
Evidence Required 满足
Decision Point 属于绑定 Version / Custom Case
```

不增加其他强制校验。

---

# 51. ProgressiveRuntimeService

这是整个 Progressive Runtime 核心。

职责：

```text
读取 Selected Decision Points
解析 Transition
生成 Branch Outcome
处理 PASS / FAIL / N_A
解析 NEXT_CASE / NEXT_CASES
创建 / 复用 Target ProjectTestCase
添加 Source PROGRESSIVE
添加 Trigger
合并 Assignees
更新 Relation Status
处理 Completed Target
```

---

# 52. ProgressiveRuntimeService Transaction

必须：

```text
@Transactional
```

完整流程：

```text
Complete
↓
Save Selection
↓
Save Outcome
↓
Resolve Transition
↓
Resolve Target
↓
Create / Reuse PTC
↓
Add Trigger
↓
Union Assignees
↓
Update Relation
↓
Mark Completed
```

任何错误：

```text
Rollback
```

---

# 53. TargetProjectTestCaseResolver

建议从 ProgressiveRuntimeService 拆出：

```text
TargetProjectTestCaseResolver
```

职责：

```text
按 Project + Master Test Case 查询现有实例

存在：
复用

不存在：
解析 Current Published Version
创建新 PTC

Custom Target：
解析同 Project Custom Case
```

---

# 54. Target Version 解析

当 Master Target 尚未存在 ProjectTestCase：

```text
MasterTestCase
↓
Current Published Version
```

如果不存在：

```text
throw ProgressiveConfigurationException
```

不能使用：

```text
DRAFT
REVIEW
DEPRECATED
```

---

# 55. AssigneeUnionService

可以单独：

```text
AssigneeUnionService
```

或作为 ProgressiveRuntimeService 内部组件。

职责：

```text
sourceAssignees ∪ targetAssignees
```

批量写入并利用 Unique Constraint 去重。

---

# 56. RelationStateService

职责：

```text
Root → CONNECTED
有 Active Incoming Trigger → CONNECTED
无 Active Incoming Trigger → FLOATING
```

所有 Trigger 修改后必须调用。

---

# 57. 修改 Completed Test Case

ExecutionService 提供：

```text
UpdateCompletedDecisionService
```

或者由 ProgressiveRuntimeService 提供修改入口。

用户确认：

```text
原节点
其他节点
增加节点
不使用节点
```

后，再执行关系调整。

---

# 58. Relation Update Strategy

具体内部命令建议：

```text
KEEP_ORIGINAL_TARGET
USE_EXISTING_TARGET
ADD_TARGET
DETACH_TARGET
```

对应 UI：

```text
原节点
其他节点
增加节点
不使用节点
```

---

# 59. DETACH_TARGET

处理：

```text
Trigger.active = false
```

不删除 Target ProjectTestCase。

然后：

```text
RelationStateService.recalculate(target)
```

可能成为：

```text
FLOATING
```

---

# 60. MyTestQueryService

专门负责 Tester 任务查询。

支持：

```text
按 Project 分组
我的用例
项目全部用例
全部
未开始
进行中
已完成
游离
NEW
Triggered By
协作者
```

不要把这些复杂查询塞进 ExecutionService。

---

# 61. NEW 状态

使用：

```text
ProjectTestCaseAssignee.firstViewedAt
```

MyTestQueryService：

```text
firstViewedAt == null
→ NEW
```

打开详情后：

```text
markViewed()
```

只更新当前 User 的 Assignment Row。

---

# 62. evidence 模块

目录：

```text
evidence/
├─ controller/
├─ dto/
├─ entity/
├─ repository/
└─ service/
```

核心：

```text
EvidenceService
```

---

# 63. EvidenceService

职责：

```text
Upload
Download
Delete
List
Metadata
Permission Check
StorageService 调用
```

---

# 64. Evidence Upload Transaction

文件与数据库不是单一 ACID Transaction。

建议：

```text
1. StorageService.save temp
2. 计算 SHA-256
3. 保存 Evidence DB
4. finalize storage
```

失败时：

```text
清理临时文件
```

---

# 65. storage 模块

目录：

```text
storage/
├─ StorageService
├─ LocalStorageService
├─ StorageObject
├─ StorageProperties
└─ StorageException
```

接口：

```text
save()
read()
delete()
exists()
```

---

# 66. Storage Key

StorageService 自己生成：

```text
evidence/{projectId}/{ptcId}/{uuid}
```

禁止让 Controller 传真实路径。

---

# 67. changerequest 模块

建议：

```text
changerequest/
├─ controller/
├─ dto/
├─ entity/
├─ repository/
└─ service/
```

核心：

```text
TestCaseChangeRequestService
RevisionContributorService
ReviewService
```

Capability Update Request 仍放：

```text
capability
```

因为它的结果直接影响 Capability Engine。

---

# 68. TestCaseChangeRequestService

负责：

```text
Submit
Approve
Reject
Create Revision Draft
```

Approve 后：

```text
创建 TestCaseVersion DRAFT
basedOnVersion
关联 ChangeRequest
问题提出人加入 Contributor
```

---

# 69. ReviewService

负责：

```text
Submit Review
Admin Publish
Return
Reject
```

Publish 最终调用：

```text
TestCasePublishService
```

---

# 70. export 模块

目录：

```text
export/
├─ controller/
├─ dto/
└─ service/
   └─ ExcelExportService
```

V1 同步生成。

---

# 71. ExcelExportService

输出：

```text
Project Summary
Test Cases
Evidence Index
```

使用：

```text
Apache POI
```

---

# 72. audit 模块

核心：

```text
AuditService
AuditLogRepository
```

建议提供统一方法：

```text
record(
  action,
  targetType,
  targetId,
  summary,
  details
)
```

---

# 73. Audit 触发点

必须记录：

```text
Login
Role Change
Project Create
Project Archive
Capability Library Update
Generation Rule Update
Test Case Publish
Test Case Deprecated
```

不记录：

```text
Tester 每次输入
每次 Note 修改
每次 Evidence Metadata 修改
```

除非后续有合规需求。

---

# 74. common 模块

允许：

```text
common/
├─ exception/
├─ response/
├─ validation/
├─ util/
└─ config/
```

禁止：

```text
把项目业务 Service 放 common
```

---

# 75. 全局异常体系

建议基础异常：

```text
CaseHubException
```

子类：

```text
ResourceNotFoundException
ForbiddenOperationException
BusinessRuleException
ConflictException
ValidationException
ProgressiveConfigurationException
GenerationConfigurationException
StorageException
```

---

# 76. HTTP Error Mapping

统一：

```text
400 BAD_REQUEST
→ 请求格式 / Bean Validation

401 UNAUTHORIZED
→ 未认证

403 FORBIDDEN
→ 无权限

404 NOT_FOUND
→ 资源不存在

409 CONFLICT
→ 唯一约束 / 状态冲突

422 UNPROCESSABLE_ENTITY
→ 业务规则不允许

500 INTERNAL_SERVER_ERROR
→ 非预期错误
```

---

# 77. 错误响应结构

统一：

```json
{
  "code": "PROJECT_TEST_CASE_NOT_ASSIGNED",
  "message": "Current user is not assigned to this test case.",
  "traceId": "...",
  "details": {}
}
```

字段：

```text
code
message
traceId
details
```

生产环境不返回：

```text
Java Stack Trace
SQL
内部类名
```

---

# 78. GlobalExceptionHandler

使用：

```text
@RestControllerAdvice
```

统一处理异常。

---

# 79. Validation

分两层：

## Request Validation

```text
Jakarta Bean Validation
@NotNull
@NotBlank
@Size
```

## Business Validation

Service：

```text
状态是否合法
版本是否可修改
是否 Assigned
是否满足 Selection Mode
DAG 是否有环
```

---

# 80. Authorization 架构

后端权限不能只靠：

```text
@PreAuthorize("hasRole(...)")
```

因为存在：

```text
Project-level Permission
Assignee-level Permission
Draft Contributor Permission
```

因此使用两层：

```text
Role Permission
+
Resource Authorization
```

---

# 81. Role Permission

通过：

```text
Spring Security
Permission Code
```

例如：

```text
project:create
test_case:publish
generation_rule:manage
```

---

# 82. ResourceAuthorizationService

建议统一服务：

```text
ResourceAuthorizationService
```

核心方法示例：

```text
requireProjectCoordinator(projectId)

requireProjectMember(projectId)

requireAssignee(projectTestCaseId)

requireDraftEditor(testCaseVersionId)

requireAdmin()
```

业务 Service 在写操作前调用。

---

# 83. Tester Execution Authorization

执行 ProjectTestCase：

```text
Current User ∈ Assignees
```

才允许：

```text
修改
Complete
Evidence
Notes
Decision Point
```

项目全部用例中的未分配 Case：

```text
只读
```

---

# 84. Draft Authorization

Draft Version 可编辑：

```text
Admin
Test Coordinator Owner
Revision Contributor
```

Published：

```text
任何人都不能直接修改
```

---

# 85. Transaction 设计总则

@Transactional 放在：

```text
Service
```

不放：

```text
Controller
Repository
Entity
```

---

# 86. 关键事务

必须明确事务边界：

```text
Create Project

Capability Update Approve

Generation Run

Add Recommendation

Test Case Publish

Complete Project Test Case

Modify Completed Decision

Remove / Restore PTC

Upgrade Test Case Version
```

---

# 87. Read-Only Transaction

查询类 Service 推荐：

```text
@Transactional(readOnly = true)
```

例如：

```text
TestCaseQueryService
MyTestQueryService
ProjectQueryService
```

---

# 88. 唯一约束冲突处理

例如：

```text
两个请求同时创建同一个 NEXT_CASE
```

处理策略：

```text
INSERT
↓
Unique Constraint Violation
↓
捕获 DataIntegrityViolationException
↓
重新查询现有 ProjectTestCase
↓
继续 Trigger / Assignee Union
```

不能返回 500。

---

# 89. 不做复杂编辑锁

按产品决定：

```text
不实现 Optimistic Lock UI
不实现实时 Lock
不实现 WebSocket 协同
```

V1 使用普通 Save。

关键一致性依靠：

```text
Database Unique
Transaction
```

---

# 90. Query 与 Command 分离原则

不实现完整 CQRS。

但代码组织上建议：

```text
Query Service
Command / Write Service
```

适当分离。

例如：

```text
TestCaseQueryService
TestCaseDraftService
```

这只是职责分离，不是 CQRS 基础设施。

---

# 91. Java Enum

所有状态统一 Java Enum。

例如：

```text
ProjectStatus
GenerationMode
CapabilityValue
CapabilitySource
TestCaseVersionStatus
SelectionMode
ProgressiveRole
TransitionType
ExecutionStatus
RelationStatus
ProjectTestCaseSourceType
GenerationRuleStatus
GenerationRuleMode
RecommendationStatus
```

禁止到处使用裸 String。

---

# 92. Entity Enum 映射

JPA：

```text
@Enumerated(EnumType.STRING)
```

必须使用：

```text
STRING
```

不要：

```text
ORDINAL
```

---

# 93. ID 类型

统一：

```text
UUID
```

Java：

```text
java.util.UUID
```

DTO 中也直接使用 UUID。

---

# 94. 时间类型

Java 使用：

```text
Instant
```

数据库：

```text
TIMESTAMPTZ
```

API 输出：

```text
ISO-8601 UTC
```

---

# 95. Pagination

所有列表 API 统一支持：

```text
page
size
sort
```

默认最大 size 后续 API 文档确定。

---

# 96. Search

Test Case 搜索由：

```text
TestCaseQueryService
```

负责。

Repository 可以使用：

```text
Native PostgreSQL Query
pg_trgm
```

Search 不需要独立 SearchService 微服务。

---

# 97. 模块依赖原则

推荐依赖方向：

```text
auth → user

project → user, testcase

capability → project, user

generation → project, capability, testcase

execution → project, testcase, user

evidence → project/execution, storage

changerequest → testcase, user

export → project, execution, evidence
```

---

# 98. 禁止循环 Service 依赖

例如禁止：

```text
ProjectService
→ ExecutionService
→ ProjectService
```

如果出现：

```text
双向依赖
```

应提取：

```text
更高层 Orchestrator
```

或者：

```text
更小职责 Service
```

---

# 99. Cross-Module Repository

原则：

```text
Module A 不直接注入 Module B Repository
```

优先：

```text
Module B QueryService / DomainService
```

例外：

```text
高性能批量查询
```

需要明确记录，不作为默认写法。

---

# 100. Application Events

V1 不依赖 Event-Driven 架构。

可使用 Spring ApplicationEvent 做：

```text
非关键 Audit
辅助通知
```

但核心业务一致性：

```text
Progressive Runtime
Generation
Publish
```

不能依赖异步 Event 才完成。

---

# 101. Async

V1 核心操作：

```text
同步执行
```

包括：

```text
Generation
Excel Export
Progressive Complete
```

后续规模变大再考虑：

```text
@Async
Worker
Queue
```

---

# 102. API Controller 规划

V1 Controller 建议：

```text
AuthController

UserController
RoleController

ProjectController
ProjectCapabilityController
ProjectTestPlanController

TestCaseController
TestCaseDraftController
TestCaseReviewController
TestCaseGraphController

ToolController

GenerationRuleController
GenerationController

ExecutionController
MyTestController

EvidenceController
NoteController

CapabilityUpdateRequestController
TestCaseChangeRequestController

ExportController

AdminAuditController
```

---

# 103. Service 规划总表

```text
AuthenticationService
CurrentUserService

UserService
RoleService

ProjectService
ProjectCompletionService
ProjectTestPlanService

CapabilityService
ProjectCapabilityService
CapabilityEngine
CapabilityUpdateRequestService

TestCaseQueryService
TestCaseDraftService
TestCaseVersionService
TestCasePublishService
DagValidationService

ToolService

GenerationRuleService
GenerationEngine
GenerationConditionEvaluator
GenerationGroupEvaluator
GenerationRecommendationService

ExecutionService
ProgressiveRuntimeService
TargetProjectTestCaseResolver
RelationStateService
MyTestQueryService

EvidenceService
StorageService

TestCaseChangeRequestService
RevisionContributorService
ReviewService

ExcelExportService

AuditService

ResourceAuthorizationService
```

---

# 104. GenerationEngine 调用链

```text
GenerationController
↓
GenerationRunService
↓
GenerationEngine
├─ ProjectService
├─ ProjectCapabilityService
├─ CapabilityEngine
├─ GenerationRuleService
├─ GenerationConditionEvaluator
└─ TestCaseVersionService
↓
GenerationRecommendationService
↓
Database
```

---

# 105. ProgressiveRuntime 调用链

```text
ExecutionController
↓
ExecutionService
↓
ProgressiveRuntimeService
├─ TestCaseVersionService
├─ TargetProjectTestCaseResolver
├─ ProjectTestPlanService
├─ RelationStateService
└─ ResourceAuthorizationService
↓
Database
```

---

# 106. Capability Update 调用链

```text
CapabilityUpdateRequestController
↓
CapabilityUpdateRequestService
↓
CapabilityEngine
↓
ProjectCapabilityService
↓
GenerationRunService
↓
GenerationEngine
↓
Recommended Test Cases
```

---

# 107. Test Case Publish 调用链

```text
TestCaseReviewController
↓
ReviewService
↓
TestCasePublishService
├─ DagValidationService
├─ TestCaseVersionService
└─ AuditService
↓
Database
```

---

# 108. Remove / Restore 调用链

```text
ProjectTestPlanController
↓
ProjectTestPlanService
├─ ResourceAuthorizationService
├─ RelationStateService
└─ AuditService(optional)
↓
Database
```

---

# 109. Testing Architecture

后端测试分层：

```text
Unit Test
Service Integration Test
Repository Test
Controller/API Test
Security Test
E2E Backend Flow
```

---

# 110. Unit Test

纯 Java Unit Test 重点：

```text
GenerationConditionEvaluator
GenerationGroupEvaluator
CapabilityEngine
DagValidationService
RelationStateService
```

不启动 Spring Context。

---

# 111. Service Integration Test

使用：

```text
@SpringBootTest
Testcontainers PostgreSQL
```

重点：

```text
ProgressiveRuntimeService
GenerationEngine
TestCasePublishService
CapabilityUpdateRequestService
ProjectTestPlanService
```

---

# 112. Repository Test

使用真实 PostgreSQL Testcontainer。

重点验证：

```text
Partial Unique Index
Native Query
pg_trgm
Trigger Query
Project Completion Query
```

---

# 113. Security Test

必须覆盖：

```text
Tester 不能 Publish
Tester 不能改 Assignees
Tester 只能执行 Assigned Case
Coordinator 不能 Publish
Admin 可以 Publish
Contributor 只能编辑指定 Draft
```

---

# 114. Progressive Runtime Test

必须覆盖：

```text
NEXT_CASE
NEXT_CASES
PASS
FAIL
N_A
MULTIPLE
同一 Target 去重
多 Trigger
Assignee Union
Completed Target 再触发
Floating
Restore Connection
Detach
```

---

# 115. Generation Test

必须覆盖：

```text
YES
NO
UNKNOWN
NE_NO
NE_YES
ANY Standard
AND
OR
Child Group
多个 Rule 命中
Output 去重
Ignored
Progressive ENTRY
```

---

# 116. Logging

所有 Service 使用：

```text
SLF4J
```

记录：

```text
关键流程开始/结束
业务异常
配置异常
非预期异常
```

避免记录：

```text
Password
Token
Evidence 文件正文
用户上传敏感内容
```

---

# 117. Trace ID

建议每个 HTTP Request 加：

```text
traceId
```

可以通过：

```text
MDC
```

实现。

错误响应返回：

```text
traceId
```

便于排查。

---

# 118. Spring Profile

建议：

```text
dev
test
prod
```

---

# 119. Configuration Package

推荐：

```text
common/config/
├─ JacksonConfig
├─ WebConfig
├─ JpaConfig
├─ FlywayConfig(optional)
└─ ClockConfig
```

建议注入：

```text
Clock
```

而不是业务代码到处：

```text
Instant.now()
```

方便测试时间逻辑。

---

# 120. Database Access Style

推荐优先：

```text
Spring Data Repository
```

复杂批量操作可使用：

```text
JdbcTemplate
NamedParameterJdbcTemplate
```

不需要强迫所有操作经过 Hibernate。

例如：

```text
批量 Assignee Union
复杂搜索
Recursive Query
```

可以使用原生 SQL。

---

# 121. N+1 控制

必须主动控制：

```text
EntityGraph
Projection
JOIN FETCH
DTO Query
```

特别是：

```text
Project Test Table
My Tests
Test Case Detail
Logic Graph
```

禁止直接序列化 Entity Graph。

---

# 122. Logic Graph Query

Logic Graph 建议由：

```text
TestCaseGraphQueryService
```

一次性返回图 DTO：

```text
nodes[]
edges[]
currentNode
```

不让前端逐节点 N 次请求。

---

# 123. Project Logic Graph

Project Execution Path 由：

```text
ProjectGraphQueryService
```

根据：

```text
ProjectTestCase
ProjectTestCaseTrigger
BranchOutcome
RelationStatus
```

生成。

Master Graph 与 Project Graph 是两种查询。

---

# 124. Graph Query Service

建议 testcase 与 execution 各自维护：

```text
MasterLogicGraphQueryService
ProjectLogicGraphQueryService
```

不要混成一个巨大 GraphService。

---

# 125. File Download

EvidenceController：

```text
GET metadata
GET download
POST upload
DELETE
```

下载前：

```text
ResourceAuthorizationService
```

检查用户是否有 Project 查看权限。

---

# 126. 文件 Streaming

大文件下载：

```text
StreamingResponseBody
```

或 Resource streaming。

不要：

```text
一次性 byte[] 全部读入 JVM Heap
```

---

# 127. Multipart Upload

Nginx 和 Spring Boot 都要设置：

```text
max upload size
```

具体大小在 Deployment 文档确定。

---

# 128. Excel Streaming

如果 Test Case 数量增加，Apache POI 使用：

```text
SXSSFWorkbook
```

避免大导出占用大量内存。

V1 可以直接采用 SXSSF，成本不高。

---

# 129. Backend Coding Rules

建议：

```text
Controller ≤ 负责 HTTP
Service ≤ 负责业务
Repository ≤ 负责数据
Entity ≤ 负责状态
DTO ≤ 负责 API
Mapper ≤ 负责转换
Engine ≤ 负责规则计算
```

---

# 130. 禁止 God Service

如果一个 Service 同时承担：

```text
项目
生成
执行
证据
版本
```

必须拆分。

例如禁止：

```text
ProjectService.completeTestCaseAndGenerateNextAndUploadEvidence()
```

---

# 131. 推荐类大小原则

不是硬限制，但建议：

```text
Controller:
通常 < 300 行

Service:
通常 < 500 行

复杂 Engine:
拆 evaluator / resolver
```

当一个类明显难以理解时，应按职责拆分。

---

# 132. V1 不引入

后端 V1 暂不引入：

```text
Kafka
RabbitMQ
Redis
WebSocket
GraphQL
CQRS Framework
Event Sourcing
Workflow Engine
Drools
Camunda
Temporal
Kubernetes SDK
```

当前业务不需要。

---

# 133. 为什么不用 Drools

Generation Rule 虽然是规则系统，但 V1 仅：

```text
YES / NO / UNKNOWN
AND / OR
一层 Group
多 Output
```

自研轻量 Rule Evaluator 更容易：

```text
理解
测试
维护
调试
```

不需要引入 Drools。

---

# 134. 为什么不用工作流引擎

Test Case Lifecycle：

```text
DRAFT
REVIEW
PUBLISHED
DEPRECATED
```

流程简单。

Progressive Runtime 又是：

```text
DAG
Decision Point
Transition
```

不是通用 BPMN。

因此不引入：

```text
Camunda
Flowable
```

---

# 135. Backend Architecture V1.0 最终冻结

后端正式采用：

```text
Spring Boot Modular Monolith

按业务模块分包
+
模块内 Controller / DTO / Entity / Repository / Service
```

核心 Engine：

```text
CapabilityEngine
GenerationEngine
ProgressiveRuntimeService
DagValidationService
```

核心 Orchestrator：

```text
ProjectTestPlanService
TestCasePublishService
CapabilityUpdateRequestService
```

权限：

```text
Spring Security
+
Permission Code
+
ResourceAuthorizationService
```

事务：

```text
Service Layer
```

查询：

```text
Query Service
Projection
Native SQL where useful
```

文件：

```text
EvidenceService
↓
StorageService
```

错误：

```text
统一业务异常
+
@RestControllerAdvice
+
traceId
```

测试：

```text
JUnit 5
Spring Boot Test
Testcontainers PostgreSQL
```

这套架构作为后续 API Design 和代码实现的唯一后端基准。

---

# 136. 下一阶段

下一份文档：

```text
IoT-Security-Case-Hub_API-Design_V1.0.md
```

将正式定义：

```text
REST Endpoint
HTTP Method
Path
Request DTO
Response DTO
Pagination
Filtering
Sorting
Error Code
Permission
Project API
Test Case API
Generation API
Execution API
Evidence API
Change Request API
Export API
```

完成 API Design 后进入：

```text
Frontend Architecture V1.0
```
