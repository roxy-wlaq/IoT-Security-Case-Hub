# IoT-Security-Case-Hub
## 系统统一设计 V0.2

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
