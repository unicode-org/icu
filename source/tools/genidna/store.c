/*
*******************************************************************************
*
*   Copyright (C) 1999-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  store.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2003-02-06
*   created by: Ram Viswanadha
*
*/

#include <stdio.h>
#include <stdlib.h>
#include "unicode/utypes.h"
#include "unicode/uchar.h"
#include "cmemory.h"
#include "cstring.h"
#include "filestrm.h"
#include "unicode/udata.h"
#include "utrie.h"
#include "unicode/uset.h"
#include "unewdata.h"
#include "genidna.h"

#ifdef WIN32
#   pragma warning(disable: 4100)
#endif

#define DO_DEBUG_OUT 0


/**
This is a simple Trie with the following structure

16-bit IDN sets:

Each 16-bit IDN word contains:

 0..2  Category flags
       Contains the enum values IDNStates
 
 3..4  Contains the length of the mapping
       If length of the mapping is < 2 the length is stored
       If length of the mapping is > 2 then _IDNA_LENGTH_IN_MAPPING_TABLE
       enum is stored and the length of mapping is stored in the first index
       in the data array

 5..16 Contains the index into the data array that contains the mapping 
       If it contains _IDNA_MAP_TO_NOTHING, then the codepoint is stripped from
       the input

*/

/* file data ---------------------------------------------------------------- */
/* indexes[] value names */

#if UCONFIG_NO_IDNA

/* dummy UDataInfo cf. udata.h */
static UDataInfo dataInfo = {
    sizeof(UDataInfo),
    0,

    U_IS_BIG_ENDIAN,
    U_CHARSET_FAMILY,
    U_SIZEOF_UCHAR,
    0,

    { 0, 0, 0, 0 },                 /* dummy dataFormat */
    { 0, 0, 0, 0 },                 /* dummy formatVersion */
    { 0, 0, 0, 0 }                  /* dummy dataVersion */
};

#else

static int32_t indexes[_IDNA_INDEX_TOP]={ 0 };

static uint16_t mappingData[_IDNA_MAPPING_DATA_SIZE]={0};

/* UDataInfo cf. udata.h */
static UDataInfo dataInfo={
    sizeof(UDataInfo),
    0,

    U_IS_BIG_ENDIAN,
    U_CHARSET_FAMILY,
    U_SIZEOF_UCHAR,
    0,

    { 0x49, 0x44, 0x4e, 0x41 },   /* dataFormat="IDNA" */
    { 2, 2, UTRIE_SHIFT, UTRIE_INDEX_SHIFT },   /* formatVersion */
    { 3, 2, 0, 0 }                /* dataVersion (Unicode version) */
};
void
setUnicodeVersion(const char *v) {
    UVersionInfo version;
    u_versionFromString(version, v);
    uprv_memcpy(dataInfo.dataVersion, version, 4);
}


static UNewTrie idnTrie={ {0},0,0,0,0,0,0,0,0,{0} };

static int32_t currentIndex = 1; /* the current index into the data trie */
static int32_t maxLength = 0;  /* maximum length of mapping string */

#define MAX_DATA_LENGTH 11500

extern void
init() {

    /* initialize the two tries */
    if(NULL==utrie_open(&idnTrie, NULL, MAX_DATA_LENGTH, 0, 0, FALSE)) {
        fprintf(stderr, "error: failed to initialize tries\n");
        exit(U_MEMORY_ALLOCATION_ERROR);
    }
}

