/*
 *******************************************************************************
 * Copyright (C) 2014-2016, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.impl;

/**
 * Formats simple patterns like "{1} was born in {0}".
 * Minimal subset of MessageFormat; fast, simple, minimal dependencies.
 * Supports only numbered arguments with no type nor style parameters,
 * and formats only string values.
 * Quoting via ASCII apostrophe compatible with ICU MessageFormat default behavior.
 *
 * <p>Factory methods throw exceptions for syntax errors
 * and for too few or too many arguments/placeholders.
 *
 * <p>SimplePatternFormatter objects are immutable and can be safely cached like strings.
 *
 * <p>Example:
 * <pre>
 * SimplePatternFormatter fmt = SimplePatternFormatter.compile("{1} '{born}' in {0}");
 *
 * // Output: "paul {born} in england"
 * System.out.println(fmt.format("england", "paul"));
 * </pre>
 *
 * @see com.ibm.icu.text.MessageFormat
 * @see com.ibm.icu.text.MessagePattern.ApostropheMode
 */
public final class SimplePatternFormatter {
    // For internal use in Java,
    // it is most efficient to compile patterns to compiled-pattern strings
    // and use them with static methods.
    //
    // If and when we make this public API,
    // we should probably make only the non-static methods public
    // and keep the static ones in an impl class.
    //
    // TODO: Consider changing methods & docs to use "argument" not "placeholder",
    // consistent with MessageFormat.

    /**
     * Argument numbers must be smaller than this limit.
     * Text segment lengths are offset by this much.
     * This is currently the only unused char value in compiled patterns,
     * except it is the maximum value of the first unit (max arg +1).
     */
    private static final int ARG_NUM_LIMIT = 0x100;
    /**
     * Initial and maximum char/UChar value set for a text segment.
     * Segment length char values are from ARG_NUM_LIMIT+1 to this value here.
     * Normally 0xffff, but can be as small as ARG_NUM_LIMIT+1 for testing.
     */
    private static final char SEGMENT_LENGTH_PLACEHOLDER_CHAR = (char)0xffff;
    /**
     * Maximum length of a text segment. Longer segments are split into shorter ones.
     */
    private static final int MAX_SEGMENT_LENGTH = SEGMENT_LENGTH_PLACEHOLDER_CHAR - ARG_NUM_LIMIT;

    /**
     * Binary representation of the compiled pattern.
     * Index 0: One more than the highest argument number.
     * Followed by zero or more arguments or literal-text segments.
     *
     * <p>An argument is stored as its number, less than ARG_NUM_LIMIT.
     * A literal-text segment is stored as its length (at least 1) offset by ARG_NUM_LIMIT,
     * followed by that many chars.
     */
    private final String compiledPattern;

    private SimplePatternFormatter(String compiledPattern) {
        this.compiledPattern = compiledPattern;
    }

    /**
     * Creates a formatter from the pattern string.
     *
     * @param pattern The pattern string.
     * @return The new SimplePatternFormatter object.
     */
    public static SimplePatternFormatter compile(CharSequence pattern) {
        return compileMinMaxPlaceholders(pattern, 0, Integer.MAX_VALUE);
    }

    /**
     * Creates a formatter from the pattern string.
     *
     * @param pattern The pattern string.
     * @param min The pattern must have at least this many placeholders.
     * @param max The pattern must have at most this many placeholders.
     * @return The new SimplePatternFormatter object.
     */
    public static SimplePatternFormatter compileMinMaxPlaceholders(CharSequence pattern, int min, int max) {
        StringBuilder sb = new StringBuilder();
        String compiledPattern = compileToStringMinMaxPlaceholders(pattern, sb, min, max);
        return new SimplePatternFormatter(compiledPattern);
    }

