; Programme de test pour le mode d'adressage [SP + offset]

	mov	AL, 0x11
	mov	BL, 0x22
	mov	CL, 0x33

	push	AL            ; La pile contient [11]
	push	BL            ; La pile contient [22, 11]
	push	CL            ; La pile contient [33, 22, 11]

; A ce stade, SP pointe sur la case mémoire vide sous la pile

; Le sommet de la pile est à SP+1
; [SP+1] contient la valeur 0x33
; [SP+2] contient la valeur 0x22
; [SP+3] contient la valeur 0x11

; On lit les valeurs de la pile dans les registres sans dépiler

	mov	AL, [SP+3]    ; AL devrait maintenant contenir 0x11
	mov	BL, [SP+2]    ; BL devrait maintenant contenir 0x22
	mov	CL, [SP+1]    ; CL devrait maintenant contenir 0x33

; A ce stade, vérifiez dans le simulateur : AL=0x11, BL=0x22, CL=0x33

; On écrit une nouvelle valeur (0xFF) à la place de 0x22 (qui est à SP+2)

	mov	DL, 0xFF
	mov	[SP+2], DL    ; La pile devrait maintenant contenir [33, FF, 11]

; On dépile les valeurs pour confirmer que la modification a bien eu lieu

	pop	AL            ; AL devrait recevoir 0x33 (le sommet de la pile)
	pop	BL            ; BL devrait recevoir 0xFF (la valeur modifiée)
	pop	CL            ; CL devrait recevoir 0x11 (la dernière valeur)

; A ce stade, vérifiez dans le simulateur : AL=0x33, BL=0xFF, CL=0x11

	hlt