import { Button, Checkbox, Popover, theme } from 'antd';
import type { ReactNode } from 'react';
import { SettingOutlined } from '@ant-design/icons';

export interface ColumnDef {
  key: string;
  title: ReactNode;
  /** false = column cannot be hidden (e.g. action column). Default true. */
  lockable?: boolean;
}

export interface ColumnSettingsPopoverProps {
  columns: ColumnDef[];
  hiddenKeys: string[];
  onChange: (hiddenKeys: string[]) => void;
}

/** Column visibility control for data tables; persisted by the caller. */
export function ColumnSettingsPopover({ columns, hiddenKeys, onChange }: ColumnSettingsPopoverProps) {
  const { token } = theme.useToken();
  const optional = columns.filter((column) => column.lockable !== false);
  const checkedList = optional
    .filter((column) => !hiddenKeys.includes(column.key))
    .map((column) => column.key);

  return (
    <Popover
      trigger="click"
      placement="bottomRight"
      title="显示列"
      content={
        <Checkbox.Group
          value={checkedList}
          onChange={(values) => {
            const checked = values as string[];
            onChange(optional.filter((column) => !checked.includes(column.key)).map((column) => column.key));
          }}
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: token.paddingXS, minWidth: 150 }}>
            {optional.map((column) => (
              <Checkbox key={column.key} value={column.key} style={{ marginInlineStart: 0 }}>
                {column.title}
              </Checkbox>
            ))}
          </div>
        </Checkbox.Group>
      }
    >
      <Button icon={<SettingOutlined />}>列设置</Button>
    </Popover>
  );
}

export default ColumnSettingsPopover;
