/*
**********************************************************************
*   Copyright (C) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/10/99    aliu        Creation.
**********************************************************************
*/
#include "transtst.h"
#include "unicode/cpdtrans.h"
#include "unicode/dtfmtsym.h"
#include "unicode/hextouni.h"
#include "unicode/nultrans.h"
#include "unicode/rbt.h"
#include "unicode/translit.h"
#include "unicode/ucnv.h"
#include "unicode/ucnv_err.h"
#include "unicode/unicode.h"
#include "unicode/unifilt.h"
#include "unicode/uniset.h"
#include "unicode/unitohex.h"
#include "unicode/utypes.h"

/***********************************************************************

                     HOW TO USE THIS TEST FILE
                               -or-
                  How I developed on two platforms
                without losing (too much of) my mind


1. Add new tests by copying/pasting/changing existing tests.  On Java,
   any public void method named Test...() taking no parameters becomes
   a test.  On C++, you need to modify the header and add a line to
   the runIndexedTest() dispatch method.

2. Make liberal use of the expect() method; it is your friend.

3. The tests in this file exactly match those in a sister file on the
   other side.  The two files are:

   icu4j:  src/com/ibm/test/translit/TransliteratorTest.java
   icu4c:  source/test/intltest/transtst.cpp

                  ==> THIS IS THE IMPORTANT PART <==

   When you add a test in this file, add it in TransliteratorTest.java
   too.  Give it the same name and put it in the same relative place.
   This makes maintenance a lot simpler for any poor soul who ends up
   trying to synchronize the tests between icu4j and icu4c.

4. If you MUST enter a test that is NOT paralleled in the sister file,
   then add it in the special non-mirrored section.  These are
   labeled

     "icu4j ONLY"

   or

     "icu4c ONLY"

   Make sure you document the reason the test is here and not there.


Thank you.
The Management
***********************************************************************/

// Define character constants thusly to be EBCDIC-friendly
enum {
    LEFT_BRACE=((UChar)0x007B), /*{*/
    PIPE      =((UChar)0x007C), /*|*/
    ZERO      =((UChar)0x0030), /*0*/
    UPPER_A   =((UChar)0x0041)  /*A*/
};

void
TransliteratorTest::runIndexedTest(int32_t index, UBool exec,
                                   const char* &name, char* /*par*/) {
    switch (index) {
        TESTCASE(0,TestInstantiation);
        TESTCASE(1,TestSimpleRules);
        TESTCASE(2,TestRuleBasedInverse);
        TESTCASE(3,TestKeyboard);
        TESTCASE(4,TestKeyboard2);
        TESTCASE(5,TestKeyboard3);
        TESTCASE(6,TestArabic);
        TESTCASE(7,TestCompoundKana);
        TESTCASE(8,TestCompoundHex);
        TESTCASE(9,TestFiltering);
        TESTCASE(10,TestInlineSet);
        TESTCASE(11,TestPatternQuoting);
        TESTCASE(12,TestJ277);
        TESTCASE(13,TestJ243);
        TESTCASE(14,TestJ329);
        TESTCASE(15,TestSegments);
        TESTCASE(16,TestCursorOffset);
        TESTCASE(17,TestArbitraryVariableValues);
        TESTCASE(18,TestPositionHandling);
        TESTCASE(19,TestHiraganaKatakana);
        TESTCASE(20,TestCopyJ476);
        TESTCASE(21,TestAnchors);
        TESTCASE(22,TestInterIndic);
        TESTCASE(23,TestFilterIDs);
        TESTCASE(24,TestCaseMap);
        TESTCASE(25,TestNameMap);
        TESTCASE(26,TestLiberalizedID);
        TESTCASE(27,TestCreateInstance);
        TESTCASE(28,TestNormalizationTransliterator);
        TESTCASE(29,TestCompoundRBT);
        TESTCASE(30,TestCompoundFilter);
        TESTCASE(31,TestRemove);
        TESTCASE(32,TestToRules);
        TESTCASE(33,TestContext);
        TESTCASE(34,TestSupplemental);
        TESTCASE(35,TestQuantifier);
        TESTCASE(36,TestSTV);
        TESTCASE(37,TestCompoundInverse);
        TESTCASE(38,TestNFDChainRBT);
        TESTCASE(39,TestNullInverse);
        TESTCASE(40,TestAliasInverseID);
        TESTCASE(41,TestCompoundInverseID);
        TESTCASE(42,TestUndefinedVariable);
        TESTCASE(43,TestEmptyContext);
        TESTCASE(44,TestCompoundFilterID);
        default: name = ""; break;
    }
}

/**
 * Make sure every system transliterator can be instantiated.
 * 
 * ALSO test that the result of toRules() for each rule is a valid
 * rule.  Do this here so we don't have to have another test that
 * instantiates everything as well.
 */
void TransliteratorTest::TestInstantiation() {
    int32_t n = Transliterator::countAvailableIDs();
    UnicodeString name;
    for (int32_t i=0; i<n; ++i) {
        UnicodeString id = Transliterator::getAvailableID(i);
        if (id.length() < 1) {
            errln(UnicodeString("FAIL: getAvailableID(") +
                  i + ") returned empty string");
            continue;
        }
        UParseError parseError;
        UErrorCode status = U_ZERO_ERROR;
        Transliterator* t = Transliterator::createInstance(id,
                              UTRANS_FORWARD, parseError,status);
        name.truncate(0);
        Transliterator::getDisplayName(id, name);
        if (t == 0) {
            errln(UnicodeString("FAIL: Couldn't create ") + id +
                  /*", parse error " + parseError.code +*/
                  ", line " + parseError.line +
                  ", offset " + parseError.offset +
                  ", pre-context " + prettify(parseError.preContext, TRUE) +
                  ", post-context " +prettify(parseError.postContext,TRUE) +
                  ", Error: " + u_errorName(status));
            // When createInstance fails, it deletes the failing
            // entry from the available ID list.  We detect this
            // here by looking for a change in countAvailableIDs.
            int32_t nn = Transliterator::countAvailableIDs();
            if (nn == (n - 1)) {
                n = nn;
                --i; // Compensate for deleted entry
            }
        } else {
            logln(UnicodeString("OK: ") + name + " (" + id + ")");

            // Now test toRules
            UnicodeString rules;
            t->toRules(rules, TRUE);
            Transliterator *u = Transliterator::createFromRules("x",
                                    rules, UTRANS_FORWARD, parseError,status);
            if (u == 0) {
                errln(UnicodeString("FAIL: ") + id +
                      ".toRules() => bad rules" +
                      /*", parse error " + parseError.code +*/
                      ", line " + parseError.line +
                      ", offset " + parseError.offset +
                      ", context " + prettify(parseError.preContext, TRUE) +
                      ", rules: " + prettify(rules, TRUE));
            } else {
                delete u;
            }
            delete t;
        }
    }

    // Now test the failure path
    UParseError parseError;
    UErrorCode status = U_ZERO_ERROR;
    UnicodeString id("<Not a valid Transliterator ID>");
    Transliterator* t = Transliterator::createInstance(id, UTRANS_FORWARD, parseError, status);
    if (t != 0) {
        errln("FAIL: " + id + " returned a transliterator");
        delete t;
    } else {
        logln("OK: Bogus ID handled properly");
    }
}

void TransliteratorTest::TestSimpleRules(void) {
    /* Example: rules 1. ab>x|y
     *                2. yc>z
     *
     * []|eabcd  start - no match, copy e to tranlated buffer
     * [e]|abcd  match rule 1 - copy output & adjust cursor
     * [ex|y]cd  match rule 2 - copy output & adjust cursor
     * [exz]|d   no match, copy d to transliterated buffer
     * [exzd]|   done
     */
    expect(UnicodeString("ab>x|y;", "") +
           "yc>z",
           "eabcd", "exzd");

    /* Another set of rules:
     *    1. ab>x|yzacw
     *    2. za>q
     *    3. qc>r
     *    4. cw>n
     *
     * []|ab       Rule 1
     * [x|yzacw]   No match
     * [xy|zacw]   Rule 2
     * [xyq|cw]    Rule 4
     * [xyqn]|     Done
     */
    expect(UnicodeString("ab>x|yzacw;") +
           "za>q;" +
           "qc>r;" +
           "cw>n",
           "ab", "xyqn");

    /* Test categories
     */
    UErrorCode status = U_ZERO_ERROR;
    RuleBasedTransliterator t(
        "<ID>",
        UnicodeString("$dummy=").append((UChar)0xE100) +
        UnicodeString(";"
                      "$vowel=[aeiouAEIOU];"
                      "$lu=[:Lu:];"
                      "$vowel } $lu > '!';"
                      "$vowel > '&';"
                      "'!' { $lu > '^';"
                      "$lu > '*';"
                      "a > ERROR", ""),
        status);
    if (U_FAILURE(status)) {
        errln("FAIL: RBT constructor failed");
        return;
    }
    expect(t, "abcdefgABCDEFGU", "&bcd&fg!^**!^*&");
}

/**
 * Test inline set syntax and set variable syntax.
 */
void TransliteratorTest::TestInlineSet(void) {
    expect("{ [:Ll:] } x > y; [:Ll:] > z;", "aAbxq", "zAyzz");
    expect("a[0-9]b > qrs", "1a7b9", "1qrs9");
    
    expect(UnicodeString(
           "$digit = [0-9];"
           "$alpha = [a-zA-Z];"
           "$alphanumeric = [$digit $alpha];" // ***
           "$special = [^$alphanumeric];"     // ***
           "$alphanumeric > '-';"
           "$special > '*';", ""),
           
           "thx-1138", "---*----");
}

/**
 * Create some inverses and confirm that they work.  We have to be
 * careful how we do this, since the inverses will not be true
 * inverses -- we can't throw any random string at the composition
 * of the transliterators and expect the identity function.  F x
 * F' != I.  However, if we are careful about the input, we will
 * get the expected results.
 */
