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
 * This program prints out resource bundles - example for
 * ICU workshop
 * TODO: make a complete i18n layout for this program.
 ******************************************************************************/

#include "unicode/ures.h"
#include "unicode/ustdio.h"
#include "unicode/uloc.h"
#include "uoptions.h"
#include <stdlib.h>
#ifdef WIN32
#include <direct.h>
#else
#include <unistd.h>
#endif

#define URESB_DEFAULTTRUNC 40

static char *currdir = NULL;
/*--locale sr_YU and --encoding cp855
 * are interesting on Win32
 */

static const char *locale = NULL;
static const char *encoding = NULL;
static const char *resPath = NULL;
static const int32_t indentsize = 4;
static UFILE *outerr = NULL;
static int32_t truncsize = URESB_DEFAULTTRUNC;
static UBool trunc = FALSE;

const UChar baderror[] = { 0x0042, 0x0041, 0x0044, 0x0000 };

const UChar *getErrorName(UErrorCode errorNumber);
void reportError(UErrorCode *status);
void printOutBundle(UFILE *out, UResourceBundle *resource, int32_t indent, UErrorCode *status);
void printIndent(UFILE *out, int32_t indent);
void printHex(UFILE *out, const int8_t *what);

static UOption options[]={
    UOPTION_HELP_H,
    UOPTION_HELP_QUESTION_MARK,
    { "locale", NULL, NULL, NULL, 'l', UOPT_REQUIRES_ARG, 0 },
	UOPTION_ENCODING,
	{ "path", NULL, NULL, NULL, 'p', UOPT_OPTIONAL_ARG, 0 },
	{ "truncate", NULL, NULL, NULL, 't', UOPT_OPTIONAL_ARG, 0 }
};


extern int
main(int argc, const char *argv[]) {

    UResourceBundle *bundle = NULL;
	UErrorCode status = U_ZERO_ERROR;
    UFILE *out = NULL;
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

	if(options[3].doesOccur) {
		encoding = options[3].value;
	} else {
		encoding = NULL;
	}

	if(options[4].doesOccur) {
		if(options[4].value != NULL) {
			resPath = options[4].value; /* we'll use users resources */
		} else {
			resPath = NULL; /* we'll use ICU system resources for dumping */
		}
	} else {
		resPath = currdir; /* we'll just dump uresb samples resources */
	}

	if(options[5].doesOccur) {
		trunc = TRUE;
		if(options[5].value != NULL) {
			truncsize = atoi(options[5].value); /* user defined printable size */
		} else {
			truncsize = URESB_DEFAULTTRUNC; /* we'll use default omitting size */
		}
	} else {
		trunc = FALSE;
	}

	outerr = u_finit(stderr, locale, encoding);
	out = u_finit(stdout, locale, encoding); 

    u_fprintf(outerr, "We are running under %s locale\n", locale);
/*
    for(i = 0; i<20; i++) {
		reportError(&i);
    }
*/
	bundle = ures_open(resPath, locale, &status);
	if(U_SUCCESS(status)) {
		u_fprintf(out, "%s\n", locale);
		printOutBundle(out, bundle, 0, &status);
	} else {
		reportError(&status);
	}

	ures_close(bundle);

    u_fclose(out);
    u_fclose(outerr);
    return 0;
}

void printIndent(UFILE *out, int32_t indent) {
	char inchar[256];
	int32_t i = 0;
	for(i = 0; i<indent; i++) {
		inchar[i] = ' ';
	}
	inchar[indent] = '\0';
	u_fprintf(out, "%s", inchar);
}

void printHex(UFILE *out, const int8_t *what) {
	int8_t value = (int8_t)*what;
	char result[256];
	itoa(value, result, 16);
	if(value < 0x10) {
		u_fprintf(out, "0%s", result);
	}  else {
		u_fprintf(out, "%s", result);
	}
}

void printOutBundle(UFILE *out, UResourceBundle *resource, int32_t indent, UErrorCode *status) {
	int32_t noOfElements = ures_getSize(resource);
	int32_t i = 0;
	const char *key = ures_getKey(resource);

	switch(ures_getType(resource)) {
	case RES_STRING :
		{
			int32_t len=0;
			const UChar* string = ures_getString(resource, &len, status);
			/* TODO: String truncation */
			/*
			if(trunc && len > truncsize) {
				printIndent(out, indent);
				u_fprintf(out, "// WARNING: this string, size %d is truncated to %d\n", len, truncsize/2);
				len = truncsize/2;
			}
			*/
			printIndent(out, indent);
			if(key != NULL) {
				u_fprintf(out, "%s { \"%U\" } \n", key, string);
			} else {
				u_fprintf(out, "\"%U\",\n", string);
			}
		}
		break;
	case RES_INT :
		printIndent(out, indent);
		if(key != NULL) {
			u_fprintf(out, "%s:int { %li } \n", key, ures_getInt(resource, status));
		} else {
			u_fprintf(out, "%li,\n", ures_getInt(resource, status));
		}
		break;
	case RES_BINARY :
		{
			int32_t len = 0;
			const int8_t *data = (const int8_t *)ures_getBinary(resource, &len, status);
			if(trunc && len > truncsize) {
				printIndent(out, indent);
				u_fprintf(out, "// WARNING: this resource, size %li is truncated to %li\n", len, truncsize/2);
				len = truncsize/2;
			}
			if(U_SUCCESS(*status) && len > 0) {
				if(key != NULL) {
					printIndent(out, indent);
					u_fprintf(out, "%s:binary { ", key);
					for(i = 0; i<len; i++) {
						printHex(out, data++);
					}
					u_fprintf(out, " }\n");
				} else {
					u_fprintf(outerr, "This is a VERY STRANGE resource INDEED!\n");
				}

			} else {
				reportError(status);
			}
		}
		break;
	case RES_TABLE :
	case RES_ARRAY :
		{
			UResourceBundle *t = NULL;
			ures_resetIterator(resource);
			printIndent(out, indent);
			if(key != NULL) {
				u_fprintf(out, "%s ", key);
			}
			u_fprintf(out, "{\n");

			while(ures_hasNext(resource)) {
				t = ures_getNextResource(resource, t, status);
				printOutBundle(out, t, indent+indentsize, status);
			}

			printIndent(out, indent);
			u_fprintf(out, "}\n");
			ures_close(t);
		}
		break;
	default:
		break;
	}

}

void reportError(UErrorCode *status) {
	u_fprintf(outerr, "Error %d : %U happened!\n", *status, getErrorName(*status));
}


const UChar *getErrorName(UErrorCode errorNumber) {
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