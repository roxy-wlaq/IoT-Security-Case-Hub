# IoT-Security-Case-Hub
## 系统统一设计 V0.6

> 本文档用于统一当前已经确认的系统设计，并替代前期文档中已经过时或互相冲突的部分。
>
> 当前阶段只定义产品功能、角色权限、Test Case 模型、递进式测试逻辑、任务分发与版本管理。
>
> **暂不进入代码、数据库表结构和最终 UI 细节设计。**

---

# 1. 系统定位

IoT-Security-Case-Hub 定位为：

**IoT 网络安全测试用例生成、分发、执行与知识库平台。**

系统主要负责：

```text
项目创建
设备信息录入
设备能力录入
测试模式选择
测试用例推荐 / 生成
项目测试计划调整
测试任务分发
多人协作执行
测试证据管理
递进式 Test Case 自动流转
公共 Test Case Library
测试工具库
测试用例版本管理
测试用例修改申请
Excel 导出
```

系统不负责：

```text
正式测试报告编写
报告模板管理
正式测试报告生成
客户报告归档
```

正式报告由其他系统完成。

---

# 2. 系统核心设计原则

当前系统必须遵循以下原则：

1. 公共 Test Case Library 同时是正式测试数据源和新人学习知识库。
2. 所有登录用户都可以浏览和搜索公共 Test Case。
3. 自动生成结果必须允许人工调整。
4. 测试协调员负责“搭好测试”，不负责逐条审核测试结果。
5. 测试人员负责实际执行、证据、备注、Decision Point 选择和完成任务。
6. Progressive Test 的执行过程尽量自动化，不增加协调员逐节点操作。
7. Test Case 本身就是递进式逻辑图中的节点。
8. 不再使用独立 Tree Node 模型。
9. PASS / FAIL / N/A 是当前分支的终止结果，不直接等于整个项目结论。
10. 公共 Test Case 必须版本化，Published 版本不能直接覆盖修改。
11. 历史项目必须保留当时绑定的 Test Case 版本。
12. 进行中项目遇到新版只提示，不自动升级。
13. 项目执行数据尽量轻量，不保存完整编辑历史。
14. 设备信息不完整是正常状态，UNKNOWN 必须是一等状态。
15. 系统生成逻辑和标签系统必须分离：标签用于搜索，生成规则用于生成。

---

# 3. 用户角色

系统第一阶段设置三个角色。

## 3.1 Admin

系统管理员负责：

```text
用户管理
角色权限管理
公共 Test Case 最终审核
公共 Test Case 发布
Deprecated
生成规则管理
分类 / 标签管理
系统管理
```

Admin 是正式公共 Test Case Library 的最终发布者。

---

## 3.2 Test Coordinator

测试协调员负责：

```text
创建项目
填写已知项目 / 设备信息
填写已知设备能力
选择测试模式
生成 / 选择项目 Test Case
人工增加 / 删除 / 调整 Project Test Case
分配测试人员
批量分配测试人员
处理 Change Request
创建 Test Case Revision
编辑 Draft
提交管理员审核
必要时调整正在执行项目的测试计划
```

测试协调员：

**不需要逐条审核 Tester 的执行结果。**

---

## 3.3 Tester

测试人员负责：

```text
查看公共 Test Case Library
搜索与学习 Test Case
查看工具与测试方法
查看逻辑图
执行被分配的 Test Case
填写备注
上传证据
选择 Decision Point
完成 Test Case
重新打开 Completed Test Case 修改
提交公共 Test Case 修改申请
提交新增用例建议
```

Tester 默认不能直接修改正式 Published Test Case。

---

## 3.4 多角色

一个用户可以拥有多个角色。

例如：

```text
User A

Roles:
Test Coordinator
Tester
```

---

# 4. 项目创建

项目创建应尽量轻量。

第一阶段只要求核心信息：

```text
Project Number
系统自动生成

Project Name
手工填写

Device Name
手工填写

Standard / Task Type[]
多选

Test Coordinator
选择负责人
```

不强制要求：

```text
客户名称
设备型号
硬件版本
固件版本
大量项目备注
客户附件
```

因为本系统不是正式报告管理系统。

---

# 5. 设备能力模型

设备能力表示：

> DUT 有什么、支持什么、如何通信。

不是安全结论。

能力值至少支持：

```text
YES
NO
UNKNOWN
```

UNKNOWN 属于正常状态。

---

## 5.1 能力层级

建议按以下层级理解：

```text
Interface
接口 / 攻击面

Communication Technology
通信技术 / 模式

Protocol / Profile
协议 / Profile

Function
功能

Security Mechanism
安全机制
```

Bluetooth 示例：

```text
Bluetooth
├─ BLE
│  ├─ GATT
│  ├─ ATT
│  ├─ SMP
│  └─ L2CAP
└─ BR/EDR
   ├─ RFCOMM
   ├─ A2DP
   ├─ AVRCP
   └─ SPP
```

---

## 5.2 能力信息来源

后续可记录：

```text
Source
Evidence
Comment
Updated By
Updated At
```

Source 可包括：

```text
Customer Provided
Tester Discovered
Document
Automatic Detection
Coordinator Input
Other
```

但这些字段不应成为项目创建的强制输入。

---

# 6. 两种测试生成模式

系统支持两种测试生成方式。

## 6.1 Full-Profile Generation

适用于设备信息相对完整的项目。

流程：

```text
Standard / Task Type
+
Device Capabilities
+
Generation Rules
↓
Recommended Test Cases
↓
Test Coordinator 人工调整
↓
Project Test Plan
```

系统一次性生成较完整的建议测试集。

---

## 6.2 Progressive Generation

适用于初始信息不完整、需要边测试边继续展开的项目。

测试协调员明确选择：

```text
Progressive Generation
```

执行逻辑不依赖协调员逐层批准。

流程：

```text
Initial Test Case
↓
Tester 执行
↓
选择 Decision Point
↓
系统读取 Transition
↓
自动激活 NEXT_CASE / NEXT_CASES
↓
自动分发
↓
继续测试
↓
PASS / FAIL / N/A
↓
当前分支结束
```

---

## 6.3 模式选择

系统可以根据设备信息完整度提示推荐模式。

但是：

> 最终使用哪一种模式，由 Test Coordinator 确认。

不能由系统强制决定。

---

# 7. 公共 Test Case Library

公共 Test Case Library 是系统核心。

它同时承担：

```text
正式测试用例数据库
新人学习知识库
项目测试计划数据源
递进式测试逻辑数据源
```

所有登录用户可以：

```text
查看
搜索
学习
查看历史版本
查看逻辑图
查看工具
查看附件
查看 Deprecated 用例
```

---

# 8. Test Case 主分类

公共 Test Case 按：

**技术 / 攻击面**

进行主分类。

不按：

```text
EN 18031
FDA
PSTI
EN 303 645
```

作为主目录。

标准只用于：

```text
映射
筛选
Generation Rule
```

---

## 8.1 一级分类

第一版建议：

```text
Network
Wi-Fi
Bluetooth
Physical Interface
Firmware
Software Update
Authentication & Access Control
Cryptography & Key Management
Logging
Data Storage
Mobile Application
Cloud & API
Fuzzing & Robustness
```

---

## 8.2 二级分类

最多两级目录。

例如：

```text
Bluetooth
├─ Discovery
├─ BLE
├─ BR/EDR
├─ Pairing & Bonding
├─ Access Control
└─ Encryption
```

更细的分类使用标签，不继续增加目录深度。

---

# 9. 标签与搜索

Test Case 支持多个标签。

例如：

```text
BLE
Pairing
Authentication
Access Control
Beginner
bluetoothctl
```

