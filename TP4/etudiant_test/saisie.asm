        mov AL, 0x00
        push AL
Rep1:   in 0x01
        push AL
        cmp AL, 0x0D
        jnz Rep1
        mov BL, 0xC0
Rep2:   pop AL
        cmp AL, 0x00
        jz Fin
        mov [BL], AL
        inc BL
        jmp Rep2
Fin:    hlt
