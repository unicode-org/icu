/***************************************************************************
*
*   Copyright (C) 2000-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
************************************************************************
*   Date        Name        Description
*   03/17/2000   Madhu        Creation.
************************************************************************/

#include "unicode/utypes.h"

#if !UCONFIG_NO_TRANSLITERATION

#include "ittrans.h"
#include "hxuntrts.h"
#include "unicode/utypes.h"
#include "unicode/translit.h"
#include "unicode/unifilt.h"
#include "unicode/uchar.h"
#include "hextouni.h"
#include "intltest.h"
#include "cmemory.h"
#include <string.h>
#include <stdio.h>
/*converts a Unicodestring to integer*/
static int32_t getInt(UnicodeString str)
{
  int32_t result = 0;
  int32_t len = str.length();
  int32_t i = 0;
  for(i=0; i<len; i++) {
    result = result*10+u_charDigitValue(str.char32At(i));
  }
  return result;
}

//---------------------------------------------
// runIndexedTest
//---------------------------------------------

void HexToUniTransliteratorTest::runIndexedTest( int32_t index, UBool exec, const char* &name, char* /*par*/ )
{
    if (exec) logln((UnicodeString)"TestSuite HexadecimalToUnicode Transliterator API ");
    switch (index) {
     
        case 0: name = "TestConstruction"; if (exec) TestConstruction(); break;
        case 1: name = "TestCloneEqual"; if (exec) TestCloneEqual(); break;
        case 2: name = "TestPattern"; if (exec) TestPattern(); break;
        case 3: name = "TestSimpleTransliterate"; if (exec) TestSimpleTransliterate(); break;
        case 4: name = "TestTransliterate"; if (exec) TestTransliterate(); break;
        default: name = ""; break; /*needed to end loop*/
    }
}
/**
 * Used by TestConstruction() and TestTransliterate.
 */
uint32_t gTestHexFilterClassID = 0;
class TestHexFilter : public UnicodeFilter {
  virtual UClassID getDynamicClassID() const { return &gTestHexFilterClassID; }
    virtual UnicodeFunctor* clone() const {
        return new TestHexFilter(*this);
    }
    virtual UBool contains(UChar32 c) const {
       if(c == 0x0061 || c == 0x0063 )
          return FALSE;
       else
          return TRUE;
    }
    // Stubs
    virtual UnicodeString& toPattern(UnicodeString& result,
                                     UBool /*escapeUnprintable*/) const {
        return result;
    }
    virtual UBool matchesIndexValue(uint8_t /*v*/) const {
        return FALSE;
    }
    virtual void addMatchSetTo(UnicodeSet& /*toUnionTo*/) const {}
};
void HexToUniTransliteratorTest::TestConstruction(){
    UErrorCode status=U_ZERO_ERROR;
    logln("Testing the construction HexToUnicodeTransliterator()");
    HexToUnicodeTransliterator *trans1=new HexToUnicodeTransliterator();
    if(trans1==0){
        errln("HexToUnicodeTransliterator construction failed Error=" + (UnicodeString)u_errorName(status));
        return;
    }
    delete trans1;

    logln("Testing the cosntruction HexToUnicodeTransliterator(pattern, status)");
    UnicodeString pattern("\\\\U+0000abc");
    trans1=new HexToUnicodeTransliterator(pattern, status);
    if(U_FAILURE(status)){
        errln("HexToUnicodeTransliterator construction failed with pattern =" + pattern + "  Error=" + (UnicodeString)u_errorName(status));
        status=U_ZERO_ERROR;
        return;
    }
    delete trans1;

    logln("Testing the construction HexToUnicodeTransliterator(pattern, status) with illegal pattern");
    UnicodeString pattern2("\\X+");
    trans1=new HexToUnicodeTransliterator(pattern2, status);
    if(U_FAILURE(status)){
        logln("OK: HexToUnicodeTransliterator construction for illegal pattern failed, as expected");
        status=U_ZERO_ERROR;
    } else {
        errln("Error: calling the HexToUnicodeTransliterator constructor with illegal pattern should fail");
    }
    delete trans1;

    logln("Testing the construction HexToUnicodeTransliterator(pattern, adoptedFilter, status)");
    trans1=new HexToUnicodeTransliterator(pattern, NULL, status);
    if(U_FAILURE(status)){
        errln("HexToUnicodeTransliterator construction failed. Error=" + (UnicodeString)u_errorName(status));
        status=U_ZERO_ERROR;
        return;
    }
    logln("Testing the copy construction");
    HexToUnicodeTransliterator *trans1copy=new HexToUnicodeTransliterator(*trans1);
    if(trans1->toPattern() != trans1copy->toPattern() || 
        trans1->getID() != trans1copy->getID() ){
        errln("Copy construction failed");
    }
    delete trans1copy; 
    delete trans1;

    logln("Testing the construction HexToUnicodeTransliterator(adoptedFilter)");

    trans1=new HexToUnicodeTransliterator(new TestHexFilter);
    if(trans1 == 0){
        errln("HexToUnicodeTransliterator construction failed. Error=" + (UnicodeString)u_errorName(status));
        return;
    }
    logln("Testing the copy construction");
    trans1copy=new HexToUnicodeTransliterator(*trans1);
    if(trans1->getFilter() == NULL || trans1copy->getFilter() == NULL ||
        trans1->toPattern() != trans1copy->toPattern() || 
        trans1->getID() != trans1copy->getID() ){
        errln("Copy construction failed");
    }

    delete trans1copy; 
    delete trans1;

}

