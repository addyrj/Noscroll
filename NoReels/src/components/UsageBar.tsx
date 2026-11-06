import React from 'react';
import { StyleSheet, View } from 'react-native';
import { useColors } from '../theme/colors';

/** Budget progress bar: green under 70%, amber under 100%, red when used up. */
export function UsageBar({ used, limit }: { used: number; limit: number }) {
  const c = useColors();
  const ratio = limit > 0 ? Math.min(1, used / limit) : 0;
  const color = ratio >= 1 ? c.primary : ratio >= 0.7 ? c.warning : c.success;
  return (
    <View
      accessibilityRole="progressbar"
      accessibilityValue={{ min: 0, max: limit, now: Math.min(used, limit) }}
      style={[styles.track, { backgroundColor: c.border }]}>
      <View style={[styles.fill, { width: `${ratio * 100}%`, backgroundColor: color }]} />
    </View>
  );
}

const styles = StyleSheet.create({
  track: { height: 10, borderRadius: 5, overflow: 'hidden' },
  fill: { height: '100%', borderRadius: 5 },
});
