# Spécification vivante : Évaluateur automatique TP4 (`tp4_grader`)

## Exigences fonctionnelles

### `REQ-01` — Découplage E/S et exécution *headless* fidèle
- `CPU.java` doit déléguer les opérations `IN` et `OUT` à une interface `PeripheralBus`.
- L'interface graphique Swing conserve exactement son comportement actuel via `SwingPeripheralBus`.
- `HeadlessRunner` assemble et exécute un fichier `.asm` sans aucune dépendance graphique, en réutilisant `Assembler`, `RAM` et `CPU`.

### `REQ-02` — Paramétrage des scénarios et capture de trace JSON
- Le runner Java accepte : le chemin du `.asm`, un budget maximal de cycles (`--max-cycles`), une file de touches clavier (`--keys`), une valeur de capteur thermostat (`--heater`), des valeurs initiales de registres (`BL`, `CL`, `DL`), d'octets en RAM, et un mode d'appel de sous-routine (`--call-addr`).
- Il produit sur `stdout` un objet JSON contenant : `assembled`, `error`, `halted`, `fault`, `timeout`, `cycles`, `registers`, `flags`, `ram` (256 octets), `outputs` (trace des `OUT` avec `cycle`, `port`, `value`) et `register_trace` (optionnel pour `AL`).

### `REQ-03` — Notation non-binaire à 3 paliers
- Chaque exercice (sur les 10 fichiers du TP4 : `feux1.asm`, `feux2.asm`, `saisie.asm`, `temp.asm`, `fibo.asm`, `feux3.asm`, `read.asm`, `print.asm`, `upcase.asm`, `compare.asm`) est noté progressivement :
  1. **Assemblage** : points accordés si le code s'assemble sans erreur.
  2. **Comportement dynamique** : points gradués par sous-critères observables sur les traces JSON (séquence des feux à rotation près, test unitaire des routines `0x20`/`0x40`/`0x60`/`0x80`, suite de Fibonacci, régulation thermostat, inversion sur pile).
  3. **Analyse statique (« bonnes idées »)** : si l'exécution échoue ou est incomplète, attribution de points partiels sur la présence d'instructions et de motifs algorithmiques pertinents.

### `REQ-04` — Évaluation individuelle, promotion et exports
- L'évaluateur Python (`grader/eval_tp4.py`) permet d'évaluer un dossier étudiant ou une promotion complète (`--dir`).
- Il génère un rapport détaillé par étudiant (note globale sur 20, détail par fichier et justification des points) ainsi que des exports `.csv` et `.xlsx`.
