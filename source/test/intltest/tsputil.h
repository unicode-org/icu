/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/


#ifndef _PUTILTEST_
#define _PUTILTEST_

#include "intltest.h"

/** 
 * Test putil.h
 **/
class PUtilTest : public IntlTest {
    // IntlTest override
    void runIndexedTest( int32_t index, UBool exec, const char* &name, char* par );
public:

//    void testIEEEremainder(void);
    void testMaxMin(void);

private:
//    void remainderTest(double x, double y, double exp);
    void maxMinTest(double a, double b, double exp, UBool max);

};
 
#endif
//eof
