/********************************************************************
 * COPYRIGHT:
 * Copyright (C) 2001 IBM, Inc.   All Rights Reserved.
 *
 ********************************************************************/
/********************************************************************************
*
* File dumpce.cpp
*
* Modification History:
* Name          Date           Description
* synwee        May 31 2001    Creation
*
*********************************************************************************
*/

/**
* This program outputs the collation elements used for a requested tailoring.
*
* Usage:
*     dumpce options...
*         -locale name          ICU locale to use.  Default is en_US
*         -serialize            Serializes the collation elements in -locale 
*                               and outputs them into -outputfile
*         -destdir     dir_name Path for outputing the serialized collation 
*                               elements. Default standard output.
*/
#include <unicode/utypes.h>
#include <unicode/ucol.h>
#include <unicode/uloc.h>
#include <unicode/ucoleitr.h>
#include <unicode/uchar.h>
#include <unicode/utf16.h>
#include <unicode/putil.h>
#include <stdio.h>
#include "ucol_tok.h"
#include "cstring.h"
#include "uoptions.h"
#include "ucol_imp.h"

/**
* Command line option variables. 
* These global variables are set according to the options specified on the 
* command line by the user.
*/
static UOption options[]={
    UOPTION_HELP_H,
    UOPTION_HELP_QUESTION_MARK,
    {"locale",      NULL, NULL, NULL, 'l', UOPT_REQUIRES_ARG, 0},
    {"serialize",   NULL, NULL, NULL, 's', UOPT_NO_ARG, 0},
	UOPTION_DESTDIR,
	UOPTION_VERBOSE
};

/**
* Collator used in this program
*/
static UCollator *COLLATOR_;
/**
* Output strea, used in this program
*/
static FILE *OUTPUT_;

/**
* Writes the hexadecimal of a null-terminated array of codepoints into a 
* file
* @param f UFILE instance to store
* @param c codepoints array
*/
void serialize(FILE *f, const UChar *c) 
{
    UChar cp = *(c ++);
    
    fprintf(f, " %04x", cp);
   
    while (*c != 0) {
        cp = *(c ++);
        fprintf(f, " %04x", cp);
    }
}

/**
* Writes the hexadecimal of a non-null-terminated array of codepoints into a 
* file
* @param f UFILE instance to store
* @param c codepoints array
* @param l codepoints array length
*/
void serialize(FILE *f, const UChar *c, int l) 
{
    int   count = 1;
    UChar cp    = *(c ++);
    
    fprintf(f, " %04x", cp);
   
    while (count < l) {
        cp = *(c ++);
        fprintf(f, " %04x", cp);
        count ++;
    }
}

/**
* Sets the iterator to the argument string and outputs the collation elements.
* @param f file output stream
* @param iter collation element iterator
*/
void serialize(FILE *f, UCollationElements *iter) {
    UChar   *codepoint = iter->iteratordata_.string;
    // unlikely that sortkeys will be over this size 
    uint8_t  sortkey[64];
    uint8_t *psortkey = sortkey;
    int      sortkeylength = 0;

    if (iter->iteratordata_.flags & UCOL_ITER_HASLEN) {
        serialize(f, codepoint, iter->iteratordata_.endp - codepoint);
        sortkeylength = ucol_getSortKey(iter->iteratordata_.coll, codepoint, 
                        iter->iteratordata_.endp - codepoint, sortkey, 64);
    }
    else {
        serialize(f, codepoint);
        sortkeylength = ucol_getSortKey(iter->iteratordata_.coll, codepoint, 
                                        -1, sortkey, 64);
    }
    if (options[5].doesOccur) {
        serialize(stdout, codepoint);
        fprintf(stdout, "\n");
    }

    fprintf(f, "; ");

    UErrorCode error = U_ZERO_ERROR;
    uint32_t ce = ucol_next(iter, &error);
    if (U_FAILURE(error)) {
        fprintf(f, "Error retrieving collation elements\n");
        return;
    }
    fprintf(f, "[");
    while (TRUE) {
        fprintf(f, "%08x", ce);
        ce = ucol_next(iter, &error);
        if (ce == UCOL_NULLORDER) {
            break;
        }
        fprintf(f, " ");
        if (U_FAILURE(error)) {
            fprintf(stdout, "Error retrieving collation elements");
            return;
        }
    }
    fprintf(f, "] ");

    if (sortkeylength > 64) {
        fprintf(f, "Sortkey exceeds pre-allocated size");
    }

    fprintf(f, "[");
    while (TRUE) {
        fprintf(f, "%02x", *psortkey);
        psortkey ++;
        if ((*psortkey) == 0) {
            break;
        }
        fprintf(f, " ");
    }
    fprintf(f, "]\n");
}

