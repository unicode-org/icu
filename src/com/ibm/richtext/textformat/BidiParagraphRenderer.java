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
// Requires Java2
package com.ibm.richtext.textformat;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Shape;

import java.util.Vector;

import com.ibm.richtext.styledtext.MConstText;
import com.ibm.richtext.styledtext.MTabRuler;
import com.ibm.richtext.styledtext.TabStop;

import com.ibm.richtext.textlayout.attributes.AttributeMap;
import com.ibm.richtext.textlayout.attributes.TextAttribute;

import com.ibm.richtext.textlayout.Graphics2DConversion;

///*JDK12IMPORTS
import java.awt.Graphics2D;

import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextHitInfo;

import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
//JDK12IMPORTS*/
/*JDK11IMPORTS
import com.ibm.richtext.textlayout.Graphics2D;

import com.ibm.richtext.textlayout.FontRenderContext;
import com.ibm.richtext.textlayout.TextLayout;
import com.ibm.richtext.textlayout.LineBreakMeasurer;
import com.ibm.richtext.textlayout.TextHitInfo;

import com.ibm.richtext.textlayout.AffineTransform;
import com.ibm.richtext.textlayout.GeneralPath;
import com.ibm.richtext.textlayout.Rectangle2D;
JDK11IMPORTS*/

final class BidiParagraphRenderer extends ParagraphRenderer {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private final class BidiSegment {
        TextLayout fLayout;
        Rectangle2D.Float fBounds;
        int fDistanceFromLeadingMargin;
    }

    private final class BidiLayoutInfo extends LayoutInfo
    {                    
        int fCharLength;      // number of characters on line (was fLength)
        int fAscent;
        int fDescent;
        int fLeading;
        int fVisibleAdvance;  // distance along line direction ie width
        int fTotalAdvance;    // distance along line direction including trailing whitespace

        int fLeadingMargin;   // screen distance from leading margin

        boolean fLeftToRight; // true iff the orientation is left-to-right

        final Vector fSegments = new Vector(); // segments to render, in logical order

        public int getCharLength() {
            return fCharLength;
        }

        public int getAscent() {
            return fAscent;
        }

        public int getDescent() {
            return fDescent;
        }

        public int getLeading() {
            return fLeading;
        }

        public int getVisibleAdvance() {
            return fVisibleAdvance;
        }

        public int getTotalAdvance() {
            return fTotalAdvance;
        }

        public int getLeadingMargin() {
            return fLeadingMargin;
        }

        public boolean isLeftToRight() {
            return fLeftToRight;
        }

        public int getHeight() {
            return fAscent + fDescent + fLeading;
        }

        public String toString()
        {
            return "LayoutInfo(charStart: " + getCharStart(0) +
                ", fCharLength: " + fCharLength +
                ", fAscent: " + fAscent +
                ", fDescent: " + fDescent +
                ", fVisibleAdvance: " + fVisibleAdvance +
                ", fTotalAdvance: " + fTotalAdvance +
                ", fLeadingMargin: " + fLeadingMargin +
                ")";
        }

        BidiParagraphRenderer fRenderer;

        // just delegate to renderer for now

        public void renderWithHighlight(int lengthBasis,
                                        Graphics2D g,
                                        int lineBound,
                                        int x,
                                        int y,
                                        TextOffset selStart,
                                        TextOffset selStop,
                                        Color highlightColor) {

            fRenderer.renderWithHighlight(this,
                                          lengthBasis,
                                          g,
                                          lineBound,
                                          x,
                                          y,
                                          selStart,
                                          selStop,
                                          highlightColor);
        }

        public void render(int lengthBasis,
                           Graphics2D g,
                           int lineBound,
                           int x,
                           int y) {
            fRenderer.render(this, lengthBasis, g, lineBound, x, y);
        }

        public void renderCaret(MConstText text,
                                int lengthBasis,
                                Graphics2D g,
                                int lineBound,
                                int x,
                                int y,
                                int charOffset,
                                Color strongCaretColor,
                                Color weakCaretColor) {
            fRenderer.renderCaret(this, text, lengthBasis, g, lineBound, x, y, charOffset,
                                        strongCaretColor, weakCaretColor);
        }

        public TextOffset pixelToOffset(int lengthBasis,
                                        TextOffset result,
                                        int lineBound,
                                        int x,
                                        int y) {
            return fRenderer.pixelToOffset(this, lengthBasis, result, lineBound, x, y);
        }

        public Rectangle caretBounds(MConstText text,
                                     int lengthBasis,
                                     int lineBound,
                                     int charOffset,
                                     int x,
                                     int y) {
            return fRenderer.caretBounds(this, text, lengthBasis, lineBound, charOffset, x, y);
        }
        
