/*
*******************************************************************************
*
*   Copyright (C) 2002, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  cstrcase.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2002feb21
*   created by: Markus W. Scherer
*
*   Test file for string casing C API functions.
*/

#include "unicode/utypes.h"
#include "unicode/uchar.h"
#include "unicode/ustring.h"
#include "unicode/uloc.h"
#include "unicode/ubrk.h"
#include "cmemory.h"
#include "cintltst.h"
#include "cucdtst.h"

/* test string case mapping functions --------------------------------------- */

U_CFUNC void
TestCaseLower() {
    static const UChar

    beforeLower[]= { 0x61, 0x42, 0x49,  0x3a3, 0xdf, 0x3a3, 0x2f, 0xd93f, 0xdfff },
    lowerRoot[]=   { 0x61, 0x62, 0x69,  0x3c3, 0xdf, 0x3c2, 0x2f, 0xd93f, 0xdfff },
    lowerTurkish[]={ 0x61, 0x62, 0x131, 0x3c3, 0xdf, 0x3c2, 0x2f, 0xd93f, 0xdfff };

    UChar buffer[32];
    int32_t length;
    UErrorCode errorCode;

    /* lowercase with root locale and separate buffers */
    buffer[0]=0xabcd;
    errorCode=U_ZERO_ERROR;
    length=u_strToLower(buffer, sizeof(buffer)/U_SIZEOF_UCHAR,
                        beforeLower, sizeof(beforeLower)/U_SIZEOF_UCHAR,
                        "",
                        &errorCode);
    if( U_FAILURE(errorCode) ||
        length!=(sizeof(lowerRoot)/U_SIZEOF_UCHAR) ||
        uprv_memcmp(lowerRoot, buffer, length*U_SIZEOF_UCHAR)!=0 ||
        buffer[length]!=0
    ) {
        log_err("error in u_strToLower(root locale)=%ld error=%s string matches: %s\t\nlowerRoot=%s\t\nbuffer=%s\n",
            length,
            u_errorName(errorCode),
            uprv_memcmp(lowerRoot, buffer, length*U_SIZEOF_UCHAR)==0 &&
buffer[length]==0 ? "yes" : "no",
            aescstrdup(lowerRoot,-1),
            aescstrdup(buffer,-1));
    }

    /* lowercase with turkish locale and in the same buffer */
    uprv_memcpy(buffer, beforeLower, sizeof(beforeLower));
    buffer[sizeof(beforeLower)/U_SIZEOF_UCHAR]=0;
    errorCode=U_ZERO_ERROR;
    length=u_strToLower(buffer, sizeof(buffer)/U_SIZEOF_UCHAR,
                        buffer, -1, /* implicit srcLength */
                        "tr",
                        &errorCode);
    if( U_FAILURE(errorCode) ||
        length!=(sizeof(lowerTurkish)/U_SIZEOF_UCHAR) ||
        uprv_memcmp(lowerTurkish, buffer, length*U_SIZEOF_UCHAR)!=0 ||
        buffer[length]!=0
    ) {
        log_err("error in u_strToLower(turkish locale)=%ld error=%s string matches: %s\n",
            length,
            u_errorName(errorCode),
            uprv_memcmp(lowerTurkish, buffer, length*U_SIZEOF_UCHAR)==0 && buffer[length]==0 ? "yes" : "no");
    }

    /* test preflighting */
    buffer[0]=buffer[2]=0xabcd;
    errorCode=U_ZERO_ERROR;
    length=u_strToLower(buffer, 2, /* set destCapacity=2 */
                        beforeLower, sizeof(beforeLower)/U_SIZEOF_UCHAR,
                        "",
                        &errorCode);
    if( errorCode!=U_BUFFER_OVERFLOW_ERROR ||
        length!=(sizeof(lowerRoot)/U_SIZEOF_UCHAR) ||
        uprv_memcmp(lowerRoot, buffer, 2*U_SIZEOF_UCHAR)!=0 ||
        buffer[2]!=0xabcd
    ) {
        log_err("error in u_strToLower(root locale preflighting)=%ld error=%s string matches: %s\n",
            length,
            u_errorName(errorCode),
            uprv_memcmp(lowerRoot, buffer, 2*U_SIZEOF_UCHAR)==0 && buffer[2]==0xabcd ? "yes" : "no");
    }

    /* test error handling */
    errorCode=U_ZERO_ERROR;
    length=u_strToLower(NULL, sizeof(buffer)/U_SIZEOF_UCHAR,
                        beforeLower, sizeof(beforeLower)/U_SIZEOF_UCHAR,
                        "",
                        &errorCode);
    if(errorCode!=U_ILLEGAL_ARGUMENT_ERROR) {
        log_err("error in u_strToLower(root locale dest=NULL)=%ld error=%s\n",
            length,
            u_errorName(errorCode));
    }

    buffer[0]=0xabcd;
    errorCode=U_ZERO_ERROR;
    length=u_strToLower(buffer, -1,
                        beforeLower, sizeof(beforeLower)/U_SIZEOF_UCHAR,
                        "",
                        &errorCode);
    if( errorCode!=U_ILLEGAL_ARGUMENT_ERROR ||
        buffer[0]!=0xabcd
    ) {
        log_err("error in u_strToLower(root locale destCapacity=-1)=%ld error=%s buffer[0]==0x%lx\n",
            length,
            u_errorName(errorCode),
            buffer[0]);
    }
}

