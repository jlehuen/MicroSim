Rep:    in 0x03        ; AL <- port 03
        and AL, 0x02   ; Isoler le bit 1 (seuil 20 °C)
        jz Fire        ; Si bit 1 == 0 (< 20 °C), allumer le brûleur
        mov AL, 0x00   ; Sinon éteindre le brûleur
        out 0x03       ; AL -> port 03
        jmp Rep        ; On boucle

;       ====== Allumage du brûleur ======

Fire:   mov AL, 0x80   ; AL <- 1000 0000
        out 0x03       ; Allumer le brûleur
        jmp Rep        ; On boucle
