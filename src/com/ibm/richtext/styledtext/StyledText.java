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

import java.io.Externalizable;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.IOException;
import java.text.CharacterIterator;

/**
 * This class is an implementation of MText, a modifyable, styled text
 * storage model.  Additionally, it supports persistance through the
 * Externalizable interface.
 * @see MText
 */

/*
    10/28/96 {jf} - split the character and paragraph style access and setter function around...
            just to keep things interesting.
    8/7/96 {jf} - moved paragraph break implementation from AbstractText into Style text.
            - added countStyles, getStyles, and ReplaceStyles implementation.

    8/14/96 sfb  eliminated StyleSheetIterator

    8/29/96 {jbr} changed iter-based replace method - doesn't call at() unless it is safe to do so
            Also, added checkStartAndLimit for debugging

    7/31/98 Switched from Style to AttributeMap

*/

public final class StyledText extends MText implements Externalizable
{
    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private static final int CURRENT_VERSION = 1;
    private static final long serialVersionUID = 22356934;

    /* unicode storage */
    private MCharBuffer         fCharBuffer;
    /* character style storage */
    private MStyleBuffer        fStyleBuffer;
    /* paragraph style storage */
    private MParagraphBuffer    fParagraphBuffer;

    private transient int fTimeStamp = 0;
    private transient int[] fDamagedRange = { Integer.MAX_VALUE,
                                              Integer.MIN_VALUE };

    private static class ForceModifier extends StyleModifier {

        private AttributeMap fStyle = AttributeMap.EMPTY_ATTRIBUTE_MAP;

        void setStyle(AttributeMap style) {

            fStyle = style;
        }

        public AttributeMap modifyStyle(AttributeMap style) {

            return fStyle;
        }
    }

    // Keep this around foruse in replaceCharStylesWith.  OK since
    // this class isn't threadsafe anyway.
    private transient ForceModifier forceModifier = null;

    //======================================================
    // CONSTRUCTORS
    //======================================================
    /**
     * Create an empty text object.
     */
    public StyledText()
    {
        this(0);
    }

    /**
     * Create an empty text object ready to hold at least capacity chars.
     * @param capacity the minimum capacity of the internal text buffer
     */
    public StyledText(int capacity)
    {
        fCharBuffer         = capacity>0? new CharBuffer(capacity) : new CharBuffer();
        fStyleBuffer        = new StyleBuffer(this, AttributeMap.EMPTY_ATTRIBUTE_MAP);
        fParagraphBuffer    = new ParagraphBuffer(fCharBuffer);
    }

    /**
     * Create a text object with the characters in the string,
     * in the given style.
     * @param string the initial contents
     * @param initialStyle the style of the initial text
     */
    public StyledText(String string, AttributeMap initialStyle)
    {
        fCharBuffer = new CharBuffer(string.length());
        fCharBuffer.replace(0, 0, string, 0, string.length());

        fStyleBuffer = new StyleBuffer(this, initialStyle);
        fParagraphBuffer = new ParagraphBuffer(fCharBuffer);
    }

    /**
     * Create a text object from the given source.
     * @param source the text to copy
     */
    public StyledText(MConstText source) {
        this();
        append(source);
    }
    
    /**
     * Create a text object from a subrange of the given source.
     * @param source the text to copy from
     * @param srcStart the index of the first character to copy
     * @param srcLimit the index after the last character to copy
     */
    public StyledText(MConstText source, int srcStart, int srcLimit) {
        this();
        replace(0, 0, source, srcStart, srcLimit);
    }

    public void writeExternal(ObjectOutput out) throws IOException {

        out.writeInt(CURRENT_VERSION);
        out.writeObject(fCharBuffer);
        out.writeObject(fStyleBuffer);
        out.writeObject(fParagraphBuffer);
    }

