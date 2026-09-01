# IoT-Security-Case-Hub
## 公共 Test Case 分类、检索与逻辑图设计 V0.1

> 本文档固化公共 Test Case Library 的分类方式、检索方式，以及递进式测试用例的逻辑图查看与跳转规则。

---

# 1. 公共 Test Case 主分类原则

公共 Test Case Library 按：

**技术 / 攻击面**

进行主分类。

不以：

```text
EN 18031
FDA
PSTI
EN 303 645
```

作为主目录。

标准只作为：

```text
关联关系
筛选条件
测试生成规则输入
```

---

# 2. 一级分类

第一版建议：

```text
Test Case Library

├─ Network
├─ Wi-Fi
├─ Bluetooth
├─ Physical Interface
├─ Firmware
├─ Software Update
├─ Authentication & Access Control
├─ Cryptography & Key Management
├─ Logging
├─ Data Storage
├─ Mobile Application
├─ Cloud & API
└─ Fuzzing & Robustness
```

---

# 3. 二级分类

一级分类下面允许存在二级分类。

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

目录层级最多控制在：

```text
一级分类
↓
二级分类
```

更细的关系通过标签实现，不继续无限向下建立目录。

---

# 4. 标签体系

Test Case 可以关联多个标签。

例如：

```text
BLE-PAIR-001

主分类：
Bluetooth

二级分类：
Pairing & Bonding

Tags:
BLE
Pairing
Authentication
Access Control
Beginner
bluetoothctl
```

标签主要用于：

```text
搜索
筛选
新人学习
相关用例推荐
```

标签不直接作为自动生成规则。

---

# 5. 搜索方式

公共 Test Case Library 支持：

```text
主分类浏览
二级分类浏览
标签筛选
关键词搜索
```

关键词搜索应至少覆盖：

```text
用例编号
用例名称
测试目的
测试步骤
工具名称
标签
```

---

# 6. 递进式 Test Case 标识

如果某条 Test Case 属于递进式逻辑链，应显示：

```text
递进式测试用例
```

以及：

```text
所属逻辑链
```

例如：

```text
BLE-PAIR-001

类型：
递进式测试用例

所属逻辑链：
Bluetooth Security
```

---

# 7. 查看逻辑图

递进式 Test Case 详情页提供：

```text
[ 查看逻辑图 ]
```

点击后查看该 Test Case 所属的完整逻辑关系。

逻辑图用于展示：

```text
Test Case 节点

Decision Point

NEXT_CASE / NEXT_CASES

PASS

FAIL

N/A

当前 Test Case
```

---

# 8. 当前 Test Case 高亮

从某条 Test Case 打开逻辑图时：

```text
当前 Test Case
```

必须在图中明显高亮。

用户可以快速知道：

```text
我当前在哪一个测试节点
```

---

# 9. 图内节点跳转

逻辑图中的 Test Case 节点可以点击。

例如：

```text
点击 TC-BLE-ENC-001
↓
打开 TC-BLE-ENC-001 Test Case 详情
```

打开新 Test Case 后，再次查看逻辑图：

```text
新的当前 Test Case
```

变为高亮节点。

---

# 10. 完整逻辑图与当前执行路径

逻辑图建议支持两种查看方式。

## 完整逻辑图

用于：

```text
学习
理解完整测试链
查看所有可能路径
```

显示该逻辑链所有节点和 Decision Point。

---

## 当前执行路径

用于项目执行。

突出：

```text
已经执行的 Test Case
已经选择的 Decision Point
当前激活的 Test Case
已经结束的 PASS / FAIL / N/A 分支
```

未走过路径降低视觉强调。

---

# 11. 未激活 Test Case 的查看权限

测试人员可以点击逻辑图中：

```text
当前项目尚未激活的 Test Case
```

并查看其详细内容。

用于：

```text
主动学习
理解后续测试逻辑
```

但测试人员点击未激活 Test Case：

**不能直接改变当前项目测试计划。**

系统应提示：

```text
该测试用例当前未被本项目逻辑路径激活。
```

---

# 12. 未激活用例如何进入项目

未激活 Test Case 只有两种正式进入项目的方式：

```text
1. Decision Point / Transition 自动触发

2. 测试协调员手动加入
```

测试人员单纯浏览、点击逻辑图不能激活项目任务。

---

# 13. 逻辑图底层模型

界面上可以称为：

```text
树图
逻辑图
测试链
```

但底层模型仍按照：

```text
Directed Acyclic Graph
DAG
有向无环图
```

设计。

因为：

```text
一个 Test Case
```

可能被多个不同前置 Test Case 引用。

---

# 14. 当前确认原则

1. 公共 Test Case 按技术 / 攻击面分类。
2. 标准不作为主目录。
3. 分类最多两级。
4. 更细粒度通过标签处理。
5. 标签负责搜索和学习，不直接控制自动生成。
6. 递进式 Test Case 可查看完整逻辑图。
7. 打开逻辑图时高亮当前 Test Case。
8. 图中 Test Case 节点可以点击跳转。
9. 支持完整逻辑图和当前执行路径两种视图。
10. 测试人员可以查看未激活 Test Case。
11. 测试人员点击未激活 Test Case 不会修改项目测试计划。
12. 未激活 Test Case 只能通过逻辑触发或测试协调员手工加入项目。
13. 底层逻辑采用 DAG 思路，避免循环测试路径。

---

# 15. 下一阶段

下一步建议设计：

**项目测试任务执行页面与任务分发机制**

需要先确定：

```text
测试协调员如何把用例分给测试人员？
是逐条分配还是支持批量分配？
一个 Test Case 能不能分给多人？
测试人员首页看到什么？
执行时如何提交 Decision Point？
提交后后续 Test Case 如何自动激活？
测试结果什么时候锁定？
```

这部分会把：

```text
Test Case Library
↓
项目测试计划
↓
测试人员执行
```

真正连接起来。
