// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include <cstring>

#include "fuzzer_utils.h"
#include "unicode/coll.h"
#include "unicode/localpointer.h"
#include "unicode/locid.h"
#include "unicode/sortkey.h"
#include "unicode/tblcoll.h"

extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size) {
  UErrorCode status = U_ZERO_ERROR;
  if (size > 2000) {
      // Limit the effective test data to only 2000 bytes to avoid meaningless
      // timeout.
      size = 2000;
  }

  // Use all bytes: allocate enough char16_t for all bytes, rounded up
  size_t unistr_size = (size + 1) / 2;  // Round up to handle odd size
  std::unique_ptr<char16_t[]> fuzzbuff(new char16_t[unistr_size]);
  // Copy all bytes, zero-initialize any remaining byte in the last char16_t
  if (size > 0) {
    std::memcpy(fuzzbuff.get(), data, size);
    // If size is odd, zero the last byte of the last char16_t
    if (size % 2 == 1) {
      // Cast to uint8_t* to access individual bytes
      uint8_t* buffer_bytes = reinterpret_cast<uint8_t*>(fuzzbuff.get());
      buffer_bytes[size] = 0;  // Zero the unused byte in last char16_t
    }
  }
  icu::UnicodeString fuzzstr(false, fuzzbuff.get(), unistr_size);

  icu::LocalPointer<icu::RuleBasedCollator> col1(
      new icu::RuleBasedCollator(fuzzstr, status));

  if (U_SUCCESS(status)) {
      col1->getVariableTop(status);
      if (U_FAILURE(status)) return 0;
      
      icu::CollationKey key;
      col1->getCollationKey(fuzzstr, key, status);
      if (U_FAILURE(status)) return 0;
      
      icu::LocalPointer<icu::UnicodeSet> tailoredSet(col1->getTailoredSet(status));
      if (U_FAILURE(status)) return 0;
      
      col1->getLocale(ULOC_ACTUAL_LOCALE, status);
      if (U_FAILURE(status)) return 0;
      
      col1->getLocale(ULOC_VALID_LOCALE, status);
      if (U_FAILURE(status)) return 0;
      
      col1->getMaxVariable();
      col1->getStrength();
      col1->getSortKey(fuzzstr, nullptr, 0);
  }
  return 0;
}