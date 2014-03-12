/*
 *******************************************************************************
 * Copyright (C) 2000-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.dev.tool.ime.indic;

import java.util.Locale;

public class BengaliInputMethodDescriptor extends IndicIMDescriptor
{
    private static final Locale BENGALI = new Locale("bn", "IN");

    public BengaliInputMethodDescriptor() {
    super(BENGALI, "Bengali");
    }
    
    private static char[] keyboardMap;
    private static char[][] substitutionTable;
    private static char[] joinWithNukta;
    private static char[] nuktaForm;

    protected IndicInputMethodImpl getImpl()  {
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
        /* 22 */ '\u09A0',   // '"'
        /* 23 */ '\uFF00',   // '#'
        /* 24 */ '\uFF01',   // '$'
        /* 25 */ '\uFF02',   // '%'
        /* 26 */ '\uFF04',   // '&'
        /* 27 */ '\u099F',   // '''
        /* 28 */ '\u0028',   // '('
        /* 29 */ '\u0029',   // ')'
        /* 2A */ '\uFF05',   // '*'
        /* 2B */ '\u098B',   // '+'
        /* 2C */ '\u002C',   // ','
        /* 2D */ '\u002D',   // '-'
        /* 2E */ '\u002E',   // '.'
        /* 2F */ '\u09DF',   // '/'
        /* 30 */ '\u09E6',   // '0'
        /* 31 */ '\u09E7',   // '1'
        /* 32 */ '\u09E8',   // '2'
        /* 33 */ '\u09E9',   // '3'
        /* 34 */ '\u09EA',   // '4'
        /* 35 */ '\u09EB',   // '5'
        /* 36 */ '\u09EC',   // '6'
        /* 37 */ '\u09ED',   // '7'
        /* 38 */ '\u09EE',   // '8'
        /* 39 */ '\u09EF',   // '9'
        /* 3A */ '\u099B',   // ':'
        /* 3B */ '\u099A',   // ';'
        /* 3C */ '\u09B7',   // '<'
        /* 3D */ '\u09C3',   // '='
        /* 3E */ '\u0964',   // '>'  (Devanagari danda)
        /* 3F */ '\u09AF',   // '?'
        /* 40 */ '\uFFFF',   // '@'
        /* 41 */ '\u0993',   // 'A'
        /* 42 */ '\uFFFF',   // 'B'
        /* 43 */ '\u09A3',   // 'C'
        /* 44 */ '\u0985',   // 'D'
        /* 45 */ '\u0986',   // 'E'
        /* 46 */ '\u0987',   // 'F'
        /* 47 */ '\u0989',   // 'G'
        /* 48 */ '\u09AB',   // 'H'
        /* 49 */ '\u0998',   // 'I'
        /* 4A */ '\uFFFF',   // 'J'
        /* 4B */ '\u0996',   // 'K'
        /* 4C */ '\u09A5',   // 'L'
        /* 4D */ '\u09B6',   // 'M'
        /* 4E */ '\uFFFF',   // 'N'
        /* 4F */ '\u09A7',   // 'O'
        /* 50 */ '\u099D',   // 'P'
        /* 51 */ '\u0994',   // 'Q'
        /* 52 */ '\u0988',   // 'R'
        /* 53 */ '\u098F',   // 'S'
        /* 54 */ '\u098A',   // 'T'
        /* 55 */ '\u0999',   // 'U'
        /* 56 */ '\uFFFF',   // 'V'
        /* 57 */ '\u0990',   // 'W'
        /* 58 */ '\u0981',   // 'X'
        /* 59 */ '\u09AD',   // 'Y'
        /* 5A */ '\uFFFF',   // 'Z'
        /* 5B */ '\u09A1',   // '['
        /* 5C */ '\uFFFF',   // '\'
        /* 5D */ '\u09BC',   // ']'
        /* 5E */ '\uFF03',   // '^'
        /* 5F */ '\u0983',   // '_'
        /* 60 */ '\uFFFF',   // '`'
        /* 61 */ '\u09CB',   // 'a'
        /* 62 */ '\u09AC',   // 'b' (this is a BA instead of a VA)
        /* 63 */ '\u09AE',   // 'c'
        /* 64 */ '\u09CD',   // 'd'
        /* 65 */ '\u09BE',   // 'e'
        /* 66 */ '\u09BF',   // 'f'
        /* 67 */ '\u09C1',   // 'g'
        /* 68 */ '\u09AA',   // 'h'
        /* 69 */ '\u0997',   // 'i'
        /* 6A */ '\u09B0',   // 'j'
        /* 6B */ '\u0995',   // 'k'
        /* 6C */ '\u09A4',   // 'l'
        /* 6D */ '\u09B8',   // 'm'
        /* 6E */ '\u09B2',   // 'n'
        /* 6F */ '\u09A6',   // 'o'
        /* 70 */ '\u099C',   // 'p'
        /* 71 */ '\u09CC',   // 'q'
        /* 72 */ '\u09C0',   // 'r'
        /* 73 */ '\u09C7',   // 's'
        /* 74 */ '\u09C2',   // 't'
        /* 75 */ '\u09B9',   // 'u'
        /* 76 */ '\u09A8',   // 'v'
        /* 77 */ '\u09C8',   // 'w'
        /* 78 */ '\u0982',   // 'x'
        /* 79 */ '\u09AC',   // 'y' (this is also a BA...)
        /* 7A */ '\uFFFF',   // 'z'
        /* 7B */ '\u09A2',   // '{'
        /* 7C */ '\uFFFF',   // '|'
        /* 7D */ '\u099E',   // '}'
        /* 7E */ '\uFFFF',   // '~'
        /* 7F */ '\u007F'    // ''
        };

        char[] RA_SUB = {'\u09CD', '\u09B0'};
        char[] RA_SUP = {'\u09B0', '\u09CD'};
        char[] CONJ_JA_NYA = {'\u099C', '\u09CD', '\u099E'};
        char[] CONJ_TA_RA = {'\u09A4', '\u09CD', '\u09B0'};
        char[] CONJ_KA_SSA = {'\u0995', '\u09CD', '\u09B7'};
        char[] CONJ_SHA_RA = {'\u09B6', '\u09CD', '\u09B0'};

        substitutionTable = new char[][] {
        RA_SUB, RA_SUP, CONJ_JA_NYA, CONJ_TA_RA, CONJ_KA_SSA, CONJ_SHA_RA
        };

        // The following characters followed by Nukta should be replaced
        // by the corresponding character as defined in ISCII91
        char LETTER_I              = '\u0987';
        char LETTER_II             = '\u0988';
        char LETTER_VOCALIC_R      = '\u098B';
        char LETTER_DDA            = '\u09A1';
        char LETTER_DDHA           = '\u09A2';
        char VOWEL_SIGN_I          = '\u09BF';
        char VOWEL_SIGN_II         = '\u09C0';
        char VOWEL_SIGN_VOCALIC_R  = '\u09C3';

        // The following characters replace the above characters followed by Nukta. These
        // are defined in one to one correspondence order.
        char LETTER_VOCALIC_L      = '\u098C';
        char LETTER_VOCALIC_LL     = '\u09E1';
        char LETTER_VOCALIC_RR     = '\u09E0';
        char LETTER_DDDHA          = '\u09DC';
        char LETTER_RHA            = '\u09DD';
        char VOWEL_SIGN_VOCALIC_L  = '\u09E2';
        char VOWEL_SIGN_VOCALIC_LL = '\u09E3';
        char VOWEL_SIGN_VOCALIC_RR = '\u09C4';

        joinWithNukta = new char[] {
        LETTER_I,
        LETTER_II,
        LETTER_VOCALIC_R,
        LETTER_DDA,
        LETTER_DDHA,
        VOWEL_SIGN_I,
        VOWEL_SIGN_II,
        VOWEL_SIGN_VOCALIC_R
        };
    
        nuktaForm = new char[] {
        LETTER_VOCALIC_L,
        LETTER_VOCALIC_LL,
        LETTER_VOCALIC_RR,
        LETTER_DDDHA,
        LETTER_RHA,
        VOWEL_SIGN_VOCALIC_L,
        VOWEL_SIGN_VOCALIC_LL,
        VOWEL_SIGN_VOCALIC_RR
        };
    }

    return new IndicInputMethodImpl(keyboardMap, joinWithNukta, nuktaForm, substitutionTable);
    }
}
