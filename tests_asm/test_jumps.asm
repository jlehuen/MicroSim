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
    HLT
