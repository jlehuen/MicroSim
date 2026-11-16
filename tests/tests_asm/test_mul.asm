; Test file for MUL instruction

ORG 0

; --- Test 1: MUL AL, number ---
; Expected: AL = 5 * 10 = 50 (0x32)
MOV AL, 5
MUL AL, 10
HLT ; Halt to check registers. AL should be 50.

; --- Test 2: MUL AL, register ---
; Expected: AL = 3 * 4 = 12 (0x0C)
MOV AL, 3
MOV BL, 4
MUL AL, BL
HLT ; Halt to check registers. AL should be 12.

; --- Test 3: MUL AL, address ---
; Expected: AL = 2 * value at 'data' (8) = 16 (0x10)
MOV AL, 2
MUL AL, [data]
HLT ; Halt to check registers. AL should be 16.

; --- Test 4: MUL AL, regaddress ---
; Expected: AL = 7 * value at address stored in BL (data address) = 7 * 8 = 56 (0x38)
MOV AL, 7
MOV BL, data  ; BL now holds the address of 'data'
MUL AL, [BL]
HLT ; Halt to check registers. AL should be 56.

; --- Test 5: Overflow test ---
; Expected: AL = 20 * 20 = 400. This overflows 8 bits (400 > 255).
; Result in AL will be 400 & 0xFF = 144 (0x90).
; Carry and Overflow flags should be set.
MOV AL, 20
MUL AL, 20
HLT ; Halt to check registers. AL=144, CF=1, OF=1

HLT ; Final halt

data:
DB 8
