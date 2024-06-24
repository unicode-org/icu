// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include <cstring>
#include <stddef.h>
#include <stdint.h>
#include <string.h>


#include "fuzzer_utils.h"
#include "unicode/regex.h"

IcuEnvironment* env = new IcuEnvironment();

extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size) {
  UErrorCode status = U_ZERO_ERROR;

  size_t unistr_size = size/2;
  std::unique_ptr<char16_t[]> fuzzbuff(new char16_t[unistr_size]);
  std::memcpy(fuzzbuff.get(), data, unistr_size * 2);
  icu::UnicodeString fuzzstr(false, fuzzbuff.get(), unistr_size);

  icu::UnicodeString regex = fuzzstr.tempSubString (0, fuzzstr.length() / 4);
  icu::UnicodeString haystack = fuzzstr.tempSubString (regex.length());

  std::unique_ptr<icu::RegexPattern> re(icu::RegexPattern::compile(regex, UREGEX_CASE_INSENSITIVE, status));
  if (U_FAILURE(status)) {
    return -1;  // invalid regex, don't explore further
  }
  std::unique_ptr<icu::RegexMatcher> regex_matcher(re->matcher(haystack, status));
  if (U_SUCCESS(status)) {
    regex_matcher->setTimeLimit(300, status);
    regex_matcher->find(0, status);
  }
  return 0;
}
