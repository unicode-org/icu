/*
 * @(#)$RCSfile: TestMText.java,v $ $Revision: 1.1 $ $Date: 2000/04/20 17:46:57 $
 *
 * (C) Copyright IBM Corp. 1998-1999.  All Rights Reserved.
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
package com.ibm.richtext.tests;

import com.ibm.textlayout.attributes.AttributeMap;
import com.ibm.textlayout.attributes.TextAttribute;

import com.ibm.richtext.styledtext.MConstText;
import com.ibm.richtext.styledtext.MText;
import com.ibm.richtext.styledtext.StyledText;
import com.ibm.richtext.styledtext.StyleModifier;

import java.text.CharacterIterator;
import java.util.Random;

import java.io.*;

public class TestMText {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
    private static final int TEST_ITERATIONS = 5000;
    private static final int STYLE_TEST_ITERATIONS = 5000;
    private static final long RAND_SEED = 598436;

    private static StyleModifier createMinusModifier(final Object attr) {
        return new StyleModifier() {
            public AttributeMap modifyStyle(AttributeMap style) {
                return style.removeAttribute(attr);
            }
        };
    }

    private static final String NO_STREAMING_ARG = "-nostreaming";

    public static void main(String[] args) {

        TestMText t = new TestMText();
        boolean streaming = true;

        if (args.length > 0) {
            if (args.length == 1 && args[0].equals(NO_STREAMING_ARG)) {
                streaming = false;
            }
            else {
                throw new Error("USAGE: java TestMText [" + NO_STREAMING_ARG + "]");
            }
        }

        t.simpleTest();
        t.styleTest();
        t.monkeyTest(streaming);
        System.out.println("MText test PASSED");
    }

    public void simpleTest() {

        AttributeMap boldStyle = new AttributeMap(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
        AttributeMap italicStyle = new AttributeMap(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE);

        MConstText allBold = new StyledText("bbbbb", boldStyle);
        MConstText allItalic = new StyledText("iii", italicStyle);
        MConstText plain = new StyledText("pppppp", AttributeMap.EMPTY_ATTRIBUTE_MAP);

        {
            MText buf = new StyledText();
            int ts = buf.getTimeStamp();
            buf.append(allBold);
            buf.append(allItalic);

            if (ts == buf.getTimeStamp()) {
                throw new Error("Time stamp not incremented");
            }

            // should be bbbbbiii now

            if (buf.length() != allBold.length() + allItalic.length()) {
                throw new Error("Length is wrong.");
            }

            for (int i=0; i < buf.length(); i++) {

                char rightChar;
                AttributeMap rightStyle;

                if (i < allBold.length()) {
                    rightChar = allBold.at(0);
                    rightStyle = boldStyle;
                }
                else {
                    rightChar = allItalic.at(0);
                    rightStyle = italicStyle;
                }

                if (buf.at(i) != rightChar) {
                    throw new Error("Character is wrong.");
                }
                if (!buf.characterStyleAt(i).equals(rightStyle)) {
                    throw new Error("Style is wrong.");
                }
            }

            int pos = 0;

            if (!buf.characterStyleAt(pos).equals(boldStyle)) {
                throw new Error("First style is wrong.");
            }
            if (buf.characterStyleLimit(pos) != allBold.length()) {
                throw new Error("Run length is wrong.");
            }

            pos = allBold.length();

            if (!buf.characterStyleAt(pos).equals(italicStyle)) {
                throw new Error("Second style is wrong.");
            }
            if (buf.characterStyleLimit(pos) != buf.length()) {
                throw new Error("Run length is wrong.");
            }

            {
                buf.resetDamagedRange();
                int oldLength = buf.length();
                buf.replace(buf.length(), buf.length(), allBold, 0, allBold.length());
                // bbbbbiiibbbbb

                if (buf.damagedRangeStart() != oldLength) {
                    throw new Error("Damaged range start is incorrect");
                }
                if (buf.damagedRangeLimit() != buf.length()) {
                    throw new Error("Damaged range limit is incorrect");
                }
            }

            int start = allBold.length();
            int limit = start + allItalic.length();
            buf.remove(start, limit);
            // bbbbbbbbbb

            if (buf.length() != 2 * allBold.length()) {
                throw new Error("Text should be twice the length of bold text.");
            }

            pos = buf.length() / 2;
            if (buf.characterStyleStart(pos) != 0 ||
                            buf.characterStyleLimit(pos) != buf.length()) {
                throw new Error("Run range is wrong.");
            }
            if (!buf.characterStyleAt(pos).equals(boldStyle)) {
                throw new Error("Run style is wrong.");
            }

            ts = buf.getTimeStamp();
            CharacterIterator cIter = buf.createCharacterIterator();
            for (char ch = cIter.first(); ch != cIter.DONE; ch = cIter.next()) {
                if (ch != allBold.at(0)) {
                    throw new Error("Character is wrong.");
                }
            }

            if (ts != buf.getTimeStamp()) {
                throw new Error("Time stamp should not have changed");
            }

            buf.replace(0, 1, plain, 0, plain.length());

            if (ts == buf.getTimeStamp()) {
                throw new Error("Time stamp not incremented");
            }

            // ppppppbbbbbbbbb
            buf.replace(plain.length(), buf.length(), allItalic, 0, allItalic.length());
            // ppppppiii

            if (buf.length() != allItalic.length()+plain.length()) {
                throw new Error("Length is wrong.");
            }

            pos = 0;
            if (buf.characterStyleLimit(pos) != plain.length()) {
                throw new Error("Run limit is wrong.");
            }

            pos = plain.length();
            if (buf.characterStyleLimit(pos) != buf.length()) {
                throw new Error("Run limit is wrong.");
            }

            buf.replace(plain.length(), plain.length(), allBold, 0, allBold.length());
            // ppppppbbbbbiii

            AttributeMap st = buf.characterStyleAt(1);
            if (!st.equals(AttributeMap.EMPTY_ATTRIBUTE_MAP)) {
                throw new Error("Style is wrong.");
            }
            if (buf.characterStyleStart(1) != 0 || buf.characterStyleLimit(1) != plain.length()) {
                throw new Error("Style start is wrong.");
            }

            st = buf.characterStyleAt(buf.length() - 1);
            if (!st.equals(italicStyle)) {
                throw new Error("Style is wrong.");
            }
            if (buf.characterStyleStart(buf.length() - 1) != plain.length()+allBold.length()) {
                throw new Error("Style start is wrong.");
            }

            if (buf.characterStyleLimit(buf.length() - 1) != buf.length()) {
                throw new Error("Style limit is wrong.");
            }
        }
    }

    private static int randInt(Random rand, int limit) {

        return randInt(rand, 0, limit);
    }

    private static int randInt(Random rand, int start, int limit) {

        if (start > limit) {
            throw new IllegalArgumentException("Range length is negative.");
        }
        else if (start == limit) {
            return start;
        }

        return start + (Math.abs(rand.nextInt())%(limit-start)) ;
    }

    public void styleTest() {

        MText text = new StyledText("0123456789", AttributeMap.EMPTY_ATTRIBUTE_MAP);

        AttributeMap[] styles = new AttributeMap[text.length()];
        for (int i=0; i < styles.length; i++) {
            styles[i] = AttributeMap.EMPTY_ATTRIBUTE_MAP;
        }
        AttributeMap[] oldStyles = new AttributeMap[styles.length];
        System.arraycopy(styles, 0, oldStyles, 0, styles.length);

        AttributeMap bigStyle = new AttributeMap(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON).
                                                    addAttribute(TextAttribute.SIZE, new Float(23.0f));

        StyleModifier[] modifiers = {
            StyleModifier.createReplaceModifier(new AttributeMap(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD)),
            StyleModifier.createAddModifier(new AttributeMap(TextAttribute.WEIGHT, new Float(1.0f))),
            createMinusModifier(TextAttribute.WEIGHT),

            StyleModifier.createAddModifier(new AttributeMap(TextAttribute.POSTURE, new Float(0.0f))),
            StyleModifier.createReplaceModifier(new AttributeMap(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE)),
            createMinusModifier(TextAttribute.POSTURE),

            StyleModifier.createAddModifier(bigStyle),
            StyleModifier.createReplaceModifier(bigStyle),
            createMinusModifier(bigStyle.getKeySet())
        };

        Random rand = new Random(RAND_SEED);
        final int stopAt = 4;

        for (int testIteration=0; testIteration < STYLE_TEST_ITERATIONS + 1; testIteration++) {

            System.arraycopy(styles, 0, oldStyles, 0, styles.length);

            int startingAt = Integer.MAX_VALUE;
            int endingAt = Integer.MIN_VALUE;
            int oldTs = text.getTimeStamp();

            // hack way to do an invariant check before starting...
            if (testIteration != 0) {
                // modify styles
                text.resetDamagedRange();
                startingAt = randInt(rand, styles.length+1);
                endingAt = randInt(rand, startingAt, styles.length+1);
                StyleModifier modifier = modifiers[randInt(rand, modifiers.length)];

                if (testIteration == stopAt) {
                    testIteration = stopAt;
                }
                text.modifyCharacterStyles(startingAt, endingAt, modifier);

                for (int j=startingAt; j < endingAt; j++) {
                    styles[j] = modifier.modifyStyle(styles[j]);
                }
            }

            // check invariants
            AttributeMap oldStyle = null;
            int textLength = text.length();
            for (int runStart = 0; runStart < textLength;) {

                AttributeMap currentStyle = text.characterStyleAt(runStart);
                int runLimit = text.characterStyleLimit(runStart);
                if (runStart >= runLimit) {
                    throw new Error("Run length is not positive");
                }
                if (currentStyle.equals(oldStyle)) {
                    throw new Error("Styles didn't merge");
                }

                for (int pos=runStart; pos < runLimit; pos++) {
                    AttributeMap charStyleAtPos = text.characterStyleAt(pos);
                    if (currentStyle != charStyleAtPos) {
                        throw new Error("Iterator style is not equal to text style at " + pos + ".");
                    }
                    AttributeMap expected = styles[pos];
                    if (!currentStyle.equals(expected)) {
                        throw new Error("Iterator style doesn't match expected style at " + pos + ".");
                    }
                    if (!(text.characterStyleStart(pos) == runStart) ||
                            !(text.characterStyleLimit(pos) == runLimit)) {
                        throw new Error("style run start / limit is not consistent");
                    }
                }
                runStart = runLimit;
            }
            if (textLength > 0) {
                if (text.characterStyleAt(textLength) !=
                            text.characterStyleAt(textLength-1)) {
                    throw new Error("Character styles at end aren't the same");
                }
            }

            // check damaged range:
            int damageStart = Integer.MAX_VALUE;
            int damageLimit = Integer.MIN_VALUE;
            for (int i=0; i < textLength; i++) {
                if (!styles[i].equals(oldStyles[i])) {
                    damageStart = Math.min(i, damageStart);
                    damageLimit = Math.max(i+1, damageLimit);
                }
            }
            if (damageStart != text.damagedRangeStart() ||
                            damageLimit != text.damagedRangeLimit()) {
                System.out.println("Test iteration: " + testIteration);
                System.out.println("startingAt: " + startingAt + ";  endingAt: " + endingAt);
                System.out.println("damageStart: " + damageStart + ";  damageLimit: " + damageLimit);
                System.out.println("text.rangeStart: " + text.damagedRangeStart() +
                                   "text.rangeLimit: " + text.damagedRangeLimit());
                throw new Error("Damage range start or limit is not expected value");
            }

            if ((damageLimit == Integer.MIN_VALUE) != (oldTs == text.getTimeStamp())) {

                throw new Error("timeStamp is incorrect");
            }
        }
    }

    /**
    * Perform a random series of operations on an MText and
    * check the result of each operation against a set of invariants.
    */
    public void monkeyTest(boolean streaming) {

        /*
            You can add any operation to the switch statement provided it
            preserves the following invariants:
            - The String plainText contains the same text as the StyledStringBuffer.
              Obviously, for the test to be meaningful plainText must be computed
              independently of the buffer (ie don't write:  plainText = buf.getStyledString().toString()).
            - Every 'b' is bold, every 'i' is italic, every 'p' is plain, and
              no other characters appear in the text.
        */

        AttributeMap boldAttrs = new AttributeMap(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
        AttributeMap italicAttrs = new AttributeMap(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE);
        AttributeMap emptyAttrs = AttributeMap.EMPTY_ATTRIBUTE_MAP;

        final String bold1Str_getString = "b";
        MConstText bold1Str = new StyledText(bold1Str_getString, boldAttrs);

        final String italic1Str_getString = "i";
        MConstText italic1Str = new StyledText(italic1Str_getString, italicAttrs);

        final String plain1Str_getString = "p";
        MConstText plain1Str = new StyledText(plain1Str_getString, emptyAttrs);

        StyledText temp = new StyledText();
        temp.append(bold1Str);
        temp.append(italic1Str);
        final String boldItalicStr_getString = bold1Str_getString.concat(italic1Str_getString);
        MConstText boldItalicStr = temp;

        temp = new StyledText();
        temp.append(bold1Str);
        temp.append(bold1Str);
        temp.append(bold1Str);
        final String bold3Str_getString = "bbb";
        MConstText bold3Str = temp;

        MText buf = new StyledText();
        String plainText = new String();
        int testIteration=0;
        int theCase=0;

        final int NUM_CASES = 14;
        boolean[] casesExecuted = new boolean[NUM_CASES];
        final int stopAt = -1;
        Random rand = new Random(RAND_SEED);

        final String ALWAYS_DIFFERENT = "\uFEFF";

        try {
            for (testIteration=0; testIteration < TEST_ITERATIONS; testIteration++) {

                theCase = randInt(rand, NUM_CASES);

                casesExecuted[theCase] = true;

                if (testIteration == stopAt) {
                    System.out.println("Stop here!");
                }

                int timeStamp = buf.getTimeStamp();
                String oldPlainText = plainText;
                if (oldPlainText == null) {
                    throw new Error("oldPlainText is null!");
                }

                switch (theCase) {

                    case 0:
                        // create new string; replace chars at start with different style
                        buf = new StyledText();
                        buf.append(bold3Str);
                        buf.replace(0, 1, italic1Str, 0, italic1Str.length());
                        buf.replace(0, 0, italic1Str, 0, italic1Str.length());

                        plainText = bold3Str_getString.substring(1, bold3Str.length());
                        plainText = italic1Str_getString.concat(plainText);
                        plainText = italic1Str_getString.concat(plainText);
                        oldPlainText = null;
                        break;

                    case 1:
                        // delete the last character from the string
                        if (buf.length() == 0) {
                            buf.replace(0, 0, italic1Str, 0, italic1Str.length());
                            plainText = italic1Str_getString;
                            oldPlainText = ALWAYS_DIFFERENT;
                        }
                        buf.remove(buf.length()-1, buf.length());
                        plainText = plainText.substring(0, plainText.length()-1);
                        break;

                    case 2:
                        // replace some of the buffer with boldItalicStr
                        int rStart = randInt(rand, buf.length()+1);
                        int rStop = randInt(rand, rStart, buf.length()+1);
                        buf.replace(rStart, rStop, boldItalicStr);
                        {
                            String newString = (rStart>0)? plainText.substring(0, rStart) : new String();
                            newString = newString.concat(boldItalicStr_getString);
                            if (rStop < plainText.length())
                                newString = newString.concat(plainText.substring(rStop, plainText.length()));
                            oldPlainText = ALWAYS_DIFFERENT;
                            plainText = newString;
                        }
                        break;

                    case 3:
                        // repeatedly insert strings into the center of the buffer
                        {
                            int insPos = buf.length() / 2;
                            String prefix = plainText.substring(0, insPos);
                            String suffix = plainText.substring(insPos, plainText.length());
                            String middle = new String();
                            for (int ii=0; ii<4; ii++) {
                                MConstText which = (ii%2==0)? boldItalicStr : bold3Str;
                                String whichString = (ii%2==0)? boldItalicStr_getString : bold3Str_getString;
                                int tempPos = insPos+middle.length();
                                buf.insert(tempPos, which);
                                middle = middle.concat(whichString);
                            }
                            plainText = prefix.concat(middle).concat(suffix);
                            oldPlainText = ALWAYS_DIFFERENT;
                        }
                        break;

                    case 4:
                    // insert bold1Str at end
                        buf.append(bold1Str);
                        plainText = plainText.concat(bold1Str_getString);
                        break;

                    case 5:
                    // delete a character from the string
                        if (buf.length() > 0) {
                            int delPos = randInt(rand, buf.length()-1);
                            buf.remove(delPos, delPos+1);
                            plainText = plainText.substring(0, delPos).concat(plainText.substring(delPos+1));
                        }
                        else {
                            buf.replace(0, 0, plain1Str, 0, plain1Str.length());
                            plainText = plain1Str_getString;
                        }
                        break;

                    case 6:
                    // replace the contents of the buffer (except the first character) with itself
                        {
                            int start = buf.length() > 1? 1 : 0;
                            buf.replace(start, buf.length(), buf);
                            plainText = plainText.substring(0, start).concat(plainText);
                            if (buf.length() > 0) {
                                oldPlainText = ALWAYS_DIFFERENT;
                            }
                        }
                        break;

                    case 7:
                    // append the contents of the buffer to itself
                        {
                            MConstText content = buf;
                            buf.insert(buf.length(), content);
                            plainText = plainText.concat(plainText);
                        }
                        break;

                    case 8:
                    // replace the buffer with boldItalicStr+bold3Str
                        {
                            MText replacement = new StyledText();
                            replacement.append(boldItalicStr);
                            replacement.append(bold3Str);
                            buf.replace(0, buf.length(), replacement, 0, replacement.length());
                            plainText = boldItalicStr_getString.concat(bold3Str_getString);
                            oldPlainText = ALWAYS_DIFFERENT;
                        }
                        break;

                    case 9:
                    // insert bold1Str at end - same as 4 but uses different API
                        buf.replace(buf.length(),
                                    buf.length(),
                                    bold1Str_getString.toCharArray(),
                                    0,
                                    bold1Str_getString.length(),
                                    boldAttrs);
                        plainText = plainText.concat(bold1Str_getString);
                        break;

                    case 10:
                    // remove all
                        buf.remove();
                        plainText = "";
                        oldPlainText = ALWAYS_DIFFERENT;
                        break;

                    case 11:
                    // remove all - different way
                        buf.remove(0, buf.length());
                        plainText = "";
                        break;

                    case 12:
                        // insert 'i' at 3rd character (or last, if fewer than 3 chars)
                        {
                            int insPos = Math.min(buf.length(), 3);
                            buf.replace(insPos, insPos, 'i', italicAttrs);
                            plainText = (plainText.substring(0, insPos)).
                                        concat(italic1Str_getString).
                                        concat(plainText.substring(insPos));
                        }
                        break;

                    case 13:
                        if (streaming) {
                            Throwable error = null;
                            try {
                                ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
                                ObjectOutputStream objOut = new ObjectOutputStream(bytesOut);
                                objOut.writeObject(buf);

                                ByteArrayInputStream bytesIn =
                                                new ByteArrayInputStream(bytesOut.toByteArray());
                                ObjectInputStream objIn = new ObjectInputStream(bytesIn);
                                buf = (MText) objIn.readObject();
                                oldPlainText = null;
                            }
                            catch(IOException e) {
                                error = e;
                            }
                            catch(ClassNotFoundException e) {
                                error = e;
                            }
                            if (error != null) {
                                error.printStackTrace();
                                throw new Error("Streaming problem: " + error);
                            }
                        }
                        break;

                    default:
                        throw new Error("Invalid case.");
                }

                // Check time stamp if oldPlainText != null.
                // Time stamp should be different iff
                // oldPlainText == plainText
                if (oldPlainText != null) {
                    if ((timeStamp==buf.getTimeStamp()) !=
                                    oldPlainText.equals(plainText)) {
                        System.out.println("plainText hashCode: " + plainText.hashCode());
                        System.out.println("oldPlainText hashCode: " + oldPlainText.hashCode());
                        throw new Error("Time stamp is incorrect");
                    }
                }

                // now check invariants:
                if (plainText.length() != buf.length()) {
                    throw new Error("Lengths don't match");
                }

                for (int j=0; j < buf.length(); j++) {
                    if (buf.at(j) != plainText.charAt(j)) {
                        throw new Error("Characters don't match.");
                    }
                }

                int start;
                for (start = 0; start < buf.length();) {

                    if (start != buf.characterStyleStart(start)) {
                        throw new Error("style start is wrong");
                    }
                    int limit = buf.characterStyleLimit(start);
                    if (start >= limit) {
                        throw new Error("start >= limit");
                    }
                    char current = plainText.charAt(start);

                    AttributeMap comp;
                    if (current == 'p') {
                        comp = emptyAttrs;
                    }
                    else if (current == 'b') {
                        comp = boldAttrs;
                    }
                    else if (current == 'i') {
                        comp = italicAttrs;
                    }
                    else {
                        throw new Error("An invalid character snuck in!");
                    }

                    AttributeMap startStyle = buf.characterStyleAt(start);
                    if (!startStyle.equals(comp)) {
                        throw new Error("Style is not expected style.");
                    }

                    for (int j = start; j < limit; j++) {
                        if (plainText.charAt(j) != current) {
                            throw new Error("Character doesn't match style.");
                        }
                        if (buf.characterStyleAt(j) != startStyle) {
                            throw new Error("Incorrect style in run");
                        }
                    }

                    if (limit < buf.length()) {
                        if (plainText.charAt(limit) == current) {
                            throw new Error("Style run ends too soon.");
                        }
                    }
                    start = limit;
                }
                if (start != buf.length()) {
                    throw new Error("Last limit is not buffer length.");
                }

                // won't try to compute and check damaged range;  however,
                // if nonempty it should always be within text
                int damageStart = buf.damagedRangeStart();
                int damageLimit = buf.damagedRangeLimit();
                if (damageStart == Integer.MAX_VALUE) {
                    if (damageLimit != Integer.MIN_VALUE) {
                        throw new Error("Invalid empty interval");
                    }
                }
                else {
                    if (damageStart > damageLimit) {
                        throw new Error("Damage range inverted");
                    }
                    if (damageStart < 0 || damageLimit > buf.length()) {
                        throw new Error("Damage range endpoint out of bounds");
                    }
                }
            }
        }
        catch(Error e) {
            System.out.println("Iteration=" + testIteration + ";  case=" + theCase);
            throw e;
        }

        boolean allCasesExecuted = true;
        for (int index=0; index < NUM_CASES; index++) {
            allCasesExecuted &= casesExecuted[index];
            if (casesExecuted[index] == false) {
                System.out.println("Case " + index + " not executed.");
            }
        }
        //if (allCasesExecuted) {
        //    System.out.println("All cases executed.");
        //}
    }
}