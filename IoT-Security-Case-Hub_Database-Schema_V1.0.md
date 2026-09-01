# IoT-Security-Case-Hub
## Database Schema V1.0

> 基于：
>
> - `IoT-Security-Case-Hub_System-Design_V0.6.md`
> - `IoT-Security-Case-Hub_Technical-Architecture_V1.0.md`
> - `IoT-Security-Case-Hub_Data-Model_V1.0.md`
>
> 本文档冻结 V1 PostgreSQL 数据库结构，包括：
>
> - Schema
> - Table
> - Column
> - PostgreSQL Data Type
> - Primary Key
> - Foreign Key
> - Unique Constraint
> - Check Constraint
> - Index
> - Delete Behavior
> - Search Index
> - Flyway Migration 顺序
>
> 后续 JPA Entity 与 Repository 应以本文档为准。

---

# 1. PostgreSQL 基线

推荐：

```text
PostgreSQL 16+
```

数据库：

```text
iot_security_case_hub
```

业务 Schema：

```text
casehub
```

---

# 2. PostgreSQL Extension

V1 启用：

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
```

用途：

```text
pgcrypto
→ gen_random_uuid()

pg_trgm
→ 中英文模糊搜索 / ILIKE 搜索加速
```

---

# 3. Primary Key 策略

所有核心业务表统一使用：

```text
UUID
```

PostgreSQL：

```sql
id UUID PRIMARY KEY DEFAULT gen_random_uuid()
```

理由：

```text
跨模块唯一
未来 Automation / 外部 Runner 易扩展
不暴露递增业务规模
Java UUID 支持成熟
```

---

# 4. 时间字段

统一使用：

```text
TIMESTAMPTZ
```

而不是：

```text
TIMESTAMP WITHOUT TIME ZONE
```

标准字段：

```sql
created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
```

`updated_at` 由 Java Service / JPA 生命周期维护。

---

# 5. Enum 实现原则

V1 不使用 PostgreSQL ENUM Type。

统一使用：

```text
VARCHAR
+
CHECK CONSTRAINT
```

例如：

```sql
status VARCHAR(32) NOT NULL
CHECK (status IN ('DRAFT', 'ACTIVE', 'COMPLETED', 'ARCHIVED'))
```

原因：

```text
Flyway 修改更容易
避免 PostgreSQL ENUM 演进成本
Java Enum 映射清晰
```

---

# 6. 删除原则

业务核心数据默认不物理删除：

```text
User
Project
Master Test Case
Published Test Case Version
Project Test Case
```

使用：

```text
enabled
status
archived
removed
```

控制。

允许物理删除：

```text
Evidence
Note
部分未发布 Draft
纯关联表
```

---

# 7. Identity & RBAC

---

## 7.1 users

```sql
casehub.users
```

| Column | Type | Null |说明 |
|---|---|---:|---|
| id | UUID | NO | PK |
| username | VARCHAR(100) | NO | 登录名 |
| display_name | VARCHAR(150) | NO | 显示名称 |
| password_hash | VARCHAR(255) | NO | BCrypt 等 Hash |
| enabled | BOOLEAN | NO | 默认 true |
| created_at | TIMESTAMPTZ | NO | |
| updated_at | TIMESTAMPTZ | NO | |

约束：

```sql
UNIQUE (username)
```

推荐额外使用大小写无关唯一索引：

```sql
CREATE UNIQUE INDEX uq_users_username_lower
ON casehub.users (LOWER(username));
```

用户不物理删除：

```text
enabled = false
```

---

## 7.2 roles

| Column | Type | Null |说明 |
|---|---|---:|---|
| id | UUID | NO | PK |
| code | VARCHAR(64) | NO | ADMIN / TEST_COORDINATOR / TESTER |
| name | VARCHAR(100) | NO | |
| description | TEXT | YES | |
| created_at | TIMESTAMPTZ | NO | |

约束：

```sql
UNIQUE (code)
```

初始 Seed：

```text
ADMIN
TEST_COORDINATOR
TESTER
```

---

## 7.3 permissions

V1 建议正式落地 Permission Table，而不是只在代码中硬编码 Role。

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| code | VARCHAR(100) | NO |
| description | TEXT | YES |
| created_at | TIMESTAMPTZ | NO |

约束：

```sql
UNIQUE (code)
```

示例：

```text
project:create
project:update
project:archive

project_test_case:assign
project_test_case:remove
project_test_case:execute

test_case:read
test_case:draft:create
test_case:review
test_case:publish

capability_library:manage
generation_rule:manage

evidence:upload
evidence:delete
```

---

## 7.4 user_roles

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| user_id | UUID | NO |
| role_id | UUID | NO |
| created_at | TIMESTAMPTZ | NO |

FK：

```text
user_id → users.id       ON DELETE CASCADE
role_id → roles.id       ON DELETE CASCADE
```

唯一：

```sql
UNIQUE (user_id, role_id)
```

索引：

```sql
INDEX (user_id)
INDEX (role_id)
```

---

## 7.5 role_permissions

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| role_id | UUID | NO |
| permission_id | UUID | NO |
| created_at | TIMESTAMPTZ | NO |

FK：

```text
role_id       → roles.id       ON DELETE CASCADE
permission_id → permissions.id ON DELETE CASCADE
```

唯一：

```sql
UNIQUE (role_id, permission_id)
```

---

# 8. Standard / Task Type

---

## 8.1 standard_task_types

| Column | Type | Null |说明 |
|---|---|---:|---|
| id | UUID | NO | PK |
| code | VARCHAR(100) | NO | EN18031 / FDA / PSTI |
| name | VARCHAR(200) | NO | |
| type | VARCHAR(32) | NO | STANDARD / TASK_TYPE |
| description | TEXT | YES | |
| enabled | BOOLEAN | NO | 默认 true |
| created_at | TIMESTAMPTZ | NO | |
| updated_at | TIMESTAMPTZ | NO | |

CHECK：

```sql
CHECK (type IN ('STANDARD', 'TASK_TYPE'))
```

唯一：

```sql
UNIQUE (code)
```

---

# 9. Category / Tag / Tool

---

## 9.1 categories

最多两级。

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| parent_id | UUID | YES |
| code | VARCHAR(100) | NO |
| name | VARCHAR(150) | NO |
| level | SMALLINT | NO |
| description | TEXT | YES |
| sort_order | INTEGER | NO |
| enabled | BOOLEAN | NO |
| created_at | TIMESTAMPTZ | NO |
| updated_at | TIMESTAMPTZ | NO |

默认：

```text
sort_order = 0
enabled = true
```

FK：

```text
parent_id → categories.id ON DELETE RESTRICT
```

CHECK：

```sql
CHECK (level IN (1, 2));

