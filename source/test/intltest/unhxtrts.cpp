
/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2003, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

/***********************************************************************
************************************************************************
*   Date        Name        Description
*   03/15/2000   Madhu        Creation.
************************************************************************/

#include "unicode/utypes.h"

#if !UCONFIG_NO_TRANSLITERATION

#include "ittrans.h"
#include "unhxtrts.h"
#include "unicode/utypes.h"
#include "unicode/translit.h"
#include "unicode/unifilt.h"
#include "unicode/uchar.h"
#include "intltest.h"
#include "unitohex.h"
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

void UniToHexTransliteratorTest::runIndexedTest( int32_t index, UBool exec, const char* &name, char* /*par*/ )
{
    if (exec) logln((UnicodeString)"TestSuite UnicodeToHexadecimal Transliterator API ");
    switch (index) {

        case 0: name = "TestConstruction"; if (exec) TestConstruction(); break;
        case 1: name = "TestCloneEqual"; if (exec) TestCloneEqual(); break;
        case 2: name = "TestUpperCase"; if (exec) TestUpperCase(); break;
        case 3: name = "TestPattern"; if (exec) TestPattern(); break;
        case 4: name = "TestSimpleTransliterate"; if (exec) TestSimpleTransliterate(); break;
        case 5: name = "TestTransliterate"; if (exec) TestTransliterate(); break;
        default: name = ""; break; /*needed to end loop*/
    }
}

// This test used to call handleTransliterate.  That is a protected
// method that isn't supposed to be called externally.  This method is
// a workaround to make it call the correct method.
static void pseudoHandleTransliterate(const Transliterator* t,
                                      Replaceable& text,
                                      UTransPosition& index,
                                      UBool incremental) {
    if (incremental) {
        UErrorCode status = U_ZERO_ERROR;
        t->transliterate(text, index, status);
    } else {
        t->finishTransliteration(text, index);
    }
}

/**
 * Used by TestConstruction() and TestTransliterate.
 */
