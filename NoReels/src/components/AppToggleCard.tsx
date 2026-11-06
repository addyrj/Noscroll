import React from 'react';
import {Pressable, StyleSheet, Switch, Text, View} from 'react-native';
import {useColors} from '../theme/colors';
import {Body, Card} from './ui';

export function AppToggleCard({
  icon,
  name,
  mode,
  usageText,
  enabled,
  disabled,
  onChange,
  timeLimitMin,
  onTimeLimitChange,
}: {
  icon: string;
  name: string;
  mode: string;
  usageText?: string;
  enabled: boolean;
  disabled?: boolean;
  onChange: (v: boolean) => void;
  timeLimitMin?: number;
  onTimeLimitChange?: (min: number | undefined) => void;
}) {
  const c = useColors();

  const limits = [0, 5, 10, 15, 20, 30];

  return (
    <Card style={styles.card}>
      <View style={styles.topRow}>
        <Text style={styles.icon}>{icon}</Text>
        <View style={styles.text}>
          <Body style={styles.name}>{name}</Body>
          <Body secondary style={styles.mode}>
            {enabled ? mode : 'Not blocked'}
            {usageText ? ` · ${usageText}` : ''}
          </Body>
        </View>
        <Switch
          accessibilityLabel={`Block ${name}`}
          value={enabled}
          disabled={disabled}
          onValueChange={onChange}
          trackColor={{true: c.primary, false: c.border}}
          thumbColor={c.onPrimary}
        />
      </View>
      {enabled && onTimeLimitChange && (
        <View style={styles.limitRow}>
          <Body secondary style={styles.limitLabel}>
            Daily limit:
          </Body>
          {limits.map(m => {
            const active = m === 0 ? !timeLimitMin : timeLimitMin === m;
            return (
              <Pressable
                key={m}
                onPress={() => onTimeLimitChange(m === 0 ? undefined : m)}
                style={[
                  styles.chip,
                  {
                    backgroundColor: active ? c.primary : c.border,
                  },
                ]}>
                <Text
                  style={[
                    styles.chipText,
                    {color: active ? c.onPrimary : c.text},
                  ]}>
                  {m === 0 ? 'Off' : `${m}m`}
                </Text>
              </Pressable>
            );
          })}
        </View>
      )}
    </Card>
  );
}

const styles = StyleSheet.create({
  card: {gap: 10},
  topRow: {flexDirection: 'row', alignItems: 'center'},
  icon: {fontSize: 28, width: 40},
  text: {flex: 1},
  name: {fontWeight: '700'},
  mode: {fontSize: 13, marginTop: 2},
  limitRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    flexWrap: 'wrap',
  },
  limitLabel: {fontSize: 13, marginRight: 4},
  chip: {
    paddingHorizontal: 10,
    paddingVertical: 5,
    borderRadius: 14,
  },
  chipText: {fontSize: 12, fontWeight: '600'},
});
