/*
******************************************************************************
*
*   Copyright (C) 2001-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
*
* File ustrtrns.c
*
* Modification History:
*
*   Date        Name        Description
*   9/10/2001    Ram    Creation.
******************************************************************************
*/

/*******************************************************************************
 *
 * u_strTo* and u_strFrom* APIs
 *
 *******************************************************************************
 */


#include "unicode/putil.h"
#include "unicode/ucnv.h"
#include "unicode/ustring.h"
#include "cstring.h"
#include "cwchar.h"
#include "cmemory.h"
#include "ustr_imp.h"


static U_INLINE UBool 
u_growAnyBufferFromStatic(void *context,
                       void **pBuffer, int32_t *pCapacity, int32_t reqCapacity,
                       int32_t length, int32_t size) {

    void *newBuffer=uprv_malloc(reqCapacity*size);
    if(newBuffer!=NULL) {
        if(length>0) {
            uprv_memcpy(newBuffer, *pBuffer, length*size);
        }
        *pCapacity=reqCapacity;
    } else {
        *pCapacity=0;
    }

    /* release the old pBuffer if it was not statically allocated */
    if(*pBuffer!=(void *)context) {
        uprv_free(*pBuffer);
    }

    *pBuffer=newBuffer;
    return (UBool)(newBuffer!=NULL);
}

#define _STACK_BUFFER_CAPACITY 1000
#define _BUFFER_CAPACITY_MULTIPLIER 2

U_CAPI UChar* U_EXPORT2 
u_strFromUTF32(UChar   *dest,
               int32_t destCapacity, 
               int32_t *pDestLength,
               const UChar32 *src,
               int32_t srcLength,
               UErrorCode *pErrorCode)
{
    int32_t reqLength = 0;
    uint32_t ch =0;
    UChar *pDestLimit =dest+destCapacity;
    UChar *pDest = dest;
    const uint32_t *pSrc = (const uint32_t *)src;

    /* args check */
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)){
        return NULL;
    }
    
    if((src==NULL) || (srcLength < -1) || (destCapacity<0) || (!dest && destCapacity > 0)){
        *pErrorCode = U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }

     /* Check if the source is null terminated */
    if(srcLength == -1 ){
        while(((ch=*pSrc)!=0) && (pDest < pDestLimit)){
            ++pSrc;
            if(ch<=0xFFFF){
                *(pDest++)=(UChar)ch;
            }else if(ch<=0x10ffff){
                *(pDest++)=UTF16_LEAD(ch);
                if(pDest<pDestLimit){
                    *(pDest++)=UTF16_TRAIL(ch);
                }else{
                    reqLength++;
                    break;
                }
            }else{
                *pErrorCode = U_INVALID_CHAR_FOUND;
                return NULL;
            }
        }
        while((ch=*pSrc++) != 0){
            reqLength+=UTF_CHAR_LENGTH(ch);
        }
    }else{
        const uint32_t* pSrcLimit = ((const uint32_t*)pSrc) + srcLength;
        while((pSrc < pSrcLimit) && (pDest < pDestLimit)){
            ch = *pSrc++;
            if(ch<=0xFFFF){
                *(pDest++)=(UChar)ch;
            }else if(ch<=0x10FFFF){
                *(pDest++)=UTF16_LEAD(ch);
                if(pDest<pDestLimit){
                    *(pDest++)=UTF16_TRAIL(ch);
                }else{
                    reqLength++;
                    break;
                }
            }else{
                *pErrorCode = U_INVALID_CHAR_FOUND;
                return NULL;
            }
        }
        while(pSrc <pSrcLimit){
            ch = *pSrc++;
            reqLength+=UTF_CHAR_LENGTH(ch);
        }
    }

    reqLength += pDest - dest;
    if(pDestLength){
        *pDestLength = reqLength;
    }

    /* Terminate the buffer */
    u_terminateUChars(dest,destCapacity,reqLength,pErrorCode); 
    
    return dest;
}


