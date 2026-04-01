import { useInput } from 'ink';

export const useBackKey = (onBack: () => void) => {
  useInput((input) => {
    if (input.toLowerCase() === 'b') {
      onBack();
    }
  });
};
