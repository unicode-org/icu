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
*     dumpce options... please check main function.
*/
#include <unicode/utypes.h>
#include <unicode/ucol.h>
#include <unicode/uloc.h>
#include <unicode/ucoleitr.h>
#include <unicode/uchar.h>
#include <unicode/utf16.h>
#include <unicode/putil.h>
#include <unicode/ustring.h>
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
    {"locale",        NULL, NULL, NULL, 'l', UOPT_REQUIRES_ARG, 0},
    {"serialize",     NULL, NULL, NULL, 'z', UOPT_NO_ARG, 0},
	UOPTION_DESTDIR,
    UOPTION_SOURCEDIR,
    {"attribute",     NULL, NULL, NULL, 'a', UOPT_REQUIRES_ARG, 0},
    {"rule",          NULL, NULL, NULL, 'r', UOPT_REQUIRES_ARG, 0},
    {"normalization", NULL, NULL, NULL, 'n', UOPT_REQUIRES_ARG, 0},
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

static UColAttributeValue ATTRIBUTE_[UCOL_ATTRIBUTE_COUNT] = {
    UCOL_DEFAULT, UCOL_DEFAULT, UCOL_DEFAULT, UCOL_DEFAULT, UCOL_DEFAULT, 
    UCOL_DEFAULT
};

static UColAttributeValue NORMALIZATION_ = UCOL_DEFAULT;

typedef struct {
    int   value;
    char *name;
} EnumNameValuePair;

static const EnumNameValuePair ATTRIBUTE_NAME_[] = {
    {UCOL_FRENCH_COLLATION, "UCOL_FRENCH_COLLATION"},
    {UCOL_ALTERNATE_HANDLING, "UCOL_ALTERNATE_HANDLING"}, 
    {UCOL_CASE_FIRST, "UCOL_CASE_FIRST"}, 
    {UCOL_CASE_LEVEL, "UCOL_CASE_LEVEL"}, 
    {UCOL_NORMALIZATION_MODE, 
        "UCOL_NORMALIZATION_MODE|UCOL_DECOMPOSITION_MODE"},
    {UCOL_STRENGTH, "UCOL_STRENGTH"},
    NULL
};
     
static const EnumNameValuePair ATTRIBUTE_VALUE_[] = {
    {UCOL_PRIMARY, "UCOL_PRIMARY"},
    {UCOL_SECONDARY, "UCOL_SECONDARY"},
    {UCOL_TERTIARY, "UCOL_TERTIARY|UCOL_DEFAULT_STRENGTH"},
    {UCOL_QUATERNARY, "UCOL_QUATERNARY"},
    {UCOL_IDENTICAL, "UCOL_IDENTICAL"},
    {UCOL_OFF, "UCOL_OFF"},
    {UCOL_ON, "UCOL_ON"},
    {UCOL_SHIFTED, "UCOL_SHIFTED"},
    {UCOL_NON_IGNORABLE, "UCOL_NON_IGNORABLE"},
    {UCOL_LOWER_FIRST, "UCOL_LOWER_FIRST"},
    {UCOL_UPPER_FIRST, "UCOL_UPPER_FIRST"},
    {UCOL_ON_WITHOUT_HANGUL, "UCOL_ON_WITHOUT_HANGUL"},
    NULL
};

