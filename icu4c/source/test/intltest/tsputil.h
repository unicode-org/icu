/*
********************************************************************
* COPYRIGHT: 
* (C) Copyright International Business Machines Corporation 1998
* Licensed Material - Program-Property of IBM - All Rights Reserved. 
* US Government Users Restricted Rights - Use, duplication, or disclosure 
* restricted by GSA ADP Schedule Contract with IBM Corp. 
*
********************************************************************
*/

#ifndef _PUTILTEST_
#define _PUTILTEST_

#include "unicode/utypes.h"
#include "intltest.h"

/** 
 * Test putil.h
 **/
class PUtilTest : public IntlTest {
    // IntlTest override
    void runIndexedTest( int32_t index, bool_t exec, char* &name, char* par );
public:

    void testIEEEremainder(void);
    void testMaxMin(void);

private:
    void remainderTest(double x, double y, double exp);
    void maxMinTest(double a, double b, double exp, bool_t max);

};
 
#endif
//eof