CHECK (
    (level = 1 AND parent_id IS NULL)
 OR (level = 2 AND parent_id IS NOT NULL)
);
```

唯一：

```sql
UNIQUE (code)
```

说明：

```text
Level 2 的 Parent 必须是 Level 1
```

由 Service 层再次校验。

---

## 9.2 tags

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| name | VARCHAR(100) | NO |
| description | TEXT | YES |
| enabled | BOOLEAN | NO |
| created_at | TIMESTAMPTZ | NO |

推荐：

```sql
CREATE UNIQUE INDEX uq_tags_name_lower
ON casehub.tags (LOWER(name));
```

---

## 9.3 tools

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| name | VARCHAR(150) | NO |
| description | TEXT | YES |
| platform | VARCHAR(100) | YES |
| website | TEXT | YES |
| enabled | BOOLEAN | NO |
| created_at | TIMESTAMPTZ | NO |
| updated_at | TIMESTAMPTZ | NO |

推荐：

```sql
CREATE UNIQUE INDEX uq_tools_name_lower
ON casehub.tools (LOWER(name));
```

---

# 10. Capability Library

---

## 10.1 capabilities

Capability 自关联树。

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| parent_id | UUID | YES |
| code | VARCHAR(120) | NO |
| name | VARCHAR(180) | NO |
| description | TEXT | YES |
| sort_order | INTEGER | NO |
| enabled | BOOLEAN | NO |
| created_at | TIMESTAMPTZ | NO |
| updated_at | TIMESTAMPTZ | NO |

FK：

```text
parent_id → capabilities.id ON DELETE RESTRICT
```

唯一：

```sql
UNIQUE (code)
```

索引：

```sql
INDEX (parent_id)
INDEX (enabled)
```

规则：

```text
Capability Tree 不允许环
```

由 Service 层递归校验。

---

# 11. Master Test Case Library

---

## 11.1 master_test_cases

稳定 Test Case Identity。

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| case_code | VARCHAR(100) | NO |
| category_id | UUID | NO |
| created_by | UUID | NO |
| enabled | BOOLEAN | NO |
| created_at | TIMESTAMPTZ | NO |
| updated_at | TIMESTAMPTZ | NO |

FK：

```text
category_id → categories.id ON DELETE RESTRICT
created_by  → users.id      ON DELETE RESTRICT
```

唯一：

```sql
UNIQUE (case_code)
```

搜索索引：

```sql
CREATE INDEX idx_master_test_cases_code_trgm
ON casehub.master_test_cases
USING GIN (case_code gin_trgm_ops);
```

---

## 11.2 test_case_versions

| Column | Type | Null |说明 |
|---|---|---:|---|
| id | UUID | NO | |
| master_test_case_id | UUID | NO | |
| version_major | INTEGER | NO | |
| version_minor | INTEGER | NO | |
| status | VARCHAR(32) | NO | |
| is_current_version | BOOLEAN | NO | |
| case_name | VARCHAR(255) | NO | |
| test_purpose | TEXT | YES | |
| preconditions | TEXT | YES | |
| selection_mode | VARCHAR(16) | NO | |
| evidence_required | BOOLEAN | NO | |
| evidence_requirement | TEXT | YES | |
| remark_requirement | TEXT | YES | |
| progressive_role | VARCHAR(16) | YES | ENTRY / NORMAL |
| based_on_version_id | UUID | YES | |
| change_request_id | UUID | YES | 后续 FK |
| change_reason | TEXT | YES | |
| created_by | UUID | NO | |
| reviewed_by | UUID | YES | |
| published_at | TIMESTAMPTZ | YES | |
| deprecated_at | TIMESTAMPTZ | YES | |
| revision_closed | BOOLEAN | NO | 默认 false |
| created_at | TIMESTAMPTZ | NO | |
| updated_at | TIMESTAMPTZ | NO | |

FK：

```text
master_test_case_id → master_test_cases.id ON DELETE RESTRICT
based_on_version_id → test_case_versions.id ON DELETE RESTRICT
created_by          → users.id ON DELETE RESTRICT
reviewed_by         → users.id ON DELETE RESTRICT
```

CHECK：

```sql
CHECK (version_major >= 1);
CHECK (version_minor >= 0);

CHECK (
  status IN ('DRAFT', 'REVIEW', 'PUBLISHED', 'DEPRECATED')
);

CHECK (
  selection_mode IN ('SINGLE', 'MULTIPLE')
);

CHECK (
  progressive_role IS NULL
  OR progressive_role IN ('ENTRY', 'NORMAL')
);

CHECK (
  is_current_version = false
  OR status = 'PUBLISHED'
);
```

唯一：

```sql
UNIQUE (
  master_test_case_id,
  version_major,
  version_minor
)
```

Current Version 唯一：

```sql
CREATE UNIQUE INDEX uq_test_case_current_version
ON casehub.test_case_versions(master_test_case_id)
WHERE is_current_version = true;
```

辅助唯一：

为了 ProjectTestCase 使用组合 FK：

```sql
UNIQUE (id, master_test_case_id)
```

索引：

```sql
INDEX (master_test_case_id)
INDEX (status)
INDEX (is_current_version)
INDEX (created_by)
```

搜索：

```sql
CREATE INDEX idx_test_case_version_name_trgm
ON casehub.test_case_versions
USING GIN (case_name gin_trgm_ops);
```

---

## 11.3 test_steps

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| test_case_version_id | UUID | NO |
| sequence_no | INTEGER | NO |
| title | VARCHAR(255) | YES |
| content | TEXT | NO |
| created_at | TIMESTAMPTZ | NO |
| updated_at | TIMESTAMPTZ | NO |

FK：

```text
test_case_version_id
→ test_case_versions.id
ON DELETE CASCADE
```

唯一：

```sql
UNIQUE (test_case_version_id, sequence_no)
```

搜索：

```sql
CREATE INDEX idx_test_steps_content_trgm
ON casehub.test_steps
USING GIN (content gin_trgm_ops);
```

---

## 11.4 test_case_tags

Test Case 标签属于稳定 Master Identity。

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| master_test_case_id | UUID | NO |
| tag_id | UUID | NO |

FK：

```text
master_test_case_id → master_test_cases.id ON DELETE CASCADE
tag_id              → tags.id              ON DELETE CASCADE
```

唯一：

```sql
UNIQUE (master_test_case_id, tag_id)
```

---

## 11.5 test_case_tools

工具版本化。

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| test_case_version_id | UUID | NO |
| tool_id | UUID | NO |
| sort_order | INTEGER | NO |

FK：

```text
test_case_version_id → test_case_versions.id ON DELETE CASCADE
tool_id              → tools.id              ON DELETE RESTRICT
```

唯一：

```sql
UNIQUE (test_case_version_id, tool_id)
```

---

## 11.6 test_case_standard_mappings

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| test_case_version_id | UUID | NO |
| standard_task_type_id | UUID | NO |
| mapping_note | TEXT | YES |

FK：

```text
test_case_version_id  → test_case_versions.id   ON DELETE CASCADE
standard_task_type_id → standard_task_types.id  ON DELETE RESTRICT
```

唯一：

```sql
UNIQUE (test_case_version_id, standard_task_type_id)
```

---

## 11.7 test_case_attachments

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| test_case_version_id | UUID | NO |
| original_filename | VARCHAR(255) | NO |
| storage_key | VARCHAR(512) | NO |
| file_size | BIGINT | NO |
| content_type | VARCHAR(150) | YES |
| sha256 | VARCHAR(64) | YES |
| description | TEXT | YES |
| uploaded_by | UUID | NO |
| created_at | TIMESTAMPTZ | NO |

FK：

```text
test_case_version_id → test_case_versions.id ON DELETE CASCADE
uploaded_by          → users.id              ON DELETE RESTRICT
```

唯一：

```sql
UNIQUE (storage_key)
```

---

## 11.8 tool_attachments

结构与 test_case_attachments 相同。

FK：

```text
tool_id     → tools.id ON DELETE CASCADE
uploaded_by → users.id ON DELETE RESTRICT
```

---

# 12. Master Progressive DAG

---

## 12.1 decision_points

Decision Point 属于具体 TestCaseVersion。

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| test_case_version_id | UUID | NO |
| name | VARCHAR(255) | NO |
| description | TEXT | YES |
| display_order | INTEGER | NO |
| created_at | TIMESTAMPTZ | NO |
| updated_at | TIMESTAMPTZ | NO |

FK：

```text
test_case_version_id
→ test_case_versions.id
ON DELETE CASCADE
```

唯一：

```sql
UNIQUE (test_case_version_id, display_order)
```

辅助唯一：

```sql
UNIQUE (id, test_case_version_id)
```

---

## 12.2 transitions

一个 DecisionPoint 只有一个 Transition。

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| decision_point_id | UUID | NO |
| type | VARCHAR(32) | NO |
| created_at | TIMESTAMPTZ | NO |
| updated_at | TIMESTAMPTZ | NO |

FK：

```text
decision_point_id
→ decision_points.id
ON DELETE CASCADE
```

唯一：

```sql
UNIQUE (decision_point_id)
```

CHECK：

```sql
CHECK (
  type IN (
    'NEXT_CASE',
    'NEXT_CASES',
    'PASS',
    'FAIL',
    'N_A'
  )
)
```

代码层可把：

```text
N_A
```

映射为：

```text
N/A
```

避免数据库值包含 `/`。

---

## 12.3 transition_targets

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| transition_id | UUID | NO |
| target_master_test_case_id | UUID | NO |
| target_order | INTEGER | NO |
| created_at | TIMESTAMPTZ | NO |

FK：

```text
transition_id
→ transitions.id
ON DELETE CASCADE

