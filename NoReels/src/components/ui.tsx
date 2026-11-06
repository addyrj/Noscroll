import React from 'react';
import {
  Pressable,
  ScrollView,
  StyleSheet,
  Switch,
  Text,
  TextStyle,
  View,
  ViewStyle,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useColors } from '../theme/colors';

// Small custom building blocks (no third-party UI library).

export function Screen({
  children,
  inset = true,
}: {
  children: React.ReactNode;
  /** False on stack screens that already have a header covering the status bar. */
  inset?: boolean;
}) {
  const c = useColors();
  const insets = useSafeAreaInsets();
  // The top inset sits outside the ScrollView so content never scrolls under the status bar.
  return (
    <View
      style={[
        styles.fill,
        { backgroundColor: c.background, paddingTop: inset ? insets.top : 0 },
      ]}
    >
      <ScrollView
        contentContainerStyle={[styles.screen, styles.screenPad]}
        keyboardShouldPersistTaps="handled"
      >
        {children}
      </ScrollView>
    </View>
  );
}

export function Card({
  children,
  style,
}: {
  children: React.ReactNode;
  style?: ViewStyle;
}) {
  const c = useColors();
  return (
    <View
      style={[
        styles.card,
        { backgroundColor: c.surface, borderColor: c.border },
        style,
      ]}
    >
      {children}
    </View>
  );
}

export function Heading({ children }: { children: React.ReactNode }) {
  const c = useColors();
  return <Text style={[styles.heading, { color: c.text }]}>{children}</Text>;
}

export function Body({
  children,
  secondary,
  style,
}: {
  children: React.ReactNode;
  secondary?: boolean;
  style?: TextStyle;
}) {
  const c = useColors();
  return (
    <Text
      style={[
        styles.body,
        { color: secondary ? c.textSecondary : c.text },
        style,
      ]}
    >
      {children}
    </Text>
  );
}

export function Button({
  title,
  onPress,
  variant = 'primary',
  disabled,
}: {
  title: string;
  onPress: () => void;
  variant?: 'primary' | 'outline';
  disabled?: boolean;
}) {
  const c = useColors();
  const primary = variant === 'primary';
  return (
    <Pressable
      accessibilityRole="button"
      onPress={onPress}
      disabled={disabled}
      style={({ pressed }) => [
        styles.button,
        {
          backgroundColor: primary ? c.primary : 'transparent',
          borderColor: primary ? c.primary : c.border,
          opacity: disabled ? 0.4 : pressed ? 0.7 : 1,
        },
      ]}
    >
      <Text
        style={[styles.buttonText, { color: primary ? c.onPrimary : c.text }]}
      >
        {title}
      </Text>
    </Pressable>
  );
}

export function ToggleRow({
  label,
  hint,
  value,
  onChange,
}: {
  label: string;
  hint?: string;
  value: boolean;
  onChange: (v: boolean) => void;
}) {
  const c = useColors();
  return (
    <View style={styles.row}>
      <View style={styles.rowText}>
        <Body>{label}</Body>
        {hint ? (
          <Body secondary style={styles.hint}>
            {hint}
          </Body>
        ) : null}
      </View>
      <Switch
        value={value}
        onValueChange={onChange}
        trackColor={{ true: c.primary, false: c.border }}
        thumbColor={c.onPrimary}
      />
    </View>
  );
}

/** Minus / value / plus control, used instead of a slider. */
export function Stepper({
  label,
  value,
  min,
  max,
  step = 1,
  unit,
  onChange,
}: {
  label: string;
  value: number;
  min: number;
  max: number;
  step?: number;
  unit: string;
  onChange: (v: number) => void;
}) {
  const c = useColors();
  const btn = (text: string, next: number, enabled: boolean) => (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`${text === '−' ? 'Decrease' : 'Increase'} ${label}`}
      disabled={!enabled}
      onPress={() => onChange(Math.min(max, Math.max(min, next)))}
      style={[
        styles.stepBtn,
        { borderColor: c.border, opacity: enabled ? 1 : 0.3 },
      ]}
    >
      <Text style={[styles.stepBtnText, { color: c.text }]}>{text}</Text>
    </Pressable>
  );
  return (
    <View style={styles.row}>
      <View style={styles.rowText}>
        <Body>{label}</Body>
        <Body secondary style={styles.hint}>
          {min}–{max} {unit}
        </Body>
      </View>
      {btn('−', value - step, value > min)}
      <Text style={[styles.stepValue, { color: c.text }]}>
        {value} {unit}
      </Text>
      {btn('+', value + step, value < max)}
    </View>
  );
}

export function Divider() {
  const c = useColors();
  return <View style={[styles.divider, { backgroundColor: c.border }]} />;
}

export function formatMinSec(totalSec: number): string {
  const s = Math.max(0, Math.round(totalSec));
  return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`;
}

export function formatMinutes(totalSec: number): string {
  const m = Math.round(totalSec / 60);
  return m < 60 ? `${m} min` : `${Math.floor(m / 60)}h ${m % 60}m`;
}

const styles = StyleSheet.create({
  fill: { flex: 1 },
  screen: { paddingHorizontal: 16, gap: 16, flexGrow: 1 },
  screenPad: { paddingTop: 16, paddingBottom: 32 },
  card: { borderRadius: 16, borderWidth: 1, padding: 16, gap: 12 },
  heading: { fontSize: 28, fontWeight: '800' },
  body: { fontSize: 16 },
  hint: { fontSize: 13, marginTop: 2 },
  button: {
    borderRadius: 999,
    borderWidth: 1,
    paddingVertical: 14,
    paddingHorizontal: 24,
    alignItems: 'center',
  },
  buttonText: { fontSize: 16, fontWeight: '700' },
  row: { flexDirection: 'row', alignItems: 'center', gap: 8, minHeight: 48 },
  rowText: { flex: 1 },
  stepBtn: {
    width: 40,
    height: 40,
    borderRadius: 20,
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  stepBtnText: { fontSize: 22, fontWeight: '600' },
  stepValue: {
    minWidth: 64,
    textAlign: 'center',
    fontSize: 16,
    fontWeight: '700',
  },
  divider: { height: 1 },
});
