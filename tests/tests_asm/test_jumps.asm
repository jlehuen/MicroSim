; Test for jump aliases

START:
    MOV AL, 10
    MOV BL, 10
    CMP AL, BL
    JE EQUAL_LABEL  ; Test JE (should jump)
    JMP NOT_EQUAL

EQUAL_LABEL:
    MOV CL, 0x01    ; Set CL to 1 if JE works
    JMP END_JE

NOT_EQUAL:
    MOV CL, 0x02    ; Set CL to 2 if JE fails

END_JE:
    ; Test JNE
    MOV AL, 10
    MOV BL, 20
    CMP AL, BL
    JNE NOT_EQUAL_2 ; Test JNE (should jump)
    JMP EQUAL_2

NOT_EQUAL_2:
    MOV DL, 0x01    ; Set DL to 1 if JNE works
    JMP END_JNE

EQUAL_2:
    MOV DL, 0x02    ; Set DL to 2 if JNE fails

END_JNE:
    ; Test JO (overflow flag set: 127 + 1 = 128 -> overflow in signed 8-bit)
    MOV AL, 127
    ADD AL, 1
    JO OVERFLOW_OK
    JMP OVERFLOW_FAIL

OVERFLOW_OK:
    MOV CL, 0x10    ; CL = 0x10 if JO works
    JMP TEST_JNO

OVERFLOW_FAIL:
    MOV CL, 0xFF

TEST_JNO:
    ; Test JNO (no overflow: 10 + 10 = 20)
    MOV AL, 10
    ADD AL, 10
    JNO NO_OVERFLOW_OK
    JMP NO_OVERFLOW_FAIL

NO_OVERFLOW_OK:
    MOV DL, 0x20    ; DL = 0x20 if JNO works
    JMP END_ALL

NO_OVERFLOW_FAIL:
    MOV DL, 0xFF

END_ALL:
    HLT
