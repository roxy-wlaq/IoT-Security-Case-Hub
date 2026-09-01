# IoT-Security-Case-Hub
## Data Model V1.0

> 本文档基于：
>
> - `IoT-Security-Case-Hub_System-Design_V0.6.md`
> - `IoT-Security-Case-Hub_Technical-Architecture_V1.0.md`
>
> 目标：
>
> 在正式设计 PostgreSQL Table / Column 之前，冻结系统核心 Entity、Cardinality、Ownership、Version Binding、DAG Relationship、Generation Relationship、Project Execution Relationship 和生命周期边界。
>
> 本文档定义“数据对象和关系”，暂不定义具体 SQL 类型、索引名称和字段长度。

---

# 1. 数据模型总体原则

系统数据模型遵循以下原则：

1. Master Test Case 与 Project Test Case 必须分离。
2. Master Test Case 与 Test Case Version 必须分离。
3. Project Test Case 必须绑定具体 Test Case Version。
4. Decision Point 属于 Test Case Version，而不是 Master Test Case。
5. Transition 属于具体 Version 的 Decision Point。
6. Progressive DAG 以 Test Case Version 之间的逻辑关系为模板。
7. Project 执行时生成独立 Project Test Case 实例。
8. Project 内同一 Master Test Case 默认只有一个 Project Test Case 实例。
9. Source、Triggered By、Assignee 允许多值。
10. Evidence / Note / Branch Outcome 属于 Project Test Case。
11. Capability Library 与 Project Capability 分离。
12. Generation Rule 与 Test Case 是多对多。
13. Generation Rule 只负责推荐，不负责运行时递进。
14. Progressive Runtime 只通过 Decision Point / Transition 推进。
15. Published Version 不允许原地覆盖。
16. 历史 Project 必须保留原 Version Binding。
17. Removed 不等于物理删除。
18. Archived 不等于物理删除。
19. Floating 是关系状态，不是执行状态。
20. Project Custom Test Case 与 Master Test Case 必须区分。

---

# 2. 数据域划分

V1 数据模型分为 9 个领域：

```text
1. Identity & RBAC
2. Project
3. Standard / Task Type
4. Capability
5. Master Test Case Library
6. Progressive DAG
7. Generation Rule
8. Project Execution
9. Change / Version / Evidence
```

---

# 3. 核心 Entity 总览

```text
User
Role
UserRole

Project
ProjectStandard
ProjectCoordinator

StandardTaskType

Capability
ProjectCapability
CapabilityUpdateRequest

MasterTestCase
TestCaseVersion
TestStep
Category
Tag
TestCaseTag
Tool
TestCaseTool
TestCaseStandardMapping
Attachment

DecisionPoint
Transition
TransitionTarget

GenerationRule
GenerationConditionGroup
GenerationCondition
GenerationRuleOutput
GenerationRun
GenerationRecommendation

ProjectTestCase
ProjectCustomTestCase
ProjectTestCaseAssignee
ProjectTestCaseSource
ProjectTestCaseTrigger
ProjectDecisionSelection
BranchOutcome
Evidence
Note

TestCaseChangeRequest
RevisionContributor
AuditLog
```

---

# 4. Identity & RBAC

## 4.1 User

表示系统用户。

核心属性：

```text
User
├─ id
├─ username
├─ displayName
├─ passwordHash
├─ enabled
├─ createdAt
└─ updatedAt
```

一个 User 可以拥有多个 Role。

---

## 4.2 Role

V1 固定角色：

```text
ADMIN
TEST_COORDINATOR
TESTER
```

Role 可作为数据表，也可以由系统初始化。

推荐使用数据表，便于后续扩展。

---

## 4.3 UserRole

多对多关系：

```text
User N ─── N Role
```

通过：

```text
UserRole
```

实现。

唯一性：

```text
(user_id, role_id)
```

必须唯一。

---

# 5. Standard / Task Type

## 5.1 StandardTaskType

统一表示：

```text
EN 18031
FDA
PSTI
EN 303 645
其他内部任务类型
```

不需要将 Standard 和 Task Type 拆成两个表。

统一成：

```text
StandardTaskType
```

通过 type 字段区分：

```text
STANDARD
TASK_TYPE
```

核心属性：

```text
id
code
name
type
enabled
description
```

---

# 6. Project

## 6.1 Project

Project 表示一次实际测试项目。

核心属性：

```text
Project
├─ id
├─ projectNumber
├─ projectName
├─ deviceName
├─ generationMode
├─ status
├─ createdBy
├─ createdAt
└─ updatedAt
```

Generation Mode：

```text
FULL
PROGRESSIVE
```

Project Status：

