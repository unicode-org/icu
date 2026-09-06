// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include <stddef.h>
#include <stdint.h>
#include <string.h>
#include <memory>

#include "fuzzer_utils.h"
#include "unicode/regex.h"

extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size) {
  // Need at least 1 byte for flags + 2 bytes for a char16_t pattern
  if (size < 3) {
    return 0;
  }

  // Use first byte to derive regex flags
  uint32_t flags = data[0];
  const uint8_t* pattern_data = data + 1;
  size_t pattern_size = size - 1;

  // Round down to even size for char16_t alignment
  size_t unistr_size = pattern_size / sizeof(char16_t);
  if (unistr_size == 0) {
    return 0;
  }

  // Copy to properly aligned buffer
  std::unique_ptr<char16_t[]> fuzzbuff(new char16_t[unistr_size]);
  std::memcpy(fuzzbuff.get(), pattern_data, unistr_size * sizeof(char16_t));

  UParseError pe = { 0, 0, {0}, {0} };
  UErrorCode status = U_ZERO_ERROR;

  URegularExpression* re = uregex_open(fuzzbuff.get(),
                                       static_cast<int>(unistr_size),
                                       flags, &pe, &status);

  if (re)
    uregex_close(re);

  return 0;
}
