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

import java.io.Externalizable;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.IOException;

/**
 * TabStop represents a position on a tab ruler.  Each tab stop has a
 * position, giving its location on the ruler, and one of several
 * types.  The type determines how a segment controled by this TabStop
 * is positioned on a line:
 * <ul>
 * <li><code>kLeading</code> - the leading edge of the segment is aligned to
 *     the TabStop's position</li>
 * <li><code>kCenter</code> - the segment is centered on this TabStop's
 *     position</li>
 * <li><code>kTrailing</code> - the trailing edge of the segment is aligned to
 *     the TabStop's position</li>
 * <li><code>kDecimal</code> - the first decimal in the segment is aligned to
 *     the TabStop's position</li>
 * <li><code>kAuto</code> - semantically the same as <code>kLeading</code>.
 *     Used by tab rulers to indicate that all subsequent tab stops
 *     will be at autospaced intervals.</li>
 * </ul>
 * @see MTabRuler
 */
public final class TabStop implements Externalizable
{
    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private static final int CURRENT_VERSION = 1;
    private static final long serialVersionUID = 22356934;

    private byte fType;    // left, center, right, decimal
    private int fPosition; // tab stop position from line origin.

    /**
     * A TabStop with this type aligns its segment's leading edge
     * to the TabStop's position.
     */
    public static final byte kLeading = 0;

    /**
     * A TabStop with this type aligns its segment's center
     * to the TabStop's position.
     */
    public static final byte kCenter = 1;

    /**
     * A TabStop with this type aligns its segment's trailing edge
     * to the TabStop's position.
     */
    public static final byte kTrailing = 2;

    /**
     * A TabStop with this type aligns its segment's first decimal
     * to the TabStop's position.
     */
    public static final byte kDecimal = 3;

    /**
     * A TabStop with this type aligns its segment's leading edge
     * to the TabStop's position.  After a TabStop of this type,
     * all tabs are at autospace intervals.  Usually, clients will
     * not construct TabStops with this type.
     */
    public static final byte kAuto = 4;

    /**
     * Create a TabStop with position 0 and type <code>kLeading</code>.
     */
    public TabStop() {

       this(0, kLeading);
    }

    /**
     * Create a TabStop with the given position and type.
     * @param position the TabStop's position
     * @param type the TabStop's type.  Must be one of constants
     *      in this class.
     */
    public TabStop(int position, byte type) {

        if (type < kLeading || type > kAuto) {
            throw new IllegalArgumentException("Invalid tab type");
        }

        fPosition = position;
        fType = type;
    }

    public void readExternal(ObjectInput in) throws IOException,
                                            ClassNotFoundException {

        int version = in.readInt();
        if (version != CURRENT_VERSION) {
            throw new IOException("Invalid version of TabStop.");
        }
        fPosition = in.readInt();
        fType = in.readByte();
    }

    public void writeExternal(ObjectOutput out) throws IOException {

        out.writeInt(CURRENT_VERSION);
        out.writeInt(fPosition);
        out.writeByte(fType);
    }

    /**
     * Compare this to another Object.  TabStops are equal if
     * their position and type are the same.
     */
    public boolean equals(Object rhs)
    {
        if (rhs == null) {
            return false;
        }

        TabStop rhsTab;
        try {
            rhsTab = (TabStop) rhs;
        }
        catch(ClassCastException e) {
            return false;
        }

        return fType == rhsTab.fType && fPosition == rhsTab.fPosition;
    }

    /**
     * Return the hash code for this TabStop.  The hash code is
     * <code>position << type</code>.
     */
    public int hashCode() {

        return fPosition << fType;
    }

    public String toString()
    {
        char typeChar;
        switch (fType) {
            case kLeading: typeChar = 'L'; break;
            case kCenter: typeChar = 'C'; break;
            case kTrailing: typeChar = 'R'; break;
            case kDecimal: typeChar = 'D'; break;
            case kAuto: typeChar = 'A'; break;
            default: typeChar = '?'; break;
        }
        return "TabStop[" + Integer.toString(fPosition) + typeChar + ']';
    }

    /**
     * Return the type of this TabStop.  Will be one of the constants
     * in this class.
     */
    public byte getType() {
        return fType;
    }

    /**
     * Return the position of this TabStop.
     */
    public int getPosition() {
        return fPosition;
    }
}

