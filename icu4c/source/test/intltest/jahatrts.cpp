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
*   03/20/2000   Madhu        Creation.
************************************************************************/

#include "ittrans.h"
#include "jahatrts.h"
#include "unicode/utypes.h"
#include "unicode/translit.h"
#include "unicode/jamohang.h"
#include "unicode/unifilt.h"
#include "intltest.h"
#include <stdio.h>
#include <string.h>
/*converts a Unicodestring to integer*/
static int32_t getInt(UnicodeString str)
{
	int len=str.length();
	char *alias;
	char *buffer=new char[len+1];
	alias=buffer;
	for(int i=0; i< len; i++){
		*alias=(char)str.charAt(i);
		alias++;
	}
	*alias='\0';
	return atoi(buffer);
}
//---------------------------------------------
// runIndexedTest
//---------------------------------------------

void JamoToHangTransliteratorTest::runIndexedTest( int32_t index, UBool exec, char* &name, char* par )
{
    if (exec) logln((UnicodeString)"TestSuite JamoToHangul Transliterator API ");
    switch (index) {
     
        case 0: name = "TestConstruction"; if (exec) TestConstruction(); break;
        case 1: name = "TestCloneEqual"; if (exec) TestCloneEqual(); break;
        case 2: name = "TestSimpleTransliterate"; if (exec) TestSimpleTransliterate(); break;
        case 3: name = "TestTransliterate"; if (exec) TestTransliterate(); break;
		case 4: name = "TestTransliterate2"; if (exec) TestTransliterate2(); break;
        default: name = ""; break; /*needed to end loop*/
    }
}
/**
 * Used by TestConstruction() and TestTransliterate.
 */
class TestJamoFilter : public UnicodeFilter {
    virtual UnicodeFilter* clone() const {
        return new TestJamoFilter(*this);
    }
    virtual UBool contains(UChar c) const {
       if(c == 0x1101 )
          return FALSE;
       else
          return TRUE;
    }
};
void JamoToHangTransliteratorTest::TestConstruction(){
	logln("Testing the construction JamoHangulTransliterator()");
	JamoHangulTransliterator *trans1=new JamoHangulTransliterator();
	if(trans1 == 0){
		errln("JamoHangulTransliterator() construction failed.");
		return;
	}
   
	JamoHangulTransliterator *trans2=new JamoHangulTransliterator(new TestJamoFilter);
	if(trans2 == 0){
		errln("JamoHangulTransliterator(UnicodeFilter) construction failed.");
		return;
	}
	logln("Testing copy construction");
	JamoHangulTransliterator *trans2copy=new JamoHangulTransliterator(*trans2);
	if(trans2copy == 0){
		errln("JamoHangulTransliterator copy construction failed");
		delete trans2;
	}
	
	if(trans2copy->getID() != trans2->getID() ||
		trans2copy->getFilter() == NULL || 
		trans2copy->getFilter()->contains(0x1101) != trans2->getFilter()->contains(0x1101) ) {
		errln("Copy construction failed");
	}
    	

	delete trans1;
	delete trans2copy;
   	delete trans2;

}
void JamoToHangTransliteratorTest::TestCloneEqual(){
	logln("Testing the clone and =operator of JamoHangulTransliterator");
	JamoHangulTransliterator *trans1=new JamoHangulTransliterator();
	if(trans1 == 0){
		errln("JamoHangulTransliterator() construction failed.");
		return;
	}
	JamoHangulTransliterator *trans2=new JamoHangulTransliterator(new TestJamoFilter);
	if(trans2 == 0){
		errln("JamoHangulTransliterator(UnicodeFilter) construction failed.");
		return;
	}
    
	JamoHangulTransliterator *trans1equal=trans1;
	JamoHangulTransliterator *trans2equal=trans2;
	if(trans1equal == 0 || trans2equal==0 ){
		errln("=Operator failed");
		delete trans1;
		delete trans2;
		return;
	}
	if(trans1->getID() != trans1equal->getID() ||
	    trans1equal->getFilter() != NULL   || 
		trans2->getID() != trans2equal->getID() ||
		trans2equal->getFilter() == NULL  ||
		trans2equal->getFilter()->contains(0x1101) != trans2->getFilter()->contains(0x1101) ) {
		errln("=Operator failed");
	}
    

	JamoHangulTransliterator *trans1clone=(JamoHangulTransliterator*)trans1->clone();
	JamoHangulTransliterator *trans2clone=(JamoHangulTransliterator*)trans2->clone();
	if(trans1clone == 0 || trans2clone==0 ){
		errln("clone() failed");
		delete trans1;
		delete trans2;
		return;
	}
	if(trans1->getID() != trans1clone->getID() ||
	    trans1clone->getFilter() != NULL   || 
		trans2->getID() != trans2clone->getID() ||
		trans2clone->getFilter() == NULL  ||
		trans2clone->getFilter()->contains(0x1101) != trans2->getFilter()->contains(0x1101) ) {
		errln("=Operator failed");
	}

	delete trans1;
	delete trans2;
}

