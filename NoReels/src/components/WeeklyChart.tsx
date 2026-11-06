import React, { useState } from 'react';
import { LayoutChangeEvent, View } from 'react-native';
import Svg, { Line, Rect, Text as SvgText } from 'react-native-svg';
import { DayUsage } from '../modules/NoReelsModule';
import { useColors } from '../theme/colors';

const LABELS = ['M', 'T', 'W', 'T', 'F', 'S', 'S'];
const HEIGHT = 180;
const TOP = 20;
const BOTTOM = 24;

/** Mon–Sun usage bars with a dashed daily-limit line. */
export function WeeklyChart({ days }: { days: DayUsage[] }) {
  const c = useColors();
  const [width, setWidth] = useState(0);
  const onLayout = (e: LayoutChangeEvent) => setWidth(e.nativeEvent.layout.width);

  const limit = Math.max(...days.map(d => d.limitSec), 60);
  const max = Math.max(limit, ...days.map(d => d.usedSec));
  const plotH = HEIGHT - TOP - BOTTOM;
  const slot = width / 7;
  const barW = Math.min(28, slot * 0.55);
  const y = (sec: number) => TOP + plotH - (sec / max) * plotH;

  return (
    <View onLayout={onLayout} accessibilityLabel="Daily usage this week">
      {width > 0 && (
        <Svg width={width} height={HEIGHT}>
          <Line
            x1={0}
            x2={width}
            y1={y(limit)}
            y2={y(limit)}
            stroke={c.textSecondary}
            strokeDasharray="4 4"
            strokeWidth={1}
          />
          {days.map((d, i) => {
            const x = i * slot + (slot - barW) / 2;
            const top = y(d.usedSec);
            const color = d.usedSec >= d.limitSec ? c.primary : d.isToday ? c.warning : c.success;
            return (
              <React.Fragment key={d.date}>
                {d.usedSec > 0 && (
                  <Rect
                    x={x}
                    y={top}
                    width={barW}
                    height={TOP + plotH - top}
                    rx={4}
                    fill={color}
                  />
                )}
                {d.usedSec > 0 && (
                  <SvgText
                    x={x + barW / 2}
                    y={top - 6}
                    fontSize={11}
                    fill={c.textSecondary}
                    textAnchor="middle">
                    {Math.round(d.usedSec / 60)}
                  </SvgText>
                )}
                <SvgText
                  x={x + barW / 2}
                  y={HEIGHT - 6}
                  fontSize={13}
                  fontWeight={d.isToday ? 'bold' : 'normal'}
                  fill={d.isToday ? c.text : c.textSecondary}
                  textAnchor="middle">
                  {LABELS[i]}
                </SvgText>
              </React.Fragment>
            );
          })}
        </Svg>
      )}
    </View>
  );
}