target_master_test_case_id
→ master_test_cases.id
ON DELETE RESTRICT
```

唯一：

```sql
UNIQUE (
  transition_id,
  target_master_test_case_id
)
```

索引：

```sql
INDEX (target_master_test_case_id)
```

---

# 13. DAG Service-Level Constraint

以下约束不适合只靠普通 CHECK 完成，由 Java Service + Transaction 保证。

## PASS / FAIL / N_A

必须：

```text
transition_targets = 0
```

## NEXT_CASE

必须：

```text
transition_targets = 1
```

## NEXT_CASES

必须：

```text
transition_targets >= 1
```

## Cycle

发布或修改 DAG 前必须验证：

```text
No Cycle
```

禁止：

```text
A → B → C → A
```

---

# 14. Projects

---

## 14.1 projects

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| project_number | VARCHAR(64) | NO |
| project_name | VARCHAR(255) | NO |
| device_name | VARCHAR(255) | NO |
| generation_mode | VARCHAR(32) | NO |
| status | VARCHAR(32) | NO |
| created_by | UUID | NO |
| created_at | TIMESTAMPTZ | NO |
| updated_at | TIMESTAMPTZ | NO |

CHECK：

```sql
CHECK (
  generation_mode IN ('FULL', 'PROGRESSIVE')
);

CHECK (
  status IN ('DRAFT', 'ACTIVE', 'COMPLETED', 'ARCHIVED')
);
```

FK：

```text
created_by → users.id ON DELETE RESTRICT
```

唯一：

```sql
UNIQUE (project_number)
```

索引：

```sql
INDEX (status)
INDEX (created_at)
```

---

## 14.2 project_standards

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| project_id | UUID | NO |
| standard_task_type_id | UUID | NO |

FK：

```text
project_id            → projects.id            ON DELETE CASCADE
standard_task_type_id → standard_task_types.id ON DELETE RESTRICT
```

唯一：

```sql
UNIQUE (project_id, standard_task_type_id)
```

---

## 14.3 project_coordinators

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| project_id | UUID | NO |
| user_id | UUID | NO |
| is_primary | BOOLEAN | NO |
| created_at | TIMESTAMPTZ | NO |

FK：

```text
project_id → projects.id ON DELETE CASCADE
user_id    → users.id    ON DELETE RESTRICT
```

唯一：

```sql
UNIQUE (project_id, user_id)
```

一个 Project 只能有一个 Primary：

```sql
CREATE UNIQUE INDEX uq_project_primary_coordinator
ON casehub.project_coordinators(project_id)
WHERE is_primary = true;
```

---

# 15. Project Capability

---

## 15.1 project_capabilities

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| project_id | UUID | NO |
| capability_id | UUID | NO |
| value | VARCHAR(16) | NO |
| source | VARCHAR(40) | NO |
| is_derived | BOOLEAN | NO |
| comment | TEXT | YES |
| updated_by | UUID | NO |
| created_at | TIMESTAMPTZ | NO |
| updated_at | TIMESTAMPTZ | NO |

CHECK：

```sql
CHECK (
  value IN ('YES', 'NO', 'UNKNOWN')
);

CHECK (
  source IN (
    'CUSTOMER_PROVIDED',
    'TESTER_DISCOVERED',
    'DOCUMENT',
    'AUTOMATIC_DETECTION',
    'COORDINATOR_INPUT',
    'DERIVED',
    'OTHER'
  )
);
```

FK：

```text
project_id    → projects.id     ON DELETE CASCADE
capability_id → capabilities.id ON DELETE RESTRICT
updated_by    → users.id        ON DELETE RESTRICT
```

唯一：

```sql
UNIQUE (project_id, capability_id)
```

说明：

```text
没有 ProjectCapability Row
也按 UNKNOWN 处理。
```

---

# 16. Capability Update Request

---

## 16.1 capability_update_requests

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| project_id | UUID | NO |
| capability_id | UUID | NO |
| related_project_test_case_id | UUID | YES |
| current_value | VARCHAR(16) | NO |
| proposed_value | VARCHAR(16) | NO |
| reason | TEXT | NO |
| status | VARCHAR(24) | NO |
| submitted_by | UUID | NO |
| reviewed_by | UUID | YES |
| reviewed_at | TIMESTAMPTZ | YES |
| review_comment | TEXT | YES |
| created_at | TIMESTAMPTZ | NO |

CHECK：

```sql
CHECK (
 current_value IN ('YES', 'NO', 'UNKNOWN')
);

CHECK (
 proposed_value IN ('YES', 'NO', 'UNKNOWN')
);

CHECK (
 status IN ('PENDING', 'APPROVED', 'REJECTED')
);
```

FK：

```text
project_id    → projects.id      ON DELETE RESTRICT
capability_id → capabilities.id  ON DELETE RESTRICT
submitted_by  → users.id         ON DELETE RESTRICT
reviewed_by   → users.id         ON DELETE RESTRICT
```

`related_project_test_case_id` 的 FK 在 ProjectTestCase 表建立后通过后续 Migration 添加。

索引：

```sql
INDEX (project_id, status)
INDEX (submitted_by)
```

---

# 17. Generation Rule

---

## 17.1 generation_rules

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| rule_code | VARCHAR(100) | NO |
| name | VARCHAR(255) | NO |
| mode | VARCHAR(32) | NO |
| status | VARCHAR(24) | NO |
| description | TEXT | YES |
| created_by | UUID | NO |
| created_at | TIMESTAMPTZ | NO |
| updated_at | TIMESTAMPTZ | NO |

CHECK：

```sql
CHECK (
  mode IN ('FULL', 'PROGRESSIVE_INITIAL', 'BOTH')
);

