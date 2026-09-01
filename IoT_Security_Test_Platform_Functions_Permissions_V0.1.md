# IoT 网络安全测试用例生成与任务分发平台
## 系统功能与权限设计 V0.1

> 当前阶段目标：先确定系统功能、角色权限、公共测试用例库与用例维护流程；暂不进入代码实现。

---

## 1. 系统定位

本系统定位为：

**IoT 网络安全测试用例生成与任务分发平台**

系统只负责：

- 项目测试任务管理
- 设备能力信息录入
- 根据设备能力与测试要求自动推荐测试用例
- 人工补充、删除、调整项目测试用例
- 测试任务分发
- 测试执行与结果提交
- 测试证据管理
- 公共测试用例数据库维护
- 测试用例版本管理
- 测试用例修改申请与审核
- Excel 导出

系统**不负责正式报告编写与报告模板管理**。正式报告由其他系统完成。

---

# 2. 核心业务流程

```text
创建项目
   ↓
填写项目基本信息
   ↓
填写设备能力信息
   ↓
选择测试范围 / 测试标准
   ↓
系统自动推荐测试用例
   ↓
测试协调员审核推荐结果
   ↓
手动增加 / 删除 / 调整测试用例
   ↓
形成项目测试计划
   ↓
分配测试人员
   ↓
测试人员执行测试
   ↓
填写结果 / 上传证据
   ↓
提交测试结果
   ↓
测试协调员审核
   ↓
任务完成
```

系统同时支持：

```text
项目测试计划
   ↓
导出 Excel
   ↓
离线查看 / 内网环境使用
```

---

# 3. 用户角色

系统第一阶段设置三个角色。

## 3.1 系统管理员（Admin）

主要职责：

- 系统配置
- 用户管理
- 角色权限管理
- 公共测试用例数据库管理
- 公共测试用例版本审核
- 测试生成规则管理
- 测试用例分类、标签、能力映射管理
- 测试用例发布、停用、废弃
- 审计日志查看

---

## 3.2 测试协调员（Test Coordinator）

主要职责：

- 创建测试项目
- 填写设备信息
- 填写设备能力
- 生成项目测试计划
- 调整项目测试用例
- 手动增加测试用例
- 删除不适用的项目测试项
- 分配测试任务
- 审核测试结果
- 创建公共测试用例草稿
- 创建测试用例新版本
- 提交公共测试用例审核
- 处理测试人员提交的修改申请

说明：

**测试协调员与测试人员可以是同一个人。**

系统应支持：

```text
一个用户
   ↓
拥有多个角色
```

例如：

```text
用户：张三

角色：
☑ Test Coordinator
☑ Tester
```

---

## 3.3 测试人员（Tester）

主要职责：

- 查看公共测试用例库
- 搜索和学习所有测试用例
- 查看测试用例详细信息
- 查看历史版本
- 查看关联工具和测试方法
- 查看自己被分配的测试任务
- 执行测试
- 填写测试结果
- 上传测试证据
- 填写执行备注
- 提交修改建议
- 提交新增测试用例建议

测试人员不能直接修改正式公共测试用例。

---

# 4. 公共测试用例库定位

公共测试用例库同时承担两个作用：

1. **测试任务数据源**
2. **新人学习知识库**

因此公共测试用例库应对所有登录用户开放查看与搜索权限。

普通测试人员可以主动搜索和学习。

例如：

```text
BLE
Wi-Fi
SWD
UART
USB
TLS
OTA
Firmware
Logging
Fuzzing
```

---

# 5. 测试用例的两种类型

系统必须区分：

## 5.1 公共测试用例（Master Test Case）

属于公司的正式测试知识库。

例如：

```text
BLE-PAIR-001
Unauthorized BLE Pairing Test

BLE-ENC-001
BLE Link Encryption Test

TLS-PROTO-001
TLS Protocol Version Test

SWD-CONNECT-001
SWD Debug Connection Test
```

公共测试用例：

- 可被多个项目复用
- 具有正式版本
- 受审核流程控制
- 不能被项目修改直接覆盖

---

## 5.2 项目测试用例（Project Test Case）

项目创建后，根据设备能力和测试要求，从公共测试用例库生成项目副本。

例如：

```text
Project:
Wireless Speaker EN18031 Test

Generated Cases:

BLE-PAIR-001 v1.2
BLE-ENC-001 v2.0
TLS-PROTO-001 v1.5
```

项目测试协调员可以：

