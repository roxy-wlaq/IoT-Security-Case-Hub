# IoT-Security-Case-Hub
## Security & RBAC Detail V1.0

> 基于：
>
> - `IoT-Security-Case-Hub_System-Design_V0.6.md`
> - `IoT-Security-Case-Hub_Technical-Architecture_V1.0.md`
> - `IoT-Security-Case-Hub_Data-Model_V1.0.md`
> - `IoT-Security-Case-Hub_Database-Schema_V1.0.md`
> - `IoT-Security-Case-Hub_Backend-Architecture_V1.0.md`
> - `IoT-Security-Case-Hub_API-Design_V1.0.md`
> - `IoT-Security-Case-Hub_Frontend-Architecture_V1.0.md`
> - `IoT-Security-Case-Hub_File-Storage-Security_V1.0.md`
>
> 本文档冻结 V1 的认证、授权与 Web 安全设计，包括：
>
> - Authentication
> - Session
> - Cookie
> - CSRF
> - Password
> - Role / Permission
> - Resource Authorization
> - Admin / Coordinator / Tester
> - Project Permission
> - Assignee Permission
> - Draft Contributor
> - Evidence / Notes
> - Login Failure
> - CORS
> - Security Headers
> - Audit
>
> V1 面向公司内网部署，但仍按正式业务系统的安全基线设计。

---

# 1. 总体安全原则

V1 安全设计遵循：

```text
Authentication
+
RBAC
+
Resource-level Authorization
+
Server-side Validation
+
Database Constraint
+
Audit
```

前端权限控制仅用于：

```text
UI 展示
```

不能替代后端鉴权。

---

# 2. Authentication 方案

V1 正式采用：

```text
Local Account
+
Spring Security
+
Server-side HTTP Session
```

不采用：

```text
JWT LocalStorage
JWT SessionStorage
OAuth2
OIDC
LDAP
Active Directory
```

第一版保持简单。

---

# 3. 为什么使用 Server-side Session

本系统是：

```text
内网 SPA
单体 Spring Boot
单一 Backend Instance 为主
```

Server-side Session 优点：

```text
实现简单
Logout 立即失效
无需 Refresh Token
无需 JWT Revocation
Spring Security 原生支持成熟
```

---

# 4. Session Cookie

Session Cookie 使用：

```text
JSESSIONID
```

必须：

```text
HttpOnly = true
Secure = true（生产 HTTPS）
SameSite = Lax
```

---

# 5. Cookie 不允许 JavaScript 读取

Session Cookie：

```text
HttpOnly
```

React 前端不能读取：

```text
JSESSIONID
```

防止 XSS 后直接窃取 Session ID。

---

# 6. Session 生命周期

V1 推荐：

```text
Idle Timeout = 8 hours
```

可配置。

例如：

```yaml
server:
  servlet:
    session:
      timeout: 8h
```

---

# 7. Session 失效

以下情况立即失效：

```text
Logout
User Disabled
Password Reset（推荐）
Admin 强制退出（未来可扩展）
```

---

# 8. Backend 重启

V1 默认使用 Spring Boot 内存 Session。

Backend 重启：

```text
现有 Session 失效
```

对于内网 V1 可接受。

如果以后需要：

```text
多实例部署
重启不退出
```

再增加：

```text
Spring Session JDBC
或 Redis
```

---

# 9. Login API

```text
POST /api/v1/auth/login
```

成功：

```text
建立 Server Session
返回用户信息
```

失败：

```text
AUTH_INVALID_CREDENTIALS
```

不区分：

```text
用户名不存在
密码错误
```

避免账号枚举。

---

# 10. Logout

```text
POST /api/v1/auth/logout
```

必须：

```text
invalidate session
delete session cookie
```

---

# 11. CSRF

由于使用 Cookie Session：

> V1 必须启用 CSRF Protection。

不能因为 API 是 JSON 就关闭 CSRF。

---

# 12. CSRF 方案

推荐：

```text
Spring Security CookieCsrfTokenRepository
```

CSRF Cookie：

```text
XSRF-TOKEN
```

该 Cookie：

```text
HttpOnly = false
```

因为 React/Axios 需要读取 token。

---

# 13. CSRF Header