void JamoToHangTransliteratorTest::TestSimpleTransliterate(){
	logln("Testing the handleTransliterate() of JamoHangulTransliterator");
	JamoHangulTransliterator *trans1=new JamoHangulTransliterator();
	if(trans1==0){
		errln("JamoHangulTransliterator construction failed");
		return;
	}
	UChar src[]={ 0x1101, 0x1109, 0x1166, 0x11A8, 0x11AD, 0x116D, 0};
    UnicodeString source(src);
	UnicodeString expected(CharsToUnicodeString("\\uAE4C\\uC139\\uC54A\\uC694"));
	expect(*trans1, "", source, expected);

	JamoHangulTransliterator *trans2=new JamoHangulTransliterator(new TestJamoFilter);
	if(trans2==0){
		errln("JamoHangulTransliterator(UnicodeFilter) construction failed");
		return;
	}
	expect(*trans2, " with Filter(0x1101) ",  source, CharsToUnicodeString("\\u1101\\uC139\\uC54A\\uC694"));


}
void JamoToHangTransliteratorTest::TestTransliterate2(){
	logln("Testing the handleTransliterate() of JamoHangulTransliterator");
	JamoHangulTransliterator *trans1=new JamoHangulTransliterator();
	if(trans1==0){
		errln("JamoHangulTransliterator construction failed");
		return;
	}
	UnicodeString source, expected, temp;
	UChar choseong=0x1100;
	UChar jungseong=0x1161;
	UChar jongseong=0x11a8;
	for(UChar c=0xac01;c<0xacff;++c){
		source.append(choseong);
		if(jongseong > 0x11c2){
			jongseong=0x11a8;
			jungseong++;
			source.append(jungseong);
		}
		else {
			source.append(jungseong);
			source.append(jongseong++);
		}
		expected.append(c);
		
	    expect(*trans1, "",  source, expected);
        source.remove();
		expected.remove();

	}
}
void JamoToHangTransliteratorTest::TestTransliterate(){
	UnicodeString Data[]={
	//	source, index.start, index.limit, index.cursor, expectedResult, expectedResult using Filter(TestJamoFilter)
		CharsToUnicodeString("\\u1100\\u1101\\u1102"), "1", "3", "1", CharsToUnicodeString("\\u1100\\uAE4C\\uB098"), CharsToUnicodeString("\\u1100\\u1101\\uB098"),		
		CharsToUnicodeString("\\u1167\\u1101"), "0", "1", "0",  CharsToUnicodeString("\\uc5ec\\u1101"), CharsToUnicodeString("\\uc5ec\\u1101"),
		CharsToUnicodeString("\\u1167\\u1101"), "0", "2", "0",  CharsToUnicodeString("\\uc5ec\\uae4c"), CharsToUnicodeString("\\uc5ec\\u1101"),
		
		
	};
	int i;
	JamoHangulTransliterator *trans1=new JamoHangulTransliterator();
	if(trans1 == 0){
		errln("JamoHangulTransliterator construction failed");
	    return;
	}
	JamoHangulTransliterator *trans2=new JamoHangulTransliterator(new TestJamoFilter);
	if(trans2 == 0){
		errln("JamoHangulTransliterator(UnicodeFilter) construction failed");
		return;
	}
	for(i=0;i<sizeof(Data)/sizeof(Data[0]);i=i+6){
		expectTranslit(*trans1, ":", Data[i+0], getInt(Data[i+1]), getInt(Data[i+2]), getInt(Data[i+3]), Data[i+4] );
		expectTranslit(*trans2, " with Filter(0x1101):", Data[i+0], getInt(Data[i+1]), getInt(Data[i+2]), getInt(Data[i+3]), Data[i+5] );
        
	}
    delete trans1;
	delete trans2;

}
//======================================================================
// Support methods
//======================================================================

