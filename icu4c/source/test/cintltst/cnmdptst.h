/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
*
* File CNMDPTST.H
*
* Modification History:
*        Name                     Description            
*     Madhu Katragadda               Creation
*********************************************************************************
*/
/* C DEPTH TEST FOR NUMBER FORMAT */

#ifndef _CNUMDEPTST
#define _CNUMDEPTST

#include "cintltst.h"

/* The function used to test differnet format patterns*/
static void TestPatterns(void);

/*  Test the handling of quotes*/
static void TestQuotes(void);

/* Test patterns with exponential representation*/
static void TestExponential(void);

/* Test the handling of the currency symbol in patterns. */
static void TestCurrencySign(void); 

/* Test proper rounding by the format method.*/
static void TestRounding487(void);

/* Test localized currency patterns. */
static void TestCurrency(void);

/*Internal functions used*/
static void roundingTest(UNumberFormat*, double,  int32_t, const char*);

#endif
