#ifndef UCOL_IMP_H
#define UCOL_IMP_H

#include "unicode/ucol.h"
#include "unicode/tblcoll.h"

U_CFUNC uint8_t *ucol_getSortKeyWithAllocation(const UCollator *coll, 
        const    UChar        *source,
        int32_t            sourceLength, int32_t *resultLen);

int32_t
ucol_calcSortKey(const    UCollator    *coll,
        const    UChar        *source,
        int32_t        sourceLength,
        uint8_t        **result,
        int32_t        resultLength,
        UBool allocatePrimary);

#endif
