/*
*******************************************************************************
*
*   Copyright (C) 2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  testidn.cpp
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

#if !UCONFIG_NO_IDNA && !UCONFIG_NO_TRANSLITERATION

#include "unicode/uchar.h"
#include "unicode/putil.h"
#include "cmemory.h"
#include "cstring.h"
#include "unicode/udata.h"
#include "unewdata.h"
#include "uoptions.h"
#include "uparse.h"
#include "utrie.h"
#include "umutex.h"
#include "sprpimpl.h"
#include "testidna.h"

UBool beVerbose=FALSE, haveCopyright=TRUE;

/* prototypes --------------------------------------------------------------- */


static UBool isDataLoaded = FALSE;
static UTrie idnTrie={ 0,0,0,0,0,0,0 };
static UDataMemory *idnData=NULL;
static UErrorCode dataErrorCode =U_ZERO_ERROR;


static const uint16_t* mappingData = NULL;
static int32_t indexes[_IDNA_INDEX_TOP]={ 0 };


static void
parseMappings(const char *filename, UBool withNorm, UBool reportError,TestIDNA& test, UErrorCode *pErrorCode);

static void
parseTable(const char *filename, UBool isUnassigned, TestIDNA& test, UErrorCode *pErrorCode);

static UBool loadIDNData(UErrorCode &errorCode);

static UBool cleanup();

static void 
compareMapping(uint32_t codepoint, uint32_t* mapping, int32_t mapLength, 
               UBool withNorm);

static void
compareFlagsForRange(uint32_t start, uint32_t end,
                     UBool isUnassigned);

static void
testAllCodepoints(TestIDNA& test);

static TestIDNA* pTestIDNA =NULL;

static const char* fileNames[] = {
                                    "rfc3454_A_1.txt", /* contains unassigned code points */
                                    "rfc3454_C_X.txt", /* contains code points that are prohibited */
                                    "rfc3454_B_1.txt", /* contains case mappings when normalization is turned off */
                                    "rfc3454_B_2.txt", /* contains case mappings when normalization it turned on */
                                    /* "NormalizationCorrections.txt",contains NFKC case mappings whicha are not included in UTR 21  */
                                };
/* -------------------------------------------------------------------------- */

/* file definitions */
#define DATA_NAME "uidna"
#define DATA_TYPE "icu"

#define MISC_DIR "misc"

extern int
testData(TestIDNA& test) {
    char* filename = (char*) malloc(strlen(IntlTest::pathToDataDirectory())*3);
    //TODO get the srcDir dynamically 
    const char *srcDir=IntlTest::pathToDataDirectory();
    char *basename=NULL;
    UErrorCode errorCode=U_ZERO_ERROR;
    char *saveBasename =NULL;

    loadIDNData(errorCode);
    if(U_FAILURE(dataErrorCode)){
        test.errln( "Could not load data. Error: %s\n",u_errorName(dataErrorCode));
        return dataErrorCode;
    }
    
    //initialize
    pTestIDNA = &test;
    
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

    /* process unassigned */
    basename=filename+uprv_strlen(filename);
    if(basename>filename && *(basename-1)!=U_FILE_SEP_CHAR) {
        *basename++=U_FILE_SEP_CHAR;
    }
    
    /* first copy misc directory */
    saveBasename = basename;
    uprv_strcpy(basename,MISC_DIR);
    basename = basename + uprv_strlen(MISC_DIR);
    *basename++=U_FILE_SEP_CHAR;
    
    /* process unassigned */
    uprv_strcpy(basename,fileNames[0]);
    parseTable(filename,TRUE, test,&errorCode);
    if(U_FAILURE(errorCode)) {
        test.errln( "Could not open file %s for reading \n", filename);
        return errorCode;
    }
    /* process prohibited */
    uprv_strcpy(basename,fileNames[1]);
    parseTable(filename,FALSE, test, &errorCode);
    if(U_FAILURE(errorCode)) {
        test.errln( "Could not open file %s for reading \n", filename);
        return errorCode;
    }

    /* process mappings */
    uprv_strcpy(basename,fileNames[2]);
    parseMappings(filename, FALSE, FALSE,test, &errorCode);
    if(U_FAILURE(errorCode)) {
        test.errln( "Could not open file %s for reading \n", filename);
        return errorCode;
    }
    uprv_strcpy(basename,fileNames[3]);
    parseMappings(filename, TRUE, FALSE,test, &errorCode);
    if(U_FAILURE(errorCode)) {
        test.errln( "Could not open file %s for reading \n", filename);
        return errorCode;
    }

    testAllCodepoints(test);

    cleanup();
    pTestIDNA = NULL;
    free(filename);
    return errorCode;
}
U_CDECL_BEGIN
static void U_CALLCONV
caseMapLineFn(void *context,
              char *fields[][2], int32_t /*fieldCount*/,
              UErrorCode *pErrorCode) {
    uint32_t mapping[40];
    char *end, *s;
    uint32_t code;
    int32_t length;
    UBool* mapWithNorm = (UBool*) context;

    /* get the character code, field 0 */
    code=(uint32_t)uprv_strtoul(fields[0][0], &end, 16);
    if(end<=fields[0][0] || end!=fields[0][1]) {
        *pErrorCode=U_PARSE_ERROR;

    }

    s = fields[1][0];
    /* parse the mapping string */
    length=u_parseCodePoints(s, mapping, sizeof(mapping)/4, pErrorCode);

    /* store the mapping */

    compareMapping(code,mapping, length, *mapWithNorm);
}
U_CDECL_END

