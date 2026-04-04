#!/usr/bin/env python3
"""Run CODT and Witty on sampled CODT datasets and save normalized benchmark results."""

from __future__ import annotations

import argparse
import csv
import re
import shutil
import subprocess
import sys
import tempfile
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Optional


WITTY_ALGO_ID = 5


@dataclass
class ProcessRun:
    returncode: Optional[int]
    stdout: str
    stderr: str
    elapsed_sec: float
    timed_out: bool
    error_message: str


@dataclass
class SolverRecord:
    solver: str
    dataset_id: str
    runtime_sec: float
    solved_strict: bool
    timeout: bool
    tree_size: Optional[int]
    return_code: Optional[int]
    error_message: str


@dataclass(frozen=True)
class WittyCacheKey:
    dataset_id: str
    timeout_sec: int
    algo_id: int
    jar_signature: str


def parse_args() -> argparse.Namespace:
    script_dir = Path(__file__).resolve().parent
    experiments_root = script_dir.parent
    codt_root = experiments_root / "algorithms" / "codt"
    parser = argparse.ArgumentParser(description=__doc__)

    parser.add_argument(
        "--sampled-dir",
        type=Path,
        default=codt_root / "data" / "sampled",
        help="Directory containing sampled CODT datasets (.txt).",
    )
    parser.add_argument(
        "--dataset-count",
        type=int,
        default=20,
        help="Number of sampled datasets to run (sorted order).",
    )
    parser.add_argument(
        "--dataset-filter",
        type=str,
        default="",
        help="Optional substring filter for dataset filenames.",
    )
    parser.add_argument(
        "--dataset-order",
        type=str,
        choices=["difficulty", "lexical"],
        default="difficulty",
        help="Dataset ordering policy before applying --dataset-count.",
    )
    parser.add_argument(
        "--difficulty-file",
        type=Path,
        default=codt_root / "crates" / "codt" / "tests" / "dataset-by-difficulty.rs",
        help="Path to CODT difficulty ranking list (used when --dataset-order difficulty).",
    )
    parser.add_argument(
        "--timeout-sec",
        type=int,
        default=300,
        help="Per-instance timeout in seconds used for both solvers.",
    )

    parser.add_argument(
        "--codt-bin",
        type=Path,
        default=codt_root / "target" / "release" / "codt-cli",
        help="Path to CODT CLI binary.",
    )
    parser.add_argument(
        "--codt-strategy",
        type=str,
        default="bfs-balance-small-lb",
        help="Search strategy passed to CODT CLI.",
    )
    parser.add_argument(
        "--codt-upperbound",
        type=str,
        default="for-remaining-interval",
        help="Upper bound strategy passed to CODT CLI.",
    )

    parser.add_argument(
        "--witty-jar",
        type=Path,
        default=None,
        help="Path to Witty jar. If omitted, an automatic search is used.",
    )
    parser.add_argument(
        "--java-bin",
        type=str,
        default="java",
        help="Java executable used to run Witty.",
    )
    parser.add_argument(
        "--witty-cache",
        type=Path,
        default=script_dir / "cache" / "witty_results_cache.csv",
        help="Persistent CSV cache for Witty results.",
    )
    parser.add_argument(
        "--recompute-witty",
        action="store_true",
        help="Ignore cached Witty entries and recompute Witty runs.",
    )

    parser.add_argument(
        "--output-csv",
        type=Path,
        default=script_dir / "out" / "codt_vs_witty_results.csv",
        help="Output CSV with normalized benchmark rows.",
    )
    parser.add_argument(
        "--keep-temp",
        action="store_true",
        help="Keep temporary converted Witty datasets and raw outputs.",
    )

    return parser.parse_args()


