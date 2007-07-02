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

import com.ibm.richtext.textlayout.attributes.AttributeMap;

/*
    8/2/96
        Added setIteratorUsingRun method

    8/5/96
        No longer has to be constructed with an MText.

    8/8/96
        Added replace method, which reads styles from a StyleRunIterator.
        Also, added a constructor which takes a StyleRunIterator.
        These methods are for copy/paste support.
    8/16/96
        StyleBuffer now takes MConstText instead of MText where possible.

    10/23/96
        Some old commented-out code removed for aesthetic reasons.

    7/31/98 Switched to AttributeMap
*/

/**
* StyleBuffer implements <tt>MStyleBuffer</tt>.  It maintains
* <tt>AttributeMap</tt> objects to apply to the text in an <tt>MText</tt> object,
* and the
* intervals on which those styles apply.
* <p>
* StyleBuffer stores the intervals on which styles apply in a <tt>RunArray</tt>
* object (see <tt>RunArray</tt> for more information).  The styles are stored in
* an array of <tt>AttributeMap</tt> objects.
* <p>
* <tt>RunArray</tt> maintains an array of integers which represent offsets into text.
* The array has a "positive" region in which offsets are given as positive distances
* from the start of the text, and a "negative" region in which offsets are given as
* negative distances from the end of the text.  Between the positive and negative regions
* is a gap, into which new offsets may be inserted.  This storage scheme allows for
* efficient response to a series of editing operations which occur in the same area of the
* text.
* <p>
* StyleBuffer uses the offsets in <tt>RunArray</tt> as the boundaries of style runs.
* A style run begins at each offset in <tt>RunArray</tt>, and each style run continues to
* the next offset.  The style array is kept in sync with the array of offsets in <tt>RunArray</tt>;
* that is, the style which begins at RunArray[i] is stored in StyleArray[i].
* <p>
* The first entry in the <tt>RunArray</tt> is always 0.
*
* @author John Raley
*
* @see AttributeMap
* @see MText
* @see RunArray
*/

final class StyleBuffer extends MStyleBuffer implements Externalizable {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    /**
    * Creates a new style buffer with length equal to the length of <tt>text</tt>,
    * and with a single run of <tt>defaultStyle</tt>.
    */
    private static final long serialVersionUID = 22356934;

    private static final int CURRENT_VERSION = 1;
    private static final int kInitialSize = 10;
    private RunArray fRunArray;

    private AttributeMap fStyleTable[];

    StyleBuffer(MConstText text, AttributeMap initialStyle) {

        this(text.length(), initialStyle);
    }

    /**
    * Creates a new style buffer with length <tt>initialLength</tt> and with a
    * single run of <tt>defaultStyle</tt>.
    */

    StyleBuffer(int initialLength, AttributeMap initialStyle) {

        fRunArray = new RunArray(kInitialSize, initialLength);
        fRunArray.fPosEnd = 0;
        fRunArray.fRunStart[0] = 0;

        fStyleTable = new AttributeMap[kInitialSize]; // do I really want to do this???

        fStyleTable[0] = initialStyle;
    }

    /**
     * Note: this constructor is ONLY for use by the Serialization
     * mechanism.  It does not leave this object in a valid state!
     */
    public StyleBuffer() {
    }

    public void writeExternal(ObjectOutput out) throws IOException {

        compress();
        out.writeInt(CURRENT_VERSION);
        out.writeObject(fRunArray);
        out.writeObject(fStyleTable);
    }

    public void readExternal(ObjectInput in) throws IOException,
                                                ClassNotFoundException {

        if (in.readInt() != CURRENT_VERSION) {
            throw new IOException("Invalid version of StyleBuffer");
        }
        fRunArray = (RunArray) in.readObject();
        fStyleTable = (AttributeMap[]) in.readObject();
    }

/**
* Shift style and run tables such that the last positive run begins before the given position.
* Since there is always a run start at 0, this method ensures that the first run will not be shifted.
* This is called by: <tt>insertText</tt> and <tt>deleteText</tt>.
* @param pos a position in the text.
*/