static void
parseMappings(const char *filename,UBool withNorm, UBool reportError, TestIDNA& test, UErrorCode *pErrorCode) {
    char *fields[3][2];

    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return;
    }

    u_parseDelimitedFile(filename, ';', fields, 3, caseMapLineFn, &withNorm, pErrorCode);

    //fprintf(stdout,"Number of code points that have mappings with length >1 : %i\n",len);

    if(U_FAILURE(*pErrorCode) && (reportError || *pErrorCode!=U_FILE_ACCESS_ERROR)) {
        test.errln( "testidn error: u_parseDelimitedFile(\"%s\") failed - %s\n", filename, u_errorName(*pErrorCode));
    }
}

/* parser for UnicodeData.txt ----------------------------------------------- */
U_CDECL_BEGIN

static void U_CALLCONV
unicodeDataLineFn(void *context,
                  char *fields[][2], int32_t /*fieldCount*/,
                  UErrorCode *pErrorCode) {
    uint32_t rangeStart=0,rangeEnd =0;
    UBool* isUnassigned = (UBool*) context;

    u_parseCodePointRange(fields[0][0], &rangeStart,&rangeEnd, pErrorCode);
    
    if(U_FAILURE(*pErrorCode)){
        *pErrorCode  = U_PARSE_ERROR;
        return;
    }


    compareFlagsForRange(rangeStart,rangeEnd,*isUnassigned);

}

U_CDECL_END

static void
parseTable(const char *filename,UBool isUnassigned,TestIDNA& test, UErrorCode *pErrorCode) {
    char *fields[2][2];
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return;
    }

    u_parseDelimitedFile(filename, ';', fields, 1, unicodeDataLineFn, &isUnassigned, pErrorCode);


    if(U_FAILURE(*pErrorCode)) {
        test.errln( "testidn error: u_parseDelimitedFile(\"%s\") failed - %s\n", filename, u_errorName(*pErrorCode));
    }
}

