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
// Requires Java2
// Revision: 70 1.38 richtext/AsyncFormatter.java, richtext, richtext

package com.ibm.richtext.textformat;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Point;
import java.awt.Color;

import java.text.BreakIterator;
import java.text.CharacterIterator;

import java.util.Hashtable;

import com.ibm.richtext.textlayout.attributes.AttributeMap;
import com.ibm.richtext.textlayout.attributes.TextAttribute;

import com.ibm.richtext.styledtext.MConstText;

import com.ibm.richtext.textlayout.Graphics2DConversion;

///*JDK12IMPORTS
import java.awt.Graphics2D;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.FontRenderContext;
//JDK12IMPORTS*/

/*JDK11IMPORTS
import com.ibm.richtext.textlayout.Graphics2D;
import com.ibm.richtext.textlayout.LineBreakMeasurer;
import com.ibm.richtext.textlayout.FontRenderContext;
JDK11IMPORTS*/

/*
    Change history:

    7/25/96 -

    8/15/96
        Fixed bug in textOffsetToPoint (fPixHeight wasn't added to negative heights). {jbr}

    8/19/96
        Removed references to JustificationStyle constants, and moved them into MFormatter {sfb}

    8/23/96
        Modified findLineAt and getLineContaining - added optimization in search loop.  Also,
            they were failing when fLTPosEnd+1 == fLTNegStart.  Fixed.

    8/26/96
        Moved FormatDaemon stuff into this class.

    9/11/96
        Shortened line returned from textOffsetToPoint by 1 pixel

    9/23/96
        textOffsetToPoint line length restored (see above).  drawText() now draws only lines
        which fall in rectangle param.

    9/26/96
        whitespace at end of line is used for caret positioning

    10/4/96
        Added static TextBox method.

    10/8/96
        Line 1300 - less than changed to less than or equal in textOffsetToPoint.  Watch for
            hangs in formatText.

    10/9/96
        Changed sync. model.  fFormatInBackground is used to start/stop bg formatting

    10/17/96
        Added new flags:  fLineInc, fFillInc for bidi support.  updateFormat, pointToTextOffset, getBoundingRect,
        and findNewInsertionOffset should now function correctly for non-Roman documents.  Nothing else has been
        modified to support intl text.

    10/21/96
        Pushed paragraph formatting and caret positioning into LineLayout.  In process of pushing
        highlighting into LineLayout.

    10/24/96
        Now getting paragraph styles from paragraph buffer.

    7/7/97
        Up-arrow doesn't move to beginning of text if you're on the first line.

    7/1/98
        No longer intersecting damaged rect with view rect in updateFormat.
*/

/**
* This class implements MFormatter.  It maintains a table of
* <tt>LayoutInfo</tt> instances which contain layout information
* for each line in the text.  This class formats lines on demand,
* and creates a low-priority thread to format text in the background.
* Note that, at times, some text may not have been formatted, especially
* if the text is large.
* <p>
* The line table is an array of <tt>LayoutInfo</tt> objects, which expands as needed to hold
* all computed lines in the text.  The line table consists of three
* regions:  a "positive" region, a "gap," and a "negative" region.
* In the positive region, character and graphics offsets are positive
* integers, computed from the beginning of the text / display.  In the
* gap, line table entries are null.  New lines may be inserted into the gap.
* In the negative region, character and graphics offsets are negative;
* their absolute values indicate distances from the end of the text / display.
* The <tt>fLTPosEnd</tt> member gives the index in the line table of the
* last positive entry.  The <tt>fLTNegStart</tt> gives the index of first
* negative entry.  If there are no negative entries, <tt>fLTNegStart</tt> is
* equal to <tt>fLTSize</tt>, the size of the line table.
* <p>
* Changes to the line table occur only in the <tt>formatText()</tt> method.
* This method calls <tt>LineLayout.layout()</tt> for each line to format.
*
* @author John Raley
*
* @see MFormatter
* @see LineLayout
* @see LayoutContext
* @see LayoutInfo
*/

final class AsyncFormatter extends MFormatter implements Runnable
{
    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    /**
    * Text to format.
    */
    private MConstText fText;

    /**
    * Default values.
    */
    private AttributeMap fDefaultValues;
    
    /**
    * Font resolver.
    */
    private FontResolver fFontResolver;
    
    /**
    * Default character metric - used to set height of empty paragraphs
    */
    private DefaultCharacterMetric fDefaultCharMetric;
    
    /**
    * Length to which lines are formatted.
    */
    private int fLineDim;

    /**
    * Table of formatted lines.
    */
    private LayoutInfo fLineTable[];

    /**
    * Size of line table.
    */
    private int fLTSize = 10; // initial size must be > 0

    /**
    * Index of last positive entry in line table.
    */
    private int fLTPosEnd;

    /**
    * Index of first negative entry in line table.
    */
    private int fLTNegStart;

    /**
    * Length of text on which negative line offsets are based.
    * @see #formatText
    */
    private int fLTCurTextLen;

    /**
    * Length of formatted text in fill direction, in pixels.
    */
    private int fPixHeight;

    /**
    * Length of formatted text including pseudoline.
    */
    private int fFullPixHeight;

    private int fMinX;
    private int fMaxX;

    /**
    * <tt>true</tt> if lines should be formatted to fit line dimension.
    */
    private boolean fWrap;

    /**
    * <tt>true</tt> if characters run horizontally.
    */
    private boolean fHLine = true;

    /**
     * <tt>true</tt> if characters run from from low to high coordinates on line.
     */
    private boolean fLineInc = true;

    /**
     * <tt>true</tt> if lines run from low to high coordinates within page.
     */
    private boolean fFillInc = true;

    /**
    * Value returned from <tt>findLineAt()</tt>
    * if pixel height precedes topmost line.
    */
    private static final int kBeforeFirstLine = -2;

    /**
    * Value returned from <tt>findLineAt()</tt> and <tt>getLineContaining()</tt>
    * if offset / pixel height is after all existing lines.
    */
    private static final int kAfterLastLine = -1;

    /**
    * Thread which invokes formatter in the background.
    */
    private Thread fDaemon;

    /**
    * FontRenderContext to measure with.  Currently not settable after
    * construction.
    */
    private FontRenderContext fFontRenderContext;

    /**
    * Controls whether background formatting can run.
    */
    private boolean fBgFormatAllowed = false;
    
    /**
    * Cached line break object.
    */
    private BreakIterator fLineBreak = null;
    
    /**
    * Cached LineBreakMeasurer.
    */
    private LineBreakMeasurer fCachedMeasurer = null;
    private int fCachedMeasurerStart;
    private int fCachedMeasurerLimit;
    
    // Some JDK's (Sun's 1.2.2) throw exceptions from 
    // LineBreakMeasurer.insertChar and deleteChar.  This class
    // detects this condition and doesn't use these two methods
    // if they throw exceptions.
    private static boolean fgCacheMeasurers = true;
    
    /**
    * Current text time stamp.  Used to maintain modification invariant.
    */
    private int fCurTimeStamp;
    
    /**
    * Cache of ParagraphRenderers.
    */
    private Hashtable fRendererCache = new Hashtable();
    
    /**
    * Draw text inside a rectangle. Does not cache any formatting information.
    * This is convenient for small amounts of text; comparable to TETextBox on the Mac.
    * <p>
    * @param text the text to draw
    * @param g Graphics on which to draw
    * @param drawRect rectangle in which text will be drawn
    * @param fillInc if true, lines run from low to high coordinates in page.
    * @param hLine if true, characters run horizontally within a line.
    */

    private static boolean isParagraphSeparator(char ch) {

        return ch == '\n' || ch == '\u2029';
    }

