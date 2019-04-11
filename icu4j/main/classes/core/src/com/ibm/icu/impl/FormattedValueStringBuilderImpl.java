// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl;

import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.FieldPosition;
import java.text.Format.Field;

import com.ibm.icu.text.ConstrainedFieldPosition;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UnicodeSet;

/**
 * Implementation of FormattedValue based on FormattedStringBuilder.
 *
 * The implementation currently revolves around numbers and number fields.
 * However, it can be generalized in the future when there is a need.
 *
 * In C++, this implements FormattedValue. In Java, it is a stateless
 * collection of static functions to avoid having to use nested objects.
 *
 * @author sffc (Shane Carr)
 */
public class FormattedValueStringBuilderImpl {


    public static boolean nextFieldPosition(FormattedStringBuilder self, FieldPosition fp) {
        java.text.Format.Field rawField = fp.getFieldAttribute();

        if (rawField == null) {
            // Backwards compatibility: read from fp.getField()
            if (fp.getField() == NumberFormat.INTEGER_FIELD) {
                rawField = NumberFormat.Field.INTEGER;
            } else if (fp.getField() == NumberFormat.FRACTION_FIELD) {
                rawField = NumberFormat.Field.FRACTION;
            } else {
                // No field is set
                return false;
            }
        }

        if (!(rawField instanceof NumberFormat.Field)) {
            throw new IllegalArgumentException(
                    "You must pass an instance of com.ibm.icu.text.NumberFormat.Field as your FieldPosition attribute.  You passed: "
                            + rawField.getClass().toString());
        }

        ConstrainedFieldPosition cfpos = new ConstrainedFieldPosition();
        cfpos.constrainField(rawField);
        cfpos.setState(rawField, null, fp.getBeginIndex(), fp.getEndIndex());
        if (nextPosition(self, cfpos, null)) {
            fp.setBeginIndex(cfpos.getStart());
            fp.setEndIndex(cfpos.getLimit());
            return true;
        }

        // Special case: fraction should start after integer if fraction is not present
        if (rawField == NumberFormat.Field.FRACTION && fp.getEndIndex() == 0) {
            boolean inside = false;
            int i = self.zero;
            for (; i < self.zero + self.length; i++) {
                if (isIntOrGroup(self.fields[i]) || self.fields[i] == NumberFormat.Field.DECIMAL_SEPARATOR) {
                    inside = true;
                } else if (inside) {
                    break;
                }
            }
            fp.setBeginIndex(i - self.zero);
            fp.setEndIndex(i - self.zero);
        }

        return false;
    }

    public static AttributedCharacterIterator toCharacterIterator(FormattedStringBuilder self, Field numericField) {
        ConstrainedFieldPosition cfpos = new ConstrainedFieldPosition();
        AttributedString as = new AttributedString(self.toString());
        while (nextPosition(self, cfpos, numericField)) {
            // Backwards compatibility: field value = field
            as.addAttribute(cfpos.getField(), cfpos.getField(), cfpos.getStart(), cfpos.getLimit());
        }
        return as.getIterator();
    }

    static class NullField extends Field {
        private static final long serialVersionUID = 1L;
        static final NullField END = new NullField("end");
        private NullField(String name) {
            super(name);
        }
    }

    /**
     * Implementation of nextPosition consistent with the contract of FormattedValue.
     *
     * @param cfpos
     *            The argument passed to the public API.
     * @param numericField
     *            Optional. If non-null, apply this field to the entire numeric portion of the string.
     * @return See FormattedValue#nextPosition.
     */
    public static boolean nextPosition(FormattedStringBuilder self, ConstrainedFieldPosition cfpos, Field numericField) {
        int fieldStart = -1;
        Field currField = null;
        for (int i = self.zero + cfpos.getLimit(); i <= self.zero + self.length; i++) {
            Field _field = (i < self.zero + self.length) ? self.fields[i] : NullField.END;
            // Case 1: currently scanning a field.
            if (currField != null) {
                if (currField != _field) {
                    int end = i - self.zero;
                    // Grouping separators can be whitespace; don't throw them out!
                    if (currField != NumberFormat.Field.GROUPING_SEPARATOR) {
                        end = trimBack(self, end);
                    }
                    if (end <= fieldStart) {
                        // Entire field position is ignorable; skip.
                        fieldStart = -1;
                        currField = null;
                        i--;  // look at this index again
                        continue;
                    }
                    int start = fieldStart;
                    if (currField != NumberFormat.Field.GROUPING_SEPARATOR) {
                        start = trimFront(self, start);
                    }
                    cfpos.setState(currField, null, start, end);
                    return true;
                }
                continue;
            }
            // Special case: coalesce the INTEGER if we are pointing at the end of the INTEGER.
            if (cfpos.matchesField(NumberFormat.Field.INTEGER, null)
                    && i > self.zero
                    // don't return the same field twice in a row:
                    && i - self.zero > cfpos.getLimit()
                    && isIntOrGroup(self.fields[i - 1])
                    && !isIntOrGroup(_field)) {
                int j = i - 1;
                for (; j >= self.zero && isIntOrGroup(self.fields[j]); j--) {}
                cfpos.setState(NumberFormat.Field.INTEGER, null, j - self.zero + 1, i - self.zero);
                return true;
            }
            // Special case: coalesce NUMERIC if we are pointing at the end of the NUMERIC.
            if (numericField != null
                    && cfpos.matchesField(numericField, null)
                    && i > self.zero
                    // don't return the same field twice in a row:
                    && (i - self.zero > cfpos.getLimit() || cfpos.getField() != numericField)
                    && isNumericField(self.fields[i - 1])
                    && !isNumericField(_field)) {
                int j = i - 1;
                for (; j >= self.zero && isNumericField(self.fields[j]); j--) {}
                cfpos.setState(numericField, null, j - self.zero + 1, i - self.zero);
                return true;
            }
            // Special case: skip over INTEGER; will be coalesced later.
            if (_field == NumberFormat.Field.INTEGER) {
                _field = null;
            }
            // Case 2: no field starting at this position.
            if (_field == null || _field == NullField.END) {
                continue;
            }
            // Case 3: check for field starting at this position
            if (cfpos.matchesField(_field, null)) {
                fieldStart = i - self.zero;
                currField = _field;
            }
        }

        assert currField == null;
        return false;
    }

    private static boolean isIntOrGroup(Field field) {
        return field == NumberFormat.Field.INTEGER || field == NumberFormat.Field.GROUPING_SEPARATOR;
    }

    private static boolean isNumericField(Field field) {
        return field == null || NumberFormat.Field.class.isAssignableFrom(field.getClass());
    }

    private static int trimBack(FormattedStringBuilder self, int limit) {
        return StaticUnicodeSets.get(StaticUnicodeSets.Key.DEFAULT_IGNORABLES)
                .spanBack(self, limit, UnicodeSet.SpanCondition.CONTAINED);
    }

    private static int trimFront(FormattedStringBuilder self, int start) {
        return StaticUnicodeSets.get(StaticUnicodeSets.Key.DEFAULT_IGNORABLES)
                .span(self, start, UnicodeSet.SpanCondition.CONTAINED);
    }
}
