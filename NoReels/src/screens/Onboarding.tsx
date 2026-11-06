import { useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import React from 'react';
import {
  Alert,
  PermissionsAndroid,
  Platform,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { Body, Button, Card, Heading, Screen } from '../components/ui';
import { usePolling } from '../hooks/usePolling';
import { PermissionKind, Permissions, NoReels } from '../modules/NoReelsModule';
import type { RootStackParamList } from '../navigation/AppNavigator';
import { useColors } from '../theme/colors';

// Prominent disclosure required by Google Play's Accessibility API policy, shown before
// the user is sent to the Accessibility settings.
const ACCESSIBILITY_DISCLOSURE =
  'NoReels uses the Accessibility service to see which app and screen are open, so it can ' +
  'detect YouTube Shorts, Instagram Reels, Facebook Reels, Snapchat Spotlight and TikTok, count ' +
  'your time, and show the block screen when your limit is reached.\n\n' +
  'It does not read your messages, passwords or typing. Nothing is collected, stored off the ' +
  'phone, or shared — all data stays on this device.';

type Step = {
  key: keyof Permissions;
  kind: PermissionKind;
  title: string;
  why: string;
  required: boolean;
};

const STEPS: Step[] = [
  {
    key: 'accessibility',
    kind: 'accessibility',
    title: 'Accessibility access',
    why: 'Reads which app and screen are open to detect Shorts and Reels. Settings → Accessibility → Installed apps → NoReels.',
    required: true,
  },
  {
    key: 'notifications',
    kind: 'notifications',
    title: 'Notifications',
    why: 'Shows the session countdown and the 1-minute warning.',
    required: true,
  },
  {
    key: 'battery',
    kind: 'battery',
    title: 'Ignore battery optimization',
    why: 'Stops Android from killing the blocker in the background. Strongly recommended.',
    required: false,
  },
  {
    key: 'usage',
    kind: 'usage',
    title: 'Usage access (optional)',
    why: 'Shows how long each app was open today on the Apps screen.',
    required: false,
  },
];

export function Onboarding() {
  const c = useColors();
  const nav = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const perms = usePolling(NoReels.getPermissions, 1500);
  const ready = perms?.accessibility && perms?.notifications;

  const grant = async (step: Step) => {
    if (step.kind === 'accessibility') {
      Alert.alert('Accessibility permission', ACCESSIBILITY_DISCLOSURE, [
        { text: 'Not now', style: 'cancel' },
        {
          text: 'Agree',
          onPress: () => NoReels.openPermissionSettings('accessibility'),
        },
      ]);
      return;
    }
    if (step.kind === 'notifications' && Number(Platform.Version) >= 33) {
      const result = await PermissionsAndroid.request(
        PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS,
      );
      if (result !== PermissionsAndroid.RESULTS.NEVER_ASK_AGAIN) return;
    }
    NoReels.openPermissionSettings(step.kind);
  };

  const finish = () => (nav.canGoBack() ? nav.goBack() : nav.replace('Tabs'));

  return (
    <Screen>
      <Heading>Set up blocking</Heading>
      <Body secondary>
        Blocking only works once the required permissions are granted.
        Everything stays on this phone.
      </Body>

      {STEPS.map((step, i) => {
        const granted = perms?.[step.key] ?? false;
        return (
          <Card key={step.key}>
            <View style={styles.row}>
              <Text style={styles.status}>
                {granted ? '✅' : step.required ? '❌' : '⚪'}
              </Text>
              <Body style={styles.title}>
                {i + 1}. {step.title}
              </Body>
            </View>
            <Body secondary>{step.why}</Body>
            {!granted && (
              <Button
                title="Open settings"
                variant="outline"
                onPress={() => grant(step)}
              />
            )}
          </Card>
        );
      })}

      <Card>
        <Body style={styles.title}>Can't turn on Accessibility?</Body>
        <Body secondary>
          On Android 13+ a sideloaded app may show "Restricted setting". Open
          App info, tap ⋮ (top right) → "Allow restricted settings", then try
          again. Installing with adb avoids this.
        </Body>
        <Button
          title="Open App info"
          variant="outline"
          onPress={() => NoReels.openPermissionSettings('appInfo')}
        />
      </Card>

      {perms?.accessibility && !perms.serviceRunning && (
        <Body style={{ color: c.warning }}>
          Accessibility is on but the service isn't running yet. Toggle it off
          and on again.
        </Body>
      )}

      <Button
        title={ready ? 'Continue' : 'Grant required permissions'}
        onPress={finish}
        disabled={!ready}
      />
      {!ready && (
        <Button title="Skip for now" variant="outline" onPress={finish} />
      )}
    </Screen>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  status: { fontSize: 18 },
  title: { fontWeight: '700' },
});
