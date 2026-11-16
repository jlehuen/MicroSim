

; Wait for a key press from the physical keyboard.
; The Keyboard window will appear.
; The ASCII code of the pressed key will be stored in AL.

	mov	BL, 0xC0

Start:
	in	0x01

	cmp	AL, 0x0D
	jz	Stop
	mov	[BL], AL
	inc	BL
	jmp	Start

Stop:
	hlt