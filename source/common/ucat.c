/*
**********************************************************************
* Copyright (c) 2003, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Alan Liu
* Created: March 19 2003
* Since: ICU 2.6
**********************************************************************
*/
#include "unicode/ucat.h"
#include "unicode/ustring.h"
#include "cstring.h"
#include "uassert.h"

/* set_num key prefix string. msg_num key is simply a number. */
static const char SET_KEY_PREFIX[] = "%cat%";

/* length of SET_KEY_PREFIX */
#define SET_KEY_PREFIX_LEN 5

/* Maximum length of a set_num/msg_num key, incl. terminating zero */
#define MAX_KEY_LEN (SET_KEY_PREFIX_LEN+11)

/**
 * Fill in buffer with a set_num/msg_num key string, given the numeric
 * value. Numeric value must be >= 0. Buffer must be of length
 * MAX_KEY_LEN or more. If isSet is true, then a set_num key is
 * created; otherwise a msg_num key is created.
 */
static char*
_catkey(char* buffer, int32_t num, UBool isSet) {
    int32_t i = 0;
    U_ASSERT(num>=0);
    if (isSet) {
        uprv_strcpy(buffer, SET_KEY_PREFIX);
        i = SET_KEY_PREFIX_LEN;
    }
    T_CString_integerToString(buffer + i, num, 10);
    return buffer;
}

U_CAPI u_nl_catd U_EXPORT2
u_catopen(const char* name, const char* locale, UErrorCode* ec) {
    return (u_nl_catd) ures_open(name, locale, ec);
}

U_CAPI void U_EXPORT2
u_catclose(u_nl_catd catd) {
    ures_close((UResourceBundle*) catd); /* may be NULL */
}

U_CAPI const UChar* U_EXPORT2
u_catgets(u_nl_catd catd, int32_t set_num, int32_t msg_num,
          const UChar* s,
          int32_t* len, UErrorCode* ec) {

    UResourceBundle* res;
    char key[MAX_KEY_LEN];
    const UChar* result;

    if (ec == NULL || U_FAILURE(*ec)) {
        goto ERROR;
    }

    res = ures_getByKey((const UResourceBundle*) catd,
                        _catkey(key, set_num, TRUE),
                        NULL, ec);
    if (U_FAILURE(*ec)) {
        goto ERROR;
    }

    result = ures_getStringByKey(res,
                                 _catkey(key, msg_num, FALSE),
                                 len, ec);
    ures_close(res);

    if (U_FAILURE(*ec)) {
        goto ERROR;
    }

    return result;

 ERROR:
    /* In case of any failure, return s */
    if (len != NULL) {
        *len = u_strlen(s);
    }
    return s;
}

/*eof*/
