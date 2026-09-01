# IoT-Security-Case-Hub
## Testing Strategy V1.0

> 基于：
>
> - `IoT-Security-Case-Hub_System-Design_V0.6.md`
> - `IoT-Security-Case-Hub_Technical-Architecture_V1.0.md`
> - `IoT-Security-Case-Hub_Data-Model_V1.0.md`
> - `IoT-Security-Case-Hub_Database-Schema_V1.0.md`
> - `IoT-Security-Case-Hub_Backend-Architecture_V1.0.md`
> - `IoT-Security-Case-Hub_API-Design_V1.0.md`
> - `IoT-Security-Case-Hub_Frontend-Architecture_V1.0.md`
> - `IoT-Security-Case-Hub_File-Storage-Security_V1.0.md`
> - `IoT-Security-Case-Hub_Security-RBAC-Detail_V1.0.md`
>
> 本文档冻结 V1 测试策略，包括：
>
> - Backend Unit Test
> - Repository Test
> - Service Integration Test
> - API Test
> - Security Test
> - Generation Rule Test
> - Progressive DAG Test
> - File Storage Test
> - Frontend Component Test
> - Frontend Integration Test
> - Playwright E2E
> - Testcontainers
> - Test Data
> - Coverage
> - CI Quality Gate
> - Acceptance Scenarios
>
> 目标不是追求机械的 100% Coverage，而是优先保证核心业务规则、权限和数据一致性不会被回归破坏。

---

# 1. 测试总体原则

V1 测试遵循：

```text
核心业务规则
>
权限
>
数据一致性
>
API
>
UI 交互
>
视觉细节
```

优先保证：

```text
系统不会生成错误 Test Case
不会重复创建 Progressive Target
不会越权
不会破坏历史版本
不会丢失 Evidence / Notes
不会产生错误 DAG
```

---

# 2. Test Pyramid

推荐：

```text
             E2E
            /   \
       Integration
       /         \
  Service       API
     \           /
       Unit Test
```

数量原则：

```text
Unit Test 最多
Integration Test 次之
E2E 最少但覆盖核心路径
```

---

# 3. Backend 测试技术栈

正式采用：

```text
JUnit 5
Mockito
AssertJ
Spring Boot Test
MockMvc
Testcontainers
PostgreSQL Container
```

---

# 4. Frontend 测试技术栈

正式采用：

```text
Vitest
React Testing Library
MSW optional
Playwright
```

---

# 5. 为什么不使用 H2 作为数据库替代

正式数据库：

```text
PostgreSQL
```

系统依赖：

```text
UUID
TIMESTAMPTZ
Partial Unique Index
pg_trgm
CTE
Native SQL
Check Constraint
```

因此核心 Repository / Integration Test：

> 必须使用真实 PostgreSQL Testcontainer。

不使用：

```text
H2
```

替代关键数据库测试。

---

# 6. Testcontainers

建议创建统一测试基类：

```text
PostgresIntegrationTestBase
```

启动：

```text
PostgreSQL Container
```

测试时自动执行：

```text
Flyway Migration
```

从而验证：

```text
Migration 本身
+
Repository
+
Constraint
```

---

# 7. 测试环境数据库

每次 Integration Test：

```text
独立 Test Database
```

不得连接：

```text
Development DB
Production DB
```

---

# 8. Flyway Migration Test

CI 必须至少完成一次：

```text
Empty PostgreSQL
↓
Flyway migrate
↓
All migrations success
```

确保：

```text
全新环境可以从零部署
```

---

# 9. Backend Unit Test 范围

Unit Test 优先测试：

```text
纯业务逻辑
不依赖 Spring Context
不依赖 Database
```

---

# 10. CapabilityEngine Unit Test

必须覆盖：

```text
Row 不存在 → UNKNOWN

Parent YES
→ Child 不自动 YES

Child YES
→ Parent Derived YES

多层 Child YES
→ 所有 Parent Derived YES

Parent NO
→ Child Effective Value 不适用

Direct YES 覆盖 Derived 状态处理

重新修改 Child
→ Derived Parent 重新计算
```