void TransliteratorTest::TestRuleBasedInverse(void) {
    UnicodeString RULES =
        UnicodeString("abc>zyx;") +
        "ab>yz;" +
        "bc>zx;" +
        "ca>xy;" +
        "a>x;" +
        "b>y;" +
        "c>z;" +

        "abc<zyx;" +
        "ab<yz;" +
        "bc<zx;" +
        "ca<xy;" +
        "a<x;" +
        "b<y;" +
        "c<z;" +

        "";

    const char* DATA[] = {
        // Careful here -- random strings will not work.  If we keep
        // the left side to the domain and the right side to the range
        // we will be okay though (left, abc; right xyz).
        "a", "x",
        "abcacab", "zyxxxyy",
        "caccb", "xyzzy",
    };

    int32_t DATA_length = (int32_t)(sizeof(DATA) / sizeof(DATA[0]));

    UErrorCode status = U_ZERO_ERROR;
    RuleBasedTransliterator fwd("<ID>", RULES, status);
    RuleBasedTransliterator rev("<ID>", RULES,
                                UTRANS_REVERSE, status);
    if (U_FAILURE(status)) {
        errln("FAIL: RBT constructor failed");
        return;
    }
    for (int32_t i=0; i<DATA_length; i+=2) {
        expect(fwd, DATA[i], DATA[i+1]);
        expect(rev, DATA[i+1], DATA[i]);
    }
}

/**
 * Basic test of keyboard.
 */
void TransliteratorTest::TestKeyboard(void) {
    UErrorCode status = U_ZERO_ERROR;
    RuleBasedTransliterator t("<ID>",
                              UnicodeString("psch>Y;")
                              +"ps>y;"
                              +"ch>x;"
                              +"a>A;",
                              status);
    if (U_FAILURE(status)) {
        errln("FAIL: RBT constructor failed");
        return;
    }
    const char* DATA[] = {
        // insertion, buffer
        "a", "A",
        "p", "Ap",
        "s", "Aps",
        "c", "Apsc",
        "a", "AycA",
        "psch", "AycAY",
        0, "AycAY", // null means finishKeyboardTransliteration
    };

    keyboardAux(t, DATA, (int32_t)(sizeof(DATA)/sizeof(DATA[0])));
}

/**
 * Basic test of keyboard with cursor.
 */
void TransliteratorTest::TestKeyboard2(void) {
    UErrorCode status = U_ZERO_ERROR;
    RuleBasedTransliterator t("<ID>",
                              UnicodeString("ych>Y;")
                              +"ps>|y;"
                              +"ch>x;"
                              +"a>A;",
                              status);
    if (U_FAILURE(status)) {
        errln("FAIL: RBT constructor failed");
        return;
    }
    const char* DATA[] = {
        // insertion, buffer
        "a", "A",
        "p", "Ap",
        "s", "Ay",
        "c", "Ayc",
        "a", "AycA",
        "p", "AycAp",
        "s", "AycAy",
        "c", "AycAyc",
        "h", "AycAY",
        0, "AycAY", // null means finishKeyboardTransliteration
    };

    keyboardAux(t, DATA, (int32_t)(sizeof(DATA)/sizeof(DATA[0])));
}

/**
 * Test keyboard transliteration with back-replacement.
 */
void TransliteratorTest::TestKeyboard3(void) {
    // We want th>z but t>y.  Furthermore, during keyboard
    // transliteration we want t>y then yh>z if t, then h are
    // typed.
    UnicodeString RULES("t>|y;"
                        "yh>z;");

    const char* DATA[] = {
        // Column 1: characters to add to buffer (as if typed)
        // Column 2: expected appearance of buffer after
        //           keyboard xliteration.
        "a", "a",
        "b", "ab",
        "t", "aby",
        "c", "abyc",
        "t", "abycy",
        "h", "abycz",
        0, "abycz", // null means finishKeyboardTransliteration
    };

    UErrorCode status = U_ZERO_ERROR;
    RuleBasedTransliterator t("<ID>", RULES, status);
    if (U_FAILURE(status)) {
        errln("FAIL: RBT constructor failed");
        return;
    }
    keyboardAux(t, DATA, (int32_t)(sizeof(DATA)/sizeof(DATA[0])));
}

void TransliteratorTest::keyboardAux(const Transliterator& t,
                                     const char* DATA[], int32_t DATA_length) {
    UErrorCode status = U_ZERO_ERROR;
    UTransPosition index={0, 0, 0, 0};
    UnicodeString s;
    for (int32_t i=0; i<DATA_length; i+=2) {
        UnicodeString log;
        if (DATA[i] != 0) {
            log = s + " + "
                + DATA[i]
                + " -> ";
            t.transliterate(s, index, DATA[i], status);
        } else {
            log = s + " => ";
            t.finishTransliteration(s, index);
        }
        // Show the start index '{' and the cursor '|'
        UnicodeString a, b, c;
        s.extractBetween(0, index.contextStart, a);
        s.extractBetween(index.contextStart, index.start, b);
        s.extractBetween(index.start, s.length(), c);
        log.append(a).
            append((UChar)LEFT_BRACE).
            append(b).
            append((UChar)PIPE).
            append(c);
        if (s == DATA[i+1] && U_SUCCESS(status)) {
            logln(log);
        } else {
            errln(UnicodeString("FAIL: ") + log + ", expected " + DATA[i+1]);
        }
    }
}

void TransliteratorTest::TestArabic(void) {
    /*
    const char* DATA[] = {
        "Arabic", "\u062a\u062a\u0645\u062a\u0639\u0020"+
                  "\u0627\u0644\u0644\u063a\u0629\u0020"+
                  "\u0627\u0644\u0639\u0631\u0628\u0628\u064a\u0629\u0020"+
                  "\u0628\u0628\u0646\u0638\u0645\u0020"+
                  "\u0643\u062a\u0627\u0628\u0628\u064a\u0629\u0020"+
                  "\u062c\u0645\u064a\u0644\u0629",
    };
    */

    UChar ar_raw[] = {
        0x062a, 0x062a, 0x0645, 0x062a, 0x0639, 0x0020, 0x0627,
        0x0644, 0x0644, 0x063a, 0x0629, 0x0020, 0x0627, 0x0644,
        0x0639, 0x0631, 0x0628, 0x0628, 0x064a, 0x0629, 0x0020,
        0x0628, 0x0628, 0x0646, 0x0638, 0x0645, 0x0020, 0x0643,
        0x062a, 0x0627, 0x0628, 0x0628, 0x064a, 0x0629, 0x0020,
        0x062c, 0x0645, 0x064a, 0x0644, 0x0629, 0
    };
    UnicodeString ar(ar_raw);
    UErrorCode status=U_ZERO_ERROR;
    UParseError parseError;
    Transliterator *t = Transliterator::createInstance("Latin-Arabic", UTRANS_FORWARD, parseError, status);
    if (t == 0) {
        errln("FAIL: createInstance failed");
        return;
    }
    expect(*t, "Arabic", ar);
    delete t;
}

/**
 * Compose the Kana transliterator forward and reverse and try
 * some strings that should come out unchanged.
 */
void TransliteratorTest::TestCompoundKana(void) {
    UParseError parseError;
    UErrorCode status = U_ZERO_ERROR;
    Transliterator* t = Transliterator::createInstance("Latin-Kana;Kana-Latin", UTRANS_FORWARD, parseError, status);
    if (t == 0) {
        errln("FAIL: construction of Latin-Kana;Kana-Latin failed");
    } else {
        expect(*t, "aaaaa", "aaaaa");
        delete t;
    }
}

/**
 * Compose the hex transliterators forward and reverse.
 */
void TransliteratorTest::TestCompoundHex(void) {
    UParseError parseError;
    UErrorCode status = U_ZERO_ERROR;
    Transliterator* a = Transliterator::createInstance("Any-Hex", UTRANS_FORWARD, parseError, status);
    Transliterator* b = Transliterator::createInstance("Hex-Any", UTRANS_FORWARD, parseError, status);
    Transliterator* transab[] = { a, b };
    Transliterator* transba[] = { b, a };
    if (a == 0 || b == 0) {
        errln("FAIL: construction failed");
        delete a;
        delete b;
        return;
    }
    // Do some basic tests of a
    expect(*a, "01", UnicodeString("\\u0030\\u0031", ""));
    // Do some basic tests of b
    expect(*b, UnicodeString("\\u0030\\u0031", ""), "01");

    Transliterator* ab = new CompoundTransliterator(transab, 2);
    UnicodeString s("abcde", "");
    expect(*ab, s, s);

    UnicodeString str(s);
    a->transliterate(str);
    Transliterator* ba = new CompoundTransliterator(transba, 2);
    expect(*ba, str, str);

    delete ab;
    delete ba;
    delete a;
    delete b;
}

/**
 * Used by TestFiltering().
 */
class TestFilter : public UnicodeFilter {
    virtual UnicodeMatcher* clone() const {
        return new TestFilter(*this);
    }
    virtual UBool contains(UChar32 c) const {
        return c != (UChar)0x0063 /*c*/;
    }
};

/**
 * Do some basic tests of filtering.
 */
void TransliteratorTest::TestFiltering(void) {
    UParseError parseError;
    UErrorCode status = U_ZERO_ERROR;
    Transliterator* hex = Transliterator::createInstance("Any-Hex", UTRANS_FORWARD, parseError, status);
    if (hex == 0) {
        errln("FAIL: createInstance(Any-Hex) failed");
        return;
    }
    hex->adoptFilter(new TestFilter());
    UnicodeString s("abcde");
    hex->transliterate(s);
    UnicodeString exp("\\u0061\\u0062c\\u0064\\u0065", "");
    if (s == exp) {
        logln(UnicodeString("Ok:   \"") + exp + "\"");
    } else {
        logln(UnicodeString("FAIL: \"") + s + "\", wanted \"" + exp + "\"");
    }
    delete hex;
}

/**
 * Test anchors
 */
void TransliteratorTest::TestAnchors(void) {
    expect(UnicodeString("^a  > 0; a$ > 2 ; a > 1;", ""),
           "aaa",
           "012");
    expect(UnicodeString("$s=[z$]; $s{a>0; a}$s>2; a>1;", ""),
           "aaa",
           "012");
    expect(UnicodeString("^ab  > 01 ;"
           " ab  > |8 ;"
           "  b  > k ;"
           " 8x$ > 45 ;"
           " 8x  > 77 ;", ""),

           "ababbabxabx",
           "018k7745");
    expect(UnicodeString("$s = [z$] ;"
           "$s{ab    > 01 ;"
           "   ab    > |8 ;"
           "    b    > k ;"
           "   8x}$s > 45 ;"
           "   8x    > 77 ;", ""),

           "abzababbabxzabxabx",
           "01z018k45z01x45");
}

