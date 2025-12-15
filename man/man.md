<style>
img[alt=center] {
	display: block;
	margin: 0 auto;
}
img[alt=sep] {
	display: block;
	margin: 0 auto;
	margin-top: 50px;
	margin-bottom: 40px;
}
pre[alt=term] {
	background: #1E2832;
	color: #7DCE10;
}
code {
	background: yellow;
}
</style>


# MicroSim 8 bits

## 1. Introduction

MicroSim est un simulateur de microprocesseur 8 bits conçu comme un outil pédagogique pour l'apprentissage de la programmation en langage machine x86 de type MASM. Il fournit un environnement complet : un éditeur de code avec coloration syntaxique, une visualisation des registres et de la mémoire RAM 256 octets (adresses 8 bits) ainsi que plusieurs périphériques virtuels interactifs.

L'objectif est d'illustrer les concepts fondamentaux de l'architecture des ordinateurs, tels que les registres, la mémoire, la pile, et le fonctionnement interne d'un CPU à travers un jeu d'instructions simple, mais complet et réaliste.

MicroSim est multiplateforme (car développé en Java) et comme il embarque sa propre machine virtuelle, il est autonome : pas besoin d'installer Java sur la machine hôte. Pour l'instant, seuls les binaires Macos et Linux sont packagés.

Ce projet est inspiré de deux autres projets open-source similaires :

