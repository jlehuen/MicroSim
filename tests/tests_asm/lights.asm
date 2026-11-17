;
; Test program for the Traffic Lights component
;

Rep:
	mov	AL, 0x84    ; AL <— 1000 0100 (rouge-vert)
	out	AL, 0x02    ; AL —> port 02
	mov	AL, 0x88    ; AL <- 1000 1000 (rouge-orange)
	out	AL, 0x02    ; AL —> port 02
	mov	AL, 0x90    ; AL <— 1001 0000 (rouge-rouge)
	out	AL, 0x02    ; AL —> port 02

	mov	AL, 0x30    ; AL <— 0011 0000 (vert-rouge)
	out	AL, 0x02    ; AL —> port 02
	mov	AL, 0x50    ; AL <— 0101 0000 (orange-rouge)
	out	AL, 0x02    ; AL —> port 02
	mov	AL, 0x90    ; AL <— 1001 0000 (rouge-rouge)
	out	AL, 0x02    ; AL —> port 02

	jmp	Rep