;       ====== Programme principal #4 ======

        mov BL, 0xA0   ; Adresse de la chaîne 1
        call Read      ; Saisie de la chaîne 1
        mov BL, 0xB0   ; Adresse de la chaîne 2
        call Read      ; Saisie de la chaîne 2

        mov CL, 0xA0   ; Adresse de la chaîne 1
        mov DL, 0xB0   ; Adresse de la chaîne 2
        call Compare   ; Comparer les deux chaînes

        mov AL, 0x04   ; AL <- 0000 0100 (vert par défaut)
        jz Fin         ; Terminer si Z = 1
        mov AL, 0x10   ; AL <- 0001 0000 (rouge si Z = 0)
Fin:    out 0x02       ; AL -> port 02
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

;       ====== Comparaison de deux chaînes ======

Compare:
        mov AL, [CL]   ; AL <- Code ASCII de la chaîne 1
        mov BL, [DL]   ; BL <- Code ASCII de la chaîne 2
        cmp AL, BL     ; Comparer les deux codes ASCII
        jnz Fin4       ; Si différents alors Z = 0
        cmp AL, 0x00   ; Fin de la chaîne 1 ?
        jz Fin4        ; Si oui alors chaînes égales et Z = 1
        inc CL
        inc DL
        jmp Compare
Fin4:   ret
