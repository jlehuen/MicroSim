
; ----- PROGRAMME -----

	mov	BL, 0x50
	call	Label
	mov	[0xC1], 0x44
	hlt

; ----- PROCÉDURE -----

;org 0x50

Label:
	mov	[0xC0], 0x44
	ret