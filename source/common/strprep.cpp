/*
 *******************************************************************************
 *
 *   Copyright (C) 2003, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *
 *******************************************************************************
 *   file name:  strprep.cpp
 *   encoding:   US-ASCII
 *   tab size:   8 (not used)
 *   indentation:4
 *
 *   created on: 2003feb1
 *   created by: Ram Viswanadha
 */

#include "unicode/utypes.h"

#if !UCONFIG_NO_IDNA

#include "strprep.h"
#include "utrie.h"
#include "umutex.h"
#include "cmemory.h"
#include "sprpimpl.h"
#include "nameprep.h"
#include "ustr_imp.h"
#include "unicode/unorm.h"
#include "unicode/udata.h"
#include "unicode/ustring.h"

static const uint16_t* mappingData = NULL;
static int32_t indexes[_IDNA_INDEX_TOP]={ 0 };
static UBool _isDataLoaded = FALSE;
static UTrie idnTrie={ 0,0,0,0,0,0,0 };
static UDataMemory* idnData=NULL;
static UErrorCode dataErrorCode =U_ZERO_ERROR;
/* file definitions */
static const char DATA_NAME[] = "uidna";
static const char DATA_TYPE[] = "icu";

U_CFUNC UBool
ustrprep_cleanup() {
    if(idnData!=NULL) {
        udata_close(idnData);
        idnData=NULL;
    }
    dataErrorCode=U_ZERO_ERROR;
    _isDataLoaded=FALSE;

    return TRUE;
}

