import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { DarkTheme, DefaultTheme, NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import React, { useEffect, useState } from 'react';
import { Text } from 'react-native';
import { NoReels } from '../modules/NoReelsModule';
import { AppList } from '../screens/AppList';
import { Dashboard } from '../screens/Dashboard';
import { EventLog } from '../screens/EventLog';
import { Focus } from '../screens/Focus';
import { Onboarding } from '../screens/Onboarding';
import { Reports } from '../screens/Reports';
import { Settings } from '../screens/Settings';
import { useColors, useIsDark } from '../theme/colors';

export type RootStackParamList = {
  Onboarding: undefined;
  Tabs: undefined;
  Focus: undefined;
  EventLog: undefined;
};

const Stack = createNativeStackNavigator<RootStackParamList>();
const Tab = createBottomTabNavigator();

const tabIcon = (glyph: string) => () => <Text style={{ fontSize: 18 }}>{glyph}</Text>;

function Tabs() {
  const c = useColors();
  return (
    <Tab.Navigator
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: c.primary,
        tabBarInactiveTintColor: c.textSecondary,
        tabBarStyle: { backgroundColor: c.surface, borderTopColor: c.border },
      }}>
      <Tab.Screen name="Dashboard" component={Dashboard} options={{ tabBarIcon: tabIcon('⏱') }} />
      <Tab.Screen name="Apps" component={AppList} options={{ tabBarIcon: tabIcon('📱') }} />
      <Tab.Screen name="Reports" component={Reports} options={{ tabBarIcon: tabIcon('📊') }} />
      <Tab.Screen name="Settings" component={Settings} options={{ tabBarIcon: tabIcon('⚙️') }} />
    </Tab.Navigator>
  );
}

export function AppNavigator() {
  const c = useColors();
  const isDark = useIsDark();
  // First launch (or blocking not set up): start on the permissions flow.
  const [initial, setInitial] = useState<'Onboarding' | 'Tabs' | null>(null);

  useEffect(() => {
    NoReels.getPermissions()
      .then(p => setInitial(p.accessibility && p.notifications ? 'Tabs' : 'Onboarding'))
      .catch(() => setInitial('Onboarding'));
  }, []);

  if (!initial) return null;

  const base = isDark ? DarkTheme : DefaultTheme;
  const theme = {
    ...base,
    colors: {
      ...base.colors,
      primary: c.primary,
      background: c.background,
      card: c.surface,
      text: c.text,
      border: c.border,
    },
  };

  return (
    <NavigationContainer theme={theme}>
      <Stack.Navigator initialRouteName={initial}>
        <Stack.Screen name="Onboarding" component={Onboarding} options={{ headerShown: false }} />
        <Stack.Screen name="Tabs" component={Tabs} options={{ headerShown: false }} />
        <Stack.Screen name="Focus" component={Focus} options={{ title: 'Hard Focus' }} />
        <Stack.Screen name="EventLog" component={EventLog} options={{ title: 'Detection Log' }} />
      </Stack.Navigator>
    </NavigationContainer>
  );
}
