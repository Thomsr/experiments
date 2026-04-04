#!/usr/bin/env python3
"""Generate a CODT-vs-Witty runtime scatter plot from benchmark CSV output."""

from __future__ import annotations

import argparse
import csv
from pathlib import Path

import matplotlib.pyplot as plt


def parse_args() -> argparse.Namespace:
    script_dir = Path(__file__).resolve().parent
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--input-csv",
        type=Path,
        default=script_dir / "out" / "codt_vs_witty_results.csv",
        help="CSV produced by run_codt_vs_witty.py",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=script_dir / "out" / "codt_vs_witty_scatter.png",
        help="Output image path",
    )
    parser.add_argument(
        "--title",
        type=str,
        default="CODT vs Witty on sampled datasets",
        help="Plot title",
    )
    parser.add_argument(
        "--timeout-sec",
        type=float,
        default=300.0,
        help="Optional horizontal timeout reference line in seconds",
    )
    return parser.parse_args()


def to_bool(value: str) -> bool:
    return value.strip().lower() == "true"


def main() -> int:
    args = parse_args()

    if not args.input_csv.is_file():
        raise FileNotFoundError(f"Input CSV not found: {args.input_csv}")

    runtime_by_dataset: dict[str, dict[str, tuple[float, bool]]] = {}

    with args.input_csv.open("r", encoding="utf-8", newline="") as fin:
        reader = csv.DictReader(fin)
        required = {"solver", "dataset_id", "runtime_sec", "solved_strict"}
        missing = required.difference(reader.fieldnames or [])
        if missing:
            raise ValueError(f"Input CSV missing columns: {sorted(missing)}")

        for row in reader:
            dataset_id = row["dataset_id"]
            solver = row["solver"]
            runtime = float(row["runtime_sec"])
            solved = to_bool(row["solved_strict"])
            runtime_by_dataset.setdefault(dataset_id, {})[solver] = (runtime, solved)

    x_witty: list[float] = []
    y_codt: list[float] = []
    for per_solver in runtime_by_dataset.values():
        if "witty" not in per_solver or "codt" not in per_solver:
            continue
        witty_time, witty_solved = per_solver["witty"]
        codt_time, codt_solved = per_solver["codt"]
        if witty_solved and codt_solved:
            x_witty.append(witty_time)
            y_codt.append(codt_time)

    if not x_witty:
        raise RuntimeError("No datasets where both CODT and Witty were solved strictly")

    fig, ax = plt.subplots(figsize=(8, 5))
    ax.scatter(x_witty, y_codt, alpha=0.75, s=32, edgecolors="none")

    max_v = max(max(x_witty), max(y_codt))
    ax.plot([0, max_v], [0, max_v], linestyle="--", linewidth=1, color="black", label="y = x")

    if args.timeout_sec > 0:
        ax.axvline(
            args.timeout_sec,
            color="gray",
            linestyle="--",
            linewidth=1,
            label=f"timeout={args.timeout_sec:.0f}s",
        )
        ax.axhline(
            args.timeout_sec,
            color="gray",
            linestyle="--",
            linewidth=1,
        )

    ax.set_xlabel("Witty solve time (s)")
    ax.set_ylabel("CODT solve time (s)")
    ax.set_title(args.title)
    ax.grid(alpha=0.25)
    ax.legend(loc="best")

    args.output.parent.mkdir(parents=True, exist_ok=True)
    fig.tight_layout()
    fig.savefig(args.output, dpi=180)
    print(f"Wrote plot to {args.output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