前端请求状态修改 API 时：

```text
X-XSRF-TOKEN
```

Header 发送 Token。

Axios 支持配置：

```text
xsrfCookieName
xsrfHeaderName
```

---

# 14. CSRF 适用 Method

必须检查：

```text
POST
PUT
PATCH
DELETE
```

无需：

```text
GET
HEAD
OPTIONS
```

---

# 15. Login CSRF

Spring Security 配置必须确保：

```text
登录流程
CSRF token 初始化
```

前端启动后可通过：

```text
GET /api/v1/auth/me
```

或单独初始化端点获得 CSRF Cookie。

---

# 16. Password Storage

正式采用：

```text
BCrypt
```

使用：

```text
PasswordEncoder
```

禁止：

```text
MD5
SHA-1
SHA-256(password)
明文密码
可逆加密密码
```

---

# 17. BCrypt Strength

推荐：

```text
cost = 12
```

最终应通过生产服务器性能测试确认。

允许配置：

```text
10~12+
```

但不能太低。

---

# 18. Password Policy

V1 推荐最低：

```text
12 characters
```

不强制复杂的：

```text
必须大小写+数字+特殊字符
```

原因：

更长密码通常比机械复杂度规则更有效。

---

# 19. Password 最低规则

至少：

```text
长度 >= 12
长度 <= 128
不能全为空白
不能等于 username
```

---

# 20. Initial Password

Admin 创建用户时：

```text
设置临时初始密码
```

建议增加：

```text
mustChangePassword = true
```

如果 V1 暂不实现首次强制修改，也至少应允许：

```text
Admin Reset Password
```

---

# 21. 推荐增加 must_change_password

虽然 Database Schema V1.0 当前未列出该字段，建议在实现前修订 users 表：

```text
must_change_password BOOLEAN NOT NULL DEFAULT false
```

用于：

```text
初始账号
管理员重置密码
```

用户下次登录后必须修改。

---

# 22. Change Password API

建议增加：

```text
POST /api/v1/auth/change-password
```

Request：

```json
{
  "currentPassword": "...",
  "newPassword": "..."
}
```

---

# 23. Admin Reset Password

Admin：

```text
POST /api/v1/users/{id}/reset-password
```

重置后：

```text
mustChangePassword = true
```

并使该用户旧 Session 失效。

---

# 24. Login Failure Protection

V1 不需要复杂 WAF。

应用层至少需要：

```text
Login Failure Counter
```

---

# 25. 登录限制

推荐：

```text
5 次失败
↓
临时锁定 15 分钟
```

---

# 26. 锁定维度

建议以：

```text
username
+
source IP
```

综合限制。

避免仅 IP：

```text
公司 NAT 下误伤全部用户
```

---

# 27. Login Failure 数据

V1 可以先使用：

```text
内存 Cache
```

因为：

```text
单实例
不需要持久化
```

Backend 重启后计数清空可接受。

---

# 28. 锁定提示

统一：

```text
AUTH_LOGIN_TEMPORARILY_BLOCKED
```

不要返回：

```text
还有几次机会
```

---

# 29. User Disable

用户：

```text
enabled = false
```

后：

```text
不能新登录
```

现有 Session 在下次请求时也应被拒绝。

---

# 30. RBAC 总体模型

权限模型：

```text
User
↓
Role[]
↓
Permission[]
```

V1 Role：

```text
ADMIN
TEST_COORDINATOR
TESTER
```

---

# 31. Role 与 Permission 分离

虽然只有三个 Role，也正式保留：

```text
permissions
role_permissions
```

原因：

```text
权限集中管理
代码不硬编码全部 Role 行为
后续扩展容易
```

---

# 32. Permission Code 命名

格式：

```text
resource:action
```

例如：

```text
project:create
project:update
test_case:publish
```

---

# 33. V1 Permission Code

推荐冻结：