void HexToUniTransliteratorTest::TestCloneEqual(){
    UErrorCode status=U_ZERO_ERROR;
    HexToUnicodeTransliterator *transdefault=new HexToUnicodeTransliterator();
    UnicodeString pattern1("\\U##00");
    UnicodeString pattern2("\\\\uni0000");
    HexToUnicodeTransliterator *trans1=new HexToUnicodeTransliterator(pattern1, status);
    if(U_FAILURE(status) && status==U_ILLEGAL_ARGUMENT_ERROR){
        errln("HexToUnicodeTransliterator construction failed");
        status=U_ZERO_ERROR;
        return;
    }
    HexToUnicodeTransliterator *trans2=new HexToUnicodeTransliterator(pattern2, status);
    if(U_FAILURE(status) && status==U_ILLEGAL_ARGUMENT_ERROR){
        errln("HexToUnicodeTransliterator construction failed");
        status=U_ZERO_ERROR;
        return;
    }

    logln("Testing the clone() API of the HexToUnicodeTransliterator");
    HexToUnicodeTransliterator *transdefaultclone=(HexToUnicodeTransliterator*)transdefault->clone();
    HexToUnicodeTransliterator *trans1clone=(HexToUnicodeTransliterator*)trans1->clone();
    HexToUnicodeTransliterator *trans2clone=(HexToUnicodeTransliterator*)trans2->clone();
    if(transdefault->toPattern() !=  transdefaultclone->toPattern() || 
        trans1->toPattern() != trans1clone->toPattern()  || 
        trans2->toPattern() != trans2clone->toPattern()  || 
        transdefault->toPattern() == trans1->toPattern() || 
        trans1->toPattern() == trans2clone->toPattern()  || 
        trans2->toPattern() == transdefault->toPattern() ) {
        errln("Error: clone() failed");
    }

    logln("Testing the =operator of the HexToUnicodeTransliterator");
    HexToUnicodeTransliterator *transdefaultequal=new HexToUnicodeTransliterator();
    HexToUnicodeTransliterator *trans1equal=new HexToUnicodeTransliterator();
    HexToUnicodeTransliterator *trans2equal=new HexToUnicodeTransliterator();
    *transdefaultequal=*transdefault;
    *trans1equal=*trans1;
    *trans2equal=*trans2;
    if(transdefault->toPattern() != transdefaultequal->toPattern()      ||
        trans1->toPattern() != trans1equal->toPattern()      ||
        trans2->toPattern() != trans2equal->toPattern()      ||
        transdefault->toPattern() == trans1->toPattern()     ||
        trans1->toPattern() == trans2equal->toPattern()      ||
        trans2->toPattern() == transdefault->toPattern()  ) {
        errln("Error: equal() failed");
    }
    if(transdefaultclone->toPattern() != transdefaultequal->toPattern() || 
        trans1equal->toPattern()  != trans1clone->toPattern()  || 
        trans2clone->toPattern() != trans2equal->toPattern()  ){
            errln("Error: equal() or clone() failed");
    }
    delete transdefaultclone;
    delete trans1clone;
    delete trans2clone;
    delete transdefaultequal;
    delete trans1equal;
    delete trans2equal;
    delete transdefault;
    delete trans1;
    delete trans2;
}

void HexToUniTransliteratorTest::TestPattern(){
    logln("Testing the applyPattern() and toPattern() API of HexToUnicodeTransliterator");
    UErrorCode status = U_ZERO_ERROR; 
    /*default transliterator has pattern \\u0000*/
    HexToUnicodeTransliterator *transdefault=new HexToUnicodeTransliterator();
    if(transdefault == 0){
        errln("HexToUnicodeTransliterator construction failed. Error=" + (UnicodeString)u_errorName(status));
        return;
    }
    UnicodeString defaultpattern=transdefault->toPattern();

    UnicodeString pattern1("\\\\U+0000", "");
    HexToUnicodeTransliterator *trans1=new HexToUnicodeTransliterator(pattern1, NULL, status);
    if(U_FAILURE(status) ){
        errln("HexToUnicodeTransliterator construction failed with pattern =" + pattern1);
        status=U_ZERO_ERROR;
        return;
    }
    /*test toPattern() */
    if(transdefault->toPattern() == trans1->toPattern() ||
        transdefault->toPattern() != UnicodeString("\\\\u0000;\\\\U0000;u+0000;U+0000", "") ||
        trans1->toPattern() != pattern1 ){
        errln("Error: toPattern() failed "+ transdefault->toPattern());
    }

    /*apply patterns for transdefault*/
    UnicodeString str("abKf");
    expectPattern(*transdefault,  pattern1,  UnicodeString("\\U+0061\\U+0062\\U+004B\\U+0066", ""), str);
    expectPattern(*transdefault,  UnicodeString("\\U##00,", ""), UnicodeString("U61,U62,U4B,U66,", ""), str);
    expectPattern(*transdefault, defaultpattern, UnicodeString("\\u0061\\u0062\\u004B\\u0066", ""),  str);
    expectPattern(*trans1, UnicodeString("\\uni0000", ""),  UnicodeString("uni0061uni0062uni004Buni0066", ""),  str);
    expectPattern(*trans1, UnicodeString("\\\\S-0000-E", ""), UnicodeString("\\S-0061-E\\S-0062-E\\S-004B-E\\S-0066-E", ""),  str);
    expectPattern(*trans1, UnicodeString("\\u##0000", ""), UnicodeString("\\u##0061\\u##0062", ""), "FAIL");
    expectPattern(*trans1, UnicodeString("\\*0000", ""),  UnicodeString("*0061*0062*004B*0066", ""),  str); 
    expectPattern(*trans1, UnicodeString("\\u####", ""), UnicodeString("\\u##0061\\u##0062", ""), "FAIL");

    delete trans1;
    delete transdefault;

}
void HexToUniTransliteratorTest::TestSimpleTransliterate(){
    logln("Testing the handleTransliterate() API of HexToUnicodeTransliterator");
    UErrorCode status=U_ZERO_ERROR;
    UnicodeString pattern1("\\\\U+0000", "");
    HexToUnicodeTransliterator *trans1=new HexToUnicodeTransliterator(pattern1, NULL, status);
    if(U_FAILURE(status)){
        errln("HexToUnicodeTransliterator construction failed with pattern =" + pattern1 + "Error: " + (UnicodeString)u_errorName(status));
        status=U_ZERO_ERROR;
        return;
    }
    UnicodeString source("He\\U+006C\\U+006C\\U+006F", "");
    UnicodeString rsource(source);
    UTransPosition index;
    index.contextStart =1;
    index.contextLimit = source.length();
    index.start = 2;
    index.limit =source.length();
    UnicodeString expected("Hello");
    trans1->handleTransliterate(rsource, index, FALSE);
    expectAux(trans1->getID() + ":handleTransliterator ", source + "-->" + rsource, rsource==expected, expected);
    expect(*trans1, "", UnicodeString("\\U+0048\\U+0065\\U+006C\\U+006C\\U+006F", ""), expected);
    delete trans1;

    HexToUnicodeTransliterator *trans2=new HexToUnicodeTransliterator(new TestHexFilter);
    expect(*trans2, "with Filter(0x0061, 0x0063) ", CharsToUnicodeString("\\u0061\\u0062\\u0063"),
        CharsToUnicodeString("\\u0061b\\u0063") );
    delete trans2;

}
void HexToUniTransliteratorTest::TestTransliterate(){
    UErrorCode status=U_ZERO_ERROR;
    UnicodeString Data[]={
        //pattern, source, index.contextStart, index.contextLimit, index.start, expectedResult,
        UnicodeString("U+##00", ""),    UnicodeString("abU+63", ""), "1", "7", "2",  UnicodeString("abc", ""), 
        UnicodeString("\\\\u0000", ""), UnicodeString("a\\u0062c", ""), "1", "7", "1",  UnicodeString("abc", ""), 
        UnicodeString("Uni0000", ""),   UnicodeString("abUni0063", ""), "1", "9", "2",  UnicodeString("abc", ""), 
        UnicodeString("U[0000]", ""),   UnicodeString("heU[006C]U[006C]o", ""), "0", "16", "2", UnicodeString("hello", ""), 
        UnicodeString("prefix-0000-suffix", ""), UnicodeString("aprefix-0062-suffixprefix-0063-suffix", ""), "1", "39", "1", UnicodeString("abc", ""), 
        UnicodeString("*##00*", ""),    UnicodeString("hell*6F**74**68**65*re", ""),  "1", "20", "4", UnicodeString("hellothere", ""), 

    };
    uint32_t i;
    for(i=0;i<sizeof(Data)/sizeof(Data[0]);i=i+6){
        HexToUnicodeTransliterator *trans1=new HexToUnicodeTransliterator(Data[i+0], NULL, status);
        if(U_FAILURE(status)){
            errln("HexToUnicodeTransliterator construction failed with pattern =" + Data[i+0]);
            status=U_ZERO_ERROR;
            continue;
        }
        expectTranslit(*trans1, "", Data[i+1], getInt(Data[i+2]), getInt(Data[i+3]), getInt(Data[i+4]), Data[i+5] );
        delete trans1;

    }


}