    private void shiftTableTo(int pos) {

        if (pos == 0)
            pos = 1;

        int oldNegStart = fRunArray.fNegStart;
        int oldPosEnd = fRunArray.fPosEnd;

        fRunArray.shiftTableTo(pos);

        if (oldPosEnd > fRunArray.fPosEnd)
            System.arraycopy(fStyleTable, fRunArray.fPosEnd+1,
                             fStyleTable, fRunArray.fNegStart,
                             oldPosEnd-fRunArray.fPosEnd);
        else if (oldNegStart < fRunArray.fNegStart)
            System.arraycopy(fStyleTable, oldNegStart,
                             fStyleTable, oldPosEnd+1,
                             fRunArray.fNegStart-oldNegStart);
    }

/**
* Update the style table to reflect a change in the RunArray's size.
*/
    private void handleArrayResize(int oldNegStart) {

        AttributeMap newStyleTable[] = new AttributeMap[fRunArray.getArrayLength()];
        System.arraycopy(fStyleTable, 0, newStyleTable, 0, fRunArray.fPosEnd+1);
        System.arraycopy(fStyleTable, oldNegStart, newStyleTable, fRunArray.fNegStart, (fRunArray.getArrayLength()-fRunArray.fNegStart));
        fStyleTable = newStyleTable;
    }

/**
* Minimize the amount of storage used by this object.
*/
    void compress() {

        int oldNegStart = fRunArray.fNegStart;
        fRunArray.compress();
        if (fRunArray.fNegStart != oldNegStart) {
            handleArrayResize(oldNegStart);
        }
    }

/**
* Increase the storage capacity of the style and run tables if no room remains.
*/
    private void expandStyleTableIfFull() {

        if (fRunArray.fPosEnd + 1 == fRunArray.fNegStart) {

            int oldNegStart = fRunArray.fNegStart;
            fRunArray.expandRunTable();
            handleArrayResize(oldNegStart);
        }
    }

/*
    public MStyleRunIterator createStyleRunIterator(int start, int limit) {

        return new StyleRunIterator(start, limit);
    }
*/
/**
* Respond to an insertion in the text.  The length of the last style run which
* begins before <tt>start</tt> is increased by <tt>length</tt>.  The run table
* is shifted such that the run into which text was inserted is the last positive run.
* This implementation assumes that all styles propogate.
* @param start the offset where the insertion began
* @param length the number of characters inserted
*/
    public void insertText(int start, int limit) {

        shiftTableTo(start);
        fRunArray.addToCurTextLength(limit - start);
    }

/**
* Respond to a deletion in the text.  The last style run before
* <tt>start</tt> is truncated to end at <tt>start</tt>.  The
* style run containing (<tt>start</tt>+<tt>length</tt>) is set to begin
* at (<tt>start</tt>+<tt>length</tt>).  Runs in between are deleted.
* If the deletion occurs entirely within one style run, the length of the style
* run is reduced by <tt>length</tt>.
* This implementation assumes that all styles propogate.
* This method shifts the run table such that the run in which the delete began
* is the last positive run.  Other methods depend on this "side effect".
* @param start the offset where the deletion began
* @param length the offset where the deletion stopped
*/
    public void deleteText(int start, int limit) {

        int length = limit - start;

        // An optimization - if a whole run wasn't deleted we don't
        // need to check for run merging, which could be expensive.
        boolean wholeRunDeleted = false;

        shiftTableTo(start);

        int firstRunLimit = fRunArray.getCurTextLength();
        if (fRunArray.fNegStart < fRunArray.getArrayLength())
            firstRunLimit += fRunArray.fRunStart[fRunArray.fNegStart];

        if (limit == fRunArray.getCurTextLength()) {
            fRunArray.fNegStart = fRunArray.getArrayLength();
        }
        else if (limit >= firstRunLimit) {

            int end = fRunArray.findRunContaining(limit);
            if (end != fRunArray.fPosEnd) {
                fRunArray.fRunStart[end] = limit - fRunArray.getCurTextLength();
                fRunArray.fNegStart = end;
                wholeRunDeleted = true;
            }
        }

        if (fRunArray.fNegStart != fRunArray.getArrayLength()) {
            if (start == 0 && limit >= firstRunLimit) {
                // the first style run was deleted;  move first "negative" run into
                // first position
                fStyleTable[0] = fStyleTable[fRunArray.fNegStart++];
            }
            else if (wholeRunDeleted) {
                if (fStyleTable[fRunArray.fNegStart].equals(fStyleTable[fRunArray.fPosEnd])) {
                    // merge style runs
                    fRunArray.fNegStart++;
                }
            }
        }

        fRunArray.addToCurTextLength(-length);

        fRunArray.runStartsChanged();
        //System.out.println("In deleteText:  number of style runs = " + numRuns(this));
    }

/**
* Arrange style table so that old styles in the provided range are removed, and
* new styles can be inserted into the insertion gap.
* After calling this method, new style starts and styles may be placed
* in the insertion gaps of fRunArray.fStyleStart and fStyleTable.
* @param start offset in the text where insertion operation begins
* @param limit offset in the text where previous styles resume
*/
    private void prepareStyleInsert(int start) {

        if (start == 0) {

            // fRunArray.fPosEnd should be 0 if we're in this branch.

            if (fRunArray.getCurTextLength() > 0) {

                /* Move first existing style run to negative end of buffer.
                   Don't do this if length==0;  that is, if there is no real
                   style run at 0.
                 */

                fRunArray.fNegStart--;
                fStyleTable[fRunArray.fNegStart] = fStyleTable[0];
                fRunArray.fRunStart[fRunArray.fNegStart] = -fRunArray.getCurTextLength();
            }

            fRunArray.fPosEnd = -1;
        }
        else {

            // consistency check: start should be in current gap
            if (fRunArray.fRunStart[fRunArray.fPosEnd] >= start) {
                throw new Error("Inconsistent state!  Start should be within insertion gap.");
            }

            int endOfInsertionGap = fRunArray.getCurTextLength();
            if (fRunArray.fNegStart < fRunArray.getArrayLength()) {
                endOfInsertionGap += fRunArray.fRunStart[fRunArray.fNegStart];
            }

            if (endOfInsertionGap < start) {
                throw new Error("Inconsistent state!  Start should be within insertion gap.");
            }

            // if no break at start (on negative end of buffer) make one

            if (endOfInsertionGap != start) {

                // split style run in insertion gap

                expandStyleTableIfFull();

                fRunArray.fNegStart--;
                fStyleTable[fRunArray.fNegStart] = fStyleTable[fRunArray.fPosEnd];
                fRunArray.fRunStart[fRunArray.fNegStart] = start - fRunArray.getCurTextLength();

                //System.out.println("splitting run.");
            }
        }
    }

