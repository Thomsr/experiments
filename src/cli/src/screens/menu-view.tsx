import React from 'react';
import { Newline, Text } from 'ink';
import SelectInput from 'ink-select-input';
import { menuItems } from '../constants/menu.js';
import type { MenuValue } from '../types/navigation.js';
import { Frame } from '../components/layout/frame.js';
import { Header } from '../components/layout/header.js';

type MenuViewProps = {
  title: string;
  onSelect: (value: MenuValue) => void;
};

export const MenuView = ({ title, onSelect }: MenuViewProps) => {
  return (
    <Frame>
      <Header title={title} />
      <Text color="gray">Choose an action:</Text>
      <Newline />
      <SelectInput
        items={menuItems}
        onSelect={(item) => onSelect(item.value)}
      />
      <Newline />
      <Text color="gray">Arrow keys to move, Enter to select.</Text>
    </Frame>
  );
};