    /**
     * Creates a compiled form of the pattern string, for use with appropriate static methods.
     *
     * @param pattern The pattern string.
     * @param min The pattern must have at least this many placeholders.
     * @param max The pattern must have at most this many placeholders.
     * @return The compiled-pattern string.
     */
    public static String compileToStringMinMaxPlaceholders(
            CharSequence pattern, StringBuilder sb, int min, int max) {
        // Parse consistent with MessagePattern, but
        // - support only simple numbered arguments
        // - build a simple binary structure into the result string
        int patternLength = pattern.length();
        sb.ensureCapacity(patternLength);
        // Reserve the first char for the number of arguments.
        sb.setLength(1);
        int textLength = 0;
        int maxArg = -1;
        boolean inQuote = false;
        for (int i = 0; i < patternLength;) {
            char c = pattern.charAt(i++);
            if (c == '\'') {
                if (i < patternLength && (c = pattern.charAt(i)) == '\'') {
                    // double apostrophe, skip the second one
                    ++i;
                } else if (inQuote) {
                    // skip the quote-ending apostrophe
                    inQuote = false;
                    continue;
                } else if (c == '{' || c == '}') {
                    // Skip the quote-starting apostrophe, find the end of the quoted literal text.
                    ++i;
                    inQuote = true;
                } else {
                    // The apostrophe is part of literal text.
                    c = '\'';
                }
            } else if (!inQuote && c == '{') {
                if (textLength > 0) {
                    sb.setCharAt(sb.length() - textLength - 1, (char)(ARG_NUM_LIMIT + textLength));
                    textLength = 0;
                }
                int argNumber;
                if ((i + 1) < patternLength &&
                        0 <= (argNumber = pattern.charAt(i) - '0') && argNumber <= 9 &&
                        pattern.charAt(i + 1) == '}') {
                    i += 2;
                } else {
                    // Multi-digit argument number (no leading zero) or syntax error.
                    // MessagePattern permits PatternProps.skipWhiteSpace(pattern, index)
                    // around the number, but this class does not.
                    int argStart = i - 1;
                    argNumber = -1;
                    if (i < patternLength && '1' <= (c = pattern.charAt(i++)) && c <= '9') {
                        argNumber = c - '0';
                        while (i < patternLength && '0' <= (c = pattern.charAt(i++)) && c <= '9') {
                            argNumber = argNumber * 10 + (c - '0');
                            if (argNumber >= ARG_NUM_LIMIT) {
                                break;
                            }
                        }
                    }
                    if (argNumber < 0 || c != '}') {
                        throw new IllegalArgumentException(
                                "Argument syntax error in pattern \"" + pattern +
                                "\" at index " + argStart +
                                ": " + pattern.subSequence(argStart, i));
                    }
                }
                if (argNumber > maxArg) {
                    maxArg = argNumber;
                }
                sb.append((char)argNumber);
                continue;
            }  // else: c is part of literal text
            // Append c and track the literal-text segment length.
            if (textLength == 0) {
                // Reserve a char for the length of a new text segment, preset the maximum length.
                sb.append(SEGMENT_LENGTH_PLACEHOLDER_CHAR);
            }
            sb.append(c);
            if (++textLength == MAX_SEGMENT_LENGTH) {
                textLength = 0;
            }
        }
        if (textLength > 0) {
            sb.setCharAt(sb.length() - textLength - 1, (char)(ARG_NUM_LIMIT + textLength));
        }
        int argCount = maxArg + 1;
        if (argCount < min) {
            throw new IllegalArgumentException(
                    "Fewer than minimum " + min + " placeholders in pattern \"" + pattern + "\"");
        }
        if (argCount > max) {
            throw new IllegalArgumentException(
                    "More than maximum " + max + " placeholders in pattern \"" + pattern + "\"");
        }
        sb.setCharAt(0, (char)argCount);
        return sb.toString();
    }

    /**
     * @return The max argument number/placeholder ID + 1.
     */
    public int getPlaceholderCount() {
        return getPlaceholderCount(compiledPattern);
    }

