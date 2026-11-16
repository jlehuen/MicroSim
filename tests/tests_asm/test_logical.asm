; Test for logical and shift instructions

; Test AND
MOV AL, 0xCC
AND AL, 0xAA  ; AL should be 0x88

; Test OR
MOV AL, 0xCC
OR AL, 0xAA   ; AL should be 0xEE

; Test XOR
MOV AL, 0xCC
XOR AL, 0xAA  ; AL should be 0x66

; Test NOT
MOV AL, 0xCC
NOT AL        ; AL should be 0x33

; Test SHL
MOV AL, 0xAA
SHL AL, 1     ; AL should be 0x54, Carry should be 1

; Test SHR
MOV AL, 0xAA
SHR AL, 1     ; AL should be 0x55, Carry should be 0

HLT