    public void readExternal(ObjectInput in) throws IOException,
                                            ClassNotFoundException {

        int version = in.readInt();
        if (version != CURRENT_VERSION) {
            throw new IOException("Invalid version of StyledText: " + version);
        }
        fCharBuffer = (MCharBuffer) in.readObject();
        fStyleBuffer = (MStyleBuffer) in.readObject();
        fParagraphBuffer = (MParagraphBuffer) in.readObject();

        resetDamagedRange();
    }

    //======================================================
    // MConstText INTERFACES
    //======================================================

    //--------------------------------------------------------
    // character access
    //--------------------------------------------------------
/**
* Return the character at offset <code>pos</code>.
* @param pos a valid offset into the text
* @return the character at offset <code>pos</code>
*/
    public char at(int pos)
    {
        return fCharBuffer.at(pos);
    }

/**
* Copy the characters in the range [<code>start</code>, <code>limit</code>)
* into the array <code>dst</code>, beginning at <code>dstStart</code>.
* @param start offset of first character which will be copied into the array
* @param limit offset immediately after the last character which will be copied into the array
* @param dst array in which to copy characters.  The length of <code>dst</code> must be at least
* (<code>dstStart + limit - start</code>).
*/
    public void extractChars(int start, int limit, char[] dst, int dstStart)
    {
        fCharBuffer.at(start, limit, dst, dstStart);
    }

    //-------------------------------------------------------
    // text model creation
    //-------------------------------------------------------
/**
* Create an MConstText containing the characters and styles in the range
* [<code>start</code>, <code>limit</code>).
* @param start offset of first character in the new text
* @param limit offset immediately after the last character in the new text
* @return an MConstText object containing the characters and styles in the given range
*/
    public MConstText extract(int start, int limit)
    {
        return extractWritable(start, limit);
    }

/**
* Create an MText containing the characters and styles in the range
* [<code>start</code>, <code>limit</code>).
* @param start offset of first character in the new text
* @param limit offset immediately after the last character in the new text
* @return an MConstText object containing the characters and styles in the given range
*/
    public MText extractWritable(int start, int limit)
    {
        MText text = new StyledText();
        text.replace(0, 0, this, start, limit);
        text.resetDamagedRange();
        return text;
    }

    //--------------------------------------------------------
    // size/capacity
    //--------------------------------------------------------
/**
* Return the length of the MConstText object.  The length is the number of characters in the text.
* @return the length of the MConstText object
*/
    public int length()
    {
        return fCharBuffer.length();
    }

/**
* Create a <code>CharacterIterator</code> over the range [<code>start</code>, <code>limit</code>).
* @param start the beginning of the iterator's range
* @param limit the limit of the iterator's range
* @return a valid <code>CharacterIterator</code> over the specified range
* @see java.text.CharacterIterator
*/
    public CharacterIterator createCharacterIterator(int start, int limit)
    {
        return fCharBuffer.createCharacterIterator(start, limit);
    }

    //--------------------------------------------------------
    // character styles
    //--------------------------------------------------------

/**
* Return the index of the first character in the character style run
* containing pos.  All characters in a style run have the same character
* style.
* @return the style at offset <code>pos</code>
*/
    public int characterStyleStart(int pos) {

        checkPos(pos, LESS_THAN_LENGTH);
        return fStyleBuffer.styleStart(pos);
    }

/**
* Return the index after the last character in the character style run
* containing pos.  All characters in a style run have the same character
* style.
* @return the style at offset <code>pos</code>
*/
    public int characterStyleLimit(int pos) {

        checkPos(pos, NOT_GREATER_THAN_LENGTH);
        return fStyleBuffer.styleLimit(pos);
    }

/**
* Return the style applied to the character at offset <code>pos</code>.
* @param pos a valid offset into the text
* @return the style at offset <code>pos</code>
*/
    public AttributeMap characterStyleAt(int pos)
    {
        checkPos(pos, NOT_GREATER_THAN_LENGTH);
        return fStyleBuffer.styleAt(pos);
    }