U_CFUNC void
TestCaseUpper() {
    static const UChar

    beforeUpper[]= { 0x61, 0x42, 0x69,  0x3c2, 0xdf,       0x3c3, 0x2f, 0xfb03,           0xd93f, 0xdfff },
    upperRoot[]=   { 0x41, 0x42, 0x49,  0x3a3, 0x53, 0x53, 0x3a3, 0x2f, 0x46, 0x46, 0x49, 0xd93f, 0xdfff },
    upperTurkish[]={ 0x41, 0x42, 0x130, 0x3a3, 0x53, 0x53, 0x3a3, 0x2f, 0x46, 0x46, 0x49, 0xd93f, 0xdfff };

    UChar buffer[32];
    int32_t length;
    UErrorCode errorCode;

    /* uppercase with root locale and in the same buffer */
    uprv_memcpy(buffer, beforeUpper, sizeof(beforeUpper));
    errorCode=U_ZERO_ERROR;
    length=u_strToUpper(buffer, sizeof(buffer)/U_SIZEOF_UCHAR,
                        buffer, sizeof(beforeUpper)/U_SIZEOF_UCHAR,
                        "",
                        &errorCode);
    if( U_FAILURE(errorCode) ||
        length!=(sizeof(upperRoot)/U_SIZEOF_UCHAR) ||
        uprv_memcmp(upperRoot, buffer, length*U_SIZEOF_UCHAR)!=0 ||
        buffer[length]!=0
    ) {
        log_err("error in u_strToUpper(root locale)=%ld error=%s string matches: %s\n",
            length,
            u_errorName(errorCode),
            uprv_memcmp(upperRoot, buffer, length*U_SIZEOF_UCHAR)==0 && buffer[length]==0 ? "yes" : "no");
    }

    /* uppercase with turkish locale and separate buffers */
    buffer[0]=0xabcd;
    errorCode=U_ZERO_ERROR;
    length=u_strToUpper(buffer, sizeof(buffer)/U_SIZEOF_UCHAR,
                        beforeUpper, sizeof(beforeUpper)/U_SIZEOF_UCHAR,
                        "tr",
                        &errorCode);
    if( U_FAILURE(errorCode) ||
        length!=(sizeof(upperTurkish)/U_SIZEOF_UCHAR) ||
        uprv_memcmp(upperTurkish, buffer, length*U_SIZEOF_UCHAR)!=0 ||
        buffer[length]!=0
    ) {
        log_err("error in u_strToUpper(turkish locale)=%ld error=%s string matches: %s\n",
            length,
            u_errorName(errorCode),
            uprv_memcmp(upperTurkish, buffer, length*U_SIZEOF_UCHAR)==0 && buffer[length]==0 ? "yes" : "no");
    }

    /* test preflighting */
    errorCode=U_ZERO_ERROR;
    length=u_strToUpper(NULL, 0,
                        beforeUpper, sizeof(beforeUpper)/U_SIZEOF_UCHAR,
                        "tr",
                        &errorCode);
    if( errorCode!=U_BUFFER_OVERFLOW_ERROR ||
        length!=(sizeof(upperTurkish)/U_SIZEOF_UCHAR)
    ) {
        log_err("error in u_strToUpper(turkish locale pure preflighting)=%ld error=%s\n",
            length,
            u_errorName(errorCode));
    }

    /* test error handling */
    buffer[0]=0xabcd;
    errorCode=U_ZERO_ERROR;
    length=u_strToUpper(buffer, sizeof(buffer)/U_SIZEOF_UCHAR,
                        NULL, sizeof(beforeUpper)/U_SIZEOF_UCHAR,
                        "tr",
                        &errorCode);
    if( errorCode!=U_ILLEGAL_ARGUMENT_ERROR ||
        buffer[0]!=0xabcd
    ) {
        log_err("error in u_strToUpper(turkish locale src=NULL)=%ld error=%s buffer[0]==0x%lx\n",
            length,
            u_errorName(errorCode),
            buffer[0]);
    }

    buffer[0]=0xabcd;
    errorCode=U_ZERO_ERROR;
    length=u_strToUpper(buffer, sizeof(buffer)/U_SIZEOF_UCHAR,
                        beforeUpper, -2,
                        "tr",
                        &errorCode);
    if( errorCode!=U_ILLEGAL_ARGUMENT_ERROR ||
        buffer[0]!=0xabcd
    ) {
        log_err("error in u_strToUpper(turkish locale srcLength=-2)=%ld error=%s buffer[0]==0x%lx\n",
            length,
            u_errorName(errorCode),
            buffer[0]);
    }
}

