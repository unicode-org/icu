// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#ifndef _TESTMESSAGEFORMAT2
#define _TESTMESSAGEFORMAT2

#include "unicode/rep.h"
#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/messageformat2_function_registry.h"
#include "unicode/unistr.h"
#include "unicode/fmtable.h"
#include "unicode/parseerr.h"
#include "intltest.h"

/**
 * TestMessageFormat2 tests MessageFormat2
 */

using namespace icu::message2;

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
    void testDataModelErrors(void);
    void testResolutionErrors(void);
    // Test the data model API
    void testAPI(void);
    void testAPISimple(void);
    void testAPICustomFunctions(void);
    // Test custom functions
    void testCustomFunctions(void);

private:
    void testMessageFormatter(const UnicodeString&, UParseError&, UErrorCode&);
    void testMessageFormatting(const UnicodeString&, UParseError&, UErrorCode&);
    void testPattern(const UnicodeString&, uint32_t, const char*);
    template<size_t N>
    void testPatterns(const UnicodeString(&) [N], const char*);
    void testSemanticallyInvalidPattern(uint32_t, const UnicodeString&, UErrorCode);
    void testRuntimeErrorPattern(uint32_t, const UnicodeString&, UErrorCode);
    void testInvalidPattern(uint32_t, const UnicodeString&);
    void testInvalidPattern(uint32_t, const UnicodeString&, uint32_t);
    void testInvalidPattern(uint32_t, const UnicodeString&, uint32_t, uint32_t);

    // Custom function testing
    void checkResult(const UnicodeString&, const UnicodeString&, const UnicodeString&, const UnicodeString&, IcuTestErrorCode&, UErrorCode);
    void testWithPatternAndArguments(const UnicodeString&, FunctionRegistry*, const UnicodeString&, const UnicodeString&, const UnicodeString&, const UnicodeString&, IcuTestErrorCode&);
    void testWithPatternAndArguments(const UnicodeString&, FunctionRegistry*, const UnicodeString&, const UnicodeString&, const UnicodeString&, const UnicodeString&, IcuTestErrorCode&, UErrorCode);
    void testPersonFormatter(IcuTestErrorCode&);
}; // class TestMessageFormat2

// Custom function classes
class PersonNameFormatterFactory : public FormatterFactory {
    
    public:
    Formatter* createFormatter(Locale, const Hashtable&, UErrorCode&) const;
};

class PersonNameFormatter : public Formatter {
    public:
    void format(const UnicodeString&, const Hashtable&, UnicodeString&, UErrorCode& errorCode) const;
};

// Custom function test utilities
class SplitString {
    public:
    static const uint32_t FIRST = 0;
    static const uint32_t LAST = -1;
    static bool nextPart(const UnicodeString&, UnicodeString&, uint32_t&);
};

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif
