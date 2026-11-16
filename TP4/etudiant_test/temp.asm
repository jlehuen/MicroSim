Rep:    in 0x03
        and AL, 0x01
        jz Fire
        mov AL, 0x00
        out 0x03
        jmp Rep
Fire:   mov AL, 0x80
        out 0x03
        jmp Rep
