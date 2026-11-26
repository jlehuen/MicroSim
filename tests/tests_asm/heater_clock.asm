; Régulation de la chaudière selon l'horloge Jour/Nuit
; Port 0x04 (Horloge)   : bit 0 = 1 (Jour), 0 (Nuit)
; Port 0x03 (Chaudière) : bit 0 = seuil bas (~15°C), bit 1 = seuil haut (20°C), bit 7 = brûleur

Loop:
	in	AL, 0x04	; Lire l'horloge (bit 0 : 1 = Jour, 0 = Nuit)
	and	AL, 0x01	; Tester le bit 0
	jz	Night		; Si 0 -> mode Nuit (15°C)

; Mode Jour : maintenir à 20°C (bit 1 du port 0x03)
Day:
	in	AL, 0x03	; Lire l'état de la chaudière
	and	AL, 0x02	; Tester le seuil 20°C
	jz	BurnerOn	; Si < 20°C -> allumer le brûleur
	jmp	BurnerOff	; Sinon -> éteindre le brûleur

; Mode Nuit : maintenir à 15°C (bit 0 du port 0x03)
Night:
	in	AL, 0x03	; Lire l'état de la chaudière
	and	AL, 0x01	; Tester le seuil 15°C
	jz	BurnerOn	; Si < 15°C -> allumer le brûleur
	jmp	BurnerOff	; Sinon -> éteindre le brûleur

BurnerOn:
	mov	AL, 0x80	; Bit 7 = 1 (allumer le brûleur)
	out	AL, 0x03
	jmp	Loop

BurnerOff:
	mov	AL, 0x00	; Bit 7 = 0 (éteindre le brûleur)
	out	AL, 0x03
	jmp	Loop