```text
user:read
user:create
user:update
user:disable
role:manage

standard:read
standard:manage
category:read
category:manage
tag:read
tag:manage
tool:read
tool:manage

capability:read
capability:manage_library
project_capability:read
project_capability:update
capability_request:create
capability_request:review

project:read
project:create
project:update
project:archive
project:complete

project_test_case:read
project_test_case:add
project_test_case:remove
project_test_case:restore
project_test_case:assign
project_test_case:execute

test_case:read
test_case:draft_create
test_case:draft_edit
test_case:submit_review
test_case:review
test_case:publish
test_case:deprecate

generation_rule:read
generation_rule:manage
generation:run
generation:review_recommendation

evidence:read
evidence:upload
evidence:delete

note:read
note:create
note:update_own
note:delete_own

change_request:create
change_request:review

export:project

audit:read
```

---

# 34. Admin 默认权限

ADMIN：

```text
拥有全部 Permission
```

但仍可受到某些：

```text
数据完整性规则
```

约束。

例如：

```text
不能发布有 DAG Cycle 的 Version
```

Admin 权限不能绕过业务一致性。

---

# 35. Coordinator 默认权限

TEST_COORDINATOR 主要拥有：

```text
project:read
project:create
project:update
project:archive
project:complete

project_capability:read
project_capability:update
capability_request:review

project_test_case:read
project_test_case:add
project_test_case:remove
project_test_case:restore
project_test_case:assign
project_test_case:execute

test_case:read
test_case:draft_create
test_case:draft_edit
test_case:submit_review

generation_rule:read
generation:run
generation:review_recommendation

evidence:read
evidence:upload
evidence:delete

note:read
note:create
note:update_own
note:delete_own

change_request:create
change_request:review

export:project
```

不包含：

```text
test_case:publish
test_case:deprecate
generation_rule:manage
capability:manage_library
user:manage
audit:read
```

---

# 36. Tester 默认权限

TESTER：

```text
project:read
project_capability:read

project_test_case:read
project_test_case:execute

test_case:read

evidence:read
evidence:upload
evidence:delete

note:read
note:create
note:update_own
note:delete_own

capability_request:create
change_request:create

tool:read
standard:read
category:read
tag:read

export:project
```

---

# 37. Tester Custom Case

产品已确定：

```text
Tester 可以创建 Project Custom Test Case
```

因此额外 Permission：

```text
project_custom_test_case:create
project_custom_test_case:edit_own_or_assigned
```

---

# 38. Resource-level Authorization

RBAC 只回答：

```text
这个角色原则上是否有这个动作
```

还必须判断：

```text
对这个具体资源是否有权限
```

---

# 39. ResourceAuthorizationService

后端统一：

```text
ResourceAuthorizationService
```

提供：

```text
requireAdmin()

requireProjectAccess(projectId)

requireProjectCoordinator(projectId)

requireProjectMember(projectId)

requireAssignee(projectTestCaseId)

requireDraftEditor(versionId)

requireNoteAuthor(noteId)
```

---

# 40. Project Access

Admin：

```text
所有 Project
```

Coordinator：

```text
自己负责的 Project
```

Tester：

```text
自己有 Assignee Task
或被授权查看的 Project
```

---

# 41. Tester 查看项目全部用例

已经冻结：

```text
Tester 可查看项目全部用例
```

因此：

如果 Tester 是该 Project 的成员：

```text
未分配给自己的 Project Test Case
→ Read Only
```

---

# 42. Project Member 定义

V1 建议 Project Member：

```text
Project Coordinator
OR
任意 ProjectTestCase Assignee
```

未来如果需要观察者角色，再增加：

```text
project_members
```

V1 不增加额外表。

---

# 43. Project Update

修改：

```text
Project Name
Device Name
Standard / Task Type
Generation Mode
Coordinator
```

只允许：

```text
Project Coordinator
Admin
```

---

# 44. Project Archive

只允许：

```text
Project Coordinator
Admin
```

Tester 不允许。

---

# 45. Project Test Case Read

项目成员：

```text
可读全部 PTC
```

非项目成员：

```text
403
```

Admin 例外。

---

# 46. Project Test Case Execute

必须：

```text
Current User ∈ Assignees
```

或：

```text
Admin / Coordinator 具备明确管理执行权限
```

V1 建议 Coordinator 也可以执行自己项目中的 Test Case，即使不在 Assignees。

但 UI 默认仍以 Assignee 为执行主体。

---

# 47. Tester 未分配用例

Tester：

