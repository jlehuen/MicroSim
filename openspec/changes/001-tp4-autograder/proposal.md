# Proposition 001 : Module de correction automatique non-binaire du TP4

## Motivation
L'évaluation manuelle des 10 programmes assembleur du TP4 (`feux1.asm` à `compare.asm`) sur toute une promotion est chronophage et sujette à variabilité. Une correction purement binaire (tout ou rien) serait toutefois injuste en L1 : un étudiant dont le programme boucle mal ou échoue à l'assemblage peut néanmoins avoir mis en œuvre les bonnes notions (pile, masque binaire, table de données, appels de routines).

## Portée
- Découplage des périphériques Swing de `CPU.java` via une interface `PeripheralBus`.
- Ajout d'un runner CLI Java *headless* (`microsim.headless.HeadlessRunner`) produisant une trace JSON déterministe.
- Ajout d'un évaluateur Python (`grader/`) mettant en œuvre la notation à 3 paliers (assemblage, exécution comportementale graduée, analyse statique des « bonnes idées »), avec barème YAML et exports CSV/Excel.