标签用于：

```text
搜索
筛选
学习
相关推荐
```

**标签不直接驱动自动生成规则。**

---

## 9.1 搜索范围

关键词搜索至少覆盖：

```text
Test Case ID
名称
测试目的
测试步骤
工具名称
标签
```

---

# 10. Test Case 核心结构

当前统一 Test Case 结构：

```text
Test Case
├─ Case ID
├─ Case Name
├─ Category
├─ Tags[]
├─ Test Purpose
├─ Preconditions
├─ Tools[]
├─ Test Steps[]
├─ Selection Mode
│  ├─ SINGLE
│  └─ MULTIPLE
├─ Decision Points[]
├─ Evidence Requirement
├─ Remark Requirement
├─ Attachments[]
├─ Standard Mapping[]
├─ Version
├─ Status
└─ Is Current Version
```

Test Case 的具体内容由编写人员自行配置。

系统只定义结构框架。

---

# 11. 测试步骤

测试步骤采用结构化多步骤。

例如：

```text
Step 1
Title
Content

Step 2
Title
Content
```

复杂操作可以通过附件补充：

```text
脚本
操作手册
拓扑图
示例日志
截图
其他文档
```

---

# 12. Tool Library

测试工具采用独立 Tool Library。

关系：

```text
Test Case
↓
Tools[]
↓
Tool Library
```

一个 Test Case 可以关联多个工具。

具体 Tool 字段后续单独设计。

---

# 13. Decision Point

Decision Point 是递进式 Test Case 的核心。

结构：

```text
Decision Point
├─ Name
├─ Description
├─ Transition Type
│  ├─ NEXT_CASE
│  ├─ NEXT_CASES
│  ├─ PASS
│  ├─ FAIL
│  └─ N/A
├─ Target Test Cases[]
└─ Display Order
```

---

# 14. Selection Mode

Selection Mode 属于 Test Case。

## SINGLE

一次只能选择一个 Decision Point。

例如：

```text
○ BLE
○ BR/EDR
○ Not Supported
```

---

## MULTIPLE

一次允许选择多个 Decision Point。

例如：

```text
☑ BLE
☑ BR/EDR
```

系统同时触发多个后续分支。

---

# 15. 递进式逻辑模型

递进式测试的底层模型为：

```text
Test Case
↓
Decision Point
↓
Transition
↓
NEXT_CASE / NEXT_CASES / PASS / FAIL / N/A
```

Test Case 本身就是逻辑节点。

---

## 15.1 示例

```text
TC-001 Bluetooth Mode Detection

├─ BLE
│   ↓
│  TC-002 BLE Security
│
├─ BR/EDR
│   ↓
│  TC-010 BR/EDR Security
│
└─ Bluetooth Not Supported
    ↓
   N/A
```

TC-002 仍然可以继续包含 Decision Point。

---

# 16. DAG 模型

界面上可以叫：

```text
Tree
Logic Graph
Test Chain
```

但底层应按：

```text
Directed Acyclic Graph
DAG
```

设计。

原因：

> 同一个 Test Case 可能被多个不同前置 Test Case 引用。

第一版不允许循环路径。

---

# 17. PASS / FAIL / N/A

PASS / FAIL / N/A 仅表示：

> 当前测试分支已经得到终止结论。

例如：

```text
Decision Point A → PASS
Decision Point B → FAIL
Decision Point C → N/A
```

它们不直接表示：

```text
整个 Test Case
整个模块
整个项目
```

的最终安全结论。

---

# 18. Test Case Execution Status

Project Test Case 执行状态第一版只使用：

```text
Not Started
In Progress
Completed
```

不使用：

```text
Pending Review
Returned
Awaiting Coordinator Approval
```

因为测试协调员不逐条审核。

---

# 19. MULTIPLE 与 Result 的统一规则

Project Test Case 不强制设置一个独立 PASS / FAIL Result。

例如：

```text
TC-001
Selection Mode = MULTIPLE

Selected:
DP-A → NEXT TC-002
DP-B → FAIL
```

Project Test Case 本身只记录：

```text
Execution Status = Completed
Selected Decision Points[]
Branch Outcomes[]
```

例如：

```text
Branch Outcomes:
DP-A → NEXT_CASE → TC-002
DP-B → FAIL
```

这样避免 MULTIPLE 情况下出现：

```text
Test Case 到底算 PASS 还是 FAIL？
```

的冲突。

---

# 20. Project Test Case

公共 Test Case 被加入项目后形成 Project Test Case。

它是项目中的执行实例。

例如：

```text
Master:
BLE-PAIR-001 v1.3

Project Instance:
Project A / BLE-PAIR-001 v1.3
```

项目实例保存自己的：

```text
Assignees[]
Execution Status
Selected Decision Points
Evidence[]
Notes
Branch Outcomes
Relation Status
Last Modified By
Last Modified At
```

---

# 21. 一个 Test Case 可分配多人

一个 Project Test Case 支持：

```text
Assignees[]
```

例如：

```text
TC-BLE-001
├─ Tester A
├─ Tester B
└─ Tester C
```

用于多人并行协作。

---

# 22. 多人共享执行记录

同一个 Project Test Case 的所有 Assignee：

**共享同一份执行记录。**

共享：

```text
Evidence[]
Notes[]
Selected Decision Points
Branch Outcomes
Execution Status
```

所有 Assignee 都可以操作。

---

# 23. 自动触发与自动分发

这是 Progressive Generation 的核心规则。

当前 Test Case 选择 Decision Point 后：

```text
Tester 完成当前 Test Case
↓
选择 Decision Point
↓
系统读取 Transition
↓
找到 NEXT_CASE / NEXT_CASES
↓
自动激活 Project Test Case
↓
自动继承当前 Test Case 的 Assignees[]
↓
自动出现在这些 Tester 的任务列表
```

不需要 Test Coordinator 再次确认。

---

## 23.1 MULTIPLE 自动分发

例如：

```text
TC-001
Assignees:
A
B

Selected:
BLE
BR/EDR
```

系统自动生成 / 激活：

```text
TC-002
Assignees:
A
B

TC-010
Assignees:
A
B
```

协调员后续有需要可以手工调整人员。

---

# 24. Completed 后允许修改

Completed Test Case 允许重新打开修改。

被分配的 Tester 均可重新编辑。

---

## 24.1 不保存完整修改历史

为了避免数据量过大：

**不保存每一次执行修改历史。**

只保存最新数据。

可保留：

```text
Last Modified By
Last Modified At
```

---

# 25. 修改不影响后续逻辑时

如果修改内容不会改变：

```text
Decision Point
Transition
后续 Test Case
```

则：

```text
直接保存
```

系统不处理后续测试链。

---

# 26. 修改影响后续关系时

如果修改会影响已经形成的后续关系：

系统必须进行：

```text
风险提示
↓
二次确认
```

并允许选择：

```text
原节点
其他节点
增加节点
不使用节点
```

---

# 27. 原节点

继续使用原来已经连接的后续 Test Case。

系统保留原关系。

---

# 28. 其他节点

选择当前逻辑图中已经存在的其他 Test Case。

建立新的连接关系。

---

# 29. 增加节点

新增一个 Project Test Case 节点，并连接到当前 Test Case。

如果是公共用例库中已有的 Test Case，可以直接引用。

如果没有合适用例，可后续支持 Project Custom Test Case。

---

# 30. 不使用节点

选择：

```text
不使用节点
```

时只断开当前边。

原则：

> **断边，不删节点。**

例如：

```text
TC-001
↓
TC-002
↓
TC-003
↓
TC-004
```

修改后：

