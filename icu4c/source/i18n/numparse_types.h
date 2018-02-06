// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING && !UPRV_INCOMPLETE_CPP11_SUPPORT
#ifndef __NUMPARSE_TYPES_H__
#define __NUMPARSE_TYPES_H__

#include "unicode/uobject.h"
#include "number_decimalquantity.h"

U_NAMESPACE_BEGIN namespace numparse {
namespace impl {

// Forward-declarations
class StringSegment;
class ParsedNumber;


/**
 * Struct-like class to hold the results of a parsing routine.
 *
 * @author sffc
 */
class ParsedNumber {
  public:
    enum ParsedNumberFlags {
        FLAG_NEGATIVE = 0x0001,
        FLAG_PERCENT = 0x0002,
        FLAG_PERMILLE = 0x0004,
        FLAG_HAS_EXPONENT = 0x0008,
        FLAG_HAS_DEFAULT_CURRENCY = 0x0010,
        FLAG_HAS_DECIMAL_SEPARATOR = 0x0020,
        FLAG_NAN = 0x0040,
        FLAG_INFINITY = 0x0080,
        FLAG_FAIL = 0x0100,
    };

    /**
     * The numerical value that was parsed.
     */
    ::icu::number::impl::DecimalQuantity quantity;

    /**
     * The index of the last char consumed during parsing. If parsing started at index 0, this is equal
     * to the number of chars consumed. This is NOT necessarily the same as the StringSegment offset;
     * "weak" chars, like whitespace, change the offset, but the charsConsumed is not touched until a
     * "strong" char is encountered.
     */
    int32_t charEnd;

    /**
     * Boolean flags (see constants below).
     */
    int32_t flags;

    /**
     * The pattern string corresponding to the prefix that got consumed.
     */
    UnicodeString prefix;

    /**
     * The pattern string corresponding to the suffix that got consumed.
     */
    UnicodeString suffix;

    /**
     * The currency that got consumed.
     */
    UnicodeString currencyCode;

    ParsedNumber();

    ParsedNumber(const ParsedNumber& other) = default;

    ParsedNumber& operator=(const ParsedNumber& other) = default;

    void clear();

    /**
     * Call this method to register that a "strong" char was consumed. This should be done after calling
     * {@link StringSegment#setOffset} or {@link StringSegment#adjustOffset} except when the char is
     * "weak", like whitespace.
     *
     * <p>
     * <strong>What is a strong versus weak char?</strong> The behavior of number parsing is to "stop"
     * after reading the number, even if there is other content following the number. For example, after
     * parsing the string "123 " (123 followed by a space), the cursor should be set to 3, not 4, even
     * though there are matchers that accept whitespace. In this example, the digits are strong, whereas
     * the whitespace is weak. Grouping separators are weak, whereas decimal separators are strong. Most
     * other chars are strong.
     *
     * @param segment
     *            The current StringSegment, usually immediately following a call to setOffset.
     */
    void setCharsConsumed(const StringSegment& segment);

    /**
     * Returns whether this the parse was successful. To be successful, at least one char must have been
     * consumed, and the failure flag must not be set.
     */
    bool success() const;

    bool seenNumber() const;

    double getDouble() const;
};


/**
 * A mutable class allowing for a String with a variable offset and length. The charAt, length, and
 * subSequence methods all operate relative to the fixed offset into the String.
 *
 * @author sffc
 */
class StringSegment : public UMemory, public ::icu::number::impl::CharSequence {
  public:
    explicit StringSegment(const UnicodeString& str);

    int32_t getOffset() const;

    void setOffset(int32_t start);

    /**
     * Equivalent to <code>setOffset(getOffset()+delta)</code>.
     *
     * <p>
     * This method is usually called by a Matcher to register that a char was consumed. If the char is
     * strong (it usually is, except for things like whitespace), follow this with a call to
     * {@link ParsedNumber#setCharsConsumed}. For more information on strong chars, see that method.
     */
    void adjustOffset(int32_t delta);

    void setLength(int32_t length);

    void resetLength();

    int32_t length() const override;

    char16_t charAt(int32_t index) const override;

    UChar32 codePointAt(int32_t index) const override;

    UnicodeString toUnicodeString() const override;

    /**
     * Returns the first code point in the string segment, or -1 if the string starts with an invalid
     * code point.
     */
    UChar32 getCodePoint() const;

    /**
     * Returns the length of the prefix shared by this StringSegment and the given CharSequence. For
     * example, if this string segment is "aab", and the char sequence is "aac", this method returns 2,
     * since the first 2 characters are the same.
     */
    int32_t getCommonPrefixLength(const UnicodeString& other);

  private:
    const UnicodeString fStr;
    int32_t fStart;
    int32_t fEnd;
};


/**
 * The core interface implemented by all matchers used for number parsing.
 *
 * Given a string, there should NOT be more than one way to consume the string with the same matcher
 * applied multiple times. If there is, the non-greedy parsing algorithm will be unhappy and may enter an
 * exponential-time loop. For example, consider the "A Matcher" that accepts "any number of As". Given
 * the string "AAAA", there are 2^N = 8 ways to apply the A Matcher to this string: you could have the A
 * Matcher apply 4 times to each character; you could have it apply just once to all the characters; you
 * could have it apply to the first 2 characters and the second 2 characters; and so on. A better version
 * of the "A Matcher" would be for it to accept exactly one A, and allow the algorithm to run it
 * repeatedly to consume a string of multiple As. The A Matcher can implement the Flexible interface
 * below to signal that it can be applied multiple times in a row.
 *
 * @author sffc
 */
class NumberParseMatcher {
  public:
    virtual ~NumberParseMatcher() = default;

    /**
     * Matchers can override this method to return true to indicate that they are optional and can be run
     * repeatedly. Used by SeriesMatcher, primarily in the context of IgnorablesMatcher.
     */
    virtual bool isFlexible() const {
        return false;
    }

    /**
     * Runs this matcher starting at the beginning of the given StringSegment. If this matcher finds
     * something interesting in the StringSegment, it should update the offset of the StringSegment
     * corresponding to how many chars were matched.
     *
     * @param segment
     *            The StringSegment to match against. Matches always start at the beginning of the
     *            segment. The segment is guaranteed to contain at least one char.
     * @param result
     *            The data structure to store results if the match succeeds.
     * @return Whether this matcher thinks there may be more interesting chars beyond the end of the
     *         string segment.
     */
    virtual bool match(StringSegment& segment, ParsedNumber& result) const = 0;

    /**
     * Should return a set representing all possible chars (UTF-16 code units) that could be the first
     * char that this matcher can consume. This method is only called during construction phase, and its
     * return value is used to skip this matcher unless a segment begins with a char in this set. To make
     * this matcher always run, return {@link UnicodeSet#ALL_CODE_POINTS}.
     */
    virtual UnicodeSet getLeadCodePoints() const = 0;

    /**
     * Method called at the end of a parse, after all matchers have failed to consume any more chars.
     * Allows a matcher to make final modifications to the result given the knowledge that no more
     * matches are possible.
     *
     * @param result
     *            The data structure to store results.
     */
    virtual void postProcess(ParsedNumber& result) const = 0;
};


} // namespace impl
} // namespace numparse
U_NAMESPACE_END

#endif //__NUMPARSE_TYPES_H__
#endif /* #if !UCONFIG_NO_FORMATTING */
