#!/bin/bash
set -e

PROJECT_ROOT=$(cd "$(dirname "$0")" && pwd) || exit 1
cd "$PROJECT_ROOT" || exit 1

MAN_HTML="man/man.html"

if [ ! -f "$MAN_HTML" ]; then
    echo "❌ Erreur: $MAN_HTML introuvable !" >&2
    exit 1
fi

echo "🔍 Optimisation et vérification de $MAN_HTML..."
python3 -c "
with open('$MAN_HTML', 'r', encoding='utf-8') as f:
    content = f.read()

# Nettoyer d'éventuels gros bundles inutilisés (Mermaid, Viz) injectés lors des exports Markdown
import re
m_mermaid = re.search(r'<script type=\"text/javascript\">\s*\"use strict\";var __esbuild_esm_mermaid_nm;', content)
m_mathjax = re.search(r'<script type=\"text/x-mathjax-config\">', content)

if m_mermaid and m_mathjax:
    content = content[:m_mermaid.start()] + content[m_mathjax.start():]
    with open('$MAN_HTML', 'w', encoding='utf-8') as f:
        f.write(content)
    print('  -> Scripts lourds inactifs nettoyés avec succès.')
"

FILE_SIZE=$(ls -lh "$MAN_HTML" | awk '{print $5}')
echo "📄 Taille actuelle de $MAN_HTML : $FILE_SIZE"

# S'assurer que le fichier .nojekyll est présent pour GitHub Pages
touch .nojekyll

# Ajouter les fichiers de documentation au staging git
echo "📦 Préparation du commit Git..."
git add "$MAN_HTML"
if [ -d "man/img" ]; then
    git add man/img/
fi
git add .nojekyll
if git status --porcelain README.md 2>/dev/null | grep -q "README.md"; then
    git add README.md
fi

# Vérifier s'il y a des modifications à commiter
if git diff --cached --quiet; then
    echo "ℹ️  Aucune modification détectée sur $MAN_HTML. Tout est déjà à jour."
else
    COMMIT_MSG="${1:-docs(man): mise à jour du manuel HTML interactif}"
    git commit -m "$COMMIT_MSG"
    echo "✅ Commit créé : $COMMIT_MSG"
fi

# Push vers le dépôt distant
echo "🚀 Envoi vers GitHub (origin main)..."
git push origin main

echo ""
echo "🎉 Le manuel man.html a été synchronisé et poussé avec succès !"
echo "🌐 Consultation en ligne (GitHub Pages) : https://jlehuen.github.io/MicroSim/man/man.html"
echo "📁 Fichier sur GitHub : https://github.com/jlehuen/MicroSim/blob/main/man/man.html"