/**
* Serializes the contraction within the given argument rule
* @param f file output stream
* @param r rule
* @param rlen rule length
* @param contractionsonly flag to indicate if only contractions are to be 
*                         output or all collation elements
* @param iter iterator to iterate over collation elements
*/
void serialize(FILE *f, UChar *rule, int rlen, UBool contractiononly, 
               UCollationElements *iter) {
    const UChar           *current  = NULL;
          uint32_t         strength = 0;
          uint32_t         chOffset = 0; 
          uint32_t         chLen    = 0;
          uint32_t         exOffset = 0; 
          uint32_t         exLen    = 0;
          uint8_t          specs    = 0;
          UBool            rstart   = TRUE;
          UColTokenParser  src;
          UColOptionSet    opts;
          UErrorCode       error    = U_ZERO_ERROR;
    
    src.opts = &opts;
      
    src.source       = src.current = rule;
    src.end          = rule + rlen;
    src.extraCurrent = src.end;
    src.extraEnd     = src.end + UCOL_TOK_EXTRA_RULE_SPACE_SIZE;
        
    while ((current = ucol_tok_parseNextToken(&src, &strength, &chOffset, 
                                              &chLen, &exOffset, &exLen,
                                              &specs, rstart, &error)) 
                                              != NULL) {
        // contractions handled here
        if (!contractiononly || chLen > 1) {
            ucol_setText(iter, rule + chOffset, chLen, &error);
            if (U_FAILURE(error)) {
                fprintf(stdout, "Error setting text in iterator\n");
                return;
            }
            serialize(f, iter);
        }
        rstart = FALSE;
    }
}

/**
* Output the collation element belonging to the locale into a file
* @param fullrules flag to indicate if only tailored collation elements are to
*        be output or all collation elements
*/
void serialize(UBool tailoredonly) {
    UErrorCode  error              = U_ZERO_ERROR;
    UChar       str[128];
    int         strlen = 0;

    fprintf(OUTPUT_, "# This file contains the serialized collation elements\n");
    fprintf(OUTPUT_, "# as of the collation version indicated below.\n");
    fprintf(OUTPUT_, "# Data format: xxxx xxxx..; [yyyyyyyy yyyyyy..] [zz zz..\n");
    fprintf(OUTPUT_, "#              where xxxx are codepoints in hexadecimals,\n");
    fprintf(OUTPUT_, "#              yyyyyyyy are the corresponding\n");
    fprintf(OUTPUT_, "#              collation elements in hexadecimals\n");
    fprintf(OUTPUT_, "#              and zz are the sortkey values in hexadecimals\n");
    
    UCollationElements *iter = ucol_openElements(COLLATOR_, str, strlen, 
                                                 &error);
    if (U_FAILURE(error)) {
        fprintf(stdout, "Error creating iterator\n");
        return;
    }

    if (!tailoredonly) {
        fprintf(OUTPUT_, "\n# Range of unicode characters\n\n");
        UChar32     codepoint          = 0;
        while (codepoint <= UCHAR_MAX_VALUE) { 
            if (u_isdefined(codepoint)) {
                strlen = 0;
                UTF16_APPEND_CHAR_UNSAFE(str, strlen, codepoint);
                str[strlen] = 0;
                ucol_setText(iter, str, strlen, &error);
                if (U_FAILURE(error)) {
                    fprintf(stdout, "Error setting text in iterator\n");
                    return;
                }
                serialize(OUTPUT_, iter);
            }
            codepoint ++;
        }
    }

    UChar    ucarules[0x10000];
    UChar   *rules;
    int32_t  rulelength = 0;
    rules      = ucarules;
    
    if (tailoredonly) {
              int32_t  rulelength = 0;
        const UChar   *temp = ucol_getRules(COLLATOR_, &rulelength);
        if (rulelength + UCOL_TOK_EXTRA_RULE_SPACE_SIZE > 0x10000) {
            rules = (UChar *)malloc(sizeof(UChar) * 
                                (rulelength + UCOL_TOK_EXTRA_RULE_SPACE_SIZE));
        }
        memcpy(rules, temp, rulelength * sizeof(UChar));
        rules[rulelength] = 0;
        fprintf(OUTPUT_, "\n# Tailorings\n\n");
        serialize(OUTPUT_, rules, rulelength, FALSE, iter);
        if (rules != ucarules) {
            free(rules);
        }
    }
    else {        
        rulelength = ucol_getRulesEx(COLLATOR_, UCOL_FULL_RULES, ucarules, 
                                     0x10000);
        if (rulelength + UCOL_TOK_EXTRA_RULE_SPACE_SIZE > 0x10000) {
            rules = (UChar *)malloc(sizeof(UChar) * 
                                (rulelength + UCOL_TOK_EXTRA_RULE_SPACE_SIZE));
            rulelength = ucol_getRulesEx(COLLATOR_, UCOL_FULL_RULES, rules, 
                                         rulelength);
        }
        fprintf(OUTPUT_, "\n# Contractions\n\n");
        serialize(OUTPUT_, rules, rulelength, TRUE, iter);
        if (rules != ucarules) {
            free(rules);
        }
    }
        
    ucol_closeElements(iter);
}

