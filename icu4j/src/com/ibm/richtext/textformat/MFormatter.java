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

package com.ibm.richtext.textformat;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;

import com.ibm.richtext.styledtext.MConstText;
import com.ibm.richtext.textlayout.attributes.AttributeMap;

/**
*
* This class formats lines of text to a given length.
* It provides services needed for static text display,
* and also editable text, including:  displaying text,
* reformatting text after an edit, converting between
* screen locations and offsets into the text, calculating
* areas of the screen for "highlighting,"  and computing
* offsets into the text resulting from arrow keys.
* <p>
* Text clients instantiate this class with an
* <tt>MConstText</tt> object and a format width.  Text
* can be formatted such that all lines fit within the
* format length.  Alternatively, text can be formatted
* such that lines end only at the end of paragraphs.
* <p>
* The format length is specified with the <tt>setLineBound()</tt>
* method.
* <p>
* Methods in the formatter which interact with the graphics
* system generally take as a paramter a <tt>Point</tt> object
* which represents the "origin" of the text display.  The
* origin represents the location, in the graphics system used to display the text, of
* the top-left corner of the text.
* <p>
* To display the text, call <tt>draw()</tt>, passing the
* a rectangle in which to draw as a parameter.  Only lines
* of text in the draw rectangle will be drawn.
* <p>
* When the formatter's text changes, it is important to first call
* <tt>stopBackgroundFormatting()</tt> to prevent the Formatter from
* accessing the text from a background thread.  After modifications are
* complete,
* call the <tt>updateFormat()</tt> method before invoking any other
* methods of the formatter.  <tt>updateFormat()</tt> reformats the
* new text, formatting no more text than is necessary.
* <p>
* The formatter provides services for responding to user input from the
* mouse and keyboard.  The method <tt>pointToTextOffset()</tt> converts
* a screen location to an offset in the text.  The method <tt>textOffsetToPoint</tt>
* converts an offset in the text to an array of two <tt>Point</tt> objects, which can be
* used to draw a verticle caret, denoting an insertion point.  <tt>highlightArea</tt>
* accepts two offsets into the text as paramters, and returns an array of <tt>Polygon</tt>
* objects representing areas where visual highlighting should be applied.
* <p>
* Finally, for
* keyboard handling, the <tt>findNewInsertionOffset()</tt> method accepts an "initial"
* offset, a "previous" offset, as well as a direction, and returns a new offset.  The direction
* can be up, down, left, or right.  The previous offset is the insertion point location, before
* the arrow key is processed.  The initial offset is the offset where an up or down arrow
* key sequence began.  Using the initial offset allows for "intelligent" handling of up and down
* arrow keys.
* <p>
* Examples of using the MFormatter class
* are given in the <tt>AsyncFormatter</tt> class
* documentation.
* <p>
* @author John Raley
*
* @see com.ibm.richtext.styledtext.MText
*/

public abstract class MFormatter {
    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";

    public abstract AttributeMap getDefaultValues();

/**
* Display text in drawArea, with highlighting.
* Does not reformat text
* @param g the Graphics object in which to draw
* @param drawArea the rectangle, in g's coordinate system, in which to draw
* @param origin the top-left corner of the text, in g's coordinate system
* @param selStart the offset where the current selection begins;  pass <tt>null</tt> if no selection
* @param selStop the offset where the current selection ends
* @param highlight the color of the highlighting
*/
    public abstract void draw(Graphics g, Rectangle drawArea, Point origin,
            TextOffset selStart, TextOffset selStop, Color highlight);

    public abstract void draw(Graphics g, Rectangle drawArea, Point origin);

/**
* Specify whether to wrap line at the edge of the destination area.
* <tt>true</tt> means wrap lines;  <tt>false</tt> means to break lines
* only when an end-of-line character is reached.
* @param wrap <tt>true</tt> to break lines at the edge of the destination
* area;  <tt>false</tt> otherwise.
*/