void JamoToHangTransliteratorTest::expectTranslit(const JamoHangulTransliterator& t,
												  const UnicodeString& message,
												const UnicodeString& source, 
												int32_t start, int32_t limit, int32_t cursor,
												const UnicodeString& expectedResult){
    

	Transliterator::Position index(start, limit, cursor);
   	UnicodeString rsource(source);
	t.handleTransliterate(rsource, index, FALSE);
	expectAux(t.getID() + ":handleTransliterator(increment=FALSE) " + message, source + "-->" + rsource, rsource==expectedResult, expectedResult);
    
	UnicodeString rsource2(source);
	index=Transliterator::Position(start, limit, cursor);
	t.handleTransliterate(rsource2, index, TRUE);
	expectAux(t.getID() + ":handleTransliterator(increment=TRUE) " + message, source + "-->" + rsource2, rsource2==expectedResult, expectedResult);
   
    /*ceates a copy constructor and checks the transliteration*/
	JamoHangulTransliterator *copy=new JamoHangulTransliterator(t);
	rsource2.remove();
	rsource2.append(source);
	index=Transliterator::Position(start, limit, cursor);
	copy->handleTransliterate(rsource2, index, FALSE);
	expectAux(t.getID() + "COPY:handleTransliterator(increment=FALSE) "+ message, source + "-->" + rsource2, rsource2==expectedResult, expectedResult);
    
	rsource2.remove();
	rsource2.append(source);
	index=Transliterator::Position(start, limit, cursor);
	copy->handleTransliterate(rsource2, index, TRUE);
	expectAux(t.getID() + "COPY:handleTransliterator(increment=TRUE) "+ message, source + "-->" + rsource2, rsource2==expectedResult, expectedResult);
    delete copy;

	/*creates a clone and tests transliteration*/
	JamoHangulTransliterator *clone=(JamoHangulTransliterator*)t.clone();
	rsource2.remove();
	rsource2.append(source);
	index=Transliterator::Position(start, limit, cursor);
	clone->handleTransliterate(rsource2, index, FALSE);
	expectAux(t.getID() + "CLONE:handleTransliterator(increment=FALSE) "+ message, source + "-->" + rsource2, rsource2==expectedResult, expectedResult);
    
	rsource2.remove();
	rsource2.append(source);
	index=Transliterator::Position(start, limit, cursor);
	clone->handleTransliterate(rsource2, index, TRUE);
	expectAux(t.getID() + "CLONE:handleTransliterator(increment=TRUE) "+ message, source + "-->" + rsource2, rsource2==expectedResult, expectedResult);
   
	/*Uses the assignment operator to create a transliterator and tests transliteration*/
	JamoHangulTransliterator equal=t;
	rsource2.remove();
	rsource2.append(source);
	index=Transliterator::Position(start, limit, cursor);
	equal.handleTransliterate(rsource2, index, FALSE);
	expectAux(t.getID() + "=OPERATOR:handleTransliterator(increment=FALSE) "+ message, source + "-->" + rsource2, rsource2==expectedResult, expectedResult);
    
	rsource2.remove();
	rsource2.append(source);
	index=Transliterator::Position(start, limit, cursor);
	equal.handleTransliterate(rsource2, index, TRUE);
	expectAux(t.getID() + "=OPERATOR:handleTransliterator(increment=TRUE) "+ message, source + "-->" + rsource2, rsource2==expectedResult, expectedResult);

}
void JamoToHangTransliteratorTest::expect(const JamoHangulTransliterator& t,
								 const UnicodeString& message,
                                const UnicodeString& source,
                                const UnicodeString& expectedResult) {
   
    UnicodeString rsource(source);
    t.transliterate(rsource);
    expectAux(t.getID() + ":Replaceable " + message, source + "->" + rsource, rsource==expectedResult, expectedResult);

	// Test handleTransliterate (incremental) transliteration -- 
    rsource.remove();
	rsource.append(source);
    Transliterator::Position index(0,source.length(),0);
	t.handleTransliterate(rsource, index, TRUE);
	expectAux(t.getID() + ":handleTransliterate " + message, source + "->" + rsource, rsource==expectedResult, expectedResult);

}
void JamoToHangTransliteratorTest::expectAux(const UnicodeString& tag,
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