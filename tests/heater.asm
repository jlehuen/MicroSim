
Rep:
	in	0x03        ; AL <— port 03
	and	AL, 0x02    ; Masquer les 7 bits de poids fort
	jz	Fire        ; Si le résultat est égal à 0 allumer le bruleur
	mo	AL, 0x00    ; Sinon éteindre le bruleur
	out	0x03        ; AL —> port 03
	jmp	Rep         ; On boucle

;       ====== Allumage du bruleur ======

Fire:
	mov	AL, 0x80    ; AL <— 1000 0000
	out	0x03        ; Allumer le bruleur
	jmp	Rep         ; On boucle
