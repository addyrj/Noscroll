import Native from '../specs/NativeNoReels';

export type AppKey = 'youtube' | 'instagram' | 'facebook' | 'tiktok' | 'snapchat';
export type BlockReason = 'COOLDOWN' | 'DAILY' | 'APP_LIMIT' | 'NIGHT' | 'FOCUS';

export type Settings = {
  sessionLimitMin: number;
  dailyLimitMin: number;
  cooldownMin: number;
  strictMode: boolean;
  nightSchedule: boolean;
  showQuote: boolean;
  quoteText: string;
  apps: Record<AppKey, boolean>;
  customPackages: string[];
  pauseEnabled: boolean;
  pauseDelaySec: number;
  curiousMode: boolean;
  curiousTimeSec: number;
  reinterventionEnabled: boolean;
  reinterventionMin: number;
  nightStartHour: number;
  nightEndHour: number;
  appTimeLimits: Record<string, number>;
};

export type Status = {
  serviceRunning: boolean;
  dailyUsedSec: number;
  dailyLimitSec: number;
  sessionActive: boolean;
  sessionInFeed: boolean;
  sessionElapsedSec: number;
  sessionLimitSec: number;
  cooldownRemainingSec: number;
  focusRemainingSec: number;
  sessionsToday: number;
  blockedToday: number;
  nightActive: boolean;
  block: BlockReason | null;
  streak: number;
};

export type DayUsage = {
  date: string;
  usedSec: number;
  limitSec: number;
  blocked: number;
  sessions: number;
  tracked: boolean;
  isToday: boolean;
  isFuture: boolean;
};

export type Week = { days: DayUsage[]; streak: number };

export type Permissions = {
  accessibility: boolean;
  serviceRunning: boolean;
  notifications: boolean;
  battery: boolean;
  usage: boolean;
};

export type PermissionKind =
  | 'accessibility'
  | 'notifications'
  | 'battery'
  | 'usage'
  | 'appInfo';

export type LogEntry = {
  ts: number;
  kind: 'uncertain' | 'strict_dismiss';
  app: string;
  detail: string;
};

export type InstalledApp = {
  pkg: string;
  label: string;
  social: boolean;
};

export type Achievement = {
  id: string;
  title: string;
  description: string;
};

export type LifetimeStats = {
  totalDays: number;
  daysUnderLimit: number;
  totalBlocked: number;
  totalSessions: number;
  totalUsedMin: number;
  bestStreak: number;
  currentStreak: number;
  points: number;
  level: number;
  achievements: Achievement[];
};

// Typed wrapper around the Kotlin module. Kotlin owns the ledger and settings;
// the UI reads them and can only request changes, which Kotlin validates.
export const NoReels = {
  async getStatus(): Promise<Status> {
    return JSON.parse(await Native.getStatus());
  },
  async getSettings(): Promise<Settings> {
    return JSON.parse(await Native.getSettings());
  },
  async saveSettings(settings: Settings): Promise<Settings> {
    return JSON.parse(await Native.saveSettings(JSON.stringify(settings)));
  },
  async getWeek(): Promise<Week> {
    return JSON.parse(await Native.getWeek());
  },
  async getPermissions(): Promise<Permissions> {
    return JSON.parse(await Native.getPermissions());
  },
  openPermissionSettings(kind: PermissionKind): void {
    Native.openPermissionSettings(kind);
  },
  startFocus(minutes: number): Promise<number> {
    return Native.startFocus(minutes);
  },
  async getEventLog(): Promise<LogEntry[]> {
    return JSON.parse(await Native.getEventLog());
  },
  async getInstalledApps(): Promise<InstalledApp[]> {
    return JSON.parse(await Native.getInstalledApps());
  },
  async getLifetimeStats(): Promise<LifetimeStats> {
    return JSON.parse(await Native.getLifetimeStats());
  },
  /** Package name -> foreground seconds today, as reported by Android. Empty without Usage Access. */
  async getAppUsageToday(): Promise<Record<string, number>> {
    return JSON.parse(await Native.getAppUsageToday());
  },
};
