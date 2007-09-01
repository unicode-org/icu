/*
 * (C) Copyright IBM Corp. 1998-2007.  All Rights Reserved.
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
    Right now, you have to construct this class with a charBuffer.  That's pretty ugly... */

/*
    8/8/96
        Added replace method, which reads styles from a ParagraphIterator.
        Also, added a constructor which takes a ParagraphIterator.
        These methods are for copy/paste support.

    8/22/96
        Replace method (which takes an iterator as an argument) tests for a
        0-length iterator.

    9/30/96
        {jbr} modified paragraphLimit();

    10/23/96
        This class now maintains paragraph styles.  Also has a timestamp.

    10/25/96
        Holds on to Style instead of Style.

    7/31/98 Switched to AttributeMap

*/

/**
* This class stores offsets where paragraph breaks occur, and the style applied to
* each paragraph.
*
* The offsets where paragraph breaks occur are stored in a RunArray object.  This is
* not strictly necessary, but it makes scanning the text for paragraph breaks unnecessary.
* However, it makes determining where paragraphs start a little confusing.  If there is a
* paragraph break at offset p, then there will be a paragraph start at offset p+1.
* If the last character in the text is a paragraph break, there will be a run array entry
* for that character (and also a paragraph style for that paragraph, even though the
* style does not apply to any text).
*
* The style of the first paragraph in the text is in the fFirstStyle member.  Other
* paragraph styles are stored in the fStyleTable array, in the following manner:  the
* paragraph with begins at offset fRunArray.fRunStart[i]+1 has style fStyleTable[i].
* The style table's "gap range" is kept in sync with the RunArray.
*
* This class propogates paragraph styles in the "Microsoft Word" fashion:  styles
* propogate backward from paragraph breaks.
*
* This class maintains a time stamp, which changes every time extra formatting (formatting
* on a range other than the current selection) is needed;  for example, when a paragraph
* break is removed.
*/


final class ParagraphBuffer extends MParagraphBuffer implements Externalizable {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private static final int kInitialSize = 10;
    private static final int CURRENT_VERSION = 1;
    private static final long serialVersionUID = 22356934;

    private RunArray fRunArray;
    private AttributeMap[] fStyleTable;
    private AttributeMap fFirstStyle;

    private static final boolean isParagraphBreak(char c) {

        return c =='\u2029' || c == '\n';
    }

/**
* Construct a new paragraph buffer from the characters in <tt>charBuffer</tt>.
*/
    ParagraphBuffer(MCharBuffer charBuffer) {

        this(charBuffer.length());

        // scan text for paragraph boundaries

        int textLength = fRunArray.getCurTextLength();

        for (int pos=0; pos < textLength; pos++) {

            if (isParagraphBreak(charBuffer.at(pos))) {
                if (fRunArray.fPosEnd+1 >= fRunArray.fNegStart)
                    expandStyleTable();
                fRunArray.fRunStart[++fRunArray.fPosEnd] = pos;
                fStyleTable[fRunArray.fPosEnd] = fFirstStyle;
            }
        }

    }

/**
* Private constructor.
*/
    private ParagraphBuffer(int initialLength) {

        fRunArray = new RunArray(kInitialSize, initialLength);
        fStyleTable = new AttributeMap[fRunArray.getArrayLength()];

        fFirstStyle = AttributeMap.EMPTY_ATTRIBUTE_MAP;
    }

    /**
     * Note: this constructor is ONLY for use by the Serialization
     * mechanism.  It does not leave this object in a valid state!
     */
    public ParagraphBuffer() {
    }

    public void writeExternal(ObjectOutput out) throws IOException {

        compress();
        out.writeInt(CURRENT_VERSION);
        out.writeObject(fRunArray);
        out.writeObject(fStyleTable);
        out.writeObject(fFirstStyle);
    }

