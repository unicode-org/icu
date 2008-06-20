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
/** LayoutInfo

    A line of text, possibly containing tab-segments.
*/

package com.ibm.richtext.textformat;

import java.awt.Color;
import java.awt.Rectangle;

///*JDK12IMPORTS
import java.awt.Graphics2D;
//JDK12IMPORTS*/

/*JDK11IMPORTS
import com.ibm.richtext.textlayout.Graphics2D;
JDK11IMPORTS*/

import com.ibm.richtext.styledtext.MConstText;

abstract class LayoutInfo
{
    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private int fCharStart;       // offset in text to start of line (was fStart)
                                  // neg. values indicate distance from end of text
    private int fGraphicStart;    // min pixel offset in fill direction
                                  // negative values indicate distance from bottom of text view

    /*
        These methods are for storing Layouts in a gap-storage,
        relative to either the start of end of text.  See AsyncFormatter.

        If you just want absolute (that is, start-relative) char and
        graphic starts, don't make them end-relative.
    */

    public final int getCharStart(int lengthBasis) {

        if (fCharStart >= 0) {
            return fCharStart;
        }
        else {
            return lengthBasis + fCharStart;
        }
    }

    public final int getGraphicStart(int graphicBasis) {

        if (fGraphicStart >= 0) {
            return fGraphicStart;
        }
        else {
            return graphicBasis + fGraphicStart;
        }
    }

    public final void setCharStart(int beginningRelativeStart) {

        if (beginningRelativeStart < 0) {
            throw new IllegalArgumentException("charStart must be nonnegavitve");
        }
        fCharStart = beginningRelativeStart;
    }

    public final void setGraphicStart(int beginningRelativeStart) {

        if (beginningRelativeStart < 0) {
            throw new IllegalArgumentException("charStart must be nonnegavitve");
        }
        fGraphicStart = beginningRelativeStart;
    }

    public final void makeRelativeToBeginning(int lengthBasis,
                                              int graphicBasis) {

        if (lengthBasis < 0 || graphicBasis < 0) {
            throw new IllegalArgumentException("Bases must be positive.");
        }
        if (fCharStart >= 0 || fGraphicStart >= 0) {
            throw new Error("Already start-relative.");
        }

        fCharStart += lengthBasis;
        fGraphicStart += graphicBasis;
    }

    public final void makeRelativeToEnd(int lengthBasis,
                                        int graphicBasis) {

        if (lengthBasis < 0 || graphicBasis < 0) {
            throw new IllegalArgumentException("Bases must be positive.");
        }
        if (fCharStart < 0 || fGraphicStart < 0) {
            throw new Error("Already end-relative.");
        }

        fCharStart -= lengthBasis;
        fGraphicStart -= graphicBasis;
    }


    public abstract int getCharLength();
    public abstract int getAscent();
    public abstract int getDescent();
    public abstract int getLeading();

    public abstract int getVisibleAdvance();
    public abstract int getTotalAdvance();
    public abstract int getLeadingMargin();

    public abstract boolean isLeftToRight();

    public int getHeight() {

        return getAscent()+getDescent()+getLeading();
    }

    /**
    * Draws text with highlighting.
    */
    public void renderWithHighlight(int lengthBasis,
                                    Graphics2D g,
                                    int lineBound,
                                    int x,
                                    int y,
                                    TextOffset selStart,
                                    TextOffset selStop,
                                    Color highlightColor)
    {
    }

    /** Use layout information to render the line at x, y.*/

    public void render(int lengthBasis,
                       Graphics2D g,
                       int lineBound,
                       int x,
                       int y)
    {
    }

    public void renderCaret(MConstText text,
                            int lengthBasis,
                            Graphics2D g,
                            int lineBound,
                            int x,
                            int y,
                            int charOffset,
                            Color strongCaretColor,
                            Color weakCaretColor)
    {
    }

    /**
     * Given a point within this line, return the character offset corresponding to that point.
     *
     * @param result.  This may be null, in which case a new TextOffset will be allocated.
     *        This object is modified in place, and also returned as the function result.
     * @param text Text to inspect.
     * @param lineX Position on this line relative to top left corner of this line.
     * @param lineY Position on this line relative to top left corner of this line.
     */
    public abstract TextOffset pixelToOffset(int lengthBasis,
                                             TextOffset result,
                                             int lineBound,
                                             int x,
                                             int y);

    public abstract int strongCaretBaselinePosition(int lengthBasis,
                                                    int lineBound,
                                                    int charOffset);

    public abstract Rectangle caretBounds(MConstText text,
                                          int lengthBasis,
                                          int lineBound,
                                          int charOffset,
                                          int x,
                                          int y);
    
    public abstract int getNextOffset(int lengthBasis,
                                      int charOffset,
                                      short dir);
}
