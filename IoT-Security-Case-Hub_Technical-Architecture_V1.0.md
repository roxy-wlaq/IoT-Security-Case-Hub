# IoT-Security-Case-Hub
## Technical Architecture V1.0

> 本文档定义 IoT-Security-Case-Hub 在正式编码前的技术架构基线。
>
> 本文档用于冻结：
>
> - 系统总体架构
> - 后端技术路线
> - 前端技术路线
> - 数据库
> - ORM 与 Migration
> - API 风格
> - 权限与认证
> - 文件存储
> - 搜索
> - Excel
> - 部署
> - 模块划分
> - 未来自动化测试扩展边界
>
> 后续数据库、API、前端和实现计划均应以本文档为基础。

---

# 1. 系统技术定位

IoT-Security-Case-Hub 的核心定位是：

**长期维护的 IoT 网络安全 Test Case / Project / Workflow / Knowledge Base 平台。**

系统第一阶段主要承担：

```text
Test Case Library
Project
Device Capability
Generation Rule
Progressive DAG
任务分发
Tester Execution
Evidence
Notes
Version
Change Request
Excel Export
```

系统第一阶段不定位为：

```text
自动化扫描平台
远程设备控制平台
漏洞扫描器
测试脚本执行平台
正式报告生成平台
```

未来可能增加自动执行能力，但不能让第一版架构因此过度复杂。

---

# 2. 后端语言选择

正式选择：

```text
Java
```

推荐基线：

```text
Java 21 LTS
Spring Boot 3.x
```

---

# 3. 为什么选择 Java

本平台后续主要是典型业务系统，而不是测试脚本集合。

核心业务对象包括：

```text
Project
Master Test Case
Test Case Version
Decision Point
Transition
Capability
Generation Rule
Assignee
Evidence
Change Request
Revision
```

同时存在较多：

```text
状态流转
权限控制
事务
版本管理
多对多关系
DAG 关系
数据一致性约束
```

Java 在以下方面更适合作为长期维护平台后端：

```text
强类型
大型项目重构
多人协作
IDE 支持
事务控制
企业权限体系
模块边界
长期维护
```

尤其适合系统中大量枚举和状态：

```text
Draft
Review
Published
Deprecated

Not Started
In Progress
Completed

CONNECTED
FLOATING

YES
NO
UNKNOWN

FULL
PROGRESSIVE_INITIAL
BOTH

NEXT_CASE
NEXT_CASES
PASS
FAIL
N/A
```

这些状态在 Java 中可以通过明确的 Enum 和类型约束实现。

---

# 4. Python 的定位

虽然核心平台使用 Java，但 Python 不被排除。

如果未来增加自动测试能力，Python 更适合作为：

```text
Automation Runner
Security Test Runner
Packet Analyzer
Fuzz Runner
Script Executor
```

例如：

```text
Bluetooth
Wi-Fi
Scapy
btmon
tshark
pyOCD
串口
Fuzz
PCAP 分析
日志分析
```

这些自动化能力未来可以作为独立 Runner 接入 Java 平台。

第一版不实现 Runner。

---

# 5. 总体系统架构

正式选择：

```text
Modular Monolith
模块化单体
```

而不是：

```text
Microservices
微服务
```

---

# 6. 为什么选择 Modular Monolith

当前系统业务对象关联非常紧密：

```text
Project
Test Case
Capability
Generation Rule
Decision Point
Evidence
Version
```

如果一开始拆微服务，会引入：

```text
服务间调用
分布式事务
服务发现
额外部署复杂度
数据一致性复杂度
更多日志与监控成本
```

当前没有必要。

因此 V1 使用：

```text
一个 Spring Boot Backend
+
内部按业务领域模块化
```

后续如果某个模块真的需要拆分，再从模块化单体演进。

---

# 7. 总体技术栈