U_CAPI UChar32* U_EXPORT2 
u_strToUTF32(UChar32 *dest, 
             int32_t  destCapacity,
             int32_t  *pDestLength,
             const UChar *src, 
             int32_t  srcLength,
             UErrorCode *pErrorCode)
{
    const UChar* pSrc = src;
    const UChar* pSrcLimit;
    int32_t reqLength=0;
    uint32_t ch=0;
    uint32_t *pDest = (uint32_t *)dest;
    uint32_t *pDestLimit = pDest + destCapacity;
    UChar ch2=0;

    /* args check */
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)){
        return NULL;
    }
    
    
    if((src==NULL) || (srcLength < -1) || (destCapacity<0) || (!dest && destCapacity > 0)){
        *pErrorCode = U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }

    if(srcLength==-1) {
        while((ch=*pSrc)!=0 && pDest!=pDestLimit) {
            ++pSrc;
            /*need not check for NUL because NUL fails UTF_IS_TRAIL() anyway*/
            if(UTF_IS_LEAD(ch) && UTF_IS_TRAIL(ch2=*pSrc)) { 
                ++pSrc;
                ch=UTF16_GET_PAIR_VALUE(ch, ch2);
            }
            *(pDest++)= ch;
        }
        while((ch=*pSrc++)!=0) {
            if(UTF_IS_LEAD(ch) && UTF_IS_TRAIL(ch2=*pSrc)) {
                ++pSrc;
            }
            ++reqLength; 
        }
    } else {
        pSrcLimit = pSrc+srcLength;
        while(pSrc<pSrcLimit && pDest<pDestLimit) {
            ch=*pSrc++;
            if(UTF_IS_LEAD(ch) && pSrc<pSrcLimit && UTF_IS_TRAIL(ch2=*pSrc)) {
                ++pSrc;
                ch=UTF16_GET_PAIR_VALUE(ch, ch2);
            }
            *(pDest++)= ch;
        }
        while(pSrc!=pSrcLimit) {
            ch=*pSrc++;
            if(UTF_IS_LEAD(ch) && pSrc<pSrcLimit && UTF_IS_TRAIL(ch2=*pSrc)) {
                ++pSrc;
            }
            ++reqLength;
        }
    }

    reqLength+=(pDest - (uint32_t *)dest);
    if(pDestLength){
        *pDestLength = reqLength;
    }

    /* Terminate the buffer */
    u_terminateUChar32s(dest,destCapacity,reqLength,pErrorCode);

    return dest;
}

U_CAPI UChar* U_EXPORT2
u_strFromUTF8(UChar *dest,             
              int32_t destCapacity,
              int32_t *pDestLength,
              const char* src, 
              int32_t srcLength,
              UErrorCode *pErrorCode){

    UChar *pDest = dest;
    UChar *pDestLimit = dest+destCapacity;
    UChar32 ch=0;
    int32_t index = 0;
    int32_t reqLength = 0;
    uint8_t* pSrc = (uint8_t*) src;

    /* args check */
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)){
        return NULL;
    }
        
    if((src==NULL) || (srcLength < -1) || (destCapacity<0) || (!dest && destCapacity > 0)){
        *pErrorCode = U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }

    if(srcLength == -1){
       srcLength = uprv_strlen((char*)pSrc);
    }
    
    while((index < srcLength)&&(pDest<pDestLimit)){
        ch = pSrc[index++];
        if(ch <=0x7f){
            *pDest++=(UChar)ch;
        }else{
            ch=utf8_nextCharSafeBody(pSrc, &index, srcLength, ch, -1);
            if(ch<0){
                *pErrorCode = U_INVALID_CHAR_FOUND;
                return NULL;
            }else if(ch<=0xFFFF){
                *(pDest++)=(UChar)ch;
            }else{
                *(pDest++)=UTF16_LEAD(ch);
                if(pDest<pDestLimit){
                    *(pDest++)=UTF16_TRAIL(ch);
                }else{
                    reqLength++;
                    break;
                }
            }
        }
    }
    /* donot fill the dest buffer just count the UChars needed */
    while(index < srcLength){
        ch = pSrc[index++];
        if(ch <= 0x7f){
            reqLength++;
        }else{
            ch=utf8_nextCharSafeBody(pSrc, &index, srcLength, ch, -1);
            if(ch<0){
                *pErrorCode = U_INVALID_CHAR_FOUND;
                return NULL;
            }
            reqLength+=UTF_CHAR_LENGTH(ch);
        }
    }

    reqLength+=(pDest - dest);

    if(pDestLength){
        *pDestLength = reqLength;
    }

    /* Terminate the buffer */
    u_terminateUChars(dest,destCapacity,reqLength,pErrorCode);

    return dest;
}

