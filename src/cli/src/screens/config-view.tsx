import React, { useEffect, useState } from 'react';
import { Newline, Text } from 'ink';
import SelectInput from 'ink-select-input';
import TextInput from 'ink-text-input';
import { datasets } from '../constants/datasets.js';
import { Frame } from '../components/layout/frame.js';
import { Header } from '../components/layout/header.js';
import { useBackKey } from '../hooks/use-back-key.js';

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
  const [typed, setTyped] = useState(dataset);

  useBackKey(onBack);

  useEffect(() => {
    setTyped(dataset);
  }, [dataset]);

  return (
    <Frame>
      <Header title="Dataset Configuration" />
      <Text>Pick a preset:</Text>
      <SelectInput
        items={datasets.map((name) => ({ label: name, value: name }))}
        onSelect={(item) => {
          onDatasetChange(item.value);
          setTyped(item.value);
        }}
      />
      <Newline />
      <Text>Or type custom path:</Text>
      <TextInput
        value={typed}
        onChange={setTyped}
        onSubmit={(value) => onDatasetChange(value.trim() || dataset)}
      />
      <Newline />
      <Text color="green">Current dataset: {dataset}</Text>
      <Text color="yellow">Press b to go back.</Text>
    </Frame>
  );
};