```text
Architecture
Modular Monolith

Frontend
React + TypeScript

Backend
Java 21 + Spring Boot 3.x

Database
PostgreSQL

ORM
Spring Data JPA + Hibernate

Database Migration
Flyway

Security
Spring Security + RBAC

API
REST + JSON

Graph UI
React Flow

Search
PostgreSQL Full Text Search + pg_trgm

File Storage
Local Persistent Storage
+
Storage Service Abstraction

Excel
Apache POI

Build
Maven

Reverse Proxy
Nginx

Deployment
Docker Compose
```

---

# 8. 前端技术栈

正式建议：

```text
React
TypeScript
```

原因：

```text
适合复杂业务后台
组件生态成熟
与 React Flow 集成方便
类型安全
前后端模型更容易对齐
```

---

# 9. Graph UI

递进式 Test Case 的逻辑图需要支持：

```text
DAG
节点点击
当前节点高亮
完整逻辑图
Current Execution Path
Floating Branch
多前置节点
多后续节点
```

前端建议：

```text
React Flow
```

用于展示：

```text
Test Case Node
Decision Point
Transition
PASS / FAIL / N/A Terminal
Floating Branch
```

---

# 10. 后端框架

正式选择：

```text
Spring Boot 3.x
```

建议使用：

```text
Spring Web
Spring Validation
Spring Security
Spring Data JPA
Spring Transaction
```

根据实际需要增加其他 Spring 组件。

---

# 11. Java 版本

正式建议：

```text
Java 21 LTS
```

不建议新项目继续使用：

```text
Java 8
Java 11
```

Java 21 作为当前长期支持版本，更适合新系统长期维护。

---

# 12. 构建工具

正式选择：

```text
Maven
```

项目使用：

```text
pom.xml
```

统一管理：

```text
依赖
测试
打包
插件
构建生命周期
```

---

# 13. 数据库

正式选择：

```text
PostgreSQL
```

原因：

```text
事务能力成熟
关系模型适合本系统
支持递归查询
支持 JSONB
支持 Full Text Search
支持 pg_trgm
索引能力强
适合长期维护
```

第一版不需要：

```text
MongoDB
Neo4j
Elasticsearch
```

---

# 14. 为什么不需要图数据库

虽然 Progressive Test 使用 DAG，但实际关系模型仍然可以很好地使用 PostgreSQL 表达：

```text
Test Case
↓
Decision Point
↓
Transition
↓
Target Test Case
```

数据库通过：

```text
Foreign Key
Relation Table
Recursive CTE
```

即可实现：

```text
图遍历
前置节点查询
后续节点查询
Floating 判断
Cycle Detection
```

因此 V1 不使用 Neo4j。

---

# 15. ORM

正式建议：

```text
Spring Data JPA
+
Hibernate
```

用途：

```text
Entity Mapping
Repository
关系映射
事务
查询
分页
```

复杂查询允许使用：

```text
JPQL
Native SQL
```

不要强迫所有逻辑都使用 JPA 自动查询。

---

# 16. 数据库 Migration

正式选择：

```text
Flyway
```

所有数据库结构变更必须使用 Migration。

例如：

```text
V001__init.sql
V002__add_project_test_case.sql
V003__add_triggered_by.sql
```

禁止：

```text
开发到一半直接手工修改生产数据库
```

---

# 17. API 风格

正式选择：

```text
REST API
+
JSON
```

API 基础路径：

```text
/api/v1/
```

例如：

```text
GET    /api/v1/projects
POST   /api/v1/projects

GET    /api/v1/projects/{id}
PUT    /api/v1/projects/{id}

GET    /api/v1/test-cases
GET    /api/v1/test-cases/{id}

POST   /api/v1/projects/{id}/generate

POST   /api/v1/project-test-cases/{id}/complete
```

---

# 18. API 版本

第一版：

```text
/api/v1
```

后续如果存在不兼容 API 改动：

```text
/api/v2
```

不要通过破坏原接口的方式升级。

---

# 19. Backend 模块划分

后端采用领域模块划分。

建议：

