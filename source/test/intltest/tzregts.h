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

#ifndef _TIMEZONEREGRESSIONTEST_
#define _TIMEZONEREGRESSIONTEST_
 
#include "utypes.h"
#include "timezone.h"

#include "intltest.h"

#include "gregocal.h"

/** 
 * Performs regression test for Calendar
 **/
class TimeZoneRegressionTest: public IntlTest {    
    
    // IntlTest override
    void runIndexedTest( int32_t index, bool_t exec, char* &name, char* par );
public:
    
    void Test4052967(void);
    void Test4073209(void);
    void Test4073215(void);
    void Test4084933(void);
    void Test4096952(void);
    void Test4109314(void);
    void Test4126678(void);
    void Test4151406(void);
    void Test4151429(void);
    void Test4154537(void);
    void Test4154542(void);
    void Test4154650(void);
    void Test4154525(void);
    void Test4162593(void);

    void TestJDK12API(void);

    bool_t checkCalendar314(GregorianCalendar *testCal, TimeZone *testTZ);


protected:
    const char* errorName(UErrorCode code);
    bool_t failure(UErrorCode status, const char* msg);
};

 
#endif // _CALENDARREGRESSIONTEST_
//eof
