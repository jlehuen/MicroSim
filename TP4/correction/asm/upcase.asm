;       ====== Programme principal #3 ======

        mov BL, 0xA0   ; Adresse de la chaîne
        call Read      ; Saisie de la chaîne

        mov BL, 0xA0   ; Adresse de la chaîne
        call Upcase    ; Capitalisation de la chaîne

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

Print:  mul CL, 0x10
        add CL, 0xC0
        add CL, DL
Rep2:   mov AL, [BL]
        cmp AL, 0x00
        jz Fin2
        mov [CL], AL
        inc BL
        inc CL
        jmp Rep2
Fin2:   ret

;       ====== Capitalisation d'une chaîne ======

Upcase: mov AL, [BL]   ; AL <- code ASCII
        cmp AL, 0x00   ; Fin de la chaîne ?
        jz Fin3        ; Si oui on sort de la boucle
        sub AL, 0x20   ; Si non AL <- AL - 0x20 (32 en décimal)
        mov [BL], AL   ; Remplacer le code ASCII en mémoire
        inc BL
        jmp Upcase
Fin3:   ret
