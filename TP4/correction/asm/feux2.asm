;       ====== Programme ======

        org 0x00       ; Adresse du programme
Deb:    mov BL, 0x50   ; Initialisation de l'index
Rep:    mov AL, [BL]   ; AL <- nouvelle combinaison
        cmp AL, 0xFF   ; Fin de la séquence ?
        jz Deb         ; Si oui on recommence
        out 0x02       ; Si non AL -> port 02
        inc BL         ; Octet suivant
        jmp Rep

;       ====== Données ======

        org 0x50       ; Adresse de la table de données
        db 0x84        ; 1000 0100 (rouge-vert)
        db 0x88        ; 1000 1000 (rouge-orange)
        db 0x90        ; 1001 0000 (rouge-rouge)
        db 0x30        ; 0011 0000 (vert-rouge)
        db 0x50        ; 0101 0000 (orange-rouge)
        db 0x90        ; 1001 0000 (rouge-rouge)
        db 0xFF        ; Code de fin de séquence
