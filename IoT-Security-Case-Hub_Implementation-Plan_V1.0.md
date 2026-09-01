# IoT-Security-Case-Hub
## Implementation Plan V1.0

> 本计划基于 `IoT-Security-Case-Hub_System-Design_V0.6.md`。
>
> 目标：按照可验收、可逐步运行的顺序完成 IoT-Security-Case-Hub V1。

---

# 1. 开发原则

V1 优先实现完整业务闭环：

```text
用户
↓
项目
↓
能力
↓
生成测试计划
↓
分配
↓
执行
↓
递进
↓
证据 / Notes
↓
版本维护
↓
Excel 导出
```

第一版避免：

```text
复杂实时协同
AI 自动判定
项目整体风险评分
复杂通知中心
复杂规则优先级
无限条件嵌套
正式报告生成
```

---

# 2. Phase 0：工程基础

## 目标

建立项目基础工程和开发规范。

## 任务

```text
Repository Structure
Backend Skeleton
Frontend Skeleton
Database Migration Framework
Environment Configuration
Logging
Error Handling
Basic CI
```

## 验收

```text
前后端均可启动
数据库可迁移
基础健康检查正常
开发环境可复现
```

---

# 3. Phase 1：用户、角色与权限

## 实现对象

```text
User
Role
UserRole
```

## 实现功能

```text
Login
Logout
Current User
Admin
Test Coordinator
Tester
多角色
权限校验
```

## 验收

不同角色登录后：

```text
只能访问对应功能
```

并支持：

```text
Coordinator + Tester
```

多角色用户。

---

# 4. Phase 2：Capability Library

## 实现对象

```text
Capability
Capability Parent/Child
Project Capability
Capability Source
```

## 功能

```text
Admin 管理能力树
YES / NO / UNKNOWN
Parent = NO 子树不可适用
Child = YES 自动 Derived Parent = YES
Project Capability 填写
```

## 验收

例如：

```text
GATT = YES
```

系统可推导：

```text
BLE = YES
Bluetooth = YES
```

并区分：

```text
Direct
Derived
```

---

# 5. Phase 3：Master Test Case Library

## 实现对象

```text
Master Test Case
Test Case Version
Test Step
Category
Tag
Tool
Standard Mapping
Attachment
```

## 功能

```text
一级 / 二级技术分类
标签
搜索
Test Case 详情
结构化步骤
多 Tool
Evidence Requirement
Remark Requirement
Progressive Role
SINGLE / MULTIPLE
```

## 验收

用户可以：

```text
浏览
搜索
查看
查看 Published / Deprecated
```

Tester 不可修改 Published 内容。

---

# 6. Phase 4：Decision Point 与 DAG

## 实现对象

```text
Decision Point
Transition
Logic Graph
```

## Transition

```text
NEXT_CASE
NEXT_CASES
PASS
FAIL
N/A
```

## 功能

```text
创建 Decision Points
配置目标 Test Case
DAG Cycle Validation
完整逻辑图数据 API
当前节点定位
```

## 验收

可以配置：

```text
TC-001
├─ BLE → TC-002
├─ BR/EDR → TC-010
└─ No Bluetooth → N/A
```

并拒绝形成循环路径。

---

# 7. Phase 5：Test Case Lifecycle

## 实现对象

```text
Change Request
Revision
Draft Contributor
```

## 流程

```text
Published
↓
Change Request
↓
Coordinator
↓
Revision Draft
↓
Review
↓
Admin
↓
Publish / Return / Reject
```

## 功能

```text
Draft
Review
Published
Deprecated
Is Current Version
历史版本
Diff 数据基础
```

## 验收

Published 不能直接覆盖修改。

新版本发布后：

```text
旧版本仍可查看
新版本成为 Current
```

---

# 8. Phase 6：Project

## 实现对象

```text
Project
Project Standard / Task Type
Project Coordinator
Project Status
```

## Project Status

```text
Draft
Active
Completed
Archived
```

## 创建字段

```text
Project Number
Project Name
Device Name
Standard / Task Type[]
Coordinator
```

## 验收

Coordinator 可创建项目，并进入 Capability / Test Plan。

---

# 9. Phase 7：Generation Rule

## 实现对象

```text
Generation Rule
Condition Group
Condition
Rule Output Test Case
```

## 支持

```text
= YES
= NO
= UNKNOWN
!= NO
!= YES

AND
OR
一层 Condition Group
```

## Generation Mode

```text
FULL
PROGRESSIVE_INITIAL
BOTH
```

## Status

```text
Enabled
Disabled
```

## 验收

