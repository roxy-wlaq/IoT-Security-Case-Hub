import { useCallback, useState } from 'react';

export type TableDensity = 'large' | 'middle' | 'small';

export interface TablePreferences {
  density: TableDensity;
  hiddenColumnKeys: string[];
}

const DEFAULTS: TablePreferences = { density: 'large', hiddenColumnKeys: [] };

function read(storageKey: string): TablePreferences {
  if (typeof window === 'undefined') {
    return DEFAULTS;
  }
  try {
    const raw = window.localStorage.getItem(storageKey);
    if (!raw) {
      return DEFAULTS;
    }
    const parsed = JSON.parse(raw) as Partial<TablePreferences>;
    return {
      density:
        parsed.density === 'middle' || parsed.density === 'small' ? parsed.density : 'large',
      hiddenColumnKeys: Array.isArray(parsed.hiddenColumnKeys) ? parsed.hiddenColumnKeys : [],
    };
  } catch {
    return DEFAULTS;
  }
}

/**
 * Per-table UI preferences (density + hidden columns), persisted to
 * localStorage under the given key so layouts survive reloads.
 */
export function useTablePreferences(storageKey: string) {
  const [prefs, setPrefs] = useState<TablePreferences>(() => read(storageKey));

  const update = useCallback(
    (next: TablePreferences) => {
      setPrefs(next);
      try {
        window.localStorage.setItem(storageKey, JSON.stringify(next));
      } catch {
        /* ignore storage errors */
      }
    },
    [storageKey],
  );

  const setDensity = useCallback(
    (density: TableDensity) => update({ ...prefs, density }),
    [prefs, update],
  );
  const setHiddenColumnKeys = useCallback(
    (hiddenColumnKeys: string[]) => update({ ...prefs, hiddenColumnKeys }),
    [prefs, update],
  );
  const reset = useCallback(() => update(DEFAULTS), [update]);

  return { prefs, setDensity, setHiddenColumnKeys, reset };
}
