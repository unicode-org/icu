/*  
**********************************************************************
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   file name:  ucnvhz.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000oct16
*   created by: Ram Viswanadha
*   10/31/2000  Ram     Implemented offsets logic function
*   
*/

#include "unicode/utypes.h"
#include "cmemory.h"
#include "ucmp16.h"
#include "ucmp8.h"
#include "unicode/ucnv_err.h"
#include "ucnv_bld.h"
#include "unicode/ucnv.h"
#include "ucnv_cnv.h"
#include "unicode/ustring.h"
#include "cstring.h"

#define UCNV_TILDE 0x7E          /* ~ */
#define UCNV_OPEN_BRACE 0x7B     /* { */
#define UCNV_CLOSE_BRACE 0x7D   /* } */
#define SB_ESCAPE   "\x7E\x7D"
#define DB_ESCAPE   "\x7E\x7B"

#define TEST_ERROR_CONDITION(args,myTargetIndex, mySourceIndex, isTargetUCharDBCS, myConverterData, err){ \
    if(*err ==U_BUFFER_OVERFLOW_ERROR){ \
    /*save the state and return */ \
    args->target += myTargetIndex; \
    args->source += mySourceIndex; \
    myConverterData->sourceIndex = 0; \
    myConverterData->targetIndex = 0; \
    args->converter->fromUnicodeStatus = isTargetUCharDBCS; \
    return; \
    } \
}

/*********** HZ Converter Protos ***********/
static void _HZOpen(UConverter *cnv, const char *name, const char *locale, uint32_t *options,UErrorCode *errorCode);
static void _HZClose(UConverter *converter);
static void _HZReset(UConverter *converter);

U_CFUNC void UConverter_toUnicode_HZ(UConverterToUnicodeArgs *args,
                                             UErrorCode *err);

U_CFUNC void UConverter_toUnicode_HZ_OFFSETS_LOGIC (UConverterToUnicodeArgs *args,
                                                            UErrorCode *err);

U_CFUNC void UConverter_fromUnicode_HZ(UConverterFromUnicodeArgs *args,
                                               UErrorCode *err);

U_CFUNC void UConverter_fromUnicode_HZ_OFFSETS_LOGIC (UConverterFromUnicodeArgs *args,
                                                              UErrorCode *err);

U_CFUNC UChar32 UConverter_getNextUChar_HZ (UConverterToUnicodeArgs *pArgs,
                                                    UErrorCode *pErrorCode);   

static UConverterImpl _HZImpl={
    UCNV_HZ,
    
    NULL,
    NULL,
    
    _HZOpen,
    _HZClose,
    _HZReset,
    
    UConverter_toUnicode_HZ,
    UConverter_toUnicode_HZ_OFFSETS_LOGIC,
    UConverter_fromUnicode_HZ,
    UConverter_fromUnicode_HZ_OFFSETS_LOGIC,
    UConverter_getNextUChar_HZ,
    
    NULL,
    NULL
};

const UConverterStaticData _HZStaticData={
    sizeof(UConverterStaticData),
        "HZ",
        2023, UCNV_IBM, UCNV_HZ, 1, 4,
    { 0x1a, 0, 0, 0 },1, FALSE, FALSE,
    { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0} /* reserved */
};
            
            
const UConverterSharedData _HZData={
    sizeof(UConverterSharedData), ~((uint32_t) 0),
        NULL, NULL, &_HZStaticData, FALSE, &_HZImpl, 
        0
};

typedef struct{
    int32_t targetIndex;
    int32_t sourceIndex;
    UBool isEscapeAppended;
    UConverter* gbConverter;
    UBool isStateDBCS;
    UBool isTargetUCharDBCS;
}UConverterDataHZ;



static void _HZOpen(UConverter *cnv, const char *name,const char *locale,uint32_t *options, UErrorCode *errorCode){
    cnv->toUnicodeStatus = 0;
    cnv->fromUnicodeStatus= 0;
    cnv->mode=0;
    cnv->fromUSurrogateLead;
    cnv->extraInfo = uprv_malloc (sizeof (UConverterDataHZ));
    if(cnv->extraInfo != NULL){
        ((UConverterDataHZ*)cnv->extraInfo)->gbConverter = ucnv_open("ibm-1386",errorCode);
        ((UConverterDataHZ*)cnv->extraInfo)->isStateDBCS = FALSE;
        ((UConverterDataHZ*)cnv->extraInfo)->isEscapeAppended = FALSE;
        ((UConverterDataHZ*)cnv->extraInfo)->targetIndex = 0;
        ((UConverterDataHZ*)cnv->extraInfo)->sourceIndex = 0;
        ((UConverterDataHZ*)cnv->extraInfo)->isTargetUCharDBCS = FALSE;
         ucnv_setSubstChars(cnv,"\x7E\x7D\x1A", 3, errorCode);
    }


}
static void _HZClose(UConverter *cnv){
    
     ucnv_close (((UConverterDataHZ *) (cnv->extraInfo))->gbConverter);
     uprv_free(cnv->extraInfo);

}
static void _HZReset(UConverter *cnv){
    cnv->toUnicodeStatus = 0;
    cnv->fromUnicodeStatus= 0;
    cnv->mode=0;
    cnv->fromUSurrogateLead; 
    if(cnv->extraInfo != NULL){
        ((UConverterDataHZ*)cnv->extraInfo)->isStateDBCS = FALSE;
        ((UConverterDataHZ*)cnv->extraInfo)->isEscapeAppended = FALSE;
        ((UConverterDataHZ*)cnv->extraInfo)->targetIndex = 0;
        ((UConverterDataHZ*)cnv->extraInfo)->sourceIndex = 0;
        ((UConverterDataHZ*)cnv->extraInfo)->isTargetUCharDBCS = FALSE;
    }
}

