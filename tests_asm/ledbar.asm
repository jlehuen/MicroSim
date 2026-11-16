;
; Programme de test pour la barre de 8 LED (Port 0x06)
; Démonstration : chenillard aller-retour (K2000)
;

Start:
    mov AL, 0x01        ; Commence avec la LED 0 (bit 0)

ToLeft:
    out 0x06            ; AL -> port 0x06 (barre de LED)
    shl AL, 1           ; Décalage vers la gauche
    cmp AL, 0x80        ; A-t-on atteint la LED 7 ?
    jnz ToLeft

ToRight:
    out 0x06            ; AL -> port 0x06 (barre de LED)
    shr AL, 1           ; Décalage vers la droite
    cmp AL, 0x01        ; A-t-on atteint la LED 0 ?
    jnz ToRight

    jmp ToLeft
