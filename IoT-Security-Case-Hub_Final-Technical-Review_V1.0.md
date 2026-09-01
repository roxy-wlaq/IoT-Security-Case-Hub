# IoT-Security-Case-Hub
## Final Technical Review V1.0

> 本文档是正式编码前的最终技术审查结果。
>
> 审查对象：
>
> - System Design V0.6
> - Technical Architecture V1.0
> - Data Model V1.0
> - Database Schema V1.0
> - Backend Architecture V1.0
> - API Design V1.0
> - Frontend Architecture V1.0
> - File Storage & Security V1.0
> - Security & RBAC Detail V1.0
> - Testing Strategy V1.0
> - Deployment & Backup V1.0
>
> 目标：
>
> - 查找跨文档冲突
> - 查找遗漏
> - 消除实现歧义
> - 明确最终技术基线
> - 冻结正式开发前的设计

---

# 1. Review 结论

最终结论：

```text
PASS
```

当前设计已经具备正式进入编码阶段的条件。

未发现需要重新设计产品模型或整体架构的阻塞问题。

审查发现若干跨文档细节需要统一，已在本文中给出最终裁决。

本文中的“最终裁决”优先于此前文档中的冲突描述。

---

# 2. 文档优先级

正式开发时按以下优先级理解：

```text
1. Final Technical Review V1.0
2. System Design V0.6
3. Security / Storage / Testing / Deployment Detail
4. API / Backend / Frontend Architecture
5. Database Schema V1.0
6. Data Model V1.0
7. Technical Architecture V1.0
8. 历史 V0.1 / V0.2 / V0.3 / V0.4 / V0.5 文档
```

历史 V0.1 文档：

```text
Archived / Superseded
```

不得作为新代码实现依据。

---

# 3. 最终技术栈

正式冻结：

```text
Architecture
Modular Monolith

Frontend
React + TypeScript + Vite

UI
Ant Design

Frontend Server State
TanStack Query

Frontend Form
React Hook Form + Zod

Graph
React Flow

Backend
Java 21 LTS

Backend Framework
Spring Boot 3.x

ORM
Spring Data JPA + Hibernate

Migration
Flyway

Database
PostgreSQL 16

Search
pg_trgm + PostgreSQL Query

Security
Spring Security
Server-side HTTP Session
CSRF

File Storage
Local Persistent Storage
+
StorageService Abstraction

Excel
Apache POI

Reverse Proxy
Nginx

Deployment
Docker Compose

Testing
JUnit 5
Testcontainers PostgreSQL
Vitest
React Testing Library
Playwright
```

---

# 4. 最终生产 Container

此前 Technical Architecture 中曾描述：

```text
nginx
frontend
backend
postgres
```

Deployment 文档后续进行了简化。

最终生产部署：

```text
nginx
backend
postgres
```

React：

```text
Vite Build
↓
dist
↓
复制到 Nginx Image
```

不运行独立 Frontend Application Server。

---

# 5. Authentication 最终裁决

最终采用：

```text
Server-side HTTP Session
```

而不是：

```text
JWT
```

Session Cookie：

```text
JSESSIONID

HttpOnly = true
Secure = true in production
SameSite = Lax
```

生产 Frontend / Backend：

```text
Same Origin
```

---

# 6. CSRF 最终裁决

因为使用 Cookie Session：

```text
CSRF 必须开启
```

建议增加显式初始化 API：

```text
GET /api/v1/auth/csrf
```

Response 可以很简单：

```json
{
  "headerName": "X-XSRF-TOKEN"
}
```

主要目的：

```text
强制 Spring Security 创建 CSRF Token Cookie
```

Frontend：

```text
Axios
XSRF-TOKEN Cookie
→ X-XSRF-TOKEN Header
```

这项补充进入最终 API 基线。

---

# 7. 用户禁用 / 密码重置与 Session

Security Detail 要求：

```text
User Disabled
Password Reset
→ 现有 Session 失效
```

最终实现：

```text
Spring Security SessionRegistry
```

Admin 执行：

```text
Disable User
Reset Password
```

时：

```text
expireSessions(userId)
```

由于 V1 单实例部署：

```text
无需 Redis Session
```

---

# 8. must_change_password

Security Detail 新增：

```text
users.must_change_password
```

正式接受。

由于项目尚未正式开始数据库 Migration：

> 不单独创建 V018。

直接合并进：

```text
V002__identity_rbac.sql
```

字段：

```sql
must_change_password BOOLEAN NOT NULL DEFAULT false
```

---

# 9. PostgreSQL UUID

Database Schema 早期要求：

```text
pgcrypto
→ gen_random_uuid()
```

最终 PostgreSQL 使用：

```text
PostgreSQL 16
```

