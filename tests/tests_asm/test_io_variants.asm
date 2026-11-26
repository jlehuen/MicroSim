; Test des deux syntaxes pour IN et OUT :
; - IN <byte>        (équivalent à IN AL, <byte>)
; - IN <reg>, <byte>
; - OUT <byte>       (équivalent à OUT AL, <byte>)
; - OUT <reg>, <byte>

    IN 0x00             ; Lit le clavier dans AL
    OUT 0x06            ; Écrit AL sur la barre de LED (0x06)

    IN BL, 0x00         ; Lit le clavier dans BL
    OUT BL, 0x06        ; Écrit BL sur la barre de LED (0x06)

    IN AL, 0x00         ; Forme explicite avec AL
    OUT AL, 0x06        ; Forme explicite avec AL

    HLT
