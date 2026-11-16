# Correcteur automatique TP4 (`grader/`)

Module de correction automatique **non-binaire** pour les 10 programmes assembleur du TP4 (`feux1.asm` à `compare.asm`), basé sur le simulateur Java `microsim_8bits` en mode *headless* et un évaluateur Python.

---

## 1. Principe de notation (non-binaire)

Chaque exercice est noté sur **3 paliers** selon le barème défini dans [`TP4.yaml`](TP4.yaml) :

1. **Assemblage (`15 %`)** : le fichier s'assemble sans erreur via `Assembler.java`.
2. **Exécution comportementale graduée (`65 %`)** : tests dynamiques sur CPU *headless* avec tolérance sur :
   - les noms de labels et adresses `org` des routines (`Read`, `Print`, `Upcase`, `Compare`, `Tempo`) ;
   - les rotations circulaires de la séquence des feux ;
   - les erreurs classiques de constante (ex. bit `0x01` au lieu de `0x02` dans `temp.asm`, table hors `0x50` dans `feux2.asm`).
3. **Analyse statique des « bonnes idées » (`20 %`)** : présence des concepts clés (`push`/`pop`, masques `and`/`xor`/`shl`, `call`/`ret`, etc.).
   - Si le code **ne s'assemble pas**, l'analyse statique accorde tout de même jusqu'à **35 %** des points pour récompenser une démarche juste.

---

## 2. Utilisation rapide

Depuis la racine du dépôt :

```bash
# Afficher le manuel intégré
./eval-tp4.sh

# Évaluer un étudiant
./eval-tp4.sh TP4/correction/asm

# Évaluer un étudiant avec exports Excel et CSV
./eval-tp4.sh TP4/correction/asm --xlsx notes.xlsx --csv notes.csv

# Évaluer toute une promotion (un sous-dossier par étudiant) + anti-plagiat
./eval-tp4.sh --dir ./TP4 --plagiarism --xlsx notes_promo.xlsx --csv notes_promo.csv --json rapport_promo.json | tee rapport_promo.txt
```

*(Le script [`eval-tp4.sh`](../eval-tp4.sh) crée automatiquement `grader/.venv` et installe `requirements.txt` au premier lancement.)*

---

## 3. Retours pédagogiques par LLM local (Ollama)

Par défaut, lorsqu'un exercice échoue à l'assemblage ou obtient moins de `70 %` aux tests dynamiques, l'évaluateur interroge **Ollama** en local (`http://127.0.0.1:11434`) pour ajouter un court retour pédagogique `[IA] ...` dans le rapport.

### Installation et démarrage d'Ollama (macOS / Homebrew)

```bash
# 1. Installer Ollama
brew install ollama

# 2. Démarrer / arrêter le service Ollama en arrière-plan (ou `ollama serve` dans un terminal)
brew services start ollama
brew services stop ollama
brew services restart ollama
brew services list | grep ollama

# 3. Télécharger le modèle utilisé par défaut (~4,7 Go)
ollama pull qwen2.5-coder:7b
```

*(Si tu disposes de moins de RAM, un modèle plus léger comme `qwen2.5-coder:3b` ou `llama3.2:3b` fonctionne très bien.)*

### Options LLM du correcteur

```bash
# Utiliser un autre modèle local
./eval-tp4.sh copies/dupont --llm-model qwen2.5-coder:3b

# Désactiver complètement les retours IA
./eval-tp4.sh copies/dupont --no-llm
```

Variables d'environnement optionnelles :
- `OLLAMA_HOST` (défaut : `http://127.0.0.1:11434`)
- `OLLAMA_MODEL` (défaut : `qwen2.5-coder:7b`)
- `LLM_PROVIDER=openai` + `OPENAI_API_KEY` (si tu souhaites basculer sur une API compatible OpenAI)

Si Ollama n'est pas démarré (ou en cas d'erreur/timeout), l'évaluateur affiche un `[AVERTISSEMENT IA]` sur `stderr` et poursuit la correction sans bloquer la notation.

---

## 4. Exemples de lignes de commande

```bash
# Commande complète pour une promotion : évalue chaque étudiant, génère le tableau Excel et les rapports détaillés (JSON + texte)
./eval-tp4.sh copies_promo --dir --plagiarism --xlsx notes_promo.xlsx --json rapport_promo.json | tee rapport_promo.txt

# Afficher le manuel intégré
./eval-tp4.sh --help

# Tester l'installation sur la copie de référence (doit afficher 20.00 / 20.00)
./eval-tp4.sh TP4/correction/asm

# Évaluer un étudiant (avec retours IA locaux par défaut)
./eval-tp4.sh copies/dupont

# Évaluer un étudiant sans IA et exporter en JSON détaillé
./eval-tp4.sh copies/dupont --no-llm --json rapport_dupont.json

# Évaluer toute une promotion (un sous-dossier par étudiant) avec exports Excel + CSV
./eval-tp4.sh --dir copies_promo --xlsx notes_promo.xlsx --csv notes_promo.csv

# Évaluer une promotion avec détection de plagiat (onglet "Similarités" dans l'Excel)
./eval-tp4.sh --dir copies_promo --plagiarism --xlsx notes_promo.xlsx

# Ajuster le seuil de plagiat (90 %) et utiliser un modèle Ollama plus léger
./eval-tp4.sh --dir copies_promo --plagiarism --plagiarism-threshold 0.90 --llm-model qwen2.5-coder:3b --xlsx notes_promo.xlsx

# Utiliser un barème personnalisé
./eval-tp4.sh --dir copies_promo --spec mon_bareme.yaml --xlsx notes_promo.xlsx
```
