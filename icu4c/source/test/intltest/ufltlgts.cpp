/***************************************************************************
*
*   Copyright (C) 2000-2002, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
***************************************************************************************************************************************************
*   Date        Name        Description
*   03/22/2000   Madhu        Creation.
************************************************************************/

#include "unicode/utypes.h"

#if !UCONFIG_NO_TRANSLITERATION

#include "unicode/translit.h"
#include "unicode/unifilt.h"
#include "unicode/unifltlg.h"
#include "intltest.h"
#include "ittrans.h"
#include "ufltlgts.h"

//---------------------------------------------
// runIndexedTest
//---------------------------------------------

void UnicodeFilterLogicTest::runIndexedTest( int32_t index, UBool exec, const char* &name, char* /*par*/ )
{
    if (exec) logln((UnicodeString)"TestSuite UnicodeFilterLogic API ");
    switch (index) {
     
        case 0: name = "TestAll"; if (exec) TestAll(); break;
       
        default: name = ""; break; /*needed to end loop*/
    }
}
int32_t gFilter1ClassID;
class Filter1: public UnicodeFilter{
  virtual UClassID getDynamicClassID() const { return (UClassID)&gFilter1ClassID; }
    virtual UnicodeFunctor* clone() const{
        return new Filter1(*this);
    }
    virtual UBool contains(UChar32 c) const {
        if(c == 0x0061 || c == 0x0041 || c == 0x0063 || c == 0x0043)
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
uint32_t gFilter2ClassID = 0;
class Filter2: public UnicodeFilter{
  virtual UClassID getDynamicClassID() const { return (UClassID)&gFilter2ClassID; }
    virtual UnicodeFunctor* clone() const{
        return new Filter2(*this);
    }
    virtual UBool contains(UChar32 c) const {
        if(c == 0x0079 || c == 0x0059 || c == 0x007a || c == 0x005a  || c == 0x0061 || c == 0x0063)
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


void UnicodeFilterLogicTest::TestAll(){
    UParseError parseError;
    UErrorCode status = U_ZERO_ERROR;
    Transliterator *t1=Transliterator::createInstance("Any-Hex", UTRANS_FORWARD, parseError, status);
    if(t1 == 0){
        errln("FAIL: Error in instantiation.");
        return;
    }
    UnicodeString source("abcdABCDyzYZ");
    Filter1 filter1;
    Filter2 filter2;

    //sanity testing wihtout any filter
    expect(*t1, "without any Filter", source, UnicodeString("\\u0061\\u0062\\u0063\\u0064\\u0041\\u0042\\u0043\\u0044\\u0079\\u007A\\u0059\\u005A", ""));
    //sanity testing using the Filter1(acAC) and Filter2(acyzYZ)
    t1->adoptFilter(new Filter1);
    expect(*t1, "with Filter(acAC)", source, UnicodeString("a\\u0062c\\u0064A\\u0042C\\u0044\\u0079\\u007A\\u0059\\u005A", ""));
    t1->adoptFilter(new Filter2);
    expect(*t1, "with Filter2(acyzYZ)", source, UnicodeString("a\\u0062c\\u0064\\u0041\\u0042\\u0043\\u0044yzYZ", ""));


    UnicodeFilter *filterNOT=UnicodeFilterLogic::createNot(&filter1);
    UnicodeFilter *filterAND=UnicodeFilterLogic::createAnd(&filter1, &filter2);
    UnicodeFilter *filterOR=UnicodeFilterLogic::createOr(&filter1, &filter2);

    TestNOT(*t1, &filter1, "Filter(acAC)", 
        source, UnicodeString("\\u0061b\\u0063d\\u0041B\\u0043DyzYZ", ""));
    TestNOT(*t1, &filter2, "Filter(acyzYZ)",
        source, UnicodeString("\\u0061b\\u0063dABCD\\u0079\\u007A\\u0059\\u005A", ""));
    TestNOT(*t1, NULL, "NULL",
        source, UnicodeString("abcdABCDyzYZ", ""));
    TestNOT(*t1, filterNOT, "FilterNOT(Fitler1(acAC))",
        source, UnicodeString("a\\u0062c\\u0064A\\u0042C\\u0044\\u0079\\u007A\\u0059\\u005A", ""));
    TestNOT(*t1, filterAND, "FilterAND(Fitler1(acAC),  Filter2(acyzYZ))",
        source, UnicodeString("\\u0061b\\u0063d\\u0041B\\u0043D\\u0079\\u007A\\u0059\\u005A", ""));
    TestNOT(*t1, filterOR, "FilterOR(Fitler1(acAC),  Filter2(acyzYZ))",
        source, UnicodeString("\\u0061b\\u0063dABCDyzYZ", ""));

    TestAND(*t1, &filter1, &filter2, "Filter1(a,c,A,C), Filter2(acyzYZ)", 
        source, UnicodeString("a\\u0062c\\u0064A\\u0042C\\u0044yzYZ", ""));
    TestAND(*t1, &filter2, &filter1, "Filter2(acyzYZ), Filter1(a,c,A,C), ", 
        source, UnicodeString("a\\u0062c\\u0064A\\u0042C\\u0044yzYZ", ""));
    TestAND(*t1, &filter1, NULL, "Filter1(a,c,A,C), NULL", 
        source, UnicodeString("a\\u0062c\\u0064A\\u0042C\\u0044\\u0079\\u007A\\u0059\\u005A", ""));
    TestAND(*t1, NULL, &filter2, "NULL, Filter2(acyzYZ)",
        source, UnicodeString("a\\u0062c\\u0064\\u0041\\u0042\\u0043\\u0044yzYZ", ""));
    TestAND(*t1, NULL, NULL, "NULL, NULL", 
        source, UnicodeString("\\u0061\\u0062\\u0063\\u0064\\u0041\\u0042\\u0043\\u0044\\u0079\\u007A\\u0059\\u005A", ""));
    TestAND(*t1, filterAND, NULL, "FilterAND(Fitler1(acAC),  Filter2(acyzYZ)), NULL",
        source, UnicodeString("a\\u0062c\\u0064A\\u0042C\\u0044yzYZ", ""));
    TestAND(*t1, filterAND, &filter1, "FilterAND(Fitler1(acAC),  Filter2(acyzYZ)), Filter1(acAC)",
        source, UnicodeString("a\\u0062c\\u0064A\\u0042C\\u0044yzYZ", ""));
    TestAND(*t1, filterAND, &filter2, "FilterAND(Fitler1(acAC),  Filter2(acyzYZ)), Filter2(acyzYZ)",
        source, UnicodeString("a\\u0062c\\u0064A\\u0042C\\u0044yzYZ", ""));
    TestAND(*t1, &filter1, filterAND, "Filter1(acAC), FilterAND(Filter1(acAC), Fitler1(acAC))",
        source, UnicodeString("a\\u0062c\\u0064A\\u0042C\\u0044yzYZ", ""));
    TestAND(*t1, &filter2, filterAND, "Filter2(acyzYZ), FilterAND(Filter1(acAC), Fitler1(acAC))",
        source, UnicodeString("a\\u0062c\\u0064A\\u0042C\\u0044yzYZ", ""));
    TestAND(*t1, filterOR, NULL, "FilterOR(Fitler1(acAC),  Filter2(acyzYZ)), NULL",
        source, UnicodeString("a\\u0062c\\u0064\\u0041\\u0042\\u0043\\u0044\\u0079\\u007A\\u0059\\u005A", ""));
    TestAND(*t1, filterOR, &filter1, "FilterOR(Fitler1(acAC),  Filter2(acyzYZ)), Fitler1(acAC)",
        source, UnicodeString("a\\u0062c\\u0064A\\u0042C\\u0044\\u0079\\u007A\\u0059\\u005A", ""));
    TestAND(*t1, filterOR, &filter2, "FilterOR(Fitler1(acAC),  Filter2(acyzYZ)), Fitler2(acyzYZ)",
        source, UnicodeString("a\\u0062c\\u0064\\u0041\\u0042\\u0043\\u0044yzYZ", ""));
    TestAND(*t1, filterNOT, &filter1, "FilterNOT(Fitler1(acAC)), Fitler1(acAC)",
        source, UnicodeString("abcdABCDyzYZ", ""));
    TestAND(*t1, &filter1, filterNOT, "Fitler1(acAC), FilterNOT(Fitler1(acAC))",
        source, UnicodeString("abcdABCDyzYZ", ""));
    TestAND(*t1, filterNOT, &filter2, "FilterNOT(Fitler1(acAC)), Fitler2(acyzYZ)",
        source, UnicodeString("abcd\\u0041B\\u0043DyzYZ", ""));
    TestAND(*t1, &filter2, filterNOT, "Fitler2(acyzYZ), FilterNOT(Fitler1(acAC))",
        source, UnicodeString("abcd\\u0041B\\u0043DyzYZ", ""));

    TestOR(*t1, &filter1, &filter2, "Filter1(a,c,A,C), Filter2(acyzYZ)",
        source, UnicodeString("a\\u0062c\\u0064\\u0041\\u0042\\u0043\\u0044\\u0079\\u007A\\u0059\\u005A", ""));
    TestOR(*t1, &filter2, &filter1, "Filter2(acyzYZ), Filter1(a,c,A,C)",
        source, UnicodeString("a\\u0062c\\u0064\\u0041\\u0042\\u0043\\u0044\\u0079\\u007A\\u0059\\u005A", ""));
    TestOR(*t1, &filter1, NULL, "Filter1(a,c,A,C), NULL",
        source, UnicodeString("a\\u0062c\\u0064A\\u0042C\\u0044\\u0079\\u007A\\u0059\\u005A", ""));
    TestOR(*t1, NULL, &filter2, "NULL, Filter2(acyzYZ)",
        source, UnicodeString("a\\u0062c\\u0064\\u0041\\u0042\\u0043\\u0044yzYZ", ""));
    TestOR(*t1, NULL, NULL, "NULL, NULL",
        source, UnicodeString("\\u0061\\u0062\\u0063\\u0064\\u0041\\u0042\\u0043\\u0044\\u0079\\u007A\\u0059\\u005A", ""));
    TestOR(*t1, filterAND, NULL, "FilterAND(Fitler1(acAC),  Filter2(acyzYZ)), NULL",
        source, UnicodeString("a\\u0062c\\u0064A\\u0042C\\u0044yzYZ", ""));
    TestOR(*t1, filterAND, &filter1, "FilterAND(Fitler1(acAC),  Filter2(acyzYZ)), Filter1(acAC)",
        source, UnicodeString("a\\u0062c\\u0064A\\u0042C\\u0044\\u0079\\u007A\\u0059\\u005A", ""));
    TestOR(*t1, filterAND, &filter2, "FilterAND(Fitler1(acAC),  Filter2(acyzYZ)), Filter2(acyzYZ)",
        source, UnicodeString("a\\u0062c\\u0064\\u0041\\u0042\\u0043\\u0044yzYZ", ""));
    TestOR(*t1, &filter1, filterAND, "Filter1(acAC), FilterAND(Filter1(acAC), Fitler1(acAC))",
        source, UnicodeString("a\\u0062c\\u0064A\\u0042C\\u0044\\u0079\\u007A\\u0059\\u005A", ""));
    TestOR(*t1, &filter2, filterAND, "Filter2(acyzYZ), FilterAND(Filter1(acAC), Fitler1(acAC))",
        source, UnicodeString("a\\u0062c\\u0064\\u0041\\u0042\\u0043\\u0044yzYZ", ""));
    TestOR(*t1, filterNOT, &filter1, "FilterNOT(Fitler1(acAC)), Fitler1(acAC)",
        source, UnicodeString("\\u0061\\u0062\\u0063\\u0064\\u0041\\u0042\\u0043\\u0044\\u0079\\u007A\\u0059\\u005A", ""));
    TestOR(*t1, &filter1, filterNOT, "Fitler1(acAC), FilterNOT(Fitler1(acAC))",
        source, UnicodeString("\\u0061\\u0062\\u0063\\u0064\\u0041\\u0042\\u0043\\u0044\\u0079\\u007A\\u0059\\u005A", ""));
    TestOR(*t1, filterNOT, &filter2, "FilterNOT(Fitler1(acAC)), Fitler1(acyzYZ)",
        source, UnicodeString("\\u0061\\u0062\\u0063\\u0064\\u0041\\u0042\\u0043\\u0044yzYZ", ""));
    TestOR(*t1, &filter2, filterNOT, "Fitler2(acyzYZ), FilterNOT(Fitler1(acAC))",
        source, UnicodeString("\\u0061\\u0062\\u0063\\u0064\\u0041\\u0042\\u0043\\u0044yzYZ", ""));


    delete filterNOT;
    delete filterAND;
    delete filterOR;
    delete t1;

}
void UnicodeFilterLogicTest::TestNOT(Transliterator& t,
                                     const UnicodeFilter* f1, 
                                     const UnicodeString& message,
                                     const UnicodeString& source,
                                     const UnicodeString& expected){
    UnicodeFilter *filter=UnicodeFilterLogic::createNot(f1);
    t.adoptFilter(filter);
    expect(t, "with FilterNOT(" + message + ")", source, expected);

}
void UnicodeFilterLogicTest::TestAND(Transliterator& t,
                                     const UnicodeFilter* f1, 
                                     const UnicodeFilter* f2, 
                                     const UnicodeString& message,
                                     const UnicodeString& source,
                                     const UnicodeString& expected){
    UnicodeFilter *filter=UnicodeFilterLogic::createAnd(f1, f2);
    t.adoptFilter(filter);
    expect(t, "with FilterAND(" + message + ")", source, expected);

}
void UnicodeFilterLogicTest::TestOR(Transliterator& t,
                                    const UnicodeFilter* f1, 
                                    const UnicodeFilter* f2, 
                                    const UnicodeString& message,
                                    const UnicodeString& source,
                                    const UnicodeString& expected){
    UnicodeFilter *filter=UnicodeFilterLogic::createOr(f1, f2);
    t.adoptFilter(filter);
    expect(t, "with FilterOR(" + message + ")", source, expected);

}

void UnicodeFilterLogicTest::expect(const Transliterator& t,
                                    const UnicodeString& message,
                                    const UnicodeString& source,
                                    const UnicodeString& expectedResult) {


    UnicodeString rsource(source);
    t.transliterate(rsource);
    expectAux(t.getID() + ":Replaceable " + message, source + "->" + rsource, rsource==expectedResult, expectedResult);

}
void UnicodeFilterLogicTest::expectAux(const UnicodeString& tag,
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
