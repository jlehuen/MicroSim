; Test PUSHF and POPF instructions

MOV AL, 0x0F
MOV BL, 0x0F
ADD AL, BL ; AL = 0x1E, Carry=0, Zero=0, Sign=0, Overflow=0

PUSHF ; Push flags (0)

MOV AL, 0xFF
INC AL ; AL = 0, Carry=1, Zero=1, Sign=0, Overflow=0

POPF ; Pop flags (0) - should restore flags to Carry=0, Zero=0

; Now check the flags.
; If Zero is 0, JNZ will jump to SUCCESS
JNZ SUCCESS

; If we are here, the test failed
HLT

SUCCESS:
; Infinite loop to indicate success
JMP SUCCESS