CHECK (
  status IN ('ENABLED', 'DISABLED')
);
```

唯一：

```sql
UNIQUE (rule_code)
```

---

## 17.2 generation_condition_groups

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| rule_id | UUID | NO |
| parent_group_id | UUID | YES |
| logic_operator | VARCHAR(8) | NO |
| sort_order | INTEGER | NO |

CHECK：

```sql
CHECK (
  logic_operator IN ('AND', 'OR')
);
```

FK：

```text
rule_id → generation_rules.id ON DELETE CASCADE
```

为保证 Parent 属于同一 Rule：

```sql
UNIQUE (id, rule_id)
```

再建立：

```text
(parent_group_id, rule_id)
→ generation_condition_groups(id, rule_id)
```

根 Group 唯一：

```sql
CREATE UNIQUE INDEX uq_generation_rule_root_group
ON casehub.generation_condition_groups(rule_id)
WHERE parent_group_id IS NULL;
```

V1 最大嵌套深度：

```text
Root
+
1 层 Child Group
```

由 Service 校验。

---

## 17.3 generation_conditions

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| group_id | UUID | NO |
| target_type | VARCHAR(40) | NO |
| capability_id | UUID | YES |
| standard_task_type_id | UUID | YES |
| operator | VARCHAR(32) | NO |
| sort_order | INTEGER | NO |

Target：

```text
CAPABILITY
STANDARD_TASK_TYPE
```

Operator：

```text
EQ_YES
EQ_NO
EQ_UNKNOWN
NE_NO
NE_YES
PRESENT
```

CHECK：

```sql
CHECK (
 target_type IN ('CAPABILITY', 'STANDARD_TASK_TYPE')
);

CHECK (
 operator IN (
   'EQ_YES',
   'EQ_NO',
   'EQ_UNKNOWN',
   'NE_NO',
   'NE_YES',
   'PRESENT'
 )
);
```

XOR：

```sql
CHECK (
  (
    target_type = 'CAPABILITY'
    AND capability_id IS NOT NULL
    AND standard_task_type_id IS NULL
  )
  OR
  (
    target_type = 'STANDARD_TASK_TYPE'
    AND capability_id IS NULL
    AND standard_task_type_id IS NOT NULL
  )
);
```

兼容性：

```text
CAPABILITY
→ operator 只能 EQ_YES / EQ_NO / EQ_UNKNOWN / NE_NO / NE_YES

STANDARD_TASK_TYPE
→ operator = PRESENT
```

由 Service 再校验。

---

## 17.4 generation_rule_outputs

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| rule_id | UUID | NO |
| master_test_case_id | UUID | NO |
| sort_order | INTEGER | NO |

FK：

```text
rule_id             → generation_rules.id  ON DELETE CASCADE
master_test_case_id → master_test_cases.id ON DELETE RESTRICT
```

唯一：

```sql
UNIQUE (rule_id, master_test_case_id)
```

---

# 18. Generation Runtime

---

## 18.1 generation_runs

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| project_id | UUID | NO |
| mode | VARCHAR(32) | NO |
| trigger_type | VARCHAR(32) | NO |
| executed_by | UUID | NO |
| executed_at | TIMESTAMPTZ | NO |

CHECK：

```sql
CHECK (
 mode IN ('FULL', 'PROGRESSIVE_INITIAL')
);

CHECK (
 trigger_type IN (
   'PROJECT_INITIAL',
   'CAPABILITY_UPDATE',
   'STANDARD_CHANGE',
   'MANUAL_REGENERATE'
 )
);
```

FK：

```text
project_id  → projects.id ON DELETE RESTRICT
executed_by → users.id    ON DELETE RESTRICT
```

索引：

```sql
INDEX (project_id, executed_at DESC)
```

---

## 18.2 generation_recommendations

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| generation_run_id | UUID | NO |
| master_test_case_id | UUID | NO |
| resolved_test_case_version_id | UUID | NO |
| status | VARCHAR(20) | NO |
| added_project_test_case_id | UUID | YES |
| created_at | TIMESTAMPTZ | NO |

Status：

```text
NEW
ADDED
IGNORED
```

唯一：

```sql
UNIQUE (
  generation_run_id,
  master_test_case_id
)
```

FK：

```text
generation_run_id            → generation_runs.id    ON DELETE CASCADE
master_test_case_id           → master_test_cases.id  ON DELETE RESTRICT
resolved_test_case_version_id → test_case_versions.id ON DELETE RESTRICT
```

`added_project_test_case_id` 后续 Migration 增加 FK。

---

## 18.3 generation_recommendation_rules

保留全部 Recommended Because。

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| recommendation_id | UUID | NO |
| generation_rule_id | UUID | NO |

唯一：

```sql
UNIQUE (recommendation_id, generation_rule_id)
```

---

## 18.4 project_test_case_preferences

用于持久化 Ignore。

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| project_id | UUID | NO |
| master_test_case_id | UUID | NO |
| state | VARCHAR(20) | NO |
| updated_by | UUID | NO |
| updated_at | TIMESTAMPTZ | NO |

V1：

```text
state = IGNORED
```

CHECK：

```sql
CHECK (state IN ('IGNORED'));
```

唯一：

```sql
UNIQUE (project_id, master_test_case_id)
```

恢复 Recommendation：

```text
删除该 Preference Row
```

---

# 19. Project Custom Test Case

---

## 19.1 project_custom_test_cases

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| project_id | UUID | NO |
| case_code | VARCHAR(100) | NO |
| case_name | VARCHAR(255) | NO |
| category_id | UUID | YES |
| test_purpose | TEXT | YES |
| preconditions | TEXT | YES |
| selection_mode | VARCHAR(16) | NO |
| evidence_required | BOOLEAN | NO |
| evidence_requirement | TEXT | YES |
| remark_requirement | TEXT | YES |
| created_by | UUID | NO |
| created_at | TIMESTAMPTZ | NO |
| updated_at | TIMESTAMPTZ | NO |

CHECK：

```sql
CHECK (
 selection_mode IN ('SINGLE', 'MULTIPLE')
);
```

FK：

```text
project_id  → projects.id   ON DELETE RESTRICT
category_id → categories.id ON DELETE RESTRICT
created_by  → users.id      ON DELETE RESTRICT
```

唯一：

```sql
UNIQUE (project_id, case_code)
```

辅助：

```sql
UNIQUE (id, project_id)
```

---

## 19.2 project_custom_test_steps

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| project_custom_test_case_id | UUID | NO |
| sequence_no | INTEGER | NO |
| title | VARCHAR(255) | YES |
| content | TEXT | NO |

唯一：

```sql
UNIQUE (
 project_custom_test_case_id,
 sequence_no
)
```

---

## 19.3 project_custom_test_case_tools

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| project_custom_test_case_id | UUID | NO |
| tool_id | UUID | NO |
| sort_order | INTEGER | NO |

唯一：

```sql
UNIQUE (
 project_custom_test_case_id,
 tool_id
)
```

---

## 19.4 project_custom_decision_points

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| project_custom_test_case_id | UUID | NO |
| name | VARCHAR(255) | NO |
| description | TEXT | YES |
| display_order | INTEGER | NO |

唯一：

```sql
UNIQUE (
 project_custom_test_case_id,
 display_order
)
```

---

## 19.5 project_custom_transitions

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| custom_decision_point_id | UUID | NO |
| type | VARCHAR(32) | NO |

唯一：

```sql
UNIQUE (custom_decision_point_id)
```

CHECK：

```sql
CHECK (
 type IN (
   'NEXT_CASE',
   'NEXT_CASES',
   'PASS',
   'FAIL',
   'N_A'
 )
);
```

---

## 19.6 project_custom_transition_targets

Custom Transition 可以指向：

```text
Master Test Case
或
另一个 Project Custom Test Case
```

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| custom_transition_id | UUID | NO |
| target_master_test_case_id | UUID | YES |
| target_project_custom_test_case_id | UUID | YES |
| target_order | INTEGER | NO |

XOR：

```sql
CHECK (
 (
   target_master_test_case_id IS NOT NULL
   AND target_project_custom_test_case_id IS NULL
 )
 OR
 (
   target_master_test_case_id IS NULL
   AND target_project_custom_test_case_id IS NOT NULL
 )
);
```

唯一通过两个 Partial Index：

```sql
CREATE UNIQUE INDEX uq_custom_target_master
ON casehub.project_custom_transition_targets(
 custom_transition_id,
 target_master_test_case_id
)
WHERE target_master_test_case_id IS NOT NULL;
```

```sql
CREATE UNIQUE INDEX uq_custom_target_custom
ON casehub.project_custom_transition_targets(
 custom_transition_id,
 target_project_custom_test_case_id
)
WHERE target_project_custom_test_case_id IS NOT NULL;
```

同一 Project 校验由 Service 完成。

---

# 20. Project Test Case

---

## 20.1 project_test_cases

这是项目运行时最核心表。

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| project_id | UUID | NO |
| master_test_case_id | UUID | YES |
| test_case_version_id | UUID | YES |
| project_custom_test_case_id | UUID | YES |
| execution_status | VARCHAR(24) | NO |
| relation_status | VARCHAR(24) | NO |
| is_root | BOOLEAN | NO |
| removed | BOOLEAN | NO |
| created_by | UUID | NO |
| last_modified_by | UUID | NO |
| started_at | TIMESTAMPTZ | YES |
| completed_at | TIMESTAMPTZ | YES |
| created_at | TIMESTAMPTZ | NO |
| last_modified_at | TIMESTAMPTZ | NO |

CHECK：

```sql
CHECK (
 execution_status IN (
   'NOT_STARTED',
   'IN_PROGRESS',
   'COMPLETED'
 )
);

