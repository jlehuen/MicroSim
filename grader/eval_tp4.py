#!/usr/bin/env python3
"""CLI for automated non-binary grading of MicroSim TP4 assembly submissions."""

from __future__ import annotations

import argparse
import csv
import json
import sys
from pathlib import Path

from evaluator import StudentResult, TP4Evaluator
from plagiarism import PlagiarismPair, detect_plagiarism


def print_student_report(res: StudentResult) -> None:
    print(f"=== {res.student_id} : {res.total_score:.2f} / {res.max_score:.2f} ===")
    for ex in res.exercises:
        status = "ABSENT" if not ex.present else ("OK" if ex.assembled else "ASM_ERR")
        print(f"  - {ex.file:<12} {ex.score:4.2f}/{ex.weight:4.2f}  [{status}]  dyn={ex.dynamic_ratio*100:3.0f}%  stat={ex.static_ratio*100:3.0f}%")
        for d in ex.details:
            print(f"      {d}")


def export_csv(results: list[StudentResult], out_path: Path) -> None:
    if not results:
        return
    headers = ["student_id", "total_score", "max_score"] + [ex.file for ex in results[0].exercises]
    with out_path.open("w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow(headers)
        for r in results:
            row = [r.student_id, f"{r.total_score:.2f}", f"{r.max_score:.2f}"] + [
                f"{ex.score:.2f}" for ex in r.exercises
            ]
            writer.writerow(row)


def export_xlsx(
    results: list[StudentResult],
    out_path: Path,
    plagiarism_pairs: list[PlagiarismPair] | None = None,
) -> None:
    import openpyxl
    from openpyxl.styles import Alignment, Font, PatternFill

    wb = openpyxl.Workbook()
    ws = wb.active
    ws.title = "Notes TP4"
    if not results:
        wb.save(out_path)
        return

    headers = ["Étudiant", "Note / 20"] + [ex.file for ex in results[0].exercises] + ["Commentaires IA"]
    ws.append(headers)
    header_fill = PatternFill(start_color="1F4E78", end_color="1F4E78", fill_type="solid")
    header_font = Font(color="FFFFFF", bold=True)
    pass_fill = PatternFill(start_color="E2EFDA", end_color="E2EFDA", fill_type="solid")
    fail_fill = PatternFill(start_color="FCE4D6", end_color="FCE4D6", fill_type="solid")
    pass_font = Font(color="274E13", bold=True)
    fail_font = Font(color="9C0006", bold=True)

    for cell in ws[1]:
        cell.fill = header_fill
        cell.font = header_font
        cell.alignment = Alignment(horizontal="center")

    for r in results:
        ia_comments = []
        for ex in r.exercises:
            for d in ex.details:
                if d.startswith("[IA] "):
                    ia_comments.append(f"{ex.file} : {d[5:]}")
        ia_text = "\n".join(ia_comments)

        ws.append([r.student_id, r.total_score] + [ex.score for ex in r.exercises] + [ia_text])
        note_cell = ws.cell(row=ws.max_row, column=2)
        note_cell.number_format = "0.00"
        if r.total_score >= r.max_score / 2.0:
            note_cell.fill = pass_fill
            note_cell.font = pass_font
        else:
            note_cell.fill = fail_fill
            note_cell.font = fail_font
        for col_idx in range(3, len(headers)):
            ws.cell(row=ws.max_row, column=col_idx).number_format = "0.00"
        ia_cell = ws.cell(row=ws.max_row, column=len(headers))
        ia_cell.alignment = Alignment(wrap_text=True, vertical="top")

    if len(results) > 1:
        scores = [r.total_score for r in results]
        avg_total = sum(scores) / len(scores)
        ws.append(["Moyenne", round(avg_total, 2)] + [
            round(sum(r.exercises[i].score for r in results) / len(results), 2)
            for i in range(len(results[0].exercises))
        ])
        for cell in ws[ws.max_row]:
            cell.font = Font(bold=True)
        ws.append(["Min / Max", f"{min(scores):.2f} / {max(scores):.2f}"])

    for col in ws.columns:
        col_letter = col[0].column_letter
        if col[0].value == "Commentaires IA":
            ws.column_dimensions[col_letter].width = 60
        else:
            max_len = max((len(str(c.value or "")) for c in col), default=8)
            ws.column_dimensions[col_letter].width = max(12, max_len + 3)

    if plagiarism_pairs is not None:
        ws_sim = wb.create_sheet("Similarités")
        ws_sim.append(["Étudiant A", "Étudiant B", "Similarité moyenne (%)", "Fichiers concernés"])
        for cell in ws_sim[1]:
            cell.fill = header_fill
            cell.font = header_font
        for pair in plagiarism_pairs:
            files_desc = ", ".join(f"{f} ({r*100:.0f}%)" for f, r in pair.matched_files)
            ws_sim.append([pair.student_a, pair.student_b, round(pair.similarity * 100, 1), files_desc])
        for col in ws_sim.columns:
            max_len = max((len(str(c.value or "")) for c in col), default=12)
            ws_sim.column_dimensions[col[0].column_letter].width = max(14, max_len + 3)

    wb.save(out_path)


def main() -> int:
    parser = argparse.ArgumentParser(description="Correcteur automatique non-binaire TP4 (MicroSim 8-bit)")
    parser.add_argument("target", type=Path, help="Dossier d'un étudiant, ou dossier-classe avec --dir")
    parser.add_argument("--dir", action="store_true", help="Traiter target comme un dossier contenant un sous-dossier par étudiant")
    parser.add_argument("--spec", type=Path, default=None, help="Chemin vers un barème YAML alternatif")
    parser.add_argument("--plagiarism", action="store_true", help="Détecter les similarités suspectes entre étudiants (avec --dir)")
    parser.add_argument("--plagiarism-threshold", type=float, default=0.85, help="Seuil de similarité (défaut : 0.85)")
    parser.add_argument("--no-llm", action="store_true", help="Désactiver le retour pédagogique par LLM local")
    parser.add_argument("--llm-model", type=str, default=None, help="Nom du modèle Ollama local (défaut : qwen2.5-coder:7b)")
    parser.add_argument("--csv", type=Path, default=None, help="Exporter les notes en CSV")
    parser.add_argument("--xlsx", type=Path, default=None, help="Exporter les notes en Excel (.xlsx)")
    parser.add_argument("--json", type=Path, default=None, help="Exporter le détail complet en JSON")
    args = parser.parse_args()

    if not args.target.exists():
        print(f"Erreur : le chemin '{args.target}' est introuvable.", file=sys.stderr)
        return 1
    if not args.target.is_dir():
        print(f"Erreur : '{args.target}' n'est pas un dossier.", file=sys.stderr)
        return 1
    if args.spec is not None and not args.spec.is_file():
        print(f"Erreur : le barème '{args.spec}' est introuvable.", file=sys.stderr)
        return 1

    evaluator = TP4Evaluator(args.spec, use_llm=not args.no_llm, llm_model=args.llm_model)
    results: list[StudentResult] = []
    plagiarism_pairs: list[PlagiarismPair] | None = None

    if args.dir:
        subdirs = sorted(p for p in args.target.iterdir() if p.is_dir() and not p.name.startswith("."))
        if not subdirs:
            print(f"Erreur : aucun sous-dossier étudiant trouvé dans '{args.target}'.", file=sys.stderr)
            return 1
        for sub in subdirs:
            res = evaluator.evaluate_student(sub)
            results.append(res)
            print_student_report(res)
        if args.plagiarism:
            plagiarism_pairs = detect_plagiarism(
                subdirs,
                evaluator.spec["exercises"],
                threshold=args.plagiarism_threshold,
            )
            print("\n=== Analyse de similarité (plagiat) ===")
            if not plagiarism_pairs:
                print(f"  Aucune paire suspecte (>= {args.plagiarism_threshold*100:.0f}%).")
            else:
                for p in plagiarism_pairs:
                    files_str = ", ".join(f"{f} ({r*100:.0f}%)" for f, r in p.matched_files)
                    print(f"  ! {p.student_a} <-> {p.student_b} : {p.similarity*100:.1f}% [{files_str}]")
    else:
        res = evaluator.evaluate_student(args.target)
        results.append(res)
        print_student_report(res)

    if args.csv:
        export_csv(results, args.csv)
        print(f"CSV exporté : {args.csv}")
    if args.xlsx:
        export_xlsx(results, args.xlsx, plagiarism_pairs=plagiarism_pairs)
        print(f"Excel exporté : {args.xlsx}")
    if args.json:
        payload = {
            "students": [
                {
                    "student_id": r.student_id,
                    "total_score": r.total_score,
                    "max_score": r.max_score,
                    "exercises": [
                        {
                            "id": e.exercise_id,
                            "file": e.file,
                            "score": e.score,
                            "weight": e.weight,
                            "present": e.present,
                            "assembled": e.assembled,
                            "assembly_error": e.assembly_error,
                            "dynamic_ratio": e.dynamic_ratio,
                            "static_ratio": e.static_ratio,
                            "details": e.details,
                        }
                        for e in r.exercises
                    ],
                }
                for r in results
            ],
            "plagiarism": [
                {
                    "student_a": p.student_a,
                    "student_b": p.student_b,
                    "similarity": round(p.similarity, 4),
                    "matched_files": [{"file": f, "similarity": round(r, 4)} for f, r in p.matched_files],
                }
                for p in (plagiarism_pairs or [])
            ],
        }
        args.json.write_text(json.dumps(payload, indent=2, ensure_ascii=False), encoding="utf-8")
        print(f"JSON exporté : {args.json}")

    return 0


if __name__ == "__main__":
    sys.exit(main())