```text
可看
不可修改
不可 Complete
不可 Reopen
不可 Decision Update
不可上传 Evidence
不可写 Note
```

---

# 48. Evidence Read

Project Member：

```text
可以查看和下载
```

---

# 49. Evidence Upload

允许：

```text
当前 Assignee
Project Coordinator
Admin
```

---

# 50. Evidence Delete

产品已冻结：

```text
所有当前 Assignee
均可删除共享 Evidence
```

此外：

```text
Project Coordinator
Admin
```

也可删除。

---

# 51. Evidence Removed Case

如果：

```text
ProjectTestCase.removed = true
```

默认：

```text
不能新增 Evidence
```

已有 Evidence：

```text
仍可读
```

---

# 52. Note Read

Project Member：

```text
可读
```

---

# 53. Note Create

允许：

```text
Assignee
Project Coordinator
Admin
```

Tester 未分配：

```text
不可新增
```

---

# 54. Note Update / Delete

Tester：

```text
仅作者本人
```

Coordinator：

产品设计并未要求 Coordinator 可以修改别人 Note。

因此正式冻结：

```text
Coordinator 也不能编辑其他人的 Note
```

Admin 默认也不直接修改内容。

管理员如需处理违规内容可未来增加管理能力。

---

# 55. Test Case Library Read

所有登录用户：

```text
可读 Published / Deprecated / Historical Published
```

Draft / Review：

```text
只对有权限人员可见
```

---

# 56. Draft Edit

允许：

```text
Admin
Draft Owner Coordinator
Revision Contributor
```

---

# 57. Revision Contributor

Tester 被加入某个 Draft：

```text
只获得该 Draft 的 Edit 权限
```

不会获得：

```text
其它 Draft
Published Test Case
Master Library 管理
```

---

# 58. Published Version

任何角色：

```text
不能原地编辑
```

包括 Admin。

修改必须：

```text
Create Revision
```

---

# 59. Submit Review

允许：

```text
Draft Owner Coordinator
Admin
```

Contributor Tester：

```text
可经资源级闸门提交（HIGH-02）——canEditDraftById 为真即放行；无关 Tester（非 owner / 非 contributor / 非 admin）不能 Submit Review
```

---

# 60. Publish

只允许：

```text
Admin
```

---

# 61. Deprecated

只允许：

```text
Admin
```

---

# 62. Generation Rule

读取：

```text
Admin
Coordinator
```

管理：

```text
Admin
```

Tester 不需要管理规则。

---

# 63. Run Generation

允许：

```text
Project Coordinator
Admin
```

Tester：

```text
不能 Run Generation
```

---

# 64. Recommendation Add / Ignore

只允许：

```text
Project Coordinator
Admin
```

---

# 65. Capability Library

读取：

```text
所有登录用户
```

管理：

```text
Admin
```

---

# 66. Project Capability

读取：

```text
Project Member
```

直接更新：

```text
Project Coordinator
Admin
```

Tester：

```text
只能 Submit Capability Update Request
```

---

# 67. Capability Request Review

只允许：

```text
Project Coordinator
Admin
```

---

# 68. Test Case Change Request

提交：

```text
所有登录用户
```

如果是 Project Context：

```text
至少能读取目标 Test Case
```

---

# 69. Change Request Review

允许：

```text
Coordinator
Admin
```

---

# 70. Custom Test Case Create

Tester：

```text
只能在自己参与的 Project 中创建
```

创建后：

```text
自动把自己加入 Assignees
```

Tester 不能：

```text
任意给其他用户分配
```

---

# 71. Custom Test Case Coordinator

Coordinator：

```text
可以创建
编辑
调整 Assignees
参与 Progressive
```

---

# 72. Submit Custom Case to Library

允许：

```text
Custom Case Creator
Project Coordinator
Admin
```

提交结果：

```text
创建 Master Test Case Draft
```

不直接 Published。

---

# 73. Export

Project Excel：

```text
Project Member
Coordinator
Admin
```

都可导出。

---

# 74. Audit Log

只允许：

```text
Admin
```

V1 不给 Coordinator 查看全系统 Audit。

---

# 75. allowedActions

API 详情建议返回：

```text
allowedActions[]
```

由后端计算。

例如：