/**
 * Test pattern quoting and escape mechanisms.
 */
void TransliteratorTest::TestPatternQuoting(void) {
    // Array of 3n items
    // Each item is <rules>, <input>, <expected output>
    const UnicodeString DATA[] = {
        UnicodeString(UChar(0x4E01)) + ">'[male adult]'",
        UnicodeString(UChar(0x4E01)),
        "[male adult]"
    };

    for (int32_t i=0; i<3; i+=3) {
        logln(UnicodeString("Pattern: ") + prettify(DATA[i]));
        UErrorCode status = U_ZERO_ERROR;
        RuleBasedTransliterator t("<ID>", DATA[i], status);
        if (U_FAILURE(status)) {
            errln("RBT constructor failed");
        } else {
            expect(t, DATA[i+1], DATA[i+2]);
        }
    }
}

/**
 * Regression test for bugs found in Greek transliteration.
 */
void TransliteratorTest::TestJ277(void) {
    UErrorCode status = U_ZERO_ERROR;
    UParseError parseError;
    Transliterator *gl = Transliterator::createInstance("Greek-Latin", UTRANS_FORWARD, parseError, status);
    if (gl == NULL) {
        errln("FAIL: createInstance(Greek-Latin) returned NULL");
        return;
    }

    UChar sigma = 0x3C3;
    UChar upsilon = 0x3C5;
    UChar nu = 0x3BD;
//    UChar PHI = 0x3A6;
    UChar alpha = 0x3B1;
//    UChar omega = 0x3C9;
//    UChar omicron = 0x3BF;
//    UChar epsilon = 0x3B5;

    // sigma upsilon nu -> syn
    UnicodeString syn;
    syn.append(sigma).append(upsilon).append(nu);
    expect(*gl, syn, "syn");

    // sigma alpha upsilon nu -> saun
    UnicodeString sayn;
    sayn.append(sigma).append(alpha).append(upsilon).append(nu);
    expect(*gl, sayn, "saun");

    // Again, using a smaller rule set
    UnicodeString rules(
                "$alpha   = \\u03B1;"
                "$nu      = \\u03BD;"
                "$sigma   = \\u03C3;"
                "$ypsilon = \\u03C5;"
                "$vowel   = [aeiouAEIOU$alpha$ypsilon];"
                "s <>           $sigma;"
                "a <>           $alpha;"
                "u <>  $vowel { $ypsilon;"
                "y <>           $ypsilon;"
                "n <>           $nu;",
                "");
    RuleBasedTransliterator mini("mini", rules, UTRANS_REVERSE, status);
    if (U_FAILURE(status)) { errln("FAIL: Transliterator constructor failed"); return; }
    expect(mini, syn, "syn");
    expect(mini, sayn, "saun");

    // Transliterate the Greek locale data
    Locale el("el");
    DateFormatSymbols syms(el, status);
    if (U_FAILURE(status)) { errln("FAIL: Transliterator constructor failed"); return; }
    int32_t i, count;
    const UnicodeString* data = syms.getMonths(count);
    for (i=0; i<count; ++i) {
        if (data[i].length() == 0) {
            continue;
        }
        UnicodeString out(data[i]);
        gl->transliterate(out);
        UBool ok = TRUE;
        if (data[i].length() >= 2 && out.length() >= 2 &&
            u_isupper(data[i].charAt(0)) && u_islower(data[i].charAt(1))) {
            if (!(u_isupper(out.charAt(0)) && u_islower(out.charAt(1)))) {
                ok = FALSE;
            }
        }
        if (ok) {
            logln(prettify(data[i] + " -> " + out));
        } else {
            errln(UnicodeString("FAIL: ") + prettify(data[i] + " -> " + out));
        }
    }

    delete gl;
}

/**
 * Prefix, suffix support in hex transliterators
 */
void TransliteratorTest::TestJ243(void) {
    UErrorCode status = U_ZERO_ERROR;
    
#if !defined(HPUX)
    // Test default Hex-Any, which should handle
    // \u, \U, u+, and U+
    HexToUnicodeTransliterator hex;
    expect(hex, UnicodeString("\\u0041+\\U0042,u+0043uu+0044z", ""), "A+B,CuDz");
    // Try a custom Hex-Unicode
    // \uXXXX and &#xXXXX;
    status = U_ZERO_ERROR;
    HexToUnicodeTransliterator hex2(UnicodeString("\\\\u###0;&\\#x###0\\;", ""), status);
    expect(hex2, UnicodeString("\\u61\\u062\\u0063\\u00645\\u66x&#x30;&#x031;&#x0032;&#x00033;", ""),
           "abcd5fx012&#x00033;");
    // Try custom Any-Hex (default is tested elsewhere)
    status = U_ZERO_ERROR;
    UnicodeToHexTransliterator hex3(UnicodeString("&\\#x###0;", ""), status);
    expect(hex3, "012", "&#x30;&#x31;&#x32;");
#endif
}

/**
 * Parsers need better syntax error messages.
 */
void TransliteratorTest::TestJ329(void) {
    
    struct { UBool containsErrors; const char* rule; } DATA[] = {
        { FALSE, "a > b; c > d" },
        { TRUE,  "a > b; no operator; c > d" },
    };
    int32_t DATA_length = (int32_t)(sizeof(DATA) / sizeof(DATA[0]));

    for (int32_t i=0; i<DATA_length; ++i) {
        UErrorCode status = U_ZERO_ERROR;
        UParseError parseError;
        RuleBasedTransliterator rbt("<ID>",
                                    DATA[i].rule,
                                    UTRANS_FORWARD,
                                    0,
                                    parseError,
                                    status);
        UBool gotError = U_FAILURE(status);
        UnicodeString desc(DATA[i].rule);
        desc.append(gotError ? " -> error" : " -> no error");
        if (gotError) {
            desc = desc + ", ParseError code=" + u_errorName(status) +
                " line=" + parseError.line +
                " offset=" + parseError.offset +
                " context=" + parseError.preContext;
        }
        if (gotError == DATA[i].containsErrors) {
            logln(UnicodeString("Ok:   ") + desc);
        } else {
            errln(UnicodeString("FAIL: ") + desc);
        }
    }
}

/**
 * Test segments and segment references.
 */
void TransliteratorTest::TestSegments(void) {
    // Array of 3n items
    // Each item is <rules>, <input>, <expected output>
    UnicodeString DATA[] = {
        "([a-z]) '.' ([0-9]) > $2 '-' $1",
        "abc.123.xyz.456",
        "ab1-c23.xy4-z56",

        // nested
        "(([a-z])([0-9])) > $1 '.' $2 '.' $3;",
        "a1 b2",
        "a1.a.1 b2.b.2",
    };
    int32_t DATA_length = (int32_t)(sizeof(DATA)/sizeof(*DATA));

    for (int32_t i=0; i<DATA_length; i+=3) {
        logln("Pattern: " + prettify(DATA[i]));
        UErrorCode status = U_ZERO_ERROR;
        RuleBasedTransliterator t("ID", DATA[i], status);
        if (U_FAILURE(status)) {
            errln("FAIL: RBT constructor");
        } else {
            expect(t, DATA[i+1], DATA[i+2]);
        }
    }
}

/**
 * Test cursor positioning outside of the key
 */
void TransliteratorTest::TestCursorOffset(void) {
    // Array of 3n items
    // Each item is <rules>, <input>, <expected output>
    UnicodeString DATA[] = {
        "pre {alpha} post > | @ ALPHA ;"
        "eALPHA > beta ;"
        "pre {beta} post > BETA @@ | ;"
        "post > xyz",

        "prealphapost prebetapost",

        "prbetaxyz preBETApost",
    };
    int32_t DATA_length = (int32_t)(sizeof(DATA)/sizeof(*DATA));

    for (int32_t i=0; i<DATA_length; i+=3) {
        logln("Pattern: " + prettify(DATA[i]));
        UErrorCode status = U_ZERO_ERROR;
        RuleBasedTransliterator t("<ID>", DATA[i], status);
        if (U_FAILURE(status)) {
            errln("FAIL: RBT constructor");
        } else {
            expect(t, DATA[i+1], DATA[i+2]);
        }
    }
}

/**
 * Test zero length and > 1 char length variable values.  Test
 * use of variable refs in UnicodeSets.
 */
void TransliteratorTest::TestArbitraryVariableValues(void) {
    // Array of 3n items
    // Each item is <rules>, <input>, <expected output>
    UnicodeString DATA[] = {
        "$abe = ab;"
        "$pat = x[yY]z;"
        "$ll  = 'a-z';"
        "$llZ = [$ll];"
        "$llY = [$ll$pat];"
        "$emp = ;"

        "$abe > ABE;"
        "$pat > END;"
        "$llZ > 1;"
        "$llY > 2;"
        "7$emp 8 > 9;"
        "",

        "ab xYzxyz stY78",
        "ABE ENDEND 1129",
    };
    int32_t DATA_length = (int32_t)(sizeof(DATA)/sizeof(*DATA));

    for (int32_t i=0; i<DATA_length; i+=3) {
        logln("Pattern: " + prettify(DATA[i]));
        UErrorCode status = U_ZERO_ERROR;
        RuleBasedTransliterator t("<ID>", DATA[i], status);
        if (U_FAILURE(status)) {
            errln("FAIL: RBT constructor");
        } else {
            expect(t, DATA[i+1], DATA[i+2]);
        }
    }
}

/**
 * Confirm that the contextStart, contextLimit, start, and limit
 * behave correctly. J474.
 */
