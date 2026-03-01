// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.format;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.text.AttributedCharacterIterator;
import java.text.Format;
import java.text.Format.Field;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.ibm.icu.text.ConstrainedFieldPosition;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.FormattedValue;
import com.ibm.icu.text.NumberFormat;

/**
 * @author sffc
 */
public class FormattedValueTest {
    @Test
    public void testBasic() {
        ConstrainedFieldPosition cfpos = new ConstrainedFieldPosition();
        assertAllPartsEqual(
            "basic",
            cfpos,
            15,
            null,
            null,
            0,
            0,
            0L);
    }

    @Test
    public void testSetters() {
        ConstrainedFieldPosition cfpos = new ConstrainedFieldPosition();

        cfpos.constrainField(NumberFormat.Field.COMPACT);
        assertAllPartsEqual(
            "setters 1",
            cfpos,
            10,
            NumberFormat.Field.COMPACT,
            null,
            0,
            0,
            0L);

        cfpos.constrainFieldAndValue(NumberFormat.Field.COMPACT, 42);
        assertAllPartsEqual(
            "setters 1.2",
            cfpos,
            8,
            NumberFormat.Field.COMPACT,
            42,
            0,
            0,
            0L);

        cfpos.constrainClass(NumberFormat.Field.class);
        assertAllPartsEqual(
            "setters 1.5",
            cfpos,
            11,
            null,
            null,
            0,
            0,
            0L);

        cfpos.setInt64IterationContext(42424242424242L);
        assertAllPartsEqual(
            "setters 2",
            cfpos,
            11,
            null,
            null,
            0,
            0,
            42424242424242L);

        cfpos.setState(NumberFormat.Field.COMPACT, BigDecimal.ONE, 5, 10);
        assertAllPartsEqual(
            "setters 3",
            cfpos,
            11,
            NumberFormat.Field.COMPACT,
            BigDecimal.ONE,
            5,
            10,
            42424242424242L);

        cfpos.reset();
        assertAllPartsEqual(
            "setters 4",
            cfpos,
            15,
            null,
            null,
            0,
            0,
            0L);
    }

