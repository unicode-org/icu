/*
 *******************************************************************************
 * Copyright (C) 2000-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.dev.tool.ime.indic;

import java.util.Locale;

public class KannadaInputMethodDescriptor extends IndicIMDescriptor
{
    private static final Locale KANNADA = new Locale("kn", "IN");

    public KannadaInputMethodDescriptor() {
    super(KANNADA, "Kannada");
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
        /* 22 */ '\u0CA0',   // '"'
        /* 23 */ '\uFF00',   // '#'
        /* 24 */ '\uFFFF',   // '$'
        /* 25 */ '\uFF01',   // '%'
        /* 26 */ '\uFF03',   // '&'
        /* 27 */ '\u0C9F',   // '''
        /* 28 */ '\u0028',   // '('
        /* 29 */ '\u0029',   // ')'
        /* 2A */ '\uFF04',   // '*'
        /* 2B */ '\u0C8B',   // '+'
        /* 2C */ '\u002C',   // ','
        /* 2D */ '\u002D',   // '-'
        /* 2E */ '\u002E',   // '.'
        /* 2F */ '\u0CAF',   // '/'
        /* 30 */ '\u0CE6',   // '0'
        /* 31 */ '\u0CE7',   // '1'
        /* 32 */ '\u0CE8',   // '2'
        /* 33 */ '\u0CE9',   // '3'
        /* 34 */ '\u0CEA',   // '4'
        /* 35 */ '\u0CEB',   // '5'
        /* 36 */ '\u0CEC',   // '6'
        /* 37 */ '\u0CED',   // '7'
        /* 38 */ '\u0CEE',   // '8'
        /* 39 */ '\u0CEF',   // '9'
        /* 3A */ '\u0C9B',   // ':'
        /* 3B */ '\u0C9A',   // ';'
        /* 3C */ '\u0CB7',   // '<'
        /* 3D */ '\u0CC3',   // '='
        /* 3E */ '\uFFFF',   // '>'
        /* 3F */ '\uFFFF',   // '?'
        /* 40 */ '\uFFFF',   // '@'
        /* 41 */ '\u0C93',   // 'A'
        /* 42 */ '\uFFFF',   // 'B'
        /* 43 */ '\u0CA3',   // 'C'
        /* 44 */ '\u0C85',   // 'D'
        /* 45 */ '\u0C86',   // 'E'
        /* 46 */ '\u0C87',   // 'F'
        /* 47 */ '\u0C89',   // 'G'
        /* 48 */ '\u0CAB',   // 'H'
        /* 49 */ '\u0C98',   // 'I'
        /* 4A */ '\u0CB1',   // 'J'
        /* 4B */ '\u0C96',   // 'K'
        /* 4C */ '\u0CA5',   // 'L'
        /* 4D */ '\u0CB6',   // 'M'
        /* 4E */ '\u0CB3',   // 'N'
        /* 4F */ '\u0CA7',   // 'O'
        /* 50 */ '\u0C9D',   // 'P'
        /* 51 */ '\u0C94',   // 'Q'
        /* 52 */ '\u0C88',   // 'R'
        /* 53 */ '\u0C8F',   // 'S'
        /* 54 */ '\u0C8A',   // 'T'
        /* 55 */ '\u0C99',   // 'U'
        /* 56 */ '\uFFFF',   // 'V'
        /* 57 */ '\u0C90',   // 'W'
        /* 58 */ '\uFFFF',   // 'X'
        /* 59 */ '\u0CAD',   // 'Y'
        /* 5A */ '\u0C8E',   // 'Z'
        /* 5B */ '\u0CA1',   // '['
        /* 5C */ '\uFFFF',   // '\'
        /* 5D */ '\uFFFF',   // ']'  (danda - not shown on INSCRIPT chart)
        /* 5E */ '\uFF02',   // '^'
        /* 5F */ '\u0C83',   // '_'
        /* 60 */ '\u0CCA',   // '`'
        /* 61 */ '\u0CCB',   // 'a'
        /* 62 */ '\u0CB5',   // 'b'
        /* 63 */ '\u0CAE',   // 'c'
        /* 64 */ '\u0CCD',   // 'd'
        /* 65 */ '\u0CBE',   // 'e'
        /* 66 */ '\u0CBF',   // 'f'
        /* 67 */ '\u0CC1',   // 'g'
        /* 68 */ '\u0CAA',   // 'h'
        /* 69 */ '\u0C97',   // 'i'
        /* 6A */ '\u0CB0',   // 'j'
        /* 6B */ '\u0C95',   // 'k'
        /* 6C */ '\u0CA4',   // 'l'
        /* 6D */ '\u0CB8',   // 'm'
        /* 6E */ '\u0CB2',   // 'n'
        /* 6F */ '\u0CA6',   // 'o'
        /* 70 */ '\u0C9C',   // 'p'
        /* 71 */ '\u0CCC',   // 'q'
        /* 72 */ '\u0CC0',   // 'r'
        /* 73 */ '\u0CC7',   // 's'
        /* 74 */ '\u0CC2',   // 't'
        /* 75 */ '\u0CB9',   // 'u'
        /* 76 */ '\u0CA8',   // 'v'
        /* 77 */ '\u0CC8',   // 'w'
        /* 78 */ '\u0C82',   // 'x'
        /* 79 */ '\u0CAC',   // 'y'
        /* 7A */ '\u0CC6',   // 'z'
        /* 7B */ '\u0CA2',   // '{'
        /* 7C */ '\uFFFF',   // '|'
        /* 7D */ '\u0C9E',   // '}'
        /* 7E */ '\u0C92',   // '~'
        /* 7F */ '\u007F'    // ''
        };

        char[] RA_SUB = {'\u0CCD', '\u0CB0'};
        char[] CONJ_JA_NYA = {'\u0C9C', '\u0CCD', '\u0C9E'};
        char[] CONJ_TA_RA = {'\u0CA4', '\u0CCD', '\u0CB0'};
        char[] CONJ_KA_SSA = {'\u0C95', '\u0CCD', '\u0CB7'};
        char[] CONJ_SHA_RA = {'\u0CB6', '\u0CCD', '\u0CB0'};

        substitutionTable = new char[][] {
        RA_SUB, CONJ_JA_NYA, CONJ_TA_RA, CONJ_KA_SSA, CONJ_SHA_RA
        };
    }

        return new IndicInputMethodImpl(keyboardMap, null, null, substitutionTable);
    }
}

