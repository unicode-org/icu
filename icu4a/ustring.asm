;; © 2016 and later: Unicode, Inc. and others.
;; License & terms of use: http://www.unicode.org/copyright.html

;; International Components for Unicode for Assembly (ICU4A)

;;  FILE USTRING.ASM
;;  String Manipulation routines for ICU4A
;;  This file is meant to be included

        PROCESSOR      6502     ; of course

UBUF    EQU             $300         ; ICU work area

;; Determine the length of an array of UChar.
;; @param UBUF The array of UChars, NULL (U+0000) terminated.
;; @return A The number of UChars in UBUF, minus the terminator.
;; @modifies A, X, and status register
;; @draft ICU 59
u_strlen SUBROUTINE

        LDX #0
.loop   LDA UBUF,X      ; load lo byte
        BEQ .hi0
        INX             ; ignore hi byte if lo is nonzero
        JMP .next
.hi0    INX
        LDA UBUF,X      ; load hi byte
        BEQ .done
.next   INX
        JMP .loop
.done   DEX             ; make it even
        TXA
        ROR             ; 
        AND #$7F        ; clear high bit
        RTS             ; and return it.
        
