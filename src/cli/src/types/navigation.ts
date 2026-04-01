export type Screen = 'menu' | 'run' | 'config';

export type MenuValue = Exclude<Screen, 'menu'> | 'quit';

export type MenuItem = {
  label: string;
  value: MenuValue;
};