```text
backend/
├─ auth
├─ user
├─ project
├─ capability
├─ testcase
├─ tool
├─ generation
├─ execution
├─ evidence
├─ changerequest
├─ export
└─ common
```

---

# 20. auth 模块

负责：

```text
Login
Logout
Authentication
Session / Token
Current User
Spring Security
```

---

# 21. user 模块

负责：

```text
User
Role
User Role
Enable / Disable User
RBAC
```

---

# 22. project 模块

负责：

```text
Project
Project Status
Standard / Task Type
Coordinator
Project Test Plan
Project Test Case
Assignees
Removed / Restore
Archived
```

---

# 23. capability 模块

负责：

```text
Capability Library
Capability Tree
Project Capability
YES / NO / UNKNOWN
Derived Capability
Capability Update Request
```

---

# 24. testcase 模块

负责：

```text
Master Test Case
Test Case Version
Test Step
Decision Point
Transition
Category
Tag
Standard Mapping
Progressive Role
Lifecycle
Revision
```

---

# 25. tool 模块

负责：

```text
Tool Library
Test Case Tool Mapping
```

第一版 Tool Library 保持相对轻量。

---

# 26. generation 模块

负责：

```text
Generation Rule
Condition
Condition Group
Rule Output
Rule Evaluation
Recommended Test Cases
Recommended Because
Ignore
Regeneration
```

---

# 27. execution 模块

负责：

```text
Project Test Case Execution
Execution Status
Selected Decision Points
Branch Outcomes
Triggered By
Progressive Runtime
Assignee Union
Floating
Completed Modification
```

---

# 28. evidence 模块

负责：

```text
Evidence Metadata
Upload
Download
Delete
Storage Service
Access Control
```

---

# 29. changerequest 模块

负责：

```text
Test Case Change Request
Revision
Review
Publish Flow
Capability Update Request
```

后续也可以根据实际代码规模进一步拆分。

---

# 30. export 模块

负责：

```text
Excel Export
Project Summary
Test Cases Sheet
Evidence Index
```

---

# 31. common 模块

只放真正跨领域公共能力：

```text
Exception
Response Model
Base Entity
Common Enum
Utility
Security Context Helper
```

不要把业务代码全部塞进 common。

---

# 32. 权限系统

正式选择：

```text
Spring Security
+
RBAC
```

角色：

```text
Admin
Test Coordinator
Tester
```

一个用户可以有多个 Role。

---

# 33. 权限控制必须在后端执行

不能只依靠：

```text
前端隐藏按钮
```

后端 API 必须检查权限。

例如：

```text
Tester
不能修改 Project 基本信息

Tester
不能修改 Assignees

Tester
不能修改 Published Master Test Case

Coordinator
不能 Publish Master Test Case

Admin
可以 Publish
```

---

# 34. Permission 设计

后续可以进一步抽象为：

```text
project:create
project:update
project:archive

project_test_case:assign
project_test_case:remove

test_case:read
test_case:draft:create
test_case:review
test_case:publish

execution:update

capability:update

generation_rule:manage
```

V1 是否直接落地 Permission Table，可在 Data Model 阶段继续确定。

---

# 35. Authentication

V1 面向公司内网使用。

建议：

```text
Local Account
+
Spring Security
```

第一版不强制接：

```text
LDAP
Active Directory
OIDC
SSO
```

但认证层应避免和业务代码强耦合，后续可以扩展。

---

# 36. 密码存储

密码不能明文保存。

应使用 Spring Security 推荐的密码 Hash 机制。

例如：

```text
BCrypt
```

数据库只保存 Hash。

---

# 37. File Storage

Evidence 和 Attachment 的实际文件：

> **不直接保存到 PostgreSQL BLOB。**

数据库只保存元数据。

实际文件保存到：

```text
Persistent File Storage
```

---

# 38. 文件存储第一版

第一版使用：

```text
Local Persistent Directory
```

例如容器挂载：

```text
/data
```

内部：

```text
/data/evidence/
/data/attachments/
/data/exports/
```

---

# 39. Storage Service 抽象