```text
TC-001


TC-002
↓
TC-003
↓
TC-004
```

只删除：

```text
TC-001 → TC-002
```

TC-002 后面的关系继续存在。

---

# 31. 游离用例 / 游离分支

断开前置关系后：

```text
TC-002 → TC-003 → TC-004
```

成为：

```text
Floating Branch
```

游离分支仍然：

```text
属于当前项目
保留 Assignees
保留已有 Evidence
保留 Notes
保留执行状态
保留内部连接
可以继续执行
```

---

# 32. Relation Status

Project Test Case 增加关系状态：

```text
CONNECTED
FLOATING
```

它和 Execution Status 分离。

例如：

```text
Execution Status:
In Progress

Relation Status:
FLOATING
```

---

# 33. 游离分支重新连接

Floating Branch 可以重新连接到其他节点。

例如：

```text
TC-100
↓
TC-002
↓
TC-003
↓
TC-004
```

重新连接时：

```text
TC-003
TC-004
```

不需要重新生成。

---

# 34. Logic Graph 功能

递进式 Test Case 详情页提供：

```text
查看逻辑图
```

打开后显示该逻辑链的完整关系。

---

# 35. Logic Graph 展示内容

至少显示：

```text
Test Case Nodes
Decision Point
Transition
NEXT_CASE / NEXT_CASES
PASS
FAIL
N/A
当前 Test Case
Floating Branch
```

---

# 36. 当前节点高亮

从某个 Test Case 打开 Logic Graph 时：

> 当前 Test Case 必须明显高亮。

---

# 37. 图内跳转

Logic Graph 中每个 Test Case 节点都可以点击。

点击后：

```text
Logic Graph
↓
Test Case Node
↓
打开对应 Test Case 详情
```

再次打开逻辑图时，新 Test Case 成为当前高亮节点。

---

# 38. 两种逻辑图视图

逻辑图支持：

## Full Logic Graph

显示：

```text
所有可能节点
所有 Decision Point
所有可能路径
```

主要用于学习和理解测试链。

---

## Current Execution Path

突出：

```text
已执行节点
已选择 Decision Point
当前激活节点
PASS / FAIL / N/A 分支
Floating Branch
```

未走路径弱化显示。

---

# 39. 未激活 Test Case

Tester 可以点击未被当前项目激活的 Test Case 查看详情。

用于学习。

但是：

> 点击查看不能自动把该 Test Case 加入项目。

正式进入项目只能通过：

```text
Decision Point / Transition 自动触发
或
Test Coordinator 手工加入
```

---

# 40. 公共 Test Case 生命周期

第一版正式状态统一为：

```text
Draft
Review
Published
Deprecated
```

为了区分多个 Published 历史版本：

增加：

```text
Is Current Version
YES / NO
```

---

# 41. Published 版本不可直接修改

正式 Published Test Case 不能原地覆盖。

需要修改时：

```text
Published v1.2
↓
Create Revision
↓
Draft v1.3
↓
Review
↓
Published v1.3
```

---

# 42. Change Request

Tester 发现公共 Test Case 有问题时：

```text
Tester
↓
Submit Change Request
↓
Test Coordinator
```

如果不需要修改：

```text
Rejected
```

如果需要修改：

```text
Create Revision
```

---

# 43. Draft 协作

Revision 创建后：

```text
Owner:
Test Coordinator

Contributors:
问题提出人
其他授权人员
```

问题提出人可以获得：

> 指定 Draft Revision 的临时编辑权限。

不获得公共 Test Case Library 的全局修改权限。

---

# 44. 管理员审核

Draft 提交 Review 后：

Admin 支持：

```text
Publish
Return
Reject
```

## Publish

发布新版本。

## Return

退回继续修改。

## Reject

拒绝本次 Revision。

---

# 45. 旧版本处理

新版本发布后：

旧版本：

```text
继续保留
继续可搜索
继续可查看
继续用于历史项目追溯
Is Current Version = NO
```

新版本：

```text
Published
Is Current Version = YES
```

---

# 46. 已完成项目的版本保护

例如：

```text
Project A

BLE-PAIR-001 v1.2
```

公共 Library 后续发布：

```text
BLE-PAIR-001 v1.3
```

Project A 继续使用：

```text
v1.2
```

系统不能自动替换。

---

# 47. 进行中项目版本提示

如果项目正在执行：

```text
Current:
BLE-PAIR-001 v1.2

Latest:
BLE-PAIR-001 v1.3
```

系统提示：

```text
New Version Available
```

Test Coordinator 可：

```text
Keep Current Version
Upgrade
View Diff
```

系统不能自动升级。

---

# 48. Diff

版本升级时应支持查看：

```text
测试步骤变化
Decision Point 变化
工具变化
证据要求变化
附件变化
其他字段变化
```

具体 UI 后续设计。

---

# 49. Deprecated

Deprecated Test Case：

```text
不建议新项目继续使用
```

但是仍然：

```text
可搜索
可查看
可学习
可查看历史版本
可查看替代关系
```

---

# 50. Excel 导出

系统需要支持项目测试计划导出 Excel。

第一阶段至少支持：

```text
完整测试表
简版任务表
```

具体字段在后续项目测试总表设计阶段确定。

---

# 51. 当前正式废弃的旧概念

以下概念不再作为 V0.2 正式模型使用。

## 51.1 独立 Progressive Tree Node

废弃：

```text
Tree Node
↓
Mapping
↓
Test Case
```

现在统一为：

```text
Test Case = Graph Node
```

---

## 51.2 DRILL_DOWN

废弃：

```text
DRILL_DOWN
```

继续测试统一通过：

```text
NEXT_CASE
NEXT_CASES
```

表达。

---

## 51.3 叶子结果向上汇总

暂不采用：

```text
ALL_LEAVES_FAIL
ANY_FAIL
MANUAL_REVIEW
```

作为 Progressive Test 的核心逻辑。

后续如果需要项目总体判定，再单独设计 Result Aggregation。

---

## 51.4 Fail Fast / Fail Scope

暂不采用：

```text
Fail Fast
Current Branch
Current Module
Project Fail
```

作为 Decision Point 本身的逻辑。

FAIL 当前只表示：

> 当前分支终止为 FAIL。

---

## 51.5 Test Coordinator 逐条审核

废弃流程：

```text
Tester Submit
↓
Coordinator Review
↓
Completed
```

现在：

```text
Tester
↓
Completed
```

Progressive 后续节点立即根据 Decision Point 自动处理。

---

## 51.6 独立 PASS / FAIL Criteria 字段

不再把：

```text
PASS Criteria
FAIL Criteria
N/A Criteria
```

作为 Test Case 独立核心字段。

判断与跳转统一通过：

```text
Decision Points[]
```

表达。

---

# 52. 当前核心数据对象

从产品模型角度，当前至少有以下核心对象：

```text
User
Role

Project
Device Capability

Master Test Case
Test Case Version
Decision Point
Transition

Tool

Project Test Case
Project Test Assignment
Evidence
Note
Branch Outcome

Change Request
Revision

Category
Tag
Standard Mapping
Generation Rule
```

这里只确定对象，不进入数据库表设计。

---

# 53. 当前完整业务链

```text
Test Coordinator
↓
Create Project
↓
填写已知信息
↓
选择 Standard / Task Type
↓
选择 Generation Mode
↓
生成 / 选择 Project Test Cases
↓
调整测试计划
↓
分配 Tester
↓
Tester 执行
↓
选择 Decision Point
↓
NEXT_CASE / NEXT_CASES
↓
系统自动激活
↓
自动继承 Assignees
↓
自动分发
↓
继续执行
↓
PASS / FAIL / N/A
↓
当前分支结束
```

