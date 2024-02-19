// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl;

import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.FieldPosition;
import java.text.Format;
import java.util.List;

import com.ibm.icu.text.ConstrainedFieldPosition;

/**
 * Implementation of FormattedValue based on FieldPositionIterator.
 *
 * In C++, this implements FormattedValue. In Java, it is a stateless
 * collection of static functions to avoid having to use nested objects.
 */
public class FormattedValueFieldPositionIteratorImpl {

    /** Do not construct instances of this class */
    private FormattedValueFieldPositionIteratorImpl() {}

    /** Helper class to keep track of fields with values in Java */
    private static class FieldWithValue extends Format.Field {
        private static final long serialVersionUID = -3850076447157793465L;

        public final Format.Field field;
        public final int value;

        public FieldWithValue(Format.Field field, int value) {
            super(field.toString());
            this.field = field;
            this.value = value;
        }
    }

    public static boolean nextPosition(List<FieldPosition> attributes, ConstrainedFieldPosition cfpos) {
        int numFields = attributes.size();
        int i = (int) cfpos.getInt64IterationContext();
        for (; i < numFields; i++) {
            FieldPosition fpos = attributes.get(i);
            Format.Field field = fpos.getFieldAttribute();
            Object value = null;
            if (field instanceof FieldWithValue) {
                value = ((FieldWithValue) field).value;
                field = ((FieldWithValue) field).field;
            }
            if (cfpos.matchesField(field, value)) {
                int start = fpos.getBeginIndex();
                int limit = fpos.getEndIndex();
                cfpos.setState(field, value, start, limit);
                break;
            }
        }
        cfpos.setInt64IterationContext(i == numFields ? i : i + 1);
        return i < numFields;
    }

    public static AttributedCharacterIterator toCharacterIterator(CharSequence cs, List<FieldPosition> attributes) {
        AttributedString as = new AttributedString(cs.toString());

        // add attributes to the AttributedString
        for (int i = 0; i < attributes.size(); i++) {
            FieldPosition fp = attributes.get(i);
            Format.Field field = fp.getFieldAttribute();
            Object value = field;
            if (field instanceof FieldWithValue) {
                value = ((FieldWithValue) field).value;
                field = ((FieldWithValue) field).field;
            }
            as.addAttribute(field, value, fp.getBeginIndex(), fp.getEndIndex());
        }

        // return the CharacterIterator from AttributedString
        return as.getIterator();
    }

    public static void addOverlapSpans(List<FieldPosition> attributes, Format.Field spanField, int firstIndex) {
        // In order to avoid fancy data structures, this is an O(N^2) algorithm,
        // which should be fine for all real-life applications of this function.
        int s1a = Integer.MAX_VALUE;
        int s1b = 0;
        int s2a = Integer.MAX_VALUE;
        int s2b = 0;
        int numFields = attributes.size();
        for (int i = 0; i<numFields; i++) {
            FieldPosition fp1 = attributes.get(i);
            for (int j = i + 1; j<numFields; j++) {
                FieldPosition fp2 = attributes.get(j);
                if (fp1.getFieldAttribute() != fp2.getFieldAttribute()) {
                    continue;
                }
                // Found a duplicate
                s1a = Math.min(s1a, fp1.getBeginIndex());
                s1b = Math.max(s1b, fp1.getEndIndex());
                s2a = Math.min(s2a, fp2.getBeginIndex());
                s2b = Math.max(s2b, fp2.getEndIndex());
                break;
            }
        }
        if (s1a != Integer.MAX_VALUE) {
            // Success: add the two span fields
            FieldPosition newPos = new FieldPosition(new FieldWithValue(spanField, firstIndex));
            newPos.setBeginIndex(s1a);
            newPos.setEndIndex(s1b);
            attributes.add(newPos);
            newPos = new FieldPosition(new FieldWithValue(spanField, 1 - firstIndex));
            newPos.setBeginIndex(s2a);
            newPos.setEndIndex(s2b);
            attributes.add(newPos);
        }
    }

    public static void sort(List<FieldPosition> attributes) {
        // Use bubble sort, O(N^2) but easy and no fancy data structures.
        int numFields = attributes.size();
        while (true) {
            boolean isSorted = true;
            for (int i=0; i<numFields-1; i++) {
                FieldPosition fp1 = attributes.get(i);
                FieldPosition fp2 = attributes.get(i + 1);
                long comparison = 0;
                if (fp1.getBeginIndex() != fp2.getBeginIndex()) {
                    // Higher start index -> higher rank
                    comparison = fp2.getBeginIndex() - fp1.getBeginIndex();
                } else if (fp1.getEndIndex() != fp2.getEndIndex()) {
                    // Higher length (end index) -> lower rank
                    comparison = fp1.getEndIndex() - fp2.getEndIndex();
                } else if (fp1.getFieldAttribute() != fp2.getFieldAttribute()) {
                    // Span category -> lower rank
                    // Pick other orders arbitrarily
                    boolean fp1isSpan = fp1.getFieldAttribute() instanceof FieldWithValue;
                    boolean fp2isSpan = fp2.getFieldAttribute() instanceof FieldWithValue;
                    if (fp1isSpan && !fp2isSpan) {
                        comparison = 1;
                    } else if (fp2isSpan && !fp1isSpan) {
                        comparison = -1;
                    } else {
                        comparison = fp1.hashCode() - fp2.hashCode();
                    }
                }
                if (comparison < 0) {
                    // Perform a swap
                    isSorted = false;
                    attributes.set(i, fp2);
                    attributes.set(i + 1, fp1);
                }
            }
            if (isSorted) {
                break;
            }
        }
    }
}
