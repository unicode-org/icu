/*****************************************************************************************
*                                                                                       *
* COPYRIGHT:                                                                            *
*   (C) Copyright International Business Machines Corporation,  2000                    *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.                  *
*   US Government Users Restricted Rights - Use, duplication, or disclosure             *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                              *
*                                                                                       *
*****************************************************************************************
************************************************************************
*   Date        Name        Description
*   03/22/2000   Madhu        Creation.
************************************************************************/

#include "ittrans.h"
#include "ufltlgts.h"
#include "unicode/utypes.h"
#include "unicode/translit.h"
#include "unicode/unifilt.h"
#include "unicode/unifltlg.h"
#include "intltest.h"

//---------------------------------------------
// runIndexedTest
//---------------------------------------------

void UnicodeFilterLogicTest::runIndexedTest( int32_t index, bool_t exec, char* &name, char* par )
{
    if (exec) logln((UnicodeString)"TestSuite UnicodeFilterLogic API ");
    switch (index) {
     
        case 0: name = "TestAll"; if (exec) TestAll(); break;
       
        default: name = ""; break; /*needed to end loop*/
    }
}

class Filter1: public UnicodeFilter{
	virtual UnicodeFilter* clone() const{
		return new Filter1(*this);
	}
	virtual bool_t contains(UChar c) const {
		if(c == 'a' || c == 'A' || c == 'c' || c == 'C')
			return FALSE;
		else
			return TRUE;
	}
};
class Filter2: public UnicodeFilter{
	virtual UnicodeFilter* clone() const{
		return new Filter2(*this);
	}
	virtual bool_t contains(UChar c) const {
		if(c == 'y' || c == 'Y' || c == 'z' || c == 'Z'  || c =='a' || c == 'c')
			return FALSE;
		else
			return TRUE;
	}
};