    //--------------------------------------------------------
    // paragraph boundaries and styles
    //--------------------------------------------------------
/**
* Return the start of the paragraph containing the character at offset <code>pos</code>.
* @param pos a valid offset into the text
* @return the start of the paragraph containing the character at offset <code>pos</code>
*/
    public int paragraphStart(int pos)
    {
        checkPos(pos, NOT_GREATER_THAN_LENGTH);
        return fParagraphBuffer.paragraphStart(pos);
    }

/**
* Return the limit of the paragraph containing the character at offset <code>pos</code>.
* @param pos a valid offset into the text
* @return the limit of the paragraph containing the character at offset <code>pos</code>
*/
    public int paragraphLimit(int pos)
    {
        checkPos(pos, NOT_GREATER_THAN_LENGTH);
        return fParagraphBuffer.paragraphLimit(pos);
    }

/**
* Return the paragraph style applied to the paragraph containing offset <code>pos</code>.
* @param pos a valid offset into the text
* @return the paragraph style in effect at <code>pos</code>
*/
    public AttributeMap paragraphStyleAt(int pos)
    {
        checkPos(pos, NOT_GREATER_THAN_LENGTH);
        return fParagraphBuffer.paragraphStyleAt(pos);
    }

/**
* Return the current time stamp.  The time stamp is
* incremented whenever the contents of the MConstText changes.
* @return the current paragraph style time stamp
*/
    public int getTimeStamp() {

        return fTimeStamp;
    }

    //======================================================
    // MText INTERFACES
    //======================================================
    //--------------------------------------------------------
    // character modfication functions
    //--------------------------------------------------------

    private void updateDamagedRange(int deleteStart,
                                    int deleteLimit,
                                    int insertLength) {

        fDamagedRange[0] = Math.min(fDamagedRange[0], deleteStart);

        if (fDamagedRange[1] >= deleteLimit) {
            int lengthChange = insertLength - (deleteLimit-deleteStart);
            fDamagedRange[1] += lengthChange;
        }
        else {
            fDamagedRange[1] = deleteStart + insertLength;
        }
    }

/**
* Replace the characters and styles in the range [<code>start</code>, <code>limit</code>) with the characters
* and styles in <code>srcText</code> in the range [<code>srcStart</code>, <code>srcLimit</code>).  <code>srcText</code> is not
* modified.
* @param start the offset at which the replace operation begins
* @param limit the offset at which the replace operation ends.  The character and style at
* <code>limit</code> is not modified.
* @param text the source for the new characters and styles
* @param srcStart the offset into <code>srcText</code> where new characters and styles will be obtained
* @param srcLimit the offset into <code>srcText</code> where the new characters and styles end
*/
    public void replace(int start, int limit, MConstText text, int srcStart, int srcLimit)
    {
        if (text == this) {
            text = new StyledText(text);
        }

        if (start == limit && srcStart == srcLimit) {
            return;
        }

        checkStartLimit(start, limit);

        updateDamagedRange(start, limit, srcLimit-srcStart);

        fCharBuffer.replace(start, limit, text, srcStart, srcLimit);
        fStyleBuffer.replace(start, limit, text, srcStart, srcLimit);
        fParagraphBuffer.replace(start, limit, text, srcStart, srcLimit, fDamagedRange);
        fTimeStamp += 1;
    }

/**
* Replace the characters and styles in the range [<code>start</code>, <code>limit</code>) with the characters
* and styles in <code>srcText</code>.  <code>srcText</code> is not
* modified.
* @param start the offset at which the replace operation begins
* @param limit the offset at which the replace operation ends.  The character and style at
* <code>limit</code> is not modified.
* @param text the source for the new characters and styles
*/
    public void replace(int start, int limit, MConstText text) {

        replace(start, limit, text, 0, text.length());
    }

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
    public void replace(int start, int limit, char[] srcChars, int srcStart, int srcLimit, AttributeMap charsStyle)
    {
        checkStartLimit(start, limit);

        if (start == limit && srcStart == srcLimit) {
            return;
        }

        updateDamagedRange(start, limit, srcLimit-srcStart);

        fCharBuffer.replace(start, limit, srcChars, srcStart, srcLimit);

        replaceCharStylesWith(start, limit, start + (srcLimit-srcStart), charsStyle);

        fParagraphBuffer.deleteText(start, limit, fDamagedRange);
        fParagraphBuffer.insertText(start, srcChars, srcStart, srcLimit);

        fTimeStamp += 1;
    }

