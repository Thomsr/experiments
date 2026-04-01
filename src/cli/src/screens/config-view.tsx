import React from 'react';
import { Newline, Text } from 'ink';
import SelectInput from 'ink-select-input';
import { datasets } from '../constants/datasets.js';
import { Frame } from '../components/layout/frame.js';
import { Header } from '../components/layout/header.js';

type ConfigActionValue = { type: 'dataset'; value: string } | { type: 'back' };

type ConfigViewProps = {
  dataset: string;
  onDatasetChange: (dataset: string) => void;
  onBack: () => void;
};

export const ConfigView = ({
  dataset,
  onDatasetChange,
  onBack,
}: ConfigViewProps) => {
  const items: Array<{ label: string; value: ConfigActionValue }> = [
    ...datasets.map((name) => ({
      label: `${name}${name === dataset ? ' (current)' : ''}`,
      value: { type: 'dataset', value: name } as ConfigActionValue,
    })),
    { label: 'Back', value: { type: 'back' } },
  ];

  return (
    <Frame>
      <Header title="Dataset Configuration" />
      <Text>Select sampled dataset:</Text>
      <SelectInput
        items={items}
        onSelect={(item) => {
          if (item.value.type === 'back') {
            onBack();
            return;
          }

          onDatasetChange(item.value.value);
        }}
      />
      <Text color="yellow">Arrow keys to move, Enter to select.</Text>
    </Frame>
  );
};
