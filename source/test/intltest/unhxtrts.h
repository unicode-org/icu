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
*   Date         Name        Description
*   03/15/2000   Madhu        Creation.
************************************************************************/

#ifndef UNITOHEXTRTST_H
#define UNITOHEXTRTST_H

#include "unicode/utypes.h"
#include "unicode/translit.h"
#include "unicode/unitohex.h"
#include "intltest.h"

class UnicodeToHexTransliterator;

/**
 * @test
 * @summary General test of UnicodeToHexadecimal Transliterator
 */
class UniToHexTransliteratorTest : public IntlTest {
public:
    void runIndexedTest(int32_t index, bool_t exec, char* &name, char* par=NULL);

    /*Tests the constructors */
    void TestConstruction(void);
	/*Tests the function clone, and operator==()*/
	void TestCloneEqual(void);
    /*Tests the function isUppercase and setUppercase()*/
	void TestUpperCase(void);
	/*Tests the function getTransliterator() and setTransliterators() and adoptTransliterators()*/
	void TestPattern(void);
	 /*Tests the function handleTransliterate()*/
	void TestSimpleTransliterate();
    /*Tests the function handleTransliterate()*/
	void TestTransliterate();
	
    //======================================================================
    // Support methods
    //======================================================================
	void expectTranslit(const UnicodeToHexTransliterator& t,
						const UnicodeString& message,
												const UnicodeString& source, 
												int32_t start, int32_t limit, int32_t cursor,
												const UnicodeString& expectedResult);

	void expectPattern(UnicodeToHexTransliterator& t,
												const UnicodeString& pattern, 
												const UnicodeString& source, 
												const UnicodeString& expectedResult);
    
    void expect(const UnicodeToHexTransliterator& t,
				const UnicodeString& message,
                const UnicodeString& source,
                const UnicodeString& expectedResult);
      
    void expectAux(const UnicodeString& tag,
                   const UnicodeString& summary, bool_t pass,
                   const UnicodeString& expectedResult);


};

#endif