static U_INLINE uint8_t *
_appendUTF8(uint8_t *pDest, UChar32 c) {
    /* c<=0x7f is handled by the caller, here it is 0x80<=c<=0x10ffff */
    if((c)<=0x7ff) {
        *pDest++=(uint8_t)((c>>6)|0xc0);
        *pDest++=(uint8_t)((c&0x3f)|0x80);
    } else if((uint32_t)(c)<=0xffff) {
        *pDest++=(uint8_t)((c>>12)|0xe0);
        *pDest++=(uint8_t)(((c>>6)&0x3f)|0x80);
        *pDest++=(uint8_t)(((c)&0x3f)|0x80);
    } else /* if((uint32_t)(c)<=0x10ffff) */ {
        *pDest++=(uint8_t)(((c)>>18)|0xf0);
        *pDest++=(uint8_t)((((c)>>12)&0x3f)|0x80);
        *pDest++=(uint8_t)((((c)>>6)&0x3f)|0x80);
        *pDest++=(uint8_t)(((c)&0x3f)|0x80);
    }
    return pDest;
}

   
U_CAPI char* U_EXPORT2 
u_strToUTF8(char *dest,           
            int32_t destCapacity,
            int32_t *pDestLength,
            const UChar *pSrc, 
            int32_t srcLength,
            UErrorCode *pErrorCode){

    int32_t reqLength=0;
    const UChar *pSrcLimit;
    uint32_t ch=0,ch2=0;
    uint8_t *pDest = (uint8_t *)dest;
    uint8_t *pDestLimit = pDest + destCapacity;


    /* args check */
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)){
        return NULL;
    }
        
    if((pSrc==NULL) || (srcLength < -1) || (destCapacity<0) || (!dest && destCapacity > 0)){
        *pErrorCode = U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }

    if(srcLength==-1) {
        while((ch=*pSrc)!=0 && pDest!=pDestLimit) {
            ++pSrc;
            if(ch <= 0x7f) {
                *pDest++ = (char)ch;
                ++reqLength;
                continue;
            }

            /*need not check for NUL because NUL fails UTF_IS_TRAIL() anyway*/
            if(UTF_IS_SURROGATE(ch)) {
                if(UTF_IS_SURROGATE_FIRST(ch) && UTF_IS_TRAIL(ch2=*pSrc)) { 
                    ++pSrc;
                    ch=UTF16_GET_PAIR_VALUE(ch, ch2);
                } else {
                    /* Unicode 3.2 forbids surrogate code points in UTF-8 */
                    *pErrorCode = U_INVALID_CHAR_FOUND;
                    return NULL;
                }
            }
            reqLength += UTF8_CHAR_LENGTH(ch);
            /* do we have enough room in destination? */
            if(destCapacity< reqLength){
                break;
            }
            /* convert and append*/
            pDest=_appendUTF8(pDest, ch);
        }
        while((ch=*pSrc++)!=0) {
            if(ch<=0x7f) {
                ++reqLength;
            } else if(ch<=0x7ff) {
                reqLength+=2;
            } else if(!UTF_IS_SURROGATE(ch)) {
                reqLength+=3;
            } else if(UTF_IS_SURROGATE_FIRST(ch) && UTF_IS_TRAIL(ch2=*pSrc)) {
                ++pSrc;
                reqLength+=4;
            } else {
                /* Unicode 3.2 forbids surrogate code points in UTF-8 */
                *pErrorCode = U_INVALID_CHAR_FOUND;
                return NULL;
            }
        }
    } else {
        pSrcLimit = pSrc+srcLength;
        while(pSrc<pSrcLimit && pDest<pDestLimit) {
            ch=*pSrc++;
            if(ch <= 0x7f) {
                *pDest++ = (char)ch;
                ++reqLength;
                continue;
            }

            if(UTF_IS_SURROGATE(ch)) {
                if(UTF_IS_SURROGATE_FIRST(ch) && pSrc<pSrcLimit && UTF_IS_TRAIL(ch2=*pSrc)) { 
                    ++pSrc;
                    ch=UTF16_GET_PAIR_VALUE(ch, ch2);
                } else {
                    /* Unicode 3.2 forbids surrogate code points in UTF-8 */
                    *pErrorCode = U_INVALID_CHAR_FOUND;
                    return NULL;
                }
            }
            reqLength += UTF8_CHAR_LENGTH(ch);
            /* do we have enough room in destination? */
            if(destCapacity< reqLength){
                break;
            }
            /* convert and append*/
            pDest=_appendUTF8(pDest, ch);
        }
        while(pSrc<pSrcLimit) {
            ch=*pSrc++;
            if(ch<=0x7f) {
                ++reqLength;
            } else if(ch<=0x7ff) {
                reqLength+=2;
            } else if(!UTF_IS_SURROGATE(ch)) {
                reqLength+=3;
            } else if(UTF_IS_SURROGATE_FIRST(ch) && pSrc<pSrcLimit && UTF_IS_TRAIL(ch2=*pSrc)) {
                ++pSrc;
                reqLength+=4;
            } else {
                /* Unicode 3.2 forbids surrogate code points in UTF-8 */
                *pErrorCode = U_INVALID_CHAR_FOUND;
                return NULL;
            }
        }
    }

    if(pDestLength){
        *pDestLength = reqLength;
    }

    /* Terminate the buffer */
    u_terminateChars((char*)dest,destCapacity,reqLength,pErrorCode);

    return (char*)dest;
}

