; Fichier de test pour les flags Overflow, Carry, et Sign.

; Test ADD avec dépassement signé (Overflow)
; L'intervalle signé 8-bits est [-128, 127]
; 100 + 100 = 200. Le résultat est hors intervalle, donc Overflow=1

MOV AL, 0x64    ; AL = 100
ADD AL, 0x64    ; AL = 0xC8 (-56), Overflow=1, Carry=0, Sign=1

; Test SUB avec dépassement signé (Overflow)
; L'intervalle signé 8-bits est [-128, 127]
; -100 - 100 = -200. Le résultat est hors intervalle, donc Overflow=1

MOV AL, 0x9C    ; AL = -100
SUB AL, 0x64    ; AL = 0x38 (56), Overflow=1, Carry=0, Sign=0

; Test ADD avec dépassement non signé (Carry)
; L'intervalle non signé 8-bits est [0, 255]
; 255 + 1 = 256. Le résultat est hors intervalle, donc Carry=1

MOV AL, 0xFF    ; AL = 255
ADD AL, 0x01    ; AL = 0x00, Carry=1, Overflow=0, Sign=0

; Test SUB avec dépassement non signé (emprunt/borrow)
; L'intervalle non signé 8-bits est [0, 255]
; 0 - 1 nécessite un emprunt, donc Carry=1

MOV AL, 0x00    ; AL = 0
SUB AL, 0x01    ; AL = 0xFF (-1), Carry=1, Overflow=0, Sign=1

; Test CMP avec emprunt (borrow)
; Compare 10 et 20. En non signé, 10 < 20, donc un emprunt est nécessaire -> Carry=1
; Le résultat interne (10 - 20 = -10) est négatif -> Sign=1

MOV AL, 0x0A    ; AL = 10
CMP AL, 0x14    ; AL ne change pas, Overflow=0, Carry=1, Sign=1
