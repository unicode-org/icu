/*
**********************************************************************
*   Copyright (C) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   05/23/00    aliu        Creation.
**********************************************************************
*/
#include "transrt.h"
#include "testutil.h"
#include "unicode/utypes.h"
#include "unicode/translit.h"
#include "unicode/rbt.h"
#include "unicode/uniset.h"
#include "unicode/unicode.h"

#define CASE(id,test) case id:                          \
                          name = #test;                 \
                          if (exec) {                   \
                              logln(#test "---");       \
                              logln((UnicodeString)""); \
                              test();                   \
                          }                             \
                          break

void
TransliteratorRoundTripTest::runIndexedTest(int32_t index, UBool exec,
                                   const char* &name, char* /*par*/) {
    switch (index) {
        CASE(0,TestHiragana);
        CASE(1,TestKatakana);
        CASE(2,TestArabic);
        CASE(3,TestHebrew);
        CASE(4,TestGreek);
        CASE(5,TestCyrillic);
        /*
        CASE(7,TestJamo);
        CASE(8,TestJamoHangul);
        */
        default: name = ""; break;
    }
}

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
    IntlTest* log;

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
              const UnicodeString& targetRange, IntlTest* log);

private:

    void test2();

    void logWrongScript(const UnicodeString& label,
                        const UnicodeString& from,
                        const UnicodeString& to);
    void logRoundTripFailure(const UnicodeString& from,
                             const UnicodeString& to,
                             const UnicodeString& back);

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

void RTTest::test(const UnicodeString& sourceRangeVal,
                  const UnicodeString& targetRangeVal, IntlTest* logVal) {

    UErrorCode status = U_ZERO_ERROR;

    this->log = logVal;

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

//|     // make a UTF-8 output file we can read with a browser
//|
//|     // note: check that every transliterator transliterates the null string correctly!
//|
//|     String logFileName = "test_" + transliteratorID + "_"
//|         + sourceScript + "_" + targetScript + ".html";
//|
//|     log.logln("Creating log file " + logFileName);
//|
//|     out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
//|               new FileOutputStream(logFileName), "UTF8"), 4*1024));
//|     //out.write('\uFFEF');    // BOM
//|     out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">");
//|     out.println("<HTML><HEAD>");
//|     out.println("<META content=\"text/html; charset=utf-8\" http-equiv=Content-Type></HEAD>");
//|     out.println("<BODY>");
//|     out.println("<TABLE>");
//|     try {
        test2();
//|         out.println("</TABLE>");
//|     } catch (TestTruncated e) {
//|         out.println("</TABLE>" + e.getMessage());
//|     }
//|     out.println("</BODY></HTML>");
//|     out.close();

    if (errorCount > 0) {
        log->errln(transliteratorID + " errors: " + errorCount); // + ", see " + logFileName);
    } else {
        log->logln(transliteratorID + " ok");
//|         new File(logFileName).delete();
    }
}

void RTTest::logWrongScript(const UnicodeString& label,
                            const UnicodeString& from,
                            const UnicodeString& to) {
//|     out.println("<TR><TD>Fail " + label + ":</TD><TD><FONT SIZE=\"6\">" +
//|                 from + "</FONT></TD><TD>(" +
//|                 TestUtility::hex(from) + ") =></TD><TD><FONT SIZE=\"6\">" +
//|                 to + "</FONT></TD><TD>(" +
//|                 TestUtility::hex(to) + ")</TD></TR>" );
//|     if (++errorCount >= errorLimit) {
//|         throw new TestTruncated("Test truncated; too many failures");
//|     }
    log->errln((UnicodeString)"Fail " +
               label + ": " +
               from + "(" + TestUtility::hex(from) + ") => " +
               to + "(" + TestUtility::hex(to) + ")");
    ++errorCount;
}

void RTTest::logRoundTripFailure(const UnicodeString& from,
                                 const UnicodeString& to,
                                 const UnicodeString& back) {
//|     out.println("<TR><TD>Fail Roundtrip:</TD><TD><FONT SIZE=\"6\">" +
//|                 from + "</FONT></TD><TD>(" +
//|                 TestUtility::hex(from) + ") =></TD><TD>" +
//|                 to + "</TD><TD>(" +
//|                 TestUtility::hex(to) + ") =></TD><TD><FONT SIZE=\"6\">" +
//|                 back + "</TD><TD>(" +
//|                 TestUtility::hex(back) + ")</TD></TR>" );
//|     if (++errorCount >= errorLimit) {
//|         throw new TestTruncated("Test truncated; too many failures");
//|     }
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
    RTTest test("Latin-Kana",
                TestUtility::LATIN_SCRIPT, TestUtility::HIRAGANA_SCRIPT);
    test.test("[a-z]", UnicodeString("[\\u3040-\\u3094]", ""), this);
}

