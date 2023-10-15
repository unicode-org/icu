// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.lang;

import com.ibm.icu.dev.test.CoreTestFmwk;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.util.ULocale;

@RunWith(Enclosed.class)
public class DataDrivenUScriptTest extends CoreTestFmwk {

    private static String scriptsToString(int[] scripts) {
        if (scripts == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        for (int script : scripts) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(UScript.getShortName(script));
        }
        return sb.toString();
    }

    private static void assertEqualScripts(String msg, int[] expectedScripts, int[] actualScripts) {
        assertEquals(msg, scriptsToString(expectedScripts), scriptsToString(actualScripts));
    }

    @RunWith(Parameterized.class)
    public static class LocaleGetCodeTest {
        private ULocale testLocaleName;
        private int expected;

        public LocaleGetCodeTest(ULocale testLocaleName, int expected) {
            this.testLocaleName = testLocaleName;
            this.expected = expected;
        }

        @Parameterized.Parameters
        public static Collection testData() {
            return Arrays.asList(new Object[][] { { new ULocale("en"), UScript.LATIN },
                    { new ULocale("en_US"), UScript.LATIN },
                    { new ULocale("sr"), UScript.CYRILLIC },
                    { new ULocale("ta"), UScript.TAMIL },
                    { new ULocale("te_IN"), UScript.TELUGU },
                    { new ULocale("hi"), UScript.DEVANAGARI },
                    { new ULocale("he"), UScript.HEBREW },
                    { new ULocale("ar"), UScript.ARABIC },
                    { new ULocale("abcde"), UScript.INVALID_CODE },
                    { new ULocale("abcde_cdef"), UScript.INVALID_CODE },
                    { new ULocale("iw"), UScript.HEBREW }
                });
        }

        @Test
        public void TestLocaleGetCode() {
            int[] code = UScript.getCode(testLocaleName);
            if (code == null) {
                if (expected != UScript.INVALID_CODE) {
                    errln("Error testing UScript.getCode(). Got: null" + " Expected: " + expected + " for locale "
                            + testLocaleName);
                }
            } else if ((code[0] != expected)) {
                errln("Error testing UScript.getCode(). Got: " + code[0] + " Expected: " + expected + " for locale "
                        + testLocaleName);
            }

            ULocale defaultLoc = ULocale.getDefault();
            ULocale esperanto = new ULocale("eo_DE");
            ULocale.setDefault(esperanto);
            code = UScript.getCode(esperanto);
            if (code != null) {
                if (code[0] != UScript.LATIN) {
                    errln("Did not get the expected script code for Esperanto");
                }
            } else {
                warnln("Could not load the locale data.");
            }
            ULocale.setDefault(defaultLoc);

            // Should work regardless of whether we have locale data for the language.
            assertEqualScripts("tg script: Cyrl", // Tajik
                    new int[] { UScript.CYRILLIC }, UScript.getCode(new ULocale("tg")));
            assertEqualScripts("xsr script: Deva", // Sherpa
                    new int[] { UScript.DEVANAGARI }, UScript.getCode(new ULocale("xsr")));

            // Multi-script languages.
            assertEqualScripts("ja scripts: Kana Hira Hani",
                    new int[] { UScript.KATAKANA, UScript.HIRAGANA, UScript.HAN }, UScript.getCode(ULocale.JAPANESE));
            assertEqualScripts("ko scripts: Hang Hani", new int[] { UScript.HANGUL, UScript.HAN },
                    UScript.getCode(ULocale.KOREAN));
            assertEqualScripts("zh script: Hani", new int[] { UScript.HAN }, UScript.getCode(ULocale.CHINESE));
            assertEqualScripts("zh-Hant scripts: Hani Bopo", new int[] { UScript.HAN, UScript.BOPOMOFO },
                    UScript.getCode(ULocale.TRADITIONAL_CHINESE));
            assertEqualScripts("zh-TW scripts: Hani Bopo", new int[] { UScript.HAN, UScript.BOPOMOFO },
                    UScript.getCode(ULocale.TAIWAN));

            // Ambiguous API, but this probably wants to return Latin rather than Rongorongo (Roro).
            assertEqualScripts("ro-RO script: Latn", new int[] { UScript.LATIN }, UScript.getCode("ro-RO")); // String
                                                                                                             // not
                                                                                                             // ULocale
        }
    }

    @RunWith(Parameterized.class)
    public static class TestMultipleUScript extends CoreTestFmwk {
        private String testLocaleName;
        private Locale testLocale;
        private int[] expected;

        public TestMultipleUScript(String testLocaleName, int[] expected, Locale testLocale) {
            this.testLocaleName = testLocaleName;
            this.testLocale = testLocale;
            this.expected = expected;
        }

