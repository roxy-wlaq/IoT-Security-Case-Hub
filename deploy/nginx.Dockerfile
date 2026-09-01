# =============================================================================
# CaseHub Nginx + React Frontend Production Image
#
# Build context = repository root:
#   docker build -f deploy/nginx.Dockerfile .
# 与 deploy/docker-compose.yml 中 `context: ..` 保持一致。
#
# 不运行独立 frontend 应用服务器：
#   frontend/ --(Vite build)--> frontend/dist --(COPY)--> nginx image
# （Final Technical Review 第 4 节 / Deployment 文档第 3-4 节）
#
# 期望的仓库结构（由 Agent B 交付）：
#   frontend/package.json
#   frontend/package-lock.json     <- 当前 Docker 构建假定使用 npm
#   frontend/...
#   frontend/dist                  <- `npm run build` 产出（Vite 默认）
#
# 如果 frontend 实际使用 pnpm / yarn，需要调整本文件（见 OPEN_ISSUES）。
# =============================================================================

# -----------------------------------------------------------------------------
# Stage 1: build frontend
# -----------------------------------------------------------------------------
FROM node:22-alpine AS frontend-build

WORKDIR /build/frontend

# 先只复制依赖声明，最大化 Docker 层缓存
COPY frontend/package.json frontend/package-lock.json* ./
RUN npm ci

COPY frontend/ ./
RUN npm run build

# 产出：/build/frontend/dist

# -----------------------------------------------------------------------------
# Stage 2: nginx runtime
# -----------------------------------------------------------------------------
FROM nginx:1.27-alpine

# 删除默认站点，避免暴露 nginx 欢迎页
RUN rm -f /etc/nginx/conf.d/default.conf

COPY deploy/nginx/nginx.conf /etc/nginx/nginx.conf
COPY deploy/nginx/conf.d/casehub.conf /etc/nginx/conf.d/casehub.conf

# 静态 SPA 产物
COPY --from=frontend-build /build/frontend/dist /usr/share/nginx/html

# nginx master 以 root 启动（监听 80 需要 root），worker 以 nginx 用户运行。
# 确保缓存 / 日志目录对 nginx 用户可写。
RUN mkdir -p /var/cache/nginx /var/log/nginx \
    && chown -R nginx:nginx /var/cache/nginx /var/log/nginx /usr/share/nginx/html \
    && touch /var/run/nginx.pid \
    && chown nginx:nginx /var/run/nginx.pid

EXPOSE 80

# 健康检查：/healthz 由 deploy/nginx/conf.d/casehub.conf 提供，返回 200
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=5 \
    CMD wget -qO- http://localhost/healthz || exit 1

CMD ["nginx", "-g", "daemon off;"]
