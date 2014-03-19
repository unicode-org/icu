/*
******************************************************************************
* Copyright (C) 2014, International Business Machines
* Corporation and others.  All Rights Reserved.
******************************************************************************
* simplepatternformatter.h
*/

#ifndef __SIMPLEPATTERNFORMATTER_H__
#define __SIMPLEPATTERNFORMATTER_H__ 

#define EXPECTED_PLACEHOLDER_COUNT 3

#include "unicode/utypes.h"
#include "unicode/unistr.h"

U_NAMESPACE_BEGIN

/**
 * Compiled version of a pattern string such as "{1} was born in {0}".
 * <p>
 * Using SimplePatternFormatter is both faster and safer than adhoc replacement.
 * They are faster because they are precompiled; they are safer because they
 * account for curly braces escaped by apostrophe (').
 * 
 * Placeholders are of the form \{[0-9]+\}. If a curly brace is preceded
 * by a single quote, it becomes a curly brace instead of the start of a
 * placeholder. Two single quotes resolve to one single quote. 
 * <p>
 * Example:
 * <pre>
 * SimplePatternFormatter fmt("{1} '{born} in {0}");
 * UnicodeString result;
 * UErrorCode status = U_ZERO_ERROR;
 * // Evaluates to: "paul {born} in england"
 * fmt.format("englad", "paul", result, status);
 * </pre>
 */
class U_COMMON_API SimplePatternFormatter : public UMemory {
public:
    /**
     * Default constructor
     */
    SimplePatternFormatter();

    /**
     * Construct from a pattern. Will never fail if pattern has three or
     * fewer placeholders in it.
     */
    explicit SimplePatternFormatter(const UnicodeString& pattern);

    /**
     * Copy constructor.
     */
    SimplePatternFormatter(const SimplePatternFormatter& other);

    /**
     * Assignment operator
     */
    SimplePatternFormatter &operator=(const SimplePatternFormatter& other);

    /**
     * Destructor.
     */
    ~SimplePatternFormatter();

    /**
     * Compiles pattern and makes this object represent pattern.
     *
     * Returns TRUE on success; FALSE on failure. Will not fail if
     * there are three or fewer placeholders in pattern. May fail with
     * U_MEMORY_ALLOCATION_ERROR if there are more than three placeholders.
     */
    UBool compile(const UnicodeString &pattern, UErrorCode &status);

    /**
     * Returns (maxPlaceholderId + 1). For example
     * <code>SimplePatternFormatter("{0} {2}").getPlaceholderCount()
     * evaluates to 3.
     * Callers use this function to find out how many values this object
     * expects when formatting.
     */
    int32_t getPlaceholderCount() const {
        return placeholderCount;
    }

    /**
     * Formats given value.
     */
    UnicodeString &format(
            const UnicodeString &args0,
            UnicodeString &appendTo,
            UErrorCode &status) const;
    
    /**
     * Formats given values.
     */
    UnicodeString &format(
            const UnicodeString &args0,
            const UnicodeString &args1,
            UnicodeString &appendTo,
            UErrorCode &status) const;
    
    /**
     * Formats given values.
     */
    UnicodeString &format(
            const UnicodeString &args0,
            const UnicodeString &args1,
            const UnicodeString &args2,
            UnicodeString &appendTo,
            UErrorCode &status) const;
    
    /**
     * Formats given values.
     *
     * The caller retains ownership of all pointers.
     * @param placeholderValues 1st one corresponds to {0}; 2nd to {1};
     *  3rd to {2} etc.
     * @param placeholderValueCount the number of placeholder values
     *  must be at least large enough to provide values for all placeholders
     *  in this object. Otherwise status set to U_ILLEGAL_ARGUMENT_ERROR.
     * @param appendTo resulting string appended here.
     * @param offsetArray The offset of each placeholder value in appendTo
     *  stored here. The first value gets the offset of the value for {0};
     *  the 2nd for {1}; the 3rd for {2} etc. -1 means that the corresponding
     *  placeholder does not exist in this object. If caller is not
     *  interested in offsets, it may pass NULL and 0 for the length.
     * @param offsetArrayLength the size of offsetArray may be less than
     *  placeholderValueCount.
     * @param status any error stored here.
     */
    UnicodeString &format(
            const UnicodeString * const *placeholderValues,
            int32_t placeholderValueCount,
            UnicodeString &appendTo,
            int32_t *offsetArray,
            int32_t offsetArrayLength,
            UErrorCode &status) const;
private:
    UnicodeString noPlaceholders;
    int32_t placeholderBuffer[EXPECTED_PLACEHOLDER_COUNT * 2];
    int32_t *placeholdersByOffset;
    int32_t placeholderSize;
    int32_t placeholderCapacity;
    int32_t placeholderCount;
    int32_t ensureCapacity(int32_t size);
    UBool addPlaceholder(int32_t id, int32_t offset);
};

U_NAMESPACE_END

#endif