        public int strongCaretBaselinePosition(int lengthBasis,
                                               int lineBound,
                                               int charOffset) {

            return fRenderer.strongCaretBaselinePosition(this, lengthBasis, lineBound, charOffset);
        }

        public int getNextOffset(int lengthBasis,
                                 int charOffset,
                                 short dir) {

            return fRenderer.getNextOffset(this, lengthBasis, charOffset, dir);
        }
    }

    //private static final int FLUSH_LEADING = TextAttribute.FLUSH_LEADING.intValue();
    private static final int FLUSH_CENTER = TextAttribute.FLUSH_CENTER.intValue();
    private static final int FLUSH_TRAILING = TextAttribute.FLUSH_TRAILING.intValue();
    private static final int FULLY_JUSTIFIED = TextAttribute.FULLY_JUSTIFIED.intValue();

    private AttributeMap cacheStyle = null;

    private float fLeadingMargin;
    private float fTrailingMargin;
    private float fFirstLineIndent;
    private float fMinLineSpacing;
    private float fExtraLineSpacing;
    
    private int fFlush = -1;
    private MTabRuler fTabRuler;
    
    private boolean fLtrDefault;
    private DefaultCharacterMetric fDefaultCharMetric;
    
    BidiParagraphRenderer(AttributeMap pStyle, DefaultCharacterMetric defaultCharMetric) {

        fDefaultCharMetric = defaultCharMetric;
        initRenderer(pStyle);
    }

    private float getFloatValue(Object key, AttributeMap style) {
        return ((Float)style.get(key)).floatValue();
    }
    
    private int getIntValue(Object key, AttributeMap style) {
        return ((Integer)style.get(key)).intValue();
    }
    
    /**
     * NOTE:  it is illegal to initialize a StandardParagraphRenderer for any style
     * other than the one it was created with.
     */
    public void initRenderer(AttributeMap pStyle) {

        if (cacheStyle == null) {

            fLeadingMargin = getFloatValue(TextAttribute.LEADING_MARGIN, pStyle);
            fTrailingMargin = getFloatValue(TextAttribute.TRAILING_MARGIN, pStyle);
            fFirstLineIndent = getFloatValue(TextAttribute.FIRST_LINE_INDENT, pStyle);
            fMinLineSpacing = getFloatValue(TextAttribute.MIN_LINE_SPACING, pStyle);
            fExtraLineSpacing = getFloatValue(TextAttribute.EXTRA_LINE_SPACING, pStyle);

            fFlush = getIntValue(TextAttribute.LINE_FLUSH, pStyle);

            fTabRuler = (MTabRuler) pStyle.get(TextAttribute.TAB_RULER);
            
            Object runDir = pStyle.get(TextAttribute.RUN_DIRECTION);
            fLtrDefault = !TextAttribute.RUN_DIRECTION_RTL.equals(runDir);

            cacheStyle = pStyle;
        }
        else if (pStyle != cacheStyle) {
            if (!pStyle.equals(cacheStyle)) {
                throw new Error("Attempt to share BidiParagraphRenderer between styles!");
            }
            else {
                cacheStyle = pStyle;
            }
        }
    }

    private static boolean isTab(char ch) {
        return ch == '\t';
    }

    /**
     * Fill in info with the next line.
     * @param measurer the LineBreakMeasurer for this paragraph.
     *  Current position should be the first character on the line.
     *  If null, a 0-length line is generated.  If measurer is null
     *  then paragraphStart and paragraphLimit should be equal.
     */
    // Usually totalFormatWidth and lineBound will be the same.
    // totalFormatWidth is used for wrapping, but lineBound is
    // for flushing.  These may be different for unwrapped text,
    // for example.
    public LayoutInfo layout(MConstText text,
                             LayoutInfo layoutToReuse,
                             LineBreakMeasurer measurer,
                             FontRenderContext frc,
                             int paragraphStart,
                             int paragraphLimit,
                             int totalFormatWidth,
                             int lineBound) {

        if ((measurer==null) != (paragraphStart==paragraphLimit)) {
            throw new IllegalArgumentException(
                    "measurer, paragraphStart, paragraphLimit are wrong.");
        }
        BidiLayoutInfo line = null;

        try {
            line = (BidiLayoutInfo) layoutToReuse;
        }
        catch(ClassCastException e) {
        }

        if (line == null) {
            line = new BidiLayoutInfo();
        }

        line.fRenderer = this;

        final int lineCharStart = measurer==null? paragraphStart : measurer.getPosition();
        line.setCharStart(lineCharStart);

        final int lineIndent = (lineCharStart==paragraphStart)? (int) fFirstLineIndent : 0;

        int formatWidth = totalFormatWidth - (int) (fLeadingMargin + fTrailingMargin);
        computeLineMetrics(text, line, measurer, frc,
                            paragraphStart, paragraphLimit, formatWidth, lineIndent);

        // position the line according to the line flush
        if (fFlush == FLUSH_TRAILING || fFlush == FLUSH_CENTER) {
            int lineArea = lineBound - (int) (fLeadingMargin + fTrailingMargin);
            int advanceDifference = lineArea - line.fVisibleAdvance;

            if (fFlush == FLUSH_TRAILING) {
                line.fLeadingMargin = ((int) (fLeadingMargin)) + advanceDifference;
            }
            else if (fFlush == FLUSH_CENTER) {
                line.fLeadingMargin = (int) (fLeadingMargin + advanceDifference/2);
            }
        }
        else {
            line.fLeadingMargin = (int) fLeadingMargin;
        }

        return line;
    }