---

# 11. Capability UNKNOWN Test

重点：

```text
UNKNOWN != NO
UNKNOWN != YES
```

例如：

```text
Pairing != NO
```

应匹配：

```text
YES
UNKNOWN
```

不匹配：

```text
NO
```

---

# 12. GenerationConditionEvaluator Unit Test

必须覆盖：

```text
EQ_YES
EQ_NO
EQ_UNKNOWN
NE_NO
NE_YES
PRESENT
```

每个 Operator：

```text
Positive
Negative
UNKNOWN
```

都应有测试。

---

# 13. GenerationGroupEvaluator

必须覆盖：

```text
AND
OR
Root Group
Child Group
```

示例：

```text
A AND B
A OR B
A AND (B OR C)
A OR (B AND C)
```

V1 不允许更深嵌套。

---

# 14. Generation Mode Test

必须覆盖：

```text
FULL
PROGRESSIVE_INITIAL
BOTH
```

---

# 15. Progressive Role Generation

Progressive Initial：

```text
ENTRY
→ 可以被推荐
```

NORMAL：

```text
→ 默认不作为 Progressive Initial 推荐
```

除非 Manual Add。

---

# 16. Multi Rule Deduplication

例如：

```text
Rule A → TC-001
Rule B → TC-001
Rule C → TC-002
```

预期：

```text
Recommendations:
TC-001
TC-002
```

TC-001：

```text
Recommended Because:
Rule A
Rule B
```

不能重复出现两条 TC-001。

---

# 17. Ignore Test

同 Project：

```text
TC-001 → IGNORED
```

重新 Generation：

```text
TC-001 不重新显示 NEW
```

其它 Project：

```text
不受影响
```

---

# 18. Generation Rule Disabled Test

Rule：

```text
DISABLED
```

不能参与新的 Generation Run。

已经生成的 Project Test Case：

```text
保持不变
```

---

# 19. DagValidationService Unit Test

必须覆盖：

```text
A → B → C
Valid

A → A
Invalid

A → B → A
Invalid

A → B → C → A
Invalid

多个分支但无环
Valid
```

---

# 20. Transition Target Count Test

必须覆盖：

```text
PASS + 0 Target
Valid

PASS + 1 Target
Invalid

FAIL + 0
Valid

N_A + 0
Valid

NEXT_CASE + 1
Valid

NEXT_CASE + 0
Invalid

NEXT_CASE + 2
Invalid

NEXT_CASES + 1
Valid

NEXT_CASES + N
Valid
```

---

# 21. Selection Mode Test

SINGLE：

```text
0 selections → Reject
1 selection → Accept
2 selections → Reject
```

MULTIPLE：

```text
0 → Reject
1 → Accept
N → Accept
```

---

# 22. Evidence Requirement Test

如果：

```text
evidenceRequired = true
```

Evidence Count：

```text
0 → Complete Reject
>=1 → Accept
```

如果 false：

```text
0 → Accept
```

---

# 23. ProgressiveRuntimeService Integration Test

这是后端最高优先级测试之一。

---

# 24. NEXT_CASE Test

```text
TC-A
DP-A → NEXT_CASE TC-B
```

Complete TC-A 后：

```text
TC-A = COMPLETED
TC-B created
Source contains PROGRESSIVE
Trigger created
Assignees inherited
TC-B CONNECTED
```

---

# 25. NEXT_CASES Test

```text
TC-A
DP → NEXT_CASES
  ├─ TC-B
  └─ TC-C
```

预期：

```text
B created
C created
2 Trigger edges
Assignees inherited
```

---

# 26. MULTIPLE Branch Test

```text
Selection Mode = MULTIPLE

DP-A → TC-B
DP-B → FAIL
```

预期：