业务层不能直接到处拼接本地路径。

统一通过：

```text
StorageService
```

接口处理：

```text
save
read
delete
exists
```

以后可以更换：

```text
MinIO
S3
NAS
Object Storage
```

而不影响业务层。

---

# 40. Evidence 数据

数据库只保存：

```text
Evidence ID
Project Test Case ID
Original Filename
Stored Filename / Storage Key
Size
Description
Uploaded By
Created At
```

实际文件由 Storage Service 管理。

---

# 41. 文件安全

系统会上传：

```text
PCAP
LOG
ZIP
BIN
Firmware
TXT
CSV
Screenshot
Photo
Video
Script
Other
```

所以必须处理：

```text
路径穿越
非法文件名
超大文件
下载鉴权
删除鉴权
Storage Key 隔离
```

不能信任客户端提供的文件路径。

---

# 42. Search

公共 Test Case Library 是知识库，因此搜索是核心能力。

V1 不引入 Elasticsearch。

正式建议：

```text
PostgreSQL Full Text Search
+
pg_trgm
```

---

# 43. 搜索范围

至少覆盖：

```text
Case ID
Case Name
Test Purpose
Test Steps
Tags
Tools
Category
```

后续根据实际性能决定是否增加搜索服务。

---

# 44. Excel

正式选择：

```text
Apache POI
```

由 Java Backend 直接生成 Excel。

V1 不要求 Python 辅助。

---

# 45. Excel Workbook

一个 Workbook 至少包含：

```text
Project Summary
Test Cases
Evidence Index
```

Evidence 文件本身不嵌入 Excel。

---

# 46. Reverse Proxy

正式选择：

```text
Nginx
```

负责：

```text
静态前端
反向代理 Backend
统一入口
上传限制
HTTPS
```

---

# 47. Deployment

V1 正式选择：

```text
Docker Compose
```

不使用 Kubernetes。

---

# 48. V1 部署组件

建议：

```text
nginx
frontend
backend
postgres
```

Persistent Volume：

```text
postgres-data
file-storage
```

---

# 49. 部署拓扑

```text
Browser
   │
   ▼
 Nginx
   │
   ├──────────────► Frontend
   │
   └──────────────► Spring Boot Backend
                         │
             ┌───────────┴───────────┐
             ▼                       ▼
        PostgreSQL              File Storage
```

---

# 50. 为什么第一版不加 Redis

当前核心业务不需要高频缓存或复杂异步调度。

因此 V1 暂不引入：

```text
Redis
```

以后如果需要：

```text
缓存
分布式锁
任务队列
Session Store
```

再增加。

---

# 51. 为什么第一版不加消息队列

当前不存在必须异步处理的大型后台任务。

因此暂不引入：

```text
RabbitMQ
Kafka
```

未来自动化测试 Runner 可能需要任务队列，到时再设计。

---

# 52. Future Automation Extension

虽然 V1 不执行测试脚本，但系统架构要允许未来扩展。

原则：

> **Java 永远负责平台业务，自动化测试由外部执行器负责。**

---

# 53. Future Automation 架构

未来可以扩展：

```text
IoT-Security-Case-Hub
        │
        │ REST / Job
        ▼
 Automation Runner
        │
   ┌────┼────────┐
   ▼    ▼        ▼
 Python Shell External Tool
```

Java Backend 仍然负责：

```text
Project
Test Case
Permission
Task
Result
Evidence Metadata
Workflow
```

Runner 负责：

```text
设备操作
脚本执行
抓取输出
测试工具集成
自动证据生成
```

---

# 54. Future Execution Provider

架构上可以预留概念：

```text
Execution Provider
```

未来可能支持：

```text
MANUAL
PYTHON_RUNNER
SHELL_RUNNER
REMOTE_AGENT
OTHER
```

但 V1：

```text
只实现 MANUAL
```

不提前开发 Runner。

---

# 55. V1 明确不实现的自动化能力

第一版不实现：

