/*
**********************************************************************
*   Copyright (C) 1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/10/99    aliu        Creation.
**********************************************************************
*/
#include "transtst.h"
#include "unicode/utypes.h"
#include "unicode/translit.h"
#include "unicode/rbt.h"
#include "unicode/unifilt.h"
#include "unicode/cpdtrans.h"
#include "unicode/dtfmtsym.h"
#include "unicode/hextouni.h"
#include "unicode/unitohex.h"
#include "unicode/ucnv.h"
#include "unicode/ucnv_err.h"

// Define character constants thusly to be EBCDIC-friendly
enum {
    LEFT_BRACE=((UChar)0x007B), /*{*/
    PIPE      =((UChar)0x007C), /*|*/
    ZERO      =((UChar)0x0030), /*0*/
    UPPER_A   =((UChar)0x0041)  /*A*/
};

#define CASE(id,test) case id:                          \
                          name = #test;                 \
                          if (exec) {                   \
                              logln(#test "---");       \
                              logln((UnicodeString)""); \
                              test();                   \
                          }                             \
                          break

void
TransliteratorTest::runIndexedTest(int32_t index, UBool exec,
                                   char* &name, char* par) {
    switch (index) {
        CASE(0,TestInstantiation);
        CASE(1,TestSimpleRules);
        CASE(2,TestRuleBasedInverse);
        CASE(3,TestKeyboard);
        CASE(4,TestKeyboard2);
        CASE(5,TestKeyboard3);
        CASE(6,TestArabic);
        CASE(7,TestCompoundKana);
        CASE(8,TestCompoundHex);
        CASE(9,TestFiltering);
        CASE(10,TestInlineSet);
        CASE(11,TestPatternQuoting);
        CASE(12,TestJ277);
        CASE(13,TestJ243);
        CASE(14,TestJ329);
        CASE(15,TestSegments);
        CASE(16,TestCursorOffset);
        CASE(17,TestArbitraryVariableValues);
        CASE(18,TestPositionHandling);
        CASE(19,TestHiraganaKatakana);
        default: name = ""; break;
    }
}

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
        Transliterator* t = Transliterator::createInstance(id,
                              UTRANS_FORWARD, &parseError);
        name.truncate(0);
        Transliterator::getDisplayName(id, name);
        if (t == 0) {
            errln(UnicodeString("FAIL: Couldn't create ") + id +
                  ", parse error " + parseError.code +
                  ", line " + parseError.line +
                  ", offset " + parseError.offset +
                  ", context " + prettify(parseError.preContext));
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
        }
        delete t;
    }

    // Now test the failure path
    UnicodeString id("<Not a valid Transliterator ID>");
    Transliterator* t = Transliterator::createInstance(id);
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
           "eabcd", "exzd");        /* Another set of rules:
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

    int32_t DATA_length = sizeof(DATA) / sizeof(DATA[0]);

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

    keyboardAux(t, DATA, sizeof(DATA)/sizeof(DATA[0]));
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

    keyboardAux(t, DATA, sizeof(DATA)/sizeof(DATA[0]));
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
    keyboardAux(t, DATA, sizeof(DATA)/sizeof(DATA[0]));
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

    Transliterator *t = Transliterator::createInstance("Latin-Arabic");
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
    Transliterator* t = Transliterator::createInstance("Latin-Kana;Kana-Latin");
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
    Transliterator* a = Transliterator::createInstance("Unicode-Hex");
    Transliterator* b = Transliterator::createInstance("Hex-Unicode");
    Transliterator* transab[] = { a, b };
    Transliterator* transba[] = { b, a };
    if (a == 0 || b == 0) {
        errln("FAIL: construction failed");
        delete a;
        delete b;
        return;
    }

    // Do some basic tests of b
    expect(*b, UnicodeString("\\u0030\\u0031", ""), "01");

    Transliterator* ab = new CompoundTransliterator(transab, 2);
    UnicodeString s("abcde");
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
    virtual UnicodeFilter* clone() const {
        return new TestFilter(*this);
    }
    virtual UBool contains(UChar c) const {
        return c != (UChar)0x0063 /*c*/;
    }
};

/**
 * Do some basic tests of filtering.
 */
