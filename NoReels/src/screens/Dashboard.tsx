import { useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import React from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { SessionTimer } from '../components/SessionTimer';
import { UsageBar } from '../components/UsageBar';
import { Body, Button, Card, formatMinSec, formatMinutes, Screen } from '../components/ui';
import { usePolling } from '../hooks/usePolling';
import { NoReels } from '../modules/NoReelsModule';
import type { RootStackParamList } from '../navigation/AppNavigator';
import { useColors } from '../theme/colors';

export function Dashboard() {
  const c = useColors();
  const nav = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const status = usePolling(NoReels.getStatus, 1000);

  if (!status) return <Screen>{null}</Screen>;

  const left = Math.max(0, status.dailyLimitSec - status.dailyUsedSec);

  return (
    <Screen>
      {!status.serviceRunning && (
        <Pressable onPress={() => nav.navigate('Onboarding')}>
          <Card style={{ borderColor: c.primary }}>
            <Body style={{ color: c.primary, fontWeight: '700' }}>
              ⚠️ Blocking is OFF
            </Body>
            <Body secondary>The accessibility service isn't running. Tap to fix.</Body>
          </Card>
        </Pressable>
      )}

      <Card>
        <Text style={[styles.big, { color: c.text }]}>{formatMinSec(status.dailyUsedSec)}</Text>
        <Body secondary>used today of {formatMinutes(status.dailyLimitSec)}</Body>
        <UsageBar used={status.dailyUsedSec} limit={status.dailyLimitSec} />
        <Body secondary>{formatMinSec(left)} left today</Body>
      </Card>

      <SessionTimer status={status} />

      <View style={styles.grid}>
        <Stat label="Sessions today" value={String(status.sessionsToday)} />
        <Stat label="Blocked attempts" value={String(status.blockedToday)} />
        <Stat label="Streak" value={`🔥 ${status.streak}`} />
      </View>

      <Button title="Start Hard Focus" onPress={() => nav.navigate('Focus')} />
    </Screen>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  const c = useColors();
  return (
    <Card style={styles.stat}>
      <Text style={[styles.statValue, { color: c.text }]}>{value}</Text>
      <Body secondary style={styles.statLabel}>
        {label}
      </Body>
    </Card>
  );
}

const styles = StyleSheet.create({
  big: { fontSize: 56, fontWeight: '800', fontVariant: ['tabular-nums'] },
  grid: { flexDirection: 'row', gap: 8 },
  stat: { flex: 1, alignItems: 'center', gap: 4, paddingHorizontal: 8 },
  statValue: { fontSize: 22, fontWeight: '800' },
  statLabel: { fontSize: 12, textAlign: 'center' },
});