    public void readExternal(ObjectInput in) throws IOException,
                                                ClassNotFoundException {

        if (in.readInt() != CURRENT_VERSION) {
            throw new IOException("Invalid version of ParagraphBuffer");
        }
        fRunArray = (RunArray) in.readObject();
        fStyleTable = (AttributeMap[]) in.readObject();
        fFirstStyle = (AttributeMap) in.readObject();
    }

/**
* Shift table such that the last positive run starts before pos.
*/
    private void shiftTableTo(int pos) {

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

    void compress() {

        int oldNegStart = fRunArray.fNegStart;
        fRunArray.compress();
        if (fRunArray.fNegStart != oldNegStart) {
            handleArrayResize(oldNegStart);
        }
    }

/**
* Make more room in run/style tables.
*/
    private void expandStyleTable() {

        int oldNegStart = fRunArray.fNegStart;
        fRunArray.expandRunTable();
        handleArrayResize(oldNegStart);
    }

/**
* Process a character insertion at offset <tt>start</tt>.
* If a paragraph break was inserted, propogate paragraph style at
* <tt>start</tt> to new paragraph.
*/
    public void insertText(int start, char insertedChar) {

        shiftTableTo(start);
        if (isParagraphBreak(insertedChar)) {
            if (fRunArray.fPosEnd+1 >= fRunArray.fNegStart)
                expandStyleTable();
            fRunArray.fRunStart[++fRunArray.fPosEnd] = start;
            fStyleTable[fRunArray.fPosEnd] =
                (fRunArray.fPosEnd == 0)? fFirstStyle : fStyleTable[fRunArray.fPosEnd-1];
            fRunArray.runStartsChanged();
        }

        //fRunArray.fCurTextLength++;
        fRunArray.addToCurTextLength(1);
    }

/**
* Process character insertion at offset <tt>start</tt>.
* Each new paragraph gets paragraph style at
* <tt>start</tt>.
*/
    public void insertText(int start, char srcChars[], int srcStart, int srcLimit) {

        shiftTableTo(start);

        int adjust = start - srcStart;

        for (int i=srcStart;  i < srcLimit; i++)
            if (isParagraphBreak(srcChars[i])) {
                if (fRunArray.fPosEnd+1 >= fRunArray.fNegStart)
                    expandStyleTable();
                fRunArray.fRunStart[++fRunArray.fPosEnd] = adjust + i;
                fStyleTable[fRunArray.fPosEnd] =
                    (fRunArray.fPosEnd == 0)? fFirstStyle : fStyleTable[fRunArray.fPosEnd-1];
                fRunArray.runStartsChanged();
            }

        //fRunArray.fCurTextLength += (srcLimit-srcStart);
        fRunArray.addToCurTextLength(srcLimit-srcStart);
    }

/**
* Process deletion by removing paragraph breaks contained in
* deleted range.  Propogate paragraph styles backward, if necessary.
*/
    public void deleteText(int start, int limit, int[] damagedRange) {

        int length = limit - start;
        if (length < 0) {
            throw new IllegalArgumentException("Invalid range");
        }

        shiftTableTo(limit);

        int newEnd = fRunArray.findRunContaining(start-1);

        if (newEnd != fRunArray.fPosEnd) {

            AttributeMap propStyle = fStyleTable[fRunArray.fPosEnd];
            boolean propogated;

            if (newEnd == -1) {
                propogated = !propStyle.equals(fFirstStyle);
                fFirstStyle = propStyle;
            }
            else {
                propogated = !propStyle.equals(fStyleTable[newEnd]);
                fStyleTable[newEnd] = propStyle;
            }

            if (propogated) {
                int pStart = (newEnd==-1)? 0 : fRunArray.fRunStart[newEnd] + 1;
                damagedRange[0] = Math.min(damagedRange[0], pStart);
            }

            fRunArray.fPosEnd = newEnd;
        }

        fRunArray.addToCurTextLength(-length);

        fRunArray.runStartsChanged();
    }

/**
* Returns the start of the paragraph containing offset <tt>pos</tt>.
*/
    public int paragraphStart(int pos) {

        int run = fRunArray.findRunContaining(pos-1);
        if (run == -1) {
            return 0;
        }
        else {
            return fRunArray.getLogicalRunStart(run) + 1;
        }
    }

/**
* Returns the limit of the paragraph containing offset <tt>pos</tt>.
*/
    public int paragraphLimit(int pos) {

        int run = fRunArray.findRunContaining(pos-1);

        if (run == fRunArray.fPosEnd)
            run = fRunArray.fNegStart;
        else
            run++;

        if (run == fRunArray.getArrayLength()) {
            return fRunArray.getCurTextLength();
        }

        int start = fRunArray.getLogicalRunStart(run);

        return start+1;
    }

/**
* Returns the style of the paragraph containing offset <tt>pos</tt>.
*/
    public AttributeMap paragraphStyleAt(int offset) {

        int run = fRunArray.findRunContaining(offset-1);
        if (run < 0)
            return fFirstStyle;
        else
            return fStyleTable[run];
    }

/**
* Create paragraph iterator.
*/
/*
    public MParagraphIterator createParagraphIterator(int start, int limit) {

        return new ParagraphIterator(start, limit);
    }
*/

/**
* Called by iterator to get run info.
*/
    private void setIterator(int pos, ParagraphIterator iter) {

        if ((pos < 0) || (pos >= fRunArray.getCurTextLength())) {
            iter.set(0, 0, kNoRun, null);
            return;
        }

        int run;

        if (pos > 0)
            run = fRunArray.findRunContaining(pos-1);
        else
            run = -1;

        setIteratorUsingRun(run, iter);
    }

/**
* Called by iterator to get run info.
*/
    private void setIteratorUsingRun(int run, ParagraphIterator iter) {

        int lastValidRun = fRunArray.lastRun();

        if (run < -1 || run > lastValidRun) {
            iter.set(0, 0, kNoRun, null);
            return;
        }

        if (run == fRunArray.fPosEnd+1)
            run = fRunArray.fNegStart;
        else if (run == fRunArray.fNegStart-1)
            run = fRunArray.fPosEnd;

        int runStart;
        AttributeMap style;

        if (run < 0) {
            runStart = 0;
            style = fFirstStyle;
        }
        else {
            runStart = fRunArray.fRunStart[run];
            style = fStyleTable[run];
            if (runStart < 0)
                runStart += fRunArray.getCurTextLength();
            runStart++;
        }

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
            runLimit++;
            }

        iter.set(runStart, runLimit, run, style);
    }

/**
* Replace paragraph breaks/styles between start and length with paragraph breaks/styles
* from <tt>srcText</tt>.
* @param start an offset into the text
* @param limit the index after the last character to replace
* @param srcText the text from which new paragraphs are taken
* @param srcStart the start of the range in <code>srcText</code> to copy
* @param srcLimit the first index after the range in <code>srcText</code> to copy
*/
    public void replace(int start,
                        int limit,
                        MConstText srcText,
                        int srcStart,
                        int srcLimit,
                        int[] damagedRange) {

        final int insLength = srcLimit - srcStart;
        if (insLength < 0) {
            throw new Error("invalid range");
        }
        final int origLength = fRunArray.getCurTextLength();
        deleteText(start, limit, damagedRange);

        if (insLength == 0)
            return;

        final int oldPosEnd = fRunArray.fPosEnd;
        AttributeMap origStyle;
        if (limit < origLength) {
            origStyle = (fRunArray.fPosEnd>=0)? fStyleTable[fRunArray.fPosEnd] : fFirstStyle;
        }
        else {
            origStyle = srcText.paragraphStyleAt(srcLimit);
        }

        int paragraphStart = srcStart;
        int lastPLimit = srcText.paragraphStart(srcLimit);
        boolean separatorAtEnd = lastPLimit > srcStart && isParagraphBreak(srcText.at(lastPLimit-1));

        if (limit == origLength && lastPLimit == paragraphStart) {
            if (fRunArray.fPosEnd > 0) {
                fStyleTable[fRunArray.fPosEnd] = origStyle;
            }
            else {
                fFirstStyle = origStyle;
            }
        }
        else {
            boolean firstPass = true;
            while (paragraphStart < lastPLimit) {

                AttributeMap style = srcText.paragraphStyleAt(paragraphStart);
                int paragraphLimit = srcText.paragraphLimit(paragraphStart);

                if (fRunArray.fPosEnd+1 >= fRunArray.fNegStart)
                    expandStyleTable();

                if (fRunArray.fPosEnd >= 0) {
                    if (!style.equals(fStyleTable[fRunArray.fPosEnd])) {
                        fStyleTable[fRunArray.fPosEnd] = style;
                        if (firstPass) {
                            int pStart = fRunArray.fRunStart[fRunArray.fPosEnd]+1;
                            damagedRange[0] = Math.min(damagedRange[0], pStart);
                        }
                    }
                }
                else if (!style.equals(fFirstStyle)) {
                    fFirstStyle = style;
                    damagedRange[0] = 0;
                }

                firstPass = false;

                if (paragraphLimit < lastPLimit || separatorAtEnd) {
                    fRunArray.fRunStart[++fRunArray.fPosEnd] = paragraphLimit - 1 + start - srcStart;
                }
                paragraphStart = paragraphLimit;
            }
            if (fRunArray.fPosEnd != oldPosEnd) {
                fStyleTable[fRunArray.fPosEnd] = origStyle;
            }
        }

        fRunArray.addToCurTextLength(insLength);
    }

/**
* Modify the style of all paragraphs containing offsets in the range [start, limit) to
* <tt>style</tt>.
*/
    public boolean modifyParagraphStyles(int start,
                                         int limit,
                                         StyleModifier modifier,
                                         int[] damagedRange)  {

        int run = fRunArray.findRunContaining(start-1);
        int currentPStart;
        if (run == -1) {
            currentPStart = 0;
        }
        else {
            currentPStart = fRunArray.getLogicalRunStart(run) + 1;
        }

        boolean modifiedAnywhere = false;

        for (;;) {

            boolean modified = false;

            if (run < 0) {

                AttributeMap newStyle = modifier.modifyStyle(fFirstStyle);

                if (!newStyle.equals(fFirstStyle)) {
                    fFirstStyle = newStyle;
                    modified = true;
                }
            }
            else {

                AttributeMap newStyle = modifier.modifyStyle(fStyleTable[run]);

                if (!fStyleTable[run].equals(newStyle)) {
                    fStyleTable[run] = newStyle;
                    modified = true;
                }
            }

            if (run == fRunArray.fPosEnd) {
                run = fRunArray.fNegStart;
            }
            else {
                run++;
            }

            int nextPStart;
            if (run == fRunArray.getArrayLength()) {
                nextPStart = fRunArray.getCurTextLength();
            }
            else {
                nextPStart = fRunArray.getLogicalRunStart(run) + 1;
            }

            if (modified) {
                modifiedAnywhere = true;
                damagedRange[0] = Math.min(damagedRange[0], currentPStart);
                damagedRange[1] = Math.max(damagedRange[1], nextPStart);
            }

            if (limit <= nextPStart) {
                break;
            }
            else {
                currentPStart = nextPStart;
            }
        }

        return modifiedAnywhere;
    }

//    private static void dumpParagraphStarts(ParagraphBuffer st) {
//
//        System.out.println("fRunArray.fPosEnd="+st.fRunArray.fPosEnd+", fRunArray.fNegStart="+st.fRunArray.fNegStart+
//                            ", fRunArray.getArrayLength()="+st.fRunArray.getArrayLength()+", fRunArray.getCurTextLength()="+st.fRunArray.getCurTextLength());
//
//        int i;
//        System.out.print("Positives: ");
//        for (i=0; i<=st.fRunArray.fPosEnd; i++)
//            System.out.print(st.fRunArray.fRunStart[i]+" ");
//
//        System.out.print("   Negatives: ");
//        for (i=st.fRunArray.fNegStart; i<st.fRunArray.getArrayLength(); i++)
//            System.out.print(st.fRunArray.fRunStart[i]+" ");
//
//        System.out.println(" ");
//    }

