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
*   file name:  strtest.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 1999nov22
*   created by: Markus W. Scherer
*/

/*
 * Test character- and string- related settings in utypes.h,
 * macros in putil.h, and constructors in unistr.h .
 */

#ifndef __STRTEST_H__
#define __STRTEST_H__

#include "utypes.h"
#include "intltest.h"

class StringTest : public IntlTest {
public:
    StringTest() {}
    ~StringTest() {}

    void runIndexedTest(int32_t index, bool_t exec, char *&name, char *par=NULL);

private:
    void TestEndian(void);
    void TestSizeofWCharT(void);
    void TestCharsetFamily(void);
};

#endif
