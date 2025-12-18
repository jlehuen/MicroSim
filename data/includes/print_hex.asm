; --- print_hex Procedure ---
; Prints the hexadecimal value of AL to the ASCII terminal.

print_hex:
    push AL         ; Save original value
    
    ; Process high nibble
    shr AL, 4       ; Move high nibble to low nibble position
    call to_hex_char
    mov [0xC0], AL  ; Print first hex char
    pop AL          ; Restore original value
    
    ; Process low nibble
    and AL, 0x0F    ; Isolate low nibble
    call to_hex_char
    mov [0xC1], AL  ; Print second hex char
    
    ret

; --- to_hex_char Sub-procedure ---
; Converts the low nibble of AL to a hex ASCII character.

to_hex_char:
    cmp AL, 9
    jbe .is_digit
    ; It's a letter (A-F)
    add AL, 0x37
    ret
.is_digit:
    ; It's a digit (0-9)
    add AL, 0x30
    ret
