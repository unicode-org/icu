/*
*******************************************************************************
*   Copyright (C) 1997-2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*   Date        Name        Description
*   03/22/00    aliu        Creation.
*******************************************************************************
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

U_CAPI void
uhash_deleteUnicodeString(void *obj) {
    delete (UnicodeString*) obj;
}

U_CAPI bool_t
uhash_compareUnicodeString(const void *key1, const void *key2) {
    if (key1 == key2) {
        return TRUE;
    }
    if (key1 == NULL || key2 == NULL) {
        return FALSE;
    }
    return *((UnicodeString*) key1) == *((UnicodeString*) key2);
}
