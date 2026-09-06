import { Segmented } from 'antd';

export type TableDensity = 'large' | 'middle' | 'small';

const OPTIONS: { label: string; value: TableDensity }[] = [
  { label: '宽松', value: 'large' },
  { label: '中等', value: 'middle' },
  { label: '紧凑', value: 'small' },
];

export interface DensitySwitchProps {
  value: TableDensity;
  onChange: (value: TableDensity) => void;
}

/** Table row-density switch (antd Table `size`), persisted by the caller. */
export function DensitySwitch({ value, onChange }: DensitySwitchProps) {
  return (
    <Segmented
      size="small"
      options={OPTIONS}
      value={value}
      onChange={(next) => onChange(next as TableDensity)}
    />
  );
}

export default DensitySwitch;
