// © 2023 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

// Fuzzer for ICU Calendar.

#include <cstring>

#include "fuzzer_utils.h"

#include "unicode/datefmt.h"
#include "unicode/locid.h"

extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size) {
    uint16_t rnd;
    UDate date;
    icu::DateFormat::EStyle dateStyle;
    icu::DateFormat::EStyle timeStyle;
    if (size < sizeof(rnd) + sizeof(date) + sizeof(dateStyle) + sizeof(timeStyle)) return 0;
    icu::StringPiece fuzzData(reinterpret_cast<const char *>(data), size);

    std::memcpy(&rnd, fuzzData.data(), sizeof(rnd));
    fuzzData.remove_prefix(sizeof(rnd));
    icu::Locale locale = GetRandomLocale(rnd);

    std::memcpy(&dateStyle, fuzzData.data(), sizeof(dateStyle));
    fuzzData.remove_prefix(sizeof(dateStyle));
    std::memcpy(&timeStyle, fuzzData.data(), sizeof(timeStyle));
    fuzzData.remove_prefix(sizeof(timeStyle));
    std::memcpy(&date, fuzzData.data(), sizeof(date));
    fuzzData.remove_prefix(sizeof(date));

    std::unique_ptr<icu::DateFormat> df(
        icu::DateFormat::createDateTimeInstance(dateStyle, timeStyle, locale));
    icu::UnicodeString appendTo;
    df->format(date, appendTo);
    icu::UnicodeString skeleton = icu::UnicodeString::fromUTF8(fuzzData);

    UErrorCode status = U_ZERO_ERROR;
    appendTo.remove();
    df.reset(icu::DateFormat::createInstanceForSkeleton(skeleton, status));
    if (U_SUCCESS(status)) {
        df->format(date, appendTo);
    }

    status = U_ZERO_ERROR;
    appendTo.remove();
    df.reset(icu::DateFormat::createInstanceForSkeleton(skeleton, locale, status));
    if (U_SUCCESS(status)) {
        df->format(date, appendTo);
    }
    return EXIT_SUCCESS;
}