    /**
     * @param compiledPattern Compiled form of a pattern string.
     * @return The max argument number/placeholder ID + 1.
     */
    public static int getPlaceholderCount(String compiledPattern) {
        return compiledPattern.charAt(0);
    }

    /**
     * Formats the given values.
     */
    public String format(CharSequence... values) {
        return formatCompiledPattern(compiledPattern, values);
    }

    /**
     * Formats the given values.
     *
     * @param compiledPattern Compiled form of a pattern string.
     */
    public static String formatCompiledPattern(String compiledPattern, CharSequence... values) {
        return formatAndAppend(compiledPattern, new StringBuilder(), null, values).toString();
    }

    /**
     * Formats the given values, appending to the appendTo builder.
     *
     * @param appendTo Gets the formatted pattern and values appended.
     * @param offsets offsets[i] receives the offset of where
     *                values[i] replaced pattern argument {i}.
     *                Can be null, or can be shorter or longer than values.
     *                If there is no {i} in the pattern, then offsets[i] is set to -1.
     * @param values The placeholder values.
     *               A placeholder value must not be the same object as appendTo.
     *               values.length must be at least getPlaceholderCount().
     *               Can be null if getPlaceholderCount()==0.
     * @return appendTo
     */
    public StringBuilder formatAndAppend(
            StringBuilder appendTo, int[] offsets, CharSequence... values) {
        return formatAndAppend(compiledPattern, appendTo, offsets, values);
    }

    /**
     * Formats the given values, appending to the appendTo builder.
     *
     * @param compiledPattern Compiled form of a pattern string.
     * @param appendTo Gets the formatted pattern and values appended.
     * @param offsets offsets[i] receives the offset of where
     *                values[i] replaced pattern argument {i}.
     *                Can be null, or can be shorter or longer than values.
     *                If there is no {i} in the pattern, then offsets[i] is set to -1.
     * @param values The placeholder values.
     *               A placeholder value must not be the same object as appendTo.
     *               values.length must be at least getPlaceholderCount().
     *               Can be null if getPlaceholderCount()==0.
     * @return appendTo
     */
    public static StringBuilder formatAndAppend(
            String compiledPattern, StringBuilder appendTo, int[] offsets, CharSequence... values) {
        int valuesLength = values != null ? values.length : 0;
        if (valuesLength < getPlaceholderCount(compiledPattern)) {
            throw new IllegalArgumentException("Too few values.");
        }
        return format(compiledPattern, values, appendTo, null, true, offsets);
    }

    /**
     * Formats the given values, replacing the contents of the result builder.
     * May optimize by actually appending to the result if it is the same object
     * as the initial argument's corresponding value.
     *
     * @param result Gets its contents replaced by the formatted pattern and values.
     * @param offsets offsets[i] receives the offset of where
     *                values[i] replaced pattern argument {i}.
     *                Can be null, or can be shorter or longer than values.
     *                If there is no {i} in the pattern, then offsets[i] is set to -1.
     * @param values The placeholder values.
     *               A placeholder value may be the same object as result.
     *               values.length must be at least getPlaceholderCount().
     * @return result
     */
    public StringBuilder formatAndReplace(
            StringBuilder result, int[] offsets, CharSequence... values) {
        return formatAndReplace(compiledPattern, result, offsets, values);
    }

