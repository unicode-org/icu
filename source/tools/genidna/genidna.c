/*
*******************************************************************************
*
*   Copyright (C) 2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  genidna.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2003-02-06
*   created by: Ram Viswanadha
*
*   This program reads the rfc3454_*.txt files,
*   parses them, and extracts the data for Nameprep conformance.
*   It then preprocesses it and writes a binary file for efficient use
*   in various IDNA conversion processes.
*/

#include <stdio.h>
#include <stdlib.h>
#include "unicode/utypes.h"
#include "unicode/uchar.h"
#include "unicode/putil.h"
#include "cmemory.h"
#include "cstring.h"
#include "unicode/udata.h"
#include "unewdata.h"
#include "uoptions.h"
#include "uparse.h"
#include "unicode/uset.h"
#include "uprops.h"

U_CDECL_BEGIN
#include "genidna.h"
U_CDECL_END

#ifdef WIN32
#   pragma warning(disable: 4100)
#endif

UBool beVerbose=FALSE, haveCopyright=TRUE, printRules = FALSE;

/* prototypes --------------------------------------------------------------- */

static void
parseMappings(const char *filename, UBool withNorm, UBool reportError, UErrorCode *pErrorCode);

static void
parseTable(const char *filename, UBool isUnassigned, UErrorCode *pErrorCode);

static void
parseNormalizationCorrections(const char *filename, UErrorCode *pErrorCode);

/*static void 
setLDHValues(UErrorCode* pErrorCode);*/

static void
setLabelSeperators(UErrorCode* pErrorCode);

static void 
printMapping(UChar32 cp,UChar32* mapping, int32_t mappingLength);

static const char* fileNames[] = {
                                    "rfc3454_A_1.txt", /* contains unassigned code points */
                                    "rfc3454_C_X.txt", /* contains code points that are prohibited */
                                    "rfc3454_B_1.txt", /* contains case mappings when normalization is turned off */
                                    "rfc3454_B_2.txt", /* contains case mappings when normalization it turned on */
                                    "NormalizationCorrections.txt",/* normalization corrections  */
                                };
static const char *UNIDATA_DIR = "unidata";
static const char *MISC_DIR    = "misc";

/* -------------------------------------------------------------------------- */

static UOption options[]={
    UOPTION_HELP_H,
    UOPTION_HELP_QUESTION_MARK,
    UOPTION_VERBOSE,
    UOPTION_COPYRIGHT,
    UOPTION_DESTDIR,
    UOPTION_SOURCEDIR,
    { "unicode", NULL, NULL, NULL, 'u', UOPT_REQUIRES_ARG, 0 },
    { "generate-rules", NULL, NULL, NULL, 'g', UOPT_NO_ARG, 0 }
};

