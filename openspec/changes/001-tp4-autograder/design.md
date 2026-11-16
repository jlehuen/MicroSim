# Architecture technique (`001-tp4-autograder`)

## 1. Découplage Java (`src/microsim/`)
- `PeripheralBus` (`microsim.simulator`) :
  - `int in(int port)`
  - `void out(int port, int value)`
- `SwingPeripheralBus` (`microsim.simulator`) : encapsule `KeyboardFrame.INSTANCE`, `LightsFrame.INSTANCE`, `HeaterFrame.INSTANCE`.
- `CPU(RAM memory, PeripheralBus bus)` : le constructeur historique `CPU(RAM memory)` délègue à `SwingPeripheralBus`.
- `HeadlessRunner` (`microsim.headless`) :
  - Assemble le `.asm` via `Assembler.assemble(source)`.
  - Configure la RAM, les registres (`BL`, `CL`, `DL`), l'adresse de départ (`--ip` ou `--call-addr` avec adresse de retour sentinelle), les touches clavier (`--keys`) et l'état du thermostat (`--heater`).
  - Exécute jusqu'à `halted`, `fault`, retour de sous-routine (`RET` sur sentinelle) ou `--max-cycles`.
  - Émet un JSON sur `stdout` avec : `assembled`, `error`, `cycles`, `halted`, `fault`, `timeout`, `registers`, `flags`, `ram`, `outputs`, `al_trace`.

## 2. Évaluateur Python (`grader/`)
- `TP4.yaml` : barème sur 20 points réparti sur les 10 fichiers `.asm`.
- `runner.py` : invoque le runner Java compilé (`class:lib/*`).
- `static_analyzer.py` : analyse lexicale/structurelle du `.asm` (détection de directives `ORG`/`DB`, registres, instructions `PUSH`/`POP`, masques `AND`/`XOR`/`SHL`, `CALL`/`RET`, `CMP`, etc.).
- `evaluator.py` : orchestre les scénarios dynamiques et l'analyse statique pour chaque exercice, produit un rapport structuré et une note sur 20.
- `eval_tp4.py` : CLI d'évaluation individuelle ou de promotion (`--dir`) avec export `.csv` / `.xlsx`.