/**************************************HZ Encoding*************************************************
* Rules for HZ encoding
* 
*   In ASCII mode, a byte is interpreted as an ASCII character, unless a
*   '~' is encountered. The character '~' is an escape character. By
*   convention, it must be immediately followed ONLY by '~', '{' or '\n'
*   (<LF>), with the following special meaning.

*   1. The escape sequence '~~' is interpreted as a '~'.
*   2. The escape-to-GB sequence '~{' switches the mode from ASCII to GB.
*   3. The escape sequence '~\n' is a line-continuation marker to be
*     consumed with no output produced.
*   In GB mode, characters are interpreted two bytes at a time as (pure)
*   GB codes until the escape-from-GB code '~}' is read. This code
*   switches the mode from GB back to ASCII.  (Note that the escape-
*   from-GB code '~}' ($7E7D) is outside the defined GB range.)
*
*   Source: RFC 1842
*/

U_CFUNC void UConverter_toUnicode_HZ(UConverterToUnicodeArgs *args,
                                              UErrorCode* err){
    char tempBuf[3];
    const char* pBuf;
    const char *mySource = ( char *) args->source;
    UChar *myTarget = args->target;
    char *tempLimit = &tempBuf[2]+1; 
    int32_t mySourceIndex = 0;
    int32_t myTargetIndex = 0;
    const char *mySourceLimit = args->sourceLimit;
    UChar32 targetUniChar = 0x0000;
    UChar mySourceChar = 0x0000;
    UConverterDataHZ* myData=(UConverterDataHZ*)(args->converter->extraInfo);
    
    
    /*Arguments Check*/
    if (U_FAILURE(*err)) 
        return;
    
    if ((args->converter == NULL) || (args->targetLimit < args->target) || (args->sourceLimit < args->source)){
        *err = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
    
    while(mySource< args->sourceLimit){
        
        if(myTarget < args->targetLimit){
            
            mySourceChar= (unsigned char) *mySource++;
            
            /*if( mySourceChar <= 0x20){
                myData->isStateDBCS = FALSE;
            }*/

            switch(mySourceChar){
                case 0x0A:
                    if(args->converter->mode ==UCNV_TILDE){
                        args->converter->mode=0;
                        
                    }
                    *(myTarget++)=(UChar)mySourceChar;
                    continue;
            
                case UCNV_TILDE:
                    if(args->converter->mode ==UCNV_TILDE){
                        *(myTarget++)=(UChar)mySourceChar;
                        args->converter->mode=0;
                        continue;
                        
                    }
                    else if(args->converter->toUnicodeStatus !=0){
                        args->converter->mode=0;
                        break;
                    }
                    else{
                        args->converter->mode = UCNV_TILDE;
                        continue;
                    }
                
                
                case UCNV_OPEN_BRACE:
                    if(args->converter->mode == UCNV_TILDE){
                        args->converter->mode=0;
                        myData->isStateDBCS = TRUE;
                        continue;
                    }
                    else{
                        break;
                    }
               
                
                case UCNV_CLOSE_BRACE:
                    if(args->converter->mode == UCNV_TILDE){
                        args->converter->mode=0;
                        myData->isStateDBCS = FALSE;
                        continue;
                    }
                    else{
                        break;
                    }
                
                default: 
                    /* if the first byte is equal to TILDE and the trail byte
                     * is not a valid byte then it is an error condition
                     */
                    if(args->converter->mode == UCNV_TILDE){
                        args->converter->mode=0;
                        mySourceChar= (UChar)(((UCNV_TILDE+0x80) << 8) | ((mySourceChar & 0x00ff)+0x80));
                        goto SAVE_STATE;
                    }
                    
                    break;

            }
             
            if(myData->isStateDBCS){
                if(args->converter->toUnicodeStatus == 0x00){
                    args->converter->toUnicodeStatus = (UChar) mySourceChar;
                    continue;
                }
                else{
                    tempBuf[0] =	(char) (args->converter->toUnicodeStatus +0x80);
                    tempBuf[1] =	(char) (mySourceChar+0x80);
                    mySourceChar= (UChar)(((args->converter->toUnicodeStatus+0x80) << 8) | ((mySourceChar & 0x00ff)+0x80));
                    args->converter->toUnicodeStatus =0x00;
                    pBuf = &tempBuf[0];
                    tempLimit = &tempBuf[2]+1;
                    targetUniChar = _MBCSSimpleGetNextUChar(myData->gbConverter->sharedData,
                        &pBuf,tempLimit,args->converter->useFallback);
                }
            }
            else{
                if(args->converter->fromUnicodeStatus == 0x00){
                    tempBuf[0] = (char) mySourceChar;
                    pBuf = &tempBuf[0];
                    tempLimit = &tempBuf[1];
                    targetUniChar = _MBCSSimpleGetNextUChar(myData->gbConverter->sharedData,
                        &pBuf,tempLimit,args->converter->useFallback);
                }
                else{
                    goto SAVE_STATE;
                }

            }
            if(targetUniChar < 0xfffe){
                *(myTarget++)=(UChar)targetUniChar;
            }
            else if(targetUniChar>=0xfffe){
SAVE_STATE:
                {
                    const char *saveSource = args->source;
                    UChar *saveTarget = args->target;
                    int32_t *saveOffsets = args->offsets;
                    UConverterCallbackReason reason;
                
                    if(targetUniChar == 0xfffe){
                        reason = UCNV_UNASSIGNED;
                        *err = U_INVALID_CHAR_FOUND;
                    }
                    else{
                        reason = UCNV_ILLEGAL;
                        *err = U_ILLEGAL_CHAR_FOUND;
                    }
                    if(myData->isStateDBCS){

                        args->converter->invalidCharBuffer[args->converter->invalidCharLength++] = (char)(tempBuf[0]-0x80);
                        args->converter->invalidCharBuffer[args->converter->invalidCharLength++] = (char)(tempBuf[1]-0x80);                    
                    }
                    else{
                        args->converter->invalidCharBuffer[args->converter->invalidCharLength++] = (char)mySourceChar;
                    }
                    args->target = myTarget;
                    args->source = mySource;
                    ToU_CALLBACK_MACRO( args->converter->toUContext,
                        args,
                        args->converter->invalidCharBuffer,
                        args->converter->invalidCharLength,
                        reason,
                        err);
                    myTarget = args->target;
                    args->source  =	saveSource;
                    args->target  =	saveTarget;
                    args->offsets =	saveOffsets;
                    args->converter->invalidCharLength=0;
                    if(U_FAILURE(*err))
                        break;

                }
            }
        }
        else{
            *err =U_BUFFER_OVERFLOW_ERROR;
            break;
        }
    }
    if((args->flush==TRUE)
        && (mySource == mySourceLimit) 
        && ( args->converter->toUnicodeStatus !=0x00)){
        if(U_SUCCESS(*err)){
            *err = U_TRUNCATED_CHAR_FOUND;
            args->converter->toUnicodeStatus = 0x00;
        }
    }
    /* Reset the state of converter if we consumed 
     * the source and flush is true
     */
    if( (mySource == mySourceLimit) && args->flush){
        _HZReset(args->converter);
    }

    args->target = myTarget;
    args->source = mySource;
}


U_CFUNC void UConverter_toUnicode_HZ_OFFSETS_LOGIC(UConverterToUnicodeArgs *args,
                                                            UErrorCode* err){
    char tempBuf[3];
    const char* pBuf;
    const char *mySource = ( char *) args->source;
    UChar *myTarget = args->target;
    char *tempLimit = &tempBuf[3]; 
    int32_t mySourceIndex = 0;
    int32_t myTargetIndex = 0;
    const char *mySourceLimit = args->sourceLimit;
    UChar32 targetUniChar = 0x0000;
    UChar mySourceChar = 0x0000;
    UConverterDataHZ* myData=(UConverterDataHZ*)(args->converter->extraInfo);
    
    /*Arguments Check*/
    if (U_FAILURE(*err)) 
        return;
    
    if ((args->converter == NULL) || (args->targetLimit < args->target) || (args->sourceLimit < args->source)){
        *err = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
    
    while(mySource< args->sourceLimit){
        
        if(myTarget < args->targetLimit){
            
            mySourceChar= (unsigned char) *mySource++;
            
            
            /*if( mySourceChar <= 0x20){
                myData->isStateDBCS = FALSE;
            }*/

            switch(mySourceChar){
                case 0x0A:
                    if(args->converter->mode ==UCNV_TILDE){
                        args->converter->mode=0;
                        
                    }
                    *(myTarget++)=(UChar)mySourceChar;
                    continue;
            
                case UCNV_TILDE:
                    if(args->converter->mode ==UCNV_TILDE){
                        *(myTarget++)=(UChar)mySourceChar;
                        args->converter->mode=0;
                        continue;
                        
                    }
                    else if(args->converter->toUnicodeStatus !=0){
                        args->converter->mode=0;
                        break;
                    }
                    else{
                        args->converter->mode = UCNV_TILDE;
                        continue;
                    }
                
                
                case UCNV_OPEN_BRACE:
                    if(args->converter->mode == UCNV_TILDE){
                        args->converter->mode=0;
                        myData->isStateDBCS = TRUE;
                        continue;
                    }
                    else{
                        break;
                    }
               
                
                case UCNV_CLOSE_BRACE:
                    if(args->converter->mode == UCNV_TILDE){
                        args->converter->mode=0;
                         myData->isStateDBCS = FALSE;
                        continue;
                    }
                    else{
                        break;
                    }
                
                default:
                     /* if the first byte is equal to TILDE and the trail byte
                     * is not a valid byte then it is an error condition
                     */
                    if(args->converter->mode == UCNV_TILDE){
                        args->converter->mode=0;
                        mySourceChar= (UChar)(((UCNV_TILDE+0x80) << 8) | ((mySourceChar & 0x00ff)+0x80));
                        goto SAVE_STATE;
                    }
                    
                    break;

            }
             
            if(myData->isStateDBCS){
                if(args->converter->toUnicodeStatus == 0x00){
                    args->converter->toUnicodeStatus = (UChar) mySourceChar;
                    continue;
                }
                else{
                    tempBuf[0] =	(char) (args->converter->toUnicodeStatus+0x80) ;
                    tempBuf[1] =	(char) (mySourceChar+0x80);
                    mySourceChar= (UChar)(((args->converter->toUnicodeStatus+0x80) << 8) | ((mySourceChar & 0x00ff)+0x80));
                    args->converter->toUnicodeStatus =0x00;
                    pBuf = &tempBuf[0];
                    tempLimit = &tempBuf[2]+1;
                    targetUniChar = _MBCSSimpleGetNextUChar(myData->gbConverter->sharedData,
                        &pBuf,tempLimit,args->converter->useFallback);
                }
            }
            else{
                if(args->converter->fromUnicodeStatus == 0x00){
                    tempBuf[0] = (char) mySourceChar;
                    pBuf = &tempBuf[0];
                    tempLimit = &tempBuf[1];
                    targetUniChar = _MBCSSimpleGetNextUChar(myData->gbConverter->sharedData,
                        &pBuf,tempLimit,args->converter->useFallback);
                }
                else{
                    goto SAVE_STATE;
                }

            }
            if(targetUniChar < 0xfffe){
                if(myData->isStateDBCS){
                    args->offsets[myTarget - args->target]=	mySource - args->source - 2;
                }
                else{
                    args->offsets[myTarget - args->target]=	mySource - args->source - 1;
                }
                *(myTarget++)=(UChar)targetUniChar;
            }
            else if(targetUniChar>=0xfffe){
SAVE_STATE:
                {
                   const char *saveSource = args->source;
                    UChar *saveTarget = args->target; 
                    int32_t *saveOffsets = args->offsets;
                    
                    UConverterCallbackReason reason;
                    int32_t currentOffset ;
                    int32_t My_i = myTarget - args->target;
                    
                    if(targetUniChar == 0xfffe){
                        reason = UCNV_UNASSIGNED;
                        *err = U_INVALID_CHAR_FOUND;
                    }
                    else{
                        reason = UCNV_ILLEGAL;
                        *err = U_ILLEGAL_CHAR_FOUND;
                    }
                    if(myData->isStateDBCS){

                        args->converter->invalidCharBuffer[args->converter->invalidCharLength++] = (char)(tempBuf[0]-0x80);
                        args->converter->invalidCharBuffer[args->converter->invalidCharLength++] = (char)(tempBuf[1]-0x80);
                        currentOffset=	mySource - args->source -2;
                    
                    }
                    else{
                        args->converter->invalidCharBuffer[args->converter->invalidCharLength++] = (char)mySourceChar;
                        currentOffset=	mySource - args->source -1;
                    }
                    args->offsets = args->offsets?args->offsets+(myTarget - args->target):0;
                    args->target = myTarget;
                    args->source = mySource;
                    myTarget = saveTarget;
                    ToU_CALLBACK_OFFSETS_LOGIC_MACRO( args->converter->toUContext,
                        args,
                        args->converter->invalidCharBuffer,
                        args->converter->invalidCharLength,
                        reason,
                        err);
                    args->converter->invalidCharLength=0;
                    args->source  =	saveSource;
                    myTarget = args->target;
                    args->target  =	saveTarget;
                    args->offsets =	saveOffsets;
                    if(U_FAILURE(*err))
                        break;
                }
            }
        }
        else{
            *err =U_BUFFER_OVERFLOW_ERROR;
            break;
        }
    }
    if((args->flush==TRUE)
        && (mySource == mySourceLimit) 
        && ( args->converter->toUnicodeStatus !=0x00)){
        if(U_SUCCESS(*err)){
            *err = U_TRUNCATED_CHAR_FOUND;
            args->converter->toUnicodeStatus = 0x00;
        }
    }
    /* Reset the state of converter if we consumed 
     * the source and flush is true
     */
    if( (mySource == mySourceLimit) && args->flush){
         _HZReset(args->converter);
    }

    args->target = myTarget;
    args->source = mySource;
}

static void concatEscape(UConverterFromUnicodeArgs* args, int32_t *targetIndex, int32_t *targetLength,
                         const	char* strToAppend,UErrorCode* err,int len,int32_t *sourceIndex){
    while(len-->0){
        if(*targetIndex < *targetLength){
            args->target[*targetIndex] = (unsigned char) *strToAppend;
            if(args->offsets!=NULL){
                args->offsets[*targetIndex] = *sourceIndex-1;
            }
            (*targetIndex)++;
        }
        else{
            args->converter->charErrorBuffer[(int)args->converter->charErrorBufferLength++] = (unsigned char) *strToAppend;
            *err =U_BUFFER_OVERFLOW_ERROR;
        }
        strToAppend++;
    }
}

static void concatString(UConverterFromUnicodeArgs* args, int32_t *targetIndex, int32_t *targetLength,
                         const	UChar32* strToAppend,UErrorCode* err, int32_t *sourceIndex){
    
    if(*strToAppend < 0x00FF){
        if( (*targetIndex)+1 >= *targetLength){
            args->converter->charErrorBuffer[args->converter->charErrorBufferLength++] = (unsigned char) *strToAppend;
            *err = U_BUFFER_OVERFLOW_ERROR;
        }else{
            args->target[*targetIndex] = (unsigned char) *strToAppend;
            
            if(args->offsets!=NULL){
                args->offsets[*targetIndex] = *sourceIndex-1;
            }
            (*targetIndex)++;
            
        }
    }
    else{
        if(*targetIndex < *targetLength){
            args->target[*targetIndex] =(unsigned char) ((*strToAppend>>8)-0x80 );
            if(args->offsets!=NULL){
                args->offsets[*targetIndex] = *sourceIndex-1;
            }
            (*targetIndex)++;
            
            if(*targetIndex < *targetLength){
                args->target[(*targetIndex)] =(unsigned char) ((*strToAppend & 0x00FF)-0x80);
                
                if(args->offsets!=NULL){
                    args->offsets[*targetIndex]	= *sourceIndex-1;
                }
                (*targetIndex)++;
            }
            else{
                args->converter->charErrorBuffer[args->converter->charErrorBufferLength++] = (unsigned char) ((*strToAppend & 0x00FF)-0x80);
                *err = U_BUFFER_OVERFLOW_ERROR;
                
            }
            
        }
        else{
            args->converter->charErrorBuffer[args->converter->charErrorBufferLength++] = (unsigned char) ((*strToAppend>>8)-0x80);
            args->converter->charErrorBuffer[args->converter->charErrorBufferLength++] = (unsigned char) ((*strToAppend & 0x00FF)-0x80);
            *err = U_BUFFER_OVERFLOW_ERROR;
            if(args->offsets!=NULL){
                args->offsets[*targetIndex] = *sourceIndex-1;
                
            }
        }
    }
    
}

U_CFUNC void UConverter_fromUnicode_HZ(UConverterFromUnicodeArgs *args, UErrorCode *err){


    const UChar *mySource = args->source;
    unsigned char *myTarget = (unsigned char *) args->target;
    int32_t mySourceIndex = 0;
    int32_t myTargetIndex = 0;
    uint32_t targetValue=0;
    int32_t targetLength = args->targetLimit - args->target;
    int32_t sourceLength = args->sourceLimit - args->source;
    int32_t length=0;
    UChar32 targetUniChar = 0x0000;
    UChar32 mySourceChar = 0x0000,c=0x0000;
    UConverterDataHZ *myConverterData=(UConverterDataHZ*)args->converter->extraInfo;
    UBool isTargetUCharDBCS = (UBool) myConverterData->isTargetUCharDBCS;
    UBool oldIsTargetUCharDBCS = isTargetUCharDBCS;
    UConverterCallbackReason reason;
    UBool isEscapeAppended =FALSE;
    
    /*Arguments Check*/
    if (U_FAILURE(*err)) 
        return;
    
    if ((args->converter == NULL) || (args->targetLimit < args->target) || (args->sourceLimit < args->source)){
        *err = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
    if(args->converter->fromUSurrogateLead!=0 && myTargetIndex < targetLength) {
        goto getTrail;
    }
    /*writing the char to the output stream */
    while (mySourceIndex < sourceLength){
        
        if (myTargetIndex < targetLength){
            
            c=mySourceChar = (UChar) args->source[mySourceIndex++];
            
            /*Handle surrogates */
            if(UTF_IS_SURROGATE(mySourceChar)) {
                if(UTF_IS_SURROGATE_FIRST(mySourceChar)) {
                    args->converter->fromUSurrogateLead = (UChar)mySourceChar;
getTrail:
                    /*look ahead to find the trail surrogate*/
                    if(mySourceIndex <  sourceLength) {
                        /* test the following code unit */
                        UChar trail=(UChar) args->source[mySourceIndex];
                        if(UTF_IS_SECOND_SURROGATE(trail)) {
                            ++mySourceIndex;
                            mySourceChar=UTF16_GET_PAIR_VALUE(mySourceChar, trail);
                            args->converter->fromUSurrogateLead = 0x00;
                            /* convert this surrogate code point */
                            /* exit this condition tree */
                        } else {
                            /* this is an unmatched lead code unit (1st surrogate) */
                            /* callback(illegal) */
                            reason=UCNV_ILLEGAL;
                            *err=U_ILLEGAL_CHAR_FOUND;
                            goto CALLBACK;
                        }
                    } else {
                        /* no more input */
                        break;
                    }
                } else {
                    /* this is an unmatched trail code unit (2nd surrogate) */
                    /* callback(illegal) */
                    reason=UCNV_ILLEGAL;
                    *err=U_ILLEGAL_CHAR_FOUND;
                    goto CALLBACK;
                }
            }
            oldIsTargetUCharDBCS = isTargetUCharDBCS;
            if(mySourceChar == 0x7E){
                concatEscape(args, &myTargetIndex, &targetLength,"\x7E\x7E",err,2,&mySourceIndex);
                continue;
            }
            else{
                length= _MBCSFromUChar32(myConverterData->gbConverter->sharedData,
                    mySourceChar,&targetValue,args->converter->useFallback);
                targetUniChar = (UChar32) targetValue;

            }
            /* only DBCS or SBCS characters are expected*/
            if(length > 2 || length==0){
                reason =UCNV_ILLEGAL;
                *err =U_INVALID_CHAR_FOUND;
                goto CALLBACK;
            }
            /* DB haracters with high bit set to 1 are expected */
            if(((targetUniChar & 0x8080) != 0x8080)&& length==2){
                reason =UCNV_ILLEGAL;
                *err =U_INVALID_CHAR_FOUND;
                goto CALLBACK;
            }
            
            myConverterData->isTargetUCharDBCS = isTargetUCharDBCS = (UBool)(targetUniChar>0x00FF);
            
            if (targetUniChar != missingCharMarker){
                    
                 if(oldIsTargetUCharDBCS != isTargetUCharDBCS || !myConverterData->isEscapeAppended ){
                    /*Shifting from a double byte to single byte mode*/
                    if(!isTargetUCharDBCS){
                        concatEscape(args, &myTargetIndex, &targetLength, SB_ESCAPE,err, 2,&mySourceIndex);
                        myConverterData->isEscapeAppended =isEscapeAppended =TRUE;
                    }
                    else{ /* Shifting from a single byte to double byte mode*/
                        concatEscape(args, &myTargetIndex, &targetLength, DB_ESCAPE,err, 2,&mySourceIndex);
                        myConverterData->isEscapeAppended =isEscapeAppended =TRUE;
                        
                    }
                }
            
                concatString(args, &myTargetIndex, &targetLength,&targetUniChar,err, &mySourceIndex);

                TEST_ERROR_CONDITION(args,myTargetIndex, mySourceIndex,	isTargetUCharDBCS,myConverterData, err);
            }
            else{
                
CALLBACK:
                    {
                        const	UChar* saveSource = args->source;
                        char*	saveTarget = args->target;
                        int32_t *saveOffsets = args->offsets;
                        *err = U_INVALID_CHAR_FOUND;
                        args->converter->invalidUCharBuffer[0] = (UChar) mySourceChar;
                        args->converter->invalidUCharLength = 1;
                
                        myConverterData->isTargetUCharDBCS = isTargetUCharDBCS;
                        args->target += myTargetIndex;
                        args->source += mySourceIndex;
                        FromU_CALLBACK_MACRO(args->converter->fromUContext,
                            args,
                            args->converter->invalidUCharBuffer,
                            1,
                            (UChar32) mySourceChar,
                            UCNV_UNASSIGNED,
                            err);
                        args->source = saveSource;
                        args->target = saveTarget;
                        args->offsets = saveOffsets;
                        isTargetUCharDBCS=(UBool)myConverterData->isTargetUCharDBCS;
                        args->converter->fromUSurrogateLead =0x00;
                        myConverterData->isEscapeAppended =isEscapeAppended =FALSE;
                        args->converter->invalidUCharLength = 0;
                        if (U_FAILURE (*err)) 
                            break;
                        
                    }
            }
        }
        else{
            *err = U_BUFFER_OVERFLOW_ERROR;
            break;
        }
        targetUniChar=missingCharMarker;
    }
    /*If at the end of conversion we are still carrying state information
     *flush is TRUE, we can deduce that the input stream is truncated
     */
    if (args->converter->fromUSurrogateLead !=0 && (mySourceIndex == sourceLength) && args->flush){
        if (U_SUCCESS(*err)){
            *err = U_TRUNCATED_CHAR_FOUND;
            args->converter->toUnicodeStatus = 0x00;
        }
    }
    /* Reset the state of converter if we consumed 
     * the source and flush is true
     */
    if( (mySourceIndex == sourceLength) && args->flush){
        _HZReset(args->converter);
    }

    args->target += myTargetIndex;
    args->source += mySourceIndex;
    myConverterData->isTargetUCharDBCS = isTargetUCharDBCS;
    
    return;
}


U_CFUNC void UConverter_fromUnicode_HZ_OFFSETS_LOGIC (UConverterFromUnicodeArgs * args,
                                                      UErrorCode * err){
    const UChar *mySource = args->source;
    unsigned char *myTarget = (unsigned char *) args->target;
    int32_t mySourceIndex = 0;
    int32_t myTargetIndex = 0;
    int32_t targetLength = args->targetLimit - args->target;
    int32_t sourceLength = args->sourceLimit - args->source;
    int32_t length=0;
    UChar32 targetUniChar = 0x0000;
    UChar32 mySourceChar = 0x0000,c=0x0000;
    UConverterDataHZ *myConverterData=(UConverterDataHZ*)args->converter->extraInfo;
    UBool isTargetUCharDBCS = (UBool) myConverterData->isTargetUCharDBCS;
    UBool oldIsTargetUCharDBCS = isTargetUCharDBCS;
    UConverterCallbackReason reason;
    UBool isEscapeAppended =FALSE;
    
    /*Arguments Check*/
    if (U_FAILURE(*err)) 
        return;
    
    if ((args->converter == NULL) || (args->targetLimit < args->target) || (args->sourceLimit < args->source)){
        *err = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
    if(args->converter->fromUSurrogateLead!=0 && myTargetIndex < targetLength) {
        goto getTrail;
    }
    /*writing the char to the output stream */
    while (mySourceIndex < sourceLength){
        
        if (myTargetIndex < targetLength){
            
            c=mySourceChar = (UChar) args->source[mySourceIndex++];
            
            /*Handle surrogates */
            if(UTF_IS_SURROGATE(mySourceChar)) {
                if(UTF_IS_SURROGATE_FIRST(mySourceChar)) {
                    args->converter->fromUSurrogateLead = (UChar) mySourceChar;
getTrail:
                    /*look ahead to find the trail surrogate*/
                    if(mySourceIndex <  sourceLength) {
                        /* test the following code unit */
                        UChar trail=(UChar) args->source[mySourceIndex];
                        if(UTF_IS_SECOND_SURROGATE(trail)) {
                            ++mySourceIndex;
                            mySourceChar=UTF16_GET_PAIR_VALUE(mySourceChar, trail);
                            args->converter->fromUSurrogateLead = 0x00;
                            /* convert this surrogate code point */
                            /* exit this condition tree */
                        } else {
                            /* this is an unmatched lead code unit (1st surrogate) */
                            /* callback(illegal) */
                            reason=UCNV_ILLEGAL;
                            *err=U_ILLEGAL_CHAR_FOUND;
                            goto CALLBACK;
                        }
                    } else {
                        /* no more input */
                        break;
                    }
                } else {
                    /* this is an unmatched trail code unit (2nd surrogate) */
                    /* callback(illegal) */
                    reason=UCNV_ILLEGAL;
                    *err=U_ILLEGAL_CHAR_FOUND;
                    goto CALLBACK;
                }
            }
            oldIsTargetUCharDBCS = isTargetUCharDBCS;
            if(mySourceChar == 0x7E){
                concatEscape(args, &myTargetIndex, &targetLength,"\x7E\x7E",err,2,&mySourceIndex);
                continue;
            }
            else{
                length= _MBCSFromUChar32(myConverterData->gbConverter->sharedData,
                    mySourceChar,&targetUniChar,args->converter->useFallback);

            }
            /* only DBCS or SBCS characters are expected*/
            if(length > 2 || length==0){
                reason =UCNV_ILLEGAL;
                *err =U_INVALID_CHAR_FOUND;
                goto CALLBACK;
            }
            /* DB haracters with high bit set to 1 are expected */
            if(((targetUniChar & 0x8080) != 0x8080)&& length==2){
                reason =UCNV_ILLEGAL;
                *err =U_INVALID_CHAR_FOUND;
                goto CALLBACK;
            }
            
            myConverterData->isTargetUCharDBCS = isTargetUCharDBCS = (UBool)(targetUniChar>0x00FF);
            
            if (targetUniChar != missingCharMarker){
                    
                 if(oldIsTargetUCharDBCS != isTargetUCharDBCS || !myConverterData->isEscapeAppended ){
                    /*Shifting from a double byte to single byte mode*/
                    if(!isTargetUCharDBCS){
                        concatEscape(args, &myTargetIndex, &targetLength, SB_ESCAPE,err, 2,&mySourceIndex);
                        myConverterData->isEscapeAppended =isEscapeAppended =TRUE;
                    }
                    else{ /* Shifting from a single byte to double byte mode*/
                        concatEscape(args, &myTargetIndex, &targetLength, DB_ESCAPE,err, 2,&mySourceIndex);
                        myConverterData->isEscapeAppended =isEscapeAppended =TRUE;
                        
                    }
                }
            
                concatString(args, &myTargetIndex, &targetLength,&targetUniChar,err, &mySourceIndex);

                TEST_ERROR_CONDITION(args,myTargetIndex, mySourceIndex,	isTargetUCharDBCS,myConverterData, err);
            }
            else{
                
CALLBACK:
                {
                    int32_t currentOffset = args->offsets[myTargetIndex-1]+1;
                    char * saveTarget = args->target;
                    const UChar* saveSource = args->source;
                    int32_t *saveOffsets = args->offsets;
                    *err = U_INVALID_CHAR_FOUND;
                    args->converter->invalidUCharBuffer[0] = (UChar) mySourceChar;
                    args->converter->invalidUCharLength = 1;
                
                    /* Breaks out of the loop since behaviour was set to stop */
                    args->converter->fromUnicodeStatus = (int32_t)isTargetUCharDBCS;
                    args->target +=	myTargetIndex;
                    args->source +=	mySourceIndex;
                    args->offsets =	args->offsets?args->offsets+myTargetIndex:0;
                    FromU_CALLBACK_OFFSETS_LOGIC_MACRO(args->converter->fromUContext,
                        args,
                        args->converter->invalidUCharBuffer,
                        1,
                        (UChar32)mySourceChar,
                        UCNV_UNASSIGNED,
                        err);
                    args->source = saveSource;
                    args->target = saveTarget;
                    args->offsets =	saveOffsets;
                    isTargetUCharDBCS=(UBool)myConverterData->isTargetUCharDBCS;
                    args->converter->fromUSurrogateLead =0x00;
                    myConverterData->isEscapeAppended =isEscapeAppended =FALSE;
                    args->converter->invalidUCharLength = 0;
                    if (U_FAILURE (*err))     
                        break;
                    
                }
            }
        }
        else{
            *err = U_BUFFER_OVERFLOW_ERROR;
            break;
        }
        targetUniChar=missingCharMarker;
    }
    /*If at the end of conversion we are still carrying state information
     *flush is TRUE, we can deduce that the input stream is truncated
     */
    if (args->converter->fromUSurrogateLead !=0 && (mySourceIndex == sourceLength) && args->flush){
        if (U_SUCCESS(*err)){
            *err = U_TRUNCATED_CHAR_FOUND;
            args->converter->toUnicodeStatus = 0x00;
        }
    }
    /* Reset the state of converter if we consumed 
     * the source and flush is true
     */
    if( (mySourceIndex == sourceLength) && args->flush){
        _HZReset(args->converter);
    }

    args->target += myTargetIndex;
    args->source += mySourceIndex;
    myConverterData->isTargetUCharDBCS = isTargetUCharDBCS;
    
    return;
}

U_CFUNC UChar32 UConverter_getNextUChar_HZ (UConverterToUnicodeArgs * pArgs,
                                            UErrorCode *pErrorCode){
    UChar buffer[UTF_MAX_CHAR_LENGTH];
    const char *realLimit=pArgs->sourceLimit;
    
    pArgs->target=buffer;
    pArgs->targetLimit=buffer+UTF_MAX_CHAR_LENGTH;
    
    while(pArgs->source<realLimit) {
        /* feed in one byte at a time to make sure to get only one character out */
        pArgs->sourceLimit=pArgs->source+1;
        pArgs->flush= (UBool)(pArgs->sourceLimit==realLimit);
        UConverter_toUnicode_HZ(pArgs, pErrorCode);
        if(U_FAILURE(*pErrorCode) && *pErrorCode!=U_BUFFER_OVERFLOW_ERROR) {
            return 0xffff;
        } else if(pArgs->target!=buffer) {
            if(*pErrorCode==U_BUFFER_OVERFLOW_ERROR) {
                *pErrorCode=U_ZERO_ERROR;
            }
            return ucnv_getUChar32KeepOverflow(pArgs->converter, buffer, pArgs->target-buffer);
        }
    }
    
    /* no output because of empty input or only state changes and skipping callbacks */
    *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
    return 0xffff;
}