extern int
main(int argc, char* argv[]) {
#if !UCONFIG_NO_IDNA
    char* filename = NULL;
#endif
    const char *srcDir=NULL, *destDir=NULL, *suffix=NULL;
    char *basename=NULL;
    char *saveBasename = NULL;
    UErrorCode errorCode=U_ZERO_ERROR;

    U_MAIN_INIT_ARGS(argc, argv);

    /* preset then read command line options */
    options[4].value=u_getDataDirectory();
    options[5].value="";
    options[6].value="3.0.0";
    argc=u_parseArgs(argc, argv, sizeof(options)/sizeof(options[0]), options);

    /* error handling, printing usage message */
    if(argc<0) {
        fprintf(stderr,
            "error in command line argument \"%s\"\n",
            argv[-argc]);
    }
    if(argc<0 || options[0].doesOccur || options[1].doesOccur) {
        /*
         * Broken into chucks because the C89 standard says the minimum
         * required supported string length is 509 bytes.
         */
        fprintf(stderr,
            "Usage: %s [-options] [suffix]\n"
            "\n"
            "Read the rfc3454_*.txt files and\n"
            "create a binary file " U_ICUDATA_NAME "_" DATA_NAME "." DATA_TYPE " with the normalization data\n"
            "\n",
            argv[0]);
        fprintf(stderr,
            "Options:\n"
            "\t-h or -? or --help		this usage text\n"
            "\t-v or --verbose			verbose output\n"
            "\t-c or --copyright		include a copyright notice\n");
        fprintf(stderr,
            "\t-d or --destdir			destination directory, followed by the path\n"
            "\t-s or --sourcedir		source directory of ICU data, followed by the path\n"
            "\t-g or --generate-rules   generate IDN rules for testing. Will print out rules to STDOUT\n"
            );
        return argc<0 ? U_ILLEGAL_ARGUMENT_ERROR : U_ZERO_ERROR;
    }

    /* get the options values */
    beVerbose=options[2].doesOccur;
    haveCopyright=options[3].doesOccur;
    srcDir=options[5].value;
    destDir=options[4].value;
    printRules = options[7].doesOccur;

    if(argc>=2) {
        suffix=argv[1];
    } else {
        suffix=NULL;
    }

#if UCONFIG_NO_IDNA

    fprintf(stderr,
        "genidna writes dummy " U_ICUDATA_NAME "_" DATA_NAME "." DATA_TYPE
        " because UCONFIG_NO_IDNA is set, \n"
        "see icu/source/common/unicode/uconfig.h\n");
    generateData(destDir);

#else

    setUnicodeVersion(options[6].value);
    filename = (char* ) uprv_malloc(uprv_strlen(srcDir) + 300); /* hopefully this should be enough */
    /* prepare the filename beginning with the source dir */
    if(uprv_strchr(srcDir,U_FILE_SEP_CHAR) == NULL){
        filename[0] = 0x2E;
        filename[1] = U_FILE_SEP_CHAR;
        uprv_strcpy(filename+2,srcDir);
    }else{
        uprv_strcpy(filename, srcDir);
    }
    basename=filename+uprv_strlen(filename);
    if(basename>filename && *(basename-1)!=U_FILE_SEP_CHAR) {
        *basename++=U_FILE_SEP_CHAR;
    }
    
    /* initialize */
    init();
    if(printRules){
        printf("// Copyright (C) 2003, International Business Machines\n\n");
        printf("// WARNING: This file is machine generated by %s tool. Please DO NOT edit.\n\n",argv[0]);

        printf("idn_rules{\n");
    }

    /* first copy misc directory */
    saveBasename = basename;
    uprv_strcpy(basename,MISC_DIR);
    basename = basename + uprv_strlen(MISC_DIR);
    *basename++=U_FILE_SEP_CHAR;

    /* process unassigned */
    uprv_strcpy(basename,fileNames[0]);
    parseTable(filename,TRUE, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "Could not open file %s for reading \n", filename);
        return errorCode;
    }
    /* process prohibited */
    uprv_strcpy(basename,fileNames[1]);
    parseTable(filename,FALSE,  &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "Could not open file %s for reading \n", filename);
        return errorCode;
    }

    /*  setLDHValues(&errorCode); */
    setLabelSeperators(&errorCode);

    /* process mappings */
    if(printRules){
        printf("\n\tMapNoNormalization{\n");
    }
    uprv_strcpy(basename,fileNames[2]);
    parseMappings(filename, FALSE, FALSE, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "Could not open file %s for reading \n", filename);
        return errorCode;
    }
    if(printRules){
        printf("\n\t}\n");
    }
    
    if(printRules){
        printf("\n\tMapNFKC{\n");
    }
    uprv_strcpy(basename,fileNames[3]);
    parseMappings(filename, TRUE, FALSE, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "Could not open file %s for reading \n", filename);
        return errorCode;
    }
    /* set up directory for NormalizationCorrections.txt */
    basename = saveBasename;
    uprv_strcpy(basename,UNIDATA_DIR);
    basename = basename + uprv_strlen(UNIDATA_DIR);
    *basename++=U_FILE_SEP_CHAR;
    uprv_strcpy(basename,fileNames[4]);
    
    parseNormalizationCorrections(filename,&errorCode);
    if(U_FAILURE(errorCode)){
        fprintf(stderr,"Could not open file %s for reading \n", filename);
        return errorCode;
    }

    /* process parsed data */
    if(U_SUCCESS(errorCode)) {
        /* write the data file */
       generateData(destDir);

       cleanUpData();
    }
    if(printRules){
        printf("\t\t\"::[:AGE=3.2:]NFKC;\"\n\t}\n}");
    }

    uprv_free(filename);

#endif

    return errorCode;
}

#if !UCONFIG_NO_IDNA

