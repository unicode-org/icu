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
/**
* This class maintains intervals within a piece of text.  Interval boundaries
* are stored in the fRunStart array.  Interval boundaries may have a
* positive or negative representation.  A positive boundary is given as an offset
* from 0.  A negative boundary is given as a negative offset from the ned of the text.
* The RunArray stores positive boundaries in the entries [0, fPosEnd], and negative
* boundaries in the entries [fNegStart, fLength).  New boundaries may be inserted into
* the undefined middle of the RunArray.  If fPosEnd < 0, there are no positive entries.
* If fNegStart >= fRunArray.length, there are no negative netries.  It's possible to have
* a runarray with neither positive or negative entries.
*
* As an example of how the RunArray works, consider a piece of text with 5 intervals,
* where each interval is 3 characters in length.  The RunArray for this text could
* look like:
*    fCurTextLength = 15, fPosEnd = 5, fNegStart = 10,
*    fRunStart = { 0, 3, 6, 9, 12, U, U, U, U, U };
* where U is an undefined array element.

* An equivalent representation would be:
*    fCurTextLength = 15, fPosEnd = 3, fNegStart = 8,
*    fRunStart = { 0, 3, 6, U, U, U, U, U, -6, -3 };
*
* The RunArray class is used in the StyleBuffer and the ParagraphBuffer.  In the StyleBuffer,
* the entries in fRunStart give the offsets where style runs begin.  In the
* ParagraphBuffer, the fRunStart entries store offsets of paragraph breaks.
*
* This class provides methods for shifting the run table to a particular position, expanding the
* run table, and returning the index of the run containing a particular offset in the text.  All
* other functionality is implemented in the RunArray clients.
*
* RunArray uses FastIntBinarySearch for searches.  The searches are constructed on demand in
* the findRunContaining method.  The searches are invalidated when the run array is shifted;
* however, the RunArray can be modified by other classes.  Thus, if another class modifies
* the entries in fRunArray, or modifies fPosEnd or fNegStart, it is responsible for
* calling runStartsChanged.
*/

package com.ibm.richtext.styledtext;

import java.io.Externalizable;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.IOException;

final class RunArray implements Externalizable {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private static final long serialVersionUID = 22356934;

    int[] fRunStart;
    private int fCurTextLength;
    int fPosEnd, fNegStart;
    
    transient private FastIntBinarySearch fPosSearch;
    transient private boolean fPosSearchValid;
    transient private FastIntBinarySearch fNegSearch;
    transient private boolean fNegSearchValid;

    private static final int CURRENT_VERSION = 1;

    RunArray(int initialSize, int curTextLength) {

        fRunStart = new int[initialSize];
        fCurTextLength = curTextLength;
        fPosEnd = -1;
        fNegStart = initialSize;
        
        fPosSearch = new FastIntBinarySearch(fRunStart, 0, 1);
        fNegSearch = new FastIntBinarySearch(fRunStart, 0, 1);
        fPosSearchValid = fNegSearchValid = false;
    }

    /**
     * Note: this constructor is ONLY for use by the Serialization
     * mechanism.  It does not leave this object in a valid state!
     */
    public RunArray() {
    }

    public void writeExternal(ObjectOutput out) throws IOException {

        out.writeInt(CURRENT_VERSION);
        out.writeObject(fRunStart);
        out.writeInt(fCurTextLength);
        out.writeInt(fPosEnd);
        out.writeInt(fNegStart);
    }

    public void readExternal(ObjectInput in) throws IOException,
                                            ClassNotFoundException {

        if (in.readInt() != CURRENT_VERSION) {
            throw new IOException("Invalid version of RunArray");
        }
        fRunStart = (int[]) in.readObject();
        fCurTextLength = in.readInt();
        fPosEnd = in.readInt();
        fNegStart = in.readInt();
        
        fPosSearch = new FastIntBinarySearch(fRunStart, 0, 1);
        fNegSearch = new FastIntBinarySearch(fRunStart, 0, 1);
        fPosSearchValid = fNegSearchValid = false;
    }