def discover_witty_jar(codt_root: Path) -> Path:
    witty_root = codt_root.parent / "witty"
    candidates = []
    candidates.extend((witty_root / "Code" / "build" / "libs").glob("*.jar"))
    candidates.extend((witty_root / "Code").glob("*.jar"))
    candidates.extend(witty_root.glob("*.jar"))
    for candidate in candidates:
        if candidate.is_file():
            return candidate
    raise FileNotFoundError(
        "Could not find a Witty jar automatically. Build Witty first or pass --witty-jar."
    )


def load_difficulty_order(difficulty_file: Path) -> list[str]:
    if not difficulty_file.is_file():
        raise FileNotFoundError(f"Difficulty file not found: {difficulty_file}")

    ranking: list[str] = []
    for line in difficulty_file.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line.startswith('"'):
            continue
        match = re.match(r'"([^"]+\.csv)"\s*,?$', line)
        if not match:
            continue
        ranking.append(Path(match.group(1)).stem)

    if not ranking:
        raise RuntimeError(f"Could not parse any entries from difficulty file: {difficulty_file}")

    return ranking


def list_datasets(
    sampled_dir: Path,
    count: int,
    dataset_filter: str,
    dataset_order: str,
    difficulty_file: Path,
) -> list[Path]:
    if not sampled_dir.is_dir():
        raise FileNotFoundError(f"Sampled dataset directory not found: {sampled_dir}")

    all_files = sorted(p for p in sampled_dir.glob("*.txt") if p.is_file())
    if dataset_filter:
        all_files = [p for p in all_files if dataset_filter in p.name]

    if not all_files:
        raise RuntimeError("No sampled datasets matched the selected criteria.")

    if dataset_order == "difficulty":
        ranking = load_difficulty_order(difficulty_file)
        rank_by_stem = {stem: idx for idx, stem in enumerate(ranking)}
        fallback_rank = len(rank_by_stem)
        all_files.sort(key=lambda p: (rank_by_stem.get(p.stem, fallback_rank), p.name))

    return all_files[:count]


def run_process(cmd: list[str], cwd: Optional[Path], timeout_sec: int) -> ProcessRun:
    started = time.perf_counter()
    try:
        proc = subprocess.run(
            cmd,
            cwd=str(cwd) if cwd else None,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            timeout=timeout_sec,
            check=False,
        )
        elapsed = time.perf_counter() - started
        return ProcessRun(
            returncode=proc.returncode,
            stdout=proc.stdout,
            stderr=proc.stderr,
            elapsed_sec=elapsed,
            timed_out=False,
            error_message="",
        )
    except subprocess.TimeoutExpired as exc:
        elapsed = time.perf_counter() - started
        return ProcessRun(
            returncode=None,
            stdout=exc.stdout or "",
            stderr=exc.stderr or "",
            elapsed_sec=elapsed,
            timed_out=True,
            error_message=f"Process timeout after {timeout_sec}s",
        )
    except Exception as exc:  # pylint: disable=broad-except
        elapsed = time.perf_counter() - started
        return ProcessRun(
            returncode=None,
            stdout="",
            stderr="",
            elapsed_sec=elapsed,
            timed_out=False,
            error_message=str(exc),
        )


def bool_to_str(v: bool) -> str:
    return "true" if v else "false"


def str_to_bool(v: str) -> bool:
    return v.strip().lower() == "true"


def get_witty_jar_signature(witty_jar: Path) -> str:
    stat = witty_jar.stat()
    return f"{witty_jar.resolve()}::{stat.st_size}::{stat.st_mtime_ns}"


def _parse_optional_int(raw: str) -> Optional[int]:
    value = raw.strip()
    if value == "":
        return None
    return int(value)