static void U_CALLCONV
normalizationCorrectionsLineFn(void *context,
                    char *fields[][2], int32_t fieldCount,
                    UErrorCode *pErrorCode) {
    uint32_t mapping[40];
    char *end, *s;
    uint32_t code;
    int32_t length;
    UVersionInfo version;
    UVersionInfo thisVersion;

    /* get the character code, field 0 */
    code=(uint32_t)uprv_strtoul(fields[0][0], &end, 16);
    if(U_FAILURE(*pErrorCode)) {
        fprintf(stderr, "genidn: error parsing FCNFKC_3_2_0.txt mapping at %s\n", fields[0][0]);
        exit(*pErrorCode);
    }
    /* Original (erroneous) decomposition */
    s = fields[1][0];

    /* parse the mapping string */
    length=u_parseCodePoints(s, mapping, sizeof(mapping)/4, pErrorCode);

    /* ignore corrected decomposition */

    u_versionFromString(version,fields[3][0] );
    u_versionFromString(thisVersion, "3.2.0");



    if(U_FAILURE(*pErrorCode)) {
        fprintf(stderr, "genidn error parsing NormalizationCorrection of U+%04lx - %s\n",
                (long)code, u_errorName(*pErrorCode));
        exit(*pErrorCode);
    }

    /* store the mapping */
    if( version[0] > thisVersion[0] || 
        ((version[0]==thisVersion[0]) && (version[1] > thisVersion[1]))
        ){
        storeMapping(code,mapping, length, TRUE, pErrorCode);
        if(printRules){
            printMapping(code,(UChar32*)mapping,length);
        }
    }
}

static void
parseNormalizationCorrections(const char *filename, UErrorCode *pErrorCode) {
    char *fields[4][2];

    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return;
    }

    u_parseDelimitedFile(filename, ';', fields, 4, normalizationCorrectionsLineFn, NULL, pErrorCode);

    /* fprintf(stdout,"Number of code points that have NormalizationCorrections mapping with length >1 : %i\n",len); */

    if(U_FAILURE(*pErrorCode) && ( *pErrorCode!=U_FILE_ACCESS_ERROR)) {
        fprintf(stderr, "genidn error: u_parseDelimitedFile(\"%s\") failed - %s\n", filename, u_errorName(*pErrorCode));
        exit(*pErrorCode);
    }
}

static void U_CALLCONV
caseMapLineFn(void *context,
              char *fields[][2], int32_t fieldCount,
              UErrorCode *pErrorCode) {
    uint32_t mapping[40];
    char *end, *s;
    uint32_t code;
    int32_t length;
    UBool* mapWithNorm = (UBool*) context;


    /* get the character code, field 0 */
    code=(uint32_t)uprv_strtoul(fields[0][0], &end, 16);
    if(end<=fields[0][0] || end!=fields[0][1]) {
        fprintf(stderr, "genidn: syntax error in field 0 at %s\n", fields[0][0]);
        *pErrorCode=U_PARSE_ERROR;
        exit(U_PARSE_ERROR);
    }

    s = fields[1][0];
    /* parse the mapping string */
    length=u_parseCodePoints(s, mapping, sizeof(mapping)/4, pErrorCode);

    if(U_FAILURE(*pErrorCode)) {
        fprintf(stderr, "genidn error parsing UnicodeData.txt decomposition of U+%04lx - %s\n",
                (long)code, u_errorName(*pErrorCode));
        exit(*pErrorCode);
    }

    /* store the mapping */

    storeMapping(code,mapping, length, *mapWithNorm, pErrorCode);
    if(printRules){
        printMapping(code,(UChar32*)mapping,length);
    }
}

static void
parseMappings(const char *filename,UBool withNorm, UBool reportError, UErrorCode *pErrorCode) {
    char *fields[3][2];

    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return;
    }

    u_parseDelimitedFile(filename, ';', fields, 3, caseMapLineFn, &withNorm, pErrorCode);

    /*fprintf(stdout,"Number of code points that have mappings with length >1 : %i\n",len);*/

    if(U_FAILURE(*pErrorCode) && (reportError || *pErrorCode!=U_FILE_ACCESS_ERROR)) {
        fprintf(stderr, "genidn error: u_parseDelimitedFile(\"%s\") failed - %s\n", filename, u_errorName(*pErrorCode));
        exit(*pErrorCode);
    }
}

/* parser for UnicodeData.txt ----------------------------------------------- */
static int32_t printedCharCount = 0;