static void 
store(uint32_t codepoint, uint32_t* mapping, int32_t length, uint32_t flags, UErrorCode* status){

    uint32_t trieWord = 0;
    int32_t i =0;
    if(flags == _IDNA_MAP_TO_NOTHING){
        trieWord = flags << 5;
    }else{
        if(length==0){
            trieWord =  flags;
        }else{
            int32_t adjustedLen = 0;
            int32_t i=0;
            /*
            int32_t delta;
        
            if(length==1 && (delta=(int32_t)codepoint-(int32_t)mapping[0])>=-4096 && delta<=4095) {
                printf("mapping of U+%04lx to U+%04lx could fit into a 13-bit delta (0x%lx)\n", codepoint, mapping[0], delta);
            }
             */
            /* set the 0..2 bits the flags */
            trieWord = flags;

            /* figure out the real length */ 
            for(i=0; i<length; i++){
                if(mapping[i] > 0xFFFF){
                    adjustedLen +=2;
                }else{
                    adjustedLen++;
                }      
            }
            length = adjustedLen;

            /* set the 3..4 bits the length */
            if(length > 2){
                trieWord += _IDNA_LENGTH_IN_MAPPING_TABLE << 3;
            }else{
                trieWord += (uint32_t)((length)<<3);
            }
            if(length > maxLength) 
                maxLength = length;

            /* get the current index in the data array 
             * and store in 5..15 bits
             */
            trieWord += currentIndex << 5;


            /* load mapping into the data array */
            i = 0;
        
            if(trieWord > 0xFFFF){
                fprintf(stderr,"size of trie word is greater than 0xFFFF.\n");
            }
            /* set the length in mapping table */
            if(length > 2){
                mappingData[currentIndex++] = (uint16_t)length;
            }
            while(i<length){
                if(currentIndex < _IDNA_MAPPING_DATA_SIZE){
                    if(mappingData[currentIndex]==0){
                        if(mapping[i] <= 0xFFFF){
                            mappingData[currentIndex++] = (uint16_t)mapping[i++];
                        }else{
                            mappingData[currentIndex++] = UTF16_LEAD(mapping[i]);
                            if(currentIndex < _IDNA_MAPPING_DATA_SIZE){
                                mappingData[currentIndex++] = UTF16_TRAIL(mapping[i++]);
                            }else{
                                fprintf(stderr, "Data Array index out of bounds.currentIndex = %i size of mapping arry = %i \n",currentIndex, _IDNA_MAPPING_DATA_SIZE);
                                *status = U_INDEX_OUTOFBOUNDS_ERROR;
                                return;
                            }
                        }
                    }
                }else{
                    fprintf(stderr, "Data Array index out of bounds.currentIndex = %i size of mapping arry = %i \n",currentIndex, _IDNA_MAPPING_DATA_SIZE);
                    *status = U_INDEX_OUTOFBOUNDS_ERROR;
                    return;
                }
            }

        }
    }


    i = utrie_get32(&idnTrie,codepoint,NULL);
    
    if(i==0){
        /* now set the value in the trie */
        if(!utrie_set32(&idnTrie,codepoint,trieWord)){
            fprintf(stderr, "error:  too many mapping entries\n");
            exit(U_BUFFER_OVERFLOW_ERROR);
        }

    }else{
        if(i== UIDNA_PROHIBITED){
            i += _IDNA_MAP_TO_NOTHING << 5;
            /* now set the value in the trie */
            if(!utrie_set32(&idnTrie,codepoint,i)){
                fprintf(stderr, "error:  too many mapping entries\n");
                exit(U_BUFFER_OVERFLOW_ERROR);
            }
        }else{
            fprintf(stderr, "Index array has been set for codepoint 0x%06X. \n",codepoint);
            exit(U_INTERNAL_PROGRAM_ERROR);
        }
    }

}
extern void
storeMapping(uint32_t codepoint, uint32_t* mapping,int32_t length, UBool withNorm, UErrorCode* status){
    
    if(withNorm){
        store(codepoint,mapping,length,UIDNA_MAP_NFKC,status);
    }else{
        store(codepoint,mapping,length,_IDNA_MAP_TO_NOTHING,status);
    }
}