```text
Branch A = NEXT_CASE
Branch B = FAIL
TC-B activated
TC-A COMPLETED
```

ProjectTestCase 不产生单一：

```text
result = FAIL
```

---

# 27. PASS Test

Decision：

```text
PASS
```

预期：

```text
BranchOutcome = PASS
No target created
```

---

# 28. FAIL Test

```text
FAIL
```

预期：

```text
BranchOutcome = FAIL
No target created
```

只结束：

```text
当前 Branch
```

---

# 29. N/A Test

```text
N_A
```

预期：

```text
No target
Branch terminal
```

---

# 30. Existing Target Reuse Test

项目中已经存在：

```text
TC-B
```

TC-A 再触发 B：

```text
不能创建第二个 B
```

预期：

```text
reuse existing PTC
new Trigger
Source PROGRESSIVE added
```

---

# 31. Concurrent Target Creation Test

模拟两个事务同时：

```text
Trigger TC-B
```

必须验证：

```text
Unique Constraint
+
Service Retry / Re-query
```

最终：

```text
仅一个 TC-B
```

不能产生：

```text
500
```

---

# 32. Multiple Predecessor Test

```text
TC-A → TC-X
TC-B → TC-X
```

预期：

```text
仅一个 TC-X
Triggered By = 2
```

---

# 33. Assignee Union Test

```text
TC-A Assignee = A,B
TC-B Assignee = C
```

共同触发：

```text
TC-X Assignees = A,B,C
```

无重复。

---

# 34. Completed Target Trigger Test

目标：

```text
TC-X = COMPLETED
```

再次触发：

```text
TC-X 仍然 COMPLETED
```

增加：

```text
Trigger
Assignee Union
```

---

# 35. Floating Test

原：

```text
A → B → C
```

Detach：

```text
A → B
```

后：

```text
B FLOATING
C 仍保留
```

如果：

```text
B → C
```

仍 active，则 C 的关系由自身 Incoming Trigger 决定。

---

# 36. Reconnect Floating Test

B：

```text
FLOATING
```

新有效 Incoming Trigger 创建后：

```text
B → CONNECTED
```

---

# 37. Root Test

Root：

```text
无 Incoming Trigger
```

仍：

```text
CONNECTED
```

---

# 38. Removed Test

Removed：

```text
executionStatus 保持
Evidence 保持
Notes 保持
```

默认项目任务列表隐藏。

---

# 39. Restore Test

Removed → Restore：

```text
Evidence
Notes
Selections
Branch Outcome
Execution Status
```

全部保留。

---

# 40. Project Completion Test

存在：

```text
未 Removed + NOT_STARTED
```

Project：

```text
不能 Completed
```

全部未 Removed Case：

```text
COMPLETED
```

则：

```text
可 Completed
```

Floating：

```text
同样参与判断
```

Removed：

```text
不参与判断
```

---

# 41. Version Binding Test

ProjectTestCase：

```text
TC-001 v1.2
```

发布：

```text
v1.3
```

预期：

```text
ProjectTestCase 仍绑定 v1.2
```

直到 Coordinator Upgrade。

---

# 42. Version Upgrade Test

Upgrade：

```text
v1.2 → v1.3
```

预期：

```text
同一个 PTC ID
Evidence 保留
Notes 保留
Assignees 保留
ExecutionStatus 保留
```

---

# 43. Decision Point Change Upgrade Test

如果：

```text
Decision Points changed
```

未确认：

```text
Upgrade Reject
```

明确确认：

```text
Upgrade Allowed
```

---

# 44. Publish Test

必须测试：

```text
DRAFT → Submit → REVIEW
REVIEW → Publish
```

Publish：

```text
旧 Current = false
新 Current = true
```

---

# 45. Published Immutable Test

Published Version：

```text
Update Draft API
```

必须：

```text
Reject
```

即使：

```text
Admin
```

也不能原地修改。

---

# 46. Current Version Unique Test

