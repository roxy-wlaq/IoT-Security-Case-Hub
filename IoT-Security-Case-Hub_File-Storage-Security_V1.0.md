# IoT-Security-Case-Hub
## File Storage & Security V1.0

> 基于：
>
> - `IoT-Security-Case-Hub_System-Design_V0.6.md`
> - `IoT-Security-Case-Hub_Technical-Architecture_V1.0.md`
> - `IoT-Security-Case-Hub_Data-Model_V1.0.md`
> - `IoT-Security-Case-Hub_Database-Schema_V1.0.md`
> - `IoT-Security-Case-Hub_Backend-Architecture_V1.0.md`
> - `IoT-Security-Case-Hub_API-Design_V1.0.md`
> - `IoT-Security-Case-Hub_Frontend-Architecture_V1.0.md`
>
> 本文档冻结 V1 文件存储与文件安全设计，包括：
>
> - Evidence Storage
> - Test Case Attachment Storage
> - Tool Attachment Storage
> - Export Storage
> - StorageService
> - Storage Key
> - Filename Handling
> - Path Traversal Prevention
> - File Size
> - Upload / Download Authorization
> - SHA-256
> - Temporary File
> - Cleanup
> - Backup Relationship
> - Nginx / Spring Upload Limit
>
> V1 使用本地持久化存储，但必须通过抽象接口实现，以便未来平滑切换 NAS、MinIO 或 S3。

---

# 1. V1 存储总体方案

正式采用：

```text
PostgreSQL
→ 保存文件元数据

Local Persistent Storage
→ 保存实际文件内容
```

不采用：

```text
PostgreSQL BLOB
Base64 写数据库
把文件直接放应用源码目录
```

---

# 2. 为什么文件不进入 PostgreSQL

系统会保存：

```text
Screenshot
Log
PCAP
TXT
CSV
ZIP
BIN
Firmware
Photo
Video
Script
Other Evidence
```

这些文件可能较大。

如果直接保存数据库：

```text
数据库体积快速增长
Backup 变慢
Restore 变慢
查询与文件 IO 混在一起
维护困难
```

因此：

```text
DB = Metadata
Storage = File Bytes
```

---

# 3. StorageService

业务代码不得直接访问：

```text
/java.nio.file.Path
/data/...
```

统一通过：

```text
StorageService
```

接口。

推荐：

```java
interface StorageService {

    StoredObject save(StorageSaveCommand command);

    StorageResource read(String storageKey);

    void delete(String storageKey);

    boolean exists(String storageKey);
}
```

V1 实现：

```text
LocalStorageService
```

未来可以增加：

```text
NasStorageService
MinioStorageService
S3StorageService
```

---

# 4. Storage Root

Docker / Production：

```text
/data/casehub
```

推荐目录：

```text
/data/casehub/
├─ evidence/
├─ test-case-attachments/
├─ tool-attachments/
├─ exports/
└─ temp/
```

---

# 5. Evidence Storage Key

格式：

```text
evidence/{projectId}/{projectTestCaseId}/{uuid}
```

例如：

```text
evidence/
a1b2.../
c3d4.../
5f6e8f10-....bin
```

Storage Key：

```text
不包含原始文件名
```

---

# 6. Test Case Attachment Storage Key

格式：

```text
test-case-attachments/{testCaseVersionId}/{uuid}
```

---

# 7. Tool Attachment Storage Key

格式：

```text
tool-attachments/{toolId}/{uuid}
```

---

# 8. Export Storage

V1 Excel 为同步生成。

优先：

```text
直接 Streaming 给浏览器
```

如果需要临时文件：

```text
exports/{userId}/{uuid}.xlsx
```

下载完成后进入清理流程。

---

# 9. Original Filename

数据库保留：

```text
original_filename
```

只用于：

```text
UI 展示
Content-Disposition 下载文件名
```

不能用于：

```text
服务器路径
Storage Key
目录创建
```

---

# 10. Filename Sanitization

上传文件名必须处理：

```text
/
\
..
NULL byte
控制字符
过长文件名
```

保留展示名称时，也要去除危险字符。

示例：

```text
../../etc/passwd
```

不能参与任何文件路径计算。

---

# 11. Path Traversal Prevention

后端绝对禁止：

```text
root.resolve(clientFilename)
```

作为最终存储位置。

所有实际路径必须由：

```text
StorageService
+
内部生成 UUID Storage Key
```

构建。

解析路径后必须确认：

