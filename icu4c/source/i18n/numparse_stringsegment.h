// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING && !UPRV_INCOMPLETE_CPP11_SUPPORT
#ifndef __NUMPARSE_STRINGSEGMENT_H__
#define __NUMPARSE_STRINGSEGMENT_H__

#include "numparse_types.h"
#include "number_types.h"
#include "unicode/unistr.h"

U_NAMESPACE_BEGIN
namespace numparse {
namespace impl {

/**
 * A mutable class allowing for a String with a variable offset and length. The charAt, length, and
 * subSequence methods all operate relative to the fixed offset into the String.
 *
 * @author sffc
 */
class StringSegment : public UMemory, public ::icu::number::impl::CharSequence {
  public:
    explicit StringSegment(const UnicodeString &str);

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
    int32_t getCommonPrefixLength(const UnicodeString &other);

  private:
    const UnicodeString fStr;
    int32_t fStart;
    int32_t fEnd;
};


} // namespace impl
} // namespace numparse
U_NAMESPACE_END

#endif //__NUMPARSE_STRINGSEGMENT_H__
#endif /* #if !UCONFIG_NO_FORMATTING */