    /**
    * Create an <tt>AsyncFormatter</tt>.
    * @param text the text to format
    * @param lineBound length to which lines are foramtted
    * @param wrap <tt>true</tt> if text should be "line wrapped" (formatted to fit destination area)
    */
    AsyncFormatter(MConstText text, 
                   AttributeMap defaultValues,
                   int lineBound,
                   boolean wrap,
                   Graphics g)
    {
        fText = text;
        fDefaultValues = defaultValues;
        fFontResolver = new FontResolver(fDefaultValues);
        
        fLineDim = lineBound;
        fWrap = wrap;
        Graphics2D g2d = Graphics2DConversion.getGraphics2D(g);
        fFontRenderContext = g2d.getFontRenderContext();
        
        fDefaultCharMetric = new DefaultCharacterMetric(fFontResolver, 
                                                        fFontRenderContext);
        fLTCurTextLen = text.length();
        removeAllLines();

        fDaemon = new Thread(this);
        fDaemon.start();
    }
    
    public AttributeMap getDefaultValues() {
    
        return fDefaultValues;
    }
    
    public void checkTimeStamp() {
        String admonition = "Probably, you modified " +
                            "the text before calling stopBackgroundFormatting().";
                            
        if (fText.getTimeStamp() != fCurTimeStamp) {
            throw new Error("Time stamp is out of sync.  " + admonition);
        }
        if (fText.length() != fLTCurTextLen) {
            throw new Error("Length changed unexpectedly.  " +
                            "fText.length()="+fText.length()+";  "+
                            "fLTCurTextLen="+fLTCurTextLen+";  "+
                            "formatter="+this+";  "+
                            "text="+fText);
        }
    }

    /**
    * Specify whether to wrap lines using the line dimension.
    * @param wrap if <tt>true</tt> lines will be wrapped; otherwise new lines will only be
    * started when a newline is encountered.
    */
    public synchronized void setWrap(boolean wrap)
    {
        if (wrap != fWrap) {
            fWrap = wrap;
            removeAllLines();
        }
    }

    /**
    * Return true if lines are wrapped using the line dimension.
    * @see #setWrap
    */
    public synchronized boolean wrap()
    {
        return fWrap;
    }

    /**
    * Specify the lineBound in pixels.  If line wrapping is on, lines
    * will be wrapped to this value.
    * <p>
    *
    * @param lineBound the distance, in pixels, used to wrap lines.
    */
    public synchronized void setLineBound(int lineBound)
    {
        if (fLineDim != lineBound) {
            fLineDim = lineBound;                
            if (fWrap) {
                removeAllLines();
            }
        }
    }

    /**
    * Return the number of pixels along the line dimension.
    */
    public synchronized int lineBound()
    {
        return fLineDim;
    }

    /**
    * Return <tt>true</tt> if characters run from low to high coordinates on line.
    */
    private boolean lineInc()
    {
        return fLineInc;
    }

    /**
    * Return <tt>true</tt> if lines run from low to high coordinates on page.
    */
    private boolean fillInc()
    {
        return fFillInc;
    }

    /**
    * Remove all lines in the line table.  Used after an operation that
    * invalidates all existing lines, such as changing line wrapping or the
    * line dim.
    */
    private synchronized void removeAllLines()
    {
        fCurTimeStamp = fText.getTimeStamp();
        stopBackgroundFormatting();

        fMinX = 0;
        fMaxX = fLineDim;
        
        fLineTable = new LayoutInfo[fLTSize]; // fLTSize must be > 0
        fLTNegStart = fLTSize;
        fLTPosEnd = 0;

        fLineTable[0] = pseudoLineInfo(null, 0);

        fPixHeight = fLineTable[0].getHeight(); // ??? or should it be zero?
        fFullPixHeight = fPixHeight;

        // format at least one line:
        formatToHeight(fPixHeight + 1);

        enableBGFormat();
    }

    /**
     * Fill the layout info with information appropriate to the pseudoline.
     */
    private synchronized LayoutInfo pseudoLineInfo(LayoutInfo info, int offset)
    {
        AttributeMap st = fText.paragraphStyleAt(fLTCurTextLen); // ??? if text is empty or this is the end of the text, what happens?
        ParagraphRenderer renderer = getRendererFor(st);
        info = renderer.layout(fText,
                               info,
                               (LineBreakMeasurer)null,
                               fFontRenderContext,
                               offset,
                               offset,
                               fLineDim,
                               fLineDim);

        return info;
    }

    /**
    * Return the index of the last valid line in the line table.
    */
    private int lastLine()
    {
        return (fLTNegStart == fLTSize) ? fLTPosEnd : fLTSize - 1;
    }

    /**
    * Shift line table such that <tt>lastPos</tt> is the last positive
    * entry in the table. <b>NOTE: <tt>lastPos</tt> must be a valid line!</b>
    * <p>
    * @param lastPos the index of the line which will become the last positive
    * entry in the line table
    */
    private void shiftTableTo(int lastPos)
    {
        LayoutInfo li;

        while (lastPos < fLTPosEnd)    { // shift +'s to -'s
            li = fLineTable[fLTPosEnd];
            fLineTable[fLTPosEnd--] = null;

            li.makeRelativeToEnd(fLTCurTextLen, fPixHeight);

            fLineTable[--fLTNegStart] = li;
        }

        while (lastPos >= fLTNegStart) { // shift -'s to +'s
            li = fLineTable[fLTNegStart];
            fLineTable[fLTNegStart++] =    null;

            li.makeRelativeToBeginning(fLTCurTextLen, fPixHeight);

            fLineTable[++fLTPosEnd]    = li;
        }
    }

    /**
    * Increase the size of the line table.
    */
    private void expandLineTable()
    {
        // This just doubles the size of the line table.

        LayoutInfo newLineTable[] = new LayoutInfo[fLineTable.length * 2];
        int newNegStart = newLineTable.length - (fLineTable.length - fLTNegStart);

        System.arraycopy(fLineTable, 0, newLineTable, 0, fLTPosEnd + 1);
        System.arraycopy(fLineTable, fLTNegStart, newLineTable, newNegStart, fLTSize - fLTNegStart);

        fLTNegStart = newNegStart;
        fLTSize = newLineTable.length;
        fLineTable = newLineTable;
    }

    /**
    * Return the index of the line containing the pixel position <tt>fillCoord</tt>.
    * If fillCoord exceeds the bottom of the text, return kAfterLastLine.
    * If fillCoord is less than the top of the text, return kBeforeFirstLine.
    * <p>
    * @param fillCoord "height" of line to locate.
    */
    private int findLineAt(int fillCoord)
    {
        int low, high, mid;
        int lowStart, highStart, midStart;

        if (fillCoord >= fPixHeight)
            return kAfterLastLine;
        else if (fillCoord < 0)
            return kBeforeFirstLine;

        if ((fLTNegStart < fLTSize) && (fillCoord >= fLineTable[fLTNegStart].getGraphicStart(fPixHeight))) {
            fillCoord -= fPixHeight;

            low = fLTNegStart;
            high = fLTSize;
            highStart = 0;
        }
        else {
            low = 0;
            high = fLTPosEnd + 1;
            highStart = fLineTable[fLTPosEnd].getGraphicStart(0) + fLineTable[fLTPosEnd].getHeight();
        }
        lowStart = fLineTable[low].getGraphicStart(0);

        do {
            if (lowStart == highStart)
                return low;

            mid = low + (fillCoord - lowStart) / (highStart - lowStart) * (high - low);
            midStart = fLineTable[mid].getGraphicStart(0);

            if (midStart > fillCoord) {
                high = mid;
                highStart = fLineTable[high].getGraphicStart(0);
            }
            else if (midStart + fLineTable[mid].getHeight() <= fillCoord) {
                low = mid + 1;
                lowStart = fLineTable[low].getGraphicStart(0);
            }
            else
                return mid;

        } while (low < high);

        return 0;
    }

