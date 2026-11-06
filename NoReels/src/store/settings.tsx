import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';
import { Settings, NoReels } from '../modules/NoReelsModule';

// Settings live in the Kotlin ledger (not MMKV) so the blocking engine and the UI
// can never disagree, and limits are clamped where they are enforced.

type SettingsContextValue = {
  settings: Settings | null;
  save: (next: Settings) => Promise<Settings>;
  reload: () => Promise<void>;
};

const SettingsContext = createContext<SettingsContextValue | null>(null);

export function SettingsProvider({ children }: { children: React.ReactNode }) {
  const [settings, setSettings] = useState<Settings | null>(null);

  const reload = useCallback(async () => {
    setSettings(await NoReels.getSettings());
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  const save = useCallback(async (next: Settings) => {
    const saved = await NoReels.saveSettings(next);
    setSettings(saved);
    return saved;
  }, []);

  const value = useMemo(() => ({ settings, save, reload }), [settings, save, reload]);

  return <SettingsContext.Provider value={value}>{children}</SettingsContext.Provider>;
}

export function useSettings(): SettingsContextValue {
  const ctx = useContext(SettingsContext);
  if (!ctx) throw new Error('useSettings must be used inside SettingsProvider');
  return ctx;
}