#if !UCONFIG_NO_BREAK_ITERATION

U_CFUNC void
TestCaseTitle() {
    static const UChar

    beforeTitle[]= { 0x61, 0x42, 0x20, 0x69,  0x3c2, 0x20, 0xdf,       0x3c3, 0x2f, 0xfb03,           0xd93f, 0xdfff },
    titleWord[]=   { 0x41, 0x62, 0x20, 0x49,  0x3c2, 0x20, 0x53, 0x73, 0x3c3, 0x2f, 0x46, 0x66, 0x69, 0xd93f, 0xdfff },
    titleChar[]=   { 0x41, 0x42, 0x20, 0x49,  0x3a3, 0x20, 0x53, 0x73, 0x3a3, 0x2f, 0x46, 0x66, 0x69, 0xd93f, 0xdfff };

    UChar buffer[32];
    UBreakIterator *titleIterChars;
    int32_t length;
    UErrorCode errorCode;

    errorCode=U_ZERO_ERROR;
    titleIterChars=ubrk_open(UBRK_CHARACTER, "", beforeTitle, sizeof(beforeTitle)/U_SIZEOF_UCHAR, &errorCode);
    if(U_FAILURE(errorCode)) {
        log_err("error: ubrk_open(UBRK_CHARACTER)->%s\n", u_errorName(errorCode));
        return;
    }

    /* titlecase with standard break iterator and in the same buffer */
    uprv_memcpy(buffer, beforeTitle, sizeof(beforeTitle));
    errorCode=U_ZERO_ERROR;
    length=u_strToTitle(buffer, sizeof(buffer)/U_SIZEOF_UCHAR,
                        buffer, sizeof(beforeTitle)/U_SIZEOF_UCHAR,
                        NULL, "",
                        &errorCode);
    if( U_FAILURE(errorCode) ||
        length!=(sizeof(titleWord)/U_SIZEOF_UCHAR) ||
        uprv_memcmp(titleWord, buffer, length*U_SIZEOF_UCHAR)!=0 ||
        buffer[length]!=0
    ) {
        log_err("error in u_strToTitle(standard iterator)=%ld error=%s string matches: %s\n",
            length,
            u_errorName(errorCode),
            uprv_memcmp(titleWord, buffer, length*U_SIZEOF_UCHAR)==0 && buffer[length]==0 ? "yes" : "no");
    }

    /* titlecase with UBRK_CHARACTERS and separate buffers */
    buffer[0]=0xabcd;
    errorCode=U_ZERO_ERROR;
    length=u_strToTitle(buffer, sizeof(buffer)/U_SIZEOF_UCHAR,
                        beforeTitle, sizeof(beforeTitle)/U_SIZEOF_UCHAR,
                        titleIterChars, "",
                        &errorCode);
    if( U_FAILURE(errorCode) ||
        length!=(sizeof(titleChar)/U_SIZEOF_UCHAR) ||
        uprv_memcmp(titleChar, buffer, length*U_SIZEOF_UCHAR)!=0 ||
        buffer[length]!=0
    ) {
        log_err("error in u_strToTitle(UBRK_CHARACTERS)=%ld error=%s string matches: %s\n",
            length,
            u_errorName(errorCode),
            uprv_memcmp(titleChar, buffer, length*U_SIZEOF_UCHAR)==0 && buffer[length]==0 ? "yes" : "no");
    }

    /* test preflighting */
    errorCode=U_ZERO_ERROR;
    length=u_strToTitle(NULL, 0,
                        beforeTitle, sizeof(beforeTitle)/U_SIZEOF_UCHAR,
                        titleIterChars, "",
                        &errorCode);
    if( errorCode!=U_BUFFER_OVERFLOW_ERROR ||
        length!=(sizeof(titleChar)/U_SIZEOF_UCHAR)
    ) {
        log_err("error in u_strToTitle(UBRK_CHARACTERS pure preflighting)=%ld error=%s\n",
            length,
            u_errorName(errorCode));
    }

    /* test error handling */
    buffer[0]=0xabcd;
    errorCode=U_ZERO_ERROR;
    length=u_strToTitle(buffer, sizeof(buffer)/U_SIZEOF_UCHAR,
                        NULL, sizeof(beforeTitle)/U_SIZEOF_UCHAR,
                        titleIterChars, "",
                        &errorCode);
    if( errorCode!=U_ILLEGAL_ARGUMENT_ERROR ||
        buffer[0]!=0xabcd
    ) {
        log_err("error in u_strToTitle(UBRK_CHARACTERS src=NULL)=%ld error=%s buffer[0]==0x%lx\n",
            length,
            u_errorName(errorCode),
            buffer[0]);
    }

    buffer[0]=0xabcd;
    errorCode=U_ZERO_ERROR;
    length=u_strToTitle(buffer, sizeof(buffer)/U_SIZEOF_UCHAR,
                        beforeTitle, -2,
                        titleIterChars, "",
                        &errorCode);
    if( errorCode!=U_ILLEGAL_ARGUMENT_ERROR ||
        buffer[0]!=0xabcd
    ) {
        log_err("error in u_strToTitle(UBRK_CHARACTERS srcLength=-2)=%ld error=%s buffer[0]==0x%lx\n",
            length,
            u_errorName(errorCode),
            buffer[0]);
    }

    ubrk_close(titleIterChars);
}

