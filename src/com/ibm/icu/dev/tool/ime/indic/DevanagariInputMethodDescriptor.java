/*
 *******************************************************************************
 * Copyright (C) 2000-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.dev.tool.ime.indic;

import java.util.Locale;

public class DevanagariInputMethodDescriptor extends IndicIMDescriptor
{
    private static final Locale DEVANAGARI = new Locale("hi", "IN");

    public DevanagariInputMethodDescriptor() {
    super(DEVANAGARI, "Devanagari");
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
        /* 21 */ '\u090D',   // '!'
        /* 22 */ '\u0920',   // '"'
        /* 23 */ '\uFF00',   // '#'
        /* 24 */ '\uFF01',   // '$'
        /* 25 */ '\uFF02',   // '%'
        /* 26 */ '\uFF04',   // '&'
        /* 27 */ '\u091F',   // '''
        /* 28 */ '\u0028',   // '('
        /* 29 */ '\u0029',   // ')'
        /* 2A */ '\uFF05',   // '*'
        /* 2B */ '\u090B',   // '+'
        /* 2C */ '\u002C',   // ','
        /* 2D */ '\u002D',   // '-'
        /* 2E */ '\u002E',   // '.'
        /* 2F */ '\u092F',   // '/'
        /* 30 */ '\u0966',   // '0'
        /* 31 */ '\u0967',   // '1'
        /* 32 */ '\u0968',   // '2'
        /* 33 */ '\u0969',   // '3'
        /* 34 */ '\u096A',   // '4'
        /* 35 */ '\u096B',   // '5'
        /* 36 */ '\u096C',   // '6'
        /* 37 */ '\u096D',   // '7'
        /* 38 */ '\u096E',   // '8'
        /* 39 */ '\u096F',   // '9'
        /* 3A */ '\u091B',   // ':'
        /* 3B */ '\u091A',   // ';'
        /* 3C */ '\u0937',   // '<'
        /* 3D */ '\u0943',   // '='
        /* 3E */ '\u0964',   // '>'
        /* 3F */ '\u095F',   // '?'
        /* 40 */ '\u0945',   // '@'
        /* 41 */ '\u0913',   // 'A'
        /* 42 */ '\u0934',   // 'B'
        /* 43 */ '\u0923',   // 'C'
        /* 44 */ '\u0905',   // 'D'
        /* 45 */ '\u0906',   // 'E'
        /* 46 */ '\u0907',   // 'F'
        /* 47 */ '\u0909',   // 'G'
        /* 48 */ '\u092B',   // 'H'
        /* 49 */ '\u0918',   // 'I'
        /* 4A */ '\u0931',   // 'J'
        /* 4B */ '\u0916',   // 'K'
        /* 4C */ '\u0925',   // 'L'
        /* 4D */ '\u0936',   // 'M'
        /* 4E */ '\u0933',   // 'N'
        /* 4F */ '\u0927',   // 'O'
        /* 50 */ '\u091D',   // 'P'
        /* 51 */ '\u0914',   // 'Q'
        /* 52 */ '\u0908',   // 'R'
        /* 53 */ '\u090F',   // 'S'
        /* 54 */ '\u090A',   // 'T'
        /* 55 */ '\u0919',   // 'U'
        /* 56 */ '\u0929',   // 'V'
        /* 57 */ '\u0910',   // 'W'
        /* 58 */ '\u0901',   // 'X'
        /* 59 */ '\u092D',   // 'Y'
        /* 5A */ '\u090E',   // 'Z'
        /* 5B */ '\u0921',   // '['
        /* 5C */ '\u0949',   // '\'
        /* 5D */ '\u093C',   // ']'
        /* 5E */ '\uFF03',   // '^'
        /* 5F */ '\u0903',   // '_'
        /* 60 */ '\u094A',   // '`'
        /* 61 */ '\u094B',   // 'a'
        /* 62 */ '\u0935',   // 'b'
        /* 63 */ '\u092E',   // 'c'
        /* 64 */ '\u094D',   // 'd'
        /* 65 */ '\u093E',   // 'e'
        /* 66 */ '\u093F',   // 'f'
        /* 67 */ '\u0941',   // 'g'
        /* 68 */ '\u092A',   // 'h'
        /* 69 */ '\u0917',   // 'i'
        /* 6A */ '\u0930',   // 'j'
        /* 6B */ '\u0915',   // 'k'
        /* 6C */ '\u0924',   // 'l'
        /* 6D */ '\u0938',   // 'm'
        /* 6E */ '\u0932',   // 'n'
        /* 6F */ '\u0926',   // 'o'
        /* 70 */ '\u091C',   // 'p'
        /* 71 */ '\u094C',   // 'q'
        /* 72 */ '\u0940',   // 'r'
        /* 73 */ '\u0947',   // 's'
        /* 74 */ '\u0942',   // 't'
        /* 75 */ '\u0939',   // 'u'
        /* 76 */ '\u0928',   // 'v'
        /* 77 */ '\u0948',   // 'w'
        /* 78 */ '\u0902',   // 'x'
        /* 79 */ '\u092C',   // 'y'
        /* 7A */ '\u0946',   // 'z'
        /* 7B */ '\u0922',   // '{'
        /* 7C */ '\u0911',   // '|'
        /* 7D */ '\u091E',   // '}'
        /* 7E */ '\u0912',   // '~'
        /* 7F */ '\u007F'    // ''
        };
    
        // the character substitutions for the meta characters.
        char[] RA_SUB = {'\u094D', '\u0930'};
        char[] RA_SUP = {'\u0930', '\u094D'};
        char[] CONJ_JA_NYA = {'\u091C', '\u094D', '\u091E'};
        char[] CONJ_TA_RA = {'\u0924', '\u094D', '\u0930'};
        char[] CONJ_KA_SSA = {'\u0915', '\u094D', '\u0937'};
        char[] CONJ_SHA_RA = {'\u0936', '\u094D', '\u0930'};

        substitutionTable = new char[][] {
        RA_SUB, RA_SUP, CONJ_JA_NYA, CONJ_TA_RA, CONJ_KA_SSA, CONJ_SHA_RA
        };

        // The following characters followed by Nukta should be replaced
        // by the corresponding character as defined in ISCII91
        char SIGN_CANDRABINDU      = '\u0901';
        char LETTER_I              = '\u0907';
        char LETTER_II             = '\u0908';
        char LETTER_VOCALIC_R      = '\u090B';
        char LETTER_KA             = '\u0915';
        char LETTER_KHA            = '\u0916';
        char LETTER_GA             = '\u0917';
        char LETTER_JA             = '\u091C';
        char LETTER_DDA            = '\u0921';
        char LETTER_DDHA           = '\u0922';
        char LETTER_PHA            = '\u092B';
        char VOWEL_SIGN_I          = '\u093F';
        char VOWEL_SIGN_II         = '\u0940';
        char VOWEL_SIGN_VOCALIC_R  = '\u0943';
        char DANDA                 = '\u0964';

        // The following characters replace the above characters followed by Nukta. These
        // are defined in one to one correspondence order.
        char SIGN_OM               = '\u0950';
        char LETTER_VOCALIC_L      = '\u090C';
        char LETTER_VOCALIC_LL     = '\u0961';
        char LETTER_VOCALIC_RR     = '\u0960';
        char LETTER_QA             = '\u0958';
        char LETTER_KHHA           = '\u0959';
        char LETTER_GHHA           = '\u095A';
        char LETTER_ZA             = '\u095B';
        char LETTER_DDDHA          = '\u095C';
        char LETTER_RHA            = '\u095D';
        char LETTER_FA             = '\u095E';
        char VOWEL_SIGN_VOCALIC_L  = '\u0962';
        char VOWEL_SIGN_VOCALIC_LL = '\u0963';
        char VOWEL_SIGN_VOCALIC_RR = '\u0944';
        char SIGN_AVAGRAHA         = '\u093D';

        joinWithNukta = new char[] {
        SIGN_CANDRABINDU,
        LETTER_I,
        LETTER_II,
        LETTER_VOCALIC_R ,
        LETTER_KA,
        LETTER_KHA,
        LETTER_GA,
        LETTER_JA,
        LETTER_DDA,
        LETTER_DDHA,
        LETTER_PHA,
        VOWEL_SIGN_I,
        VOWEL_SIGN_II,
        VOWEL_SIGN_VOCALIC_R,
        DANDA
        };
    
        nuktaForm = new char[] {
        SIGN_OM,
        LETTER_VOCALIC_L,
        LETTER_VOCALIC_LL,
        LETTER_VOCALIC_RR,
        LETTER_QA,
        LETTER_KHHA,
        LETTER_GHHA,
        LETTER_ZA,
        LETTER_DDDHA,
        LETTER_RHA,
        LETTER_FA,
        VOWEL_SIGN_VOCALIC_L,
        VOWEL_SIGN_VOCALIC_LL,
        VOWEL_SIGN_VOCALIC_RR,
        SIGN_AVAGRAHA
        };
        }

        return new IndicInputMethodImpl(keyboardMap, joinWithNukta, nuktaForm, substitutionTable);
    }
}

