# IoT-Security-Case-Hub
## Implementation Plan V1.1

> 本计划替代 `Implementation Plan V1.0`。
>
> V1.1 已按照最终冻结架构更新：
>
> - Java 21
> - Spring Boot 3.x
> - React + TypeScript + Vite
> - PostgreSQL 16
> - Flyway
> - Spring Security Session + CSRF
> - Ant Design
> - TanStack Query
> - Docker Compose
>
> 实施必须遵循 `Final Technical Review V1.0`。

---

# 1. 开发原则

```text
先基础设施
再核心模型
再业务流程
最后复杂 Progressive
```

每个 Phase 必须：

```text
代码
Migration
Test
Acceptance
```

一起完成。

禁止：

```text
先把所有表写完但业务不可运行
```

推荐持续形成可工作的纵向切片。

---

# 2. Phase 0：Repository 与工程骨架

创建：

```text
backend/
frontend/
deploy/
docs/
```

Backend：

```text
Java 21
Spring Boot 3.x
Maven
```

Frontend：

```text
React
TypeScript
Vite
Ant Design
```

验收：

```text
mvn test
npm build
均成功
```

---

# 3. Phase 1：Docker / PostgreSQL / Health

实现：

```text
docker-compose
PostgreSQL 16
Backend
Nginx dev baseline
```

Flyway：

```text
V001 schema + pg_trgm
```

实现：

```text
/actuator/health
```

验收：

```text
docker compose up
→ DB healthy
→ Backend healthy
```

---

# 4. Phase 2：Identity / RBAC

Flyway：

```text
users
roles
permissions
user_roles
role_permissions
must_change_password
```

实现：

```text
Spring Security
BCrypt
Session
CSRF
Login
Logout
/me
/csrf
change-password
```

Seed：

```text
ADMIN
TEST_COORDINATOR
TESTER
Permissions
```

测试：

```text
Authentication
CSRF
Disabled User
Permission
```

---

# 5. Phase 3：Frontend Shell / Auth

实现：

```text
AppLayout
Login
Sidebar
Route Guard
Axios
TanStack Query
Current User
PermissionGuard
```

验收：

```text
不同角色登录后看到不同菜单
```

---

# 6. Phase 4：基础字典

实现：

```text
Standard / Task Type
Category
Tag
Tool
```

Admin CRUD。

Library Read。

测试：

```text
RBAC
Category two-level
Unique Code
```

---

# 7. Phase 5：Capability Library

实现：

```text
Capability Tree
Admin CRUD
Cycle Validation
```

Frontend：

```text
Capability Tree Admin
```

测试：

```text
Tree Cycle
Parent Relation
```

---

# 8. Phase 6：Master Test Case 基础

实现：

```text
MasterTestCase
TestCaseVersion
TestStep
Tags
Tools
Standard Mapping
Attachments
```

先不做 DAG。

实现：

```text
Create Draft
Edit Draft
Library Search
Version History
```

---

# 9. Phase 7：Test Case Lifecycle

实现：

```text
Draft
Review
Publish
Return
Reject
Deprecated
Current Version
Revision Contributor
```

测试：

```text
Published Immutable
Current Version Unique
Reject Closed
Contributor Permission
```

---

# 10. Phase 8：Decision Point / DAG

实现：

```text
DecisionPoint
Transition
TransitionTarget
```

Service：

```text
DagValidationService
```

Frontend：

```text
DecisionPointEditor
Master Logic Graph
```

测试：

```text
Target Count
Cycle Detection
SINGLE/MULTIPLE
```

---

# 11. Phase 9：Project Core

实现：

```text
Project
Project Standard
Coordinator
Status
```

Frontend：

```text
Project List
Create
Overview
```

测试：

```text
Project RBAC
Status
Primary Coordinator
```

---

# 12. Phase 10：Project Capability

实现：

```text
ProjectCapability
YES / NO / UNKNOWN
Source
Derived
```

Engine：

```text
CapabilityEngine
```

Frontend：

```text
Project Capability Tree
```

测试：

```text
UNKNOWN
Child YES
Derived Parent
Parent NO
```

---

# 13. Phase 11：Generation Rule Admin

实现：

```text
GenerationRule
ConditionGroup
Condition
Outputs
```

Engine components：

```text
ConditionEvaluator
GroupEvaluator
```

Frontend：

```text
Generation Rule Editor
```

测试：

```text
AND
OR
Operators
One-level group
```

---

# 14. Phase 12：Generation Runtime

实现：

```text
GenerationRun
Recommendation
Recommended Because
Ignore Preference
```

Engine：

```text
GenerationEngine
```

Frontend：

```text
Project Generation Page
Recommendation Table
```

测试：

```text
Multi Rule Dedupe
Ignored
Mode
ENTRY
```

---

# 15. Phase 13：Project Test Plan

实现：

```text
ProjectTestCase
Sources
Assignees
Removed
Restore
Version Binding
```

Service：

```text
ProjectTestPlanService
```

Frontend：

```text
Project Test Plan Table
Filters
Bulk Assign
```

约束：

```text
Assignee must have TESTER role
Project + Master unique
```

---

# 16. Phase 14：My Tests

实现：

```text
My Projects
My Cases
All Project Cases
NEW
firstViewedAt
```

Frontend：

```text
Project Cards
Tabs
NEW Badge
Floating placeholder
```

---