    private static final int kNoRun = -42; // iterator use

    private final class ParagraphIterator /*implements MParagraphIterator*/
    {
        ParagraphIterator(int start, int limit)
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
            return fCurrentRun != kNoRun;
        }

        public void next()
        {
            if (fRunLimit < fLimit) {
                fCurrentRun++;
                setIteratorUsingRun(fCurrentRun, this);
            }
            else
                set(0, 0, kNoRun, null);
        }

        public void prev()
        {
            if (fRunStart > fStart) {
                fCurrentRun--;
                setIteratorUsingRun(fCurrentRun, this);
            }
            else
                set(0, 0, kNoRun, null);
        }

        public void set(int pos)
        {
            if (pos >= fStart && pos < fLimit) {
                setIterator(pos, this);
            } else {
                set(0, 0, kNoRun, null);
            }
        }

        // ParagraphBuffer calls back on this to set iterators
        void set(int start, int limit, int currentRun, AttributeMap style)
        {
            fRunStart = start < fStart ? fStart : start;
            fRunLimit = limit > fLimit ? fLimit : limit;
            fCurrentRun = currentRun;
            fStyle = style;
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

        public AttributeMap style() {

            return fStyle;
        }

        private int fStart;
        private int fLimit;
        private int fRunStart;
        private int fRunLimit;
        private int fCurrentRun;
        private AttributeMap fStyle;
    }
}