    /**
     * Return the index of the first character in the line.
     * @param line the internal index of the line (direct index into linetable).
     */
    private int lineCharStartInternal(int line)
    {
        return fLineTable[line].getCharStart(fLTCurTextLen);
    }

    /**
     * Return the index of the character following the last character in the line.
     * @param line the internal index of the line (direct index into linetable).
     */
    private int lineCharLimitInternal(int line)
    {
        return lineCharStartInternal(line) + fLineTable[line].getCharLength();
    }

    /**
     * Return the graphic start of the line, unadjusted for fill direction.
     * @param line the internal index of the line (direct index into linetable).
     */
    private int lineGraphicStartInternal(int line)
    {
        return fLineTable[line].getGraphicStart(fPixHeight);
    }

    /**
     * Return the graphic limit of the line, unadjusted for fill direction.
     * @param line the internal index of the line (direct index into linetable).
     */
    private int lineGraphicLimitInternal(int line)
    {
        return lineGraphicStartInternal(line) + fLineTable[line].getHeight();
    }

    /**
    * Return the offset of the first character which has not been formatted.
    * If all text has been formatted, return the current text length.
    */
    private int lastLineCharStop()
    {
        return lineCharLimitInternal(lastLine());
    }

    /**
     * Return a 'valid' line containing offset.  This differs from getLineContaining in
     * this maps kAfterLastLine to lastLine(), so that the result is always a valid
     * linetable index.
     */
    private int getValidLineContaining(TextOffset offset) {

        return getValidLineContaining(offset.fOffset, offset.fPlacement);
    }

    /**
     * Return a 'valid' line containing offset.  This differs from getLineContaining in
     * this maps kAfterLastLine to lastLine(), so that the result is always a valid
     * linetable index.
     */
    private int getValidLineContaining(int insOffset, boolean placement)
    {
        int line = getLineContaining(insOffset, placement);
        if (line == kAfterLastLine)
            line = lastLine();
        else if (line == kBeforeFirstLine)
            throw new IllegalArgumentException("Debug: getLineContaining returned kBeforeFirstLine");

        return line;
    }

    /**
    * Return index of line containing <tt>offset</tt>.
    * ??? If offset is after last formatted line, returns kAfterLastLine.  Is that good?
    * <p>
    * @param offset the offset whose line should be located
    * @returns line containing <tt>offset</tt>
    */
    private int getLineContaining(TextOffset offset) {

        return getLineContaining(offset.fOffset, offset.fPlacement);
    }

    private int getLineContaining(int insOffset, boolean placement)
    {
        int pos = insOffset;
        if (placement == TextOffset.BEFORE_OFFSET && pos > 0)
            --pos;

        if (pos < 0) {
            throw new IllegalArgumentException("Debug: getLineContaining offset < 0: " + pos);
        }

        if (pos >= lastLineCharStop()) {
            return emptyParagraphAtEndOfText()? kAfterLastLine : lastLine();
        }

        int low, high, mid;
        int lowStart, highStart, midStart;

        if ((fLTNegStart < fLTSize) && (pos >= fLineTable[fLTNegStart].getCharStart(fLTCurTextLen))) {
            pos -= fLTCurTextLen;

            low = fLTNegStart;
            high = fLTSize;
            highStart = 0;
        }
        else {
            low = 0;
            high = fLTPosEnd + 1;
            highStart = fLineTable[fLTPosEnd].getCharStart(0) + fLineTable[fLTPosEnd].getCharLength();
        }
        lowStart = fLineTable[low].getCharStart(0);

        do {
            if (highStart == lowStart) {
                return low;
            }

            mid = low + (pos - lowStart) / (highStart - lowStart) * (high - low);
            midStart = fLineTable[mid].getCharStart(0);

            if (midStart > pos) {
                high = mid;
                highStart = fLineTable[high].getCharStart(0);
            }
            else if (midStart + fLineTable[mid].getCharLength() <= pos) {
                low = mid + 1;
                lowStart = fLineTable[low].getCharStart(0);
            }
            else {
                return mid;
            }

        } while (low < high);

        return 0;
    }

    /**
    * Display text in drawArea. Does not reformat text.
    * <p>
    * @param g the Graphics object in which to draw
    * @param drawArea the rectangle, in g's coordinate system, in which to draw
    * @param origin the top-left corner of the text, in g's coordinate system
    */
    public synchronized void draw(Graphics g, Rectangle drawArea, Point origin)
    {
        draw(g, drawArea, origin, null, null, null);
    }

    public synchronized void draw(Graphics g, Rectangle drawArea, Point origin,
                TextOffset selStart, TextOffset selStop, Color highlight) {

        checkTimeStamp();
        Graphics2D g2d = Graphics2DConversion.getGraphics2D(g);

        // Get starting and ending fill 'heights.'

        int startFill;
        int endFill;

        if (fFillInc)
            startFill = drawArea.y - origin.y;
        else
            startFill = origin.y - (drawArea.y + drawArea.height);

        endFill = startFill + drawArea.height;

        // We're drawing one more line than necessary when we update because of a
        // selection change.  But we're drawing the right amount of lines when we
        // refresh the whole display.  This affects rendering speed significantly,
        // and creating a new paragraph renderer for each line doesn't help either.
        // For now, I'm going to subtract one from the fill height, on the theory
        // that we're picking up the extra line because of a one-pixel slop.
        // This seems to work, although perhaps if one pixel of a line at the
        // bottom should draw, it won't.

        --endFill;

        // Format to ending fill height, so line table is valid for all lines we need to draw.

        formatToHeight(endFill);

        // Get starting and ending lines for fill height.  If the start of the fill is after the last line,
        // or the end of the fill is before the first line, return.

        int curLine = findLineAt(startFill);
        if (curLine == kAfterLastLine)
            return;
        else if (curLine == kBeforeFirstLine)
            curLine = 0;

        int lastLine = findLineAt(endFill);
        if (lastLine == kBeforeFirstLine)
            return;
        else if (lastLine == kAfterLastLine)
            lastLine = lastLine();

        // Get the base coordinates (lineX, lineY) for the starting line.

        int lineX, lineY;

        int gStart = lineGraphicStartInternal(curLine);

        if (fHLine) {
            if (fLineInc)
                lineX = origin.x;
            else
                lineX = origin.x - fLineDim;
            if (fFillInc)
                lineY = origin.y + gStart;
            else
                lineY = origin.y - (gStart + fLineTable[curLine].getHeight());
        }
        else {
            if (fLineInc)
                lineY = origin.y;
            else
                lineY = origin.y - fLineDim;
            if (fFillInc)
                lineX = origin.x + gStart;
            else
                lineX = origin.x - (gStart + fLineTable[curLine].getHeight());
        }

        // Iterate through lines, drawing each one and incrementing the base coordinate by the line height.


        for (; curLine <= lastLine; curLine++) {
            // Adjust curLine around gap in line table.
            if ((curLine > fLTPosEnd) && (curLine < fLTNegStart))
                curLine = fLTNegStart;

            fLineTable[curLine].renderWithHighlight(fLTCurTextLen, g2d, fLineDim, lineX, lineY, selStart, selStop, highlight);

            // Increment line base for next iteration.
            int lineInc = fLineTable[curLine].getHeight();
            if (fFillInc) {
                if (fHLine)
                    lineY += lineInc;
                else
                    lineX += lineInc;
            } else {
                if (fHLine)
                    lineY -= lineInc;
                else
                    lineX -= lineInc;
            }
        }
    }

