/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/************************************************************************
*   This test program is intended for testing Replaceable class.
*
*   Date        Name        Description
*   11/28/2001  hshih       Ported back from Java.
* 
************************************************************************/

#include "unicode/utypes.h"

#if !UCONFIG_NO_TRANSLITERATION

#include "ittrans.h"
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include "unicode/rep.h"
#include "reptest.h"

//---------------------------------------------
// runIndexedTest
//---------------------------------------------

    /**
     * This is a test class that simulates styled text.
     * It associates a style number (0..65536) with each character,
     * and maintains that style in the normal fashion:
     * When setting text from raw string or characters,<br>
     * Set the styles to the style of the first character replaced.<br>
     * If no characters are replaced, use the style of the previous character.<br>
     * If at start, use the following character<br>
     * Otherwise use defaultStyle.
     */
class TestReplaceable : public Replaceable {
    UnicodeString chars;
    UnicodeString styles;
    
    static const UChar defaultStyle;

    /**
     * The address of this static class variable serves as this class's ID
     * for ICU "poor man's RTTI".
     */
    static const char fgClassID;

public:    
    TestReplaceable (const UnicodeString& text, 
                     const UnicodeString& newStyles) {
        chars = text;
        UnicodeString s;
        for (int i = 0; i < text.length(); ++i) {
            if (i < newStyles.length()) {
                s.append(newStyles.charAt(i));
            } else {
                s.append((UChar)(i + 0x0041));
            }
        }
        this->styles = s;
    }
    
    ~TestReplaceable(void) {}

    UnicodeString getStyles() {
        return styles;
    }
    
    UnicodeString toString() {
        UnicodeString s = chars;
        s.append("{");
        s.append(styles);
        s.append("}");
        return s;
    }

    void extractBetween(int32_t start, int32_t limit, UnicodeString& result) const {
        chars.extractBetween(start, limit, result);
    }

    /**
     * ICU "poor man's RTTI", returns a UClassID for the actual class.
     *
     * @draft ICU 2.2
     */
    virtual inline UClassID getDynamicClassID() const { return getStaticClassID(); }

    /**
     * ICU "poor man's RTTI", returns a UClassID for this class.
     *
     * @draft ICU 2.2
     */
    static inline UClassID getStaticClassID() { return (UClassID)&fgClassID; }

protected:
    virtual int32_t getLength() const {
        return chars.length();
    }

    virtual UChar getCharAt(int32_t offset) const{
        return chars.charAt(offset);
    }

    virtual UChar32 getChar32At(int32_t offset) const{
        return chars.char32At(offset);
    }

    virtual void handleReplaceBetween(int32_t start, int32_t limit, const UnicodeString& text) {
        UnicodeString s;
        this->extractBetween(start, limit, s);
        if (s == text) return; // NO ACTION!
        this->chars.replaceBetween(start, limit, text);
        fixStyles(start, limit, text.length());
    }
    

    void fixStyles(int32_t start, int32_t limit, int32_t newLen) {
        UChar newStyle = defaultStyle;
        if (start != limit) {
            newStyle = styles.charAt(start);
        } else if (start > 0) {
            newStyle = styles.charAt(start-1);
        } else if (limit < styles.length() - 1) {
            newStyle = styles.charAt(limit+1);
        }
        // dumb implementation for now.
        UnicodeString s;
        for (int i = 0; i < newLen; ++i) {
            s.append(newStyle);
        }
        styles.replaceBetween(start, limit, s);
    }

    virtual void copy(int32_t start, int32_t limit, int32_t dest) {
        chars.copy(start, limit, dest);
        styles.copy(start, limit, dest);
    }
};

const char TestReplaceable::fgClassID=0;

const UChar TestReplaceable::defaultStyle  = 0x005F;

void
ReplaceableTest::runIndexedTest(int32_t index, UBool exec,
                                      const char* &name, char* /*par*/) {
    switch (index) {
        TESTCASE(0,TestReplaceableClass);
        default: name = ""; break;
    }
}


void ReplaceableTest::TestReplaceableClass(void) {
    UChar rawTestArray[5][6] = {
        {0x0041, 0x0042, 0x0043, 0x0044, 0x0000, 0x0000}, // ABCD
        {0x0061, 0x0062, 0x0063, 0x0064, 0x00DF, 0x0000}, // abcd\u00DF
        {0x0061, 0x0042, 0x0043, 0x0044, 0x0000, 0x0000}, // aBCD
        {0x0041, 0x0300, 0x0045, 0x0300, 0x0000, 0x0000}, // A\u0300E\u0300
        {0x00C0, 0x00C8, 0x0000, 0x0000, 0x0000, 0x0000}  // \u00C0\u00C8
    };
    Check("Lower", rawTestArray[0], "1234", "");
    Check("Upper", rawTestArray[1], "123455", ""); // must map 00DF to SS
    Check("Title", rawTestArray[2], "1234", "");
    Check("NFC",   rawTestArray[3], "1234", "13");
    Check("NFD",   rawTestArray[4], "12", "1122");
}
    
void ReplaceableTest::Check(const UnicodeString& transliteratorName, 
                            const UnicodeString& test, 
                            const UnicodeString& styles, 
                            const UnicodeString& shouldProduceStyles) 
{
    UErrorCode status = U_ZERO_ERROR;
    TestReplaceable *tr = new TestReplaceable(test, styles);
    UnicodeString expectedStyles = shouldProduceStyles;
    if (expectedStyles.length() == 0) {
        expectedStyles = styles;
    }
    UnicodeString original = tr->toString();
    
    Transliterator* t = Transliterator::createInstance(transliteratorName, UTRANS_FORWARD, status);
    if (U_FAILURE(status)) {
        log("FAIL: failed to create the ");
        log(transliteratorName);
        errln(" transliterator.");
        delete tr;
        return;
    }
    t->transliterate(*tr);
    UnicodeString newStyles = tr->getStyles();
    if (newStyles != expectedStyles) {
        errln("FAIL Styles: " + transliteratorName + "{" + original + "} => "
            + tr->toString() + "; should be {" + expectedStyles + "}!");
    } else {
        log("OK: ");
        log(transliteratorName);
        log("(");
        log(original);
        log(") => ");
        logln(tr->toString());
    }
    delete tr;
    delete t;
}

#endif /* #if !UCONFIG_NO_TRANSLITERATION */
