// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#pragma once

#include "number_stringbuilder.h"
#include "intltest.h"
#include "number_affixutils.h"

using namespace icu::number;
using namespace icu::number::impl;

class AffixUtilsTest : public IntlTest {
  public:
    void testEscape();

    void testUnescape();

    void testContainsReplaceType();

    void testInvalid();

    void testUnescapeWithSymbolProvider();

    void runIndexedTest(int32_t index, UBool exec, const char *&name, char *par = 0);

  private:
    UnicodeString unescapeWithDefaults(const SymbolProvider &defaultProvider, UnicodeString input,
                                       UErrorCode &status);
};

class NumberStringBuilderTest : public IntlTest {
  public:
    void testInsertAppendUnicodeString();

    void testInsertAppendCodePoint();

    void testCopy();

    void testFields();

    void testUnlimitedCapacity();

    void testCodePoints();

    void runIndexedTest(int32_t index, UBool exec, const char *&name, char *par = 0);

  private:
    void assertEqualsImpl(const UnicodeString &a, const NumberStringBuilder &b);
};
