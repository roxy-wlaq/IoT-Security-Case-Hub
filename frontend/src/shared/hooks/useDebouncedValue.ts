import { useEffect, useState } from 'react';

/**
 * 返回 debounced 之后的值，用于搜索框输入节流。
 *
 * @param value 原始值
 * @param delay 延迟毫秒，默认 300ms
 */
export function useDebouncedValue<T>(value: T, delay = 300): T {
  const [debounced, setDebounced] = useState<T>(value);

  useEffect(() => {
    const timer = window.setTimeout(() => setDebounced(value), delay);
    return () => window.clearTimeout(timer);
  }, [value, delay]);

  return debounced;
}