```text
resolvedPath.normalize()
startsWith(storageRoot)
```

---

# 12. Upload API 输入

客户端只允许提交：

```text
file
description
```

客户端不能提交：

```text
storageKey
serverPath
absolutePath
directory
```

---

# 13. MIME Type

客户端：

```text
Content-Type
```

只能作为参考。

不能认为：

```text
Content-Type = image/png
```

就一定是 PNG。

数据库可以保存：

```text
declaredContentType
```

但不作为安全判断的唯一依据。

---

# 14. V1 文件类型策略

考虑本系统属于安全测试平台，合法 Evidence 可能包括：

```text
PCAP
BIN
Firmware
ZIP
Script
Unknown binary
```

因此 V1 不采用严格扩展名白名单。

正式策略：

```text
允许通用文件上传
+
严格路径隔离
+
下载鉴权
+
禁止服务器直接执行
```

---

# 15. 文件执行

上传目录必须视为：

```text
Untrusted Data
```

禁止：

```text
chmod +x
shell execute
Runtime.exec(uploadedFile)
Python import
自动解压后执行
```

V1 所有上传文件：

```text
只存储 / 下载
```

---

# 16. Web Server Exposure

禁止把：

```text
/data/casehub
```

映射成 Nginx 静态目录。

例如禁止：

```text
https://server/evidence/xxx.pcap
```

直接访问。

所有文件下载必须经过：

```text
Spring Boot Authorization
```

---

# 17. Evidence Download Authorization

流程：

```text
GET /api/v1/evidence/{id}/download
↓
EvidenceService
↓
ResourceAuthorizationService
↓
Project Access Check
↓
StorageService.read()
↓
Stream
```

不能仅凭：

```text
知道 UUID
```

直接下载。

---

# 18. Attachment Download Authorization

Test Case Library Attachment：

```text
登录用户
+
拥有 Test Case Library Read 权限
```

即可下载。

Draft Attachment：

```text
必须具有 Draft 查看权限
```

---

# 19. Evidence Upload Authorization

必须满足：

```text
Current User ∈ ProjectTestCase Assignees
```

或者：

```text
Admin / Coordinator 有对应管理权限
```

具体最终权限以 RBAC Detail 文档为准。

---

# 20. Evidence Delete Authorization

已经冻结：

```text
所有当前 Assignee
都可以删除该共享 Project Test Case 的任意 Evidence
```

Coordinator / Admin 也可依据管理权限执行。

---

# 21. Project Test Case Removed

如果：

```text
removed = true
```

默认禁止新增 Evidence。

但已有：

```text
Evidence
```

仍保留并可查看。

Restore 后恢复正常上传。

---

# 22. File Size Limit

V1 推荐默认：

```text
单文件最大 500 MB
```

理由：

安全测试 Evidence 可能存在：

```text
PCAP
Firmware
Large Logs
Video
```

100 MB 可能过小。

---

# 23. 可配置 File Size

不要把：

```text
500 MB
```

写死在 Java 代码。

配置：

```yaml
casehub:
  storage:
    max-file-size: 500MB
```

同时配置：

```text
Spring Multipart
Nginx client_max_body_size
```

---

# 24. Nginx Upload Limit

推荐：

```nginx
client_max_body_size 500m;
```

必须与 Spring Boot 保持一致。

---

# 25. Spring Multipart Limit

例如：

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 500MB
      max-request-size: 520MB
