/*
 *******************************************************************************
 * Copyright (C) 2000-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.dev.tool.ime.indic;

import java.util.Locale;

public class TeluguInputMethodDescriptor extends IndicIMDescriptor
{
    private static final Locale TELUGU = new Locale("te", "IN");

    public TeluguInputMethodDescriptor() {
    super(TELUGU, "Telugu");
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
        /* 22 */ '\u0C20',   // '"'
        /* 23 */ '\uFF00',   // '#'
        /* 24 */ '\uFFFF',   // '$'
        /* 25 */ '\uFF01',   // '%'
        /* 26 */ '\uFF03',   // '&'
        /* 27 */ '\u0C1F',   // '''
        /* 28 */ '\u0028',   // '('
        /* 29 */ '\u0029',   // ')'
        /* 2A */ '\uFF04',   // '*'
        /* 2B */ '\u0C0B',   // '+'
        /* 2C */ '\u002C',   // ','
        /* 2D */ '\u002D',   // '-'
        /* 2E */ '\u002E',   // '.'
        /* 2F */ '\u0C2F',   // '/'
        /* 30 */ '\u0C66',   // '0'
        /* 31 */ '\u0C67',   // '1'
        /* 32 */ '\u0C68',   // '2'
        /* 33 */ '\u0C69',   // '3'
        /* 34 */ '\u0C6A',   // '4'
        /* 35 */ '\u0C6B',   // '5'
        /* 36 */ '\u0C6C',   // '6'
        /* 37 */ '\u0C6D',   // '7'
        /* 38 */ '\u0C6E',   // '8'
        /* 39 */ '\u0C6F',   // '9'
        /* 3A */ '\u0C1B',   // ':'
        /* 3B */ '\u0C1A',   // ';'
        /* 3C */ '\u0C37',   // '<'
        /* 3D */ '\u0C43',   // '='
        /* 3E */ '\u0964',   // '>'  (Devanagari danda - not shown on INSCRIPT chart)
        /* 3F */ '\uFFFF',   // '?'
        /* 40 */ '\uFFFF',   // '@'
        /* 41 */ '\u0C13',   // 'A'
        /* 42 */ '\uFFFF',   // 'B'
        /* 43 */ '\u0C23',   // 'C'
        /* 44 */ '\u0C05',   // 'D'
        /* 45 */ '\u0C06',   // 'E'
        /* 46 */ '\u0C07',   // 'F'
        /* 47 */ '\u0C09',   // 'G'
        /* 48 */ '\u0C2B',   // 'H'
        /* 49 */ '\u0C18',   // 'I'
        /* 4A */ '\u0C31',   // 'J'
        /* 4B */ '\u0C16',   // 'K'
        /* 4C */ '\u0C25',   // 'L'
        /* 4D */ '\u0C36',   // 'M'
        /* 4E */ '\u0C33',   // 'N'
        /* 4F */ '\u0C27',   // 'O'
        /* 50 */ '\u0C1D',   // 'P'
        /* 51 */ '\u0C14',   // 'Q'
        /* 52 */ '\u0C08',   // 'R'
        /* 53 */ '\u0C0F',   // 'S'
        /* 54 */ '\u0C0A',   // 'T'
        /* 55 */ '\u0C19',   // 'U'
        /* 56 */ '\uFFFF',   // 'V'
        /* 57 */ '\u0C10',   // 'W'
        /* 58 */ '\u0C01',   // 'X'
        /* 59 */ '\u0C2D',   // 'Y'
        /* 5A */ '\u0C0E',   // 'Z'
        /* 5B */ '\u0C21',   // '['
        /* 5C */ '\uFFFF',   // '\'
        /* 5D */ '\uFFFF',   // ']'
        /* 5E */ '\uFF02',   // '^'
        /* 5F */ '\u0C03',   // '_'
        /* 60 */ '\u0C4A',   // '`'
        /* 61 */ '\u0C4B',   // 'a'
        /* 62 */ '\u0C35',   // 'b'
        /* 63 */ '\u0C2E',   // 'c'
        /* 64 */ '\u0C4D',   // 'd'
        /* 65 */ '\u0C3E',   // 'e'
        /* 66 */ '\u0C3F',   // 'f'
        /* 67 */ '\u0C41',   // 'g'
        /* 68 */ '\u0C2A',   // 'h'
        /* 69 */ '\u0C17',   // 'i'
        /* 6A */ '\u0C30',   // 'j'
        /* 6B */ '\u0C15',   // 'k'
        /* 6C */ '\u0C24',   // 'l'
        /* 6D */ '\u0C38',   // 'm'
        /* 6E */ '\u0C32',   // 'n'
        /* 6F */ '\u0C26',   // 'o'
        /* 70 */ '\u0C1C',   // 'p'
        /* 71 */ '\u0C4C',   // 'q'
        /* 72 */ '\u0C40',   // 'r'
        /* 73 */ '\u0C47',   // 's'
        /* 74 */ '\u0C42',   // 't'
        /* 75 */ '\u0C39',   // 'u'
        /* 76 */ '\u0C28',   // 'v'
        /* 77 */ '\u0C48',   // 'w'
        /* 78 */ '\u0C02',   // 'x'
        /* 79 */ '\u0C2C',   // 'y'
        /* 7A */ '\u0C46',   // 'z'
        /* 7B */ '\u0C22',   // '{'
        /* 7C */ '\uFFFF',   // '|'
        /* 7D */ '\u0C1E',   // '}'
        /* 7E */ '\u0C12',   // '~'
        /* 7F */ '\u007F'    // ''
        };

        char[] RA_SUB = {'\u0C4D', '\u0C30'};
        char[] CONJ_JA_NYA = {'\u0C1C', '\u0C4D', '\u0C1E'};
        char[] CONJ_TA_RA = {'\u0C24', '\u0C4D', '\u0C30'};
        char[] CONJ_KA_SSA = {'\u0C15', '\u0C4D', '\u0C37'};
        char[] CONJ_SHA_RA = {'\u0C36', '\u0C4D', '\u0C30'};

        substitutionTable = new char[][] {
        RA_SUB, CONJ_JA_NYA, CONJ_TA_RA, CONJ_KA_SSA, CONJ_SHA_RA
        };
    }

    return new IndicInputMethodImpl(keyboardMap, null, null, substitutionTable);
    }
}
