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
    Change history:

    10/29/96 jef    split the character and paragraph style access functions
    8/14/96 sfb     eliminated StyleSheetIterator
    8/21/96 jef        completed abstract interface (changed iterator classes etc.)
    1/30/97 rtg     cleaned up interface, brought in functions from SimpleTextView
    7/31/98 jbr switched from Style to AttributeMap

*/

/**
 * This class is a mutable extension of MConstText.  It has methods for
 * inserting, appending, replacing, and removing styled text.  Additionally,
 * it has methods for modifying paragraph and character styles.
 * <p>
 * Styled characters (from another <code>MConstText</code> instance) added
 * to the text retain their original character styles.  The style of plain characters
 * (specified as a <code>char</code> or <code>char[]</code>) is always
 * specified explicitly when they are added to the text.  MText does not do
 * character style "propagation", where unstyled characters take on the
 * style of previous characters.  Clients can implement this behavior by
 * specifying the styles to propagate.
 * <p>
 * When unstyled characters are added to the text, their paragraph style
 * is the paragraph style in effect immediately after the last new character.
 * If the characters contain paragraph separators, then every new paragraph
 * will have the same paragraph style.  When styled characters are added
 * to the text, their resulting paragraph style is determined by the
 * following rule:
 * <blockquote>
 * The paragraph styles in the new text
 * become the paragraph styles in the target text, with the exception of the
 * last paragraph in the new text, which takes on the paragraph style in
 * effect immediately after the inserted text.
 * If the new text is added at the end of the target text, the new text's
 * paragraph styles take effect in any paragraph affected by the addition.
 * </blockquote>
 * For example, suppose there is a single paragraph of text with style 'A',
 * delimited with a paragraph separator 'P':
 * <blockquote>
 * AAAAAAP
 * </blockquote>
 * Suppose the following styled paragraphs are inserted into the above text
 * after the fourth character:
 * <blockquote>
 * BBBBPCCCPDDD
 * </blockquote>
 * Then the original paragraph style of each character is:
 * <blockquote>
 * AAAABBBBPCCCPDDDAAP
 * </blockquote>
 * The resulting paragraph styles are:
 * <blockquote>
 * BBBBBBBBPCCCPAAAAAP
 * </blockquote>
 * Similarly, if characters are deleted, the paragraph style immediately
 * after the deletion takes effect on the paragraph containing the deletion.
 * So, if characters 4-16 were deleted in the example above, the paragraph
 * styles would be:
 * <blockquote>
 * AAAAAAP
 * </blockquote>
 * This paragraph-style propagation policy is sometimes referred to as <strong>
 * following styles win</strong>, since styles at the end of the paragraph
 * become the style for the entire paragraph.
 * <p>
 * This class can accumulate a <strong>damaged range</strong> - an interval in
 * which characters, character styles, or paragraph styles have changed.  This is
 * useful for clients such as text editors which reformat and draw text after
 * changes.  Usually the damaged range is exactly the range of characters
 * operated upon;  however, larger ranges may be damaged if paragraph styles
 * change.
 * @see StyleModifier
 */