```text
Python Runner
Shell Runner
Remote Agent
Test Script Scheduler
测试脚本调度
设备控制
自动 BLE 测试
自动 Wi-Fi 测试
自动 PCAP 分析
Fuzz 调度
任务队列
Runner 节点管理
```

---

# 56. 并发设计原则

第一版不做复杂实时多人协作。

不实现：

```text
WebSocket 协同编辑
实时锁
文档协同 OT / CRDT
```

但数据库仍必须保证核心一致性。

---

# 57. 数据一致性必须依赖数据库与事务

以下逻辑不能只靠前端：

```text
同一 NEXT_CASE 不重复创建
多 Rule 推荐去重
多 Trigger Test Case 去重
Assignee 自动取并集
Triggered By 去重
```

必须使用：

```text
Unique Constraint
Transaction
```

保证。

---

# 58. Transaction

以下操作应考虑作为事务执行：

```text
Complete Test Case
+
保存 Decision Point
+
保存 Branch Outcome
+
触发 NEXT_CASE
+
创建 / 复用后续 Project Test Case
+
合并 Assignees
+
保存 Triggered By
```

避免出现部分成功导致图状态不一致。

---

# 59. Project Test Case 唯一性

数据库层必须保证：

```text
同一 Project
+
同一 Master Test Case
```

不会重复创建多个有效 Project Test Case 实例。

Custom Test Case 另行建模。

具体 Unique Constraint 在 Database Schema 阶段确定。

---

# 60. Progressive DAG 一致性

必须实现：

```text
Cycle Detection
```

正式 Master Test Case DAG 不允许形成环。

例如禁止：

```text
TC-A → TC-B → TC-C → TC-A
```

---

# 61. Logging

Backend 使用标准 Java Logging。

建议：

```text
SLF4J
+
Logback
```

禁止在正式代码里大量：

```text
System.out.println()
```

---

# 62. Log Level

建议：

```text
Development
DEBUG

Production
INFO / WARN / ERROR
```

敏感信息不能写入日志：

```text
Password
Token
Secret
```

---

# 63. Audit Log

虽然不保存 Tester 每一次执行编辑历史，但系统级关键操作建议保留审计日志：

```text
Login
User Role Change
Project Create
Project Archive
Master Test Case Publish
Master Test Case Deprecated
Generation Rule Change
Capability Library Change
```

这和 Project Test Case 的详细编辑历史是不同概念。

---

# 64. Environment

至少区分：

```text
Development
Testing
Production
```

---

# 65. Configuration

运行配置通过：

```text
Environment Variables
```

或 Spring Profile。

例如：

```text
application.yml
application-dev.yml
application-test.yml
application-prod.yml
```

秘密信息不能提交到 Git。

---

# 66. Secret

例如：

```text
Database Password
JWT / Session Secret
Admin Initial Password
```

应通过：

```text
Environment Variable
Docker Secret
外部配置
```

提供。

---

# 67. Testing

至少需要：

```text
Unit Test
Repository Test
Service Test
API Test
Permission Test
Rule Engine Test
Progressive DAG Test
```

Java 推荐：

```text
JUnit 5
Spring Boot Test
Testcontainers
```

---

# 68. Testcontainers

建议使用：

```text
Testcontainers PostgreSQL
```

避免核心数据库测试依赖 H2。

因为 PostgreSQL 的：

```text
CTE
JSONB
FTS
pg_trgm
```

和 H2 行为不完全一致。

---

# 69. 重点自动化测试

必须覆盖：

```text
Capability Parent / Child
Derived Capability
UNKNOWN Matching
Generation Rule AND / OR
多 Rule 去重
Project Test Case 唯一性
多 Trigger
Assignee Union
Floating
Removed / Restore
Cycle Detection
Version Binding
Progressive Complete Transaction
```

---

# 70. Backup

必须备份：

```text
PostgreSQL
File Storage
```

只备份数据库不够，因为 Evidence 实际文件在独立存储目录。

---

# 71. Database Backup

生产环境至少支持：