- 增加
- 删除
- 调整
- 分配
- 标记 N/A
- 增加项目说明

但不能直接影响公共测试用例数据库。

---

# 6. 项目临时测试用例

如果公共测试用例库中没有合适用例，测试协调员可以创建：

```text
Project Custom Test Case
```

例如：

```text
CUSTOM-001
厂商私有诊断端口访问测试
```

该用例默认只属于当前项目。

如果认为具有复用价值，可以：

```text
Submit to Test Case Library
```

进入公共测试用例新增申请流程。

---

# 7. 公共测试用例库搜索与学习功能

所有用户均可：

- 查看全部正式测试用例
- 全文搜索
- 按分类筛选
- 按接口筛选
- 按协议筛选
- 按工具筛选
- 按难度筛选
- 按平台筛选
- 按测试方向筛选
- 查看测试步骤
- 查看命令
- 查看 PASS / FAIL 条件
- 查看证据要求
- 查看历史版本
- 查看修改记录
- 查看替代关系
- 查看相关测试用例

推荐支持：

```text
收藏
最近查看
相关用例推荐
```

用于新人自主学习。

---

# 8. 推荐的测试用例分类方式

## 8.1 按接口

```text
Network
Wi-Fi
BLE
BR/EDR
USB
UART
SWD
JTAG
SPI
I2C
NFC
Zigbee
Cellular
```

## 8.2 按测试方向

```text
Information Gathering
Authentication
Authorization
Access Control
Encryption
Key Management
Secure Communication
Firmware
Software Update
Logging
Fuzzing
Robustness
Cloud
API
Mobile APP
Physical Interface
Data Protection
```

## 8.3 按难度

```text
Beginner
Intermediate
Advanced
```

## 8.4 按执行方式

```text
Manual
Semi-Automated
Automated
```

---

# 9. 设备能力输入

系统根据设备能力生成建议测试用例。

设备能力应尽量结构化，而不是依赖大段自由文本。

状态建议使用：

```text
YES
NO
UNKNOWN
```

UNKNOWN 用于客户资料不完整时。

---

## 9.1 系统能力

```text
Operating System
├─ Linux
├─ Android
├─ RTOS
├─ Bare Metal
└─ Other
```

---

## 9.2 网络能力

```text
Network
├─ Ethernet
├─ Wi-Fi
│  ├─ STA
│  ├─ AP
│  └─ Hotspot
├─ Cellular
│  ├─ LTE
│  └─ 5G
├─ IPv4
├─ IPv6
└─ Internet Communication
```

---

## 9.3 无线能力

```text
Bluetooth
├─ BLE
│  ├─ Advertising
│  ├─ Connection
│  ├─ Pairing
│  ├─ Bonding
│  └─ GATT
└─ BR/EDR

Other Wireless
├─ Zigbee
├─ NFC
└─ 433 MHz
```

---

## 9.4 物理接口

```text
USB
UART
SWD
JTAG
SPI
I2C
```

接口可继续描述：

```text
External
Internal
Power Only
Data
Maintenance
Debug
```

---

## 9.5 软件更新

```text
Software Update
├─ Supported
├─ OTA
├─ APP
├─ USB
├─ Local File
├─ Cloud
├─ Secure Channel
├─ Signature Verification
├─ Integrity Verification
└─ Rollback
```

---

## 9.6 应用与云

```text
Mobile APP
Web UI
Local Client
Cloud API
HTTPS
MQTT
WebSocket
```

---

## 9.7 安全能力

```text
TLS
Certificate
Secure Boot
Encryption
Authentication
Access Control
Logging
Audit Log
```

---

# 10. 自动测试用例生成

自动生成逻辑：

```text
项目测试要求
      +
设备能力
      +
生成规则
      ↓
Recommended Test Cases
```

例如：

```text
BLE = YES
```

系统可以推荐：

```text
BLE-DISC-001
BLE-CONNECT-001
BLE-PAIR-001
BLE-BOND-001
BLE-GATT-001
BLE-ENC-001
```

例如：

```text
Wi-Fi = YES
Cloud Communication = YES
HTTPS = YES
```

系统可以推荐：

```text
NET-PORT-001
TLS-PROTO-001
TLS-CIPHER-001
TLS-CERT-001
```

---

# 11. 推荐原因

系统自动生成的每一条测试用例，应显示推荐原因。

例如：

```text
BLE-PAIR-001

Generated Because:

Bluetooth = YES
BLE = YES
BLE Pairing = YES
```

