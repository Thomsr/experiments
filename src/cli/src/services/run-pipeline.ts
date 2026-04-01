import { spawn } from 'node:child_process';
import { promises as fs } from 'node:fs';
import { existsSync } from 'node:fs';
import path from 'node:path';
import type {
  CodtResult,
  PipelineResult,
  SelectedAlgorithms,
  WittyResult,
} from '../types/run-results.js';

const findRepoRoot = (startDir: string): string => {
  let current = startDir;

  for (let i = 0; i < 6; i += 1) {
    const hasAlgorithms = path.join(current, 'algorithms');
    const hasData = path.join(current, 'data');
    if (existsSync(hasAlgorithms) && existsSync(hasData)) {
      return current;
    }
    current = path.dirname(current);
  }

  throw new Error(
    'Could not determine repository root from current working directory.',
  );
};

const runCommand = (
  command: string,
  args: string[],
  cwd: string,
): Promise<{ exitCode: number; output: string }> => {
  return new Promise((resolve, reject) => {
    const child = spawn(command, args, {
      cwd,
      stdio: ['ignore', 'pipe', 'pipe'],
    });

    let output = '';

    child.stdout.on('data', (chunk) => {
      output += chunk.toString();
    });

    child.stderr.on('data', (chunk) => {
      output += chunk.toString();
    });

    child.on('error', (error) => {
      reject(error);
    });

    child.on('close', (code) => {
      resolve({ exitCode: code ?? 1, output });
    });
  });
};

const parseCodtResult = (output: string, success: boolean): CodtResult => {
  const accuracyMatch = output.match(/Accuracy:\s*([0-9.]+)%/);
  const branchNodesMatch = output.match(/Branch nodes:\s*(\d+)/);
  const solveTimeMatch = output.match(/Solve time \(s\):\s*([0-9.]+)/);

  const accuracy = accuracyMatch
    ? Number.parseFloat(accuracyMatch[1])
    : undefined;
  const branchNodes = branchNodesMatch
    ? Number.parseInt(branchNodesMatch[1], 10)
    : undefined;
  const runtimeMs = solveTimeMatch
    ? Math.round(Number.parseFloat(solveTimeMatch[1]) * 1000)
    : undefined;

  const summary = success
    ? `CODT finished${
        accuracy !== undefined ? `, accuracy ${accuracy}%` : ''
      }${branchNodes !== undefined ? `, branch nodes ${branchNodes}` : ''}.`
    : 'CODT failed. Check output log.';

  return {
    success,
    accuracy,
    branchNodes,
    runtimeMs,
    summary,
  };
};

const parseWittyResult = (line: string, success: boolean): WittyResult => {
  const fields = line.split(';');
  const runtimeMs = fields[9] ? Number.parseInt(fields[9], 10) : undefined;
  const optimal = fields[12] ? fields[12] === 'true' : undefined;
  const treeSizeRaw = fields[13] ? Number.parseInt(fields[13], 10) : undefined;
  const treeSize =
    treeSizeRaw !== undefined && treeSizeRaw >= 0 ? treeSizeRaw : undefined;

  const summary = success
    ? `Witty finished${
        runtimeMs !== undefined ? `, runtime ${runtimeMs} ms` : ''
      }${treeSize !== undefined ? `, tree size ${treeSize}` : ''}${
        optimal !== undefined ? `, optimal ${optimal}` : ''
      }.`
    : 'Witty failed. Check output log.';

  return {
    success,
    optimal,
    treeSize,
    runtimeMs,
    summary,
  };
};

const buildCodtInput = async (
  csvPath: string,
  codtInputPath: string,
): Promise<void> => {
  const csv = await fs.readFile(csvPath, 'utf8');
  const lines = csv
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line.length > 0);

  if (lines.length <= 1) {
    throw new Error('Sampled dataset is empty or missing rows.');
  }

  const transformed = lines
    .slice(1)
    .map((line) => {
      const parts = line.split(',');
      if (parts.length < 2) {
        throw new Error('Invalid CSV row while preparing CODT input.');
      }
      const label = parts[parts.length - 1];
      const features = parts.slice(0, parts.length - 1);
      return [label, ...features].join(' ');
    })
    .join('\n');

  await fs.writeFile(codtInputPath, `${transformed}\n`, 'utf8');
};

export const runAlgorithmsSequentially = async (
  datasetFile: string,
  selected: SelectedAlgorithms,
): Promise<PipelineResult> => {
  const repoRoot = findRepoRoot(process.cwd());
  const sampledDir = path.join(repoRoot, 'data', 'normal', 'sampled');
  const inputCsv = path.join(sampledDir, datasetFile);
  const runsDir = path.join(repoRoot, 'src', 'cli', '.runs');

  await fs.mkdir(runsDir, { recursive: true });
  await fs.access(inputCsv);

  const result: PipelineResult = {
    selectedDataset: datasetFile,
  };

  if (selected.codt) {
    const codtInput = path.join(
      runsDir,
      `${datasetFile.replace('.csv', '')}_codt.txt`,
    );
    await buildCodtInput(inputCsv, codtInput);

    const codtRun = await runCommand(
      'cargo',
      ['run', '--release', '--', '-f', codtInput, '-t', '30'],
      path.join(repoRoot, 'algorithms', 'codt'),
    );

    result.codt = parseCodtResult(codtRun.output, codtRun.exitCode === 0);
  }

  if (selected.witty) {
    const wittyOutput = path.join(
      runsDir,
      `witty_${datasetFile.replace('.csv', '')}.csv`,
    );

    const wittyArgs = `${sampledDir} ${datasetFile} ${wittyOutput} 0.2 0 1000 30 101 2 1000 0`;

    const wittyRun = await runCommand(
      'bash',
      ['./gradlew', 'run', `--args=${wittyArgs}`],
      path.join(repoRoot, 'algorithms', 'witty', 'Code'),
    );

    if (wittyRun.exitCode === 0) {
      const output = await fs.readFile(wittyOutput, 'utf8');
      const lines = output
        .split(/\r?\n/)
        .map((line) => line.trim())
        .filter((line) => line.length > 0);
      const lastLine = lines[lines.length - 1] ?? '';
      result.witty = parseWittyResult(lastLine, true);
      result.outputPath = wittyOutput;
    } else {
      result.witty = parseWittyResult('', false);
    }
  }

  return result;
};