```json
{
  "allowedActions": [
    "READ",
    "EDIT",
    "COMPLETE",
    "UPLOAD_EVIDENCE",
    "ADD_NOTE"
  ]
}
```

---

# 76. 前端不自行推理复杂权限

前端可根据 Role 控制：

```text
菜单
```

但资源按钮优先依赖：

```text
allowedActions[]
```

---

# 77. Method Security

建议结合：

```text
@PreAuthorize
```

处理简单 Permission。

例如：

```text
@PreAuthorize("hasAuthority('test_case:publish')")
```

---

# 78. Resource Check

复杂条件放 Service：

```text
authorization.requireProjectCoordinator(projectId)
```

不要写几十行 SpEL。

---

# 79. Security Filter Chain

建议顺序：

```text
Security Headers
CSRF
Authentication
Authorization
Exception Handling
```

---

# 80. CORS

生产推荐：

```text
Frontend 与 Backend 通过同一 Nginx Origin
```

例如：

```text
https://casehub.company.local/
https://casehub.company.local/api/v1/
```

这样：

```text
不需要开放跨域 CORS
```

---

# 81. Development CORS

开发环境：

```text
Vite localhost
→ Spring Boot localhost
```

允许明确 Origin：

```text
http://localhost:5173
```

禁止：

```text
Access-Control-Allow-Origin: *
+
Credentials
```

---

# 82. CORS Methods

仅允许必要：

```text
GET
POST
PUT
DELETE
OPTIONS
```

如果未来使用 PATCH 再增加。

---

# 83. Security Headers

生产至少：

```text
X-Content-Type-Options: nosniff

X-Frame-Options: DENY
或
Content-Security-Policy frame-ancestors 'none'

Referrer-Policy: no-referrer

Permissions-Policy:
camera=(),
microphone=(),
geolocation=()
```

---

# 84. Content Security Policy

推荐基线：

```text
default-src 'self'
script-src 'self'
style-src 'self' 'unsafe-inline'
img-src 'self' data: blob:
connect-src 'self'
object-src 'none'
frame-ancestors 'none'
base-uri 'self'
```

Ant Design 具体样式可能需要：

```text
'unsafe-inline'
```

后续可根据构建结果收紧。

---

# 85. HSTS

如果生产确定全站 HTTPS：

```text
Strict-Transport-Security
```

推荐：

```text
max-age=31536000
```

内网初期证书环境未稳定时可以暂缓开启长周期 HSTS。

---

# 86. HTTPS

生产环境推荐：

```text
HTTPS
```

即使是内网。

HTTP 只允许：

```text
开发环境
```

---

# 87. Session Fixation

Spring Security 登录成功必须：

```text
renew session id
```

Spring Security 默认支持。

---

# 88. Concurrent Sessions

V1 不强制：

```text
一个用户只能登录一处
```

允许多个 Session。

未来如有需求再限制。

---

# 89. Remember Me

V1：

```text
不实现 Remember Me
```

避免长期 Cookie。

---

# 90. API Rate Limit

业务 API V1 不做全局严格 Rate Limit。

登录接口建议限制。

其它 API 通过：

```text
Nginx 基础限制
```

防止明显异常请求即可。

---

# 91. Nginx Login Rate Limit

可配置：

```text
/api/v1/auth/login
```

例如：

```text
每 IP 10 次/分钟
```

应用层仍做 username 失败锁定。

---

# 92. SQL Injection

使用：

```text
JPA Parameter Binding
NamedParameterJdbcTemplate
```

禁止：

```text
String 拼 SQL
```

尤其 Search / Sort。

---

# 93. Sort 白名单

前端传：

```text
sort=field,desc
```

Backend 必须：

```text
允许字段白名单
```

不能直接把用户字段拼入 SQL。

---

# 94. XSS

React 默认：

```text
转义文本
```

业务页面禁止随意：

```text
dangerouslySetInnerHTML
```

---

# 95. Notes / Test Steps

默认按：

```text
Plain Text
```

展示。

如果未来支持 Markdown：

```text
必须经过 Sanitization
```

V1 不支持 HTML 内容。

---

# 96. Upload Security

上传文件：

```text
Untrusted Data
```

必须遵守 File Storage Security 文档。

核心：