#endif

/* test case folding and case-insensitive string compare -------------------- */

U_CFUNC void
TestCaseFolding() {
    static const UChar32
    simple[]={
        /* input, default, exclude special i */
        0x61,   0x61,  0x61,
        0x49,   0x69,  0x131,
        0x130,  0x69,  0x69,
        0x131,  0x131, 0x131,
        0xdf,   0xdf,  0xdf,
        0xfb03, 0xfb03, 0xfb03,
        0x1040e,0x10436,0x10436,
        0x5ffff,0x5ffff,0x5ffff
    };

    static const UChar
    mixed[]=                { 0x61, 0x42, 0x130,       0x49,  0x131, 0x3d0, 0xdf,       0xfb03,           0xd93f, 0xdfff },
    foldedDefault[]=        { 0x61, 0x62, 0x69, 0x307, 0x69,  0x131, 0x3b2, 0x73, 0x73, 0x66, 0x66, 0x69, 0xd93f, 0xdfff },
    foldedExcludeSpecialI[]={ 0x61, 0x62, 0x69,        0x131, 0x131, 0x3b2, 0x73, 0x73, 0x66, 0x66, 0x69, 0xd93f, 0xdfff };

    UVersionInfo unicodeVersion={ 0, 0, 17, 89 }, unicode_3_1={ 3, 1, 0, 0 };

    const UChar32 *p;
    int32_t i;

    UChar buffer[32];
    int32_t length;
    UErrorCode errorCode;
    UBool isUnicode_3_1;

    /* if unicodeVersion()>=3.1 then test exclude-special-i cases as well */
    u_getUnicodeVersion(unicodeVersion);
    isUnicode_3_1= uprv_memcmp(unicodeVersion, unicode_3_1, 4)>=0;

    /* test simple case folding */
    p=simple;
    for(i=0; i<sizeof(simple)/12; p+=3, ++i) {
        if(u_foldCase(p[0], U_FOLD_CASE_DEFAULT)!=p[1]) {
            log_err("error: u_foldCase(0x%04lx, default)=0x%04lx instead of 0x%04lx\n",
                    p[0], u_foldCase(p[0], U_FOLD_CASE_DEFAULT), p[1]);
            return;
        }

        if(isUnicode_3_1 && u_foldCase(p[0], U_FOLD_CASE_EXCLUDE_SPECIAL_I)!=p[2]) {
            log_err("error: u_foldCase(0x%04lx, exclude special i)=0x%04lx instead of 0x%04lx\n",
                    p[0], u_foldCase(p[0], U_FOLD_CASE_EXCLUDE_SPECIAL_I), p[2]);
            return;
        }
    }

    /* test full string case folding with default option and separate buffers */
    buffer[0]=0xabcd;
    errorCode=U_ZERO_ERROR;
    length=u_strFoldCase(buffer, sizeof(buffer)/U_SIZEOF_UCHAR,
                        mixed, sizeof(mixed)/U_SIZEOF_UCHAR,
                        U_FOLD_CASE_DEFAULT,
                        &errorCode);
    if( U_FAILURE(errorCode) ||
        length!=(sizeof(foldedDefault)/U_SIZEOF_UCHAR) ||
        uprv_memcmp(foldedDefault, buffer, length*U_SIZEOF_UCHAR)!=0 ||
        buffer[length]!=0
    ) {
        log_err("error in u_strFoldCase(default)=%ld error=%s string matches: %s\n",
            length,
            u_errorName(errorCode),
            uprv_memcmp(foldedDefault, buffer, length*U_SIZEOF_UCHAR)==0 && buffer[length]==0 ? "yes" : "no");
    }

    /* exclude special i */
    if(isUnicode_3_1) {
        buffer[0]=0xabcd;
        errorCode=U_ZERO_ERROR;
        length=u_strFoldCase(buffer, sizeof(buffer)/U_SIZEOF_UCHAR,
                            mixed, sizeof(mixed)/U_SIZEOF_UCHAR,
                            U_FOLD_CASE_EXCLUDE_SPECIAL_I,
                            &errorCode);
        if( U_FAILURE(errorCode) ||
            length!=(sizeof(foldedExcludeSpecialI)/U_SIZEOF_UCHAR) ||
            uprv_memcmp(foldedExcludeSpecialI, buffer, length*U_SIZEOF_UCHAR)!=0 ||
            buffer[length]!=0
        ) {
            log_err("error in u_strFoldCase(exclude special i)=%ld error=%s string matches: %s\n",
                length,
                u_errorName(errorCode),
                uprv_memcmp(foldedExcludeSpecialI, buffer, length*U_SIZEOF_UCHAR)==0 && buffer[length]==0 ? "yes" : "no");
        }
    }

    /* test full string case folding with default option and in the same buffer */
    uprv_memcpy(buffer, mixed, sizeof(mixed));
    buffer[sizeof(mixed)/U_SIZEOF_UCHAR]=0;
    errorCode=U_ZERO_ERROR;
    length=u_strFoldCase(buffer, sizeof(buffer)/U_SIZEOF_UCHAR,
                        buffer, -1, /* implicit srcLength */
                        U_FOLD_CASE_DEFAULT,
                        &errorCode);
    if( U_FAILURE(errorCode) ||
        length!=(sizeof(foldedDefault)/U_SIZEOF_UCHAR) ||
        uprv_memcmp(foldedDefault, buffer, length*U_SIZEOF_UCHAR)!=0 ||
        buffer[length]!=0
    ) {
        log_err("error in u_strFoldCase(default same buffer)=%ld error=%s string matches: %s\n",
            length,
            u_errorName(errorCode),
            uprv_memcmp(foldedDefault, buffer, length*U_SIZEOF_UCHAR)==0 && buffer[length]==0 ? "yes" : "no");
    }

    /* test full string case folding, exclude special i, in the same buffer */
    if(isUnicode_3_1) {
        uprv_memcpy(buffer, mixed, sizeof(mixed));
        errorCode=U_ZERO_ERROR;
        length=u_strFoldCase(buffer, sizeof(buffer)/U_SIZEOF_UCHAR,
                            buffer, sizeof(mixed)/U_SIZEOF_UCHAR,
                            U_FOLD_CASE_EXCLUDE_SPECIAL_I,
                            &errorCode);
        if( U_FAILURE(errorCode) ||
            length!=(sizeof(foldedExcludeSpecialI)/U_SIZEOF_UCHAR) ||
            uprv_memcmp(foldedExcludeSpecialI, buffer, length*U_SIZEOF_UCHAR)!=0 ||
            buffer[length]!=0
        ) {
            log_err("error in u_strFoldCase(exclude special i same buffer)=%ld error=%s string matches: %s\n",
                length,
                u_errorName(errorCode),
                uprv_memcmp(foldedExcludeSpecialI, buffer, length*U_SIZEOF_UCHAR)==0 && buffer[length]==0 ? "yes" : "no");
        }
    }

    /* test preflighting */
    buffer[0]=buffer[2]=0xabcd;
    errorCode=U_ZERO_ERROR;
    length=u_strFoldCase(buffer, 2, /* set destCapacity=2 */
                        mixed, sizeof(mixed)/U_SIZEOF_UCHAR,
                        U_FOLD_CASE_DEFAULT,
                        &errorCode);
    if( errorCode!=U_BUFFER_OVERFLOW_ERROR ||
        length!=(sizeof(foldedDefault)/U_SIZEOF_UCHAR) ||
        uprv_memcmp(foldedDefault, buffer, 2*U_SIZEOF_UCHAR)!=0 ||
        buffer[2]!=0xabcd
    ) {
        log_err("error in u_strFoldCase(default preflighting)=%ld error=%s string matches: %s\n",
            length,
            u_errorName(errorCode),
            uprv_memcmp(foldedDefault, buffer, 2*U_SIZEOF_UCHAR)==0 && buffer[2]==0xabcd ? "yes" : "no");
    }

    errorCode=U_ZERO_ERROR;
    length=u_strFoldCase(NULL, 0,
                        mixed, sizeof(mixed)/U_SIZEOF_UCHAR,
                        U_FOLD_CASE_DEFAULT,
                        &errorCode);
    if( errorCode!=U_BUFFER_OVERFLOW_ERROR ||
        length!=(sizeof(foldedDefault)/U_SIZEOF_UCHAR)
    ) {
        log_err("error in u_strFoldCase(default pure preflighting)=%ld error=%s\n",
            length,
            u_errorName(errorCode));
    }

    /* test error handling */
    errorCode=U_ZERO_ERROR;
    length=u_strFoldCase(NULL, sizeof(buffer)/U_SIZEOF_UCHAR,
                        mixed, sizeof(mixed)/U_SIZEOF_UCHAR,
                        U_FOLD_CASE_DEFAULT,
                        &errorCode);
    if(errorCode!=U_ILLEGAL_ARGUMENT_ERROR) {
        log_err("error in u_strFoldCase(default dest=NULL)=%ld error=%s\n",
            length,
            u_errorName(errorCode));
    }

    buffer[0]=0xabcd;
    errorCode=U_ZERO_ERROR;
    length=u_strFoldCase(buffer, -1,
                        mixed, sizeof(mixed)/U_SIZEOF_UCHAR,
                        U_FOLD_CASE_DEFAULT,
                        &errorCode);
    if( errorCode!=U_ILLEGAL_ARGUMENT_ERROR ||
        buffer[0]!=0xabcd
    ) {
        log_err("error in u_strFoldCase(default destCapacity=-1)=%ld error=%s buffer[0]==0x%lx\n",
            length,
            u_errorName(errorCode),
            buffer[0]);
    }

    buffer[0]=0xabcd;
    errorCode=U_ZERO_ERROR;
    length=u_strFoldCase(buffer, sizeof(buffer)/U_SIZEOF_UCHAR,
                        NULL, sizeof(mixed)/U_SIZEOF_UCHAR,
                        U_FOLD_CASE_EXCLUDE_SPECIAL_I,
                        &errorCode);
    if( errorCode!=U_ILLEGAL_ARGUMENT_ERROR ||
        buffer[0]!=0xabcd
    ) {
        log_err("error in u_strFoldCase(exclude special i src=NULL)=%ld error=%s buffer[0]==0x%lx\n",
            length,
            u_errorName(errorCode),
            buffer[0]);
    }

    buffer[0]=0xabcd;
    errorCode=U_ZERO_ERROR;
    length=u_strFoldCase(buffer, sizeof(buffer)/U_SIZEOF_UCHAR,
                        mixed, -2,
                        U_FOLD_CASE_EXCLUDE_SPECIAL_I,
                        &errorCode);
    if( errorCode!=U_ILLEGAL_ARGUMENT_ERROR ||
        buffer[0]!=0xabcd
    ) {
        log_err("error in u_strFoldCase(exclude special i srcLength=-2)=%ld error=%s buffer[0]==0x%lx\n",
            length,
            u_errorName(errorCode),
            buffer[0]);
    }
}