同时设置两个：

```text
is_current_version = true
```

数据库必须拒绝。

---

# 47. Change Request Test

Tester：

```text
Submit CR
```

Coordinator：

```text
Approve
```

预期：

```text
Revision Draft created
basedOnVersion set
Tester Contributor added
```

---

# 48. Capability Update Request Test

Tester：

```text
Submit
```

Coordinator Approve：

```text
Capability updated
Derived recalculated
GenerationRun created
Recommendations generated
```

不能：

```text
自动 Add Project Test Case
```

---

# 49. Repository Test

重点验证数据库行为。

---

# 50. ProjectTestCase Unique

同 Project：

```text
Master TC-001
```

插入两次：

```text
数据库拒绝
```

不同 Project：

```text
允许
```

---

# 51. Assignee Unique

同：

```text
PTC + User
```

两次插入：

```text
拒绝
```

---

# 52. Source Unique

```text
PTC + PROGRESSIVE
```

两次：

```text
拒绝
```

---

# 53. Trigger Unique

相同：

```text
BranchOutcome + Target
```

重复：

```text
拒绝
```

---

# 54. Master / Custom XOR

ProjectTestCase：

```text
Master and Custom both set
```

拒绝。

两个都 null：

```text
拒绝
```

只 Master：

```text
允许
```

只 Custom：

```text
允许
```

---

# 55. Project Capability Unique

```text
Project + Capability
```

只能一行。

---

# 56. Primary Coordinator Unique

同 Project：

```text
最多一个 isPrimary=true
```

---

# 57. Search Repository Test

至少测试：

```text
Case Code
Case Name
Test Purpose
Step
Tag
Tool
```

查询能命中。

---

# 58. Security Test

属于 V1 最高优先级测试。

---

# 59. Unauthenticated

所有：

```text
/api/v1/**
```

除 Login / 必要初始化接口外：

```text
401
```

---

# 60. Tester Publish

Tester：

```text
POST /publish
```

预期：

```text
403
```

---

# 61. Coordinator Publish

Coordinator：

```text
403
```

Admin：

```text
Allowed
```

---

# 62. Tester Project Edit

Tester：

```text
PUT /projects/{id}
```

预期：

```text
403
```

---

# 63. Tester Assignee Update

预期：

```text
403
```

---

# 64. Tester Execute Assigned Case

Assigned：

```text
Allowed
```

---

# 65. Tester Execute Unassigned Case

同 Project 但未 Assigned：

```text
Read Allowed
Write Denied
```

---

# 66. Evidence Cross Project Download

Tester A：

```text
Project A member
```

尝试下载：

```text
Project B Evidence
```

必须：

```text
403 / 404
```

---

# 67. Note Ownership Test

Tester A Note：

Tester B：

```text
PUT
DELETE
```

必须拒绝。

---

# 68. Draft Contributor Test

Tester A 是：

```text
Draft X Contributor
```

可编辑 X。

Draft Y：

```text
不可编辑
```

---

# 69. CSRF Test

Cookie Session 存在。

状态修改请求：

```text
无 CSRF Token
```

必须失败。

---

# 70. Disabled User Test

用户登录后被 Admin Disable。

后续请求：

```text
拒绝
```

---

# 71. Login Failure Test

错误密码连续：

```text
5 次
```

触发：

```text
Temporary Block
```

过期后：

```text
可再次尝试
```

---

# 72. File Storage Unit Test

必须测试：

```text
Save
Read
Delete
Exists
SHA-256
Temp File
Atomic Move
```

---

# 73. Path Traversal Test

上传：

```text
../../etc/passwd
..\..\windows\system32
/absolute/path
```

实际 Storage：

```text
仍使用 UUID Key
```

不能逃离：

```text
Storage Root
```

---

# 74. Unicode Filename Test

例如：

```text
蓝牙测试证据 01.pcap
```

上传 / 下载：

```text
文件名正常显示
```

---

