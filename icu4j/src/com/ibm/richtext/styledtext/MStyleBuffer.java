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

/*
    8/1/96
        Style -> ResolvedStyle
    8/7/96 jf
        added countStyles and getStyles protocol
    8/13/96
        ResolvedStyle->Style
    8/22/96 jf
        Removed the setIterator methods.
*/
/*
* MStyleBuffer is the abstract interface for a class which maintains
* style runs in an <tt>MText</tt>.  A "style run" consists of a
* style and the interval on which the style applies.
* <p>
* MStyleBuffer includes methods to call when text is inserted into
* or deleted from the <tt>MText</tt>.  These methods update the
* style runs in accordance with the commonly accepted behavior for
* style runs.
* <p>
* Additionally, MStyleBuffer provides methods for replacing the style runs on a
* text range with another set of style runs.  MStyleBuffer does not do style "combining" (for
* example, adding the bold attribute to text which is italicized);  clients are
* responsible for computing the combined styles, and passing these styles into
* MStyleBuffer.
* <p>
* MStyleBuffer supplies a method for replacing the style runs on a text range with the runs
* represented in an <tt>MStyleRunIterator</tt>.  This is useful for implementing paste
* operations, in which the style runs on a range of text are replaced by style runs
* from an external source.
* <p>
*
* @author John Raley
*
* @see AttributeMap
* @see MText
*/
abstract class MStyleBuffer
{

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
/**
* Respond to an insertion in the text.  The length of the last style run which
* begins before <tt>start</tt> is increased by <tt>limit-start</tt>.
* @param start the offset where the insertion began
* @param limit the offset where the insertion ended
*/
    abstract void insertText(int start, int limit);

/**
* Respond to a deletion in the text.  The last style run before
* <tt>start</tt> is truncated to end at <tt>start</tt>.  The
* style run containing (<tt>start</tt>+<tt>length</tt>) is set to begin
* at (<tt>start</tt>+<tt>length</tt>).  Runs in between are deleted.
* If the deletion occurs entirely within one style run, the length of the style
* run is reduced by <tt>length</tt>.
* @param start the offset where the deletion began
* @param length the offset where the deletion ended
*/
    abstract void deleteText(int start, int limit);

/*
* Replace style runs between offsets <tt>start</tt> and <tt>limit</tt> with styles in
* <tt>iter</tt>.  This method can be used to perform a "paste" operation.
* @param start the offset where the replacement begins
* @param limit the offset where the replacement ends
* @param iter an <tt>MStyleRunIterator</tt> containing style runs which will replace old
* style runs.
*/
    abstract void replace(int start, int limit, MConstText srcText, int srcStart, int srcLimit);

    abstract int styleStart(int pos);
    abstract int styleLimit(int pos);

/**
* Return style at location <tt>pos</tt>.
* @param pos an offset into the text
* @returns the style of the character at <tt>offset</tt>
*/
    abstract AttributeMap styleAt(int pos);

/**
 * Return true if styles were modified.
 */
    abstract boolean modifyStyles(int start,
                                  int limit,
                                  StyleModifier modifier,
                                  int[] damagedRange);

/**
* Minimize the amount of memory used by this object.
*/
    abstract void compress();
}
