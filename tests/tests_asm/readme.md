Instructions pour l'utilisation des programmes de test


### 1. `test_mul_div.asm`

Ce test vérifie les opérations de base de `MUL` et `DIV`.

1.  Dans le simulateur, lancez le test avec la commande `run` :
    ```
    > run tests/test_mul_div.asm
    ```
2.  Affichez les registres :
    ```
    > regs
    ```
3.  **Vérification** :
    *   Le registre `AL` doit être `10` (`0x0A`).
    *   Les drapeaux doivent être à zéro (`S=0, Z=0, O=0, C=0`).

---

### 2. `test_jumps.asm`

Ce test vérifie les alias de saut.

1.  Lancez le test :
    ```
    > run tests/test_jumps.asm
    ```
2.  Affichez les registres :
    ```
    > regs
    ```
3.  **Vérification** :
    *   `CL` doit être `1` (le saut `JE` a fonctionné).
    *   `DL` doit être `1` (le saut `JNE` a fonctionné).

---

### 3. `test_logical.asm`

Ce test vérifie les instructions logiques et de décalage.

1.  Chargez le programme : `> load tests/test_logical.asm`
2.  **Test `AND`** :
    *   Exécutez 2 pas : `> step 2`
    *   Affichez les registres : `> regs`
    *   **Vérifiez** : `AL` = `0x88`. Drapeaux : `S=1, Z=0, O=0, C=0`.
3.  **Test `SHL`** :
    *   Réinitialisez : `> reset`, puis `> load tests/test_logical.asm`
    *   Exécutez 10 pas : `> step 10`
    *   Affichez les registres : `> regs`
    *   **Vérifiez** : `AL` = `0x54`. Drapeaux : `S=0, Z=0, O=0, C=1`.

---

### 4. `test_flags.asm`

Ce test est le plus important pour valider la nouvelle logique des drapeaux.

1.  Chargez le programme : `> load tests/test_flags.asm`
2.  **Test 1 : `ADD` avec débordement signé (`100 + 100`)**
    *   Exécutez 2 pas : `> step 2`
    *   Affichez les registres : `> regs`
    *   **Vérifiez** : `AL` = `0xC8` (-56). Drapeaux attendus : `S=1, Z=0, O=1, C=0`.
3.  **Test 2 : `SUB` avec débordement signé (`-100 - 100`)**
    *   Réinitialisez : `> reset`, puis `> load tests/test_flags.asm`
    *   Exécutez 4 pas : `> step 4`
    *   Affichez les registres : `> regs`
    *   **Vérifiez** : `AL` = `0x38` (56). Drapeaux attendus : `S=0, Z=0, O=1, C=1`.
4.  **Test 3 : `ADD` avec retenue non signée (`255 + 1`)**
    *   Réinitialisez : `> reset`, puis `> load tests/test_flags.asm`
    *   Exécutez 6 pas : `> step 6`
    *   Affichez les registres : `> regs`
    *   **Vérifiez** : `AL` = `0x00`. Drapeaux attendus : `S=0, Z=1, O=0, C=1`.
5.  **Test 4 : `SUB` avec emprunt non signé (`0 - 1`)**
    *   Réinitialisez : `> reset`, puis `> load tests/test_flags.asm`
    *   Exécutez 8 pas : `> step 8`
    *   Affichez les registres : `> regs`
    *   **Vérifiez** : `AL` = `0xFF` (-1). Drapeaux attendus : `S=1, Z=0, O=0, C=1`.
6.  **Test 5 : `CMP` avec emprunt (`10 - 20`)**
    *   Réinitialisez : `> reset`, puis `> load tests/test_flags.asm`
    *   Exécutez 10 pas : `> step 10`
    *   Affichez les registres : `> regs`
    *   **Vérifiez** : `AL` est inchangé (`0x0A`). Drapeaux attendus : `S=1, Z=0, O=0, C=1`.