/**
* Output the collation element into a file
*/
void serialize() {
    const char    *locale      = (char *)options[2].value;
          int32_t  localeindex = 0;
    char filename[128];
    int  dirlength = 0;

    if (options[4].doesOccur) {
        const char *dir      = options[4].value;
        strcpy(filename, dir);
        dirlength = strlen(dir);
        char dirending = dir[dirlength - 1];
        if (dirending != U_FILE_SEP_CHAR) {
            filename[dirlength] = U_FILE_SEP_CHAR;
            filename[dirlength + 1] = 0;
        }
        dirlength ++;
    }
    if (strcmp(locale, "all") == 0) {
        if (options[4].doesOccur) {
            strcat(filename, "UCA.txt");
            OUTPUT_ = fopen(filename, "w");
            if (OUTPUT_ == NULL) {
                fprintf(stdout, "Cannot open file:%s\n", filename);
                return;
            }
        }
        fprintf(OUTPUT_, "# UCA collation elements\n");
        fprintf(stdout, "UCA\n");
        UErrorCode error = U_ZERO_ERROR;
        COLLATOR_ = ucol_open("en_US", &error);
        if (U_FAILURE(error)) {
            fprintf(stdout, "Collator creation failed:");
            fprintf(stdout, u_errorName(error));
            return;
        }
        UVersionInfo version;
        ucol_getVersion(COLLATOR_, version);
        fprintf(OUTPUT_, "# Collation version number: %d.%d.%d.%d\n", 
                         version[0], version[1], version[2], version[3]);

        serialize(FALSE);
        if (options[4].doesOccur) {
            filename[dirlength] = 0;
            fclose(OUTPUT_);
        }
        ucol_close(COLLATOR_);
        localeindex = ucol_countAvailable() - 1;
        fprintf(stdout, "Number of locales: %d\n", localeindex + 1);
        locale      = ucol_getAvailable(localeindex);
    }

    while (TRUE) {
        UErrorCode error = U_ZERO_ERROR;
        COLLATOR_ = ucol_open(locale, &error);
        if (U_FAILURE(error)) {
            fprintf(stdout, "Collator creation failed:");
            fprintf(stdout, u_errorName(error));
            return;
        }

        if (options[4].doesOccur) {
            strcat(filename, locale);
            strcat(filename, ".txt");
            OUTPUT_ = fopen(filename, "w");
            if (OUTPUT_ == NULL) {
                fprintf(stdout, "Cannot open file:%s\n", filename);
                return;
            }
        }
        
        fprintf(OUTPUT_, "# Locale: %s\n", locale);
        fprintf(stdout, "Locale: %s\n", locale);
        UVersionInfo version;
        ucol_getVersion(COLLATOR_, version);
        fprintf(OUTPUT_, "# Collation version number: %d.%d.%d.%d\n", 
                         version[0], version[1], version[2], version[3]);

        if (options[3].doesOccur) {
            serialize(TRUE);
        }

        ucol_close(COLLATOR_);

        if (options[4].doesOccur) {
            filename[dirlength] = 0;
            fclose(OUTPUT_);
        }
    
        localeindex --;
        if (localeindex < 0) {
            break;
        }
        locale = ucol_getAvailable(localeindex);
    }
}

/** 
* Main   --  process command line, read in and pre-process the test file,
*            call other functions to do the actual tests.
*/
int main(int argc, char *argv[]) {
    
    argc = u_parseArgs(argc, argv, sizeof(options)/sizeof(options[0]), 
                       options);
    
    // error handling, printing usage message
    if (argc < 0) {
        fprintf(stdout, "error in command line argument: ");
        fprintf(stdout, argv[-argc]);
        fprintf(stdout, "\n");
    }
    if (argc < 0 || options[0].doesOccur || options[1].doesOccur) {
        fprintf(stdout, "Usage: strperf options...\n"
                        "--help                 Display this message.\n"
                        "--locale    [name|all] ICU locale to use. Default is en_US\n"
                        "--serialize            Serializes the collation elements in -locale or all locales available and outputs them into --outputdir/locale_ce.txt\n"
                        "--destdir   dir_name   Path for outputing the serialized collation elements. Defaults to stdout if no defined\n");
        return argc < 0 ? U_ILLEGAL_ARGUMENT_ERROR : U_ZERO_ERROR;
    }

    OUTPUT_ = stdout;
    if (options[3].doesOccur) {
        serialize();
    }
    return 0;
}