```text
DRAFT
ACTIVE
COMPLETED
ARCHIVED
```

---

## 6.2 ProjectStandard

Project 与 StandardTaskType 为多对多：

```text
Project N ─── N StandardTaskType
```

通过：

```text
ProjectStandard
```

实现。

---

## 6.3 ProjectCoordinator

虽然 V1 创建项目时只选择主要 Test Coordinator，但建议数据模型直接支持：

```text
Project N ─── N User
```

原因：

后续项目可能存在多个 Coordinator。

ProjectCoordinator：

```text
project_id
user_id
is_primary
```

V1 UI 可以只允许一个 primary coordinator。

---

# 7. Capability Library

## 7.1 Capability

Capability 使用自关联树。

核心属性：

```text
Capability
├─ id
├─ parentId
├─ code
├─ name
├─ description
├─ enabled
├─ sortOrder
└─ createdAt
```

关系：

```text
Capability 1 ─── N Capability
```

例如：

```text
Bluetooth
├─ BLE
│  ├─ GATT
│  ├─ Pairing
│  └─ Bonding
└─ BR/EDR
```

---

## 7.2 ProjectCapability

表示某 Project 对某 Capability 的实际状态。

关系：

```text
Project 1 ─── N ProjectCapability
Capability 1 ─── N ProjectCapability
```

唯一性：

```text
(project_id, capability_id)
```

必须唯一。

核心属性：

```text
value
source
isDerived
evidenceReference
comment
updatedBy
updatedAt
```

Value：

```text
YES
NO
UNKNOWN
```

Source：

```text
CUSTOMER_PROVIDED
TESTER_DISCOVERED
DOCUMENT
AUTOMATIC_DETECTION
COORDINATOR_INPUT
DERIVED
OTHER
```

---

## 7.3 Capability 推导规则

数据层原则：

```text
Parent = NO
→ Child 在规则匹配阶段视为不可适用
```

不需要物理写入所有 Child = NO。

```text
Parent = YES
→ Child 仍保持原值
```

```text
Child = YES
→ 系统向上推导 Parent = YES
```

自动推导值：

```text
isDerived = true
source = DERIVED
```

---

## 7.4 CapabilityUpdateRequest

表示 Tester 对 Project Capability 的修改建议。

关系：

```text
Project 1 ─── N CapabilityUpdateRequest
Capability 1 ─── N CapabilityUpdateRequest
User 1 ─── N CapabilityUpdateRequest
```

核心状态：

```text
PENDING
APPROVED
REJECTED
```

核心属性：

```text
currentValue
proposedValue
reason
evidenceReference
submittedBy
reviewedBy
reviewedAt
```

Approved 后：

```text
更新 ProjectCapability
↓
触发新的 Generation Run
```

但不直接触发 Progressive Runtime。

---

# 8. Master Test Case

## 8.1 MasterTestCase

MasterTestCase 只表示：

> 这条 Test Case 的稳定身份。

例如：

```text
BLE-PAIR-001
```

核心属性：

```text
MasterTestCase
├─ id
├─ caseCode
├─ categoryId
├─ createdBy
├─ createdAt
└─ enabled
```

MasterTestCase 不保存版本化的具体测试内容。

---

## 8.2 TestCaseVersion

TestCaseVersion 保存某一具体版本的完整内容。

关系：

```text
MasterTestCase 1 ─── N TestCaseVersion
```

例如：

```text
BLE-PAIR-001
├─ v1.0
├─ v1.1
├─ v1.2
└─ v2.0
```

核心属性：

```text
version
status
isCurrentVersion
caseName
testPurpose
preconditions
selectionMode
evidenceRequirement
remarkRequirement
progressiveRole
changeReason
createdBy
reviewedBy
publishedAt
createdAt
updatedAt
```

Status：

```text
DRAFT
REVIEW
PUBLISHED
DEPRECATED
```

Selection Mode：

```text
SINGLE
MULTIPLE
```

Progressive Role：

```text
ENTRY
NORMAL
```

---

# 9. MasterTestCase 与 TestCaseVersion 的关系

正式冻结：

```text
MasterTestCase
= 稳定身份

TestCaseVersion
= 某个具体版本内容
```

Project 执行实例必须绑定：

```text
TestCaseVersion
```

不能只绑定 MasterTestCase。

---

# 10. TestStep

TestStep 属于 TestCaseVersion。

关系：

```text
TestCaseVersion 1 ─── N TestStep
```

核心属性：

```text
sequence
title
content
```

TestStep 不保存：

```text
Decision Point
Evidence
Result
```

因为 Decision Point 属于整个 Test Case Version。

---

# 11. Category

Category 使用最多两层结构。

