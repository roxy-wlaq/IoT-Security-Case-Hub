/**
 * Phase 5 能力库（Capability Library）类型。
 *
 * 与后端契约对应（Lead 冻结）：
 *   GET  /api/v1/capabilities/tree
 *   POST /api/v1/capabilities
 *   PUT  /api/v1/capabilities/{capabilityId}
 *   POST /api/v1/capabilities/{capabilityId}/enable
 *   POST /api/v1/capabilities/{capabilityId}/disable
 *
 * 语义边界：Capability 回答"设备有什么能力"，是全局能力定义树。
 * 它不承载 YES / NO / UNKNOWN 的安全结论——那是后续 Project Capability 的事，
 * 因此这里不存在任何取值枚举。
 * Capability Tree 与 Category Tree 是两棵独立的树，不得合并。
 */

/** 能力定义。enabled 仅表示该能力定义是否仍可选，不代表任何项目上的安全结论。 */
export interface Capability {
  id: string;
  /** null 表示 Root Capability */
  parentId: string | null;
  code: string;
  name: string;
  description?: string | null;
  sortOrder: number;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

/** 树节点。children 永不为 null：叶子节点是空数组。深度不限（Bluetooth → BLE → GATT）。 */
export interface CapabilityTreeNode extends Capability {
  children: CapabilityTreeNode[];
}

export interface CapabilityCreatePayload {
  /** 省略或 null 表示创建 Root Capability */
  parentId?: string | null;
  code: string;
  name: string;
  description?: string | null;
  sortOrder?: number;
}

/** PUT 为整体替换：不传 parentId 即把该能力移回根节点。 */
export interface CapabilityUpdatePayload {
  parentId?: string | null;
  code: string;
  name: string;
  description?: string | null;
  sortOrder?: number;
}
