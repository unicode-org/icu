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

import java.io.*;
import java.awt.Color;

import com.ibm.richtext.styledtext.MText;
import com.ibm.richtext.styledtext.StandardTabRuler;
import com.ibm.richtext.styledtext.StyledText;
import com.ibm.richtext.styledtext.StyleModifier;

import com.ibm.richtext.textlayout.attributes.AttributeMap;
import com.ibm.richtext.textlayout.attributes.TextAttribute;

public class TestMTextStreaming extends TestFmwk {

    static final String COPYRIGHT =
                "(C) Copyright IBM Corp. 1998-1999 - All Rights Reserved";
                
    public static void main(String[] args) throws Exception {

        new TestMTextStreaming().run(args);
    }

    public TestMTextStreaming() {
    }

    public void test() {

        simpleTest();
        allAttributesTest();
    }

    private void simpleTest() {

        AttributeMap style = AttributeMap.EMPTY_ATTRIBUTE_MAP;
        MText text = new StyledText("Hello world!", style);

        streamAndCompare(text);
    }

    private static class TestModifier extends StyleModifier {

        private Object fKey;
        private Object fValue;

        public AttributeMap modifyStyle(AttributeMap style) {

            return style.addAttribute(fKey, fValue);
        }

        TestModifier(Object key, Object value) {

            fKey = key;
            fValue = value;
        }
    }

    private void allAttributesTest() {

        AttributeMap style = AttributeMap.EMPTY_ATTRIBUTE_MAP;
        MText text = new StyledText("Hello world!", style);

        int length = text.length();

        final boolean CHARACTER = true;
        final boolean PARAGRAPH = false;

        addStyle(text, 0, length/2, TextAttribute.FAMILY, "Times", CHARACTER);
        addStyle(text, length/2, length, TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD, CHARACTER);
        addStyle(text, 0, length/2, TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE, CHARACTER);
        addStyle(text, 0, length/2, TextAttribute.SIZE, new Float(13.7f), CHARACTER);
        addStyle(text, length/2, length, TextAttribute.SUPERSCRIPT, TextAttribute.SUPERSCRIPT_SUB, CHARACTER);
        addStyle(text, 0, length/2, TextAttribute.FOREGROUND, Color.blue, CHARACTER);
        addStyle(text, 0, length/2, TextAttribute.BACKGROUND, Color.red, CHARACTER);
        addStyle(text, 0, length-1, TextAttribute.STRIKETHROUGH, Boolean.TRUE, CHARACTER);

        addStyle(text, 0, length, TextAttribute.EXTRA_LINE_SPACING, new Float(4), PARAGRAPH);
        addStyle(text, 0, length, TextAttribute.FIRST_LINE_INDENT, new Float(6), PARAGRAPH);
        addStyle(text, 0, length, TextAttribute.MIN_LINE_SPACING, new Float(7), PARAGRAPH);
        addStyle(text, 0, length, TextAttribute.LINE_FLUSH, TextAttribute.FLUSH_TRAILING, PARAGRAPH);
        addStyle(text, 0, length, TextAttribute.LEADING_MARGIN, new Float(9), PARAGRAPH);
        addStyle(text, 0, length, TextAttribute.TRAILING_MARGIN, new Float(9), PARAGRAPH);
        addStyle(text, 0, length, TextAttribute.TAB_RULER, new StandardTabRuler(), PARAGRAPH);

        streamAndCompare(text);
    }

    private static void addStyle(MText text,
                                 int start,
                                 int limit,
                                 Object key,
                                 Object value,
                                 boolean character) {

        StyleModifier modifier = new TestModifier(key, value);

        if (character) {
            text.modifyCharacterStyles(start, limit, modifier);
        }
        else {
            text.modifyParagraphStyles(start, limit, modifier);
        }
    }

    public void streamAndCompare(MText text) {

        Throwable error = null;

        try {
            ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
            ObjectOutputStream objOut = new ObjectOutputStream(bytesOut);
            objOut.writeObject(text);

            ByteArrayInputStream bytesIn =
                            new ByteArrayInputStream(bytesOut.toByteArray());
            ObjectInputStream objIn = new ObjectInputStream(bytesIn);
            MText streamedText = (MText) objIn.readObject();
            if (!isEqual(text, streamedText)) {
                isEqual(text, streamedText);
                errln("Streamed text is not equal");
            }
        }
/*        catch(OptionalDataException e) {
            error = e;
        }
        catch(StreamCorruptedException e) {
            error = e;
        }*/
        catch(IOException e) {
            error = e;
        }
        catch(ClassNotFoundException e) {
            error = e;
        }

        if (error != null) {
            error.printStackTrace();
            errln("Serialization failed.");
        }
    }

    public static boolean isEqual(MText lhs, MText rhs) {

        return lhs.equals(rhs);
    }
}