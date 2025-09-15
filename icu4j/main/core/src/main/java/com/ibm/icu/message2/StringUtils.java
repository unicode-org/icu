// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.message2;

import com.ibm.icu.text.Normalizer2;

class StringUtils {
    
    // abnf: simple-start-char = %x01-08 ; omit NULL (%x00), HTAB (%x09) and LF (%x0A)
    // abnf:   / %x0B-0C ; omit CR (%x0D)
    // abnf:   / %x0E-1F ; omit SP (%x20)
    // abnf:   / %x21-2D ; omit . (%x2E)
    // abnf:   / %x2F-5B ; omit \ (%x5C)
    // abnf:   / %x5D-7A ; omit { (%x7B)
    // abnf:   / %x7C ; omit } (%x7D)
    // abnf:   / %x7E-2FFF ; omit IDEOGRAPHIC SPACE (%x3000)
    // abnf:   / %x3001-10FFFF
    static boolean isSimpleStartChar(int cp) {
        return (cp >= 0x01 && cp <= 0x08)
                || (cp >= 0x0B && cp <= 0x0C)
                || (cp >= 0x0E && cp <= 0x1F)
                || (cp >= 0x21 && cp <= 0x2D)
                || (cp >= 0x2F && cp <= 0x5B)
                || (cp >= 0x5D && cp <= 0x7A)
                || cp == 0x7C
                || (cp >= 0x7E && cp <= 0x2FFF)
                || (cp >= 0x3001 && cp <= 0x10FFFF);
    }

    
    // abnf: text-char = %x01-5B ; omit NULL (%x00) and \ (%x5C)
    // abnf:         / %x5D-7A ; omit { (%x7B)
    // abnf:         / %x7C ; omit } (%x7D)
    // abnf:         / %x7E-10FFFF
    static boolean isTextChar(int cp) {
        return (cp >= 0x01 && cp <= 0x5B)
            || (cp >= 0x5D && cp <= 0x7A)
            || cp == 0x7C
            || (cp >= 0x7E && cp <= 0x10FFFF);
    }

    // abnf: backslash = %x5C ; U+005C REVERSE SOLIDUS "\"
    static boolean isBackslash(int cp) {
        return cp == '\\';
    }

    /*
     * ; Whitespace
     * abnf: ws = SP / HTAB / CR / LF / %x3000
     */
    static boolean isWhitespace(int cp) {
        return cp == ' ' || cp == '\t' || cp == '\r' || cp == '\n' || cp == 0x3000;
    }

    /*
     * ; Bidirectional marks and isolates
     * ; ALM / LRM / RLM / LRI, RLI, FSI & PDI
     * abnf: bidi = %x061C / %x200E / %x200F / %x2066-2069
     */
    static boolean isBidi(int cp) {
        return cp == 0x061C || cp == 0x200E || cp == 0x200F || (cp >= 0x2066 && cp <= 0x2069);
    }