    /**
     * Fill in the following fields in line:
     * fCharLength, fAscent, fDescent, fLeading, fVisibleAdvance,
     * fTotalAdvance.
     * Uses: line.fLeadingMargin
     * @param formatWidth the width to fit the line into.
     */
    private void computeLineMetrics(MConstText text,
                                    BidiLayoutInfo line,
                                    LineBreakMeasurer measurer,
                                    FontRenderContext frc,
                                    final int paragraphStart,
                                    final int paragraphLimit,
                                    final int formatWidth,
                                    final int lineIndent) {

        int segmentCount = 0;
        /* variable not used boolean firstLine = measurer==null ||
                            measurer.getPosition() == paragraphStart; */

        if (measurer != null) {
            computeSegments(text, line, measurer, paragraphLimit, formatWidth, lineIndent);

            // iterate through segments and accumulate ascent, descent,
            // leading, char length
            float ascent = 0;
            float descent = 0;
            float descentPlusLeading = 0;

            segmentCount = line.fSegments.size();
            for (int i=0; i < segmentCount; i++) {
                TextLayout layout = ((BidiSegment)line.fSegments.elementAt(i)).fLayout;
                ascent = Math.max(ascent, layout.getAscent());
                float segDescent = layout.getDescent();
                descent = Math.max(descent, segDescent);
                descentPlusLeading = Math.max(descentPlusLeading, segDescent+layout.getLeading());
                line.fCharLength += layout.getCharacterCount();
            }

            line.fAscent = (int) Math.ceil(ascent);
            line.fDescent = (int) Math.ceil(descent);
            line.fLeading = (int) Math.ceil(descentPlusLeading) - line.fDescent;
        }
        else {
            line.fLeftToRight = fLtrDefault;
            line.fSegments.removeAllElements();

            line.fCharLength = 0;

            AttributeMap style = text.characterStyleAt(paragraphStart);
            DefaultCharacterMetric.Metric cm = fDefaultCharMetric.getMetricForStyle(style);
            line.fAscent = cm.getAscent();
            line.fDescent = cm.getDescent();
            line.fLeading = cm.getLeading();

            line.fVisibleAdvance = line.fTotalAdvance = 0;
        }

        if (fExtraLineSpacing != 0) {
            line.fAscent += (int) Math.ceil(fExtraLineSpacing);
        }

        if (fMinLineSpacing != 0){
            int height = line.getHeight();
            if (height < fMinLineSpacing) {
                line.fAscent += Math.ceil(fMinLineSpacing - height);
            }
        }

        final int lineNaturalAdvance = line.fTotalAdvance;

        line.fTotalAdvance += lineIndent;
        line.fVisibleAdvance += lineIndent;

        if (measurer != null) {
            // Now fill in fBounds field of BidiSegments.  fBounds should tile
            // the line.
            final float lineHeight = line.getHeight();

            for (int i=1; i < segmentCount; i++) {

                BidiSegment currentSegment = (BidiSegment) line.fSegments.elementAt(i-1);
                BidiSegment nextSegment = (BidiSegment) line.fSegments.elementAt(i);

                float origin;
                float width;

                if (line.fLeftToRight) {
                    origin = 0;
                    width = nextSegment.fDistanceFromLeadingMargin -
                                currentSegment.fDistanceFromLeadingMargin;
                }
                else {
                    origin = currentSegment.fDistanceFromLeadingMargin;
                    origin -= nextSegment.fDistanceFromLeadingMargin;
                    origin += (float) Math.ceil(nextSegment.fLayout.getAdvance());
                    width = (float) Math.ceil(currentSegment.fLayout.getAdvance()) - origin;
                }
                currentSegment.fBounds = new Rectangle2D.Float(origin, -line.fAscent, width, lineHeight);
            }

            // set last segment's bounds
            {
                BidiSegment currentSegment = (BidiSegment) line.fSegments.elementAt(segmentCount-1);
                float origin;
                float width;

                if (line.fLeftToRight) {
                    origin = 0;
                    width = lineNaturalAdvance - currentSegment.fDistanceFromLeadingMargin;
                }
                else {
                    origin = currentSegment.fDistanceFromLeadingMargin - lineNaturalAdvance;
                    width = (float) Math.ceil(currentSegment.fLayout.getAdvance()) - origin;
                }

                currentSegment.fBounds = new Rectangle2D.Float(origin, -line.fAscent, width, lineHeight);
            }
        }
    }

