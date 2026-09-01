# IoT-Security-Case-Hub
## Test Case 结构、版本与修改生命周期设计 V0.1

> 本文档固化当前已确认的公共 Test Case 结构、版本管理、修改申请和项目版本处理规则。

---

# 1. Test Case 核心结构

```text
Test Case
├─ 用例编号
├─ 用例名称
├─ 测试目的
├─ 前置条件
├─ 测试工具[]          # 从独立工具库多选
├─ 测试步骤[]          # 分步骤结构
├─ Selection Mode
│  ├─ SINGLE
│  └─ MULTIPLE
├─ Decision Points[]
├─ 证据要求
├─ 备注要求
├─ 附件[]
├─ 版本
└─ 状态
```

说明：

- PASS / FAIL 条件不单独作为 Test Case 字段。
- 判定逻辑统一放在 Decision Point 中。
- 证据要求、备注要求放在 Test Case 级别。
- 测试工具支持多选，并来自独立 Tool Library。
- 测试步骤采用分步骤结构。
- 特别详细的操作说明、脚本、拓扑图、示例日志等可放附件。
- 具体测试内容由 Test Case 编写人员自行配置，系统只提供结构框架。

---

# 2. Decision Point

Decision Point 保持轻量。

```text
Decision Point
├─ 判断点名称
├─ 判断点说明
├─ 跳转类型
│  ├─ NEXT_CASE
│  ├─ NEXT_CASES
│  ├─ PASS
│  ├─ FAIL
│  └─ N/A
├─ 目标 Test Case[]
└─ 显示顺序
```

Selection Mode 放在 Test Case 级别：

```text
SINGLE
MULTIPLE
```

---

# 3. Tool Library

测试工具做成独立工具库。

Test Case 只负责关联工具：

```text
Test Case
   ↓
Tools[]
   ↓
Tool Library
```

工具库内容由后续单独设计。

---

# 4. Test Case 状态

V1 先使用四种状态：

```text
Draft
草稿

Review
待审核

Published
已发布

Deprecated
已废弃
```

---

# 5. 版本规则

版本采用：

```text
v1.0
v1.1
v1.2
v2.0
```

正式 Published 版本不能直接覆盖修改。

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

# 6. 修改申请流程

测试人员发现公共 Test Case 存在问题：

```text
测试人员发现问题
   ↓
提交修改申请
   ↓
测试协调员判断是否需要修改
   ↓
┌───────────────────┐
│ 不需要修改         │
│ → Rejected        │
└───────────────────┘

需要修改
   ↓
创建该 Test Case 的新 Draft 版本
   ↓
测试协调员 / 问题提出人修改
   ↓
测试协调员提交审核
   ↓
管理员审核
```

测试人员平时不能修改公共 Test Case。

但当其 Change Request 被批准后：

> 可获得该 Draft Revision 的临时编辑权限。

该权限只针对指定 Draft，不代表拥有公共用例库全局修改权限。

---

# 7. Draft 协作

Draft 可支持：

```text
Owner:
测试协调员

Contributors:
问题提出人
其他被授权人员
```

测试人员可以参与修改。

最终提交管理员审核由测试协调员负责。

---

# 8. 管理员审核结果

管理员审核支持三个结果：

```text
Publish
发布

Return
退回修改

Reject
拒绝本次版本更新
```

## Publish

新版本正式发布。

## Return

修改方向成立，但内容需要继续完善。

退回 Draft，继续修改后重新提交。

## Reject

本次 Revision 不成立，结束该版本更新。

原 Published 版本继续有效。

---

# 9. 旧版本保留

新版本发布后：

**旧版本不能删除或覆盖。**

例如：

```text
BLE-PAIR-001

v1.2  历史版本
v1.3  当前版本
```

旧版本：

- 继续保留
- 所有用户可搜索
- 可查看历史内容
- 可查看版本差异
- 可用于历史项目追溯

新项目默认使用当前最新 Published 版本。

---

# 10. 已完成项目

已完成项目必须继续绑定当时使用的 Test Case 版本。

例如：

```text
Project A

BLE-PAIR-001 v1.2
```

即使公共数据库已经发布：

```text
BLE-PAIR-001 v1.3
```

Project A 仍然保持：

```text
v1.2
```

不能自动升级。

---

# 11. 正在执行的项目

如果项目当前使用：

```text
BLE-PAIR-001 v1.2
```

公共数据库发布：

```text
BLE-PAIR-001 v1.3
```

系统提示：

```text
New Version Available

Current Project Version:
v1.2

Latest Published Version:
v1.3
```

由测试协调员决定：

```text
Keep Current Version
继续使用 v1.2

Upgrade
升级到 v1.3
```

系统不能自动替换。

---

# 12. 版本升级建议显示 Diff

协调员决定是否升级时，应可以查看：

```text
v1.2 → v1.3
```

的差异，例如：

```text
测试步骤变化
Decision Point 变化
测试工具变化
证据要求变化
附件变化
```

具体 Diff 展示方式后续设计。

---

# 13. 当前确定原则

1. Test Case 使用统一结构框架，具体内容由编写人员配置。
2. PASS / FAIL 不单独作为字段，统一由 Decision Point 表达。
3. Selection Mode 在 Test Case 级别控制。
4. 工具采用独立 Tool Library，并支持 Test Case 多选关联。
5. 正式 Published 版本不能直接覆盖修改。
6. 公共 Test Case 必须版本化。
7. 测试人员通过 Change Request 提出修改。
8. Change Request 批准后，问题提出人可参与指定 Draft 修改。
9. 测试协调员负责 Draft 和提交审核。
10. 管理员拥有最终 Publish 权限。
11. 新版本发布后旧版本永久保留可搜索。
12. 新项目默认使用最新 Published 版本。
13. 已完成项目永久保留当时绑定版本。
14. 进行中项目发现新版时仅提示，不自动替换。
15. 是否升级由测试协调员决定。

---

# 14. 下一阶段

下一步设计：

**公共 Test Case 的分类、检索与标签体系**

需要先确定：

```text
用例如何分类？
接口类标签怎么放？
协议 / 技术怎么表示？
难度要不要作为字段？
测试类型怎么分类？
如何让新人快速搜索和学习？
```

这一块会直接影响公共 Test Case Library 的总表和搜索页面。