测试协调员只在需要时：

```text
手工增加用例
删除 / 调整用例
调整 Tester
处理版本升级
```

---

# 54. 当前不急于确定的内容

以下内容后续再设计：

```text
Tool Library 详细字段
Dashboard
项目测试总表 UI
Tester 首页 UI
Generation Rule 详细规则
能力库完整字段
Excel 具体列
Project Custom Test Case 详细流程
最终项目 Result Aggregation
项目整体 PASS / FAIL 规则
统计报表
通知系统
```

---

# 55. V0.2 当前结论

当前正式架构可以概括为：

```text
Master Test Case Library
        ↓
Project Test Plan
        ↓
Project Test Case
        ↓
Assignees[]
        ↓
Tester Execution
        ↓
Decision Point
        ↓
Transition
        ↓
NEXT_CASE / NEXT_CASES
        ↓
自动激活 + 自动分发
        ↓
继续递进
```

递进式关系采用：

```text
DAG
```

执行结束采用：

```text
PASS
FAIL
N/A
```

项目任务执行状态采用：

```text
Not Started
In Progress
Completed
```

多人执行采用：

```text
Shared Execution Record
```

修改后续关系采用：

```text
风险提示
+
原节点 / 其他节点 / 增加节点 / 不使用节点
```

不使用节点时采用：

```text
断边不删节点
↓
Floating Branch
```

公共 Test Case 更新采用：

```text
Change Request
↓
Revision
↓
Review
↓
Publish
```

这套模型作为后续数据库、接口与 UI 设计的统一基础。
---

# 56. 项目测试总表

项目测试总表采用：

> **一行 = 一个 Project Test Case**

即使一个 Test Case 被分配给多个 Tester，也不拆成多行。

例如：

```text
BLE-001 | Bluetooth Mode Detection | 张三、李四 | In Progress
BLE-002 | BLE Pairing Test          | 王五       | Not Started
```

多人共享同一条 Project Test Case 执行记录。

---

# 57. 项目测试总表默认列

第一版默认列：

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

---

# 58. Source

Source 表示该 Project Test Case 是如何进入当前项目的。

建议：

```text
Initial
项目开始时加入

Generated
根据设备能力和生成规则产生

Progressive
由 Decision Point 自动触发

Manual
Test Coordinator 手工加入
```

用于回答：

> 为什么这个 Test Case 会出现在当前项目中？

---

# 59. Type

Project Test Case 类型：

```text
Independent
Progressive
```

Independent：

```text
独立执行
不依赖递进逻辑
```

Progressive：

```text
属于 Decision Point / Transition 递进逻辑
```

---

# 60. Decision / Branch Summary

项目总表不强制显示单一 PASS / FAIL Result。

Decision / Branch Summary 用于概括当前 Test Case 的分支情况。

例如：

```text
BLE → TC-002

BLE + BR/EDR
→ TC-002
→ TC-010

1 FAIL Branch

PASS

N/A
```

具体信息进入 Test Case 详情查看。

---

# 61. Evidence Count

Evidence 在总表中只显示数量。

例如：

```text
0
2
5
```

点击后进入 Test Case 详情查看具体文件。

---

# 62. 测试工具不作为默认列

Test Tools 不作为项目测试总表默认列。

原因：

```text
一个 Test Case 可能包含多个工具
工具名称会明显增加表格宽度
```

工具信息放在：

```text
Test Case 详情页
```

或后续支持：

```text
用户自定义显示列
```

---

# 63. 项目测试总表筛选

第一版支持：

```text
Category
Source
Assignee
Execution Status
Relation Status
Type
```

并支持：

```text
只看我的用例
```

用于 Test Coordinator 同时兼任 Tester 的场景。

---

# 64. 项目测试总表批量操作

第一版支持：

```text
批量分配 Tester
批量调整 Tester
批量加入项目
批量移出项目
```

具体权限遵循 Test Coordinator 权限。

---

# 65. 项目测试总表单行操作

单行主要操作：

```text
打开 Test Case
查看逻辑图
调整 Assignees
```

不在总表堆叠大量执行编辑按钮。

具体测试执行进入详情页完成。

---

# 66. Tester 测试执行页

Tester 执行页统一采用以下结构：

```text
Test Case 基本信息
↓
前置条件
↓
测试工具
↓
测试步骤
↓
Evidence[]
↓
Notes[]
↓
Decision Points[]
↓
Complete Test Case
```

---

# 67. Test Case 基本信息

页面顶部展示当前测试用例的基础信息。

例如：

```text
Case ID
Case Name
Category
Type
Execution Status
Assignees
```

递进式用例可以提供：

```text
查看逻辑图
```

---

# 68. 前置条件

Preconditions 属于整个 Test Case。

用于告诉 Tester：

```text
执行当前测试前需要满足什么条件
```

具体内容由 Test Case 作者配置。

---

# 69. 测试工具

Tools 属于整个 Test Case。

一个 Test Case 可以关联多个 Tool Library 项。

Tester 可在执行页查看工具信息。

---

# 70. 测试步骤

Test Steps 使用结构化多步骤。

例如：

```text
Step 1
Title
Content

Step 2
Title
Content
```

测试步骤只负责：

> **怎么测**

不负责控制递进跳转。

---

# 71. Decision Point 统一放在 Test Case 底部

整个 Test Case 共用一组：

```text
Decision Points[]
```

不把 Decision Point 绑定到单独测试步骤。

设计原则：

```text
Test Steps
负责怎么测

Decision Points
负责测完以后怎么走
```

Decision Points 统一放在执行页底部。

---

# 72. SINGLE Decision Point UI

如果：

```text
Selection Mode = SINGLE
```

Tester 只能选择一个 Decision Point。

例如：

```text
○ BLE
○ BR/EDR
○ Not Supported
```

---

# 73. MULTIPLE Decision Point UI

如果：

```text
Selection Mode = MULTIPLE
```

Tester 可以同时选择多个 Decision Point。

例如：

```text
☑ BLE
☑ BR/EDR
☐ Not Supported
```

系统根据所有被选择节点同时处理 Transition。

---

# 74. Evidence

整个 Project Test Case 共用一个：

```text
Evidence[]
```

Evidence：

- 不绑定具体 Step
- 不绑定具体 Decision Point
- 直接属于当前 Project Test Case

---

# 75. Evidence 结构

第一版保持轻量：

```text
Evidence
├─ File
├─ Description
└─ Uploaded By
```

不强制增加：

```text
Evidence Type
Evidence Status
Evidence Review Status
```

避免把系统做成正式报告管理系统。

---

# 76. Evidence 文件

Evidence 可以上传实际测试证据文件。

例如：

```text
Screenshot
Log
PCAP
TXT
CSV
ZIP
BIN
Photo
Video
Other
```

系统不要求 Tester 必须先选择 Evidence Type。

具体文件格式以实际上传为准。

---

# 77. Notes

整个 Project Test Case 共用一个：

```text
Notes[]
```

多人共享查看。

但是不是多人共同编辑同一段文本。

采用：

```text
一条备注 = 一条 Note
```

---

# 78. Note 结构

每条 Note 至少包含：

```text
Author
Content
Created At
```

页面示例：

```text
张三：
设备只能通过配套 APP 建立 BLE 连接。

李四：
bluetoothctl 直接配对失败。

王五：
建议继续验证 GATT 访问控制。
```

每条备注前必须显示：

```text
用户名 / Author
```

---

# 79. Note 编辑原则

Notes 采用追加模式：

```text
Add Note
```

而不是所有人共同编辑一个 Shared Text。

建议：

