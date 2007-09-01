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

import com.ibm.richtext.textlayout.attributes.AttributeMap;

abstract class MParagraphBuffer
{
    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";

/**
* Returns the start of the paragraph containing offset <tt>pos</tt>.
*/
    abstract int paragraphStart(int pos);

/**
* Returns the limit of the paragraph containing offset <tt>pos</tt>.
*/
    abstract int paragraphLimit(int pos);

/**
* Returns the style of the paragraph containing offset <tt>pos</tt>.
*/
    abstract AttributeMap paragraphStyleAt(int offset);

/**
* Process a character insertion at offset <tt>start</tt>.
* If a paragraph break was inserted, propogate paragraph style at
* <tt>start</tt> to new paragraph.
*/
    abstract void insertText(int start, char insertedChar);

/**
* Process character insertion at offset <tt>start</tt>.
* Each new paragraph gets paragraph style at
* <tt>start</tt>.
*/
    abstract void insertText(int start,
                             char[] srcChars,
                             int srcStart,
                             int srcLimit);

/**
* Process deletion by removing paragraph breaks contained in
* deleted range.  Propogate paragraph styles backward, if necessary.
*/
    abstract void deleteText(int start,
                             int limit,
                             int[] damagedRange);

/*
* Replace paragraph breaks/styles between start and limit with paragraph breaks/styles
* from <tt>srcText</tt>.
* @param start an offset into the text
* @param limit the index after the last character to replace
* @param srcText the text from which new paragraphs are taken
* @param srcStart the start of the range in <code>srcText</code> to copy
* @param srcLimit the first index after the range in <code>srcText</code> to copy
*/
    abstract void replace(int start,
                          int limit,
                          MConstText srcText,
                          int srcStart,
                          int srcLimit,
                          int[] damagedRange);

/**
* Set the style of all paragraphs containing offsets in the range [start, limit) to
* <tt>style</tt>.
*/
    abstract boolean modifyParagraphStyles(int start,
                                           int limit,
                                           StyleModifier modifier,
                                           int[] damagedRange);

/**
* Minimize the amount of memory used by this object.
*/
    abstract void compress();
}
