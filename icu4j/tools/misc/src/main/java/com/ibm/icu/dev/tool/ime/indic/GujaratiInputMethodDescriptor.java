// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2000-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.dev.tool.ime.indic;

import java.util.Locale;

public class GujaratiInputMethodDescriptor extends IndicIMDescriptor
{
    private static final Locale GUJARATI = new Locale("gu", "IN");

    public GujaratiInputMethodDescriptor() {
    super(GUJARATI, "Gujarati");
    }

    private static char[] keyboardMap;
    private static char[][] substitutionTable;

    protected IndicInputMethodImpl getImpl() {
    if (keyboardMap == null) {
        keyboardMap = new char[] {
        /* 00 */ '\u0000',
        /* 01 */ '\u0001',
        /* 02 */ '\u0002',
        /* 03 */ '\u0003',
        /* 04 */ '\u0004',
        /* 05 */ '\u0005',
        /* 06 */ '\u0006',
        /* 07 */ '\u0007',
        /* 08 */ '\u0008',
        /* 09 */ '\u0009',
        /* 0A */ '\012',
        /* 0B */ '\u000B',
        /* 0C */ '\u000C',
        /* 0D */ '\015',
        /* 0E */ '\u000E',
        /* 0F */ '\u000F',
        /* 10 */ '\u0010',
        /* 11 */ '\u0011',
        /* 12 */ '\u0012',
        /* 13 */ '\u0013',
        /* 14 */ '\u0014',
        /* 15 */ '\u0015',
        /* 16 */ '\u0016',
        /* 17 */ '\u0017',
        /* 18 */ '\u0018',
        /* 19 */ '\u0019',
        /* 1A */ '\u001A',
        /* 1B */ '\u001B',
        /* 1C */ '\u001C',
        /* 1D */ '\u001D',
        /* 1E */ '\u001E',
        /* 1F */ '\u001F',
        /* 20 */ '\u0020',
        /* 21 */ '\u0A8D',   // '!'
        /* 22 */ '\u0AA0',   // '"'
        /* 23 */ '\uFF00',   // '#'
        /* 24 */ '\uFF01',   // '$'
        /* 25 */ '\uFF02',   // '%'
        /* 26 */ '\uFF04',   // '&'
        /* 27 */ '\u0A9F',   // '''
        /* 28 */ '\u0028',   // '('
        /* 29 */ '\u0029',   // ')'
        /* 2A */ '\uFF05',   // '*'
        /* 2B */ '\u0A8B',   // '+'
        /* 2C */ '\u002C',   // ','
        /* 2D */ '\u002D',   // '-'
        /* 2E */ '\u002E',   // '.'
        /* 2F */ '\u0AAF',   // '/'
        /* 30 */ '\u0AE6',   // '0'
        /* 31 */ '\u0AE7',   // '1'
        /* 32 */ '\u0AE8',   // '2'
        /* 33 */ '\u0AE9',   // '3'
        /* 34 */ '\u0AEA',   // '4'
        /* 35 */ '\u0AEB',   // '5'
        /* 36 */ '\u0AEC',   // '6'
        /* 37 */ '\u0AED',   // '7'
        /* 38 */ '\u0AEE',   // '8'
        /* 39 */ '\u0AEF',   // '9'
        /* 3A */ '\u0A9B',   // ':'
        /* 3B */ '\u0A9A',   // ';'
        /* 3C */ '\u0AB7',   // '<'
        /* 3D */ '\u0AC3',   // '='
        /* 3E */ '\u0964',   // '>'  (Devanagari danda)
        /* 3F */ '\uFFFF',   // '?'
        /* 40 */ '\u0AC5',   // '@'
        /* 41 */ '\u0A93',   // 'A'
        /* 42 */ '\uFFFF',   // 'B'
        /* 43 */ '\u0AA3',   // 'C'
        /* 44 */ '\u0A85',   // 'D'
        /* 45 */ '\u0A86',   // 'E'
        /* 46 */ '\u0A87',   // 'F'
        /* 47 */ '\u0A89',   // 'G'
        /* 48 */ '\u0AAB',   // 'H'
        /* 49 */ '\u0A98',   // 'I'
        /* 4A */ '\uFFFF',   // 'J'
        /* 4B */ '\u0A96',   // 'K'
        /* 4C */ '\u0AA5',   // 'L'
        /* 4D */ '\u0AB6',   // 'M'
        /* 4E */ '\u0AB3',   // 'N'
        /* 4F */ '\u0AA7',   // 'O'
        /* 50 */ '\u0A9D',   // 'P'
        /* 51 */ '\u0A94',   // 'Q'
        /* 52 */ '\u0A88',   // 'R'
        /* 53 */ '\u0A8F',   // 'S'
        /* 54 */ '\u0A8A',   // 'T'
        /* 55 */ '\u0A99',   // 'U'
        /* 56 */ '\uFFFF',   // 'V'
        /* 57 */ '\u0A90',   // 'W'
        /* 58 */ '\u0A81',   // 'X'
        /* 59 */ '\u0AAD',   // 'Y'
        /* 5A */ '\uFFFF',   // 'Z'
        /* 5B */ '\u0AA1',   // '['
        /* 5C */ '\u0AC9',   // '\'
        /* 5D */ '\u0ABC',   // ']'
        /* 5E */ '\uFF03',   // '^'
        /* 5F */ '\u0A83',   // '_'
        /* 60 */ '\uFFFF',   // '`'
        /* 61 */ '\u0ACB',   // 'a'
        /* 62 */ '\u0AB5',   // 'b'
        /* 63 */ '\u0AAE',   // 'c'
        /* 64 */ '\u0ACD',   // 'd'
        /* 65 */ '\u0ABE',   // 'e'
        /* 66 */ '\u0ABF',   // 'f'
        /* 67 */ '\u0AC1',   // 'g'
        /* 68 */ '\u0AAA',   // 'h'
        /* 69 */ '\u0A97',   // 'i'
        /* 6A */ '\u0AB0',   // 'j'
        /* 6B */ '\u0A95',   // 'k'
        /* 6C */ '\u0AA4',   // 'l'
        /* 6D */ '\u0AB8',   // 'm'
        /* 6E */ '\u0AB2',   // 'n'
        /* 6F */ '\u0AA6',   // 'o'
        /* 70 */ '\u0A9C',   // 'p'
        /* 71 */ '\u0ACC',   // 'q'
        /* 72 */ '\u0AC0',   // 'r'
        /* 73 */ '\u0AC7',   // 's'
        /* 74 */ '\u0AC2',   // 't'
        /* 75 */ '\u0AB9',   // 'u'
        /* 76 */ '\u0AA8',   // 'v'
        /* 77 */ '\u0AC8',   // 'w'
        /* 78 */ '\u0A82',   // 'x'
        /* 79 */ '\u0AAC',   // 'y'
        /* 7A */ '\uFFFF',   // 'z'
        /* 7B */ '\u0AA2',   // '{'
        /* 7C */ '\u0A91',   // '|'
        /* 7D */ '\u0A9E',   // '}'
        /* 7E */ '\uFFFF',   // '~'
        /* 7F */ '\u007F'    // ''
        };

        char[] RA_SUB = {'\u0ACD', '\u0AB0'};
        char[] RA_SUP = {'\u0AB0', '\u0ACD'};
        char[] CONJ_JA_NYA = {'\u0A9C', '\u0ACD', '\u0A9E'};
        char[] CONJ_TA_RA = {'\u0AA4', '\u0ACD', '\u0AB0'};
        char[] CONJ_KA_SSA = {'\u0A95', '\u0ACD', '\u0AB7'};
        char[] CONJ_SHA_RA = {'\u0AB6', '\u0ACD', '\u0AB0'};

        substitutionTable = new char[][] {
        RA_SUB, RA_SUP, CONJ_JA_NYA, CONJ_TA_RA, CONJ_KA_SSA, CONJ_SHA_RA
        };
    }

        return new IndicInputMethodImpl(keyboardMap, null, null, substitutionTable);
    }
}