void TransliteratorTest::TestPositionHandling(void) {
    // Array of 3n items
    // Each item is <rules>, <input>, <expected output>
    const char* DATA[] = {
        "a{t} > SS ; {t}b > UU ; {t} > TT ;",
        "xtat txtb", // pos 0,9,0,9
        "xTTaSS TTxUUb",

        "a{t} > SS ; {t}b > UU ; {t} > TT ; a > A ; b > B ;",
        "xtat txtb", // pos 2,9,3,8
        "xtaSS TTxUUb",

        "a{t} > SS ; {t}b > UU ; {t} > TT ; a > A ; b > B ;",
        "xtat txtb", // pos 3,8,3,8
        "xtaTT TTxTTb",
    };

    // Array of 4n positions -- these go with the DATA array
    // They are: contextStart, contextLimit, start, limit
    int32_t POS[] = {
        0, 9, 0, 9,
        2, 9, 3, 8,
        3, 8, 3, 8,
    };

    int32_t n = (int32_t)(sizeof(DATA) / sizeof(DATA[0])) / 3;
    for (int32_t i=0; i<n; i++) {
        UErrorCode status = U_ZERO_ERROR;
        Transliterator *t = new RuleBasedTransliterator("<ID>",
                                                        DATA[3*i], status);
        if (U_FAILURE(status)) {
            delete t;
            errln("FAIL: RBT constructor");
            return;
        }
        UTransPosition pos;
        pos.contextStart= POS[4*i];
        pos.contextLimit = POS[4*i+1];
        pos.start = POS[4*i+2];
        pos.limit = POS[4*i+3];
        UnicodeString rsource(DATA[3*i+1]);
        t->transliterate(rsource, pos, status);
        if (U_FAILURE(status)) {
            delete t;
            errln("FAIL: transliterate");
            return;
        }
        t->finishTransliteration(rsource, pos);
        expectAux(DATA[3*i],
                  DATA[3*i+1],
                  rsource,
                  DATA[3*i+2]);
        delete t;
    }
}

/**
 * Test the Hiragana-Katakana transliterator.
 */
void TransliteratorTest::TestHiraganaKatakana(void) {
    UParseError parseError;
    UErrorCode status = U_ZERO_ERROR;
    Transliterator* hk = Transliterator::createInstance("Hiragana-Katakana", UTRANS_FORWARD, parseError, status);
    Transliterator* kh = Transliterator::createInstance("Katakana-Hiragana", UTRANS_FORWARD, parseError, status);
    if (hk == 0 || kh == 0) {
        errln("FAIL: createInstance failed");
        delete hk;
        delete kh;
        return;
    }

    // Array of 3n items
    // Each item is "hk"|"kh"|"both", <Hiragana>, <Katakana>
    const char* DATA[] = {
        "both",
        "\\u3042\\u3090\\u3099\\u3092\\u3050",
        "\\u30A2\\u30F8\\u30F2\\u30B0",

        "kh",
        "\\u307C\\u3051\\u3060\\u3042\\u3093\\u30FC",
        "\\u30DC\\u30F6\\u30C0\\u30FC\\u30F3\\u30FC",
    };
    int32_t DATA_length = (int32_t)(sizeof(DATA) / sizeof(DATA[0]));

    for (int32_t i=0; i<DATA_length; i+=3) {
        UnicodeString h = CharsToUnicodeString(DATA[i+1]);
        UnicodeString k = CharsToUnicodeString(DATA[i+2]);
        switch (*DATA[i]) {
        case 0x68: //'h': // Hiragana-Katakana
            expect(*hk, h, k);
            break;
        case 0x6B: //'k': // Katakana-Hiragana
            expect(*kh, k, h);
            break;
        case 0x62: //'b': // both
            expect(*hk, h, k);
            expect(*kh, k, h);
            break;
        }
    }
    delete hk;
    delete kh;
}

/**
 * Test cloning / copy constructor of RBT.
 */
void TransliteratorTest::TestCopyJ476(void) {
    // The real test here is what happens when the destructors are
    // called.  So we let one object get destructed, and check to
    // see that its copy still works.
    RuleBasedTransliterator *t2 = 0;
    {
        UErrorCode status = U_ZERO_ERROR;
        RuleBasedTransliterator t1("t1", "a>A;b>B;", status);
        if (U_FAILURE(status)) {
            errln("FAIL: RBT constructor");
            return;
        }
        t2 = new RuleBasedTransliterator(t1);
        expect(t1, "abc", "ABc");
    }
    expect(*t2, "abc", "ABc");
    delete t2;
}

/**
 * Test inter-Indic transliterators.  These are composed.
 * ICU4C Jitterbug 483.
 */
void TransliteratorTest::TestInterIndic(void) {
    UnicodeString ID("Devanagari-Gujarati", "");
    UErrorCode status = U_ZERO_ERROR;
    UParseError parseError;
    Transliterator* dg = Transliterator::createInstance(ID, UTRANS_FORWARD, parseError, status);
    if (dg == 0) {
        errln("FAIL: createInstance(" + ID + ") returned NULL");
        return;
    }
    UnicodeString id = dg->getID();
    if (id != ID) {
        errln("FAIL: createInstance(" + ID + ")->getID() => " + id);
    }
    UnicodeString dev = CharsToUnicodeString("\\u0901\\u090B\\u0925");
    UnicodeString guj = CharsToUnicodeString("\\u0A81\\u0A8B\\u0AA5");
    expect(*dg, dev, guj);
    delete dg;
}

/**
 * Test filter syntax in IDs. (J918)
 */
void TransliteratorTest::TestFilterIDs(void) {
    // Array of 3n strings:
    // <id>, <inverse id>, <input>, <expected output>
    const char* DATA[] = {
        "Any[aeiou]-Hex",
        "Hex[aeiou]-Any",
        "quizzical",
        "q\\u0075\\u0069zz\\u0069c\\u0061l",
        
        "Any[aeiou]-Hex;Hex[^5]-Any",
        "Any[^5]-Hex;Hex[aeiou]-Any",
        "quizzical",
        "q\\u0075izzical",

        "Null[abc]",
        "Null[abc]",
        "xyz",
        "xyz",
    };
    enum { DATA_length = sizeof(DATA) / sizeof(DATA[0]) };

    for (int i=0; i<DATA_length; i+=4) {
        UnicodeString ID(DATA[i], "");
        UnicodeString uID(DATA[i+1], "");
        UnicodeString data2(DATA[i+2], "");
        UnicodeString data3(DATA[i+3], "");
        UParseError parseError;
        UErrorCode status = U_ZERO_ERROR;
        Transliterator *t = Transliterator::createInstance(ID, UTRANS_FORWARD, parseError, status);
        if (t == 0) {
            errln("FAIL: createInstance(" + ID + ") returned NULL");
            return;
        }
        expect(*t, data2, data3);

        // Check the ID
        if (ID != t->getID()) {
            errln("FAIL: createInstance(" + ID + ").getID() => " +
                  t->getID());
        }

        // Check the inverse
        Transliterator *u = t->createInverse(status);
        if (u == 0) {
            errln("FAIL: " + ID + ".createInverse() returned NULL");
        } else if (u->getID() != uID) {
            errln("FAIL: " + ID + ".createInverse().getID() => " +
                  u->getID() + ", expected " + uID);
        }

        delete t;
        delete u;
    }
}

/**
 * Test the case mapping transliterators.
 */
void TransliteratorTest::TestCaseMap(void) {
    UParseError parseError;
    UErrorCode status = U_ZERO_ERROR;
    Transliterator* toUpper =
        Transliterator::createInstance("Any-Upper[^xyzXYZ]", UTRANS_FORWARD, parseError, status);
    Transliterator* toLower =
        Transliterator::createInstance("Any-Lower[^xyzXYZ]", UTRANS_FORWARD, parseError, status);
    Transliterator* toTitle =
        Transliterator::createInstance("Any-Title[^xyzXYZ]", UTRANS_FORWARD, parseError, status);
    if (toUpper==0 || toLower==0 || toTitle==0) {
        errln("FAIL: createInstance returned NULL");
        delete toUpper;
        delete toLower;
        delete toTitle;
        return;
    }

    expect(*toUpper, "The quick brown fox jumped over the lazy dogs.",
           "THE QUICK BROWN FOx JUMPED OVER THE LAzy DOGS.");
    expect(*toLower, "The quIck brown fOX jUMPED OVER THE LAzY dogs.",
           "the quick brown foX jumped over the lazY dogs.");
    expect(*toTitle, "the quick brown foX can't jump over the laZy dogs.",
           "The Quick Brown FoX Can't Jump Over The LaZy Dogs.");

    delete toUpper;
    delete toLower;
    delete toTitle;
}

/**
 * Test the name mapping transliterators.
 */
void TransliteratorTest::TestNameMap(void) {
    UParseError parseError;
    UErrorCode status = U_ZERO_ERROR;
    Transliterator* uni2name =
        Transliterator::createInstance("Any-Name[^abc]", UTRANS_FORWARD, parseError, status);
    Transliterator* name2uni =
        Transliterator::createInstance("Name-Any", UTRANS_FORWARD, parseError, status);
    if (uni2name==0 || name2uni==0) {
        errln("FAIL: createInstance returned NULL");
        delete uni2name;
        delete name2uni;
        return;
    }

    expect(*uni2name, CharsToUnicodeString("\\u00A0abc\\u4E01\\u00B5\\u0A81\\uFFFD\\uFFFF"),
           CharsToUnicodeString("{NO-BREAK SPACE}abc{CJK UNIFIED IDEOGRAPH-4E01}{MICRO SIGN}{GUJARATI SIGN CANDRABINDU}{REPLACEMENT CHARACTER}\\uFFFF"));
    expect(*name2uni, "{NO-BREAK SPACE}abc{CJK UNIFIED IDEOGRAPH-4E01}{x{MICRO SIGN}{GUJARATI SIGN CANDRABINDU}{REPLACEMENT CHARACTER}{",
           CharsToUnicodeString("\\u00A0abc\\u4E01{x\\u00B5\\u0A81\\uFFFD{"));

    delete uni2name;
    delete name2uni;
}

/**
 * Test liberalized ID syntax.  1006c
 */
void TransliteratorTest::TestLiberalizedID(void) {
    // Some test cases have an expected getID() value of NULL.  This
    // means I have disabled the test case for now.  This stuff is
    // still under development, and I haven't decided whether to make
    // getID() return canonical case yet.  It will all get rewritten
    // with the move to Source-Target/Variant IDs anyway. [aliu]
    const char* DATA[] = {
        "latin-arabic", NULL /*"Latin-Arabic"*/, "case insensitivity",
        "  Null  ", "Null", "whitespace",
        " Latin[a-z]-Arabic  ", "Latin[a-z]-Arabic", "inline filter",
        "  null  ; latin-arabic  ", NULL /*"Null;Latin-Arabic"*/, "compound whitespace",
    };
    const int32_t DATA_length = sizeof(DATA)/sizeof(DATA[0]);
    UParseError parseError;
    UErrorCode status= U_ZERO_ERROR;
    for (int32_t i=0; i<DATA_length; i+=3) {
        Transliterator *t = Transliterator::createInstance(DATA[i], UTRANS_FORWARD, parseError, status);
        if (t == 0) {
            errln(UnicodeString("FAIL: ") + DATA[i+2] +
                  " cannot create ID \"" + DATA[i] + "\"");
        } else {
            UnicodeString exp;
            if (DATA[i+1]) {
                exp = UnicodeString(DATA[i+1], "");
            }
            // Don't worry about getID() if the expected char*
            // is NULL -- see above.
            if (exp.length() == 0 || exp == t->getID()) {
                logln(UnicodeString("Ok: ") + DATA[i+2] +
                      " create ID \"" + DATA[i] + "\" => \"" +
                      exp + "\"");
            } else {
                errln(UnicodeString("FAIL: ") + DATA[i+2] +
                      " create ID \"" + DATA[i] + "\" => \"" +
                      t->getID() + "\", exp \"" + exp + "\"");
            }
            delete t;
        }
    }
}