extern void
storeRange(uint32_t start, uint32_t end, int8_t flag,UErrorCode* status){
    uint32_t trieWord = 0, i=0;

    trieWord += flag;

    if(start == end){
        i = utrie_get32(&idnTrie,start,NULL);
        if(i == 0 || i==(uint8_t)flag){
            if(!utrie_set32(&idnTrie,start,trieWord)){
                fprintf(stderr, "error: too  many entries\n");
                exit(U_BUFFER_OVERFLOW_ERROR);
            }
        }else{
            fprintf(stderr, "Index array has been set for codepoint 0x%06X. \n",start);
            exit(U_INTERNAL_PROGRAM_ERROR);
        }
    }else{
        if(!utrie_setRange32(&idnTrie,start,end+1,trieWord,FALSE)){
            fprintf(stderr, "error: too  many entries\n");
            exit(U_BUFFER_OVERFLOW_ERROR);
        }
    }

}

/* folding value: just store the offset (16 bits) if there is any non-0 entry */
static uint32_t U_CALLCONV
getFoldedValue(UNewTrie *trie, UChar32 start, int32_t offset) {
    uint32_t foldedValue, value;
    UChar32 limit;
    UBool inBlockZero;

    foldedValue=0;

    limit=start+0x400;
    while(start<limit) {
        value=utrie_get32(trie, start, &inBlockZero);
        if(inBlockZero) {
            start+=UTRIE_DATA_BLOCK_LENGTH;
        } else {
            foldedValue|=value;
            ++start;
        }
    }

    if(foldedValue!=0) {
        return (uint32_t)(offset|0x8000);
    } else {
        return 0;
    }
}

#endif /* #if !UCONFIG_NO_IDNA */

extern void
generateData(const char *dataDir) {
    static uint8_t idnTrieBlock[100000];

    UNewDataMemory *pData;
    UErrorCode errorCode=U_ZERO_ERROR;
    int32_t size, dataLength;

#if UCONFIG_NO_IDNA

    size=0;

#else

    int32_t idnTrieSize;

    idnTrieSize=utrie_serialize(&idnTrie, idnTrieBlock, sizeof(idnTrieBlock), getFoldedValue, TRUE, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "error: utrie_serialize(idn trie) failed, %s\n", u_errorName(errorCode));
        exit(errorCode);
    }
    size = idnTrieSize + sizeof(mappingData) + sizeof(indexes);
    if(beVerbose) {
        printf("size of idn trie              %5u bytes\n", idnTrieSize);
        printf("size of " U_ICUDATA_NAME "_" DATA_NAME "." DATA_TYPE " contents: %ld bytes\n", (long)size);
        printf("size of mapping data array %5u bytes\n", sizeof(mappingData));
        printf("Number of code units in mappingData (currentIndex) are: %i \n", currentIndex);
        printf("Maximum length of the mapping string is : %i \n", maxLength);
    }

#endif
    
    /* write the data */
    pData=udata_create(dataDir, DATA_TYPE, U_ICUDATA_NAME "_" DATA_NAME, &dataInfo,
                       haveCopyright ? U_COPYRIGHT_STRING : NULL, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "gennorm: unable to create the output file, error %d\n", errorCode);
        exit(errorCode);
    }

#if !UCONFIG_NO_IDNA

    indexes[_IDNA_INDEX_TRIE_SIZE]=idnTrieSize;
    indexes[_IDNA_INDEX_MAPPING_DATA_SIZE]=sizeof(mappingData);

    udata_writeBlock(pData, indexes, sizeof(indexes));
    udata_writeBlock(pData, idnTrieBlock, idnTrieSize);
    udata_writeBlock(pData, mappingData, sizeof(mappingData));

#endif

    /* finish up */
    dataLength=udata_finish(pData, &errorCode);
    if(U_FAILURE(errorCode)) {
        fprintf(stderr, "genidn: error %d writing the output file\n", errorCode);
        exit(errorCode);
    }

    if(dataLength!=size) {
        fprintf(stderr, "genidn error: data length %ld != calculated size %ld\n",
            (long)dataLength, (long)size);
        exit(U_INTERNAL_PROGRAM_ERROR);
    }
}

#if !UCONFIG_NO_IDNA

extern void
cleanUpData(void) {

    utrie_close(&idnTrie);

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
