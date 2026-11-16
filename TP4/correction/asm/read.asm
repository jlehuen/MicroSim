;       ====== Programme principal #1 ======

        mov BL, 0xA0   ; Adresse de la chaîne 1
        call Read      ; Saisie de la chaîne 1
        mov BL, 0xB0   ; Adresse de la chaîne 2
        call Read      ; Saisie de la chaîne 2
        hlt

;       ====== Saisie d'une chaîne ======

Read:   in 0x01        ; AL <- code ASCII
        cmp AL, 0x0D   ; Action sur ENTER ?
        jz Fin1        ; Si oui on sort de la boucle
        mov [BL], AL   ; Si non on mémorise le code
        inc BL         ; Adresse suivante
        jmp Read       ; On boucle
Fin1:   mov AL, 0x00   ; AL <- NULL
        mov [BL], AL   ; Ajouter le code de fin de chaîne
        ret
