// © 2023 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

// Fuzzer for ICU Calendar.

#include <cstring>

#include "fuzzer_utils.h"

#include "unicode/listformatter.h"
#include "unicode/locid.h"

extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size) {
    uint16_t rnd;
    UListFormatterType type;
    UListFormatterWidth width;
    if (size < sizeof(rnd) + sizeof(type) + sizeof(width)) return 0;
    icu::StringPiece fuzzData(reinterpret_cast<const char *>(data), size);

    std::memcpy(&rnd, fuzzData.data(), sizeof(rnd));
    icu::Locale locale = GetRandomLocale(rnd);
    fuzzData.remove_prefix(sizeof(rnd));

    std::memcpy(&type, fuzzData.data(), sizeof(type));
    fuzzData.remove_prefix(sizeof(type));
    type = static_cast<UListFormatterType>(
        static_cast<int>(type) % (static_cast<int>(ULISTFMT_TYPE_UNITS) + 1));
    std::memcpy(&width, fuzzData.data(), sizeof(width));
    fuzzData.remove_prefix(sizeof(width));
    width = static_cast<UListFormatterWidth>(
        static_cast<int>(width) % (static_cast<int>(ULISTFMT_WIDTH_NARROW) + 1));

    size_t len = fuzzData.size() / sizeof(char16_t);
    icu::UnicodeString text(false, reinterpret_cast<const char16_t*>(fuzzData.data()), len);
    const icu::UnicodeString items[] = { text, text, text, text };

    UErrorCode status = U_ZERO_ERROR;
    std::unique_ptr<icu::ListFormatter> listFormat(
        icu::ListFormatter::createInstance(locale, status));

    if (U_SUCCESS(status)) {
        for (size_t i = 0; i <= 4; i++) {
            icu::UnicodeString appendTo;
            status = U_ZERO_ERROR;
            listFormat->format(items, i, appendTo, status);
        }
    }
    status = U_ZERO_ERROR;
    listFormat.reset(
        icu::ListFormatter::createInstance(locale, type, width, status));
    if (U_SUCCESS(status)) {
        for (size_t i = 0; i <= 4; i++) {
            icu::UnicodeString appendTo;
            status = U_ZERO_ERROR;
            listFormat->format(items, i, appendTo, status);
        }
    }

    return EXIT_SUCCESS;
}
