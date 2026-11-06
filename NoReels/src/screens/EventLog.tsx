import React from 'react';
import { StyleSheet } from 'react-native';
import { Body, Card, Screen } from '../components/ui';
import { usePolling } from '../hooks/usePolling';
import { NoReels } from '../modules/NoReelsModule';

export function EventLog() {
  const log = usePolling(NoReels.getEventLog, 10000);

  return (
    <Screen inset={false}>
      <Body secondary>
        Uncertain detections are never counted against your budget — they're
        listed here so you can tell when an app update broke detection. Strict
        Mode exits show the reason you picked.
      </Body>
      {log?.length === 0 && <Body secondary>Nothing logged yet.</Body>}
      {log?.map(entry => (
        <Card key={`${entry.ts}-${entry.kind}`} style={styles.card}>
          <Body style={styles.title}>
            {entry.kind === 'uncertain'
              ? `❓ Uncertain · ${entry.app}`
              : `🚪 Strict exit · ${entry.app}`}
          </Body>
          <Body secondary>{entry.detail}</Body>
          <Body secondary style={styles.time}>
            {new Date(entry.ts).toLocaleString()}
          </Body>
        </Card>
      ))}
    </Screen>
  );
}

const styles = StyleSheet.create({
  card: { gap: 4 },
  title: { fontWeight: '700' },
  time: { fontSize: 12 },
});
