; Test des deux syntaxes pour IN et OUT :
; - IN <byte>        (équivalent à IN AL, <byte>)
; - IN <reg>, <byte>
; - OUT <byte>       (équivalent à OUT AL, <byte>)
; - OUT <reg>, <byte>

    IN 0x00             ; Lit le clavier dans AL
    OUT 0x07            ; Écrit AL sur la barre de LED (0x07)

    IN BL, 0x00         ; Lit le clavier dans BL
    OUT BL, 0x07        ; Écrit BL sur la barre de LED (0x07)

    IN AL, 0x00         ; Forme explicite avec AL
    OUT AL, 0x07        ; Forme explicite avec AL

    HLT