/* test for Jitterbug 912 */
void TransliteratorTest::TestCreateInstance(){
    UParseError err;
    UErrorCode status = U_ZERO_ERROR;
    Transliterator* myTrans = Transliterator::createInstance(UnicodeString("Latin-Hangul"),UTRANS_REVERSE,err,status);
    if (myTrans == 0) {
        errln("FAIL: createInstance failed");
        return;
    }
    UnicodeString newID =myTrans->getID();
    if(newID!=UnicodeString("Hangul-Latin")){
        errln(UnicodeString("Test for Jitterbug 912 Transliterator::createInstance(id,UTRANS_REVERSE) failed"));
    }
    delete myTrans;
}

/**
 * Test the normalization transliterator.
 */
void TransliteratorTest::TestNormalizationTransliterator() {
    // THE FOLLOWING TWO TABLES ARE COPIED FROM com.ibm.test.normalizer.BasicTest
    // PLEASE KEEP THEM IN SYNC WITH BasicTest.
    const char* CANON[] = {
        // Input               Decomposed            Composed
        "cat",                "cat",                "cat"               ,
        "\\u00e0ardvark",      "a\\u0300ardvark",     "\\u00e0ardvark"    ,

        "\\u1e0a",             "D\\u0307",            "\\u1e0a"            , // D-dot_above
        "D\\u0307",            "D\\u0307",            "\\u1e0a"            , // D dot_above

        "\\u1e0c\\u0307",       "D\\u0323\\u0307",      "\\u1e0c\\u0307"      , // D-dot_below dot_above
        "\\u1e0a\\u0323",       "D\\u0323\\u0307",      "\\u1e0c\\u0307"      , // D-dot_above dot_below
        "D\\u0307\\u0323",      "D\\u0323\\u0307",      "\\u1e0c\\u0307"      , // D dot_below dot_above

        "\\u1e10\\u0307\\u0323", "D\\u0327\\u0323\\u0307","\\u1e10\\u0323\\u0307", // D dot_below cedilla dot_above
        "D\\u0307\\u0328\\u0323","D\\u0328\\u0323\\u0307","\\u1e0c\\u0328\\u0307", // D dot_above ogonek dot_below

        "\\u1E14",             "E\\u0304\\u0300",      "\\u1E14"            , // E-macron-grave
        "\\u0112\\u0300",       "E\\u0304\\u0300",      "\\u1E14"            , // E-macron + grave
        "\\u00c8\\u0304",       "E\\u0300\\u0304",      "\\u00c8\\u0304"      , // E-grave + macron

        "\\u212b",             "A\\u030a",            "\\u00c5"            , // angstrom_sign
        "\\u00c5",             "A\\u030a",            "\\u00c5"            , // A-ring

        "\\u00fdffin",         "y\\u0301ffin",        "\\u00fdffin"        ,    //updated with 3.0
        "\\u00fd\\uFB03n",      "y\\u0301\\uFB03n",     "\\u00fd\\uFB03n"     , //updated with 3.0

        "Henry IV",           "Henry IV",           "Henry IV"          ,
        "Henry \\u2163",       "Henry \\u2163",       "Henry \\u2163"      ,

        "\\u30AC",             "\\u30AB\\u3099",       "\\u30AC"            , // ga (Katakana)
        "\\u30AB\\u3099",       "\\u30AB\\u3099",       "\\u30AC"            , // ka + ten
        "\\uFF76\\uFF9E",       "\\uFF76\\uFF9E",       "\\uFF76\\uFF9E"      , // hw_ka + hw_ten
        "\\u30AB\\uFF9E",       "\\u30AB\\uFF9E",       "\\u30AB\\uFF9E"      , // ka + hw_ten
        "\\uFF76\\u3099",       "\\uFF76\\u3099",       "\\uFF76\\u3099"      , // hw_ka + ten

        "A\\u0300\\u0316",      "A\\u0316\\u0300",      "\\u00C0\\u0316"      ,
        0 // end
    };

    const char* COMPAT[] = {
        // Input               Decomposed            Composed
        "\\uFB4f",             "\\u05D0\\u05DC",       "\\u05D0\\u05DC"     , // Alef-Lamed vs. Alef, Lamed

        "\\u00fdffin",         "y\\u0301ffin",        "\\u00fdffin"        ,    //updated for 3.0
        "\\u00fd\\uFB03n",      "y\\u0301ffin",        "\\u00fdffin"        , // ffi ligature -> f + f + i

        "Henry IV",           "Henry IV",           "Henry IV"          ,
        "Henry \\u2163",       "Henry IV",           "Henry IV"          ,

        "\\u30AC",             "\\u30AB\\u3099",       "\\u30AC"            , // ga (Katakana)
        "\\u30AB\\u3099",       "\\u30AB\\u3099",       "\\u30AC"            , // ka + ten

        "\\uFF76\\u3099",       "\\u30AB\\u3099",       "\\u30AC"            , // hw_ka + ten
        0 // end
    };

    int32_t i;
    UParseError parseError;
    UErrorCode status = U_ZERO_ERROR;
    Transliterator* NFD = Transliterator::createInstance("NFD", UTRANS_FORWARD, parseError, status);
    Transliterator* NFC = Transliterator::createInstance("NFC", UTRANS_FORWARD, parseError, status);
    if (!NFD || !NFC) {
        errln("FAIL: createInstance failed");
        delete NFD;
        delete NFC;
        return;
    }
    for (i=0; CANON[i]; i+=3) {
        UnicodeString in = CharsToUnicodeString(CANON[i]);
        UnicodeString expd = CharsToUnicodeString(CANON[i+1]);
        UnicodeString expc = CharsToUnicodeString(CANON[i+2]);
        expect(*NFD, in, expd);
        expect(*NFC, in, expc);
    }
    delete NFD;
    delete NFC;

    Transliterator* NFKD = Transliterator::createInstance("NFKD", UTRANS_FORWARD, parseError, status);
    Transliterator* NFKC = Transliterator::createInstance("NFKC", UTRANS_FORWARD, parseError, status);
    if (!NFKD || !NFKC) {
        errln("FAIL: createInstance failed");
        delete NFKD;
        delete NFKC;
        return;
    }
    for (i=0; COMPAT[i]; i+=3) {
        UnicodeString in = CharsToUnicodeString(COMPAT[i]);
        UnicodeString expkd = CharsToUnicodeString(COMPAT[i+1]);
        UnicodeString expkc = CharsToUnicodeString(COMPAT[i+2]);
        expect(*NFKD, in, expkd);
        expect(*NFKC, in, expkc);
    }
    delete NFKD;
    delete NFKC;

    UParseError pe;
    status = U_ZERO_ERROR;
    Transliterator *t = Transliterator::createInstance("NFD; [x]Remove",
                                                       UTRANS_FORWARD,
                                                       pe, status);
    if (t == 0) {
        errln("FAIL: createInstance failed");
    }
    expect(*t, CharsToUnicodeString("\\u010dx"),
           CharsToUnicodeString("c\\u030C"));
    delete t;
}

/**
 * Test compound RBT rules.
 */
void TransliteratorTest::TestCompoundRBT(void) {
    // Careful with spacing and ';' here:  Phrase this exactly
    // as toRules() is going to return it.  If toRules() changes
    // with regard to spacing or ';', then adjust this string.
    UnicodeString rule("::Hex-Any;\n"
                       "::Any-Lower;\n"
                       "a > '.A.';\n"
                       "b > '.B.';\n"
                       "::Any[^t]-Upper;", "");
    UParseError parseError;
    UErrorCode status = U_ZERO_ERROR;
    Transliterator *t = Transliterator::createFromRules("Test", rule, UTRANS_FORWARD, parseError, status);
    if (t == 0) {
        errln("FAIL: createFromRules failed");
        return;
    }
    expect(*t, "\\u0043at in the hat, bat on the mat",
           "C.A.t IN tHE H.A.t, .B..A.t ON tHE M.A.t");
    UnicodeString r;
    t->toRules(r, TRUE);
    if (r == rule) {
        logln((UnicodeString)"OK: toRules() => " + r);
    } else {
        errln((UnicodeString)"FAIL: toRules() => " + r +
              ", expected " + rule);
    }
    delete t;

    // Now test toRules
    t = Transliterator::createInstance("Greek-Latin; Latin-Cyrillic", UTRANS_FORWARD, parseError, status);
    if (t == 0) {
        errln("FAIL: createInstance failed");
        return;
    }
    UnicodeString exp("::Greek-Latin;\n::Latin-Cyrillic;");
    t->toRules(r, TRUE);
    if (r != exp) {
        errln((UnicodeString)"FAIL: toRules() => " + r +
              ", expected " + exp);
    } else {
        logln((UnicodeString)"OK: toRules() => " + r);
    }
    delete t;

    // Round trip the result of toRules
    t = Transliterator::createFromRules("Test", r, UTRANS_FORWARD, parseError, status);
    if (t == 0) {
        errln("FAIL: createFromRules #2 failed");
        return;
    } else {
        logln((UnicodeString)"OK: createFromRules(" + r + ") succeeded");
    }

    // Test toRules again
    t->toRules(r, TRUE);
    if (r != exp) {
        errln((UnicodeString)"FAIL: toRules() => " + r +
              ", expected " + exp);
    } else {
        logln((UnicodeString)"OK: toRules() => " + r);
    }

    delete t;

    // Test Foo(Bar) IDs.  Careful with spacing in id; make it conform
    // to what the regenerated ID will look like.
    UnicodeString id("Upper(Lower);(NFKC)", "");
    t = Transliterator::createInstance(id, UTRANS_FORWARD, parseError, status);
    if (t == 0) {
        errln("FAIL: createInstance #2 failed");
        return;
    }
    if (t->getID() == id) {
        logln((UnicodeString)"OK: created " + id);
    } else {
        errln((UnicodeString)"FAIL: createInstance(" + id +
              ").getID() => " + t->getID());
    }

    Transliterator *u = t->createInverse(status);
    if (u == 0) {
        errln("FAIL: createInverse failed");
        delete t;
        return;
    }
    exp = "NFKC();Lower(Upper)";
    if (u->getID() == exp) {
        logln((UnicodeString)"OK: createInverse(" + id + ") => " +
              u->getID());
    } else {
        errln((UnicodeString)"FAIL: createInverse(" + id + ") => " +
              u->getID());
    }
    delete t;
    delete u;
}

