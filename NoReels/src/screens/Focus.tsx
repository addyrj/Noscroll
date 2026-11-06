import React from 'react';
import { Alert, StyleSheet, Text, View } from 'react-native';
import Svg, { Circle } from 'react-native-svg';
import { Body, Button, Card, formatMinSec, Screen } from '../components/ui';
import { usePolling } from '../hooks/usePolling';
import { NoReels } from '../modules/NoReelsModule';
import { useColors } from '../theme/colors';

// Hard Focus: 30 min minimum, with an optional +10 or +20 add-on chosen before starting.
const BASE_MIN = 30;
const OPTIONS = [
  { minutes: BASE_MIN, label: '30 minutes' },
  { minutes: BASE_MIN + 10, label: '30 + 10 minutes' },
  { minutes: BASE_MIN + 20, label: '30 + 20 minutes' },
];
const SIZE = 220;
const STROKE = 14;
const R = (SIZE - STROKE) / 2;
const CIRC = 2 * Math.PI * R;

export function Focus() {
  const c = useColors();
  const status = usePolling(NoReels.getStatus, 1000);
  const remaining = status?.focusRemainingSec ?? 0;
  // Progress against the nearest option that fits, so the ring makes sense after restarts.
  const totalMin =
    OPTIONS.find(o => o.minutes * 60 >= remaining)?.minutes ??
    Math.ceil(remaining / 60);

  const start = (minutes: number) =>
    Alert.alert(
      `Start Hard Focus for ${minutes} min?`,
      'YouTube, Facebook, Instagram, TikTok, Snapchat and your custom apps are fully blocked — long videos too. It cannot be stopped until the timer ends.',
      [
        { text: 'Cancel', style: 'cancel' },
        { text: 'Start', onPress: () => NoReels.startFocus(minutes) },
      ],
    );

  if (remaining <= 0) {
    return (
      <Screen inset={false}>
        <Card>
          <Body>
            Hard Focus blocks all social media completely. Calls, messages and
            other apps keep working.
          </Body>
          {OPTIONS.map(o => (
            <Button
              key={o.minutes}
              title={o.label}
              variant="outline"
              onPress={() => start(o.minutes)}
            />
          ))}
        </Card>
      </Screen>
    );
  }

  return (
    <Screen inset={false}>
      <Card style={styles.center}>
        <View>
          <Svg width={SIZE} height={SIZE}>
            <Circle
              cx={SIZE / 2}
              cy={SIZE / 2}
              r={R}
              stroke={c.border}
              strokeWidth={STROKE}
              fill="none"
            />
            <Circle
              cx={SIZE / 2}
              cy={SIZE / 2}
              r={R}
              stroke={c.primary}
              strokeWidth={STROKE}
              fill="none"
              strokeLinecap="round"
              strokeDasharray={`${CIRC} ${CIRC}`}
              strokeDashoffset={CIRC * (1 - remaining / (totalMin * 60))}
              transform={`rotate(-90 ${SIZE / 2} ${SIZE / 2})`}
            />
          </Svg>
          <View style={styles.ringLabel}>
            <Text style={[styles.time, { color: c.text }]}>
              {formatMinSec(remaining)}
            </Text>
            <Body secondary>left</Body>
          </View>
        </View>
        <Body secondary>Social media is blocked. You've got this.</Body>
      </Card>
      {/* Visible but locked until the timer ends. */}
      <Button
        title="Add 25 minutes"
        variant="outline"
        onPress={() => {}}
        disabled
      />
      <Button title="End focus" variant="outline" onPress={() => {}} disabled />
      <Body secondary style={styles.note}>
        🔒 Locked until Hard Focus ends
      </Body>
    </Screen>
  );
}

const styles = StyleSheet.create({
  center: { alignItems: 'center' },
  ringLabel: {
    ...StyleSheet.absoluteFill,
    alignItems: 'center',
    justifyContent: 'center',
  },
  time: { fontSize: 44, fontWeight: '800', fontVariant: ['tabular-nums'] },
  note: { textAlign: 'center' },
});
