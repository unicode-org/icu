// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include <cstring>
#include <stddef.h>
#include <stdint.h>
#include <string.h>


#include "fuzzer_utils.h"
#include "unicode/messageformat2.h"
#include "unicode/messagepattern.h"
#include "unicode/msgfmt.h"

IcuEnvironment* env = new IcuEnvironment();

extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size) {
  UParseError pe = { 0, 0, {0}, {0} };
  UErrorCode status = U_ZERO_ERROR;

  size_t unistr_size = size/2;
  std::unique_ptr<char16_t[]> fuzzbuff(new char16_t[unistr_size]);
  std::memcpy(fuzzbuff.get(), data, unistr_size * 2);
  icu::UnicodeString fuzzstr(false, fuzzbuff.get(), unistr_size);

  icu::MessageFormat mfmt(fuzzstr, status);

  status = U_ZERO_ERROR;
  icu::MessagePattern mpat(fuzzstr, &pe, status);
  pe = { 0, 0, {0}, {0} };

  status = U_ZERO_ERROR;
  icu::message2::MessageFormatter msgfmt2 =
      icu::message2::MessageFormatter::Builder(status)
      .setPattern(fuzzstr, pe, status)
      .build(status);
  return 0;
}
