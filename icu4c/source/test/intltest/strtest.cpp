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

void StringTest::TestEndian() {
    union {
        uint8_t byte;
        uint16_t word;
    } u;
    u.word=0x0100;
    if(U_IS_BIG_ENDIAN!=u.byte) {
        errln("TestEndian: U_IS_BIG_ENDIAN needs to be fixed in platform.h");
    }
}

void StringTest::TestSizeofWCharT() {
    if(U_SIZEOF_WCHAR_T!=sizeof(wchar_t)) {
        errln("TestSizeofWCharT: U_SIZEOF_WCHAR_T!=sizeof(wchar_t) - U_SIZEOF_WCHAR_T needs to be fixed in platform.h");
    }
}

void StringTest::TestCharsetFamily() {
    unsigned char c='A';
    if( U_CHARSET_FAMILY==U_ASCII_FAMILY && c!=0x41 ||
        U_CHARSET_FAMILY==U_EBCDIC_FAMILY && c!=0xc1
    ) {
        errln("TestCharsetFamily: U_CHARSET_FAMILY needs to be fixed in platform.h");
    }
}

void StringTest::runIndexedTest(int32_t index, bool_t exec, char *&name, char *par) {
    if(exec) {
        logln("TestSuite Character and String Test: ");
    }
    switch(index) {
    case 0:
        name="TestEndian";
        if(exec) {
            TestEndian();
        }
        break;
    case 1:
        name="TestSizeofWCharT";
        if(exec) {
            TestSizeofWCharT();
        }
        break;
    case 2:
        name="TestCharsetFamily";
        if(exec) {
            TestCharsetFamily();
        }
        break;
    default:
        name="";
        break;
    }
}
