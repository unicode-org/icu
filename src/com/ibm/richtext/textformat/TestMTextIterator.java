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

import com.ibm.richtext.styledtext.StyledText;
import com.ibm.richtext.styledtext.MConstText;
import com.ibm.richtext.styledtext.MText;

import com.ibm.richtext.textlayout.attributes.TextAttribute;
import com.ibm.richtext.textlayout.attributes.AttributeMap;

import com.ibm.richtext.textpanel.TextPanel;

/**
 * Test for MTextIterator.
 */
class TestMTextIterator {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private static final FontResolver FONT_MAPPER;
    static {
        AttributeMap attrs = TextPanel.getDefaultSettings().getDefaultValues();
        FONT_MAPPER = new FontResolver(attrs);
    }

    public static void compareIterToText(MTextIterator iter,
                                         MConstText text) {

        //System.out.println("Text: " + text);
        final int beginIndex = iter.getBeginIndex();
        final int endIndex = iter.getEndIndex();

        char ch = iter.setIndex(beginIndex);

        for (int i=beginIndex; i < endIndex; i++) {
            //System.out.print(ch+ " ");
            if (ch != text.at(i)) {
                throw new Error("Characters are not the same.");
            }
            ch = iter.next();
        }

        if (ch != MTextIterator.DONE) {
            throw new Error("Iterator is not done.");
        }

        for (int i=endIndex-1; i >= beginIndex; i--) {
            ch = iter.previous();
            //System.out.print(ch+ " ");
            if (ch != text.at(i)) {
                throw new Error("Backward iteration failed.");
            }
        }

        iter.setIndex(beginIndex);

        int runLimit;
        for (int runStart = beginIndex; runStart < endIndex; runStart = runLimit) {

            runLimit = Math.min(endIndex, text.characterStyleLimit(runStart));

            if (iter.getRunStart() != runStart) {
                System.out.println(iter.getRunStart() + "; " + runStart);
                throw new Error("getRunStart is wrong.");
            }
            if (iter.getRunLimit() != runLimit) {
                System.out.println(iter.getRunLimit() + "; " + runLimit);
                throw new Error("getRunLimit is wrong.");
            }

            AttributeMap style = text.characterStyleAt(runStart);

            while (iter.getIndex() < runLimit) {
                AttributeMap resolved = FONT_MAPPER.applyFont(style);
                if (!iter.getAttributes().equals(resolved)) {
                    throw new Error("Style is wrong.");
                }
                iter.next();
            }
        }
    }

    public void test() {

        AttributeMap bold = new AttributeMap(TextAttribute.WEIGHT,
                                             TextAttribute.WEIGHT_BOLD);
        MText text = new StyledText("Hello there!", AttributeMap.EMPTY_ATTRIBUTE_MAP);
        text.replace(2, 2, 'V', bold);

        MTextIterator iter = new MTextIterator(text, FONT_MAPPER, 0, text.length());
        compareIterToText(iter, text);

        text.replace(6, 8, new StyledText("ALL_BOLD", bold), 0, 8);
        iter = new MTextIterator(text, FONT_MAPPER, 1, text.length()-3);
        compareIterToText(iter, text);

        iter = new MTextIterator(text, FONT_MAPPER, 0, text.length());
        compareIterToText(iter, text);
    }

    public static void main(String[] args) {

        new TestMTextIterator().test();
        System.out.println("PASSED");
    }
}
