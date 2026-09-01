# IoT-Security-Case-Hub
## 分层下钻式渐进测试生成设计 V0.1

> 本文档定义 IoT-Security-Case-Hub 的 Progressive Generation（渐进式生成）核心逻辑。当前阶段仅进行系统功能与业务逻辑设计，不进入代码实现。

---

# 1. 功能定位

渐进式生成采用：

**分层大纲 + 异常触发下钻 + 递归展开 + 叶子节点判定 + 结果向上汇总**

推荐名称：

**Hierarchical Progressive Generation**

中文：

**分层下钻式渐进测试生成**

核心流程：

```text
上层测试项
   ↓
执行判断
   ↓
PASS
   └─ 当前分支结束，不再展开

异常 / 需要进一步验证
   ↓
展开与该异常相关的下级测试项
   ↓
继续执行
   ↓
必要时继续向下展开
   ↓
直到最底层叶子节点
   ↓
叶子结果汇总
   ↓
形成上层结论
```

---

# 2. 树状层级模型

渐进测试采用树结构。

```text
无线通信安全
│
├─ Bluetooth
│  ├─ 连接与访问控制
│  │  ├─ 未授权连接
│  │  ├─ 配对认证
│  │  └─ Bonding 状态
│  ├─ BLE
│  │  ├─ GATT 权限
│  │  ├─ 链路加密
│  │  └─ SMP 安全
│  └─ BR/EDR
│     ├─ SSP
│     ├─ 链路加密
│     └─ Profile 安全
└─ Wi-Fi
   ├─ STA
   ├─ AP
   └─ 网络通信保护
```

节点类型：

```text
MODULE
CATEGORY
CHECK
TEST
LEAF
```

---

# 3. 渐进生成原则

第一轮不生成整棵树，只生成：

- 顶层测试项
- 基础识别项
- 筛查项
- 测试协调员手动选择的测试项

如果上层节点 PASS：

```text
当前分支结束
```

如果上层节点出现异常：

```text
只展开与该异常相关的下级节点
```

如果下级节点再次异常，则继续递归，直到叶子节点。

---

# 4. “异常”与“FAIL”必须区分

系统必须把：

```text
异常 / 需下钻
```

与：

```text
FAIL
```

分开。

推荐增加状态：

```text
DRILL_DOWN
异常 / 需下钻
```

含义：

> 当前上层测试无法确认满足要求，发现可疑情况，需要继续执行下级测试定位问题。

它不是最终失败。

FAIL 表示：

> 已有足够证据确认安全要求未满足。

---

# 5. 推荐测试状态

```text
NOT_STARTED
未开始

IN_PROGRESS
进行中

PASS
通过

DRILL_DOWN
异常 / 需下钻

FAIL
失败

N/A
不适用

BLOCKED
阻塞

INCONCLUSIVE
无法确定
```

叶子节点不再使用 DRILL_DOWN。

---

# 6. 基本递归逻辑

## PASS

```text
当前节点 PASS
↓
停止展开当前分支
```

## DRILL_DOWN

```text
当前节点 DRILL_DOWN
↓
读取关联的下级规则
↓
生成相关子节点
↓
执行子节点
↓
若子节点仍 DRILL_DOWN
↓
继续递归
```

## 直接 FAIL

部分节点已经能够直接证明存在明确问题，不需要继续展开：

```text
FAIL
↓
Fail Fast = YES
↓
直接产生失败结论
```

---

# 7. 只展开相关子节点

异常后不能简单展开全部 Children。

每个子节点应有：

```text
Activation Condition
触发条件
```

例如：

```text
Parent:
BT-SEC-001

Result:
DRILL_DOWN

Reason:
PAIRING_ANOMALY
```

则生成：

```text
BLE-PAIR-001
BLE-BOND-001
BLE-AUTH-001
```

如果异常原因是：

```text
LINK_ENCRYPTION_UNKNOWN
```

则生成：

```text
BLE-ENC-001
BREDR-ENC-001
```

---

# 8. 叶子节点

叶子节点是最终可执行测试项，没有更下级子项。

最终状态：

```text
PASS
FAIL
N/A
BLOCKED
INCONCLUSIVE
```

---

# 9. 叶子结果向上汇总

父节点需要配置：

```text
Aggregation Policy
结果汇总策略
```

V1 建议支持三种。

## ALL_LEAVES_FAIL

```text
所有适用叶子节点全部 FAIL
↓
父节点 FAIL
↓
Remediation Required = YES
```

用于表达：

> 该问题已被全部底层验证项确认，需要客户侧修改。