    /**
     * Formats the given values, replacing the contents of the result builder.
     * May optimize by actually appending to the result if it is the same object
     * as the initial argument's corresponding value.
     *
     * @param compiledPattern Compiled form of a pattern string.
     * @param result Gets its contents replaced by the formatted pattern and values.
     * @param offsets offsets[i] receives the offset of where
     *                values[i] replaced pattern argument {i}.
     *                Can be null, or can be shorter or longer than values.
     *                If there is no {i} in the pattern, then offsets[i] is set to -1.
     * @param values The placeholder values.
     *               A placeholder value may be the same object as result.
     *               values.length must be at least getPlaceholderCount().
     * @return result
     */
    public static StringBuilder formatAndReplace(
            String compiledPattern, StringBuilder result, int[] offsets, CharSequence... values) {
        int valuesLength = values != null ? values.length : 0;
        if (valuesLength < getPlaceholderCount(compiledPattern)) {
            throw new IllegalArgumentException("Too few values.");
        }

        // If the pattern starts with an argument whose value is the same object
        // as the result, then we keep the result contents and append to it.
        // Otherwise we replace its contents.
        int firstArg = -1;
        // If any non-initial argument value is the same object as the result,
        // then we first copy its contents and use that instead while formatting.
        String resultCopy = null;
        if (getPlaceholderCount(compiledPattern) > 0) {
            for (int i = 1; i < compiledPattern.length();) {
                int n = compiledPattern.charAt(i++);
                if (n < ARG_NUM_LIMIT) {
                    if (values[n] == result) {
                        if (i == 2) {
                            firstArg = n;
                        } else if (resultCopy == null) {
                            resultCopy = result.toString();
                        }
                    }
                } else {
                    i += n - ARG_NUM_LIMIT;
                }
            }
        }
        if (firstArg < 0) {
            result.setLength(0);
        }
        return format(compiledPattern, values, result, resultCopy, false, offsets);
    }

    /**
     * Returns a string similar to the original pattern, only for debugging.
     */
    @Override
    public String toString() {
        String[] values = new String[getPlaceholderCount()];
        for (int i = 0; i < values.length; i++) {
            values[i] = String.format("{%d}", i);
        }
        return formatAndAppend(new StringBuilder(), null, values).toString();
    }

    /**
     * Returns the pattern text with none of the placeholders.
     * Like formatting with all-empty string values.
     */
    public String getTextWithNoPlaceholders() {
        return getTextWithNoPlaceholders(compiledPattern);
    }

    /**
     * Returns the pattern text with none of the placeholders.
     * Like formatting with all-empty string values.
     *
     * @param compiledPattern Compiled form of a pattern string.
     */
    public static String getTextWithNoPlaceholders(String compiledPattern) {
        int capacity = compiledPattern.length() - 1 - getPlaceholderCount(compiledPattern);
        StringBuilder sb = new StringBuilder(capacity);
        for (int i = 1; i < compiledPattern.length();) {
            int segmentLength = compiledPattern.charAt(i++) - ARG_NUM_LIMIT;
            if (segmentLength > 0) {
                int limit = i + segmentLength;
                sb.append(compiledPattern, i, limit);
                i = limit;
            }
        }
        return sb.toString();
    }

    private static StringBuilder format(
            String compiledPattern, CharSequence[] values,
            StringBuilder result, String resultCopy, boolean forbidResultAsValue,
            int[] offsets) {
        int offsetsLength;
        if (offsets == null) {
            offsetsLength = 0;
        } else {
            offsetsLength = offsets.length;
            for (int i = 0; i < offsetsLength; i++) {
                offsets[i] = -1;
            }
        }
        for (int i = 1; i < compiledPattern.length();) {
            int n = compiledPattern.charAt(i++);
            if (n < ARG_NUM_LIMIT) {
                CharSequence value = values[n];
                if (value == result) {
                    if (forbidResultAsValue) {
                        throw new IllegalArgumentException("Value must not be same object as result");
                    }
                    if (i == 2) {
                        // We are appending to result which is also the first value object.
                        if (n < offsetsLength) {
                            offsets[n] = 0;
                        }
                    } else {
                        if (n < offsetsLength) {
                            offsets[n] = result.length();
                        }
                        result.append(resultCopy);
                    }
                } else {
                    if (n < offsetsLength) {
                        offsets[n] = result.length();
                    }
                    result.append(value);
                }
            } else {
                int limit = i + (n - ARG_NUM_LIMIT);
                result.append(compiledPattern, i, limit);
                i = limit;
            }
        }
        return result;
    }
}