```text
不执行
不静态暴露
UUID Storage Key
下载鉴权
```

---

# 97. File Download Header

默认：

```text
Content-Disposition: attachment
X-Content-Type-Options: nosniff
```

---

# 98. Sensitive Logging

禁止日志记录：

```text
Password
Session ID
CSRF Token
Cookie
Authorization Header
完整 Evidence 内容
```

---

# 99. Login Audit

Audit：

```text
LOGIN_SUCCESS
LOGIN_FAILURE
LOGOUT
```

推荐记录：

```text
username
userId if known
IP
time
```

失败日志不要记录 Password。

---

# 100. Permission Audit

必须记录：

```text
USER_ROLE_CHANGE
ROLE_PERMISSION_CHANGE
USER_DISABLE
PASSWORD_RESET
```

---

# 101. Business Audit

继续记录：

```text
PROJECT_CREATE
PROJECT_ARCHIVE
TEST_CASE_PUBLISH
TEST_CASE_DEPRECATE
GENERATION_RULE_UPDATE
CAPABILITY_LIBRARY_UPDATE
EVIDENCE_DELETE
```

---

# 102. Audit details

Audit details 使用：

```text
JSONB
```

只能放：

```text
必要字段
```

不要复制整条 Evidence / Note 内容。

---

# 103. Trace ID

每个请求：

```text
traceId
```

日志和错误响应关联。

---

# 104. Error Response

权限失败：

```text
403
```

Response：

```json
{
  "code": "FORBIDDEN",
  "message": "You do not have permission to perform this action.",
  "traceId": "...",
  "details": {}
}
```

不要暴露：

```text
内部权限表达式
SQL
StackTrace
```

---

# 105. 404 vs 403

对于敏感资源：

如果用户根本不应知道资源存在，可统一：

```text
404
```

对于已知 Project 内的普通动作：

```text
403
```

V1 可以优先保持业务清晰，按 API 场景决定。

---

# 106. Project Number Enumeration

Project ID 使用 UUID。

Project Number 可读但不作为：

```text
鉴权依据
```

---

# 107. UUID 不是权限

即使 UUID 不容易猜：

> 仍必须做 Resource Authorization。

---

# 108. Admin 安全

初始 Admin：

```text
不得使用固定默认密码
```

必须通过：

```text
环境变量
首次初始化
```

设置。

---

# 109. Admin 初始密码

不写入：

```text
Flyway SQL
Git
Docker Compose 明文
README 示例真实密码
```

---

# 110. Production Secret

至少：

```text
DB Password
Initial Admin Password
TLS Private Key
```

不提交 Git。

---

# 111. Session Cookie Domain

生产建议不显式设置宽泛：

```text
.company.local
```

优先 Host-only Cookie。

避免其它子域共享 Session。

---

# 112. SameSite

默认：

```text
Lax
```

如果 Frontend / Backend 同 Origin：

```text
完全足够
```

无需：

```text
SameSite=None
```

---

# 113. CSRF 与 Multipart

Evidence Upload：

```text
multipart/form-data
```

同样必须带：

```text
X-XSRF-TOKEN
```

---

# 114. Swagger

开发环境：

```text
Swagger UI
```

可以开放。

生产：

```text
可关闭
或只允许内网管理员网段
```

---

# 115. Actuator

生产只开放：

```text
health
```

必要时：

```text
info
```

不要公开：

```text
env
beans
mappings
heapdump
```

---

# 116. Database Account

Backend PostgreSQL 用户：

```text
只能访问 casehub schema 所需权限
```

不要使用：

```text
postgres superuser
```

运行应用。

---

# 117. Migration Account

如果部署环境允许，可区分：

```text
Flyway migration account
Application runtime account
```

V1 可以先使用同一非 superuser 账号，但权限要受控。

---

# 118. File Permission

Storage Root：

```text
Backend container user
```

拥有权限。

Nginx：

```text
不能读 Evidence Volume
```

---

# 119. Container Security

Backend：

```text
non-root
read-only application filesystem where possible
仅 storage volume 可写
```

---

# 120. Dependency Security

Maven / npm 依赖需：

```text
锁定版本
定期更新
```

建议 CI 后续加入：

