; This program demonstrates how to print the hexadecimal value of AL.

; --- Data Section ---
    org 0x50
hex_value:
    db 0xB7 ; The value to print in hex

; --- Code Section ---
    org 0x00

main:
    mov AL, [hex_value]
    call print_hex
    hlt

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
    add AL, 'A' - 10
    ret
.is_digit:
    ; It's a digit (0-9)
    add AL, '0'
    ret
