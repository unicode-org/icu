/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 2005, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/*
* File utexttst.c
*
* Modification History:
*
*   Date          Name               Description
*   06/13/2005    Andy Heninger      Creation 
*******************************************************************************
*/

#include "unicode/utypes.h"
#include "unicode/utext.h"
#include "cintltst.h"



static void TestAPI(void);

void
addUTextTest(TestNode** root)
{
  addTest(root, &TestAPI           ,    "tsutil/UTextTest/TestAPI");
}


#define TEST_ASSERT(x) \
   {if ((x)==FALSE) {log_err("Test failure in file %s at line %d\n", __FILE__, __LINE__);\
                     gFailed = TRUE;\
   }}


#define TEST_SUCCESS(status) \
   {if (U_FAILURE(status)) {log_err("Test failure in file %s at line %d. Error = \"%s\"\n", \
       __FILE__, __LINE__, u_errorName(status)); \
       gFailed = TRUE;\
   }}



/*
 *  TestAPI   verify that the UText API is accessible from C programs.
 *            This is not intended to be a complete test of the API functionality.  That is
 *             in the C++ intltest program.
 *            This test is intended to check that everything can be accessed and built in 
 *            a pure C enviornment.
 */


static void TestAPI(void) {
    UErrorCode      status = U_ZERO_ERROR;
    UBool           gFailed = FALSE;

    UText           utLoc = UTEXT_INITIALIZER;
    const char *    cString = "Hello, World";
    UChar   uString[]  = {0x41, 0x42, 0x43, 0};
    uint8_t        *utf8String;
    UText          *uta;
    UText          *utb;

    utf8String = (uint8_t *)cString;
    uta = utext_openUTF8(&utLoc, utf8String, -1, &status);
    TEST_SUCCESS(status);
    TEST_ASSERT(uta == &utLoc);

    uta = utext_close(&utLoc);
    TEST_ASSERT(uta == &utLoc);

}