```text
pg_dump
```

后续根据部署环境制定：

```text
Daily Backup
Retention
Restore Test
```

---

# 72. File Storage Backup

必须备份：

```text
Evidence
Attachments
Exports
```

恢复时数据库和文件存储需要保持一致。

---

# 73. Git

代码仓库使用 Git。

推荐开发方式：

```text
main
+
feature branches
+
Pull Request
```

是否增加 develop 分支，可根据实际团队规模决定。

---

# 74. Code Style

开发前统一：

```text
Java Formatter
TypeScript Formatter
Lint
```

避免多人开发后代码格式差异过大。

---

# 75. Backend Package 建议

可以使用：

```text
com.company.iotsecuritycasehub
```

内部：

```text
auth
user
project
capability
testcase
generation
execution
evidence
export
common
```

具体公司包名在创建工程时替换。

---

# 76. Frontend 目录建议

```text
frontend/
├─ src/
│  ├─ api/
│  ├─ components/
│  ├─ features/
│  │  ├─ auth/
│  │  ├─ projects/
│  │  ├─ test-cases/
│  │  ├─ capabilities/
│  │  ├─ generation/
│  │  └─ execution/
│  ├─ routes/
│  ├─ types/
│  └─ utils/
```

按 Feature 组织，不建议全部组件堆在一个 components 目录。

---

# 77. Repository 总体结构建议

```text
IoT-Security-Case-Hub/
├─ backend/
│  ├─ pom.xml
│  └─ src/
│
├─ frontend/
│  ├─ package.json
│  └─ src/
│
├─ deploy/
│  ├─ docker-compose.yml
│  └─ nginx/
│
├─ docs/
│  ├─ 01-product/
│  ├─ 02-architecture/
│  ├─ 03-data/
│  ├─ 04-api/
│  ├─ 05-frontend/
│  ├─ 06-deployment/
│  └─ archive/
│
├─ .gitignore
└─ README.md
```

---

# 78. 文档体系

建议正式文档分层：

```text
docs/

01-product/
System-Design_V0.6.md

02-architecture/
Technical-Architecture_V1.0.md

03-data/
Data-Model_V1.0.md
Database-Schema_V1.0.md

04-api/
API-Design_V1.0.md

05-frontend/
Frontend-Design_V1.0.md

06-deployment/
Deployment_V1.0.md

archive/
旧设计
```

---

# 79. 技术设计顺序

正式编码前按以下顺序完成：

```text
1. Technical Architecture
2. Data Model
3. Database Schema
4. Backend Architecture Detail
5. API Design
6. Frontend Architecture
7. File Storage Design
8. Security / RBAC
9. Testing Strategy
10. Deployment / Backup
11. Final Technical Review
12. Start Coding
```

---

# 80. V1 技术基线最终总结

当前正式冻结的技术基线：

```text
Architecture
Modular Monolith

Frontend
React + TypeScript

Backend
Java 21 + Spring Boot 3.x

Database
PostgreSQL

ORM
Spring Data JPA + Hibernate

Migration
Flyway

Security
Spring Security + RBAC

API
REST / JSON

Graph
React Flow

Search
PostgreSQL FTS + pg_trgm

Files
Local Persistent Storage + StorageService

Excel
Apache POI

Build
Maven

Proxy
Nginx

Deployment
Docker Compose

Testing
JUnit 5 + Spring Boot Test + Testcontainers
```

---

# 81. 下一阶段

下一份技术设计文档：

```text
IoT-Security-Case-Hub_Data-Model_V1.0.md
```

该文档将正式定义：

```text
所有 Entity
Entity 之间关系
1:1
1:N
N:M
主键
外键
唯一性
生命周期
删除行为
版本绑定
DAG 关系
Generation Rule 关系
Project Test Case 执行关系
```

之后再进入：

```text
Database Schema V1.0
```

定义具体：

```text
Table
Column
Type
Index
Unique Constraint
Foreign Key
Nullable
Default Value
```

完成这些后再开始写代码。
