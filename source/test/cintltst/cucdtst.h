/*
*****************************************************************************************
*                                                                                       *
* COPYRIGHT:                                                                            *
*   (C) Copyright Taligent, Inc.,  1996                                                 *
*   (C) Copyright International Business Machines Corporation,  1999                    *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.                  *
*   US Government Users Restricted Rights - Use, duplication, or disclosure             *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                              *
*                                                                                       *
*****************************************************************************************
********************************************************************************
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



static void addUnicodeTests(TestNode**);


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
#endif