    @Test
    public void testIllegalArgumentException() {
        ConstrainedFieldPosition cfpos = new ConstrainedFieldPosition();
        try {
            cfpos.matchesField(null, null);
            fail("Expected an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }

    private void assertAllPartsEqual(
            String messagePrefix,
            ConstrainedFieldPosition cfpos,
            int matching,
            Field field,
            Object value,
            int start,
            int limit,
            long context) {
        assertEquals(messagePrefix + ": field", field, cfpos.getField());
        assertEquals(messagePrefix + ": field value", value, cfpos.getFieldValue());
        assertEquals(messagePrefix + ": start", start, cfpos.getStart());
        assertEquals(messagePrefix + ": limit", limit, cfpos.getLimit());
        assertEquals(messagePrefix + ": context", context, cfpos.getInt64IterationContext());

        assertEquals(messagePrefix + ": integer field",
            ((matching & 1) != 0), cfpos.matchesField(NumberFormat.Field.INTEGER, null));
        assertEquals(messagePrefix + ": compact field",
            ((matching & 2) != 0), cfpos.matchesField(NumberFormat.Field.COMPACT, null));
        assertEquals(messagePrefix + ": date field",
            ((matching & 4) != 0), cfpos.matchesField(DateFormat.Field.AM_PM, null));
        assertEquals(messagePrefix + ": compact field with value",
            ((matching & 8) != 0), cfpos.matchesField(NumberFormat.Field.COMPACT, 42));
    }

    public static void checkFormattedValue(String message, FormattedValue fv, String expectedString,
            Object[][] expectedFieldPositions) {
        checkFormattedValue(message, fv, expectedString, expectedFieldPositions, false);
    }

    public static void checkFormattedValue(String message, FormattedValue fv, String expectedString,
            Object[][] expectedFieldPositions, boolean skipAttributedCharacterIterator) {
        // Calculate some initial expected values
        int stringLength = fv.toString().length();
        HashSet<Format.Field> uniqueFields = new HashSet<>();
        Set<Class<?>> uniqueFieldClasses = new HashSet<>();
        for (int i=0; i<expectedFieldPositions.length; i++) {
            uniqueFields.add((Format.Field) expectedFieldPositions[i][0]);
            uniqueFieldClasses.add(expectedFieldPositions[i][0].getClass());
        }
        String baseMessage = message + ": " + fv.toString() + ": ";

        // Check the String and CharSequence
        assertEquals(baseMessage + " string", expectedString, fv.toString());
        assertCharSequenceEquals(expectedString, fv);

        // Check the AttributedCharacterIterator
        AttributedCharacterIterator fpi = fv.toCharacterIterator();
        Set<AttributedCharacterIterator.Attribute> allAttributes = fpi.getAllAttributeKeys();
        assertEquals(baseMessage + "All known fields should be in the iterator", uniqueFields.size(), allAttributes.size());
        assertEquals(baseMessage + "Iterator should have length of string output", stringLength, fpi.getEndIndex());
        int i = 0;
        for (char c = fpi.first(); c != AttributedCharacterIterator.DONE; c = fpi.next(), i++) {
            // Strings with adjacent fields cannot be disambiguated using AttributedCharacterIterator,
            // so skip this part of the test for strings with adjacent fields.
            if (skipAttributedCharacterIterator) {
                i = stringLength;
                break;
            }
            Set<AttributedCharacterIterator.Attribute> currentAttributes = fpi.getAttributes().keySet();
            int attributesRemaining = currentAttributes.size();
            for (Object[] cas : expectedFieldPositions) {
                Format.Field expectedField = (Format.Field) cas[0];
                int expectedBeginIndex = (Integer) cas[1];
                int expectedEndIndex = (Integer) cas[2];
                Object expectedValue = cas.length == 4 ? cas[3] : expectedField;
                if (expectedBeginIndex > i || expectedEndIndex <= i) {
                    // Field position does not overlap with the current character
                    continue;
                }

                assertTrue(baseMessage + "A Character at " + i + " should have field " + expectedField,
                        currentAttributes.contains(expectedField));
                assertTrue(baseMessage + "A Field " + expectedField + " should be a known attribute",
                        allAttributes.contains(expectedField));
                int actualBeginIndex = fpi.getRunStart(expectedField);
                int actualEndIndex = fpi.getRunLimit(expectedField);
                Object actualValue = fpi.getAttribute(expectedField);
                assertEquals(baseMessage + expectedField + " A begin @" + i, expectedBeginIndex, actualBeginIndex);
                assertEquals(baseMessage + expectedField + " A end @" + i, expectedEndIndex, actualEndIndex);
                assertEquals(baseMessage + expectedField + " A value @" + i, expectedValue, actualValue);
                attributesRemaining--;
            }
            assertEquals(baseMessage + "A Should have looked at every field: " + i + ": " + currentAttributes,
                    0, attributesRemaining);
        }
        assertEquals(baseMessage + "A Should have looked at every character", stringLength, i);

        // Check nextPosition over all fields
        ConstrainedFieldPosition cfpos = new ConstrainedFieldPosition();
        i = 0;
        for (Object[] cas : expectedFieldPositions) {
            assertTrue(baseMessage + i, fv.nextPosition(cfpos));
            Format.Field expectedField = (Format.Field) cas[0];
            int expectedStart = (Integer) cas[1];
            int expectedLimit = (Integer) cas[2];
            Object expectedValue = cas.length == 4 ? cas[3] : null;
            assertEquals(baseMessage + "B field " + i, expectedField, cfpos.getField());
            assertEquals(baseMessage + "B start " + i, expectedStart, cfpos.getStart());
            assertEquals(baseMessage + "B limit " + i, expectedLimit, cfpos.getLimit());
            assertEquals(baseMessage + "B value " + i, expectedValue, cfpos.getFieldValue());
            i++;
        }
        boolean afterLoopResult = fv.nextPosition(cfpos);
        assertFalse(baseMessage + "B after loop: " + cfpos, afterLoopResult);
        afterLoopResult = fv.nextPosition(cfpos);
        assertFalse(baseMessage + "B after loop again: " + cfpos, afterLoopResult);

        // Check nextPosition constrained over each class one at a time
        for (Class<?> classConstraint : uniqueFieldClasses) {
            cfpos.reset();
            cfpos.constrainClass(classConstraint);
            i = 0;
            for (Object[] cas : expectedFieldPositions) {
                if (cas[0].getClass() != classConstraint) {
                    continue;
                }
                assertTrue(baseMessage + i, fv.nextPosition(cfpos));
                Format.Field expectedField = (Format.Field) cas[0];
                int expectedStart = (Integer) cas[1];
                int expectedLimit = (Integer) cas[2];
                Object expectedValue = cas.length == 4 ? cas[3] : null;
                assertEquals(baseMessage + "C field " + i, expectedField, cfpos.getField());
                assertEquals(baseMessage + "C start " + i, expectedStart, cfpos.getStart());
                assertEquals(baseMessage + "C limit " + i, expectedLimit, cfpos.getLimit());
                assertEquals(baseMessage + "C value " + i, expectedValue, cfpos.getFieldValue());
                i++;
            }
            afterLoopResult = fv.nextPosition(cfpos);
            assertFalse(baseMessage + "C after loop: " + cfpos, afterLoopResult);
            afterLoopResult = fv.nextPosition(cfpos);
            assertFalse(baseMessage + "C after loop again: " + cfpos, afterLoopResult);
        }

        // Check nextPosition constrained over an unrelated class
        cfpos.reset();
        cfpos.constrainClass(HashSet.class);
        assertFalse(baseMessage + "C unrelated class", fv.nextPosition(cfpos));

        // Check nextPosition constrained over each field one at a time
        for (Format.Field field : uniqueFields) {
            cfpos.reset();
            cfpos.constrainField(field);
            i = 0;
            for (Object[] cas : expectedFieldPositions) {
                if (cas[0] != field) {
                    continue;
                }
                assertTrue(baseMessage + i, fv.nextPosition(cfpos));
                Format.Field expectedField = (Format.Field) cas[0];
                int expectedStart = (Integer) cas[1];
                int expectedLimit = (Integer) cas[2];
                Object expectedValue = cas.length == 4 ? cas[3] : null;
                assertEquals(baseMessage + "D field " + i, expectedField, cfpos.getField());
                assertEquals(baseMessage + "D start " + i, expectedStart, cfpos.getStart());
                assertEquals(baseMessage + "D limit " + i, expectedLimit, cfpos.getLimit());
                assertEquals(baseMessage + "D value " + i, expectedValue, cfpos.getFieldValue());
                i++;
            }
            afterLoopResult = fv.nextPosition(cfpos);
            assertFalse(baseMessage + "D after loop: " + cfpos, afterLoopResult);
            afterLoopResult = fv.nextPosition(cfpos);
            assertFalse(baseMessage + "D after loop again: " + cfpos, afterLoopResult);
        }
    }

    public static void assertCharSequenceEquals(CharSequence a, CharSequence b) {
        assertEquals(a.toString(), b.toString());

        assertEquals(a.length(), b.length());
        for (int i = 0; i < a.length(); i++) {
            assertEquals(a.charAt(i), b.charAt(i));
        }

        int start = Math.min(2, a.length());
        int end = Math.min(8, a.length());
        if (start != end) {
            assertCharSequenceEquals(a.subSequence(start, end), b.subSequence(start, end));
        }
    }
}
