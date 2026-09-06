// © 2023 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

// Fuzzer for ICU Calendar.

#include <cstring>

#include "fuzzer_utils.h"

#include "unicode/listformatter.h"
#include "unicode/locid.h"


void TestFormat(icu::ListFormatter* listFormat, const icu::UnicodeString* items) {
    for (size_t i = 0; i <= 4; i++) {
        icu::UnicodeString appendTo;
        UErrorCode status = U_ZERO_ERROR;
        listFormat->format(items, i, appendTo, status);
        status = U_ZERO_ERROR;
        icu::FormattedList formatted = listFormat->formatStringsToValue(items, i, status);
    }
}
extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size) {
    uint16_t rnd;
    int32_t raw_type;
    int32_t raw_width;
    if (size < sizeof(rnd) + sizeof(raw_type) + sizeof(raw_width)) return 0;
    icu::StringPiece fuzzData(reinterpret_cast<const char *>(data), size);

    std::memcpy(&rnd, fuzzData.data(), sizeof(rnd));
    icu::Locale locale = GetRandomLocale(rnd);
    fuzzData.remove_prefix(sizeof(rnd));

    std::memcpy(&raw_type, fuzzData.data(), sizeof(raw_type));
    fuzzData.remove_prefix(sizeof(raw_type));
    std::memcpy(&raw_width, fuzzData.data(), sizeof(raw_width));
    fuzzData.remove_prefix(sizeof(raw_width));

    // Ensure non-negative before modulo.
    int32_t type_index = raw_type < 0 ? -raw_type : raw_type;
    int32_t width_index = raw_width < 0 ? -raw_width : raw_width;

    UListFormatterType type = static_cast<UListFormatterType>(
        type_index % (static_cast<int>(ULISTFMT_TYPE_UNITS) + 1));
    UListFormatterWidth width = static_cast<UListFormatterWidth>(
        width_index % (static_cast<int>(ULISTFMT_WIDTH_NARROW) + 1));

    size_t len = fuzzData.size() / sizeof(char16_t);
    icu::UnicodeString text(false, reinterpret_cast<const char16_t*>(fuzzData.data()), len);
    const icu::UnicodeString items[] = { text, text, text, text };

    UErrorCode status = U_ZERO_ERROR;
    std::unique_ptr<icu::ListFormatter> listFormat(
        icu::ListFormatter::createInstance(locale, status));
    if (U_SUCCESS(status)) {
        TestFormat(listFormat.get(), items);
    }

    status = U_ZERO_ERROR;
    listFormat.reset(
        icu::ListFormatter::createInstance(locale, type, width, status));
    if (U_SUCCESS(status)) {
        TestFormat(listFormat.get(), items);
    }

    return EXIT_SUCCESS;
}
