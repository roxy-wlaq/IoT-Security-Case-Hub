import { createContext, useContext } from 'react';
import type { ThemeMode } from '@/app/theme';

export interface ThemeModeValue {
  mode: ThemeMode;
  setMode: (mode: ThemeMode) => void;
  toggle: () => void;
}

export const ThemeModeContext = createContext<ThemeModeValue | null>(null);

export function useThemeMode(): ThemeModeValue {
  const ctx = useContext(ThemeModeContext);
  if (!ctx) {
    throw new Error('useThemeMode must be used within AppProviders');
  }
  return ctx;
}
