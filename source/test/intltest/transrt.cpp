/*
**********************************************************************
*   Copyright (C) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   05/23/00    aliu        Creation.
**********************************************************************
*/
#include "unicode/utypes.h"
#include "unicode/translit.h"
#include "unicode/rbt.h"
#include "unicode/uniset.h"
#include "unicode/unicode.h"
#include "unicode/normlzr.h"
#include "unicode/uchar.h"
#include "transrt.h"
#include "testutil.h"

#define CASE(id,test) case id:                          \
                          name = #test;                 \
                          if (exec) {                   \
                              logln(#test "---");       \
                              logln((UnicodeString)""); \
                              test();                   \
                          }                             \
                          break

#define EXHAUSTIVE(id,test) case id:                            \
                              if(quick==FALSE){                 \
                                  name = #test;                 \
                                  if (exec){                    \
                                      logln(#test "---");       \
                                      logln((UnicodeString)""); \
                                      test();                   \
                                  }                             \
                              }else{                            \
                                name="";                        \
                              }                                 \
                              break
void
TransliteratorRoundTripTest::runIndexedTest(int32_t index, UBool exec,
                                   const char* &name, char* /*par*/) {
    switch (index) {
        CASE(0,TestHiragana);
        CASE(1,TestKatakana);
        //CASE(2,TestArabic);
        //CASE(3,TestHebrew);
        CASE(2,TestGreek);
        CASE(3,Testel);
        CASE(4,TestCyrillic);
        CASE(5,TestDevanagariLatin);

#ifdef ENABLE_FAILING_TESTS
        CASE(6,TestJamo);
        CASE(7,TestJamoHangul);
#else 
        case 6: /* falls through */
        case 7: /* falls through */
#endif
        EXHAUSTIVE(8,TestDevanagariBengali);
        EXHAUSTIVE(9,TestDevanagariGurmukhi);
        EXHAUSTIVE(10,TestDevanagariGujarati);
        EXHAUSTIVE(11,TestDevanagariOriya);
        EXHAUSTIVE(12,TestDevanagariTamil);
        EXHAUSTIVE(13,TestDevanagariTelugu);
        EXHAUSTIVE(14,TestDevanagariKannada);
        EXHAUSTIVE(15,TestDevanagariMalayalam);
        EXHAUSTIVE(16,TestBengaliGurmukhi);
        EXHAUSTIVE(17,TestBengaliGujarati);
        EXHAUSTIVE(18,TestBengaliOriya);
        EXHAUSTIVE(19,TestBengaliTamil);
        EXHAUSTIVE(20,TestBengaliTelugu);
        EXHAUSTIVE(21,TestBengaliKannada);
        EXHAUSTIVE(22,TestBengaliMalayalam );
        EXHAUSTIVE(23,TestGurmukhiGujarati);
        EXHAUSTIVE(24,TestGurmukhiOriya);
        EXHAUSTIVE(25,TestGurmukhiTamil);
        EXHAUSTIVE(26,TestGurmukhiTelugu);
        EXHAUSTIVE(27,TestGurmukhiKannada);
        EXHAUSTIVE(28,TestGurmukhiMalayalam);
        EXHAUSTIVE(29,TestGujaratiOriya);
        EXHAUSTIVE(30,TestGujaratiTamil);
        EXHAUSTIVE(31,TestGujaratiTelugu);
        EXHAUSTIVE(32,TestGujaratiKannada);
        EXHAUSTIVE(33,TestGujaratiMalayalam);
        EXHAUSTIVE(34,TestOriyaTamil);
        EXHAUSTIVE(35,TestOriyaTelugu);
        EXHAUSTIVE(36,TestOriyaKannada);
        EXHAUSTIVE(37,TestOriyaMalayalam);
        EXHAUSTIVE(38,TestTamilTelugu);
        EXHAUSTIVE(39,TestTamilKannada);
        EXHAUSTIVE(40,TestTamilMalayalam);
        EXHAUSTIVE(41,TestTeluguKannada);
        EXHAUSTIVE(42,TestTeluguMalayalam);
        EXHAUSTIVE(43,TestKannadaMalayalam);
        default: name = ""; break;
    }
}

//--------------------------------------------------------------------
// BitSet
//--------------------------------------------------------------------

/**
 * Tiny and incomplete BitSet.  Hardcoded to support 0..FFFF.
 */
class BitSet {
    int32_t bits[65536/32];

public:
    BitSet();
    ~BitSet();
    void clear();
    void set(int32_t x);
    UBool get(int32_t x) const;
};

BitSet::BitSet() {
    clear();
}

BitSet::~BitSet() {
}

void BitSet::clear() {
    int32_t *limit = bits + 65536/32;
    int32_t *p = bits;
    while (p < limit) *p++ = 0;
}

void BitSet::set(int32_t x) {
    x &= 0xFFFF;
    int32_t i = x / 32;
    int32_t bit = 1L << (x & 31);
    bits[i] |= bit;
}

UBool BitSet::get(int32_t x) const {
    x &= 0xFFFF;
    int32_t i = x / 32;
    int32_t bit = 1L << (x & 31);
    return (bits[i] & bit) != 0L;
}

//--------------------------------------------------------------------
// Legal
//--------------------------------------------------------------------

class Legal {
public:
    Legal() {}
    virtual ~Legal() {}
    virtual UBool is(const UnicodeString& sourceString) const {return TRUE;}
};

class LegalGreek : public Legal {
    UBool full;
public:
    LegalGreek(UBool _full) { full = _full; }
    virtual ~LegalGreek() {}

    virtual UBool is(const UnicodeString& sourceString) const;

    static UBool isVowel(UChar c);
    
    static UBool isRho(UChar c);
};

UBool LegalGreek::is(const UnicodeString& sourceString) const { 
    UnicodeString decomp;
    UErrorCode ec = U_ZERO_ERROR;
    Normalizer::decompose(sourceString, FALSE, 0, decomp, ec);
                
    // modern is simpler: don't care about anything but a grave
    if (!full) {
        if (sourceString == CharsToUnicodeString("\\u039C\\u03C0"))
            return FALSE;
        for (int32_t i = 0; i < decomp.length(); ++i) {
            UChar c = decomp.charAt(i);
            // exclude all the accents
            if (c == 0x0313 || c == 0x0314 || c == 0x0300 || c == 0x0302
                || c == 0x0342 || c == 0x0345
                ) return FALSE;
        }
        return TRUE;
    }

    // Legal greek has breathing marks IFF there is a vowel or RHO at the start
    // IF it has them, it has exactly one.
    // IF it starts with a RHO, then the breathing mark must come before the second letter.
    // Since there are no surrogates in greek, don't worry about them
    UBool firstIsVowel = FALSE;
    UBool firstIsRho = FALSE;
    UBool noLetterYet = TRUE;
    int32_t breathingCount = 0;
    int32_t letterCount = 0;
    for (int32_t i = 0; i < decomp.length(); ++i) {
        UChar c = decomp.charAt(i);
        if (u_isalpha(c)) {
            ++letterCount;
            if (noLetterYet) {
                noLetterYet =  FALSE;
                firstIsVowel = isVowel(c);
                firstIsRho = isRho(c);
            }
            if (firstIsRho && letterCount == 2 && breathingCount == 0) return FALSE;
        }
        if (c == 0x0313 || c == 0x0314) {
            ++breathingCount;
        }
    }
    
    if (firstIsVowel || firstIsRho) return breathingCount == 1;
    return breathingCount == 0;
}

UBool LegalGreek::isVowel(UChar c) {
    switch (c) {
    case 0x03B1:
    case 0x03B5:
    case 0x03B7:
    case 0x03B9:
    case 0x03BF:
    case 0x03C5:
    case 0x03C9:
    case 0x0391:
    case 0x0395:
    case 0x0397:
    case 0x0399:
    case 0x039F:
    case 0x03A5:
    case 0x03A9:
        return TRUE;
    }
    return FALSE;
}

UBool LegalGreek::isRho(UChar c) {
    switch (c) {
    case 0x03C1:
    case 0x03A1:
        return TRUE;
    }
    return FALSE;
}

class LegalDeleter {
    Legal* obj;
    Legal*& zeroMe;
public:
    LegalDeleter(Legal* adopted, Legal*& ptrToClean) :
        obj(adopted),
        zeroMe(ptrToClean) {}
    ~LegalDeleter() { delete obj; zeroMe = NULL; }
};

//--------------------------------------------------------------------
// RTTest Interface
//--------------------------------------------------------------------

class RTTest {

    // PrintWriter out;

    UnicodeString transliteratorID; 
    int8_t sourceScript;
    int8_t targetScript;
    int32_t errorLimit;
    int32_t errorCount;
    int32_t pairLimit;
    UnicodeSet sourceRange;
    UnicodeSet targetRange;
    UnicodeSet roundtripExclusions;
    IntlTest* log;
    Legal* legalSource; // NOT owned
    UnicodeSet badCharacters;

public:

    /*
     * create a test for the given script transliterator.
     */
    RTTest(const UnicodeString& transliteratorIDStr, 
           int8_t sourceScriptVal, int8_t targetScriptVal);

    virtual ~RTTest();

    void setErrorLimit(int32_t limit);

    void setPairLimit(int32_t limit);

    void test(const UnicodeString& sourceRange,
              const UnicodeString& targetRange,
              const char* roundtripExclusions,
              IntlTest* log,
              Legal* adoptedLegal);

private:

    // Added to do better equality check.
        
    static UBool isSame(const UnicodeString& a, const UnicodeString& b);
        
    UBool includesSome(const UnicodeSet& set, const UnicodeString& a);
        
    static UBool isCamel(const UnicodeString& a);

    void test2();

    void logWrongScript(const UnicodeString& label,
                        const UnicodeString& from,
                        const UnicodeString& to);
    void logRoundTripFailure(const UnicodeString& from,
                             const UnicodeString& to,
                             const UnicodeString& back);
    void logNotCanonical(const UnicodeString& label,
                         const UnicodeString& from,
                         const UnicodeString& to,
                         const UnicodeString& toCan);

protected:

    /*
     * Characters to filter for source-target mapping completeness
     * Typically is base alphabet, minus extended characters
     * Default is ASCII letters for Latin
     */
    virtual UBool isSource(UChar c);

    /*
     * Characters to check for target back to source mapping.
     * Typically the same as the target script, plus punctuation
     */
    inline UBool isReceivingSource(UChar c);

    /*
     * Characters to filter for target-source mapping
     * Typically is base alphabet, minus extended characters
     */
    inline UBool isTarget(UChar c);

    /*
     * Characters to check for target-source mapping
     * Typically the same as the source script, plus punctuation
     */
    inline UBool isReceivingTarget(UChar c);

    UBool isSource(const UnicodeString& s);
    UBool isTarget(const UnicodeString& s);
    UBool isReceivingSource(const UnicodeString& s);
    UBool isReceivingTarget(const UnicodeString& s);
};

//--------------------------------------------------------------------
// RTTest Implementation
//--------------------------------------------------------------------

/*
 * create a test for the given script transliterator.
 */
RTTest::RTTest(const UnicodeString& transliteratorIDStr, 
               int8_t sourceScriptVal, int8_t targetScriptVal) {
    this->transliteratorID = transliteratorIDStr;
    this->sourceScript = sourceScriptVal;
    this->targetScript = targetScriptVal;
    legalSource = NULL;
    errorLimit = (int32_t)0x7FFFFFFFL;
    errorCount = 0;
    pairLimit  = 0x10000;
}

RTTest::~RTTest() {
}

void RTTest::setErrorLimit(int32_t limit) {
    errorLimit = limit;
}

void RTTest::setPairLimit(int32_t limit) {
    pairLimit = limit;
}

UBool RTTest::isSame(const UnicodeString& a, const UnicodeString& b) {
    if (a == b) return TRUE;
    if (a.caseCompare(b, U_FOLD_CASE_DEFAULT)==0 && isCamel(a)) return TRUE;
    UnicodeString aa, bb;
    UErrorCode ec = U_ZERO_ERROR;
    Normalizer::decompose(a, FALSE, 0, aa, ec);
    Normalizer::decompose(b, FALSE, 0, bb, ec);
    if (aa == bb) return TRUE;
    if (aa.caseCompare(bb, U_FOLD_CASE_DEFAULT)==0 && isCamel(aa)) return TRUE;
    return FALSE;
}

UBool RTTest::includesSome(const UnicodeSet& set, const UnicodeString& a) {
    UChar32 cp;
    for (int32_t i = 0; i < a.length(); i += UTF_CHAR_LENGTH(cp)) {
        cp = a.char32At(i);
        if (set.contains(cp)) return TRUE;
    }
    return FALSE;
}

UBool RTTest::isCamel(const UnicodeString& a) {
    // see if string is of the form aB; e.g. lower, then upper or title
    UChar32 cp;
    UBool haveLower = FALSE;
    for (int32_t i = 0; i < a.length(); i += UTF_CHAR_LENGTH(cp)) {
        cp = a.char32At(i);
        int8_t t = u_charType(cp);
        switch (t) {
        case U_UPPERCASE_LETTER:
            if (haveLower) return TRUE;
            break;
        case U_TITLECASE_LETTER:
            if (haveLower) return TRUE;
            // drop through, since second letter is lower.
        case U_LOWERCASE_LETTER:
            haveLower = TRUE;
            break;
        }
    }
    return FALSE;
}

void RTTest::test(const UnicodeString& sourceRangeVal,
                  const UnicodeString& targetRangeVal,
                  const char* roundtripExclusions,
                  IntlTest* logVal,
                  Legal* adoptedLegal) {

    UErrorCode status = U_ZERO_ERROR;

    this->log = logVal;
    this->legalSource = adoptedLegal;
    LegalDeleter cleaner(adoptedLegal, this->legalSource);

    if (sourceRangeVal.length() > 0) {
        this->sourceRange.applyPattern(sourceRangeVal, status);
        if (U_FAILURE(status)) {
            log->errln("FAIL: UnicodeSet::applyPattern(" +
                       sourceRangeVal + ")");
            return;
        }
    } else {
        this->sourceRange.applyPattern("[a-zA-Z]", status);
        if (U_FAILURE(status)) {
            log->errln("FAIL: UnicodeSet::applyPattern([a-z])");
            return;
        }
    }
    this->targetRange.clear();
    if (targetRangeVal.length() > 0) {
        this->targetRange.applyPattern(targetRangeVal, status);
        if (U_FAILURE(status)) {
            log->errln("FAIL: UnicodeSet::applyPattern(" +
                       targetRangeVal + ")");
            return;
        }
    }
    this->roundtripExclusions.clear();
    if (roundtripExclusions != NULL) {
        UErrorCode ec = U_ZERO_ERROR;
        this->roundtripExclusions.applyPattern(roundtripExclusions, ec);
    }
    if (badCharacters.isEmpty()) {
        UErrorCode ec = U_ZERO_ERROR;
        badCharacters.applyPattern("[:Other:]", ec);
    }

    test2();

    if (errorCount > 0) {
        log->errln(transliteratorID + " errors: " + errorCount); // + ", see " + logFileName);
    } else {
        log->logln(transliteratorID + " ok");
    }
}

void RTTest::logWrongScript(const UnicodeString& label,
                            const UnicodeString& from,
                            const UnicodeString& to) {
    log->errln((UnicodeString)"Fail " +
               label + ": " +
               from + "(" + TestUtility::hex(from) + ") => " +
               to + "(" + TestUtility::hex(to) + ")");
    ++errorCount;
}

void RTTest::logNotCanonical(const UnicodeString& label,
                             const UnicodeString& from,
                             const UnicodeString& to,
                             const UnicodeString& toCan) {
    log->errln((UnicodeString)"Fail (can.equiv)" +
               label + ": " +
               from + "(" + TestUtility::hex(from) + ") => " +
               to + "(" + TestUtility::hex(to) + ")" +
               toCan + " (" +
               TestUtility::hex(to) + ")"
               );
    ++errorCount;
}

void RTTest::logRoundTripFailure(const UnicodeString& from,
                                 const UnicodeString& to,
                                 const UnicodeString& back) {
    if (!legalSource->is(from)) return; // skip illegals

    log->errln((UnicodeString)"Fail Roundtrip: " +
               from + "(" + TestUtility::hex(from) + ") => " +
               to + "(" + TestUtility::hex(to) + ") => " +
               back + "(" + TestUtility::hex(back) + ") => ");
    ++errorCount;
}

/*
 * Characters to filter for source-target mapping completeness
 * Typically is base alphabet, minus extended characters
 * Default is ASCII letters for Latin
 */
UBool RTTest::isSource(UChar c) {
    return (TestUtility::getScript(c) == sourceScript && u_isalpha(c)
        && sourceRange.contains(c));
}

/*
 * Characters to check for target back to source mapping.
 * Typically the same as the target script, plus punctuation
 */
inline UBool
RTTest::isReceivingSource(UChar c) {
    int8_t script = TestUtility::getScript(c);
    return (script == sourceScript || script == TestUtility::COMMON_SCRIPT);
}

/*
 * Characters to filter for target-source mapping
 * Typically is base alphabet, minus extended characters
 */
inline UBool
RTTest::isTarget(UChar c) {
    return (TestUtility::getScript(c) == targetScript && u_isalpha(c)
        && (targetRange.isEmpty() || targetRange.contains(c)));
}

/*
 * Characters to check for target-source mapping
 * Typically the same as the source script, plus punctuation
 */
inline UBool
RTTest::isReceivingTarget(UChar c) {
    int8_t script = TestUtility::getScript(c);
    return (script == targetScript || script == TestUtility::COMMON_SCRIPT);
}

UBool RTTest::isSource(const UnicodeString& s) {
    int32_t length = s.length();
    for (int32_t i = 0; i < length; ++i) {
        if (!isSource(s.charAt(i)))
            return FALSE;
    }
    return TRUE;
}

UBool RTTest::isTarget(const UnicodeString& s) {
    int32_t length = s.length();
    for (int32_t i = 0; i < length; ++i) {
        if (!isTarget(s.charAt(i)))
            return FALSE;
    }
    return TRUE;
}

UBool RTTest::isReceivingSource(const UnicodeString& s) {
    int32_t length = s.length();
    for (int32_t i = 0; i < length; ++i) {
        if (!isReceivingSource(s.charAt(i)))
            return FALSE;
    }
    return TRUE;
}

UBool RTTest::isReceivingTarget(const UnicodeString& s) {
    int32_t length = s.length();
    for (int32_t i = 0; i < length; ++i) {
        if (!isReceivingTarget(s.charAt(i)))
            return FALSE;
    }
    return TRUE;
}

//--------------------------------------------------------------------
// Specific Tests
//--------------------------------------------------------------------

void TransliteratorRoundTripTest::TestHiragana() {
    RTTest test("Latin-Hiragana",
                TestUtility::LATIN_SCRIPT, TestUtility::HIRAGANA_SCRIPT);
    test.test("[a-z]", UnicodeString("[\\u3040-\\u3094]", ""), NULL, this, new Legal());
}

void TransliteratorRoundTripTest::TestKatakana() {
    RTTest test("Latin-Katakana", 
                TestUtility::LATIN_SCRIPT, TestUtility::KATAKANA_SCRIPT);
    test.test("[a-z]", UnicodeString("[\\u30A1-\\u30FA\\u30FC]", ""), NULL, this, new Legal());
}

void TransliteratorRoundTripTest::TestArabic() {
//  RTTest test("Latin-Arabic", 
//              TestUtility::LATIN_SCRIPT, TestUtility::ARABIC_SCRIPT);
//  test.test("[a-z]", UnicodeString("[\\u0620-\\u065F-[\\u0640]]", ""), this, new Legal());
}

void TransliteratorRoundTripTest::TestHebrew() {
//  RTTest test("Latin-Hebrew", 
//              TestUtility::LATIN_SCRIPT, TestUtility::HEBREW_SCRIPT);
//  test.test("", UnicodeString("[\\u05D0-\\u05EF]", ""), this, new Legal());
}

void TransliteratorRoundTripTest::TestJamo() {
    RTTest t("Latin-Jamo", 
             TestUtility::LATIN_SCRIPT, TestUtility::JAMO_SCRIPT);
    t.setErrorLimit(200); // Don't run full test -- too long
    t.test("", "", NULL, this, new Legal());
}

void TransliteratorRoundTripTest::TestJamoHangul() {
    RTTest t("Latin-Hangul", 
             TestUtility::LATIN_SCRIPT, TestUtility::HANGUL_SCRIPT);
    t.setErrorLimit(50); // Don't run full test -- too long
    t.test("", "", NULL, this, new Legal());
}

void TransliteratorRoundTripTest::TestGreek() {
    RTTest test("Latin-Greek", 
                TestUtility::LATIN_SCRIPT, TestUtility::GREEK_SCRIPT);
    test.test("", UnicodeString("[\\u003B\\u00B7[:Greek:]-[\\u03D7-\\u03EF]]", ""),
              "[\\u037A\\u03D0-\\u03F5]", /* exclusions */
              this, new LegalGreek(TRUE));
}

void TransliteratorRoundTripTest::Testel() {
    RTTest test("Latin-el", 
                TestUtility::LATIN_SCRIPT, TestUtility::GREEK_SCRIPT);
    test.test("", "[\\u003B\\u00B7[:Greek:]-[\\u03D7-\\u03EF]]", 
              "[\\u037A\\u03D0-\\u03F5]", /* exclusions */
              this, new LegalGreek(FALSE));
    }

void TransliteratorRoundTripTest::TestCyrillic() {
    RTTest test("Latin-Cyrillic", 
                TestUtility::LATIN_SCRIPT, TestUtility::CYRILLIC_SCRIPT);
    test.test("", UnicodeString("[\\u0400-\\u045F]", ""), NULL, this, new Legal());
}


//----------------------------------
// Inter-Indic Tests
//----------------------------------
void TransliteratorRoundTripTest::TestDevanagariLatin() {
    RTTest test("Latin-DEVANAGARI", 
                TestUtility::LATIN_SCRIPT, TestUtility::DEVANAGARI_SCRIPT);
    test.test("", "[:Devanagari:]", NULL, this, new Legal());
}

void TransliteratorRoundTripTest::TestDevanagariBengali()  {
    RTTest test("BENGALI-DEVANAGARI", 
        TestUtility::BENGALI_SCRIPT, TestUtility::DEVANAGARI_SCRIPT);
         test.test("[:BENGALI:]", "[:Devanagari:]", 
                "[\\u0950\\u0935\\u0912\\u0933\\u090e\\u090D\\u0911\\u093d\\u0929\\u0934[\\u0958-\\u095f]\\u09F0\\u09F1]", /*roundtrip exclusions*/
                this, new Legal());
    RTTest test1("DEVANAGARI-BENGALI", 
            TestUtility::DEVANAGARI_SCRIPT, TestUtility::BENGALI_SCRIPT );
          test1.test( "[:Devanagari:]", "[:BENGALI:]",
                  "[\\u0950\\u0935\\u0912\\u0933\\u090e\\u090D\\u0911\\u093d\\u0929\\u0934[\\u0958-\\u095f]\\u09F0\\u09F1]", /*roundtrip exclusions*/
                  this, new Legal());
}
void TransliteratorRoundTripTest::TestDevanagariGurmukhi()  {
    RTTest test("GURMUKHI-DEVANAGARI", 
      TestUtility::GURMUKHI_SCRIPT, TestUtility::DEVANAGARI_SCRIPT);
      test.test("[:GURMUKHI:]", "[:Devanagari:]", 
            "[\\u0950\\u090D\\u090e\\u0912\\u0911\\u090b\\u090c\\u0934\\u0960\\u0961\\u0937\\u0a72\\u0a73\\u0a74\\u093d]", /*roundtrip exclusions*/
            this, new Legal());
    RTTest test1("DEVANAGARI-GURMUKHI", 
        TestUtility::DEVANAGARI_SCRIPT, TestUtility::GURMUKHI_SCRIPT );
      test1.test( "[:Devanagari:]", "[:GURMUKHI:]",
              "[\\u0950\\u090D\\u090e\\u0912\\u0911\\u090b\\u090c\\u0934\\u0960\\u0961\\u0937\\u0a72\\u0a73\\u0a74\\u093d]", /*roundtrip exclusions*/
              this, new Legal());
} 
void TransliteratorRoundTripTest::TestDevanagariGujarati()  {
    RTTest test("GUJARATI-DEVANAGARI", 
      TestUtility::GUJARATI_SCRIPT, TestUtility::DEVANAGARI_SCRIPT);
      test.test("[:GUJARATI:]", "[:Devanagari:]", 
            "[\\u0961\\u090c\\u090e\\u0912]", /*roundtrip exclusions*/
            this, new Legal());
    RTTest test1("DEVANAGARI-GUJARATI", 
        TestUtility::DEVANAGARI_SCRIPT, TestUtility::GUJARATI_SCRIPT );
      test1.test( "[:Devanagari:]", "[:GUJARATI:]",NULL,
              this, new Legal());
}
void TransliteratorRoundTripTest::TestDevanagariOriya()  {
    RTTest test("ORIYA-DEVANAGARI", 
      TestUtility::ORIYA_SCRIPT, TestUtility::DEVANAGARI_SCRIPT);
      test.test("[:ORIYA:]", "[:Devanagari:]", 
            "[\\u0950\\u090D\\u090e\\u0912\\u0911\\u0931\\u0935]", /*roundtrip exclusions*/
            this, new Legal());
    RTTest test1("DEVANAGARI-ORIYA", 
        TestUtility::DEVANAGARI_SCRIPT, TestUtility::ORIYA_SCRIPT );
      test1.test( "[:Devanagari:]", "[:ORIYA:]",
              "[\\u0950\\u090D\\u090e\\u0912\\u0911\\u0931\\u0935]", /*roundtrip exclusions*/ 
              this, new Legal());
}
void TransliteratorRoundTripTest::TestDevanagariTamil()  {
    RTTest test("Tamil-DEVANAGARI", 
      TestUtility::TAMIL_SCRIPT, TestUtility::DEVANAGARI_SCRIPT);
      test.test("[:tamil:]", "[:Devanagari:]", 
              "[\\u0950\\u090D\\u0911\\u093d\\u0929\\u0934[\\u0958-\\u095f]"
              "\\u090B\\u090C\\u0916\\u0917\\u0918\\u091B\\u091D\\u0920\\u0921"
              "\\u0922\\u0925\\u0926\\u0927\\u092B\\u092C\\u092D\\u0936\\u0960\\u0961]", /*roundtrip exclusions*/
              this, new Legal());
    RTTest test1("DEVANAGARI-Tamil", 
        TestUtility::DEVANAGARI_SCRIPT, TestUtility::TAMIL_SCRIPT );
      test1.test( "[:Devanagari:]", "[:tamil:]",
              "[\\u0950\\u090D\\u0911\\u093d\\u0929\\u0934[\\u0958-\\u095f]]", /*roundtrip exclusions*/
              this, new Legal());
}
void TransliteratorRoundTripTest::TestDevanagariTelugu()  {
    RTTest test("Telugu-DEVANAGARI", 
      TestUtility::TELUGU_SCRIPT, TestUtility::DEVANAGARI_SCRIPT);
      test.test("[:telugu:]", "[:Devanagari:]", 
            "[\\u0950\\u090D\\u0911\\u093d\\u0929\\u0934[\\u0958-\\u095f]]", /*roundtrip exclusions*/
            this, new Legal());
    RTTest test1("DEVANAGARI-TELUGU", 
        TestUtility::DEVANAGARI_SCRIPT, TestUtility::TELUGU_SCRIPT );
      test1.test( "[:Devanagari:]", "[:TELUGU:]",
              "[\\u0950\\u090D\\u0911\\u093d\\u0929\\u0934[\\u0958-\\u095f]]", /*roundtrip exclusions*/
              this, new Legal());
}
void TransliteratorRoundTripTest::TestDevanagariKannada()  {
    RTTest test("KANNADA-DEVANAGARI", 
      TestUtility::KANNADA_SCRIPT, TestUtility::DEVANAGARI_SCRIPT);
      test.test("[:KANNADA:]", "[:Devanagari:]", 
            "[\\u0950\\u090D\\u0911\\u093d\\u0929\\u0934[\\u0958-\\u095f]]", /*roundtrip exclusions*/
            this, new Legal());
    RTTest test1("DEVANAGARI-KANNADA", 
        TestUtility::DEVANAGARI_SCRIPT, TestUtility::KANNADA_SCRIPT );
      test1.test( "[:Devanagari:]", "[:KANNADA:]",
              "[\\u0950\\u090D\\u0911\\u093d\\u0929\\u0934[\\u0958-\\u095f]]", /*roundtrip exclusions*/
              this, new Legal());
}
void TransliteratorRoundTripTest::TestDevanagariMalayalam()  {
    RTTest test("MALAYALAM-DEVANAGARI", 
      TestUtility::MALAYALAM_SCRIPT, TestUtility::DEVANAGARI_SCRIPT);
      test.test("[:MALAYALAM:]", "[:Devanagari:]", 
            "[\\u0950\\u090D\\u0911\\u093d\\u0929\\u0934[\\u0958-\\u095f]]", /*roundtrip exclusions*/
            this, new Legal());
    RTTest test1("DEVANAGARI-MALAYALAM", 
        TestUtility::DEVANAGARI_SCRIPT, TestUtility::MALAYALAM_SCRIPT );
      test1.test( "[:Devanagari:]", "[:MALAYALAM:]",
              "[\\u0950\\u090D\\u0911\\u093d\\u0929\\u0934[\\u0958-\\u095f]]", /*roundtrip exclusions*/
              this, new Legal());
}
void TransliteratorRoundTripTest::TestBengaliGurmukhi()  {
    RTTest test("GURMUKHI-BENGALI", 
      TestUtility::GURMUKHI_SCRIPT, TestUtility::BENGALI_SCRIPT);
      test.test("[:GURMUKHI:]", "[:BENGALI:]",  
            "[\\u098B\\u098C\\u09B7\\u09E0\\u09E1\\u09F0\\u09F1]", /*roundtrip exclusions*/
            this, new Legal());
    RTTest test1("BENGALI-GURMUKHI", 
        TestUtility::BENGALI_SCRIPT, TestUtility::GURMUKHI_SCRIPT);
      test1.test( "[:BENGALI:]", "[:GURMUKHI:]",
              "[\\u0A33\\u0A35\\u0A59\\u0A5A\\u0A5B\\u0A5E\\u0A72\\u0A73\\u0A74]", /*roundtrip exclusions*/
              this, new Legal());
} 
void TransliteratorRoundTripTest::TestBengaliGujarati()  {
    RTTest test("GUJARATI-BENGALI", 
      TestUtility::GUJARATI_SCRIPT, TestUtility::BENGALI_SCRIPT);
      test.test("[:GUJARATI:]", "[:BENGALI:]", 
            "[\\u098c\\u09e1\\u09f0\\u09f1]", /*roundtrip exclusions*/
            this, new Legal());
    RTTest test1("BENGALI-GUJARATI", 
        TestUtility::BENGALI_SCRIPT, TestUtility::GUJARATI_SCRIPT);
      test1.test( "[:BENGALI:]", "[:GUJARATI:]",
              "[\\u0A8D\\u0A91\\u0AB3\\u0AB5\\u0ABD\\u0AD0]", /*roundtrip exclusions*/
              this, new Legal());
}
void TransliteratorRoundTripTest::TestBengaliOriya()  {
    RTTest test("ORIYA-BENGALI", 
      TestUtility::ORIYA_SCRIPT, TestUtility::BENGALI_SCRIPT);
      test.test("[:ORIYA:]", "[:BENGALI:]", 
            "[\\u09f0\\u09f1]", /*roundtrip exclusions*/
            this, new Legal());
    RTTest test1("BENGALI-ORIYA", 
        TestUtility::BENGALI_SCRIPT, TestUtility::ORIYA_SCRIPT);
      test1.test( "[:BENGALI:]", "[:ORIYA:]",
              "[\\u0b33\\u0b3d]", /*roundtrip exclusions*/
              this, new Legal());
}
void TransliteratorRoundTripTest::TestBengaliTamil()  {
    RTTest test("Tamil-BENGALI", 
      TestUtility::TAMIL_SCRIPT, TestUtility::BENGALI_SCRIPT);
      test.test("[:tamil:]", "[:BENGALI:]", 
              "[\\u09f0\\u09f1\\u098B\\u098C\\u0996\\u0997\\u0998\\u099B\\u099D\\u09A0\\u09A1\\u09A2\\u09A5\\u09A6\\u09A7\\u09AB\\u09AC\\u09AD\\u09B6\\u09DC\\u09DD\\u09DF\\u09E0\\u09E1]", /*roundtrip exclusions*/
              this, new Legal());
    RTTest test1("BENGALI-Tamil", 
        TestUtility::BENGALI_SCRIPT, TestUtility::TAMIL_SCRIPT);
      test1.test( "[:BENGALI:]", "[:tamil:]",
              "[\\u0B8E\\u0B92\\u0BA9\\u0BB1\\u0BB3\\u0BB4\\u0BB5]", /*roundtrip exclusions*/
              this, new Legal());
}
void TransliteratorRoundTripTest::TestBengaliTelugu()  {
    RTTest test("Telugu-BENGALI", 
      TestUtility::TELUGU_SCRIPT, TestUtility::BENGALI_SCRIPT);
      test.test("[:telugu:]", "[:BENGALI:]", 
            "[\\u09f0\\u09f1\\u09dc\\u09dd\\u09df]", /*roundtrip exclusions*/
            this, new Legal());
    RTTest test1("BENGALI-TELUGU", 
        TestUtility::BENGALI_SCRIPT, TestUtility::TELUGU_SCRIPT);
      test1.test( "[:BENGALI:]", "[:TELUGU:]",
              "[\\u0C0E\\u0C12\\u0C31\\u0C33\\u0C35]", /*roundtrip exclusions*/
              this, new Legal());
}
void TransliteratorRoundTripTest::TestBengaliKannada()  {
    RTTest test("KANNADA-BENGALI", 
      TestUtility::KANNADA_SCRIPT, TestUtility::BENGALI_SCRIPT);
      test.test("[:KANNADA:]", "[:BENGALI:]", 
            "[\\u09f0\\u09f1\\u09dc\\u09dd\\u09df]", /*roundtrip exclusions*/
            this, new Legal());
    RTTest test1("BENGALI-KANNADA", 
        TestUtility::BENGALI_SCRIPT, TestUtility::KANNADA_SCRIPT);
      test1.test( "[:BENGALI:]", "[:KANNADA:]",
              "[\\u0C8E\\u0C92\\u0CB1\\u0cb3\\u0cb5\\u0cde]", /*roundtrip exclusions*/ 
              this, new Legal());
}
void TransliteratorRoundTripTest::TestBengaliMalayalam()  {
    RTTest test("MALAYALAM-BENGALI", 
      TestUtility::MALAYALAM_SCRIPT, TestUtility::BENGALI_SCRIPT);
      test.test("[:MALAYALAM:]", "[:BENGALI:]", 
            "[\\u09f0\\u09f1\\u09dc\\u09dd\\u09df]", /*roundtrip exclusions*/
            this, new Legal());
    RTTest test1("BENGALI-MALAYALAM", 
        TestUtility::BENGALI_SCRIPT, TestUtility::MALAYALAM_SCRIPT);
      test1.test( "[:BENGALI:]", "[:MALAYALAM:]",
              "[\\u0d31-\\u0d35\\u0d0e\\u0d12]", /*roundtrip exclusions*/
              this, new Legal());
} 
void TransliteratorRoundTripTest::TestGurmukhiGujarati()  {
    RTTest test("GUJARATI-GURMUKHI", 
      TestUtility::GUJARATI_SCRIPT, TestUtility::GURMUKHI_SCRIPT);
      test.test("[:GUJARATI:]", "[:GURMUKHI:]", 
            "[\\u0a5c\\u0a72\\u0a73\\u0a74\\u0a8b\\u0a8d\\u0a91\\u0abd]", /*roundtrip exclusions*/
            this, new Legal());
    RTTest test1("GURMUKHI-GUJARATI", 
        TestUtility::GURMUKHI_SCRIPT, TestUtility::GUJARATI_SCRIPT);;
      test1.test( "[:GURMUKHI:]", "[:GUJARATI:]",
              "[\\u0a5c\\u0a72\\u0a73\\u0a74\\u0a8b\\u0a8d\\u0a91\\u0ab7\\u0abd\\u0ad0\\u0ae0]", /*roundtrip exclusions*/
              this, new Legal());
}
void TransliteratorRoundTripTest::TestGurmukhiOriya()  {
    RTTest test("ORIYA-GURMUKHI", 
      TestUtility::ORIYA_SCRIPT, TestUtility::GURMUKHI_SCRIPT);
      test.test("[:ORIYA:]", "[:GURMUKHI:]", 
            "[\\u0B0B\\u0B0C\\u0B37\\u0B3D\\u0B5F\\u0B60\\u0B61\\u0a35\\u0a72\\u0a73\\u0a74]", /*roundtrip exclusions*/
            this, new Legal());
    RTTest test1("GURMUKHI-ORIYA", 
        TestUtility::GURMUKHI_SCRIPT, TestUtility::ORIYA_SCRIPT);;
      test1.test( "[:GURMUKHI:]", "[:ORIYA:]",
              "[\\u0B0B\\u0B0C\\u0B37\\u0B3D\\u0B5F\\u0B60\\u0B61]", /*roundtrip exclusions*/
              this, new Legal());
}
void TransliteratorRoundTripTest::TestGurmukhiTamil()  {
    RTTest test("TAMIL-GURMUKHI", 
      TestUtility::TAMIL_SCRIPT, TestUtility::GURMUKHI_SCRIPT);
      test.test("[:TAMIL:]", "[:GURMUKHI:]", 
            "[\\u0A16\\u0A17\\u0A18\\u0A1B\\u0A1D\\u0A20\\u0A21\\u0A22\\u0A25\\u0A26\\u0A27\\u0A2B\\u0A2C\\u0A2D\\u0A59\\u0A5A\\u0A5B\\u0A5C\\u0A5E\\u0A72\\u0A73\\u0A74]", /*roundtrip exclusions*/
            this, new Legal());
    RTTest test1("GURMUKHI-TAMIL", 
        TestUtility::GURMUKHI_SCRIPT, TestUtility::TAMIL_SCRIPT);;
      test1.test( "[:GURMUKHI:]", "[:TAMIL:]",
              "[\\u0B8E\\u0B92\\u0BA9\\u0BB1\\u0BB4]", /*roundtrip exclusions*/
              this, new Legal());
}
void TransliteratorRoundTripTest::TestGurmukhiTelugu()  {
    RTTest test("TELUGU-GURMUKHI", 
      TestUtility::TELUGU_SCRIPT, TestUtility::GURMUKHI_SCRIPT);
      test.test("[:TELUGU:]", "[:GURMUKHI:]", 
            "[\\u0A59\\u0A5A\\u0A5B\\u0A5C\\u0A5E\\u0A72\\u0A73\\u0A74]", /*roundtrip exclusions*/
            this, new Legal());
    RTTest test1("GURMUKHI-TELUGU", 
        TestUtility::GURMUKHI_SCRIPT, TestUtility::TELUGU_SCRIPT);;
      test1.test( "[:GURMUKHI:]", "[:TELUGU:]",
              "[\\u0C0B\\u0C0C\\u0C0E\\u0C12\\u0C31\\u0C37\\u0C60\\u0C61]", /*roundtrip exclusions*/
              this, new Legal());
}
void TransliteratorRoundTripTest::TestGurmukhiKannada()  {
    RTTest test("KANNADA-GURMUKHI", 
      TestUtility::KANNADA_SCRIPT, TestUtility::GURMUKHI_SCRIPT);
      test.test("[:KANNADA:]", "[:GURMUKHI:]", 
            "[\\u0A59\\u0A5A\\u0A5B\\u0A5C\\u0A5E\\u0A72\\u0A73\\u0A74]", /*roundtrip exclusions*/
            this, new Legal());
    RTTest test1("GURMUKHI-KANNADA", 
        TestUtility::GURMUKHI_SCRIPT, TestUtility::KANNADA_SCRIPT);;
      test1.test( "[:GURMUKHI:]", "[:KANNADA:]",
              "[\\u0C8B\\u0C8C\\u0C8E\\u0C92\\u0CB1\\u0CB7\\u0CE0\\u0CE1]", /*roundtrip exclusions*/
              this, new Legal());
}
void TransliteratorRoundTripTest::TestGurmukhiMalayalam()  {
    RTTest test("MALAYALAM-GURMUKHI", 
      TestUtility::MALAYALAM_SCRIPT, TestUtility::GURMUKHI_SCRIPT);
      test.test("[:MALAYALAM:]", "[:GURMUKHI:]", 
            "[\\u0A59\\u0A5A\\u0A5B\\u0A5C\\u0A5E\\u0A72\\u0A73\\u0A74]", /*roundtrip exclusions*/
            this, new Legal());
    RTTest test1("GURMUKHI-MALAYALAM", 
        TestUtility::GURMUKHI_SCRIPT, TestUtility::MALAYALAM_SCRIPT);;
      test1.test( "[:GURMUKHI:]", "[:MALAYALAM:]",
              "[\\u0D0B\\u0D0C\\u0D0E\\u0D12\\u0D31\\u0D34\\u0D37\\u0D60\\u0D61]", /*roundtrip exclusions*/
              this, new Legal());
}

 void TransliteratorRoundTripTest::TestGujaratiOriya()  {
    RTTest test("GUJARATI-ORIYA", 
      TestUtility::GUJARATI_SCRIPT, TestUtility::ORIYA_SCRIPT);
      test.test("[:GUJARATI:]", "[:ORIYA:]", 
            "[\\u0B0C\\u0B5F\\u0B61]", /*roundtrip exclusions*/
            this, new Legal());
    RTTest test1("ORIYA-GUJARATI", 
        TestUtility::ORIYA_SCRIPT, TestUtility::GUJARATI_SCRIPT);;
      test1.test( "[:ORIYA:]", "[:GUJARATI:]",
              "[\\u0A8D\\u0A91\\u0AB5\\u0Ad0]", /*roundtrip exclusions*/
              this, new Legal());
}
void TransliteratorRoundTripTest::TestGujaratiTamil()  {
    RTTest test("TAMIL-GUJARATI", 
      TestUtility::TAMIL_SCRIPT, TestUtility::GUJARATI_SCRIPT);
      test.test("[:TAMIL:]", "[:GUJARATI:]", 
            "[\\u0A8B\\u0A8D\\u0A91\\u0A96\\u0A97\\u0A98\\u0A9B\\u0A9D\\u0AA0\\u0AA1\\u0AA2\\u0AA5\\u0AA6\\u0AA7\\u0AAB\\u0AAC\\u0AAD\\u0AB6\\u0ABD\\u0AD0\\u0AE0]", /*roundtrip exclusions*/
            this, new Legal());
    RTTest test1("GUJARATI-TAMIL", 
        TestUtility::GUJARATI_SCRIPT, TestUtility::TAMIL_SCRIPT);;
      test1.test( "[:GUJARATI:]", "[:TAMIL:]",
              "[\\u0B8E\\u0B92\\u0BA9\\u0BB1\\u0BB4]", /*roundtrip exclusions*/
              this, new Legal());
}
void TransliteratorRoundTripTest::TestGujaratiTelugu()  {
    RTTest test("TELUGU-GUJARATI", 
      TestUtility::TELUGU_SCRIPT, TestUtility::GUJARATI_SCRIPT);
      test.test("[:TELUGU:]", "[:GUJARATI:]", 
            "[\\u0A8D\\u0A91\\u0ABD\\u0Ad0]", /*roundtrip exclusions*/
            this, new Legal());
    RTTest test1("GUJARATI-TELUGU", 
        TestUtility::GUJARATI_SCRIPT, TestUtility::TELUGU_SCRIPT);;
      test1.test( "[:GUJARATI:]", "[:TELUGU:]",
              "[\\u0C0C\\u0C0E\\u0C12\\u0C31\\u0C61]", /*roundtrip exclusions*/
              this, new Legal());
}
void TransliteratorRoundTripTest::TestGujaratiKannada()  {
    RTTest test("KANNADA-GUJARATI", 
      TestUtility::KANNADA_SCRIPT, TestUtility::GUJARATI_SCRIPT);
      test.test("[:KANNADA:]", "[:GUJARATI:]", 
            "[\\u0A8D\\u0A91\\u0ABD\\u0Ad0]", /*roundtrip exclusions*/
            this, new Legal());
    RTTest test1("GUJARATI-KANNADA", 
        TestUtility::GUJARATI_SCRIPT, TestUtility::KANNADA_SCRIPT);;
      test1.test( "[:GUJARATI:]", "[:KANNADA:]",
              "[\\u0C8C\\u0C8E\\u0C92\\u0CB1\\u0CDE\\u0CE1]", /*roundtrip exclusions*/
              this, new Legal());
}
void TransliteratorRoundTripTest::TestGujaratiMalayalam()  {
    RTTest test("MALAYALAM-GUJARATI", 
      TestUtility::MALAYALAM_SCRIPT, TestUtility::GUJARATI_SCRIPT);
      test.test("[:MALAYALAM:]", "[:GUJARATI:]", 
            "[\\u0A8D\\u0A91\\u0ABD\\u0Ad0]", /*roundtrip exclusions*/
            this, new Legal());
    RTTest test1("GUJARATI-MALAYALAM", 
        TestUtility::GUJARATI_SCRIPT, TestUtility::MALAYALAM_SCRIPT);;
      test1.test( "[:GUJARATI:]", "[:MALAYALAM:]",
              "[\\u0D0C\\u0D0E\\u0D12\\u0D31\\u0D34\\u0D61]", /*roundtrip exclusions*/
              this, new Legal());
}
void TransliteratorRoundTripTest::TestOriyaTamil()  {
    RTTest test("TAMIL-ORIYA", 
      TestUtility::TAMIL_SCRIPT, TestUtility::ORIYA_SCRIPT);
      test.test("[:TAMIL:]", "[:ORIYA:]", 
            "[\\u0B0B\\u0B0C\\u0B16\\u0B17\\u0B18\\u0B1B\\u0B1D\\u0B20\\u0B21\\u0B22\\u0B25\\u0B26\\u0B27\\u0B2B\\u0B2C\\u0B2D\\u0B36\\u0B3D\\u0B5C\\u0B5D\\u0B5F\\u0B60\\u0B61]", /*roundtrip exclusions*/
            this, new Legal());
    RTTest test1("ORIYA-TAMIL", 
        TestUtility::ORIYA_SCRIPT, TestUtility::TAMIL_SCRIPT);;
      test1.test( "[:ORIYA:]", "[:TAMIL:]",
              "[\\u0B8E\\u0B92\\u0BA9\\u0BB1\\u0BB4\\u0BB5]", /*roundtrip exclusions*/
              this, new Legal());
}
void TransliteratorRoundTripTest::TestOriyaTelugu()  {
    RTTest test("TELUGU-ORIYA", 
      TestUtility::TELUGU_SCRIPT, TestUtility::ORIYA_SCRIPT);
      test.test("[:TELUGU:]", "[:ORIYA:]", 
            "[\\u0B3D\\u0B5C\\u0B5D\\u0B5F]", /*roundtrip exclusions*/
            this, new Legal());
    RTTest test1("ORIYA-TELUGU", 
        TestUtility::ORIYA_SCRIPT, TestUtility::TELUGU_SCRIPT);;
      test1.test( "[:ORIYA:]", "[:TELUGU:]",
              "[\\u0C0E\\u0C12\\u0C31\\u0C35]", /*roundtrip exclusions*/
              this, new Legal());
}
void TransliteratorRoundTripTest::TestOriyaKannada()  {
    RTTest test("KANNADA-ORIYA", 
      TestUtility::KANNADA_SCRIPT, TestUtility::ORIYA_SCRIPT);
      test.test("[:KANNADA:]", "[:ORIYA:]", 
            "[\\u0B3D\\u0B5C\\u0B5D\\u0B5F]", /*roundtrip exclusions*/
            this, new Legal());
    RTTest test1("ORIYA-KANNADA", 
        TestUtility::ORIYA_SCRIPT, TestUtility::KANNADA_SCRIPT);;
      test1.test( "[:ORIYA:]", "[:KANNADA:]",
              "[\\u0C8E\\u0C92\\u0CB1\\u0CB5\\u0CDE]", /*roundtrip exclusions*/
              this, new Legal());
}
void TransliteratorRoundTripTest::TestOriyaMalayalam()  {
    RTTest test("MALAYALAM-ORIYA", 
      TestUtility::MALAYALAM_SCRIPT, TestUtility::ORIYA_SCRIPT);
      test.test("[:MALAYALAM:]", "[:ORIYA:]", 
            "[\\u0B3D\\u0B5C\\u0B5D\\u0B5F]", /*roundtrip exclusions*/
            this, new Legal());
    RTTest test1("ORIYA-MALAYALAM", 
        TestUtility::ORIYA_SCRIPT, TestUtility::MALAYALAM_SCRIPT);;
      test1.test( "[:ORIYA:]", "[:MALAYALAM:]",
              "[\\u0D0E\\u0D12\\u0D31\\u0D34\\u0D35]", /*roundtrip exclusions*/
              this, new Legal());
}

void TransliteratorRoundTripTest::TestTamilTelugu()  {
    RTTest test("TELUGU-TAMIL", 
      TestUtility::TELUGU_SCRIPT, TestUtility::TAMIL_SCRIPT);
      test.test("[:TELUGU:]", "[:TAMIL:]", 
            "[\\u0ba9\\u0bb4]", /*roundtrip exclusions*/
            this, new Legal());
    RTTest test1("TAMIL-TELUGU", 
        TestUtility::TAMIL_SCRIPT, TestUtility::TELUGU_SCRIPT);;
      test1.test( "[:TAMIL:]", "[:TELUGU:]",
              "[\\u0C0B\\u0C0C\\u0C16\\u0C17\\u0C18\\u0C1B\\u0C1D\\u0C20\\u0C21\\u0C22\\u0C25\\u0C26\\u0C27\\u0C2B\\u0C2C\\u0C2D\\u0C36\\u0C60\\u0C61]", /*roundtrip exclusions*/
              this, new Legal());
}
void TransliteratorRoundTripTest::TestTamilKannada()  {
    RTTest test("KANNADA-TAMIL", 
      TestUtility::KANNADA_SCRIPT, TestUtility::TAMIL_SCRIPT);
      test.test("[:KANNADA:]", "[:TAMIL:]", 
            "[\\u0ba9\\u0bb4]", /*roundtrip exclusions*/
            this, new Legal());
    RTTest test1("TAMIL-KANNADA", 
        TestUtility::TAMIL_SCRIPT, TestUtility::KANNADA_SCRIPT);;
      test1.test( "[:TAMIL:]", "[:KANNADA:]",
              "[\\u0C8B\\u0C8C\\u0C96\\u0C97\\u0C98\\u0C9B\\u0C9D\\u0CA0\\u0CA1\\u0CA2\\u0CA5\\u0CA6\\u0CA7\\u0CAB\\u0CAC\\u0CAD\\u0CB6\\u0CDE\\u0CE0\\u0CE1]", /*roundtrip exclusions*/
              this, new Legal());
}
void TransliteratorRoundTripTest::TestTamilMalayalam()  {
    RTTest test("MALAYALAM-TAMIL", 
      TestUtility::MALAYALAM_SCRIPT, TestUtility::TAMIL_SCRIPT);
      test.test("[:MALAYALAM:]", "[:TAMIL:]", 
            "[\\u0ba9]", /*roundtrip exclusions*/
            this, new Legal());
    RTTest test1("TAMIL-MALAYALAM", 
        TestUtility::TAMIL_SCRIPT, TestUtility::MALAYALAM_SCRIPT);;
      test1.test( "[:TAMIL:]", "[:MALAYALAM:]",
              "[\\u0D0B\\u0D0C\\u0D16\\u0D17\\u0D18\\u0D1B\\u0D1D\\u0D20\\u0D21\\u0D22\\u0D25\\u0D26\\u0D27\\u0D2B\\u0D2C\\u0D2D\\u0D36\\u0D60\\u0D61]", /*roundtrip exclusions*/
              this, new Legal());
}
void TransliteratorRoundTripTest::TestTeluguKannada()  {
    RTTest test("KANNADA-TELUGU", 
      TestUtility::KANNADA_SCRIPT, TestUtility::TELUGU_SCRIPT);
      test.test("[:KANNADA:]", "[:TELUGU:]", 
            "[]", /*roundtrip exclusions*/
            this, new Legal());
    RTTest test1("TELUGU-KANNADA", 
        TestUtility::TELUGU_SCRIPT, TestUtility::KANNADA_SCRIPT);;
      test1.test( "[:TELUGU:]", "[:KANNADA:]",
              "[\\u0CDE]", /*roundtrip exclusions*/
              this, new Legal());
}
void TransliteratorRoundTripTest::TestTeluguMalayalam()  {
    RTTest test("MALAYALAM-TELUGU", 
      TestUtility::MALAYALAM_SCRIPT, TestUtility::TELUGU_SCRIPT);
      test.test("[:MALAYALAM:]", "[:TELUGU:]", 
            "[]", /*roundtrip exclusions*/
            this, new Legal());
    RTTest test1("TELUGU-MALAYALAM", 
        TestUtility::TELUGU_SCRIPT, TestUtility::MALAYALAM_SCRIPT);;
      test1.test( "[:TELUGU:]", "[:MALAYALAM:]",
              "[\\u0D34]", /*roundtrip exclusions*/
              this, new Legal());
}

void TransliteratorRoundTripTest::TestKannadaMalayalam()  {
    RTTest test("MALAYALAM-KANNADA", 
      TestUtility::MALAYALAM_SCRIPT, TestUtility::KANNADA_SCRIPT);
      test.test("[:MALAYALAM:]", "[:KANNADA:]", 
            "[\\u0cDe]", /*roundtrip exclusions*/
            this, new Legal());
    RTTest test1("KANNADA-MALAYALAM", 
        TestUtility::KANNADA_SCRIPT, TestUtility::MALAYALAM_SCRIPT);
      test1.test( "[:KANNADA:]", "[:MALAYALAM:]",
              "[\\u0D34]", /*roundtrip exclusions*/
              this, new Legal());
}

//---------------
// End Indic
//---------------

void RTTest::test2() {

    UChar c;
    UnicodeString cs, targ, reverse;
    int8_t *type = new int8_t[0xFFFF];
    UParseError parseError;
    UErrorCode status = U_ZERO_ERROR;
    Transliterator* sourceToTarget = Transliterator::createInstance(transliteratorID, UTRANS_FORWARD, parseError, status);
    if (sourceToTarget == NULL) {
        log->errln("Fail: createInstance(" + transliteratorID +
                   ") returned NULL");
        return;
    }
    Transliterator* targetToSource = sourceToTarget->createInverse(status);
    if (targetToSource == NULL) {
        log->errln("Fail: " + transliteratorID +
                   ".createInverse() returned NULL");
        delete sourceToTarget;
        return;
    }

    log->logln("Initializing type array");

    for (c = 0; c < 0xFFFF; ++c) {
        type[c] = u_charType(c);
    }

    BitSet failSourceTarg;

    log->logln("Checking that all source characters convert to target - Singles");

    for (c = 0; c < 0xFFFF; ++c) {
        if (type[c] == U_UNASSIGNED || !isSource(c))
            continue;
        cs.remove();
        cs.append(c);
        targ = cs;
        sourceToTarget->transliterate(targ);
        if (!isReceivingTarget(targ) || includesSome(badCharacters, targ)) {
            logWrongScript("Source-Target", cs, targ);
            failSourceTarg.set(c);
            if (errorCount >= errorLimit)
                return;
        } else {
            UnicodeString cs2;
            UErrorCode ec = U_ZERO_ERROR;
            Normalizer::decompose(cs, FALSE, 0, cs2, ec);
            UnicodeString targ2 = cs2;
            sourceToTarget->transliterate(targ2);
            if (targ != targ2) {
                logNotCanonical("Source-Target", cs, targ, targ2);
                if (errorCount >= errorLimit)
                    return;
            }
        }
    }

    log->logln("Checking that all source characters convert to target - Doubles");

    for (c = 0; c < 0xFFFF; ++c) {
        if (type[c] == U_UNASSIGNED ||
            !isSource(c)) continue;
        if (failSourceTarg.get(c)) continue;

        for (UChar d = 0; d < 0xFFFF; ++d) {
            if (type[d] == U_UNASSIGNED || !isSource(d))
                continue;
            if (failSourceTarg.get(d)) continue;

            cs.remove();
            cs.append(c).append(d);
            targ = cs;
            sourceToTarget->transliterate(targ);
            if (!isReceivingTarget(targ) || includesSome(badCharacters, targ)) {
                logWrongScript("Source-Target", cs, targ);
                if (errorCount >= errorLimit)
                    return;
            } else {
                UnicodeString cs2;
                UErrorCode ec = U_ZERO_ERROR;
                Normalizer::decompose(cs, FALSE, 0, cs2, ec);
                UnicodeString targ2 = cs2;
                sourceToTarget->transliterate(targ2);
                if (targ != targ2) {
                    logNotCanonical("Source-Target", cs, targ, targ2);
                    if (errorCount >= errorLimit)
                        return;
                }
            }
        }
    }

    log->logln("Checking that target characters convert to source and back - Singles");

    BitSet failTargSource;
    BitSet failRound;

    for (c = 0; c < 0xFFFF; ++c) {
        if (type[c] == U_UNASSIGNED || !isTarget(c))
            continue;
        cs.remove();
        cs.append(c);
        targ = cs;
        targetToSource->transliterate(targ);
        reverse = targ;
        sourceToTarget->transliterate(reverse);
        if (!isReceivingSource(targ) || includesSome(badCharacters, targ)) {
            logWrongScript("Target-Source", cs, targ);
            failTargSource.set(c);
            if (errorCount >= errorLimit)
                return;
        } else if (!isSame(cs, reverse) && !roundtripExclusions.contains(c)) {
            logRoundTripFailure(cs, targ, reverse);
            failRound.set(c);
            if (errorCount >= errorLimit)
                return;
        } else {
            UnicodeString targ2;
            UErrorCode ec = U_ZERO_ERROR;
            Normalizer::decompose(targ, FALSE, 0, targ2, ec);
            UnicodeString reverse2 = targ2;
            sourceToTarget->transliterate(reverse2);
            if (reverse != reverse2) {
                logNotCanonical("Target-Source", cs, targ, targ2);
                if (errorCount >= errorLimit)
                    return;
            }
        }
    }

    log->logln("Checking that target characters convert to source and back - Doubles");
    int32_t count = 0;
    cs = UNICODE_STRING("aa", 2);
    for (c = 0; c < 0xFFFF; ++c) {
        if (type[c] == U_UNASSIGNED || !isTarget(c))
            continue;
        if (++count > pairLimit) {
            //throw new TestTruncated("Test truncated at " + pairLimit + " x 64k pairs");
            log->logln("");
            log->logln((UnicodeString)"Test truncated at " + pairLimit + " x 64k pairs");
            return;
        }
        cs.setCharAt(0, c);
        log->log(TestUtility::hex(c));
        for (UChar d = 0; d < 0xFFFF; ++d) {
            if (type[d] == U_UNASSIGNED || !isTarget(d))
                continue;
            cs.setCharAt(1, d);
            targ = cs;
            targetToSource->transliterate(targ);
            reverse = targ;
            sourceToTarget->transliterate(reverse);
            if (!isReceivingSource(targ) && !failTargSource.get(c) && !failTargSource.get(d)
                || includesSome(badCharacters, targ)) {
                logWrongScript("Target-Source", cs, targ);
                if (errorCount >= errorLimit)
                    return;
            } else if (!isSame(cs, reverse) && !failRound.get(c) && !failRound.get(d)
                       && !roundtripExclusions.contains(c) && !roundtripExclusions.contains(d)) {
                logRoundTripFailure(cs, targ, reverse);
                if (errorCount >= errorLimit)
                    return;
            } else {
                UnicodeString targ2;
                UErrorCode ec = U_ZERO_ERROR;
                Normalizer::decompose(targ, FALSE, 0, targ2, ec);
                UnicodeString reverse2 = targ2;
                sourceToTarget->transliterate(reverse2);
                if (reverse != reverse2) {
                    logNotCanonical("Target-Source", cs, targ, targ2);
                    if (errorCount >= errorLimit)
                        return;
                }
            }
        }
    }
    log->logln("");
    delete []type;
    delete sourceToTarget;
    delete targetToSource;
}