    /**
    * Format text to given height.
    * @param height the height to which text will be formatted.
    */
    public synchronized void formatToHeight(int reqHeight)
    {
        checkTimeStamp();
        if (reqHeight <= fPixHeight) // already formatted to this height
            return;

        if (fText.length() == lastLineCharStop()) // already formatted all the text
            return;

        // +++ should disable update thread here

        if (fLTNegStart < fLTSize)
            shiftTableTo(fLTSize - 1);

        formatText(0, 0, reqHeight, false);
    }

    /**
     * Format text to given offset.
     * @param offset the offset to which text will be formatted.
     */
     private void formatToOffset(TextOffset offset)
     {
        formatToOffset(offset.fOffset, offset.fPlacement);
     }
     
     private synchronized void formatToOffset(int offset, boolean placement) {
        
        checkTimeStamp();
        int llcs = lastLineCharStop();
        if (llcs < fLTCurTextLen) {
            int limit = offset;
            if (placement == TextOffset.AFTER_OFFSET) // format to past character offset is associated with
                limit++;
            if (limit >= llcs) { // ??? would '>' be ok instead or '>='?
                if (limit > fLTCurTextLen)
                    limit = fLTCurTextLen;

                shiftTableTo(lastLine());
                formatText(llcs, limit - llcs, Integer.MAX_VALUE, true);
            }
        }
     }

/**
* Reformat text after a change.
* After the formatter's text changes, call this method to reformat.  Does
* not redraw.
* @param afStart the offset into the text where modification began;  ie, the
* first character in the text which is "different" in some way.  Does not
* have to be nonnegative.
* @param afLength the number of new or changed characters in the text.  Should never
* be less than 0.
* @param viewRect the Rectangle in which the text will be displayed.  This is needed for
* returning the "damaged" area - the area of the screen in which the text must be redrawn.
* @param origin the top-left corner of the text, in the display's coordinate system
* @returns a <tt>Rectangle</tt> which specifies the area in which text must be
* redrawn to reflect the change to the text.
*/
    public Rectangle updateFormat(final int afStart,
                                               final int afLength,
                                               Rectangle viewRect,
                                               Point origin)
    {
        if (afStart < 0) {
            throw new IllegalArgumentException("Debug: updateFormat afStart < 0: " + afStart);
        }
        if (fBgFormatAllowed) {
            throw new IllegalArgumentException("Background formatting should have been disabled");
        }
        fCurTimeStamp = fText.getTimeStamp();
        
        int curLine = getValidLineContaining(afStart, TextOffset.AFTER_OFFSET);
        int lineStartPos = lineCharStartInternal(curLine);

        // optimize by finding out whether change occurred
        // after first word break on curline

        int firstPossibleBreak;

        if (lineStartPos < fText.length()) {

            if (fLineBreak == null) {
                fLineBreak = BreakIterator.getLineInstance();
            }
            CharacterIterator charIter = fText.createCharacterIterator();
            charIter.setIndex(lineStartPos);
            fLineBreak.setText(charIter);

            firstPossibleBreak = fLineBreak.following(lineStartPos);
        }
        else
            firstPossibleBreak = afStart;

        if ((curLine > 0) && (firstPossibleBreak == BreakIterator.DONE || afStart <= firstPossibleBreak)) {
            curLine--;
            if (curLine < fLTNegStart && curLine > fLTPosEnd)
                curLine = fLTPosEnd;
        }

        shiftTableTo(curLine);

        int pixHeight; // after the formatText call, at least pixHeight text must be formatted

        if (fHLine) {
            if (fFillInc)
                pixHeight = viewRect.y + viewRect.height - origin.y;
            else
                pixHeight = origin.y - viewRect.y;
        }
        else {
            if (fFillInc)
                pixHeight = viewRect.x + viewRect.width - origin.x;
            else
                pixHeight = origin.x - viewRect.x;
        }

        Rectangle r = formatText(afStart, afLength, pixHeight, false);

        //dumpLineTable();

        if ((fPixHeight < pixHeight) && (fLTNegStart < fLTSize) && (fLTCurTextLen > lastLineCharStop())) {
            shiftTableTo(lastLine());
            Rectangle s = formatText(0, 0, pixHeight, false);
            r = r.union(s);
        }

        intlRect(origin, r);
        //System.out.println("Damaged rect: "+r+"; origin: "+origin);

        // don't need to synchronized here, b/c the daemon shouldn't be running when
        // this is executing

        if (fText.length() < lastLineCharStop())
            enableBGFormat();
        else
            stopBackgroundFormatting();

        //dumpLineTable();

        return r;
    }
    
    private LineBreakMeasurer makeMeasurer(int paragraphStart, int paragraphLimit) {

        MTextIterator iter = new MTextIterator(fText,
                                               fFontResolver,
                                               paragraphStart,
                                               paragraphLimit);
        LineBreakMeasurer measurer = new LineBreakMeasurer(iter, fFontRenderContext);
        if (fgCacheMeasurers) {
            fCachedMeasurerStart = paragraphStart;
            fCachedMeasurerLimit = paragraphLimit;
            fCachedMeasurer = measurer;
        }
        return measurer;
    }        

/**
* Compute text format.  This method calculates text format;  it can be
* called for various purposes:  to reformat text after an edit, to
* format text to a particular height, or to format text up to a
* particular offset.
* <p>
* The calling method must ensure that <tt>fLineTable</tt> has been shifted
* such that the last positive line is where the formatting operation will
* begin.
* <p>
* Called by: <tt>formatToHeight()</tt>, <tt>updateFormat()</tt>,
* <tt>textOffsetToPoint()</tt>, <tt>getBoundingRect()</tt>
* @param afStart the offset of the first character in the text which has changed
* @param afLength the number of new or changed characters in the text
* @param reqHeight the pixel height to which text must be formatted.  Ignored
* if <tt>formatAllNewText</tt> is <tt>true</tt>, or if old lines remain in the
* line table after all changed text has been formatted.
* @param seekOffsetAtEnd if <tt>true</tt>, formatting continues until the line
* containing afStart+afLength has been formatted.  If false, formatting may stop
* when reqHeight has been reached.  This parameter should be <tt>true</tt> <b>only</b>
* if the object of the formatting operation is to extend formatting to a particular
* offset within the text;  it should be <tt>false</tt> everywhere else.
* @returns a rectangle, relative to the top-left of the text, which encloses the
* screen area whose appearance has changed due to the reformatting.
*/