/**
 * Compound filter semantics were orginially not implemented
 * correctly.  Originally, each component filter f(i) is replaced by
 * f'(i) = f(i) && g, where g is the filter for the compound
 * transliterator.
 * 
 * From Mark:
 *
 * Suppose and I have a transliterator X. Internally X is
 * "Greek-Latin; Latin-Cyrillic; Any-Lower". I use a filter [^A].
 * 
 * The compound should convert all greek characters (through latin) to
 * cyrillic, then lowercase the result. The filter should say "don't
 * touch 'A' in the original". But because an intermediate result
 * happens to go through "A", the Greek Alpha gets hung up.
 */
void TransliteratorTest::TestCompoundFilter(void) {
    UParseError parseError;
    UErrorCode status = U_ZERO_ERROR;
    Transliterator *t = Transliterator::createInstance
        ("Greek-Latin; Latin-Cyrillic; Lower", UTRANS_FORWARD, parseError, status);
    if (t == 0) {
        errln("FAIL: createInstance failed");
        return;
    }
    t->adoptFilter(new UnicodeSet("[^A]", status));
    if (U_FAILURE(status)) {
        errln("FAIL: UnicodeSet ct failed");
        delete t;
        return;
    }
    
    // Only the 'A' at index 1 should remain unchanged
    expect(*t,
           CharsToUnicodeString("CA\\u039A\\u0391"),
           CharsToUnicodeString("\\u043AA\\u043A\\u0430"));
    delete t;
}

void TransliteratorTest::TestRemove(void) {
    UParseError parseError;
    UErrorCode status = U_ZERO_ERROR;
    Transliterator *t = Transliterator::createInstance("Remove[abc]", UTRANS_FORWARD, parseError, status);
    if (t == 0) {
        errln("FAIL: createInstance failed");
        return;
    }
    
    expect(*t, "Able bodied baker's cats", "Ale odied ker's ts");
    delete t;
}

void TransliteratorTest::TestToRules(void) {
    const char* RBT = "rbt";
    const char* SET = "set";
    static const char* DATA[] = {
        RBT,
        "$a=\\u4E61; [$a] > A;",
        "[\\u4E61] > A;",

        RBT,
        "$white=[[:Zs:][:Zl:]]; $white{a} > A;",
        "[[:Zs:][:Zl:]]{a} > A;",

        SET,
        "[[:Zs:][:Zl:]]",
        "[[:Zs:][:Zl:]]",

        SET,
        "[:Ps:]",
        "[:Ps:]",

        SET,
        "[:L:]",
        "[:L:]",

        SET,
        "[[:L:]-[A]]",
        "[[:L:]-[A]]",

        SET,
        "[~[:Lu:][:Ll:]]",
        "[~[:Lu:][:Ll:]]",

        SET,
        "[~[a-z]]",
        "[~[a-z]]",

        RBT,
        "$white=[:Zs:]; $black=[^$white]; $black{a} > A;",
        "[^[:Zs:]]{a} > A;",

        RBT,
        "$a=[:Zs:]; $b=[[a-z]-$a]; $b{a} > A;",
        "[[a-z]-[:Zs:]]{a} > A;",

        RBT,
        "$a=[:Zs:]; $b=[$a&[a-z]]; $b{a} > A;",
        "[[:Zs:]&[a-z]]{a} > A;",

        RBT,
        "$a=[:Zs:]; $b=[x$a]; $b{a} > A;",
        "[x[:Zs:]]{a} > A;",
    };
    static const int32_t DATA_length = (int32_t)(sizeof(DATA) / sizeof(DATA[0]));

    for (int32_t d=0; d < DATA_length; d+=3) {
        if (DATA[d] == RBT) {
            // Transliterator test
            UParseError parseError;
            UErrorCode status = U_ZERO_ERROR;
            Transliterator *t = Transliterator::createFromRules("ID",
                                                                DATA[d+1], UTRANS_FORWARD, parseError, status);
            if (t == 0) {
                errln("FAIL: createFromRules failed");
                return;
            }
            UnicodeString rules, escapedRules;
            t->toRules(rules, FALSE);
            t->toRules(escapedRules, TRUE);
            UnicodeString expRules = CharsToUnicodeString(DATA[d+2]);
            UnicodeString expEscapedRules(DATA[d+2]);
            if (rules == expRules) {
                logln((UnicodeString)"Ok: " + DATA[d+1] +
                      " => " + rules);
            } else {
                errln((UnicodeString)"FAIL: " + DATA[d+1] +
                      " => " + rules + ", exp " + expRules);
            }
            if (escapedRules == expEscapedRules) {
                logln((UnicodeString)"Ok: " + DATA[d+1] +
                      " => " + escapedRules);
            } else {
                errln((UnicodeString)"FAIL: " + DATA[d+1] +
                      " => " + escapedRules + ", exp " + expEscapedRules);
            }
            delete t;
            
        } else {
            // UnicodeSet test
            UErrorCode status = U_ZERO_ERROR;
            UnicodeString pat(DATA[d+1]);
            UnicodeString expToPat(DATA[d+2]);
            UnicodeSet set(pat, status);
            if (U_FAILURE(status)) {
                errln("FAIL: UnicodeSet ct failed");
                return;
            }
            // Adjust spacing etc. as necessary.
            UnicodeString toPat;
            set.toPattern(toPat);
            if (expToPat == toPat) {
                logln((UnicodeString)"Ok: " + pat +
                      " => " + toPat);
            } else {
                errln((UnicodeString)"FAIL: " + pat +
                      " => " + prettify(toPat, TRUE) +
                      ", exp " + prettify(pat, TRUE));
            }
        }
    }
}

void TransliteratorTest::TestContext() {
    UTransPosition pos = {0, 2, 0, 1}; // cs cl s l
    expect("de > x; {d}e > y;",
           "de",
           "ye",
           &pos);

    expect("ab{c} > z;",
           "xadabdabcy",
           "xadabdabzy");
}

void TransliteratorTest::TestSupplemental() { 

    expect(CharsToUnicodeString("$a=\\U00010300; $s=[\\U00010300-\\U00010323];"
                                "a > $a; $s > i;"),
           CharsToUnicodeString("ab\\U0001030Fx"),
           CharsToUnicodeString("\\U00010300bix"));

    expect(CharsToUnicodeString("$a=[a-z\\U00010300-\\U00010323];"
                                "$b=[A-Z\\U00010400-\\U0001044D];"
                                "($a)($b) > $2 $1;"),
           CharsToUnicodeString("aB\\U00010300\\U00010400c\\U00010401\\U00010301D"),
           CharsToUnicodeString("Ba\\U00010400\\U00010300\\U00010401cD\\U00010301"));

    // k|ax\\U00010300xm

    // k|a\\U00010400\\U00010300xm
    // ky|\\U00010400\\U00010300xm
    // ky\\U00010400|\\U00010300xm

    // ky\\U00010400|\\U00010300\\U00010400m
    // ky\\U00010400y|\\U00010400m
    expect(CharsToUnicodeString("$a=[a\\U00010300-\\U00010323];"
                                "$a {x} > | @ \\U00010400;"
                                "{$a} [^\\u0000-\\uFFFF] > y;"),
           CharsToUnicodeString("kax\\U00010300xm"),
           CharsToUnicodeString("ky\\U00010400y\\U00010400m"));
}

void TransliteratorTest::TestQuantifier() { 

    // Make sure @ in a quantified anteContext works
    expect("a+ {b} > | @@ c; A > a; (a+ c) > '(' $1 ')';",
           "AAAAAb",
           "aaa(aac)");

    // Make sure @ in a quantified postContext works
    expect("{b} a+ > c @@ |; (a+) > '(' $1 ')';",
           "baaaaa",
           "caa(aaa)");

    // Make sure @ in a quantified postContext with seg ref works
    expect("{(b)} a+ > $1 @@ |; (a+) > '(' $1 ')';",
           "baaaaa",
           "baa(aaa)");

    // Make sure @ past ante context doesn't enter ante context
    UTransPosition pos = {0, 5, 3, 5};
    expect("a+ {b} > | @@ c; x > y; (a+ c) > '(' $1 ')';",
           "xxxab",
           "xxx(ac)",
           &pos);

    // Make sure @ past post context doesn't pass limit
    UTransPosition pos2 = {0, 4, 0, 2};
    expect("{b} a+ > c @@ |; x > y; a > A;",
           "baxx",
           "caxx",
           &pos2);

    // Make sure @ past post context doesn't enter post context
    expect("{b} a+ > c @@ |; x > y; a > A;",
           "baxx",
           "cayy");

    expect("(ab)? c > d;",
           "c abc ababc",
           "d d abd");
    
    expect("(ab)+ {x} > '(' $1 ')';",
           "x abx ababxy",
           "x ab(ab) abab(abab)y");

    expect("b+ > x;",
           "ac abc abbc abbbc",
           "ac axc axc axc");

    expect("[abc]+ > x;",
           "qac abrc abbcs abtbbc",
           "qx xrx xs xtx");

    expect("q{(ab)+} > x;",
           "qa qab qaba qababc qaba",
           "qa qx qxa qxc qxa");

    expect("q(ab)* > x;",
           "qa qab qaba qababc",
           "xa x xa xc");

    // Oddity -- "(foo)* > $1" causes $1 to match the run of "foo"s
    // In perl, it only matches the first occurrence, so the output
    // is "()a (ab) (ab)a (ab)c".
    expect("q(ab)* > '(' $1 ')';",
           "qa qab qaba qababc",
           "()a (ab) (ab)a (abab)c");

    // 'foo'+ and 'foo'* -- the quantifier should apply to the entire
    // quoted string
    expect("'ab'+ > x;",
           "bb ab ababb",
           "bb x xb");

    // $foo+ and $foo* -- the quantifier should apply to the entire
    // variable reference
    expect("$var = ab; $var+ > x;",
           "bb ab ababb",
           "bb x xb");
}