    /*
     * abnf: name-start = ALPHA
     * abnf:         ; omit Cc: %x0-1F, Whitespace: SPACE, Ascii: «!"#$%&'()*»
     * abnf:         / %x2B ; «+» omit Ascii: «,-./0123456789:;<=>?@» «[\]^»
     * abnf:         / %x5F ; «_» omit Cc: %x7F-9F, Whitespace: %xA0, Ascii: «`» «{|}~»
     * abnf:         / %xA1-61B ; omit BidiControl: %x61C
     * abnf:         / %x61D-167F ; omit Whitespace: %x1680
     * abnf:         / %x1681-1FFF ; omit Whitespace: %x2000-200A
     * abnf:         / %x200B-200D ; omit BidiControl: %x200E-200F
     * abnf:         / %x2010-2027 ; omit Whitespace: %x2028-2029 %x202F, BidiControl: %x202A-202E
     * abnf:         / %x2030-205E ; omit Whitespace: %x205F
     * abnf:         / %x2060-2065 ; omit BidiControl: %x2066-2069
     * abnf:         / %x206A-2FFF ; omit Whitespace: %x3000
     * abnf:         / %x3001-D7FF ; omit Cs: %xD800-DFFF
     * abnf:         / %xE000-FDCF ; omit NChar: %xFDD0-FDEF
     * abnf:         / %xFDF0-FFFD ; omit NChar: %xFFFE-FFFF
     * abnf:         / %x10000-1FFFD ; omit NChar: %x1FFFE-1FFFF
     * abnf:         / %x20000-2FFFD ; omit NChar: %x2FFFE-2FFFF
     * abnf:         / %x30000-3FFFD ; omit NChar: %x3FFFE-3FFFF
     * abnf:         / %x40000-4FFFD ; omit NChar: %x4FFFE-4FFFF
     * abnf:         / %x50000-5FFFD ; omit NChar: %x5FFFE-5FFFF
     * abnf:         / %x60000-6FFFD ; omit NChar: %x6FFFE-6FFFF
     * abnf:         / %x70000-7FFFD ; omit NChar: %x7FFFE-7FFFF
     * abnf:         / %x80000-8FFFD ; omit NChar: %x8FFFE-8FFFF
     * abnf:         / %x90000-9FFFD ; omit NChar: %x9FFFE-9FFFF
     * abnf:         / %xA0000-AFFFD ; omit NChar: %xAFFFE-AFFFF
     * abnf:         / %xB0000-BFFFD ; omit NChar: %xBFFFE-BFFFF
     * abnf:         / %xC0000-CFFFD ; omit NChar: %xCFFFE-CFFFF
     * abnf:         / %xD0000-DFFFD ; omit NChar: %xDFFFE-DFFFF
     * abnf:         / %xE0000-EFFFD ; omit NChar: %xEFFFE-EFFFF
     * abnf:         / %xF0000-FFFFD ; omit NChar: %xFFFFE-FFFFF
     * abnf:         / %x100000-10FFFD ; omit NChar: %x10FFFE-10FFFF
     */
    static boolean isNameStart(int cp) {
        return isAlpha(cp)
                || cp == 0x2B
                || cp == 0x5F
                || (cp >= 0xA1 && cp <= 0x61B)
                || (cp >= 0x61D && cp <= 0x167F)
                || (cp >= 0x1681 && cp <= 0x1FFF)
                || (cp >= 0x200B && cp <= 0x200D)
                || (cp >= 0x2010 && cp <= 0x2027)
                || (cp >= 0x2030 && cp <= 0x205E)
                || (cp >= 0x2060 && cp <= 0x2065)
                || (cp >= 0x206A && cp <= 0x2FFF)
                || (cp >= 0x3001 && cp <= 0xD7FF)
                || (cp >= 0xE000 && cp <= 0xFDCF)
                || (cp >= 0xFDF0 && cp <= 0xFFFD)
                || (cp >= 0x10000 && cp <= 0x1FFFD)
                || (cp >= 0x20000 && cp <= 0x2FFFD)
                || (cp >= 0x30000 && cp <= 0x3FFFD)
                || (cp >= 0x40000 && cp <= 0x4FFFD)
                || (cp >= 0x50000 && cp <= 0x5FFFD)
                || (cp >= 0x60000 && cp <= 0x6FFFD)
                || (cp >= 0x70000 && cp <= 0x7FFFD)
                || (cp >= 0x80000 && cp <= 0x8FFFD)
                || (cp >= 0x90000 && cp <= 0x9FFFD)
                || (cp >= 0xA0000 && cp <= 0xAFFFD)
                || (cp >= 0xB0000 && cp <= 0xBFFFD)
                || (cp >= 0xC0000 && cp <= 0xCFFFD)
                || (cp >= 0xD0000 && cp <= 0xDFFFD)
                || (cp >= 0xE0000 && cp <= 0xEFFFD)
                || (cp >= 0xF0000 && cp <= 0xFFFFD)
                || (cp >= 0x100000 && cp <= 0x10FFFD);
        
    }

    /*
     * abnf: name-char = name-start / DIGIT / "-" / "."
     */
    static boolean isNameChar(int cp) {
        return isNameStart(cp)
                || isDigit(cp)
                || cp == '-'
                || cp == '.';
    }

    // abnf: quoted-char = %x01-5B ; omit NULL (%x00) and \ (%x5C)
    // abnf:         / %x5D-7B ; omit | (%x7C)
    // abnf:         / %x7D-10FFFF
    static boolean isQuotedChar(int cp) {
        return (cp >= 0x01 && cp <= 0x5B)
                || (cp >= 0x5D && cp <= 0x7B)
                || (cp >= 0x7D && cp <= 0x10FFFF);
    }

    // ALPHA is predefined in ABNF as plain ASCII, A-Z and a-z
    // See https://en.wikipedia.org/wiki/Augmented_Backus%E2%80%93Naur_form
    static boolean isAlpha(int cp) {
        return (cp >= 'a' && cp <= 'z') || (cp >= 'A' && cp <= 'Z');
    }

    // DIGIT is predefined in ABNF as plain ASCII, 0-9
    // See https://en.wikipedia.org/wiki/Augmented_Backus%E2%80%93Naur_form
    static boolean isDigit(int cp) {
        return cp >= '0' && cp <= '9';
    }

    // abnf: function = ":" identifier *(s option)
    static boolean isFunctionSigil(int cp) {
        return cp == ':';
    }

    final private static Normalizer2 NFC_NORMALIZER = Normalizer2.getNFCInstance();

    static String toNfc(CharSequence value) {
        return value == null ? null : NFC_NORMALIZER.normalize(value);
    }
}