    private Rectangle formatText(int afStart, final int afLength, int reqHeight, boolean seekOffsetAtEnd)
    {
    /* assumes line table shifted such that first line to format is
    last positive line */

        if (afLength < 0) {
            throw new IllegalArgumentException("afLength < 0.  afLength=" + afLength);
        }

        int newTextEnd = afStart + afLength;

        final int newCurTextLen = fText.length();

        // variable not used int oldPixHeight = fPixHeight;
        int oldFullPixHeight = fFullPixHeight;
        fPixHeight -= fLineTable[fLTPosEnd].getHeight();

        int curGraphicStart = fLineTable[fLTPosEnd].getGraphicStart(fPixHeight);
        int curLineStart = fLineTable[fLTPosEnd].getCharStart(newCurTextLen);
        
        int curParagraphStart = fText.paragraphStart(curLineStart);
        int curParagraphLimit = Integer.MIN_VALUE; // dummy value

        int damageStart = curGraphicStart;

        ParagraphRenderer renderer = null;
        LineBreakMeasurer measurer = null;
        
        // try to use cached LineBreakMeasurer if possible
        if (fCachedMeasurer != null && 
            curParagraphStart == fCachedMeasurerStart) {

            curParagraphLimit = fText.paragraphLimit(curParagraphStart);
            
            try {
                if (newCurTextLen - fLTCurTextLen == 1 && afLength == 1) {
                    if (curParagraphLimit == fCachedMeasurerLimit+1) {
                        MTextIterator iter = new MTextIterator(fText,
                                                               fFontResolver, 
                                                               curParagraphStart,
                                                               curParagraphLimit);
                        fCachedMeasurer.insertChar(iter, afStart);
                        fCachedMeasurerLimit += 1;
                        measurer = fCachedMeasurer;
                    }
                }
                else if (fLTCurTextLen - newCurTextLen == 1 && afLength == 0) {
                    if (fCachedMeasurerLimit > fCachedMeasurerStart + 1 &&
                                curParagraphLimit == fCachedMeasurerLimit-1) {
                        MTextIterator iter = new MTextIterator(fText,
                                                               fFontResolver,
                                                               curParagraphStart,
                                                               curParagraphLimit);
                        fCachedMeasurer.deleteChar(iter, afStart);
                        fCachedMeasurerLimit -= 1;
                        measurer = fCachedMeasurer;
                    }
                }
            }
            catch(ArrayIndexOutOfBoundsException e) {
                fCachedMeasurer = null;
                fgCacheMeasurers = false;
            }
            
            if (measurer != null) {
                // need to set up renderer since the paragraph update in the
                // formatting loop will not happen
                AttributeMap style = fText.paragraphStyleAt(curParagraphStart);
                renderer = getRendererFor(style);
                measurer.setPosition(curLineStart);
            }
        }
        
        if (measurer == null) {
            // trigger paragraph update at start of formatting loop
            curParagraphLimit = curParagraphStart;
            curParagraphStart = 0;
        }

        fLTCurTextLen = newCurTextLen;
        
        while (true) {
            // System.out.println("line: " + fLTPosEnd + ", cls: " + curLineStart);


            if (curLineStart >= curParagraphLimit) {
                curParagraphStart = curParagraphLimit;
                curParagraphLimit = fText.paragraphLimit(curParagraphStart);

                AttributeMap style = fText.paragraphStyleAt(curParagraphStart);
                renderer = getRendererFor(style);

                if (curParagraphStart < curParagraphLimit) {
                    measurer = makeMeasurer(curParagraphStart, curParagraphLimit);
                    measurer.setPosition(curLineStart);
                }
                else {
                    measurer = null;
                }
            }

            {
                boolean haveOldDirection = fLineTable[fLTPosEnd] != null;
                boolean oldDirection = false; //  dummy value for compiler
                if (haveOldDirection) {
                    oldDirection = fLineTable[fLTPosEnd].isLeftToRight();
                }

                fLineTable[fLTPosEnd] = renderer.layout(fText,
                                                        fLineTable[fLTPosEnd],
                                                        measurer,
                                                        fFontRenderContext,
                                                        curParagraphStart,
                                                        curParagraphLimit,
                                                        fWrap ? fLineDim : Integer.MAX_VALUE,
                                                        fLineDim);
                if (haveOldDirection) {
                    if (fLineTable[fLTPosEnd].isLeftToRight() != oldDirection) {
                        newTextEnd = Math.max(newTextEnd, curParagraphLimit);
                    }
                }
            }

            {
                LayoutInfo theLine = fLineTable[fLTPosEnd];

                theLine.setGraphicStart(curGraphicStart);
                curGraphicStart += theLine.getHeight();

                fPixHeight += theLine.getHeight();
                curLineStart += theLine.getCharLength();

                if (!fWrap) {
                    int lineWidth = theLine.getTotalAdvance() + theLine.getLeadingMargin();
                    if (theLine.isLeftToRight()) {
                        if (fMaxX < lineWidth) {
                            fMaxX = lineWidth;
                        }
                    }
                    else {
                        if (fLineDim-lineWidth < fMinX) {
                            fMinX = fLineDim-lineWidth;
                        }
                    }
                }
            }
            /*
                Next, discard obsolete lines.  A line is obsolete if it
                contains new text or text which has been formatted.
            */

            while (fLTNegStart < fLTSize) {
                int linePos = fLineTable[fLTNegStart].getCharStart(newCurTextLen);
                if (linePos >= curLineStart && linePos >= newTextEnd)
                    break;

                // System.out.println("delete neg line: " + fLTNegStart);
                fPixHeight -= fLineTable[fLTNegStart].getHeight();
                fLineTable[fLTNegStart++] = null;
            }

            int stopAt;
            if (fLTNegStart < fLTSize)
                stopAt = fLineTable[fLTNegStart].getCharStart(newCurTextLen);
            else
                stopAt = newCurTextLen;

            /*
                Now, if exit conditions aren't met, create a new line.
            */

            if (seekOffsetAtEnd) {
                if ((curLineStart >= newTextEnd) && (fLTNegStart == fLTSize)) {
                    // System.out.println("break 1");
                    break;
                }
            }
            else {
                if (curLineStart >= stopAt) {
                    // System.out.println("curLineStart: " + curLineStart + " >= stopAt: " + stopAt);
                    break;
                }
                else if (fLTNegStart==fLTSize && fPixHeight >= reqHeight) {
                    // System.out.println("break 3");
                    break;
                }
            }

            if (fLTPosEnd + 1 == fLTNegStart)
                expandLineTable();

            fLineTable[++fLTPosEnd] = null; // will be created by Renderer
        }
        //System.out.print("\n");

        if (newCurTextLen == 0) {
            fLineTable[0] = pseudoLineInfo(fLineTable[0], 0);
            fPixHeight = fLineTable[0].getHeight();
        }
        fFullPixHeight = fPixHeight;

        if (isParaBreakBefore(newCurTextLen)) {
            fFullPixHeight += lastCharHeight();
        }
/*
        System.out.println("curLineStart: " + curLineStart +
            ", fLTPosEnd: " + fLTPosEnd +
            ", fLTNegStart: " + fLTNegStart +
            ", fLTSize: " + fLTSize);

        System.out.println("oldFullPixHeight: " + oldFullPixHeight + ", newFullPixHeight: " + fFullPixHeight);
*/
        int damageLength;
        if (fFullPixHeight == oldFullPixHeight) {
            damageLength = fLineTable[fLTPosEnd].getGraphicStart(fPixHeight)
                            + fLineTable[fLTPosEnd].getHeight() - damageStart;
        }
        else {
            damageLength = Math.max(fFullPixHeight, oldFullPixHeight);
        }

        return new Rectangle(fMinX, damageStart, fMaxX-fMinX, damageLength);
    }

    private void dumpLineTable()
    {
        int i;

        System.out.println("fLTCurTextLen=" + fLTCurTextLen + " " );
        for (i=0; i<= fLTPosEnd; i++)
            System.out.println("Line " + i + " starts at "
                                + fLineTable[i].getCharStart(fLTCurTextLen)
                                + " and extends " + fLineTable[i].getCharLength());

        for (i=fLTNegStart; i< fLTSize; i++)
            System.out.println("Line " + (i-fLTNegStart+fLTPosEnd+1) + " starts at "
                                + fLineTable[i].getCharStart(fLTCurTextLen)
                                + " and extends " + fLineTable[i].getCharLength());
    }

    public synchronized int minX() {
    
        return fMinX;
    }
    
    /**
    * Return the horizontal extent of the text, in pixels.
    * <p>
    * This returns an approximation based on the currently formatted text.
    */
    public synchronized int maxX()
    {
        checkTimeStamp();

        return fMaxX;
    }

