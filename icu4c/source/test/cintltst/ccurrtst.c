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
* File CCURRTST.C
*
* Modification History:
*        Name                     Description            
*     Madhu Katragadda             Ported for C API
*********************************************************************************
*/

#include "utypes.h"
#include "ucol.h"
#include "uloc.h"
#include "cintltst.h"
#include "ccurrtst.h"
#include "ccolltst.h"
#include "ustring.h"
#include <memory.h>

#define ARRAY_LENGTH(array) (sizeof array / sizeof array[0]) 

void addCurrencyTest(TestNode** root)
{
    
    addTest(root, &currTest, "tscoll/ccurrtst/currTest");
    
}


void currTest()
{
    /* All the currency symbols, in collation order*/
    static const UChar currency[][2] =
    {
        { 0x00a4, 0x0000}, /* generic currency*/
        { 0x0e3f, 0x0000}, /* baht*/
        { 0x00a2, 0x0000}, /* cent*/
        { 0x20a1, 0x0000}, /* colon*/
        { 0x20a2, 0x0000}, /* cruzeiro*/
        { 0x0024, 0x0000}, /* dollar */
        { 0x20ab, 0x0000}, /* dong */
        { 0x20ac, 0x0000}, /* euro */
        { 0x20a3, 0x0000}, /* franc */
        { 0x20a4, 0x0000}, /* lira */
        { 0x20a5, 0x0000}, /* mill */
        { 0x20a6, 0x0000}, /* naira */
        { 0x20a7, 0x0000}, /* peseta */
        { 0x00a3, 0x0000}, /* pound */
        { 0x20a8, 0x0000}, /* rupee */
        { 0x20aa, 0x0000}, /* shekel*/
        { 0x20a9, 0x0000}, /* won*/
        { 0x00a5, 0x0000}  /* yen*/
    };
    UChar source[2], target[2];
    int32_t i, j, sortklen;
    int res;
    UCollator *c;
    uint8_t *sortKey1, *sortKey2;
    UErrorCode status = U_ZERO_ERROR;
    UCollationResult compareResult, keyResult;
    UCollationResult expectedResult = UCOL_EQUAL;
    log_verbose("Testing currency of all locales\n");
    c = ucol_open(NULL, &status);
    if (U_FAILURE(status))
    {
        log_err("collator open failed! :%s\n", myErrorName(status));
        return;
    }

    /*Compare each currency symbol against all the
     currency symbols, including itself*/
    for (i = 0; i < ARRAY_LENGTH(currency); i += 1)
    {
        for (j = 0; j < ARRAY_LENGTH(currency); j += 1)
        {
             u_strcpy(source, currency[i]);
             u_strcpy(target, currency[j]);
            
            if (i < j)
            {
                expectedResult = UCOL_LESS;
            }
            else if ( i == j)
            {
                expectedResult = UCOL_EQUAL;
            }
            else
            {
                expectedResult = UCOL_GREATER;
            }

            compareResult = ucol_strcoll(c, source, u_strlen(source), target, u_strlen(target));

            
            status = U_ZERO_ERROR;

            sortklen=ucol_getSortKey(c, source, u_strlen(source),  NULL, 0);
            sortKey1=(uint8_t*)malloc(sizeof(uint8_t) * (sortklen+1));
            ucol_getSortKey(c, source, u_strlen(source), sortKey1, sortklen+1);
    
            sortklen=ucol_getSortKey(c, target, u_strlen(target),  NULL, 0);
            sortKey2=(uint8_t*)malloc(sizeof(uint8_t) * (sortklen+1));
            ucol_getSortKey(c, target, u_strlen(target), sortKey2, sortklen+1);
    
            res = memcmp(sortKey1, sortKey2, sortklen);
            if (res < 0) keyResult = -1;
            else if (res > 0) keyResult = 1;
            else keyResult = 0;
            
            reportCResult(source, target, sortKey1, sortKey2,
                          compareResult, keyResult, expectedResult);

        }
    }
}



