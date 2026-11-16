"""Normalized assembly similarity detector for TP4 plagiarism checks."""

from __future__ import annotations

import re
from dataclasses import dataclass
from difflib import SequenceMatcher
from pathlib import Path

from evaluator import TP4Evaluator


@dataclass
class PlagiarismPair:
    student_a: str
    student_b: str
    similarity: float
    matched_files: list[tuple[str, float]]


def normalize_asm(source: str) -> str:
    """Normalize an assembly source by removing comments, whitespace, and label names."""
    label_names: set[str] = set()
    cleaned_lines: list[str] = []

    for raw in source.splitlines():
        line = raw.split(";", 1)[0].strip()
        if not line:
            continue
        m = re.match(r"^([A-Za-z_.][A-Za-z0-9_]*)\s*:(.*)$", line)
        if m:
            label_names.add(m.group(1))
            rest = m.group(2).strip()
            line = f"LBL: {rest}" if rest else "LBL:"
        cleaned_lines.append(line.lower())

    text = "\n".join(cleaned_lines)
    for lbl in label_names:
        text = re.sub(rf"\b{re.escape(lbl.lower())}\b", "lbl", text)
    text = re.sub(r"[ \t]+", " ", text)
    return text


def detect_plagiarism(
    student_dirs: list[Path],
    spec_exercises: list[dict],
    threshold: float = 0.85,
) -> list[PlagiarismPair]:
    """Compare all student pairs across submitted .asm files and return suspicious pairs."""
    normalized_by_student: dict[str, dict[str, str]] = {}
    for sdir in student_dirs:
        files_map: dict[str, str] = {}
        for ex in spec_exercises:
            fname = ex["file"]
            found = TP4Evaluator._find_file(sdir, fname)
            if found is not None:
                norm = normalize_asm(found.read_text(encoding="utf-8", errors="replace"))
                if len(norm) >= 20:
                    files_map[fname] = norm
        normalized_by_student[sdir.name] = files_map

    names = sorted(normalized_by_student.keys())
    suspicious: list[PlagiarismPair] = []

    for i in range(len(names)):
        for j in range(i + 1, len(names)):
            a, b = names[i], names[j]
            fa, fb = normalized_by_student[a], normalized_by_student[b]
            common = sorted(set(fa.keys()) & set(fb.keys()))
            if not common:
                continue
            sims: list[tuple[str, float]] = []
            for fname in common:
                ratio = SequenceMatcher(None, fa[fname], fb[fname]).ratio()
                if ratio >= threshold:
                    sims.append((fname, ratio))
            if sims:
                avg_sim = sum(r for _, r in sims) / len(sims)
                if len(sims) >= 2 or avg_sim >= threshold:
                    suspicious.append(
                        PlagiarismPair(
                            student_a=a,
                            student_b=b,
                            similarity=avg_sim,
                            matched_files=sorted(sims, key=lambda x: -x[1]),
                        )
                    )

    suspicious.sort(key=lambda p: -p.similarity)
    return suspicious
