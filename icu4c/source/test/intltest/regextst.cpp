/********************************************************************
 * COPYRIGHT:
 * Copyright (c) 2002, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

//
//   regex.cpp
//
//      ICU Regular Expressions test, part of intltest.
//

#include "unicode/utypes.h"
#include "intltest.h"
#include "regextst.h"


RegexTest::RegexTest() 
{
};


RegexTest::~RegexTest()
{
};



void RegexTest::runIndexedTest( int32_t index, UBool exec, const char* &name, char* /*par*/ )
{
    if (exec) logln("TestSuite RegexTest: ");
    switch (index) {

        case 0: name = "TestRegexAPI";
            if(exec) TestRegexAPI(); break;

        default: name = ""; break; //needed to end loop
    }
}


//---------------------------------------------------------------------------
//
//    REGEX_TESTLM       Macro + invocation function to simplify writing quick tests
//                       for the LookingAt() and  Match() functions.
//
//       usage:
//          REGEX_TESTLM("pattern",  "input text",  lookingAt expected, matches expected);
//
//          The expected results are UBool - TRUE or FALSE.
//          The input text is unescaped.  The pattern is not.
//            
//
//---------------------------------------------------------------------------
#define REGEX_CHECK_STATUS {if (U_FAILURE(status)) {errln("RegexTest failure at line %d.  status=%d\n", \
__LINE__, status); return;}}

#define REGEX_ASSERT(expr) {if ((expr)==FALSE) {errln("RegexTest failure at line %d.\n", __LINE__);};}

#define REGEX_TESTLM(pat, text, looking, match) doRegexLMTest(pat, text, looking, match, __LINE__);

UBool RegexTest::doRegexLMTest(char *pat, char *text, UBool looking, UBool match, int line) {
    const UnicodeString pattern(pat);
    const UnicodeString inputText(text);
    UErrorCode          status  = U_ZERO_ERROR;
    UParseError         pe;
    RegexPattern        *REPattern = NULL;
    RegexMatcher        *REMatcher = NULL;
    UBool               retVal     = TRUE;

    UnicodeString patString(pat);
    REPattern = RegexPattern::compile(patString, 0, pe, status);
    if (U_FAILURE(status)) {
        errln("RegexTest failure in RegexPattern::compile() at line %d.  Status = %d\n", line, status);
        return FALSE;
    }

    UnicodeString inputString(inputText);
    UnicodeString unEscapedInput = inputString.unescape();
    REMatcher = REPattern->matcher(unEscapedInput, status);
    if (U_FAILURE(status)) {
        errln("RegexTest failure in REPattern::matcher() at line %d.  Status = %d\n", line, status);
        return FALSE;
    }
  
    UBool actualmatch;
    actualmatch = REMatcher->lookingAt(status);
    if (U_FAILURE(status)) {
        errln("RegexTest failure in lookingAt() at line %d.  Status = %d\n", line, status);
        retVal =  FALSE;
    }
    if (actualmatch != looking) {
        errln("RegexTest: wrong return from lookingAt() at line %d.\n", line);
        retVal = FALSE;
    }

    status = U_ZERO_ERROR;
    actualmatch = REMatcher->matches(status);
    if (U_FAILURE(status)) {
        errln("RegexTest failure in matches() at line %d.  Status = %d\n", line, status);
        retVal = FALSE;
    }
    if (actualmatch != match) {
        errln("RegexTest: wrong return from matches() at line %d.\n", line);
        retVal = FALSE;
    }

    if (retVal == FALSE) {
        REPattern->dump();
    }

    delete REPattern;
    delete REMatcher;
    return retVal;
}
    

