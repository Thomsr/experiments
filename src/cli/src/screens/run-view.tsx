import React, { useEffect, useMemo, useState } from 'react';
import { Box, Newline, Text, useInput } from 'ink';
import { Frame } from '../components/layout/frame.js';
import { Header } from '../components/layout/header.js';
import { runAlgorithmsSequentially } from '../services/run-pipeline.js';
import type {
  PipelineResult,
  SelectedAlgorithms,
} from '../types/run-results.js';

type RunViewProps = {
  dataset: string;
  onBack: () => void;
};

type RunState = 'setup' | 'running' | 'done' | 'error';

const METRIC_WIDTH = 6;
const VALUE_WIDTH = 6;
const REL_WIDTH = 3;

const relationSymbol = (
  codtValue: number | undefined,
  wittyValue: number | undefined,
): string => {
  if (codtValue === undefined || wittyValue === undefined) {
    return '?';
  }

  if (codtValue < wittyValue) {
    return '<';
  }

  if (codtValue > wittyValue) {
    return '>';
  }

  return '=';
};

const metricRow = (
  label: string,
  codtValue: number | undefined,
  wittyValue: number | undefined,
): string => {
  const codtText = codtValue !== undefined ? String(codtValue) : '-';
  const wittyText = wittyValue !== undefined ? String(wittyValue) : '-';
  const relation = relationSymbol(codtValue, wittyValue);

  return `${label.padEnd(METRIC_WIDTH)} ${codtText.padStart(VALUE_WIDTH)} ${relation.padStart(REL_WIDTH)} ${wittyText.padStart(VALUE_WIDTH)}`;
};

const tableHeader = `${'Metric'.padEnd(METRIC_WIDTH)} ${'CODT'.padStart(VALUE_WIDTH)} ${'Rel'.padStart(REL_WIDTH)} ${'Witty'.padStart(VALUE_WIDTH)}`;

const algorithmStateLabel = (enabled: boolean): string => {
  return enabled ? 'ENABLED ' : 'DISABLED';
};