```text
Category
├─ id
├─ parentId
├─ name
├─ code
├─ enabled
└─ sortOrder
```

关系：

```text
Category 1 ─── N Category
```

MasterTestCase 关联一个主 Category。

推荐：

```text
MasterTestCase N ─── 1 Category
```

---

# 12. Tag

Test Case 与 Tag 多对多：

```text
MasterTestCase N ─── N Tag
```

通过：

```text
TestCaseTag
```

实现。

标签不参与 Generation Rule 的直接执行逻辑。

---

# 13. Tool

Tool Library：

```text
Tool
├─ id
├─ name
├─ description
├─ enabled
└─ createdAt
```

TestCaseVersion 与 Tool 多对多：

```text
TestCaseVersion N ─── N Tool
```

通过：

```text
TestCaseTool
```

实现。

原因：

不同版本可能调整工具。

---

# 14. Standard Mapping

TestCaseVersion 与 StandardTaskType 多对多。

通过：

```text
TestCaseStandardMapping
```

用于：

```text
检索
学习
展示
标准关联
```

Generation Rule 仍然是实际的自动推荐逻辑。

---

# 15. Attachment

Attachment 支持：

```text
Test Case Version Attachment
Tool Attachment
其他未来扩展
```

V1 推荐使用通用 Attachment：

```text
Attachment
├─ id
├─ ownerType
├─ ownerId
├─ originalFilename
├─ storageKey
├─ size
├─ uploadedBy
└─ createdAt
```

或者在具体 Schema 阶段改为明确 FK 表。

V1 更推荐明确的业务表，不推荐过度 Generic FK。

因此正式 Database Schema 阶段建议优先拆成：

```text
TestCaseAttachment
ToolAttachment
```

---

# 16. DecisionPoint

DecisionPoint 必须属于：

```text
TestCaseVersion
```

而不是 MasterTestCase。

关系：

```text
TestCaseVersion 1 ─── N DecisionPoint
```

原因：

不同版本的测试逻辑可能变化。

核心属性：

```text
name
description
transitionType
displayOrder
```

Transition Type：

```text
NEXT_CASE
NEXT_CASES
PASS
FAIL
N/A
```

---

# 17. Transition

逻辑上：

```text
Decision Point
↓
Transition
```

虽然 DecisionPoint 已经保存 transitionType，但为了后续扩展，推荐独立 Transition Entity。

关系：

```text
DecisionPoint 1 ─── 1 Transition
```

V1 一个 Decision Point 只有一个 Transition 语义。

Transition：

```text
type
```

---

# 18. TransitionTarget

当 Transition Type：

```text
NEXT_CASE
NEXT_CASES
```

时，Transition 可指向一个或多个目标 Test Case。

关系：

```text
Transition 1 ─── N TransitionTarget
```

TransitionTarget 指向：

```text
MasterTestCase
```

而不是固定 TestCaseVersion。

原因：

Master DAG 表达“逻辑上的下一条 Test Case”。

实际 Project 激活时选择：

```text
当前最新有效 Published Version
```

或者按项目已有绑定版本复用。

---

# 19. 为什么 TransitionTarget 指向 MasterTestCase

假设：

```text
TC-A v1.0
→ TC-B
```

以后：

```text
TC-B v1.1
```

发布。

如果 TransitionTarget 直接绑定：

```text
TC-B v1.0
```

逻辑图会被旧 Version 锁死。

因此推荐：

```text
TransitionTarget
→ MasterTestCase
```

运行时再确定具体 Version。

---

# 20. DAG 约束

Master Test Case 的 Progressive Graph：

```text
MasterTestCase
↓
TestCaseVersion
↓
DecisionPoint
↓
Transition
↓
Target MasterTestCase
```

发布新 Version 前必须进行：

```text
Cycle Detection
```

禁止：

```text
A → B → C → A
```

V1 Progressive Graph 必须是 DAG。

---

# 21. GenerationRule

GenerationRule 表示一条结构化推荐规则。

核心属性：

```text
ruleCode
name
mode
status
description
createdBy
createdAt
updatedAt
```

Mode：

```text
FULL
PROGRESSIVE_INITIAL
BOTH
```

Status：

```text
ENABLED
DISABLED
```

---

# 22. GenerationConditionGroup

Rule 可以拥有多个 Condition Group。

关系：

```text
GenerationRule 1 ─── N GenerationConditionGroup
```

Group Operator：

```text
AND
OR
```

V1 只允许：

```text
顶层 Group
+
一层 Child Group
```

---

# 23. GenerationCondition

属于 Condition Group。

关系：

```text
GenerationConditionGroup 1 ─── N GenerationCondition
```

Condition Target Type：

