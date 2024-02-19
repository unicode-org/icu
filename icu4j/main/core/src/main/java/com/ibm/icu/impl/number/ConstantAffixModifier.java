// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number;

import java.text.Format.Field;

import com.ibm.icu.impl.FormattedStringBuilder;

/**
 * The canonical implementation of {@link Modifier}, containing a prefix and suffix string.
 */
public class ConstantAffixModifier implements Modifier {

    // TODO: Avoid making a new instance by default if prefix and suffix are empty
    public static final ConstantAffixModifier EMPTY = new ConstantAffixModifier();

    private final String prefix;
    private final String suffix;
    private final Field field;
    private final boolean strong;

    /**
     * Constructs an instance with the given strings.
     *
     * <p>
     * The arguments need to be Strings, not CharSequences, because Strings are immutable but
     * CharSequences are not.
     *
     * @param prefix
     *            The prefix string.
     * @param suffix
     *            The suffix string.
     * @param field
     *            The field type to be associated with this modifier. Can be null.
     * @param strong
     *            Whether this modifier should be strongly applied.
     * @see Field
     */
    public ConstantAffixModifier(String prefix, String suffix, Field field, boolean strong) {
        // Use an empty string instead of null if we are given null
        // TODO: Consider returning a null modifier if both prefix and suffix are empty.
        this.prefix = (prefix == null ? "" : prefix);
        this.suffix = (suffix == null ? "" : suffix);
        this.field = field;
        this.strong = strong;
    }

    /** Constructs a new instance with an empty prefix, suffix, and field. */
    public ConstantAffixModifier() {
        prefix = "";
        suffix = "";
        field = null;
        strong = false;
    }

    @Override
    public int apply(FormattedStringBuilder output, int leftIndex, int rightIndex) {
        // Insert the suffix first since inserting the prefix will change the rightIndex
        int length = output.insert(rightIndex, suffix, field);
        length += output.insert(leftIndex, prefix, field);
        return length;
    }

    @Override
    public int getPrefixLength() {
        return prefix.length();
    }

    @Override
    public int getCodePointCount() {
        return prefix.codePointCount(0, prefix.length()) + suffix.codePointCount(0, suffix.length());
    }

    @Override
    public boolean isStrong() {
        return strong;
    }

    @Override
    public boolean containsField(Field field) {
        // This method is not currently used.
        assert false;
        return false;
    }

    @Override
    public Parameters getParameters() {
        return null;
    }

    @Override
    public boolean semanticallyEquivalent(Modifier other) {
        if (!(other instanceof ConstantAffixModifier)) {
            return false;
        }
        ConstantAffixModifier _other = (ConstantAffixModifier) other;
        return prefix.equals(_other.prefix) && suffix.equals(_other.suffix) && field == _other.field
                && strong == _other.strong;
    }

    @Override
    public String toString() {
        return String.format("<ConstantAffixModifier prefix:'%s' suffix:'%s'>", prefix, suffix);
    }
}