//======================================================================
// Support methods
//======================================================================

void HexToUniTransliteratorTest::expectTranslit(const HexToUnicodeTransliterator& t,
                                                const UnicodeString& message,
                                                const UnicodeString& source, 
                                                int32_t start, int32_t limit, int32_t cursor,
                                                const UnicodeString& expectedResult){


    UTransPosition _index;
    _index.contextStart =start;
    _index.contextLimit = limit;
    _index.start = cursor;
    _index.limit = limit;
    UTransPosition index;
    uprv_memcpy(&index, &_index, sizeof(index));
    UnicodeString rsource(source);
    t.handleTransliterate(rsource, index, FALSE);
    expectAux(t.getID() + ":handleTransliterator(increment=FALSE) "+ message, source + "-->" + rsource, rsource==expectedResult, expectedResult);

    UnicodeString rsource2(source);
    uprv_memcpy(&index, &_index, sizeof(index));
    t.handleTransliterate(rsource2, index, TRUE);
    expectAux(t.getID() + ":handleTransliterator(increment=TRUE) "+ message, source + "-->" + rsource2, rsource2==expectedResult, expectedResult);

    /*ceates a copy constructor and checks the transliteration*/
    HexToUnicodeTransliterator *copy=new HexToUnicodeTransliterator(t);
    rsource2.remove();
    rsource2.append(source);
    uprv_memcpy(&index, &_index, sizeof(index));
    copy->handleTransliterate(rsource2, index, FALSE);
    expectAux(t.getID() + "COPY:handleTransliterator(increment=FALSE) "+ message, source + "-->" + rsource2, rsource2==expectedResult, expectedResult);

    rsource2.remove();
    rsource2.append(source);
    uprv_memcpy(&index, &_index, sizeof(index));
    copy->handleTransliterate(rsource2, index, TRUE);
    expectAux(t.getID() + "COPY:handleTransliterator(increment=TRUE) "+ message, source + "-->" + rsource2, rsource2==expectedResult, expectedResult);
    delete copy;

    /*creates a clone and tests transliteration*/
    HexToUnicodeTransliterator *clone=(HexToUnicodeTransliterator*)t.clone();
    rsource2.remove();
    rsource2.append(source);
    uprv_memcpy(&index, &_index, sizeof(index));
    clone->handleTransliterate(rsource2, index, FALSE);
    expectAux(t.getID() + "CLONE:handleTransliterator(increment=FALSE) "+ message, source + "-->" + rsource2, rsource2==expectedResult, expectedResult);

    rsource2.remove();
    rsource2.append(source);
    uprv_memcpy(&index, &_index, sizeof(index));
    clone->handleTransliterate(rsource2, index, TRUE);
    expectAux(t.getID() + "CLONE:handleTransliterator(increment=TRUE) "+ message, source + "-->" + rsource2, rsource2==expectedResult, expectedResult);
    delete clone;

    /*Uses the assignment operator to create a transliterator and tests transliteration*/
    HexToUnicodeTransliterator equal=t;
    rsource2.remove();
    rsource2.append(source);
    uprv_memcpy(&index, &_index, sizeof(index));
    equal.handleTransliterate(rsource2, index, FALSE);
    expectAux(t.getID() + "=OPERATOR:handleTransliterator(increment=FALSE) "+ message, source + "-->" + rsource2, rsource2==expectedResult, expectedResult);

    rsource2.remove();
    rsource2.append(source);
    uprv_memcpy(&index, &_index, sizeof(index));
    equal.handleTransliterate(rsource2, index, TRUE);
    expectAux(t.getID() + "=OPERATOR:handleTransliterator(increment=TRUE) "+ message, source + "-->" + rsource2, rsource2==expectedResult, expectedResult);

}


