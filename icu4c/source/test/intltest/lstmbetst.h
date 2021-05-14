// © 2021 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#ifndef LSTMBETEST_H
#define LSTMBETEST_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_BREAK_ITERATION

#include <memory>

#include "intltest.h"

#include "unicode/uscript.h"

struct TestParams;

U_NAMESPACE_BEGIN
class LanguageBreakEngine;
U_NAMESPACE_END


/**
 * Test the LSTMBreakEngine class giving different rules
 */
class LSTMBETest: public IntlTest {
public:

    LSTMBETest();
    virtual ~LSTMBETest();

    void runIndexedTest( int32_t index, UBool exec, const char* &name, char* par = NULL );

    void TestThaiGraphclust();
    void TestThaiCodepoints();
    void TestBurmeseGraphclust();
    void runTestFromFile(const char* filename);

private:
    const LanguageBreakEngine* createEngineFromTestData(const char* model, UScriptCode script, UErrorCode& status);

    // Test parameters, from the test framework and test invocation.
    const char* fTestParams;
};

#endif /* #if !UCONFIG_NO_BREAK_ITERATION */

#endif
