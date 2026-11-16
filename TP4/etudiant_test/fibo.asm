        mov BL, 0x00
        mov AL, 0x01
Rep:    add AL, BL
        jnc Rep
        hlt
