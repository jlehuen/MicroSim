"""Non-binary 3-tier evaluator for MicroSim TP4 assembly programs."""

from __future__ import annotations

import re
import statistics
from dataclasses import dataclass, field
from decimal import Decimal, ROUND_HALF_UP
from pathlib import Path
from typing import Any, Callable

import yaml

from runner import run_asm
from static_analyzer import evaluate_static_checks
from llm_feedback import generate_feedback


TRAFFIC_SEQ = [0x84, 0x88, 0x90, 0x30, 0x50, 0x90]
FIBO_DISTINCT = [1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233]


@dataclass
class ExerciseResult:
    exercise_id: str
    file: str
    title: str
    weight: float
    score: float
    present: bool
    assembled: bool
    assembly_error: str | None
    dynamic_ratio: float
    static_ratio: float
    details: list[str] = field(default_factory=list)


@dataclass
class StudentResult:
    student_id: str
    total_score: float
    max_score: float
    exercises: list[ExerciseResult]


def _round_to_step(value: float, step: float) -> float:
    if step <= 0:
        return round(value, 2)
    d_val = Decimal(str(value))
    d_step = Decimal(str(step))
    rounded = (d_val / d_step).quantize(Decimal("1"), rounding=ROUND_HALF_UP) * d_step
    return float(rounded)


