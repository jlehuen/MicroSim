;
; ====================================================
; Programme de test pour le Bandeau de LED 64x8 (Ports 0x09 / 0x0A)
; Démonstration : Défilement fluide de texte (Ticker)
;
; Contrôle :
;   - Port 0x09 : Registre de donnée (motif de colonne 8 bits, bit 0 en haut, bit 7 en bas)
;   - Port 0x0A : Registre de commande
;       0 = CLEAR      (Efface tout le bandeau)
;       1 = SHL        (Décalage d'une colonne vers la gauche)
;       2 = SHR        (Décalage d'une colonne vers la droite)
;       3 = WRITE_LEFT (Écrit la donnée en colonne 0)
;       4 = WRITE_RIGHT(Écrit la donnée en colonne 63)
;       5 = PUSH_RIGHT (Décalage gauche + Écrit la donnée en colonne 63)
;       6 = PUSH_LEFT  (Décalage droite + Écrit la donnée en colonne 0)
; ====================================================

        org 0x00

Start:
        ; 1. Efface tout le bandeau
        mov AL, 0
        out 0x0A

MainLoop:
        mov BL, 0x40        ; Pointeur vers les motifs de colonnes en 0x40

ColLoop:
        mov AL, [BL]        ; Lecture de la colonne
        cmp AL, 0xFF        ; Marqueur de fin de message ?
        jz MainLoop         ; Reboucle à l'infini

        out 0x09            ; Envoie l'octet de colonne sur le port 0x09
        mov AL, 5           ; Commande 5 : Décalage gauche + injection à droite
        out 0x0A            ; Port 0x0A -> Exécute PUSH_RIGHT

        inc BL
        call Delay          ; Temporisation pour la fluidité visuelle
        jmp ColLoop

; ----------------------------------------------------
; Routine Delay : temporisation logicielle
; ----------------------------------------------------
Delay:
        mov CL, 60
DelayLoop:
        dec CL
        jnz DelayLoop
        ret

; ====================================================
; Motifs graphiques des colonnes (Format 5x7 colonnes + 1 blanc)
; Message : "MICRO"
; ====================================================
        org 0x40

        ; Espace initial (4 colonnes vides)
        db 0x00
        db 0x00
        db 0x00
        db 0x00

        ; Lettre 'M' (5 colonnes)
        db 0x7F             ; XXXXXXX.
        db 0x02             ; .X......
        db 0x04             ; ..X.....
        db 0x02             ; .X......
        db 0x7F             ; XXXXXXX.
        db 0x00             ; espace

        ; Lettre 'I' (3 colonnes)
        db 0x41             ; X.....X.
        db 0x7F             ; XXXXXXX.
        db 0x41             ; X.....X.
        db 0x00             ; espace

        ; Lettre 'C' (5 colonnes)
        db 0x3E             ; .XXXXX..
        db 0x41             ; X.....X.
        db 0x41             ; X.....X.
        db 0x41             ; X.....X.
        db 0x22             ; .X...X..
        db 0x00             ; espace

        ; Lettre 'R' (5 colonnes)
        db 0x7F             ; XXXXXXX.
        db 0x09             ; X..X....
        db 0x19             ; X..XX...
        db 0x29             ; X..X.X..
        db 0x46             ; .XX...X.
        db 0x00             ; espace

        ; Lettre 'O' (5 colonnes)
        db 0x3E             ; .XXXXX..
        db 0x41             ; X.....X.
        db 0x41             ; X.....X.
        db 0x41             ; X.....X.
        db 0x3E             ; .XXXXX..
        db 0x00             ; espace

        ; Espaces de fin (8 colonnes vides)
        db 0x00
        db 0x00
        db 0x00
        db 0x00
        db 0x00
        db 0x00
        db 0x00
        db 0x00

        ; Sentinelle de fin
        db 0xFF
