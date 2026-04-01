export type AlgorithmKey = 'codt' | 'witty';

export type SelectedAlgorithms = {
  codt: boolean;
  witty: boolean;
};

export type CodtResult = {
  success: boolean;
  accuracy?: number;
  branchNodes?: number;
  runtimeMs?: number;
  summary: string;
};

export type WittyResult = {
  success: boolean;
  optimal?: boolean;
  treeSize?: number;
  runtimeMs?: number;
  summary: string;
};

export type PipelineResult = {
  selectedDataset: string;
  codt?: CodtResult;
  witty?: WittyResult;
  outputPath?: string;
};
