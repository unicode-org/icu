/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2003, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
*
* File CALLCOLL.H
*
* Modification History:
*        Name                     Description            
*     Madhu Katragadda              Ported to C
*********************************************************************************
*/
/**
 * CollationDummyTest is a third level test class.  This tests creation of 
 * a customized collator object.  For example, number 1 to be sorted 
 * equlivalent to word 'one'.
 */
#ifndef _CALLCOLLTST
#define _CALLCOLLTST

#include "unicode/utypes.h"
#include "unicode/ucoleitr.h"

#if !UCONFIG_NO_COLLATION

#include "cintltst.h"

#define RULE_BUFFER_LEN 8192


    /* tests comparison of custom collation with different strengths */
void doTest(UCollator*, const UChar* source, const UChar* target, UCollationResult result);
/* verify that iterating forward and backwards over the string yields same CEs */
void backAndForth(UCollationElements *iter);
/* gets an array of CEs for a string in UCollationElements iterator. */
int32_t* getOrders(UCollationElements *iter, int32_t *orderLength);

void genericOrderingTestWithResult(UCollator *coll, const char *s[], uint32_t size, UCollationResult result);
void genericOrderingTest(UCollator *coll, const char *s[], uint32_t size);
void genericLocaleStarter(const char *locale, const char *s[], uint32_t size);
void genericLocaleStarterWithResult(const char *locale, const char *s[], uint32_t size, UCollationResult result);
void genericLocaleStarterWithOptions(const char *locale, const char *s[], uint32_t size, const UColAttribute *attrs, const UColAttributeValue *values, uint32_t attsize);
void genericRulesTestWithResult(const char *rules, const char *s[], uint32_t size, UCollationResult result);
void genericRulesStarter(const char *rules, const char *s[], uint32_t size);
UBool hasCollationElements(const char *locName);


#endif /* #if !UCONFIG_NO_COLLATION */

#endif