    public boolean modifyStyles(int start,
                                int limit,
                                StyleModifier modifier,
                                int[] damagedRange) {

        if (limit == start) {
            return false;
        }

        shiftTableTo(start);

        int currentRunStart = start;
        AttributeMap oldStyle;
        AttributeMap mergeStyle = fStyleTable[fRunArray.fPosEnd];

        if (fRunArray.fNegStart < fRunArray.getArrayLength() &&
                fRunArray.fRunStart[fRunArray.fNegStart]+fRunArray.getCurTextLength() == start) {

            oldStyle = fStyleTable[fRunArray.fNegStart];
            ++fRunArray.fNegStart;
        }
        else {
            oldStyle = mergeStyle;
        }

        boolean modifiedAnywhere = false;
        for(;;) {

            boolean modified = false;

            // push new style into gap on positive side
            AttributeMap newStyle = modifier.modifyStyle(oldStyle);
            if (damagedRange != null && !newStyle.equals(oldStyle)) {
                modified = modifiedAnywhere = true;
                damagedRange[0] = Math.min(currentRunStart, damagedRange[0]);
            }

            if (!newStyle.equals(mergeStyle)) {

                if (currentRunStart != 0) {
                    expandStyleTableIfFull();
                    ++fRunArray.fPosEnd;
                }

                fStyleTable[fRunArray.fPosEnd] = newStyle;
                fRunArray.fRunStart[fRunArray.fPosEnd] = currentRunStart;
            }

            mergeStyle = newStyle;

            int nextRunStart = fRunArray.getLogicalRunStart(fRunArray.fNegStart);

            if (limit > nextRunStart) {
                oldStyle = fStyleTable[fRunArray.fNegStart];
                currentRunStart = nextRunStart;
                if (modified) {
                    damagedRange[1] = Math.max(currentRunStart, damagedRange[1]);
                }
                ++fRunArray.fNegStart;
            }
            else {
                if (limit < nextRunStart && !oldStyle.equals(mergeStyle)) {
                    expandStyleTableIfFull();
                    ++fRunArray.fPosEnd;
                    fStyleTable[fRunArray.fPosEnd] = oldStyle;
                    fRunArray.fRunStart[fRunArray.fPosEnd] = limit;
                }
                if (modified) {
                    damagedRange[1] = Math.max(limit, damagedRange[1]);
                }
                break;
            }
        }

        // merge last run if needed
        if ((fRunArray.fNegStart < fRunArray.getArrayLength()) &&
                    (fStyleTable[fRunArray.fNegStart].equals(fStyleTable[fRunArray.fPosEnd]))) {
            fRunArray.fNegStart++;
        }

        fRunArray.runStartsChanged();

        return modifiedAnywhere;
    }