## ANY_FAIL

```text
任意适用子项 FAIL
↓
父节点 FAIL
```

适用于任一关键安全控制失效就不能接受的节点。

## MANUAL_REVIEW

```text
部分 PASS
部分 FAIL
↓
测试协调员人工判定
```

---

# 10. “全部叶子失败 → 客户整改”

示例：

```text
蓝牙访问控制异常
│
├─ 未授权连接        FAIL
├─ 未授权配对        FAIL
├─ 未授权 Bond       FAIL
└─ GATT 未授权访问   FAIL
```

结果：

```text
所有适用叶子项 = FAIL
↓
父节点 = FAIL
↓
Remediation Required = YES
```

系统提示：

```text
该问题已被全部底层测试项确认，
需要客户侧进行安全机制修改。
```

---

# 11. 部分叶子失败

例如：

```text
Leaf 1 = PASS
Leaf 2 = FAIL
Leaf 3 = PASS
```

不能统一套用“全部叶子失败”的规则。

根据节点配置：

```text
ANY_FAIL
```

则父节点 FAIL；

或者：

```text
MANUAL_REVIEW
```

由测试协调员判断。

因此“所有叶子失败才要求客户修改”应是某些节点的汇总策略，而不是全局固定规则。

---

# 12. Fail Fast：关键项直接整体 FAIL

某些测试项可配置：

```text
Fail Fast = YES
```

并配置失败影响范围：

```text
Fail Scope:

CURRENT_BRANCH
CURRENT_MODULE
PROJECT
```

如果：

```text
Fail Fast = YES
Fail Scope = PROJECT
```

则该节点 FAIL 后：

```text
Project Overall Result = FAIL
```

这对应：

> 某些 FAIL 项可以直接判定整个测试任务 FAIL。

---

# 13. 整体 FAIL 后仍允许继续测试

项目已经 FAIL，不代表剩余测试没有价值。

因此推荐：

```text
Overall Result = FAIL
```

但：

```text
Remaining Tests = Continue
```

继续执行可以收集完整问题清单。

测试协调员可人工停止剩余任务。

---

# 14. 树节点建议字段

```text
Node ID
Node Name
Parent Node
Node Type
Level
Activation Condition
Test Case Mapping
Result
Drill-down Reason
Aggregation Policy
Fail Fast
Fail Scope
Remediation Required
Order
Status
```

---

# 15. 树节点与公共 Test Case 分离

树节点负责：

```text
什么时候展开
展开到哪里
什么情况下停止
如何汇总结论
```

公共测试用例库负责：

```text
具体怎么测试
用什么工具
执行什么步骤
PASS / FAIL 如何判断
需要什么证据
```

关系：

```text
Progressive Tree Node
       ↓
Mapping
       ↓
Master Test Case
```

同一个 Test Case 可以被多个树节点引用。

例如 TLS-PROTO-001 可以同时用于：

```text
Cloud Communication Security
Software Update Security
Remote Management Security
```

---

# 16. 递归遍历伪逻辑

```text
Execute Node

IF Result == PASS:
    Stop Current Branch

IF Result == DRILL_DOWN:
    Find Related Child Nodes
    Generate Child Tests

    FOR each Child:
        Execute Child

        IF Child Result == DRILL_DOWN:
            Repeat Recursively

    When all applicable leaves complete:
        Aggregate Results

IF Result == FAIL
AND Fail Fast == YES:
    Apply Fail Scope
```

---

# 17. 测试协调员保留人工控制权

测试协调员可以：

```text
手动增加测试项
手动跳过测试项
标记 N/A
调整测试顺序
强制展开某个分支
停止某个分支
要求继续深入测试
修改项目最终判定
```

人工覆盖必须记录：

```text
Operator
Time
Reason
Before
After
```

---

# 18. Force Expand

建议支持：

```text
Force Expand
强制展开
```

即使上级已经 PASS，测试协调员仍可主动展开子项。

适用于：

- 高风险项目
- 内部全面测试
- 特殊客户要求
- 对某项有额外怀疑
- 新人培训

---

# 19. 页面展示建议

渐进模式建议使用树形测试计划：

```text
▼ Bluetooth Security                  DRILL_DOWN
   ▼ Access Control                   FAIL
      ✓ Device Discovery              PASS
      ✕ Unauthorized Pairing          FAIL
      ✕ Unauthorized Bond             FAIL

   ▼ Encryption                       Testing
      ✓ BLE Encryption                PASS
      ○ BR/EDR Encryption             Pending

   ▶ Profile Security                 Not Generated
```