CHECK (
 relation_status IN (
   'CONNECTED',
   'FLOATING'
 )
);
```

Master / Custom XOR：

```sql
CHECK (
 (
   master_test_case_id IS NOT NULL
   AND test_case_version_id IS NOT NULL
   AND project_custom_test_case_id IS NULL
 )
 OR
 (
   master_test_case_id IS NULL
   AND test_case_version_id IS NULL
   AND project_custom_test_case_id IS NOT NULL
 )
);
```

FK：

```text
project_id                  → projects.id                  ON DELETE RESTRICT
master_test_case_id         → master_test_cases.id         ON DELETE RESTRICT
project_custom_test_case_id → project_custom_test_cases.id ON DELETE RESTRICT
created_by                  → users.id                     ON DELETE RESTRICT
last_modified_by            → users.id                     ON DELETE RESTRICT
```

Version 必须属于相同 Master：

```text
(test_case_version_id, master_test_case_id)
→ test_case_versions(id, master_test_case_id)
```

Master 唯一：

```sql
CREATE UNIQUE INDEX uq_project_master_test_case
ON casehub.project_test_cases(
 project_id,
 master_test_case_id
)
WHERE master_test_case_id IS NOT NULL;
```

Custom 唯一：

```sql
CREATE UNIQUE INDEX uq_project_custom_test_case
ON casehub.project_test_cases(
 project_id,
 project_custom_test_case_id
)
WHERE project_custom_test_case_id IS NOT NULL;
```

常用索引：

```sql
INDEX (project_id, removed, execution_status)
INDEX (project_id, relation_status)
INDEX (last_modified_at DESC)
```

---

## 20.2 project_test_case_assignees

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| project_test_case_id | UUID | NO |
| user_id | UUID | NO |
| assigned_at | TIMESTAMPTZ | NO |

FK：

```text
project_test_case_id → project_test_cases.id ON DELETE CASCADE
user_id              → users.id              ON DELETE RESTRICT
```

唯一：

```sql
UNIQUE (
 project_test_case_id,
 user_id
)
```

索引：

```sql
INDEX (user_id, project_test_case_id)
```

用于：

```text
我的测试
```

查询。

---

## 20.3 project_test_case_sources

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| project_test_case_id | UUID | NO |
| source_type | VARCHAR(24) | NO |
| created_at | TIMESTAMPTZ | NO |

Source：

```text
INITIAL
GENERATED
PROGRESSIVE
MANUAL
CUSTOM
```

CHECK：

```sql
CHECK (
 source_type IN (
   'INITIAL',
   'GENERATED',
   'PROGRESSIVE',
   'MANUAL',
   'CUSTOM'
 )
);
```

唯一：

```sql
UNIQUE (
 project_test_case_id,
 source_type
)
```

---

# 21. Project Decision Selection

---

## 21.1 project_decision_selections

支持 Master 与 Custom DecisionPoint。

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| project_test_case_id | UUID | NO |
| decision_point_id | UUID | YES |
| custom_decision_point_id | UUID | YES |
| selected_by | UUID | NO |
| selected_at | TIMESTAMPTZ | NO |

XOR：

```sql
CHECK (
 (
   decision_point_id IS NOT NULL
   AND custom_decision_point_id IS NULL
 )
 OR
 (
   decision_point_id IS NULL
   AND custom_decision_point_id IS NOT NULL
 )
);
```

FK：

```text
project_test_case_id     → project_test_cases.id             ON DELETE CASCADE
decision_point_id        → decision_points.id                ON DELETE RESTRICT
custom_decision_point_id → project_custom_decision_points.id ON DELETE RESTRICT
selected_by              → users.id                          ON DELETE RESTRICT
```

Master Selection 唯一：

```sql
CREATE UNIQUE INDEX uq_project_selection_master_dp
ON casehub.project_decision_selections(
 project_test_case_id,
 decision_point_id
)
WHERE decision_point_id IS NOT NULL;
```

Custom：

```sql
CREATE UNIQUE INDEX uq_project_selection_custom_dp
ON casehub.project_decision_selections(
 project_test_case_id,
 custom_decision_point_id
)
WHERE custom_decision_point_id IS NOT NULL;
```

重新修改时：

```text
当前 Selection Set 更新
```

不保留完整历史。

---

# 22. Branch Outcome

---

## 22.1 project_branch_outcomes

每个 Selected DecisionPoint 对应一个 Outcome。

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| project_decision_selection_id | UUID | NO |
| outcome_type | VARCHAR(32) | NO |
| created_at | TIMESTAMPTZ | NO |
| updated_at | TIMESTAMPTZ | NO |

Outcome：

```text
NEXT_CASE
NEXT_CASES
PASS
FAIL
N_A
```

CHECK：

```sql
CHECK (
 outcome_type IN (
   'NEXT_CASE',
   'NEXT_CASES',
   'PASS',
   'FAIL',
   'N_A'
 )
);
```

唯一：

```sql
UNIQUE (project_decision_selection_id)
```

---

# 23. Progressive Runtime Trigger

---

## 23.1 project_test_case_triggers

它既是：

```text
Triggered By
```

记录，也是实际 Project DAG Edge。

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| branch_outcome_id | UUID | NO |
| source_project_test_case_id | UUID | NO |
| target_project_test_case_id | UUID | NO |
| active | BOOLEAN | NO |
| created_at | TIMESTAMPTZ | NO |
| updated_at | TIMESTAMPTZ | NO |

FK：

```text
branch_outcome_id
→ project_branch_outcomes.id
ON DELETE CASCADE

