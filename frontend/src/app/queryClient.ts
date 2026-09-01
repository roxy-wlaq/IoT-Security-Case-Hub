import { QueryClient } from '@tanstack/react-query';

/**
 * 全局 TanStack Query 客户端。
 *
 * - 查询默认重试 1 次；401 由 httpClient 拦截器统一处理（清空 /me 缓存并引导登录）
 * - 写操作（mutation）不重试，避免重复提交
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
      staleTime: 30_000,
    },
    mutations: {
      retry: 0,
    },
  },
});
