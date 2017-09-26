// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number;

import com.ibm.icu.text.NumberFormat.Field;

/**
 * An implementation of {@link Modifier} that allows for multiple types of fields in the same modifier. Constructed
 * based on the contents of two {@link NumberStringBuilder} instances (one for the prefix, one for the suffix).
 */
public class ConstantMultiFieldModifier implements Modifier {

    // NOTE: In Java, these are stored as array pointers. In C++, the NumberStringBuilder is stored by
    // value and is treated internally as immutable.
    protected final char[] prefixChars;
    protected final char[] suffixChars;
    protected final Field[] prefixFields;
    protected final Field[] suffixFields;
    private final boolean strong;

    public ConstantMultiFieldModifier(NumberStringBuilder prefix, NumberStringBuilder suffix, boolean strong) {
        prefixChars = prefix.toCharArray();
        suffixChars = suffix.toCharArray();
        prefixFields = prefix.toFieldArray();
        suffixFields = suffix.toFieldArray();
        this.strong = strong;
    }

    @Override
    public int apply(NumberStringBuilder output, int leftIndex, int rightIndex) {
        // Insert the suffix first since inserting the prefix will change the rightIndex
        int length = output.insert(rightIndex, suffixChars, suffixFields);
        length += output.insert(leftIndex, prefixChars, prefixFields);
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
    public String toString() {
        NumberStringBuilder temp = new NumberStringBuilder();
        apply(temp, 0, 0);
        int prefixLength = getPrefixLength();
        return String.format("<ConstantMultiFieldModifier prefix:'%s' suffix:'%s'>", temp.subSequence(0, prefixLength),
                temp.subSequence(prefixLength, temp.length()));
    }
}