# 75. 500MB File Test

不需要 CI 每次真上传 500MB。

可通过：

```text
模拟 InputStream / configurable test limit
```

验证：

```text
size limit
stream behavior
```

---

# 76. DB Failure Compensation Test

文件已写。

模拟：

```text
DB insert throws
```

预期：

```text
file removed
```

---

# 77. Missing Storage File Test

DB Row 存在。

实际文件删除。

Download：

```text
STORAGE_OBJECT_MISSING
```

---

# 78. Frontend Unit / Component Test

重点针对复杂业务组件。

---

# 79. StatusBadge Test

确保：

```text
状态文字存在
```

不只是颜色。

---

# 80. DecisionPointEditor Test

必须测试：

```text
SINGLE / MULTIPLE 展示
PASS 不显示 Target
NEXT_CASE 显示一个 Target
NEXT_CASES 多 Target
```

---

# 81. Generation Condition Builder Test

必须测试：

```text
Capability Operators
Standard PRESENT
Root Group
Child Group
禁止第三层 Group
```

---

# 82. Capability Tree Test

必须测试：

```text
YES
NO
UNKNOWN
Derived
Read Only
Edit Mode
```

---

# 83. PermissionGuard Test

没有 Permission：

```text
Button hidden
```

有 Permission：

```text
Button visible
```

但该测试不替代 Backend Security Test。

---

# 84. Execution Page Test

测试：

```text
Evidence
Notes
Decision Point
Complete Button
SINGLE Radio
MULTIPLE Checkbox
```

---

# 85. Floating UI Test

FLOATING：

```text
明确文字 / 图标
```

---

# 86. Removed UI Test

Removed：

```text
默认列表不显示
Show Removed 后显示
```

---

# 87. Query Cache Test

关键 Mutation 后：

```text
相关 Query 被 invalidate
```

例如 Complete：

```text
Current PTC
My Tests
Project Test Plan
Logic Graph
```

---

# 88. Frontend Integration Test

可以使用：

```text
MSW
```

模拟 API。

重点：

```text
Project Create Form
Generation Recommendation Page
Execution Complete
Change Request
Version Upgrade Warning
```

---

# 89. E2E

正式采用：

```text
Playwright
```

---

# 90. E2E 环境

使用：

```text
Frontend
Backend
PostgreSQL
Storage
```

完整 Docker Compose 测试环境。

---

# 91. E2E Account

Seed：

```text
Admin
Coordinator
Tester A
Tester B
```

---

# 92. E2E Scenario A：Full Profile

```text
Coordinator Login
↓
Create Project
↓
Fill Capabilities
↓
Run FULL Generation
↓
Add Recommendations
↓
Assign Tester
↓
Tester Login
↓
Execute Test Case
↓
Upload Evidence
↓
Complete
↓
Export Excel
```

---

# 93. E2E Scenario B：Progressive Bluetooth

```text
Bluetooth = YES
BLE = UNKNOWN
BR/EDR = UNKNOWN
↓
PROGRESSIVE_INITIAL
↓
ENTRY Test Case
↓
Tester selects BLE + BR/EDR
↓
Complete
↓
Two branches activated
↓
Both appear in My Tests
```

---

# 94. E2E Scenario C：Multi-predecessor Merge

```text
A → X
B → X
```

完成 A / B。

验证：

```text
X 只有一个
Triggered By = 2
Assignees Union
```

---

# 95. E2E Scenario D：Floating

```text
A → B → C
↓
Reopen A
↓
Detach B
↓
B Floating
↓
B / C 数据仍存在
↓
Reconnect
↓
B Connected
```

---

# 96. E2E Scenario E：Capability Request

```text
Tester
↓
Submit Capability Update
↓
Coordinator Approve
↓
New Recommendations
```

---

# 97. E2E Scenario F：Change Request

```text
Tester Submit Test Case CR
↓
Coordinator Approve
↓
Draft Revision
↓
Tester Contributor Edit
↓
Coordinator Submit Review
↓
Admin Publish
```