    /**
     * Fill in fSegments, fLeftToRight.  measurer must not be null
     */
    private void computeSegments(MConstText text,
                                 BidiLayoutInfo line,
                                 LineBreakMeasurer measurer,
                                 final int paragraphLimit,
                                 final int formatWidth,
                                 final int lineIndent) {

        // Note on justification:  only the last segment of a line is
        // justified.  
        // Also, if a line ends in a tab it will not be justified.
        // This behavior is consistent with other word processors
        // I tried (MS Word and Lotus Word Pro).
        
        line.fSegments.removeAllElements();
        line.fCharLength = 0;

        TabStop currentTabStop = new TabStop((int)fLeadingMargin+lineIndent, TabStop.kLeading);

        int segmentLimit = measurer.getPosition();
        boolean firstSegment = true;

        int advanceFromLeadingMargin = lineIndent;

        boolean computeSegs = true;
        
        computeTabbedSegments: do {

            // compute sementLimit:
            if (segmentLimit <= measurer.getPosition()) {
                while (segmentLimit < paragraphLimit) {
                    if (isTab(text.at(segmentLimit++))) {
                        break;
                    }
                }
            }

            // NOTE:  adjust available width for center tab!!!
            //System.out.println("Format width: " + (formatWidth-advanceFromLeadingMargin) +
            //                   ";  segmentLimit: " + segmentLimit);

            int wrappingWidth = Math.max(formatWidth-advanceFromLeadingMargin, 0);
            TextLayout layout = null;
            if (firstSegment || wrappingWidth > 0 || segmentLimit > measurer.getPosition()+1) {
                layout = measurer.nextLayout(wrappingWidth, segmentLimit, !firstSegment);
            }

            if (layout == null) {
                if (firstSegment) {
                    // I doubt this would happen, but check anyway
                    throw new Error("First layout is null!");
                }
                break computeTabbedSegments;
            }
            
            final int measurerPos = measurer.getPosition();
            if (measurerPos < segmentLimit) {
                computeSegs = false;
                if (fFlush == FULLY_JUSTIFIED) {
                    layout = layout.getJustifiedLayout(wrappingWidth);
                }
            }
            else {
                computeSegs = !(measurerPos == paragraphLimit);
            }

            if (firstSegment) {
                firstSegment = false;
                // Have to get ltr off of layout.  Not available from measurer,
                // unfortunately.
                line.fLeftToRight = layout.isLeftToRight();
            }

            BidiSegment segment = new BidiSegment();
            segment.fLayout = layout;
            int layoutAdvance = (int) Math.ceil(layout.getAdvance());

            // position layout relative to leading margin, update logicalPositionOnLine
            
            int relativeTabPosition = currentTabStop.getPosition()-(int)fLeadingMargin;
            int logicalPositionOfLayout;
            switch (currentTabStop.getType()) {
                case TabStop.kTrailing:
                    logicalPositionOfLayout = Math.max(
                                    relativeTabPosition-layoutAdvance,
                                    advanceFromLeadingMargin);
                    break;
                case TabStop.kCenter:
                    logicalPositionOfLayout = Math.max(
                                    relativeTabPosition-(layoutAdvance/2),
                                    advanceFromLeadingMargin);
                    break;
                default:  // includes decimal tab right now
                    logicalPositionOfLayout = relativeTabPosition;
                    break;
            }

            // position layout in segment
            if (line.fLeftToRight) {
                segment.fDistanceFromLeadingMargin = logicalPositionOfLayout;
            }
            else {
                segment.fDistanceFromLeadingMargin = logicalPositionOfLayout+layoutAdvance;
            }

            // update advanceFromLeadingMargin
            advanceFromLeadingMargin = logicalPositionOfLayout + layoutAdvance;

            // add segment to segment Vector
            line.fSegments.addElement(segment);

            // get next tab
            currentTabStop = fTabRuler.nextTab((int)fLeadingMargin+advanceFromLeadingMargin);
            if (currentTabStop.getType() == TabStop.kLeading ||
                            currentTabStop.getType() == TabStop.kAuto)  {
                advanceFromLeadingMargin = currentTabStop.getPosition();
                //System.out.println("Advance from leading margin:" + advanceFromLeadingMargin);

            }
            else {
               //System.out.println("Non-leading tab, type=" + currentTabStop.getType());
            }

        } while (computeSegs);

        // Now compute fTotalAdvance, fVisibleAdvance.  These metrics may be affected
        // by a trailing tab.

        {
            BidiSegment lastSegment = (BidiSegment) line.fSegments.lastElement();
            TextLayout lastLayout = lastSegment.fLayout;

            if (line.fLeftToRight) {
                line.fTotalAdvance = (int) Math.ceil(lastLayout.getAdvance()) +
                                        lastSegment.fDistanceFromLeadingMargin;
                line.fVisibleAdvance = (int) Math.ceil(lastLayout.getVisibleAdvance()) +
                                        lastSegment.fDistanceFromLeadingMargin;
            }
            else {
                line.fTotalAdvance = lastSegment.fDistanceFromLeadingMargin;
                line.fVisibleAdvance = lastSegment.fDistanceFromLeadingMargin -
                                        (int) Math.ceil(lastLayout.getAdvance() -
                                            lastLayout.getVisibleAdvance());
            }

            if (isTab(text.at(measurer.getPosition()-1))) {
                line.fTotalAdvance = Math.max(line.fTotalAdvance,
                                                currentTabStop.getPosition());
            }
        }
    }

