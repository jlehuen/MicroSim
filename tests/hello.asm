
; ----- PROGRAMME ------------

	mov	CL, 0xC0
	mov	BL, 0x50
Rep:
	mov	AL, [BL]
	cmp	AL, 0x00
	jz	Fin
	mov	[CL], AL
	inc	CL
	inc	BL
	jmp	Rep
Fin:
	hlt

; ----- DONNEES ------------

	org	0x50
<<<<<<< HEAD
	db	"HELLO WORLD"
=======
	db	"Hello World"
>>>>>>> 760aa85 (First commit)
	db	0x00