```text
作者可以修改自己的 Note
作者可以删除自己的 Note
其他 Tester 不能直接修改别人的 Note
```

---

# 80. Complete Test Case

Tester 执行完成后点击：

```text
Complete Test Case
```

系统将：

```text
Execution Status
↓
Completed
```

如果 Decision Point 包含：

```text
NEXT_CASE
NEXT_CASES
```

则立即执行 Progressive 自动触发和自动分发。

---

# 81. Complete Test Case 的最小强制校验

点击 Complete Test Case 时，只进行必要校验。

## 校验 1：Decision Point

检查是否已经满足当前：

```text
Selection Mode
```

要求。

例如：

```text
SINGLE
必须选择 1 个

MULTIPLE
至少选择 1 个
```

---

## 校验 2：Evidence Requirement

如果当前 Test Case 明确配置：

```text
Evidence Requirement
```

要求必须提交证据，则检查 Evidence 是否满足最基本要求。

---

# 82. 不增加更多强制校验

第一版不额外强制：

```text
必须填写备注
必须填写所有步骤
必须上传指定文件类型
必须经过 Test Coordinator 审核
必须填写额外 Result
必须填写风险评级
必须填写测试时间
```

原则：

> **只做必要约束，不增加 Tester 的无意义操作负担。**

---

# 83. Complete 后自动递进

Progressive Test Case 完成后：

```text
Complete Test Case
↓
读取 Selected Decision Points[]
↓
执行 Transition
```

如果：

```text
PASS
FAIL
N/A
```

则对应当前分支结束。

如果：

```text
NEXT_CASE
NEXT_CASES
```

则：

```text
自动激活后续 Project Test Case
↓
自动继承当前 Assignees[]
↓
自动分发
↓
进入对应 Tester 的任务列表
```

不需要 Test Coordinator 确认。

---

# 84. 本阶段新增确认原则

1. 项目总表一行对应一个 Project Test Case。
2. 多 Tester 不拆成多行。
3. Test Tools 不作为项目总表默认列。
4. Source 必须可以区分 Initial / Generated / Progressive / Manual。
5. 总表使用 Decision / Branch Summary，而不是强制单一 PASS / FAIL Result。
6. Tester 执行页按 Test Case 级别组织。
7. 一个 Test Case 共用一组 Decision Points。
8. Decision Points 统一位于执行页底部。
9. Evidence 为 Test Case 级共享区域。
10. Evidence 不绑定 Step 和 Decision Point。
11. Notes 为 Test Case 级共享区域。
12. Notes 使用逐条追加模式。
13. 每条 Note 必须显示 Author。
14. Complete Test Case 只做最小必要校验。
15. 不增加额外强制填写负担。
16. Complete 后 Progressive Transition 自动执行。
17. NEXT_CASE / NEXT_CASES 自动激活并自动分发。
18. 后续用例默认继承当前 Test Case 的 Assignees[]。

---

# 85. 下一阶段

下一阶段设计：

**“我的测试”任务中心与自动分发后的任务呈现**

需要确定：

```text
Tester 登录后首先看到什么
项目和 Test Case 如何分组
新自动分发的 Test Case 如何提示
如何区分 Initial / Progressive / Manual
多人共享任务如何显示
Floating Branch 如何显示
Completed 如何快速找到
任务数量如何统计
```

完成这一部分后，再进入：

```text
Generation Rule
+
Device Capability
```

的最终规则设计。
---

# 86. “我的测试”任务中心

Tester 登录后，任务中心首先按：

```text
Project
```

进行分组。

不把所有 Project Test Case 混在一个全局表中。

---

# 87. Tester 项目列表

“我的测试”首页显示当前用户参与的项目。

示例：

```text
Project A

Device:
Wireless Speaker

我的任务:
12

新任务:
2

进行中:
3

已完成:
7

未开始:
2
```

点击项目后进入该项目的 Tester 任务视图。

---

# 88. 进入项目后的默认视图

Tester 进入项目后：

> **默认只显示“我的用例”。**

即：

```text
当前用户 ∈ Assignees[]
```

的 Project Test Case。

同时提供：

```text
[ 我的用例 ]
[ 项目全部用例 ]
```

默认选中：

```text
我的用例
```

---

# 89. “我的用例”包含范围

只要当前用户属于 Assignees[]，均显示。

包括：

```text
Initial
Generated
Progressive
Manual
Floating
```

---

# 90. “我的用例”页签

第一版使用：

```text
[ 全部 ]
[ 未开始 ]
[ 进行中 ]
[ 已完成 ]
[ 游离 ]
```

默认打开：

```text
全部
```

---

# 91. 页签规则

## 全部

显示当前用户被分配的全部 Project Test Case。

NEW 用例优先排序。

## 未开始

```text
Execution Status = Not Started
```

## 进行中

```text
Execution Status = In Progress
```

## 已完成

```text
Execution Status = Completed
```

## 游离

```text
Relation Status = FLOATING
```

游离筛选与 Execution Status 独立。

因此 Floating Test Case 可以同时是：

```text
Not Started
In Progress
Completed
```

---

# 92. 新任务提示

Progressive 自动分发或其他新加入任务，不建设复杂通知系统。

第一版只使用：

```text
项目卡片上的新任务数量
+
任务列表中的 NEW 标记
```

例如：

```text
Project A
New Tasks: 2
```

任务列表：

```text
NEW TC-BLE-003
NEW TC-BLE-004
```

---

# 93. NEW 标记

NEW：

- 不是 Execution Status
- 不是 Relation Status
- 只表示当前 Tester 尚未查看该新任务

Tester 打开该 Test Case 后：

```text
NEW
↓
已读
```

标记消失。

---

# 94. 多人共享任务显示

如果 Project Test Case 被多人共享执行：

```text
Assignees:
张三
李四
王五
```

则 Tester 在“我的用例”列表中默认看到协作者。

示例：

```text
TC-BLE-001
BLE Pairing Test

状态：
In Progress

来源：
Progressive

协作者：
张三、李四、王五

最后修改：
李四 · 14:32
```

---

# 95. 协作者人数较多时

列表中不必展开全部用户名。

例如：

```text
张三、李四 +3
```

点击后再查看完整 Assignees[]。

---

# 96. Progressive 触发来源

仅当：

```text
Source = Progressive
```

时，在任务列表中默认显示触发来源。

例如：

```text
Triggered By:
TC-BT-001 / BLE
```

或：

```text
触发来源：
TC-BT-001 / Decision Point: BLE
```

---

# 97. 多个触发来源

同一个 Test Case 可能由多个前置路径触发。

列表可简化显示：

```text
触发来源：2 个
```

展开后显示：

```text
TC-BLE-001 / GATT
TC-BT-005 / BLE Service Found
```

用于解释：

> 为什么这个 Progressive Test Case 会出现在当前项目中？

---

# 98. 非 Progressive Source

以下 Source：

```text
Initial
Generated
Manual
```

默认不显示：

```text
Triggered By
```

避免任务列表信息过载。

---

# 99. Floating / 游离任务显示

当：

```text
Relation Status = FLOATING
```

时，在“我的用例”列表中显示明显标识：

```text
⚠ FLOATING
```

并可显示轻量说明：

```text
当前没有有效前置连接
```

---

# 100. Floating 不阻塞执行

FLOATING 只表示：

```text
当前没有有效前置连接
```

不表示：

```text
任务失效
任务不可执行
任务必须删除
```

Floating Test Case 仍然可以：

```text
打开
查看
执行
上传 Evidence
添加 Notes
选择 Decision Point
Completed
```

---

# 101. 任务中心当前确认原则

