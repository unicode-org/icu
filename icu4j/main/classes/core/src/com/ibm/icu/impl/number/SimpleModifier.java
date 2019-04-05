// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number;

import java.text.Format.Field;

import com.ibm.icu.impl.FormattedStringBuilder;
import com.ibm.icu.impl.SimpleFormatterImpl;
import com.ibm.icu.impl.number.range.PrefixInfixSuffixLengthHelper;
import com.ibm.icu.util.ICUException;

/**
 * The second primary implementation of {@link Modifier}, this one consuming a
 * {@link com.ibm.icu.text.SimpleFormatter} pattern.
 */
public class SimpleModifier implements Modifier {
    private final String compiledPattern;
    private final Field field;
    private final boolean strong;
    private final int prefixLength;
    private final int suffixOffset;
    private final int suffixLength;

    // Parameters: used for number range formatting
    private final Parameters parameters;

    /** TODO: This is copied from SimpleFormatterImpl. */
    private static final int ARG_NUM_LIMIT = 0x100;

    /** Creates a modifier that uses the SimpleFormatter string formats. */
    public SimpleModifier(String compiledPattern, Field field, boolean strong) {
        this(compiledPattern, field, strong, null);
    }

    /** Creates a modifier that uses the SimpleFormatter string formats. */
    public SimpleModifier(String compiledPattern, Field field, boolean strong, Parameters parameters) {
        assert compiledPattern != null;
        this.compiledPattern = compiledPattern;
        this.field = field;
        this.strong = strong;
        this.parameters = parameters;

        int argLimit = SimpleFormatterImpl.getArgumentLimit(compiledPattern);
        if (argLimit == 0) {
            // No arguments in compiled pattern
            prefixLength = compiledPattern.charAt(1) - ARG_NUM_LIMIT;
            assert 2 + prefixLength == compiledPattern.length();
            // Set suffixOffset = -1 to indicate no arguments in compiled pattern.
            suffixOffset = -1;
            suffixLength = 0;
        } else {
            assert argLimit == 1;
            if (compiledPattern.charAt(1) != '\u0000') {
                prefixLength = compiledPattern.charAt(1) - ARG_NUM_LIMIT;
                suffixOffset = 3 + prefixLength;
            } else {
                prefixLength = 0;
                suffixOffset = 2;
            }
            if (3 + prefixLength < compiledPattern.length()) {
                suffixLength = compiledPattern.charAt(suffixOffset) - ARG_NUM_LIMIT;
            } else {
                suffixLength = 0;
            }
        }
    }

    @Override
    public int apply(FormattedStringBuilder output, int leftIndex, int rightIndex) {
        return formatAsPrefixSuffix(output, leftIndex, rightIndex);
    }

    @Override
    public int getPrefixLength() {
        return prefixLength;
    }

    @Override
    public int getCodePointCount() {
        int count = 0;
        if (prefixLength > 0) {
            count += Character.codePointCount(compiledPattern, 2, 2 + prefixLength);
        }
        if (suffixLength > 0) {
            count += Character
                    .codePointCount(compiledPattern, 1 + suffixOffset, 1 + suffixOffset + suffixLength);
        }
        return count;
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
        return parameters;
    }

    @Override
    public boolean semanticallyEquivalent(Modifier other) {
        if (!(other instanceof SimpleModifier)) {
            return false;
        }
        SimpleModifier _other = (SimpleModifier) other;
        if (parameters != null && _other.parameters != null && parameters.obj == _other.parameters.obj) {
            return true;
        }
        return compiledPattern.equals(_other.compiledPattern) && field == _other.field && strong == _other.strong;
    }

    /**
     * TODO: This belongs in SimpleFormatterImpl. The only reason I haven't moved it there yet is because
     * DoubleSidedStringBuilder is an internal class and SimpleFormatterImpl feels like it should not
     * depend on it.
     *
     * <p>
     * Formats a value that is already stored inside the StringBuilder <code>result</code> between the
     * indices <code>startIndex</code> and <code>endIndex</code> by inserting characters before the start
     * index and after the end index.
     *
     * <p>
     * This is well-defined only for patterns with exactly one argument.
     *
     * @param result
     *            The StringBuilder containing the value argument.
     * @param startIndex
     *            The left index of the value within the string builder.
     * @param endIndex
     *            The right index of the value within the string builder.
     * @return The number of characters (UTF-16 code points) that were added to the StringBuilder.
     */
    public int formatAsPrefixSuffix(
            FormattedStringBuilder result,
            int startIndex,
            int endIndex) {
        if (suffixOffset == -1) {
            // There is no argument for the inner number; overwrite the entire segment with our string.
            return result.splice(startIndex, endIndex, compiledPattern, 2, 2 + prefixLength, field);
        } else {
            if (prefixLength > 0) {
                result.insert(startIndex, compiledPattern, 2, 2 + prefixLength, field);
            }
            if (suffixLength > 0) {
                result.insert(endIndex + prefixLength,
                        compiledPattern,
                        1 + suffixOffset,
                        1 + suffixOffset + suffixLength,
                        field);
            }
            return prefixLength + suffixLength;
        }
    }

    /**
     * TODO: Like above, this belongs with the rest of the SimpleFormatterImpl code.
     * I put it here so that the SimpleFormatter uses in FormattedStringBuilder are near each other.
     *
     * <p>
     * Applies the compiled two-argument pattern to the FormattedStringBuilder.
     *
     * <p>
     * This method is optimized for the case where the prefix and suffix are often empty, such as
     * in the range pattern like "{0}-{1}".
     */
    public static void formatTwoArgPattern(String compiledPattern, FormattedStringBuilder result, int index, PrefixInfixSuffixLengthHelper h,
            Field field) {
        int argLimit = SimpleFormatterImpl.getArgumentLimit(compiledPattern);
        if (argLimit != 2) {
            throw new ICUException();
        }
        int offset = 1; // offset into compiledPattern
        int length = 0; // chars added to result

        int prefixLength = compiledPattern.charAt(offset);
        offset++;
        if (prefixLength < ARG_NUM_LIMIT) {
            // No prefix
            prefixLength = 0;
        } else {
            prefixLength -= ARG_NUM_LIMIT;
            result.insert(index + length, compiledPattern, offset, offset + prefixLength, field);
            offset += prefixLength;
            length += prefixLength;
            offset++;
        }

        int infixLength = compiledPattern.charAt(offset);
        offset++;
        if (infixLength < ARG_NUM_LIMIT) {
            // No infix
            infixLength = 0;
        } else {
            infixLength -= ARG_NUM_LIMIT;
            result.insert(index + length, compiledPattern, offset, offset + infixLength, field);
            offset += infixLength;
            length += infixLength;
            offset++;
        }

        int suffixLength;
        if (offset == compiledPattern.length()) {
            // No suffix
            suffixLength = 0;
        } else {
            suffixLength = compiledPattern.charAt(offset) -  ARG_NUM_LIMIT;
            offset++;
            result.insert(index + length, compiledPattern, offset, offset + suffixLength, field);
            length += suffixLength;
        }

        h.lengthPrefix = prefixLength;
        h.lengthInfix = infixLength;
        h.lengthSuffix = suffixLength;
    }
}