```text
OWASP Dependency-Check
npm audit
```

但不要把所有低风险告警都作为立即阻断。

---

# 121. Secrets in Frontend

React 构建产物中不能放：

```text
DB Password
Backend Secret
Admin Password
Private Key
```

前端所有环境变量都视为：

```text
公开信息
```

---

# 122. Security Test 必须覆盖

至少：

```text
未登录访问
Tester 越权 Publish
Tester 越权 Assign
Tester 修改未分配 Case
Tester 下载其它 Project Evidence
Note 修改他人内容
Draft Contributor 编辑其它 Draft
CSRF 缺失
Session 失效
Disabled User
Login Brute Force
Path Traversal Filename
CORS 非允许 Origin
```

---

# 123. API Authorization Test

每个写 API 至少测试：

```text
Allowed Role
Denied Role
Allowed Resource
Denied Resource
```

---

# 124. Permission Matrix

## Admin

```text
System Admin
User / Role
Capability Library
Generation Rule
Project Management
Master Test Case Review / Publish
All Project Access
Audit
```

---

## Coordinator

```text
Own Project Management
Project Capability
Generation
Test Plan
Assignee
Remove / Restore
Custom Case
Change Request Review
Draft / Revision
Execution Management
```

不能：

```text
Publish
Global User Admin
Capability Library Admin
Generation Rule Admin
```

---

## Tester

```text
Library Read
Own Project Read
Assigned Task Execute
Evidence
Notes
Decision Point
Capability Request
Test Case Change Request
Project Custom Case
```

不能：

```text
Project Edit
Assignee Edit
Project Capability Direct Edit
Master Published Edit
Publish
```

---

# 125. Resource Matrix

| Resource | Admin | Coordinator | Tester |
|---|---|---|---|
| Public Test Case Read | Yes | Yes | Yes |
| Published Edit | No direct edit | No direct edit | No |
| Draft Edit | Yes | Owned/authorized | Contributor only |
| Publish | Yes | No | No |
| Project Create | Yes | Yes | No |
| Project Update | Yes | Own | No |
| Project Capability Update | Yes | Own | Request only |
| Generation Run | Yes | Own | No |
| Assignee Update | Yes | Own | No |
| Assigned PTC Execute | Yes | Yes | Yes |
| Unassigned PTC Execute | Yes | Coordinator own project | No |
| Evidence Upload | Yes | Own project | Assigned only |
| Evidence Delete | Yes | Own project | Assigned shared |
| Notes Create | Yes | Own project | Assigned only |
| Change Request | Yes | Yes | Yes |
| Audit Read | Yes | No | No |

---

# 126. Security & RBAC V1.0 最终冻结

V1 正式采用：

```text
Spring Security
Server-side HTTP Session
HttpOnly JSESSIONID
SameSite=Lax
CSRF Enabled
BCrypt
RBAC
Permission Codes
ResourceAuthorizationService
allowedActions
Same-origin Production
Strict CORS Development
Security Headers
Audit
```

核心原则：

```text
Role 决定基础能力
Resource Authorization 决定具体数据权限
前端只负责显示
后端始终重新校验
```

文件：

```text
永远作为 Untrusted Data
```

Published Test Case：

```text
永远不可原地修改
```

Tester：

```text
可以阅读项目全部用例
但只能执行 Assigned Case
```

这一安全模型作为 V1 后端和前端权限实现的唯一基准。

---

# 127. 对已有数据库设计的一个小修订

由于本安全设计增加：

```text
must_change_password
```

建议在 Database Schema 的后续 Flyway 增加：

```text
V018__add_user_password_change_flag.sql
```

字段：

```sql
ALTER TABLE casehub.users
ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT false;
```

如果开发前统一重写初始 Migration，也可以直接合并进：

```text
V002__identity_rbac.sql
```

---

# 128. 下一阶段

下一份文档：

```text
IoT-Security-Case-Hub_Testing-Strategy_V1.0.md
```

将冻结：

```text
Unit Test
Integration Test
Repository Test
API Test
Security Test
Progressive DAG Test
Generation Rule Test
Frontend Component Test
E2E
Testcontainers
Playwright
Coverage Priorities
CI Quality Gate
Acceptance Scenarios