1. Tester 首先按项目查看任务。
2. 进入项目后默认只显示“我的用例”。
3. 可以切换查看“项目全部用例”。
4. “我的用例”包括 Initial / Generated / Progressive / Manual / Floating。
5. 页签为：全部、未开始、进行中、已完成、游离。
6. 默认打开“全部”。
7. NEW 不属于执行状态。
8. 新任务通过项目卡片数量和 NEW 标记提示。
9. 打开任务后 NEW 标记消失。
10. 多人共享任务默认显示协作者。
11. Source = Progressive 时默认显示 Triggered By。
12. 多前置触发可折叠显示具体来源。
13. Floating 使用醒目标识。
14. Floating Test Case 不阻塞执行。
15. 第一版不建设复杂通知中心。

---

# 102. 下一阶段

下一阶段进入：

**Generation Rule + Device Capability**

核心需要确定：

```text
1. Device Capability 到底记录到什么粒度
2. Generation Rule 如何引用 Capability
3. Standard / Task Type 如何参与生成
4. UNKNOWN 如何处理
5. Full-Profile Generation 如何一次生成
6. Progressive Generation 初始用例如何选择
7. Generation Rule 与 Decision Point 的边界
8. Coordinator 可以如何人工覆盖推荐结果
```

这一阶段完成后，核心产品业务逻辑将基本闭环。

---

# 103. Capability Library

系统维护统一的 Capability Library。项目不能随意创建新的临时能力字段，只能对能力库中已有 Capability 填写当前项目值：

```text
YES
NO
UNKNOWN
```

这样可以保证不同项目结构一致、Generation Rule 可以稳定引用、避免同义字段重复，并便于后续统计与维护。

# 104. Capability Library 管理权限

```text
Admin
- 新增 / 修改 / 停用 Capability
- 调整 Capability 父子关系

Test Coordinator
- 在 Project 中填写 Capability Value
- 审核 Capability Update Request

Tester
- 查看 Project Capability
- 提交 Capability Update Request
```

Tester 默认不直接修改 Project 的正式 Device Capability。

# 105. Capability Update Request

Capability Update Request 保留，但只负责更新设备能力信息，不直接负责 Progressive Test Case 自动展开。

```text
Tester 发现新能力
↓
Submit Capability Update Request
↓
Test Coordinator
↓
Approved / Rejected
```

Approved 后：

```text
更新当前 Project Device Capability
↓
重新运行 Generation Rule
↓
产生新的 Recommended Test Cases
```

Capability Update 与 Progressive 必须严格分开：

```text
Capability Update
→ Generation Rule
→ Recommended Test Cases
→ Coordinator 决定是否加入

Decision Point
→ NEXT_CASE / NEXT_CASES
→ 自动激活
→ 自动分发
```

# 106. UNKNOWN 处理原则

UNKNOWN 不能默认当作 NO。

```text
YES     = 明确适用 / 明确存在
NO      = 明确不适用 / 明确不存在
UNKNOWN = 当前无法确认，不能直接排除
```

Generation Rule V1 支持：

```text
= YES
= NO
= UNKNOWN
!= NO
!= YES
```

例如：

```text
IF Bluetooth = YES
AND BLE = YES
AND Pairing != NO
THEN Recommend BLE-PAIR-001
```

匹配结果：

```text
Pairing = YES      → 匹配
Pairing = UNKNOWN  → 匹配
Pairing = NO       → 不匹配
```

是否允许 UNKNOWN 匹配，由 Rule 作者自行配置。

# 107. Generation Rule 与 Test Case 关系

一条 Generation Rule 可以推荐多个 Test Case；同一个 Test Case 也可以被多条 Generation Rule 推荐。

因此整体关系为：

```text
Generation Rule ←→ Test Case
Many-to-Many
```

例如：

```text
GEN-BLE-001
IF Bluetooth = YES AND BLE = YES
THEN:
- BLE-DISC-001
- BLE-CONNECT-001
- BLE-PAIR-001
- BLE-ENC-001
```

如果多条 Rule 命中同一个 Test Case：

```text
自动去重
↓
推荐列表只出现一次
↓
保留所有 Recommended Because
```

例如：

```text
BLE-ENC-001
Recommended Because:
- GEN-BLE-001
- GEN-EN18031-BT-003
```

# 108. Standard / Task Type 参与 Generation Rule

Generation Rule 条件可以同时引用：

```text
Device Capability
+
Standard / Task Type
```

同一个 Test Case 可以被 EN 18031、FDA、PSTI、EN 303 645 等不同规则复用，不需要按标准复制多份 Test Case。

项目选择多个 Standard / Task Type 时，匹配方式统一为：

```text
ANY
```

即只要 Rule 指定的 Standard / Task Type 存在于项目选择列表中即可命中。

# 109. Generation Rule 条件组合

V1 支持：

```text
AND
OR
Condition Group
```

例如：

```text
IF:
Bluetooth = YES
AND
(
    BLE = YES
    OR
    BR/EDR = YES
)
AND
(
    Standard = EN 18031
    OR
    Task Type = FDA
)
```

第一版不支持无限嵌套，限制为顶层逻辑组 + 一层子 Condition Group。

V1 不允许自定义 Python、JavaScript、SQL 或其他任意代码表达式，全部使用结构化条件配置。

# 110. Generation Mode

Generation Rule 增加适用模式：

```text
FULL
PROGRESSIVE_INITIAL
BOTH
```

FULL：用于 Full-Profile Generation，推荐较完整的适用 Test Case 集合。

PROGRESSIVE_INITIAL：用于 Progressive Generation 的项目开始阶段，只推荐入口、识别、枚举、能力确认类 Test Case。

BOTH：两种模式都适用。

# 111. 项目开始前的 Generation 流程

无论 FULL 还是 PROGRESSIVE_INITIAL，都采用：

```text
Generation Rule 命中
↓
Recommended Test Cases
↓
Test Coordinator 选择 / 调整
↓
加入 Project Test Plan
```

Generation Rule 不直接创建执行任务。

Progressive 项目开始执行后：

```text
Tester 执行 ENTRY Test Case
↓
选择 Decision Point
↓
NEXT_CASE / NEXT_CASES
↓
自动激活
↓
自动继承 Assignees[]
↓
自动分发
```

此阶段不再由 Test Coordinator 逐层确认。

# 112. Generation Rule 状态

V1 只使用：

```text
Enabled
Disabled
```

暂不加入：

```text
Priority
Weight
Override
Exclude Rule
```

原因是当前 Generation Rule 只负责推荐、合并和去重，不存在规则覆盖关系。

Disabled Rule 不参与新的 Generation Run，但不会自动删除历史项目已经生成的 Project Test Case。

# 113. Capability 父子关系

Capability Library 支持父子层级，例如：

```text
Bluetooth
├─ BLE
│  ├─ GATT
│  ├─ Pairing
│  └─ Bonding
└─ BR/EDR
```

规则：

```text
Parent = NO
→ 所有 Child 在 Generation Rule 匹配时视为不可适用

Parent = YES
→ 不代表 Child = YES，Child 可以继续 UNKNOWN

Child = YES
→ Parent 自动向上推导为 YES
```

例如：

```text
GATT = YES
↓
BLE = YES
↓
Bluetooth = YES
```

自动推导出的值标记为：

```text
Source = Derived
```

以区分直接确认值和系统推导值。

# 114. Progressive Role

Test Case 增加：

```text
Progressive Role
├─ ENTRY
└─ NORMAL
```

ENTRY：可以作为 Progressive Initial 的入口 Test Case。

NORMAL：不作为 Progressive Initial 默认入口，只能通过 Decision Point / Transition 触发，或者由 Test Coordinator 手工加入。

