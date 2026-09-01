/**
 * Phase 4 基础数据字典类型。
 *
 * 与后端契约对应（Lead 冻结）：
 *   /api/v1/standards   标准任务类型 / 任务类型
 *   /api/v1/categories  二级分类树
 *   /api/v1/tags        标签
 *   /api/v1/tools       工具
 */

/** 标准任务类型 / 任务类型 */
export type StandardType = 'STANDARD' | 'TASK_TYPE';

export const STANDARD_TYPES: readonly StandardType[] = ['STANDARD', 'TASK_TYPE'];

export interface StandardTaskType {
  id: string;
  code: string;
  name: string;
  type: StandardType;
  description?: string;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

/** 分类（二级树） */
export type CategoryLevel = 1 | 2;

export interface Category {
  id: string;
  parentId?: string | null;
  code: string;
  name: string;
  level: CategoryLevel;
  description?: string;
  sortOrder: number;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
  /** level=1 时可能挂载的二级子分类 */
  children?: Category[];
}

/** 标签 */
export interface Tag {
  id: string;
  name: string;
  description?: string;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

/** 工具 */
export interface Tool {
  id: string;
  name: string;
  description?: string;
  platform?: string;
  website?: string;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

/** 列表查询通用参数 */
export interface DictionaryListParams {
  search?: string;
  enabled?: boolean;
}

/** 标准任务类型列表额外支持按 type 过滤 */
export interface StandardListParams extends DictionaryListParams {
  type?: StandardType;
}