应能直观看到：

- 哪些节点已经展开
- 为什么展开
- 当前递归到哪一层
- 哪些分支已结束
- 哪些分支触发 FAIL

---

# 20. 自动展开原因必须可解释

每个自动生成的子项都应支持：

```text
Why Generated?
```

例如：

```text
BLE-BOND-001

Generated Because:

Parent:
Bluetooth Access Control

Parent Result:
DRILL_DOWN

Reason:
Unauthorized Pairing Anomaly

Rule:
BT-RULE-012
```

---

# 21. 与完整信息生成模式的区别

完整信息生成：

```text
已知设备能力较完整
↓
一次性匹配大量适用用例
↓
测试协调员选择
```

分层渐进生成：

```text
初始信息有限
↓
生成上层判断项
↓
异常
↓
展开相关子项
↓
继续递归
↓
叶子判定
```

两种模式共享同一个：

```text
Master Test Case Library
```

只是生成策略不同。

---

# 22. V1 建议实现范围

第一版只实现：

```text
Parent → Child
PASS → Stop
DRILL_DOWN → Expand
FAIL_FAST
ALL_LEAVES_FAIL
ANY_FAIL
MANUAL_REVIEW
Force Expand
Why Generated
```

暂不加入：

```text
复杂评分模型
概率模型
AI 自动判定
加权风险树
机器学习规则
```

---

# 23. 当前确定的核心原则

1. 渐进式生成采用树状分级结构。
2. 默认不一次性生成全部底层测试用例。
3. 上层 PASS 时当前分支可以结束。
4. 上层异常时展开相关下级测试。
5. 下级仍异常时继续递归。
6. 递归直到叶子节点。
7. “异常/需下钻”与“FAIL”是不同状态。
8. 叶子节点负责最终测试结论。
9. 叶子结果通过 Aggregation Policy 向上汇总。
10. 某些节点可以配置“所有叶子全部 FAIL → 客户整改”。
11. 某些节点可以配置“任一子项 FAIL → 父节点 FAIL”。
12. 某些情况需要测试协调员人工判定。
13. 某些关键 FAIL 项可以直接触发项目整体 FAIL。
14. 整体 FAIL 后仍允许继续测试，以收集完整问题。
15. 测试协调员始终可以人工增加测试项。
16. 测试协调员可以强制展开分支。
17. 所有自动生成与展开都必须显示原因。
18. 所有人工覆盖必须可追溯。

---

# 24. 下一阶段

下一步设计：

**测试树节点模型 + 公共 Test Case 字段模型**

需要明确：

```text
一个树节点有哪些字段？
一个真正可执行的 Test Case 有哪些字段？
哪些内容属于树结构？
哪些内容属于公共测试用例库？
树节点如何引用 Test Case？
父子节点触发规则如何配置？
Fail Fast 如何配置？
Aggregation Policy 如何配置？
```

这是后续数据库结构和规则引擎设计的直接基础。


---

# 25. 父节点支持多选子节点 / 多测试用例

现实测试中，一个上层问题通常不只对应一个测试点。

例如：

```text
Bluetooth Access Control
```

可能同时需要验证：

```text
☑ 未授权连接测试
☑ 未授权配对测试
☑ Bonding 状态测试
☑ GATT 未授权访问测试
☑ 已绑定设备访问控制测试
```

因此系统不能设计为：

```text
Parent
↓
Single Child
```

而应支持：

```text
Parent
↓
Children[]
```

即：

**一个父节点可以关联多个子节点，也可以关联多个公共 Test Case。**

---

# 26. 多选子节点的两种触发方式

## 26.1 固定多选

某个父节点出现 DRILL_DOWN 后，固定展开一组子项。

例如：

```text
Parent:
BLE Pairing Security

Result:
DRILL_DOWN
```

自动展开：

```text
☑ Passkey Test
☑ Just Works Test
☑ Bonding Test
☑ Re-Pairing Test
```

即：

```text
Trigger
↓
Generate Multiple Children
```

---

## 26.2 按异常原因多选

同一个父节点可以根据不同异常原因，展开不同的子项组合。

例如：

```text
Parent:
Bluetooth Security Check
```

如果：

```text
Reason:
PAIRING_ANOMALY
```

则展开：

```text
☑ BLE-PAIR-001
☑ BLE-BOND-001
☑ BLE-AUTH-001
```

如果：

```text
Reason:
ENCRYPTION_ANOMALY
```

则展开：

```text
☑ BLE-ENC-001
☑ BREDR-ENC-001
```

如果多个异常同时存在：