`gen_random_uuid()` 可直接使用。

因此最终：

```text
pgcrypto 不作为 V1 必需 Extension
```

只要求：

```text
pg_trgm
```

UUID 也可以由 Java Hibernate：

```text
@UuidGenerator
```

生成。

最终推荐：

```text
Java 生成 UUID
+
DB column UUID
```

减少数据库默认函数依赖。

---

# 10. Deprecated 与 Current Version 冲突

Data Model 曾描述：

```text
Current Version
status = DEPRECATED
```

但 Database Schema 规定：

```text
is_current_version = true
只能 status = PUBLISHED
```

最终裁决采用 Database Schema 规则。

即：

```text
PUBLISHED + Current
→ 可用于新 Project / Generation

Deprecated
→ is_current_version = false
```

当整条 Master Test Case 被废弃：

```text
没有 Current Published Version
```

因此：

```text
Generation
Progressive Target Resolution
```

不能再自动选择该 Test Case。

历史 Project：

```text
继续保留原 Version Binding
```

---

# 11. Test Case Reject 状态

产品生命周期正式仍然保持：

```text
DRAFT
REVIEW
PUBLISHED
DEPRECATED
```

不增加：

```text
REJECTED
```

作为第五种 Version Status。

Admin Reject：

```text
ReviewRecord.action = REJECT
revision_closed = true
status 保持 REVIEW
```

该 Revision：

```text
不可继续编辑
不可 Publish
```

UI 显示业务状态：

```text
Rejected
```

来源于：

```text
revision_closed
+
latest ReviewRecord
```

如果未来用户希望重新修改：

```text
创建新的 Revision Draft
```

---

# 12. Published 不可变

正式冻结：

```text
Published Version
永远不可原地修改
```

包括 Admin。

任何变化：

```text
Published
↓
Create Revision
↓
Draft
↓
Review
↓
Published
```

---

# 13. TransitionTarget Version 设计

最终保留：

```text
TransitionTarget
→ MasterTestCase
```

而不是：

```text
固定 Target Version
```

Project Runtime：

## Target 已存在

```text
复用当前 ProjectTestCase
保持其 Version Binding
```

## Target 不存在

```text
解析目标 Master 的 Current Published Version
创建 ProjectTestCase
```

这样：

```text
历史 Project 不自动升级
新 Project 使用最新版
```

---

# 14. Project Test Case 唯一性

正式冻结：

```text
同一 Project
+
同一 Master Test Case
=
一个 ProjectTestCase
```

即使：

```text
Removed = true
```

也不允许创建第二个相同 Master 实例。

用户要重新使用：

```text
Restore
```

而不是重新 Add 一个新实例。

---

# 15. Project Custom Test Case

正式：

```text
Coordinator
+
Tester
```

都可以创建。

Tester 创建时：

```text
自动把自己加入 Assignees
```

Tester：

```text
不能自行给其他用户分配
```

Coordinator：

```text
可以调整 Assignees
```

---

# 16. Assignee 最终语义

正式定义：

```text
Assignee = 实际可以执行这个 Project Test Case 的用户
```

推荐要求：

```text
Assignee User 必须拥有 TESTER Role
```

Coordinator 与 Tester 可为同一用户。

因此 Coordinator 如果需要亲自执行：

```text
该账号必须同时拥有 TESTER Role
+
被加入 Assignees
```

---

# 17. 修正 Coordinator 未分配执行权限

Security Detail 曾提出：

```text
Coordinator 可以执行自己项目里未 Assignee 的 Test Case
```

这与系统“共享执行记录由 Assignees 执行”的核心模型不够一致。

最终取消该例外。

正式规则：

```text
修改 Execution
Complete
Reopen
Decision
Evidence Upload
Note Create

→ 必须是当前 Assignee
```

Admin / Coordinator 如果需要参与执行：

```text
先加入 Assignees
```

Coordinator 仍拥有：

```text
分配权限
```

---

# 18. Tester 未分配 Test Case

正式：

```text
Project Member
→ 可查看全部 Project Test Cases

Current User ∉ Assignees
→ Read Only
```

不能：

```text
Complete
Reopen
Decision Update
Evidence Upload
Note Create
```

---

# 19. Project Member

V1 不增加独立：

```text
project_members
```

Project Member 定义：

```text
Project Coordinator
OR
至少一个 ProjectTestCase Assignee
```

Admin：

```text
全局访问
```

---

# 20. Evidence 权限

正式：

```text
Project Member
→ Read / Download

Assignee
→ Upload

任意当前 Assignee
→ Delete 任意共享 Evidence
```

Coordinator 如果没有成为 Assignee：

```text
可查看
不可作为普通执行者上传
```

