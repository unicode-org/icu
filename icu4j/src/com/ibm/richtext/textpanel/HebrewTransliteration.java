/*
 * (C) Copyright IBM Corp. 1998-2004.  All Rights Reserved.
 *
 * The program is provided "as is" without any warranty express or
 * implied, including the warranty of non-infringement and the implied
 * warranties of merchantibility and fitness for a particular purpose.
 * IBM will not be liable for any damages suffered by you as a result
 * of using the Program. In no event will IBM be liable for any
 * special, indirect or consequential damages or lost profits even if
 * IBM has been advised of the possibility of their occurrence. IBM
 * will not be liable for any third party claims against you.
 */
/*
 *
 * (C) Copyright IBM Corp. 1998, All Rights Reserved
 */

package com.ibm.richtext.textpanel;

/**
 * This class implements KeyRemap to produce transliterated Hebrew
 * characters from Latin-1 characters.
 */

final class HebrewTransliteration extends KeyRemap {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    public char remap(char c) {

        switch (c) {
            case 'a': return '\u05D0'; // HEBREW LETTER ALEF
            case 'A': return '\u05E2'; // HEBREW LETTER AYIN
            case 'b': return '\u05D1'; // HEBREW LETTER BET
            case 'B': return '\u05D1'; // HEBREW LETTER BET
            case 'c': return '\u05E6'; // HEBREW LETTER TSADI
            case 'C': return '\u05E5'; // HEBREW LETTER FINAL TSADI
            case 'd': return '\u05D3'; // HEBREW LETTER DALET
            case 'D': return '\u05BC'; // HEBREW POINT DAGESH
            case 'e': return '\u05B5'; // HEBREW POINT TSERE
            case 'E': return '\u05B6'; // HEBREW POINT SEGOL
            case 'f': return '\u05E4'; // HEBREW LETTER PE
            case 'F': return '\u05E4'; // HEBREW LETTER PE
            case 'g': return '\u05D2'; // HEBREW LETTER GIMEL
            case 'G': return '\u05D2'; // HEBREW LETTER GIMEL
            case 'h': return '\u05D4'; // HEBREW LETTER HE
            case 'H': return '\u05D7'; // HEBREW LETTER HET
            case 'i': return '\u05D9'; // HEBREW LETTER YOD
            case 'I': return '\u05B4'; // HEBREW POINT HIRIQ
            case 'j': return '\u05D9'; // HEBREW LETTER YOD
            case 'J': return '\u05C1'; // HEBREW POINT SHIN DOT
            case 'k': return '\u05DB'; // HEBREW LETTER KAF
            case 'K': return '\u05DA'; // HEBREW LETTER FINAL KAF
            case 'l': return '\u05DC'; // HEBREW LETTER LAMED
            case 'L': return '\u05DC'; // HEBREW LETTER LAMED
            case 'm': return '\u05DE'; // HEBREW LETTER MEM
            case 'M': return '\u05DD'; // HEBREW LETTER FINAL MEM
            case 'n': return '\u05E0'; // HEBREW LETTER NUN
            case 'N': return '\u05DF'; // HEBREW LETTER FINAL NUN
            case 'o': return '\u05D5'; // HEBREW LETTER VAV
            case 'O': return '\u05B9'; // HEBREW POINT HOLAM
            case 'p': return '\u05E4'; // HEBREW LETTER PE
            case 'P': return '\u05E3'; // HEBREW LETTER FINAL PE
            case 'q': return '\u05E7'; // HEBREW LETTER QOF
            case 'Q': return '\u05E7'; // HEBREW LETTER QOF
            case 'r': return '\u05E8'; // HEBREW LETTER RESH
            case 'R': return '\u05BF'; // HEBREW POINT RAFE
            case 's': return '\u05E9'; // HEBREW LETTER SHIN
            case 'S': return '\u05E1'; // HEBREW LETTER SAMEKH
            case 't': return '\u05EA'; // HEBREW LETTER TAV
            case 'T': return '\u05D8'; // HEBREW LETTER TET
            case 'u': return '\u05D5'; // HEBREW LETTER VAV
            case 'U': return '\u05BB'; // HEBREW POINT QUBUTS
            case 'v': return '\u05D5'; // HEBREW LETTER VAV
            case 'V': return '\u05B7'; // HEBREW POINT PATAH
            case 'w': return '\u05D5'; // HEBREW LETTER VAV
            case 'W': return '\u05B8'; // HEBREW POINT QAMATS
            case 'x': return '\u05E6'; // HEBREW LETTER TSADI
            case 'X': return '\u05E5'; // HEBREW LETTER FINAL TSADI
            case 'y': return '\u05D9'; // HEBREW LETTER YOD
            case 'Y': return '\u05D9'; // HEBREW LETTER YOD
            case 'z': return '\u05D6'; // HEBREW LETTER ZAYIN
            case 'Z': return '\u05C2'; // HEBREW POINT SIN DOT
        }

        return c;
    }
}