public abstract class MText extends MConstText
{
    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    protected MText() {
    }

//==================================================
// MAIN CHARACTER MODIFICATION FUNCTIONS
//==================================================
/**
* Replace the characters and styles in the range [<code>start</code>, <code>limit</code>) with the characters
* and styles in <code>srcText</code> in the range [<code>srcStart</code>, <code>srcLimit</code>).  <code>srcText</code> is not
* modified.
* @param start the offset at which the replace operation begins
* @param limit the offset at which the replace operation ends.  The character and style at
* <code>limit</code> is not modified.
* @param srcText the source for the new characters and styles
* @param srcStart the offset into <code>srcText</code> where new characters and styles will be obtained
* @param srcLimit the offset into <code>srcText</code> where the new characters and styles end
*/
    public abstract void replace(int start, int limit, MConstText srcText, int srcStart, int srcLimit);

/**
* Replace the characters and styles in the range [<code>start</code>, <code>limit</code>) with the characters
* and styles in <code>srcText</code>.  <code>srcText</code> is not
* modified.
* @param start the offset at which the replace operation begins
* @param limit the offset at which the replace operation ends.  The character and style at
* <code>limit</code> is not modified.
* @param text the source for the new characters and styles
*/
    public abstract void replace(int start, int limit, MConstText text);

/**
* Replace the characters in the range [<code>start</code>, <code>limit</code>) with the characters
* in <code>srcChars</code> in the range [<code>srcStart</code>, <code>srcLimit</code>).  New characters take on the style
* <code>charsStyle</code>.
* <code>srcChars</code> is not modified.
* @param start the offset at which the replace operation begins
* @param limit the offset at which the replace operation ends.  The character at
* <code>limit</code> is not modified.
* @param srcChars the source for the new characters
* @param srcStart the offset into <code>srcChars</code> where new characters will be obtained
* @param srcLimit the offset into <code>srcChars</code> where the new characters end
* @param charsStyle the style of the new characters
*/
    public abstract void replace(int start, int limit, char[] srcChars, int srcStart, int srcLimit, AttributeMap charsStyle);

/**
* Replace the characters in the range [<code>start</code>, <code>limit</code>) with the character <code>srcChar</code>.
* The new character takes on the style <code>charStyle</code>
* @param start the offset at which the replace operation begins
* @param limit the offset at which the replace operation ends.  The character at
* <code>limit</code> is not modified.
* @param srcChar the new character
* @param charStyle the style of the new character
*/
    public abstract void replace(int start, int limit, char srcChar, AttributeMap charStyle);

/**
* Replace the entire contents of this MText (both characters and styles) with
* the contents of <code>srcText</code>.
* @param srcText the source for the new characters and styles
*/
    public abstract void replaceAll(MConstText srcText);

/**
* Insert the contents of <code>srcText</code> (both characters and styles) into this
* MText at the position specified by <code>pos</code>.
* @param pos The character offset where the new text is to be inserted.
* @param srcText The text to insert. */
    public abstract void insert(int pos, MConstText srcText);

/**
* Append the contents of <code>srcText</code> (both characters and styles) to the
* end of this MText.
* @param srcText The text to append. */
    public abstract void append(MConstText srcText);

/**
* Delete the specified range of characters (and styles).
* @param start Offset of the first character to delete.
* @param limit Offset of the first character after the range to delete. */
    public abstract void remove(int start, int limit);

/**
* Delete all characters and styles.
*/
    public abstract void remove();

/**
* Create an MText containing the characters and styles in the range
* [<code>start</code>, <code>limit</code>).
* @param start offset of first character in the new text
* @param limit offset immediately after the last character in the new text
* @return an MConstText object containing the characters and styles in the given range
*/
    public abstract MText extractWritable(int start, int limit);


//==================================================
// STORAGE MANAGEMENT
//==================================================

/**
* Minimize the amount of memory used by the MText object.
*/
    public abstract void compress();

//==================================================
// STYLE MODIFICATION
//==================================================

/**
* Set the character style of all characters in the MText object to
* <code>AttributeMap.EMPTY_ATTRIBUTE_MAP</code>.
*/
    public abstract void removeCharacterStyles();

/**
* Invoke the given modifier on all character styles from start to limit.
* @param modifier the modifier to apply to the range.
* @param start the start of the range of text to modify.
* @param limit the limit of the range of text to modify.
*/
    public abstract void modifyCharacterStyles(int start, int limit, StyleModifier modifier);

/**
* Invoke the given modifier on all paragraph styles in paragraphs
* containing characters in the range [start, limit).
* @param modifier the modifier to apply to the range.
* @param start the start of the range of text to modify.
* @param limit the limit of the range of text to modify.
*/
    public abstract void modifyParagraphStyles(int start, int limit, StyleModifier modifier);

//==================================================
// DAMAGED RANGE
//==================================================
/**
* Reset the damaged range to an empty interval, and begin accumulating the damaged
* range.  The damaged range includes every index where a character, character style,
* or paragraph style has changed.
* @see #damagedRangeStart
* @see #damagedRangeLimit
*/
    public abstract void resetDamagedRange();
}