    private void replaceCharStylesWith(int start, int oldLimit, int newLimit, AttributeMap style) {

        if (start < oldLimit) {
            fStyleBuffer.deleteText(start, oldLimit);
        }
        if (start < newLimit) {
            if (forceModifier == null) {
                forceModifier = new ForceModifier();
            }
            forceModifier.setStyle(style);
            fStyleBuffer.insertText(start, newLimit);
            fStyleBuffer.modifyStyles(start, newLimit, forceModifier, null);
        }
    }

/**
* Replace the characters in the range [<code>start</code>, <code>limit</code>) with the character <code>srcChar</code>.
* The new character takes on the style <code>charStyle</code>
* @param start the offset at which the replace operation begins
* @param limit the offset at which the replace operation ends.  The character at
* <code>limit</code> is not modified.
* @param srcChar the new character
* @param charStyle the style of the new character
*/
    public void replace(int start, int limit, char srcChar, AttributeMap charStyle)
    {
        checkStartLimit(start, limit);

        updateDamagedRange(start, limit, 1);

        fCharBuffer.replace(start, limit, srcChar);

        replaceCharStylesWith(start, limit, start + 1, charStyle);

        if (start < limit) {
            fParagraphBuffer.deleteText(start, limit, fDamagedRange);
        }

        fParagraphBuffer.insertText(start, srcChar);

        fTimeStamp += 1;
    }

/**
* Replace the entire contents of this MText (both characters and styles) with
* the contents of <code>srcText</code>.
* @param srcText the source for the new characters and styles
*/
    public void replaceAll(MConstText srcText)
    {
        replace(0, length(), srcText, 0, srcText.length());
    }

/**
* Insert the contents of <code>srcText</code> (both characters and styles) into this
* MText at the position specified by <code>pos</code>.
* @param pos The character offset where the new text is to be inserted.
* @param srcText The text to insert.
*/
    public void insert(int pos, MConstText srcText)
    {
        replace(pos, pos, srcText, 0, srcText.length());
    }

/**
* Append the contents of <code>srcText</code> (both characters and styles) to the
* end of this MText.
* @param srcText The text to append.
*/
    public void append(MConstText srcText)
    {
        replace(length(), length(), srcText, 0, srcText.length());
    }

/**
* Delete the specified range of characters (and styles).
* @param start Offset of the first character to delete.
* @param limit Offset of the first character after the range to delete.
*/
    public void remove(int start, int limit)
    {
        replace(start, limit, (char[])null, 0, 0, AttributeMap.EMPTY_ATTRIBUTE_MAP);
    }

/**
* Delete all characters and styles.  Always increments time stamp.
*/
    public void remove()
    {
        // rather than going through replace(), just reinitialize the StyledText,
        // letting the old data structures fall on the floor
        fCharBuffer = new CharBuffer();
        fStyleBuffer = new StyleBuffer(this, AttributeMap.EMPTY_ATTRIBUTE_MAP);
        fParagraphBuffer = new ParagraphBuffer(fCharBuffer);
        fTimeStamp += 1;
        fDamagedRange[0] = fDamagedRange[1] = 0;
    }

    //--------------------------------------------------------
    // storage management
    //--------------------------------------------------------

/**
* Minimize the amount of memory used by the MText object.
*/
    public void compress() {

        fCharBuffer.compress();
        fStyleBuffer.compress();
        fParagraphBuffer.compress();
    }

