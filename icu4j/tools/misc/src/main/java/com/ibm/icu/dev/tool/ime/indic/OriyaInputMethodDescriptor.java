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

public class OriyaInputMethodDescriptor extends IndicIMDescriptor
{
    private static final Locale ORIYA = new Locale("or", "IN");

    public OriyaInputMethodDescriptor() {
    super(ORIYA, "Oriya");
    }

    private static char[] keyboardMap;
    private static char[][] substitutionTable;
    private static char[] joinWithNukta;
    private static char[] nuktaForm;

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
        /* 22 */ '\u0B20',   // '"'
        /* 23 */ '\uFF00',   // '#'
        /* 24 */ '\uFF01',   // '$'
        /* 25 */ '\uFF02',   // '%'
        /* 26 */ '\uFF04',   // '&'
        /* 27 */ '\u0B1F',   // '''
        /* 28 */ '\u0028',   // '('
        /* 29 */ '\u0029',   // ')'
        /* 2A */ '\uFF05',   // '*'
        /* 2B */ '\u0B0B',   // '+'
        /* 2C */ '\u002C',   // ','
        /* 2D */ '\u002D',   // '-'
        /* 2E */ '\u002E',   // '.'
        /* 2F */ '\u0B5F',   // '/'
        /* 30 */ '\u0B66',   // '0'
        /* 31 */ '\u0B67',   // '1'
        /* 32 */ '\u0B68',   // '2'
        /* 33 */ '\u0B69',   // '3'
        /* 34 */ '\u0B6A',   // '4'
        /* 35 */ '\u0B6B',   // '5'
        /* 36 */ '\u0B6C',   // '6'
        /* 37 */ '\u0B6D',   // '7'
        /* 38 */ '\u0B6E',   // '8'
        /* 39 */ '\u0B6F',   // '9'
        /* 3A */ '\u0B1B',   // ':'
        /* 3B */ '\u0B1A',   // ';'
        /* 3C */ '\u0B37',   // '<'
        /* 3D */ '\u0B43',   // '='
        /* 3E */ '\u0964',   // '>' (Devanagari danda)
        /* 3F */ '\u0B2F',   // '?'
        /* 40 */ '\uFFFF',   // '@'
        /* 41 */ '\u0B13',   // 'A'
        /* 42 */ '\uFFFF',   // 'B'
        /* 43 */ '\u0B23',   // 'C'
        /* 44 */ '\u0B05',   // 'D'
        /* 45 */ '\u0B06',   // 'E'
        /* 46 */ '\u0B07',   // 'F'
        /* 47 */ '\u0B09',   // 'G'
        /* 48 */ '\u0B2B',   // 'H'
        /* 49 */ '\u0B18',   // 'I'
        /* 4A */ '\uFFFF',   // 'J'
        /* 4B */ '\u0B16',   // 'K'
        /* 4C */ '\u0B25',   // 'L'
        /* 4D */ '\u0B36',   // 'M'
        /* 4E */ '\u0B33',   // 'N'
        /* 4F */ '\u0B27',   // 'O'
        /* 50 */ '\u0B1D',   // 'P'
        /* 51 */ '\u0B14',   // 'Q'
        /* 52 */ '\u0B08',   // 'R'
        /* 53 */ '\u0B0F',   // 'S'
        /* 54 */ '\u0B0A',   // 'T'
        /* 55 */ '\u0B19',   // 'U'
        /* 56 */ '\uFFFF',   // 'V'
        /* 57 */ '\u0B10',   // 'W'
        /* 58 */ '\u0B01',   // 'X'
        /* 59 */ '\u0B2D',   // 'Y'
        /* 5A */ '\uFFFF',   // 'Z'
        /* 5B */ '\u0B21',   // '['
        /* 5C */ '\uFFFF',   // '\'
        /* 5D */ '\u0B3C',   // ']'
        /* 5E */ '\uFF03',   // '^'
        /* 5F */ '\u0B03',   // '_'
        /* 60 */ '\uFFFF',   // '`'
        /* 61 */ '\u0B4B',   // 'a'
        /* 62 */ '\u0B2C',   // 'b' (va, but no Unicode code point, used ba instead)
        /* 63 */ '\u0B2E',   // 'c'
        /* 64 */ '\u0B4D',   // 'd'
        /* 65 */ '\u0B3E',   // 'e'
        /* 66 */ '\u0B3F',   // 'f'
        /* 67 */ '\u0B41',   // 'g'
        /* 68 */ '\u0B2A',   // 'h'
        /* 69 */ '\u0B17',   // 'i'
        /* 6A */ '\u0B30',   // 'j'
        /* 6B */ '\u0B15',   // 'k'
        /* 6C */ '\u0B24',   // 'l'
        /* 6D */ '\u0B38',   // 'm'
        /* 6E */ '\u0B32',   // 'n'
        /* 6F */ '\u0B26',   // 'o'
        /* 70 */ '\u0B1C',   // 'p'
        /* 71 */ '\u0B4C',   // 'q'
        /* 72 */ '\u0B40',   // 'r'
        /* 73 */ '\u0B47',   // 's'
        /* 74 */ '\u0B42',   // 't'
        /* 75 */ '\u0B39',   // 'u'
        /* 76 */ '\u0B28',   // 'v'
        /* 77 */ '\u0B48',   // 'w'
        /* 78 */ '\u0B02',   // 'x'
        /* 79 */ '\u0B2C',   // 'y'
        /* 7A */ '\uFFFF',   // 'z'
        /* 7B */ '\u0B22',   // '{'
        /* 7C */ '\uFFFF',   // '|'
        /* 7D */ '\u0B1E',   // '}'
        /* 7E */ '\uFFFF',   // '~'
        /* 7F */ '\u007F'    // ''
        };

        char[] RA_SUB = {'\u0B4D', '\u0B30'};
        char[] RA_SUP = {'\u0B30', '\u0B4D'};
        char[] CONJ_JA_NYA = {'\u0B1C', '\u0B4D', '\u0B1E'};
        char[] CONJ_TA_RA = {'\u0B24', '\u0B4D', '\u0B30'};
        char[] CONJ_KA_SSA = {'\u0B15', '\u0B4D', '\u0B37'};
        char[] CONJ_SHA_RA = {'\u0B36', '\u0B4D', '\u0B30'};

        substitutionTable = new char[][] {
        RA_SUB, RA_SUP, CONJ_JA_NYA, CONJ_TA_RA, CONJ_KA_SSA, CONJ_SHA_RA
        };

        // The following characters followed by Nukta should be replaced
        // by the corresponding character as defined in ISCII91
        char LETTER_I              = '\u0B07';
        char LETTER_II             = '\u0B08';
        char LETTER_VOCALIC_R      = '\u0B0B';
        char LETTER_DDA            = '\u0B21';
        char LETTER_DDHA           = '\u0B22';
        char VOWEL_SIGN_I          = '\u0B3F';
        char VOWEL_SIGN_II         = '\u0B40';

        // The following characters replace the above characters followed by Nukta. These
        // are defined in one to one correspondence order.
        char LETTER_VOCALIC_L      = '\u0B0C';
        char LETTER_VOCALIC_LL     = '\u0B61';
        char LETTER_VOCALIC_RR     = '\u0B60';
        char LETTER_RRA            = '\u0B5C';
        char LETTER_RHA            = '\u0B5D';
        char VOWEL_SIGN_VOCALIC_L  = '\u0B62';
        char VOWEL_SIGN_VOCALIC_LL = '\u0B63';

        joinWithNukta = new char[] {
        LETTER_VOCALIC_R ,
        LETTER_I,
        LETTER_II,
        LETTER_DDA,
        LETTER_DDHA,
        VOWEL_SIGN_I,
        VOWEL_SIGN_II
        };
    
        nuktaForm = new char[] {
        LETTER_VOCALIC_RR,
        LETTER_VOCALIC_L,
        LETTER_VOCALIC_LL,
        LETTER_RRA,
        LETTER_RHA,
        VOWEL_SIGN_VOCALIC_L,
        VOWEL_SIGN_VOCALIC_LL
        };
    }
        
        return new IndicInputMethodImpl(keyboardMap, joinWithNukta, nuktaForm, substitutionTable);
    }
}

