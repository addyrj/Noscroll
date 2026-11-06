import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

// Codegen spec for the Kotlin bridge (android/.../bridge/NoReelsModule.kt).
// Rich payloads travel as JSON strings; src/modules/NoReelsModule.ts types them.
export interface Spec extends TurboModule {
  getStatus(): Promise<string>;
  getSettings(): Promise<string>;
  saveSettings(settingsJson: string): Promise<string>;
  getWeek(): Promise<string>;
  getPermissions(): Promise<string>;
  openPermissionSettings(kind: string): void;
  startFocus(minutes: number): Promise<number>;
  getEventLog(): Promise<string>;
  getInstalledApps(): Promise<string>;
  getLifetimeStats(): Promise<string>;
  getAppUsageToday(): Promise<string>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('NoReels');