source_project_test_case_id
→ project_test_cases.id
ON DELETE RESTRICT

target_project_test_case_id
→ project_test_cases.id
ON DELETE RESTRICT
```

唯一：

```sql
UNIQUE (
 branch_outcome_id,
 target_project_test_case_id
)
```

索引：

```sql
INDEX (
 target_project_test_case_id,
 active
)

INDEX (
 source_project_test_case_id,
 active
)
```

规则：

```text
active = false
```

表示原 Edge 已不再使用。

数据量非常小，但能帮助 Floating 判断。

---

# 24. Floating 计算

ProjectTestCase：

```text
is_root = true
→ CONNECTED
```

否则：

```text
存在 active Incoming Trigger
→ CONNECTED

不存在 active Incoming Trigger
→ FLOATING
```

`relation_status` 是派生状态缓存。

所有修改 Trigger 的 Service 必须同步更新目标 ProjectTestCase.relation_status。

---

# 25. Assignee Union

当新的 Trigger 指向已经存在的 ProjectTestCase：

```text
Source Assignees
∪
Existing Target Assignees
```

写入：

```text
project_test_case_assignees
```

通过唯一约束自动去重。

---

# 26. Complete Transaction

以下步骤必须在一个 Spring Transaction 中：

```text
1. 更新 ProjectTestCase
2. 更新 Selected Decision Points
3. 更新 Branch Outcomes
4. 解析 Transition
5. 创建 / 复用 Target ProjectTestCase
6. 写入 Source = PROGRESSIVE
7. 写入 Trigger
8. 合并 Assignees
9. 更新 Floating / Connected
10. 设置 Completed
```

任何一步失败：

```text
全部 Rollback
```

---

# 27. Evidence

---

## 27.1 evidence

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| project_test_case_id | UUID | NO |
| original_filename | VARCHAR(255) | NO |
| storage_key | VARCHAR(512) | NO |
| file_size | BIGINT | NO |
| content_type | VARCHAR(150) | YES |
| sha256 | VARCHAR(64) | YES |
| description | TEXT | YES |
| uploaded_by | UUID | NO |
| created_at | TIMESTAMPTZ | NO |

FK：

```text
project_test_case_id → project_test_cases.id ON DELETE RESTRICT
uploaded_by          → users.id              ON DELETE RESTRICT
```

唯一：

```sql
UNIQUE (storage_key)
```

索引：

```sql
INDEX (project_test_case_id)
INDEX (uploaded_by)
```

---

# 28. Notes

---

## 28.1 notes

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| project_test_case_id | UUID | NO |
| author_id | UUID | NO |
| content | TEXT | NO |
| created_at | TIMESTAMPTZ | NO |
| updated_at | TIMESTAMPTZ | NO |

FK：

```text
project_test_case_id → project_test_cases.id ON DELETE RESTRICT
author_id            → users.id              ON DELETE RESTRICT
```

索引：

```sql
INDEX (project_test_case_id, created_at)
INDEX (author_id)
```

---

# 29. Capability Update Request Evidence

---

## 29.1 capability_update_request_evidence

如果 Capability Request 引用现有 Project Evidence：

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| request_id | UUID | NO |
| evidence_id | UUID | NO |

唯一：

```sql
UNIQUE (request_id, evidence_id)
```

FK：

```text
request_id  → capability_update_requests.id ON DELETE CASCADE
evidence_id → evidence.id                   ON DELETE RESTRICT
```

---

# 30. Test Case Change Request

---

## 30.1 test_case_change_requests

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| master_test_case_id | UUID | NO |
| target_test_case_version_id | UUID | NO |
| issue_type | VARCHAR(64) | YES |
| issue_description | TEXT | NO |
| proposed_change | TEXT | YES |
| status | VARCHAR(24) | NO |
| submitted_by | UUID | NO |
| reviewed_by | UUID | YES |
| reviewed_at | TIMESTAMPTZ | YES |
| review_comment | TEXT | YES |
| created_revision_version_id | UUID | YES |
| created_at | TIMESTAMPTZ | NO |

CHECK：

```sql
CHECK (
 status IN ('PENDING', 'APPROVED', 'REJECTED')
);
```

FK：

```text
master_test_case_id
→ master_test_cases.id
ON DELETE RESTRICT

target_test_case_version_id
→ test_case_versions.id
ON DELETE RESTRICT

submitted_by
→ users.id
ON DELETE RESTRICT