```text
CAPABILITY
STANDARD_TASK_TYPE
```

Capability Operator：

```text
EQ_YES
EQ_NO
EQ_UNKNOWN
NE_NO
NE_YES
```

Standard / Task Type 匹配：

```text
ANY
```

---

# 24. GenerationRuleOutput

GenerationRule 与 MasterTestCase 多对多。

通过：

```text
GenerationRuleOutput
```

实现。

关系：

```text
GenerationRule N ─── N MasterTestCase
```

一条 Rule 可以输出多个 Test Case。

一个 Test Case 可以被多条 Rule 推荐。

---

# 25. GenerationRun

每次项目执行 Generation 时创建一个 GenerationRun。

关系：

```text
Project 1 ─── N GenerationRun
```

核心属性：

```text
mode
triggerType
executedBy
executedAt
```

Trigger Type：

```text
PROJECT_INITIAL
CAPABILITY_UPDATE
STANDARD_CHANGE
MANUAL_REGENERATE
```

---

# 26. GenerationRecommendation

每次 GenerationRun 的推荐结果。

关系：

```text
GenerationRun 1 ─── N GenerationRecommendation
MasterTestCase 1 ─── N GenerationRecommendation
```

核心属性：

```text
status
```

Recommendation Status：

```text
NEW
ADDED
IGNORED
```

推荐原因不要只存单一 ruleId。

需要保留所有命中 Rule。

可以通过额外关系：

```text
GenerationRecommendationRule
```

实现：

```text
GenerationRecommendation N ─── N GenerationRule
```

---

# 27. Ignore 持久化

Project 对某 MasterTestCase 的 Ignore 状态必须可持久化。

推荐独立实体：

```text
ProjectTestCasePreference
```

核心：

```text
projectId
masterTestCaseId
state
```

State：

```text
IGNORED
```

这样重新 Generation 时：

```text
同一 Project + MasterTestCase
```

不会反复成为 NEW。

---

# 28. ProjectTestCase

ProjectTestCase 表示：

> 某 Project 中真正的测试执行实例。

关系：

```text
Project 1 ─── N ProjectTestCase
```

如果来源于 Master：

```text
ProjectTestCase N ─── 1 MasterTestCase
ProjectTestCase N ─── 1 TestCaseVersion
```

核心属性：

```text
executionStatus
relationStatus
removed
lastModifiedBy
lastModifiedAt
```

Execution Status：

```text
NOT_STARTED
IN_PROGRESS
COMPLETED
```

Relation Status：

```text
CONNECTED
FLOATING
```

---

# 29. ProjectTestCase 唯一性

正式冻结：

同一 Project 内：

```text
Project + MasterTestCase
```

最多一个有效 ProjectTestCase 实例。

因此推荐数据库 Unique：

```text
(project_id, master_test_case_id)
```

对于 Custom Test Case：

```text
master_test_case_id = NULL
```

需要单独设计唯一逻辑。

---

# 30. ProjectTestCase Version Binding

ProjectTestCase 必须保存：

```text
test_case_version_id
```

用于锁定当前使用版本。

如果项目升级：

```text
v1.2 → v1.3
```

ProjectTestCase 本身不变。

只修改：

```text
test_case_version_id
```

同时保留现有：

```text
Evidence
Notes
Execution Status
Assignees
```

---

# 31. ProjectCustomTestCase

Custom Case 不进入 MasterTestCase。

推荐独立实体：

```text
ProjectCustomTestCase
```

关系：

```text
Project 1 ─── N ProjectCustomTestCase
```

它保存类似 TestCaseVersion 的执行定义：

```text
caseCode
caseName
testPurpose
preconditions
selectionMode
evidenceRequirement
remarkRequirement
```

并拥有自己的：

```text
Test Steps
Decision Points
Transitions
```

---

# 32. ProjectTestCase 与 Custom 的统一执行方式

为了让执行层统一，ProjectTestCase 使用二选一引用：

```text
master_test_case_id + test_case_version_id
```

或者：

```text
project_custom_test_case_id
```

必须满足：

```text
二者只能有一种有效
```

即：

```text
Master-based
OR
Custom-based
```

不能同时存在。

---

# 33. Custom Test Step / Decision Point

V1 有两种实现选择：

A：

```text
单独建 CustomTestStep / CustomDecisionPoint
```

B：

```text
TestStep / DecisionPoint 使用 ownerType
```

推荐 A。

原因：

```text
强 FK
结构清晰
JPA 映射简单
避免 Generic Owner
```

因此 Custom 使用独立子表。

---

# 34. ProjectTestCaseAssignee

ProjectTestCase 与 User 多对多。

通过：