如业务管理确实需要上传：

```text
先加入 Assignees
```

保持执行模型一致。

---

# 21. Notes 权限

正式：

```text
Project Member
→ Read

Assignee
→ Create

Author
→ Update Own
→ Delete Own
```

Coordinator / Admin：

```text
不直接修改别人的 Note
```

---

# 22. Project Custom Case 提交 Library

Tester / Coordinator 可以：

```text
Submit to Library
```

结果：

```text
创建 Master Test Case Draft
```

如果提交者是 Tester：

```text
提交者成为 Revision Contributor
```

Draft Owner：

```text
由对应 Coordinator 接管
```

不能直接进入 Published。

---

# 23. Progressive Complete 的并发

产品不需要用户可见的并发编辑控制。

但 Runtime 数据一致性仍然必须保证。

最终增加：

```text
ProjectTestCase Complete / Decision Update
```

时对当前 ProjectTestCase 使用数据库：

```text
PESSIMISTIC_WRITE
```

或等价：

```text
SELECT ... FOR UPDATE
```

实现事务级串行化。

这不属于：

```text
多人编辑锁 UI
```

只是后端一致性保护。

---

# 24. NEXT_CASE 并发

正式：

```text
Project + Master Test Case
Partial Unique Index
```

作为最后防线。

流程：

```text
Query
↓
Create
↓
Unique conflict if raced
↓
Re-query
↓
Reuse
```

不能产生重复 PTC。

---

# 25. Complete Transaction

正式必须一个事务完成：

```text
Lock PTC
↓
Validate
↓
Selections
↓
Branch Outcomes
↓
Target Resolve
↓
Create / Reuse PTC
↓
Source
↓
Trigger
↓
Assignee Union
↓
Relation Recalculate
↓
Completed
```

失败：

```text
全部数据库操作 Rollback
```

---

# 26. File Delete 一致性

文件与 DB 无法使用同一事务。

最终推荐删除策略修订为：

```text
1. Authorization
2. Move File → Storage Trash
3. Delete DB Metadata in Transaction
4. DB commit
5. Purge Trash
```

如果 DB 删除失败：

```text
Restore file from Trash
```

如果最终 Trash Purge 失败：

```text
Cleanup Job 后续处理
```

相比直接先删文件：

```text
更不容易产生 DB Row 存在但文件丢失
```

---

# 27. Storage Trash

V1 Storage Root 增加：

```text
/data/casehub/trash/
```

Trash：

```text
不是用户可访问区域
```

清理：

```text
24h+
```

---

# 28. File Upload

正式继续采用：

```text
Temp
↓
Hash
↓
Final Storage
↓
DB Metadata
```

失败执行补偿。

---

# 29. 文件类型

保持：

```text
通用文件允许上传
```

因为安全测试证据可能是：

```text
PCAP
BIN
Firmware
ZIP
Script
Malformed Sample
```

安全边界依赖：

```text
No Execution
No Static Exposure
Download Authorization
Content-Disposition attachment
```

---

# 30. Generation Engine

正式：

```text
Java Lightweight Rule Engine
```

不引入：

```text
Drools
```

Evaluator：

```text
GenerationConditionEvaluator
GenerationGroupEvaluator
GenerationEngine
```

---

# 31. Progressive Runtime

正式：

```text
Java Service
+
PostgreSQL Transaction
```

不引入：

```text
Workflow Engine
BPMN
Neo4j
```

---

# 32. Graph Database

最终：

```text
不需要
```

DAG：

```text
PostgreSQL relationship tables
+
Java validation/query
```

---

# 33. Logic Graph 编辑

最终：

```text
Decision Point Editor
```

负责配置逻辑。

React Flow：

```text
只用于查看 / 导航
```

V1 不支持：

```text
拖线直接修改生产 DAG
```

---

# 34. Search

正式：

```text
pg_trgm
```

作为中英文模糊搜索主要方式。

PostgreSQL `simple` FTS：

```text
辅助英文查询
```

不依赖其进行中文分词。

---

# 35. Database Extension

最终 V1 必需：

```text
pg_trgm
```

不必：

```text
pgcrypto
```

---

# 36. Production Database

正式：

```text
PostgreSQL 16
```

生产使用固定版本 Image。

禁止：

```text
latest
```

---

# 37. Production Frontend

正式：

```text
React SPA
→ Vite Build
→ Nginx Static
```

---

# 38. Production Backend

正式：

```text
Java 21
Spring Boot 3.x
```

单 Backend Instance 为 V1 默认。

---

# 39. Session 扩展边界

因为单实例：

```text
Memory Session
```

足够。

未来多实例才引入：

```text
Spring Session
+
Redis / JDBC
```

V1 不提前实现。

---

