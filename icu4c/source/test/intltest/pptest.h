
/*
********************************************************************
* COPYRIGHT: 
* (C) Copyright International Business Machines Corporation, 1998
* Licensed Material - Program-Property of IBM - All Rights Reserved. 
* US Government Users Restricted Rights - Use, duplication, or disclosure 
* restricted by GSA ADP Schedule Contract with IBM Corp. 
*
********************************************************************
*/

#ifndef _PARSEPOSITIONIONTEST_
#define _PARSEPOSITIONIONTEST_
 
#include "utypes.h"
#include "intltest.h"


/** 
 * Performs test for ParsePosition
 **/
class ParsePositionTest: public IntlTest {
    // IntlTest override
    void runIndexedTest( int32_t index, bool_t exec, char* &name, char* par );
public:

    void TestParsePosition(void);
    void TestFieldPosition(void);
    void TestFieldPosition_example(void);
    void Test4109023(void);

protected:
    bool_t failure(UErrorCode status, const char* msg);
};
 
#endif // _PARSEPOSITIONIONTEST_
//eof