    /**
     * Return the highlight shape for the given character offsets.
     * The Shape returned is relative to the leftmost point on the
     * baseline of line.
     */
    private Shape getHighlightShape(BidiLayoutInfo line,
                                    int lengthBasis,
                                    int lineBound,
                                    int hlStart,
                                    int hlLimit) {

        if (hlStart >= hlLimit) {
            throw new IllegalArgumentException("Highlight range length is not positive.");
        }

        final int leadingMargin = (line.fLeftToRight)?
                line.fLeadingMargin : lineBound - line.fLeadingMargin;
        final int segmentCount = line.fSegments.size();

        Shape rval = null;
        GeneralPath highlightPath = null;

        int currentLayoutStart = line.getCharStart(lengthBasis);

        for (int i=0; i < segmentCount; i++) {

            BidiSegment segment = (BidiSegment) line.fSegments.elementAt(i);
            TextLayout layout = segment.fLayout;
            int charCount = layout.getCharacterCount();
            int currentLayoutLimit = currentLayoutStart + charCount;
            boolean rangesIntersect;
            if (hlStart <= currentLayoutStart) {
                rangesIntersect = hlLimit > currentLayoutStart;
            }
            else {
                rangesIntersect = hlStart < currentLayoutLimit;
            }

            if (rangesIntersect) {

                Shape currentHl = layout.getLogicalHighlightShape(
                                        Math.max(hlStart-currentLayoutStart, 0),
                                        Math.min(hlLimit-currentLayoutStart, charCount),
                                        segment.fBounds);

                float xTranslate;
                if (line.fLeftToRight) {
                    xTranslate = leadingMargin +
                                 segment.fDistanceFromLeadingMargin;
                }
                else {
                    xTranslate = leadingMargin -
                                 segment.fDistanceFromLeadingMargin;
                }

                if (xTranslate != 0) {
                    AffineTransform xform =
                        AffineTransform.getTranslateInstance(xTranslate, 0);
                    currentHl = xform.createTransformedShape(currentHl);
                }

                if (rval == null) {
                    rval = currentHl;
                }
                else {
                    if (highlightPath == null) {
                        highlightPath = new GeneralPath();
                        highlightPath.append(rval, false);
                        rval = highlightPath;
                    }
                    highlightPath.append(currentHl, false);
                }
            }
            currentLayoutStart = currentLayoutLimit;
        }

        return rval;
    }

    private void renderWithHighlight(BidiLayoutInfo line,
                                     int lengthBasis,
                                     Graphics2D g,
                                     int lineBound,
                                     int x,
                                     int y,
                                     TextOffset selStart,
                                     TextOffset selStop,
                                     Color highlightColor) {

        final int lineCharStart = line.getCharStart(lengthBasis);

        if (selStart != null && selStop != null && !selStart.equals(selStop) &&
                line.fCharLength != 0 &&
                selStart.fOffset < lineCharStart + line.fCharLength &&
                selStop.fOffset > lineCharStart) {

            Shape highlight = getHighlightShape(line, lengthBasis, lineBound, selStart.fOffset, selStop.fOffset);
            if (highlight != null) {
                Graphics2D hl = (Graphics2D) g.create();
                hl.setColor(highlightColor);
                hl.translate(x, y + line.fAscent);
                hl.fill(highlight);
            }
        }

        render(line, lengthBasis, g, lineBound, x, y);
    }