# 40. Deployment

正式：

```text
Docker Compose
```

服务：

```text
nginx
backend
postgres
```

Volume：

```text
postgres data
file storage
```

---

# 41. Server 文件目录

推荐：

```text
/srv/casehub/
├─ postgres/
├─ files/
├─ backups/
├─ logs/
└─ certs/
```

---

# 42. Backup

必须：

```text
Database
+
File Storage
```

每日备份。

推荐：

```text
Daily 14 days
Weekly 8 weeks
Monthly 12 months
```

---

# 43. RPO / RTO

V1 目标：

```text
RPO ≤ 24h
RTO ≤ 4h
```

---

# 44. Testing 最终重点

必须优先保护：

```text
Capability Engine
Generation Engine
DAG
Progressive Runtime
Project Test Case uniqueness
Assignee Union
Floating
Version Binding
RBAC
File Authorization
CSRF
Migration
Backup Restore
```

---

# 45. Database Schema 小修订清单

开发创建初始 Migration 时直接吸收：

## users

增加：

```text
must_change_password BOOLEAN NOT NULL DEFAULT false
```

## UUID

优先：

```text
Java UUID generation
```

不要求 pgcrypto。

## Storage

无需数据库新增 Trash Table。

Trash 属于 Storage Implementation。

## Assignee

Service 校验：

```text
被分配用户具有 TESTER Role
```

## Reject

继续使用：

```text
revision_closed
```

不增加 REJECTED Version Status。

---

# 46. API 小修订清单

增加：

```text
GET /api/v1/auth/csrf
POST /api/v1/auth/change-password
```

修订：

```text
Execution write operations
Evidence Upload
Note Create
```

统一要求：

```text
Current User is Assignee
```

Coordinator 无未分配执行旁路。

---

# 47. Backend 小修订清单

增加/明确：

```text
SessionRegistryService

ProjectTestCase Lock Repository Method

Storage Trash / Restore operation

AssigneeRoleValidator
```

---

# 48. Frontend 小修订清单

如果 Coordinator 想执行：

```text
前端不显示直接执行按钮
```

除非：

```text
allowedActions contains EXECUTE
```

该 allowedActions 只有在：

```text
Current User ∈ Assignees
```

时返回。

---

# 49. 不需要修改的核心设计

以下设计经过最终 Review，保持不变：

```text
Modular Monolith
Java
React
PostgreSQL
Master / Version 分离
ProjectTestCase Version Binding
Decision Point belongs Version
TransitionTarget → Master
Generation Rule → Recommendation
Progressive Runtime
Multiple Assignees
Shared Execution Record
Source[]
Triggered By[]
Assignee Union
Floating
Removed / Restore
Custom Test Case
Capability UNKNOWN
Change Request
Excel
```

---

# 50. V1 明确不实现

最终确认 V1 不实现：

```text
Python Runner
Remote Agent
Automatic Test Execution
Redis
Kafka
RabbitMQ
Neo4j
Elasticsearch
Drools
Camunda
GraphQL
WebSocket Collaboration
CRDT
Microservices
Kubernetes
Notification Center
Project Overall PASS / FAIL Aggregation
Automatic Report Generation
```

---

# 51. 文档状态

最终文档体系：

```text
System Design V0.6
→ Product Source of Truth

Technical Architecture V1.0
→ Technical Stack

Data Model V1.0
→ Entity Model

Database Schema V1.0
→ Persistence Baseline

Backend Architecture V1.0
→ Java Structure

API Design V1.0
→ REST Contract

Frontend Architecture V1.0
→ React Structure

File Storage & Security V1.0
→ File Baseline

Security & RBAC Detail V1.0
→ Security Baseline

Testing Strategy V1.0
→ Test Baseline

Deployment & Backup V1.0
→ Production Baseline

Final Technical Review V1.0
→ Conflict Resolution / Final Override
```

---

# 52. 技术冻结

至此：

```text
Product Design
Architecture
Data Model
Database
Backend
API
Frontend
Storage
Security
Testing
Deployment
```

全部完成。

状态：

```text
TECHNICAL DESIGN FROZEN
```

可以正式进入编码阶段。

---

# 53. 开发入口

正式开发按：

```text
Implementation Plan V1.1
```

执行。

第一阶段：

```text
Repository / Project Skeleton
↓
Spring Boot
↓
React
↓
Docker Compose
↓
PostgreSQL
↓
Flyway V001/V002
↓
Health Check
↓
Authentication / RBAC
```

不应从：

```text
Progressive Runtime
```

直接开写。

先把工程骨架和基础设施建稳。

---

# 54. Final Verdict

```text
READY FOR IMPLEMENTATION
```

当前没有阻塞编码的产品或技术问题。