```text
ProjectTestCaseAssignee
```

实现。

唯一：

```text
(project_test_case_id, user_id)
```

---

# 35. Assignee 自动继承

Progressive 首次创建后续 ProjectTestCase：

```text
继承触发它的前置 ProjectTestCase Assignees
```

多前置触发同一后续：

```text
取 Assignee Union
```

通过数据库唯一约束去重。

---

# 36. ProjectTestCaseSource

ProjectTestCase Source 允许多值。

通过：

```text
ProjectTestCaseSource
```

实现。

Source Type：

```text
INITIAL
GENERATED
PROGRESSIVE
MANUAL
CUSTOM
```

唯一：

```text
(project_test_case_id, source_type)
```

---

# 37. ProjectTestCaseTrigger

记录 Progressive Triggered By。

关系：

```text
ProjectTestCase 1 ─── N ProjectTestCaseTrigger
```

每个 Trigger 记录：

```text
sourceProjectTestCaseId
sourceDecisionPointIdentity
targetProjectTestCaseId
createdAt
```

注意：

Project 执行时不能只引用 Master DecisionPoint，因为 Project 使用的是具体 Version。

因此 Trigger 推荐记录：

```text
source_project_test_case_id
source_test_case_version_id
source_decision_point_id
```

---

# 38. Trigger 唯一性

同一个来源：

```text
Source ProjectTestCase
+
DecisionPoint
+
Target ProjectTestCase
```

不能重复插入。

---

# 39. ProjectDecisionSelection

表示 ProjectTestCase 执行时 Tester 选择了哪些 Decision Point。

关系：

```text
ProjectTestCase 1 ─── N ProjectDecisionSelection
```

记录：

```text
decisionPointId
selectedBy
selectedAt
```

MULTIPLE 时存在多条。

重新修改 Completed Test Case 时：

```text
更新当前 Selection Set
```

V1 不保留完整 Selection 历史。

---

# 40. BranchOutcome

BranchOutcome 表示每个被选择 Decision Point 的运行结果。

关系：

```text
ProjectTestCase 1 ─── N BranchOutcome
```

例如：

```text
DP-A → NEXT_CASE → TC-002
DP-B → FAIL
```

Branch Outcome Type：

```text
NEXT_CASE
NEXT_CASES
PASS
FAIL
N/A
```

---

# 41. Test Case 不设置单一 Result

正式冻结：

```text
ProjectTestCase
```

不需要：

```text
result = PASS / FAIL
```

ProjectTestCase 只保存：

```text
Execution Status
Selected Decision Points
Branch Outcomes
```

原因：

MULTIPLE 模式可能同时存在：

```text
一个分支 FAIL
另一个分支 NEXT_CASE
```

---

# 42. Floating

FLOATING 的真实判断来自：

```text
有效 Incoming Trigger / Transition
```

而不是纯手工字段。

但为了查询性能，可以在 ProjectTestCase 保存：

```text
relation_status
```

作为当前派生状态缓存。

规则：

```text
至少一个有效 Incoming Trigger
→ CONNECTED

无有效 Incoming Trigger
→ FLOATING
```

入口节点、Manual / Initial 节点需要例外：

```text
Root / Initial
```

即使没有 Incoming Trigger 也可视为 CONNECTED。

因此 ProjectTestCase 还需要识别：

```text
isRoot
```

或根据 Source 判断。

推荐显式字段：

```text
is_root
```

---

# 43. Removed

ProjectTestCase 推荐使用：

```text
removed = true / false
```

而不是增加 Execution Status = REMOVED。

原因：

Removed 与 Execution Status 正交。

例如：

```text
Execution Status = COMPLETED
Removed = true
```

合法。

---

# 44. Restore

Restore：

```text
removed = false
```

恢复：

```text
Evidence
Notes
BranchOutcome
Assignees
Execution Status
```

不变。

---

# 45. Evidence

Evidence 属于：

```text
ProjectTestCase
```

关系：

```text
ProjectTestCase 1 ─── N Evidence
```

核心属性：

```text
originalFilename
storageKey
size
description
uploadedBy
createdAt
```

实际文件不存 PostgreSQL。

---

# 46. Evidence 权限数据模型

Evidence 不需要保存单独 ACL。

权限通过：

```text
Project
ProjectTestCase
Assignee
Role
```

动态判断。

所有当前 Assignee 可以删除共享 Evidence。

---

# 47. Note

Note 属于：

```text
ProjectTestCase
```

关系：

```text
ProjectTestCase 1 ─── N Note
```

核心属性：

```text
authorId
content
createdAt
updatedAt
```

作者可修改 / 删除自己的 Note。

其他 Tester 不可修改。

---

