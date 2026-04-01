import React, { useEffect, useState } from 'react';
import { Newline, Text } from 'ink';
import { Frame } from '../components/layout/frame.js';
import { Header } from '../components/layout/header.js';
import { useBackKey } from '../hooks/use-back-key.js';

type RunViewProps = {
  onBack: () => void;
};

export const RunView = ({ onBack }: RunViewProps) => {
  const [progress, setProgress] = useState(0);

  useBackKey(onBack);

  useEffect(() => {
    const interval = setInterval(() => {
      setProgress((previous) => {
        if (previous >= 100) {
          clearInterval(interval);
          return 100;
        }

        const next = previous + Math.floor(Math.random() * 13 + 4);
        return next > 100 ? 100 : next;
      });
    }, 170);

    return () => clearInterval(interval);
  }, []);

  const filled = Math.round(progress / 5);
  const bar = `[${'#'.repeat(filled)}${'-'.repeat(20 - filled)}]`;

  return (
    <Frame>
      <Header title="Benchmark Simulation" />
      <Text>Launching algorithm suite against selected dataset...</Text>
      <Newline />
      <Text color="greenBright">
        {bar} {progress}%
      </Text>
      <Newline />
      {progress === 100 ? (
        <Text color="green">Done. Press b to go back to menu.</Text>
      ) : (
        <Text color="yellow">Running. Press b to stop and go back.</Text>
      )}
    </Frame>
  );
};
