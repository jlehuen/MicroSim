;
; ====================================================
; Programme de test pour le Buzzer 8-bit (Ports 0x04 / 0x05)
; Démonstration : Thème de Super Mario Bros
;
; Contrôle :
;   - Port 0x04 : Note MIDI (1 à 127, 0 = silence)
;   - Port 0x05 : Durée automatique en x10 ms
;   - IN AL, 0x05 : 1 si le son/durée est actif, 0 si terminé
; ====================================================

        org 0x00

Start:
        mov BL, 0x28        ; Pointeur vers la partition en 0x28

PlayLoop:
        mov AL, [BL]        ; Lecture de la note
        cmp AL, 0xFF        ; Fin de la partition ?
        jz SongEnd

        out AL, 0x04        ; Port 0x04 -> Note MIDI
        inc BL

        mov AL, [BL]        ; Lecture de la durée
        out AL, 0x05        ; Port 0x05 -> Durée (x10 ms)
        inc BL

WaitNote:
        in AL, 0x05         ; Attend la fin de la durée matérielle
        cmp AL, 1
        jz WaitNote

        jmp PlayLoop

SongEnd:
        mov AL, 0
        out AL, 0x04        ; Coupe le son
        jmp Start           ; Rejoue la mélodie en boucle

; ====================================================
; Partition : Thème complet de Super Mario Bros
; Format : paires de [Note MIDI, Durée en x10 ms]
; ====================================================
        org 0x28

; --- 1. INTRO ---
        db 76               ; Mi5 (E5)
        db 9
        db 0                ; silence
        db 3
        db 76               ; Mi5 (E5)
        db 9
        db 0                ; silence
        db 11
        db 76               ; Mi5 (E5)
        db 9
        db 0                ; silence
        db 11
        db 72               ; Do5 (C5)
        db 9
        db 0                ; silence
        db 3
        db 76               ; Mi5 (E5)
        db 13
        db 0                ; silence
        db 4
        db 79               ; Sol5 (G5)
        db 16
        db 0                ; silence
        db 14
        db 67               ; Sol4 (G4)
        db 16
        db 0                ; silence
        db 18

; --- 2. THÈME PRINCIPAL (PHRASE A) ---
        db 72               ; Do5 (C5)
        db 13
        db 0                ; silence
        db 8
        db 67               ; Sol4 (G4)
        db 13
        db 0                ; silence
        db 8
        db 64               ; Mi4 (E4)
        db 13
        db 0                ; silence
        db 8
        db 69               ; La4 (A4)
        db 11
        db 0                ; silence
        db 4
        db 71               ; Si4 (B4)
        db 11
        db 0                ; silence
        db 4
        db 70               ; Si bémol 4 (Bb4)
        db 8
        db 69               ; La4 (A4)
        db 13
        db 0                ; silence
        db 5
        db 67               ; Sol4 (G4)
        db 9
        db 76               ; Mi5 (E5)
        db 9
        db 79               ; Sol5 (G5)
        db 9
        db 81               ; La5 (A5)
        db 13
        db 0                ; silence
        db 4
        db 77               ; Fa5 (F5)
        db 8
        db 79               ; Sol5 (G5)
        db 11
        db 0                ; silence
        db 5
        db 76               ; Mi5 (E5)
        db 13
        db 0                ; silence
        db 4
        db 72               ; Do5 (C5)
        db 8
        db 74               ; Ré5 (D5)
        db 8
        db 71               ; Si4 (B4)
        db 15
        db 0                ; silence
        db 22

; --- FIN ---
        db 0xFF             ; Marqueur de fin de partition