static const EnumNameValuePair NORMALIZATION_VALUE_[] = {
    {UNORM_NONE, "UNORM_NONE"},
    {UNORM_NFD, "UNORM_NFD"},
    {UNORM_NFKD, "UNORM_NFKD"},
    {UNORM_NFC, "UNORM_NFC|UNORM_DEFAULT"},
    {UNORM_NFKC, "UNORM_NFKC"},
    NULL
};

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
    if (options[9].doesOccur) {
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
    
    while (TRUE) {
        fprintf(f, "[");
        if (UCOL_PRIMARYORDER(ce) != 0) {
            fprintf(f, "%04x", UCOL_PRIMARYORDER(ce));
        }
        fprintf(f, ",");
        if (UCOL_SECONDARYORDER(ce) != 0) {
            fprintf(f, " %02x", UCOL_SECONDARYORDER(ce));
        }
        fprintf(f, ",");
        if (UCOL_TERTIARYORDER(ce) != 0) {
            fprintf(f, " %02x", UCOL_TERTIARYORDER(ce));
        }
        fprintf(f, "] ");

        ce = ucol_next(iter, &error);
        if (ce == UCOL_NULLORDER) {
            break;
        }
        if (U_FAILURE(error)) {
            fprintf(stdout, "Error retrieving collation elements");
            return;
        }
    }
    
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
          UParseError      parseError;
          UErrorCode       error    = U_ZERO_ERROR;
    
    src.opts = &opts;
      
    src.source       = src.current = rule;
    src.end          = rule + rlen;
    src.extraCurrent = src.end;
    src.extraEnd     = src.end + UCOL_TOK_EXTRA_RULE_SPACE_SIZE;
        
    while ((current = ucol_tok_parseNextToken(&src, &strength, &chOffset, 
                                              &chLen, &exOffset, &exLen,
                                              &specs, rstart, &parseError,
                                              &error)) 
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
* Prints the attribute values in the argument collator into the output stream
* @param collator
*/
void outputAttribute(UCollator *collator, UErrorCode *error) 
{
    UColAttribute attribute = UCOL_FRENCH_COLLATION;
    while (attribute < UCOL_ATTRIBUTE_COUNT) {
        int count = 0;
        while (TRUE) {
            // getting attribute name
            if (ATTRIBUTE_NAME_[count].value == attribute) {
                fprintf(OUTPUT_, "%s = ", ATTRIBUTE_NAME_[count].name);
                break;
            }
            count ++;
        }
        count = 0;
        int attributeval = ucol_getAttribute(collator, attribute, error);
        if (U_FAILURE(*error)) {
            fprintf(stdout, "Failure in reading collator attribute\n");
            return;
        }
        while (TRUE) {
            // getting attribute value
            if (ATTRIBUTE_VALUE_[count].value == attributeval) {
                fprintf(OUTPUT_, "%s\n", ATTRIBUTE_VALUE_[count].name);
                break;
            }
            count ++;
        }
        attribute = (UColAttribute)(attribute + 1);
    }
}

/**
* Prints the normalization mode in the argument collator into the output stream
* @param collator
*/
void outputNormalization(UCollator *collator) 
{
    int normmode = ucol_getNormalization(collator);
    int count = 0;
    while (TRUE) {
        // getting attribute name
        if (NORMALIZATION_VALUE_[count].value == normmode) {
            break;
        }
        count ++;
    }
    fprintf(OUTPUT_, "NORMALIZATION MODE = %s\n", 
        NORMALIZATION_VALUE_[count].name);
}

/**
* Output the collation element belonging to the locale into a file
* @param locale string
* @param fullrules flag to indicate if only tailored collation elements are to
*        be output or all collation elements
*/
void serialize(const char *locale, UBool tailoredonly) {
    UErrorCode  error              = U_ZERO_ERROR;
    UChar       str[128];
    int         strlen = 0;

    fprintf(OUTPUT_, "# This file contains the serialized collation elements\n");
    fprintf(OUTPUT_, "# as of the collation version indicated below.\n");
    fprintf(OUTPUT_, "# Data format: xxxx xxxx..; [yyyy, yy, yy] [yyyy, yy, yy] ... [yyyy, yy, yy] [zz zz..\n");
    fprintf(OUTPUT_, "#              where xxxx are codepoints in hexadecimals,\n");
    fprintf(OUTPUT_, "#              yyyyyyyy are the corresponding\n");
    fprintf(OUTPUT_, "#              collation elements in hexadecimals\n");
    fprintf(OUTPUT_, "#              and zz are the sortkey values in hexadecimals\n");

    fprintf(OUTPUT_, "\n# Collator information\n");

    fprintf(OUTPUT_, "\nLocale: %s\n", locale);
    fprintf(stdout, "Locale: %s\n", locale);
    UVersionInfo version;
    ucol_getVersion(COLLATOR_, version);
    fprintf(OUTPUT_, "Version number: %d.%d.%d.%d\n", 
                      version[0], version[1], version[2], version[3]);
    outputAttribute(COLLATOR_, &error);
    outputNormalization(COLLATOR_);
    
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
* Sets the collator with the attribute values
* @param collator
* @param error status
*/
void setAttributes(UCollator *collator, UErrorCode *error) 
{
    int count = 0;
    while (count < UCOL_ATTRIBUTE_COUNT) {
        if (ATTRIBUTE_[count] != UCOL_DEFAULT) {
            ucol_setAttribute(collator, (UColAttribute)count, 
                              ATTRIBUTE_[count], error);
            if (U_FAILURE(*error)) {
                return;
            }
        }
        count ++;
    }
}

/**
* Sets the collator with the normalization mode
* @param collator
*/
void setNormalization(UCollator *collator) 
{
    if (NORMALIZATION_ != UCOL_DEFAULT) {
        ucol_setNormalization(collator, NORMALIZATION_);
    }
}

/**
* Appends directory path with an ending seperator if necessary.
* @param path with enough space to append one seperator
* @return new directory path length
*/
int appendDirSeparator(char *dir) 
{
    int dirlength = strlen(dir);
    char dirending = dir[dirlength - 1];
    if (dirending != U_FILE_SEP_CHAR) {
        dir[dirlength] = U_FILE_SEP_CHAR;
        dir[dirlength + 1] = 0;
        return dirlength + 1;
    }
    return dirlength;
}

/**
* Output the collation element into a file
*/
void serialize() {
    char filename[128];
    int  dirlength = 0;

    if (options[4].doesOccur) {
        strcpy(filename, options[4].value);
        dirlength = appendDirSeparator(filename);
    }

    if (options[2].doesOccur) {
        const char    *locale      = (char *)options[2].value;
              int32_t  localeindex = 0;
        
        if (strcmp(locale, "all") == 0) {
            if (options[4].doesOccur) {
                strcat(filename, "UCA.txt");
                OUTPUT_ = fopen(filename, "w");
                if (OUTPUT_ == NULL) {
                    fprintf(stdout, "Cannot open file:%s\n", filename);
                    return;
                }
            }
            fprintf(stdout, "UCA\n");
            UErrorCode error = U_ZERO_ERROR;
            COLLATOR_ = ucol_open("en_US", &error);
            if (U_FAILURE(error)) {
                fprintf(stdout, "Collator creation failed:");
                fprintf(stdout, u_errorName(error));
                goto CLOSEUCA;
                return;
            }
            setAttributes(COLLATOR_, &error);
            setNormalization(COLLATOR_);
            if (U_FAILURE(error)) {
                fprintf(stdout, "Collator attribute setting failed:");
                fprintf(stdout, u_errorName(error));
                goto CLOSEUCA;
                return;
            }
        
            serialize("UCA", FALSE);
CLOSEUCA :  
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
                goto CLOSETAILOR;
                return;
            }
            setAttributes(COLLATOR_, &error);
            setNormalization(COLLATOR_);
            if (U_FAILURE(error)) {
                fprintf(stdout, "Collator attribute setting failed:");
                fprintf(stdout, u_errorName(error));
                goto CLOSETAILOR;
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

            if (options[3].doesOccur) {
                serialize(locale, TRUE);
            }

            ucol_close(COLLATOR_);

CLOSETAILOR : 
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

    if (options[7].doesOccur) {
        char inputfilename[128];
        // rules are to be used
        if (options[5].doesOccur) {
            strcpy(inputfilename, options[5].value);
            appendDirSeparator(inputfilename);
        }
        strcat(inputfilename, options[7].value);
        FILE *input = fopen(inputfilename, "r");
        if (input == NULL) {
            fprintf(stdout, "Cannot open file:%s\n", filename);
            return;
        }
        
        char   s[1024];
        UChar  rule[1024];
        UChar *prule = rule;
        int    size = 1024;
        // synwee TODO: make this part dynamic
        while (fscanf(input, "%[^\n]s", s) != EOF) {
            size -= u_unescape(s, prule, size);
            prule = prule + u_strlen(prule);
        }
        fclose(input);

        if (options[4].doesOccur) {
            strcat(filename, "Rules.txt");
            OUTPUT_ = fopen(filename, "w");
            if (OUTPUT_ == NULL) {
                fprintf(stdout, "Cannot open file:%s\n", filename);
                return;
            }
        }

        fprintf(stdout, "Rules\n");
        UErrorCode  error = U_ZERO_ERROR;
        UParseError parseError;
        COLLATOR_ = ucol_openRules(rule, u_strlen(rule), NORMALIZATION_, 
                                   UCOL_DEFAULT_STRENGTH, &parseError, &error);
        if (U_FAILURE(error)) {
            fprintf(stdout, "Collator creation failed:");
            fprintf(stdout, u_errorName(error));
            goto CLOSERULES;
            return;
        }
        setAttributes(COLLATOR_, &error);
        if (U_FAILURE(error)) {
            fprintf(stdout, "Collator attribute setting failed:");
            fprintf(stdout, u_errorName(error));
            goto CLOSERULES;
            return;
        }
        
        serialize("Rule-based", TRUE);
        ucol_close(COLLATOR_);

CLOSERULES :
        if (options[4].doesOccur) {
            filename[dirlength] = 0;
            fclose(OUTPUT_);
        }
    }
}

/**
* Parse for enum values.
* Note this only works for positive enum values.
* @param enumarray array containing names of the enum values in string and 
*        their corresponding value.
*        declared enum value.
* @param str string to be parsed
* @return corresponding integer enum value or -1 if value is not found.
*/
int parseEnums(const EnumNameValuePair enumarray[], const char *str) 
{
    const char *enumname = enumarray[0].name;
    int result = atoi(str);
    if (result == 0 && str[0] != '0') {
        while (strcmp(enumname, str) != 0) {
            // checking for multiple enum names sharing the same values
            enumname = strstr(enumname, str);
            if (enumname != NULL) {
                int size = strchr(enumname, '|') - enumname;
                if (size < 0) {
                    size = strlen(enumname);
                }
                if (size == (int)strlen(str)) {
                    return enumarray[result].value;
                }
            }
            result ++;
            if (&(enumarray[result]) == NULL) {
                return -1;
            }
            enumname = enumarray[result].name;
        }
    }
    return -1;
}

/**
* Parser for attribute name value pair
*/
void parseAttributes() {
    char str[32];
    const char *pname = options[6].value;
    const char *pend  = options[6].value + strlen(options[6].value);
    const char *pvalue;
    
    while (pname < pend) {
        pvalue = strchr(pname, '=');
        if (pvalue == NULL) {
            fprintf(stdout, 
                    "No matching value found for attribute argument %s\n", 
                    pname);        
            return;
        }
        int count = pvalue - pname;
        strncpy(str, pname, count);
        str[count] = 0;

        int name = parseEnums(ATTRIBUTE_NAME_, str);
        if (name == -1) {
            fprintf(stdout, "Attribute name not found: %s\n", str);
            return;
        }
        
        pvalue ++;
        // getting corresponding enum value
        pname = strchr(pvalue, ',');
        if (pname == NULL) {
            pname = pend;
        }
        count = pname - pvalue;
        strncpy(str, pvalue, count);
        str[count] = 0;
        int value = parseEnums(ATTRIBUTE_VALUE_, str);
        if (value == -1) {
            fprintf(stdout, "Attribute value not found: %s\n", str);
            return;
        }
        ATTRIBUTE_[name] = (UColAttributeValue)value;
        pname ++;
    }
}

/**
* Parser for normalization mode
*/
void parseNormalization() {
    const char *str = options[6].value;
    int norm = parseEnums(NORMALIZATION_VALUE_, str);
    if (norm == -1) {
        fprintf(stdout, "Normalization mode not found: %s\n", str);
        return;
    }
    NORMALIZATION_ = (UColAttributeValue)norm;
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
                        "--help\n"
                        "    Display this message.\n"
                        "--locale name|all\n"
                        "    ICU locale to use. Default is en_US\n"
                        "--serialize\n"
                        "    Serializes the collation elements in -locale or all locales available and outputs them into --outputdir/locale_ce.txt\n"
                        "--destdir dir_name\n"
                        "    Path for outputing the serialized collation elements. Defaults to stdout if no defined\n"
                        "--sourcedir dir_name\n"
                        "    Path for the input rule file for collation\n"
                        "--attribute name=value,name=value...\n" 
                        "    Pairs of attribute names and values for setting\n"
                        "--rule filename\n" 
                        "    Name of file containing the collation rules.\n"
                        "--normalizaton mode\n" 
                        "    UNormalizationMode mode to be used.\n");
        fprintf(stdout, "Example: dumpce --serialize --locale af --destdir /temp --attribute UCOL_STRENGTH=UCOL_DEFAULT_STRENGTH,4=17");
        return argc < 0 ? U_ILLEGAL_ARGUMENT_ERROR : U_ZERO_ERROR;
    }

    OUTPUT_ = stdout;
    if (options[6].doesOccur) {
        fprintf(stdout, "attributes %s\n", options[6].value);
        parseAttributes();
    }
    if (options[8].doesOccur) {
        fprintf(stdout, "normalization mode %s\n", options[8].value);
        parseNormalization();
    }
    if (options[3].doesOccur) {
        serialize();
    }
    return 0;
}