void HexToUniTransliteratorTest::expectPattern(HexToUnicodeTransliterator& t,
                                               const UnicodeString& pattern, 
                                               const UnicodeString& source, 
                                               const UnicodeString& expectedResult){

    UErrorCode status=U_ZERO_ERROR; 
    t.applyPattern(pattern, status);
    if(expectedResult == "FAIL"){
        if(U_FAILURE(status)){
            logln("OK: calling applyPattern() with illegal pattern failed as expected. Error=" + (UnicodeString)u_errorName(status));
            status=U_ZERO_ERROR;
            return;
        }
    }
    else{
        if(U_FAILURE(status)){
            errln("Error: applyPattern() failed with pattern =" + pattern + "--->" + (UnicodeString)u_errorName(status));
            return;
        }else {
            if(t.toPattern() != pattern) {
                errln("Error: applyPattern or toPatten failed.  Expected: " + pattern + "Got: " + t.toPattern());
            }
            else{
                logln("OK: applyPattern passed. Testing transliteration");
                expect(t, " with pattern "+pattern, source, expectedResult);
            }
        }
    }

}
void HexToUniTransliteratorTest::expect(const HexToUnicodeTransliterator& t,
                                const UnicodeString& message,
                                const UnicodeString& source,
                                const UnicodeString& expectedResult) {
    
    UnicodeString rsource(source);
    t.transliterate(rsource);
    expectAux(t.getID() + ":Replaceable " + message, source + "->" + rsource, rsource==expectedResult, expectedResult);

    // Test handleTransliterate (incremental) transliteration -- 
    rsource.remove();
    rsource.append(source);
    UTransPosition index;
    index.contextStart =0;
    index.contextLimit =source.length();
    index.start=0;
    index.limit = source.length();
    t.handleTransliterate(rsource, index, TRUE);
    expectAux(t.getID() + ":handleTransliterate " + message, source + "->" + rsource, rsource==expectedResult, expectedResult);


}
void HexToUniTransliteratorTest::expectAux(const UnicodeString& tag,
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

#endif /* #if !UCONFIG_NO_TRANSLITERATION */
