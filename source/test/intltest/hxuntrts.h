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
*   03/17/2000   Madhu        Creation.
************************************************************************/

#ifndef HEXTOUNITRTST_H
#define HEXTOUNITRTST_H

#include "unicode/utypes.h"
#include "unicode/translit.h"
#include "unicode/hextouni.h"
#include "intltest.h"

class HexToUnicodeTransliterator;

/**
 * @test
 * @summary General test of HexadecimalToUnicodeTransliterator
 */
class HexToUniTransliteratorTest : public IntlTest {
public:
    void runIndexedTest(int32_t index, UBool exec, char* &name, char* par=NULL);

    /*Tests the constructors */
    void TestConstruction(void);
	/*Tests the function clone, and operator==()*/
	void TestCloneEqual(void);
   	/*Tests the function getTransliterator() and setTransliterators() and adoptTransliterators()*/
	void TestPattern(void);
	 /*Tests the function handleTransliterate()*/
	void TestSimpleTransliterate(void);
    /*Tests the function handleTransliterate()*/
	void TestTransliterate(void);
	
    //======================================================================
    // Support methods
    //======================================================================
	void expectTranslit(const HexToUnicodeTransliterator& t,
						const UnicodeString& message,
												const UnicodeString& source, 
												int32_t start, int32_t limit, int32_t cursor,
												const UnicodeString& expectedResult);

	void expectPattern(HexToUnicodeTransliterator& t,
												const UnicodeString& pattern, 
												const UnicodeString& source, 
												const UnicodeString& expectedResult);
    
    void expect(const HexToUnicodeTransliterator& t,
				const UnicodeString& message,
                const UnicodeString& source,
                const UnicodeString& expectedResult);
      
    void expectAux(const UnicodeString& tag,
                   const UnicodeString& summary, UBool pass,
                   const UnicodeString& expectedResult);


};

#endif


