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
#include "utypes.h"
#include "translit.h"
#include "rbt.h"
#include "unifilt.h"
#include "cpdtrans.h"

#define CASE(id,test) case id:                          \
                          name = #test;                 \
                          if (exec) {                   \
                              logln(#test "---");       \
                              logln((UnicodeString)""); \
                              test();                   \
                          }                             \
                          break;

void
TransliteratorTest::runIndexedTest(int32_t index, bool_t exec,
                                   char* &name, char* par) {
    switch (index) {
        CASE(0,TestInstantiation)
        CASE(1,TestSimpleRules)
        CASE(2,TestRuleBasedInverse)
        CASE(3,TestKeyboard)
        CASE(4,TestKeyboard2)
        CASE(5,TestKeyboard3)
        CASE(6,TestArabic)
        CASE(7,TestCompoundKana)
        CASE(8,TestCompoundHex)
        CASE(9,TestFiltering)
        default: name = ""; break;
    }
}

void TransliteratorTest::TestInstantiation() {
    int32_t n = Transliterator::countAvailableIDs();
    for (int32_t i=0; i<n; ++i) {
        UnicodeString id = Transliterator::getAvailableID(i);
        if (id.length() < 1) {
            errln(UnicodeString("FAIL: getAvailableID(") +
                  i + ") returned empty string");
            continue;
        }
        Transliterator* t = Transliterator::createInstance(id);
        if (t == 0) {
            errln(UnicodeString("FAIL: Couldn't create ") + id);
            // When createInstance fails, it deletes the failing
            // entry from the available ID list.  We detect this
            // here by looking for a change in countAvailableIDs.
            int nn = Transliterator::countAvailableIDs();
            if (nn == (n - 1)) {
                n = nn;
                --i; // Compensate for deleted entry
            }
        } else {
            logln(UnicodeString("OK: ") + id);
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

void TransliteratorTest::TestSimpleRules() {
    /* Example: rules 1. ab>x|y
     *                2. yc>z
     *
     * []|eabcd  start - no match, copy e to tranlated buffer
     * [e]|abcd  match rule 1 - copy output & adjust cursor
     * [ex|y]cd  match rule 2 - copy output & adjust cursor
     * [exz]|d   no match, copy d to transliterated buffer
     * [exzd]|   done
     */
    expect(UnicodeString("ab>x|y\n") +
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
    expect(UnicodeString("ab>x|yzacw\n") +
           "za>q\n" +
           "qc>r\n" +
           "cw>n",
           "ab", "xyqn");

    /* Test categories
     */
    UErrorCode status = U_ZERO_ERROR;
    RuleBasedTransliterator t(
        "<ID>",
        UnicodeString("dummy=").append((UChar)0xE100) + "\n" +
        "vowel=[aeiouAEIOU]\n" +
        "lu=[:Lu:]\n" +
        "{vowel}[{lu}>!\n" +
        "{vowel}>&\n" +
        "!]{lu}>^\n" +
        "{lu}>*\n" +
        "a>ERROR",
        status);
    if (U_FAILURE(status)) {
        errln("FAIL: RBT constructor failed");
        return;
    }
    expect(t, "abcdefgABCDEFGU", "&bcd&fg!^**!^*&");
}

/**
 * Create some inverses and confirm that they work.  We have to be
 * careful how we do this, since the inverses will not be true
 * inverses -- we can't throw any random string at the composition
 * of the transliterators and expect the identity function.  F x
 * F' != I.  However, if we are careful about the input, we will
 * get the expected results.
 */
void TransliteratorTest::TestRuleBasedInverse() {
    UnicodeString RULES =
        UnicodeString("abc>zyx\n") +
        "ab>yz\n" +
        "bc>zx\n" +
        "ca>xy\n" +
        "a>x\n" +
        "b>y\n" +
        "c>z\n" +

        "abc<zyx\n" +
        "ab<yz\n" +
        "bc<zx\n" +
        "ca<xy\n" +
        "a<x\n" +
        "b<y\n" +
        "c<z\n" +

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
                                RuleBasedTransliterator::REVERSE, status);
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
void TransliteratorTest::TestKeyboard() {
    UErrorCode status = U_ZERO_ERROR;
    RuleBasedTransliterator t("<ID>", 
                              UnicodeString("psch>Y\n")
                              +"ps>y\n"
                              +"ch>x\n"
                              +"a>A\n",
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
void TransliteratorTest::TestKeyboard2() {
    UErrorCode status = U_ZERO_ERROR;
    RuleBasedTransliterator t("<ID>", 
                              UnicodeString("ych>Y\n")
                              +"ps>|y\n"
                              +"ch>x\n"
                              +"a>A\n",
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
void TransliteratorTest::TestKeyboard3() {
    // We want th>z but t>y.  Furthermore, during keyboard
    // transliteration we want t>y then yh>z if t, then h are
    // typed.
    UnicodeString RULES("t>|y\n"
                        "yh>z\n");

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
    int32_t index[3] = {0, 0, 0};
    UnicodeString s;
    for (int32_t i=0; i<DATA_length; i+=2) {
        UnicodeString log;
        if (DATA[i] != 0) {
            log = s + " + "
                + DATA[i]
                + " -> ";
            t.keyboardTransliterate(s, index, DATA[i], status);
        } else {
            log = s + " => ";
            t.finishKeyboardTransliteration(s, index);
        }
        // Show the start index '{' and the cursor '|'
        UnicodeString a, b, c;
        s.extractBetween(0, index[Transliterator::START], a);
        s.extractBetween(index[Transliterator::START],
                           index[Transliterator::CURSOR], b);
        s.extractBetween(index[Transliterator::CURSOR],
                           s.length(), c);
        log.append(a).
            append('{').
            append(b).
            append('|').
            append(c);
        if (s == DATA[i+1] && U_SUCCESS(status)) {
            logln(log);
        } else {
            errln(UnicodeString("FAIL: ") + log + ", expected " + DATA[i+1]);
        }
    }
}

void TransliteratorTest::TestArabic() {
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
void TransliteratorTest::TestCompoundKana() {
    Transliterator* kana = Transliterator::createInstance("Latin-Kana");
    Transliterator* rkana = Transliterator::createInstance("Kana-Latin");
    Transliterator* trans[] = { kana, rkana };
    if (kana == 0 || rkana == 0) {
        errln("FAIL: construction failed");
        delete kana;
        delete rkana;
        return;
    }
    Transliterator* t = new CompoundTransliterator("<ID>", trans, 2);

    expect(*t, "aaaaa", "aaaaa");
    delete t;
    delete kana;
    delete rkana;
}

/**
 * Compose the hex transliterators forward and reverse.
 */
void TransliteratorTest::TestCompoundHex() {
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
    expect(*b, "\\u0030\\u0031", "01");

    Transliterator* ab = new CompoundTransliterator("ab", transab, 2);
    UnicodeString s("abcde");
    expect(*ab, s, s);

    UnicodeString str(s);
    a->transliterate(str);
    Transliterator* ba = new CompoundTransliterator("ba", transba, 2);
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
    virtual bool_t isIn(UChar c) const {
        return c != (UChar)'c';
    }
};

/**
 * Do some basic tests of filtering.
 */
void TransliteratorTest::TestFiltering() {
    Transliterator* hex = Transliterator::createInstance("Unicode-Hex");
    if (hex == 0) {
        errln("FAIL: createInstance(Unicode-Hex) failed");
        return;
    }
    hex->adoptFilter(new TestFilter());
    UnicodeString s("abcde");
    UnicodeString out;
    hex->transliterate(s, out);
    UnicodeString exp("\\u0061\\u0062c\\u0064\\u0065");
    if (out == exp) {
        logln(UnicodeString("Ok:   \"") + exp + "\"");
    } else {
        logln(UnicodeString("FAIL: \"") + out + "\", wanted \"" + exp + "\"");
    }
    delete hex;
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
    UnicodeString result;
    t.transliterate(source, result);
    expectAux(t.getID() + ":String", source, result, expectedResult);

    UnicodeString rsource(source);
    t.transliterate(rsource);
    expectAux(t.getID() + ":Replaceable", source, rsource, expectedResult);

    // Test keyboard (incremental) transliteration -- this result
    // must be the same after we finalize (see below).
    rsource.remove();
    int32_t index[3] = { 0, 0, 0 };
    UnicodeString log;

    for (int32_t i=0; i<source.length(); ++i) {
        if (i != 0) {
            log.append(" + ");
        }
        log.append(source.charAt(i)).append(" -> ");
        UErrorCode status = U_ZERO_ERROR;
        t.keyboardTransliterate(rsource, index, source.charAt(i), status);
        // Append the string buffer with a vertical bar '|' where
        // the committed index is.
        UnicodeString left, right;
        rsource.extractBetween(0, index[Transliterator::CURSOR], left);
        rsource.extractBetween(index[Transliterator::CURSOR], rsource.length(), right);
        log.append(left).append((UChar)'|').append(right);
    }
    
    // As a final step in keyboard transliteration, we must call
    // transliterate to finish off any pending partial matches that
    // were waiting for more input.
    t.finishKeyboardTransliteration(rsource, index);
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
                                   const UnicodeString& summary, bool_t pass,
                                   const UnicodeString& expectedResult) {
    if (pass) {
        logln(UnicodeString("(")+tag+") " + escape(summary));
    } else {
        errln(UnicodeString("FAIL: (")+tag+") "
              + escape(summary)
              + ", expected " + escape(expectedResult));
    }
}

static UChar toHexString(int32_t i) { return i + (i < 10 ? '0' : ('A' - 10)); }

UnicodeString
TransliteratorTest::escape(const UnicodeString& s) {
    UnicodeString buf;
    for (int32_t i=0; i<s.size(); ++i)
    {
        UChar c = s[(UTextOffset)i];
        if (' ' <= c && c <= (UChar)0x7F) {
            buf += c;
        } else {
            buf += '\\'; buf += 'u';
            buf += toHexString((c & 0xF000) >> 12);
            buf += toHexString((c & 0x0F00) >> 8);
            buf += toHexString((c & 0x00F0) >> 4);
            buf += toHexString(c & 0x000F);
        }
    }
    return buf;
}