    public int styleStart(int pos) {

        if (pos == fRunArray.getCurTextLength()) {
            return pos;
        }

        return fRunArray.getLogicalRunStart(fRunArray.findRunContaining(pos));
    }

    public int styleLimit(int pos) {

        if (pos == fRunArray.getCurTextLength()) {
            return pos;
        }

        int run = fRunArray.findRunContaining(pos);

        if (run == fRunArray.fPosEnd) {
            run = fRunArray.fNegStart;
        }
        else {
            ++run;
        }

        return fRunArray.getLogicalRunStart(run);
    }

/**
* Return style at location <tt>pos</tt>.
* @param pos an offset into the text
* @returns the style of the character at <tt>offset</tt>
*/
    public AttributeMap styleAt(int pos) {

        return fStyleTable[ fRunArray.findRunContaining(pos) ];
    }

/*
* Set run start, run length, and run value in an iterator.  This method is
* only called by a <tt>StyleRunIterator</tt>.
* @param pos an offset into the text.  The iterator's run start and run limit are
* set to the run containing <tt>pos</tt>.
* @param iter the iterator to set
*/
    void setIterator(int pos, StyleRunIterator iter) {

        if ((pos < 0) || (pos > fRunArray.getCurTextLength())) {

            iter.set(null, 0, 0, kNoRun);
            return;
        }

        int run = fRunArray.findRunContaining(pos);

        setIteratorUsingRun(run, iter);
    }

/**
* Set run start, run length, and run value in an iterator.  This method is
* only called by a <tt>StyleRunIterator</tt>.
* @param run the index of the run to which the iterator should be set
* @param iter the iterator to set
*/
    private void setIteratorUsingRun(int run, StyleRunIterator iter) {

        int lastValidRun = fRunArray.lastRun();

        if (run < 0 || run > lastValidRun) {

            iter.set(null, 0, 0, kNoRun);
            return;
        }

        if (run == fRunArray.fPosEnd+1)
            run = fRunArray.fNegStart;
        else if (run == fRunArray.fNegStart-1)
            run = fRunArray.fPosEnd;

        int runStart = fRunArray.fRunStart[run];
        if (runStart < 0)
            runStart += fRunArray.getCurTextLength();

        AttributeMap style = fStyleTable[run];

        int nextRun;

        if (run == fRunArray.fPosEnd)
            nextRun = fRunArray.fNegStart;
        else
            nextRun = run + 1;

        int runLimit;

        if (nextRun >= fRunArray.getArrayLength())
            runLimit = fRunArray.getCurTextLength();
        else {
            runLimit = fRunArray.fRunStart[nextRun];
            if (runLimit < 0)
                runLimit += fRunArray.getCurTextLength();
        }

        //System.out.println("setIterator: pos="+pos+", runStart="+runStart+", runLimit="+runLimit+
        //                  ", run="+run+", fPosEnd="+fPosEnd);

        iter.set(style, runStart, runLimit, run);
    }

