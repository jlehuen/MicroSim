#!/bin/bash

export HTTP_PROXY="http://proxy.univ-lemans.fr:3128" 
export HTTPS_PROXY="http://proxy.univ-lemans.fr:3128" 

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd) || exit 1
VENV_DIR="$SCRIPT_DIR/grader/.venv"
PYTHON_BIN="$VENV_DIR/bin/python"

show_help() {
    cat <<'EOF'
NOM
    eval-tp4.sh - Correcteur automatique non-binaire pour le TP4 (MicroSim 8-bit)

SYNOPSIS
    ./eval-tp4.sh [OPTIONS] <cible>

DESCRIPTION
    Évalue les 10 programmes assembleur du TP4 (feux1.asm à compare.asm) à l'aide
    du simulateur MicroSim en mode headless et d'un barème déclaratif (grader/TP4.yaml).

    La notation est progressive (non-binaire) sur 3 paliers :
      1. Assemblage (15 %) : vérification syntaxique via Assembler.java.
      2. Exécution (65 %)  : tests comportementaux gradués sur CPU headless.
      3. Idées clés (20 %) : analyse statique des motifs algorithmiques.
      (Si le code ne s'assemble pas, l'analyse statique peut accorder jusqu'à 35 %.)

ARGUMENTS
    <cible>
        Dossier contenant les fichiers .asm d'un étudiant, ou dossier d'une
        promotion contenant un sous-dossier par étudiant (avec --dir).

OPTIONS
    -h, --help, --man
        Affiche ce manuel d'utilisation.

    --dir
        Traite <cible> comme un dossier-classe (un sous-dossier par étudiant).

    --plagiarism
        Détecte les similarités suspectes entre copies (avec --dir).

    --plagiarism-threshold <0.0..1.0>
        Seuil de similarité pour le plagiat (défaut : 0.85).

    --no-llm
        Désactive le retour pédagogique IA par défaut (Ollama local sur 127.0.0.1:11434).

    --llm-model <nom>
        Modèle Ollama local à utiliser (défaut : qwen2.5-coder:7b).

    --spec <fichier.yaml>
        Utilise un fichier de barème YAML alternatif (défaut : grader/TP4.yaml).

    --csv <fichier.csv>
        Exporte le tableau des notes au format CSV.

    --xlsx <fichier.xlsx>
        Exporte le tableau des notes au format Excel (.xlsx).

    --json <fichier.json>
        Exporte le rapport complet et les justifications au format JSON.

EXEMPLES
    ./eval-tp4.sh copies/dupont
        Évalue le dossier d'un étudiant et affiche le détail en console.

    ./eval-tp4.sh --dir copies_promo --csv notes.csv --xlsx notes.xlsx
        Évalue toute une promotion et génère les exports CSV et Excel.
EOF
}

if [ $# -eq 0 ] || [ "$1" = "-h" ] || [ "$1" = "--help" ] || [ "$1" = "--man" ]; then
    show_help
    exit 0
fi

# Create the virtual environment and install dependencies on first run
if [ ! -x "$PYTHON_BIN" ]; then
    echo "[eval-tp4] Initialisation de l'environnement Python (premier lancement)..." >&2
    python3 -m venv "$VENV_DIR" || exit 1
    echo "[eval-tp4] Installation des dépendances (PyYAML, openpyxl)..." >&2
    "$VENV_DIR/bin/pip" install --timeout 15 -r "$SCRIPT_DIR/grader/requirements.txt" || exit 1
fi

exec "$PYTHON_BIN" "$SCRIPT_DIR/grader/eval_tp4.py" "$@"