int32_t gTestUniFilterClassID;
class TestUniFilter : public UnicodeFilter {
  virtual UClassID getDynamicClassID() const { return &gTestUniFilterClassID; }
    virtual UnicodeFunctor* clone() const {
        return new TestUniFilter(*this);
    }
    virtual UBool contains(UChar32 c) const {
       if(c==0x0063 || c==0x0061 || c==0x0043 || c==0x0041)
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
void UniToHexTransliteratorTest::TestConstruction(){
    UErrorCode status=U_ZERO_ERROR;
    logln("Testing the construction UnicodeToHexTransliterator()");
    UnicodeToHexTransliterator *trans1=new UnicodeToHexTransliterator();
    if(trans1==0){
        errln("UnicodeToHexTransliterator construction failed Error=" + (UnicodeString)u_errorName(status));
        return;
    }
    delete trans1;

    logln("Testing the cosntruction UnicodeToHexTransliterator(pattern, status)");
    UnicodeString pattern("\\\\U+0000abc");
    trans1=new UnicodeToHexTransliterator(pattern, status);
    if(U_FAILURE(status)){
        errln("UnicodeToHexTransliterator construction failed with pattern =" + pattern + " Error=" + (UnicodeString)u_errorName(status));
        status=U_ZERO_ERROR;
        return;
    }
    delete trans1;

    logln("Testing the cosntruction UnicodeToHexTransliterator(pattern, status) with illegal pattern");
    UnicodeString pattern2("\\X+");
    trans1=new UnicodeToHexTransliterator(pattern2, status);
    if(U_FAILURE(status)){
        logln("OK: UnicodeToHexTransliterator construction for illegal pattern failed, as expected");
        status=U_ZERO_ERROR;
    } else {
        errln("Error: calling the UnicodeToHexTransliterator constructor with illegal pattern should fail");
    }
    delete trans1;

    logln("Testing the construction UnicodeToHexTransliterator(pattern, isUppercase, adoptedFilter, status)");
    trans1=new UnicodeToHexTransliterator(pattern, FALSE, NULL, status);
    if(U_FAILURE(status)){
        errln("UnicodeToHexTransliterator construction failed. Error=" + (UnicodeString)u_errorName(status));
        status=U_ZERO_ERROR;
        return;
    }
    logln("Testing the copy construction");
    UnicodeToHexTransliterator *trans1copy=new UnicodeToHexTransliterator(*trans1);
    if(trans1->toPattern() != trans1copy->toPattern() || trans1->isUppercase() != trans1copy->isUppercase() ||
         trans1->getID() != trans1copy->getID()){
        errln("Copy construction failed");
    }
    delete trans1copy; 
    delete trans1;

    logln("Testing the construction UnicodeToHexTransliterator(pattern, isUppercase, adoptedFilter, status)");
    trans1=new UnicodeToHexTransliterator(pattern, TRUE, new TestUniFilter, status);
    if(U_FAILURE(status)){
        errln("UnicodeToHexTransliterator construction failed Error=" + (UnicodeString)u_errorName(status));
        status=U_ZERO_ERROR;
        return;
    }
    logln("Testing the copy construction");
    trans1copy=new UnicodeToHexTransliterator(*trans1);
    if(trans1->toPattern() != trans1copy->toPattern() || trans1->isUppercase() != trans1copy->isUppercase() ||
        trans1->getID() != trans1copy->getID() || 
        trans1->getFilter() == NULL || trans1copy->getFilter() == NULL ){
        errln("Copy construction failed");
    }
    delete trans1copy;
    delete trans1;



}

void UniToHexTransliteratorTest::TestCloneEqual(){
    UErrorCode status=U_ZERO_ERROR;
    UnicodeToHexTransliterator *transdefault=new UnicodeToHexTransliterator();
    UnicodeString pattern1("\\U##00");
    UnicodeString pattern2("\\\\uni0000");
    UnicodeToHexTransliterator *trans1=new UnicodeToHexTransliterator(pattern1, status);
    if(U_FAILURE(status) && status==U_ILLEGAL_ARGUMENT_ERROR){
        errln("UnicodeToHexTransliterator construction failed");
        status=U_ZERO_ERROR;
        return;
    }
    UnicodeToHexTransliterator *trans2=new UnicodeToHexTransliterator(pattern2, status);
    if(U_FAILURE(status) && status==U_ILLEGAL_ARGUMENT_ERROR){
        errln("UnicodeToHexTransliterator construction failed");
        status=U_ZERO_ERROR;
        return;
    }

    logln("Testing the clone() API of the UnicodeToHexTransliterator");
    UnicodeToHexTransliterator *transdefaultclone=(UnicodeToHexTransliterator*)transdefault->clone();
    UnicodeToHexTransliterator *trans1clone=(UnicodeToHexTransliterator*)trans1->clone();
    UnicodeToHexTransliterator *trans2clone=(UnicodeToHexTransliterator*)trans2->clone();
    if(transdefault->toPattern() !=  transdefaultclone->toPattern()      || 
        transdefault->isUppercase() !=  transdefaultclone->isUppercase() ||
        trans1->toPattern() != trans1clone->toPattern()     || 
        trans1->isUppercase() != trans1clone->isUppercase() || 
        trans2->toPattern() != trans2clone->toPattern()     || 
        trans2->isUppercase() != trans2clone->isUppercase() ||
        transdefault->toPattern() == trans1->toPattern()    || 
        trans1->toPattern() == trans2clone->toPattern()     || 
        trans2->toPattern() == transdefault->toPattern()    ) {
        errln("Error: clone() failed");
    }


    logln("Testing the =operator of the UnicodeToHexTransliterator");
    UnicodeToHexTransliterator *transdefaultequal=new UnicodeToHexTransliterator();
    UnicodeToHexTransliterator *trans1equal=new UnicodeToHexTransliterator();
    UnicodeToHexTransliterator *trans2equal=new UnicodeToHexTransliterator();
    *transdefaultequal=*transdefault;
    *trans1equal=*trans1;
    *trans2equal=*trans2;

    if(transdefault->toPattern() != transdefaultequal->toPattern()      ||
        transdefault->isUppercase() != transdefaultequal->isUppercase() ||
        trans1->toPattern() != trans1equal->toPattern()      ||
        trans1->isUppercase() != trans1equal->isUppercase()  ||
        trans2->toPattern() != trans2equal->toPattern()      ||
        trans2->isUppercase() != trans2equal->isUppercase()  ||
        transdefault->toPattern() == trans1->toPattern()     ||
        trans1->toPattern() == trans2equal->toPattern()      ||
        trans2->toPattern() == transdefault->toPattern()  ) {
        errln("Error: equal() failed");
    }
    if(transdefaultclone->toPattern() != transdefaultequal->toPattern() || 
        transdefaultclone->isUppercase() != transdefaultequal->isUppercase() ||
        trans1equal->toPattern()  != trans1clone->toPattern()  || 
        trans1equal->isUppercase() != trans1clone->isUppercase()  ||
        trans2clone->toPattern() != trans2equal->toPattern()  ||
        trans2clone->isUppercase() != trans2equal->isUppercase() ) {
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

void UniToHexTransliteratorTest::TestUpperCase(){
    logln("Testing the isUppercase() and setUppercase() API of UnicodeToHexTransliterator");
    UErrorCode status = U_ZERO_ERROR; 
    UnicodeString str("abk");
    /*default transliterator has upper case TRUE*/
    UnicodeToHexTransliterator *transdefault=new UnicodeToHexTransliterator();
    if(transdefault == 0){
        errln("UnicodeToHexTransliterator construction failed");
        return;
    }
    expect(*transdefault, "where uppercase=default", str, UnicodeString("\\u0061\\u0062\\u006B", ""));

    UnicodeString pattern("\\\\u0000", "");
    /*transliterator with Uppercase FALSE*/
    UnicodeToHexTransliterator *trans1=new UnicodeToHexTransliterator(pattern, FALSE, NULL, status);
    if(U_FAILURE(status) && status==U_ILLEGAL_ARGUMENT_ERROR){
        errln("UnicodeToHexTransliterator construction failed with pattern =" + pattern);
        status=U_ZERO_ERROR;
        return;
    }
    expect(*trans1, "where uppercase=FALSE", str, UnicodeString("\\u0061\\u0062\\u006b", ""));  /*doesn't display uppercase*/

    if(transdefault->isUppercase() != TRUE  || trans1->isUppercase() != FALSE ){
        errln("isUpperCase() failed");
    }
    /*changing the outputhexdigits to lower case for the default transliterator*/
    transdefault->setUppercase(trans1->isUppercase());
    if(transdefault->isUppercase() != trans1->isUppercase() || transdefault->isUppercase() != FALSE){
        errln("setUppercase() failed");
    }
    /*doesn't ouput uppercase hex, since transdefault's uppercase is set to FALSE using setUppercase*/
    expect(*transdefault, "where uppercase=FALSE", str, UnicodeString("\\u0061\\u0062\\u006b", ""));     

    /*trying round trip*/
    transdefault->setUppercase(TRUE);
    if(transdefault->isUppercase() != TRUE || transdefault->isUppercase() == trans1->isUppercase() ){
        errln("setUppercase() failed");
    }
    /*displays upper case since it is set to TRUE*/
    expect(*transdefault, "where uppercase=TRUE", str, UnicodeString("\\u0061\\u0062\\u006B", ""));

    delete transdefault;
    delete trans1;

}
void UniToHexTransliteratorTest::TestPattern(){
    logln("Testing the applyPattern() and toPattern() API of UnicodeToHexTransliterator");
    UErrorCode status = U_ZERO_ERROR; 
    /*default transliterator has pattern \\u0000*/
    UnicodeToHexTransliterator *transdefault=new UnicodeToHexTransliterator();
    if(transdefault == 0){
        errln("UnicodeToHexTransliterator construction failed");
        return;
    }
    UnicodeString defaultpattern=transdefault->toPattern();

    UnicodeString pattern1("\\\\U+0000", "");
    UnicodeToHexTransliterator *trans1=new UnicodeToHexTransliterator(pattern1, TRUE, NULL, status);
    if(U_FAILURE(status) && status==U_ILLEGAL_ARGUMENT_ERROR){
        errln("UnicodeToHexTransliterator construction failed with pattern =" + pattern1);
        status=U_ZERO_ERROR;
        return;
    }
    /*test toPattern() */
    if(transdefault->toPattern() == trans1->toPattern() ||
        transdefault->toPattern() != UnicodeString("\\\\u0000", "") ||
        trans1->toPattern() != pattern1 ){
        errln("Error: toPattern() failed");
    }

    /*apply patterns for transdefault*/
    UnicodeString str("abKf");
    expectPattern(*transdefault,  pattern1, str, UnicodeString("\\U+0061\\U+0062\\U+004B\\U+0066", ""));
    expectPattern(*transdefault,  UnicodeString("\\U##00,", ""), str, UnicodeString("U61,U62,U4B,U66,", ""));
    expectPattern(*transdefault, defaultpattern, str, UnicodeString("\\u0061\\u0062\\u004B\\u0066", ""));
    expectPattern(*trans1, UnicodeString("\\uni0000", ""), str, UnicodeString("uni0061uni0062uni004Buni0066", ""));
    expectPattern(*trans1, UnicodeString("\\\\S-0000-E", ""), str, UnicodeString("\\S-0061-E\\S-0062-E\\S-004B-E\\S-0066-E", ""));
    expectPattern(*trans1, UnicodeString("\\u##0000", ""), str, UnicodeString("FAIL", ""));
    expectPattern(*trans1, UnicodeString("\\*0000", ""), str, UnicodeString("*0061*0062*004B*0066", "")); 
    expectPattern(*trans1, UnicodeString("\\u####", ""), str, UnicodeString("FAIL", ""));

    delete trans1;
    delete transdefault;

}
void UniToHexTransliteratorTest::TestSimpleTransliterate(){
    UErrorCode status=U_ZERO_ERROR;
    UnicodeString pattern1("\\\\U+0000", "");
    UnicodeToHexTransliterator *trans1=new UnicodeToHexTransliterator(pattern1, TRUE, NULL, status);
    if(U_FAILURE(status) && status==U_ILLEGAL_ARGUMENT_ERROR){
        errln("UnicodeToHexTransliterator construction failed with pattern =" + pattern1);
        status=U_ZERO_ERROR;
        return;
    }
    UTransPosition index={1,5,2,5};
    UnicodeString source("Hello");
    UnicodeString rsource(source);
    UnicodeString expected("He\\U+006C\\U+006C\\U+006F", "");
    pseudoHandleTransliterate(trans1, rsource, index, FALSE);
    expectAux(trans1->getID() + ":handleTransliterator ", source + "-->" + rsource, rsource==expected, expected);
    delete trans1;
}

void UniToHexTransliteratorTest::TestTransliterate(){
    UErrorCode status=U_ZERO_ERROR;
    UnicodeString Data[]={
        //pattern, source, index.contextStart, index.contextLimit, index.start, expectedResult, expectedResult using filter(a, b)
        UnicodeString("U+##00", ""),    UnicodeString("abc", ""), "1", "3", "2", UnicodeString("abU+63", ""), UnicodeString("abc", ""),
        UnicodeString("\\\\u0000", ""), UnicodeString("abc", ""), "1", "2", "1", UnicodeString("a\\u0062c", ""), UnicodeString("a\\u0062c", ""),
        UnicodeString("Uni0000", ""),   UnicodeString("abc", ""), "1", "3", "2", UnicodeString("abUni0063", ""), UnicodeString("abc", ""),
        UnicodeString("U[0000]", ""),   UnicodeString("hello", ""), "0", "4", "2", UnicodeString("heU[006C]U[006C]o", ""), UnicodeString("heU[006C]U[006C]o", ""),
        UnicodeString("prefix-0000-suffix", ""), UnicodeString("abc", ""), "1", "3", "1", UnicodeString("aprefix-0062-suffixprefix-0063-suffix", ""), UnicodeString("aprefix-0062-suffixc", ""),
        UnicodeString("*##00*", ""),     UnicodeString("hellothere", ""), "1", "8", "4", UnicodeString("hell*6F**74**68**65*re", ""), UnicodeString("hell*6F**74**68**65*re", ""),

    };
    uint32_t i;
    for(i=0;i<sizeof(Data)/sizeof(Data[0]);i=i+7){
        UnicodeToHexTransliterator *trans1=new UnicodeToHexTransliterator(Data[i+0], TRUE, NULL, status);
        if(U_FAILURE(status)){
            errln("UnicodeToHexTransliterator construction failed with pattern =" + Data[i+0]);
            status=U_ZERO_ERROR;
            continue;
        }
        expectTranslit(*trans1, "", Data[i+1], getInt(Data[i+2]), getInt(Data[i+3]), getInt(Data[i+4]), Data[i+5] );
        delete trans1;
        UnicodeToHexTransliterator *trans2=new UnicodeToHexTransliterator(Data[i+0], TRUE, new TestUniFilter, status);
        if(U_FAILURE(status)){
            errln("UnicodeToHexTransliterator construction failed with pattern=" + Data[i+0] + "with filter(a,c)" );
            status=U_ZERO_ERROR;
            continue;
        }
        expectTranslit(*trans2, " with filter(a,A,c,C)", Data[i+1], getInt(Data[i+2]), getInt(Data[i+3]), getInt(Data[i+4]), Data[i+6] );
        delete trans2;

    }



}

//======================================================================
// Support methods
//======================================================================

void UniToHexTransliteratorTest::expectTranslit(const UnicodeToHexTransliterator& t,
                                                const UnicodeString& message,
                                                const UnicodeString& source, 
                                                int32_t start, int32_t limit, int32_t cursor,
                                                const UnicodeString& expectedResult){
    

    UTransPosition _index;
    _index.contextStart=start;
    _index.contextLimit= limit;
    _index.start = cursor;
    _index.limit = limit;
    UTransPosition index = _index;
    UnicodeString rsource(source);
    pseudoHandleTransliterate(&t, rsource, index, FALSE);
    expectAux(t.getID() + ":handleTransliterator(increment=FALSE) " + message, source + "-->" + rsource, rsource==expectedResult, expectedResult);

    UnicodeString rsource2(source);
    index=_index;
    pseudoHandleTransliterate(&t, rsource2, index, TRUE);
    expectAux(t.getID() + ":handleTransliterator(increment=TRUE) "+ message, source + "-->" + rsource2, rsource2==expectedResult, expectedResult);

    /*ceates a copy constructor and checks the transliteration*/
    UnicodeToHexTransliterator *copy=new UnicodeToHexTransliterator(t);
    rsource2.remove();
    rsource2.append(source);
    index=_index;
    pseudoHandleTransliterate(copy, rsource2, index, FALSE);
    expectAux(t.getID() + "COPY:handleTransliterator(increment=FALSE) " + message, source + "-->" + rsource2, rsource2==expectedResult, expectedResult);

    rsource2.remove();
    rsource2.append(source);
    index=_index;
    pseudoHandleTransliterate(copy, rsource2, index, TRUE);
    expectAux(t.getID() + "COPY:handleTransliterator(increment=TRUE) " + message, source + "-->" + rsource2, rsource2==expectedResult, expectedResult);
    delete copy;

    /*creates a clone and tests transliteration*/
    UnicodeToHexTransliterator *clone=(UnicodeToHexTransliterator*)t.clone();
    rsource2.remove();
    rsource2.append(source);
    index=_index;
    pseudoHandleTransliterate(clone, rsource2, index, FALSE);
    expectAux(t.getID() + "CLONE:handleTransliterator(increment=FALSE) "+ message,source + "-->" + rsource2, rsource2==expectedResult, expectedResult);

    rsource2.remove();
    rsource2.append(source);
    index=_index;
    pseudoHandleTransliterate(clone, rsource2, index, TRUE);
    expectAux(t.getID() + "CLONE:handleTransliterator(increment=TRUE) "+ message, source + "-->" + rsource2, rsource2==expectedResult, expectedResult);
    delete clone;

    /*Uses the assignment operator to create a transliterator and tests transliteration*/
    UnicodeToHexTransliterator equal=t;
    rsource2.remove();
    rsource2.append(source);
    index=_index;
    pseudoHandleTransliterate(&equal, rsource2, index, FALSE);
    expectAux(t.getID() + "=OPERATOR:handleTransliterator(increment=FALSE) "+ message, source + "-->" + rsource2, rsource2==expectedResult, expectedResult);

    rsource2.remove();
    rsource2.append(source);
    index=_index;
    pseudoHandleTransliterate(&equal, rsource2, index, TRUE);
    expectAux(t.getID() + "=OPERATOR:handleTransliterator(increment=TRUE) "+ message, source + "-->" + rsource2, rsource2==expectedResult, expectedResult);





}
void UniToHexTransliteratorTest::expectPattern(UnicodeToHexTransliterator& t,
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
                expect(t, (UnicodeString)" with pattern "+pattern, source, expectedResult);
            }
        }
    }

}
void UniToHexTransliteratorTest::expect(const UnicodeToHexTransliterator& t,
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
    index.contextStart=0;
    index.contextLimit = source.length();
    index.start =0;
    index.limit=source.length();
    pseudoHandleTransliterate(&t, rsource, index, TRUE);
    expectAux(t.getID() + ":handleTransliterate " + message, source + "->" + rsource, rsource==expectedResult, expectedResult);

}
void UniToHexTransliteratorTest::expectAux(const UnicodeString& tag,
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
