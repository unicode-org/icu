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
*   03/09/2000   Madhu        Creation.
************************************************************************/

#ifndef CPDTRTST_H
#define CPDTRTST_H

#include "unicode/utypes.h"
#include "unicode/translit.h"
#include "unicode/cpdtrans.h"
#include "intltest.h"

class CompoundTransliterator;

/**
 * @test
 * @summary General test of Compound Transliterator
 */
class CompoundTransliteratorTest : public IntlTest {
public:
    void runIndexedTest(int32_t index, bool_t exec, char* &name, char* par=NULL);

    /*Tests the constructors */
    void TestConstruction(void);
	/*Tests the function clone, and operator==()*/
	void TestCloneEqual(void);
    /*Tests the function getCount()*/
	void TestGetCount(void);
	/*Tests the function getTransliterator() and setTransliterators() and adoptTransliterators()*/
	void TestGetSetAdoptTransliterator(void);
    /*Tests the function handleTransliterate()*/
	void TestTransliterate(void);
	
    //======================================================================
    // Support methods
    //======================================================================

	/**
	 * Splits a UnicodeString
	 */
	UnicodeString* CompoundTransliteratorTest::split(const UnicodeString& str, UChar seperator, int32_t& count);
    
    void expect(const CompoundTransliterator& t,
                const UnicodeString& source,
                const UnicodeString& expectedResult);
      
    void expectAux(const UnicodeString& tag,
                   const UnicodeString& summary, bool_t pass,
                   const UnicodeString& expectedResult);


};

#endif




