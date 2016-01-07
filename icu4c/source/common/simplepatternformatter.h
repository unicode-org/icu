/*
******************************************************************************
* Copyright (C) 2014-2016, International Business Machines
* Corporation and others.  All Rights Reserved.
******************************************************************************
* simplepatternformatter.h
*/

#ifndef __SIMPLEPATTERNFORMATTER_H__
#define __SIMPLEPATTERNFORMATTER_H__ 

#include "unicode/utypes.h"
#include "unicode/unistr.h"

U_NAMESPACE_BEGIN

/**
 * Formats simple patterns like "{1} was born in {0}".
 * Minimal subset of MessageFormat; fast, simple, minimal dependencies.
 * Supports only numbered arguments with no type nor style parameters,
 * and formats only string values.
 * Quoting via ASCII apostrophe compatible with ICU MessageFormat default behavior.
 *
 * Factory methods throw exceptions for syntax errors
 * and for too few or too many arguments/placeholders.
 *
 * SimplePatternFormatter objects are immutable and can be safely cached like strings.
 *
 * Example:
 * <pre>
 * UErrorCode errorCode = U_ZERO_ERROR;
 * SimplePatternFormatter fmt("{1} '{born}' in {0}", errorCode);
 * UnicodeString result;
 *
 * // Output: "paul {born} in england"
 * fmt.format("england", "paul", result, errorCode);
 * </pre>
 *
 * @see MessageFormat
 * @see UMessagePatternApostropheMode
 */
class U_COMMON_API SimplePatternFormatter : public UMemory {
public:
    /**
     * Default constructor.
     */
    SimplePatternFormatter() : compiledPattern((UChar)0) {}

    /**
     * Constructs a formatter from the pattern string.
     *
     * @param pattern The pattern string.
     */
    explicit SimplePatternFormatter(const UnicodeString& pattern, UErrorCode &errorCode) {
        compile(pattern, errorCode);
    }

    /**
     * Constructs a formatter from the pattern string.
     *
     * @param pattern The pattern string.
     * @param min The pattern must have at least this many placeholders.
     * @param max The pattern must have at most this many placeholders.
     */
    SimplePatternFormatter(const UnicodeString& pattern, int32_t min, int32_t max,
                           UErrorCode &errorCode) {
        compileMinMaxPlaceholders(pattern, min, max, errorCode);
    }

    /**
     * Copy constructor.
     */
    SimplePatternFormatter(const SimplePatternFormatter& other)
            : compiledPattern(other.compiledPattern) {}

    /**
     * Assignment operator.
     */
    SimplePatternFormatter &operator=(const SimplePatternFormatter& other);

    /**
     * Destructor.
     */
    ~SimplePatternFormatter();

    /**
     * Changes this object according to the new pattern.
     *
     * @param pattern The pattern string.
     * @return TRUE if U_SUCCESS(errorCode).
     */
    UBool compile(const UnicodeString &pattern, UErrorCode &errorCode) {
        return compileMinMaxPlaceholders(pattern, 0, INT32_MAX, errorCode);
    }

    /**
     * Changes this object according to the new pattern.
     *
     * @param pattern The pattern string.
     * @param min The pattern must have at least this many placeholders.
     * @param max The pattern must have at most this many placeholders.
     * @return TRUE if U_SUCCESS(errorCode).
     */
    UBool compileMinMaxPlaceholders(const UnicodeString &pattern,
                                    int32_t min, int32_t max, UErrorCode &errorCode);

    /**
     * @return The max argument number/placeholder ID + 1.
     */
    int32_t getPlaceholderCount() const {
        return getPlaceholderCount(compiledPattern.getBuffer(), compiledPattern.length());
    }

    /**
     * Formats the given value, appending to the appendTo builder.
     * The placeholder value must not be the same object as appendTo.
     * getPlaceholderCount() must be at most 1.
     *
     * @param value0 Value for argument {0}.
     * @param appendTo Gets the formatted pattern and value appended.
     * @param errorCode ICU error code in/out parameter.
     *                  Must fulfill U_SUCCESS before the function call.
     * @return appendTo
     */
    UnicodeString &format(
            const UnicodeString &value0,
            UnicodeString &appendTo, UErrorCode &errorCode) const;

    /**
     * Formats the given values, appending to the appendTo builder.
     * A placeholder value must not be the same object as appendTo.
     * getPlaceholderCount() must be at most 2.
     *
     * @param value0 Value for argument {0}.
     * @param value1 Value for argument {1}.
     * @param appendTo Gets the formatted pattern and values appended.
     * @param errorCode ICU error code in/out parameter.
     *                  Must fulfill U_SUCCESS before the function call.
     * @return appendTo
     */
    UnicodeString &format(
            const UnicodeString &value0,
            const UnicodeString &value1,
            UnicodeString &appendTo, UErrorCode &errorCode) const;