Rule 可以：

```text
一对多输出 Test Case
```

多个 Rule 输出同一 Case 时：

```text
自动去重
保留所有 Recommended Because
```

---

# 10. Phase 8：Recommended Test Cases

## 功能

Generation Run 后生成：

```text
Recommended Test Cases
```

Coordinator 可以：

```text
Add
Ignore
Manual Add
```

重新 Generation：

```text
已加入 → 保留
新增 → New Recommended
Ignored → 保持 Ignored
```

## 验收

Generation 不直接改变执行计划。

---

# 11. Phase 9：Project Test Plan

## 实现对象

```text
Project Test Case
Project Test Case Source[]
Assignees[]
Relation Status
Removed
Triggered By[]
```

## 唯一性

同一 Project 内：

```text
一个 Master Test Case
=
一个 Project Test Case
```

## Source

```text
Initial
Generated
Progressive
Manual
Custom
```

## 验收

多 Source / Trigger 不产生重复执行实例。

---

# 12. Phase 10：项目测试总表

## 默认列

```text
Case ID
Test Case Name
Category
Source
Assignees
Type
Execution Status
Relation Status
Decision / Branch Summary
Evidence Count
Last Modified
```

## 筛选

```text
Category
Source
Assignee
Execution Status
Relation Status
Type
只看我的用例
```

## 批量操作

```text
批量分配
批量调整人员
批量加入
批量移出
```

## 验收

多人任务保持一行一个 Project Test Case。

---

# 13. Phase 11：Tester Execution

## 执行页面

```text
基本信息
前置条件
Tools
Test Steps
Evidence
Notes
Decision Points
Complete Test Case
```

## Execution Status

```text
Not Started
In Progress
Completed
```

## 验收

Tester 可完成自己被分配的 Test Case。

Completed 后可以重新打开修改。

---

# 14. Phase 12：Evidence

## Evidence

```text
File
Description
Uploaded By
```

## 权限

所有当前 Assignee 都可以：

```text
上传
查看
删除任意共享 Evidence
```

## 验收

Evidence Count 能在项目总表实时反映。

---

# 15. Phase 13：Notes

## Note

```text
Author
Content
Created At
```

## 权限

```text
Assignee → 新增
Author → 修改 / 删除自己的
其他 Tester → 只读他人 Note
```

## 验收

Notes 采用追加模式，不是共享大文本框。

---

# 16. Phase 14：Progressive Runtime

## 核心流程

```text
Complete Test Case
↓
Selected Decision Points
↓
Transition
↓
NEXT_CASE / NEXT_CASES
↓
自动激活
↓
自动分发
```

## Assignee

首次触发：

```text
继承前置 Assignees
```

多前置触发：

```text
Assignees 取并集
```

## 去重

已经存在：

```text
复用 Project Test Case
增加 Triggered By
```

Completed 再触发：

```text
保持 Completed
```

## 验收

整个 Progressive 链无需 Coordinator 逐层审核即可继续。

---

# 17. Phase 15：Floating Branch

## Relation Status

```text
CONNECTED
FLOATING
```

## 规则

```text
有有效 Incoming Transition
→ CONNECTED

无有效 Incoming Transition
→ FLOATING
```

## 移除节点

```text
断边
不删下游节点
```

## 验收

Floating Test Case 仍可正常执行。

重新连接后自动恢复 CONNECTED。

---

# 18. Phase 16：Completed 后修改逻辑

如果修改：

```text
不影响后续关系
```

直接保存。

如果影响：

```text
Decision Point
Transition
后续关系
```

显示二次确认：

```text
原节点
其他节点
增加节点
不使用节点
```

不使用：

```text
断边不删节点
```

## 验收

已有下游数据不会因为前置修改被自动删除。

---

# 19. Phase 17：Project Custom Test Case

## 创建权限

```text
Test Coordinator
Tester
```

## 支持

```text
独立执行
Progressive Target
NEXT_CASE
NEXT_CASES
Evidence
Notes
Decision Point
```

## Library Submission

```text
Submit to Test Case Library
```

形成新的 Draft。

正式发布后不自动替换当前 Custom Case。

---

# 20. Phase 18：Capability Update Request

## 流程

```text
Tester
↓
Capability Update Request
↓
Coordinator
↓
Approved / Rejected
```

Approved：

```text
Update Project Capability
↓
Generation Rule
↓
New Recommended Test Cases
```

不直接触发 Progressive Runtime。

---

# 21. Phase 19：“我的测试”

## 首页

按 Project 分组。

显示：

```text
我的任务
NEW
未开始
进行中
已完成
```