U_CDECL_BEGIN
static UBool U_CALLCONV
isAcceptable(void * /* context */,
             const char * /* type */, 
             const char * /* name */,
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



static int32_t U_CALLCONV
getFoldingOffset(uint32_t data) {
    if(data&0x8000) {
        return (int32_t)(data&0x7fff);
    } else {
        return 0;
    }
}

U_CDECL_END

static UBool U_CALLCONV
loadData(UErrorCode &errorCode) {
    /* load Unicode IDNA data from file */
    UBool isCached;

    /* do this because double-checked locking is broken */
    umtx_lock(NULL);
    isCached=_isDataLoaded;
    umtx_unlock(NULL);

    if(!isCached) {
        UTrie _idnTrie={ 0,0,0,0,0,0,0 };
        UDataMemory *data;
        const int32_t *p=NULL;
        const uint8_t *pb;

        if(&errorCode==NULL || U_FAILURE(errorCode)) {
            return 0;
        }

        /* open the data outside the mutex block */
        //TODO: change the path
        data=udata_openChoice(NULL, DATA_TYPE, DATA_NAME, isAcceptable, NULL, &errorCode);
        dataErrorCode=errorCode;
        if(U_FAILURE(errorCode)) {
            return _isDataLoaded=FALSE;
        }

        p=(const int32_t *)udata_getMemory(data);
        pb=(const uint8_t *)(p+_IDNA_INDEX_TOP);
        utrie_unserialize(&_idnTrie, pb, p[_IDNA_INDEX_TRIE_SIZE], &errorCode);
        _idnTrie.getFoldingOffset=getFoldingOffset;


        if(U_FAILURE(errorCode)) {
            dataErrorCode=errorCode;
            udata_close(data);
            return _isDataLoaded=FALSE;
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

        _isDataLoaded = TRUE;

        /* if a different thread set it first, then close the extra data */
        if(data!=NULL) {
            udata_close(data); /* NULL if it was set correctly */
        }
    }

    return _isDataLoaded;
}

// *****************************************************************************
// class StringPrep
// *****************************************************************************

U_NAMESPACE_BEGIN

const char StringPrep::fgClassID=0;

UBool StringPrep::isDataLoaded(UErrorCode& status){
    if(U_FAILURE(status)){
        return FALSE;
    }
    if(_isDataLoaded==FALSE && U_FAILURE(dataErrorCode)){
        status = dataErrorCode;
        return FALSE;
    }
    loadData(dataErrorCode);
    if(U_FAILURE(dataErrorCode)){
        status = dataErrorCode;
        return FALSE;
    }
    return TRUE;
}


StringPrep* StringPrep::createDefaultInstance(UErrorCode& status){
    StringPrep* strprep = new StringPrep();
    if(!isDataLoaded(status)){
        delete strprep;
        return NULL;
    }
    return strprep;
}

StringPrep* StringPrep::createNameprepInstance(UErrorCode& status){
    StringPrep* strprep = new NamePrep(status);
    if(!isDataLoaded(status)){
        delete strprep;
        return NULL;
    }
    return strprep;
}

UBool StringPrep::isNotProhibited(UChar32 /*ch*/){
    return FALSE;
}
UBool StringPrep::isUnassigned(UChar32 ch){

    uint32_t result;
    UTRIE_GET16(&idnTrie,ch,result);
    return (result == UIDNA_UNASSIGNED);

}


static inline void getValues(uint32_t result, int8_t& flag, 
                             int8_t& length, int32_t& index){
    /* first 3 bits contain the flag */
    flag = (int8_t) (result & 0x07);
    /* next 2 bits contain the length */
    length = (int8_t) ((result>>3) & 0x03);
    /* next 10 bits contain the index */
    index  = (result>> 5);
}


int32_t StringPrep::map(const UChar* src, int32_t srcLength, 
                        UChar* dest, int32_t destCapacity, 
                        UBool allowUnassigned,
                        UParseError* parseError,
                        UErrorCode& status ){
    
    uint32_t result;
    int8_t flag;
    int8_t length;
    int32_t index;
    int32_t destIndex=0;
    int32_t srcIndex=0;

    // check error status
    if(U_FAILURE(status)){
        return 0;
    }
    
    //check arguments
    if(src==NULL || srcLength<-1 || (dest==NULL && destCapacity!=0)) {
        status=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }
    if(srcLength == -1){
        srcLength = u_strlen(src);
    }

    for(;srcIndex<srcLength;){
        UChar32 ch;

        U16_NEXT(src,srcIndex,srcLength,ch);
        
        UTRIE_GET16(&idnTrie,ch,result);
        
        getValues(result,flag,length,index);

        // check if the source codepoint is unassigned
        if(flag == UIDNA_UNASSIGNED){
            if(allowUnassigned == TRUE){
                //copy the ch to destination
                if(ch <= 0xFFFF){
                    if(destIndex < destCapacity ){
                        dest[destIndex] = (UChar)ch;
                    }
                    destIndex++;
                }else{
                    if(destIndex+1 < destCapacity ){
                        dest[destIndex]   = U16_LEAD(ch);
                        dest[destIndex+1] = U16_TRAIL(ch);
                    }
                    destIndex +=2;
                }
            }else{
                uprv_syntaxError(src,srcIndex-U16_LENGTH(ch), srcLength,parseError);
                status = U_IDNA_UNASSIGNED_CODEPOINT_FOUND_ERROR;
                return 0;
            }
        }else if((flag == UIDNA_MAP_NFKC && doNFKC == TRUE) ||
            (index == _IDNA_MAP_TO_NOTHING && doNFKC == FALSE)){
            
            if(length == _IDNA_LENGTH_IN_MAPPING_TABLE){
                length = (int8_t) mappingData[index++];
            }
            
            for(int8_t i =0; i< length; i++){
                if(destIndex < destCapacity  ){
                    dest[destIndex] = mappingData[index+i];
                }
                destIndex++; /* for pre-flighting */
            }
        }else{
            //copy the source into destination
            if(ch <= 0xFFFF){
                if(destIndex < destCapacity ){
                    dest[destIndex] = (UChar)ch;
                }
                destIndex++;
            }else{
                if(destIndex+1 < destCapacity ){
                    dest[destIndex]   = U16_LEAD(ch);
                    dest[destIndex+1] = U16_TRAIL(ch);
                }
                destIndex +=2;
            }
        }
    }
        
    return u_terminateUChars(dest, destCapacity, destIndex, &status);
}


int32_t StringPrep::normalize(  const UChar* src, int32_t srcLength, 
                                    UChar* dest, int32_t destCapacity, 
                                    UErrorCode& status ){

    return unorm_normalize(src,srcLength,UNORM_NFKC,UNORM_UNICODE_3_2,dest,destCapacity,&status);
}


 /*
   1) Map -- For each character in the input, check if it has a mapping
      and, if so, replace it with its mapping.  

   2) Normalize -- Possibly normalize the result of step 1 using Unicode
      normalization. 

   3) Prohibit -- Check for any characters that are not allowed in the
      output.  If any are found, return an error.  

   4) Check bidi -- Possibly check for right-to-left characters, and if
      any are found, make sure that the whole string satisfies the
      requirements for bidirectional strings.  If the string does not
      satisfy the requirements for bidirectional strings, return an
      error.  
      [Unicode3.2] defines several bidirectional categories; each character
       has one bidirectional category assigned to it.  For the purposes of
       the requirements below, an "RandALCat character" is a character that
       has Unicode bidirectional categories "R" or "AL"; an "LCat character"
       is a character that has Unicode bidirectional category "L".  Note


       that there are many characters which fall in neither of the above
       definitions; Latin digits (<U+0030> through <U+0039>) are examples of
       this because they have bidirectional category "EN".

       In any profile that specifies bidirectional character handling, all
       three of the following requirements MUST be met:

       1) The characters in section 5.8 MUST be prohibited.

       2) If a string contains any RandALCat character, the string MUST NOT
          contain any LCat character.

       3) If a string contains any RandALCat character, a RandALCat
          character MUST be the first character of the string, and a
          RandALCat character MUST be the last character of the string.
*/

#define MAX_STACK_BUFFER_SIZE 300

int32_t StringPrep::process(const UChar* src, int32_t srcLength, 
                            UChar* dest, int32_t destCapacity,
                            UBool allowUnassigned,
                            UParseError* parseError,
                            UErrorCode& status ){
    // check error status
    if(U_FAILURE(status)){
        return 0;
    }
    
    //check arguments
    if(src==NULL || srcLength<-1 || (dest==NULL && destCapacity!=0)) {
        status=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    UChar b1Stack[MAX_STACK_BUFFER_SIZE], b2Stack[MAX_STACK_BUFFER_SIZE];
    UChar *b1 = b1Stack, *b2 = b2Stack;
    int32_t b1Len, b2Len=0,
            b1Capacity = MAX_STACK_BUFFER_SIZE , 
            b2Capacity = MAX_STACK_BUFFER_SIZE;
    uint32_t result;
    int32_t b2Index = 0;
    int8_t flag;
    int8_t length;
    int32_t index;
    UCharDirection direction=U_CHAR_DIRECTION_COUNT, firstCharDir=U_CHAR_DIRECTION_COUNT;
    UBool leftToRight=FALSE, rightToLeft=FALSE;
    int32_t rtlPos =-1, ltrPos =-1;

    b1Len = map(src,srcLength, b1, b1Capacity,allowUnassigned, parseError, status);

    if(status == U_BUFFER_OVERFLOW_ERROR){
        // redo processing of string
        /* we do not have enough room so grow the buffer*/
        b1 = (UChar*) uprv_malloc(b1Len * U_SIZEOF_UCHAR);
        if(b1==NULL){
            status = U_MEMORY_ALLOCATION_ERROR;
            goto CLEANUP;
        }

        status = U_ZERO_ERROR; // reset error
        
        b1Len = map(src,srcLength, b1, b1Len,allowUnassigned, parseError, status);
        
    }
        
    b2Len = normalize(b1,b1Len, b2,b2Capacity,status);
    
    if(status == U_BUFFER_OVERFLOW_ERROR){
        // redo processing of string
        /* we do not have enough room so grow the buffer*/
        b2 = (UChar*) uprv_malloc(b2Len * U_SIZEOF_UCHAR);
        if(b2==NULL){
            status = U_MEMORY_ALLOCATION_ERROR;
            goto CLEANUP;
        }

        status = U_ZERO_ERROR; // reset error
        
        b2Len = normalize(b2,b2Len, b2,b2Len,status);
        
    }

    if(U_FAILURE(status)){
        goto CLEANUP;
    }

    UChar32 ch;

    for(; b2Index<b2Len;){
        
        ch = 0;

        U16_NEXT(b2, b2Index, b2Len, ch);

        UTRIE_GET16(&idnTrie,ch,result);
        
        getValues(result,flag,length,index);

        if(flag == UIDNA_PROHIBITED 
            && isNotProhibited(ch) == FALSE){
            status = U_IDNA_PROHIBITED_CODEPOINT_FOUND_ERROR;
            uprv_syntaxError(b1, b2Index-U16_LENGTH(ch), b2Len, parseError);
            goto CLEANUP;
        }

        direction = u_charDirection(ch);
        if(firstCharDir == U_CHAR_DIRECTION_COUNT){
            firstCharDir = direction;
        }
        if(direction == U_LEFT_TO_RIGHT){
            leftToRight = TRUE;
            ltrPos = b2Index-1;
        }
        if(direction == U_RIGHT_TO_LEFT || direction == U_RIGHT_TO_LEFT_ARABIC){
            rightToLeft = TRUE;
            rtlPos = b2Index-1;
        }
    }           
    
    // satisfy 2
    if( leftToRight == TRUE && rightToLeft == TRUE){
        status = U_IDNA_CHECK_BIDI_ERROR;
        uprv_syntaxError(b2,(rtlPos>ltrPos) ? rtlPos : ltrPos, b2Len, parseError);
        goto CLEANUP;
    }

    //satisfy 3
    if( rightToLeft == TRUE && 
        !((firstCharDir == U_RIGHT_TO_LEFT || firstCharDir == U_RIGHT_TO_LEFT_ARABIC) &&
          (direction == U_RIGHT_TO_LEFT || direction == U_RIGHT_TO_LEFT_ARABIC))
       ){
        status = U_IDNA_CHECK_BIDI_ERROR;
        uprv_syntaxError(b2, rtlPos, b2Len, parseError);
        return FALSE;
    }

    if(b2Len <= destCapacity){
        uprv_memmove(dest,b2, b2Len*U_SIZEOF_UCHAR);
    }

CLEANUP:
    if(b1!=b1Stack){
        uprv_free(b1);
    }
    if(b2!=b2Stack){
        uprv_free(b2);
    }
    return u_terminateUChars(dest, destCapacity, b2Len, &status);
}


UBool StringPrep::isLabelSeparator(UChar32 ch, UErrorCode& status){
    // check error status
    if(U_FAILURE(status)){
        return FALSE;
    }

    if(isDataLoaded(status)){
        int32_t result;
        UTRIE_GET16(&idnTrie,ch, result);
        if( (result & 0x07)  == UIDNA_LABEL_SEPARATOR){
            return TRUE;
        }
    }
    return FALSE;
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_IDNA */
