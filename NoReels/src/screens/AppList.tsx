import React, {useCallback, useEffect, useState} from 'react';
import {
  Alert,
  FlatList,
  Pressable,
  StyleSheet,
  Switch,
  Text,
  TextInput,
  View,
} from 'react-native';
import {AppToggleCard} from '../components/AppToggleCard';
import {Body, Card, formatMinutes, Heading} from '../components/ui';
import {usePolling} from '../hooks/usePolling';
import {AppKey, InstalledApp, NoReels} from '../modules/NoReelsModule';
import {useSettings} from '../store/settings';
import {useColors} from '../theme/colors';
import {useSafeAreaInsets} from 'react-native-safe-area-context';

const KNOWN_APPS: {
  key: AppKey;
  icon: string;
  name: string;
  mode: string;
  pkgs: string[];
}[] = [
  {
    key: 'youtube',
    icon: '▶️',
    name: 'YouTube',
    mode: 'Shorts only',
    pkgs: ['com.google.android.youtube'],
  },
  {
    key: 'instagram',
    icon: '📸',
    name: 'Instagram',
    mode: 'Reels only',
    pkgs: ['com.instagram.android'],
  },
  {
    key: 'facebook',
    icon: '📘',
    name: 'Facebook',
    mode: 'Reels only',
    pkgs: ['com.facebook.katana'],
  },
  {
    key: 'snapchat',
    icon: '👻',
    name: 'Snapchat',
    mode: 'Spotlight only',
    pkgs: ['com.snapchat.android'],
  },
  {
    key: 'tiktok',
    icon: '🎵',
    name: 'TikTok',
    mode: 'Whole app',
    pkgs: ['com.zhiliaoapp.musically', 'com.ss.android.ugc.trill'],
  },
];

const KNOWN_PKGS = new Set(KNOWN_APPS.flatMap(a => a.pkgs));