    /**
    * Return the height of the last character in the text.
    *
    * This is used for the 'extra height' needed to display a caret at the end of the text when the
    * text is empty or ends with a newline.
    */
    private int lastCharHeight()
    {
        int charIndex = lastLineCharStop() - 1;
        AttributeMap st = fText.characterStyleAt(charIndex);
        DefaultCharacterMetric.Metric metric = fDefaultCharMetric.getMetricForStyle(st);

        int height = metric.getAscent();
        height += metric.getDescent();
        height += metric.getLeading();

        return height;
    }

    /**
     * Return true if the character at pos is a paragraph separator.
     */
    private boolean isParaBreakBefore(int pos)
    {
        return pos > 0 && (fText.at(pos - 1) == '\u2029' || fText.at(pos - 1) == '\n');
        // we really need to take look at this and determine what this function
        // should be doing.  What I've got here right now is a temporary implementation.
    }

    public synchronized int minY() {
    
        return 0;
    }
    
    /**
    * Return the vertical extent of the text, in pixels.
    * <p>
    * This returns an approximation based on the currently formatted text.
    */
    public synchronized int maxY()
    {
        checkTimeStamp();

        int numChars = lastLineCharStop();

        int pixHeight = fPixHeight;
        if (numChars == fLTCurTextLen && isParaBreakBefore(fLTCurTextLen)) {
            pixHeight += lastCharHeight();
        }

        if (numChars != 0)
            return pixHeight * fText.length() / numChars;
        else
            return 0;
    }

    /**
    * Return the actual pixel length of the text which has been formatted.
    */
    public synchronized int formattedHeight()
    {
        checkTimeStamp();
        return fPixHeight;
    }

    /**
     * There are two modes for dealing with carriage returns at the end of a line.  In the 'infinite width'
     * mode, the last character is considered to have infinite width.  Thus if the point is past the 'real'
     * end of the line, the offset is the position before that last character, and the offset is associated
     * with that character (placement after). In the 'actual width' mode, the offset is positioned after
     * that character, but still associated with it (placement before).
     */

    private TextOffset lineDimToOffset(TextOffset result, int line, int lineX, int lineY, TextOffset anchor, boolean infiniteMode)
    {
    // temporarily adjust line info to remove the negative char starts used in the line table.
    // then call through to the paragraph renderer to get the offset.  Don't put line end
    // optimization here, let the renderer do it (perhaps it does fancy stuff with the margins).

        LayoutInfo lineInfo = fLineTable[line];

        result = lineInfo.pixelToOffset(fLTCurTextLen, result, fLineDim, lineX, lineY);

        if (infiniteMode &&
            (result.fOffset > lineInfo.getCharStart(fLTCurTextLen)) &&
            isParaBreakBefore(result.fOffset) &&
            (anchor == null || anchor.fOffset == result.fOffset - 1)) {

            result.setOffset(result.fOffset - 1, TextOffset.AFTER_OFFSET);
        }

        return result;
    }

    /**
    * Given a screen location p, return the offset of the character in the text nearest to p.
    */
    public synchronized TextOffset pointToTextOffset(TextOffset result, int px, int py, Point origin, TextOffset anchor, boolean infiniteMode)
    {
        checkTimeStamp();
        if (result == null)
            result = new TextOffset();

        int fillD;

        if (fHLine)
            fillD = py - origin.y;
        else
            fillD = px - origin.x;

        if (!fFillInc)
            fillD = -fillD;

        if (fillD < 0) {
            result.setOffset(0, TextOffset.AFTER_OFFSET);
            return result;
        }

        formatToHeight(fillD);

        if (fillD >= fPixHeight) {
            boolean bias = fLTCurTextLen==0? TextOffset.AFTER_OFFSET : TextOffset.BEFORE_OFFSET;
            result.setOffset(fLTCurTextLen, bias);
            return result;
        }

        int line = findLineAt(fillD); // always a valid line
        int gStart = lineGraphicStartInternal(line);

        int lineX, lineY;  // upper-left corner of line
        if (fHLine) {
            lineX = origin.x;
            lineY = fFillInc? origin.y + gStart : origin.y - (gStart + fLineTable[line].getHeight());
        }
        else {
            lineY = origin.y;
            lineX = fFillInc? origin.x + gStart : origin.x - (gStart + fLineTable[line].getHeight());
        }

        return lineDimToOffset(result, line, px - lineX, py - lineY, anchor, infiniteMode);
    }

    private boolean emptyParagraphAtEndOfText() {

        return fLTCurTextLen > 0 &&
                isParagraphSeparator(fText.at(fLTCurTextLen-1));
    }

    /**
     * Return true if the offset designates a point on the pseudoline following a paragraph
     * separator at the end of text.  This is true if the offset is the end of text
     * and the last character in the text is a paragraph separator.
     */
    private boolean afterLastParagraph(TextOffset offset)
    {
        return offset.fOffset == fLTCurTextLen &&
            emptyParagraphAtEndOfText();
    }

    /**
    * Given an offset, return the Rectangle bounding the caret at the offset.
    * @param offset an offset into the text
    * @param origin the top-left corner of the text, in the display's coordinate system
    * @return a Rectangle bounding the caret.
    */
    public synchronized Rectangle getCaretRect(TextOffset offset, Point origin) {

        Rectangle r = new Rectangle();
        getCaretRect(r, offset, origin);
        return r;
    }
    
    private void getCaretRect(Rectangle r, TextOffset offset, Point origin) {
        
        checkTimeStamp();
        formatToOffset(offset);

        if (afterLastParagraph(offset)) {
            int pseudoLineHeight = lastCharHeight();
            if (fHLine) {
                int lineY = fFillInc ? origin.y + fPixHeight : origin.y - fPixHeight - pseudoLineHeight;
                r.setBounds(origin.x, lineY, 0, pseudoLineHeight);
            }
            else {
                int lineX = fFillInc? origin.x + fPixHeight : origin.x - fPixHeight - pseudoLineHeight;
                r.setBounds(lineX, origin.y, pseudoLineHeight, 0);
            }
            return;
        }

        int line = getValidLineContaining(offset);

        int gStart = lineGraphicStartInternal(line);

        int lineX, lineY;

        if (fHLine) {
            lineX = origin.x;
            if (fFillInc)
                lineY = origin.y + gStart;
            else
                lineY = origin.y - (gStart + fLineTable[line].getHeight());
        }
        else {
            lineY = origin.y;
            if (fFillInc)
                lineX = origin.x + gStart;
            else
                lineX = origin.x - (gStart + fLineTable[line].getHeight());
        }

        Rectangle bounds = fLineTable[line].caretBounds(fText, fLTCurTextLen, fLineDim, offset.fOffset, lineX, lineY);

        r.setBounds(bounds);
    }