# 48. TestCaseChangeRequest

表示公共 Master Test Case 修改申请。

关系：

```text
MasterTestCase 1 ─── N TestCaseChangeRequest
TestCaseVersion 1 ─── N TestCaseChangeRequest
User 1 ─── N TestCaseChangeRequest
```

核心状态：

```text
PENDING
APPROVED
REJECTED
```

---

# 49. Revision

Revision 不建议另建“Revision”主表。

推荐直接使用：

```text
TestCaseVersion
status = DRAFT
```

并保存：

```text
basedOnVersionId
changeRequestId
```

即可表达：

```text
Revision Draft
```

这样避免 Version / Revision 双模型。

---

# 50. RevisionContributor

Draft Version 与 User 多对多：

```text
TestCaseVersion N ─── N User
```

通过：

```text
RevisionContributor
```

实现。

用于问题提出人获得：

```text
指定 Draft
```

的临时编辑权限。

---

# 51. Review / Publish

TestCaseVersion 通过状态完成：

```text
DRAFT
↓
REVIEW
↓
PUBLISHED
```

管理员审核结果：

```text
PUBLISH
RETURN
REJECT
```

Return：

```text
REVIEW → DRAFT
```

Reject：

```text
Revision 结束
```

具体是否增加：

```text
REJECTED
```

版本状态，在 Schema 阶段再定。

当前 V0.6 产品文档只使用四个正式状态，所以建议：

```text
Reject 后 Draft 标记 inactive / closed
```

而不是增加第五个公开状态。

---

# 52. Current Version

同一个 MasterTestCase：

```text
最多一个
PUBLISHED + isCurrentVersion = true
```

必须由数据库 / Service 保证。

旧 Published Version：

```text
isCurrentVersion = false
```

仍可查看。

---

# 53. Deprecated

Deprecated 属于：

```text
TestCaseVersion
```

同时 MasterTestCase 可保持存在。

如果整条 Test Case 不再使用：

推荐将最新 Current Version：

```text
status = DEPRECATED
```

历史 Version 继续保留。

---

# 54. AuditLog

AuditLog 只记录系统级关键操作。

核心属性：

```text
actorUserId
action
targetType
targetId
summary
createdAt
```

例如：

```text
LOGIN
PROJECT_CREATE
PROJECT_ARCHIVE
TEST_CASE_PUBLISH
TEST_CASE_DEPRECATED
GENERATION_RULE_UPDATE
CAPABILITY_LIBRARY_UPDATE
USER_ROLE_CHANGE
```

不记录 Tester 每一次字段编辑。

---

# 55. 数据关系总图

```text
User
 ├─< UserRole >─ Role
 ├─< ProjectCoordinator >─ Project
 ├─< ProjectTestCaseAssignee >─ ProjectTestCase
 ├─ Evidence
 ├─ Note
 └─ Change Request

Project
 ├─< ProjectStandard >─ StandardTaskType
 ├─ ProjectCapability
 ├─ GenerationRun
 ├─ ProjectTestCase
 └─ ProjectCustomTestCase

Capability
 ├─ parent Capability
 ├─ child Capability
 └─ ProjectCapability

MasterTestCase
 ├─ TestCaseVersion
 ├─ GenerationRuleOutput
 ├─ TestCaseTag
 └─ ProjectTestCase

TestCaseVersion
 ├─ TestStep
 ├─ TestCaseTool
 ├─ TestCaseStandardMapping
 ├─ DecisionPoint
 └─ RevisionContributor

DecisionPoint
 └─ Transition
       └─ TransitionTarget
            └─ MasterTestCase

GenerationRule
 ├─ GenerationConditionGroup
 │    └─ GenerationCondition
 └─ GenerationRuleOutput
      └─ MasterTestCase

ProjectTestCase
 ├─ Assignees
 ├─ Sources
 ├─ Triggers
 ├─ Decision Selections
 ├─ Branch Outcomes
 ├─ Evidence
 └─ Notes
```

---

# 56. 关键 Cardinality

```text
User N ─── N Role

Project N ─── N StandardTaskType

Project N ─── N User
(Coordinator)

Capability 1 ─── N Capability
(Self Tree)

Project 1 ─── N ProjectCapability

MasterTestCase 1 ─── N TestCaseVersion

TestCaseVersion 1 ─── N TestStep

MasterTestCase N ─── N Tag

TestCaseVersion N ─── N Tool

TestCaseVersion N ─── N StandardTaskType

TestCaseVersion 1 ─── N DecisionPoint

DecisionPoint 1 ─── 1 Transition

Transition 1 ─── N TransitionTarget

GenerationRule 1 ─── N ConditionGroup

ConditionGroup 1 ─── N Condition

GenerationRule N ─── N MasterTestCase

Project 1 ─── N ProjectTestCase

ProjectTestCase N ─── N User
(Assignee)

ProjectTestCase 1 ─── N Source

ProjectTestCase 1 ─── N Trigger

ProjectTestCase 1 ─── N DecisionSelection

ProjectTestCase 1 ─── N BranchOutcome

ProjectTestCase 1 ─── N Evidence

ProjectTestCase 1 ─── N Note
```