class TestTrans : public NullTransliterator {
public:
    TestTrans(const UnicodeString& id) {
        setID(id);
    }
};

/**
 * Test Source-Target/Variant.
 */
void TransliteratorTest::TestSTV(void) {
    int32_t ns = Transliterator::countAvailableSources();
    if (ns < 0 || ns > 255) {
        errln((UnicodeString)"FAIL: Bad source count: " + ns);
        return;
    }
    int32_t i;
    for (i=0; i<ns; ++i) {
        UnicodeString source;
        Transliterator::getAvailableSource(i, source);
        logln((UnicodeString)"" + i + ": " + source);
        if (source.length() == 0) {
            errln("FAIL: empty source");
            continue;
        }
        int32_t nt = Transliterator::countAvailableTargets(source);
        if (nt < 0 || nt > 255) {
            errln((UnicodeString)"FAIL: Bad target count: " + nt);
            continue;
        }
        for (int32_t j=0; j<nt; ++j) {
            UnicodeString target;
            Transliterator::getAvailableTarget(j, source, target);
            logln((UnicodeString)" " + j + ": " + target);
            if (target.length() == 0) {
                errln("FAIL: empty target");
                continue;
            }
            int32_t nv = Transliterator::countAvailableVariants(source, target);
            if (nv < 0 || nv > 255) {
                errln((UnicodeString)"FAIL: Bad variant count: " + nv);
                continue;
            }
            for (int32_t k=0; k<nv; ++k) {
                UnicodeString variant;
                Transliterator::getAvailableVariant(k, source, target, variant);
                if (variant.length() == 0) { 
                    logln((UnicodeString)"  " + k + ": <empty>");
                } else {
                    logln((UnicodeString)"  " + k + ": " + variant);
                }
            }
        }
    }

    // Test registration
    const char* IDS[] = { "Fieruwer", "Seoridf-Sweorie", "Oewoir-Oweri/Vsie" };
    for (i=0; i<3; ++i) {
        Transliterator *t = new TestTrans(IDS[i]);
        if (t == 0) {
            errln("FAIL: out of memory");
            return;
        }
        if (t->getID() != IDS[i]) {
            errln((UnicodeString)"FAIL: ID mismatch for " + IDS[i]);
            delete t;
            return;
        }
        Transliterator::registerInstance(t);
        UErrorCode status = U_ZERO_ERROR;
        t = Transliterator::createInstance(IDS[i], UTRANS_FORWARD, status);
        if (t == NULL) {
            errln((UnicodeString)"FAIL: Registration/creation failed for ID " +
                  IDS[i]);
        } else {
            logln((UnicodeString)"Ok: Registration/creation succeeded for ID " +
                  IDS[i]);
            delete t;
        }
        Transliterator::unregister(IDS[i]);
        t = Transliterator::createInstance(IDS[i], UTRANS_FORWARD, status);
        if (t != NULL) {
            errln((UnicodeString)"FAIL: Unregistration failed for ID " +
                  IDS[i]);
            delete t;
        }
    }
}

/**
 * Test inverse of Greek-Latin; Title()
 */
void TransliteratorTest::TestCompoundInverse(void) {
    UParseError parseError;
    UErrorCode status = U_ZERO_ERROR;
    Transliterator *t = Transliterator::createInstance
        ("Greek-Latin; Title()", UTRANS_REVERSE,parseError, status);
    if (t == 0) {
        errln("FAIL: createInstance");
        return;
    }
    UnicodeString exp("(Title);Latin-Greek");
    if (t->getID() == exp) {
        logln("Ok: inverse of \"Greek-Latin; Title()\" is \"" +
              t->getID());
    } else {
        errln("FAIL: inverse of \"Greek-Latin; Title()\" is \"" +
              t->getID() + "\", expected \"" + exp + "\"");
    }
    delete t;
}

/**
 * Test NFD chaining with RBT
 */
void TransliteratorTest::TestNFDChainRBT() {
    UParseError pe;
    UErrorCode ec = U_ZERO_ERROR;
    Transliterator* t = Transliterator::createFromRules(
                               "TEST", "::NFD; aa > Q; a > q;",
                               UTRANS_FORWARD, pe, ec);
    expect(*t, "aa", "Q");
    delete t;

    // TEMPORARY TESTS -- BEING DEBUGGED
//=-    UnicodeString s, s2;
//=-    t = Transliterator::createInstance("Latin-Devanagari", UTRANS_FORWARD, pe, ec);
//=-    s = CharsToUnicodeString("rmk\\u1E63\\u0113t");
//=-    s2 = CharsToUnicodeString("\\u0930\\u094D\\u092E\\u094D\\u0915\\u094D\\u0937\\u0947\\u0924\\u094D");
//=-    expect(*t, s, s2);
//=-    delete t;
//=-
//=-    t = Transliterator::createInstance("Devanagari-Latin", UTRANS_FORWARD, pe, ec);
//=-    expect(*t, s2, s);
//=-    delete t;
//=-
//=-    t = Transliterator::createInstance("Latin-Devanagari;Devanagari-Latin", UTRANS_FORWARD, pe, ec);
//=-    s = CharsToUnicodeString("rmk\\u1E63\\u0113t");
//=-    expect(*t, s, s);
//=-    delete t;

//    const char* source[] = {
//        /*
//        "\\u015Br\\u012Bmad",
//        "bhagavadg\\u012Bt\\u0101",
//        "adhy\\u0101ya",
//        "arjuna",
//        "vi\\u1E63\\u0101da",
//        "y\\u014Dga",
//        "dhr\\u0325tar\\u0101\\u1E63\\u1E6Dra",
//        "uv\\u0101cr\\u0325",
//        */
//        "rmk\\u1E63\\u0113t",
//      //"dharmak\\u1E63\\u0113tr\\u0113",
//        /*
//        "kuruk\\u1E63\\u0113tr\\u0113",
//        "samav\\u0113t\\u0101",
//        "yuyutsava-\\u1E25",
//        "m\\u0101mak\\u0101-\\u1E25",
//     // "p\\u0101\\u1E47\\u1E0Dav\\u0101\\u015Bcaiva",
//        "kimakurvata",
//        "san\\u0304java",
//        */
//
//        0
//    };
//    const char* expected[] = {
//        /*
//        "\\u0936\\u094d\\u0930\\u0940\\u092e\\u0926\\u094d",
//        "\\u092d\\u0917\\u0935\\u0926\\u094d\\u0917\\u0940\\u0924\\u093e",
//        "\\u0905\\u0927\\u094d\\u092f\\u093e\\u092f",
//        "\\u0905\\u0930\\u094d\\u091c\\u0941\\u0928",
//        "\\u0935\\u093f\\u0937\\u093e\\u0926",
//        "\\u092f\\u094b\\u0917",
//        "\\u0927\\u0943\\u0924\\u0930\\u093e\\u0937\\u094d\\u091f\\u094d\\u0930",
//        "\\u0909\\u0935\\u093E\\u091A\\u0943",
//        */
//        "\\u0927",
//        //"\\u0927\\u0930\\u094d\\u092e\\u0915\\u094d\\u0937\\u0947\\u0924\\u094d\\u0930\\u0947",
//        /*
//        "\\u0915\\u0941\\u0930\\u0941\\u0915\\u094d\\u0937\\u0947\\u0924\\u094d\\u0930\\u0947",
//        "\\u0938\\u092e\\u0935\\u0947\\u0924\\u093e",
//        "\\u092f\\u0941\\u092f\\u0941\\u0924\\u094d\\u0938\\u0935\\u0903",
//        "\\u092e\\u093e\\u092e\\u0915\\u093e\\u0903",
//    //  "\\u092a\\u093e\\u0923\\u094d\\u0921\\u0935\\u093e\\u0936\\u094d\\u091a\\u0948\\u0935",
//        "\\u0915\\u093f\\u092e\\u0915\\u0941\\u0930\\u094d\\u0935\\u0924",
//        "\\u0938\\u0902\\u091c\\u0935",
//        */
//        0
//    };
//    UErrorCode status = U_ZERO_ERROR;
//    UParseError parseError;
//    UnicodeString message;
//    Transliterator* latinToDevToLatin=Transliterator::createInstance("Latin-Devanagari;Devanagari-Latin", UTRANS_FORWARD, parseError, status);
//    Transliterator* devToLatinToDev=Transliterator::createInstance("Devanagari-Latin;Latin-Devanagari", UTRANS_FORWARD, parseError, status);
//    if(U_FAILURE(status)){
//        errln("FAIL: construction " +   UnicodeString(" Error: ") + u_errorName(status));
//        errln("PreContext: " + prettify(parseError.preContext) + "PostContext: " + prettify( parseError.postContext) );
//        delete latinToDevToLatin;
//        delete devToLatinToDev;
//        return;
//    }
//    UnicodeString gotResult;
//    for(int i= 0; source[i] != 0; i++){
//        gotResult = source[i];
//        expect(*latinToDevToLatin,CharsToUnicodeString(source[i]),CharsToUnicodeString(source[i]));
//        expect(*devToLatinToDev,CharsToUnicodeString(expected[i]),CharsToUnicodeString(expected[i]));
//    }
//    delete latinToDevToLatin;
//    delete devToLatinToDev;
}

/**
 * Inverse of "Null" should be "Null". (J21)
 */
void TransliteratorTest::TestNullInverse() {
    UParseError pe;
    UErrorCode ec = U_ZERO_ERROR;
    Transliterator *t = Transliterator::createInstance("Null", UTRANS_FORWARD, pe, ec);
    if (t == 0 || U_FAILURE(ec)) {
        errln("FAIL: createInstance");
        return;
    }
    Transliterator *u = t->createInverse(ec);
    if (u == 0 || U_FAILURE(ec)) {
        errln("FAIL: createInverse");
        delete t;
        return;
    }
    if (u->getID() != "Null") {
        errln("FAIL: Inverse of Null should be Null");
    }
    delete t;
    delete u;
}

