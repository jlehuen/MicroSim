; Test file for DIV instruction

ORG 0

; --- Test 1: DIV AL, number ---
; Expected: AL = 50 / 10 = 5
MOV AL, 50
DIV AL, 10
HLT ; Halt to check registers. AL should be 5.

; --- Test 2: DIV AL, register ---
; Expected: AL = 12 / 4 = 3
MOV AL, 12
MOV BL, 4
DIV AL, BL
HLT ; Halt to check registers. AL should be 3.

; --- Test 3: DIV AL, address ---
; Expected: AL = 16 / value at 'data' (8) = 2
MOV AL, 16
DIV AL, [data]
HLT ; Halt to check registers. AL should be 2.

; --- Test 4: DIV AL, regaddress ---
; Expected: AL = 56 / value at address in BL (data) = 56 / 8 = 7
MOV AL, 56
MOV BL, data  ; BL now holds the address of 'data'
DIV AL, [BL]
HLT ; Halt to check registers. AL should be 7.

; --- Test 5: Division by zero test ---
; Expected: CPU should enter a fault state.
MOV AL, 10
DIV AL, 0
HLT ; This HLT should not be reached.

HLT ; Final halt

data:
DB 8
