// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING && !UPRV_INCOMPLETE_CPP11_SUPPORT

#include "numbertest.h"
#include "numparse_impl.h"
#include "numparse_unisets.h"
#include "unicode/dcfmtsym.h"
#include "unicode/testlog.h"

#include <cmath>

using icu::numparse::impl::unisets::get;

void NumberParserTest::runIndexedTest(int32_t index, UBool exec, const char*& name, char*) {
    if (exec) {
        logln("TestSuite NumberParserTest: ");
    }
    TESTCASE_AUTO_BEGIN;
        TESTCASE_AUTO(testBasic);
    TESTCASE_AUTO_END;
}

void NumberParserTest::testBasic() {
    IcuTestErrorCode status(*this, "testBasic");

    static const struct TestCase {
        int32_t flags;
        const char16_t* inputString;
        const char16_t* patternString;
        int32_t expectedCharsConsumed;
        double expectedResultDouble;
    } cases[] = {{3, u"51423", u"0", 5, 51423.},
                 {3, u"51423x", u"0", 5, 51423.},
                 {3, u" 51423", u"0", 6, 51423.},
                 {3, u"51423 ", u"0", 5, 51423.},
                 {3, u"𝟱𝟭𝟰𝟮𝟯", u"0", 10, 51423.},
                 {3, u"𝟱𝟭𝟰𝟮𝟯x", u"0", 10, 51423.},
                 {3, u" 𝟱𝟭𝟰𝟮𝟯", u"0", 11, 51423.},
                 {3, u"𝟱𝟭𝟰𝟮𝟯 ", u"0", 10, 51423.},
                 {7, u"51,423", u"#,##,##0", 6, 51423.},
                 {7, u" 51,423", u"#,##,##0", 7, 51423.},
                 {7, u"51,423 ", u"#,##,##0", 6, 51423.},
                 {7, u"𝟱𝟭,𝟰𝟮𝟯", u"#,##,##0", 11, 51423.},
                 {7, u"𝟳,𝟴𝟵,𝟱𝟭,𝟰𝟮𝟯", u"#,##,##0", 19, 78951423.},
                 {7, u"𝟳𝟴,𝟵𝟱𝟭.𝟰𝟮𝟯", u"#,##,##0", 18, 78951.423},
                 {7, u"𝟳𝟴,𝟬𝟬𝟬", u"#,##,##0", 11, 78000.},
                 {7, u"𝟳𝟴,𝟬𝟬𝟬.𝟬𝟬𝟬", u"#,##,##0", 18, 78000.},
                 {7, u"𝟳𝟴,𝟬𝟬𝟬.𝟬𝟮𝟯", u"#,##,##0", 18, 78000.023},
                 {7, u"𝟳𝟴.𝟬𝟬𝟬.𝟬𝟮𝟯", u"#,##,##0", 11, 78.},
                 {3, u"-51423", u"0", 6, -51423.},
                 {3, u"51423-", u"0", 5, 51423.}, // plus and minus sign by default do NOT match after
                 {3, u"+51423", u"0", 6, 51423.},
                 {3, u"51423+", u"0", 5, 51423.}, // plus and minus sign by default do NOT match after
                 {3, u"%51423", u"0", 6, 514.23},
                 {3, u"51423%", u"0", 6, 514.23},
                 {3, u"51423%%", u"0", 6, 514.23},
                 {3, u"‰51423", u"0", 6, 51.423},
                 {3, u"51423‰", u"0", 6, 51.423},
                 {3, u"51423‰‰", u"0", 6, 51.423},
                 {3, u"∞", u"0", 1, INFINITY},
                 {3, u"-∞", u"0", 2, -INFINITY},
                 {3, u"@@@123  @@", u"0", 6, 123.}, // TODO: Should padding be strong instead of weak?
                 {3, u"@@@123@@  ", u"0", 6, 123.}, // TODO: Should padding be strong instead of weak?
//                 {3, u"a51423US dollars", u"a0¤¤¤", 16, 51423.},
//                 {3, u"a 51423 US dollars", u"a0¤¤¤", 18, 51423.},
                 {3, u"514.23 USD", u"¤0", 10, 514.23},
                 {3, u"514.23 GBP", u"¤0", 10, 514.23},
//                 {3, u"a 𝟱𝟭𝟰𝟮𝟯 b", u"a0b", 14, 51423.},
//                 {3, u"-a 𝟱𝟭𝟰𝟮𝟯 b", u"a0b", 15, -51423.},
//                 {3, u"a -𝟱𝟭𝟰𝟮𝟯 b", u"a0b", 15, -51423.},
//                 {3, u"𝟱𝟭𝟰𝟮𝟯", u"[0];(0)", 10, 51423.},
//                 {3, u"[𝟱𝟭𝟰𝟮𝟯", u"[0];(0)", 11, 51423.},
//                 {3, u"𝟱𝟭𝟰𝟮𝟯]", u"[0];(0)", 11, 51423.},
//                 {3, u"[𝟱𝟭𝟰𝟮𝟯]", u"[0];(0)", 12, 51423.},
//                 {3, u"(𝟱𝟭𝟰𝟮𝟯", u"[0];(0)", 11, -51423.},
//                 {3, u"𝟱𝟭𝟰𝟮𝟯)", u"[0];(0)", 11, -51423.},
//                 {3, u"(𝟱𝟭𝟰𝟮𝟯)", u"[0];(0)", 12, -51423.},
//                 {3, u"𝟱𝟭𝟰𝟮𝟯", u"{0};{0}", 10, 51423.},
//                 {3, u"{𝟱𝟭𝟰𝟮𝟯", u"{0};{0}", 11, 51423.},
//                 {3, u"𝟱𝟭𝟰𝟮𝟯}", u"{0};{0}", 11, 51423.},
//                 {3, u"{𝟱𝟭𝟰𝟮𝟯}", u"{0};{0}", 12, 51423.},
//                 {1, u"a40b", u"a0'0b'", 3, 40.}, // greedy code path thinks "40" is the number
//                 {2, u"a40b", u"a0'0b'", 4, 4.}, // slow code path finds the suffix "0b"
                 {3, u"𝟱.𝟭𝟰𝟮E𝟯", u"0", 12, 5142.},
                 {3, u"𝟱.𝟭𝟰𝟮E-𝟯", u"0", 13, 0.005142},
                 {3, u"𝟱.𝟭𝟰𝟮e-𝟯", u"0", 13, 0.005142},
                 {7, u"5,142.50 Canadian dollars", u"#,##,##0 ¤¤¤", 25, 5142.5},
//                 {3, u"a$ b5", u"a ¤ b0", 5, 5.0},
//                 {3, u"📺1.23", u"📺0;📻0", 6, 1.23},
//                 {3, u"📻1.23", u"📺0;📻0", 6, -1.23},
                 {3, u".00", u"0", 3, 0.0},
                 {3, u"                              1,234", u"a0", 35, 1234.}, // should not hang
                 {3, u"NaN", u"0", 3, NAN},
                 {3, u"NaN E5", u"0", 3, NAN},
                 {3, u"0", u"0", 1, 0.0}};

    parse_flags_t parseFlags = PARSE_FLAG_IGNORE_CASE | PARSE_FLAG_INCLUDE_UNPAIRED_AFFIXES;
    for (auto cas : cases) {
        UnicodeString inputString(cas.inputString);
        UnicodeString patternString(cas.patternString);
        LocalPointer<const NumberParserImpl> parser(
                NumberParserImpl::createSimpleParser(
                        Locale("en"), patternString, parseFlags, status));
        UnicodeString message =
                UnicodeString("Input <") + inputString + UnicodeString("> Parser ") + parser->toString();

        if (0 != (cas.flags & 0x01)) {
            // Test greedy code path
            ParsedNumber resultObject;
            parser->parse(inputString, true, resultObject, status);
            assertTrue("Greedy Parse failed: " + message, resultObject.success());
            assertEquals(
                    "Greedy Parse failed: " + message, cas.expectedCharsConsumed, resultObject.charEnd);
            assertEquals(
                    "Greedy Parse failed: " + message, cas.expectedResultDouble, resultObject.getDouble());
        }

        if (0 != (cas.flags & 0x02)) {
            // Test slow code path
            ParsedNumber resultObject;
            parser->parse(inputString, false, resultObject, status);
            assertTrue("Non-Greedy Parse failed: " + message, resultObject.success());
            assertEquals(
                    "Non-Greedy Parse failed: " + message,
                    cas.expectedCharsConsumed,
                    resultObject.charEnd);
            assertEquals(
                    "Non-Greedy Parse failed: " + message,
                    cas.expectedResultDouble,
                    resultObject.getDouble());
        }

        if (0 != (cas.flags & 0x04)) {
            // Test with strict separators
            parser.adoptInstead(
                    NumberParserImpl::createSimpleParser(
                            Locale("en"),
                            patternString,
                            parseFlags | PARSE_FLAG_STRICT_GROUPING_SIZE,
                            status));
            ParsedNumber resultObject;
            parser->parse(inputString, true, resultObject, status);
            assertTrue("Strict Parse failed: " + message, resultObject.success());
            assertEquals(
                    "Strict Parse failed: " + message, cas.expectedCharsConsumed, resultObject.charEnd);
            assertEquals(
                    "Strict Parse failed: " + message, cas.expectedResultDouble, resultObject.getDouble());
        }
    }
}


#endif
