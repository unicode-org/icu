/*
*******************************************************************************
*                                                                             *
* COPYRIGHT:                                                                  *
*   (C) Copyright International Business Machines Corporation, 1999           *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *
*   US Government Users Restricted Rights - Use, duplication, or disclosure   *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *
*                                                                             *
*******************************************************************************
*   file name:  strtest.cpp
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 1999nov22
*   created by: Markus W. Scherer
*/

#include <wchar.h>
#include "utypes.h"
#include "putil.h"
#include "intltest.h"
#include "strtest.h"

void StringTest::TestSizeofWCharT() {
    if(U_SIZEOF_WCHAR_T!=sizeof(wchar_t)) {
        errln("TestSizeofWCharT: U_SIZEOF_WCHAR_T!=sizeof(wchar_t) - U_SIZEOF_WCHAR_T needs to be fixed in platform.h");
    }
}

void StringTest::runIndexedTest(int32_t index, bool_t exec, char *&name, char *par) {
    if(exec) {
        logln("TestSuite Character and String Test: ");
    }
    switch(index) {
    case 0:
        name="TestSizeofWCharT";
        if(exec) {
            TestSizeofWCharT();
        }
        break;
    default:
        name="";
        break;
    }
}