    /**
     * Draw the line into the graphics.  (x, y) is the upper-left corner
     * of the line.  The leading edge of a right-aligned line is aligned
     * to (x + lineBound).
     */
    private void render(BidiLayoutInfo line,
                        int lengthBasis,
                        Graphics2D g,
                        int lineBound,
                        int x,
                        int y) {

        final int leadingMargin = (line.fLeftToRight)?
                x + line.fLeadingMargin : x + lineBound - line.fLeadingMargin;
        final int baseline = y + line.fAscent;
        final int segmentCount = line.fSegments.size();

        for (int i=0; i < segmentCount; i++) {

            BidiSegment segment = (BidiSegment) line.fSegments.elementAt(i);

            float drawX;
            if (line.fLeftToRight) {
                drawX = leadingMargin + segment.fDistanceFromLeadingMargin;
            }
            else {
                drawX = leadingMargin - segment.fDistanceFromLeadingMargin;
            }

            segment.fLayout.draw(g, drawX, baseline);
        }
    }

    private TextOffset hitTestSegment(TextOffset result,
                                      int segmentCharStart,
                                      BidiSegment segment,
                                      int xInSegment,
                                      int yInSegment) {

        final TextLayout layout = segment.fLayout;
        final int charCount = layout.getCharacterCount();
        final int layoutAdvance = (int) Math.ceil(layout.getAdvance());
        Rectangle2D bounds = segment.fBounds;

        final boolean ltr = layout.isLeftToRight();

        if (ltr && (xInSegment >= layoutAdvance) || !ltr && (xInSegment <= 0)) {

            // pretend the extra space at the end of the line is a
            // tab and 'hit-test' it.
            double tabCenter;
            if (ltr) {
                tabCenter = (layoutAdvance+bounds.getMaxX()) / 2;
            }
            else {
                tabCenter = bounds.getX() / 2;
            }

            if ((xInSegment >= tabCenter) == ltr) {
                result.fOffset = charCount;
                result.fPlacement = TextOffset.BEFORE_OFFSET;
            }
            else {
                result.fOffset = charCount-1;
                result.fPlacement = TextOffset.AFTER_OFFSET;
            }
        }
        else {
            TextHitInfo info = layout.hitTestChar(xInSegment, yInSegment, segment.fBounds);
            result.fOffset = info.getInsertionIndex();
            if (result.fOffset == 0) {
                result.fPlacement = TextOffset.AFTER_OFFSET;
            }
            else if (result.fOffset == charCount) {
                result.fPlacement = TextOffset.BEFORE_OFFSET;
            }
            else {
                result.fPlacement = info.isLeadingEdge()?
                        TextOffset.AFTER_OFFSET : TextOffset.BEFORE_OFFSET;
            }
        }

        result.fOffset += segmentCharStart;
        return result;
    }

    /**
     * Return the offset at the point (x, y).  (x, y) is relative to the top-left
     * of the line.  The leading edge of a right-aligned line is aligned
     * to lineBound.
     */
    private TextOffset pixelToOffset(BidiLayoutInfo line,
                                     int lengthBasis,
                                     TextOffset result,
                                     int lineBound,
                                     int x,
                                     int y) {

        if (result == null) {
            result = new TextOffset();
        }

        final int yInSegment = y - line.fAscent;
        final int leadingMargin = (line.fLeftToRight)?
                line.fLeadingMargin : lineBound - line.fLeadingMargin;
        final int lineCharStart = line.getCharStart(lengthBasis);

        // first see if point is before leading edge of line
        final int segmentCount = line.fSegments.size();
        {
            int segLeadingMargin = leadingMargin;
            if (segmentCount > 0) {
                BidiSegment firstSeg = (BidiSegment) line.fSegments.elementAt(0);
                if (line.fLeftToRight) {
                    segLeadingMargin += firstSeg.fDistanceFromLeadingMargin;
                }
                else {
                    segLeadingMargin -= firstSeg.fDistanceFromLeadingMargin;
                    segLeadingMargin += (float) firstSeg.fBounds.getMaxX();
                }
            }
            if (line.fLeftToRight == (x <= segLeadingMargin)) {
                result.fOffset = lineCharStart;
                result.fPlacement = TextOffset.AFTER_OFFSET;
                return result;
            }
        }

        int segmentCharStart = lineCharStart;

        for (int i=0; i < segmentCount; i++) {

            BidiSegment segment = (BidiSegment) line.fSegments.elementAt(i);
            int segmentOrigin = line.fLeftToRight?
                            leadingMargin+segment.fDistanceFromLeadingMargin :
                            leadingMargin-segment.fDistanceFromLeadingMargin;
            int xInSegment = x - segmentOrigin;
            if (line.fLeftToRight) {
                if (segment.fBounds.getMaxX() > xInSegment) {
                    return hitTestSegment(result, segmentCharStart, segment, xInSegment, yInSegment);
                }
            }
            else {
                if (segment.fBounds.getX() < xInSegment) {
                    return hitTestSegment(result, segmentCharStart, segment, xInSegment, yInSegment);
                }
            }
            segmentCharStart += segment.fLayout.getCharacterCount();
        }

        result.fOffset = lineCharStart + line.fCharLength;
        result.fPlacement = TextOffset.BEFORE_OFFSET;
        return result;
    }

