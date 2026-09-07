import type { Category } from '@/shared/types/dictionary';

/**
 * 编码规则：非数字前缀 + 数字序号（零填充）。
 * 例：CAT-001 / CAT-002 ... 取全列表（含二级子类）中序号最大者，序号 +1 作为下一个编码。
 * 没有任何编码时回退到 CAT-001。
 */
const CODE_PATTERN = /^([A-Za-z0-9-]*?)(\d+)$/;

/** 把两级树拍平成扁平数组，便于在全量范围内取最大序号。 */
function flattenCategories(categories: Category[]): Category[] {
  const result: Category[] = [];
  for (const cat of categories) {
    result.push(cat);
    if (cat.children?.length) {
      result.push(...flattenCategories(cat.children));
    }
  }
  return result;
}

export function computeNextCategoryCode(categories: Category[]): string {
  const all = flattenCategories(categories ?? []);
  let maxNum = 0;
  let prefix = 'CAT-';
  let width = 3;

  for (const cat of all) {
    const match = CODE_PATTERN.exec(cat.code.trim());
    if (!match) continue;
    const num = Number.parseInt(match[2], 10);
    if (num > maxNum) {
      maxNum = num;
      prefix = match[1];
      width = match[2].length;
    }
  }

  const next = String(maxNum + 1).padStart(width, '0');
  return `${prefix}${next}`;
}