---

# 98. E2E Scenario G：Version Upgrade

```text
Project uses v1.2
↓
Publish v1.3
↓
Project shows New Version
↓
View Diff
↓
Upgrade
↓
Execution data preserved
```

---

# 99. E2E Scenario H：Permission

Tester：

```text
尝试修改 Project
尝试 Publish
尝试修改 Assignee
```

全部失败。

---

# 100. Test Data Strategy

不要依赖：

```text
开发数据库现有数据
```

测试数据必须：

```text
程序化 Seed
Fixture
Factory
```

---

# 101. Backend Test Factory

建议：

```text
TestDataFactory
```

提供：

```text
createUser()
createProject()
createMasterTestCase()
createPublishedVersion()
createProjectTestCase()
```

减少大量重复测试代码。

---

# 102. Frontend Fixture

Playwright：

```text
通过 API
```

准备数据优先于 UI 手工逐步构造所有前置数据。

核心业务流程本身再通过 UI 测。

---

# 103. Test Isolation

每个测试必须：

```text
可独立运行
```

不能依赖：

```text
测试执行顺序
```

---

# 104. 时间测试

后端已建议注入：

```text
Clock
```

测试：

```text
NEW
Session-related domain time
PublishedAt
CompletedAt
```

时可固定时间。

---

# 105. Coverage

不追求：

```text
100%
```

推荐总体：

```text
Backend Line Coverage >= 70%
```

但核心 Engine：

```text
CapabilityEngine
GenerationEngine / Evaluator
DagValidationService
ProgressiveRuntimeService
Authorization
```

应达到：

```text
接近 90% 业务分支覆盖
```

---

# 106. Coverage 的真正指标

更关注：

```text
Branch Coverage
Critical Path Coverage
```

而不是只看：

```text
Line Coverage
```

---

# 107. Frontend Coverage

不设置过高全局门槛。

建议：

```text
核心共享组件
复杂表单
权限组件
```

优先覆盖。

---

# 108. CI Pipeline

Pull Request 至少执行：

```text
Backend compile
Backend unit test
Backend integration test
Flyway migration test
Frontend typecheck
Frontend lint
Frontend unit test
Frontend build
```

---

# 109. Main Branch CI

Main 增加：

```text
Playwright E2E
```

如果执行时间可接受，也可在 PR 对核心 E2E 执行。

---

# 110. Backend Quality Gate

必须：

```text
mvn test
成功
```

禁止：

```text
skipTests
```

合并。

---

# 111. Frontend Quality Gate

必须：

```text
TypeScript strict check
ESLint
Vitest
Vite build
```

全部成功。

---

# 112. Migration Quality Gate

必须验证：

```text
空数据库 migrate success
```

同时建议验证：

```text
重复启动 migration no-op
```

---

# 113. Static Analysis

Java 推荐后续加入：

```text
SpotBugs
Checkstyle
```

或：

```text
SonarQube
```

如果公司已有。

V1 不强制同时上全部工具。

---

# 114. Dependency Scan

建议 CI：

```text
OWASP Dependency-Check
npm audit
```

作为：

```text
warning / scheduled check
```

初期不建议因所有中低风险传递依赖自动阻塞所有开发。

---

# 115. Security Regression Set

每次权限模型修改：

必须运行：

```text
Security Test Suite
```

---

# 116. Progressive Regression Set

每次修改：

```text
Execution
Decision Point
ProjectTestCase
Assignee
Trigger
```

必须运行：

```text
Progressive Runtime Suite
```

---

# 117. Generation Regression Set

每次修改：

```text
Capability
Generation Rule
Standard
Recommendation
```

必须运行：

```text
Generation Suite
```

---

# 118. Bug Fix Rule

任何生产/测试环境发现的 Bug：

```text
先增加能复现 Bug 的 Test
↓
确认 Test Fail
↓
修复
↓
确认 Test Pass
```