#if !defined(U_WCHAR_IS_UTF16) && !defined(U_WCHAR_IS_UTF32)
/* helper function */
static wchar_t* 
_strToWCS(wchar_t *dest, 
           int32_t destCapacity,
           int32_t *pDestLength,
           const UChar *src, 
           int32_t srcLength,
           UErrorCode *pErrorCode){

    char stackBuffer [_STACK_BUFFER_CAPACITY];
    char* tempBuf = stackBuffer;
    int32_t tempBufCapacity = _STACK_BUFFER_CAPACITY;
    char* tempBufLimit = stackBuffer + tempBufCapacity;
    UConverter* conv = NULL;
    char* saveBuf = tempBuf;
    wchar_t* intTarget=NULL;
    int32_t intTargetCapacity=0;
    int count=0,retVal=0;
    
    const UChar *pSrcLimit =NULL;
    const UChar *pSrc = src;

    conv = u_getDefaultConverter(pErrorCode);
    
    if(U_FAILURE(*pErrorCode)){
        return NULL;
    }
    
    if(srcLength == -1){
        srcLength = u_strlen(pSrc);
    }
    
    pSrcLimit = pSrc + srcLength;

    for(;;) {
        /* reset the error state */
        *pErrorCode = U_ZERO_ERROR;

        /* convert to chars using default converter */
        ucnv_fromUnicode(conv,&tempBuf,tempBufLimit,&pSrc,pSrcLimit,NULL,(UBool)(pSrc==pSrcLimit),pErrorCode);
        count =(tempBuf - saveBuf);
        
        /* This should rarely occur */
        if(*pErrorCode==U_BUFFER_OVERFLOW_ERROR){
            tempBuf = saveBuf;
            
            /* we dont have enough room on the stack grow the buffer */
            if(!u_growAnyBufferFromStatic(stackBuffer,(void**) &tempBuf, &tempBufCapacity, 
                (_BUFFER_CAPACITY_MULTIPLIER * (srcLength)), count,sizeof(char))){
                goto cleanup;
            }
          
           saveBuf = tempBuf;
           tempBufLimit = tempBuf + tempBufCapacity;
           tempBuf = tempBuf + count;

        } else {
            break;
        }
    }

    if(U_FAILURE(*pErrorCode)){
        goto cleanup;
    }

    /* done with conversion null terminate the char buffer */
    if(count>=tempBufCapacity){
        tempBuf = saveBuf;
        /* we dont have enough room on the stack grow the buffer */
        if(!u_growAnyBufferFromStatic(stackBuffer,(void**) &tempBuf, &tempBufCapacity, 
            tempBufCapacity-count+1, count,sizeof(char))){
            goto cleanup;
        }              
       saveBuf = tempBuf;
    }
    
    saveBuf[count]=0;
      

    /* allocate more space than required 
     * here we assume that every char requires 
     * no more than 2 wchar_ts
     */
    intTargetCapacity =  (count * _BUFFER_CAPACITY_MULTIPLIER + 1) /*for null termination */;
    intTarget = (wchar_t*)uprv_malloc( intTargetCapacity * sizeof(wchar_t) );

    if(intTarget){

        int32_t nulLen = 0;
        int32_t remaining = intTargetCapacity;
        wchar_t* pIntTarget=intTarget;
        tempBuf = saveBuf;
        
        /* now convert the mbs to wcs */
        for(;;){
            
            /* we can call the system API since we are sure that
             * there is atleast 1 null in the input
             */
            retVal = uprv_mbstowcs(pIntTarget,(tempBuf+nulLen),remaining);
            
            if(retVal==-1){
                *pErrorCode = U_INVALID_CHAR_FOUND;
                break;
            }else if(retVal== remaining){/* should never occur */
                int numWritten = (pIntTarget-intTarget);
                u_growAnyBufferFromStatic(NULL,(void**) &intTarget,
                                          &intTargetCapacity,
                                          intTargetCapacity * _BUFFER_CAPACITY_MULTIPLIER,
                                          numWritten,
                                          sizeof(wchar_t));
                pIntTarget = intTarget;
                remaining=intTargetCapacity;

                if(nulLen!=count){ /*there are embedded nulls*/
                    pIntTarget+=numWritten;
                    remaining-=numWritten;
                }

            }else{
                int32_t nulVal;
                /*scan for nulls */
                /* we donot check for limit since tempBuf is null terminated */
                while(tempBuf[nulLen++] != 0){
                }
                nulVal = (nulLen < srcLength) ? 1 : 0; 
                pIntTarget = pIntTarget + retVal+nulVal;
                remaining -=(retVal+nulVal);
            
                /* check if we have reached the source limit*/
                if(nulLen>=(count)){
                    break;
                }
            }
        }
        count = (int32_t)(pIntTarget-intTarget);
       
        if(0 < count && count <= destCapacity){
            uprv_memcpy(dest,intTarget,count*sizeof(wchar_t));
        }  

        if(pDestLength){
            *pDestLength = count;
        }

        /* free the allocated memory */
        uprv_free(intTarget);

    }else{
        *pErrorCode = U_MEMORY_ALLOCATION_ERROR;
    }
cleanup:
    /* are we still using stack buffer */
    if(stackBuffer != saveBuf){
        uprv_free(saveBuf);
    }
    u_terminateWChars(dest,destCapacity,count,pErrorCode);

    u_releaseDefaultConverter(conv);

    return dest;
}
#endif