---

# 57. 关键唯一性约束

Database Schema 阶段必须实现以下唯一性：

```text
User.username

Role.code

StandardTaskType.code

Project.projectNumber

Capability.code

ProjectCapability(project_id, capability_id)

MasterTestCase.caseCode

TestCaseVersion(master_test_case_id, version)

UserRole(user_id, role_id)

ProjectStandard(project_id, standard_task_type_id)

ProjectCoordinator(project_id, user_id)

TestCaseTag(master_test_case_id, tag_id)

TestCaseTool(test_case_version_id, tool_id)

GenerationRule.ruleCode

GenerationRuleOutput(rule_id, master_test_case_id)

ProjectTestCase(project_id, master_test_case_id)
[Master-based only]

ProjectTestCaseAssignee(project_test_case_id, user_id)

ProjectTestCaseSource(project_test_case_id, source_type)

ProjectTestCaseTrigger(source_project_test_case_id, decision_point_id, target_project_test_case_id)

RevisionContributor(test_case_version_id, user_id)
```

---

# 58. 删除策略

## User

默认不物理删除：

```text
enabled = false
```

---

## Project

默认：

```text
ARCHIVED
```

不物理删除。

---

## MasterTestCase

不物理删除。

通过：

```text
enabled
Deprecated Version
```

控制。

---

## TestCaseVersion

Published 后不可物理删除。

Draft 如果从未发布，可允许管理员清理，具体 Schema 阶段决定。

---

## ProjectTestCase

不物理删除业务数据。

使用：

```text
removed = true
```

---

## Evidence

允许删除实际文件和数据库记录。

是否保留轻量删除日志由 AuditLog 处理。

---

## Note

作者删除后可以物理删除。

V1 不要求 Note 历史。

---

# 59. Version Binding 原则

ProjectTestCase 必须始终知道：

```text
当前使用哪个 TestCaseVersion
```

项目完成后不自动升级。

Master 发布新版：

```text
不会修改已有 ProjectTestCase
```

只有 Coordinator 主动 Upgrade 才更新 Binding。

---

# 60. Progressive Target Version 解析

TransitionTarget 指向 MasterTestCase。

运行时自动激活目标 ProjectTestCase 时：

## 情况 A

项目已经存在目标 ProjectTestCase：

```text
复用现有实例
```

不管它当前绑定哪个 Version。

## 情况 B

项目不存在目标 ProjectTestCase：

选择：

```text
目标 MasterTestCase 当前 Published + Current Version
```

创建 ProjectTestCase。

如果没有可用 Published Version：

```text
Progressive Runtime 必须报配置错误
```

不能自动使用 Draft。

---

# 61. Progressive ENTRY

Progressive Initial Generation：

只允许默认推荐：

```text
Progressive Role = ENTRY
```

NORMAL 仍可：

```text
Manual Add
```

或者运行时被 TransitionTarget 激活。

---

# 62. Root ProjectTestCase

为了正确计算 FLOATING：

建议 ProjectTestCase 增加：

```text
isRoot
```

以下情况可以设置 Root：

```text
Initial
Generated
Progressive Initial ENTRY
Manual Add
Custom Manual Add
```

运行时 NEXT_CASE 自动产生的普通后续节点：

```text
isRoot = false
```

Root 节点即使没有 Incoming Trigger：

```text
Relation Status = CONNECTED
```

---

# 63. 多 Source 与 Root 的关系

Source 只表示“为什么存在”。

例如同一个 Case：

```text
GENERATED
+
PROGRESSIVE
```

都可存在。

isRoot 是独立执行关系属性。

不能只通过 Source 动态判断 Root。

---

# 64. Trigger 与 Floating

当修改 Decision Point 选择并选择：

```text
不使用节点
```

实际处理：

```text
使对应 ProjectTestCaseTrigger 失效
```

推荐 Trigger 增加：

```text
active
```

而不是物理删除。

原因：

方便判断：

```text
当前有效 Incoming Trigger
```

同时仍能解释原先关系。

但产品要求不保存完整修改历史。

因此 V1 也可以直接删除 Trigger。

推荐折中：

```text
Trigger 使用 active 字段
但不做完整 Version History
```

