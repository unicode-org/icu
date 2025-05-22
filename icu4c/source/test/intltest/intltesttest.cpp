// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

#include "intltest.h"

class IntlTestTest : public IntlTest {
  public:
    void runIndexedTest(int32_t index, UBool exec, const char*& name, char* /*par*/) override {
        if (exec) {
            logln("TestSuite IntlTestTest: ");
        }
        TESTCASE_AUTO_BEGIN;

        TESTCASE_AUTO(testAssertEquals);
        TESTCASE_AUTO(testAssertNotEquals);

        TESTCASE_AUTO_END;
    }

  protected:
    class TestableIntlTest : public IntlTest {
      public:
        TestableIntlTest(std::u16string* const lastLogLine) : lastLogLine(lastLogLine) {}

        size_t newErrors() {
            const size_t result = errors;
            errors = 0;
            return result;
        }

        virtual int32_t IncErrorCount() override {
          ++errors;
          return IntlTest::IncErrorCount();
        }

      protected:
        virtual void LL_message(std::u16string_view message, UBool newline) override {
            currentLogLine += message;
            if (newline) {
                *lastLogLine = currentLogLine;
                currentLogLine.clear();
            }
        }

      private:
        size_t errors = 0;
        std::u16string* lastLogLine;
        std::u16string currentLogLine;
    };

