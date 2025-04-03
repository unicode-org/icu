// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

// Fuzzer for DecimalFormatSymbols::DecimalFormatSymbols.

#include <cstring>
#include <stddef.h>
#include <stdint.h>
#include <string>
#include <memory>
#include "fuzzer_utils.h"
#include "unicode/dcfmtsym.h"
#include "unicode/unum.h"
#include "uassert.h"

IcuEnvironment* env = new IcuEnvironment();

void testMethods(
    const icu::DecimalFormatSymbols& dfs,
    icu::DecimalFormatSymbols::ENumberFormatSymbol symbol,
    UCurrencySpacing spacing,
    int32_t digit) {
  dfs.getLocale();
  dfs.getSymbol(symbol);
  dfs.getConstSymbol(symbol);
  dfs.getCurrencyPattern();
  dfs.getNumberingSystemName();
  dfs.isCustomCurrencySymbol();
  dfs.isCustomIntlCurrencySymbol();
  dfs.getCodePointZero();
  dfs.getConstDigitSymbol(digit);
  UErrorCode status = U_ZERO_ERROR;
  dfs.getPatternForCurrencySpacing(spacing, true, status);
  dfs.getPatternForCurrencySpacing(spacing, false, status);

}
extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size) {
  UErrorCode status = U_ZERO_ERROR;
  if (size < sizeof(uint16_t)) {
    return 0;
  }
  uint16_t rnd = *(reinterpret_cast<const uint16_t *>(data));
  const icu::Locale& locale = GetRandomLocale(rnd);
  data = data + sizeof(uint16_t);
  size = size - sizeof(uint16_t);

  if (size < sizeof(uint16_t)) {
    return 0;
  }
  uint16_t rnd2 = *(reinterpret_cast<const uint16_t *>(data));
  std::unique_ptr<const icu::NumberingSystem> ns(CreateRandomNumberingSystem(rnd2, status));
  U_ASSERT(U_SUCCESS(status));
  data = data + sizeof(uint16_t);
  size = size - sizeof(uint16_t);

  if (size < sizeof(int32_t)) {
    return 0;
  }
  int32_t digit = *(reinterpret_cast<const int32_t *>(data));
  data = data + sizeof(int32_t);
  size = size - sizeof(int32_t);

  if (size < sizeof(uint8_t)) {
    return 0;
  }
  icu::DecimalFormatSymbols::ENumberFormatSymbol symbol =
      static_cast<icu::DecimalFormatSymbols::ENumberFormatSymbol>(
      *data % icu::DecimalFormatSymbols::ENumberFormatSymbol::kFormatSymbolCount);
  data = data + sizeof(uint8_t);
  size = size - sizeof(uint8_t);

  if (size < sizeof(uint8_t)) {
    return 0;
  }
  UCurrencySpacing spacing =
      static_cast<UCurrencySpacing>(
      *data % UCurrencySpacing::UNUM_CURRENCY_SPACING_COUNT);
  data = data + sizeof(uint8_t);
  size = size - sizeof(uint8_t);

  size_t unistr_size = size/2;
  std::unique_ptr<char16_t[]> fuzzbuff(new char16_t[unistr_size]);
  std::memcpy(fuzzbuff.get(), data, unistr_size * 2);

  icu::UnicodeString fuzzstr(false, fuzzbuff.get(), unistr_size);
  icu::DecimalFormatSymbols dfs1(locale, status);
  U_ASSERT(U_SUCCESS(status));
  testMethods(dfs1, symbol, spacing, digit);

  icu::DecimalFormatSymbols dfs2(locale, *ns, status);
  U_ASSERT(U_SUCCESS(status));
  testMethods(dfs2, symbol, spacing, digit);

  return 0;
}