void TransliteratorTest::TestFiltering(void) {
    Transliterator* hex = Transliterator::createInstance("Unicode-Hex");
    if (hex == 0) {
        errln("FAIL: createInstance(Unicode-Hex) failed");
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
    Transliterator *gl = Transliterator::createInstance("Greek-Latin");
    if (gl == NULL) {
        errln("FAIL: createInstance(Greek-Latin) returned NULL");
        return;
    }

    UChar sigma = 0x3C3;
    UChar upsilon = 0x3C5;
    UChar nu = 0x3BD;
    UChar PHI = 0x3A6;
    UChar alpha = 0x3B1;
    UChar omega = 0x3C9;
    UChar omicron = 0x3BF;
    UChar epsilon = 0x3B5;

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
    // Test default Hex-Unicode, which should handle
    // \u, \U, u+, and U+
    HexToUnicodeTransliterator hex;
    expect(hex, UnicodeString("\\u0041+\\U0042,u+0043uu+0044z", ""), "A+B,CuDz");
    // Try a custom Hex-Unicode
    // \uXXXX and &#xXXXX;
    status = U_ZERO_ERROR;
    HexToUnicodeTransliterator hex2(UnicodeString("\\\\u###0;&\\#x###0\\;", ""), status); 
    expect(hex2, UnicodeString("\\u61\\u062\\u0063\\u00645\\u66x&#x30;&#x031;&#x0032;&#x00033;", ""),
           "abcd5fx012&#x00033;");
    // Try custom Unicode-Hex (default is tested elsewhere)
    status = U_ZERO_ERROR;
    UnicodeToHexTransliterator hex3("&\\#x###0;", status);
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
    int32_t DATA_length = sizeof(DATA) / sizeof(DATA[0]);

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
            desc = desc + ", ParseError code=" + parseError.code +
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
    };
    int32_t DATA_length = sizeof(DATA)/sizeof(*DATA);

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
    int32_t DATA_length = sizeof(DATA)/sizeof(*DATA);

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
    int32_t DATA_length = sizeof(DATA)/sizeof(*DATA);

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

    int32_t n = sizeof(DATA) / sizeof(DATA[0]) / 3;
    for (int32_t i=0; i<n; i++) {
        UErrorCode status = U_ZERO_ERROR;
        Transliterator *t = new RuleBasedTransliterator("<ID>",
                                                        DATA[3*i], status);
        if (U_FAILURE(status)) {
            delete t;
            errln("FAIL: RBT constructor");
            return;
        }
        UTransPosition pos = {POS[4*i], POS[4*i+1], POS[4*i+2], POS[4*i+3]};
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
    Transliterator* hk = Transliterator::createInstance("Hiragana-Katakana");
    Transliterator* kh = Transliterator::createInstance("Katakana-Hiragana");
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
    int32_t DATA_length = sizeof(DATA) / sizeof(DATA[0]);

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

}

//======================================================================
// Support methods
//======================================================================
void TransliteratorTest::expect(const UnicodeString& rules,
                                const UnicodeString& source,
                                const UnicodeString& expectedResult) {
    UErrorCode status = U_ZERO_ERROR;
    Transliterator *t = new RuleBasedTransliterator("<ID>", rules, status);
    if (U_FAILURE(status)) {
        errln("FAIL: Transliterator constructor failed");
    } else {
        expect(*t, source, expectedResult);
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
                                const UnicodeString& expectedResult) {
    UnicodeString result(source);
    t.transliterate(result);
    expectAux(t.getID() + ":String", source, result, expectedResult);

    UnicodeString rsource(source);
    t.transliterate(rsource);
    expectAux(t.getID() + ":Replaceable", source, rsource, expectedResult);

    // Test keyboard (incremental) transliteration -- this result
    // must be the same after we finalize (see below).
    rsource.remove();
    UTransPosition index={0, 0, 0, 0};
    UnicodeString log;

    for (int32_t i=0; i<source.length(); ++i) {
        if (i != 0) {
            log.append(" + ");
        }
        log.append(source.charAt(i)).append(" -> ");
        UErrorCode status = U_ZERO_ERROR;
        t.transliterate(rsource, index, source.charAt(i), status);
        // Append the string buffer with a vertical bar '|' where
        // the committed index is.
        UnicodeString left, right;
        rsource.extractBetween(0, index.start, left);
        rsource.extractBetween(index.start, rsource.length(), right);
        log.append(left).append((UChar)PIPE).append(right);
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

static UChar toHexString(int32_t i) { return i + (i < 10 ? ZERO : (UPPER_A - 10)); }