    void testAssertEquals() {
        std::u16string lastLogLine;
        TestableIntlTest metatest(&lastLogLine);
        // Booleans: Both UBool, mixed, both bool.
        metatest.assertEquals(WHERE "should fail", static_cast<UBool>(true), static_cast<UBool>(false));
        assertEquals(WHERE "should have failed", 1, metatest.newErrors());
        assertNotEquals("boolean in " + lastLogLine, std::u16string::npos, lastLogLine.find(u"true"));
        metatest.assertEquals(WHERE "should fail", static_cast<UBool>(true), false);
        assertEquals(WHERE "should have failed", 1, metatest.newErrors());
        assertNotEquals("boolean in " + lastLogLine, std::u16string::npos, lastLogLine.find(u"true"));
        metatest.assertEquals(WHERE "should fail", true, false);
        assertEquals(WHERE "should have failed", 1, metatest.newErrors());
        assertNotEquals("boolean in " + lastLogLine, std::u16string::npos, lastLogLine.find(u"true"));
        // The mysterious int32_t.
        metatest.assertEquals(WHERE "should fail", -1, 66);
        assertEquals(WHERE "should have failed", 1, metatest.newErrors());
        assertNotEquals("decimal int32", std::u16string::npos, lastLogLine.find(u"-1"));
        assertNotEquals("hex int32 in " + lastLogLine, std::u16string::npos,
                        lastLogLine.find(u"0xFFFFFFFF"));
        metatest.assertEquals(WHERE "should fail", 65, 66);
        assertEquals(WHERE "should have failed", 1, metatest.newErrors());
        assertNotEquals("decimal int32 in " + lastLogLine, std::u16string::npos,
                        lastLogLine.find(u"65"));
        assertNotEquals("hex int32 in " + lastLogLine, std::u16string::npos, lastLogLine.find(u"0x41"));
        metatest.assertEquals(WHERE "should fail", UnicodeString("").char32At(0),
                              UnicodeString("B").char32At(0));
        assertEquals(WHERE "should have failed", 1, metatest.newErrors());
        assertNotEquals("decimal int32 in " + lastLogLine, std::u16string::npos,
                        lastLogLine.find(u"65535"));
        assertNotEquals("hex int32 in " + lastLogLine, std::u16string::npos,
                        lastLogLine.find(u"0xFFFF"));
        metatest.assertEquals(WHERE "should fail", UnicodeString("AB").char32At(0),
                              UnicodeString("AB").char32At(1));
        assertEquals(WHERE "should have failed", 1, metatest.newErrors());
        assertNotEquals("decimal int32 in " + lastLogLine, std::u16string::npos,
                        lastLogLine.find(u"65"));
        assertNotEquals("hex int32 in " + lastLogLine, std::u16string::npos, lastLogLine.find(u"0x41"));
        // Definitely characters.
        // char32_t.
        metatest.assertEquals(WHERE "should fail", U'A', U'B');
        assertEquals(WHERE "should have failed", 1, metatest.newErrors());
        assertNotEquals("char32_t in " + lastLogLine, std::u16string::npos,
                        lastLogLine.find(u"U+0041 A"));
        metatest.assertEquals(WHERE "should fail", UnicodeString("A").char32At(0), U'B');
        assertEquals(WHERE "should have failed", 1, metatest.newErrors());
        assertNotEquals("char32_t in " + lastLogLine, std::u16string::npos,
                        lastLogLine.find(u"U+0041 A"));
        metatest.assertEquals(WHERE "should fail", U'A', 0x42);
        assertEquals(WHERE "should have failed", 1, metatest.newErrors());
        assertNotEquals("char32_t in " + lastLogLine, std::u16string::npos,
                        lastLogLine.find(u"U+0041 A"));
        // char16_t.
        metatest.assertEquals(WHERE "should fail", u'A', u'B');
        assertEquals(WHERE "should have failed", 1, metatest.newErrors());
        assertNotEquals("char16_t in " + lastLogLine, std::u16string::npos,
                        lastLogLine.find(u"U+0041 A"));
        metatest.assertEquals(WHERE "should fail", 0x41, u'B');
        assertEquals(WHERE "should have failed", 1, metatest.newErrors());
        assertNotEquals("char16_t in " + lastLogLine, std::u16string::npos,
                        lastLogLine.find(u"U+0041 A"));
        metatest.assertEquals(WHERE "should fail", UnicodeString("A").charAt(0), 0x42);
        assertEquals(WHERE "should have failed", 1, metatest.newErrors());
        assertNotEquals("char16_t in " + lastLogLine, std::u16string::npos,
                        lastLogLine.find(u"U+0041 A"));
        // Actual numeric comparisons.
        metatest.assertEquals(WHERE "should fail", static_cast<uint32_t>(65), 66);
        assertEquals(WHERE "should have failed", 1, metatest.newErrors());
        assertNotEquals("decimal uint32 in " + lastLogLine, std::u16string::npos,
                        lastLogLine.find(u"65"));
        assertEquals("NO hex uint32 in " + lastLogLine, std::u16string::npos, lastLogLine.find(u"41"));
        constexpr std::u16string_view s = u"𒌉 𒂍𒁾𒁀𒀀 𒌓 𒌌𒆷𒀀𒀭 𒈨𒂠 𒉌𒁺𒉈𒂗";
        metatest.assertEquals(WHERE "should fail", s.size(), 38);
        assertEquals(WHERE "should have failed", 1, metatest.newErrors());
        assertNotEquals("decimal size_t in " + lastLogLine, std::u16string::npos,
                        lastLogLine.find(u"37"));
        assertEquals("NO hex size_t in " + lastLogLine, std::u16string::npos, lastLogLine.find(u"25"));
        // This one is int32_t on 32-bit platforms.
        metatest.assertEquals(WHERE "should fail", s.end() - s.begin(), 38);
        assertEquals(WHERE "should have failed", 1, metatest.newErrors());
        assertNotEquals("decimal ptrdiff_t in " + lastLogLine, std::u16string::npos,
                        lastLogLine.find(u"37"));
        if (sizeof(std::ptrdiff_t) == 4) {
            assertNotEquals("hex 32-bit ptrdiff_t in " + lastLogLine, std::u16string::npos,
                            lastLogLine.find(u"25"));
        } else {
            assertEquals("NO hex ptrdiff_t in " + lastLogLine, std::u16string::npos,
                         lastLogLine.find(u"25"));
        }
        // Making the enum inherit from int8_t, which is UBool, also allows us to check that we don’t
        // fall into the UBool overload (this would have been the case before this got templatized).
        enum Button : int8_t {
            Abort = 65,
            Retry = 66,
            Ignore = 67,
        };
        metatest.assertEquals(WHERE "should fail", Abort, Retry);
        assertEquals(WHERE "should have failed", 1, metatest.newErrors());
        assertNotEquals("decimal enum in " + lastLogLine, std::u16string::npos, lastLogLine.find(u"65"));
        assertEquals("NO hex enum in " + lastLogLine, std::u16string::npos, lastLogLine.find(u"41"));
        assertEquals("NO UBool in " + lastLogLine, std::u16string::npos, lastLogLine.find(u"UBool"));
    }

