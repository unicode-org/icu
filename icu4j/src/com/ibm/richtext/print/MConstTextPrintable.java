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
package com.ibm.richtext.print;

import com.ibm.richtext.styledtext.MConstText;

import com.ibm.richtext.textformat.MFormatter;

import com.ibm.richtext.textlayout.attributes.AttributeMap;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;

import java.util.Vector;

/**
 * This class's interface is very close to that of the JDK 1.2 Printable 
 * interface, but can execute on JDK 1.1.  On 1.2, this class is wrapped
 * in a real Printable.  On 1.1, the PrintContext class uses this class
 * and a PrintJob for printing.
 *
 * Note that this class paginates the text in the first call to print,
 * or to getPageCount.
 * After construction, its page size is essentially fixed.  This is not 
 * as flexible as the 1.2 classes allow, but it should suffice.
 */
final class MConstTextPrintable {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";

    static final int PAGE_EXISTS = 0;
    static final int NO_SUCH_PAGE = 1;
    
    private MConstText fText;
    private AttributeMap fDefaultStyles;
    private Rectangle fPageRect;
    
    // If these two fields are null the text has not been paginated.
    private MFormatter fFormatter;
    private Vector fPageStarts;

    /**
     * Construct an MConstTextPrintable to print the given text.  Each page will fit
     * into pageRect.
     */
    MConstTextPrintable(MConstText text, 
                        AttributeMap defaultStyles,
                        Rectangle pageRect) {

        fText = text;
        fDefaultStyles = defaultStyles;
        fPageRect = new Rectangle(pageRect);
    }
    
    private static boolean emptyParagraphAtEndOfText(MConstText text) {

        if (text.length() > 0) {
            char ch = text.at(text.length()-1);
            return ch == '\n' || ch == '\u2029';
        }
        else {
            return false;
        }
    }

    private void paginate(Graphics graphics) {
        
        if (fPageStarts == null) {

            fFormatter = MFormatter.createFormatter(fText,
                                                    fDefaultStyles,
                                                    fPageRect.width,
                                                    true,
                                                    graphics);
 
            fFormatter.formatToHeight(Integer.MAX_VALUE);
            fFormatter.stopBackgroundFormatting();

            fPageStarts = new Vector();
            
            int lineCount = fFormatter.getLineCount();
            if (emptyParagraphAtEndOfText(fText)) {
                lineCount -= 1;
            }

            int startLine = 0;
            fPageStarts.addElement(new Integer(startLine));
            int startHeight = 0;
            final int pageHeight = fPageRect.height;

            while (startLine < lineCount) {

                int nextStart = fFormatter.lineAtHeight(startHeight + pageHeight);
                fPageStarts.addElement(new Integer(nextStart));
                startHeight = fFormatter.lineGraphicStart(nextStart);
                startLine = nextStart;
            }
        }
    }

    /**
     * Print the given page in the given graphics.  Page numbers are
     * 0-based.  The the return value indicates whether
     * the page number is valid (as in JDK 1.2).  Since you can get the page count
     * directly, there's really no excuse for passing in an invalid page
     * index.
     * @param graphics the Graphics to print to
     * @param pageNumber the 0-based page number.  Should be nonnegative and
     * less than getPageCount()
     * @return PAGE_EXISTS if the page number is valid, or 
     *         NO_SUCH_PAGE otherwise
     */
    int print(Graphics graphics, int pageNumber) {
        
        paginate(graphics);
        
        if (pageNumber < getPageCount(graphics) && pageNumber >= 0) {
            graphics.setColor(Color.black); // workaround for 1.2 printing bug
            int startLine = ((Integer)fPageStarts.elementAt(pageNumber)).intValue();
            int limitLine = ((Integer)fPageStarts.elementAt(pageNumber+1)).intValue();

            int topOfPage = fFormatter.lineGraphicStart(startLine);
            int pageHeight = fFormatter.lineGraphicStart(limitLine) - topOfPage;

            Point origin = new Point(fPageRect.x, fPageRect.y - topOfPage);
            Rectangle drawRect = new Rectangle(fPageRect);
            drawRect.height = pageHeight;
            
            fFormatter.draw(graphics, drawRect, origin);
            return PAGE_EXISTS;
        }
        else {
            return NO_SUCH_PAGE;
        }
    }
    
    /**
     * Return the number of pages that can be printed.
     * @param graphics a Graphics instance representative of those 
     * which will be printed into
     */
    int getPageCount(Graphics graphics) {
        
        paginate(graphics);
        return fPageStarts.size() - 1;
    }
}