        @Parameterized.Parameters
        public static Collection testData() {
            return Arrays.asList(new Object[][] {
                    { "ja", new int[] { UScript.KATAKANA, UScript.HIRAGANA, UScript.HAN }, Locale.JAPANESE },
                    { "ko_KR", new int[] { UScript.HANGUL, UScript.HAN }, Locale.KOREA },
                    { "zh", new int[] { UScript.HAN }, Locale.CHINESE },
                    { "zh_TW", new int[] { UScript.HAN, UScript.BOPOMOFO }, Locale.TAIWAN }
                });
        }

        @Test
        public void TestMultipleCodes() {
            int[] code = UScript.getCode(testLocaleName);
            if (code != null) {
                for (int j = 0; j < code.length; j++) {
                    if (code[j] != expected[j]) {
                        errln("Error testing UScript.getCode(). Got: " + code[j] + " Expected: " + expected[j]
                                + " for locale " + testLocaleName);
                    }
                }
            } else {
                errln("Error testing UScript.getCode() for locale " + testLocaleName);
            }

            logln("  Testing UScript.getCode(Locale) with locale: " + testLocale.getDisplayName());
            code = UScript.getCode(testLocale);
            if (code != null) {
                for (int j = 0; j < code.length; j++) {
                    if (code[j] != expected[j]) {
                        errln("Error testing UScript.getCode(). Got: " + code[j] + " Expected: " + expected[j]
                                + " for locale " + testLocaleName);
                    }
                }
            } else {
                errln("Error testing UScript.getCode() for locale " + testLocaleName);
            }
        }
    }

    @RunWith(Parameterized.class)
    public static class GetCodeTest extends CoreTestFmwk {
        private String testName;
        private int expected;

        public GetCodeTest(String testName, int expected) {
            this.testName = testName;
            this.expected = expected;
        }

        @Parameterized.Parameters
        public static Collection testData() {
            return Arrays.asList(new Object[][] {
                    /* test locale */
                    { "en", UScript.LATIN },
                    { "en_US", UScript.LATIN },
                    { "sr", UScript.CYRILLIC },
                    { "ta", UScript.TAMIL },
                    { "gu", UScript.GUJARATI },
                    { "te_IN", UScript.TELUGU },
                    { "hi", UScript.DEVANAGARI },
                    { "he", UScript.HEBREW },
                    { "ar", UScript.ARABIC },
                    { "abcde", UScript.INVALID_CODE },
                    { "abscde_cdef", UScript.INVALID_CODE },
                    { "iw", UScript.HEBREW },
                    /* test abbr */
                    { "Hani", UScript.HAN },
                    { "Hang", UScript.HANGUL },
                    { "Hebr", UScript.HEBREW },
                    { "Hira", UScript.HIRAGANA },
                    { "Knda", UScript.KANNADA },
                    { "Kana", UScript.KATAKANA },
                    { "Khmr", UScript.KHMER },
                    { "Lao", UScript.LAO },
                    { "Latn", UScript.LATIN }, /* "Latf","Latg", */
                    { "Mlym", UScript.MALAYALAM },
                    { "Mong", UScript.MONGOLIAN },
                    /* test names */
                    { "CYRILLIC", UScript.CYRILLIC },
                    { "DESERET", UScript.DESERET },
                    { "DEVANAGARI", UScript.DEVANAGARI },
                    { "ETHIOPIC", UScript.ETHIOPIC },
                    { "GEORGIAN", UScript.GEORGIAN },
                    { "GOTHIC", UScript.GOTHIC },
                    { "GREEK", UScript.GREEK },
                    { "GUJARATI", UScript.GUJARATI },
                    { "COMMON", UScript.COMMON },
                    { "INHERITED", UScript.INHERITED },
                    /* test lower case names */
                    { "malayalam", UScript.MALAYALAM },
                    { "mongolian", UScript.MONGOLIAN },
                    { "myanmar", UScript.MYANMAR },
                    { "ogham", UScript.OGHAM },
                    { "old-italic", UScript.OLD_ITALIC },
                    { "oriya", UScript.ORIYA },
                    { "runic", UScript.RUNIC },
                    { "sinhala", UScript.SINHALA },
                    { "syriac", UScript.SYRIAC },
                    { "tamil", UScript.TAMIL },
                    { "telugu", UScript.TELUGU },
                    { "thaana", UScript.THAANA },
                    { "thai", UScript.THAI },
                    { "tibetan", UScript.TIBETAN },
                    /* test the bounds */
                    { "Cans", UScript.CANADIAN_ABORIGINAL },
                    { "arabic", UScript.ARABIC },
                    { "Yi", UScript.YI },
                    { "Zyyy", UScript.COMMON },
                    /* test other cases that are ambiguous (script alias vs language tag) */
                    { "han", UScript.HAN },
                    { "mro", UScript.MRO },
                    { "nko", UScript.NKO },
                    { "old-hungarian", UScript.OLD_HUNGARIAN },
                    { "new-tai-lue", UScript.NEW_TAI_LUE },
                });
        }

