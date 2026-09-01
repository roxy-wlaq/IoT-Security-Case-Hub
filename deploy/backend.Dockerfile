# =============================================================================
# CaseHub Backend Production Image
#
# Build context = repository root:
#   docker build -f deploy/backend.Dockerfile .
# 与 deploy/docker-compose.yml 中 `context: ..` 保持一致。
#
# 期望的仓库结构（由 Agent A 交付）：
#   backend/pom.xml
#   backend/src/...
#
# 说明：
#   - 使用 Maven 官方镜像自带的 `mvn`，不依赖仓库内 Maven Wrapper。
#   - 先只复制 pom.xml 解析依赖，最大化 Docker 层缓存；再复制 src 打包。
#   - 若 `dependency:go-offline` 因某些插件无法离线解析而失败，
#     可直接删除该 RUN 步骤，只保留 `mvn package -DskipTests`（缓存效果变差但可用）。
# =============================================================================

# -----------------------------------------------------------------------------
# Stage 1: build
# -----------------------------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /build

# 先只复制 pom.xml，解析依赖（此层源码不变时不重建）
COPY backend/pom.xml ./pom.xml
RUN mvn -B -ntp dependency:go-offline

# 再复制源码并打包（跳过测试，测试由 CI 单独执行）
COPY backend/src ./src
RUN mvn -B -ntp clean package -DskipTests \
    && cp target/*.jar /build/app.jar

# -----------------------------------------------------------------------------
# Stage 2: runtime
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine

# 非 root 运行（Deployment 文档第 22 节）
RUN addgroup -S app && adduser -S app -G app

# Backend 唯一需要写入的目录（Deployment 文档第 23 节 / File Storage 文档第 4 节）
# Final Technical Review 第 27 节：删除流程依赖 /data/casehub/trash/
# 这里预先创建并修正属主，named volume 首次挂载时会继承该属主与权限。
RUN mkdir -p /data/casehub/trash /data/casehub/temp \
    && chown -R app:app /data/casehub

WORKDIR /app

COPY --from=build --chown=app:app /build/app.jar /app/app.jar

USER app

# 声明数据卷挂载点（compose 中挂载 file-storage）
VOLUME ["/data/casehub"]

# JVM 内存基线（Deployment 文档第 24 节），可通过 compose 的 JAVA_OPTS 覆盖。
# Java 21 原生感知容器内存限制，这里只给保守的初始堆设置。
ENV JAVA_OPTS="-Xms512m -Xmx2g"

EXPOSE 8080

# 健康检查：alpine 自带 busybox wget（Deployment 文档第 26/31 节）
HEALTHCHECK --interval=30s --timeout=5s --start-period=180s --retries=5 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# 使用 shell 形式以展开 JAVA_OPTS；exec 保证 java 为 PID 1，信号可正常传递。
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
