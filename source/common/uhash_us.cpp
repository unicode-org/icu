/*
******************************************************************************
*   Copyright (C) 1997-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
******************************************************************************
*   Date        Name        Description
*   03/22/00    aliu        Creation.
******************************************************************************
*/

#include "uhash.h"
#include "unicode/unistr.h"

/********************************************************************
 * PUBLIC UnicodeString support functions for UHashtable
 ********************************************************************/

U_CAPI int32_t
uhash_hashUnicodeString(const void *key) {
    return (key == NULL) ? 0 : ((UnicodeString*)key)->hashCode();
}

U_CAPI int32_t
uhash_hashCaselessUnicodeString(const void *key) {
    if (key == NULL) {
        return 0;
    }
    // Inefficient; a better way would be to have a hash function in
    // UnicodeString that does case folding on the fly.
    UnicodeString copy(*(UnicodeString*) key);
    return copy.foldCase().hashCode();
}

U_CAPI void
uhash_deleteUnicodeString(void *obj) {
    delete (UnicodeString*) obj;
}

U_CAPI UBool
uhash_compareUnicodeString(const void *key1, const void *key2) {
    if (key1 == key2) {
        return TRUE;
    }
    if (key1 == NULL || key2 == NULL) {
        return FALSE;
    }
    return *((UnicodeString*) key1) == *((UnicodeString*) key2);
}

U_CAPI UBool
uhash_compareCaselessUnicodeString(const void *key1, const void *key2) {
    if (key1 == key2) {
        return TRUE;
    }
    if (key1 == NULL || key2 == NULL) {
        return FALSE;
    }
    return ((UnicodeString*) key1)->caseCompare(*(UnicodeString*)key2,
                                                U_FOLD_CASE_DEFAULT) == 0;
}