def _best_cyclic_match(observed: list[int], target: list[int]) -> tuple[int, bool]:
    """Return (best consecutive prefix match out of len(target), whether >= 2 full cycles appear)."""
    if not observed:
        return 0, False
    n = len(target)
    best = 0
    full_cycles = 0
    for rot in range(n):
        rotated = target[rot:] + target[:rot]
        # Find best match starting anywhere in observed
        for start in range(len(observed)):
            matched = 0
            while start + matched < len(observed) and observed[start + matched] == rotated[matched % n]:
                matched += 1
            best = max(best, min(matched, n))
            full_cycles = max(full_cycles, matched // n)
    return best, full_cycles >= 2


def _eval_feux1(asm_path: Path) -> tuple[dict[str, Any], float, list[str]]:
    res = run_asm(asm_path, max_cycles=500)
    if not res.get("assembled"):
        return res, 0.0, [f"Erreur d'assemblage : {res.get('error')}"]
    outs = [o["value"] for o in res.get("outputs", []) if o["port"] == 2]
    matched, looped = _best_cyclic_match(outs, TRAFFIC_SEQ)
    ratio = 0.8 * (matched / len(TRAFFIC_SEQ)) + (0.2 if looped else 0.0)
    notes = [f"Séquence feux port 0x02 : {matched}/6 états corrects" + (" (boucle OK)" if looped else " (boucle non détectée)")]
    return res, ratio, notes


def _eval_feux2(asm_path: Path) -> tuple[dict[str, Any], float, list[str]]:
    res = run_asm(asm_path, max_cycles=1000)
    if not res.get("assembled"):
        return res, 0.0, [f"Erreur d'assemblage : {res.get('error')}"]
    outs = [o["value"] for o in res.get("outputs", []) if o["port"] == 2]
    matched, looped = _best_cyclic_match(outs, TRAFFIC_SEQ)
    seq_ratio = 0.8 * (matched / len(TRAFFIC_SEQ)) + (0.2 if looped else 0.0)

    ram = res.get("ram", [])
    table_slice = ram[0x50:0x57] if len(ram) >= 0x57 else []
    has_table_50 = False
    has_table_elsewhere = False
    for rot in range(len(TRAFFIC_SEQ)):
        pattern = TRAFFIC_SEQ[rot:] + TRAFFIC_SEQ[:rot] + [0xFF]
        if table_slice == pattern:
            has_table_50 = True
            break
        for idx in range(0, 0xC0 - len(pattern) + 1):
            if ram[idx : idx + len(pattern)] == pattern:
                has_table_elsewhere = True
                break

    if has_table_50:
        table_pts = 0.30
        table_note = "Table en 0x50 terminée par 0xFF : OK"
    elif has_table_elsewhere:
        table_pts = 0.15
        table_note = "Table terminée par 0xFF présente (adresse différente de 0x50)"
    else:
        table_pts = 0.0
        table_note = "Table en 0x50..0x56 non conforme"

    ratio = 0.7 * seq_ratio + table_pts
    notes = [
        f"Séquence feux port 0x02 : {matched}/6 états" + (" (boucle OK)" if looped else ""),
        table_note,
    ]
    return res, ratio, notes


def _eval_saisie(asm_path: Path) -> tuple[dict[str, Any], float, list[str]]:
    res = run_asm(asm_path, keys=[0x61, 0x62, 0x63, 0x0D], max_cycles=1000)
    if not res.get("assembled"):
        return res, 0.0, [f"Erreur d'assemblage : {res.get('error')}"]
    ram = res.get("ram", [])
    screen = bytes(ram[0xC0:0x100]).rstrip(b" \x00")
    score = 0.0
    notes: list[str] = []

    if not res.get("keyboard_exhausted"):
        score += 0.20
        notes.append("Lecture clavier jusqu'à 0x0D : OK")
    else:
        notes.append("Lecture clavier bloquée / 0x0D non détecté")

    if len(screen) > 0:
        score += 0.20
        notes.append(f"Écriture terminal ASCII ({len(screen)} octets)")
    else:
        notes.append("Aucun caractère écrit dans 0xC0..0xFF")

    if b"cba" in screen and 0x0D not in screen:
        score += 0.25
        notes.append("Affichage inversé sans 0x0D : OK")
    elif b"cba" in screen:
        score += 0.15
        notes.append("Affichage inversé mais 0x0D présent")
    elif b"abc" in screen:
        score += 0.10
        notes.append("Chaîne affichée mais non inversée")
    else:
        notes.append("Chaîne attendue 'cba' absente du terminal")

    if res.get("halted") and not res.get("fault"):
        score += 0.20
        notes.append("Arrêt propre sans erreur de pile : OK")
    else:
        notes.append(f"Fin anormale (halted={res.get('halted')}, fault={res.get('fault')})")

    res_empty = run_asm(asm_path, keys=[0x0D], max_cycles=500)
    if res_empty.get("halted") and not res_empty.get("fault"):
        score += 0.15
        notes.append("Saisie vide (Entrée seule) sans Stack Underflow : OK")
    else:
        notes.append("Saisie vide (Entrée seule) provoque une erreur ou un blocage")

    return res, score, notes


def _eval_temp(asm_path: Path) -> tuple[dict[str, Any], float, list[str]]:
    res_cold = run_asm(asm_path, heater=0x00, max_cycles=300)
    if not res_cold.get("assembled"):
        return res_cold, 0.0, [f"Erreur d'assemblage : {res_cold.get('error')}"]
    res_hot = run_asm(asm_path, heater=0x02, max_cycles=300)

    outs_cold = [o["value"] for o in res_cold.get("outputs", []) if o["port"] == 3]
    outs_hot = [o["value"] for o in res_hot.get("outputs", []) if o["port"] == 3]

    score = 0.0
    notes: list[str] = []
    if 0x80 in outs_cold:
        score += 0.45
        notes.append("T < 20°C -> brûleur allumé (0x80) : OK")
    else:
        notes.append("T < 20°C -> brûleur non allumé (0x80 attendu)")

    if 0x00 in outs_hot:
        score += 0.45
        notes.append("T >= 20°C -> brûleur éteint (0x00) : OK")
    else:
        res_bit0 = run_asm(asm_path, heater=0x01, max_cycles=300)
        outs_bit0 = [o["value"] for o in res_bit0.get("outputs", []) if o["port"] == 3]
        if 0x00 in outs_bit0:
            score += 0.15
            notes.append("Seuil 10°C (bit 0) testé au lieu de 20°C (bit 1)")
        else:
            notes.append("T >= 20°C -> brûleur non éteint (0x00 attendu)")

    if len(outs_cold) >= 2 and len(outs_hot) >= 2:
        score += 0.10
        notes.append("Boucle de régulation continue : OK")
    else:
        notes.append("Boucle de régulation non détectée")

    return res_cold, score, notes


def _eval_fibo(asm_path: Path) -> tuple[dict[str, Any], float, list[str]]:
    res = run_asm(asm_path, trace_al=True, max_cycles=500)
    if not res.get("assembled"):
        return res, 0.0, [f"Erreur d'assemblage : {res.get('error')}"]
    trace = res.get("al_trace", [])

    # Match longest contiguous prefix of FIBO_DISTINCT appearing in al_trace
    best_len = 0
    for start in range(len(trace)):
        k = 0
        while start + k < len(trace) and k < len(FIBO_DISTINCT) and trace[start + k] == FIBO_DISTINCT[k]:
            k += 1
        best_len = max(best_len, k)

    # Full unsigned (12 distinct terms up to 233) or signed stop (10 distinct terms up to 89)
    term_ratio = min(1.0, best_len / 10.0)
    clean_stop = 0.2 if (res.get("halted") and not res.get("fault")) else 0.0
    score = 0.8 * term_ratio + clean_stop
    notes = [
        f"Termes Fibonacci reconnus dans AL : {best_len}/12",
        "Arrêt propre (HLT) : OK" if clean_stop > 0 else "Arrêt non propre (timeout ou fault)",
    ]
    return res, score, notes


def _eval_feux3(asm_path: Path) -> tuple[dict[str, Any], float, list[str]]:
    res = run_asm(asm_path, max_cycles=3000)
    if not res.get("assembled"):
        return res, 0.0, [f"Erreur d'assemblage : {res.get('error')}"]
    outs = [o for o in res.get("outputs", []) if o["port"] == 2]
    vals = [o["value"] for o in outs]
    matched, looped = _best_cyclic_match(vals, TRAFFIC_SEQ)
    seq_ratio = 0.8 * (matched / len(TRAFFIC_SEQ)) + (0.2 if looped else 0.0)

    # Check alternating data/duration table at 0x50
    ram = res.get("ram", [])
    has_alt_table = ram[0x50 + 12] == 0xFF if len(ram) > 0x5C else False

    # Check timing differentiation between green states (0x84, 0x30) and short states
    long_deltas: list[int] = []
    short_deltas: list[int] = []
    for i in range(len(outs) - 1):
        delta = outs[i + 1]["cycle"] - outs[i]["cycle"]
        v = outs[i]["value"]
        if v in (0x84, 0x30):
            long_deltas.append(delta)
        elif v in (0x88, 0x50, 0x90):
            short_deltas.append(delta)

    timing_ok = (
        bool(long_deltas)
        and bool(short_deltas)
        and statistics.median(long_deltas) >= 2.0 * statistics.median(short_deltas)
    )
    score = 0.5 * seq_ratio + (0.2 if has_alt_table else 0.0) + (0.3 if timing_ok else 0.0)
    notes = [
        f"Séquence feux : {matched}/6 états" + (" (boucle OK)" if looped else ""),
        "Table alternée en 0x50 (fin 0xFF) : OK" if has_alt_table else "Table alternée en 0x50 non détectée",
        "Temporisation différenciée (verts > autres) : OK" if timing_ok else "Temporisation différenciée non observée",
    ]
    return res, score, notes


def _candidate_subroutine_targets(
    full_res: dict[str, Any],
    preferred_labels: list[str],
    call_index: int,
    fallback_addr: int,
) -> list[int]:
    """Return ordered unique candidate entry addresses for a subroutine."""
    candidates: list[int] = []

    def add(addr: int | None) -> None:
        if addr is not None and addr not in candidates:
            candidates.append(addr)

    labels = full_res.get("labels") or {}
    lower_labels = {k.lower(): int(v) for k, v in labels.items()}
    for name in preferred_labels:
        if name.lower() in lower_labels:
            add(lower_labels[name.lower()])

    call_targets = [int(a) for a in (full_res.get("call_targets") or [])]
    if 0 <= call_index < len(call_targets):
        add(call_targets[call_index])
    for addr in call_targets:
        add(addr)
    add(fallback_addr)
    return candidates


def _eval_read(asm_path: Path) -> tuple[dict[str, Any], float, list[str]]:
    res_full = run_asm(asm_path, keys=[0x61, 0x62, 0x0D, 0x63, 0x64, 0x0D], max_cycles=1000)
    if not res_full.get("assembled"):
        return res_full, 0.0, [f"Erreur d'assemblage : {res_full.get('error')}"]

    best_unit: tuple[float, dict[str, Any], list[str]] | None = None
    for target in _candidate_subroutine_targets(res_full, ["Read", "Saisie"], 0, 0x20):
        res_u = run_asm(
            asm_path,
            call_addr=target,
            regs={"BL": 0xA0},
            keys=[0x74, 0x65, 0x73, 0x74, 0x0D],
            max_cycles=500,
        )
        u_score = 0.0
        u_notes: list[str] = []
        if res_u.get("returned") and not res_u.get("fault"):
            u_score += 0.25
            u_notes.append("Routine Read retourne proprement (RET) : OK")
        else:
            u_notes.append("Routine Read ne retourne pas proprement")
        ram_u = res_u.get("ram", [])
        if bytes(ram_u[0xA0:0xA5]) == b"test\x00":
            u_score += 0.50
            u_notes.append("Chaîne 'test\\0' mémorisée en 0xA0 : OK")
        else:
            u_notes.append("Chaîne 'test\\0' absente en 0xA0")
        if best_unit is None or u_score > best_unit[0]:
            best_unit = (u_score, res_u, u_notes)
        if u_score >= 0.75:
            break

    assert best_unit is not None
    score, res_unit, notes = best_unit

    ram_f = res_full.get("ram", [])
    if bytes(ram_f[0xA0:0xA3]) == b"ab\x00" and bytes(ram_f[0xB0:0xB3]) == b"cd\x00" and res_full.get("halted"):
        score += 0.25
        notes.append("Programme principal (0xA0 + 0xB0) : OK")
    else:
        notes.append("Programme principal (double saisie 0xA0/0xB0) incomplet")

    return res_unit, score, notes


def _eval_print(asm_path: Path) -> tuple[dict[str, Any], float, list[str]]:
    res_full = run_asm(asm_path, keys=[0x61, 0x62, 0x0D], max_cycles=1000)
    if not res_full.get("assembled"):
        return res_full, 0.0, [f"Erreur d'assemblage : {res_full.get('error')}"]

    best_unit: tuple[float, dict[str, Any], list[str]] | None = None
    for target in _candidate_subroutine_targets(res_full, ["Print", "Affichage"], 1, 0x40):
        res_u = run_asm(
            asm_path,
            call_addr=target,
            regs={"BL": 0xA0, "CL": 1, "DL": 2},
            mem={0xA0: [0x4F, 0x4B, 0x00]},
            max_cycles=500,
        )
        u_score = 0.0
        u_notes: list[str] = []
        if res_u.get("returned") and not res_u.get("fault"):
            u_score += 0.20
            u_notes.append("Routine Print retourne proprement (RET) : OK")
        else:
            u_notes.append("Routine Print ne retourne pas proprement")
        ram_u = res_u.get("ram", [])
        if bytes(ram_u[0xD2:0xD4]) == b"OK":
            u_score += 0.60
            u_notes.append("Affichage 'OK' en ligne 1, col 2 (0xD2) : OK")
        elif b"OK" in bytes(ram_u[0xC0:0x100]):
            u_score += 0.30
            u_notes.append("Affichage 'OK' dans le terminal mais mauvaise position")
        else:
            u_notes.append("Affichage 'OK' absent du terminal")
        if best_unit is None or u_score > best_unit[0]:
            best_unit = (u_score, res_u, u_notes)
        if u_score >= 0.80:
            break

    assert best_unit is not None
    score, res_unit, notes = best_unit

    screen_f = bytes(res_full.get("ram", [])[0xC0:0x100])
    if screen_f.count(b"ab") >= 2 and res_full.get("halted"):
        score += 0.20
        notes.append("Double affichage dans le programme principal : OK")
    elif b"ab" in screen_f:
        score += 0.10
        notes.append("Un seul affichage dans le programme principal")
    else:
        notes.append("Programme principal n'affiche pas la chaîne saisie")

    return res_unit, score, notes


def _eval_upcase(asm_path: Path) -> tuple[dict[str, Any], float, list[str]]:
    res_full = run_asm(asm_path, keys=[0x61, 0x62, 0x63, 0x0D], max_cycles=1000)
    if not res_full.get("assembled"):
        return res_full, 0.0, [f"Erreur d'assemblage : {res_full.get('error')}"]

    best_unit: tuple[float, dict[str, Any], list[str]] | None = None
    for target in _candidate_subroutine_targets(res_full, ["Upcase", "Majuscules"], 1, 0x60):
        res_u = run_asm(
            asm_path,
            call_addr=target,
            regs={"BL": 0xA0},
            mem={0xA0: list(b"bonjour\x00")},
            max_cycles=500,
        )
        u_score = 0.0
        u_notes: list[str] = []
        if res_u.get("returned") and not res_u.get("fault"):
            u_score += 0.20
            u_notes.append("Routine Upcase retourne proprement (RET) : OK")
        else:
            u_notes.append("Routine Upcase ne retourne pas proprement")
        ram_u = res_u.get("ram", [])
        if bytes(ram_u[0xA0:0xA8]) == b"BONJOUR\x00":
            u_score += 0.60
            u_notes.append("Capitalisation 'bonjour' -> 'BONJOUR' en 0xA0 : OK")
        else:
            u_notes.append("Chaîne en 0xA0 non capitalisée correctement")
        if best_unit is None or u_score > best_unit[0]:
            best_unit = (u_score, res_u, u_notes)
        if u_score >= 0.80:
            break

    assert best_unit is not None
    score, res_unit, notes = best_unit

    screen_f = bytes(res_full.get("ram", [])[0xC0:0x100])
    if b"ABC" in screen_f and res_full.get("halted"):
        score += 0.20
        notes.append("Programme principal (saisie + majuscules + affichage) : OK")
    else:
        notes.append("Programme principal incomplet")

    return res_unit, score, notes


def _eval_compare(asm_path: Path) -> tuple[dict[str, Any], float, list[str]]:
    full_eq = run_asm(asm_path, keys=[0x61, 0x62, 0x0D, 0x61, 0x62, 0x0D], max_cycles=1000)
    if not full_eq.get("assembled"):
        return full_eq, 0.0, [f"Erreur d'assemblage : {full_eq.get('error')}"]

    best_unit: tuple[float, dict[str, Any], list[str]] | None = None
    for target in _candidate_subroutine_targets(full_eq, ["Compare", "Cmp"], 1, 0x80):
        res_eq = run_asm(
            asm_path,
            call_addr=target,
            regs={"CL": 0xA0, "DL": 0xB0},
            mem={0xA0: list(b"abc\x00"), 0xB0: list(b"abc\x00")},
            max_cycles=500,
        )
        res_neq = run_asm(
            asm_path,
            call_addr=target,
            regs={"CL": 0xA0, "DL": 0xB0},
            mem={0xA0: list(b"abc\x00"), 0xB0: list(b"abd\x00")},
            max_cycles=500,
        )
        res_prefix = run_asm(
            asm_path,
            call_addr=target,
            regs={"CL": 0xA0, "DL": 0xB0},
            mem={0xA0: list(b"ab\x00"), 0xB0: list(b"abc\x00")},
            max_cycles=500,
        )
        u_score = 0.0
        u_notes: list[str] = []
        if res_eq.get("returned") and res_eq.get("flags", {}).get("Z") is True:
            u_score += 0.30
            u_notes.append("Routine Compare (chaînes égales -> Z=1) : OK")
        else:
            u_notes.append("Routine Compare (chaînes égales -> Z=1) échouée")
        if res_neq.get("returned") and res_neq.get("flags", {}).get("Z") is False:
            u_score += 0.20
            u_notes.append("Routine Compare (chaînes différentes -> Z=0) : OK")
        else:
            u_notes.append("Routine Compare (chaînes différentes -> Z=0) échouée")
        if res_prefix.get("returned") and res_prefix.get("flags", {}).get("Z") is False:
            u_score += 0.20
            u_notes.append("Routine Compare (préfixe 'ab' vs 'abc' -> Z=0) : OK")
        else:
            u_notes.append("Routine Compare (préfixe 'ab' vs 'abc' -> Z=0) échouée")
        if best_unit is None or u_score > best_unit[0]:
            best_unit = (u_score, res_eq, u_notes)
        if u_score >= 0.70:
            break

    assert best_unit is not None
    score, res_eq, notes = best_unit

    full_neq = run_asm(asm_path, keys=[0x61, 0x62, 0x0D, 0x61, 0x63, 0x0D], max_cycles=1000)
    outs_eq = [o["value"] for o in full_eq.get("outputs", []) if o["port"] == 2]
    outs_neq = [o["value"] for o in full_neq.get("outputs", []) if o["port"] == 2]

    if 0x04 in outs_eq:
        score += 0.15
        notes.append("Feu vert (0x04) si chaînes égales : OK")
    else:
        notes.append("Feu vert (0x04) non émis si chaînes égales")

    if 0x10 in outs_neq:
        score += 0.15
        notes.append("Feu rouge (0x10) si chaînes différentes : OK")
    else:
        notes.append("Feu rouge (0x10) non émis si chaînes différentes")

    return res_eq, score, notes


DYNAMIC_EVALUATORS: dict[str, Callable[[Path], tuple[dict[str, Any], float, list[str]]]] = {
    "feux1": _eval_feux1,
    "feux2": _eval_feux2,
    "saisie": _eval_saisie,
    "temp": _eval_temp,
    "fibo": _eval_fibo,
    "feux3": _eval_feux3,
    "read": _eval_read,
    "print": _eval_print,
    "upcase": _eval_upcase,
    "compare": _eval_compare,
}


class TP4Evaluator:
    def __init__(
        self,
        spec_path: Path | None = None,
        *,
        use_llm: bool = False,
        llm_model: str | None = None,
    ) -> None:
        if spec_path is None:
            spec_path = Path(__file__).resolve().parent / "TP4.yaml"
        self.spec = yaml.safe_load(spec_path.read_text(encoding="utf-8"))
        self.use_llm = use_llm
        self.llm_model = llm_model

    def evaluate_student(self, student_dir: Path, student_id: str | None = None) -> StudentResult:
        sid = student_id or student_dir.name
        exercises_res: list[ExerciseResult] = []
        raw_total = 0.0
        raw_max = 0.0

        for ex in self.spec["exercises"]:
            ex_id = ex["id"]
            filename = ex["file"]
            weight = float(ex["weight"])
            raw_max += weight

            asm_path = self._find_file(student_dir, filename)
            if asm_path is None:
                exercises_res.append(
                    ExerciseResult(
                        exercise_id=ex_id,
                        file=filename,
                        title=ex["title"],
                        weight=weight,
                        score=0.0,
                        present=False,
                        assembled=False,
                        assembly_error=None,
                        dynamic_ratio=0.0,
                        static_ratio=0.0,
                        details=["Fichier absent"],
                    )
                )
                continue

            source = asm_path.read_text(encoding="utf-8", errors="replace")
            static_ratio, static_notes = evaluate_static_checks(source, ex.get("static_checks", []))

            dyn_fn = DYNAMIC_EVALUATORS[ex_id]
            run_res, dyn_ratio, dyn_notes = dyn_fn(asm_path)
            assembled = bool(run_res.get("assembled", False))
            asm_error = run_res.get("error")

            if assembled:
                score = weight * (0.15 + 0.65 * dyn_ratio + 0.20 * static_ratio)
            else:
                # Non-binary fallback: reward good algorithmic ideas even on assembly error
                score = weight * (0.35 * static_ratio)

            step = float(self.spec.get("score_rounding", 0.25))
            score = min(weight, _round_to_step(score, step))
            raw_total += score

            details = dyn_notes + static_notes
            if self.use_llm and (not assembled or dyn_ratio < 0.70):
                hint = generate_feedback(
                    exercise_title=ex["title"],
                    source_code=source,
                    assembly_error=asm_error,
                    notes=dyn_notes,
                    model=self.llm_model,
                )
                if hint:
                    details.append(f"[IA] {hint}")

            exercises_res.append(
                ExerciseResult(
                    exercise_id=ex_id,
                    file=filename,
                    title=ex["title"],
                    weight=weight,
                    score=score,
                    present=True,
                    assembled=assembled,
                    assembly_error=asm_error,
                    dynamic_ratio=dyn_ratio,
                    static_ratio=static_ratio,
                    details=details,
                )
            )

        target_total = float(self.spec.get("total_points", 20.0))
        scaled = (raw_total / raw_max * target_total) if raw_max > 0 else 0.0
        final_score = _round_to_step(scaled, float(self.spec.get("score_rounding", 0.25)))
        return StudentResult(
            student_id=sid,
            total_score=final_score,
            max_score=target_total,
            exercises=exercises_res,
        )

    @staticmethod
    def _find_file(student_dir: Path, filename: str) -> Path | None:
        direct = student_dir / filename
        if direct.is_file():
            return direct
        target_lower = filename.lower()
        all_asm = sorted(student_dir.rglob("*.asm"))
        for candidate in all_asm:
            if candidate.name.lower() == target_lower:
                return candidate
        # Fuzzy stem fallback (e.g., TP4_feux_1.asm -> feux1.asm)
        target_norm = re.sub(r"[^a-z0-9]", "", Path(filename).stem.lower())
        for candidate in all_asm:
            cand_norm = re.sub(r"[^a-z0-9]", "", candidate.stem.lower())
            if target_norm in cand_norm:
                return candidate
        return None
