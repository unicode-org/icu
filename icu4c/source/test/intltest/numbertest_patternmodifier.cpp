// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING && !UPRV_INCOMPLETE_CPP11_SUPPORT

#include "numbertest.h"
#include "number_patternmodifier.h"

void PatternModifierTest::runIndexedTest(int32_t index, UBool exec, const char *&name, char *) {
    if (exec) {
        logln("TestSuite PatternModifierTest: ");
    }
    TESTCASE_AUTO_BEGIN;
        TESTCASE_AUTO(testBasic);
        TESTCASE_AUTO(testMutableEqualsImmutable);
    TESTCASE_AUTO_END;
}

void PatternModifierTest::testBasic() {
    UErrorCode status = U_ZERO_ERROR;
    MutablePatternModifier mod(false);
    ParsedPatternInfo patternInfo;
    PatternParser::parseToPatternInfo(u"a0b", patternInfo, status);
    assertSuccess("Spot 1", status);
    mod.setPatternInfo(&patternInfo);
    mod.setPatternAttributes(UNUM_SIGN_AUTO, false);
    DecimalFormatSymbols symbols(Locale::getEnglish(), status);
    CurrencyUnit currency(u"USD", status);
    assertSuccess("Spot 2", status);
    mod.setSymbols(&symbols, currency, UNUM_UNIT_WIDTH_SHORT, nullptr);

    mod.setNumberProperties(1, StandardPlural::Form::COUNT);
    assertEquals("Pattern a0b", u"a", getPrefix(mod, status));
    assertEquals("Pattern a0b", u"b", getSuffix(mod, status));
    mod.setPatternAttributes(UNUM_SIGN_ALWAYS, false);
    assertEquals("Pattern a0b", u"+a", getPrefix(mod, status));
    assertEquals("Pattern a0b", u"b", getSuffix(mod, status));
    mod.setNumberProperties(0, StandardPlural::Form::COUNT);
    assertEquals("Pattern a0b", u"+a", getPrefix(mod, status));
    assertEquals("Pattern a0b", u"b", getSuffix(mod, status));
    mod.setPatternAttributes(UNUM_SIGN_EXCEPT_ZERO, false);
    assertEquals("Pattern a0b", u"a", getPrefix(mod, status));
    assertEquals("Pattern a0b", u"b", getSuffix(mod, status));
    mod.setNumberProperties(-1, StandardPlural::Form::COUNT);
    assertEquals("Pattern a0b", u"-a", getPrefix(mod, status));
    assertEquals("Pattern a0b", u"b", getSuffix(mod, status));
    mod.setPatternAttributes(UNUM_SIGN_NEVER, false);
    assertEquals("Pattern a0b", u"a", getPrefix(mod, status));
    assertEquals("Pattern a0b", u"b", getSuffix(mod, status));
    assertSuccess("Spot 3", status);

    ParsedPatternInfo patternInfo2;
    PatternParser::parseToPatternInfo(u"a0b;c-0d", patternInfo2, status);
    assertSuccess("Spot 4", status);
    mod.setPatternInfo(&patternInfo2);
    mod.setPatternAttributes(UNUM_SIGN_AUTO, false);
    mod.setNumberProperties(1, StandardPlural::Form::COUNT);
    assertEquals("Pattern a0b;c-0d", u"a", getPrefix(mod, status));
    assertEquals("Pattern a0b;c-0d", u"b", getSuffix(mod, status));
    mod.setPatternAttributes(UNUM_SIGN_ALWAYS, false);
    assertEquals("Pattern a0b;c-0d", u"c+", getPrefix(mod, status));
    assertEquals("Pattern a0b;c-0d", u"d", getSuffix(mod, status));
    mod.setNumberProperties(0, StandardPlural::Form::COUNT);
    assertEquals("Pattern a0b;c-0d", u"c+", getPrefix(mod, status));
    assertEquals("Pattern a0b;c-0d", u"d", getSuffix(mod, status));
    mod.setPatternAttributes(UNUM_SIGN_EXCEPT_ZERO, false);
    assertEquals("Pattern a0b;c-0d", u"a", getPrefix(mod, status));
    assertEquals("Pattern a0b;c-0d", u"b", getSuffix(mod, status));
    mod.setNumberProperties(-1, StandardPlural::Form::COUNT);
    assertEquals("Pattern a0b;c-0d", u"c-", getPrefix(mod, status));
    assertEquals("Pattern a0b;c-0d", u"d", getSuffix(mod, status));
    mod.setPatternAttributes(UNUM_SIGN_NEVER, false);
    // TODO: What should this behavior be?
    assertEquals("Pattern a0b;c-0d", u"c-", getPrefix(mod, status));
    assertEquals("Pattern a0b;c-0d", u"d", getSuffix(mod, status));
    assertSuccess("Spot 5", status);
}

void PatternModifierTest::testMutableEqualsImmutable() {
    UErrorCode status = U_ZERO_ERROR;
    MutablePatternModifier mod(false);
    ParsedPatternInfo patternInfo;
    PatternParser::parseToPatternInfo("a0b;c-0d", patternInfo, status);
    assertSuccess("Spot 1", status);
    mod.setPatternInfo(&patternInfo);
    mod.setPatternAttributes(UNUM_SIGN_AUTO, false);
    DecimalFormatSymbols symbols(Locale::getEnglish(), status);
    CurrencyUnit currency(u"USD", status);
    assertSuccess("Spot 2", status);
    if (U_FAILURE(status)) { return; }
    mod.setSymbols(&symbols, currency, UNUM_UNIT_WIDTH_SHORT, nullptr);
    DecimalQuantity fq;
    fq.setToInt(1);

    NumberStringBuilder nsb1;
    MicroProps micros1;
    mod.addToChain(&micros1);
    mod.processQuantity(fq, micros1, status);
    micros1.modMiddle->apply(nsb1, 0, 0, status);
    assertSuccess("Spot 3", status);

    NumberStringBuilder nsb2;
    MicroProps micros2;
    LocalPointer<ImmutablePatternModifier> immutable(mod.createImmutable(status));
    immutable->applyToMicros(micros2, fq);
    micros2.modMiddle->apply(nsb2, 0, 0, status);
    assertSuccess("Spot 4", status);

    NumberStringBuilder nsb3;
    MicroProps micros3;
    mod.addToChain(&micros3);
    mod.setPatternAttributes(UNUM_SIGN_ALWAYS, false);
    mod.processQuantity(fq, micros3, status);
    micros3.modMiddle->apply(nsb3, 0, 0, status);
    assertSuccess("Spot 5", status);

    assertTrue(nsb1.toUnicodeString() + " vs " + nsb2.toUnicodeString(), nsb1.contentEquals(nsb2));
    assertFalse(nsb1.toUnicodeString() + " vs " + nsb3.toUnicodeString(), nsb1.contentEquals(nsb3));
}

UnicodeString PatternModifierTest::getPrefix(const MutablePatternModifier &mod, UErrorCode &status) {
    NumberStringBuilder nsb;
    mod.apply(nsb, 0, 0, status);
    int32_t prefixLength = mod.getPrefixLength(status);
    return UnicodeString(nsb.toUnicodeString(), 0, prefixLength);
}

UnicodeString PatternModifierTest::getSuffix(const MutablePatternModifier &mod, UErrorCode &status) {
    NumberStringBuilder nsb;
    mod.apply(nsb, 0, 0, status);
    int32_t prefixLength = mod.getPrefixLength(status);
    return UnicodeString(nsb.toUnicodeString(), prefixLength, nsb.length() - prefixLength);
}

#endif /* #if !UCONFIG_NO_FORMATTING */
