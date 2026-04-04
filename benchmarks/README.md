# CODT vs Witty benchmark

This folder contains a simple benchmark harness to compare CODT and Witty on sampled datasets from `algorithms/codt/data/sampled`.

## What it does

- selects sampled CODT datasets (ordered by CODT difficulty list by default, configurable count)
- runs CODT CLI and Witty once per dataset with the same timeout
- reuses cached Witty results across runs by default
- uses strict solved semantics:
  - CODT: process finished and did not print "No perfect tree exists for the given data and constraints."
  - Witty: `foundTree == true` and `timeout == false` from Witty output columns
- writes normalized CSV rows (one row per solver per dataset)
- plots a cactus curve (solved instances vs runtime)

## Prerequisites

- CODT release binary built at `target/release/codt-cli`
  - from `algorithms/codt`: `cargo build --release`
- Witty jar built (auto-discovered) or provided via `--witty-jar`
  - from `algorithms/witty/Code`: `./gradlew jar`
- Java available in PATH
- Python with matplotlib (`pip install matplotlib`)

## Run benchmark

From the repository root (`experiments/`):

```bash
python benchmarks/run_codt_vs_witty.py --dataset-count 40 --timeout-sec 300
```

Useful options:

- `--dataset-filter appendicitis_0.2` only run matching sampled files
- `--dataset-order difficulty` (default) runs easiest to hardest using CODT ranking
- `--dataset-order lexical` switches back to filename ordering
- `--difficulty-file algorithms/codt/crates/codt/tests/dataset-by-difficulty.rs` explicit ranking file
- `--witty-jar /path/to/Code-1.0-SNAPSHOT-all.jar` explicit Witty jar path
- `--witty-cache benchmarks/cache/witty_results_cache.csv` cache location
- `--recompute-witty` ignore cache and force rerun of Witty
- `--output-csv benchmarks/out/my_results.csv`
- `--keep-temp` keep temporary converted Witty inputs and raw Witty output files

Defaults expect datasets in `algorithms/codt/data/sampled` and CODT binary in `algorithms/codt/target/release/codt-cli`.

Output CSV default:

- `benchmarks/out/codt_vs_witty_results.csv`

## Plot CODT vs Witty Time Scatter

```bash
python benchmarks/plot_solved_vs_time.py \
  --input-csv benchmarks/out/codt_vs_witty_results.csv \
  --output benchmarks/out/codt_vs_witty_scatter.png \
  --timeout-sec 300
```

The plot is a scatter plot over datasets solved strictly by both solvers:

- x-axis: Witty solve time (seconds)
- y-axis: CODT solve time (seconds)