这样数据量仍非常小。

---

# 65. BranchOutcome 与 Trigger 的关系

当 BranchOutcome：

```text
NEXT_CASE / NEXT_CASES
```

产生后续节点时：

对应创建：

```text
ProjectTestCaseTrigger
```

BranchOutcome 保存逻辑结果。

Trigger 保存 Project DAG 实际连接关系。

两者职责不同。

---

# 66. Completed 后修改

修改 Selected Decision Points：

```text
重新计算当前 BranchOutcome
```

如果不影响下游：

```text
直接更新
```

如果影响：

```text
原节点
其他节点
增加节点
不使用节点
```

由业务逻辑更新 Trigger。

已有下游 ProjectTestCase 不自动删除。

---

# 67. Generation Recommendation 与 ProjectTestCase

Recommendation：

```text
只是候选
```

只有 Coordinator：

```text
Add
```

后才创建 / 复用：

```text
ProjectTestCase
```

Ignore 不创建执行实例。

---

# 68. Manual Add

Coordinator Manual Add：

```text
直接创建 / 复用 ProjectTestCase
```

并增加：

```text
Source = MANUAL
isRoot = true
```

---

# 69. Initial / Generated

如果项目初始 Generation 后 Coordinator Add：

```text
Source = GENERATED
```

如果系统预置 Initial Case：

```text
Source = INITIAL
```

Progressive Initial ENTRY 由 Generation Rule 推荐后加入：

推荐仍然：

```text
Source = GENERATED
isRoot = true
```

后续运行时自动展开：

```text
Source = PROGRESSIVE
isRoot = false
```

---

# 70. Custom Source

ProjectCustomTestCase 进入 ProjectTestCase：

```text
Source = CUSTOM
```

如果 Custom Case 被手工加入：

```text
CUSTOM
+
MANUAL
```

两个 Source 可以同时存在。

---

# 71. Project 完成判断

Project 可 Completed 当：

```text
所有
removed = false
的 ProjectTestCase
executionStatus = COMPLETED
```

包括：

```text
FLOATING
```

Test Case。

---

# 72. No Longer Recommended

Capability / Standard 更新后重新 Generation：

已有 ProjectTestCase 不再推荐时：

不自动移除。

推荐增加 ProjectTestCase 派生字段：

```text
recommendationState
```

例如：

```text
RECOMMENDED
NO_LONGER_RECOMMENDED
UNKNOWN
```

或者通过最新 GenerationRun 动态计算。

V1 推荐不增加持久字段，优先由最新 Generation Recommendation 动态展示。

---

# 73. 数据库边界：不存什么

V1 不保存：

```text
Tester 每次字段修改历史
实时编辑锁
完整 CRDT / OT 数据
Project Overall PASS / FAIL
复杂风险评分
Automation Runner Job
Script Execution Result
实时通知消息
```

---

# 74. 数据模型边界：未来自动执行

未来如果增加 Automation：

建议新增：

```text
ExecutionDefinition
AutomationJob
AutomationRunner
AutomationResult
```

不改变：

```text
ProjectTestCase
DecisionPoint
BranchOutcome
Evidence
```

核心模型。

---

# 75. Data Model V1.0 最终冻结

正式核心关系：

```text
MasterTestCase
1
↓
N
TestCaseVersion
1
↓
N
DecisionPoint
1
↓
1
Transition
1
↓
N
TransitionTarget
↓
MasterTestCase
```

项目执行：

```text
Project
1
↓
N
ProjectTestCase
├─ Version Binding
├─ Assignees[]
├─ Sources[]
├─ Triggers[]
├─ Decision Selections[]
├─ Branch Outcomes[]
├─ Evidence[]
└─ Notes[]
```

生成：

```text
Project Capability
+
Standard / Task Type
↓
Generation Rule
↓
Generation Recommendation
↓
Coordinator Add
↓
ProjectTestCase
```

递进：

```text
ProjectTestCase
↓
Selected Decision Point
↓
BranchOutcome
↓
Trigger
↓
Target ProjectTestCase
```

版本：

```text
MasterTestCase
↓
TestCaseVersion
↓
ProjectTestCase Binding
```

这套模型作为 Database Schema V1.0 的唯一数据建模基础。

---

# 76. 下一阶段

下一份文档：

```text
IoT-Security-Case-Hub_Database-Schema_V1.0.md
```

将正式定义：

```text
PostgreSQL Table
Column
Data Type
Primary Key
Foreign Key
Unique Constraint
Index
Nullable
Default Value
Check Constraint
Delete Behavior
```

并最终给出：

```text
数据库表清单
关键 SQL Constraint
核心索引设计
Flyway Migration 顺序
```
