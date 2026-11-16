;       ====== Programme principal #2 ======

        mov BL, 0xA0   ; Adresse de la chaîne
        call Read      ; Saisie de la chaîne

        mov BL, 0xA0   ; Adresse de la chaîne
        mov CL, 0x01   ; Ligne 1
        mov DL, 0x02   ; Colonne 2
        call Print     ; Affichage de la chaîne

        mov BL, 0xA0   ; Adresse de la chaîne
        mov CL, 0x02   ; Ligne 2
        mov DL, 0x06   ; Colonne 6
        call Print     ; Affichage de la chaîne
        hlt

;       ====== Saisie d'une chaîne ======

Read:   in 0x01
        cmp AL, 0x0D
        jz Fin1
        mov [BL], AL
        inc BL
        jmp Read
Fin1:   mov AL, 0x00
        mov [BL], AL
        ret

;       ====== Affichage d'une chaîne ======

Print:  mul CL, 0x10   ; Multiplier par la largeur de ligne (16)
        add CL, 0xC0   ; Ajouter l'adresse de début du terminal ASCII
        add CL, DL     ; Ajouter le numéro de colonne
Rep2:   mov AL, [BL]   ; Récupérer le code ASCII
        cmp AL, 0x00   ; Fin de la chaîne ?
        jz Fin2        ; Si oui on sort de la boucle
        mov [CL], AL   ; Si non afficher le caractère
        inc BL
        inc CL
        jmp Rep2
Fin2:   ret
