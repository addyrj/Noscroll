import { useColorScheme } from 'react-native';

// All app colors live here. The native block overlay mirrors the dark palette
// (android/.../blocking/BlockingOverlay.kt).
export type Palette = {
  background: string;
  surface: string;
  border: string;
  primary: string;
  success: string;
  warning: string;
  text: string;
  textSecondary: string;
  onPrimary: string;
};

export const dark: Palette = {
  background: '#0A0A0A',
  surface: '#111111',
  border: '#1E1E1E',
  primary: '#FF3B5C',
  success: '#00E676',
  warning: '#FFB300',
  text: '#FFFFFF',
  textSecondary: '#888888',
  onPrimary: '#FFFFFF',
};

export const light: Palette = {
  background: '#F5F5F5',
  surface: '#FFFFFF',
  border: '#E0E0E0',
  primary: '#FF3B5C',
  success: '#00C853',
  warning: '#FF8F00',
  text: '#0A0A0A',
  textSecondary: '#555555',
  onPrimary: '#FFFFFF',
};

export function useColors(): Palette {
  return useColorScheme() === 'light' ? light : dark;
}

export function useIsDark(): boolean {
  return useColorScheme() !== 'light';
}