    //--------------------------------------------------------
    // style modification
    //--------------------------------------------------------

/**
* Set the style of all characters in the MText object to
* <code>AttributeMap.EMPTY_ATTRIBUTE_MAP</code>.
*/
    public void removeCharacterStyles() {

        fStyleBuffer = new StyleBuffer(this, AttributeMap.EMPTY_ATTRIBUTE_MAP);
        fTimeStamp += 1;
        fDamagedRange[0] = 0;
        fDamagedRange[1] = length();
    }

/**
* Invoke the given modifier on all character styles from start to limit.
* @param modifier the modifier to apply to the range.
* @param start the start of the range of text to modify.
* @param limit the limit of the range of text to modify.
*/
    public void modifyCharacterStyles(int start, int limit, StyleModifier modifier) {

        checkStartLimit(start, limit);
        boolean modified = fStyleBuffer.modifyStyles(start,
                                                     limit,
                                                     modifier,
                                                     fDamagedRange);
        if (modified) {
            fTimeStamp += 1;
        }
    }

/**
* Invoke the given modifier on all paragraph styles in paragraphs
* containing characters in the range [start, limit).
* @param modifier the modifier to apply to the range.
* @param start the start of the range of text to modify.
* @param limit the limit of the range of text to modify.
*/
    public void modifyParagraphStyles(int start, int limit, StyleModifier modifier) {

        checkStartLimit(start, limit);
        boolean modified = fParagraphBuffer.modifyParagraphStyles(start,
                                                                  limit,
                                                                  modifier,
                                                                  fDamagedRange);
        if (modified) {
            fTimeStamp += 1;
        }
    }

/**
* Reset the damaged range to an empty interval, and begin accumulating the damaged
* range.  The damaged range includes every index where a character, character style,
* or paragraph style has changed.
* @see #damagedRangeStart
* @see #damagedRangeLimit
*/
    public void resetDamagedRange() {

        fDamagedRange[0] = Integer.MAX_VALUE;
        fDamagedRange[1] = Integer.MIN_VALUE;
    }

/**
* Return the start of the damaged range.
* If the start is
* <code>Integer.MAX_VALUE</code> and the limit is
* <code>Integer.MIN_VALUE</code>, then the damaged range
* is empty.
* @return the start of the damaged range
* @see #damagedRangeLimit
* @see #resetDamagedRange
*/
    public int damagedRangeStart() {

        return fDamagedRange[0];
    }

/**
* Return the limit of the damaged range.
* If the start is
* <code>Integer.MAX_VALUE</code> and the limit is
* <code>Integer.MIN_VALUE</code>, then the damaged range
* is empty.
* @return the limit of the damaged range
* @see #damagedRangeStart
* @see #resetDamagedRange
*/
    public int damagedRangeLimit() {

        return fDamagedRange[1];
    }

    public String toString()
    {
        String result ="";
        for (int i = 0; i < length(); i++) {
            result += at(i);
        }
        return result;
    }

    //======================================================
    // IMPLEMENTATION
    //======================================================

    /* check a range to see if it is well formed and within the bounds of the text */
    private void checkStartLimit(int start, int limit)
    {
        if (start > limit) {
            //System.out.println("Start is less than limit. start:"+start+"; limit:"+limit);
            throw new IllegalArgumentException("Start is greater than limit. start:"+start+"; limit:"+limit);
        }

        if (start < 0) {
            //System.out.println("Start is negative. start:"+start);
            throw new IllegalArgumentException("Start is negative. start:"+start);
        }

        if (limit > length()) {
            //System.out.println("Limit is greater than length.  limit:"+limit);
            throw new IllegalArgumentException("Limit is greater than length.  limit:"+limit);
        }
    }

    private static final boolean LESS_THAN_LENGTH = false;
    private static final boolean NOT_GREATER_THAN_LENGTH = true;

    private void checkPos(int pos, boolean endAllowed) {

        int lastValidPos = length();
        if (endAllowed == LESS_THAN_LENGTH) {
            --lastValidPos;
        }

        if (pos < 0 || pos > lastValidPos) {
            throw new IllegalArgumentException("Position is out of range.");
        }
    }
}
