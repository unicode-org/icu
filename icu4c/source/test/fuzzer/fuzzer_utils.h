// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#ifndef FUZZER_UTILS_H_
#define FUZZER_UTILS_H_

#include <assert.h>

#include "unicode/locid.h"
#include "unicode/numsys.h"
#include "unicode/strenum.h"

struct IcuEnvironment {
  IcuEnvironment() {
    // nothing to initialize yet;
  }
};

const icu::Locale& GetRandomLocale(uint16_t rnd) {
  int32_t num_locales = 0;
  const icu::Locale* locales = icu::Locale::getAvailableLocales(num_locales);
  assert(num_locales > 0);
  return locales[rnd % num_locales];
}

const icu::NumberingSystem* CreateRandomNumberingSystem(uint16_t rnd, UErrorCode &status) {
  std::unique_ptr<icu::StringEnumeration> se(icu::NumberingSystem::getAvailableNames(status));
  if (U_FAILURE(status)) return nullptr;
  int32_t count = se->count(status);
  if (U_FAILURE(status)) return nullptr;
  int32_t index = rnd % count;
  se->reset(status);
  for (int32_t i = 0; i < index - 1; i++, se->next(nullptr, status)) {
      // empty
  }
  const char* name = se->next(nullptr, status);
  if (U_FAILURE(status)) return nullptr;
  return icu::NumberingSystem::createInstanceByName(name, status);
}


#endif  // FUZZER_UTILS_H_
