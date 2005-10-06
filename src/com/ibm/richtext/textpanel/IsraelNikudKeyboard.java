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

import java.awt.event.KeyEvent;

/**
 * This class simulates a Nikud keyboard on a US-English
 * keyboard.  It is very much a work in progress.
 */

final class IsraelNikudKeyboard extends KeyRemap {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    public char remap(char c) {

        switch(c) {
            case 't': return '\u05D0'; // alef
            case 'c': return '\u05D1'; // bet
            case 'd': return '\u05D2'; // gimel
            case 's': return '\u05D3'; // dalet
            case 'v': return '\u05D4'; // he
            case 'u': return '\u05D5'; // vav
            case 'z': return '\u05D6'; // zayin
            case 'j': return '\u05D7'; // het
            case 'y': return '\u05D8'; // tet
            case 'h': return '\u05D9'; // yod
            case 'l': return '\u05DA'; // final kaf
            case 'f': return '\u05DB'; // kaf
            case 'k': return '\u05DC'; // lamed
            case 'o': return '\u05DD'; // final mem
            case 'n': return '\u05DE'; // mem
            case 'i': return '\u05DF'; // final nun
            case 'b': return '\u05E0'; // nun
            case 'x': return '\u05E1'; // samech
            case 'g': return '\u05E2'; // ayin
            case ';': return '\u05E3'; // final pe
            case 'p': return '\u05E4'; // pe
            case '.': return '\u05E5'; // final tsadi
            case 'm': return '\u05E6'; // tsadi
            case 'e': return '\u05E7'; // qof
            case 'r': return '\u05E8'; // resh
            case 'a': return '\u05E9'; // shin
            case ',': return '\u05EA'; // tav
            case 'w': return ',';
            case 'q': return '/';
            case '/': return '.';
        }

        return c;
    }

    public char remap(KeyEvent keyEvent) {

        //  Note:  only one ctrl case now (ctrl-/ -> dagesh).
        //  Better implementation will be needed for more cases.

        if (keyEvent.isControlDown()) {
            if (keyEvent.getKeyCode() == KeyEvent.VK_SLASH) {
                return '\u05BC'; // dagesh
            }
        }

        return remap(keyEvent.getKeyChar());
    }
}