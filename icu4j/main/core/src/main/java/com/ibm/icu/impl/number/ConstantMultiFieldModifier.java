// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number;

import java.text.Format.Field;
import java.util.Arrays;

import com.ibm.icu.impl.FormattedStringBuilder;

/**
 * An implementation of {@link Modifier} that allows for multiple types of fields in the same modifier.
 * Constructed based on the contents of two {@link FormattedStringBuilder} instances (one for the prefix,
 * one for the suffix).
 */
public class ConstantMultiFieldModifier implements Modifier {

    // NOTE: In Java, these are stored as array pointers. In C++, the FormattedStringBuilder is stored by
    // value and is treated internally as immutable.
    protected final char[] prefixChars;
    protected final char[] suffixChars;
    protected final Object[] prefixFields;
    protected final Object[] suffixFields;
    private final boolean overwrite;
    private final boolean strong;

    // Parameters: used for number range formatting
    private final Parameters parameters;

    public ConstantMultiFieldModifier(
            FormattedStringBuilder prefix,
            FormattedStringBuilder suffix,
            boolean overwrite,
            boolean strong) {
        this(prefix, suffix, overwrite, strong, null);
    }

    public ConstantMultiFieldModifier(
            FormattedStringBuilder prefix,
            FormattedStringBuilder suffix,
            boolean overwrite,
            boolean strong,
            Parameters parameters) {
        prefixChars = prefix.toCharArray();
        suffixChars = suffix.toCharArray();
        prefixFields = prefix.toFieldArray();
        suffixFields = suffix.toFieldArray();
        this.overwrite = overwrite;
        this.strong = strong;
        this.parameters = parameters;
    }

    @Override
    public int apply(FormattedStringBuilder output, int leftIndex, int rightIndex) {
        int length = output.insert(leftIndex, prefixChars, prefixFields);
        if (overwrite) {
            length += output.splice(leftIndex + length, rightIndex + length, "", 0, 0, null);
        }
        length += output.insert(rightIndex + length, suffixChars, suffixFields);
        return length;
    }

    @Override
    public int getPrefixLength() {
        return prefixChars.length;
    }

    @Override
    public int getCodePointCount() {
        return Character.codePointCount(prefixChars, 0, prefixChars.length)
                + Character.codePointCount(suffixChars, 0, suffixChars.length);
    }

    @Override
    public boolean isStrong() {
        return strong;
    }

    @Override
    public boolean containsField(Field field) {
        for (int i = 0; i < prefixFields.length; i++) {
            if (prefixFields[i] == field) {
                return true;
            }
        }
        for (int i = 0; i < suffixFields.length; i++) {
            if (suffixFields[i] == field) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Parameters getParameters() {
        return parameters;
    }

    @Override
    public boolean strictEquals(Modifier other) {
        if (!(other instanceof ConstantMultiFieldModifier)) {
            return false;
        }
        ConstantMultiFieldModifier _other = (ConstantMultiFieldModifier) other;
        return Arrays.equals(prefixChars, _other.prefixChars) && Arrays.equals(prefixFields, _other.prefixFields)
                && Arrays.equals(suffixChars, _other.suffixChars) && Arrays.equals(suffixFields, _other.suffixFields)
                && overwrite == _other.overwrite && strong == _other.strong;
    }

    @Override
    public String toString() {
        FormattedStringBuilder temp = new FormattedStringBuilder();
        apply(temp, 0, 0);
        int prefixLength = getPrefixLength();
        return String.format("<ConstantMultiFieldModifier prefix:'%s' suffix:'%s'>",
                temp.subSequence(0, prefixLength),
                temp.subSequence(prefixLength, temp.length()));
    }
}