防止同类问题再次出现。

---

# 119. Test Naming

Java 推荐：

```text
method_condition_expected
```

例如：

```text
complete_multipleSelections_createsMultipleBranchOutcomes()
```

或者使用：

```text
@DisplayName
```

写清业务语义。

---

# 120. Test Package

测试目录跟随生产模块：

```text
src/test/java/.../
├─ capability/
├─ generation/
├─ execution/
├─ testcase/
├─ project/
└─ security/
```

---

# 121. Smoke Test

部署后至少自动检查：

```text
/actuator/health
Login
Project List
Test Case Search
```

---

# 122. Backup Restore Test

虽然属于 Deployment，但 Testing Strategy 要求：

> Backup 不能只“生成成功”，必须定期进行 Restore Test。

至少验证：

```text
Database Restore
Evidence Restore
SHA-256
```

---

# 123. Performance Test

V1 不做大规模性能工程。

但至少做基础压力验证：

```text
Test Case Search
Project Test Plan List
My Tests
Logic Graph
Generation Run
```

---

# 124. 基础性能目标

内网 V1 推荐目标：

```text
普通列表 / 详情：
P95 < 1 秒

复杂 Generation：
常规项目 < 3 秒

Project Logic Graph：
常规规模 < 2 秒
```

不作为绝对 SLA，但用于发现明显性能问题。

---

# 125. Large Dataset Test

准备一套：

```text
5000 Master Test Cases
300 PTC / Project
多 Rule
多 Tags
```

验证：

```text
Search
Table
Generation
```

不会明显失控。

---

# 126. File Large Stream Test

验证：

```text
大 Evidence Download
```

不会把全部文件读入 JVM Heap。

---

# 127. Test Failure Artifacts

CI E2E 失败时保存：

```text
Screenshot
Playwright Trace
Browser Log
Backend Log
```

便于排查。

---

# 128. Flaky Test

不允许长期：

```text
retry until pass
```

掩盖 Flaky Test。

Playwright 可有限 retry，但出现重复 flaky 必须修。

---

# 129. Manual Acceptance Test

正式 V1 发布前仍需要一次人工验收。

重点：

```text
UI 易用性
表格
Draft Editor
Decision Point
Logic Graph
Evidence
My Tests
```

自动测试不能完全替代使用体验。

---

# 130. V1 Release Acceptance Checklist

必须通过：

```text
Login / Logout
RBAC
Project Lifecycle
Capability
Generation
Test Plan
Assignment
Execution
Progressive
Evidence
Notes
Floating
Removed / Restore
Custom Test Case
Change Request
Publish
Version Upgrade
Excel Export
Backup Restore
```

---

# 131. Testing Strategy V1.0 最终冻结

正式采用：

```text
Backend:
JUnit 5
Mockito
AssertJ
Spring Boot Test
MockMvc
Testcontainers PostgreSQL

Frontend:
Vitest
React Testing Library
Playwright
```

最高优先级回归：

```text
Capability Engine
Generation Rule
Progressive Runtime
DAG
Version Binding
RBAC
Database Constraints
File Authorization
```

CI 必须保证：

```text
Compile
Migration
Unit Test
Integration Test
Typecheck
Lint
Frontend Test
Build
```

Main / Release：

```text
Playwright E2E
```

测试目标：

> **保护业务正确性，而不是追求表面 Coverage 数字。**

---

# 132. 下一阶段

下一份文档：

```text
IoT-Security-Case-Hub_Deployment-Backup_V1.0.md
```

将冻结：

```text
Docker Compose
Nginx
Frontend
Spring Boot
PostgreSQL
Persistent Volume
HTTPS
Environment Variables
Production Profile
Backup
Restore
Retention
Logging
Health Check
Upgrade
Rollback
Storage Monitoring
```

完成后只剩：

```text
Final Technical Review V1.0
```

然后正式进入编码阶段。
