/*
 *******************************************************************************
 * Copyright (C) 2000-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.dev.tool.ime.indic;

import java.util.Locale;

public class MalayalamInputMethodDescriptor extends IndicIMDescriptor
{
    private static final Locale MALAYALAM = new Locale("ml", "IN");

    public MalayalamInputMethodDescriptor() {
    super(MALAYALAM, "Malayalam");
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
        /* 21 */ '\uFFFF',   // '!'
        /* 22 */ '\u0D20',   // '"'
        /* 23 */ '\uFF00',   // '#'
        /* 24 */ '\uFFFF',   // '$'
        /* 25 */ '\uFFFF',   // '%'
        /* 26 */ '\uFFFF',   // '&'
        /* 27 */ '\u0D1F',   // '''
        /* 28 */ '\u0028',   // '('
        /* 29 */ '\u0029',   // ')'
        /* 2A */ '\uFF01',   // '*'
        /* 2B */ '\u0D0B',   // '+'
        /* 2C */ '\u002C',   // ','
        /* 2D */ '\u002D',   // '-'
        /* 2E */ '\u002E',   // '.'
        /* 2F */ '\u0D2F',   // '/'
        /* 30 */ '\u0D66',   // '0'
        /* 31 */ '\u0D67',   // '1'
        /* 32 */ '\u0D68',   // '2'
        /* 33 */ '\u0D69',   // '3'
        /* 34 */ '\u0D6A',   // '4'
        /* 35 */ '\u0D6B',   // '5'
        /* 36 */ '\u0D6C',   // '6'
        /* 37 */ '\u0D6D',   // '7'
        /* 38 */ '\u0D6E',   // '8'
        /* 39 */ '\u0D6F',   // '9'
        /* 3A */ '\u0D1B',   // ':'
        /* 3B */ '\u0D1A',   // ';'
        /* 3C */ '\u0D37',   // '<'
        /* 3D */ '\u0D43',   // '='
        /* 3E */ '\uFFFF',   // '>'
        /* 3F */ '\uFFFF',   // '?'
        /* 40 */ '\uFFFF',   // '@'
        /* 41 */ '\u0D13',   // 'A'
        /* 42 */ '\u0D34',   // 'B'
        /* 43 */ '\u0D23',   // 'C'
        /* 44 */ '\u0D05',   // 'D'
        /* 45 */ '\u0D06',   // 'E'
        /* 46 */ '\u0D07',   // 'F'
        /* 47 */ '\u0D09',   // 'G'
        /* 48 */ '\u0D2B',   // 'H'
        /* 49 */ '\u0D18',   // 'I'
        /* 4A */ '\u0D31',   // 'J'
        /* 4B */ '\u0D16',   // 'K'
        /* 4C */ '\u0D25',   // 'L'
        /* 4D */ '\u0D36',   // 'M'
        /* 4E */ '\u0D33',   // 'N'
        /* 4F */ '\u0D27',   // 'O'
        /* 50 */ '\u0D1D',   // 'P'
        /* 51 */ '\u0D14',   // 'Q'
        /* 52 */ '\u0D08',   // 'R'
        /* 53 */ '\u0D0F',   // 'S'
        /* 54 */ '\u0D0A',   // 'T'
        /* 55 */ '\u0D19',   // 'U'
        /* 56 */ '\uFFFF',   // 'V'
        /* 57 */ '\u0D10',   // 'W'
        /* 58 */ '\uFFFF',   // 'X'
        /* 59 */ '\u0D2D',   // 'Y'
        /* 5A */ '\u0D0E',   // 'Z'
        /* 5B */ '\u0D21',   // '['
        /* 5C */ '\uFFFF',   // '\'
        /* 5D */ '\uFFFF',   // ']' (nukta - no Unicode code value)
        /* 5E */ '\uFFFF',   // '^'
        /* 5F */ '\u0D03',   // '_'
        /* 60 */ '\u0D4A',   // '`'
        /* 61 */ '\u0D4B',   // 'a'
        /* 62 */ '\u0D35',   // 'b'
        /* 63 */ '\u0D2E',   // 'c'
        /* 64 */ '\u0D4D',   // 'd'
        /* 65 */ '\u0D3E',   // 'e'
        /* 66 */ '\u0D3F',   // 'f'
        /* 67 */ '\u0D41',   // 'g'
        /* 68 */ '\u0D2A',   // 'h'
        /* 69 */ '\u0D17',   // 'i'
        /* 6A */ '\u0D30',   // 'j'
        /* 6B */ '\u0D15',   // 'k'
        /* 6C */ '\u0D24',   // 'l'
        /* 6D */ '\u0D38',   // 'm'
        /* 6E */ '\u0D32',   // 'n'
        /* 6F */ '\u0D26',   // 'o'
        /* 70 */ '\u0D1C',   // 'p'
        /* 71 */ '\u0D4C',   // 'q'
        /* 72 */ '\u0D40',   // 'r'
        /* 73 */ '\u0D47',   // 's'
        /* 74 */ '\u0D42',   // 't'
        /* 75 */ '\u0D39',   // 'u'
        /* 76 */ '\u0D28',   // 'v'
        /* 77 */ '\u0D48',   // 'w'
        /* 78 */ '\u0D02',   // 'x'
        /* 79 */ '\u0D2C',   // 'y'
        /* 7A */ '\u0D46',   // 'z'
        /* 7B */ '\u0D22',   // '{'
        /* 7C */ '\uFFFF',   // '|'
        /* 7D */ '\u0D1E',   // '}'
        /* 7E */ '\u0D12',   // '~'
        /* 7F */ '\u007F'    // ''
        };

        char[] RA_SUB = {'\u0D4D', '\u0D30'};
        char[] CONJ_KA_SSA = {'\u0D15', '\u0D4D', '\u0D37'};

        substitutionTable = new char[][] {
        RA_SUB, CONJ_KA_SSA
        };
    }

        return new IndicInputMethodImpl(keyboardMap, null, null, substitutionTable);
    }
}
