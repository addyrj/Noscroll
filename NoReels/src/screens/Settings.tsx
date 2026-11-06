import {useNavigation} from '@react-navigation/native';
import type {NativeStackNavigationProp} from '@react-navigation/native-stack';
import React, {useEffect, useState} from 'react';
import {Alert, StyleSheet, TextInput} from 'react-native';
import {
  Body,
  Button,
  Card,
  Divider,
  Heading,
  Screen,
  Stepper,
  ToggleRow,
} from '../components/ui';
import {Settings as SettingsT} from '../modules/NoReelsModule';
import type {RootStackParamList} from '../navigation/AppNavigator';
import {useSettings} from '../store/settings';
import {useColors} from '../theme/colors';

export function Settings() {
  const c = useColors();
  const nav = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const {settings, save} = useSettings();
  const [draft, setDraft] = useState<SettingsT | null>(settings);

  useEffect(() => setDraft(settings), [settings]);

  if (!draft || !settings) return <Screen>{null}</Screen>;

  const set = <K extends keyof SettingsT>(key: K, value: SettingsT[K]) =>
    setDraft({...draft, [key]: value});
  const dirty = JSON.stringify(draft) !== JSON.stringify(settings);

  const onSave = async () => {
    try {
      await save(draft);
      Alert.alert('Saved', 'New limits apply immediately.');
    } catch (e: any) {
      Alert.alert('Not saved', String(e?.message ?? e));
    }
  };

  return (
    <Screen>
      <Heading>Settings</Heading>

      <Card>
        <Body style={styles.sectionTitle}>Time Limits</Body>
        <Stepper
          label="Session limit"
          value={draft.sessionLimitMin}
          min={1}
          max={5}
          unit="min"
          onChange={v => set('sessionLimitMin', v)}
        />
        <Divider />
        <Stepper
          label="Daily limit"
          value={draft.dailyLimitMin}
          min={5}
          max={30}
          step={5}
          unit="min"
          onChange={v => set('dailyLimitMin', v)}
        />
        <Divider />
        <Stepper
          label="Cooldown"
          value={draft.cooldownMin}
          min={5}
          max={30}
          step={5}
          unit="min"
          onChange={v => set('cooldownMin', v)}
        />
      </Card>

      <Card>
        <Body style={styles.sectionTitle}>Pause Before Opening</Body>
        <Body secondary>
          Shows a breathing prompt when you open a social media app. You decide
          whether to continue or go back.
        </Body>
        <ToggleRow
          label="Enable pause"
          hint="Interrupt automatic app-opening habits"
          value={draft.pauseEnabled}
          onChange={v => set('pauseEnabled', v)}
        />
        {draft.pauseEnabled && (
          <>
            <Divider />
            <Stepper
              label="Pause delay"
              value={draft.pauseDelaySec}
              min={3}
              max={30}
              unit="sec"
              onChange={v => set('pauseDelaySec', v)}
            />
          </>
        )}
      </Card>

      <Card>
        <Body style={styles.sectionTitle}>Curious Mode</Body>
        <Body secondary>
          Allow a short peek at Reels/Shorts before blocking. Just enough for
          one reel, then the block screen appears.
        </Body>
        <ToggleRow
          label="Enable Curious Mode"
          hint="Overrides the session limit with a short peek time"
          value={draft.curiousMode}
          onChange={v => set('curiousMode', v)}
        />
        {draft.curiousMode && (
          <>
            <Divider />
            <Stepper
              label="Peek time"
              value={draft.curiousTimeSec}
              min={10}
              max={60}
              step={5}
              unit="sec"
              onChange={v => set('curiousTimeSec', v)}
            />
          </>
        )}
      </Card>

      <Card>
        <Body style={styles.sectionTitle}>Re-intervention</Body>
        <Body secondary>
          After you've been in a social app for a while, show the pause prompt
          again to remind you.
        </Body>
        <ToggleRow
          label="Enable re-intervention"
          hint="Prompts you again after the set time in the app"
          value={draft.reinterventionEnabled}
          onChange={v => set('reinterventionEnabled', v)}
        />
        {draft.reinterventionEnabled && (
          <>
            <Divider />
            <Stepper
              label="Re-prompt after"
              value={draft.reinterventionMin}
              min={5}
              max={60}
              step={5}
              unit="min"
              onChange={v => set('reinterventionMin', v)}
            />
          </>
        )}
      </Card>

      <Card>
        <Body style={styles.sectionTitle}>Block Screen</Body>
        <ToggleRow
          label="Strict mode"
          hint="Block screen needs a 5-second hold + a reason to leave"
          value={draft.strictMode}
          onChange={v => set('strictMode', v)}
        />
        <Divider />
        <ToggleRow
          label="Night schedule"
          hint={`Block ${draft.nightStartHour > 12 ? draft.nightStartHour - 12 : draft.nightStartHour}${draft.nightStartHour >= 12 ? 'pm' : 'am'} - ${draft.nightEndHour > 12 ? draft.nightEndHour - 12 : draft.nightEndHour}${draft.nightEndHour >= 12 ? 'pm' : 'am'} regardless of budget`}
          value={draft.nightSchedule}
          onChange={v => set('nightSchedule', v)}
        />
        {draft.nightSchedule && (
          <>
            <Divider />
            <Stepper
              label="Start blocking at"
              value={draft.nightStartHour}
              min={0}
              max={23}
              unit="h"
              onChange={v => set('nightStartHour', v)}
            />
            <Divider />
            <Stepper
              label="Stop blocking at"
              value={draft.nightEndHour}
              min={0}
              max={23}
              unit="h"
              onChange={v => set('nightEndHour', v)}
            />
          </>
        )}
        <Divider />
        <ToggleRow
          label="Show motivational quote"
          hint="Display your goal on the block screen"
          value={draft.showQuote}
          onChange={v => set('showQuote', v)}
        />
        {draft.showQuote && (
          <TextInput
            value={draft.quoteText}
            onChangeText={t => set('quoteText', t)}
            maxLength={140}
            placeholder="Your goal"
            placeholderTextColor={c.textSecondary}
            style={[styles.input, {color: c.text, borderColor: c.border}]}
          />
        )}
      </Card>

      <Button title="Save Changes" onPress={onSave} disabled={!dirty} />

      <Button
        title="Permissions"
        variant="outline"
        onPress={() => nav.navigate('Onboarding')}
      />
      <Button
        title="Detection log"
        variant="outline"
        onPress={() => nav.navigate('EventLog')}
      />
    </Screen>
  );
}

const styles = StyleSheet.create({
  sectionTitle: {fontWeight: '700', fontSize: 17},
  input: {
    borderWidth: 1,
    borderRadius: 12,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 15,
  },
});