    public abstract void setWrap(boolean wrap);

/**
* Return whether text is wrapped at the edge of the destination area.
* @see #setWrap
*/
    public abstract boolean wrap();

/**
* Specify the number of pixels along the "line dimension".
* Lines are formatted to fit within the line dimension.  The
* line dimension in Roman script is horizontal.
* @param lineBound the length, in pixels, to which lines will be formatted
*/
    public abstract void setLineBound(int lineBound);

/**
* Return the number of pixels along the line dimension.
* @return the number of pixels along the line dimension.
*/
    public abstract int lineBound();
    
/**
* Format text down to given height.
* @param height the height to which text will be formatted
*/
    public abstract void formatToHeight(int height);

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
* @return a <tt>Rectangle</tt> which specifies the area in which text must be
* redrawn to reflect the change to the text.
*/
    public abstract Rectangle updateFormat(int afStart,
                                  int afLength,
                                  Rectangle viewRect,
                                  Point origin);


    public abstract int minY();
    
/**
 * Return the maximum vertical coordinate of the document area.
 */
    public abstract int maxY();

    public abstract int minX();

/**
 * Return the maximum horizontal coordinate of the document area.
 */
    public abstract int maxX();

/**
* Return the actual pixel length of the text which has been formatted.
*/
    public abstract int formattedHeight();

    public static final short eUp = -10, eDown = 10, eLeft = -1, eRight = 1;

/**
* Given a screen location p, return the offset of the character in the text nearest to p.
*
* The offset may or may not include a newline at the end of a line, determined by anchor and infiniteMode.
* The newline is not included if infiniteMode is true and the anchor is the position before the newline.
*
* @param result TextOffset to modify and return.  If null, one will be allocated, modified, and returned.
* @param px the x component of the point.
* @param py the y component of the point.
* @param origin the top-left corner of the text, in the display's coordinate system
* @param anchor the previous offset.  May be null.  Used to determine whether newlines are included.
* @param infiniteMode if true, treat newlines at end of line as having infinite width.
*/
    public abstract TextOffset pointToTextOffset(TextOffset result, int px, int py, Point origin, TextOffset anchor, boolean infiniteMode);

/**
* Given an offset, return the Rectangle bounding the caret at the offset.
* @param offset an offset into the text
* @param origin the top-left corner of the text, in the display's coordinate system
* @return a Rectangle bounding the caret.
*/
    public abstract Rectangle getCaretRect(TextOffset offset, Point origin);

/**
* Draw the caret(s) associated with the given offset into the given Graphics.
* @param g the Graphics to draw into
* @param offset the offset in the text for which the caret is drawn
* @param origin the top-left corner of the text, in the display's coordinate system
* @param strongCaretColor the color of the strong caret
* @param weakCaretColor the color of the weak caret (if any)
*/
    public abstract void drawCaret(Graphics g,
                                   TextOffset offset,
                                   Point origin,
                                   Color strongCaretColor,
                                   Color weakCaretColor);

    /**
     * @see #getBoundingRect
     */
    public static final boolean LOOSE = false;
    /**
     * @see #getBoundingRect
     */
    public static final boolean TIGHT = true;

/**
* Given two offsets in the text, return a rectangle which encloses the lines containing the offsets.
* Offsets do not need to be ordered or nonnegative.
* @param offset1 an offset into the text
* @param offset2 the other offset into the text
* @param origin the top-left corner of the text, in the display's coordinate system
* @param tight if equal to TIGHT, the bounds is as small as possible.  If LOOSE, the width
* of the bounds is allowed to be wider than necesary.  Loose bounds are easier to compute.
* @return a <tt>Rectangle</tt>, relative to <tt>origin</tt>, which encloses the lines containing the offsets
*/
    public abstract Rectangle getBoundingRect(TextOffset offset1,
                                              TextOffset offset2, 
                                              Point origin,
                                              boolean tight);

