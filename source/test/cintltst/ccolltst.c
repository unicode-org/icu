/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
*
* File CCOLLTST.C
*
* Modification History:
*        Name                     Description            
*     Madhu Katragadda               Creation
*********************************************************************************
*/
#include "cintltst.h"
#include "ccolltst.h"
#include "unicode/ucol.h"
#include <string.h>
#include "unicode/ustring.h"
#include <stdio.h>

void addCollAPITest(TestNode**);
void addCurrencyTest(TestNode**);
void addNormTest(TestNode**);
void addDanishCollTest(TestNode**);
void addGermanCollTest(TestNode**);
void addSpanishCollTest(TestNode**);
void addFrenchCollTest(TestNode**);
void addKannaCollTest(TestNode**);
void addTurkishCollTest(TestNode**);
void addEnglishCollTest(TestNode**);
void addAllCollTest(TestNode**);

void addRuleBasedCollTest(TestNode**);
void addCollIterTest(TestNode**);

void addCollTest(TestNode** root)
{
    addCollAPITest(root);
    addCurrencyTest(root);
    addNormTest(root);
    addDanishCollTest(root);
    addGermanCollTest(root);
    addSpanishCollTest(root);
    addFrenchCollTest(root);
    addKannaCollTest(root);
    addTurkishCollTest(root);
    addEnglishCollTest(root);
    

    addRuleBasedCollTest(root);
    addCollIterTest(root);
    addAllCollTest(root);
}



/*Internal functions used*/

void reportCResult( const UChar source[], const UChar target[], 
                         uint8_t *sourceKey, uint8_t *targetKey,
                         UCollationResult compareResult,
                         UCollationResult keyResult,
                         UCollationResult expectedResult )
{
    UChar *sResult, *sExpect;
    sResult=(UChar*)malloc(sizeof(UChar) * 10);
    sExpect=(UChar*)malloc(sizeof(UChar) * 10);
    if (expectedResult < -1 || expectedResult > 1)
    {
        log_err("***** invalid call to reportCResult ****\n");
        return;
    }

    if (compareResult != expectedResult)
    {
        
        appendCompareResult(compareResult, sResult);
        appendCompareResult(expectedResult, sExpect);
        log_err("Compare(%s , %s) returned: %s expected: %s\n", austrdup(source), austrdup(target),
            austrdup(sResult), austrdup(sExpect) );
    }

    if (keyResult != expectedResult)
    {
    
        appendCompareResult(keyResult, sResult);
        appendCompareResult(expectedResult, sExpect);

        log_err("KeyCompare(%s , %s) returned: %s expected: %s\n", austrdup(source), austrdup(target), 
            austrdup(sResult), austrdup(sExpect) );

    
    }
    free(sExpect);
    free(sResult);
}

UChar* appendCompareResult(UCollationResult result, UChar* target)
{
    if (result == UCOL_LESS)
    {
        u_uastrcpy(target, "LESS");
    }
    else if (result == UCOL_EQUAL)
    {
        u_uastrcpy(target, "EQUAL");
    }
    else if (result == UCOL_GREATER)
    {
        u_uastrcpy(target, "GREATER");
    }
    else
    {
        u_uastrcpy(target, "huh???");
            
    }

    return target;
}

UChar* CharsToUChars(const char* chars)
{
    int unicode;
    int i;
    UChar *buffer;
    UChar *alias;
    int count = 0;

    /* preflight */
    for (i = 0; chars[i] != 0;) {
        if ((chars[i] == '\\') && (chars[i+1] == 'u')) {
            i += 6;
        } else {
            i++;
        }
        ++count;
    }

    alias = buffer = (UChar*) malloc(sizeof(UChar) * (count + 1));
    
    for (;;) {
        /* search for \u or the end */
        for(i = 0; chars[i] != 0 && !(chars[i] == '\\' && chars[i+1] == 'u'); ++i) {}

        /* convert characters between escape sequences */
        if(i > 0) {
            u_charsToUChars(chars, alias, i);
            chars += i;
            alias += i;
        }

        /* did we reach the end or an escape sequence? */
        if(*chars == 0) {
            break;
        }

        /* unescape one character: we know that there is a \u sequence at chars[limit] */
        chars += 2;
        sscanf(chars, "%4X", &unicode);
        *alias++ = (UChar)unicode;
        chars += 4;
    }
    *alias = 0x0000;
    return buffer;
}