推荐原因便于：

- 测试协调员审核
- 排查规则错误
- 新人理解测试项来源
- 后期规则维护

---

# 12. 项目测试计划编辑

自动生成后，测试协调员可以人工调整。

支持：

- 从公共测试用例库增加
- 删除推荐用例
- 创建项目临时测试用例
- 设置优先级
- 设置执行人员
- 设置 N/A
- 添加项目测试备注
- 批量分配测试人员
- 重新执行适用性分析

手工修改应保留操作原因。

---

# 13. 测试人员任务视图

测试人员应至少有两个视图。

## 13.1 测试总表

示例：

| ID | 测试项 | 分类 | 状态 | 结果 |
|---|---|---|---|---|
| BLE-001 | BLE设备发现 | Bluetooth | 已完成 | PASS |
| BLE-002 | 未授权连接 | Bluetooth | 进行中 | - |
| BLE-003 | BLE配对 | Bluetooth | 未开始 | - |

---

## 13.2 测试详细视图

应显示：

```text
测试编号
测试名称
测试目的
安全风险
适用条件
前置条件
测试环境
测试工具
测试步骤
命令
预期结果
PASS 条件
FAIL 条件
N/A 条件
证据要求
注意事项
参考资料
相关测试用例
历史版本
```

---

# 14. 测试执行结果

推荐支持：

```text
PASS
FAIL
N/A
BLOCKED
INCONCLUSIVE
```

含义：

### PASS
满足预期安全要求。

### FAIL
存在测试失败或安全问题。

### N/A
当前设备不适用该测试。

### BLOCKED
由于环境、资料、设备状态等原因无法执行。

### INCONCLUSIVE
已经执行，但证据不足以得出确定结论。

---

# 15. 测试任务状态

推荐：

```text
未开始
   ↓
进行中
   ↓
待审核
   ↓
已完成
```

特殊状态：

```text
Blocked
N/A
已退回
```

退回流程：

```text
待审核
   ↓
已退回
   ↓
进行中
```

---

# 16. 测试证据

虽然系统不负责报告，但必须保存测试证据。

推荐证据类型：

```text
Screenshot
Terminal Output
Log
PCAP
TXT
CSV
ZIP
BIN
Firmware
Configuration
Photo
Video
Other
```

证据应关联：

```text
项目
测试用例
执行人员
执行时间
测试结果
证据说明
```

---

# 17. Excel 导出

测试协调员应可以导出项目测试计划。

## 17.1 完整测试表

推荐字段：

```text
测试编号
分类
测试名称
测试目的
测试步骤
测试工具
预期结果
PASS 条件
FAIL 条件
执行人员
状态
测试结果
备注
```

## 17.2 简版任务表

```text
测试编号
测试名称
测试人员
状态
结果
备注
```

后期可考虑支持 Excel 结果重新导入系统。

---

# 18. 公共测试用例修改原则

公共测试用例会随着：

- 工具版本变化
- 操作系统变化
- 协议变化
- 测试经验积累
- 测试标准变化
- 安全要求变化

持续更新。

因此不能使用：

```text
直接编辑正式版本 → 覆盖保存
```

必须使用版本控制。

---

# 19. 测试用例生命周期

推荐状态：

```text
Draft
草稿

Pending Review
待审核

Changes Requested
要求修改

Published
正式发布

Superseded
已被新版本替代

Deprecated
已废弃

Archived
归档
```

---

# 20. 测试用例修改流程

## 20.1 测试人员发现问题

```text
Tester
   ↓
发现测试用例问题
   ↓
Submit Change Request
```

---

## 20.2 测试协调员判断

```text
Change Request
      ↓
Test Coordinator
      ↓
┌───────────────┐
│               │
Reject      Create Revision
```

---

## 20.3 创建新版本

例如：

```text
BLE-PAIR-001 v1.2 Published
```

创建：

```text
BLE-PAIR-001 v1.3 Draft
```

修改后：

```text
Submit Review
```

---

## 20.4 管理员审核

```text
Pending Review
       ↓
Admin Review
```

结果：

```text
Reject
```

或者：

```text
Request Changes
      ↓
Draft
```

或者：

```text
Publish
```

---

## 20.5 发布

```text
BLE-PAIR-001 v1.3
Published
```

旧版本：

```text
BLE-PAIR-001 v1.2
Superseded
```

旧版本继续保留。

---

# 21. Change Request 与 Revision 的区别

## Change Request