/**
 * Check ID of inverse of alias. (J22)
 */
void TransliteratorTest::TestAliasInverseID() {
    UnicodeString ID("Latin-Hangul", ""); // This should be any alias ID with an inverse
    UParseError pe;
    UErrorCode ec = U_ZERO_ERROR;
    Transliterator *t = Transliterator::createInstance(ID, UTRANS_FORWARD, pe, ec);
    if (t == 0 || U_FAILURE(ec)) {
        errln("FAIL: createInstance");
        return;
    }
    Transliterator *u = t->createInverse(ec);
    if (u == 0 || U_FAILURE(ec)) {
        errln("FAIL: createInverse");
        delete t;
        return;
    }
    UnicodeString exp = "Hangul-Latin";
    UnicodeString got = u->getID();
    if (got != exp) {
        errln((UnicodeString)"FAIL: Inverse of " + ID + " is " + got +
              ", expected " + exp);
    }
    delete t;
    delete u;
}

/**
 * Test IDs of inverses of compound transliterators. (J20)
 */
void TransliteratorTest::TestCompoundInverseID() {
    UnicodeString ID = "Latin-Jamo;NFC(NFD)";
    UParseError pe;
    UErrorCode ec = U_ZERO_ERROR;
    Transliterator *t = Transliterator::createInstance(ID, UTRANS_FORWARD, pe, ec);
    if (t == 0 || U_FAILURE(ec)) {
        errln("FAIL: createInstance");
        return;
    }
    Transliterator *u = t->createInverse(ec);
    if (u == 0 || U_FAILURE(ec)) {
        errln("FAIL: createInverse");
        delete t;
        return;
    }
    UnicodeString exp = "NFD(NFC);Jamo-Latin";
    UnicodeString got = u->getID();
    if (got != exp) {
        errln((UnicodeString)"FAIL: Inverse of " + ID + " is " + got +
              ", expected " + exp);
    }
    delete t;
    delete u;
}

/**
 * Test undefined variable.
 */
void TransliteratorTest::TestUndefinedVariable() {
    UnicodeString rule = "$initial } a <> \\u1161;";
    UParseError pe;
    UErrorCode ec = U_ZERO_ERROR;
    Transliterator *t = new RuleBasedTransliterator("<ID>", rule, UTRANS_FORWARD, 0, pe, ec);
    delete t;
    if (U_FAILURE(ec)) {
        logln((UnicodeString)"OK: Got exception for " + rule + ", as expected: " +
              u_errorName(ec));
        return;
    }
    errln((UnicodeString)"Fail: bogus rule " + rule + " compiled with error " +
          u_errorName(ec));
}

/**
 * Test empty context.
 */
void TransliteratorTest::TestEmptyContext() {
    expect(" { a } > b;", "xay a ", "xby b ");
}

/**
* Test compound filter ID syntax
*/
void TransliteratorTest::TestCompoundFilterID(void) {
    static const char* DATA[] = {
        // Col. 1 = ID or rule set (latter must start with #)

        // = columns > 1 are null if expect col. 1 to be illegal =

        // Col. 2 = direction, "F..." or "R..."
        // Col. 3 = source string
        // Col. 4 = exp result

        "[abc]; [abc]", NULL, NULL, NULL, // multiple filters
        "Latin-Greek; [abc];", NULL, NULL, NULL, // misplaced filter
        "[b]; Latin-Greek; Upper; ([xyz])", "F", "abc", "a\\u0392c",
        "[b]; (Lower); Latin-Greek; Upper(); ([\\u0392])", "R", "\\u0391\\u0392\\u0393", "\\u0391b\\u0393",
        "#\n::[b]; ::Latin-Greek; ::Upper; ::([xyz]);", "F", "abc", "a\\u0392c",
        "#\n::[b]; ::(Lower); ::Latin-Greek; ::Upper(); ::([\\u0392]);", "R", "\\u0391\\u0392\\u0393", "\\u0391b\\u0393",
        NULL,
    };

    for (int32_t i=0; DATA[i]; i+=4) {
        UnicodeString id = CharsToUnicodeString(DATA[i]);
        UTransDirection direction = (DATA[i+1] != NULL && DATA[i+1][0] == 'R') ?
            UTRANS_REVERSE : UTRANS_FORWARD;
        UnicodeString source;
        UnicodeString exp;
        if (DATA[i+2] != NULL) {
            source = CharsToUnicodeString(DATA[i+2]);
            exp = CharsToUnicodeString(DATA[i+3]);
        }
        UBool expOk = (DATA[i+1] != NULL);
        Transliterator* t = NULL;
        UParseError pe;
        UErrorCode ec = U_ZERO_ERROR;
        if (id.charAt(0) == 0x23/*#*/) {
            t = Transliterator::createFromRules("ID", id, direction, pe, ec);
        } else {
            t = Transliterator::createInstance(id, direction, pe, ec);
        }
        UBool ok = (t != NULL && U_SUCCESS(ec));
        if (ok == expOk) {
            logln((UnicodeString)"Ok: " + id + " => " + (t!=0?t->getID():(UnicodeString)"NULL") + ", " +
                  u_errorName(ec));
            if (source.length() != 0) {
                expect(*t, source, exp);
            }
        } else {
            errln((UnicodeString)"FAIL: " + id + " => " + (t!=0?t->getID():(UnicodeString)"NULL") + ", " +
                  u_errorName(ec));
        }
    }
}

//======================================================================
// icu4c ONLY
// These tests are not mirrored (yet) in icu4j at
// src/com/ibm/test/translit/TransliteratorTest.java
//======================================================================

//======================================================================
// Support methods
//======================================================================
void TransliteratorTest::expect(const UnicodeString& rules,
                                const UnicodeString& source,
                                const UnicodeString& expectedResult,
                                UTransPosition *pos) {
    UErrorCode status = U_ZERO_ERROR;
    Transliterator *t = new RuleBasedTransliterator("<ID>", rules, status);
    if (U_FAILURE(status)) {
        errln("FAIL: Transliterator constructor failed");
    } else {
        expect(*t, source, expectedResult, pos);
    }
    delete t;
}

void TransliteratorTest::expect(const Transliterator& t,
                                const UnicodeString& source,
                                const UnicodeString& expectedResult,
                                const Transliterator& reverseTransliterator) {
    expect(t, source, expectedResult);
    expect(reverseTransliterator, expectedResult, source);
}

void TransliteratorTest::expect(const Transliterator& t,
                                const UnicodeString& source,
                                const UnicodeString& expectedResult,
                                UTransPosition *pos) {
    if (pos == 0) {
        UnicodeString result(source);
        t.transliterate(result);
        expectAux(t.getID() + ":String", source, result, expectedResult);
    }

    UTransPosition index={0, 0, 0, 0};
    if (pos != 0) {
        index = *pos;
    }

    UnicodeString rsource(source);
    if (pos == 0) {
        t.transliterate(rsource);
    } else {
        // Do it all at once -- below we do it incrementally
        t.finishTransliteration(rsource, *pos);
    }
    expectAux(t.getID() + ":Replaceable", source, rsource, expectedResult);

    // Test keyboard (incremental) transliteration -- this result
    // must be the same after we finalize (see below).
    UnicodeString log;
    rsource.remove();
    if (pos != 0) {
        rsource = source;
        formatInput(log, rsource, index);
        log.append(" -> ");
        UErrorCode status = U_ZERO_ERROR;
        t.transliterate(rsource, index, status);
        formatInput(log, rsource, index);
    } else {
        for (int32_t i=0; i<source.length(); ++i) {
            if (i != 0) {
                log.append(" + ");
            }
            log.append(source.charAt(i)).append(" -> ");
            UErrorCode status = U_ZERO_ERROR;
            t.transliterate(rsource, index, source.charAt(i), status);
            formatInput(log, rsource, index);
        }
    }
    
    // As a final step in keyboard transliteration, we must call
    // transliterate to finish off any pending partial matches that
    // were waiting for more input.
    t.finishTransliteration(rsource, index);
    log.append(" => ").append(rsource);

    expectAux(t.getID() + ":Keyboard", log,
              rsource == expectedResult,
              expectedResult);
}

/**
 * @param appendTo result is appended to this param.
 * @param input the string being transliterated
 * @param pos the index struct
 */
UnicodeString& TransliteratorTest::formatInput(UnicodeString &appendTo,
                                               const UnicodeString& input,
                                               const UTransPosition& pos) {
    // Output a string of the form aaa{bbb|ccc|ddd}eee, where
    // the {} indicate the context start and limit, and the ||
    // indicate the start and limit.
    if (0 <= pos.contextStart &&
        pos.contextStart <= pos.start &&
        pos.start <= pos.limit &&
        pos.limit <= pos.contextLimit &&
        pos.contextLimit <= input.length()) {

        UnicodeString a, b, c, d, e;
        input.extractBetween(0, pos.contextStart, a);
        input.extractBetween(pos.contextStart, pos.start, b);
        input.extractBetween(pos.start, pos.limit, c);
        input.extractBetween(pos.limit, pos.contextLimit, d);
        input.extractBetween(pos.contextLimit, input.length(), e);
        appendTo.append(a).append((UChar)123/*{*/).append(b).
            append((UChar)PIPE).append(c).append((UChar)PIPE).append(d).
            append((UChar)125/*}*/).append(e);
    } else {
        appendTo.append((UnicodeString)"INVALID UTransPosition {cs=" +
                        pos.contextStart + ", s=" + pos.start + ", l=" +
                        pos.limit + ", cl=" + pos.contextLimit + "} on " +
                        input);
    }
    return appendTo;
}

void TransliteratorTest::expectAux(const UnicodeString& tag,
                                   const UnicodeString& source,
                                   const UnicodeString& result,
                                   const UnicodeString& expectedResult) {
    expectAux(tag, source + " -> " + result,
              result == expectedResult,
              expectedResult);
}

void TransliteratorTest::expectAux(const UnicodeString& tag,
                                   const UnicodeString& summary, UBool pass,
                                   const UnicodeString& expectedResult) {
    if (pass) {
        logln(UnicodeString("(")+tag+") " + prettify(summary));
    } else {
        errln(UnicodeString("FAIL: (")+tag+") "
              + prettify(summary)
              + ", expected " + prettify(expectedResult));
    }
}