def load_witty_cache(cache_path: Path) -> dict[WittyCacheKey, SolverRecord]:
    if not cache_path.is_file():
        return {}

    cache: dict[WittyCacheKey, SolverRecord] = {}
    with cache_path.open("r", encoding="utf-8", newline="") as fin:
        reader = csv.DictReader(fin)
        expected = {
            "dataset_id",
            "timeout_sec",
            "algo_id",
            "jar_signature",
            "runtime_sec",
            "solved_strict",
            "timeout",
            "tree_size",
            "return_code",
            "error_message",
        }
        if not expected.issubset(set(reader.fieldnames or [])):
            return {}

        for row in reader:
            key = WittyCacheKey(
                dataset_id=row["dataset_id"],
                timeout_sec=int(row["timeout_sec"]),
                algo_id=int(row["algo_id"]),
                jar_signature=row["jar_signature"],
            )
            cache[key] = SolverRecord(
                solver="witty",
                dataset_id=row["dataset_id"],
                runtime_sec=float(row["runtime_sec"]),
                solved_strict=str_to_bool(row["solved_strict"]),
                timeout=str_to_bool(row["timeout"]),
                tree_size=_parse_optional_int(row["tree_size"]),
                return_code=_parse_optional_int(row["return_code"]),
                error_message=row["error_message"],
            )

    return cache


def append_witty_cache_entry(
    cache_path: Path,
    key: WittyCacheKey,
    record: SolverRecord,
) -> None:
    cache_path.parent.mkdir(parents=True, exist_ok=True)
    file_exists = cache_path.is_file()
    with cache_path.open("a", encoding="utf-8", newline="") as fout:
        writer = csv.DictWriter(
            fout,
            fieldnames=[
                "dataset_id",
                "timeout_sec",
                "algo_id",
                "jar_signature",
                "runtime_sec",
                "solved_strict",
                "timeout",
                "tree_size",
                "return_code",
                "error_message",
            ],
        )
        if not file_exists:
            writer.writeheader()
        writer.writerow(
            {
                "dataset_id": key.dataset_id,
                "timeout_sec": key.timeout_sec,
                "algo_id": key.algo_id,
                "jar_signature": key.jar_signature,
                "runtime_sec": f"{record.runtime_sec:.6f}",
                "solved_strict": bool_to_str(record.solved_strict),
                "timeout": bool_to_str(record.timeout),
                "tree_size": "" if record.tree_size is None else record.tree_size,
                "return_code": "" if record.return_code is None else record.return_code,
                "error_message": record.error_message,
            }
        )


def extract_codt_tree_size(stdout: str) -> Optional[int]:
    # Expected shape: "Misclassifications: 0. Branch nodes: 7. Accuracy: ..."
    match = re.search(r"Branch nodes:\s*(\d+)", stdout)
    if match:
        return int(match.group(1))
    return None


def run_codt(dataset_file: Path, args: argparse.Namespace) -> SolverRecord:
    cmd = [
        str(args.codt_bin),
        "-f",
        str(dataset_file),
        "-t",
        str(args.timeout_sec),
        "-s",
        args.codt_strategy,
        "-u",
        args.codt_upperbound,
    ]
    result = run_process(cmd, cwd=None, timeout_sec=args.timeout_sec + 10)

    no_perfect = "No perfect tree exists for the given data and constraints." in result.stdout
    solved = (
        not result.timed_out
        and result.returncode == 0
        and not no_perfect
        and bool(result.stdout.strip())
    )

    return SolverRecord(
        solver="codt",
        dataset_id=dataset_file.stem,
        runtime_sec=result.elapsed_sec,
        solved_strict=solved,
        timeout=result.timed_out,
        tree_size=extract_codt_tree_size(result.stdout),
        return_code=result.returncode,
        error_message=result.error_message,
    )


