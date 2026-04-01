import React, { useMemo, useState } from 'react';
import { useApp } from 'ink';
import { ConfigView } from './screens/config-view.js';
import { MenuView } from './screens/menu-view.js';
import { RunView } from './screens/run-view.js';
import type { MenuValue, Screen } from './types/navigation.js';

export const App = () => {
  const { exit } = useApp();
  const [screen, setScreen] = useState<Screen>('menu');
  const [dataset, setDataset] = useState('appendicitis_0.2_0.csv');

  const title = useMemo(() => `Sampled dataset: ${dataset}`, [dataset]);

  const handleMenuSelect = (value: MenuValue) => {
    if (value === 'quit') {
      exit();
      return;
    }

    setScreen(value);
  };

  if (screen === 'run') {
    return <RunView dataset={dataset} onBack={() => setScreen('menu')} />;
  }

  if (screen === 'config') {
    return (
      <ConfigView
        dataset={dataset}
        onDatasetChange={setDataset}
        onBack={() => setScreen('menu')}
      />
    );
  }

  return <MenuView title={title} onSelect={handleMenuSelect} />;
};
