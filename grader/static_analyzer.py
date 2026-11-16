"""Static analyzer for MicroSim 8-bit assembly source files.

Used to award partial credit for sound algorithmic ideas even when a student's
program fails to assemble or fails dynamic execution.
"""

from __future__ import annotations

import re
from dataclasses import dataclass


@dataclass
class StaticFeatures:
    mnemonics: set[str]
    in_ports: set[int]
    out_ports: set[int]
    uses_indirect_reg: bool
    line_count: int


def _parse_int_token(token: str) -> int | None:
    t = token.strip().lower()
    try:
        if t.startswith("0x"):
            return int(t[2:], 16)
        return int(t, 10)
    except ValueError:
        return None


def extract_features(source: str) -> StaticFeatures:
    mnemonics: set[str] = set()
    in_ports: set[int] = set()
    out_ports: set[int] = set()
    uses_indirect_reg = False
    line_count = 0

    for raw_line in source.splitlines():
        line = raw_line.split(";", 1)[0].strip()
        if not line:
            continue
        line_count += 1

        # Strip optional leading label(s)
        while ":" in line:
            before, after = line.split(":", 1)
            if re.match(r"^[A-Za-z_.][A-Za-z0-9_]*$", before.strip()):
                line = after.strip()
            else:
                break
        if not line:
            continue

        parts = line.split(None, 1)
        mnemonic = parts[0].upper()
        mnemonics.add(mnemonic)
        operands = parts[1] if len(parts) > 1 else ""

        if re.search(r"\[\s*(AL|BL|CL|DL|SP)\b", operands, re.IGNORECASE):
            uses_indirect_reg = True

        if mnemonic == "IN":
            port = _parse_int_token(operands)
            if port is not None:
                in_ports.add(port)
        elif mnemonic == "OUT":
            port = _parse_int_token(operands)
            if port is not None:
                out_ports.add(port)

    return StaticFeatures(
        mnemonics=mnemonics,
        in_ports=in_ports,
        out_ports=out_ports,
        uses_indirect_reg=uses_indirect_reg,
        line_count=line_count,
    )


def match_pattern(features: StaticFeatures, pattern: str) -> bool:
    """Evaluate a declarative static pattern expression."""
    if pattern.startswith("all:"):
        sub = pattern[4:].split(",")
        return all(match_pattern(features, p.strip()) for p in sub)
    if pattern.startswith("any:"):
        sub = pattern[4:].split(",")
        return any(match_pattern(features, p.strip()) for p in sub)
    if pattern.startswith("mnemonic:"):
        return pattern.split(":", 1)[1].upper() in features.mnemonics
    if pattern.startswith("in_port:"):
        return int(pattern.split(":", 1)[1]) in features.in_ports
    if pattern.startswith("out_port:"):
        return int(pattern.split(":", 1)[1]) in features.out_ports
    if pattern == "indirect_reg":
        return features.uses_indirect_reg
    return False


def evaluate_static_checks(source: str, checks: list[dict]) -> tuple[float, list[str]]:
    """Return (score_ratio in [0.0, 1.0], list of matched/missed messages)."""
    if not checks:
        return 0.0, []
    features = extract_features(source)
    total_pts = sum(float(c.get("points", 1.0)) for c in checks)
    earned_pts = 0.0
    notes: list[str] = []
    for chk in checks:
        pts = float(chk.get("points", 1.0))
        name = chk.get("name", chk.get("pattern", ""))
        if match_pattern(features, chk["pattern"]):
            earned_pts += pts
            notes.append(f"[+] {name}")
        else:
            notes.append(f"[-] {name}")
    ratio = (earned_pts / total_pts) if total_pts > 0 else 0.0
    return ratio, notes