reviewed_by
→ users.id
ON DELETE RESTRICT
```

`created_revision_version_id` FK 在版本表初始化后可直接建立。

索引：

```sql
INDEX (status, created_at)
INDEX (master_test_case_id)
INDEX (submitted_by)
```

---

# 31. Test Case Version Change Request FK

前面：

```text
test_case_versions.change_request_id
```

FK：

```text
→ test_case_change_requests.id
ON DELETE SET NULL
```

由于循环依赖：

```text
test_case_versions
↔
test_case_change_requests
```

建议在 Flyway 后续 Migration 使用：

```sql
ALTER TABLE
```

添加其中一侧 FK。

---

# 32. Revision Contributor

---

## 32.1 revision_contributors

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| test_case_version_id | UUID | NO |
| user_id | UUID | NO |
| added_by | UUID | NO |
| created_at | TIMESTAMPTZ | NO |

唯一：

```sql
UNIQUE (
 test_case_version_id,
 user_id
)
```

用途：

```text
Tester 获得指定 Draft 的临时编辑权限
```

---

# 33. Review Record

---

## 33.1 test_case_review_records

用于 Admin Review 轻量审计。

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| test_case_version_id | UUID | NO |
| action | VARCHAR(20) | NO |
| reviewer_id | UUID | NO |
| comment | TEXT | YES |
| created_at | TIMESTAMPTZ | NO |

Action：

```text
PUBLISH
RETURN
REJECT
```

CHECK：

```sql
CHECK (
 action IN ('PUBLISH', 'RETURN', 'REJECT')
);
```

不替代 Version Status。

只是记录 Review 操作。

---

# 34. Audit Log

---

## 34.1 audit_logs

| Column | Type | Null |
|---|---|---:|
| id | UUID | NO |
| actor_user_id | UUID | YES |
| action | VARCHAR(100) | NO |
| target_type | VARCHAR(100) | YES |
| target_id | UUID | YES |
| summary | TEXT | YES |
| details | JSONB | YES |
| ip_address | INET | YES |
| created_at | TIMESTAMPTZ | NO |

FK：

```text
actor_user_id → users.id ON DELETE SET NULL
```

索引：

```sql
INDEX (created_at DESC)
INDEX (actor_user_id, created_at DESC)
INDEX (action, created_at DESC)
```

只记录系统级关键操作。

---

# 35. Search Design

V1 搜索主要依赖：

```text
pg_trgm
+
必要的 PostgreSQL FTS
```

由于 Test Case 内容可能包含大量中文：

> **pg_trgm 是 V1 的主要模糊搜索机制。**

---

## 35.1 Trigram Index

建议至少：

```text
master_test_cases.case_code
test_case_versions.case_name
test_steps.content
tags.name
tools.name
```

示例：

```sql
CREATE INDEX idx_test_case_name_trgm
ON casehub.test_case_versions
USING GIN (case_name gin_trgm_ops);
```

---

## 35.2 English FTS

可增加：

```sql
CREATE INDEX idx_test_case_version_fts
ON casehub.test_case_versions
USING GIN (
  to_tsvector(
    'simple',
    coalesce(case_name, '') || ' ' ||
    coalesce(test_purpose, '') || ' ' ||
    coalesce(preconditions, '')
  )
);
```

中文检索不依赖该 FTS 分词结果。

---

# 36. 常用业务索引

必须重点优化：

```text
我的测试
项目测试总表
Progressive Trigger
公共 Test Case 搜索
Generation
Change Request
```

---

## 36.1 My Tests

```sql
CREATE INDEX idx_assignees_user
ON casehub.project_test_case_assignees(
 user_id,
 project_test_case_id
);
```

---

## 36.2 Project Test Table

```sql
CREATE INDEX idx_ptc_project_status
ON casehub.project_test_cases(
 project_id,
 removed,
 execution_status
);
```

```sql
CREATE INDEX idx_ptc_project_relation
ON casehub.project_test_cases(
 project_id,
 relation_status
);
```

---

## 36.3 Trigger

```sql
CREATE INDEX idx_trigger_target_active
ON casehub.project_test_case_triggers(
 target_project_test_case_id,
 active
);
```

---

## 36.4 Evidence

```sql
CREATE INDEX idx_evidence_ptc
ON casehub.evidence(project_test_case_id);
```

---

## 36.5 Generation

```sql
CREATE INDEX idx_generation_runs_project_time
ON casehub.generation_runs(
 project_id,
 executed_at DESC
);
```

---

# 37. Database Constraint 与 Service Constraint 边界

## Database 必须保证

```text
PK
FK
Unique
XOR
合法状态值
Master/Custom 二选一
同项目 Master Test Case 唯一
Assignee 去重
Source 去重
Trigger 去重
当前版本唯一
```

---

## Service 必须保证

```text
Capability Tree 无环

Category Level 2 Parent 必须 Level 1

Child YES → Parent Derived YES

Parent NO → Child Rule Evaluation 不适用

Generation Condition 最大嵌套 1 层

Rule Operator 与 Target Type 合法组合

Progressive DAG 无环

Transition Type 与 Target Count 匹配

Project Custom Target 必须属于同一 Project

SINGLE Complete 时只能选择 1 个 Decision Point

MULTIPLE Complete 时至少选择 1 个

Evidence Required 时至少有 Evidence

版本升级 Decision Point 变化时风险提示

Floating Relation Status 重新计算

Project Completed 条件判断
```

---

# 38. ON DELETE 策略

## CASCADE

仅用于纯从属表 / 映射表：

```text
user_roles
role_permissions
project_standards
project_coordinators
test_steps
test_case_tags
test_case_tools
test_case_standard_mappings
decision_points
transitions
transition_targets
generation_condition_groups
generation_conditions
generation_rule_outputs
generation_recommendation_rules
project_test_case_assignees
project_test_case_sources
project_decision_selections
project_branch_outcomes
revision_contributors
```

注意：

V1 业务层通常不会删除其 Parent 正式数据。

---

## RESTRICT

用于需要保护历史的核心引用：

```text
Master Test Case
Published Version
Project
Project Test Case
User
Tool
Capability
Evidence
```

---

## SET NULL

仅用于不应阻止历史数据保留的弱引用：

```text
AuditLog.actor
TestCaseVersion.changeRequest
```

---

# 39. Java JPA Mapping 原则

推荐：

```text
Entity 不直接暴露给 API
```

使用：

```text
Entity
DTO
Mapper
```

---

## 39.1 Fetch

默认：

```text
@ManyToOne(fetch = LAZY)
@OneToMany(fetch = LAZY)
```

避免：

```text
EAGER
```

导致大对象图一次加载。

---

## 39.2 Cascade

JPA Cascade 只在明确 Aggregate 内使用。

例如：

```text
TestCaseVersion
→ TestStep
```

可以：

```text
CascadeType.ALL
orphanRemoval = true
```

但：

```text
ProjectTestCase
→ User
```

绝对不能 Cascade Remove。

---

# 40. Transaction Boundary

重点 Transaction：

```text
Test Case Publish

Generation Run

Coordinator Add Recommendation

Capability Update Approve

Complete Project Test Case

Modify Completed Progressive Relation

Remove / Restore Project Test Case

Upgrade Test Case Version
```

---

# 41. Lock / Concurrency

产品已确认：

```text
不做复杂多人编辑冲突控制
```

因此 V1 不设计：

```text
Optimistic Lock UI
实时编辑锁
WebSocket 协同
```

但唯一约束冲突必须捕获并转换为业务结果。

例如：

```text
两个请求同时触发同一 NEXT_CASE
```

数据库 Unique 保证：

```text
只存在一个 ProjectTestCase
```

Service 捕获唯一约束冲突后：

```text
重新查询并复用现有实例
```

---

# 42. Project Test Case Source / Trigger 区别

必须保持：

```text
Source
= 为什么这个 Case 在项目里存在

Trigger
= 哪个 Progressive 分支实际连接到了它
```

例如：

```text
Sources:
GENERATED
PROGRESSIVE