static void
testAllCodepoints(TestIDNA& test){
    if(isDataLoaded){
        uint32_t i = 0;
        int32_t unassigned      = 0;
        int32_t prohibited      = 0;
        int32_t mappedWithNorm  = 0;
        int32_t mapped          = 0;
        int32_t noValueInTrie   = 0;


        for(i=0;i<=0x10FFFF;i++){
            uint32_t result = 0;
            UTRIE_GET16(&idnTrie,i, result);

            if(result != UIDNA_NO_VALUE ){
                if((result & 0x07) == UIDNA_UNASSIGNED){
                    unassigned++;
                }
                if((result & 0x07) == UIDNA_PROHIBITED){
                    prohibited++;
                }
                if((result>>5) == _IDNA_MAP_TO_NOTHING){
                    mapped++;
                }
                if((result & 0x07) == UIDNA_MAP_NFKC){
                    mappedWithNorm++;
                }
            }else{
                noValueInTrie++;
                if(result > 0){
                    test.errln("The return value for 0x%06X is wrong. %i\n",i,result);
                }
            }
        }

        test.logln("Number of Unassinged code points : %i \n",unassigned);
        test.logln("Number of Prohibited code points : %i \n",prohibited);
        test.logln("Number of Mapped code points : %i \n",mapped);
        test.logln("Number of Mapped with NFKC code points : %i \n",mappedWithNorm);
        test.logln("Number of code points that have no value in Trie: %i \n",noValueInTrie);

    }
}

static inline void getValues(uint32_t result, int8_t& flag, 
                             int8_t& length, int32_t& index){
    /* first 3 bits contain the flag */
    flag = (int8_t) (result & 0x07);
    /* next 2 bits contain the length */
    length = (int8_t) ((result>>3) & 0x03);
    /* next 11 bits contain the index */
    index  = (result>> 5);
}

static void 
compareMapping(uint32_t codepoint, uint32_t* mapping,int32_t mapLength, 
               UBool withNorm){
    if(isDataLoaded){
        uint32_t result = 0;
        UTRIE_GET16(&idnTrie,codepoint, result);

        int8_t flag, length;
        int32_t index;
        getValues(result,flag,length, index);


        if(withNorm){
            if(flag != UIDNA_MAP_NFKC){
                pTestIDNA->errln( "Did not get the assigned flag for codepoint 0x%08X. Expected: %i Got: %i\n",codepoint, UIDNA_MAP_NFKC, flag);
            }
        }else{
            if(flag==UIDNA_NO_VALUE || flag == UIDNA_PROHIBITED){
                if(index != _IDNA_MAP_TO_NOTHING ){
                    pTestIDNA->errln( "Did not get the assigned flag for codepoint 0x%08X. Expected: %i Got: %i\n", codepoint, _IDNA_MAP_TO_NOTHING, index);
                }
            }
        }
        if(length ==_IDNA_LENGTH_IN_MAPPING_TABLE){
            length = (int8_t)mappingData[index];
            index++;
        }
        int32_t realLength =0;
        /* figure out the real length */ 
        for(int32_t j=0; j<mapLength; j++){
            if(mapping[j] > 0xFFFF){
                realLength +=2;
            }else{
                realLength++;
            }      
        }

        if(realLength != length){
            pTestIDNA->errln( "Did not get the expected length. Expected: %i Got: %i\n", mapLength, length);
        }
        

        for(int8_t i =0; i< mapLength; i++){
            if(mapping[i] <= 0xFFFF){
                if(mappingData[index+i] != (uint16_t)mapping[i]){
                    pTestIDNA->errln("Did not get the expected result. Expected: 0x%04X Got: 0x%04X \n", mapping[i], mappingData[index+i]);
                }
            }else{
                UChar lead  = UTF16_LEAD(mapping[i]);
                UChar trail = UTF16_TRAIL(mapping[i]);
                if(mappingData[index+i] != lead ||
                    mappingData[index+i+1] != trail){
                    pTestIDNA->errln( "Did not get the expected result. Expected: 0x%04X 0x%04X  Got: 0x%04X 0x%04X", lead, trail, mappingData[index+i], mappingData[index+i+1]);
                }
            }
        }

    }

}