        @Test
        public void TestGetCode() {
            int[] code = UScript.getCode(testName);
            if (code == null) {
                if (expected != UScript.INVALID_CODE) {
                    // getCode returns null if the code could not be found
                    errln("Error testing UScript.getCode(). Got: null" + " Expected: " + expected + " for locale "
                            + testName);
                }
            } else if ((code[0] != expected)) {
                errln("Error testing UScript.getCode(). Got: " + code + " Expected: " + expected + " for locale "
                        + testName);
            }
        }
    }

    @RunWith(Parameterized.class)
    public static class GetNameTest {
        private int testCode;
        private String expected;

        public GetNameTest(int testCode, String expected) {
            this.testCode = testCode;
            this.expected = expected;
        }

        @Parameterized.Parameters
        public static Collection testData() {
            return Arrays.asList(new Object[][] {
                    { UScript.CYRILLIC, "Cyrillic" },
                    { UScript.DESERET, "Deseret" },
                    { UScript.DEVANAGARI, "Devanagari" },
                    { UScript.ETHIOPIC, "Ethiopic" },
                    { UScript.GEORGIAN, "Georgian" },
                    { UScript.GOTHIC, "Gothic" },
                    { UScript.GREEK, "Greek" },
                    { UScript.GUJARATI, "Gujarati" }
                });
        }

        @Test
        public void TestGetName() {
            String scriptName = UScript.getName(testCode);
            if (!expected.equals(scriptName)) {
                errln("Error testing UScript.getName(). Got: " + scriptName + " Expected: " + expected);
            }
        }
    }

    @RunWith(Parameterized.class)
    public static class GetShortNameTest {
        private int testCode;
        private String expected;

        public GetShortNameTest(int testCode, String expected) {
            this.testCode = testCode;
            this.expected = expected;
        }

        @Parameterized.Parameters
        public static Collection testData() {
            return Arrays.asList(new Object[][] {
                    { UScript.HAN, "Hani" },
                    { UScript.HANGUL, "Hang" },
                    { UScript.HEBREW, "Hebr" },
                    { UScript.HIRAGANA, "Hira" },
                    { UScript.KANNADA, "Knda" },
                    { UScript.KATAKANA, "Kana" },
                    { UScript.KHMER, "Khmr" },
                    { UScript.LAO, "Laoo" },
                    { UScript.LATIN, "Latn" },
                    { UScript.MALAYALAM, "Mlym" },
                    { UScript.MONGOLIAN, "Mong" },
                });
        }

        @Test
        public void TestGetShortName() {
            String shortName = UScript.getShortName(testCode);
            if (!expected.equals(shortName)) {
                errln("Error testing UScript.getShortName(). Got: " + shortName + " Expected: " + expected);
            }
        }
    }

    @RunWith(Parameterized.class)
    public static class GetScriptTest {
        private int codepoint;
        private int expected;

        public GetScriptTest(int[] codepoint) {
            this.codepoint = codepoint[0];
            this.expected = codepoint[1];
        }

        @Parameterized.Parameters
        public static Collection testData() {
            return Arrays.asList(new int[][] {
                    { 0x0000FF9D, UScript.KATAKANA },
                    { 0x0000FFBE, UScript.HANGUL },
                    { 0x0000FFC7, UScript.HANGUL },
                    { 0x0000FFCF, UScript.HANGUL },
                    { 0x0000FFD7, UScript.HANGUL },
                    { 0x0000FFDC, UScript.HANGUL },
                    { 0x00010300, UScript.OLD_ITALIC },
                    { 0x00010330, UScript.GOTHIC },
                    { 0x0001034A, UScript.GOTHIC },
                    { 0x00010400, UScript.DESERET },
                    { 0x00010428, UScript.DESERET },
                    { 0x0001D167, UScript.INHERITED },
                    { 0x0001D17B, UScript.INHERITED },
                    { 0x0001D185, UScript.INHERITED },
                    { 0x0001D1AA, UScript.INHERITED },
                    { 0x00020000, UScript.HAN },
                    { 0x00000D02, UScript.MALAYALAM },
                    { 0x00050005, UScript.UNKNOWN }, // new Zzzz value in Unicode 5.0
                    { 0x00000000, UScript.COMMON },
                    { 0x0001D169, UScript.INHERITED },
                    { 0x0001D182, UScript.INHERITED },
                    { 0x0001D18B, UScript.INHERITED },
                    { 0x0001D1AD, UScript.INHERITED },
                });
        }

        @Test
        public void TestGetScript() {

            int code = UScript.INVALID_CODE;

            code = UScript.getScript(codepoint);

            if (code != expected) {
                errln("Error testing UScript.getScript(). Got: " + code + " Expected: " + expected
                        + " for codepoint 0x + hex(codepoint).");
            }
        }
    }
}