- Le simulateur [x86 Microprocessor Simulator](https://github.com/dwhinham/Microprocessor-Simulator) de C. N. Bauers, hébergé par Dale Whinham.
- Le simulateur en ligne [Simple 8-bit Assembler Simulator](https://github.com/Schweigi/assembler-simulator) de Marco Schweighauser.

## 2. Interface principale

L'interface du simulateur comprends plusieurs zones qui vous permettent d'écrire, d'exécuter et de déboguer du code x86. Le contenu de la mémoire et des registres sont actualisés en continu pendant l'exécution, à vitesse ajustable ou en mode pas-à-pas :

![center](img/main.png)

### L'éditeur de code :

C'est la zone où vous écrivez votre programme en assembleur. L'éditeur offre une coloration syntaxique pour une meilleure lisibilité, ainsi qu'une fonction de reformatage (beautifier) afin de normaliser la mise en forme du code.

### Le panneau des registres :

Situé à gauche de l'éditeur, ce panneau affiche en temps réel l'état de tous les registres du CPU :

- **Registres généraux :** `AL`, `BL`, `CL`, `DL`
- **Registres spécialisés :** `IP` (Instruction Pointer) et `SP` (Stack Pointer)
- **Registre d'état (flags) :** `SR` affiche les drapeaux `...SZOCF` qui indiquent le résultat de la dernière opération.

### La barre d'outils :

Située au dessus de l'éditeur, elle donne accès aux fonctionnalités principales :

- Créer un nouveau fichier assembleur ou ouvrir un fichier existant ;
- Assembler le code source en code exécutable et le charger en mémoire ;
- Contrôler l'exécution du programme en continu ou en mode pas-à-pas ;
- Accéder à la mémoire RAM et aux périphériques virtuels.

### La mémoire RAM :

Il est possible de visualiser le contenu de la mémoire vive (RAM) sou forme d'une table de 16x16 octets notés sous forme hexadécimale ou ASCII. C'est un outil essentiel pour déboguer et comprendre comment vos données et votre code sont stockés. La visualisation indique également les emplacements pointés par les regitres `IP` (en rouge) et `SP` (en bleu) :

![center](img/ram.png)

## 3. Les périphériques virtuels

MicroSim inclus plusieurs périphériques d'entrée/sortie qui peuvent être contrôlés par programme :

### Le terminal ASCII (mapé en mémoire) :

C'est un petit écran texte de 4 lignes de 16 colonnes. Pour afficher un caractère, il suffit d'écrire son code ASCII en RAM dans la zone-mémoire située entre les adresses `0xC0` et `0xFF` (soit 64 caractères) :

![center](img/ascii.png)

### Le clavier (via le port 01) :

Le clavier permet d'entrer des caractères via l'intruction `IN 0x01` qui déclenche une attente d'une action sur le clavier physique de l'ordinateur. Le code ASCII de la touche actionnée sera placé dans le registre `AL` :

![center](img/keyboard.png)

### Les feux tricolores (via le port 02) :

Ce périphérique simule deux feux de circulation qu'il est possible de contrôler via l'intruction `OUT 0x02`. L'octet présent dans le regitre `AL` sera envoyé au périphérique :

![center](img/lights.png)

### Le système de chauffage (via le port 03) :

Ce périphérique représente un système de chauffage composé d'une chaudière pilotée via l'instruction `OUT 0x03` et d'un thermostat accessible via l'intruction `IN 0x03`. L'objectif est de piloter la chaudière pour maintenir une température constante :

![center](img/heater.png)

## 4. Guide de démarrage rapide

1.  **Écrivez votre code :** Tapez votre programme dans l'éditeur de code.
2.  **Assemblez-le :** Cliquez sur le bouton "Assembler". Les erreurs de syntaxe seront affichées dans une fenêtre de dialogue.
3.  **Exécutez votre programme :**
    - Cliquez sur "Exécuter" pour lancer le programme et réglez la vitesse d'exécution.
    - Cliquez sur "Pas à pas" pour exécuter une instruction à la fois et observer les effets de chaque instruction sur les registres et la mémoire.
4.  **Déboguez :** Utilisez le mode "Pas à pas" et les fenêtres des périphériques pour trouver et corriger les erreurs dans votre logique.

![sep](img/sep.png)

<center>
# Manuel de référence de l'assembleur
</center>

## 1. Concepts de base

### Les registres `AL`, `BL`, `CL`, `DL`, `SP`, `IP`, `SR` :

Les registres sont des zones de stockage ultra-rapides intégrées au CPU. Leur taille correspond à la taille des bus internes du microprocesseur (soit 8 bits), Pour manipuler une donnée, le processeur doit d'abord la charger depuis la mémoire dans un de ses registres. On distingue les registres à usage général et les registres spécialisés :

- **Registres généraux `AL`, `BL`, `CL`, `DL` :** Utilisés pour les calculs (`ADD`, `SUB`, etc.), les comparaisons (`CMP`) et le stockage temporaire de données (`MOV`).
- **Pointeur de pile `SP` :** Un registre spécial qui contient l'adresse du sommet de la pile. Il est modifié par les instructions dédiées `PUSH` et `POP`.
- **Pointeur d'instruction `IP` :** Pointe toujours vers la prochaine instruction à exécuter. Vous ne le modifiez jamais directement, mais via les instructions de saut (`JMP`, `CALL`, etc.).
- **Le registre d'état `SR` :** Contient des drapeaux (flags) qui donnent des informations sur le résultat de la dernière opération effectuée. C'est un octet de la forme `...SZOCF` où chaque lettre correspond à un bit significatif :

| Bit | Nom | Description |
|:---:|:---:|---|
| `S` | Sign | indique un résultat **négatif** pour les nombres **signés en complément à 2** (correspond à la valeur du MSB) |
| `Z` | Zero | indique un résultat égal à zéro |
| `O` | Overflow | indique un **dépassement de capacité** pour les nombres **signés en complément à 2** lorsque le résultat d'une opération sort de l'intervalle `[-128, 127]` |
| `C` | Carry | indique un **dépassement de capacité** pour les nombres **non signés** suite à une addition (retenue) ou d'une soustraction (emprunt) |
| `F` | Fault | indique une erreur irrécupérable causée par :<br>- Division par zéro<br>- Débordement de pile (Stack Overflow) lorsque `SP < 0x80`<br>- Sous-dépassement de pile (Stack Underflow) lorsque `SP > 0xBF`<br>- Pointeur d'instruction `IP` hors des limites de la mémoire<br>- Code d'opération (Opcode) invalide ou inconnu |

### La mémoire adressable :

La mémoire peut être vue comme une **grande armoire avec des tiroirs numérotés**. Chaque emplacement contient un octet (8 bits) et possède une adresse unique. Vous pouvez y stocker des données à plus long terme que dans les registres. On y accède via les syntaxes `[adresse]` et `[registre]` (notation crochet).

### La pile (stack) :

La pile est une **zone de mémoire spéciale organisée comme une pile d'assiettes** (c'est une structure de données LIFO : Last In / First Out). Elle est essentielle pour appeler des procédures (subroutines) et sauvegarder l'état du processeur.

Dans une architecture x86, la pile grandit vers les adresses basses et `SP` pointe toujours vers la **case mémoire vide** juste en dessous du dernier élément ajouté. Dans MicroSim, le pointeur `SP` est initialisé à l'adresse `0xBF`, juste avant la mémoire du terminal ASCII.

**Schéma de la pile après `PUSH AL` puis `PUSH BL` :**

```
           Mémoire
        +-----------+
0xBD    |    ...    | <--- SP (Pointeur de la pile)
        +-----------+
0xBE    | Valeur BL | <--- [SP+1]
        +-----------+
0xBF    | Valeur AL | <--- [SP+2] (Base de la pile)
        +-----------+
        |    ...    |
```

### Légende des opérandes :

Une instruction est composée d'un **code** (opcode) et éventuellement de plusieurs **opérandes**. Certaines instructions (comme `HLT`) n'ont pas d'opérande, d'autres (comme `OUT`) en ont une seule, d'autre encore (comme `MOV`) en ont deux. Les notations des opérandes sont données dans le tableau suivant :

| Opérande | Description | Exemples |
|---|---|---|
| `reg` | Un des registres généraux 8 bits : `AL`, `BL`, `CL`, `DL` | `AL` |
| `[reg]` | Une adresse mémoire contenue dans un registre | `[BL]` |
| `[addr]` | Une adresse mémoire directe | `[0x50]`  |
| `[SP+offset]` | Adresse mémoire relative au pointeur de pile avec un offset (décalage) | `[SP+4]` |
| `byte` | Un octet noté en hexadécimal ou en décimal | `0xFF`, `255` |
| `addr` | Une adresse notée en hexadécimal ou en décimal (utilisé par les sauts) | `0x50`, `80` |
| `label` | Une étiquette (utilisé par les sauts) | `my_label` |
| `char` | Un caractère littéral | `'A'` |
| `string` | Une chaîne de caractères littérale | `"Hello"` |


### Tableau de référence des flags par instructions :

Ce second tableau résume quelles instructions affectent quels drapeaux :

- **`-`** indique que le flag n'est pas affecté par l'instruction
- **`✓`** indique que le flag est modifié en fonction du résultat
- **`0`** indique que le flag est toujours mis à 0

| Instruction | SF | ZF | OF | CF |
|:---:|:---:|:---:|:---:|:---:|
| `MOV`, `PUSH`, `POP` | - | - | - | - |
| `ADD`, `SUB`, `CMP`, `NEG` | ✓ | ✓ | ✓ | ✓ |
| `INC` | ✓ | ✓ | ✓ | ✓ |
| `DEC` | ✓ | ✓ | ✓ | - |
| `AND`, `OR`, `XOR` | ✓ | ✓ | 0 | 0 |
| `NOT` | ✓ | ✓ | 0 | 0 |
| `SHL`, `SHR` | ✓ | ✓ | 0 | ✓ |
| `MUL` | ✓ | ✓ | ✓ | ✓ |
| `DIV` | ✓ | ✓ | 0 | 0 |
| `Jumps`, `CALL`, `RET` | - | - | - | - |
| `PUSHF`, `POPF` | ✓ | ✓ | ✓ | ✓ |

## 2. Programme de démonstration

L'incontournable "Hello World" implémenté dans MicroSim :

```
; ----- Section programme ------------

	mov	CL, 0xC0  ; Adresse d'affichage
	mov	BL, 0x50  ; Adresse de la chaîne
Rep:
	mov	AL, [BL]  ; Adresse du caractère
	cmp	AL, 0x00  ; Fin de la chaîne ?
	jz	Fin
	mov	[CL], AL  ; Afficher le caractère
	inc	CL        ; Caractère suivant
	inc	BL        ; Emplacement suivant
	jmp	Rep
Fin:
	hlt

; ----- Section données ------------

	org	0x50
	
	db	"Hello World"
	db	0x00
```

## 3. Directives (commandes) d'assemblage

#### `ORG` (origin)
- **Description :** Détermine l'adresse de la prochaine instruction (ou donnée) à charger.
- **Syntaxe :**
    - `ORG addr`

#### `DB` (define byte)
- **Description :** Alloue et initialise un ou plusieurs octets de données.
- **Syntaxe :
    - `DB byte`
    - `DB 'char'`
    - `DB "string"`

## 4. Jeu d'instructions du micro-processeur

#### `HLT`
- **Description :** Arrête l'exécution du programme.
- **Syntaxe :** `HLT`

### Instructions de mouvement de données :

#### `MOV`
- **Description :** Copie une valeur d'une source vers une destination.
- **Syntaxes :**
    - `MOV reg, reg`
    - `MOV reg, [addr]`
    - `MOV reg, [reg]`
    - `MOV reg, [SP+offset]`
    - `MOV reg, byte`
    - `MOV [addr], reg`
    - `MOV [reg], reg`
    - `MOV [SP+offset], reg`
    - `MOV [addr], byte`
    - `MOV [reg], byte`

### Instructions arithmétiques :

#### `INC`
- **Description :** Incrémente la valeur d'un registre de 1.
- **Syntaxe :** `INC reg`

#### `DEC`
- **Description :** Décrémente la valeur d'un registre de 1.
- **Syntaxe :** `DEC reg`

#### `NEG`
- **Description :** Effectue un complément à 2 sur la valeur d'un registre.
- **Syntaxe :** `NEG reg`

#### `ADD`
- **Description :** Additionne la valeur de la source au registre de destination.
- **Syntaxes :**
    - `ADD reg, reg`
    - `ADD reg, [reg]`
    - `ADD reg, [addr]`
    - `ADD reg, byte`

#### `SUB`
- **Description :** Soustrait la valeur de la source du registre de destination.
- **Syntaxes :**
    - `SUB reg, reg`
    - `SUB reg, [reg]`
    - `SUB reg, [addr]`
    - `SUB reg, byte`

#### `MUL`
- **Description : :** Multiplie la valeur de la source au registre de destination.
- **Syntaxes :**
    - `MUL reg, reg`
    - `MUL reg, [reg]`
    - `MUL reg, [addr]`
    - `MUL reg, byte`

#### `DIV`
- **Description :** Divise la valeur de la source au registre de destination.
- **Syntaxes :**
    - `DIV reg, reg`
    - `DIV reg, [reg]`
    - `DIV reg, [addr]`
    - `DIV reg, byte`

### Instructions logiques :

#### `AND`
- **Description :** Effectue un ET logique (bitwise AND) entre le registre de destination et la source.
- **Syntaxes :**
    - `AND reg, reg`
    - `AND reg, [reg]`
    - `AND reg, [addr]`
    - `AND reg, byte`

#### `OR`
- **Description : :** Effectue un OU logique (bitwise OR) entre le registre de destination et la source.
- **Syntaxes :**
    - `OR reg, reg`
    - `OR reg, [reg]`
    - `OR reg, [addr]`
    - `OR reg, byte`

#### `XOR`
- **Description : :** Effectue un OU exclusif (bitwise XOR) entre le registre de destination et la source.
- **Syntaxes :**
    - `XOR reg, reg`
    - `XOR reg, [reg]`
    - `XOR reg, [addr]`
    - `XOR reg, byte`

#### `NOT`
- **Description : :** Inverse tous les bits d'un registre (bitwise NOT).
- **Syntaxe :** `NOT reg`

#### `SHL` (Shift Left)
- **Description : :** Décale les bits du registre de destination vers la gauche.
- **Syntaxes :**
    - `SHL reg, reg`
    - `SHL reg, [reg]`
    - `SHL reg, [addr]`
    - `SHL reg, byte`

#### `SHR` (Shift Right)
- **Description : :** Décale les bits du registre de destination vers la droite.
- **Syntaxes :**
    - `SHR reg, reg`
    - `SHR reg, [reg]`
    - `SHR reg, [addr]`
    - `SHR reg, byte`

### Instructions de comparaison et de saut :

#### `CMP`
- **Description : :** Compare deux valeurs et met à jour les drapeaux (Zero, Carry) en fonction du résultat de la soustraction (destination - source).
- **Syntaxes :**
    - `CMP reg, reg`
    - `CMP reg, [reg]`
    - `CMP reg, [addr]`
    - `CMP reg, byte`

#### `JMP`
- **Description : :** Saut inconditionnel vers une adresse (ou un label)
- **Syntaxes :**
    - `JMP addr`
    - `JMP label`
    - `JMP [reg]`

#### `JZ` et `JE` (Jump if Zero / Jump if Equal)
- **Description : :** Saute si le drapeau Zéro (Zero Flag) est à 1.
- **Syntaxes :**
    - `JZ addr`
    - `JZ label`
    - `JZ [reg]`

#### `JNZ` et `JNE` (Jump if Not Zero / Jump if Not Equal)
- **Description : :** Saute si le drapeau Zéro (Zero Flag) est à 0.
- **Syntaxes :**
    - `JNZ addr`
    - `JNZ label`
    - `JNZ [reg]`

#### `JC` et `JB` (Jump if Carry / Jump if Below)
- **Description : :** Saute si le drapeau de retenue (Carry Flag) est à 1.
- **Syntaxes :**
    - `JC addr`
    - `JC label`
    - `JC [reg]`

#### `JNC` et `JAE` (Jump if No Carry / Jump if Above or Equal)
- **Description : :** Saute si le drapeau de retenue (Carry Flag) est à 0.
- **Syntaxes :**
    - `JNC addr`
    - `JNC label`
    - `JNC [reg]`

#### `JA` et `JNBE` (Jump if Above / Jump if Not Below or Equal)
- **Description : :** Saute si les drapeaux Zéro et Retenue sont tous les deux à 0.
- **Syntaxes :**
    - `JA addr`
    - `JA label`
    - `JA [reg]`

#### `JNA` et `JBE` (Jump if Not Above / Jump if Below or Equal)
- **Description : :** Saute si le drapeau Zéro ou le drapeau Retenue est à 1.
- **Syntaxes :**
    - `JNA addr`
    - `JNA label`
    - `JNA [reg]`

#### `JS` (Jump if Sign)
- **Description : :** Saute si le drapeau de signe (Sign Flag) est à 1 (résultat négatif).
- **Syntaxes :**
    - `JS addr`
    - `JS label`
    - `JS [reg]`

#### `JNS` (Jump if Not Sign)
- **Description : :** Saute si le drapeau de signe (Sign Flag) est à 0 (résultat positif ou nul).
- **Syntaxes :**
    - `JNS addr`
    - `JNS label`
    - `JNS [reg]`

### Instructions de pile (stack) :

#### `PUSH`
- **Description : :** Empile une valeur sur la pile.
- **Syntaxes :**
    - `PUSH reg`
    - `PUSH [reg]`
    - `PUSH [addr]`
    - `PUSH byte`

#### `PUSHF`
- **Description : :** Empile le contenu du registre SR (sauvegarde des flags).
- **Syntaxe :** `PUSHF`

#### `POP`
- **Description : :** Dépile une valeur de la pile vers un registre.
- **Syntaxe :** `POP reg`

#### `POPF`
- **Description : :** Dépile vers le registre SR (restauration des flags).
- **Syntaxe :** `POPF`

### Instructions de sous-programmes (subroutines) :

#### `CALL`
- **Description : :** Appelle un sous-programme en empilant l'adresse de retour et en sautant à l'adresse du sous-programme.
- **Syntaxes :**
    - `CALL addr`
    - `CALL label`
    - `CALL [reg]`

#### `RET`
- **Description : :** Revient d'un sous-programme en dépilant l'adresse de retour de la pile.
- **Syntaxe :** `RET`

## 5. Codage hexadécimal

Le processeur ne comprend pas directement les instructions textuelles comme `MOV AL, 10`. L'assembleur se charge de traduire chaque ligne de code en une séquence d'octets que le CPU peut exécuter : c'est le **code machine**.

Chaque instruction est convertie en un premier octet appelé **code opératoire** (ou *opcode*), qui identifie de manière unique l'opération et ses types d'opérandes. Cet opcode est ensuite suivi en mémoire par les octets représentant les arguments (les *opérandes*), comme un numéro de registre, une valeur numérique ou une adresse.

Les tableaux suivants détaillent ces codes numériques et le format complet en mémoire pour chaque composant d'une instruction.

### Codes des Registres

Toute opérande de type `reg` est codée sur un octet :

| Registre | Code Hexa |
|---|---|
| `AL` | `0x00` |
| `BL` | `0x01` |
| `CL` | `0x02` |
| `DL` | `0x03` |
| `SP` | `0x04` |

### Codes des Instructions (Opcodes)

#### Contrôle
| Mnémonique | Syntaxe Complète | Opcode | Adressage | Format en Mémoire |
|:---|:---|:---:|:---|:---|
| `HLT` | `HLT` | `0x00` | Implicite | `00` |

#### Mouvement de données
| Mnémonique | Syntaxe Complète | Opcode | Adressage | Format en Mémoire |
|:---|:---|:---:|:---|:---|
| `MOV` | `MOV reg, byte` | `0x01` | Immédiat | `01 reg byte` |
| | `MOV reg, reg` | `0x02` | Registre | `02 reg reg` |
| | `MOV reg, [addr]` | `0x03` | Direct | `03 reg addr` |
| | `MOV reg, [reg]` | `0x04` | Indirect | `04 reg reg` |
| | `MOV reg, [SP+offset]` | `0x05` | Offset | `05 reg offset`|
| | `MOV [addr], byte` | `0x06` | Immédiat | `06 addr byte` |
| | `MOV [addr], reg` | `0x07` | Registre | `07 addr reg` |
| | `MOV [reg], byte` | `0x08` | Immédiat | `08 reg byte` |
| | `MOV [reg], reg` | `0x09` | Registre | `09 reg reg` |
| | `MOV [SP+offset], reg`| `0x0A` | Registre | `0A offset reg`|

#### Arithmétique
| Mnémonique | Syntaxe Complète | Opcode | Adressage | Format en Mémoire |
|:---|:---|:---:|:---|:---|
| `ADD` | `ADD reg, byte` | `0x0B` | Immédiat | `0B reg byte` |
| | `ADD reg, reg` | `0x0C` | Registre | `0C reg reg` |
| | `ADD reg, [addr]` | `0x0D` | Direct | `0D reg addr` |
| | `ADD reg, [reg]` | `0x0E` | Indirect | `0E reg reg` |
| `SUB` | `SUB reg, byte` | `0x0F` | Immédiat | `0F reg byte` |
| | `SUB reg, reg` | `0x10` | Registre | `10 reg reg` |
| | `SUB reg, [addr]` | `0x11` | Direct | `11 reg addr` |
| | `SUB reg, [reg]` | `0x12` | Indirect | `12 reg reg` |
| `INC` | `INC reg` | `0x13` | Registre | `13 reg` |
| `DEC` | `DEC reg` | `0x14` | Registre | `14 reg` |
| `NEG` | `NEG reg` | `0x15` | Registre | `15 reg` |
| `MUL` | `MUL reg, byte` | `0x36` | Immédiat | `36 reg byte` |
| | `MUL reg, reg` | `0x37` | Registre | `37 reg reg` |
| | `MUL reg, [addr]` | `0x38` | Direct | `38 reg addr` |
| | `MUL reg, [reg]` | `0x39` | Indirect | `39 reg reg` |
| `DIV` | `DIV reg, byte` | `0x3A` | Immédiat | `3A reg byte` |
| | `DIV reg, reg` | `0x3B` | Registre | `3B reg reg` |
| | `DIV reg, [addr]` | `0x3C` | Direct | `3C reg addr` |
| | `DIV reg, [reg]` | `0x3D` | Indirect | `3D reg reg` |

#### Comparaison
| Mnémonique | Syntaxe Complète | Opcode | Adressage | Format en Mémoire |
|:---|:---|:---:|:---|:---|
| `CMP` | `CMP reg, byte` | `0x16` | Immédiat | `16 reg byte` |
| | `CMP reg, reg` | `0x17` | Registre | `17 reg reg` |
| | `CMP reg, [addr]` | `0x18` | Direct | `18 reg addr` |
| | `CMP reg, [reg]` | `0x19` | Indirect | `19 reg reg` |

#### Logique
| Instruction | Syntaxe Complète | Opcode | Adressage | Format en Mémoire |
|:---|:---|:---:|:---|:---|
| `AND` | `AND reg, byte` | `0x3E` | Immédiat | `3E reg byte` |
| | `AND reg, reg` | `0x3F` | Registre | `3F reg reg` |
| | `AND reg, [addr]` | `0x40` | Direct | `40 reg addr` |
| | `AND reg, [reg]` | `0x41` | Indirect | `41 reg reg` |
| `OR` | `OR reg, byte` | `0x42` | Immédiat | `42 reg byte` |
| | `OR reg, reg` | `0x43` | Registre | `43 reg reg` |
| | `OR reg, [addr]` | `0x44` | Direct | `44 reg addr` |
| | `OR reg, [reg]` | `0x45` | Indirect | `45 reg reg` |
| `XOR` | `XOR reg, byte` | `0x46` | Immédiat | `46 reg byte` |
| | `XOR reg, reg` | `0x47` | Registre | `47 reg reg` |
| | `XOR reg, [addr]` | `0x48` | Direct | `48 reg addr` |
| | `XOR reg, [reg]` | `0x49` | Indirect | `49 reg reg` |
| `NOT` | `NOT reg` | `0x4A` | Registre | `4A reg` |
| `SHL` | `SHL reg, byte` | `0x4B` | Immédiat | `4B reg byte` |
| | `SHL reg, reg` | `0x4C` | Registre | `4C reg reg` |
| | `SHL reg, [addr]` | `0x4D` | Direct | `4D reg addr` |
| | `SHL reg, [reg]` | `0x4E` | Indirect | `4E reg reg` |
| `SHR` | `SHR reg, byte` | `0x4F` | Immédiat | `4F reg byte` |
| | `SHR reg, reg` | `0x50` | Registre | `50 reg reg` |
| | `SHR reg, [addr]` | `0x51` | Direct | `51 reg addr` |
| | `SHR reg, [reg]` | `0x52` | Indirect | `52 reg reg` |

#### Sauts et Sous-programmes
| Mnémonique | Syntaxe Complète | Opcode | Adressage | Format en Mémoire |
|:---|:---|:---:|:---|:---|
| `JMP` | `JMP addr` | `0x1A` | Direct | `1A addr` |
| | `JMP [reg]` | `0x1B` | Indirect | `1B reg` |
| `JC` / `JB` | `JC addr` | `0x1C` | Direct | `1C addr` |
| | `JC [reg]` | `0x1D` | Indirect | `1D reg` |
| `JNC`/`JAE` | `JNC addr` | `0x1E` | Direct | `1E addr` |
| | `JNC [reg]` | `0x1F` | Indirect | `1F reg` |
| `JZ` / `JE` | `JZ addr` | `0x20` | Direct | `20 addr` |
| | `JZ [reg]` | `0x21` | Indirect | `21 reg` |
| `JNZ`/`JNE` | `JNZ addr` | `0x22` | Direct | `22 addr` |
| | `JNZ [reg]` | `0x23` | Indirect | `23 reg` |
| `JA` /`JNBE` | `JA addr` | `0x24` | Direct | `24 addr` |
| | `JA [reg]` | `0x25` | Indirect | `25 reg` |
| `JNA`/`JBE` | `JNA addr` | `0x26` | Direct | `26 addr` |
| | `JNA [reg]` | `0x27` | Indirect | `27 reg` |
| `JS` | `JS addr` | `0x28` | Direct | `28 addr` |
| | `JS [reg]` | `0x29` | Indirect | `29 reg` |
| `JNS` | `JNS addr` | `0x2A` | Direct | `2A addr` |
| | `JNS [reg]` | `0x2B` | Indirect | `2B reg` |
| `CALL` | `CALL addr` | `0x33` | Direct | `33 addr` |
| | `CALL [reg]` | `0x34` | Indirect | `34 reg` |
| `RET` | `RET` | `0x35` | Implicite | `35` |

#### Pile
| Mnémonique | Syntaxe Complète | Opcode | Adressage | Format en Mémoire |
|:---|:---|:---:|:---|:---|
| `PUSH` | `PUSH byte` | `0x2C` | Immédiat | `2C byte` |
| | `PUSH reg` | `0x2D` | Registre | `2D reg` |
| | `PUSH [addr]` | `0x2E` | Direct | `2E addr` |
| | `PUSH [reg]` | `0x2F` | Indirect | `2F reg` |
| `POP` | `POP reg` | `0x30` | Registre | `30 reg` |
| `PUSHF`| `PUSHF` | `0x31` | Implicite | `31` |
| `POPF` | `POPF` | `0x32` | Implicite | `32` |

#### Entrée/Sortie
| Mnémonique | Syntaxe Complète | Opcode | Adressage | Format en Mémoire |
|:---|:---|:---:|:---|:---|
| `OUT` | `OUT byte` | `0x53` | Immédiat | `53 byte` |
| `IN` | `IN byte` | `0x54` | Immédiat | `54 byte` |
