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

import com.ibm.richtext.styledtext.StyledText;
import com.ibm.richtext.styledtext.MConstText;
import com.ibm.richtext.styledtext.MText;
import com.ibm.richtext.textlayout.attributes.AttributeMap;
import com.ibm.richtext.styledtext.StyleModifier;
import java.util.Random;

public final class TestParagraphStyles extends TestFmwk {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
                
    public static void main(String[] args) throws Exception {

        new TestParagraphStyles().run(args);
    }

    private static final int RAND_SEED = 1234;
    private static final int NUM_TESTS = 2500;

    private static final boolean isParagraphBreak(char c) {

        return c =='\u2029' || c == '\n';
    }

    private static final Object KEY = "KEY";
    private static final AttributeMap PLAIN = AttributeMap.EMPTY_ATTRIBUTE_MAP;
    private static final AttributeMap A_STYLE = new AttributeMap(KEY, new Character('a'));
    private static final StyleModifier A_MOD =
                            StyleModifier.createReplaceModifier(A_STYLE);
    private static final AttributeMap B_STYLE = new AttributeMap(KEY, new Character('b'));
    private static final StyleModifier B_MOD =
                            StyleModifier.createReplaceModifier(B_STYLE);
    private static final AttributeMap C_STYLE = new AttributeMap(KEY, new Character('c'));
    private static final StyleModifier C_MOD =
                            StyleModifier.createReplaceModifier(C_STYLE);
    private static final AttributeMap D_STYLE = new AttributeMap(KEY, new Character('d'));
    private static final StyleModifier D_MOD =
                            StyleModifier.createReplaceModifier(D_STYLE);
    private static final AttributeMap E_STYLE = new AttributeMap(KEY, new Character('e'));
    private static final StyleModifier E_MOD =
                            StyleModifier.createReplaceModifier(E_STYLE);

    public void test() {

        easyTests();
        randomTest();
    }

    private void easyTests() {

        MText text = new StyledText("a\nb\nc\nd\n", PLAIN);
        text.modifyParagraphStyles(0, text.length(), A_MOD);
        verifyParagraphCount(text);

        MText src = new StyledText("XXX\nYYY", PLAIN);
        src.modifyParagraphStyles(0, src.length(), B_MOD);
        verifyParagraphCount(src);

        MText temp = text.extractWritable(0, text.length());
        temp.append(src);
        verifyParagraphCount(temp);
        for (int i=0; i < text.length(); i++) {
            if (!temp.paragraphStyleAt(i).equals(text.paragraphStyleAt(i))) {
                errln("Paragraph styles are wrong");
            }
        }
        for (int i=0; i < src.length(); i++) {
            if (!temp.paragraphStyleAt(i+text.length()).equals(src.paragraphStyleAt(i))) {
                errln("Paragraph styles are wrong");
            }
        }

        temp = text.extractWritable(0, text.length());
        temp.replace(0, 1, src, 0, src.length());
        verifyParagraphCount(temp);
        if (temp.paragraphLimit(0) != 4) {
            errln("Paragraph limit is wrong");
        }
        if (!temp.paragraphStyleAt(0).equals(B_STYLE)) {
            errln("First style is wrong");
        }
        if (!temp.paragraphStyleAt(4).equals(A_STYLE)) {
            errln("Style after insert is wrong");
        }

        // test append
        MConstText newSrc = src.extract(4, 7);
        MText initC = new StyledText("cccccc", PLAIN);
        initC.modifyParagraphStyles(0, initC.length(), C_MOD);
        initC.append(newSrc);
        // now initC should be one paragraph with style B
        if (initC.paragraphLimit(0) != initC.length()) {
            errln("Should only be one paragraph");
        }
        if (initC.paragraphStyleAt(0) != initC.paragraphStyleAt(initC.length())) {
            errln("Two different paragraph styles");
        }
        if (!initC.paragraphStyleAt(initC.length()/2).equals(B_STYLE)) {
            errln("Incorrect paragraph style");
        }
        
        text = new StyledText("aaa\n", PLAIN);
        text.modifyParagraphStyles(0, text.length(), A_MOD);
        text.modifyParagraphStyles(text.length(), text.length(), B_MOD);
        if (text.paragraphStyleAt(text.length()) != B_STYLE) {
            errln("0-length paragraph at end has incorrect style");
        }
    }

    private static int randInt(Random rand, int limit) {

        return randInt(rand, 0, limit);
    }

    private static int randInt(Random rand, int start, int limit) {

        if (start > limit) {
            throw new IllegalArgumentException("Range is 0-length.");
        }
        else if (start == limit) {
            return start;
        }

        return start + (Math.abs(rand.nextInt())%(limit-start)) ;
    }