static void printEscaped(UChar32 ch){
    if(ch > 0xFFFF){
        printf("\\\\U%08X",ch);
        printedCharCount+=11;
    }else{
        if(uprv_isRuleWhiteSpace(ch)){
            /* double escape the rule white space */
            printf("\\\\u%04X", ch);
            printedCharCount+=7;
        }else if(0x20< ch && ch <0x7f){
            if(ch == 0x2E){
                /* double escape dot */
                printf("\\\\%c",(char)ch);
                printedCharCount+=3;
            }else{
                printf("%c",(char)ch);
                printedCharCount++;
            }
        }else{
            printf("\\\\u%04X",ch);
            printedCharCount+=7;
        }
    }
}
static void printEscapedRange(UChar32 rangeStart, UChar32 rangeEnd){
    if(rangeStart != rangeEnd){
        printEscaped(rangeStart);
        printf("-");
        printedCharCount++;
        printEscaped(rangeEnd);
        printf(" ");
    }else{
        printEscaped(rangeStart);
        printf(" ");
    }
    if(printedCharCount > 70){
        printf("\"\n\t\t\t\"");
        printedCharCount =0 ;
    }
}
static void printMapping( UChar32 cp, UChar32* mapping, int32_t mappingLength){
    
    int32_t i;
    printf("\t\t\"");
    printEscaped(cp);
    printf(" > ");
    for(i=0;i<mappingLength;i++){
        printEscaped(mapping[i]);
    }
    printf(";\"\n");
    
    printedCharCount=0; 
}
static void U_CALLCONV
unicodeDataLineFn(void *context,
                  char *fields[][2], int32_t fieldCount,
                  UErrorCode *pErrorCode) {
    uint32_t rangeStart=0,rangeEnd =0;
    UBool* isUnassigned = (UBool*) context;


    u_parseCodePointRange(fields[0][0], &rangeStart,&rangeEnd, pErrorCode);
    
    if(U_FAILURE(*pErrorCode)){
        fprintf(stderr, "Could not parse code point range. Error: %s\n",u_errorName(*pErrorCode));
        return;
    }

    if(*isUnassigned == TRUE){
        storeRange(rangeStart,rangeEnd,UIDNA_UNASSIGNED, pErrorCode);
    }else{
        storeRange(rangeStart,rangeEnd,UIDNA_PROHIBITED, pErrorCode);
    }
    /*TODO: comment out the printer */
    if(printRules){
        printEscapedRange(rangeStart,rangeEnd);
    }
}

static void
parseTable(const char *filename,UBool isUnassigned, UErrorCode *pErrorCode) {
    char *fields[1][2];
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return;
    }
    /*TODO: comment out the printer */
    if(printRules){
        printedCharCount = 0;
        if(isUnassigned){
            printf("\n\tUnassignedSet{\"[ ");
        }else{
            printf("\n\tProhibitedSet{\"[ ");
        }
    }
    u_parseDelimitedFile(filename, ';', fields, 1, unicodeDataLineFn, &isUnassigned, pErrorCode);


    if(U_FAILURE(*pErrorCode)) {
        fprintf(stderr, "genidn error: u_parseDelimitedFile(\"%s\") failed - %s\n", filename, u_errorName(*pErrorCode));
        exit(*pErrorCode);
    }
    if(printRules){
        printf("]\"}\n");
    }
}

/*
static void 
setLDHValues(UErrorCode* pErrorCode){
    USet* set = uset_openPattern(LDH_PATTERN, LDH_PATTERN_LEN, pErrorCode);
    int32_t itemCount;
    int32_t index = 0;
    UChar32 start,end;

    if(U_FAILURE(*pErrorCode)){
        fprintf(stderr,"Could not open USet. Error :%s \n",u_errorName(*pErrorCode));
        exit(*pErrorCode);
    }
    
    itemCount = uset_getItemCount(set);

    for(;index < itemCount; index++){
        uset_getItem(set,index, &start, &end, NULL, 0, pErrorCode);
        storeRange(start,end,UIDNA_LDH_OR_MAP_NFKC, pErrorCode);
    }
    if(printRules){
        printf(PAT);
    }

}
*/
static void
setLabelSeperators(UErrorCode *pErrorCode){
    /* U+002E, U+3002, U+FF0E, U+FF61 */
    storeRange(0x002E, 0x002E, UIDNA_LABEL_SEPARATOR, pErrorCode);
    storeRange(0x3002, 0x3002, UIDNA_LABEL_SEPARATOR, pErrorCode);
    storeRange(0xFF0E, 0xFF0E, UIDNA_LABEL_SEPARATOR, pErrorCode);
    storeRange(0xFF61, 0xFF61, UIDNA_LABEL_SEPARATOR, pErrorCode);
    if(U_FAILURE(*pErrorCode)){
        fprintf(stderr, "Could not store values for label separators\n");
    }
    if(printRules){
        printf("\tLabelSeparatorSet{\"[ ");
        printEscaped(0x002E);
        printEscaped(0x3002);
        printEscaped(0xFF0E);
        printEscaped(0xFF61);
        printf(" ]\"}\n\n");
    }
}

#endif /* #if !UCONFIG_NO_IDNA */

/*
 * Hey, Emacs, please set the following:
 *
 * Local Variables:
 * indent-tabs-mode: nil
 * End:
 *
 */