    public abstract void getBoundingRect(Rectangle boundingRect,
                                         TextOffset offset1,
                                         TextOffset offset2,
                                         Point origin,
                                         boolean tight);

/**
* Compute the offset resulting from moving from a previous offset in direction dir.
* For arrow keys.
* @param previousOffset the insertion offset prior to the arrow key press
* @param direction the direction of the arrow key (eUp, eDown, eLeft, or eRight)
* @return new offset based on direction and previous offset.
*/
    public abstract TextOffset findInsertionOffset(TextOffset result,
                                          TextOffset previousOffset,
                                          short direction);

/**
* Compute the offset resulting from moving from a previous offset, starting at an original offset, in direction dir.
* For arrow keys.  Use this for "smart" up/down keys.
* @param result TextOffset to modify and return.  If null, a new TextOffset is created, modified, and returned.
* @param initialOffset The offset at which an up-down arrow key sequence began.
* @param previousOffset The insertion offset prior to the arrow key press.
* @param direction The direction of the arrow key (eUp, eDown, eLeft, or eRight)
* @return new offset based on direction and previous offset(s).
*/
    public abstract TextOffset findNewInsertionOffset(TextOffset result,
                                             TextOffset initialOffset,
                                             TextOffset previousOffset,
                                             short direction);

/**
* Return the index of the line containing the given character index.
* This method has complicated semantics, arising from not knowing 
* which side of the index to check.  The index will be given an
* implicit AFTER bias, unless the index is the last index in the text,
* the text length is non-zero, and there is not a paragraph separator
* at the end of the text.
*/
    public abstract int lineContaining(int index);
    
/**
* Return the index of the line containing the given offset.
*/
    public abstract int lineContaining(TextOffset offset);

/**
* Return the number of lines.
*/
    public abstract int getLineCount();

/**
* Return the index of the first character on the given line.
*/
    public abstract int lineRangeLow(int lineNumber);

/**
* Return the index of the first character following the given line.
*/
    public abstract int lineRangeLimit(int lineNumber);

/**
* Tells the formatter to stop accessing the text until updateFormat is called.
*/
    public abstract void stopBackgroundFormatting();

/**
* Return the line number at the given graphic height.  If height is greater than
* the text height, maxLineNumber + 1 is returned.
*/
    public abstract int lineAtHeight(int height);

/**
* Return the graphic height where the given line begins.  If the lineNumber is
* maxLineNumber the entire text height is returned.
*/
    public abstract int lineGraphicStart(int lineNumber);

/**
* Return true if the given line is left-to-right.
* @param lineNumber a valid line
* @return true if lineNumber is left-to-right
*/
    public abstract boolean lineIsLeftToRight(int lineNumber);

/**
* Return a new <tt>MFormatter</tt>.
* @param text the text to format
* @param defaultValues values to use when certain attributes are not specified. 
*    <tt>defaultValues</tt> must contain values for the following attributes:
*    <tt>FAMILY</tt>, <tt>WEIGHT</tt>, <tt>POSTURE</tt>, <tt>SIZE</tt>, <tt>SUPERSCRIPT</tt>, 
*    <tt>FOREGROUND</tt>, <tt>UNDERLINE</tt>, <tt>STRIKETHROUGH</tt>,
*    <tt>EXTRA_LINE_SPACING</tt>, <tt>FIRST_LINE_INDENT</tt>,<tt>MIN_LINE_SPACING</tt>,
*    <tt>LINE_FLUSH</tt>, <tt>LEADING_MARGIN</tt>, <tt>TRAILING_MARGIN</tt>, <tt>TAB_RULER</tt>
* @param lineBound length to which lines are formatted
* @param wrap <tt>true</tt> if text should be "line wrapped" (formatted to fit destination area)
*/
    public static MFormatter createFormatter(MConstText text,
                                             AttributeMap defaultValues,
                                             int lineBound, 
                                             boolean wrap, 
                                             Graphics g) {
                                                
        return new AsyncFormatter(text, defaultValues, lineBound, wrap, g);
    }
}