def convert_codt_txt_to_witty_csv(src_txt: Path, dst_csv: Path) -> None:
    lines_out = []
    feature_count = None

    with src_txt.open("r", encoding="utf-8") as fin:
        for raw in fin:
            stripped = raw.strip()
            if not stripped:
                continue
            parts = stripped.split()
            if len(parts) < 2:
                raise ValueError(f"Malformed dataset row in {src_txt}: {raw!r}")
            label = parts[0]
            features = parts[1:]
            if feature_count is None:
                feature_count = len(features)
            elif len(features) != feature_count:
                raise ValueError(f"Inconsistent feature count in {src_txt}")

            lines_out.append(",".join(features + [label]))

    if feature_count is None:
        raise ValueError(f"Empty dataset file: {src_txt}")

    header = ",".join([f"d{i}" for i in range(1, feature_count + 1)] + ["class"])

    dst_csv.parent.mkdir(parents=True, exist_ok=True)
    with dst_csv.open("w", encoding="utf-8", newline="") as fout:
        fout.write(header + "\n")
        for row in lines_out:
            fout.write(row + "\n")


def parse_witty_output_line(output_line: str) -> tuple[Optional[float], bool, bool, Optional[int]]:
    # 0-based fields according to Witty output format:
    # [9]=runtime_ms [11]=timeout [12]=found_tree [13]=tree_size
    parts = output_line.split(";")
    if len(parts) < 14:
        raise ValueError("Witty output line has too few fields")

    runtime_sec = float(parts[9]) / 1000.0
    timeout = parts[11].strip().lower() == "true"
    found_tree = parts[12].strip().lower() == "true"

    size_raw = int(parts[13])
    tree_size = size_raw if size_raw >= 0 else None

    return runtime_sec, timeout, found_tree, tree_size


def run_witty(
    dataset_txt: Path,
    converted_dir: Path,
    witty_out_dir: Path,
    args: argparse.Namespace,
    witty_jar: Path,
    problem_id: int,
) -> SolverRecord:
    converted_csv = converted_dir / f"{dataset_txt.stem}.csv"
    convert_codt_txt_to_witty_csv(dataset_txt, converted_csv)

    output_file = witty_out_dir / f"{dataset_txt.stem}.out"
    output_file.parent.mkdir(parents=True, exist_ok=True)

    cmd = [
        args.java_bin,
        "-jar",
        str(witty_jar),
        str(converted_dir),
        converted_csv.name,
        str(output_file),
        "1.0",  # informational only
        "0",  # informational only
        "0",  # max size (ignored for algorithm id 5 / strategy1)
        str(args.timeout_sec),
        str(problem_id),
        str(WITTY_ALGO_ID),
        "999999",  # upper bound placeholder
        "0",  # upper bound time placeholder
    ]
    result = run_process(cmd, cwd=None, timeout_sec=args.timeout_sec + 30)

    runtime_sec = result.elapsed_sec
    timeout = result.timed_out
    solved = False
    tree_size: Optional[int] = None
    error_message = result.error_message

    if result.returncode == 0 and not result.timed_out and output_file.exists():
        lines = [l.strip() for l in output_file.read_text(encoding="utf-8").splitlines() if l.strip()]
        if lines:
            try:
                parsed_runtime, timeout_flag, found_tree, parsed_tree_size = parse_witty_output_line(lines[-1])
                runtime_sec = parsed_runtime
                timeout = timeout_flag
                solved = found_tree and not timeout_flag
                tree_size = parsed_tree_size
            except Exception as exc:  # pylint: disable=broad-except
                error_message = f"Failed parsing Witty output: {exc}"
        else:
            error_message = "Witty output file was empty"
    elif result.returncode != 0 and not error_message:
        error_message = "Witty process exited with non-zero status"

    return SolverRecord(
        solver="witty",
        dataset_id=dataset_txt.stem,
        runtime_sec=runtime_sec,
        solved_strict=solved,
        timeout=timeout,
        tree_size=tree_size,
        return_code=result.returncode,
        error_message=error_message,
    )