U_CAPI wchar_t* U_EXPORT2
u_strToWCS(wchar_t *dest, 
           int32_t destCapacity,
           int32_t *pDestLength,
           const UChar *src, 
           int32_t srcLength,
           UErrorCode *pErrorCode){
    
    /* args check */
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)){
        return NULL;
    }
        
    if((src==NULL) || (srcLength < -1) || (destCapacity<0) || (!dest && destCapacity > 0)){
        *pErrorCode = U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }
    
#ifdef U_WCHAR_IS_UTF16
    /* wchar_t is UTF-16 just do a memcpy */
    if(srcLength == -1){
        srcLength = u_strlen(src);
    }
    if(0 < srcLength && srcLength <= destCapacity){
        uprv_memcpy(dest,src,srcLength*U_SIZEOF_UCHAR);
    }
    if(pDestLength){
       *pDestLength = srcLength;
    }

    u_terminateUChars(dest,destCapacity,srcLength,pErrorCode);

    return dest;

#elif defined U_WCHAR_IS_UTF32
    
    return (wchar_t*)u_strToUTF32((UChar32*)dest, destCapacity, pDestLength,
                                  src, srcLength, pErrorCode);

#else
    
    return _strToWCS(dest,destCapacity,pDestLength,src,srcLength, pErrorCode);
    