# 17. Phase 15：Evidence / Storage

实现：

```text
StorageService
LocalStorageService
Temp
Final
Trash
SHA-256
Evidence
Attachment
```

测试：

```text
Path Traversal
Authorization
Compensation
Trash Restore
Missing Object
```

---

# 18. Phase 16：Notes

实现：

```text
Note
Create
Update Own
Delete Own
```

Frontend：

```text
Notes List
Author Prefix
```

---

# 19. Phase 17：Basic Execution

实现：

```text
NOT_STARTED
IN_PROGRESS
COMPLETED
REOPEN
Decision Selection
Branch Outcome
```

先完成：

```text
PASS
FAIL
N_A
```

不立即写 NEXT_CASE。

测试：

```text
Assignee-only execution
Evidence Required
Selection Mode
```

---

# 20. Phase 18：Progressive Runtime

实现：

```text
PESSIMISTIC_WRITE PTC lock
NEXT_CASE
NEXT_CASES
Target Resolver
Trigger
Assignee Union
Source PROGRESSIVE
```

核心：

```text
ProgressiveRuntimeService
```

测试：

```text
Reuse
Concurrent Create
Multi predecessor
Completed Target
```

---

# 21. Phase 19：Floating / Relation Update

实现：

```text
CONNECTED
FLOATING
isRoot
active Trigger
```

实现：

```text
KEEP_ORIGINAL_TARGET
USE_EXISTING_TARGET
ADD_TARGET
DETACH_TARGET
```

Frontend：

```text
Warning Modal
Floating Badge
```

---

# 22. Phase 20：Project Logic Graph

实现：

```text
Full Graph
Current Execution Path
```

Frontend：

```text
React Flow
Fit View
Current Node
Floating
Triggered By
```

---

# 23. Phase 21：Project Custom Test Case

实现：

```text
Custom Case
Custom Steps
Custom Decision Points
Custom Transitions
```

权限：

```text
Coordinator + Tester create
Tester auto self-assignee
```

支持：

```text
Progressive Target
Submit to Library
```

---

# 24. Phase 22：Capability Update Request

实现：

```text
Submit
Approve
Reject
```

Approve：

```text
Capability Update
Derived Recalc
Generation Run
Recommendations
```

不自动 Add。

---

# 25. Phase 23：Test Case Change Request

实现：

```text
Submit
Approve
Reject
Create Revision
Contributor
```

完整打通：

```text
Tester → Coordinator → Draft → Admin Publish
```

---

# 26. Phase 24：Version Upgrade

实现：

```text
New Version Available
Diff
Keep
Upgrade
Decision Point Warning
```

测试：

```text
Same PTC ID
Execution data preserved
```

---

# 27. Phase 25：Excel Export

实现：

```text
Apache POI SXSSF
```

Workbook：

```text
Project Summary
Test Cases
Evidence Index
```

---

# 28. Phase 26：Audit

实现：

```text
AuditService
Admin Audit Page
```

覆盖：

```text
Login
Role Change
Project Create/Archive
Publish/Deprecated
Rule Update
Capability Library Update
Evidence Delete
```

---

# 29. Phase 27：Production Security

完成：

```text
Security Headers
Production CORS
Nginx HTTPS
Login Rate Limit
Session Registry
Secret Configuration
Actuator Restriction
```

---

# 30. Phase 28：Deployment / Backup

完成：

```text
Production Docker Compose
Nginx Image
Backend Image
Postgres Volume
File Volume
backup.sh
restore.sh
health-check.sh
```

执行：

```text
Fresh Install
Backup
Restore
Upgrade
Rollback
```

真实验证。

---

# 31. Phase 29：E2E Acceptance

Playwright：

```text
Full Profile
Progressive Bluetooth
Multi-predecessor
Floating
Capability Request
Change Request
Version Upgrade
Permission
```

---

# 32. Phase 30：V1 Release Review

检查：

```text
No TODO blocking
No default password
No secret in Git
All Flyway pass
All backend tests pass
Frontend build pass
E2E pass
Backup restore pass
```

然后：

```text
V1 Release Candidate
```

---

# 33. 首个开发 Sprint

建议第一轮只做到：

```text
Phase 0
Phase 1
Phase 2
Phase 3
```

结果必须是一个真正可运行的系统：

```text
Browser
↓
Nginx/Frontend
↓
Login
↓
Spring Boot
↓
PostgreSQL
```

可以：

```text
Admin Login
Tester Login
Coordinator Login
Logout
CSRF
RBAC
```

---

# 34. 开发顺序冻结

正式顺序：

```text
Skeleton
↓
Infrastructure
↓
Auth/RBAC
↓
Frontend Shell
↓
Dictionary
↓
Capability
↓
Master Test Case
↓
Lifecycle
↓
DAG
↓
Project
↓
Generation
↓
Project Test Plan
↓
Execution
↓
Progressive
↓
Change Workflow
↓
Excel
↓
Production
↓
E2E
```

不要跨越核心依赖随意实现。

---

# 35. Definition of Done

每个 Phase 的 Done 至少要求：

```text
Code Complete
Migration Complete
Backend Test Complete
Frontend Test where applicable
Permission Checked
API Works
Documentation updated if contract changed
```

---

# 36. Final Implementation Status

设计状态：

```text
READY
```

开发入口：

```text
Phase 0
Repository & Project Skeleton
```
