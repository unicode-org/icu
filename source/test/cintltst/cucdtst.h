/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
*
* File CUCDTST.H
*
* Modification History:
*        Name                     Description 
*     Madhu Katragadda            Converted to C, added tests for string functions
*********************************************************************************
*/
/* C API TEST For Unicode */

#ifndef _CUCDTST
#define _CUCDTST


static void setUpDataTable(void);
static void cleanUpDataTable(void);

static void TestUpperLower(void);
static void TestLetterNumber(void);
static void TestMisc(void);
static void TestControlPrint(void);
static void TestIdentifier(void);
static void TestUnicodeData(void);
static void TestStringCopy(void);
static void TestStringFunctions(void);
static void TestStringSearching(void);
static void TestCharNames(void);
static void TestMirroring(void);
static void TestUnescape(void);
static void TestCaseMapping(void);

/* internal methods used */
static int32_t MakeProp(char* str);
static int32_t MakeDir(char* str);

#endif
