/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 2000, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/************************************************************************
*   Date        Name        Description
*   1/03/2000   Madhu        Creation.
************************************************************************/

#ifndef TRANSAPI_H
#define TRANSAPI_H

#include "unicode/utypes.h"
#include "unicode/translit.h"
#include "intltest.h"

class Transliterator;

/**
 * @test
 * @summary General test of Transliterator
 */
class TransliteratorAPITest : public IntlTest {
public:
    void runIndexedTest(int32_t index, UBool exec, char* &name, char* par=NULL);

    /*Tests the function getDisplayName() */
    void TestGetDisplayName();
    
	void TestgetID();
	
	void TestgetInverse();

	void TestClone();
	
	void TestTransliterate1();
	
	void TestTransliterate2();

	void TestTransliterate3();

	void TestSimpleKeyboardTransliterator();

	void TestKeyboardTransliterator1();

	void TestKeyboardTransliterator2();

	void TestKeyboardTransliterator3();

	void TestGetAdoptFilter();

    void TestNullTransliterator();

	/*Internal functions used*/
	void doTest(const UnicodeString& , const UnicodeString& , const UnicodeString& );

	void keyboardAux(Transliterator*, UnicodeString[] , UnicodeString&, int32_t, int32_t);
     
    void displayOutput(const UnicodeString&, const UnicodeString&, UnicodeString&,
                       UTransPosition&);

};

#endif