```

留少量 multipart overhead。

---

# 26. Project Total Storage Limit

V1 暂不强制项目总容量配额。

但系统应保留监控能力。

后续可增加：

```text
Project Storage Quota
User Storage Quota
```

---

# 27. Disk Free Space

StorageService 在上传前应检查：

```text
Available Disk Space
```

至少避免磁盘已经接近 100% 时继续写入。

推荐阈值：

```text
剩余空间 < 5 GB
或
剩余 < 5%
```

时：

```text
拒绝大文件上传
+
记录 ERROR
```

具体部署阈值可配置。

---

# 28. SHA-256

每个上传文件建议计算：

```text
SHA-256
```

数据库保存：

```text
sha256
```

用途：

```text
Evidence Integrity
文件重复排查
Backup 验证
用户核对
```

不用于：

```text
身份认证
数字签名
```

---

# 29. SHA-256 Streaming

计算 Hash 时：

```text
流式计算
```

不能为了 SHA-256：

```text
整个 500MB 文件一次读进内存
```

---

# 30. Upload Processing

推荐流程：

```text
1. Authorization
2. Validate Size
3. Sanitize Display Filename
4. Generate Storage Key
5. Save to Temp
6. Stream SHA-256
7. Verify Final Size
8. Move Temp → Final
9. Insert DB Metadata
10. Return Evidence DTO
```

---

# 31. 文件与 DB 一致性

文件系统和 PostgreSQL 不共享 ACID Transaction。

因此必须显式处理失败。

---

# 32. Upload Failure A

如果：

```text
文件写入失败
```

则：

```text
不创建 DB Evidence Row
```

---

# 33. Upload Failure B

如果：

```text
文件已写成功
DB Insert 失败
```

则：

```text
立即删除刚刚保存的文件
```

---

# 34. Upload Failure C

如果：

```text
删除补偿失败
```

记录：

```text
Orphan Storage Error
```

并交给：

```text
Storage Cleanup Job
```

后续清理。

---

# 35. Temp Storage

所有上传先进入：

```text
/data/casehub/temp/
```

Temp Filename：

```text
UUID
```

不使用原文件名。

---

# 36. Atomic Move

Temp 和 Final Storage 尽量位于：

```text
同一文件系统
```

然后使用：

```text
Files.move(...)
```

完成原子/近原子 rename。

---

# 37. Partial Upload

请求中断时：

```text
Temp File
```

可能残留。

需要定期清理。

---

# 38. Temp Cleanup

建议：

```text
每 24 小时
```

清理：

```text
超过 24h
且未关联 DB
```

的临时文件。

V1 可使用：

```text
Spring @Scheduled
```

不需要消息队列。

---

# 39. Orphan File Cleanup

不能仅根据年龄直接删除正式 Storage。

建议提供管理员维护任务：

```text
Storage Integrity Check
```

检查：

```text
DB → File missing
File → DB missing
```

V1 可以先实现：

```text
只记录报告
```

不自动删除正式 Orphan。

---

# 40. Missing File

数据库有 Evidence：

```text
storage_key = X
```

但文件不存在。

下载时：

```text
404/422
```

错误码：

```text
STORAGE_OBJECT_MISSING
```

同时记录：

```text
ERROR
```

---

# 41. Delete Flow

Evidence Delete：

```text
1. Authorization
2. Load Evidence
3. Storage Delete
4. DB Delete
```

---

# 42. Delete Failure

如果 Storage Delete 失败：

```text
默认不删除 DB Metadata
```

返回：

```text
STORAGE_DELETE_FAILED
```

避免数据库显示已经删除但文件仍然存在。

---

# 43. Note 与文件无关

Note 只存数据库：

```text
TEXT
```

不进入 StorageService。

---

# 44. Attachment

Test Case Attachment 和 Tool Attachment：

```text
同样使用 StorageService
```

但使用不同：

```text
Storage Namespace
```

---

# 45. Storage Namespace

建议内部 Enum：

```text
EVIDENCE
TEST_CASE_ATTACHMENT
TOOL_ATTACHMENT
EXPORT
TEMP
```

---

# 46. StorageKey Object

建议不要在业务代码传裸字符串。

可定义：

```text
StorageKey
```

Value Object。

负责：

```text
namespace
resource IDs
uuid
```

---

# 47. File Metadata

Evidence 表：

```text
original_filename
storage_key
file_size
content_type
sha256
description
uploaded_by
created_at
```

---

# 48. Content-Disposition

下载响应：

```http
Content-Disposition: attachment; filename*=UTF-8''...
```

必须正确处理：

```text
中文文件名
特殊字符
```

---

# 49. Browser Inline Preview

V1 默认：

```text
attachment download
```

不自动 inline 打开：

```text
HTML
SVG
Script
```

降低浏览器执行恶意内容风险。

---

# 50. Dangerous Browser Content

以下即使上传成功：

```text
HTML
SVG
JS
```

默认下载时：

```text
Content-Disposition: attachment
```

不 inline。

---

# 51. X-Content-Type-Options

Nginx / Backend 推荐：

```http
X-Content-Type-Options: nosniff
```

---

# 52. Download Cache

Evidence 默认：

```text
Cache-Control: private, no-store
```

避免共享设备/代理长期缓存敏感测试文件。

---

# 53. Filename Length

建议原始文件名：

```text
最多 255 字符
```

超出：

```text
截断展示名称
```

但保留合理扩展名。

---

# 54. Description Length

Evidence Description：

推荐：

```text
最大 2000 字符
```

具体在 API / DB 字段可使用 TEXT + Service 限制。

---

# 55. File Count

V1 不限制单 ProjectTestCase Evidence 数量。

但 UI 使用：

```text
分页 / 懒加载
```

的能力可以后续加入。

---

# 56. ZIP

ZIP 文件：

```text
允许上传
```

但 V1：

```text
不自动解压
```

避免：

```text
Zip Slip
Zip Bomb
恶意嵌套
```

---

# 57. Firmware / BIN

允许：

```text
.bin
.img
.fw
```

仅作为 Evidence / Attachment 保存。

V1 不自动解析。

---

# 58. Scripts

允许：

```text
.py
.sh
.ps1
```

作为 Evidence / Attachment。

但：

```text
只允许下载
不允许服务器执行
```

---

# 59. Antivirus

V1 不强制内置 Antivirus。

因为内网安全测试材料可能：

```text
包含 exploit sample
malformed file
security test artifacts
```

普通 AV 可能产生大量误报。

但 StorageService 架构应允许未来插入：

```text
FileInspectionService
```

---

# 60. Future FileInspectionService

未来可扩展：

```text
SHA-256
MIME Detection
AV
YARA
Metadata
```

V1 不实现。

---

# 61. Storage Encryption

V1 不在应用层自行加密每个文件。

推荐依赖部署环境：

```text
磁盘加密
NAS 加密
主机访问控制
备份加密
```

应用层自研 AES 文件加密会增加：

```text
密钥管理
恢复复杂度
性能成本
```

当前无必要。

---

# 62. HTTPS

即使部署内网：

```text
生产环境仍建议 HTTPS
```

特别是：

```text
登录凭据
Evidence Download
```

---

# 63. Local File Permission

Linux：

Storage Root：

```text
仅 Backend Service User
```

需要读写权限。

例如：

```text
750 / 700
```

不要：

```text
777
```

---

# 64. Container User

Spring Boot Container：

```text
非 root 用户运行
```

并只挂载：

```text
必要 Storage Volume
```

---

# 65. Frontend Container

Frontend / Nginx：

```text
不挂载 Evidence Storage
```

避免静态服务器直接接触文件目录。

---

# 66. Postgres Container

Postgres：

```text
不挂载 Evidence Storage
```

Database 与 Evidence Volume 分离。

---

# 67. Docker Volumes

推荐：

```text
casehub-postgres-data
casehub-file-storage
```

---

# 68. Storage Layout Example

```text
/data/casehub/