Triggered By:
TC-A / BLE
TC-B / GATT
```

两个概念不能合并成一个表。

---

# 43. Version Upgrade

Project Test Case Version Upgrade：

```text
只修改：
test_case_version_id
```

不新建 ProjectTestCase。

保留：

```text
Assignees
Evidence
Notes
Execution Status
Sources
Triggers
```

如果新的 Version DecisionPoint 与旧版本不同：

```text
Service 显示风险确认
```

当前历史 Selection / Outcome 可继续指向旧 DecisionPoint。

---

# 44. Completed 再触发

如果 Target ProjectTestCase：

```text
execution_status = COMPLETED
```

再次被新的 Branch Trigger：

```text
不修改 execution_status
```

只：

```text
新增 / 激活 Trigger
合并 Assignees
增加 Source PROGRESSIVE
```

---

# 45. Project Completion Query

Project Completed 条件：

```sql
NOT EXISTS (
    SELECT 1
    FROM casehub.project_test_cases ptc
    WHERE ptc.project_id = :projectId
      AND ptc.removed = false
      AND ptc.execution_status <> 'COMPLETED'
)
```

Floating Case 仍参与该判断。

---

# 46. Excel 不需要数据库表

V1 Excel：

```text
同步生成
```

无需建立：

```text
export_jobs
```

如果后续导出变成大型异步任务，再增加 Job Table。

---

# 47. Notification 不需要数据库表

V1 NEW：

不建立复杂 Notification Center。

建议在：

```text
project_test_case_assignees
```

增加：

```text
first_viewed_at TIMESTAMPTZ NULL
```

这样：

```text
first_viewed_at IS NULL
→ NEW
```

因此最终 `project_test_case_assignees` 建议增加字段：

```text
first_viewed_at
```

每个 Tester 的 NEW 状态独立。

---

# 48. NEW 任务索引

```sql
CREATE INDEX idx_assignee_new_tasks
ON casehub.project_test_case_assignees(
 user_id,
 first_viewed_at
);
```

---

# 49. Attachment / Evidence Storage Key

禁止使用：

```text
用户原始文件名
```

作为服务器真实路径。

推荐 Storage Key：

```text
evidence/{projectId}/{projectTestCaseId}/{uuid}
```

附件：

```text
test-case/{testCaseVersionId}/{uuid}
tool/{toolId}/{uuid}
```

数据库：

```text
original_filename
```

仅用于下载显示。

---

# 50. Initial Data Seed

Flyway 初始化至少 Seed：

```text
Roles
Permissions
Role-Permission Mapping
Admin Account（可通过环境变量初始化）
基础 Standard / Task Type 可选
```

Admin 初始密码不能写死到 Migration SQL。

---

# 51. Flyway Migration 顺序

推荐：

```text
V001__create_schema_extensions.sql

V002__identity_rbac.sql

V003__standard_category_tag_tool_capability.sql

V004__master_test_case_library.sql

V005__master_progressive_dag.sql

V006__project_core_and_capability.sql

V007__generation_rules.sql

V008__generation_runtime.sql

V009__project_custom_test_cases.sql

V010__project_test_execution.sql

V011__evidence_notes.sql

V012__change_request_review.sql

V013__audit_log.sql

V014__cross_module_foreign_keys.sql

V015__search_indexes.sql

V016__business_indexes.sql

V017__seed_roles_permissions.sql
```

---

# 52. V001

内容：

```text
CREATE SCHEMA casehub
pgcrypto
pg_trgm
```

---

# 53. V002

内容：

```text
users
roles
permissions
user_roles
role_permissions
```

---

# 54. V003

内容：

```text
standard_task_types
categories
tags
tools
capabilities
```

---

# 55. V004

内容：

```text
master_test_cases
test_case_versions
test_steps
test_case_tags
test_case_tools
test_case_standard_mappings
test_case_attachments
tool_attachments
```

---

# 56. V005

内容：

```text
decision_points
transitions
transition_targets
```

---

# 57. V006

内容：

```text
projects
project_standards
project_coordinators
project_capabilities
capability_update_requests
```

---

# 58. V007

内容：

```text
generation_rules
generation_condition_groups
generation_conditions
generation_rule_outputs
```

---

# 59. V008

内容：

```text
generation_runs
generation_recommendations
generation_recommendation_rules
project_test_case_preferences
```

---

# 60. V009

内容：

```text
project_custom_test_cases
project_custom_test_steps
project_custom_test_case_tools
project_custom_decision_points
project_custom_transitions
project_custom_transition_targets
```

---

# 61. V010

内容：

```text
project_test_cases
project_test_case_assignees
project_test_case_sources
project_decision_selections
project_branch_outcomes
project_test_case_triggers
```

---

# 62. V011

内容：

```text
evidence
notes
capability_update_request_evidence
```

---

# 63. V012

内容：

```text
test_case_change_requests
revision_contributors
test_case_review_records
```

---

# 64. V013

内容：

```text
audit_logs
```

---

# 65. V014

用于解决循环依赖：

```text
capability_update_requests.related_project_test_case_id

generation_recommendations.added_project_test_case_id

test_case_versions.change_request_id

test_case_change_requests.created_revision_version_id
```

等跨模块 FK。

---

# 66. V015

创建：

```text
pg_trgm Search Index
FTS Index
```

---

# 67. V016

创建：

```text
项目查询索引
我的测试索引
Trigger 索引
Evidence 索引
Generation 索引
Change Request 索引
```

---

# 68. V017

Seed：

```text
Role
Permission
RolePermission
```

---

# 69. 预计表数量

V1 预计：

```text
约 40 张表
```

这是正常的关系型业务建模结果。

大部分是：

```text
关系表
版本子表
运行时关系表
```

并不是 40 个独立复杂模块。

---

# 70. 核心大表

真正需要重点维护的核心表只有：

```text
users

projects

capabilities
project_capabilities

master_test_cases
test_case_versions
decision_points
transitions

generation_rules

project_test_cases
project_test_case_assignees
project_test_case_triggers
project_branch_outcomes

evidence
notes
```

---

# 71. 第一版容量预估

内部 Test Case 平台通常不会达到极端数据库规模。

即使：

```text
5,000 Master Test Cases
10,000 Versions
5,000 Projects
每项目 300 Project Test Cases
```

Project Test Case：

```text
约 1,500,000 Rows
```

对 PostgreSQL 仍属于可轻松管理的范围。

真正占空间的是：

```text
Evidence File
```

不是数据库关系数据。

---

# 72. Evidence 容量策略

数据库只存 metadata。

文件存储容量必须独立规划。

后续 Deployment 文档需明确：

```text
最大单文件
项目总 Evidence 大小
磁盘监控
备份保留
```

---

# 73. 数据库备份对象

必须完整备份：

```text
casehub Schema
```

包括：

```text
业务表
索引
约束
```

实际文件需要独立备份：

```text
/data/evidence
/data/attachments
```

---

# 74. Database Schema V1.0 冻结结果

V1 数据库正式采用：

```text
PostgreSQL
UUID PK
TIMESTAMPTZ
VARCHAR + CHECK Enum
Strong FK
Unique Constraint
Partial Unique Index
pg_trgm
Flyway
```

核心 Version：

```text
MasterTestCase
↓
TestCaseVersion
↓
ProjectTestCase Version Binding
```

核心 DAG：

```text
TestCaseVersion
↓
DecisionPoint
↓
Transition
↓
TransitionTarget
↓
MasterTestCase
```

核心 Runtime：

```text
ProjectTestCase
↓
DecisionSelection
↓
BranchOutcome
↓
ProjectTestCaseTrigger
↓
Target ProjectTestCase
```

核心 Generation：

```text
Project Capability
+
Standard / Task Type
↓
Generation Rule
↓
Generation Run
↓
Recommendation
↓
Coordinator Add
↓
ProjectTestCase
```

---

# 75. 下一阶段

下一份技术文档：

```text
IoT-Security-Case-Hub_Backend-Architecture_V1.0.md
```

将正式定义：

```text
Spring Boot Package Structure
Domain Module Boundary
Entity
Repository
Service
Controller
DTO
Mapper
Transaction Boundary
Exception
RBAC Enforcement
ProgressiveRuntimeService
GenerationEngine
CapabilityEngine
VersionService
StorageService
```

完成 Backend Architecture 后，再写：

```text
API Design V1.0
```

然后进入：

```text
Frontend Architecture V1.0
```