    private void randomTest() {

        MText noParagraph = new StyledText("zzzz", PLAIN);
        noParagraph.modifyParagraphStyles(0, noParagraph.length(), A_MOD);
        MText twoParagraphs = new StyledText("aaa\nbbb", PLAIN);
        twoParagraphs.modifyParagraphStyles(0, twoParagraphs.paragraphLimit(0), B_MOD);
        MText threeParagraphs = new StyledText("cc\ndd\nee", PLAIN);
        threeParagraphs.modifyParagraphStyles(0, 3, C_MOD);
        threeParagraphs.modifyParagraphStyles(3, 6, D_MOD);
        threeParagraphs.modifyParagraphStyles(6, 8, E_MOD);
        MText trailingP1 = new StyledText("hhhh\n", PLAIN);
        trailingP1.modifyParagraphStyles(0, trailingP1.paragraphLimit(0), C_MOD);
        MText trailingP2 = new StyledText("iii\n", PLAIN);
        trailingP2.modifyParagraphStyles(0, 0, D_MOD);
        trailingP2.modifyParagraphStyles(trailingP2.length(), trailingP2.length(), B_MOD);

        if (!trailingP2.paragraphStyleAt(trailingP2.length()-1).equals(D_STYLE)) {
            errln("Style incorrect in trailingP2");
        }
        if (!trailingP2.paragraphStyleAt(trailingP2.length()).equals(B_STYLE)) {
            errln("Ending style incorrect in trailingP2");
        }

        MConstText[] tests = { noParagraph, twoParagraphs,
                                    threeParagraphs, trailingP1, trailingP2 };

        Random random = new Random(RAND_SEED);

        int stopAt = 465;
        int i = 0;
        try {
            for (i=0; i < NUM_TESTS; i++) {

                int srcIndex = randInt(random, tests.length);
                int targetIndex = randInt(random, tests.length);
                MText target = new StyledText(tests[targetIndex]);
                MConstText src = tests[srcIndex];

                int srcStart = randInt(random, src.length());
                int srcLimit = randInt(random, srcStart, src.length());
                int start = randInt(random, target.length());
                int limit = randInt(random, start, target.length());

                if (i == stopAt) {
                    stopAt = i;
                }

                insertAndCheck(src, srcStart, srcLimit, target, start, limit);
            }
        }
        finally {
            if (i < NUM_TESTS) {
                logln("iteration=" + i);
            }
        }
    }

    private void insertAndCheck(MConstText src, int srcStart, int srcLimit,
                                MText target, int start, int limit) {

        // p-style after insertion
        AttributeMap after;
        if (limit == target.length() && srcLimit > srcStart) {
            after = src.paragraphStyleAt(srcLimit);
        }
        else {
            after = target.paragraphStyleAt(limit);
        }

        AttributeMap before;
        boolean srcHasPBreak = false;
        for (int i=srcStart; i < srcLimit; i++) {
            if (isParagraphBreak(src.at(i))) {
                srcHasPBreak = true;
                break;
            }
        }

        if (start > 0 && isParagraphBreak(target.at(start-1))) {
            before = target.paragraphStyleAt(start-1);
        }
        else {
            before = srcHasPBreak? src.paragraphStyleAt(srcStart) : after;
        }
        boolean stylePropogated = !before.equals(target.paragraphStyleAt(Math.max(0, start-1)));


        target.resetDamagedRange();
        target.replace(start, limit, src, srcStart, srcLimit);
        final int damageLimit = (start==limit && srcStart==srcLimit)?
                        Integer.MIN_VALUE : start + (srcLimit-srcStart);

        if (target.damagedRangeLimit() != damageLimit) {
            logln("limit: " + damageLimit + ";  target.limit: " +
                                target.damagedRangeLimit());
            errln("Damaged range limit is incorrect");
        }

        final int damageStart = (damageLimit==Integer.MIN_VALUE)? Integer.MAX_VALUE :
                (stylePropogated? target.paragraphStart(Math.max(0, start-1)) : start);
        if (target.damagedRangeStart() > damageStart) {
            logln("start: " + damageStart + ";  target.start: " +
                                target.damagedRangeStart());
            errln("Damaged range start is incorrect");
        }

        verifyParagraphCount(target);

        // check endpoints
        if (!before.equals(target.paragraphStyleAt(Math.max(start-1, 0)))) {
            errln("Incorrect paragraph style before modified range");
        }

        int lengthDelta = (srcLimit-srcStart) - (limit-start);
        int indexAfterInsert = Math.min(target.length(), limit + lengthDelta);
        if (!after.equals(target.paragraphStyleAt(indexAfterInsert))) {
            errln("Incorrect paragraph style after modified range");
        }

        if (srcHasPBreak) {
            int startP = target.paragraphLimit(start);
            int limitOfTest = target.paragraphStart(indexAfterInsert);

            int offset = start - srcStart;

            while (startP < limitOfTest) {
                int limitP = target.paragraphLimit(startP);
                if (src.paragraphLimit(startP-offset) + offset != limitP) {
                    errln("paragraph limits are not consistent");
                }
                if (!src.paragraphStyleAt(startP-offset)
                                    .equals(target.paragraphStyleAt(startP))) {
                    errln("paragraph styles are not consistent");
                }
                startP = limitP;
            }
        }
        else {
            for (int i=start; i < start+(srcLimit-srcStart); i++) {
                if (!after.equals(target.paragraphStyleAt(i))) {
                    errln("paragraph style changed unexpectedly");
                }
            }
        }
    }

    private void verifyParagraphCount(MConstText text) {

        int pCount = 0;
        int textLength = text.length();

        if (textLength == 0) {
            pCount = 1;
        }
        else {
            for (int s=0; s < textLength; s = text.paragraphLimit(s)) {
                pCount++;
            }
            if (isParagraphBreak(text.at(textLength-1))) {
                pCount++;
            }
        }

        int sepCount = 0;
        for (int i=0; i < textLength; i++) {
            if (isParagraphBreak(text.at(i))) {
                sepCount++;
            }
        }

        if (sepCount + 1 != pCount) {
            logln("sepCount=" + sepCount + ";  pCount=" + pCount);
            errln("Paragraph count is not consistent with characters");
        }
    }

    private void checkEndpoint(MConstText text) {

        boolean emptyFinalParagraph;
        int length = text.length();

        if (length != 0) {
            char ch = text.at(length-1);
            emptyFinalParagraph = isParagraphBreak(ch);
        }
        else {
            emptyFinalParagraph = true;
        }

        if ((text.paragraphStart(length) == length) != emptyFinalParagraph) {
            errln("Final paragraph length is incorrect");
        }
    }
}