export function AppList() {
  const c = useColors();
  const insets = useSafeAreaInsets();
  const {settings, save} = useSettings();
  const usage = usePolling(NoReels.getAppUsageToday, 30000);
  const [installed, setInstalled] = useState<InstalledApp[]>([]);
  const [search, setSearch] = useState('');

  useEffect(() => {
    NoReels.getInstalledApps().then(apps => {
      const filtered = apps
        .filter(a => !KNOWN_PKGS.has(a.pkg))
        .sort((a, b) => {
          if (a.social !== b.social) return a.social ? -1 : 1;
          return a.label.localeCompare(b.label);
        });
      setInstalled(filtered);
    });
  }, []);

  if (!settings) return <View style={[styles.fill, {backgroundColor: c.background}]} />;

  const persist = (next: typeof settings) =>
    save(next).catch(e => Alert.alert('Not saved', String(e?.message ?? e)));

  const usageFor = (pkgs: string[]) => {
    const sec = pkgs.reduce((sum, p) => sum + (usage?.[p] ?? 0), 0);
    return sec > 0 ? `${formatMinutes(sec)} open today` : undefined;
  };

  const customSet = new Set(settings.customPackages);

  const toggleCustom = useCallback(
    (pkg: string, on: boolean) => {
      const next = on
        ? [...new Set([...settings.customPackages, pkg])]
        : settings.customPackages.filter(p => p !== pkg);
      persist({...settings, customPackages: next});
    },
    [settings, persist],
  );

  const q = search.trim().toLowerCase();
  const filtered = q
    ? installed.filter(
        a =>
          a.label.toLowerCase().includes(q) ||
          a.pkg.toLowerCase().includes(q),
      )
    : installed;

  const blockedCount = settings.customPackages.length;

  const renderItem = useCallback(
    ({item}: {item: InstalledApp}) => {
      const blocked = customSet.has(item.pkg);
      const usageSec = usage?.[item.pkg] ?? 0;
      return (
        <View style={[styles.appRow, {borderBottomColor: c.border}]}>
          <View style={styles.appInfo}>
            <Text style={[styles.appLabel, {color: c.text}]} numberOfLines={1}>
              {item.label}
            </Text>
            <Text style={[styles.appPkg, {color: c.textSecondary}]} numberOfLines={1}>
              {item.pkg}
              {usageSec > 0 ? ` · ${formatMinutes(usageSec)}` : ''}
            </Text>
          </View>
          <Switch
            value={blocked}
            onValueChange={v => toggleCustom(item.pkg, v)}
            trackColor={{true: c.primary, false: c.border}}
            thumbColor={c.onPrimary}
          />
        </View>
      );
    },
    [customSet, usage, c, toggleCustom],
  );

  return (
    <View style={[styles.fill, {backgroundColor: c.background, paddingTop: insets.top}]}>
      <FlatList
        data={filtered}
        keyExtractor={item => item.pkg}
        renderItem={renderItem}
        contentContainerStyle={styles.list}
        keyboardShouldPersistTaps="handled"
        ListHeaderComponent={
          <>
            <Heading>App Limits</Heading>
            <Body secondary>
              All enabled apps share one daily budget of {settings.dailyLimitMin}{' '}
              min and {settings.sessionLimitMin} min per session.
            </Body>

            {KNOWN_APPS.map(app => (
              <AppToggleCard
                key={app.key}
                icon={app.icon}
                name={app.name}
                mode={app.mode}
                usageText={usageFor(app.pkgs)}
                enabled={settings.apps[app.key]}
                onChange={v =>
                  persist({
                    ...settings,
                    apps: {...settings.apps, [app.key]: v},
                  })
                }
                timeLimitMin={settings.appTimeLimits[app.key]}
                onTimeLimitChange={min => {
                  const next = {...settings.appTimeLimits};
                  if (min === undefined) {
                    delete next[app.key];
                  } else {
                    next[app.key] = min;
                  }
                  persist({...settings, appTimeLimits: next});
                }}
              />
            ))}

            <View style={styles.sectionHeader}>
              <Text style={[styles.sectionTitle, {color: c.text}]}>
                Block Entire Apps
              </Text>
              <Text style={[styles.sectionCount, {color: c.textSecondary}]}>
                {blockedCount} selected
              </Text>
            </View>
            <Body secondary>
              Pick any app to fully block it. Social media apps are shown first.
            </Body>
            <TextInput
              value={search}
              onChangeText={setSearch}
              placeholder="Search apps..."
              placeholderTextColor={c.textSecondary}
              autoCapitalize="none"
              autoCorrect={false}
              style={[
                styles.search,
                {
                  color: c.text,
                  borderColor: c.border,
                  backgroundColor: c.surface,
                },
              ]}
            />
          </>
        }
        ListEmptyComponent={
          <Body secondary style={styles.empty}>
            {search ? 'No apps match your search.' : 'Loading installed apps...'}
          </Body>
        }
      />
    </View>
  );
}

const styles = StyleSheet.create({
  fill: {flex: 1},
  list: {paddingHorizontal: 16, gap: 12, paddingBottom: 32, paddingTop: 16},
  sectionHeader: {
    flexDirection: 'row',
    alignItems: 'baseline',
    justifyContent: 'space-between',
    marginTop: 8,
  },
  sectionTitle: {fontSize: 20, fontWeight: '700'},
  sectionCount: {fontSize: 14},
  search: {
    borderWidth: 1,
    borderRadius: 12,
    paddingHorizontal: 14,
    paddingVertical: 10,
    fontSize: 15,
  },
  appRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 10,
    borderBottomWidth: StyleSheet.hairlineWidth,
    gap: 12,
  },
  appInfo: {flex: 1},
  appLabel: {fontSize: 15, fontWeight: '600'},
  appPkg: {fontSize: 12, marginTop: 2},
  empty: {textAlign: 'center', marginTop: 24},
});