表示：

> 有人认为测试用例存在问题。

只属于问题反馈。

例如：

```text
CR-2026-0015

Target:
BLE-PAIR-001 v1.2

Issue:
bluetoothctl 命令已过期
```

---

## Revision

表示：

> 已经确认需要修改，并开始编写新版本。

例如：

```text
BLE-PAIR-001 v1.3 Draft
```

流程应保持：

```text
问题反馈
   ↓
Change Request
   ↓
确认问题
   ↓
Revision
   ↓
Review
   ↓
Publish
```

---

# 22. 修改类型

推荐：

```text
Bug Fix
Procedure Update
Tool Update
Criteria Change
Applicability Change
Evidence Change
Standard Mapping Change
New Method
Security Requirement Change
Documentation Update
```

---

# 23. 版本规则

推荐使用：

```text
Major.Minor
```

例如：

```text
1.0
1.1
1.2
2.0
```

## Minor

适用于：

- 命令修正
- 工具说明变化
- 描述优化
- 证据说明调整
- 不影响测试核心逻辑

例如：

```text
1.2 → 1.3
```

## Major

适用于：

- 测试逻辑变化
- PASS / FAIL 标准变化
- 适用条件变化
- 测试目的变化
- 使用新的测试方法

例如：

```text
1.3 → 2.0
```

---

# 24. 历史项目版本保护

公共测试用例发布新版本后：

**历史项目不能自动更新。**

例如：

```text
Project A

Used:
BLE-PAIR-001 v1.2
```

公共数据库：

```text
Latest:
BLE-PAIR-001 v1.3
```

Project A 仍然使用：

```text
v1.2
```

这样保证测试结果可追溯。

---

# 25. 正在进行项目的用例更新

如果正在执行的项目使用旧版测试用例，应提醒测试协调员：

```text
New Test Case Version Available

Current:
BLE-PAIR-001 v1.2

Latest:
BLE-PAIR-001 v1.3
```

测试协调员可以：

```text
View Diff

Keep v1.2

Upgrade to v1.3
```

系统不能自动替换。

---

# 26. Diff 功能

新旧版本必须可以比较。

例如：

```text
BLE-PAIR-001

v1.2 → v1.3
```

显示：

```diff
Test Step:

- pair MAC

+ connect MAC
+ pair MAC
+ info MAC
```

以及：

```diff
PASS Criteria:

- Pairing failed

+ Pairing failed
+ Paired: no
+ Bonded: no
```

---

# 27. 测试用例替代关系

有些旧测试用例不是更新，而是被拆分或替换。

例如：

```text
BLE-SEC-001
Deprecated
```

替换为：

```text
BLE-PAIR-001
BLE-ENC-001
BLE-GATT-001
```

测试用例应支持：

```text
Replaced By
Replaces
Related Cases
Parent Case
Child Cases
```

---

# 28. Deprecated 与 Archived 的搜索原则

公共测试用例库作为学习资料，不应把历史用例完全隐藏。

推荐：

### Published

默认显示。

### Superseded

可搜索，显示：

```text
旧版本
已有新版
```

### Deprecated

可以搜索，但明确提示：

```text
已废弃
不建议用于新项目
```

并显示替代用例。

### Archived

高级筛选后可查看。

### Emergency Disabled

可以查看，但明确标记：

```text
禁止继续执行
```

---

# 29. 紧急停用机制

管理员可以：

```text
Emergency Disable
```

适用于：

- 测试命令存在危险
- 测试步骤可能损坏设备
- 测试结论被确认错误
- 工具存在严重问题

效果：

```text
新项目：
禁止自动生成

进行中项目：
显示高优先级警告
```

---

# 30. 修改原因与 Change Log

创建新版本时必须填写：

```text
Change Reason
```

例如：

```text
原 PASS 判定只依赖 AuthenticationFailed，
不能充分证明设备未建立 Bond，
因此增加 Paired 与 Bonded 状态验证。
```

系统自动形成：

| Version | 日期 | 修改人 | 类型 | 修改说明 |
|---|---|---|---|---|
| 2.0 | 2026-09-01 | 张三 | Major | 修改配对验证逻辑 |
| 1.3 | 2026-07-14 | 李四 | Minor | 更新命令 |
| 1.2 | 2026-05-22 | 王五 | Minor | 增加证据要求 |

---

# 31. 角色权限矩阵