当关键 Capability 为 UNKNOWN 时，Progressive Initial 优先推荐 Discovery / Enumeration / Identification / Capability Confirmation 类型的 ENTRY Test Case，而不是直接推荐高度专项测试。

例如：

```text
Bluetooth = YES
BLE = UNKNOWN
BR/EDR = UNKNOWN
```

优先推荐：

```text
BT-DISC-001
BT-MODE-001
BT-SVC-001
```

而不是直接推荐 BLE GATT Fuzz 或 BR/EDR A2DP 专项测试。

# 115. Generation Result 人工控制

Test Coordinator 对 Recommended Test Cases 支持：

```text
Add
Ignore
Manual Add
```

Add：正式加入 Project Test Plan。

Ignore：仅当前 Project 不采用，不修改 Master Test Case，不修改 Generation Rule，也不影响其他项目。

Manual Add：随时从 Master Test Case Library 手工补充其他用例，不要求必须由 Generation Rule 命中。

# 116. 重新 Generation 时保留人工选择

Capability Update 后重新运行 Generation Rule：

```text
已加入 Project Test Plan 的 Test Case
→ 保留

新增命中的 Test Case
→ New Recommended

之前 Ignore 的相同 Test Case
→ 保持 Ignored
```

系统不反复把已经明确 Ignore 的同一用例重新作为全新推荐弹出。

如果 Test Coordinator 后续改变判断，可以主动 Restore Recommendation 或直接 Manual Add。

# 117. 三个核心对象的最终边界

```text
Device Capability
= 设备有什么

Generation Rule
= 项目开始前或能力变化后，推荐应该测什么

Decision Point
= 测试执行过程中，下一步实际进入什么
```

Generation Rule 不负责 Progressive 执行过程中的逐节点递进。

Decision Point 不负责项目初始适用性推荐。

# 118. Full-Profile Generation 最终流程

```text
Project
↓
Standard / Task Type
+
Device Capability
↓
Generation Rule
↓
Recommended Test Cases
↓
Coordinator Add / Ignore / Manual Add
↓
Project Test Plan
```

# 119. Progressive Generation 最终流程

```text
Project
↓
Standard / Task Type
+
Device Capability
↓
Generation Rule
Mode = PROGRESSIVE_INITIAL
↓
ENTRY Test Cases
↓
Coordinator Add / Ignore / Manual Add
↓
Project Test Plan
↓
Tester 执行
↓
Decision Point
↓
NEXT_CASE / NEXT_CASES
↓
自动激活 + 自动分发
```

# 120. 本阶段确认原则

1. Capability 使用统一 Capability Library。
2. Project 不允许随意创建新的能力字段。
3. Capability Value 使用 YES / NO / UNKNOWN。
4. Capability Update Request 保留。
5. Capability Update Request 只负责设备能力更新。
6. Capability Update 不直接驱动 Progressive 自动展开。
7. UNKNOWN 不默认等于 NO。
8. Generation Rule 支持 =YES / =NO / =UNKNOWN / !=NO / !=YES。
9. 是否允许 UNKNOWN 匹配由 Rule 自身决定。
10. 一条 Rule 可以推荐多个 Test Case。
11. 同一个 Test Case 可以被多条 Rule 推荐。
12. Rule 与 Test Case 整体为多对多。
13. 多 Rule 命中同一 Test Case 时自动去重，并保留所有推荐原因。
14. Standard / Task Type 可以参与 Generation Rule。
15. 多 Standard / Task Type 使用 ANY 匹配。
16. Generation Rule 支持 AND / OR / 一层 Condition Group。
17. V1 不允许自定义脚本表达式。
18. Generation Mode 支持 FULL / PROGRESSIVE_INITIAL / BOTH。
19. 项目开始前 Generation Rule 只产生 Recommended Test Cases。
20. Coordinator 决定 Add / Ignore / Manual Add。
21. Progressive 开始执行后由 Decision Point 自动流转。
22. Generation Rule V1 只使用 Enabled / Disabled。
23. V1 暂不加入 Priority / Override / Exclude Rule。
24. Capability 支持父子关系。
25. Parent=NO 时 Child 在匹配上视为不可适用。
26. Parent=YES 不代表 Child=YES。
27. Child=YES 时 Parent 自动推导为 YES。
28. 自动推导值标记为 Derived。
29. Progressive Role 使用 ENTRY / NORMAL。
30. UNKNOWN 时 Progressive Initial 优先选择 ENTRY 识别类用例。
31. 已 Ignore 的推荐在重新 Generation 时保持 Ignored。
32. Generation Rule 与 Decision Point 必须保持职责分离。

# 121. 下一阶段

下一阶段进入：

**系统整体收口与异常场景检查**

主要确认：

```text
1. 项目删除 / 用例移除怎么处理
2. 多人同时编辑同一 Test Case 时怎么办
3. Progressive NEXT_CASE 已存在时是否重复创建
4. 多前置触发同一 Project Test Case 时如何合并
5. Project Custom Test Case 是否保留
6. Excel 导出最低要求
7. 权限矩阵最终核对
8. 是否还有会阻塞数据库设计的业务问题
```

完成这一阶段后，可以进入：

```text
数据模型
↓
数据库表设计
↓
API
↓
前端结构
↓
开发
```
---

# 150. 最终收口：项目与执行异常场景

本章节固化进入数据库设计前最后一轮业务决策。

---

# 151. 同一后续 Test Case 被多个前置节点触发

同一个 Project Test Case 在同一 Project 中只保留一个执行实例。

如果多个前置 Test Case 指向同一个后续 Test Case：

```text
TC-001 / DP-A
        ↓
      TC-010

TC-005 / DP-B
        ↓
      TC-010
```

系统：

```text
不重复创建 TC-010
↓
复用现有 Project Test Case
↓
增加 Triggered By 来源
```

例如：

```text
TC-010

Triggered By:
- TC-001 / DP-A
- TC-005 / DP-B
```

---

# 152. 多前置触发时 Assignees 合并

如果多个前置 Test Case 的 Assignees 不同：

```text
TC-001
Assignees: A, B

TC-005
Assignees: C
```

共同触发 TC-010 时：

```text
TC-010
Assignees:
A
B
C
```

即：

> **Assignees 自动取并集并去重。**

---

# 153. Completed Test Case 再次被触发

如果某个已经：

```text
Execution Status = Completed
```

的 Project Test Case 再次被其他前置节点触发：

```text
复用现有实例
保持 Completed
增加 Triggered By
```

不自动重新变成 In Progress。

---

# 154. Project Status

Project 第一版状态：

```text
Draft
Active
Completed
Archived
```

---

# 155. Project 删除

Project 不采用直接物理删除作为常规业务操作。

默认采用：

```text
Archived / Soft Delete
```

以保留：

```text
Project Test Cases
Evidence
Notes
Decision Points
Branch Outcomes
历史版本绑定
```

---

# 156. Removed Project Test Case

Project Test Case 支持：

```text
Removed
```

语义：

> 当前用例已经从项目有效测试计划中移除。

---

# 157. Project Test Case 移除规则

## Not Started

可以从当前项目有效测试计划中移除。

## In Progress / Completed

不物理删除数据。

改为：

```text
Removed
```

并保留：

```text
Evidence
Notes
Selected Decision Points
Branch Outcomes
Assignees
执行状态
```

---

# 158. Removed Test Case 可恢复

支持：

```text
Removed
↓
Restore
```

恢复后继续使用原有：

```text
Evidence
Notes
Execution Status
Decision Points
Branch Outcomes
```

---

# 159. 移除中间节点

例如：

```text
TC-001
↓
TC-002
↓
TC-003
```

