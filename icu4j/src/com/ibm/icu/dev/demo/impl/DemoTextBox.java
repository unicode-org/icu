/*
 * $RCSfile: DemoTextBox.java,v $ $Revision: 1.1 $ $Date: 2000/02/10 06:25:47 $
 *
 * (C) Copyright Taligent, Inc. 1996 - 1997 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996 - 1998 - All Rights Reserved
 *
 * Portions copyright (c) 1996 Sun Microsystems, Inc. All Rights Reserved.
 *
 *   The original version of this source code and documentation is copyrighted
 * and owned by Taligent, Inc., a wholly-owned subsidiary of IBM. These
 * materials are provided under terms of a License Agreement between Taligent
 * and Sun. This technology is protected by multiple US and International
 * patents. This notice and attribution to Taligent may not be removed.
 *   Taligent is a registered trademark of Taligent, Inc.
 *
 * Permission to use, copy, modify, and distribute this software
 * and its documentation for NON-COMMERCIAL purposes and without
 * fee is hereby granted provided that this copyright notice
 * appears in all copies. Please refer to the file "copyright.html"
 * for further important copyright and licensing information.
 *
 * SUN MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE, OR NON-INFRINGEMENT. SUN SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 *
 */
package com.ibm.demo;


import java.text.BreakIterator;
import java.awt.*;

public class DemoTextBox {

    public DemoTextBox(Graphics g, String text, int width)
    {
        this.text = text;
        this.chars = new char[text.length()];
        text.getChars(0, text.length(), chars, 0);

        this.width = width;
        this.port = g;
        this.metrics = g.getFontMetrics();

        breakText();
    }

    public  int getHeight() {
        return (nbreaks + 1) * metrics.getHeight();
    }

    public  void draw(Graphics g, int x, int y)
    {
        int index = 0;

        y += metrics.getAscent();

        for (int i = 0; i < nbreaks; i++)
        {
            g.drawChars(chars, index, breakPos[i] - index, x, y);
            index = breakPos[i];
            y += metrics.getHeight();
        }

        g.drawChars(chars, index, chars.length - index, x, y);
    }


    private void breakText()
    {
        if (metrics.charsWidth(chars, 0, chars.length) > width)
        {
            BreakIterator iter = BreakIterator.getWordInstance();
            iter.setText(text);

            int start = iter.first();
            int end = start;
            int pos;

            while ( (pos = iter.next()) != BreakIterator.DONE )
            {
                int w = metrics.charsWidth(chars, start, pos - start);
                if (w > width)
                {
                    // We've gone past the maximum width, so break the line
                    if (end > start) {
                        // There was at least one break position before this point
                        breakPos[nbreaks++] = end;
                        start = end;
                        end = pos;
                    } else {
                        // There weren't any break positions before this one, so
                        // let this word overflow the margin (yuck)
                        breakPos[nbreaks++] = pos;
                        start = end = pos;
                    }
                } else {
                    // the current position still fits on the line; it's the best
                    // tentative break position we have so far.
                    end = pos;
                }

            }
        }
    }

    private String          text;
    private char[]          chars;
    private Graphics        port;
    private FontMetrics     metrics;
    private int             width;

    private int[]           breakPos = new int[10]; // TODO: get real
    private int             nbreaks = 0;
}