```text
PAIRING_ANOMALY
+
ENCRYPTION_ANOMALY
```

则系统合并：

```text
BLE-PAIR-001
BLE-BOND-001
BLE-AUTH-001
BLE-ENC-001
BREDR-ENC-001
```

并自动去重。

---

# 27. 规则配置页面支持多选

管理员配置父子规则时，应支持多选。

示例：

```text
父节点：
Bluetooth Access Control

触发条件：
Result = DRILL_DOWN

展开子节点：

☑ Unauthorized Connection
☑ Pairing Authentication
☑ Bonding Security
☑ GATT Access Control
☐ A2DP Security
☐ Firmware Update
```

系统保存为：

```text
Children = [
  Child-1,
  Child-2,
  Child-3,
  Child-4
]
```

---

# 28. 子项执行策略

多个子节点之间不一定总是同一种执行关系。

因此父节点需要增加：

```text
Child Execution Policy
子项执行策略
```

V1 建议支持：

```text
ALL
全部执行

MANUAL_SELECT
测试协调员手动选择
```

后期可扩展：

```text
ANY
满足任一子项即可
```

---

## 28.1 ALL

表示：

> 当前规则触发后，所有被激活的子项都需要执行。

例如：

```text
Parent:
Bluetooth Access Control

Execution Policy:
ALL

Children:
☑ BLE-CONNECT-001
☑ BLE-PAIR-001
☑ BLE-BOND-001
☑ BLE-GATT-AUTH-001
```

以上 4 个测试均进入测试计划。

---

## 28.2 MANUAL_SELECT

表示：

> 系统提供一组候选子项，由测试协调员根据设备实际情况选择。

例如：

```text
Parent:
Bluetooth Profile Security

Execution Policy:
MANUAL_SELECT

Available Children:
☑ GATT
☑ A2DP
☑ AVRCP
☑ SPP
☑ RFCOMM
```

适用于：

- 设备 Profile 尚未完全明确
- 同一父节点存在多种实现方式
- 具体测试范围需要协调员判断

---

# 29. 子项执行策略与结果汇总策略必须分离

需要区分：

```text
Execution Policy
```

和：

```text
Aggregation Policy
```

两者含义不同。

## Execution Policy

决定：

> 哪些子项需要执行。

例如：

```text
ALL
MANUAL_SELECT
```

## Aggregation Policy

决定：

> 子项执行完成后，父节点如何得出结论。

例如：

```text
ALL_LEAVES_FAIL
ANY_FAIL
MANUAL_REVIEW
```

示例：

```text
Node:
Bluetooth Access Control

Execution Policy:
ALL

Aggregation Policy:
ALL_LEAVES_FAIL
```

含义：

1. 所有被激活的子项都需要执行；
2. 当所有适用叶子项全部 FAIL 时，父节点判定 FAIL。

---

# 30. 父节点与 Test Case 的多对多关系

一个父节点可以引用多个 Test Case。

同时，一个 Test Case 也可以被多个父节点复用。

因此实际关系应为：

```text
Tree Node
   ↕
Node-TestCase Mapping
   ↕
Master Test Case
```

即：

**多对多关系。**

示例：

```text
Bluetooth Access Control
├─ BLE-PAIR-001
├─ BLE-BOND-001
└─ BLE-GATT-AUTH-001
```

同时：

```text
BLE-PAIR-001
```

也可能被：

```text
Bluetooth Authentication
Device Binding Security
Access Control Verification
```

等多个树节点引用。

---

# 31. 推荐节点字段补充

在原树节点字段基础上增加：

```text
Children[]

TestCaseMappings[]

Child Selection Mode

Execution Policy
```

完整核心字段建议：

```text
Node ID
Node Name
Parent Node
Children[]
Node Type
Level
Activation Condition
TestCaseMappings[]
Child Selection Mode
Execution Policy
Result
Drill-down Reason
Aggregation Policy
Fail Fast
Fail Scope
Remediation Required
Order
Status
```

---

# 32. 本次新增原则

在原设计原则基础上新增：

19. 一个父节点可以关联多个子节点。
20. 一个父节点可以关联多个 Test Case。
21. 一个 Test Case 可以被多个父节点复用。
22. 父节点与 Test Case 采用多对多映射。
23. 规则配置必须支持子节点多选。
24. 多个异常原因同时命中时，应合并对应子项并自动去重。
25. 父节点需要独立配置 Child Execution Policy。
26. V1 至少支持 ALL 和 MANUAL_SELECT 两种执行策略。
27. Execution Policy 与 Aggregation Policy 必须分开配置。
