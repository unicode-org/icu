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
package com.ibm.richtext.textformat;

import com.ibm.richtext.textlayout.attributes.AttributeMap;

import com.ibm.richtext.styledtext.MConstText;


///*JDK12IMPORTS
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
//JDK12IMPORTS*/

/*JDK11IMPORTS
import com.ibm.richtext.textlayout.FontRenderContext;
import com.ibm.richtext.textlayout.LineBreakMeasurer;
import com.ibm.richtext.textlayout.Graphics2D;
JDK11IMPORTS*/

/**
 * ParagraphRenderer is a factory for LayoutInfo objects.
 */
abstract class ParagraphRenderer {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    // If renderers can ever travel with their styles, then this attribute will denote a
    // renderer.  For now, renderers are not added as styles so this isn't needed.
    //static final TextAttribute PARAGRAPH_RENDERER = new TextAttribute("Paragraph_Renderer") {};

    /**
     * Reset the renderer to use information from this style. Since renderers may be shared, you should
     * make sure the renderer is initialized for the style you wish to render before you use it.
     */
    public void initRenderer(AttributeMap pStyle) {
    }

    /**
     * Return a LayoutInfo for the paragraph represented by
     * measurer.
     * @param text the text containing the paragraph
     * @param layoutToReuse clients can pass in a LayoutInfo
     * which the ParagraphRenderer may choose to reuse
     * and return.  If null, a new LayoutInfo will be
     * created and returned.
     * @param measurer the LineBreakMeasurer for this paragraph.
     *  Current position should be the first character on the line.
     *  If null, a 0-length line is generated.  If measurer is null
     *  then paragraphStart and paragraphLimit should be equal.
     * @param frc the FontRenderContext used for measurerment
     * @param paragraphStart the index in the text where the
     * current paragraph begins
     * @param paragraphLimit the index of the first character
     * after the current paragraph
     * @param totalFormatWidth the width in which the line should fit
     * @param lineBound where right-aligned lines are aligned
     */
    public abstract LayoutInfo layout(MConstText text,
                                      LayoutInfo layoutToReuse,
                                      LineBreakMeasurer measurer,
                                      FontRenderContext frc,
                                      int paragraphStart,
                                      int paragraphLimit,
                                      int totalFormatWidth,
                                      int lineBound);

}
