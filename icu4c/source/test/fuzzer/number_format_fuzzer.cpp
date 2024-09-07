// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

// Fuzzer for NumberFormat::parse.

#include <cstring>
#include <stddef.h>
#include <stdint.h>
#include <string>
#include <memory>
#include "fuzzer_utils.h"
#include "unicode/choicfmt.h"
#include "unicode/compactdecimalformat.h"
#include "unicode/decimfmt.h"
#include "unicode/numfmt.h"
#include "unicode/rbnf.h"

IcuEnvironment* env = new IcuEnvironment();

extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size) {
  UErrorCode status = U_ZERO_ERROR;
  uint16_t rnd = 0;

  if (size < 2) {
    return 0;
  }

  rnd = *(reinterpret_cast<const uint16_t *>(data));
  data = data + 2;
  size = size - 2;

  size_t unistr_size = size/2;
  std::unique_ptr<char16_t[]> fuzzbuff(new char16_t[unistr_size]);
  std::memcpy(fuzzbuff.get(), data, unistr_size * 2);

  const icu::Locale& locale = GetRandomLocale(rnd);

  icu::UnicodeString fuzzstr(false, fuzzbuff.get(), unistr_size);
  icu::Formattable result;
  std::unique_ptr<icu::NumberFormat> fmt(
      icu::NumberFormat::createInstance(locale, status));
  if (U_SUCCESS(status)) {
      fmt->parse(fuzzstr, result, status);
  }

  status = U_ZERO_ERROR;
  fmt.reset(icu::NumberFormat::createCurrencyInstance(locale, status));
  if (U_SUCCESS(status)) {
      fmt->parse(fuzzstr, result, status);
  }

  status = U_ZERO_ERROR;
  fmt.reset(icu::NumberFormat::createPercentInstance(locale, status));
  if (U_SUCCESS(status)) {
      fmt->parse(fuzzstr, result, status);
  }

  status = U_ZERO_ERROR;
  fmt.reset(icu::NumberFormat::createScientificInstance(locale, status));
  if (U_SUCCESS(status)) {
      fmt->parse(fuzzstr, result, status);
  }

  status = U_ZERO_ERROR;
  icu::ChoiceFormat cfmt(fuzzstr, status);
  if (U_SUCCESS(status)) {
      cfmt.parse(fuzzstr, result, status);
  }

  UParseError perror;
  status = U_ZERO_ERROR;
  icu::RuleBasedNumberFormat rbfmt(fuzzstr, locale, perror, status);
  if (U_SUCCESS(status)) {
      rbfmt.parse(fuzzstr, result, status);
  }

  status = U_ZERO_ERROR;
  icu::DecimalFormat dfmt(fuzzstr, status);
  if (U_SUCCESS(status)) {
      dfmt.parse(fuzzstr, result, status);
  }

  status = U_ZERO_ERROR;
  fmt.reset(icu::CompactDecimalFormat::createInstance(locale, UNUM_SHORT, status));
  if (U_SUCCESS(status)) {
      fmt->parse(fuzzstr, result, status);
  }

  status = U_ZERO_ERROR;
  fmt.reset(icu::CompactDecimalFormat::createInstance(locale, UNUM_LONG, status));
  if (U_SUCCESS(status)) {
      fmt->parse(fuzzstr, result, status);
  }
  return 0;
}