    /**
     * Formats the given values, appending to the appendTo builder.
     * A placeholder value must not be the same object as appendTo.
     * getPlaceholderCount() must be at most 3.
     *
     * @param value0 Value for argument {0}.
     * @param value1 Value for argument {1}.
     * @param value2 Value for argument {2}.
     * @param appendTo Gets the formatted pattern and values appended.
     * @param errorCode ICU error code in/out parameter.
     *                  Must fulfill U_SUCCESS before the function call.
     * @return appendTo
     */
    UnicodeString &format(
            const UnicodeString &value0,
            const UnicodeString &value1,
            const UnicodeString &value2,
            UnicodeString &appendTo, UErrorCode &errorCode) const;

    /**
     * Formats the given values, appending to the appendTo string.
     *
     * @param values The placeholder values.
     *               A placeholder value must not be the same object as appendTo.
     *               Can be NULL if valuesLength==getPlaceholderCount()==0.
     * @param valuesLength The length of the values array.
     *                     Must be at least getPlaceholderCount().
     * @param appendTo Gets the formatted pattern and values appended.
     * @param offsets offsets[i] receives the offset of where
     *                values[i] replaced pattern argument {i}.
     *                Can be shorter or longer than values. Can be NULL if offsetsLength==0.
     *                If there is no {i} in the pattern, then offsets[i] is set to -1.
     * @param offsetsLength The length of the offsets array.
     * @param errorCode ICU error code in/out parameter.
     *                  Must fulfill U_SUCCESS before the function call.
     * @return appendTo
     */
    UnicodeString &formatAndAppend(
            const UnicodeString *const *values, int32_t valuesLength,
            UnicodeString &appendTo,
            int32_t *offsets, int32_t offsetsLength, UErrorCode &errorCode) const;

    /**
     * Formats the given values, replacing the contents of the result string.
     * May optimize by actually appending to the result if it is the same object
     * as the initial argument's corresponding value.
     *
     * @param values The placeholder values.
     *               A placeholder value may be the same object as result.
     *               Can be NULL if valuesLength==getPlaceholderCount()==0.
     * @param valuesLength The length of the values array.
     *                     Must be at least getPlaceholderCount().
     * @param result Gets its contents replaced by the formatted pattern and values.
     * @param offsets offsets[i] receives the offset of where
     *                values[i] replaced pattern argument {i}.
     *                Can be shorter or longer than values. Can be NULL if offsetsLength==0.
     *                If there is no {i} in the pattern, then offsets[i] is set to -1.
     * @param offsetsLength The length of the offsets array.
     * @param errorCode ICU error code in/out parameter.
     *                  Must fulfill U_SUCCESS before the function call.
     * @return result
     */
    UnicodeString &formatAndReplace(
            const UnicodeString *const *values, int32_t valuesLength,
            UnicodeString &result,
            int32_t *offsets, int32_t offsetsLength, UErrorCode &errorCode) const;

    /**
     * Returns the pattern text with none of the placeholders.
     * Like formatting with all-empty string values.
     */
    UnicodeString getTextWithNoPlaceholders() const {
        return getTextWithNoPlaceholders(compiledPattern.getBuffer(), compiledPattern.length());
    }

private:
    /**
     * Binary representation of the compiled pattern.
     * Index 0: One more than the highest argument number.
     * Followed by zero or more arguments or literal-text segments.
     *
     * An argument is stored as its number, less than ARG_NUM_LIMIT.
     * A literal-text segment is stored as its length (at least 1) offset by ARG_NUM_LIMIT,
     * followed by that many chars.
     */
    UnicodeString compiledPattern;

    static inline int32_t getPlaceholderCount(const UChar *compiledPattern,
                                              int32_t compiledPatternLength) {
        return compiledPatternLength == 0 ? 0 : compiledPattern[0];
    }

    static UnicodeString getTextWithNoPlaceholders(const UChar *compiledPattern, int32_t compiledPatternLength);

    static UnicodeString &format(
            const UChar *compiledPattern, int32_t compiledPatternLength,
            const UnicodeString *const *values,
            UnicodeString &result, const UnicodeString *resultCopy, UBool forbidResultAsValue,
            int32_t *offsets, int32_t offsetsLength,
            UErrorCode &errorCode);
};

U_NAMESPACE_END

#endif