移除 TC-002 时：

```text
TC-001

TC-003
⚠ FLOATING
```

仍采用：

> **断边，不删后续节点。**

---

# 160. Project Custom Test Case

保留：

```text
Project Custom Test Case
```

用于：

```text
厂商私有接口
私有协议
临时测试方法
公共 Library 暂未覆盖的场景
```

---

# 161. Project Custom Test Case 可参与 Progressive

Project Custom Test Case 可以作为：

```text
Decision Point Target
NEXT_CASE
NEXT_CASES
```

参与当前 Project 的 Progressive DAG。

---

# 162. Project Custom Test Case 创建权限

Project Custom Test Case 可以由：

```text
Test Coordinator
Tester
```

创建。

Tester 创建的是当前 Project 内的临时用例，不直接进入 Master Test Case Library。

---

# 163. Custom Test Case 提交公共 Library

如果 Custom Test Case 具有复用价值：

```text
Project Custom Test Case
↓
Submit to Test Case Library
↓
Draft
↓
Review
↓
Published
```

正式发布后：

> 当前 Project 不自动替换原 Custom Test Case。

以后项目可使用正式 Master Test Case。

---

# 164. Project Test Case Source

一个 Project Test Case 可以记录多个 Source。

例如：

```text
Sources:
Generated
Progressive
```

不只保留最初 Source。

---

# 165. Source 去重与解释

Source 可包括：

```text
Initial
Generated
Progressive
Manual
Custom
```

如果同一个 Project Test Case 由多个来源进入项目：

```text
Sources[]
```

自动去重并全部保留。

---

# 166. 修改 Assignees 不回写已生成后续节点

例如：

```text
TC-001
Assignees: A, B
↓
TC-002
Assignees: A, B
```

后来将 TC-001 改为：

```text
A, C
```

已经存在的 TC-002：

```text
不自动同步
仍保持 A, B
```

新的后续节点以后再触发时，继承最新 Assignees。

---

# 167. Project 完成条件

Project 可以标记 Completed，当：

```text
所有未 Removed 的 Project Test Case
都已经 Completed
```

其中：

```text
Floating Test Case
```

仍然属于有效项目任务，因此也需要 Completed。

---

# 168. V1 不自动计算 Project PASS / FAIL

V1 暂不设置：

```text
Project Overall Result = PASS / FAIL
```

系统当前只展示：

```text
Execution Status
Selected Decision Points
Branch Outcomes
PASS / FAIL / N/A Branch
统计信息
```

项目级 Result Aggregation 后续独立设计。

---

# 169. Evidence 删除权限

所有当前 Project Test Case Assignee：

> **都可以删除该共享 Test Case 中的任意 Evidence。**

不限制必须由上传者本人删除。

---

# 170. Notes 最终权限

Notes 规则：

```text
所有相关用户可以查看

Assignee 可以新增 Note

Note 作者可以：
修改自己的 Note
删除自己的 Note

其他 Tester：
不能修改别人的 Note

Test Coordinator：
不需要审核 Note
```

---

# 171. 项目开始后修改 Standard / Task Type

如果 Project 的：

```text
Standard / Task Type[]
```

发生变化：

```text
重新运行 Generation Rule
↓
产生新的 Recommended Test Cases
```

系统：

```text
不自动重做整个 Project Test Plan
不自动移除已有用例
不自动加入新用例
```

仍由 Test Coordinator：

```text
Add
Ignore
Manual Add
```

---

# 172. Capability 修改后用例不再推荐

如果 Capability Update 导致某些已有 Project Test Case 不再适用：

系统只标记：

```text
No Longer Recommended
或
Possibly Not Applicable
```

不自动移除。

由 Test Coordinator 决定是否：

```text
保留
Removed
调整
```

---

# 173. 进行中项目升级 Master Test Case

例如：

```text
BLE-001 v1.2
↓
Upgrade
↓
BLE-001 v1.3
```

Project Test Case 保留当前执行数据：

```text
Evidence
Notes
Execution Status
Assignees
```

并更新绑定的 Master Test Case Version。

如果：

```text
Decision Point Structure
```

发生变化，则提示逻辑风险，由用户确认后继续。

---

# 174. Excel V1

V1 Excel 导出使用：

> **一个工作簿，多个 Sheet。**

至少包含：

```text
Sheet 1: Project Summary
Sheet 2: Test Cases
Sheet 3: Evidence Index
```

实际 Evidence 文件不嵌入 Excel。

Evidence Index 只记录：

```text
文件名
说明
关联 Test Case
上传人
```

---

# 175. Excel Test Cases 字段

V1 Test Cases Sheet 至少包含：

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

# 176. 最终项目权限

## Admin

负责：

```text
全系统管理
用户与角色
Capability Library
Generation Rule
Master Test Case Library
Test Case Review
Publish
Deprecated
系统管理
```

## Test Coordinator

负责：

```text
创建 Project
Project 基本信息
Device Capability
Generation
选择 Project Test Case
增加 / 移除 / 恢复 Project Test Case
调整 Assignees
处理 Capability Update Request
处理 Test Case Change Request
创建 Revision
提交 Review
```

## Tester

负责：

```text
查看 Project
查看项目全部 Test Case
执行自己 Assignees 中的 Test Case
Evidence
Notes
Decision Point
Complete
重新打开修改
Capability Update Request
Test Case Change Request
Project Custom Test Case
```

Tester 不可以直接：

```text
修改 Project 基本信息
修改正式 Device Capability
修改 Assignees
移除正式 Project Test Case
修改 Published Master Test Case
发布 Master Test Case
```

---

# 177. Tester 查看未分配用例

在：

```text
项目全部用例
```

视图中：

```text
Assigned to me
→ 可执行

Not assigned to me
→ 只读
```

未分配给当前 Tester 的 Project Test Case 不能由其直接修改执行数据。

---

# 178. Floating 自动恢复 CONNECTED

Relation Status 根据有效 Incoming Transition 自动判断。

```text
存在至少一个有效 Incoming Transition
→ CONNECTED

不存在任何有效 Incoming Transition
→ FLOATING
```

因此 Floating Branch 后续重新接入图中时：

```text
FLOATING
↓
CONNECTED
```

自动恢复。

---

# 179. Project Test Case 唯一性

同一 Project 内：

> **一个 Master Test Case 对应一个 Project Test Case 实例。**

不会因为：

```text
Source 不同
Triggered By 不同
Assignee 不同
```

重复创建。

---

# 180. Project Test Case Version Upgrade

Master Test Case Version 升级：

```text
v1.2 → v1.3
```

仍然属于同一个 Project Test Case 实例。

只是：

```text
Master Version Binding
```

发生变化。

---

# 181. 最终业务模型收口

当前进入数据库设计前，核心流程已经闭环：

```text
Capability Library
+
Standard / Task Type
↓
Generation Rule
↓
Recommended Test Cases
↓
Coordinator 选择
↓
Project Test Plan
↓
Assignees
↓
Tester Execution
↓
Decision Point
↓
NEXT_CASE / NEXT_CASES
↓
自动激活
↓
自动继承 / 合并 Assignees
↓
继续执行
↓
PASS / FAIL / N/A Branch
```

同时支持：

```text
多人共享执行
多前置合并
Triggered By
Floating Branch
Removed / Restore
Project Custom Test Case
Capability Update
Test Case Revision
Excel Export
```

---

# 182. V0.6 之后

产品业务逻辑设计阶段至此收口。

下一阶段正式进入：

```text
Implementation Plan V1.0
↓
Data Model
↓
Database Schema
↓
API Design
↓
Frontend Structure
↓
Development
```