    /**
    * Draw the caret(s) associated with the given offset into the given Graphics.
    * @param g the Graphics to draw into
    * @param offset the offset in the text for which the caret is drawn
    * @param origin the top-left corner of the text, in the display's coordinate system
    * @param strongCaretColor the color of the strong caret
    * @param weakCaretColor the color of the weak caret (if any)
    */
    public synchronized void drawCaret(Graphics g,
                                       TextOffset offset,
                                       Point origin,
                                       Color strongCaretColor,
                                       Color weakCaretColor) {

        checkTimeStamp();
        Graphics2D g2d = Graphics2DConversion.getGraphics2D(g);
        formatToOffset(offset);

        LayoutInfo line;
        int gStart;
        
        if (afterLastParagraph(offset)) {
            gStart = fPixHeight;
            line = pseudoLineInfo(null, offset.fOffset);
        }
        else {
            int lineIndex = getValidLineContaining(offset);
            gStart = lineGraphicStartInternal(lineIndex);
            line = fLineTable[lineIndex];
        }
        
        int lineX, lineY;

        if (fHLine) {
            lineX = origin.x;
            if (fFillInc)
                lineY = origin.y + gStart;
            else
                lineY = origin.y - (gStart + line.getHeight());
        }
        else {
            lineY = origin.y;
            if (fFillInc)
                lineX = origin.x + gStart;
            else
                lineX = origin.x - (gStart + line.getHeight());
        }

        line.renderCaret(fText, fLTCurTextLen, g2d, fLineDim, lineX, lineY,
                                offset.fOffset, strongCaretColor, weakCaretColor);
    }

/**
* Given two offsets in the text, return a rectangle which encloses the lines containing the offsets.
* Offsets do not need to be ordered or nonnegative.
* @param offset1,offset2 offsets into the text
* @param origin the top-left corner of the text, in the display's coordinate system
* @returns a <tt>Rectangle</tt>, relative to <tt>origin</tt>, which encloses the lines containing the offsets
*/
    public synchronized Rectangle getBoundingRect(TextOffset offset1,
                                                  TextOffset offset2,
                                                  Point origin,
                                                  boolean tight) {

        Rectangle r = new Rectangle();
        getBoundingRect(r, offset1, offset2, origin, tight);
        return r;
    }

/*
    Transform r from "text" coordinates to "screen" coordinates.
*/

    private void intlRect(Point origin, Rectangle r) {

        int lineOrig, fillOrig;

        if (fHLine) {
            lineOrig = origin.x;
            fillOrig = origin.y;
        }
        else {
            lineOrig = origin.y;
            fillOrig = origin.x;
        }

        if (fLineInc)
            r.x += lineOrig;
        else
            r.x = lineOrig - (r.x + r.width);

        if (fFillInc)
            r.y += fillOrig;
        else
            r.y = fillOrig - (r.y + r.height);


        if (!fHLine) {
            int t = r.x;
            r.x = r.y;
            r.y = t;
            t = r.width;
            r.width = r.height;
            r.height = t;
        }
    }


    public synchronized void getBoundingRect(Rectangle r, 
                                             TextOffset offset1,
                                             TextOffset offset2,
                                             Point origin,
                                             boolean tight)
    {
        checkTimeStamp();
        if (offset1.equals(offset2)) {
            getCaretRect(r, offset1, origin);
            return;
        }
        if (offset1.greaterThan(offset2)) {
            TextOffset t; t = offset1; offset1 = offset2; offset2 = t;
        }

        formatToOffset(offset2);

        int line = getValidLineContaining(offset1);
        r.y = lineGraphicStartInternal(line);
        
        int gLimit;
        boolean sameLine = false;
                
        if (afterLastParagraph(offset2))
            gLimit = fPixHeight + lastCharHeight();
        else {
            int line2 = getValidLineContaining(offset2);
            gLimit = lineGraphicLimitInternal(line2);
            sameLine = (line == line2);
        }

        r.height = gLimit - r.y;

        if (sameLine && tight==TIGHT) {
            Rectangle rt = new Rectangle();
            getCaretRect(rt, offset1, origin);
            r.setBounds(rt);
            if (!offset1.equals(offset2)) {
                getCaretRect(rt, offset2, origin);
                r.add(rt);
            }
        }
        else {
            r.x = fMinX;
            r.width = fMaxX - fMinX;
            intlRect(origin, r);
        }

        // System.out.print("gbr: " + r.x + ", " + r.y + ", " + r.width + ", " + r.height);

        // System.out.println(" --> " + r.x + ", " + r.y + ", " + r.width + ", " + r.height);
    }

    /**
    * Compute the offset resulting from moving from a previous offset in direction dir.
    * For arrow keys.
    * @param result the offset to modify and return.  may be null, if so a new offset is allocated, modified, and returned.
    * @param previousOffset the insertion offset prior to the arrow key press.
    * @param direction the direction of the arrow key (eUp, eDown, eLeft, or eRight)
    * @returns new offset based on direction and previous offset.
    */
    public synchronized TextOffset findInsertionOffset(TextOffset result, TextOffset prevOffset, short dir)
    {
        return findNewInsertionOffset(result, prevOffset, prevOffset, dir);
    }

    /**
     * Transform key direction:  after this step, "left" means previous glyph, "right" means next glyph,
     *"up" means previous line, "down" means next line
     */
    private short remapArrowKey(short dir) {

        if (!fLineInc) {
            if (dir == eLeft)
                dir = eRight;
            else if (dir == eRight)
                dir = eLeft;
        }

        if (!fFillInc) {
            if (dir == eUp)
                dir = eDown;
            else if (dir == eDown)
                dir = eUp;
        }

        if (!fHLine) {
            if (dir == eLeft)
                dir = eUp;
            else if (dir == eRight)
                dir = eDown;
            else if (dir == eUp)
                dir = eLeft;
            else if (dir == eDown)
                dir = eRight;
        }

        return dir;
    }

    /**
    * Compute the offset resulting from moving from a previous offset, starting at an original offset, in direction dir.
    * For arrow keys.  Use this for "smart" up/down keys.
    * @param result the offset to modify and return.  May be null, if so a new offset is allocated, modified, and returned.
    * @param origOffset the offset at which an up-down arrow key sequence began.
    * @param prevOffset the insertion offset prior to the arrow key press
    * @param dir the direction of the arrow key (eUp, eDown, eLeft, or eRight)
    * @returns new offset based on direction, original offset, and previous offset.
    */
    public synchronized TextOffset findNewInsertionOffset(TextOffset result, TextOffset origOffset, TextOffset prevOffset, short dir)
    {
        checkTimeStamp();
        if (result == null)
            result = new TextOffset();

        dir = remapArrowKey(dir);

        // assume that text at origOffset and prevOffset has already been formatted

        if (dir == eLeft || dir == eRight) {
            formatToOffset(prevOffset);
            int line = getValidLineContaining(prevOffset);

            result.fPlacement = TextOffset.AFTER_OFFSET;
            result.fOffset = fLineTable[line].getNextOffset(fLTCurTextLen, prevOffset.fOffset, dir);
            if (result.fOffset < 0) {
                result.fOffset = 0;
            }
            else if (result.fOffset >= fLTCurTextLen) {
                result.setOffset(fLTCurTextLen, TextOffset.BEFORE_OFFSET);
            }
        }
        else {
            int distOnLine;

            if (afterLastParagraph(origOffset))
                distOnLine = 0;
            else {
                int line = getValidLineContaining(origOffset);

                distOnLine = fLineTable[line].strongCaretBaselinePosition(fLTCurTextLen, fLineDim, origOffset.fOffset);
            }

            // get prevOffset's line
            int line;
            if (afterLastParagraph(prevOffset))
                line = lastLine() + 1;
            else {
                line = getLineContaining(prevOffset);

                if (dir == eDown && (line == kAfterLastLine || line == lastLine())
                        && (lastLineCharStop() < fText.length())) {
                    shiftTableTo(lastLine());
                    formatText(lastLineCharStop(), 1, Integer.MAX_VALUE, true);
                    line = getLineContaining(prevOffset);
                }

                if (line == kBeforeFirstLine)
                    line = 0;
                else if (line == kAfterLastLine)
                    line = lastLine();
            }

            if (dir == eUp)
                line--;
            else if (dir == eDown)
                line++;
            else
                throw new IllegalArgumentException("Debug: Illegal direction parameter in findNewInsertionOffset");

            if (line < 0) {
                //result.setOffset(0, TextOffset.AFTER_OFFSET);
                result.assign(prevOffset);
            }
            else if (line > lastLine()) {
                result.setOffset(fLTCurTextLen, TextOffset.BEFORE_OFFSET);
            }
            else {
                if (fLineTable[line] == null)
                    line = (dir == eUp)? fLTPosEnd : fLTNegStart;

                // anchor is null since we never want a position after newline.  If we used the real anchor,
                // we might not ignore the newline even though infiniteMode is true.
                lineDimToOffset(result, line, distOnLine, 0, null, true);
            }
        }

        // System.out.println("fnio prev: " + prevOffset + ", new: " + result);

        return result;
    }

