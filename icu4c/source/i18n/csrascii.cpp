// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_CONVERSION

#include "csrascii.h"
#include "csrsbcs.h"
#include "csmatch.h"

U_NAMESPACE_BEGIN

CharsetRecog_ASCII::~CharsetRecog_ASCII()
{
    // nothing to do
}

const char *CharsetRecog_ASCII::getName() const
{
    return "ASCII";
}

UBool CharsetRecog_ASCII::match(InputText* input, CharsetMatch *results) const {
    // code similar to determining com.ibm.icu.text.CharsetDetector.fC1Bytes
    bool highestBitSet = false;
    for (int32_t i = 0x80; i <= 0xFF; i += 1) {
        if (input->fByteStats[i] != 0) {
            highestBitSet = true;
            break;
        }
    }

    if (highestBitSet) {
        // non-ASCII, because (at least) one byte in the stream is >= 128
        results->set(input, this, 0);
        return false;
    }

    // ASCII, because ALL bytes in the stream are <= 127.
    // However, there could be some encoding (such as Hebrew or ISO-2022) which also has this property.
    // Thus, we have a confidence lower than 100.
    // We could execute the charset detectors of the other languages;
    // if they don't have a hit, we can increase our confidence.
    // However, this would lead to dependencies to outer CharsetRecognizers which is not a well-designed architecture.

    // We re-use the language detection feature of the ISO-8859-1 detector
    CharsetRecog_8859_1 charsetRecog_8859_1;
    CharsetMatch match_8859_1;
    UBool isMatch_8859_1 = charsetRecog_8859_1.match(input, &match_8859_1);
    if (!isMatch_8859_1) {
        results->set(input, this, 95);
    } else {
        // We are sure that we return ASCII (instead of ISO-8859-1), thus we have a higher confidence
        results->set(input, this, match_8859_1.getConfidence() + 1, "ASCII", match_8859_1.getLanguage());
    }
    return true;
}

U_NAMESPACE_END
#endif