void TransliteratorRoundTripTest::TestKatakana() {
    RTTest test("Latin-Kana", 
                TestUtility::LATIN_SCRIPT, TestUtility::KATAKANA_SCRIPT);
    test.test("[A-Z]", UnicodeString("[\\u30A1-\\u30FA]", ""), this);
}

 void TransliteratorRoundTripTest::TestArabic() {
    RTTest test("Latin-Arabic", 
                TestUtility::LATIN_SCRIPT, TestUtility::ARABIC_SCRIPT);
    test.test("[a-z]", UnicodeString("[\\u0620-\\u065F-[\\u0640]]", ""), this);
}

void TransliteratorRoundTripTest::TestHebrew() {
    RTTest test("Latin-Hebrew", 
                TestUtility::LATIN_SCRIPT, TestUtility::HEBREW_SCRIPT);
    test.test("", UnicodeString("[\\u05D0-\\u05EF]", ""), this);
}

void TransliteratorRoundTripTest::TestJamo() {
    RTTest t("Latin-Jamo", 
             TestUtility::LATIN_SCRIPT, TestUtility::JAMO_SCRIPT);
    t.setErrorLimit(200); // Don't run full test -- too long
    t.test("", "", this);
}

void TransliteratorRoundTripTest::TestJamoHangul() {
    RTTest t("Latin-Jamo;Jamo-Hangul", 
             TestUtility::LATIN_SCRIPT, TestUtility::HANGUL_SCRIPT);
    t.setErrorLimit(50); // Don't run full test -- too long
    t.test("", "", this);
}

void TransliteratorRoundTripTest::TestGreek() {
    RTTest test("Latin-Greek", 
                TestUtility::LATIN_SCRIPT, TestUtility::GREEK_SCRIPT);
    test.test("", UnicodeString("[\\u0380-\\u03CF]", ""), this);
}

void TransliteratorRoundTripTest::TestCyrillic() {
    RTTest test("Latin-Cyrillic", 
                TestUtility::LATIN_SCRIPT, TestUtility::CYRILLIC_SCRIPT);
    test.test("", UnicodeString("[\\u0401\\u0410-\\u044F\\u0451]", ""), this);
}

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

    log->logln("Checking that all source characters convert to target - Singles");

    for (c = 0; c < 0xFFFF; ++c) {
        if (type[c] == U_UNASSIGNED || !isSource(c))
            continue;
        cs.remove();
        cs.append(c);
        targ = cs;
        sourceToTarget->transliterate(targ);
        if (!isReceivingTarget(targ)) {
            logWrongScript("Source-Target", cs, targ);
            if (errorCount >= errorLimit)
                return;
        }
    }

    log->logln("Checking that all source characters convert to target - Doubles");

    for (c = 0; c < 0xFFFF; ++c) {
        if (type[c] == U_UNASSIGNED ||
            !isSource(c)) continue;
        for (UChar d = 0; d < 0xFFFF; ++d) {
            if (type[d] == U_UNASSIGNED || !isSource(d))
                continue;
            cs.remove();
            cs.append(c).append(d);
            targ = cs;
            sourceToTarget->transliterate(targ);
            if (!isReceivingTarget(targ)) {
                logWrongScript("Source-Target", cs, targ);
                if (errorCount >= errorLimit)
                    return;
            }
        }
    }

    log->logln("Checking that target characters convert to source and back - Singles");

    for (c = 0; c < 0xFFFF; ++c) {
        if (type[c] == U_UNASSIGNED || !isTarget(c))
            continue;
        cs.remove();
        cs.append(c);
        targ = cs;
        targetToSource->transliterate(targ);
        reverse = targ;
        sourceToTarget->transliterate(reverse);
        if (!isReceivingSource(targ)) {
            logWrongScript("Target-Source", cs, targ);
            if (errorCount >= errorLimit)
                return;
        } else if (cs != reverse) {
            logRoundTripFailure(cs, targ, reverse);
            if (errorCount >= errorLimit)
                return;
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
        log->logln(TestUtility::hex(c));
        for (UChar d = 0; d < 0xFFFF; ++d) {
            if (type[d] == U_UNASSIGNED || !isTarget(d))
                continue;
            cs.setCharAt(1, d);
            targ = cs;
            targetToSource->transliterate(targ);
            reverse = targ;
            sourceToTarget->transliterate(reverse);
            if (!isReceivingSource(targ)) {
                logWrongScript("Target-Source", cs, targ);
                if (errorCount >= errorLimit)
                    return;
            } else if (cs != reverse) {
                logRoundTripFailure(cs, targ, reverse);
                if (errorCount >= errorLimit)
                    return;
            }
        }
    }
    log->logln("");
    delete []type;
}

