import React from 'react';
import { Box, Text } from 'ink';

type HeaderProps = {
  title: string;
};

export const Header = ({ title }: HeaderProps) => {
  return (
    <Box flexDirection="column" marginBottom={1}>
      <Text color="gray">{title}</Text>
    </Box>
  );
};