#endif

}

#if !defined(U_WCHAR_IS_UTF16) && !defined(U_WCHAR_IS_UTF32)
/* helper function */
static UChar* 
_strFromWCS( UChar   *dest,
             int32_t destCapacity, 
             int32_t *pDestLength,
             const wchar_t *src,
             int32_t srcLength,
             UErrorCode *pErrorCode){

    int32_t retVal =0, count =0 ;
    UConverter* conv = NULL;
    UChar* pTarget = NULL;
    UChar* pTargetLimit = NULL;
    UChar* target = NULL;
    
    UChar uStack [_STACK_BUFFER_CAPACITY];

    wchar_t wStack[_STACK_BUFFER_CAPACITY];
    wchar_t* pWStack = wStack;


    char cStack[_STACK_BUFFER_CAPACITY];
    int32_t cStackCap = _STACK_BUFFER_CAPACITY;
    char* pCSrc=cStack;
    char* pCSave=pCSrc;
    char* pCSrcLimit=NULL;

    const wchar_t* pSrc = src;
    const wchar_t* pSrcLimit = NULL;

    if(srcLength ==-1){
        /* if the wchar_t source is null terminated we can safely
         * assume that there are no embedded nulls, this is a fast
         * path for null terminated strings.
         */
        for(;;){
            /* convert wchars  to chars */
            retVal = uprv_wcstombs(pCSrc,src, cStackCap);
    
            if(retVal == -1){
                *pErrorCode = U_ILLEGAL_CHAR_FOUND;
                goto cleanup;
            }else if(retVal == cStackCap){
                /* Should rarely occur */
                u_growAnyBufferFromStatic(cStack,(void**)&pCSrc,&cStackCap,
                    cStackCap * _BUFFER_CAPACITY_MULTIPLIER, 0, sizeof(char));
                pCSave = pCSrc;
            }else{
                /* converted every thing */
                pCSrc = pCSrc+retVal;
                break;
            }
        }
        
    }else{
        /* here the source is not null terminated 
         * so it may have nulls embeded and we need to
         * do some extra processing 
         */
        int32_t remaining =cStackCap;
        
        pSrcLimit = src + srcLength;

        for(;;){
            register int32_t nulLen = 0;

            /* find nulls in the string */
            while(nulLen<srcLength && pSrc[nulLen++]!=0){
            }

            if((pSrc+nulLen) < pSrcLimit){
                /* check if we have enough room in pCSrc */
                if(remaining < (nulLen * MB_CUR_MAX)){
                    /* should rarely occur */
                    int32_t len = (pCSrc-pCSave);
                    pCSrc = pCSave;
                    /* we do not have enough room so grow the buffer*/
                    u_growAnyBufferFromStatic(cStack,(void**)&pCSrc,&cStackCap,
                           _BUFFER_CAPACITY_MULTIPLIER*cStackCap+(nulLen*MB_CUR_MAX),len,sizeof(char));

                    pCSave = pCSrc;
                    pCSrc = pCSave+len;
                    remaining = cStackCap-(pCSrc - pCSave);
                }

                /* we have found a null  so convert the 
                 * chunk from begining of non-null char to null
                 */
                retVal = uprv_wcstombs(pCSrc,pSrc,remaining);

                if(retVal==-1){
                    /* an error occurred bail out */
                    *pErrorCode = U_ILLEGAL_CHAR_FOUND;
                    goto cleanup;
                }

                pCSrc += retVal+1 /* already null terminated */;

                pSrc += nulLen; /* skip past the null */
                srcLength-=nulLen; /* decrement the srcLength */
                remaining -= (pCSrc-pCSave);


            }else{
                /* the source is not null terminated and we are 
                 * end of source so we copy the source to a temp buffer
                 * null terminate it and convert wchar_ts to chars
                 */
                if(nulLen > _STACK_BUFFER_CAPACITY){
                    /* Should rarely occcur */
                    /* allocate new buffer buffer */
                    pWStack =(wchar_t*) uprv_malloc(sizeof(wchar_t) * nulLen);
                    if(pWStack==NULL){
                        *pErrorCode = U_MEMORY_ALLOCATION_ERROR;
                        goto cleanup;
                    }
                }
                if(nulLen>0){
                    /* copy the contents to tempStack */
                    uprv_memcpy(pWStack,pSrc,nulLen*sizeof(wchar_t));
                }
            
                /* null terminate the tempBuffer */
                pWStack[nulLen] =0 ;
            
                if(remaining < (nulLen * MB_CUR_MAX)){
                    /* Should rarely occur */
                    int32_t len = (pCSrc-pCSave);
                    pCSrc = pCSave;
                    /* we do not have enough room so grow the buffer*/
                    u_growAnyBufferFromStatic(cStack,(void**)&pCSrc,&cStackCap,
                           cStackCap+(nulLen*MB_CUR_MAX),len,sizeof(char));

                    pCSave = pCSrc;
                    pCSrc = pCSave+len;
                    remaining = cStackCap-(pCSrc - pCSave);
                }
                /* convert to chars */
                retVal = uprv_wcstombs(pCSrc,pWStack,remaining);
            
                pCSrc += retVal;
                pSrc  += nulLen;
                srcLength-=nulLen; /* decrement the srcLength */
                break;
            }
        }
    }

    /* OK..now we have converted from wchar_ts to chars now 
     * convert chars to UChars 
     */
    pCSrcLimit = pCSrc;
    pCSrc = pCSave;
    pTarget = target= dest;
    pTargetLimit = dest + destCapacity;    
    
    conv= u_getDefaultConverter(pErrorCode);
    
    if(U_FAILURE(*pErrorCode)|| conv==NULL){
        goto cleanup;
    }
    
    for(;;) {
        
        *pErrorCode = U_ZERO_ERROR;
        
        /* convert to stack buffer*/
        ucnv_toUnicode(conv,&pTarget,pTargetLimit,(const char**)&pCSrc,pCSrcLimit,NULL,(UBool)(pCSrc==pCSrcLimit),pErrorCode);
        
        /* increment count to number written to stack */
        count+= pTarget - target;
        
        if(*pErrorCode==U_BUFFER_OVERFLOW_ERROR){
            target = uStack;
            pTarget = uStack;
            pTargetLimit = uStack + _STACK_BUFFER_CAPACITY;
        } else {
            break;
        }
        
    }
    
    if(pDestLength){
        *pDestLength =count;
    }

    u_terminateUChars(dest,destCapacity,count,pErrorCode);
    
cleanup:
 
    if(cStack != pCSave){
        uprv_free(pCSave);
    }

    if(wStack != pWStack){
        uprv_free(pWStack);
    }
    
    u_releaseDefaultConverter(conv);

    return dest;
}
#endif

