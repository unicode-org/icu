/*
 *******************************************************************************
 * Copyright (C) 2000-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.dev.tool.ime.indic;

import java.util.Locale;

public class TamilInputMethodDescriptor extends IndicIMDescriptor
{
    private static final Locale TAMIL = new Locale("ta", "IN");

    public TamilInputMethodDescriptor() {
    super(TAMIL, "Tamil");
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
        /* 22 */ '\uFFFF',   // '"'
        /* 23 */ '\uFFFF',   // '#'
        /* 24 */ '\uFFFF',   // '$'
        /* 25 */ '\uFFFF',   // '%'
        /* 26 */ '\uFF00',   // '&'
        /* 27 */ '\u0B9F',   // '''
        /* 28 */ '\u0028',   // '('
        /* 29 */ '\u0029',   // ')'
        /* 2A */ '\uFF01',   // '*'
        /* 2B */ '\uFFFF',   // '+'
        /* 2C */ '\u002C',   // ','
        /* 2D */ '\u002D',   // '-'
        /* 2E */ '\u002E',   // '.'
        /* 2F */ '\u0BAF',   // '/'
        /* 30 */ '\u0BE6',   // '0'
        /* 31 */ '\u0BE7',   // '1'
        /* 32 */ '\u0BE8',   // '2'
        /* 33 */ '\u0BE9',   // '3'
        /* 34 */ '\u0BEA',   // '4'
        /* 35 */ '\u0BEB',   // '5'
        /* 36 */ '\u0BEC',   // '6'
        /* 37 */ '\u0BED',   // '7'
        /* 38 */ '\u0BEE',   // '8'
        /* 39 */ '\u0BEF',   // '9'
        /* 3A */ '\uFFFF',   // ':'
        /* 3B */ '\u0B9A',   // ';'
        /* 3C */ '\u0BB7',   // '<'
        /* 3D */ '\uFFFF',   // '='
        /* 3E */ '\u0964',   // '>'  (Devanagari danda)
        /* 3F */ '\uFFFF',   // '?'
        /* 40 */ '\uFFFF',   // '@'
        /* 41 */ '\u0B93',   // 'A'
        /* 42 */ '\u0BB4',   // 'B'
        /* 43 */ '\u0BA3',   // 'C'
        /* 44 */ '\u0B85',   // 'D'
        /* 45 */ '\u0B86',   // 'E'
        /* 46 */ '\u0B87',   // 'F'
        /* 47 */ '\u0B89',   // 'G'
        /* 48 */ '\uFFFF',   // 'H'
        /* 49 */ '\uFFFF',   // 'I'
        /* 4A */ '\u0BB1',   // 'J'
        /* 4B */ '\uFFFF',   // 'K'
        /* 4C */ '\uFFFF',   // 'L'
        /* 4D */ '\uFFFF',   // 'M'
        /* 4E */ '\u0BB3',   // 'N'
        /* 4F */ '\uFFFF',   // 'O'
        /* 50 */ '\uFFFF',   // 'P'
        /* 51 */ '\u0B94',   // 'Q'
        /* 52 */ '\u0B88',   // 'R'
        /* 53 */ '\u0B8F',   // 'S'
        /* 54 */ '\u0B8A',   // 'T'
        /* 55 */ '\u0B99',   // 'U'
        /* 56 */ '\u0BA9',   // 'V'
        /* 57 */ '\u0B90',   // 'W'
        /* 58 */ '\uFFFF',   // 'X'
        /* 59 */ '\uFFFF',   // 'Y'
        /* 5A */ '\u0B8E',   // 'Z'
        /* 5B */ '\uFFFF',   // '['
        /* 5C */ '\uFFFF',   // '\'
        /* 5D */ '\uFFFF',   // ']'
        /* 5E */ '\uFFFF',   // '^'
        /* 5F */ '\u0B83',   // '_'
        /* 60 */ '\u0BCA',   // '`'
        /* 61 */ '\u0BCB',   // 'a'
        /* 62 */ '\u0BB5',   // 'b'
        /* 63 */ '\u0BAE',   // 'c'
        /* 64 */ '\u0BCD',   // 'd'
        /* 65 */ '\u0BBE',   // 'e'
        /* 66 */ '\u0BBF',   // 'f'
        /* 67 */ '\u0BC1',   // 'g'
        /* 68 */ '\u0BAA',   // 'h'
        /* 69 */ '\uFFFF',   // 'i'
        /* 6A */ '\u0BB0',   // 'j'
        /* 6B */ '\u0B95',   // 'k'
        /* 6C */ '\u0BA4',   // 'l'
        /* 6D */ '\u0BB8',   // 'm'
        /* 6E */ '\u0BB2',   // 'n'
        /* 6F */ '\uFFFF',   // 'o'
        /* 70 */ '\u0B9C',   // 'p'
        /* 71 */ '\u0BCC',   // 'q'
        /* 72 */ '\u0BC0',   // 'r'
        /* 73 */ '\u0BC7',   // 's'
        /* 74 */ '\u0BC2',   // 't'
        /* 75 */ '\u0BB9',   // 'u'
        /* 76 */ '\u0BA8',   // 'v'
        /* 77 */ '\u0BC8',   // 'w'
        /* 78 */ '\u0B82',   // 'x'
        /* 79 */ '\uFFFF',   // 'y'
        /* 7A */ '\u0BC6',   // 'z'
        /* 7B */ '\uFFFF',   // '{'
        /* 7C */ '\uFFFF',   // '|'
        /* 7D */ '\u0B9E',   // '}'
        /* 7E */ '\u0B92',   // '~'
        /* 7F */ '\u007F'    // ''
        };

        char[] CONJ_KA_SSA = {'\u0B95', '\u0BCD', '\u0BB7'};
        char[] CONJ_SSA_RA = {'\u0BB7', '\u0BCD', '\u0BB0'};

        substitutionTable = new char[][] {
        CONJ_KA_SSA, CONJ_SSA_RA
        };
    }

        return new IndicInputMethodImpl(keyboardMap, null, null, substitutionTable);
    }
}