    private void renderCaret(BidiLayoutInfo line,
                             MConstText text,
                             int lengthBasis,
                             Graphics2D g,
                             int lineBound,
                             int x,
                             int y,
                             final int charOffset,
                             Color strongCaretColor,
                             Color weakCaretColor)
    {
        final int segmentCount = line.fSegments.size();
        final int lineStart = line.getCharStart(lengthBasis);

        int currentStart = lineStart;
        BidiSegment segment = null;
        int segmentIndex;

        for (segmentIndex=0; segmentIndex < segmentCount; segmentIndex++) {
            segment = (BidiSegment) line.fSegments.elementAt(segmentIndex);
            int currentEndpoint = currentStart + segment.fLayout.getCharacterCount();
            if (currentEndpoint > charOffset) {
                break;
            }
            currentStart = currentEndpoint;
        }

        /*
            There are two choices here:
            1. get carets from a TextLayout and render them, or
            2. make up a caret ourselves and render it.
            We want to do 2 when:
                * there is no text on the line, or
                * the line ends with a tab and we are drawing the last caret on the line
            Otherwise, we want 1.
        */

        if (segmentIndex == segmentCount && segmentCount > 0) {
            // If we get here, line length is not 0, and charOffset is at end of line
            if (!isTab(text.at(charOffset-1))) {
                segmentIndex = segmentCount-1;
                segment = (BidiSegment) line.fSegments.elementAt(segmentIndex);
                currentStart = lineStart + line.getCharLength() -
                                    segment.fLayout.getCharacterCount();
            }
        }

        Object savedPaint = Graphics2DConversion.getColorState(g);

        try {
            if (segmentIndex < segmentCount) {
                TextLayout layout = segment.fLayout;
                int offsetInLayout = charOffset - currentStart;
                Shape[] carets = layout.getCaretShapes(offsetInLayout, segment.fBounds);
                g.setColor(strongCaretColor);
                int layoutPos = line.fLeadingMargin + segment.fDistanceFromLeadingMargin;
                int layoutX = line.fLeftToRight?
                        x + layoutPos : x + lineBound - layoutPos;
                int layoutY = y + line.fAscent;

                // Translating and then clipping doesn't work.  Try this:
                Rectangle2D.Float clipRect = new Rectangle2D.Float();
                clipRect.setRect(segment.fBounds);
                clipRect.x += layoutX;
                clipRect.y += layoutY;
                clipRect.width += 1;
                clipRect.height -= 1;

                Object savedClip = ClipWorkaround.saveClipState(g);
                try {
                    ClipWorkaround.translateAndDrawShapeWithClip(g,
                                                                layoutX,
                                                                layoutY,
                                                                clipRect,
                                                                carets[0]);
                    if (carets[1] != null) {
                        g.setColor(weakCaretColor);
                        ClipWorkaround.translateAndDrawShapeWithClip(g,
                                                                    layoutX,
                                                                    layoutY,
                                                                    clipRect,
                                                                    carets[1]);
                    }
                }
                finally {
                    ClipWorkaround.restoreClipState(g, savedClip);
                }
            }
            else {
                int lineEnd = line.fLeadingMargin + line.fTotalAdvance;
                int endX = line.fLeftToRight? lineEnd : lineBound-lineEnd;
                endX += x;
                g.drawLine(endX, y, endX, y+line.getHeight()-1);
            }
        }
        finally {
            Graphics2DConversion.restoreColorState(g, savedPaint);
        }
    }

