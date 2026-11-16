;       ====== Programme principal ======

        org 0x00       ; Adresse du programme
Deb:    mov BL, 0x50   ; Initialisation de l'index
Rep:    mov AL, [BL]   ; AL <- nouvelle combinaison
        cmp AL, 0xFF   ; Fin de la séquence ?
        jz Deb         ; Si oui on recommence
        out 0x02       ; Si non AL -> port 02
        inc BL         ; Octet suivant (durée)
        mov AL, [BL]   ; AL <- durée (paramètre de la routine)
        call Tempo     ; Appel de la routine de temporisation
        inc BL         ; Octet suivant (configuration)
        jmp Rep

;       ====== Table de données ======

        org 0x50       ; Adresse de la table de données
        db 0x84        ; 1000 0100 (rouge-vert)
        db 0x10        ; Temporisation longue (16)
        db 0x88        ; 1000 1000 (rouge-orange)
        db 0x03        ; Temporisation courte (3)
        db 0x90        ; 1001 0000 (rouge-rouge)
        db 0x03        ; Temporisation courte (3)
        db 0x30        ; 0011 0000 (vert-rouge)
        db 0x10        ; Temporisation longue (16)
        db 0x50        ; 0101 0000 (orange-rouge)
        db 0x03        ; Temporisation courte (3)
        db 0x90        ; 1001 0000 (rouge-rouge)
        db 0x03        ; Temporisation courte (3)
        db 0xFF        ; Code de fin de séquence

;       ====== Routine de temporisation ======

Tempo:  pushf          ; Sauvegarde de SR
        push AL        ; Sauvegarde de AL
Rep2:   dec AL         ; AL <- AL - 1
        jnz Rep2       ; Boucler si AL != 0
        pop AL         ; Restauration de AL
        popf           ; Restauration de SR
        ret            ; Fin de la routine
