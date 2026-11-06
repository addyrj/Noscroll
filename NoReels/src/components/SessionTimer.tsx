import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { Status } from '../modules/NoReelsModule';
import { useColors } from '../theme/colors';
import { Body, Card, formatMinSec } from './ui';

/** Live state of the current session: running, paused, cooling down or blocked. */
export function SessionTimer({ status }: { status: Status }) {
  const c = useColors();
  let label: string;
  let value: string;
  let color = c.text;

  if (status.focusRemainingSec > 0) {
    label = 'Hard Focus — social media blocked';
    value = formatMinSec(status.focusRemainingSec);
    color = c.primary;
  } else if (status.block === 'NIGHT') {
    label = 'Night schedule active';
    value = 'until 7:00';
    color = c.primary;
  } else if (status.block === 'DAILY') {
    label = 'Daily allowance used';
    value = 'resets at midnight';
    color = c.primary;
  } else if (status.block === 'COOLDOWN') {
    label = 'Cooldown — next session in';
    value = formatMinSec(status.cooldownRemainingSec);
    color = c.warning;
  } else if (status.sessionActive) {
    const left = Math.min(
      status.sessionLimitSec - status.sessionElapsedSec,
      status.dailyLimitSec - status.dailyUsedSec,
    );
    label = status.sessionInFeed ? 'Session running — time left' : 'Session paused — time left';
    value = formatMinSec(left);
    color = left <= 60 ? c.warning : c.success;
  } else {
    label = 'No session running';
    value = `${Math.round(status.sessionLimitSec / 60)} min available`;
    color = c.success;
  }

  return (
    <Card>
      <Body secondary>{label}</Body>
      <View>
        <Text style={[styles.value, { color }]}>{value}</Text>
      </View>
    </Card>
  );
}

const styles = StyleSheet.create({
  value: { fontSize: 32, fontWeight: '800', fontVariant: ['tabular-nums'] },
});
