        mov BL, 0x00   ; BL <- 0 (Fn-2)
        mov AL, 0x01   ; AL <- 1 (Fn-1)
Rep:    push AL        ; Sauvegarder AL sur la pile
        add AL, BL     ; AL <- AL + BL (Fn-1 + Fn-2)
        pop BL         ; Restaurer l'ancien AL dans BL
        jnc Rep        ; Tant qu'il n'y a pas de retenue (C == 0), continuer
        hlt            ; Sinon c'est terminé