    public synchronized void stopBackgroundFormatting()
    {
        checkTimeStamp();
        fBgFormatAllowed = false;
    }

    private synchronized void enableBGFormat()
    {
        try {
            fBgFormatAllowed = true;
            notify();
        }
        catch (IllegalMonitorStateException e) {
        }
    }

    private int lineIndexToNumber(int lineIndex) {

        if (lineIndex <= fLTPosEnd) {
            return lineIndex;
        }
        else {
            return lineIndex - (fLTNegStart-fLTPosEnd-1);
        }
    }

    private int lineNumberToIndex(int lineNumber) {

        if (lineNumber <= fLTPosEnd) {
            return lineNumber;
        }
        else {
            return lineNumber + (fLTNegStart-fLTPosEnd-1);
        }
    }

    private void formatToLineNumber(int lineNumber) {

        while (lastLineCharStop() < fLTCurTextLen &&
                lineNumber >= lineIndexToNumber(fLTSize)) {
            // could be smarter and choose larger amounts for
            // larger lines, but probably not worth the effort
            formatToHeight(fPixHeight + kPixIncrement);
        }
    }

    private static final boolean STRICT = true;
    private static final boolean LENIENT = false;

    /**
     * Insure that at least lineNumber lines exist, doing
     * extra formatting if necessary.
     * Throws exception if lineNumber is not valid.
     * @param strict if STRICT, only lines [0...maxLineNumber()]
     *        are permitted.  If LENIENT, maxLineNumber()+1 is
     *        the greatest valid value.
     */
    private void validateLineNumber(int lineNumber, boolean strict) {

        formatToLineNumber(lineNumber);

        int maxNumber = lineIndexToNumber(fLTSize);
        if (strict == STRICT) {
            maxNumber -= 1;
        }

        if (lineNumber > maxNumber+1 ||
                (lineNumber == maxNumber+1 && !emptyParagraphAtEndOfText())) {
            throw new IllegalArgumentException("Invalid line number: " + lineNumber);
        }
    }

    public synchronized int getLineCount() {

        // format all text:
        formatToHeight(Integer.MAX_VALUE);

        int lineCount = lineIndexToNumber(fLTSize);

        if (emptyParagraphAtEndOfText()) {
            lineCount += 1;
        }

        return lineCount;
    }
    
    public synchronized int lineContaining(int charIndex) {
        
        formatToOffset(charIndex, TextOffset.AFTER_OFFSET);
        
        boolean placement = TextOffset.AFTER_OFFSET;
        if (charIndex == fLTCurTextLen && charIndex > 0) {
            placement = emptyParagraphAtEndOfText()? TextOffset.AFTER_OFFSET :
                                                     TextOffset.BEFORE_OFFSET;
        }
        
        return lineContaining(charIndex, placement);
    }

    public synchronized int lineContaining(TextOffset offset) {

        formatToOffset(offset);

        if (afterLastParagraph(offset)) {
            return lineIndexToNumber(fLTSize);
        }
        
        return lineContaining(offset.fOffset, offset.fPlacement);
    }
    
    private int lineContaining(int off, boolean placement) {
        
        int line = off==0? 0 : getLineContaining(off, placement);
        
        if (line == kAfterLastLine) {
            line = fLTSize;
        }
        else if (line == kBeforeFirstLine) {
            throw new Error("lineContaining got invalid result from getLineContaining().");
        }

        return lineIndexToNumber(line);
    }

    public synchronized int lineRangeLow(int lineNumber) {

        validateLineNumber(lineNumber, STRICT);
        int index = lineNumberToIndex(lineNumber);

        if (index == fLTSize) {
            if (emptyParagraphAtEndOfText()) {
                return lastLineCharStop();
            }
        }

        if (index >= fLTSize) {
            throw new IllegalArgumentException("lineNumber is invalid.");
        }
        else {
            return lineCharStartInternal(index);
        }
    }

    public synchronized int lineRangeLimit(int lineNumber) {

        validateLineNumber(lineNumber, STRICT);
        int index = lineNumberToIndex(lineNumber);

        if (index == fLTSize) {
            if (emptyParagraphAtEndOfText()) {
                return lastLineCharStop();
            }
        }

        if (index >= fLTSize) {
            throw new IllegalArgumentException("lineNumber is invalid.");
        }
        else {
            return lineCharLimitInternal(index);
        }
    }

    /**
     * Return the number of the line at the given graphic height.
     * If height is greater than full height, return line count.
     */
    public synchronized int lineAtHeight(int height) {

        if (height >= fPixHeight) {

            int line = getLineCount();
            if (height < fFullPixHeight) {
                line -= 1;
            }
            return line;
        }
        else if (height < 0) {
            return -1;
        }
        else {
            return lineIndexToNumber(findLineAt(height));
        }
    }

    public synchronized int lineGraphicStart(int lineNumber) {

        checkTimeStamp();
        validateLineNumber(lineNumber, LENIENT);

        int index = lineNumberToIndex(lineNumber);

        if (index < fLTSize) {
            return lineGraphicStartInternal(index);
        }
        else {
            if (index == fLTSize+1) {
                return fFullPixHeight;
            }
            else {
                return fPixHeight;
            }
        }
    }

    public synchronized boolean lineIsLeftToRight(int lineNumber) {
        
        validateLineNumber(lineNumber, STRICT);
        
        int index = lineNumberToIndex(lineNumber);
        
        if (index < fLTSize) {
            return fLineTable[index].isLeftToRight();
        }
        else {
            AttributeMap st = fText.paragraphStyleAt(fLTCurTextLen);
            return !TextAttribute.RUN_DIRECTION_RTL.equals(st.get(TextAttribute.RUN_DIRECTION));
        }
    }

    /**
    * Number of pixels by which to advance formatting in the background.
    */
    private static final int kPixIncrement = 100;

    /**
    * Time to sleep between background formatting operations.
    */
    private static final int kInterval = 100;

    /**
    * Perform periodic background formatting.
    */
    public void run()
    {
        while (true) {
            synchronized (this) {
                while(!fBgFormatAllowed) {
                    try {
                        wait();
                    } catch(InterruptedException e) {
                    }
                }

                checkTimeStamp();                
                formatToHeight(fPixHeight + kPixIncrement);

                if (lastLineCharStop() == fLTCurTextLen) {
                    stopBackgroundFormatting();
                }
            }

            try {
                Thread.sleep(kInterval);
            }
            catch(InterruptedException e) {
            }
        }
    }

    private ParagraphRenderer getRendererFor(AttributeMap s) {
    
        // Note:  eventually we could let clients put their own renderers
        // on the text.
        BidiParagraphRenderer renderer = (BidiParagraphRenderer) fRendererCache.get(s);
        if (renderer == null) {
            renderer = new BidiParagraphRenderer(fDefaultValues.addAttributes(s), fDefaultCharMetric);
            fRendererCache.put(s, renderer);
        }
        return renderer;
    }

}
