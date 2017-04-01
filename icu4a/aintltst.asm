;; © 2016 and later: Unicode, Inc. and others.
;; License & terms of use: http://www.unicode.org/copyright.html

;; International Components for Unicode for Assembly (ICU4A)

;;  FILE AINTLTST.ASM
;;  International Test Suite for ICU4A

        PROCESSOR      6502     ; of course

        org             $100

        INCLUDE         "ustring.asm"

        ORG             $200

;;; Don't assume any particular i/o for now 
;;; so we will just call once and then break.ADC
AINTLTST    SUBROUTINE
        JSR             u_strlen
        BRK

        ORG             UBUF    ;; load directly into UBUF

        ;; sample data - 26  ($1A) code units not counting trailing null
        HEX             00  a1  00  66  00  65  00  6c  00  69  00  7a  00  20  00  70
        HEX             00  72  00  69  00  6d  00  65  00  72  00  6f  00  20  00  64
        HEX             00  65  00  20  00  61  00  62  00  72  00  69  00  6c  00  21
        HEX             d8  3d  dc  c5
        HEX             00  00  ;; trailing NULL


        