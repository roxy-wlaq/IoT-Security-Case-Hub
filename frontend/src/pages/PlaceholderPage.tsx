import { Empty } from 'antd';
import type { NavigationItem } from '@/shared/config/navigation';

export function PlaceholderPage({ item }: { item: NavigationItem }) {
  return (
    <div style={{ padding: '24px 0' }}>
      <Empty
        description={
          <div>
            <div style={{ fontSize: 16, fontWeight: 600 }}>{item.label}</div>
            <div style={{ color: 'rgba(0, 0, 0, 0.45)', marginTop: 8 }}>
              {item.description}
            </div>
            <div style={{ color: 'rgba(0, 0, 0, 0.25)', marginTop: 4, fontSize: 12 }}>
              计划实现阶段：{item.plannedPhase}
            </div>
          </div>
        }
      />
    </div>
  );
}

export default PlaceholderPage;
