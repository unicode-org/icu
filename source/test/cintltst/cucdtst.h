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
#define _CUNDTST



static void TestUpperLower(void);
static void TestLetterNumber(void);
static void TestMisc(void);
static void TestControlPrint(void);
static void TestIdentifier(void);
static void TestUnicodeData(void);
static void TestStringFunctions(void);

/* internal methods used */
int32_t MakeProp(char* str);
int32_t MakeDir(char* str);

void setUpDataTable(void);
U_CFUNC void cleanUpDataTable(void);
#endif
