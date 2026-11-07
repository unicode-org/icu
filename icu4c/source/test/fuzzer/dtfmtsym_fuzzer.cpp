// © 2023 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

// Fuzzer for ICU DateFormatSymbols.

#include <cstring>

#include "fuzzer_utils.h"

#include "unicode/dtfmtsym.h"
#include "unicode/locid.h"

extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size) {
    uint16_t rnd;
    int32_t raw_context;
    int32_t raw_width;
    if (size < sizeof(rnd) + sizeof(raw_context) + sizeof(raw_width)) return 0;
    icu::StringPiece fuzzData(reinterpret_cast<const char *>(data), size);

    std::memcpy(&rnd, fuzzData.data(), sizeof(rnd));
    icu::Locale locale = GetRandomLocale(rnd);
    fuzzData.remove_prefix(sizeof(rnd));

    std::memcpy(&raw_context, fuzzData.data(), sizeof(raw_context));
    fuzzData.remove_prefix(sizeof(raw_context));

    std::memcpy(&raw_width, fuzzData.data(), sizeof(raw_width));
    fuzzData.remove_prefix(sizeof(raw_width));

    // Ensure non-negative before modulo to avoid implementation-defined behavior.
    int32_t context_index = raw_context < 0 ? -raw_context : raw_context;
    int32_t width_index = raw_width < 0 ? -raw_width : raw_width;

    icu::DateFormatSymbols::DtContextType context =
        static_cast<icu::DateFormatSymbols::DtContextType>(
            context_index % icu::DateFormatSymbols::DtContextType::DT_CONTEXT_COUNT);

    icu::DateFormatSymbols::DtWidthType width =
        static_cast<icu::DateFormatSymbols::DtWidthType>(
            width_index % icu::DateFormatSymbols::DtWidthType::DT_WIDTH_COUNT);

    size_t len = fuzzData.size() / sizeof(char16_t);
    icu::UnicodeString text(false, reinterpret_cast<const char16_t*>(fuzzData.data()), len);
    const icu::UnicodeString items[] = { text, text, text, text };

    UErrorCode status = U_ZERO_ERROR;
    std::unique_ptr<icu::DateFormatSymbols> dfs(
        new icu::DateFormatSymbols(locale, status));
    if (U_FAILURE(status)) {
        return EXIT_SUCCESS;
    }

    int32_t count;
    dfs->getEras(count);
    dfs->getEraNames(count);
    dfs->getNarrowEras(count);
    dfs->getMonths(count);
    dfs->getShortMonths(count);
    dfs->getMonths(count, context, width);
    dfs->getWeekdays(count);
    dfs->getShortWeekdays(count);
    dfs->getWeekdays(count, context, width);
    dfs->getShortWeekdays(count);
    dfs->getQuarters(count, context, width);
    dfs->getAmPmStrings(count);

    icu::UnicodeString output;
    dfs->getTimeSeparatorString(output);
    dfs->getYearNames(count, context, width);
    dfs->getZodiacNames(count, context, width);
    dfs->getLeapMonthPatterns(count);
    int32_t count2;
    dfs->getZoneStrings(count, count2);
    dfs->getLocalPatternChars(output);

    return EXIT_SUCCESS;
}