evidence/
  56b...project/
    47a...ptc/
      f5e....bin
      a83....pcap

test-case-attachments/
  87a...version/
    e32....pdf

tool-attachments/
  09c...tool/
    761....zip

temp/
```

---

# 69. Backup

必须一起备份：

```text
PostgreSQL
+
/data/casehub
```

只备份其中一个：

```text
无法完整恢复系统
```

---

# 70. Backup Consistency

理想方案：

```text
短暂进入 Backup Mode
或
使用一致性时间点
```

至少确保：

```text
数据库 Backup
文件 Backup
```

时间窗口尽量接近。

更详细策略放：

```text
Deployment & Backup V1.0
```

---

# 71. Restore Validation

恢复后至少验证：

```text
Evidence DB Row → File Exists
Attachment DB Row → File Exists
SHA-256 matches
```

---

# 72. Storage Monitoring

生产环境监控：

```text
Total Disk
Used Disk
Free Disk
File Count
Upload Error
Storage Error
```

---

# 73. Storage Log

记录：

```text
Upload Failed
Delete Failed
Missing File
Disk Low
Cleanup Error
```

不要把：

```text
文件内容
```

写日志。

---

# 74. Audit

Evidence 上传不要求完整 AuditLog。

数据库本身已经保存：

```text
uploadedBy
createdAt
```

Evidence Delete 可根据未来合规需要增加审计。

V1 可记录轻量 Audit：

```text
EVIDENCE_DELETE
```

推荐保留。

---

# 75. Attachment Version Behavior

Test Case Version Published 后：

```text
附件也视为 Published Version 内容的一部分
```

不得直接修改附件集合。

如需更新：

```text
Create Revision
```

---

# 76. Draft Attachment

DRAFT：

```text
允许新增
删除
替换
```

---

# 77. Tool Attachment

Tool Attachment 不跟 Test Case Version 生命周期。

由 Tool 管理权限控制。

---

# 78. Evidence 生命周期

Evidence 与：

```text
ProjectTestCase
```

绑定。

Project Archived：

```text
Evidence 继续保留
```

ProjectTestCase Removed：

```text
Evidence 继续保留
```

---

# 79. Project 删除

由于 Project 采用：

```text
Archive / Soft Delete
```

不会因为用户删除项目而立即清除 Evidence。

---

# 80. Retention

V1 不自动过期删除 Evidence。

默认：

```text
长期保留
```

直到管理员明确制定未来 Retention Policy。

---

# 81. Export 文件生命周期

同步导出优先：

```text
不落盘
```

如果落盘：

```text
临时文件
```

推荐：

```text
24h 后清理
```

---

# 82. Storage API 边界

业务模块只知道：

```text
Storage Key
```

不知道：

```text
实际 Linux 路径
```

---

# 83. LocalStorageService Boundary

只有：

```text
LocalStorageService
```

知道：

```text
/data/casehub
```

以后切换 MinIO：

```text
EvidenceService 无需改变
```

---

# 84. Configuration

推荐：

```yaml
casehub:
  storage:
    type: local
    root: /data/casehub
    max-file-size: 500MB
    min-free-space: 5GB
    temp-retention: 24h
