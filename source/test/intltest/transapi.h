/*
*****************************************************************************************
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
    void runIndexedTest(int32_t index, bool_t exec, char* &name, char* par=NULL);

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

	/*Internal functions used*/
	void doTest(const UnicodeString& , const UnicodeString& , const UnicodeString& );

	void keyboardAux(Transliterator*, UnicodeString[] , UnicodeString&, int32_t, int32_t);
     
    void displayOutput(const UnicodeString&, const UnicodeString&, UnicodeString&,
                       Transliterator::Position&);

};

#endif




