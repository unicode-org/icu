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
package com.ibm.richtext.styledtext;
import java.text.CharacterIterator;

/** A dynamic character array optimized for sequences of insert
    or delete operations in a local region. */

abstract class MCharBuffer
{
    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    abstract void replace(int start, int limit, MConstText text, int srcStart, int srcLimit);
    abstract void replace(int start, int limit, char[] srcChars, int srcStart, int srcLimit);
    abstract void replace(int start, int limit, String srcString, int srcStart, int srcLimit);
    abstract void replace(int start, int limit, char srcChar);
    abstract CharacterIterator createCharacterIterator(int start, int limit);
    abstract char at(int pos);
    abstract void at(int start, int limit, char[] dst, int dstStart);

    abstract int  length();

    abstract int  capacity();
    abstract void reserveCapacity(int pos, int length);
    abstract void compress();
}
