// © 2023 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

// Fuzzer for ICU Calendar.

#include <cstring>

#include "fuzzer_utils.h"

#include "unicode/datefmt.h"
#include "unicode/locid.h"

extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size) {
    uint16_t rnd;
    uint8_t rnd2;
    UDate date;
    icu::DateFormat::EStyle styles[] = {
        icu::DateFormat::EStyle::kNone,
        icu::DateFormat::EStyle::kFull,
        icu::DateFormat::EStyle::kLong,
        icu::DateFormat::EStyle::kMedium,
        icu::DateFormat::EStyle::kShort,
        icu::DateFormat::EStyle::kDateOffset,
        icu::DateFormat::EStyle::kDateTime,
        icu::DateFormat::EStyle::kDateTimeOffset,
        icu::DateFormat::EStyle::kRelative,
        icu::DateFormat::EStyle::kFullRelative,
        icu::DateFormat::EStyle::kLongRelative,
        icu::DateFormat::EStyle::kMediumRelative,
        icu::DateFormat::EStyle::kShortRelative,
    };
    int32_t numStyles = sizeof(styles) / sizeof(icu::DateFormat::EStyle);

    if (size < sizeof(rnd) + sizeof(date) + 4*sizeof(uint8_t)) {
        return 0;
    }
    icu::StringPiece fuzzData(reinterpret_cast<const char *>(data), size);

    std::memcpy(&rnd, fuzzData.data(), sizeof(rnd));
    fuzzData.remove_prefix(sizeof(rnd));
    icu::Locale locale = GetRandomLocale(rnd);

    // Use styles[] lookup with modulo for all style variables to avoid
    // undefined behavior from loading invalid enum values.
    std::memcpy(&rnd2, fuzzData.data(), sizeof(rnd2));
    icu::DateFormat::EStyle dateStyle = styles[rnd2 % numStyles];
    fuzzData.remove_prefix(sizeof(rnd2));

    std::memcpy(&rnd2, fuzzData.data(), sizeof(rnd2));
    icu::DateFormat::EStyle timeStyle = styles[rnd2 % numStyles];
    fuzzData.remove_prefix(sizeof(rnd2));

    std::memcpy(&rnd2, fuzzData.data(), sizeof(rnd2));
    icu::DateFormat::EStyle dateStyle2 = styles[rnd2 % numStyles];
    fuzzData.remove_prefix(sizeof(rnd2));

    std::memcpy(&rnd2, fuzzData.data(), sizeof(rnd2));
    icu::DateFormat::EStyle timeStyle2 = styles[rnd2 % numStyles];
    fuzzData.remove_prefix(sizeof(rnd2));

    std::memcpy(&date, fuzzData.data(), sizeof(date));
    fuzzData.remove_prefix(sizeof(date));

    std::unique_ptr<icu::DateFormat> df(
        icu::DateFormat::createDateTimeInstance(dateStyle, timeStyle, locale));
    if (df) {
        icu::UnicodeString appendTo;
        df->format(date, appendTo);
    }

    df.reset(
        icu::DateFormat::createDateTimeInstance(dateStyle2, timeStyle2, locale));
    if (df) {
        icu::UnicodeString appendTo;
        df->format(date, appendTo);
    }

    icu::UnicodeString skeleton = icu::UnicodeString::fromUTF8(fuzzData);

    UErrorCode status = U_ZERO_ERROR;
    df.reset(icu::DateFormat::createInstanceForSkeleton(skeleton, status));
    if (U_SUCCESS(status) && df) {
        icu::UnicodeString appendTo;
        df->format(date, appendTo);
    }

    status = U_ZERO_ERROR;
    df.reset(icu::DateFormat::createInstanceForSkeleton(skeleton, locale, status));
    if (U_SUCCESS(status) && df) {
        icu::UnicodeString appendTo;
        df->format(date, appendTo);
    }

    std::string str(fuzzData.data(), fuzzData.size());
    icu::Locale locale2(str.c_str());
    df.reset(
        icu::DateFormat::createDateTimeInstance(dateStyle, timeStyle, locale2));
    df.reset(
        icu::DateFormat::createDateTimeInstance(dateStyle2, timeStyle2, locale2));

    status = U_ZERO_ERROR;
    UDateFormat* udf = udat_open(UDAT_PATTERN, UDAT_PATTERN, str.c_str(), nullptr, 0,
                                 skeleton.getBuffer(), skeleton.length(), &status);
    if (udf && U_SUCCESS(status)) {
        udat_close(udf);
    }
    return 0;
}