static void
compareFlagsForRange(uint32_t start, uint32_t end,
                     UBool isUnassigned){
    if(isDataLoaded){
        uint32_t result =0 ;
        while(start < end+1){
            UTRIE_GET16(&idnTrie,start, result);
            if(isUnassigned){
                if(result != UIDNA_UNASSIGNED){
                    pTestIDNA->errln( "UIDNA_UASSIGNED flag failed for 0x%06X. Expected: %04X Got: %04X\n",start,UIDNA_UNASSIGNED, result);
                }
            }else{
                if((result & 0x03) != UIDNA_PROHIBITED){
                    pTestIDNA->errln( "UIDNA_PROHIBITED flag failed for 0x%06X. Expected: %04X Got: %04X\n\n",start,UIDNA_PROHIBITED, result);
                }
            }
            start++;
        }
    }
}

UBool
cleanup() {
    if(idnData!=NULL) {
        udata_close(idnData);
        idnData=NULL;
    }
    dataErrorCode=U_ZERO_ERROR;
    isDataLoaded=FALSE;

    return TRUE;
}
U_CDECL_BEGIN
static UBool U_CALLCONV
isAcceptable(void * /* context */,
             const char * /* type */, const char * /* name */,
             const UDataInfo *pInfo) {
    if(
        pInfo->size>=20 &&
        pInfo->isBigEndian==U_IS_BIG_ENDIAN &&
        pInfo->charsetFamily==U_CHARSET_FAMILY &&
        pInfo->dataFormat[0]==0x49 &&   /* dataFormat="IDNA" 0x49, 0x44, 0x4e, 0x41  */
        pInfo->dataFormat[1]==0x44 &&
        pInfo->dataFormat[2]==0x4e &&
        pInfo->dataFormat[3]==0x41 &&
        pInfo->formatVersion[0]==2 &&
        pInfo->formatVersion[2]==UTRIE_SHIFT &&
        pInfo->formatVersion[3]==UTRIE_INDEX_SHIFT
    ) {
        return TRUE;
    } else {
        return FALSE;
    }
}

/* idnTrie: the folding offset is the lead FCD value itself */
static int32_t U_CALLCONV
getFoldingOffset(uint32_t data) {
    if(data&0x8000) {
        return (int32_t)(data&0x7fff);
    } else {
        return 0;
    }
}
U_CDECL_END

static UBool
loadIDNData(UErrorCode &errorCode) {
    /* load Unicode normalization data from file */
    if(isDataLoaded==FALSE) {
        UTrie _idnTrie={ 0,0,0,0,0,0,0 };
        UDataMemory *data;
        const int32_t *p=NULL;
        const uint8_t *pb;
        if(&errorCode==NULL || U_FAILURE(errorCode)) {
            return 0;
        }

        /* open the data outside the mutex block */
        data=udata_openChoice(NULL, DATA_TYPE, DATA_NAME, isAcceptable, NULL, &errorCode);
        dataErrorCode=errorCode;
        if(U_FAILURE(errorCode)) {
            return isDataLoaded=FALSE;
        }

        p=(const int32_t *)udata_getMemory(data);
        pb=(const uint8_t *)(p+_IDNA_INDEX_TOP);
        utrie_unserialize(&_idnTrie, pb, p[_IDNA_INDEX_TRIE_SIZE], &errorCode);
        _idnTrie.getFoldingOffset=getFoldingOffset;


        if(U_FAILURE(errorCode)) {
            dataErrorCode=errorCode;
            udata_close(data);
            return isDataLoaded=FALSE;
        }

        /* in the mutex block, set the data for this process */
        umtx_lock(NULL);
        if(idnData==NULL) {
            idnData=data;
            data=NULL;
            uprv_memcpy(&indexes, p, sizeof(indexes));
            uprv_memcpy(&idnTrie, &_idnTrie, sizeof(UTrie));
        } else {
            p=(const int32_t *)udata_getMemory(idnData);
        }
        umtx_unlock(NULL);
        /* initialize some variables */
        mappingData=(uint16_t *)((uint8_t *)(p+_IDNA_INDEX_TOP)+indexes[_IDNA_INDEX_TRIE_SIZE]);

        isDataLoaded = TRUE;

        /* if a different thread set it first, then close the extra data */
        if(data!=NULL) {
            udata_close(data); /* NULL if it was set correctly */
        }
    }

    return isDataLoaded;
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