    private Rectangle caretBounds(BidiLayoutInfo line,
                                  MConstText text,
                                  int lengthBasis,
                                  int lineBound,
                                  int charOffset,
                                  int x,
                                  int y) {

        final int segmentCount = line.fSegments.size();
        final int lineStart = line.getCharStart(lengthBasis);
        int currentStart = lineStart;
        BidiSegment segment = null;
        int segmentIndex;

        for (segmentIndex=0; segmentIndex < segmentCount; segmentIndex++) {
            segment = (BidiSegment) line.fSegments.elementAt(segmentIndex);
            int currentEndpoint = currentStart + segment.fLayout.getCharacterCount();
            if (currentEndpoint > charOffset) {
                break;
            }
            currentStart = currentEndpoint;
        }

        if (segmentIndex == segmentCount && segmentCount > 0) {
            // If we get here, line length is not 0, and charOffset is at end of line
            if (!isTab(text.at(charOffset-1))) {
                segmentIndex = segmentCount-1;
                segment = (BidiSegment) line.fSegments.elementAt(segmentIndex);
                currentStart = lineStart + line.getCharLength() -
                                    segment.fLayout.getCharacterCount();
            }
        }

        Rectangle r;
        
        if (segmentIndex < segmentCount) {
            TextLayout layout = segment.fLayout;
            int offsetInLayout = charOffset - currentStart;
            Shape[] carets = layout.getCaretShapes(offsetInLayout, segment.fBounds);
            r = carets[0].getBounds();
            if (carets[1] != null) {
                r.add(carets[1].getBounds());
            }
            r.width += 1;
            
            int layoutPos = line.fLeadingMargin + segment.fDistanceFromLeadingMargin;
            if (line.fLeftToRight) {
                r.x += layoutPos;
            }
            else {
                r.x += lineBound - layoutPos;
            }
            r.y += line.fAscent;
        }
        else {
            r = new Rectangle();
            r.height = line.getHeight();
            r.width = 1;
            int lineEnd = line.fLeadingMargin + line.fTotalAdvance;
            if (line.fLeftToRight) {
                r.x = lineEnd;
            }
            else {
                r.x = lineBound - lineEnd;
            }
        }
        
        r.translate(x, y);
        return r;
    }

    private int strongCaretBaselinePosition(BidiLayoutInfo line,
                                            int lengthBasis,
                                            int lineBound,
                                            int charOffset) {

        final int segmentCount = line.fSegments.size();
        int currentStart = line.getCharStart(lengthBasis);
        BidiSegment segment = null;
        int segmentIndex;

        for (segmentIndex=0; segmentIndex < segmentCount; segmentIndex++) {
            segment = (BidiSegment) line.fSegments.elementAt(segmentIndex);
            int currentEndpoint = currentStart + segment.fLayout.getCharacterCount();
            if (currentEndpoint > charOffset) {
                break;
            }
            currentStart = currentEndpoint;
        }

        if (segmentIndex < segmentCount) {
            TextLayout layout = segment.fLayout;
            int offsetInLayout = charOffset - currentStart;
            TextHitInfo hit = TextHitInfo.afterOffset(offsetInLayout);
            hit = TextLayout.DEFAULT_CARET_POLICY.getStrongCaret(hit, hit.getOtherHit(), layout);
            float[] info = layout.getCaretInfo(hit);
            int layoutPos = line.fLeadingMargin + segment.fDistanceFromLeadingMargin;
            if (line.fLeftToRight) {
                return layoutPos + (int) info[0];
            }
            else {
                return lineBound - layoutPos + (int) info[0];
            }
        }
        else {
            int lineEnd = line.fLeadingMargin + line.fTotalAdvance;
            if (line.fLeftToRight) {
                return lineEnd;
            }
            else {
                return lineBound - lineEnd;
            }
        }
    }

    private int getNextOffset(BidiLayoutInfo line,
                              int lengthBasis,
                              int charOffset,
                              short dir) {

        if (dir != MFormatter.eLeft && dir != MFormatter.eRight) {
            throw new IllegalArgumentException("Invalid direction.");
        }

        // find segment containing offset:
        final int segmentCount = line.fSegments.size();
        final int lineCharStart = line.getCharStart(lengthBasis);

        int currentStart = lineCharStart;
        BidiSegment segment = null;
        int segmentIndex;

        for (segmentIndex=0; segmentIndex < segmentCount; segmentIndex++) {
            segment = (BidiSegment) line.fSegments.elementAt(segmentIndex);
            int currentEndpoint = currentStart + segment.fLayout.getCharacterCount();
            if (currentEndpoint > charOffset ||
                    (segmentIndex == segmentCount-1 && currentEndpoint==charOffset)) {
                break;
            }
            currentStart = currentEndpoint;
        }

        final boolean logAdvance = (dir==MFormatter.eRight)==(line.fLeftToRight);

        int result;

        if (segmentIndex < segmentCount) {
            TextLayout layout = segment.fLayout;
            int offsetInLayout = charOffset - currentStart;
            TextHitInfo hit = (dir==MFormatter.eLeft)?
                        layout.getNextLeftHit(offsetInLayout) :
                        layout.getNextRightHit(offsetInLayout);
            if (hit == null) {
                result = logAdvance?
                    currentStart+layout.getCharacterCount()+1 : currentStart-1;
            }
            else {
                result = hit.getInsertionIndex() + currentStart;
            }
        }
        else {
            result = logAdvance? lineCharStart + line.fCharLength + 1 :
                                         lineCharStart - 1;
        }

        return result;
    }
}