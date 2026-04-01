import React from 'react';
import { Box } from 'ink';

type FrameProps = {
  children: React.ReactNode;
};

export const Frame = ({ children }: FrameProps) => {
  return (
    <Box
      borderStyle="round"
      borderColor="cyan"
      paddingX={2}
      paddingY={1}
      flexDirection="column"
      width={80}
    >
      {children}
    </Box>
  );
};