U_CFUNC void
TestCaseCompare() {
    static const UChar

    mixed[]=               { 0x61, 0x42, 0x131, 0x3a3, 0xdf,       0xfb03,           0xd93f, 0xdfff, 0 },
    otherDefault[]=        { 0x41, 0x62, 0x131, 0x3c3, 0x73, 0x53, 0x46, 0x66, 0x49, 0xd93f, 0xdfff, 0 },
    otherExcludeSpecialI[]={ 0x41, 0x62, 0x131, 0x3c3, 0x53, 0x73, 0x66, 0x46, 0x69, 0xd93f, 0xdfff, 0 },
    different[]=           { 0x41, 0x62, 0x131, 0x3c3, 0x73, 0x53, 0x46, 0x66, 0x49, 0xd93f, 0xdffd, 0 };

    UVersionInfo unicodeVersion={ 0, 0, 17, 89 }, unicode_3_1={ 3, 1, 0, 0 };

    int32_t result, lenMixed, lenOtherDefault, lenOtherExcludeSpecialI, lenDifferent;
    UErrorCode errorCode;
    UBool isUnicode_3_1;

    errorCode=U_ZERO_ERROR;

    lenMixed=u_strlen(mixed);
    lenOtherDefault=u_strlen(otherDefault);
    lenOtherExcludeSpecialI=u_strlen(otherExcludeSpecialI);
    lenDifferent=u_strlen(different);

    /* if unicodeVersion()>=3.1 then test exclude-special-i cases as well */
    u_getUnicodeVersion(unicodeVersion);
    isUnicode_3_1= uprv_memcmp(unicodeVersion, unicode_3_1, 4)>=0;

    /* test u_strcasecmp() */
    result=u_strcasecmp(mixed, otherDefault, U_FOLD_CASE_DEFAULT);
    if(result!=0) {
        log_err("error: u_strcasecmp(mixed, other, default)=%ld instead of 0\n", result);
    }
    result=u_strCaseCompare(mixed, -1, otherDefault, -1, U_FOLD_CASE_DEFAULT, &errorCode);
    if(result!=0) {
        log_err("error: u_strCaseCompare(mixed, other, default)=%ld instead of 0\n", result);
    }

    /* test u_strcasecmp() - exclude special i */
    result=u_strcasecmp(mixed, otherExcludeSpecialI, U_FOLD_CASE_EXCLUDE_SPECIAL_I);
    if(result!=0) {
        log_err("error: u_strcasecmp(mixed, other, exclude special i)=%ld instead of 0\n", result);
    }
    result=u_strCaseCompare(mixed, lenMixed, otherExcludeSpecialI, lenOtherExcludeSpecialI, U_FOLD_CASE_EXCLUDE_SPECIAL_I, &errorCode);
    if(result!=0) {
        log_err("error: u_strCaseCompare(mixed, other, exclude special i)=%ld instead of 0\n", result);
    }

    /* test u_strcasecmp() */
    result=u_strcasecmp(mixed, different, U_FOLD_CASE_DEFAULT);
    if(result<=0) {
        log_err("error: u_strcasecmp(mixed, different, default)=%ld instead of positive\n", result);
    }
    result=u_strCaseCompare(mixed, -1, different, lenDifferent, U_FOLD_CASE_DEFAULT, &errorCode);
    if(result<=0) {
        log_err("error: u_strCaseCompare(mixed, different, default)=%ld instead of positive\n", result);
    }

    /* test u_strncasecmp() - stop before the sharp s (U+00df) */
    result=u_strncasecmp(mixed, different, 4, U_FOLD_CASE_DEFAULT);
    if(result!=0) {
        log_err("error: u_strncasecmp(mixed, different, 4, default)=%ld instead of 0\n", result);
    }
    result=u_strCaseCompare(mixed, 4, different, 4, U_FOLD_CASE_DEFAULT, &errorCode);
    if(result!=0) {
        log_err("error: u_strCaseCompare(mixed, 4, different, 4, default)=%ld instead of 0\n", result);
    }

    /* test u_strncasecmp() - stop in the middle of the sharp s (U+00df) */
    result=u_strncasecmp(mixed, different, 5, U_FOLD_CASE_DEFAULT);
    if(result<=0) {
        log_err("error: u_strncasecmp(mixed, different, 5, default)=%ld instead of positive\n", result);
    }
    result=u_strCaseCompare(mixed, 5, different, 5, U_FOLD_CASE_DEFAULT, &errorCode);
    if(result<=0) {
        log_err("error: u_strCaseCompare(mixed, 5, different, 5, default)=%ld instead of positive\n", result);
    }

    /* test u_memcasecmp() - stop before the sharp s (U+00df) */
    result=u_memcasecmp(mixed, different, 4, U_FOLD_CASE_DEFAULT);
    if(result!=0) {
        log_err("error: u_memcasecmp(mixed, different, 4, default)=%ld instead of 0\n", result);
    }

    /* test u_memcasecmp() - stop in the middle of the sharp s (U+00df) */
    result=u_memcasecmp(mixed, different, 5, U_FOLD_CASE_DEFAULT);
    if(result<=0) {
        log_err("error: u_memcasecmp(mixed, different, 5, default)=%ld instead of positive\n", result);
    }
}