//---------------------------------------------------------------------------
//
//      TestRegexAPI
//
//---------------------------------------------------------------------------
void RegexTest::TestRegexAPI() {
    UParseError         pe;
    UErrorCode          status=U_ZERO_ERROR;

    RegexPattern        pat1;    // Test default constructor to not crash.

    RegexPattern        *pat2;
    int32_t             flags = 0;

    //
    // Debug - slide failing test cases early
    //
#if 0
    REGEX_TESTLM("b+", "", FALSE, FALSE);
        return;
#endif

    //
    // Simple pattern compilation
    //
    UnicodeString       re("abc");
    pat2 = RegexPattern::compile(re, flags, pe, status);
    REGEX_CHECK_STATUS;

    UnicodeString inStr1 = "abcdef this is a test";
    UnicodeString instr2 = "not abc";
    UnicodeString empty  = "";


    //
    // Matcher creation and reset.
    //
    RegexMatcher *m1 = pat2->matcher(inStr1, status);
    REGEX_CHECK_STATUS;
    REGEX_ASSERT(m1->lookingAt(status) == TRUE); 
    m1->reset(instr2);
    REGEX_ASSERT(m1->lookingAt(status) == FALSE);
    m1->reset(inStr1);
    REGEX_ASSERT(m1->lookingAt(status) == TRUE);
    m1->reset(empty);
    REGEX_ASSERT(m1->lookingAt(status) == FALSE);
    delete m1;
    delete pat2;

    //
    // Pattern with parentheses
    //
    REGEX_TESTLM("st(abc)ring", "stabcring thing", TRUE,  FALSE);
    REGEX_TESTLM("st(abc)ring", "stabcring",       TRUE,  TRUE);
    REGEX_TESTLM("st(abc)ring", "stabcrung",       FALSE, FALSE);

    //
    // Patterns with *
    //
    REGEX_TESTLM("st(abc)*ring", "string", TRUE, TRUE);
    REGEX_TESTLM("st(abc)*ring", "stabcring", TRUE, TRUE);
    REGEX_TESTLM("st(abc)*ring", "stabcabcring", TRUE, TRUE);
    REGEX_TESTLM("st(abc)*ring", "stabcabcdring", FALSE, FALSE);
    REGEX_TESTLM("st(abc)*ring", "stabcabcabcring etc.", TRUE, FALSE);

    REGEX_TESTLM("a*", "",  TRUE, TRUE);
    REGEX_TESTLM("a*", "b", TRUE, FALSE);


    //
    //  Patterns with "."
    //
    REGEX_TESTLM(".", "abc", TRUE, FALSE);
    REGEX_TESTLM("...", "abc", TRUE, TRUE);
    REGEX_TESTLM("....", "abc", FALSE, FALSE);
    REGEX_TESTLM(".*", "abcxyz123", TRUE, TRUE);
    REGEX_TESTLM("ab.*xyz", "abcdefghij", FALSE, FALSE);
    REGEX_TESTLM("ab.*xyz", "abcdefg...wxyz", TRUE, TRUE);
    REGEX_TESTLM("ab.*xyz", "abcde...wxyz...abc..xyz", TRUE, TRUE);
    REGEX_TESTLM("ab.*xyz", "abcde...wxyz...abc..xyz...", TRUE, FALSE);

    //
    //  Patterns with * applied to chars at end of literal string
    //
    REGEX_TESTLM("abc*", "ab", TRUE, TRUE);
    REGEX_TESTLM("abc*", "abccccc", TRUE, TRUE);

    //
    //  Supplemental chars match as single chars, not a pair of surrogates.
    //
    REGEX_TESTLM(".", "\\U00011000", TRUE, TRUE);
    REGEX_TESTLM("...", "\\U00011000x\\U00012002", TRUE, TRUE);
    REGEX_TESTLM("...", "\\U00011000x\\U00012002y", TRUE, FALSE);


    //
    //  UnicodeSets in the pattern
    //
    REGEX_TESTLM("[1-6]", "1", TRUE, TRUE);
    REGEX_TESTLM("[1-6]", "3", TRUE, TRUE);
    REGEX_TESTLM("[1-6]", "7", FALSE, FALSE);
    REGEX_TESTLM("a[1-6]", "a3", TRUE, TRUE);
    REGEX_TESTLM("a[1-6]", "a3", TRUE, TRUE);
    REGEX_TESTLM("a[1-6]b", "a3b", TRUE, TRUE);

    REGEX_TESTLM("a[0-9]*b", "a123b", TRUE, TRUE);
    REGEX_TESTLM("a[0-9]*b", "abc", TRUE, FALSE);
    REGEX_TESTLM("[\\p{Nd}]*", "123456", TRUE, TRUE);
    REGEX_TESTLM("[\\p{Nd}]*", "a123456", TRUE, FALSE);   // note that * matches 0 occurences.
    REGEX_TESTLM("[a][b][[:Zs:]]*", "ab   ", TRUE, TRUE);

    //
    //   OR operator in patterns
    //
    REGEX_TESTLM("(a|b)", "a", TRUE, TRUE);
    REGEX_TESTLM("(a|b)", "b", TRUE, TRUE);
    REGEX_TESTLM("(a|b)", "c", FALSE, FALSE);
    REGEX_TESTLM("a|b", "b", TRUE, TRUE);

    REGEX_TESTLM("(a|b|c)*", "aabcaaccbcabc", TRUE, TRUE);
    REGEX_TESTLM("(a|b|c)*", "aabcaaccbcabdc", TRUE, FALSE);
    REGEX_TESTLM("(a(b|c|d)(x|y|z)*|123)", "ac", TRUE, TRUE);
    REGEX_TESTLM("(a(b|c|d)(x|y|z)*|123)", "123", TRUE, TRUE);
    REGEX_TESTLM("(a|(1|2)*)(b|c|d)(x|y|z)*|123", "123", TRUE, TRUE);
    REGEX_TESTLM("(a|(1|2)*)(b|c|d)(x|y|z)*|123", "222211111czzzzw", TRUE, FALSE);

    //
    //  +
    //
    REGEX_TESTLM("ab+", "abbc", TRUE, FALSE);
    REGEX_TESTLM("ab+c", "ac", FALSE, FALSE);
    REGEX_TESTLM("b+", "", FALSE, FALSE);
    REGEX_TESTLM("(abc|def)+", "defabc", TRUE, TRUE);
    REGEX_TESTLM(".+y", "zippity dooy dah ", TRUE, FALSE);
    REGEX_TESTLM(".+y", "zippity dooy", TRUE, TRUE);

    //
    //   ?
    //
    REGEX_TESTLM("ab?", "ab", TRUE, TRUE);
    REGEX_TESTLM("ab?", "a", TRUE, TRUE);
    REGEX_TESTLM("ab?", "ac", TRUE, FALSE);
    REGEX_TESTLM("ab?", "abb", TRUE, FALSE);
    REGEX_TESTLM("a(b|c)?d", "abd", TRUE, TRUE);
    REGEX_TESTLM("a(b|c)?d", "acd", TRUE, TRUE);
    REGEX_TESTLM("a(b|c)?d", "ad", TRUE, TRUE);
    REGEX_TESTLM("a(b|c)?d", "abcd", FALSE, FALSE);
    REGEX_TESTLM("a(b|c)?d", "ab", FALSE, FALSE);


};

