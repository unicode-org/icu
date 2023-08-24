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

public class GurmukhiInputMethodDescriptor extends IndicIMDescriptor
{
    private static final Locale GURMUKHI = new Locale("pa", "IN"); // pa = Punjabi

    public GurmukhiInputMethodDescriptor() {
    super(GURMUKHI, "Gurmukhi"); 
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
        /* 21 */ '\u0A0D',   // '!'
        /* 22 */ '\u0A20',   // '"'
        /* 23 */ '\uFF00',   // '#'
        /* 24 */ '\uFFFF',   // '$'
        /* 25 */ '\uFFFF',   // '%'
        /* 26 */ '\uFFFF',   // '&'
        /* 27 */ '\u0A1F',   // '''
        /* 28 */ '\u0028',   // '('
        /* 29 */ '\u0029',   // ')'
        /* 2A */ '\uFFFF',   // '*'
        /* 2B */ '\u0A0B',   // '+'
        /* 2C */ '\u002C',   // ','
        /* 2D */ '\u002D',   // '-'
        /* 2E */ '\u002E',   // '.'
        /* 2F */ '\u0A2F',   // '/'
        /* 30 */ '\u0A66',   // '0'
        /* 31 */ '\u0A67',   // '1'
        /* 32 */ '\u0A68',   // '2'
        /* 33 */ '\u0A69',   // '3'
        /* 34 */ '\u0A6A',   // '4'
        /* 35 */ '\u0A6B',   // '5'
        /* 36 */ '\u0A6C',   // '6'
        /* 37 */ '\u0A6D',   // '7'
        /* 38 */ '\u0A6E',   // '8'
        /* 39 */ '\u0A6F',   // '9'
        /* 3A */ '\u0A1B',   // ':'
        /* 3B */ '\u0A1A',   // ';'
        /* 3C */ '\u0A37',   // '<'
        /* 3D */ '\u0A43',   // '='
        /* 3E */ '\u0964',   // '>'  (Devanagari danda)
        /* 3F */ '\u0A5F',   // '?'
        /* 40 */ '\u0A45',   // '@'
        /* 41 */ '\u0A13',   // 'A'
        /* 42 */ '\u0A34',   // 'B'
        /* 43 */ '\u0A23',   // 'C'
        /* 44 */ '\u0A05',   // 'D'
        /* 45 */ '\u0A06',   // 'E'
        /* 46 */ '\u0A07',   // 'F'
        /* 47 */ '\u0A09',   // 'G'
        /* 48 */ '\u0A2B',   // 'H'
        /* 49 */ '\u0A18',   // 'I'
        /* 4A */ '\u0A31',   // 'J'
        /* 4B */ '\u0A16',   // 'K'
        /* 4C */ '\u0A25',   // 'L'
        /* 4D */ '\u0A36',   // 'M'
        /* 4E */ '\u0A33',   // 'N'
        /* 4F */ '\u0A27',   // 'O'
        /* 50 */ '\u0A1D',   // 'P'
        /* 51 */ '\u0A14',   // 'Q'
        /* 52 */ '\u0A08',   // 'R'
        /* 53 */ '\u0A0F',   // 'S'
        /* 54 */ '\u0A0A',   // 'T'
        /* 55 */ '\u0A19',   // 'U'
        /* 56 */ '\u0A29',   // 'V'
        /* 57 */ '\u0A10',   // 'W'
        /* 58 */ '\u0A01',   // 'X'
        /* 59 */ '\u0A2D',   // 'Y'
        /* 5A */ '\u0A0E',   // 'Z'
        /* 5B */ '\u0A21',   // '['
        /* 5C */ '\u0A49',   // '\'
        /* 5D */ '\u0A3C',   // ']'
        /* 5E */ '\uFFFF',   // '^'
        /* 5F */ '\u0A03',   // '_'
        /* 60 */ '\u0A4A',   // '`'
        /* 61 */ '\u0A4B',   // 'a'
        /* 62 */ '\u0A35',   // 'b'
        /* 63 */ '\u0A2E',   // 'c'
        /* 64 */ '\u0A4D',   // 'd'
        /* 65 */ '\u0A3E',   // 'e'
        /* 66 */ '\u0A3F',   // 'f'
        /* 67 */ '\u0A41',   // 'g'
        /* 68 */ '\u0A2A',   // 'h'
        /* 69 */ '\u0A17',   // 'i'
        /* 6A */ '\u0A30',   // 'j'
        /* 6B */ '\u0A15',   // 'k'
        /* 6C */ '\u0A24',   // 'l'
        /* 6D */ '\u0A38',   // 'm'
        /* 6E */ '\u0A32',   // 'n'
        /* 6F */ '\u0A26',   // 'o'
        /* 70 */ '\u0A1C',   // 'p'
        /* 71 */ '\u0A4C',   // 'q'
        /* 72 */ '\u0A40',   // 'r'
        /* 73 */ '\u0A47',   // 's'
        /* 74 */ '\u0A42',   // 't'
        /* 75 */ '\u0A39',   // 'u'
        /* 76 */ '\u0A28',   // 'v'
        /* 77 */ '\u0A48',   // 'w'
        /* 78 */ '\u0A70',   // 'x' (Gurmukhi TIPPI rather than BINDI)
        /* 79 */ '\u0A2C',   // 'y'
        /* 7A */ '\u0A46',   // 'z'
        /* 7B */ '\u0A22',   // '{'
        /* 7C */ '\u0A11',   // '|'
        /* 7D */ '\u0A1E',   // '}'
        /* 7E */ '\u0A12',   // '~'
        /* 7F */ '\u007F'    // ''
        };

        char[] RA_SUB = {'\u0A4D', '\u0A30'};

        substitutionTable = new char[][] {
        RA_SUB
        };

        // The following characters followed by Nukta should be replaced
        // by the corresponding character as defined in ISCII91
        char LETTER_KHA            = '\u0A16';
        char LETTER_GA             = '\u0A17';
        char LETTER_JA             = '\u0A1C';
        char LETTER_DDA            = '\u0A21';
        char LETTER_PHA            = '\u0A2B';

        // The following characters replace the above characters followed by Nukta. These
        // are defined in one to one correspondence order.
        // NOTE: the inscript keyboard doc. lists a KA + NUKTA and a DDHA + NUKTA
        // neither of which seem to have Unicode code points...
        char LETTER_KHHA           = '\u0A59';
        char LETTER_GHHA           = '\u0A5A';
        char LETTER_ZA             = '\u0A5B';
        char LETTER_RRA            = '\u0A5C';
        char LETTER_FA             = '\u0A5E';

        joinWithNukta = new char[] {
        LETTER_KHA,
        LETTER_GA,
        LETTER_JA,
        LETTER_DDA,
        LETTER_PHA
        };
    
        nuktaForm = new char[] {
        LETTER_KHHA,
        LETTER_GHHA,
        LETTER_ZA,
        LETTER_RRA,
        LETTER_FA
        };
    }

        return new IndicInputMethodImpl(keyboardMap, joinWithNukta, nuktaForm, substitutionTable);
    }
}

