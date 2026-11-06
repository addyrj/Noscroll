import { useFocusEffect } from '@react-navigation/native';
import { useCallback, useRef, useState } from 'react';
import { AppState } from 'react-native';

/** Calls `load` immediately and every `intervalMs` while the screen is focused and the app is active. */
export function usePolling<T>(load: () => Promise<T>, intervalMs: number) {
  const [data, setData] = useState<T | null>(null);
  const loadRef = useRef(load);
  loadRef.current = load;

  useFocusEffect(
    useCallback(() => {
      let alive = true;
      const run = () => {
        if (AppState.currentState !== 'active') return;
        loadRef.current().then(
          d => alive && setData(d),
          () => undefined,
        );
      };
      run();
      const id = setInterval(run, intervalMs);
      const sub = AppState.addEventListener('change', s => s === 'active' && run());
      return () => {
        alive = false;
        clearInterval(id);
        sub.remove();
      };
    }, [intervalMs]),
  );

  return data;
}
