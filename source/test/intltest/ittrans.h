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
*   12/14/99    Madhu        Creation.
************************************************************************/
/**
 * IntlTestRBBI is the top level test class for the RuleBasedBreakIterator tests
 */

#ifndef INTLTRANSLIT_H
#define INTLTRANSLIT_H


#include "unicode/utypes.h"
#include "intltest.h"


class IntlTestTransliterator: public IntlTest {
public:
    void runIndexedTest( int32_t index, bool_t exec, char* &name, char* par = NULL );
};


#endif