    public void replace(int start, int limit, MConstText srcText, int srcStart, int srcLimit)
    {
        deleteText(start, limit);
        if (srcStart == srcLimit)
            return;
        prepareStyleInsert(start);
        for (int j2 = srcStart; j2 < srcLimit; j2 = srcText.characterStyleLimit(j2))
        {
            AttributeMap attributeMap = srcText.characterStyleAt(j2);
            if (fRunArray.fPosEnd < 0 || !fStyleTable[fRunArray.fPosEnd].equals(attributeMap))
            {
                expandStyleTableIfFull();
                fRunArray.fPosEnd++;
                fRunArray.fRunStart[fRunArray.fPosEnd] = j2 - srcStart + start;
                fStyleTable[fRunArray.fPosEnd] = attributeMap;
            }
        }
        fRunArray.addToCurTextLength(srcLimit - srcStart);
        if (fRunArray.fNegStart < fRunArray.getArrayLength() && fStyleTable[fRunArray.fNegStart].equals(fStyleTable[fRunArray.fPosEnd]))
            fRunArray.fNegStart++;
    }

    private  static final int kNoRun = -42; // iterator use

    private final class StyleRunIterator /*implements MStyleRunIterator*/ {

        StyleRunIterator(int start, int limit)
        {
            reset(start, limit, start);
        }

        public void reset(int start, int limit, int pos)
        {
            fStart = start;
            fLimit = limit;
            setIterator(fStart, this);
        }

        public boolean isValid()
        {
            return fStyle != null;
        }

        public void next()
        {
            if (fRunLimit < fLimit) {
                fCurrentRun++;
                setIteratorUsingRun(fCurrentRun, this);
            }
            else
                set(null, 0, 0, kNoRun);
        }

        public void prev()
        {
            if (fRunStart > fStart) {
                fCurrentRun--;
                setIteratorUsingRun(fCurrentRun, this);
            }
            else
                set(null, 0, 0, kNoRun);
        }

        public void set(int pos)
        {
            if (pos >= fStart && pos < fLimit) {
                setIterator(pos, this);
            } else {
                set(null, 0, 0, kNoRun);
            }
        }

        void set(AttributeMap style, int start, int limit, int currentRun)
        {
            fStyle = style;
            fCurrentRun = currentRun;
            fRunStart = start < fStart ? fStart : start;
            fRunLimit = limit > fLimit ? fLimit : limit;
        }

        public void reset(int start, int limit)
        {
            reset(start, limit, start);
        }

        public void first()
        {
            set(fStart);
        }

        public void last()
        {
            set(fLimit - 1);
        }

        public int rangeStart()
        {
            return fStart;
        }

        public int rangeLimit()
        {
            return fLimit;
        }

        public int rangeLength()
        {
            return fLimit - fStart;
        }

        public AttributeMap style()
        {
            return fStyle;
        }

        public int runStart()
        {
            return fRunStart;
        }

        public int runLimit()
        {
            return fRunLimit;
        }

        public int runLength()
        {
            return fRunLimit - fRunStart;
        }

        private int fStart;
        private int fLimit;
        private AttributeMap fStyle;
        private int fRunStart;
        private int fRunLimit;
        private int fCurrentRun;
    }
}