U_CAPI UChar* U_EXPORT2
u_strFromWCS(UChar   *dest,
             int32_t destCapacity, 
             int32_t *pDestLength,
             const wchar_t *src,
             int32_t srcLength,
             UErrorCode *pErrorCode)
{

    /* args check */
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)){
        return NULL;
    }
        
    if((src==NULL) || (srcLength < -1) || (destCapacity<0) || (!dest && destCapacity > 0)){
        *pErrorCode = U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }

#ifdef U_WCHAR_IS_UTF16
    /* wchar_t is UTF-16 just do a memcpy */
    if(srcLength == -1){
        srcLength = u_strlen(src);
    }
    if(0 < srcLength && srcLength <= destCapacity){
        uprv_memcpy(dest,src,srcLength*U_SIZEOF_UCHAR);
    }
    if(pDestLength){
       *pDestLength = srcLength;
    }

    u_terminateUChars(dest,destCapacity,srcLength,pErrorCode);

    return dest;

#elif defined U_WCHAR_IS_UTF32
    
    return u_strFromUTF32(dest, destCapacity, pDestLength,
                          (UChar32*)src, srcLength, pErrorCode);

#else

    return _strFromWCS(dest,destCapacity,pDestLength,src,srcLength,pErrorCode);  

#endif

}
