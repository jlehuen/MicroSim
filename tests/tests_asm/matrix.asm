;
; Programme de test pour la matrice de LED 8x8
; Contrôle :
;   - Port 0x07 : index de ligne (0 à 7)
;   - Port 0x08 : données de la ligne (8 LED)
; Démonstration : Animation cœur battant
;

        org 0x00

Start:
        ; Affichage du grand cœur
        mov CL, 0x40
        call Draw

        ; Affichage du petit cœur
        mov CL, 0x48
        call Draw

        jmp Start

; ----------------------------------------------------
; Routine Draw : envoie les 8 octets pointés par CL
; ----------------------------------------------------
Draw:
        mov BL, 0           ; Ligne 0
DrawLoop:
        mov AL, BL
        out AL, 0x07        ; Port 0x07 -> sélection de la ligne BL

        mov AL, [CL]        ; Lecture du motif
        out AL, 0x08        ; Port 0x08 -> écriture des 8 LED de la ligne

        inc CL              ; Pointeur de motif suivant
        inc BL              ; Ligne suivante
        cmp BL, 8
        jnz DrawLoop
        ret

; ====================================================
; Données : Grand cœur (8 octets en 0x40)
; ====================================================
        org 0x40
        db 0x00             ; ........
        db 0x66             ; .XX..XX.
        db 0xFF             ; XXXXXXXX
        db 0xFF             ; XXXXXXXX
        db 0x7E             ; .XXXXXX.
        db 0x3C             ; ..XXXX..
        db 0x18             ; ...XX...
        db 0x00             ; ........

; ====================================================
; Données : Petit cœur (8 octets en 0x48)
; ====================================================
        org 0x48
        db 0x00             ; ........
        db 0x00             ; ........
        db 0x24             ; ..X..X..
        db 0x7E             ; .XXXXXX.
        db 0x3C             ; ..XXXX..
        db 0x18             ; ...XX...
        db 0x00             ; ........
        db 0x00             ; ........
