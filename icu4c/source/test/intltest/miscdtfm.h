
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

#ifndef _DATEFORMATMISCTEST_
#define _DATEFORMATMISCTEST_
 
#include "unicode/utypes.h"
#include "intltest.h"

/** 
 * Performs miscellaneous tests for DateFormat, SimpleDateFormat, DateFormatSymbols
 **/
class DateFormatMiscTests : public IntlTest {    
    
    // IntlTest override
    void runIndexedTest( int32_t index, bool_t exec, char* &name, char* par );
public:

    void test4097450(void);
    void test4099975(void);
    void test4117335(void);

protected:
    bool_t failure(UErrorCode status, const char* msg);

};
 
#endif // _DATEFORMATMISCTEST_
//eof
