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
*   03/22/2000   Madhu        Creation.
************************************************************************/

#ifndef UNIFLTLOGICTST_H
#define UNIFLTLOGICTST_H

#include "unicode/utypes.h"
#include "unicode/translit.h"
#include "unicode/unifltlg.h"
#include "intltest.h"

class UnicodeFilterLogic;

/**
 * @test
 * @summary General test of UnicodeFilterLogic API
 */
class UnicodeFilterLogicTest : public IntlTest {
public:
    void runIndexedTest(int32_t index, bool_t exec, char* &name, char* par=NULL);

    /*Tests all the NOT, OR and AND filters */
    void TestAll(void);

	void TestNOT(Transliterator& t,
				 const UnicodeFilter* f1, 
				 const UnicodeString& message,
				 const UnicodeString& source,
				 const UnicodeString& expected);

	void TestAND(Transliterator& t,
		         const UnicodeFilter* f1,
				 const UnicodeFilter* f2,
				 const UnicodeString& message,
				 const UnicodeString& source,
				 const UnicodeString& expected);
	
	void TestOR(Transliterator& t,
		         const UnicodeFilter* f1,
				 const UnicodeFilter* f2,
				 const UnicodeString& message,
				 const UnicodeString& source,
				 const UnicodeString& expected);

	//support functions
	void expect(const Transliterator& t,
								 const UnicodeString& message,
                                const UnicodeString& source,
                                const UnicodeString& expectedResult);

	void expectAux(const UnicodeString& tag,
                                   const UnicodeString& summary, bool_t pass,
                                   const UnicodeString& expectedResult); 

};

#endif