| 功能 | 管理员 | 测试协调员 | 测试人员 |
|---|:---:|:---:|:---:|
| 查看全部公共测试用例 | ✓ | ✓ | ✓ |
| 搜索测试用例 | ✓ | ✓ | ✓ |
| 查看详细步骤 | ✓ | ✓ | ✓ |
| 查看历史版本 | ✓ | ✓ | ✓ |
| 查看修改记录 | ✓ | ✓ | ✓ |
| 查看 Deprecated 用例 | ✓ | ✓ | ✓ |
| 收藏测试用例 | ✓ | ✓ | ✓ |
| 创建项目 | ✓ | ✓ | × |
| 修改项目基本信息 | ✓ | ✓ | × |
| 填写设备能力 | ✓ | ✓ | × |
| 自动生成项目测试用例 | ✓ | ✓ | × |
| 手动增加项目用例 | ✓ | ✓ | × |
| 删除项目用例 | ✓ | ✓ | × |
| 分配测试人员 | ✓ | ✓ | × |
| 执行测试 | ✓ | ✓ | ✓ |
| 提交测试结果 | ✓ | ✓ | ✓ |
| 上传证据 | ✓ | ✓ | ✓ |
| 审核项目测试结果 | ✓ | ✓ | × |
| 提交公共用例修改建议 | ✓ | ✓ | ✓ |
| 提交新增用例建议 | ✓ | ✓ | ✓ |
| 创建公共用例草稿 | ✓ | ✓ | × |
| 创建公共用例 Revision | ✓ | ✓ | × |
| 编辑 Draft | ✓ | 授权范围 | × |
| 提交版本审核 | ✓ | ✓ | × |
| 审核公共测试用例 | ✓ | × | × |
| 发布公共测试用例 | ✓ | × | × |
| Deprecated / Archived | ✓ | × | × |
| Emergency Disable | ✓ | × | × |
| 修改自动生成规则 | ✓ | × | × |
| 用户与权限管理 | ✓ | × | × |
| 查看完整系统审计日志 | ✓ | × | × |

---

# 32. 系统菜单建议

## 管理员

```text
Dashboard
项目管理
我的测试
测试用例库
设备能力库
测试生成规则
修改申请
系统管理
审计日志
```

## 测试协调员

```text
Dashboard
项目管理
我的测试
测试用例库
修改申请
```

## 测试人员

```text
Dashboard
我的测试
测试用例库
我的申请
```

---

# 33. 当前已确定的系统核心边界

## 系统负责

```text
测试项目
设备能力
自动推荐测试用例
测试计划
任务分配
测试执行
证据
测试用例数据库
测试用例版本
修改申请
审核
Excel 导出
```

## 系统不负责

```text
正式测试报告编写
报告模板管理
最终报告生成
```

---

# 34. 下一阶段需要继续确定的内容

当前角色权限与公共测试用例更新机制基本确定。

下一步建议依次设计：

## 第一项：公共测试用例字段结构

确定一条正式 Test Case 包含哪些字段，例如：

```text
编号
名称
分类
标签
适用条件
测试目的
风险说明
前置条件
工具
环境
步骤
命令
PASS 条件
FAIL 条件
N/A 条件
证据要求
注意事项
难度
执行方式
关联能力
关联标准
参考资料
版本
作者
审核人
修改记录
```

## 第二项：设备能力填写表

确定测试协调员创建项目时具体需要填写哪些信息。

## 第三项：自动生成规则模型

确定：

```text
什么设备能力
+
什么测试要求
=
生成什么测试用例
```

## 第四项：项目测试总表

确定总表需要显示哪些列。

## 第五项：测试用例详情页

确定新人实际执行测试时看到的页面结构。

---

# 35. 当前系统设计原则

系统设计需要始终遵循以下原则：

1. **公共测试用例库也是新人学习知识库。**
2. **所有登录用户均可以查看和搜索公共测试用例。**
3. **测试人员可以学习和反馈，但不能直接修改正式内容。**
4. **测试协调员负责项目测试计划和公共用例草稿维护。**
5. **管理员负责正式公共数据库的最终审核与发布。**
6. **公共测试用例必须版本化。**
7. **历史项目必须保留当时使用的测试版本。**
8. **公共测试用例更新不能自动覆盖正在执行或已完成项目。**
9. **自动生成结果必须允许人工调整。**
10. **自动生成的测试用例必须说明推荐原因。**
11. **所有关键修改必须可追溯。**
12. **系统第一阶段聚焦测试用例，不扩展到正式报告系统。**
