/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2001, International Business Machines Corporation and
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

/* cstrcase.c */
U_CFUNC void TestCaseLower(void);
U_CFUNC void TestCaseUpper(void);
U_CFUNC void TestCaseTitle(void);
U_CFUNC void TestCaseFolding(void);
U_CFUNC void TestCaseCompare(void);

#endif
