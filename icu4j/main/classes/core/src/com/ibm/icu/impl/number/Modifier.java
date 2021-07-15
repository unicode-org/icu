// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.number;

import java.text.Format.Field;

import com.ibm.icu.impl.FormattedStringBuilder;
import com.ibm.icu.impl.StandardPlural;

/**
 * A Modifier is an object that can be passed through the formatting pipeline until it is finally applied
 * to the string builder. A Modifier usually contains a prefix and a suffix that are applied, but it
 * could contain something else, like a {@link com.ibm.icu.text.SimpleFormatter} pattern.
 *
 * A Modifier is usually immutable, except in cases such as {@link MutablePatternModifier}, which are
 * mutable for performance reasons.
 */
public interface Modifier {

    static enum Signum {
        NEG,
        NEG_ZERO,
        POS_ZERO,
        POS;

        static final int COUNT = Signum.values().length;
    };

    /**
     * Apply this Modifier to the string builder.
     *
     * @param output
     *            The string builder to which to apply this modifier.
     * @param leftIndex
     *            The left index of the string within the builder. Equal to 0 when only one number is
     *            being formatted.
     * @param rightIndex
     *            The right index of the string within the string builder. Equal to length when only one
     *            number is being formatted.
     * @return The number of characters (UTF-16 code units) that were added to the string builder.
     */
    public int apply(FormattedStringBuilder output, int leftIndex, int rightIndex);

    /**
     * Gets the length of the prefix. This information can be used in combination with {@link #apply} to
     * extract the prefix and suffix strings.
     *
     * @return The number of characters (UTF-16 code units) in the prefix.
     */
    public int getPrefixLength();

    /**
     * Returns the number of code points in the modifier, prefix plus suffix.
     */
    public int getCodePointCount();

    /**
     * Whether this modifier is strong. If a modifier is strong, it should always be applied immediately
     * and not allowed to bubble up. With regard to padding, strong modifiers are considered to be on the
     * inside of the prefix and suffix.
     *
     * @return Whether the modifier is strong.
     */
    public boolean isStrong();

    /**
     * Whether the modifier contains at least one occurrence of the given field.
     */
    public boolean containsField(Field currency);

    /**
     * A fill-in for getParameters(). obj will always be set; if non-null, the other
     * two fields are also safe to read.
     */
    public static class Parameters {
        public ModifierStore obj;
        public Signum signum;
        public StandardPlural plural;
    }

    /**
     * Gets a set of "parameters" for this Modifier.
     */
    public Parameters getParameters();

    /**
     * Returns whether this Modifier is *semantically equivalent* to the other Modifier;
     * in many cases, this is the same as equal, but parameters should be ignored.
     */
    public boolean semanticallyEquivalent(Modifier other);
}