void UnicodeFilterLogicTest::TestAll(){
	
	Transliterator *t1=Transliterator::createInstance("Unicode-Hex");
	if(t1 == 0){
		errln("FAIL: Error in instantiation.");
		return;
	}
	UnicodeString source("abcdABCDyzYZ");
	
	//sanity testing wihtout any filter
	expect(*t1, "without any Filter", source, "\\u0061\\u0062\\u0063\\u0064\\u0041\\u0042\\u0043\\u0044\\u0079\\u007A\\u0059\\u005A");
	//sanity testing using the Filter1(acAC) and Filter2(acyzYZ)
	t1->adoptFilter(new Filter1);
	expect(*t1, "with Filter(acAC)", source, "a\\u0062c\\u0064A\\u0042C\\u0044\\u0079\\u007A\\u0059\\u005A");
	t1->adoptFilter(new Filter2);
	expect(*t1, "with Filter2(acyzYZ)", source, "a\\u0062c\\u0064\\u0041\\u0042\\u0043\\u0044yzYZ");


   	UnicodeFilter *filterNOT=UnicodeFilterLogic::createNot(new Filter1);
	UnicodeFilter *filterAND=UnicodeFilterLogic::createAnd(new Filter1, new Filter2);
	UnicodeFilter *filterOR=UnicodeFilterLogic::createOr(new Filter1, new Filter2);
   
	TestNOT(*t1, new Filter1, "Filter(acAC)", 
		source, "\\u0061b\\u0063d\\u0041B\\u0043DyzYZ");
	TestNOT(*t1, new Filter2, "Filter(acyzYZ)",
		source, "\\u0061b\\u0063dABCD\\u0079\\u007A\\u0059\\u005A");
	TestNOT(*t1, NULL, "NULL",
		source, "abcdABCDyzYZ");
	TestNOT(*t1, filterNOT, "FilterNOT(Fitler1(acAC))",
		source, "a\\u0062c\\u0064A\\u0042C\\u0044\\u0079\\u007A\\u0059\\u005A");
	TestNOT(*t1, filterAND, "FilterAND(Fitler1(acAC),  Filter2(acyzYZ))",
		source, "\\u0061b\\u0063d\\u0041B\\u0043D\\u0079\\u007A\\u0059\\u005A");
	TestNOT(*t1, filterOR, "FilterOR(Fitler1(acAC),  Filter2(acyzYZ))",
		source, "\\u0061b\\u0063dABCDyzYZ");

	TestAND(*t1, new Filter1, new Filter2, "Filter1(a,c,A,C), Filter2(acyzYZ)", 
		source, "a\\u0062c\\u0064A\\u0042C\\u0044yzYZ");
	TestAND(*t1, new Filter2, new Filter1, "Filter2(acyzYZ), Filter1(a,c,A,C), ", 
		source, "a\\u0062c\\u0064A\\u0042C\\u0044yzYZ");
	TestAND(*t1, new Filter1, NULL, "Filter1(a,c,A,C), NULL", 
		source, "a\\u0062c\\u0064A\\u0042C\\u0044\\u0079\\u007A\\u0059\\u005A");
	TestAND(*t1, NULL, new Filter2, "NULL, Filter2(acyzYZ)",
		source, "a\\u0062c\\u0064\\u0041\\u0042\\u0043\\u0044yzYZ");
	TestAND(*t1, NULL, NULL, "NULL, NULL", 
		source, "\\u0061\\u0062\\u0063\\u0064\\u0041\\u0042\\u0043\\u0044\\u0079\\u007A\\u0059\\u005A");
	TestAND(*t1, filterAND, NULL, "FilterAND(Fitler1(acAC),  Filter2(acyzYZ)), NULL",
		source, "a\\u0062c\\u0064A\\u0042C\\u0044yzYZ");
	TestAND(*t1, filterAND, new Filter1, "FilterAND(Fitler1(acAC),  Filter2(acyzYZ)), Filter1(acAC)",
		source, "a\\u0062c\\u0064A\\u0042C\\u0044yzYZ");
	TestAND(*t1, filterAND, new Filter2, "FilterAND(Fitler1(acAC),  Filter2(acyzYZ)), Filter2(acyzYZ)",
		source, "a\\u0062c\\u0064A\\u0042C\\u0044yzYZ");
	TestAND(*t1, new Filter1, filterAND, "Filter1(acAC), FilterAND(Filter1(acAC), Fitler1(acAC))",
		source, "a\\u0062c\\u0064A\\u0042C\\u0044yzYZ");
	TestAND(*t1, new Filter2, filterAND, "Filter2(acyzYZ), FilterAND(Filter1(acAC), Fitler1(acAC))",
		source, "a\\u0062c\\u0064A\\u0042C\\u0044yzYZ");
	TestAND(*t1, filterOR, NULL, "FilterOR(Fitler1(acAC),  Filter2(acyzYZ)), NULL",
		source, "a\\u0062c\\u0064\\u0041\\u0042\\u0043\\u0044\\u0079\\u007A\\u0059\\u005A");
	TestAND(*t1, filterOR, new Filter1, "FilterOR(Fitler1(acAC),  Filter2(acyzYZ)), Fitler1(acAC)",
		source, "a\\u0062c\\u0064A\\u0042C\\u0044\\u0079\\u007A\\u0059\\u005A");
	TestAND(*t1, filterOR, new Filter2, "FilterOR(Fitler1(acAC),  Filter2(acyzYZ)), Fitler2(acyzYZ)",
		source, "a\\u0062c\\u0064\\u0041\\u0042\\u0043\\u0044yzYZ");
	TestAND(*t1, filterNOT, new Filter1, "FilterNOT(Fitler1(acAC)), Fitler1(acAC)",
		source, "abcdABCDyzYZ");
	TestAND(*t1, new Filter1, filterNOT, "Fitler1(acAC), FilterNOT(Fitler1(acAC))",
		source, "abcdABCDyzYZ");
	TestAND(*t1, filterNOT, new Filter2, "FilterNOT(Fitler1(acAC)), Fitler2(acyzYZ)",
		source, "abcd\\u0041B\\u0043DyzYZ");
	TestAND(*t1, new Filter2, filterNOT, "Fitler2(acyzYZ), FilterNOT(Fitler1(acAC))",
		source, "abcd\\u0041B\\u0043DyzYZ");

	TestOR(*t1, new Filter1, new Filter2, "Filter1(a,c,A,C), Filter2(acyzYZ)",
		source, "a\\u0062c\\u0064\\u0041\\u0042\\u0043\\u0044\\u0079\\u007A\\u0059\\u005A");
	TestOR(*t1, new Filter2, new Filter1, "Filter2(acyzYZ), Filter1(a,c,A,C)",
		source, "a\\u0062c\\u0064\\u0041\\u0042\\u0043\\u0044\\u0079\\u007A\\u0059\\u005A");
	TestOR(*t1, new Filter1, NULL, "Filter1(a,c,A,C), NULL",
		source, "a\\u0062c\\u0064A\\u0042C\\u0044\\u0079\\u007A\\u0059\\u005A");
	TestOR(*t1, NULL, new Filter2, "NULL, Filter2(acyzYZ)",
		source, "a\\u0062c\\u0064\\u0041\\u0042\\u0043\\u0044yzYZ");
	TestOR(*t1, NULL, NULL, "NULL, NULL",
		source, "\\u0061\\u0062\\u0063\\u0064\\u0041\\u0042\\u0043\\u0044\\u0079\\u007A\\u0059\\u005A");
	TestOR(*t1, filterAND, NULL, "FilterAND(Fitler1(acAC),  Filter2(acyzYZ)), NULL",
		source, "a\\u0062c\\u0064A\\u0042C\\u0044yzYZ");
	TestOR(*t1, filterAND, new Filter1, "FilterAND(Fitler1(acAC),  Filter2(acyzYZ)), Filter1(acAC)",
		source, "a\\u0062c\\u0064A\\u0042C\\u0044\\u0079\\u007A\\u0059\\u005A");
	TestOR(*t1, filterAND, new Filter2, "FilterAND(Fitler1(acAC),  Filter2(acyzYZ)), Filter2(acyzYZ)",
		source, "a\\u0062c\\u0064\\u0041\\u0042\\u0043\\u0044yzYZ");
	TestOR(*t1, new Filter1, filterAND, "Filter1(acAC), FilterAND(Filter1(acAC), Fitler1(acAC))",
		source, "a\\u0062c\\u0064A\\u0042C\\u0044\\u0079\\u007A\\u0059\\u005A");
	TestOR(*t1, new Filter2, filterAND, "Filter2(acyzYZ), FilterAND(Filter1(acAC), Fitler1(acAC))",
		source, "a\\u0062c\\u0064\\u0041\\u0042\\u0043\\u0044yzYZ");
	TestOR(*t1, filterNOT, new Filter1, "FilterNOT(Fitler1(acAC)), Fitler1(acAC)",
		source, "\\u0061\\u0062\\u0063\\u0064\\u0041\\u0042\\u0043\\u0044\\u0079\\u007A\\u0059\\u005A");
	TestOR(*t1, new Filter1, filterNOT, "Fitler1(acAC), FilterNOT(Fitler1(acAC))",
		source, "\\u0061\\u0062\\u0063\\u0064\\u0041\\u0042\\u0043\\u0044\\u0079\\u007A\\u0059\\u005A");
	TestOR(*t1, filterNOT, new Filter2, "FilterNOT(Fitler1(acAC)), Fitler1(acyzYZ)",
		source, "\\u0061\\u0062\\u0063\\u0064\\u0041\\u0042\\u0043\\u0044yzYZ");
	TestOR(*t1, new Filter2, filterNOT, "Fitler2(acyzYZ), FilterNOT(Fitler1(acAC))",
		source, "\\u0061\\u0062\\u0063\\u0064\\u0041\\u0042\\u0043\\u0044yzYZ");


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
                                   const UnicodeString& summary, bool_t pass,
                                   const UnicodeString& expectedResult) {
    if (pass) {
        logln(UnicodeString("(")+tag+") " + prettify(summary));
    } else {
        errln(UnicodeString("FAIL: (")+tag+") "
              + prettify(summary)
              + ", expected " + prettify(expectedResult));
    }
}


