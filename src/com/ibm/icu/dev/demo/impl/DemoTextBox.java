/*
 *******************************************************************************
 * Copyright (C) 1997-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.demo.impl;


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