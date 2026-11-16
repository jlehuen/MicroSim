;       ====== Saisie de la chaîne ======

        mov AL, 0x00   ; Code de fin de chaîne (sentinelle)
        push AL        ; Empiler la sentinelle
Rep1:   in 0x01        ; AL <- code ASCII de la touche
        push AL        ; Empiler le code ASCII
        cmp AL, 0x0D   ; Code ASCII de la touche Entrée ?
        jnz Rep1       ; Si non on recommence
        pop AL         ; Dépiler le code 0x0D (Entrée)

;       ====== Affichage de la chaîne ======

        mov BL, 0xC0   ; Adresse du terminal ASCII
Rep2:   pop AL         ; AL <- code ASCII dépilé
        cmp AL, 0x00   ; Sentinelle atteinte (pile vide) ?
        jz Fin         ; Si oui on sort de la boucle
        mov [BL], AL   ; Envoyer le caractère vers le terminal ASCII
        inc BL         ; Emplacement suivant dans le terminal
        jmp Rep2       ; On boucle
Fin:    hlt
