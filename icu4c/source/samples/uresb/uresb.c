/*
*******************************************************************************
*
*   Copyright (C) 1999-2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  uresb.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000sep6
*   created by: Vladimir Weinstein
*/

/******************************************************************************
 * A program uses simple resource bundle readable by udata - example for
 * ICU workshop
 ******************************************************************************/
#include "unicode/ures.h"
#include "unicode/ustdio.h"
#include "unicode/uloc.h"
#include "uoptions.h"
#ifdef WIN32
#include <direct.h>
#else
#include <unistd.h>
#endif

static char *currdir = NULL;
static const char *locale = NULL;

const UChar baderror[] = { 0x0042, 0x0041, 0x0044, 0x0000 };

const UChar *getErrorName(int32_t errorNumber);

static UOption options[]={
    UOPTION_HELP_H,
    UOPTION_HELP_QUESTION_MARK,
    { "locale", NULL, NULL, NULL, 'l', UOPT_REQUIRES_ARG, 0 }
};


extern int
main(int argc, const char *argv[]) {

    UResourceBundle *example = NULL;
    UFILE *out = u_finit(stdout, NULL, NULL);
    int32_t i = 0;
#ifdef WIN32
    currdir = _getcwd(NULL, 0);
#else
    currdir = getcwd(NULL, 0);
#endif

    argc=u_parseArgs(argc, argv, sizeof(options)/sizeof(options[0]), options);

    /* error handling, printing usage message */
    if(argc<0) {
        fprintf(stderr,
            "error in command line argument \"%s\"\n",
            argv[-argc]);
    }
    if(argc<0 || options[0].doesOccur || options[1].doesOccur) {
        fprintf(stderr,
            "usage: %s [-options]\n",
            argv[0]);
        return argc<0 ? U_ILLEGAL_ARGUMENT_ERROR : U_ZERO_ERROR;
    }

    if(options[2].doesOccur) {
        locale = options[2].value;
    } else {
        locale = uloc_getDefault();
    }

    printf("We are running under %s locale\n", locale);

    for(i = 0; i<20; i++) {
        u_fprintf(out, "Error number %d is %U\n", i, getErrorName(i));
    }

    u_fclose(out);
    return 0;
}

const UChar *getErrorName(int32_t errorNumber) {
    UErrorCode status = U_ZERO_ERROR;
    int32_t len = 0;

    UResourceBundle *error = ures_open(currdir, locale, &status);

    UResourceBundle *errorcodes = ures_getByKey(error, "errorcodes", NULL, &status);

    const UChar *result = ures_getStringByIndex(errorcodes, errorNumber, &len, &status);

    ures_close(errorcodes);
    ures_close(error);

    if(U_SUCCESS(status)) {
        return result;
    } else {
        return baderror;
    }

}