## 进入项目

默认：

```text
我的用例
```

可切换：

```text
项目全部用例
```

## Tabs

```text
全部
未开始
进行中
已完成
游离
```

## Progressive

显示：

```text
Triggered By
```

## Floating

显示：

```text
⚠ FLOATING
```

## 验收

未分配给当前 Tester 的用例：

```text
只读
```

---

# 22. Phase 20：Logic Graph UI

## 功能

```text
查看完整逻辑图
查看 Current Execution Path
当前 Test Case 高亮
点击节点跳转详情
Floating Branch 标识
```

## 验收

Tester 可以从任意 Progressive Test Case 打开图并定位当前节点。

---

# 23. Phase 21：Removed / Restore

## Project Test Case

支持：

```text
Removed
Restore
```

执行过的用例不物理删除。

## Project

默认：

```text
Archived / Soft Delete
```

## 验收

恢复后原 Evidence / Notes / Results 保留。

---

# 24. Phase 22：Version Upgrade

进行中 Project 可看到：

```text
New Version Available
```

Coordinator：

```text
View Diff
Keep Current
Upgrade
```

升级保留：

```text
Evidence
Notes
Execution Status
Assignees
```

Decision Point 结构变化时显示风险提示。

---

# 25. Phase 23：Excel Export

一个 Workbook：

```text
Project Summary
Test Cases
Evidence Index
```

Evidence 文件不嵌入 Excel。

## Test Cases Sheet

至少：

```text
Case ID
Test Case Name
Category
Source
Assignees
Type
Execution Status
Relation Status
Test Purpose
Preconditions
Tools
Test Steps
Selected Decision Points
Branch Outcomes
Evidence Count
Notes
Last Modified
```

---

# 26. Phase 24：最终权限核对

执行完整 RBAC 验收。

重点测试：

```text
Tester 不能修改 Project 基本信息
Tester 不能修改 Assignees
Tester 不能修改正式 Capability
Tester 不能修改 Published Master Case

Coordinator 不能 Publish Master Case

Admin 可以 Publish
```

---

# 27. Phase 25：数据一致性测试

重点覆盖：

```text
多 Rule 命中去重
多 Trigger 命中去重
Assignee 并集
Floating 恢复
Removed Restore
Version Binding
Capability Derived
UNKNOWN Rule Matching
Progressive DAG 无环
```

---

# 28. Phase 26：V1 验收场景

建议至少准备以下端到端场景。

## Case A：Full Profile

```text
创建 Project
↓
填写 Capability
↓
FULL Generation
↓
选择 Test Cases
↓
分配 Tester
↓
执行
↓
Excel Export
```

## Case B：Progressive Bluetooth

```text
Bluetooth = YES
BLE / BR/EDR = UNKNOWN
↓
PROGRESSIVE_INITIAL
↓
BT-MODE ENTRY
↓
Tester 选择 BLE + BR/EDR
↓
自动激活两个分支
↓
自动分发
↓
继续执行
```

## Case C：多前置合流

```text
两个前置节点
↓
同一 NEXT_CASE
↓
只产生一个 Project Test Case
↓
Triggered By = 2
↓
Assignees 取并集
```

## Case D：Floating

```text
修改前置 Decision Point
↓
不使用原节点
↓
下游变 Floating
↓
继续执行
↓
重新接入
↓
CONNECTED
```

## Case E：Capability Update

```text
Tester 发现 GATT
↓
Capability Update Request
↓
Coordinator Approved
↓
Generation Rule
↓
New Recommended
```

---

# 29. 推荐开发顺序

实际实施建议按下面顺序：

```text
0 工程基础
1 用户权限
2 Capability Library
3 Master Test Case Library
4 Decision Point / DAG
5 Test Case Lifecycle
6 Project
7 Generation Rule
8 Recommended Cases
9 Project Test Plan
10 Project Test Table
11 Tester Execution
12 Evidence
13 Notes
14 Progressive Runtime
15 Floating
16 Completed 修改
17 Custom Case
18 Capability Request
19 My Tests
20 Logic Graph
21 Removed / Restore
22 Version Upgrade
23 Excel
24 RBAC Final
25 Consistency Test
26 E2E Acceptance
```

---

# 30. 下一份技术设计

Implementation Plan V1.0 之后，下一份文档应为：

```text
IoT-Security-Case-Hub_Data-Model_V1.0.md
```

该文档开始正式定义：

```text
Entity
Primary Key
Foreign Key
Cardinality
Unique Constraint
State Field
核心数据关系
```

然后进入 Database Schema。
