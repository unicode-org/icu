/*
**********************************************************************
*   Copyright (C) 2000, International Business Machines
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
                                   const char* &name, char* par) {
    switch (index) {
        CASE(0,TestHiragana);
        CASE(1,TestKatakana);
        CASE(2,TestArabic);
        CASE(3,TestHebrew);
        CASE(4,TestHangul);
        CASE(5,TestGreek);
        CASE(6,TestCyrillic);
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
    RTTest(const UnicodeString& transliteratorID, 
           int8_t sourceScript, int8_t targetScript);

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
    UBool isReceivingSource(UChar c);

    /*
     * Characters to filter for target-source mapping
     * Typically is base alphabet, minus extended characters
     */
    UBool isTarget(UChar c);

    /*
     * Characters to check for target-source mapping
     * Typically the same as the source script, plus punctuation
     */
    UBool isReceivingTarget(UChar c);

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
RTTest::RTTest(const UnicodeString& transliteratorID, 
               int8_t sourceScript, int8_t targetScript) {
    this->transliteratorID = transliteratorID;
    this->sourceScript = sourceScript;
    this->targetScript = targetScript;
    errorLimit = 0x7FFFFFFFL;
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

void RTTest::test(const UnicodeString& sourceRange,
                  const UnicodeString& targetRange, IntlTest* log) {

    UErrorCode status = U_ZERO_ERROR;
    if (sourceRange.length() > 0) {
        this->sourceRange.applyPattern(sourceRange, status);
        if (U_FAILURE(status)) {
            log->errln("FAIL: UnicodeSet::applyPattern(" +
                       sourceRange + ")");
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
    if (targetRange.length() > 0) {
        this->targetRange.applyPattern(targetRange, status);
        if (U_FAILURE(status)) {
            log->errln("FAIL: UnicodeSet::applyPattern(" +
                       targetRange + ")");
            return;
        }
    }

    this->log = log;

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

void RTTest::test2() {

    UChar c;
    UnicodeString cs, targ, reverse;

    Transliterator* sourceToTarget = Transliterator::createInstance(transliteratorID);
    if (sourceToTarget == NULL) {
        log->errln("Fail: createInstance(" + transliteratorID +
                   ") returned NULL");
        return;
    }
    Transliterator* targetToSource = sourceToTarget->createInverse();
    if (targetToSource == NULL) {
        log->errln("Fail: " + transliteratorID +
                   ".createInverse() returned NULL");
        delete sourceToTarget;
        return;
    }

    log->logln("Checking that all source characters convert to target - Singles");

    for (c = 0; c < 0xFFFF; ++c) {
        if (Unicode::getType(c) == Unicode::UNASSIGNED ||
            !isSource(c)) continue;
        cs.remove(); cs.append(c);
        targ = cs;
        sourceToTarget->transliterate(targ);
        if (!isReceivingTarget(targ)) {
            logWrongScript("Source-Target", cs, targ);
            if (errorCount >= errorLimit) return;
        }
    }

    log->logln("Checking that all source characters convert to target - Doubles");

    for (c = 0; c < 0xFFFF; ++c) {
        if (Unicode::getType(c) == Unicode::UNASSIGNED ||
            !isSource(c)) continue;
        for (UChar d = 0; d < 0xFFFF; ++d) {
            if (Unicode::getType(d) == Unicode::UNASSIGNED ||
                !isSource(d)) continue;
            cs.remove(); cs.append(c).append(d);
            targ = cs;
            sourceToTarget->transliterate(targ);
            if (!isReceivingTarget(targ)) {
                logWrongScript("Source-Target", cs, targ);
                if (errorCount >= errorLimit) return;
            }
        }
    }

    log->logln("Checking that target characters convert to source and back - Singles");

    for (c = 0; c < 0xFFFF; ++c) {
        if (Unicode::getType(c) == Unicode::UNASSIGNED ||
            !isTarget(c)) continue;
        cs.remove(); cs.append(c);
        targ = cs;
        targetToSource->transliterate(targ);
        reverse = targ;
        sourceToTarget->transliterate(reverse);
        if (!isReceivingSource(targ)) {
            logWrongScript("Target-Source", cs, targ);
            if (errorCount >= errorLimit) return;
        } else if (cs != reverse) {
            logRoundTripFailure(cs, targ, reverse);
            if (errorCount >= errorLimit) return;
        }
    }

    log->logln("Checking that target characters convert to source and back - Doubles");
    int32_t count = 0;
    cs = UNICODE_STRING("aa", 2);
    for (c = 0; c < 0xFFFF; ++c) {
        if (Unicode::getType(c) == Unicode::UNASSIGNED ||
            !isTarget(c)) continue;
        if (++count > pairLimit) {
            //throw new TestTruncated("Test truncated at " + pairLimit + " x 64k pairs");
            log->logln("");
            log->logln((UnicodeString)"Test truncated at " + pairLimit + " x 64k pairs");
            return;
        }
        cs.setCharAt(0, c);
        log->log(TestUtility::hex(c));
        log->log(" ");
        for (UChar d = 0; d < 0xFFFF; ++d) {
            if (Unicode::getType(d) == Unicode::UNASSIGNED ||
                !isTarget(d)) continue;
            cs.setCharAt(1, d);
            targ = cs;
            targetToSource->transliterate(targ);
            reverse = targ;
            sourceToTarget->transliterate(reverse);
            if (!isReceivingSource(targ)) {
                logWrongScript("Target-Source", cs, targ);
                if (errorCount >= errorLimit) return;
            } else if (cs != reverse) {
                logRoundTripFailure(cs, targ, reverse);
                if (errorCount >= errorLimit) return;
            }
        }
    }
    log->logln("");
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
    int8_t script = TestUtility::getScript(c);
    if (script != sourceScript) return FALSE;
    if (!Unicode::isLetter(c)) return FALSE;
    if (!sourceRange.contains(c)) return FALSE;
    return TRUE;
}

/*
 * Characters to check for target back to source mapping.
 * Typically the same as the target script, plus punctuation
 */
UBool RTTest::isReceivingSource(UChar c) {
    int8_t script = TestUtility::getScript(c);
    return (script == sourceScript || script == TestUtility::COMMON_SCRIPT);
}

/*
 * Characters to filter for target-source mapping
 * Typically is base alphabet, minus extended characters
 */
UBool RTTest::isTarget(UChar c) {
    int8_t script = TestUtility::getScript(c);
    if (script != targetScript) return FALSE;
    if (!Unicode::isLetter(c)) return FALSE;
    if (!targetRange.isEmpty() && !targetRange.contains(c)) return FALSE;
    return TRUE;
}

/*
 * Characters to check for target-source mapping
 * Typically the same as the source script, plus punctuation
 */
UBool RTTest::isReceivingTarget(UChar c) {
    int8_t script = TestUtility::getScript(c);
    return (script == targetScript || script == TestUtility::COMMON_SCRIPT);
}

UBool RTTest::isSource(const UnicodeString& s) {
    for (int32_t i = 0; i < s.length(); ++i) {
        if (!isSource(s.charAt(i))) return FALSE;
    }
    return TRUE;
}

UBool RTTest::isTarget(const UnicodeString& s) {
    for (int32_t i = 0; i < s.length(); ++i) {
        if (!isTarget(s.charAt(i))) return FALSE;
    }
    return TRUE;
}

UBool RTTest::isReceivingSource(const UnicodeString& s) {
    for (int32_t i = 0; i < s.length(); ++i) {
        if (!isReceivingSource(s.charAt(i))) return FALSE;
    }
    return TRUE;
}

UBool RTTest::isReceivingTarget(const UnicodeString& s) {
    for (int32_t i = 0; i < s.length(); ++i) {
        if (!isReceivingTarget(s.charAt(i))) return FALSE;
    }
    return TRUE;
}

//--------------------------------------------------------------------
// RTHangulTest
//--------------------------------------------------------------------

class RTHangulTest : public RTTest {
public:
    RTHangulTest();
protected:
    virtual UBool isSource(UChar c);
};

RTHangulTest::RTHangulTest() : RTTest("Jamo-Hangul",
                                  TestUtility::JAMO_SCRIPT,
                                  TestUtility::HANGUL_SCRIPT) {}

UBool RTHangulTest::isSource(UChar c) {
    if (0x1113 <= c && c <= 0x1160) return FALSE;
    if (0x1176 <= c && c <= 0x11F9) return FALSE;
    if (0x3131 <= c && c <= 0x318E) return FALSE;
    return RTTest::isSource(c);
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

void TransliteratorRoundTripTest::TestHangul() {
    RTHangulTest t;
    t.setPairLimit(30); // Don't run full test -- too long
    t.test("", "", this);
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