    public int getCurTextLength() {

        return fCurTextLength;
    }

    public void setCurTextLength(int curTextLength) {

        fCurTextLength = curTextLength;
    }

    public void addToCurTextLength(int delta) {

        fCurTextLength += delta;
    }

    public void runStartsChanged() {

        fPosSearchValid = fNegSearchValid = false;
    }

/**
* Returns the index of the last valid run.
*/
    int lastRun() {

        return (fNegStart == fRunStart.length)? fPosEnd : fRunStart.length-1;
    }

/**
* Returns the length of the run array.  Replaces old fLength member.
*/
    int getArrayLength() {

        return fRunStart.length;
    }

/**
* Shifts style table such that the last positive run
* starts before pos.
*/
    void shiftTableTo(int pos) {

        int oldPosEnd = fPosEnd;

        while (fPosEnd >= 0 && fRunStart[fPosEnd] >= pos) {

            fNegStart--;
            fRunStart[fNegStart] = fRunStart[fPosEnd] - fCurTextLength;
            fPosEnd--;

        }

        pos -= fCurTextLength;

        while (fNegStart<fRunStart.length && fRunStart[fNegStart] < pos) {

            fPosEnd++;
            fRunStart[fPosEnd] = fRunStart[fNegStart] + fCurTextLength;
            fNegStart++;
        }

        if (oldPosEnd != fPosEnd) {
            fPosSearchValid = fNegSearchValid = false;
        }
    }

/**
* Returns index of style run containing pos.  If first style run starts before
* pos, -1 is returned.  If pos is greater than text length, lastrun is returned.
*/
    int findRunContaining(int pos) {

        FastIntBinarySearch search;
        final int length = fRunStart.length;

        if (fNegStart < length && (pos-fCurTextLength >= fRunStart[fNegStart])) {

            pos -= fCurTextLength;

            if (!fNegSearchValid) {
                fNegSearch.setData(fRunStart, fNegStart, length-fNegStart);
            }
            search = fNegSearch;
        }
        else if (fPosEnd >= 0) {

            if (!fPosSearchValid) {
                fPosSearch.setData(fRunStart, 0, fPosEnd+1);
            }
            search = fPosSearch;
        }
        else
            return -1;

        int run = search.findIndex(pos);

        return run;
    }

    int getLogicalRunStart(int run) {

        if (run == -1) {
            return 0;
        }
        else if (run == fRunStart.length) {
            return fCurTextLength;
        }
        else {
            if (run <= fPosEnd) {
                return fRunStart[run];
            }
            else if (run >= fNegStart) {
                return fRunStart[run] + fCurTextLength;
            }
            else {
                throw new IllegalArgumentException("Illegal run");
            }
        }
    }

/**
* Increases size of run table.  Current implementation doubles the run table's size.
*/
    void expandRunTable() {

        resizeRunTable(fRunStart.length * 2);
    }

/**
* Return the minimum number of elements possible in fRunStart.
*/
    private int getMinSize() {

        return Math.max(fPosEnd + (fRunStart.length-fNegStart) + 1, 1);
    }

    void compress() {

        int minSize = getMinSize();
        if (fRunStart.length > minSize) {
            resizeRunTable(minSize);
        }
    }

    private void resizeRunTable(int newSize) {

        if (newSize < getMinSize()) {
            throw new IllegalArgumentException("Attempt to make RunArray too small.");
        }

        final int oldLength = fRunStart.length;

        int newRunStart[] = new int[newSize];
        System.arraycopy(fRunStart, 0, newRunStart, 0, fPosEnd+1);
        int newNegStart = newRunStart.length - (oldLength-fNegStart);
        System.arraycopy(fRunStart, fNegStart, newRunStart, newNegStart, (oldLength-fNegStart));

        fNegStart = newNegStart;
        fRunStart = newRunStart;

        fPosSearchValid = fNegSearchValid = false;
    }
}