    void testAssertNotEquals() {
        std::u16string lastLogLine;
        TestableIntlTest metatest(&lastLogLine);
        // Booleans: Both UBool, mixed, both bool.
        metatest.assertNotEquals(WHERE "should fail", static_cast<UBool>(true), static_cast<UBool>(true));
        assertEquals(WHERE "should have failed", 1, metatest.newErrors());
        assertNotEquals("boolean in " + lastLogLine, std::u16string::npos, lastLogLine.find(u"true"));
        metatest.assertNotEquals(WHERE "should fail", static_cast<UBool>(true), true);
        assertEquals(WHERE "should have failed", 1, metatest.newErrors());
        assertNotEquals("boolean in " + lastLogLine, std::u16string::npos, lastLogLine.find(u"true"));
        metatest.assertNotEquals(WHERE "should fail", true, true);
        assertEquals(WHERE "should have failed", 1, metatest.newErrors());
        assertNotEquals("boolean in " + lastLogLine, std::u16string::npos, lastLogLine.find(u"true"));
        // The mysterious int32_t.
        metatest.assertNotEquals(WHERE "should fail", -1, -1);
        assertEquals(WHERE "should have failed", 1, metatest.newErrors());
        assertNotEquals("decimal int32", std::u16string::npos, lastLogLine.find(u"-1"));
        assertNotEquals("hex int32 in " + lastLogLine, std::u16string::npos,
                        lastLogLine.find(u"0xFFFFFFFF"));
        metatest.assertNotEquals(WHERE "should fail", 65, 65);
        assertEquals(WHERE "should have failed", 1, metatest.newErrors());
        assertNotEquals("decimal int32 in " + lastLogLine, std::u16string::npos,
                        lastLogLine.find(u"65"));
        assertNotEquals("hex int32 in " + lastLogLine, std::u16string::npos, lastLogLine.find(u"0x41"));
        metatest.assertNotEquals(WHERE "should fail", UnicodeString("").char32At(0),
                              UnicodeString("B").char32At(1));
        assertEquals(WHERE "should have failed", 1, metatest.newErrors());
        assertNotEquals("decimal int32 in " + lastLogLine, std::u16string::npos,
                        lastLogLine.find(u"65535"));
        assertNotEquals("hex int32 in " + lastLogLine, std::u16string::npos,
                        lastLogLine.find(u"0xFFFF"));
        metatest.assertNotEquals(WHERE "should fail", UnicodeString("AB").char32At(0),
                              UnicodeString("BA").char32At(1));
        assertEquals(WHERE "should have failed", 1, metatest.newErrors());
        assertNotEquals("decimal int32 in " + lastLogLine, std::u16string::npos,
                        lastLogLine.find(u"65"));
        assertNotEquals("hex int32 in " + lastLogLine, std::u16string::npos, lastLogLine.find(u"0x41"));
        // Definitely characters.
        // char32_t.
        metatest.assertNotEquals(WHERE "should fail", U'A', U'A');
        assertEquals(WHERE "should have failed", 1, metatest.newErrors());
        assertNotEquals("char32_t in " + lastLogLine, std::u16string::npos,
                        lastLogLine.find(u"U+0041 A"));
        metatest.assertNotEquals(WHERE "should fail", UnicodeString("A").char32At(0), U'A');
        assertEquals(WHERE "should have failed", 1, metatest.newErrors());
        assertNotEquals("char32_t in " + lastLogLine, std::u16string::npos,
                        lastLogLine.find(u"U+0041 A"));
        metatest.assertNotEquals(WHERE "should fail", U'A', 0x41);
        assertEquals(WHERE "should have failed", 1, metatest.newErrors());
        assertNotEquals("char32_t in " + lastLogLine, std::u16string::npos,
                        lastLogLine.find(u"U+0041 A"));
        // char16_t.
        metatest.assertNotEquals(WHERE "should fail", u'A', u'A');
        assertEquals(WHERE "should have failed", 1, metatest.newErrors());
        assertNotEquals("char16_t in " + lastLogLine, std::u16string::npos,
                        lastLogLine.find(u"U+0041 A"));
        metatest.assertNotEquals(WHERE "should fail", 0x41, u'A');
        assertEquals(WHERE "should have failed", 1, metatest.newErrors());
        assertNotEquals("char16_t in " + lastLogLine, std::u16string::npos,
                        lastLogLine.find(u"U+0041 A"));
        metatest.assertNotEquals(WHERE "should fail", UnicodeString("A").charAt(0), 0x41);
        assertEquals(WHERE "should have failed", 1, metatest.newErrors());
        assertNotEquals("char16_t in " + lastLogLine, std::u16string::npos,
                        lastLogLine.find(u"U+0041 A"));
        // Actual numeric comparisons.
        metatest.assertNotEquals(WHERE "should fail", static_cast<uint32_t>(65), 65);
        assertEquals(WHERE "should have failed", 1, metatest.newErrors());
        assertNotEquals("decimal uint32 in " + lastLogLine, std::u16string::npos,
                        lastLogLine.find(u"65"));
        assertEquals("NO hex uint32 in " + lastLogLine, std::u16string::npos, lastLogLine.find(u"41"));
        constexpr std::u16string_view s = u"𒌉 𒂍𒁾𒁀𒀀 𒌓 𒌌𒆷𒀀𒀭 𒈨𒂠 𒉌𒁺𒉈𒂗";
        metatest.assertNotEquals(WHERE "should fail", s.size(), 37);
        assertEquals(WHERE "should have failed", 1, metatest.newErrors());
        assertNotEquals("decimal size_t in " + lastLogLine, std::u16string::npos,
                        lastLogLine.find(u"37"));
        assertEquals("NO hex size_t in " + lastLogLine, std::u16string::npos, lastLogLine.find(u"25"));
        // This one is int32_t on 32-bit platforms.
        metatest.assertNotEquals(WHERE "should fail", s.end() - s.begin(), 37);
        assertEquals(WHERE "should have failed", 1, metatest.newErrors());
        assertNotEquals("decimal ptrdiff_t in " + lastLogLine, std::u16string::npos,
                        lastLogLine.find(u"37"));
        if (sizeof(std::ptrdiff_t) == 4) {
            assertNotEquals("hex 32-bit ptrdiff_t in " + lastLogLine, std::u16string::npos,
                            lastLogLine.find(u"25"));
        } else {
            assertEquals("NO hex ptrdiff_t in " + lastLogLine, std::u16string::npos,
                         lastLogLine.find(u"25"));
        }
        // Making the enum inherit from int8_t, which is UBool, also allows us to check that we don’t
        // fall into the UBool overload (this would have been the case before this got templatized).
        enum Button : int8_t {
            Abort = 65,
            Retry = 66,
            Ignore = 67,
        };
        metatest.assertNotEquals(WHERE "should fail", Abort, Abort);
        assertEquals(WHERE "should have failed", 1, metatest.newErrors());
        assertNotEquals("decimal enum in " + lastLogLine, std::u16string::npos, lastLogLine.find(u"65"));
        assertEquals("NO hex enum in " + lastLogLine, std::u16string::npos, lastLogLine.find(u"41"));
        assertEquals("NO UBool in " + lastLogLine, std::u16string::npos, lastLogLine.find(u"UBool"));
    }
};

extern IntlTest *createIntlTestTest() {
    return new IntlTestTest();
}
