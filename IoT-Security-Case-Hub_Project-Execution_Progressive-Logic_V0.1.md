# IoT-Security-Case-Hub
## 项目测试执行与递进逻辑设计 V0.1

> 本文档固化当前已确认的项目 Test Case 分配、多人共享执行、递进式 Decision Point、结果修改及游离分支处理规则。

---

# 1. 一个 Test Case 可分配多人

一个项目中的 Project Test Case 支持同时分配给多个测试人员。

```text
Project Test Case
└─ Assignees[]
   ├─ Tester A
   ├─ Tester B
   └─ Tester C
```

适用于同一项目由多人并行协作测试的场景。

---

# 2. 共享执行记录

多人被分配到同一个 Project Test Case 时：

**共享同一份执行记录。**

```text
Project Test Case
├─ Assignees[]
├─ Shared Evidence[]
├─ Shared Notes[]
├─ Shared Decision Selection
└─ Shared Result
```

所有被分配人员均可：

```text
查看测试内容
执行测试
上传证据
填写备注
选择 Decision Point
修改执行内容
完成 Test Case
重新打开并修改
```

---

# 3. 不设置协调员逐条审核

测试协调员在项目中的主要职责为：

```text
创建项目
填写已知设备信息
选择 / 生成 Test Case
调整项目 Test Case
分配测试人员
必要时手工增删用例
```

测试协调员不需要逐条执行：

```text
审核 PASS
审核 FAIL
审核证据
审核 NEXT_CASE
```

测试结果由 Tester 直接完成。

---

# 4. 执行状态

Project Test Case 第一版使用简单状态：

```text
Not Started
↓
In Progress
↓
Completed
```

Completed 后仍允许被分配 Tester 重新打开并修改。

---

# 5. 修改历史

为了避免执行数据膨胀：

**不保存完整执行修改历史。**

只保留当前最终执行内容。

可保留轻量字段：

```text
Last Modified By
Last Modified At
```

---

# 6. 递进式 Test Case 核心模型

每一个递进式 Test Case 可以包含：

```text
一个或多个 Decision Point
```

Tester 根据实际测试结果选择 Decision Point。

系统根据被选择的 Decision Point 决定：

```text
进入一个下一个 Test Case
进入多个下一个 Test Case
PASS
FAIL
N/A
```

---

# 7. Decision Point 跳转

例如：

```text
TC-001：识别蓝牙类型

Decision Points:
├─ BLE
│   ↓
│  TC-002
│
├─ BR/EDR
│   ↓
│  TC-010
│
└─ 不支持蓝牙
    ↓
   N/A
```

---

# 8. SINGLE / MULTIPLE

Selection Mode 放在 Test Case 级别。

## SINGLE

只能选择一个 Decision Point。

```text
TC-001

○ BLE
○ BR/EDR
○ 不支持蓝牙
```

选择：

```text
BLE
```

进入：

```text
TC-002
```

---

## MULTIPLE

允许同时选择多个 Decision Point。

例如：

```text
☑ BLE
☑ BR/EDR
```

系统同时激活：

```text
TC-002
TC-010
```

---

# 9. 递进关系

下一个 Test Case 本身仍然可以继续包含 Decision Point。

```text
TC-001
   ↓
Decision Point
   ↓
TC-002
   ↓
Decision Point
   ↓
TC-003
```

一直递进，直到某个分支进入：

```text
PASS
FAIL
N/A
```

该分支结束。

---

# 10. PASS / FAIL / N/A

PASS / FAIL / N/A 的含义：

> 当前测试分支已经得到结论，不再继续向该分支后面递进。

它们不直接代表整个项目结论。

---

# 11. Completed 后修改

Tester 可以重新打开 Completed Test Case 并修改。

如果修改内容：

```text
不影响当前 Test Case 与后续 Test Case 的逻辑关系
```

则：

```text
直接保存
```

系统不处理后续节点。

---

# 12. 影响后续关系的修改

如果修改影响：

```text
Decision Point
前置关系
后续连接关系
```

系统必须进行：

```text
风险提示
↓
二次确认
```

然后允许选择后续节点处理方式。

---

# 13. 后续节点处理方式

提供四种方式：

```text
原节点
其他节点
增加节点
不使用节点
```

## 原节点

继续保留原来连接的后续 Test Case。

## 其他节点

连接到当前逻辑图中已经存在的其他 Test Case。

## 增加节点

新增 Test Case，并将其作为新的后续节点。

## 不使用节点

断开当前 Test Case 与原后续节点的连接。

---

# 14. 不使用节点时采用“断边不删节点”

例如原结构：

```text
TC-001
   ↓
TC-002
   ↓
TC-003
   ↓
TC-004
```

如果 TC-001 不再使用 TC-002：

```text
TC-001

⚠ Floating Branch

TC-002
   ↓
TC-003
   ↓
TC-004
```

系统只断开：

```text
TC-001 → TC-002
```

不会删除：

```text
TC-002
TC-003
TC-004
```

并且：

```text
TC-002 → TC-003 → TC-004
```

原有内部关系继续保留。

---

# 15. 游离用例 / 游离分支

断开前置关系后形成：

```text
Floating Test Case
Floating Branch
```

它们仍然：

```text
属于当前项目
保留测试任务
保留 Tester 分配
保留已有证据
保留已有结果
允许继续执行
```

只增加“游离”标识。

---

# 16. 关系状态与执行状态分离

关系状态可以简单表示为：

```text
Relation Status

CONNECTED
FLOATING
```

它与执行状态独立。

例如：

```text
Execution Status:
In Progress

Relation Status:
FLOATING
```

---

# 17. 游离分支重新连接

游离分支可以重新连接到其他 Test Case。

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

重新连接后，不需要重新生成 TC-003 / TC-004。

已有后续关系继续保留。

---

# 18. 当前确认原则

1. 一个 Project Test Case 可以同时分配多人。
2. 多人共享同一份执行记录。
3. 所有 Assignee 都有执行和修改权限。
4. 测试协调员不负责逐条审核执行结果。
5. Test Case Completed 后允许重新打开修改。
6. 不保存完整执行修改历史。
7. 每个递进式 Test Case 可包含一个或多个 Decision Point。
8. Decision Point 可进入一个或多个后续 Test Case。
9. Decision Point 也可以结束为 PASS / FAIL / N/A。
10. SINGLE / MULTIPLE 由 Test Case.SelectionMode 控制。
11. 不影响后续关系的修改直接保存。
12. 影响后续关系的修改必须二次风险确认。
13. 后续节点处理支持：原节点、其他节点、增加节点、不使用节点。
14. 不使用节点采用“断边，不删节点”。
15. 被断开的后续测试链成为游离分支。
16. 游离分支继续属于当前项目并可继续执行。
17. 游离分支内部原有连接关系保持不变。
18. 游离分支可以再次接回其他节点。

---

# 19. 下一阶段

下一步建议设计：

**项目 Test Case 的自动分配继承规则**

重点确定：

```text
前置 Test Case 触发新的 NEXT_CASE 后
新的 Test Case 默认分给谁？

是否继承前一个 Test Case 的 Assignees？
还是进入“未分配”状态？
不同分支是否允许不同 Tester？
测试人员是否可以自己认领新激活的 Test Case？
```

这会直接影响递进式测试在多人并行项目中的实际使用方式。