export const RunView = ({ dataset, onBack }: RunViewProps) => {
  const [selected, setSelected] = useState<SelectedAlgorithms>({
    codt: true,
    witty: true,
  });
  const [setupIndex, setSetupIndex] = useState(0);
  const [resultActionIndex, setResultActionIndex] = useState(0);
  const [state, setState] = useState<RunState>('setup');
  const [elapsedMs, setElapsedMs] = useState(0);
  const [startedAt, setStartedAt] = useState<number | null>(null);
  const [result, setResult] = useState<PipelineResult | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    if (state !== 'running' || startedAt === null) {
      return;
    }

    const interval = setInterval(() => {
      setElapsedMs(Date.now() - startedAt);
    }, 100);

    return () => clearInterval(interval);
  }, [state, startedAt]);

  const runExperiment = async () => {
    if (!selected.codt && !selected.witty) {
      setErrorMessage('Select at least one algorithm before running.');
      setState('error');
      return;
    }

    setErrorMessage(null);
    setResult(null);
    setResultActionIndex(0);
    setElapsedMs(0);
    const start = Date.now();
    setStartedAt(start);
    setState('running');

    try {
      const runResult = await runAlgorithmsSequentially(dataset, selected);
      setResult(runResult);
      setElapsedMs(Date.now() - start);
      setState('done');
    } catch (error) {
      const message =
        error instanceof Error ? error.message : 'Unknown error while running.';
      setErrorMessage(message);
      setElapsedMs(Date.now() - start);
      setState('error');
    }
  };

  useInput((input, key) => {
    if (state === 'running') {
      return;
    }

    if (state === 'setup') {
      if (key.upArrow) {
        setSetupIndex((previous) => Math.max(0, previous - 1));
        return;
      }

      if (key.downArrow) {
        setSetupIndex((previous) => Math.min(3, previous + 1));
        return;
      }

      if (input === ' ') {
        if (setupIndex === 0) {
          setSelected((previous) => ({ ...previous, codt: !previous.codt }));
        }
        if (setupIndex === 1) {
          setSelected((previous) => ({ ...previous, witty: !previous.witty }));
        }
        return;
      }

      if (input === '\r') {
        if (setupIndex === 0) {
          setSelected((previous) => ({ ...previous, codt: !previous.codt }));
          return;
        }
        if (setupIndex === 1) {
          setSelected((previous) => ({ ...previous, witty: !previous.witty }));
          return;
        }
        if (setupIndex === 2) {
          void runExperiment();
          return;
        }
        if (setupIndex === 3) {
          onBack();
        }
      }
      return;
    }

    if (state === 'done' || state === 'error') {
      if (key.upArrow) {
        setResultActionIndex((previous) => Math.max(0, previous - 1));
        return;
      }

      if (key.downArrow) {
        setResultActionIndex((previous) => Math.min(1, previous + 1));
        return;
      }

      if (input === '\r') {
        if (resultActionIndex === 0) {
          void runExperiment();
          return;
        }
        onBack();
      }
    }
  });

  const elapsedSeconds = (elapsedMs / 1000).toFixed(1);

  const conclusion = useMemo(() => {
    if (!result?.codt || !result?.witty) {
      return null;
    }

    const sizeRow = metricRow(
      'Size',
      result.codt.branchNodes,
      result.witty.treeSize,
    );

    const timeRow = metricRow(
      'Time',
      result.codt.runtimeMs,
      result.witty.runtimeMs,
    );

    return { sizeRow, timeRow };
  }, [result]);

  return (
    <Frame>
      <Header title="Run Algorithms" />
      <Text color="gray">Dataset: {dataset}</Text>

      {state === 'setup' && (
        <Box flexDirection="column">
          <Text>Choose what to run:</Text>
          <Box>
            <Text color={setupIndex === 0 ? 'cyan' : undefined}>
              {setupIndex === 0 ? '>' : ' '}
            </Text>
            <Text> CODT </Text>
            <Text color={selected.codt ? 'green' : 'red'}>
              {algorithmStateLabel(selected.codt)}
            </Text>
          </Box>
          <Box>
            <Text color={setupIndex === 1 ? 'cyan' : undefined}>
              {setupIndex === 1 ? '>' : ' '}
            </Text>
            <Text> Witty </Text>
            <Text color={selected.witty ? 'green' : 'red'}>
              {algorithmStateLabel(selected.witty)}
            </Text>
          </Box>
          <Text color={setupIndex === 2 ? 'cyan' : undefined}>
            {setupIndex === 2 ? '>' : ' '} Run experiment
          </Text>
          <Text color={setupIndex === 3 ? 'cyan' : undefined}>
            {setupIndex === 3 ? '>' : ' '} Back
          </Text>

          <Text color="yellow">
            Arrow keys to move, Enter to toggle/select.
          </Text>
        </Box>
      )}

      {state === 'running' && (
        <Box flexDirection="column">
          <Text color="cyan">Running experiment...</Text>
          <Text color="greenBright">Total elapsed: {elapsedSeconds}s</Text>
          <Text color="gray">Please wait for completion.</Text>
        </Box>
      )}

      {state === 'done' && result && (
        <Box flexDirection="column">
          <Text color="green">Run finished in {elapsedSeconds}s.</Text>

          {result.codt && (
            <Text>
              CODT: {result.codt.summary}
              {result.codt.runtimeMs !== undefined
                ? ` runtime ${result.codt.runtimeMs} ms.`
                : ''}
            </Text>
          )}
          {result.witty && (
            <Text>
              Witty: {result.witty.summary}
              {result.witty.runtimeMs !== undefined
                ? ` runtime ${result.witty.runtimeMs} ms.`
                : ''}
            </Text>
          )}
          {conclusion && (
            <>
              <Text color="cyan">Conclusion (lower is better):</Text>
              <Text>{tableHeader}</Text>
              <Text>{conclusion.sizeRow}</Text>
              <Text>{conclusion.timeRow}</Text>
            </>
          )}

          <Text color={resultActionIndex === 0 ? 'cyan' : undefined}>
            {resultActionIndex === 0 ? '>' : ' '} Run experiment again
          </Text>
          <Text color={resultActionIndex === 1 ? 'cyan' : undefined}>
            {resultActionIndex === 1 ? '>' : ' '} Back
          </Text>
          <Text color="yellow">Arrow keys to move, Enter to select.</Text>
        </Box>
      )}

      {state === 'error' && (
        <Box flexDirection="column">
          <Text color="red">Run failed.</Text>
          {errorMessage && <Text>{errorMessage}</Text>}

          <Text color={resultActionIndex === 0 ? 'cyan' : undefined}>
            {resultActionIndex === 0 ? '>' : ' '} Run experiment again
          </Text>
          <Text color={resultActionIndex === 1 ? 'cyan' : undefined}>
            {resultActionIndex === 1 ? '>' : ' '} Back
          </Text>
          <Text color="yellow">Arrow keys to move, Enter to select.</Text>
        </Box>
      )}
    </Frame>
  );
};
