/*
******************************************************************************
*   Copyright (C) 1997-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
******************************************************************************
*   Date        Name        Description
*   03/22/00    aliu        Creation.
*   07/06/01    aliu        Modified to support int32_t keys on
*                           platforms with sizeof(void*) < 32.
******************************************************************************
*/

#include "uhash.h"
#include "unicode/unistr.h"

/********************************************************************
 * PUBLIC UnicodeString support functions for UHashtable
 ********************************************************************/

U_CAPI int32_t
uhash_hashUnicodeString(const UHashKey key) {
    const UnicodeString *str = (const UnicodeString*) key.pointer;
    return (str == NULL) ? 0 : str->hashCode();
}

U_CAPI int32_t
uhash_hashCaselessUnicodeString(const UHashKey key) {
    const UnicodeString *str = (const UnicodeString*) key.pointer;
    if (str == NULL) {
        return 0;
    }
    // Inefficient; a better way would be to have a hash function in
    // UnicodeString that does case folding on the fly.
    UnicodeString copy(*str);
    return copy.foldCase().hashCode();
}

U_CAPI void
uhash_deleteUnicodeString(void *obj) {
    delete (UnicodeString*) obj;
}

U_CAPI UBool
uhash_compareUnicodeString(const UHashKey key1, const UHashKey key2) {
    const UnicodeString *str1 = (const UnicodeString*) key1.pointer;
    const UnicodeString *str2 = (const UnicodeString*) key2.pointer;
    if (str1 == str2) {
        return TRUE;
    }
    if (str1 == NULL || str2 == NULL) {
        return FALSE;
    }
    return *str1 == *str2;
}

U_CAPI UBool
uhash_compareCaselessUnicodeString(const UHashKey key1, const UHashKey key2) {
    const UnicodeString *str1 = (const UnicodeString*) key1.pointer;
    const UnicodeString *str2 = (const UnicodeString*) key2.pointer;
    if (str1 == str2) {
        return TRUE;
    }
    if (str1 == NULL || str2 == NULL) {
        return FALSE;
    }
    return str1->caseCompare(*str2, U_FOLD_CASE_DEFAULT) == 0;
}