```

---

# 85. Production Configuration

Root Path：

```text
通过环境变量覆盖
```

例如：

```text
CASEHUB_STORAGE_ROOT=/data/casehub
```

---

# 86. Dev Environment

开发环境：

```text
./.local-data/casehub
```

应加入：

```text
.gitignore
```

绝不能提交测试 Evidence 到 Git。

---

# 87. Test Environment

集成测试使用：

```text
TemporaryDirectory
```

每次测试：

```text
自动创建
自动清理
```

---

# 88. StorageService Unit Test

必须覆盖：

```text
正常 Save
Read
Delete
Exists
Path Traversal
Filename
Large Stream
Missing File
Duplicate StorageKey
Temp Cleanup
```

---

# 89. EvidenceService Integration Test

必须覆盖：

```text
Upload + DB
DB failure compensation
Download Permission
Delete Permission
Removed Case upload rejected
SHA-256
```

---

# 90. Security Test

重点：

```text
../ filename
absolute path
unicode filename
null byte
very long filename
HTML attachment
SVG attachment
unauthorized UUID download
deleted Evidence
missing Storage Object
```

---

# 91. V1 不实现

文件系统 V1 不实现：

```text
S3
MinIO
NAS Protocol Client
Chunk Upload
Multipart Resume
Deduplication Storage
Automatic ZIP Extraction
Automatic PCAP Parse
Automatic Firmware Parse
Automatic AV Scan
Automatic YARA Scan
Application-level File Encryption
Per-project Storage Quota
```

---

# 92. Future NAS

如果部署服务器本身挂载：

```text
/mnt/casehub-nas
```

只需要把：

```text
CASEHUB_STORAGE_ROOT
```

指向 NAS Mount。

业务代码仍然使用：

```text
LocalStorageService
```

---

# 93. Future MinIO / S3

以后增加：

```text
ObjectStorageService
```

实现相同接口：

```text
save
read
delete
exists
```

数据库：

```text
storage_key
```

仍可继续使用。

---

# 94. File Storage & Security V1.0 最终冻结

正式 V1：

```text
Metadata
→ PostgreSQL

File Bytes
→ Local Persistent Storage
```

业务：

```text
EvidenceService
↓
StorageService
↓
LocalStorageService
```

核心安全：

```text
UUID Storage Key
原始文件名不参与路径
Path Normalize
Root Boundary Check
Download Authorization
No Static Exposure
No Server Execution
SHA-256
Temp + Final Move
Failure Compensation
Non-root Container
500MB Configurable Limit
Backup DB + Files
```

上传内容作为：

```text
Untrusted Data
```

处理。

这套方案作为 V1 文件存储实现唯一基准。

---

# 95. 下一阶段

下一份文档：

```text
IoT-Security-Case-Hub_Security-RBAC-Detail_V1.0.md
```

将冻结：

```text
Authentication
Session / Cookie
CSRF
Password Policy
Role
Permission
Resource Authorization
Admin
Coordinator
Tester
Project Permission
Assignee Permission
Draft Contributor
Evidence Permission
Security Headers
Rate Limiting
Login Failure
CORS
Audit
```