def save_results(output_csv: Path, rows: list[SolverRecord]) -> None:
    output_csv.parent.mkdir(parents=True, exist_ok=True)
    with output_csv.open("w", encoding="utf-8", newline="") as fout:
        writer = csv.DictWriter(
            fout,
            fieldnames=[
                "solver",
                "dataset_id",
                "runtime_sec",
                "solved_strict",
                "timeout",
                "tree_size",
                "return_code",
                "error_message",
            ],
        )
        writer.writeheader()
        for r in rows:
            writer.writerow(
                {
                    "solver": r.solver,
                    "dataset_id": r.dataset_id,
                    "runtime_sec": f"{r.runtime_sec:.6f}",
                    "solved_strict": bool_to_str(r.solved_strict),
                    "timeout": bool_to_str(r.timeout),
                    "tree_size": "" if r.tree_size is None else r.tree_size,
                    "return_code": "" if r.return_code is None else r.return_code,
                    "error_message": r.error_message,
                }
            )


def check_prerequisites(args: argparse.Namespace, witty_jar: Path) -> None:
    if shutil.which(args.java_bin) is None:
        raise RuntimeError(f"Java executable not found: {args.java_bin}")

    if not args.codt_bin.is_file():
        raise FileNotFoundError(
            f"CODT binary not found: {args.codt_bin}. Build it with cargo build --release."
        )

    if not witty_jar.is_file():
        raise FileNotFoundError(f"Witty jar not found: {witty_jar}")


def main() -> int:
    args = parse_args()
    codt_root = Path(__file__).resolve().parent.parent / "algorithms" / "codt"

    witty_jar = args.witty_jar if args.witty_jar else discover_witty_jar(codt_root)
    check_prerequisites(args, witty_jar)
    jar_signature = get_witty_jar_signature(witty_jar)
    witty_cache = load_witty_cache(args.witty_cache)

    datasets = list_datasets(
        args.sampled_dir,
        args.dataset_count,
        args.dataset_filter,
        args.dataset_order,
        args.difficulty_file,
    )
    print(f"Running {len(datasets)} sampled datasets")
    print(f"CODT binary: {args.codt_bin}")
    print(f"Witty jar:   {witty_jar}")
    print(f"Dataset order: {args.dataset_order}")
    print(f"Witty cache: {args.witty_cache}")

    base_temp = Path(tempfile.mkdtemp(prefix="codt-vs-witty-"))
    converted_dir = base_temp / "witty_input"
    witty_out_dir = base_temp / "witty_output"

    rows: list[SolverRecord] = []
    try:
        for index, dataset in enumerate(datasets, start=1):
            print(f"[{index}/{len(datasets)}] {dataset.name}")
            codt_row = run_codt(dataset, args)

            cache_key = WittyCacheKey(
                dataset_id=dataset.stem,
                timeout_sec=args.timeout_sec,
                algo_id=WITTY_ALGO_ID,
                jar_signature=jar_signature,
            )
            witty_row: SolverRecord
            if not args.recompute_witty and cache_key in witty_cache:
                witty_row = witty_cache[cache_key]
                print("  witty: cache-hit")
            else:
                witty_row = run_witty(dataset, converted_dir, witty_out_dir, args, witty_jar, index)
                witty_cache[cache_key] = witty_row
                append_witty_cache_entry(args.witty_cache, cache_key, witty_row)

            rows.extend([codt_row, witty_row])

            print(
                "  codt: solved="
                f"{codt_row.solved_strict} time={codt_row.runtime_sec:.3f}s "
                f"timeout={codt_row.timeout}"
            )
            print(
                "  witty: solved="
                f"{witty_row.solved_strict} time={witty_row.runtime_sec:.3f}s "
                f"timeout={witty_row.timeout}"
            )

        save_results(args.output_csv, rows)
        print(f"Wrote results to {args.output_csv}")

        solved_summary = {}
        for row in rows:
            solved_summary.setdefault(row.solver, 0)
            solved_summary[row.solver] += int(row.solved_strict)

        print("Solved instances summary:")
        for solver, solved in sorted(solved_summary.items()):
            print(f"  {solver}: {solved}/{len(datasets)}")

        return 0
    finally:
        if args.keep_temp:
            print(f"Kept temp directory: {base_temp}")
        else:
            shutil.rmtree(base_temp, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())
