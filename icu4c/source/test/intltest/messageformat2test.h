// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#ifndef _TESTMESSAGEFORMAT2
#define _TESTMESSAGEFORMAT2

#include "unicode/rep.h"
#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/unistr.h"
#include "unicode/fmtable.h"
#include "unicode/parseerr.h"
#include "intltest.h"

/**
 * TestMessageFormat2 tests MessageFormat2
 */
class TestMessageFormat2: public IntlTest {
public:
    void runIndexedTest( int32_t index, UBool exec, const char* &name, char* par = NULL ) override;

    /** 
     * test MessageFormat2 with various given patterns
     **/
    void testStaticFormat2(void);
    void testComplexMessage(void);
    void testValidPatterns(void);
    void testValidJsonPatterns(void);
    void testInvalidPatterns(void);
    // Test the data model API
    void testAPI(void);
    void testAPISimple(void);

private:
    void testMessageFormatter(const UnicodeString&, UParseError&, UErrorCode&);
    void testPattern(const UnicodeString&, uint32_t, const char*);
    template<size_t N>
    void testPatterns(const UnicodeString(&) [N], const char*);
    void testInvalidPattern(uint32_t, const UnicodeString&);
    void testInvalidPattern(uint32_t, const UnicodeString&, uint32_t);
    void testInvalidPattern(uint32_t, const UnicodeString&, uint32_t, uint32_t);
};

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif
