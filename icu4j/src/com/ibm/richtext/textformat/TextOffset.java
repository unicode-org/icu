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
// Revision: 03 1.16 richtext/TextOffset.java, richtext, richtext

/*
    9/5/96 {jbr} added set and equals methods
*/

package com.ibm.richtext.textformat;

/**
 * A TextOffset indicates both an integer offset into text and a placement
 * on one of the characters adjacent to the offset.  An offset is a
 * position between two characters;  offset n
 * is between character n-1 and character n.  The placement specifies whether
 * it is associated with the character
 * after the offset
 * (character n) or the character before the offset (character n-1).
 * <p>
 * Knowing which character the TextOffset is associated with is necessary
 * when displaying carets.  In bidirectional text, a single offset may
 * have two distinct carets.  Also, in multiline text, an offset at a line
 * break has a possible caret on each line.
 * <p>
 * Most clients will not be interested in the placement, and will just use
 * the offset.
 */
public final class TextOffset
{
    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    /**
     * Indicates that the TextOffset is associated with character
     * <code>fOffset - 1</code> - ie the character before its offset.
     */
    public final static boolean BEFORE_OFFSET = true;

    /**
     * Indicates that the TextOffset is associated with character
     * <code>fOffset</code> - ie the character after its offset.
     */
    public final static boolean AFTER_OFFSET = false;

    /**
     * The offset into the text.
     */
    public int fOffset = 0;

    /**
     * The placement - before or after.
     */
    public boolean fPlacement = AFTER_OFFSET;

    /**
    * Constructs a new TextOffset
    * @param offset the offset into the text to represent.  Placement is implicitly AFTER_OFFSET.
    */
    public TextOffset(int offset)
    {
        if (offset < 0)
            throw new IllegalArgumentException("Offset is negative in TextOffset constructor.");

        fOffset = offset;

        fPlacement = AFTER_OFFSET;
    }

    /**
    * Constructs a new TextOffset at 0, with placement AFTER_OFFSET.
    */
    public TextOffset() {
        this(0);
    }

    /**
    * Constructs a new TextOffset with the given offset and placement.
    * @param offset the offset into the text
    * @param placement indicates the position of the caret; one of BEFORE_OFFSET or AFTER_OFFSET
    */
    public TextOffset(int offset, boolean placement)
    {
        if (offset < 0)
            throw new IllegalArgumentException("TextOffset constructor offset < 0: " + offset);

        fOffset = offset;
        fPlacement = placement;
    }

    /**
    * Constructs a new TextOffset from an existing one.
    * @param rhs the TextOffset to copy
    */
    public TextOffset(TextOffset rhs) {

        this(rhs.fOffset, rhs.fPlacement);
    }

    /**
    * Set the value of the TextOffset
    * @param offset the offset into the text
    * @param placement indicates the position of the caret; one of BEFORE_OFFSET or AFTER_OFFSET
    */
    public void setOffset(int offset, boolean placement)
    {
        if (offset < 0)
            throw new IllegalArgumentException("TextOffset setOffset offset < 0: " + offset);

        fOffset = offset;
        fPlacement = placement;
    }

    /**
     * Compare this to another Object.
     */
    public boolean equals(Object other) {

        try {
            return equals((TextOffset)other);
        }
        catch(ClassCastException e) {
            return false;
        }
    }

    /**
    * Return true if offset and placement are the same.
    *
    * @param other offset to compare against
    * @return true if both offsets are equal
    */
    public boolean equals(TextOffset other) {

        return fOffset == other.fOffset && fPlacement == other.fPlacement;
    }

    /**
     * Return the hashCode for this object.
     */
    public int hashCode() {

        return fPlacement==AFTER_OFFSET? fOffset : -fOffset;
    }

    /**
    * Return true if this offset is 'greaterThan' other.  If the fOffset fields are equal, the
    * placement field is considered, and AFTER_OFFSET is considered 'greaterThan' BEFORE_OFFSET.
    *
    * @param other the other offset
    * @return true if this offset appears after other
    */
    public boolean greaterThan(TextOffset other)
    {
        return fOffset > other.fOffset ||
            (fOffset == other.fOffset && fPlacement == AFTER_OFFSET && other.fPlacement == BEFORE_OFFSET);
    }

    /**
    * Return true if this offset is 'lessThan' other.  If the fOffset fields are equal, the
    * placement field is considered, and BEFORE_OFFSET is considered 'lessThan' AFTER_OFFSET.
    *
    * @param other the other offset
    * @return true if this offset appears before other
    */
    public boolean lessThan(TextOffset other) {

        return fOffset < other.fOffset ||
            (fOffset == other.fOffset && fPlacement == BEFORE_OFFSET && other.fPlacement == AFTER_OFFSET);
    }

    /**
    * Copy the value of another TextOffset into this
    * @param other the TextOffset to copy
    */
    public void assign(TextOffset other) {
        fOffset = other.fOffset;
        fPlacement = other.fPlacement;
    }

    /**
    * Return a string representation of this object.
    */
    public String toString() {

        return "[" + (fPlacement ? "before " : "after ") + fOffset + "]";
    }
}
