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
package com.ibm.richtext.test.unit;

import com.ibm.icu.dev.test.TestFmwk;

import com.ibm.richtext.textlayout.attributes.AttributeMap;
import com.ibm.richtext.textlayout.attributes.TextAttribute;
import com.ibm.richtext.styledtext.MText;
import com.ibm.richtext.styledtext.MConstText;
import com.ibm.richtext.styledtext.StandardTabRuler;
import com.ibm.richtext.styledtext.StyledText;

import com.ibm.richtext.textformat.TextOffset;
import com.ibm.richtext.textformat.MFormatter;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import java.util.Hashtable;

public final class TestFormatter extends TestFmwk {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    public static void main(String[] args) throws Exception {

        new TestFormatter().run(args);
    }

    private static final Point ORIGIN = new Point(0, 0);

    private static final AttributeMap DEFAULTS;
    static {
        final Float floatZero = new Float(0.0f);

        Hashtable defaults = new Hashtable();
        defaults.put(TextAttribute.FAMILY, "Serif");
        defaults.put(TextAttribute.WEIGHT, new Float(1.0f));
        defaults.put(TextAttribute.POSTURE, floatZero);
        defaults.put(TextAttribute.SIZE, new Float(18.0f));
        defaults.put(TextAttribute.SUPERSCRIPT, new Integer(0));
        defaults.put(TextAttribute.FOREGROUND, Color.black);
        defaults.put(TextAttribute.UNDERLINE, new Integer(-1));
        defaults.put(TextAttribute.STRIKETHROUGH, Boolean.FALSE);

        defaults.put(TextAttribute.EXTRA_LINE_SPACING, floatZero);
        defaults.put(TextAttribute.FIRST_LINE_INDENT, floatZero);
        defaults.put(TextAttribute.MIN_LINE_SPACING, floatZero);
        defaults.put(TextAttribute.LINE_FLUSH, TextAttribute.FLUSH_LEADING);
        defaults.put(TextAttribute.LEADING_MARGIN, floatZero);
        defaults.put(TextAttribute.TRAILING_MARGIN, floatZero);
        defaults.put(TextAttribute.TAB_RULER, new StandardTabRuler());

        DEFAULTS = new AttributeMap(defaults);
    }
    
    // arg to testLineExceptions
    private static final int UNKNOWN = -1;

    private Graphics fGraphics;

    public TestFormatter() {

        fGraphics = new BufferedImage(100, 100, BufferedImage.TYPE_3BYTE_BGR).getGraphics();
        
        //JDK 1.1:
        //Frame f = new Frame();
        //f.show();
        //fGraphics = f.getGraphics();
    }

    private String fiveLines = "a\nb\nc\nd\ne";
    private String twelveLines = fiveLines + "\n" + fiveLines + "\nf\n";
    AttributeMap PLAIN = AttributeMap.EMPTY_ATTRIBUTE_MAP;

    public void test() {

        MConstText text = new StyledText(fiveLines, PLAIN);
        _testLineExceptions(makeFormatter(text, 100, true), 5);
        _testLineAccess(makeFormatter(text, 100, true), 5);

        text = new StyledText(twelveLines, PLAIN);
        _testLineExceptions(makeFormatter(text, 3, false), 12);
        _testLineAccess(makeFormatter(text, 100, true), 12);

        _testWithModification();
    }

    private void _testWithModification() {

        MText text = new StyledText(fiveLines, PLAIN);
        MFormatter formatter = makeFormatter(text, 100, true);
        Rectangle viewRect = new Rectangle(0, 0, 100, Integer.MAX_VALUE);

        formatter.stopBackgroundFormatting();
        text.append(new StyledText("\n", PLAIN));
        formatter.updateFormat(text.length()-1, 1, viewRect, ORIGIN);

        _testLineAccess(formatter, 6);

        formatter.stopBackgroundFormatting();
        text.append(new StyledText("ad", PLAIN));
        formatter.updateFormat(text.length()-2, 2, viewRect, ORIGIN);
        _testLineAccess(formatter, 6);
        _testLineExceptions(formatter, 6);

        formatter.stopBackgroundFormatting();
        text.remove(0, 1);
        formatter.updateFormat(0, 0, viewRect, ORIGIN);
        _testLineAccess(formatter, 6);
        _testLineExceptions(formatter, 6);
    }


    private MFormatter makeFormatter(MConstText text,
                                     int lineBound,
                                     boolean wrap) {

        return MFormatter.createFormatter(text, 
                                          DEFAULTS,
                                          lineBound,
                                          wrap,
                                          fGraphics);
    }

    private void _testLineExceptions(MFormatter formatter,
                                    int numLines) {

        if (numLines == UNKNOWN) {
            numLines = formatter.getLineCount();
        }

        boolean caught = false;

        try {
            formatter.lineRangeLow(numLines);
        }
        catch(IllegalArgumentException e) {
            caught = true;
        }

        if (!caught) {
            errln("Didn't get exception");
        }
        caught = false;

        try {
            formatter.lineRangeLimit(numLines);
        }
        catch(IllegalArgumentException e) {
            caught = true;
        }

        if (!caught) {
            errln("Didn't get exception");
        }
        caught = false;

        try {
            formatter.lineGraphicStart(numLines+1);
        }
        catch(IllegalArgumentException e) {
            caught = true;
        }

        if (!caught) {
            errln("Didn't get exception");
        }
        caught = false;
    }

    private void _testLineAccess(MFormatter formatter,
                                 int numLines) {

        if (numLines == UNKNOWN) {
            numLines = formatter.getLineCount();
        }

        if (formatter.lineGraphicStart(0) != 0) {
            errln("Line 0 doesn't start at height 0");
        }
        if (formatter.lineRangeLow(0) != 0) {
            errln("Line 0 doesn't start at character 0");
        }

        int lastLimit = formatter.lineRangeLimit(0);
        final int lineBound = formatter.lineBound();
        int[] hitX = new int[] { -1, 1, lineBound + 2 };

        TextOffset offset = new TextOffset();

        for (int i=1; i < numLines; i++) {

            int height = formatter.lineGraphicStart(i);
            if (lastLimit != formatter.lineRangeLow(i)) {
                errln("lastLine limit is not current line start");
            }
            int limit = formatter.lineRangeLimit(i);

            if (limit < lastLimit || (limit == lastLimit && i != numLines-1)) {
                errln("line has negative or 0 length");
            }

            int nextHeight = formatter.lineGraphicStart(i+1);
            if (nextHeight <= height) {
                errln("0-height line");
            }
            int incAmount = Math.max((nextHeight-height)/4, 1);
            for (int hitY = height; hitY < nextHeight; hitY += incAmount) {

                if (formatter.lineAtHeight(hitY) != i) {
                    errln("lineAtHeight is wrong");
                }

                for (int j=0; j < hitX.length; j++) {
                    offset = formatter.pointToTextOffset(offset,
                                        hitX[j], hitY, ORIGIN, null, false);
                    if (offset.fOffset < lastLimit || offset.fOffset > limit) {
                        errln("Inconsistent offset from pointToTextOffset");
                    }
                    //if (formatter.lineContaining(offset) != i) {
                    //    int debug = formatter.lineContaining(offset);
                    //    errln("lineContaining is incorrect");
                    //}
                }
            }

            lastLimit = limit;
        }
    }
}