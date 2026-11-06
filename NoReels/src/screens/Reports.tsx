import React, {useEffect, useState} from 'react';
import {StyleSheet, Text, View} from 'react-native';
import {WeeklyChart} from '../components/WeeklyChart';
import {Body, Card, formatMinutes, Heading, Screen} from '../components/ui';
import {usePolling} from '../hooks/usePolling';
import {DayUsage, LifetimeStats, NoReels} from '../modules/NoReelsModule';
import {useColors} from '../theme/colors';

const DAY_NAMES = [
  'Monday',
  'Tuesday',
  'Wednesday',
  'Thursday',
  'Friday',
  'Saturday',
  'Sunday',
];

const LEVEL_NAMES = ['Beginner', 'Aware', 'Focused', 'Disciplined', 'Master'];
const LEVEL_EMOJI = ['🌱', '🌿', '🌳', '🏆', '👑'];

export function Reports() {
  const c = useColors();
  const week = usePolling(NoReels.getWeek, 15000);
  const [stats, setStats] = useState<LifetimeStats | null>(null);

  useEffect(() => {
    NoReels.getLifetimeStats().then(setStats);
    const id = setInterval(() => NoReels.getLifetimeStats().then(setStats), 30000);
    return () => clearInterval(id);
  }, []);

  if (!week) return <Screen>{null}</Screen>;

  const indexed = week.days.map((d, i) => ({...d, name: DAY_NAMES[i]}));
  const past = indexed.filter(d => !d.isFuture && (d.tracked || d.isToday));
  const pick = (better: (a: DayUsage, b: DayUsage) => boolean) =>
    past.reduce<(typeof past)[number] | null>(
      (acc, d) => (!acc || better(d, acc) ? d : acc),
      null,
    );
  const best = pick((a, b) => a.usedSec < b.usedSec);
  const worst = pick((a, b) => a.usedSec > b.usedSec);
  const total = past.reduce((s, d) => s + d.usedSec, 0);
  const blocked = past.reduce((s, d) => s + d.blocked, 0);

  return (
    <Screen>
      <Heading>This Week</Heading>
      <Card>
        <WeeklyChart days={week.days} />
        <Body secondary style={styles.small}>
          Minutes per day · dashed line = daily limit
        </Body>
      </Card>

      <Card>
        <Text style={[styles.streak, {color: c.text}]}>
          🔥 {week.streak}-day streak
        </Text>
        <Body secondary>Days in a row under your daily limit</Body>
      </Card>

      <View style={styles.grid}>
        <Stat
          label="Best day"
          value={
            best ? `${best.name}\n${formatMinutes(best.usedSec)}` : '—'
          }
        />
        <Stat
          label="Worst day"
          value={
            worst ? `${worst.name}\n${formatMinutes(worst.usedSec)}` : '—'
          }
        />
      </View>
      <View style={styles.grid}>
        <Stat label="Total this week" value={formatMinutes(total)} />
        <Stat label="Blocked attempts" value={String(blocked)} />
      </View>

      {stats && (
        <>
          <Heading>Focus Progress</Heading>

          <Card style={styles.levelCard}>
            <Text style={styles.levelEmoji}>
              {LEVEL_EMOJI[(stats.level - 1) || 0]}
            </Text>
            <Text style={[styles.levelTitle, {color: c.text}]}>
              Level {stats.level} — {LEVEL_NAMES[(stats.level - 1) || 0]}
            </Text>
            <Text style={[styles.points, {color: c.primary}]}>
              {stats.points} focus points
            </Text>
            <Body secondary>
              {stats.daysUnderLimit} of {stats.totalDays} days under
              limit · best streak: {stats.bestStreak} days
            </Body>
          </Card>

          <View style={styles.grid}>
            <Stat
              label="Total days"
              value={String(stats.totalDays)}
            />
            <Stat
              label="Sessions"
              value={String(stats.totalSessions)}
            />
            <Stat
              label="Time saved"
              value={formatMinutes(
                Math.max(0, stats.totalDays * 60 * 60 - stats.totalUsedMin * 60),
              )}
            />
          </View>

          {stats.achievements.length > 0 && (
            <Card>
              <Body style={styles.achieveTitle}>Achievements</Body>
              {stats.achievements.map(a => (
                <View key={a.id} style={styles.achieveRow}>
                  <Text style={styles.achieveBadge}>🏅</Text>
                  <View style={styles.achieveText}>
                    <Body style={styles.achieveName}>{a.title}</Body>
                    <Body secondary style={styles.small}>
                      {a.description}
                    </Body>
                  </View>
                </View>
              ))}
            </Card>
          )}
        </>
      )}
    </Screen>
  );
}

function Stat({label, value}: {label: string; value: string}) {
  const c = useColors();
  return (
    <Card style={styles.stat}>
      <Body secondary style={styles.small}>
        {label}
      </Body>
      <Text style={[styles.statValue, {color: c.text}]}>{value}</Text>
    </Card>
  );
}

const styles = StyleSheet.create({
  small: {fontSize: 13},
  streak: {fontSize: 22, fontWeight: '800'},
  grid: {flexDirection: 'row', gap: 8},
  stat: {flex: 1, gap: 4},
  statValue: {fontSize: 18, fontWeight: '800'},
  levelCard: {alignItems: 'center', gap: 8},
  levelEmoji: {fontSize: 48},
  levelTitle: {fontSize: 20, fontWeight: '800'},
  points: {fontSize: 16, fontWeight: '700'},
  achieveTitle: {fontWeight: '700', fontSize: 17},
  achieveRow: {flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 6},
  achieveBadge: {fontSize: 24},
  achieveText: {flex: 1},
  achieveName: {fontWeight: '600'},
});
